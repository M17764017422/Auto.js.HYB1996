/*
 * 文件操作异常封装
 * 提供统一的异常处理机制，便于调试和错误追踪
 */

package com.stardust.pio.exception

import java.io.IOException

/**
 * 文件操作异常基类
 */
sealed class FileProviderException(
    message: String,
    cause: Throwable? = null
) : IOException(message, cause) {

    /**
     * 文件不存在异常
     */
    class FileNotFound(
        path: String,
        cause: Throwable? = null
    ) : FileProviderException("File not found: $path", cause)

    /**
     * 访问被拒绝异常
     */
    class AccessDenied(
        path: String,
        cause: Throwable? = null
    ) : FileProviderException("Access denied: $path", cause)

    /**
     * 操作失败异常
     */
    class OperationFailed(
        operation: String,
        path: String,
        cause: Throwable? = null
    ) : FileProviderException("Operation '$operation' failed on: $path", cause)

    /**
     * 目录不存在异常
     */
    class DirectoryNotFound(
        path: String,
        cause: Throwable? = null
    ) : FileProviderException("Directory not found: $path", cause)

    /**
     * 文件已存在异常
     */
    class FileAlreadyExists(
        path: String,
        cause: Throwable? = null
    ) : FileProviderException("File already exists: $path", cause)

    /**
     * 不支持的操作异常
     */
    class UnsupportedOperation(
        operation: String,
        path: String? = null,
        cause: Throwable? = null
    ) : FileProviderException(
        if (path != null) "Operation '$operation' is not supported on: $path"
        else "Operation '$operation' is not supported",
        cause
    )

    /**
     * 权限不足异常
     */
    class PermissionDenied(
        path: String,
        permission: String? = null,
        cause: Throwable? = null
    ) : FileProviderException(
        if (permission != null) "Permission '$permission' denied for: $path"
        else "Permission denied for: $path",
        cause
    )

    /**
     * SAF 特定异常
     */
    class SafException(
        message: String,
        val uri: String? = null,
        cause: Throwable? = null
    ) : FileProviderException(
        if (uri != null) "$message (URI: $uri)" else message,
        cause
    )

    /**
     * 操作被取消异常
     */
    class OperationCancelled(
        operation: String? = null,
        cause: Throwable? = null
    ) : FileProviderException(
        if (operation != null) "Operation '$operation' was cancelled"
        else "Operation was cancelled",
        cause
    )
}

/**
 * 异常扩展函数：将异常转换为更具体的 FileProviderException
 */
fun Throwable.toFileProviderException(
    operation: String,
    path: String? = null
): FileProviderException {
    return when (this) {
        is FileProviderException -> this
        is java.io.FileNotFoundException -> FileProviderException.FileNotFound(path ?: "", this)
        is SecurityException -> FileProviderException.AccessDenied(path ?: "", this)
        is java.nio.file.NoSuchFileException -> FileProviderException.FileNotFound(path ?: "", this)
        is java.nio.file.AccessDeniedException -> FileProviderException.AccessDenied(path ?: "", this)
        is java.nio.file.FileAlreadyExistsException -> FileProviderException.FileAlreadyExists(path ?: "", this)
        is InterruptedException, is java.io.InterruptedIOException -> 
            FileProviderException.OperationCancelled(operation, this)
        else -> FileProviderException.OperationFailed(operation, path ?: "", this)
    }
}
