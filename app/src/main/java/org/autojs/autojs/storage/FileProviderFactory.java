package org.autojs.autojs.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.stardust.app.GlobalAppContext;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.TraditionalFileProvider;
import com.stardust.autojs.project.ProjectConfig;

import org.autojs.autojs.Pref;

import java.io.File;

/**
 * 文件访问提供者工厂
 * 根据当前权限状态选择合适的实现
 */
public class FileProviderFactory {

    private static final String TAG = "FileProviderFactory";
    
    private static IFileProvider sInstance;
    private static int sCurrentMode = -1;

    /**
     * 权限模式
     */
    public static final int MODE_UNKNOWN = 0;
    public static final int MODE_FULL_ACCESS = 1;      // MANAGE_EXTERNAL_STORAGE
    public static final int MODE_SAF_DIRECTORY = 2;    // SAF 目录授权
    public static final int MODE_LEGACY = 3;           // Android 10 及以下传统模式

    /**
     * 获取当前权限模式
     */
    public static int getCurrentMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            boolean isStorageManager = Environment.isExternalStorageManager();
            String safUri = Pref.getSafDirectoryUri();
            boolean hasSafUri = safUri != null && !safUri.isEmpty();
            boolean hasSafAccess = false;
            
            if (hasSafUri) {
                hasSafAccess = StoragePermissionHelper.hasSafAccess(GlobalAppContext.get());
            }
            
            Log.d(TAG, "getCurrentMode: SDK=" + Build.VERSION.SDK_INT 
                    + ", isStorageManager=" + isStorageManager
                    + ", safUri=" + safUri
                    + ", hasSafAccess=" + hasSafAccess);
            
            if (isStorageManager) {
                Log.i(TAG, "Mode: FULL_ACCESS (MANAGE_EXTERNAL_STORAGE)");
                return MODE_FULL_ACCESS;
            }
            
            if (hasSafUri && hasSafAccess) {
                Log.i(TAG, "Mode: SAF_DIRECTORY");
                return MODE_SAF_DIRECTORY;
            }
            
