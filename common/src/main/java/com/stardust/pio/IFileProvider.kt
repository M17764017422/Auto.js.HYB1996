package com.stardust.pio

import java.io.InputStream
import java.io.OutputStream

/**
 * 统一文件访问接口
 * 支持传统 File API 和 SAF (Storage Access Framework) 两种实现
 */
interface IFileProvider {

    /**
     * 检查文件或目录是否存在
     */
    fun exists(path: String): Boolean

    /**
     * 检查是否为文件
     */
    fun isFile(path: String): Boolean

    /**
     * 检查是否为目录
     */
    fun isDirectory(path: String): Boolean

    /**
     * 创建目录
     */
    fun mkdir(path: String): Boolean

    /**
     * 创建目录（包含父目录）
     */
    fun mkdirs(path: String): Boolean

    /**
     * 删除文件
     */
    fun delete(path: String): Boolean

    /**
     * 递归删除目录
     */
    fun deleteRecursively(path: String): Boolean

    /**
     * 重命名文件或目录
     */
    fun rename(path: String, newName: String): Boolean

    /**
     * 移动文件或目录
     */
    fun move(fromPath: String, toPath: String): Boolean

    /**
     * 复制文件
     */
    fun copy(fromPath: String, toPath: String): Boolean

    /**
     * 列出目录内容
     */
    fun listFiles(path: String): List<FileInfo>

    /**
     * 读取文件内容为字符串
     */
    fun read(path: String, encoding: String): String?

    /**
     * 读取文件内容为字符串（UTF-8）
     */
    fun read(path: String): String?

    /**
     * 读取文件为字节数组
     */
    fun readBytes(path: String): ByteArray?

    /**
     * 获取文件输入流
     */
    @Throws(Exception::class)
    fun openInputStream(path: String): InputStream?

    /**
     * 写入字符串到文件
     */
    fun write(path: String, content: String, encoding: String): Boolean

    /**
     * 写入字符串到文件（UTF-8）
     */
    fun write(path: String, content: String): Boolean

    /**
     * 追加字符串到文件
     */
    fun append(path: String, content: String, encoding: String): Boolean

    /**
     * 追加字符串到文件（UTF-8）
     */
    fun append(path: String, content: String): Boolean

    /**
     * 写入字节数组到文件
     */
    fun writeBytes(path: String, bytes: ByteArray): Boolean

    /**
     * 获取文件输出流
     */
    @Throws(Exception::class)
    fun openOutputStream(path: String): OutputStream?

    /**
     * 获取文件输出流（追加模式）
     */
    @Throws(Exception::class)
    fun openOutputStream(path: String, append: Boolean): OutputStream?

    /**
     * 获取文件大小
     */
    fun length(path: String): Long

    /**
     * 获取最后修改时间
     */
    fun lastModified(path: String): Long

    /**
     * 获取文件名
     */
    fun getName(path: String): String

    /**
     * 获取父目录路径
     */
    fun getParent(path: String): String?

    /**
     * 获取文件扩展名
     */
    fun getExtension(path: String): String

    /**
     * 检查路径是否在授权范围内
     */
    fun isAccessible(path: String): Boolean

    /**
     * 获取当前工作目录
     */
    fun getWorkingDirectory(): String

    /**
     * 设置当前工作目录
     */
    fun setWorkingDirectory(path: String)

    /**
     * 解析相对路径为绝对路径
     */
    fun resolvePath(path: String): String

    /**
     * 文件信息类
     */
    data class FileInfo(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    ) {
        override fun toString(): String {
            return "FileInfo{name='$name', path='$path', isDirectory=$isDirectory, size=$size, lastModified=$lastModified}"
        }
    }
}
