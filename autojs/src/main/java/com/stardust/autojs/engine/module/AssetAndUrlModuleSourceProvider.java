package com.stardust.autojs.engine.module;

import android.content.res.AssetManager;
import android.util.Log;

import com.stardust.autojs.engine.encryption.ScriptEncryption;
import com.stardust.autojs.script.EncryptedScriptFileHeader;
import com.stardust.pio.PFiles;

import org.mozilla.javascript.commonjs.module.provider.ModuleSource;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by Stardust on 2017/5/9.
 */

public class AssetAndUrlModuleSourceProvider extends UrlModuleSourceProvider {

    private static final String TAG = "AssetAndUrlModuleSource";

    private android.content.Context mContext;
    private final URI mBaseURI;
    private final String mAssetDirPath;
    private final AssetManager mAssetManager;

    public AssetAndUrlModuleSourceProvider(android.content.Context context, String assetDirPath, List<URI> list) {
        super(list, null);
        mContext = context;
        mAssetDirPath = assetDirPath;
        mBaseURI = URI.create("file:///android_asset/" + assetDirPath);
        mAssetManager = mContext.getAssets();
    }

    @Override
    protected ModuleSource loadFromActualUri(URI uri, URI base, Object validator)
            throws IOException {
        // 对于 file:// URI，使用 PFiles 支持 SAF 模式
        if ("file".equals(uri.getScheme())) {
            try {
                String path = new URL(uri.toString()).getPath();
                Log.d(TAG, "loadFromActualUri: path=" + path);
                
                // 使用 PFiles.read() 支持 SAF
                String content = PFiles.read(path);
                if (content == null) {
                    return null;
                }
                
                byte[] bytes = content.getBytes("UTF-8");
                
                // 检查是否是加密脚本
                if (EncryptedScriptFileHeader.INSTANCE.isValidFile(bytes)) {
                    byte[] clearText = ScriptEncryption.INSTANCE.decrypt(bytes, 
                            EncryptedScriptFileHeader.BLOCK_SIZE, bytes.length);
                    return new ModuleSource(
                            new InputStreamReader(new ByteArrayInputStream(clearText)),
                            null, uri, base, validator);
                }
                
                return new ModuleSource(
                        new InputStreamReader(new ByteArrayInputStream(bytes)),
                        null, uri, base, validator);
            } catch (Exception e) {
                Log.e(TAG, "loadFromActualUri: failed for " + uri, e);
                return null;
            }
        }
        
        // 对于其他 URI（http/https），使用父类方法
        return super.loadFromActualUri(uri, base, validator);
    }

    @Override
    protected ModuleSource loadFromPrivilegedLocations(String moduleId, Object validator) throws IOException, URISyntaxException {
        String moduleIdWithExtension = moduleId;
        if (!moduleIdWithExtension.endsWith(".js")) {
            moduleIdWithExtension += ".js";
        }
        try {
            return new ModuleSource(new InputStreamReader(mAssetManager.open(mAssetDirPath + "/" + moduleIdWithExtension)), null,
                    new URI(mBaseURI.toString() + "/" + moduleIdWithExtension), mBaseURI, validator);
        } catch (FileNotFoundException e) {
            return super.loadFromPrivilegedLocations(moduleId, validator);
        }
    }

    @Override
    protected Reader getReader(URLConnection urlConnection) throws IOException {
        InputStream stream = urlConnection.getInputStream();
        byte[] bytes = new byte[stream.available()];
        stream.read(bytes);
        stream.close();
        if (EncryptedScriptFileHeader.INSTANCE.isValidFile(bytes)) {
            byte[] clearText = ScriptEncryption.INSTANCE.decrypt(bytes, EncryptedScriptFileHeader.BLOCK_SIZE, bytes.length);
            return new InputStreamReader(new ByteArrayInputStream(clearText));
        }
        return new InputStreamReader(new ByteArrayInputStream(bytes));
    }
}