package org.autojs.autojs.storage;

import android.content.Context;

import com.stardust.app.GlobalAppContext;
import com.stardust.pio.FileProviderConfig;

import org.autojs.autojs.Pref;

/**
 * 应用文件访问提供者配置实现
 */
public class AppFileProviderConfig implements FileProviderConfig {

    private final Context mContext;

    public AppFileProviderConfig(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public String getSafDirectoryUri() {
        return Pref.getSafDirectoryUri();
    }

    @Override
    public String getScriptDirPath() {
        return Pref.getScriptDirPath();
    }

    @Override
    public boolean hasSafAccess() {
        return StoragePermissionHelper.hasSafAccess(mContext);
    }

    @Override
    public String getPackageName() {
        return mContext.getPackageName();
    }
}