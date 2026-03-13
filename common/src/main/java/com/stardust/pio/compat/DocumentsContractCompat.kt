/*
 * SAF API 兼容层
 * 参考 MaterialFiles 实现，提供跨版本的 DocumentsContract API 支持
 */

package com.stardust.pio.compat

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract

/**
 * DocumentsContract API 兼容层
 * 提供跨 Android 版本的统一 API
 */
object DocumentsContractCompat {
    const val EXTRA_INITIAL_URI = "android.provider.extra.INITIAL_URI"
    const val EXTRA_SHOW_ADVANCED = "android.provider.extra.SHOW_ADVANCED"

    const val EXTERNAL_STORAGE_PROVIDER_AUTHORITY = "com.android.externalstorage.documents"
    const val EXTERNAL_STORAGE_PRIMARY_EMULATED_ROOT_ID = "primary"

    private const val PATH_DOCUMENT = "document"
    private const val PATH_CHILDREN = "children"
    private const val PATH_TREE = "tree"

    /**
     * 检查 URI 是否为文档 URI
     * @see DocumentsContract.isDocumentUri
     */
    fun isDocumentUri(uri: Uri): Boolean {
        if (uri.scheme != ContentResolver.SCHEME_CONTENT) {
            return false
        }
        val pathSegments = uri.pathSegments
        return when (pathSegments.size) {
            2 -> pathSegments[0] == PATH_DOCUMENT
            4 -> pathSegments[0] == PATH_TREE && pathSegments[2] == PATH_DOCUMENT
            else -> false
        }
    }

    /**
     * 检查 URI 是否为树 URI
     * @see DocumentsContract.isTreeUri
     */
    fun isTreeUri(uri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            DocumentsContract.isTreeUri(uri)
        } else {
            uri.pathSegments.let { it.size >= 2 && it[0] == PATH_TREE }
        }
    }

    /**
     * 检查 URI 是否为子文档 URI
     */
    fun isChildDocumentsUri(uri: Uri): Boolean {
        val pathSegments = uri.pathSegments
        return when (pathSegments.size) {
            3 -> pathSegments[0] == PATH_DOCUMENT && pathSegments[2] == PATH_CHILDREN
            5 -> pathSegments[0] == PATH_TREE && pathSegments[2] == PATH_DOCUMENT && pathSegments[4] == PATH_CHILDREN
            else -> false
        }
    }

    /**
     * 获取树文档 ID
     * 兼容 Android N 以下版本
     */
    fun getTreeDocumentId(treeUri: Uri): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentsContract.getTreeDocumentId(treeUri)
        } else {
            val pathSegments = treeUri.pathSegments
            if (pathSegments.size >= 2 && pathSegments[0] == PATH_TREE) {
                pathSegments[1]
            } else {
                null
            }
        }
    }

    /**
     * 构建文档 URI（使用树 URI）
     */
    fun buildDocumentUriUsingTree(treeUri: Uri, documentId: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        } else {
            null
        }
    }

    /**
     * 构建子文档 URI（使用树 URI）
     */
    fun buildChildDocumentsUriUsingTree(treeUri: Uri, parentDocumentId: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        } else {
            null
        }
    }

    /**
     * 从 URI 获取文档 ID
     */
    fun getDocumentId(documentUri: Uri): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            DocumentsContract.getDocumentId(documentUri)
        } else {
            val pathSegments = documentUri.pathSegments
            when (pathSegments.size) {
                2 -> if (pathSegments[0] == PATH_DOCUMENT) pathSegments[1] else null
                4 -> if (pathSegments[0] == PATH_TREE && pathSegments[2] == PATH_DOCUMENT) pathSegments[3] else null
                else -> null
            }
        }
    }

    /**
     * 复制文档到目标目录
     * 使用 Storage Provider 原生操作，数据不流经应用内存
     * 
     * @param contentResolver ContentResolver
     * @param sourceDocumentUri 源文档 URI
     * @param sourceParentUri 源父目录 URI
     * @param targetParentUri 目标父目录 URI
     * @return 新文档的 URI，失败返回 null
     * @requires API 24 (Android 7.0)
     */
    fun copyDocument(
        contentResolver: ContentResolver,
        sourceDocumentUri: Uri,
        sourceParentUri: Uri,
        targetParentUri: Uri
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                DocumentsContract.copyDocument(contentResolver, sourceDocumentUri, targetParentUri)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * 移动文档到目标目录
     * 使用 Storage Provider 原生操作，数据不流经应用内存
     * 
     * @param contentResolver ContentResolver
     * @param sourceDocumentUri 源文档 URI
     * @param sourceParentUri 源父目录 URI
     * @param targetParentUri 目标父目录 URI
     * @return 新文档的 URI，失败返回 null
     * @requires API 24 (Android 7.0)
     */
    fun moveDocument(
        contentResolver: ContentResolver,
        sourceDocumentUri: Uri,
        sourceParentUri: Uri,
        targetParentUri: Uri
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                DocumentsContract.moveDocument(contentResolver, sourceDocumentUri, sourceParentUri, targetParentUri)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * 重命名文档
     * 使用 Storage Provider 原生操作
     * 
     * @param contentResolver ContentResolver
     * @param documentUri 文档 URI
     * @param displayName 新文件名
     * @return 新文档的 URI，失败返回 null
     * @requires API 21 (Android 5.0)
     */
    fun renameDocument(
        contentResolver: ContentResolver,
        documentUri: Uri,
        displayName: String
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DocumentsContract.renameDocument(contentResolver, documentUri, displayName)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * 删除文档
     * 使用 Storage Provider 原生操作
     * 
     * @param contentResolver ContentResolver
     * @param documentUri 文档 URI
     * @return 是否成功删除
     * @requires API 19 (Android 4.4)
     */
    fun deleteDocument(contentResolver: ContentResolver, documentUri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                DocumentsContract.deleteDocument(contentResolver, documentUri)
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * 创建文档
     * 
     * @param contentResolver ContentResolver
     * @param parentDocumentUri 父目录 URI
     * @param mimeType MIME 类型
     * @param displayName 文件名
     * @return 新文档的 URI，失败返回 null
     * @requires API 21 (Android 5.0)
     */
    fun createDocument(
        contentResolver: ContentResolver,
        parentDocumentUri: Uri,
        mimeType: String,
        displayName: String
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                DocumentsContract.createDocument(contentResolver, parentDocumentUri, mimeType, displayName)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    /**
     * 创建目录
     * 
     * @param contentResolver ContentResolver
     * @param parentDocumentUri 父目录 URI
     * @param displayName 目录名
     * @return 新目录的 URI，失败返回 null
     * @requires API 21 (Android 5.0)
     */
    fun createDirectory(
        contentResolver: ContentResolver,
        parentDocumentUri: Uri,
        displayName: String
    ): Uri? {
        return createDocument(contentResolver, parentDocumentUri, DocumentsContract.Document.MIME_TYPE_DIR, displayName)
    }
}
