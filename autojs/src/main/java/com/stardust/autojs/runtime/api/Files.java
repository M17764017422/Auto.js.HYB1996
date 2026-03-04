package com.stardust.autojs.runtime.api;

import android.util.Log;

import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.PFileInterface;
import com.stardust.pio.PFiles;
import com.stardust.pio.UncheckedIOException;
import com.stardust.util.Func1;

import com.stardust.pio.FileProviderFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by Stardust on 2018/1/23.
 */

public class Files {

    private static final String TAG = "AutoJS.Files";

    private final ScriptRuntime mRuntime;

    public Files(ScriptRuntime runtime) {
        mRuntime = runtime;
    }

    // FIXME: 2018/10/16 is not correct in sub-directory?
    public String path(String relativePath) {
        String cwd = cwd();
        if (cwd == null || relativePath == null || relativePath.startsWith("/"))
            return relativePath;
        File f = new File(cwd);
        String[] paths = relativePath.split("/");
        for (String path : paths) {
            if (path.equals("."))
                continue;
            if (path.equals("..")) {
                f = f.getParentFile();
                continue;
            }
            f = new File(f, path);
        }
        String path = f.getPath();
        return relativePath.endsWith(File.separator) ? path + "/" : path;
    }

    public String cwd() {
        return mRuntime.engines.myEngine().cwd();
    }

    public PFileInterface open(String path, String mode, String encoding, int bufferSize) {
        String resolvedPath = path(path);
        Log.d(TAG + ".open", "path=" + resolvedPath + ", mode=" + mode + ", encoding=" + encoding + ", bufferSize=" + bufferSize);
        return PFiles.open(resolvedPath, mode, encoding, bufferSize);
    }

    public Object open(String path, String mode, String encoding) {
        String resolvedPath = path(path);
        Log.d(TAG + ".open", "path=" + resolvedPath + ", mode=" + mode + ", encoding=" + encoding);
        return PFiles.open(resolvedPath, mode, encoding);
    }

    public Object open(String path, String mode) {
        String resolvedPath = path(path);
        Log.d(TAG + ".open", "path=" + resolvedPath + ", mode=" + mode);
        return PFiles.open(resolvedPath, mode);
    }