            Log.w(TAG, "Mode: UNKNOWN (no valid permission)");
            return MODE_UNKNOWN;
        } else {
            // Android 10 及以下
            Log.i(TAG, "Mode: LEGACY (Android " + Build.VERSION.SDK_INT + ")");
            return MODE_LEGACY;
        }
    }

    /**
     * 获取文件访问提供者实例
     */
    public static synchronized IFileProvider getProvider() {
        return getProvider(null);
    }
    
    /**
     * 根据路径获取合适的文件访问提供者实例
     * @param path 要访问的文件路径，如果为 null 则返回默认提供者
     */
    public static synchronized IFileProvider getProvider(String path) {
        // 对于应用私有目录，始终使用 TraditionalFileProvider
        if (path != null && isAppPrivatePath(path)) {
            Log.d(TAG, "getProvider: path=" + path + " is app private, using TraditionalFileProvider");
            return new TraditionalFileProvider(Pref.getScriptDirPath());
        }
        
        int mode = getCurrentMode();
        
        Log.d(TAG, "getProvider: path=" + path + ", mode=" + mode + " (" + getModeDescription() + ")" 
                + ", cachedInstance=" + (sInstance != null) 
                + ", cachedMode=" + sCurrentMode);
        
        // 如果模式没变，返回缓存实例
        if (sInstance != null && sCurrentMode == mode) {
            Log.d(TAG, "Returning cached provider: " + sInstance.getClass().getSimpleName());
            return sInstance;
        }
        
        sCurrentMode = mode;
        Context context = GlobalAppContext.get();
        
        switch (mode) {
            case MODE_SAF_DIRECTORY:
                String safUri = Pref.getSafDirectoryUri();
                // 尝试从 SAF URI 解析实际路径，失败则使用配置的脚本目录
                String rootPath = getActualPathFromSafUri(safUri);
                if (rootPath == null) {
                    rootPath = Pref.getScriptDirPath();
                }
                Log.i(TAG, "Creating SafFileProviderImpl: treeUri=" + safUri + ", rootPath=" + rootPath);
                
                if (safUri != null && !safUri.isEmpty()) {
                    Uri treeUri = Uri.parse(safUri);
                    sInstance = new SafFileProviderImpl(context, treeUri, rootPath);
                    Log.i(TAG, "SafFileProviderImpl created successfully");
                    return sInstance;
                }
                // SAF 无效，降级到传统模式
                Log.w(TAG, "SAF URI is empty, falling back to TraditionalFileProvider");
                // fall through
                
            case MODE_FULL_ACCESS:
            case MODE_LEGACY:
                String workDir = Pref.getScriptDirPath();
                Log.i(TAG, "Creating TraditionalFileProvider: workingDir=" + workDir);
                sInstance = new TraditionalFileProvider(workDir);
                return sInstance;
                
            case MODE_UNKNOWN:
            default:
                Log.w(TAG, "No valid permission, creating TraditionalFileProvider (operations may fail)");
                sInstance = new TraditionalFileProvider(Pref.getScriptDirPath());
                return sInstance;
        }
    }
    
    /**
     * 检查路径是否为应用私有目录
     */
    private static boolean isAppPrivatePath(String path) {
        if (path == null) return false;
        
        Context context = GlobalAppContext.get();
        if (context == null) return false;
        
        // 应用私有目录前缀
        String privatePrefix = "/data/user/0/" + context.getPackageName();
        String privatePrefixAlt = "/data/data/" + context.getPackageName();
        
        // 应用内部存储目录
        String filesDir = context.getFilesDir().getAbsolutePath();
        String cacheDir = context.getCacheDir().getAbsolutePath();
        
        return path.startsWith(privatePrefix) 
                || path.startsWith(privatePrefixAlt)
                || path.startsWith(filesDir)
                || path.startsWith(cacheDir);
    }

    /**
     * 强制刷新提供者实例
     */
    public static synchronized void refresh() {
        Log.i(TAG, "refresh: clearing cached provider");
        sInstance = null;
        sCurrentMode = -1;
        // 同步更新 ProjectConfig 的文件提供者
        IFileProvider newProvider = getProvider();
        ProjectConfig.setFileProvider(newProvider);
        Log.i(TAG, "refresh: ProjectConfig updated with new provider");
    }

    /**
     * 检查是否有有效的文件访问权限
     */
    public static boolean hasValidAccess() {
        int mode = getCurrentMode();
        return mode != MODE_UNKNOWN;
    }

    /**
     * 获取权限模式描述
     */
    public static String getModeDescription() {
        int mode = getCurrentMode();
        switch (mode) {
            case MODE_FULL_ACCESS:
                return "完全访问模式";
            case MODE_SAF_DIRECTORY:
                return "SAF 目录授权模式";
            case MODE_LEGACY:
                return "传统模式";
            default:
                return "无权限";
        }
    }

    /**
     * 从 SAF URI 解析实际文件系统路径
     * 支持 ExternalStorageProvider 和 MediaDocumentsProvider
     * 
     * @param safUri SAF 目录 URI 字符串
     * @return 实际文件系统路径，解析失败返回 null
     */
    private static String getActualPathFromSafUri(String safUri) {
        if (safUri == null || safUri.isEmpty()) {
            return null;
        }
        
        try {
            Uri treeUri = Uri.parse(safUri);
            String documentId = DocumentsContract.getTreeDocumentId(treeUri);
            
            Log.d(TAG, "getActualPathFromSafUri: documentId=" + documentId);
            
            // ExternalStorageProvider 格式: primary:Download 或 1111-2222:Download
            if (documentId.startsWith("primary:")) {
                // 主存储
                String relativePath = documentId.substring(8); // "primary:".length()
                String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
                return sdcard + "/" + relativePath;
            } else if (documentId.contains(":")) {
                // 可能是 SD 卡或其他存储设备
                String[] parts = documentId.split(":", 2);
                String volumeId = parts[0];
                String relativePath = parts.length > 1 ? parts[1] : "";
                
                // 尝试查找对应的存储路径
                String storagePath = findStoragePath(volumeId);
                if (storagePath != null) {
                    return relativePath.isEmpty() ? storagePath : storagePath + "/" + relativePath;
                }
            }
            
            // 尝试通过 ContentResolver 查询获取路径
            Context context = GlobalAppContext.get();
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                String[] projection = {DocumentsContract.Document.COLUMN_DISPLAY_NAME};
                try (Cursor cursor = context.getContentResolver().query(docUri, projection, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        String displayName = cursor.getString(0);
                        Log.d(TAG, "getActualPathFromSafUri: displayName=" + displayName);
                        // 如果查询到了显示名称，返回一个基于主存储的路径
                        // 这不是完美的解决方案，但至少能让用户看到授权的目录名
                        String sdcard = Environment.getExternalStorageDirectory().getAbsolutePath();
                        return sdcard + "/" + displayName;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "getActualPathFromSafUri failed", e);
        }
        
        return null;
    }
    
    /**
     * 根据 volume ID 查找存储路径
     */
    private static String findStoragePath(String volumeId) {
        // primary 通常是主存储
        if ("primary".equalsIgnoreCase(volumeId)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        
        // 查找外部存储（SD 卡）
        File[] externalDirs = ContextCompat.getExternalFilesDirs(GlobalAppContext.get(), null);
        if (externalDirs != null) {
            for (File dir : externalDirs) {
                if (dir != null) {
                    String path = dir.getAbsolutePath();
                    // 例如: /storage/1111-2222/Android/data/...
                    // 提取 /storage/1111-2222 部分
                    if (path.contains("/storage/")) {
                        String[] parts = path.split("/");
                        if (parts.length >= 3) {
                            String storageId = parts[2]; // 1111-2222
                            if (storageId.equals(volumeId)) {
                                return "/storage/" + storageId;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }
}
