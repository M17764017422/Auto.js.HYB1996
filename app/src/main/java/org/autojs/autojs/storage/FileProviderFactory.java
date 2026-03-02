package org.autojs.autojs.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;

import com.stardust.app.GlobalAppContext;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.TraditionalFileProvider;
import com.stardust.autojs.project.ProjectConfig;

import org.autojs.autojs.Pref;

/**
 * 文件访问提供者工厂
 * 根据当前权限状态选择合适的实现
 */
public class FileProviderFactory {

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
            if (Environment.isExternalStorageManager()) {
                return MODE_FULL_ACCESS;
            }
            
            String safUri = Pref.getSafDirectoryUri();
            if (safUri != null && !safUri.isEmpty()) {
                // 检查 SAF 权限是否仍然有效
                if (StoragePermissionHelper.hasSafAccess(GlobalAppContext.get())) {
                    return MODE_SAF_DIRECTORY;
                }
            }
            
            return MODE_UNKNOWN;
        } else {
            // Android 10 及以下
            return MODE_LEGACY;
        }
    }

    /**
     * 获取文件访问提供者实例
     */
    public static synchronized IFileProvider getProvider() {
        int mode = getCurrentMode();
        
        // 如果模式没变，返回缓存实例
        if (sInstance != null && sCurrentMode == mode) {
            return sInstance;
        }
        
        sCurrentMode = mode;
        Context context = GlobalAppContext.get();
        
        switch (mode) {
            case MODE_SAF_DIRECTORY:
                String safUri = Pref.getSafDirectoryUri();
                if (safUri != null && !safUri.isEmpty()) {
                    Uri treeUri = Uri.parse(safUri);
                    String rootPath = Pref.getScriptDirPath();
                    sInstance = new SafFileProviderImpl(context, treeUri, rootPath);
                    return sInstance;
                }
                // SAF 无效，降级到传统模式
                // fall through
                
            case MODE_FULL_ACCESS:
            case MODE_LEGACY:
                sInstance = new TraditionalFileProvider(Pref.getScriptDirPath());
                return sInstance;
                
            case MODE_UNKNOWN:
            default:
                // 无权限时返回传统模式，但操作会失败
                sInstance = new TraditionalFileProvider(Pref.getScriptDirPath());
                return sInstance;
        }
    }

    /**
     * 强制刷新提供者实例
     */
    public static synchronized void refresh() {
        sInstance = null;
        sCurrentMode = -1;
        // 同步更新 ProjectConfig 的文件提供者
        ProjectConfig.setFileProvider(getProvider());
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
