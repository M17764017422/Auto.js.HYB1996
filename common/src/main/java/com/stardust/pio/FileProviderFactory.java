package com.stardust.pio;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * 文件访问提供者工厂
 * 根据当前权限状态选择合适的实现
 */
public class FileProviderFactory {

    private static final String TAG = "FileProviderFactory";
    
    private static IFileProvider sInstance;
    private static int sCurrentMode = -1;
    private static WeakReference<FileProviderConfig> sConfigRef;

    /**
     * 权限模式
     */
    public static final int MODE_UNKNOWN = 0;
    public static final int MODE_FULL_ACCESS = 1;      // MANAGE_EXTERNAL_STORAGE
    public static final int MODE_SAF_DIRECTORY = 2;    // SAF 目录授权
    public static final int MODE_LEGACY = 3;           // Android 10 及以下传统模式

    /**
     * 设置配置提供者（由应用模块在初始化时调用）
     */
    public static synchronized void setConfig(FileProviderConfig config) {
        sConfigRef = new WeakReference<>(config);
        Log.i(TAG, "Config set: " + (config != null ? "provided" : "null"));
    }

    /**
     * 获取配置提供者
     */
    private static FileProviderConfig getConfig() {
        return sConfigRef != null ? sConfigRef.get() : null;
    }

    /**
     * 获取当前权限模式
     */
    public static int getCurrentMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+
            boolean isStorageManager = Environment.isExternalStorageManager();
            
            FileProviderConfig config = getConfig();
            String safUri = config != null ? config.getSafDirectoryUri() : null;
            boolean hasSafUri = safUri != null && !safUri.isEmpty();
            boolean hasSafAccess = false;
            
            if (hasSafUri && config != null) {
                hasSafAccess = config.hasSafAccess();
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
            FileProviderConfig config = getConfig();
            String workDir = config != null ? config.getScriptDirPath() : "";
            Log.d(TAG, "getProvider: path=" + path + " is app private, using TraditionalFileProvider");
            return new TraditionalFileProvider(workDir);
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
        FileProviderConfig config = getConfig();
        String workDir = config != null ? config.getScriptDirPath() : "";
        
        switch (mode) {
            case MODE_SAF_DIRECTORY:
                String safUri = config != null ? config.getSafDirectoryUri() : null;
                Log.i(TAG, "Creating SafFileProviderImpl: treeUri=" + safUri + ", rootPath=" + workDir);
                
                if (safUri != null && !safUri.isEmpty()) {
                    Uri treeUri = Uri.parse(safUri);
                    try {
                        sInstance = new SafFileProviderImpl(treeUri, workDir);
                        Log.i(TAG, "SafFileProviderImpl created successfully");
                        return sInstance;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to create SafFileProviderImpl", e);
                    }
                }
                // SAF 无效，降级到传统模式
                Log.w(TAG, "SAF URI is empty, falling back to TraditionalFileProvider");
                // fall through
                
            case MODE_FULL_ACCESS:
            case MODE_LEGACY:
                Log.i(TAG, "Creating TraditionalFileProvider: workingDir=" + workDir);
                sInstance = new TraditionalFileProvider(workDir);
                return sInstance;
                
            case MODE_UNKNOWN:
            default:
                Log.w(TAG, "No valid permission, creating TraditionalFileProvider (operations may fail)");
                sInstance = new TraditionalFileProvider(workDir);
                return sInstance;
        }
    }
    
    /**
     * 检查路径是否为应用私有目录
     */
    private static boolean isAppPrivatePath(String path) {
        if (path == null) return false;
        
        FileProviderConfig config = getConfig();
        if (config == null) return false;
        
        String packageName = config.getPackageName();
        if (packageName == null) return false;
        
        // 应用私有目录前缀
        String privatePrefix = "/data/user/0/" + packageName;
        String privatePrefixAlt = "/data/data/" + packageName;
        
        return path.startsWith(privatePrefix) || path.startsWith(privatePrefixAlt);
    }

    /**
     * 强制刷新提供者实例
     */
    public static synchronized void refresh() {
        Log.i(TAG, "refresh: clearing cached provider");
        sInstance = null;
        sCurrentMode = -1;
        Log.i(TAG, "refresh: provider refreshed");
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
}