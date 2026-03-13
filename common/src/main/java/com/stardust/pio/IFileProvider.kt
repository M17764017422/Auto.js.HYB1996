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
     * 获取文件 MIME 类型
     * @param path 文件路径
     * @return MIME 类型字符串，如 "text/plain"、"image/jpeg" 等；目录返回 "application/vnd.android.document/directory"；未知返回 null
     */
    fun getMimeType(path: String): String?

    /**
     * 批量复制文件
     * @param operations 操作列表，每个元素是 Pair(fromPath, toPath)
     * @param stopOnError 遇到错误是否停止
     * @return 批量操作结果
     */
    fun copyBatch(operations: List<Pair<String, String>>, stopOnError: Boolean = false): BatchResult {
        return performBatchOperation("copy", operations, stopOnError) { from, to -> copy(from, to) }
    }

    /**
     * 批量移动文件
     * @param operations 操作列表，每个元素是 Pair(fromPath, toPath)
     * @param stopOnError 遇到错误是否停止
     * @return 批量操作结果
     */
    fun moveBatch(operations: List<Pair<String, String>>, stopOnError: Boolean = false): BatchResult {
        return performBatchOperation("move", operations, stopOnError) { from, to -> move(from, to) }
    }

    /**
     * 批量删除文件
     * @param paths 要删除的文件路径列表
     * @param stopOnError 遇到错误是否停止
     * @return 批量操作结果
     */
    fun deleteBatch(paths: List<String>, stopOnError: Boolean = false): BatchResult {
        val results = mutableListOf<BatchResult.OperationResult>()
        var successCount = 0
        var failureCount = 0
        
        for (path in paths) {
            val result = try {
                val success = delete(path)
                if (success) {
                    successCount++
                    BatchResult.OperationResult(path, true, null)
                } else {
                    failureCount++
                    BatchResult.OperationResult(path, false, "Delete failed")
                }
            } catch (e: Exception) {
                failureCount++
                BatchResult.OperationResult(path, false, e.message)
            }
            results.add(result)
            
            if (!result.success && stopOnError) {
                break
            }
        }
        
        return BatchResult(
            operationType = "delete",
            successCount = successCount,
            failureCount = failureCount,
            results = results
        )
    }

    /**
     * 执行批量操作的通用方法
     */
    private fun performBatchOperation(
        operationType: String,
        operations: List<Pair<String, String>>,
        stopOnError: Boolean,
        operation: (String, String) -> Boolean
    ): BatchResult {
        val results = mutableListOf<BatchResult.OperationResult>()
        var successCount = 0
        var failureCount = 0
        
        for ((from, to) in operations) {
            val result = try {
                val success = operation(from, to)
                if (success) {
                    successCount++
                    BatchResult.OperationResult("$from -> $to", true, null)
                } else {
                    failureCount++
                    BatchResult.OperationResult("$from -> $to", false, "$operationType failed")
                }
            } catch (e: Exception) {
                failureCount++
                BatchResult.OperationResult("$from -> $to", false, e.message)
            }
            results.add(result)
            
            if (!result.success && stopOnError) {
                break
            }
        }
        
        return BatchResult(
            operationType = operationType,
            successCount = successCount,
            failureCount = failureCount,
            results = results
        )
    }

    /**
     * 文件标志常量
     */
    object FileFlags {
        /** 支持写入 */
        const val SUPPORTS_WRITE = 0x1
        /** 支持删除 */
        const val SUPPORTS_DELETE = 0x2
        /** 支持重命名 */
        const val SUPPORTS_RENAME = 0x4
        /** 支持复制 */
        const val SUPPORTS_COPY = 0x8
        /** 支持移动 */
        const val SUPPORTS_MOVE = 0x10
        /** 虚拟文档（如云文件，需要先下载） */
        const val VIRTUAL_DOCUMENT = 0x20
        /** 支持缩略图 */
        const val SUPPORTS_THUMBNAIL = 0x40
    }

    /**
     * 文件信息类
     */
    data class FileInfo(
        val name: String,
        val path: String,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long,
        val mimeType: String? = null,
        val flags: Int = 0
    ) {
        override fun toString(): String {
            return "FileInfo{name='$name', path='$path', isDirectory=$isDirectory, size=$size, lastModified=$lastModified, mimeType=$mimeType, flags=$flags}"
        }

        /** 检查是否支持写入 */
        fun supportsWrite(): Boolean = (flags and FileFlags.SUPPORTS_WRITE) != 0
        /** 检查是否支持删除 */
        fun supportsDelete(): Boolean = (flags and FileFlags.SUPPORTS_DELETE) != 0
        /** 检查是否支持重命名 */
        fun supportsRename(): Boolean = (flags and FileFlags.SUPPORTS_RENAME) != 0
        /** 检查是否支持复制 */
        fun supportsCopy(): Boolean = (flags and FileFlags.SUPPORTS_COPY) != 0
        /** 检查是否支持移动 */
        fun supportsMove(): Boolean = (flags and FileFlags.SUPPORTS_MOVE) != 0
        /** 检查是否为虚拟文档 */
        fun isVirtual(): Boolean = (flags and FileFlags.VIRTUAL_DOCUMENT) != 0
        /** 检查是否支持缩略图 */
        fun supportsThumbnail(): Boolean = (flags and FileFlags.SUPPORTS_THUMBNAIL) != 0
    }

    /**
     * 批量操作结果类
     */
    data class BatchResult(
        /** 操作类型: "copy", "move", "delete" */
        val operationType: String,
        /** 成功数量 */
        val successCount: Int,
        /** 失败数量 */
        val failureCount: Int,
        /** 详细结果列表 */
        val results: List<OperationResult>
    ) {
        /** 总操作数 */
        val totalCount: Int get() = successCount + failureCount
        
        /** 是否全部成功 */
        val isAllSuccess: Boolean get() = failureCount == 0
        
        override fun toString(): String {
            return "BatchResult{type='$operationType', success=$successCount, failure=$failureCount, total=$totalCount}"
        }

        /**
         * 单个操作结果
         */
        data class OperationResult(
            /** 操作目标（路径或 from->to） */
            val target: String,
            /** 是否成功 */
            val success: Boolean,
            /** 错误信息（失败时） */
            val error: String?
        ) {
            override fun toString(): String {
                return if (success) {
                    "OperationResult{target='$target', success=true}"
                } else {
                    "OperationResult{target='$target', success=false, error='$error'}"
                }
            }
        }
    }
}
