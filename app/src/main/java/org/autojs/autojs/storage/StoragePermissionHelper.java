package org.autojs.autojs.storage;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.autojs.autojs.Pref;

import java.util.List;

/**
 * 存储权限管理帮助类
 * 支持 Android 11+ 的 SAF (Storage Access Framework) 目录授权
 * 和 MANAGE_EXTERNAL_STORAGE 完全访问权限
 */
public class StoragePermissionHelper {

    private static final String TAG = "StoragePermission";

    private static final int REQUEST_CODE_SAF_DIRECTORY = 10001;
    private static final int REQUEST_CODE_MANAGE_STORAGE = 10002;

    /**
     * 检查是否有存储访问权限
     */
    public static boolean hasStorageAccess(Context context) {
        Log.d(TAG, "hasStorageAccess: checking...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: 检查 MANAGE_EXTERNAL_STORAGE 或 SAF 权限
            boolean isManager = Environment.isExternalStorageManager();
            boolean hasSaf = hasSafAccess(context);
            Log.d(TAG, "hasStorageAccess: Android 11+, isStorageManager=" + isManager + ", hasSaf=" + hasSaf);
            if (isManager) {
                return true;
            }
            return hasSaf;
        } else {
            // Android 10 及以下：检查传统权限
            boolean result = hasLegacyStoragePermission(context);
            Log.d(TAG, "hasStorageAccess: Android " + Build.VERSION.SDK_INT + ", result=" + result);
            return result;
        }
    }

    /**
     * 检查是否有 SAF 目录访问权限
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean hasSafAccess(Context context) {
        String safUri = Pref.getSafDirectoryUri();
        Log.d(TAG, "hasSafAccess: safUri=" + safUri);
        if (safUri == null || safUri.isEmpty()) {
            Log.d(TAG, "hasSafAccess: no SAF URI stored");
            return false;
        }
        
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        Log.d(TAG, "hasSafAccess: persisted permissions count=" + permissions.size());
        
        for (UriPermission permission : permissions) {
            String permUri = permission.getUri().toString();
            Log.v(TAG, "hasSafAccess: checking permission uri=" + permUri 
                    + ", read=" + permission.isReadPermission() 
                    + ", write=" + permission.isWritePermission());
            if (permUri.equals(safUri)) {
                boolean result = permission.isReadPermission() && permission.isWritePermission();
                Log.d(TAG, "hasSafAccess: found matching URI, result=" + result);
                return result;
            }
        }
        Log.d(TAG, "hasSafAccess: no matching permission found");
        return false;
    }

    /**
     * 请求存储权限（自动选择最佳方式）
     */
    public static void requestStoragePermission(Activity activity) {
        Log.d(TAG, "requestStoragePermission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+: 优先使用 SAF
            requestSafDirectoryAccess(activity);
        } else {
            // Android 10 及以下：使用传统权限（由调用者处理）
            // 这里不做处理，由 MainActivity 的 checkPermission 处理
            Log.d(TAG, "requestStoragePermission: legacy mode, handled by caller");
        }
    }

    /**
     * 使用 SAF 请求目录访问权限
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void requestSafDirectoryAccess(Activity activity) {
        Log.i(TAG, "requestSafDirectoryAccess: opening SAF directory picker");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        
        // 尝试设置初始目录为外部存储根目录
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Uri initialUri = Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri);
            Log.d(TAG, "requestSafDirectoryAccess: set initial URI=" + initialUri);
        }
        
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION 
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        
        activity.startActivityForResult(intent, REQUEST_CODE_SAF_DIRECTORY);
    }

    /**
     * 请求完全存储访问权限 (MANAGE_EXTERNAL_STORAGE)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public static void requestFullStorageAccess(Activity activity) {
        Log.i(TAG, "requestFullStorageAccess: opening MANAGE_APP_ALL_FILES_ACCESS_PERMISSION settings");
        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE);
    }

    /**
     * 处理 SAF 授权结果
     * @return 是否授权成功
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean handleSafResult(Activity activity, int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "handleSafResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode != REQUEST_CODE_SAF_DIRECTORY) {
            return false;
        }
        
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            Log.i(TAG, "handleSafResult: SAF authorized, treeUri=" + treeUri);
            if (treeUri != null) {
                // 持久化权限
                activity.getContentResolver().takePersistableUriPermission(
                        treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
                Log.i(TAG, "handleSafResult: persisted URI permission");
                
                // 保存授权的 URI
                Pref.setSafDirectoryUri(treeUri.toString());
                Log.i(TAG, "handleSafResult: saved SAF URI to preferences");
                
                return true;
            }
        }
        Log.w(TAG, "handleSafResult: SAF authorization failed or cancelled");
        return false;
    }

    /**
     * 处理完全存储访问授权结果
     */
    @RequiresApi(Build.VERSION_CODES.R)
    public static boolean handleManageStorageResult(int requestCode) {
        Log.d(TAG, "handleManageStorageResult: requestCode=" + requestCode);
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            boolean result = Environment.isExternalStorageManager();
            Log.i(TAG, "handleManageStorageResult: isExternalStorageManager=" + result);
            return result;
        }
        return false;
    }

    /**
     * 获取已授权的 SAF 目录 Uri
     */
    public static Uri getSafDirectoryUri() {
        String uriString = Pref.getSafDirectoryUri();
        if (uriString != null && !uriString.isEmpty()) {
            return Uri.parse(uriString);
        }
        return null;
    }

    /**
     * 清除 SAF 权限
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public static void clearSafPermission(Context context) {
        Log.i(TAG, "clearSafPermission: releasing all persisted URI permissions");
        List<UriPermission> permissions = context.getContentResolver().getPersistedUriPermissions();
        for (UriPermission permission : permissions) {
            context.getContentResolver().releasePersistableUriPermission(
                    permission.getUri(),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
            Log.d(TAG, "clearSafPermission: released " + permission.getUri());
        }
        Pref.setSafDirectoryUri(null);
        Log.i(TAG, "clearSafPermission: cleared SAF URI from preferences");
    }

    /**
     * 检查传统存储权限
     */
    private static boolean hasLegacyStoragePermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            return result == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    /**
     * 获取请求码
     */
    public static int getRequestCodeSafDirectory() {
        return REQUEST_CODE_SAF_DIRECTORY;
    }

    public static int getRequestCodeManageStorage() {
        return REQUEST_CODE_MANAGE_STORAGE;
    }
}
