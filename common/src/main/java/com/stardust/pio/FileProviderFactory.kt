package com.stardust.pio

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.util.Log
import java.io.File

/**
 * 文件访问提供者工厂
 * 根据当前权限状态选择合适的实现
 */
object FileProviderFactory {

    private const val TAG = "FileProviderFactory"

    private var sInstance: IFileProvider? = null
    private var sCurrentMode = -1
    // 使用强引用保存配置，防止被 GC 回收导致 SAF 配置丢失
    private var sConfig: FileProviderConfig? = null

    /**
     * 权限模式
     */
    const val MODE_UNKNOWN = 0
    const val MODE_FULL_ACCESS = 1      // MANAGE_EXTERNAL_STORAGE
    const val MODE_SAF_DIRECTORY = 2    // SAF 目录授权
    const val MODE_LEGACY = 3           // Android 10 及以下传统模式

    /**
     * 设置配置提供者（由应用模块在初始化时调用）
     */
    @JvmStatic
    @Synchronized
    fun setConfig(config: FileProviderConfig?) {
        sConfig = config
        Log.i(TAG, "Config set: ${if (config != null) "provided" else "null"}")
    }

    /**
     * 获取当前权限模式
     */
    @JvmStatic
    fun getCurrentMode(): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            val isStorageManager = Environment.isExternalStorageManager()

            val safUri = sConfig?.safDirectoryUri
            val hasSafUri = !safUri.isNullOrEmpty()
            val hasSafAccess = if (hasSafUri) sConfig?.hasSafAccess() ?: false else false

            Log.d(TAG, "getCurrentMode: SDK=${Build.VERSION.SDK_INT}, isStorageManager=$isStorageManager, safUri=$safUri, hasSafAccess=$hasSafAccess")

            if (isStorageManager) {
                Log.i(TAG, "Mode: FULL_ACCESS (MANAGE_EXTERNAL_STORAGE)")
                return MODE_FULL_ACCESS
            }

            if (hasSafUri && hasSafAccess) {
                Log.i(TAG, "Mode: SAF_DIRECTORY")
                return MODE_SAF_DIRECTORY
            }

            Log.w(TAG, "Mode: UNKNOWN (no valid permission)")
            return MODE_UNKNOWN
        } else {
            // Android 10 及以下
            Log.i(TAG, "Mode: LEGACY (Android ${Build.VERSION.SDK_INT})")
            return MODE_LEGACY
        }
    }

    /**
     * 获取文件访问提供者实例
     */
    @JvmStatic
    @Synchronized
    fun getProvider(): IFileProvider {
        return getProvider(null)
    }

    /**
     * 根据路径获取合适的文件访问提供者实例
     * @param path 要访问的文件路径，如果为 null 则返回默认提供者
     */
    @JvmStatic
    @Synchronized
    fun getProvider(path: String?): IFileProvider {
        // 对于应用私有目录，始终使用 TraditionalFileProvider
        if (path != null && isAppPrivatePath(path)) {
            val workDir = sConfig?.scriptDirPath ?: ""
            Log.d(TAG, "getProvider: path=$path is app private, using TraditionalFileProvider")
            return TraditionalFileProvider(workDir)
        }

        val mode = getCurrentMode()

        Log.d(TAG, "getProvider: path=$path, mode=$mode (${getModeDescription()}), cachedInstance=${sInstance != null}, cachedMode=$sCurrentMode")

        // 如果模式没变，返回缓存实例
        if (sInstance != null && sCurrentMode == mode) {
            Log.d(TAG, "Returning cached provider: ${sInstance!!::class.simpleName}")
            return sInstance!!
        }

        sCurrentMode = mode
        val workDir = sConfig?.scriptDirPath ?: ""

        when (mode) {
            MODE_SAF_DIRECTORY -> {
                val safUri = sConfig?.safDirectoryUri
                Log.i(TAG, "Creating SafFileProviderImpl: treeUri=$safUri, rootPath=$workDir")

                if (!safUri.isNullOrEmpty()) {
                    val treeUri = Uri.parse(safUri)
                    try {
                        // 从 SAF URI 解析实际路径，确保 rootPath 与授权目录匹配
                        val actualRootPath = getActualPathFromSafUri(treeUri)
                        val finalWorkDir = actualRootPath ?: workDir
                        if (actualRootPath != null) {
                            Log.i(TAG, "Using actual rootPath from SAF URI: $finalWorkDir")
                        }
                        sInstance = SafFileProviderImpl(treeUri, finalWorkDir)
                        Log.i(TAG, "SafFileProviderImpl created successfully")
                        return sInstance!!
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to create SafFileProviderImpl", e)
                    }
                }
                // SAF 无效，降级到传统模式
                Log.w(TAG, "SAF URI is empty, falling back to TraditionalFileProvider")
            }
        }

        // MODE_FULL_ACCESS, MODE_LEGACY, MODE_UNKNOWN, or fallback
        Log.i(TAG, "Creating TraditionalFileProvider: workingDir=$workDir")
        sInstance = TraditionalFileProvider(workDir)
        return sInstance!!
    }

    /**
     * 检查路径是否为应用私有目录
     */
    private fun isAppPrivatePath(path: String): Boolean {
        val packageName = sConfig?.packageName ?: return false

        // 应用私有目录前缀
        val privatePrefix = "/data/user/0/$packageName"
        val privatePrefixAlt = "/data/data/$packageName"

        return path.startsWith(privatePrefix) || path.startsWith(privatePrefixAlt)
    }

    /**
     * 强制刷新提供者实例
     */
    @JvmStatic
    @Synchronized
    fun refresh() {
        Log.i(TAG, "refresh: clearing cached provider")
        sInstance = null
        sCurrentMode = -1
        Log.i(TAG, "refresh: provider refreshed")
    }

    /**
     * 检查是否有有效的文件访问权限
     */
    @JvmStatic
    fun hasValidAccess(): Boolean {
        return getCurrentMode() != MODE_UNKNOWN
    }

    /**
     * 获取权限模式描述
     */
    @JvmStatic
    fun getModeDescription(): String {
        return when (getCurrentMode()) {
            MODE_FULL_ACCESS -> "完全访问模式"
            MODE_SAF_DIRECTORY -> "SAF 目录授权模式"
            MODE_LEGACY -> "传统模式"
            else -> "无权限"
        }
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
                // 主存储
                val relativePath = documentId.substring(8)
                val sdcard = Environment.getExternalStorageDirectory().absolutePath
                return "$sdcard/$relativePath"
            } else if (documentId.contains(":")) {
                // 可能是 SD 卡或其他存储设备
                val parts = documentId.split(":", limit = 2)
                val volumeId = parts[0]
                val relativePath = if (parts.size > 1) parts[1] else ""

                // 查找对应的存储路径
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
        if (storageDir.exists()) {
            return storageDir.absolutePath
        }

        return null
    }
}