    public Object open(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".open", "path=" + resolvedPath);
        return PFiles.open(resolvedPath);
    }

    public boolean create(String path) {
        String resolvedPath = path(path);
        boolean result = PFiles.create(resolvedPath);
        Log.d(TAG + ".create", "path=" + resolvedPath + ", result=" + result);
        return result;
    }

    public boolean createIfNotExists(String path) {
        String resolvedPath = path(path);
        boolean result = PFiles.createIfNotExists(resolvedPath);
        Log.d(TAG + ".create", "path=" + resolvedPath + ", result=" + result);
        return result;
    }

    public boolean createWithDirs(String path) {
        String resolvedPath = path(path);
        boolean result = PFiles.createWithDirs(resolvedPath);
        Log.d(TAG + ".create", "path=" + resolvedPath + ", result=" + result);
        return result;
    }

    public boolean mkdir(String path) {
        String resolvedPath = path(path);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        boolean result = provider.mkdir(resolvedPath);
        Log.d(TAG + ".mkdir", "path=" + resolvedPath + ", result=" + result);
        return result;
    }

    public boolean mkdirs(String path) {
        String resolvedPath = path(path);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        boolean result = provider.mkdirs(resolvedPath);
        Log.d(TAG + ".mkdirs", "path=" + resolvedPath + ", result=" + result);
        return result;
    }

    public boolean exists(String path) {
        String resolvedPath = path(path);
        boolean result = PFiles.exists(resolvedPath);
        Log.d(TAG + ".exists", "path=" + resolvedPath + ", result=" + result);
        return result;
    }

    public boolean ensureDir(String path) {
        return PFiles.ensureDir(path(path));
    }

    public String read(String path, String encoding) {
        String resolvedPath = path(path);
        Log.d(TAG + ".read", "path=" + resolvedPath + ", encoding=" + encoding);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        String result = provider.read(resolvedPath, encoding);
        Log.d(TAG + ".read", "result: length=" + (result != null ? result.length() : 0));
        return result;
    }


    public String read(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".read", "path=" + resolvedPath);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        String result = provider.read(resolvedPath);
        Log.d(TAG + ".read", "result: length=" + (result != null ? result.length() : 0));
        return result;
    }

    public String readAssets(String path, String encoding) {
        try {
            return PFiles.read(mRuntime.getUiHandler().getContext().getAssets().open(path), encoding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String readAssets(String path) {
        return readAssets(path, "UTF-8");
    }

    public byte[] readBytes(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".readBytes", "path=" + resolvedPath);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        byte[] result = provider.readBytes(resolvedPath);
        Log.d(TAG + ".readBytes", "result: size=" + (result != null ? result.length : 0) + " bytes");
        return result;
    }

    public boolean write(String path, String text) {
        String resolvedPath = path(path);
        Log.d(TAG + ".write", "path=" + resolvedPath + ", length=" + (text != null ? text.length() : 0));
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        return provider.write(resolvedPath, text);
    }

    public boolean write(String path, String text, String encoding) {
        String resolvedPath = path(path);
        Log.d(TAG + ".write", "path=" + resolvedPath + ", length=" + (text != null ? text.length() : 0) + ", encoding=" + encoding);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        return provider.write(resolvedPath, text, encoding);
    }

    public boolean append(String path, String text) {
        String resolvedPath = path(path);
        Log.d(TAG + ".append", "path=" + resolvedPath + ", length=" + (text != null ? text.length() : 0));
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        return provider.append(resolvedPath, text, "UTF-8");
    }

    public boolean append(String path, String text, String encoding) {
        String resolvedPath = path(path);
        Log.d(TAG + ".append", "path=" + resolvedPath + ", length=" + (text != null ? text.length() : 0) + ", encoding=" + encoding);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        return provider.append(resolvedPath, text, encoding);
    }

    public boolean writeBytes(String path, byte[] bytes) {
        String resolvedPath = path(path);
        Log.d(TAG + ".writeBytes", "path=" + resolvedPath + ", size=" + (bytes != null ? bytes.length : 0));
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        return provider.writeBytes(resolvedPath, bytes);
    }

    public boolean copy(String pathFrom, String pathTo) {
        String from = path(pathFrom);
        String to = path(pathTo);
        Log.d(TAG + ".copy", "from=" + from + ", to=" + to);
        IFileProvider provider = FileProviderFactory.getProvider(from);
        boolean result = provider.copy(from, to);
        Log.d(TAG + ".copy", "result=" + result);
        return result;
    }

    public boolean renameWithoutExtension(String path, String newName) {
        String resolvedPath = path(path);
        Log.d(TAG + ".rename", "path=" + resolvedPath + ", newName=" + newName);
        boolean result = PFiles.renameWithoutExtension(resolvedPath, newName);
        Log.d(TAG + ".rename", "result=" + result);
        return result;
    }

    public boolean rename(String path, String newName) {
        String resolvedPath = path(path);
        Log.d(TAG + ".rename", "path=" + resolvedPath + ", newName=" + newName);
        boolean result = PFiles.rename(resolvedPath, newName);
        Log.d(TAG + ".rename", "result=" + result);
        return result;
    }

    public boolean move(String path, String newPath) {
        String from = path(path);
        String to = path(newPath);
        Log.d(TAG + ".move", "from=" + from + ", to=" + to);
        IFileProvider provider = FileProviderFactory.getProvider(from);
        boolean result = provider.move(from, to);
        Log.d(TAG + ".move", "result=" + result);
        return result;
    }

    public String getExtension(String fileName) {
        return PFiles.getExtension(fileName);
    }

    public String getName(String filePath) {
        return PFiles.getName(filePath);
    }

    public String getNameWithoutExtension(String filePath) {
        return PFiles.getNameWithoutExtension(filePath);
    }

    public boolean remove(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".remove", "path=" + resolvedPath);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        boolean result = provider.delete(resolvedPath);
        Log.d(TAG + ".remove", "result=" + result);
        return result;
    }

    public boolean removeDir(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".removeDir", "path=" + resolvedPath);
        boolean result = PFiles.removeDir(resolvedPath);
        Log.d(TAG + ".removeDir", "result=" + result);
        return result;
    }

    public String getSdcardPath() {
        return PFiles.getSdcardPath();
    }

    public String[] listDir(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".listDir", "path=" + resolvedPath);
        String[] result = PFiles.listDir(resolvedPath);
        Log.d(TAG + ".listDir", "result: count=" + (result != null ? result.length : 0));
        return result;
    }

    public String[] listDir(String path, Func1<String, Boolean> filter) {
        String resolvedPath = path(path);
        Log.d(TAG + ".listDir", "path=" + resolvedPath + " (with filter)");
        String[] result = PFiles.listDir(resolvedPath, filter);
        Log.d(TAG + ".listDir", "result: count=" + (result != null ? result.length : 0));
        return result;
    }

    public boolean isFile(String path) {
        return PFiles.isFile(path(path));
    }

    public boolean isDir(String path) {
        return PFiles.isDir(path(path));
    }

    public boolean isEmptyDir(String path) {
        return PFiles.isEmptyDir(path(path));
    }

    public static String join(String parent, String... child) {
        return PFiles.join(parent, child);
    }

    public String getHumanReadableSize(long bytes) {
        return PFiles.getHumanReadableSize(bytes);
    }

    public String getSimplifiedPath(String path) {
        return PFiles.getSimplifiedPath(path);
    }

}
