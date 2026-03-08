@file:Suppress("DEPRECATION")

package com.stardust.pio

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * SAF (Storage Access Framework) 文件访问实现
 * 适用于用户授权特定目录的场景
 */
class SafFileProviderImpl(
    private val treeUri: Uri,
    private val rootPath: String,
    private val context: Context? = null
) : IFileProvider {

    companion object {
        private const val TAG = "SafFileProvider"
    }

    private var workingDirectory: String = rootPath

    init {
        Log.i(TAG, "Created: treeUri=$treeUri, rootPath=$rootPath")
    }

    private fun getContext(): Context? {
        if (context != null) return context
        return try {
            Class.forName("com.stardust.app.GlobalAppContext")
                .getMethod("get")
                .invoke(null) as? Context
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get GlobalAppContext", e)
            null
        }
    }

    override fun exists(path: String): Boolean = findDocumentId(path) != null

    override fun isFile(path: String): Boolean {
        val documentId = findDocumentId(path) ?: return false
        val mimeType = getMimeType(documentId)
        return mimeType != null && mimeType != DocumentsContract.Document.MIME_TYPE_DIR
    }

    override fun isDirectory(path: String): Boolean {
        val documentId = findDocumentId(path) ?: return false
        val mimeType = getMimeType(documentId)
        return mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    override fun mkdir(path: String): Boolean = createDirectory(path) != null

    override fun mkdirs(path: String): Boolean = createDirectory(path) != null

    override fun delete(path: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        val documentUri = getDocumentUri(path) ?: return false
        val ctx = getContext() ?: return false
        return try {
            DocumentsContract.deleteDocument(ctx.contentResolver, documentUri)
        } catch (e: Exception) {
            false
        }
    }

    override fun deleteRecursively(path: String): Boolean = deleteRecursive(path)

    private fun deleteRecursive(path: String): Boolean {
        if (!isDirectory(path)) return delete(path)
        listFiles(path).forEach { child ->
            if (child.isDirectory) deleteRecursive(child.path) else delete(child.path)
        }
        return delete(path)
    }

    override fun rename(path: String, newName: String): Boolean {
        val documentUri = getDocumentUri(path) ?: return false
        val ctx = getContext() ?: return false
        return try {
            DocumentsContract.renameDocument(ctx.contentResolver, documentUri, newName) != null
        } catch (e: Exception) {
            Log.e(TAG, "rename: failed", e)
            false
        }
    }

    override fun move(fromPath: String, toPath: String): Boolean {
        return copy(fromPath, toPath) && delete(fromPath)
    }

    override fun copy(fromPath: String, toPath: String): Boolean {
        val data = readBytes(fromPath) ?: return false
        return writeBytes(toPath, data)
    }

    override fun listFiles(path: String): List<IFileProvider.FileInfo> {
        val result = mutableListOf<IFileProvider.FileInfo>()
        val ctx = getContext() ?: return result
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return result

        val childrenUri = getChildrenUri(path) ?: return result
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        try {
            ctx.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val name = cursor.getString(1)
                    val mimeType = cursor.getString(2)
                    val size = cursor.getLong(3)
                    val lastModified = cursor.getLong(4)
                    val isDir = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    val childPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
                    result.add(IFileProvider.FileInfo(name, childPath, isDir, size, lastModified))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "listFiles: error=${e.message}", e)
        }
        return result
    }

    override fun read(path: String, encoding: String): String? {
        val data = readBytes(path) ?: return null
        return try { String(data, charset(encoding)) } catch (e: Exception) { null }
    }

    override fun read(path: String): String? = read(path, "UTF-8")

    override fun readBytes(path: String): ByteArray? {
        return try {
            openInputStream(path)?.use { input ->
                ByteArrayOutputStream().use { bos ->
                    val buffer = ByteArray(8192)
                    var len: Int
                    while (input.read(buffer).also { len = it } > 0) {
                        bos.write(buffer, 0, len)
                    }
                    bos.toByteArray()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "readBytes: error=${e.message}", e)
            null
        }
    }

    override fun openInputStream(path: String): InputStream? {
        val documentUri = getDocumentUri(path) ?: throw Exception("File not found: $path")
        val ctx = getContext() ?: throw Exception("Context is null")
        return ctx.contentResolver.openInputStream(documentUri)
    }

    override fun write(path: String, content: String, encoding: String): Boolean {
        return writeBytes(path, content.toByteArray(charset(encoding)))
    }

    override fun write(path: String, content: String): Boolean = write(path, content, "UTF-8")

    override fun append(path: String, content: String, encoding: String): Boolean {
        val existing = read(path, encoding) ?: ""
        return write(path, existing + content, encoding)
    }

    override fun append(path: String, content: String): Boolean = append(path, content, "UTF-8")

    override fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return try {
            openOutputStream(path)?.use { 
                it.write(bytes)
                true
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "writeBytes: error=${e.message}", e)
            false
        }
    }

    override fun openOutputStream(path: String): OutputStream? = openOutputStream(path, false)

    override fun openOutputStream(path: String, append: Boolean): OutputStream? {
        val ctx = getContext() ?: throw Exception("Context is null")
        var documentUri = getDocumentUri(path)

        if (documentUri == null) {
            documentUri = createFile(path) ?: throw Exception("Cannot create file: $path")
        }

        return if (append) {
            AppendOutputStream(ctx, documentUri, path)
        } else {
            ctx.contentResolver.openOutputStream(documentUri, "wt")
        }
    }

    private inner class AppendOutputStream(
        private val ctx: Context,
        private val documentUri: Uri,
        private val path: String
    ) : ByteArrayOutputStream() {
        override fun close() {
            super.close()
            val existingData = try {
                ctx.contentResolver.openInputStream(documentUri)?.use { input ->
                    ByteArrayOutputStream().use { bos ->
                        val buffer = ByteArray(8192)
                        var len: Int
                        while (input.read(buffer).also { len = it } != -1) {
                            bos.write(buffer, 0, len)
                        }
                        bos.toByteArray()
                    }
                } ?: ByteArray(0)
            } catch (e: Exception) {
                ByteArray(0)
            }

            ctx.contentResolver.openOutputStream(documentUri, "wt")?.use { output ->
                output.write(existingData)
                output.write(toByteArray())
            }
        }
    }

    override fun length(path: String): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0
        val ctx = getContext() ?: return 0
        val documentUri = getDocumentUri(path) ?: return 0
        return queryLong(ctx, documentUri, DocumentsContract.Document.COLUMN_SIZE)
    }

    override fun lastModified(path: String): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0
        val ctx = getContext() ?: return 0
        val documentUri = getDocumentUri(path) ?: return 0
        return queryLong(ctx, documentUri, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
    }

    private fun queryLong(ctx: Context, uri: Uri, column: String): Long {
        try {
            ctx.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) return cursor.getLong(0)
            }
        } catch (e: Exception) { }
        return 0
    }

    override fun getName(path: String): String = path.substringAfterLast('/')

    override fun getParent(path: String): String? {
        val lastSlash = path.lastIndexOf('/')
        return if (lastSlash > 0) path.substring(0, lastSlash) else rootPath
    }

    override fun getExtension(path: String): String {
        val name = getName(path)
        val lastDot = name.lastIndexOf('.')
        return if (lastDot > 0) name.substring(lastDot + 1) else ""
    }

    override fun isAccessible(path: String): Boolean = resolvePath(path).startsWith(rootPath)

    override fun getWorkingDirectory(): String = workingDirectory

    override fun setWorkingDirectory(path: String) { workingDirectory = path }

    override fun resolvePath(path: String): String {
        if (path.isEmpty()) return workingDirectory
        return if (path.startsWith("/")) path else "$workingDirectory/$path"
    }

    // SAF 辅助方法
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun findDocumentId(path: String): String? {
        val relativePath = getRelativePath(path)
        val ctx = getContext() ?: return null

        if (relativePath.isEmpty()) return DocumentsContract.getTreeDocumentId(treeUri)

        val parts = relativePath.split("/")
        var currentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

        for (part in parts) {
            if (part.isEmpty()) continue
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocumentId)
            var found = false
            try {
                ctx.contentResolver.query(
                    childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        if (cursor.getString(1) == part) {
                            currentDocumentId = cursor.getString(0)
                            found = true
                            break
                        }
                    }
                }
            } catch (e: Exception) { return null }
            if (!found) return null
        }
        return currentDocumentId
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getDocumentUri(path: String): Uri? {
        return findDocumentId(path)?.let { DocumentsContract.buildDocumentUriUsingTree(treeUri, it) }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getChildrenUri(path: String): Uri? {
        return findDocumentId(path)?.let { DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, it) }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun getMimeType(documentId: String): String? {
        val ctx = getContext() ?: return null
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        try {
            ctx.contentResolver.query(
                documentUri, arrayOf(DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null
            )?.use { if (it.moveToFirst()) return it.getString(0) }
        } catch (e: Exception) { }
        return null
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createFile(path: String): Uri? {
        val ctx = getContext() ?: return null
        val parentPath = getParent(path) ?: return null
        val fileName = getName(path)
        var parentDocumentId = findDocumentId(parentPath)
        if (parentDocumentId == null) {
            parentDocumentId = createDirectory(parentPath) ?: return null
        }
        val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, parentDocumentId)
        return try {
            DocumentsContract.createDocument(ctx.contentResolver, parentUri, "application/x-javascript", fileName)
        } catch (e: Exception) { null }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createDirectory(path: String): String? {
        val ctx = getContext() ?: return null
        val relativePath = getRelativePath(path)
        if (relativePath.isEmpty()) return DocumentsContract.getTreeDocumentId(treeUri)

        val parts = relativePath.split("/")
        var currentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

        for (part in parts) {
            if (part.isEmpty()) continue
            val existingId = findChildDocumentId(currentDocumentId, part, true)
            if (existingId != null) {
                currentDocumentId = existingId
                continue
            }
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentDocumentId)
            try {
                val newDirUri = DocumentsContract.createDocument(
                    ctx.contentResolver, parentUri, DocumentsContract.Document.MIME_TYPE_DIR, part
                ) ?: return null
                currentDocumentId = DocumentsContract.getDocumentId(newDirUri)
            } catch (e: Exception) { return null }
        }
        return currentDocumentId
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun findChildDocumentId(parentDocumentId: String, name: String, isDirectory: Boolean): String? {
        val ctx = getContext() ?: return null
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        try {
            ctx.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == name) {
                        val isDir = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                        if (isDirectory == isDir) return cursor.getString(0)
                    }
                }
            }
        } catch (e: Exception) { }
        return null
    }

    private fun getRelativePath(path: String): String {
        val resolvedPath = resolvePath(path)
        return if (resolvedPath.startsWith(rootPath)) {
            val relative = resolvedPath.substring(rootPath.length)
            if (relative.startsWith("/")) relative.substring(1) else relative
        } else resolvedPath
    }
}
