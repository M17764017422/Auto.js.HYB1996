package com.stardust.pio

import android.util.Log
import java.io.*

/**
 * 传统 File API 实现
 * 适用于拥有完全存储权限或应用私有目录的场景
 */
class TraditionalFileProvider : IFileProvider {

    companion object {
        private const val TAG = "TraditionalFileProvider"
    }

    private var workingDirectory: String

    constructor() {
        workingDirectory = "/"
        Log.d(TAG, "Created: workingDir=/")
    }

    constructor(workingDirectory: String) {
        this.workingDirectory = workingDirectory
        Log.d(TAG, "Created: workingDir=$workingDirectory")
    }

    override fun exists(path: String): Boolean {
        val resolved = resolvePath(path)
        val result = File(resolved).exists()
        Log.d(TAG, "exists: path=$path, resolved=$resolved, result=$result")
        return result
    }

    override fun isFile(path: String): Boolean {
        val resolved = resolvePath(path)
        return File(resolved).isFile
    }

    override fun isDirectory(path: String): Boolean {
        val resolved = resolvePath(path)
        return File(resolved).isDirectory
    }

    override fun mkdir(path: String): Boolean {
        val resolved = resolvePath(path)
        return File(resolved).mkdir()
    }

    override fun mkdirs(path: String): Boolean {
        val resolved = resolvePath(path)
        return File(resolved).mkdirs()
    }

    override fun delete(path: String): Boolean {
        val resolved = resolvePath(path)
        return File(resolved).delete()
    }

    override fun deleteRecursively(path: String): Boolean {
        val resolved = resolvePath(path)
        return deleteRecursive(File(resolved))
    }

    private fun deleteRecursive(file: File): Boolean {
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursive(it) }
        }
        return file.delete()
    }

    override fun rename(path: String, newName: String): Boolean {
        val resolved = resolvePath(path)
        val file = File(resolved)
        val newFile = File(file.parent, newName)
        return file.renameTo(newFile)
    }

    override fun move(fromPath: String, toPath: String): Boolean {
        val fromResolved = resolvePath(fromPath)
        val toResolved = resolvePath(toPath)
        return File(fromResolved).renameTo(File(toResolved))
    }

    override fun copy(fromPath: String, toPath: String): Boolean {
        val fromResolved = resolvePath(fromPath)
        val toResolved = resolvePath(toPath)
        return try {
            val from = File(fromResolved)
            val to = File(toResolved)
            if (from.isDirectory) copyDirectory(from, to) else copyFile(from, to)
        } catch (e: Exception) {
            Log.e(TAG, "copy: error=${e.message}", e)
            false
        }
    }

    private fun copyFile(from: File, to: File): Boolean {
        FileInputStream(from).use { fis ->
            FileOutputStream(to).use { fos ->
                val buffer = ByteArray(8192)
                var len: Int
                while (fis.read(buffer).also { len = it } > 0) {
                    fos.write(buffer, 0, len)
                }
            }
        }
        return true
    }

    private fun copyDirectory(from: File, to: File): Boolean {
        if (!to.exists()) to.mkdirs()
        from.listFiles()?.forEach { child ->
            val newChild = File(to, child.name)
            if (child.isDirectory) copyDirectory(child, newChild) else copyFile(child, newChild)
        }
        return true
    }

    override fun listFiles(path: String): List<IFileProvider.FileInfo> {
        val resolvedPath = resolvePath(path)
        val result = mutableListOf<IFileProvider.FileInfo>()
        val dir = File(resolvedPath)
        val files = dir.listFiles() ?: return result
        files.forEach { file ->
            result.add(IFileProvider.FileInfo(
                file.name,
                file.absolutePath,
                file.isDirectory,
                file.length(),
                file.lastModified()
            ))
        }
        return result
    }

    override fun read(path: String, encoding: String): String? {
        return try {
            PFiles.read(resolvePath(path), encoding)
        } catch (e: Exception) {
            Log.e(TAG, "read: error=${e.message}", e)
            null
        }
    }

    override fun read(path: String): String? = read(resolvePath(path), "UTF-8")

    override fun readBytes(path: String): ByteArray? {
        return try {
            File(resolvePath(path)).readBytes()
        } catch (e: Exception) {
            Log.e(TAG, "readBytes: error=${e.message}", e)
            null
        }
    }

    override fun openInputStream(path: String): InputStream {
        return BufferedInputStream(FileInputStream(resolvePath(path)))
    }

    override fun write(path: String, content: String, encoding: String): Boolean {
        return try {
            PFiles.write(resolvePath(path), content, encoding)
            true
        } catch (e: Exception) {
            Log.e(TAG, "write: error=${e.message}", e)
            false
        }
    }

    override fun write(path: String, content: String): Boolean = write(resolvePath(path), content, "UTF-8")

    override fun append(path: String, content: String, encoding: String): Boolean {
        return try {
            PFiles.append(resolvePath(path), content, encoding)
            true
        } catch (e: Exception) {
            Log.e(TAG, "append: error=${e.message}", e)
            false
        }
    }

    override fun append(path: String, content: String): Boolean = append(path, content, "UTF-8")

    override fun writeBytes(path: String, bytes: ByteArray): Boolean {
        return try {
            File(resolvePath(path)).writeBytes(bytes)
            true
        } catch (e: Exception) {
            Log.e(TAG, "writeBytes: error=${e.message}", e)
            false
        }
    }

    override fun openOutputStream(path: String): OutputStream = openOutputStream(path, false)

    override fun openOutputStream(path: String, append: Boolean): OutputStream {
        return BufferedOutputStream(FileOutputStream(resolvePath(path), append))
    }

    override fun length(path: String): Long = File(resolvePath(path)).length()

    override fun lastModified(path: String): Long = File(resolvePath(path)).lastModified()

    override fun getName(path: String): String = File(resolvePath(path)).name

    override fun getParent(path: String): String? = File(resolvePath(path)).parent

    override fun getExtension(path: String): String = PFiles.getExtension(resolvePath(path))

    override fun isAccessible(path: String): Boolean {
        val resolvedPath = resolvePath(path)
        val file = File(resolvedPath)
        return if (file.exists()) file.canRead() else file.parentFile?.canWrite() ?: false
    }

    override fun getWorkingDirectory(): String = workingDirectory

    override fun setWorkingDirectory(path: String) {
        Log.d(TAG, "setWorkingDirectory: $path")
        workingDirectory = path
    }

    override fun resolvePath(path: String): String {
        if (path.isEmpty()) return workingDirectory
        val file = File(path)
        return if (file.isAbsolute) path else File(workingDirectory, path).absolutePath
    }
}
