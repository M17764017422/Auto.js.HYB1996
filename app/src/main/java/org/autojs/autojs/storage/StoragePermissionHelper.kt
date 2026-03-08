package org.autojs.autojs.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import org.autojs.autojs.Pref
import java.io.File

/**
 * 存储权限管理帮助类
 * 支持 Android 11+ 的 SAF (Storage Access Framework) 目录授权
 * 和 MANAGE_EXTERNAL_STORAGE 完全访问权限
 */
object StoragePermissionHelper {

    private const val TAG = "StoragePermission"

    const val REQUEST_CODE_SAF_DIRECTORY = 10001
    const val REQUEST_CODE_MANAGE_STORAGE = 10002

    /**
     * 检查是否有存储访问权限
     */
    @JvmStatic
    fun hasStorageAccess(context: Context): Boolean {
        Log.d(TAG, "hasStorageAccess: checking...")
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: 检查 MANAGE_EXTERNAL_STORAGE 或 SAF 权限
            val isManager = Environment.isExternalStorageManager()
            val hasSaf = hasSafAccess(context)
            Log.d(TAG, "hasStorageAccess: Android 11+, isStorageManager=$isManager, hasSaf=$hasSaf")
            isManager || hasSaf
        } else {
            // Android 10 及以下：检查传统权限
            val result = hasLegacyStoragePermission(context)
            Log.d(TAG, "hasStorageAccess: Android ${Build.VERSION.SDK_INT}, result=$result")
            result
        }
    }

    /**
     * 检查是否有 SAF 目录访问权限
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun hasSafAccess(context: Context): Boolean {
        val safUri = Pref.getSafDirectoryUri()
        Log.d(TAG, "hasSafAccess: safUri=$safUri")
        if (safUri.isNullOrEmpty()) {
            Log.d(TAG, "hasSafAccess: no SAF URI stored")
            return false
        }

        val permissions = context.contentResolver.persistedUriPermissions
        Log.d(TAG, "hasSafAccess: persisted permissions count=${permissions.size}")

        for (permission in permissions) {
            val permUri = permission.uri.toString()
            Log.v(TAG, "hasSafAccess: checking permission uri=$permUri, read=${permission.isReadPermission}, write=${permission.isWritePermission}")
            if (permUri == safUri) {
                val result = permission.isReadPermission && permission.isWritePermission
                Log.d(TAG, "hasSafAccess: found matching URI, result=$result")
                return result
            }
        }
        Log.d(TAG, "hasSafAccess: no matching permission found")
        return false
    }

    /**
     * 请求存储权限（自动选择最佳方式）
     */
    @JvmStatic
    fun requestStoragePermission(activity: Activity) {
        Log.d(TAG, "requestStoragePermission")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: 优先使用 SAF
            requestSafDirectoryAccess(activity)
        } else {
            // Android 10 及以下：使用传统权限（由调用者处理）
            Log.d(TAG, "requestStoragePermission: legacy mode, handled by caller")
        }
    }

    /**
     * 使用 SAF 请求目录访问权限
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun requestSafDirectoryAccess(activity: Activity) {
        Log.i(TAG, "requestSafDirectoryAccess: opening SAF directory picker")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)

        // 尝试设置初始目录为外部存储根目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A")
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
            Log.d(TAG, "requestSafDirectoryAccess: set initial URI=$initialUri")
        }

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )

        activity.startActivityForResult(intent, REQUEST_CODE_SAF_DIRECTORY)
    }

    /**
     * 请求完全存储访问权限 (MANAGE_EXTERNAL_STORAGE)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    @JvmStatic
    fun requestFullStorageAccess(activity: Activity) {
        Log.i(TAG, "requestFullStorageAccess: opening MANAGE_APP_ALL_FILES_ACCESS_PERMISSION settings")
        val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        intent.data = Uri.parse("package:${activity.packageName}")
        activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
    }

    /**
     * 处理 SAF 授权结果
     * @return 是否授权成功
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun handleSafResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        Log.d(TAG, "handleSafResult: requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode != REQUEST_CODE_SAF_DIRECTORY) return false

        if (resultCode == Activity.RESULT_OK && data != null) {
            val treeUri = data.data
            Log.i(TAG, "handleSafResult: SAF authorized, treeUri=$treeUri")
            if (treeUri != null) {
                // 持久化权限
                activity.contentResolver.takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                Log.i(TAG, "handleSafResult: persisted URI permission")

                // 保存授权的 URI
                Pref.setSafDirectoryUri(treeUri.toString())
                Log.i(TAG, "handleSafResult: saved SAF URI to preferences")

                // 从 SAF URI 解析实际路径并同步更新脚本目录
                val actualPath = getActualPathFromSafUri(treeUri)
                if (actualPath != null) {
                    Pref.setScriptDirPath(actualPath)
                    Log.i(TAG, "handleSafResult: synced script dir path to $actualPath")
                }

                // 刷新 FileProvider
                com.stardust.pio.FileProviderFactory.refresh()
                Log.i(TAG, "handleSafResult: FileProvider refreshed")

                return true
            }
        }
        Log.w(TAG, "handleSafResult: SAF authorization failed or cancelled")
        return false
    }

    /**
     * 处理完全存储访问授权结果
     */
    @RequiresApi(Build.VERSION_CODES.R)
    @JvmStatic
    fun handleManageStorageResult(requestCode: Int): Boolean {
        Log.d(TAG, "handleManageStorageResult: requestCode=$requestCode")
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            val result = Environment.isExternalStorageManager()
            Log.i(TAG, "handleManageStorageResult: isExternalStorageManager=$result")
            return result
        }
        return false
    }

    /**
     * 获取已授权的 SAF 目录 Uri
     */
    @JvmStatic
    fun getSafDirectoryUri(): Uri? {
        val uriString = Pref.getSafDirectoryUri()
        return if (!uriString.isNullOrEmpty()) Uri.parse(uriString) else null
    }

    /**
     * 清除 SAF 权限
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @JvmStatic
    fun clearSafPermission(context: Context) {
        Log.i(TAG, "clearSafPermission: releasing all persisted URI permissions")
        val permissions = context.contentResolver.persistedUriPermissions
        for (permission in permissions) {
            context.contentResolver.releasePersistableUriPermission(
                permission.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            Log.d(TAG, "clearSafPermission: released ${permission.uri}")
        }
        Pref.setSafDirectoryUri(null)
        Log.i(TAG, "clearSafPermission: cleared SAF URI from preferences")
    }

    /**
     * 检查传统存储权限
     */
    private fun hasLegacyStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * 从 SAF URI 解析实际文件系统路径
     */
    private fun getActualPathFromSafUri(treeUri: Uri): String? {
        try {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            Log.d(TAG, "getActualPathFromSafUri: documentId=$documentId")

            // ExternalStorageProvider 格式: primary:Download 或 XXXX-XXXX:Directory
            if (documentId.startsWith("primary:")) {
                val relativePath = documentId.substring(8)
                val sdcard = Environment.getExternalStorageDirectory().absolutePath
                return "$sdcard/$relativePath"
            } else if (documentId.contains(":")) {
                val parts = documentId.split(":", limit = 2)
                val volumeId = parts[0]
                val relativePath = if (parts.size > 1) parts[1] else ""

                val storagePath = findStoragePath(volumeId)
                if (storagePath != null) {
                    return if (relativePath.isEmpty()) storagePath else "$storagePath/$relativePath"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "getActualPathFromSafUri failed", e)
        }
        return null
    }

    /**
     * 根据 volume ID 查找存储路径
     */
    private fun findStoragePath(volumeId: String): String? {
        // primary 通常是主存储
        if ("primary".equals(volumeId, ignoreCase = true)) {
            return Environment.getExternalStorageDirectory().absolutePath
        }

        // SD 卡或其他存储: /storage/XXXX-XXXX
        val storageDir = File("/storage/$volumeId")
        if (storageDir.exists()) return storageDir.absolutePath

        return null
    }
}
