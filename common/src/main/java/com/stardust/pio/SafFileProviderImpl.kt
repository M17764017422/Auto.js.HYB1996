@file:Suppress("DEPRECATION")

package com.stardust.pio

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import com.stardust.pio.compat.DocumentsContractCompat
import com.stardust.pio.exception.FileProviderException
import com.stardust.pio.exception.toFileProviderException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InterruptedIOException
import java.io.OutputStream
import java.util.Collections
import java.util.WeakHashMap

/**
 * SAF (Storage Access Framework) 文件访问实现
 * 适用于用户授权特定目录的场景
 * 
 * 优化特性：
 * - DocumentId 缓存：减少路径查询次数，提升深层目录访问性能
 * - 进度回调：支持大文件复制时的进度通知
 * - 取消操作：支持中断长时间操作
 * - 缩略图支持：获取图片文件缩略图
 */
class SafFileProviderImpl(
    private val treeUri: Uri,
    private val rootPath: String,
    private val context: Context? = null
) : IFileProvider {

    companion object {
        private const val TAG = "SafFileProvider"
        private const val DEFAULT_BUFFER_SIZE = 8192

        // DocumentId 缓存 - 使用 WeakHashMap 自动回收不再引用的路径
        private val pathDocumentIdCache = Collections.synchronizedMap(WeakHashMap<String, String>())
        
        // 目录子项名称缓存 - 用于加速 findDocumentId
        private val directoryChildrenCache = Collections.synchronizedMap(WeakHashMap<String, MutableMap<String, String>>())
        
        /**
         * 清除所有缓存
         * 当 SAF 授权变化或需要强制刷新时调用
         */
        @JvmStatic
        fun clearAllCaches() {
            synchronized(pathDocumentIdCache) {
                pathDocumentIdCache.clear()
            }
            synchronized(directoryChildrenCache) {
                directoryChildrenCache.clear()
            }
            Log.i(TAG, "All caches cleared")
        }
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

    // ==================== 缓存管理 ====================

    /**
     * 获取缓存的 DocumentId
     */
    private fun getCachedDocumentId(path: String): String? {
        val resolvedPath = resolvePath(path)
        return synchronized(pathDocumentIdCache) {
            pathDocumentIdCache[resolvedPath]
        }
    }

    /**
     * 缓存 DocumentId
     */
    private fun cacheDocumentId(path: String, documentId: String) {
        val resolvedPath = resolvePath(path)
        synchronized(pathDocumentIdCache) {
            pathDocumentIdCache[resolvedPath] = documentId
        }
    }

    /**
     * 使指定路径的缓存失效
     */
    private fun invalidateCache(path: String) {
        val resolvedPath = resolvePath(path)
        synchronized(pathDocumentIdCache) {
            pathDocumentIdCache.remove(resolvedPath)
        }
        // 同时清除父目录的子项缓存
        val parentPath = getParent(resolvedPath)
        if (parentPath != null) {
            synchronized(directoryChildrenCache) {
                directoryChildrenCache.remove(parentPath)
            }
        }
    }

    /**
     * 使指定路径及其所有子路径的缓存失效
     */
    private fun invalidateCacheRecursively(path: String) {
        val resolvedPath = resolvePath(path)
        Log.d(TAG, "invalidateCacheRecursively: path=$resolvedPath")
        
        synchronized(pathDocumentIdCache) {
            pathDocumentIdCache.keys.removeIf { it.startsWith(resolvedPath) }
        }
        
        synchronized(directoryChildrenCache) {
            // 清除以该路径开头的目录缓存
            directoryChildrenCache.keys.removeIf { it.startsWith(resolvedPath) }
            
            // 还需要从父目录的子项缓存中移除该项
            val parentPath = getParent(resolvedPath)
            if (parentPath != null) {
                val childName = getName(resolvedPath)
                val parentChildren = directoryChildrenCache[parentPath]
                if (parentChildren != null && parentChildren.containsKey(childName)) {
                    parentChildren.remove(childName)
                    Log.d(TAG, "invalidateCacheRecursively: removed '$childName' from parent cache '$parentPath'")
                }
            }
        }
    }

    /**
     * 缓存目录子项
     */
    private fun cacheDirectoryChild(parentPath: String, childName: String, childDocumentId: String) {
        synchronized(directoryChildrenCache) {
            val children = directoryChildrenCache.getOrPut(parentPath) { mutableMapOf() }
            children[childName] = childDocumentId
        }
    }

    /**
     * 获取缓存的目录子项
     */
    private fun getCachedDirectoryChild(parentPath: String, childName: String): String? {
        return synchronized(directoryChildrenCache) {
            directoryChildrenCache[parentPath]?.get(childName)
        }
    }

    // ==================== 基础操作 ====================

    override fun exists(path: String): Boolean = findDocumentId(path) != null

    override fun isFile(path: String): Boolean {
        val documentId = findDocumentId(path) ?: return false
        val mimeType = queryMimeTypeByDocumentId(documentId)
        return mimeType != null && mimeType != DocumentsContract.Document.MIME_TYPE_DIR
    }

    override fun isDirectory(path: String): Boolean {
        val documentId = findDocumentId(path) ?: return false
        val mimeType = queryMimeTypeByDocumentId(documentId)
        return mimeType == DocumentsContract.Document.MIME_TYPE_DIR
    }

    override fun mkdir(path: String): Boolean = createDirectory(path) != null

    override fun mkdirs(path: String): Boolean = createDirectory(path) != null

    override fun delete(path: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        val documentUri = getDocumentUri(path)
        if (documentUri == null) {
            Log.d(TAG, "delete: documentUri is null for $path, path may not exist")
            return false
        }
        val ctx = getContext() ?: return false
        return try {
            Log.d(TAG, "delete: path=$path, documentUri=$documentUri")
            val result = DocumentsContract.deleteDocument(ctx.contentResolver, documentUri)
            Log.d(TAG, "delete: DocumentsContract.deleteDocument returned $result")
            if (result) {
                invalidateCacheRecursively(path)
                Log.d(TAG, "delete: cache invalidated for $path")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "delete: failed for $path", e)
            false
        }
    }

    override fun deleteRecursively(path: String): Boolean = deleteRecursive(path)

    private fun deleteRecursive(path: String): Boolean {
        Log.d(TAG, "deleteRecursive: path=$path, isDirectory=${isDirectory(path)}")
        if (!isDirectory(path)) {
            val result = delete(path)
            Log.d(TAG, "deleteRecursive: delete file result=$result")
            return result
        }
        val children = listFiles(path)
        Log.d(TAG, "deleteRecursive: found ${children.size} children")
        children.forEach { child ->
            if (child.isDirectory) deleteRecursive(child.path) else delete(child.path)
        }
        val result = delete(path)
        Log.d(TAG, "deleteRecursive: delete directory result=$result")
        return result
    }

    override fun rename(path: String, newName: String): Boolean {
        val documentUri = getDocumentUri(path) ?: return false
        val ctx = getContext() ?: return false
        return try {
            val result = DocumentsContract.renameDocument(ctx.contentResolver, documentUri, newName) != null
            if (result) {
                // 清除旧路径缓存
                invalidateCache(path)
                // 缓存新路径
                val parentPath = getParent(path)
                val newPath = if (parentPath != null) "$parentPath/$newName" else newName
                findDocumentId(newPath)?.let { cacheDocumentId(newPath, it) }
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "rename: failed for $path -> $newName", e)
            false
        }
    }

    override fun move(fromPath: String, toPath: String): Boolean {
        // API 24+ 尝试使用原生移动
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val result = moveNative(fromPath, toPath)
            if (result) return true
            Log.i(TAG, "move: native move failed, fallback to copy+delete")
        }
        // 回退到复制+删除
        return copy(fromPath, toPath) && delete(fromPath)
    }

    override fun copy(fromPath: String, toPath: String): Boolean {
        // API 24+ 尝试使用原生复制
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val result = copyNative(fromPath, toPath)
            if (result) return true
            Log.i(TAG, "copy: native copy failed, fallback to streaming")
        }
        // 回退到流式复制
        return copyStreaming(fromPath, toPath)
    }

    /**
     * 原生复制（API 24+）
     * 使用 DocumentsProvider 原生操作，数据不流经应用内存
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun copyNative(fromPath: String, toPath: String): Boolean {
        val ctx = getContext() ?: return false
        val sourceUri = getDocumentUri(fromPath) ?: return false
        val targetParentPath = getParent(toPath) ?: return false
        val targetParentUri = getDocumentUri(targetParentPath) ?: return false
        
        return try {
            val newUri = DocumentsContract.copyDocument(ctx.contentResolver, sourceUri, targetParentUri)
            if (newUri != null) {
                // 缓存新文档
                val newDocumentId = DocumentsContract.getDocumentId(newUri)
                cacheDocumentId(resolvePath(toPath), newDocumentId)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "copyNative: failed for $fromPath -> $toPath", e)
            false
        }
    }

    /**
     * 原生移动（API 24+）
     * 使用 DocumentsProvider 原生操作，数据不流经应用内存
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun moveNative(fromPath: String, toPath: String): Boolean {
        val ctx = getContext() ?: return false
        val sourceUri = getDocumentUri(fromPath) ?: return false
        val sourceParentPath = getParent(fromPath) ?: return false
        val sourceParentUri = getDocumentUri(sourceParentPath) ?: return false
        val targetParentPath = getParent(toPath) ?: return false
        val targetParentUri = getDocumentUri(targetParentPath) ?: return false
        
        return try {
            val newUri = DocumentsContract.moveDocument(
                ctx.contentResolver,
                sourceUri,
                sourceParentUri,
                targetParentUri
            )
            if (newUri != null) {
                // 清除旧路径缓存
                invalidateCache(fromPath)
                // 缓存新路径
                val newDocumentId = DocumentsContract.getDocumentId(newUri)
                cacheDocumentId(resolvePath(toPath), newDocumentId)
                true
            } else false
        } catch (e: Exception) {
            Log.e(TAG, "moveNative: failed for $fromPath -> $toPath", e)
            false
        }
    }

    /**
     * 流式复制（回退方案）
     */
    private fun copyStreaming(fromPath: String, toPath: String): Boolean {
        return try {
            openInputStream(fromPath)?.use { input ->
                openOutputStream(toPath)?.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                    }
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "copyStreaming: failed for $fromPath -> $toPath", e)
            false
        }
    }

    /**
     * 带进度回调的复制操作
     * @param fromPath 源路径
     * @param toPath 目标路径
     * @param progressIntervalMillis 进度回调间隔（毫秒）
     * @param progressListener 进度回调，参数为总已复制字节数
     * @param cancellationSignal 取消信号
     * @return 是否成功
     */
    fun copyWithProgress(
        fromPath: String,
        toPath: String,
        progressIntervalMillis: Long = 500,
        progressListener: ((Long) -> Unit)? = null,
        cancellationSignal: CancellationSignal? = null
    ): Boolean {
        return try {
            openInputStream(fromPath)?.use { input ->
                openOutputStream(toPath)?.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalCopied = 0L
                    var lastProgressTime = System.currentTimeMillis()
                    while (true) {
                        // 检查取消
                        cancellationSignal?.throwIfCanceled()
                        if (Thread.interrupted()) {
                            throw InterruptedIOException("Operation cancelled")
                        }
                        
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        totalCopied += read
                        
                        // 进度回调
                        val currentTime = System.currentTimeMillis()
                        if (progressListener != null && currentTime >= lastProgressTime + progressIntervalMillis) {
                            progressListener(totalCopied)
                            lastProgressTime = currentTime
                        }
                    }
                    progressListener?.invoke(totalCopied)
                    true
                }
            } ?: false
        } catch (e: Exception) {
            if (e is InterruptedIOException || e is java.util.concurrent.CancellationException) {
                Log.i(TAG, "copyWithProgress: cancelled")
            } else {
                Log.e(TAG, "copyWithProgress: failed", e)
            }
            false
        }
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
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS
        )

        try {
            ctx.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val resolvedPath = resolvePath(path)
                while (cursor.moveToNext()) {
                    val documentId = cursor.getString(0)
                    val name = cursor.getString(1)
                    val mimeType = cursor.getString(2)
                    val size = cursor.getLong(3)
                    val lastModified = cursor.getLong(4)
                    val flags = cursor.getInt(5)
                    val isDir = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                    val childPath = if (resolvedPath.endsWith("/")) "$resolvedPath$name" else "$resolvedPath/$name"
                    
                    // 转换 DocumentsContract flags 到 FileFlags
                    val fileFlags = convertDocumentFlags(flags)
                    
                    result.add(IFileProvider.FileInfo(name, childPath, isDir, size, lastModified, mimeType, fileFlags))
                    
                    // 缓存子项
                    cacheDocumentId(childPath, documentId)
                    cacheDirectoryChild(resolvedPath, name, documentId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "listFiles: error=${e.message}", e)
        }
        return result
    }

    /**
     * 转换 DocumentsContract flags 到 FileFlags
     */
    private fun convertDocumentFlags(docFlags: Int): Int {
        var flags = 0
        if (docFlags and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0) {
            flags = flags or IFileProvider.FileFlags.SUPPORTS_WRITE
        }
        if (docFlags and DocumentsContract.Document.FLAG_SUPPORTS_DELETE != 0) {
            flags = flags or IFileProvider.FileFlags.SUPPORTS_DELETE
        }
        if (docFlags and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0) {
            flags = flags or IFileProvider.FileFlags.SUPPORTS_RENAME
        }
        if (docFlags and DocumentsContract.Document.FLAG_SUPPORTS_COPY != 0) {
            flags = flags or IFileProvider.FileFlags.SUPPORTS_COPY
        }
        if (docFlags and DocumentsContract.Document.FLAG_SUPPORTS_MOVE != 0) {
            flags = flags or IFileProvider.FileFlags.SUPPORTS_MOVE
        }
        if (docFlags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0) {
            flags = flags or IFileProvider.FileFlags.VIRTUAL_DOCUMENT
        }
        if (docFlags and DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL != 0) {
            flags = flags or IFileProvider.FileFlags.SUPPORTS_THUMBNAIL
        }
        return flags
    }

    override fun getMimeType(path: String): String? {
        val documentId = findDocumentId(path) ?: return null
        return queryMimeTypeByDocumentId(documentId)
    }

    // ==================== 读写操作 ====================

    override fun read(path: String, encoding: String): String? {
        val data = readBytes(path) ?: return null
        return try { String(data, charset(encoding)) } catch (e: Exception) { null }
    }

    override fun read(path: String): String? = read(path, "UTF-8")

    override fun readBytes(path: String): ByteArray? {
        return try {
            openInputStream(path)?.use { input ->
                ByteArrayOutputStream().use { bos ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
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
        val documentUri = getDocumentUri(path) ?: throw FileProviderException.FileNotFound(path)
        val ctx = getContext() ?: throw FileProviderException.AccessDenied(path)
        return try {
            ctx.contentResolver.openInputStream(documentUri)
        } catch (e: Exception) {
            throw e.toFileProviderException("openInputStream", path)
        }
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
        val ctx = getContext() ?: throw FileProviderException.AccessDenied(path)
        var documentUri = getDocumentUri(path)

        if (documentUri == null) {
            documentUri = createFile(path) ?: throw FileProviderException.OperationFailed("create", path)
        }

        return if (append) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Android N+ 支持原生追加模式 "wa"
                ctx.contentResolver.openOutputStream(documentUri, "wa")
            } else {
                // 低版本降级处理
                AppendOutputStream(ctx, documentUri, path)
            }
        } else {
            ctx.contentResolver.openOutputStream(documentUri, "wt")
        }
    }

    /**
     * 追加写入输出流（低版本兼容实现）
     * 注意：效率较低，每次追加都要读取整个文件
     */
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
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
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

    // ==================== 文件属性 ====================

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

    // ==================== 缩略图支持 ====================

    /**
     * 获取文件缩略图
     * @param path 文件路径
     * @param width 缩略图宽度
     * @param height 缩略图高度
     * @param cancellationSignal 取消信号
     * @return 缩略图 Bitmap，如果不支持则返回 null
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getThumbnail(
        path: String,
        width: Int,
        height: Int,
        cancellationSignal: CancellationSignal? = null
    ): Bitmap? {
        val documentUri = getDocumentUri(path) ?: return null
        val ctx = getContext() ?: return null

        return try {
            DocumentsContract.getDocumentThumbnail(
                ctx.contentResolver,
                documentUri,
                Point(width, height),
                cancellationSignal
            )
        } catch (e: Exception) {
            Log.e(TAG, "getThumbnail: failed for $path", e)
            null
        }
    }

    // ==================== SAF 辅助方法 ====================

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun findDocumentId(path: String): String? {
        val resolvedPath = resolvePath(path)
        
        // 先查缓存
        getCachedDocumentId(resolvedPath)?.let { return it }

        val relativePath = getRelativePath(resolvedPath)
        val ctx = getContext() ?: return null

        if (relativePath.isEmpty()) {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            cacheDocumentId(resolvedPath, documentId)
            return documentId
        }

        val parts = relativePath.split("/")
        var currentPath = rootPath
        var currentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

        for (part in parts) {
            if (part.isEmpty()) continue
            
            val childPath = if (currentPath.endsWith("/")) "$currentPath$part" else "$currentPath/$part"
            
            // 先检查缓存
            val cachedId = getCachedDocumentId(childPath)
            if (cachedId != null) {
                currentDocumentId = cachedId
                currentPath = childPath
                continue
            }
            
            // 检查目录子项缓存
            val cachedChild = getCachedDirectoryChild(currentPath, part)
            if (cachedChild != null) {
                currentDocumentId = cachedChild
                currentPath = childPath
                cacheDocumentId(childPath, cachedChild)
                continue
            }
            
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, currentDocumentId)
            var found = false
            try {
                ctx.contentResolver.query(
                    childrenUri,
                    arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null, null, null
                )?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val docId = cursor.getString(0)
                        val name = cursor.getString(1)
                        // 缓存所有子项
                        val childFullPath = if (currentPath.endsWith("/")) "$currentPath$name" else "$currentPath/$name"
                        cacheDocumentId(childFullPath, docId)
                        cacheDirectoryChild(currentPath, name, docId)
                        
                        if (name == part) {
                            currentDocumentId = docId
                            currentPath = childPath
                            found = true
                        }
                    }
                }
            } catch (e: Exception) { return null }
            if (!found) return null
        }
        
        cacheDocumentId(resolvedPath, currentDocumentId)
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
    private fun queryMimeTypeByDocumentId(documentId: String): String? {
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
            val newUri = DocumentsContract.createDocument(ctx.contentResolver, parentUri, "application/octet-stream", fileName)
            if (newUri != null) {
                val newDocumentId = DocumentsContract.getDocumentId(newUri)
                cacheDocumentId(resolvePath(path), newDocumentId)
                cacheDirectoryChild(resolvePath(parentPath), fileName, newDocumentId)
            }
            newUri
        } catch (e: Exception) { 
            Log.e(TAG, "createFile: failed for $path", e)
            null 
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun createDirectory(path: String): String? {
        val ctx = getContext() ?: return null
        val relativePath = getRelativePath(path)
        if (relativePath.isEmpty()) return DocumentsContract.getTreeDocumentId(treeUri)

        val parts = relativePath.split("/")
        var currentPath = rootPath
        var currentDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

        for (part in parts) {
            if (part.isEmpty()) continue
            
            val childPath = if (currentPath.endsWith("/")) "$currentPath$part" else "$currentPath/$part"
            
            // 检查是否已存在
            val existingId = findChildDocumentId(currentDocumentId, part, true)
            if (existingId != null) {
                currentDocumentId = existingId
                currentPath = childPath
                cacheDocumentId(childPath, existingId)
                cacheDirectoryChild(currentPath, part, existingId)
                continue
            }
            
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, currentDocumentId)
            try {
                val newDirUri = DocumentsContract.createDocument(
                    ctx.contentResolver, parentUri, DocumentsContract.Document.MIME_TYPE_DIR, part
                ) ?: return null
                currentDocumentId = DocumentsContract.getDocumentId(newDirUri)
                cacheDocumentId(childPath, currentDocumentId)
                cacheDirectoryChild(currentPath, part, currentDocumentId)
                currentPath = childPath
            } catch (e: Exception) { 
                Log.e(TAG, "createDirectory: failed for $childPath", e)
                return null 
            }
        }
        return currentDocumentId
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun findChildDocumentId(parentDocumentId: String, name: String, isDirectory: Boolean): String? {
        // 先检查缓存
        val ctx = getContext() ?: return null
        
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        try {
            ctx.contentResolver.query(
                childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0)
                    val docName = cursor.getString(1)
                    val isDir = cursor.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR
                    if (docName == name && isDirectory == isDir) return docId
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