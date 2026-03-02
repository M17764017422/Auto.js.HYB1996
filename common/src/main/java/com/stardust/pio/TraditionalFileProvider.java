package com.stardust.pio;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 传统 File API 实现
 * 适用于拥有完全存储权限或应用私有目录的场景
 */
public class TraditionalFileProvider implements IFileProvider {

    private static final String TAG = "TraditionalFileProvider";

    private String mWorkingDirectory;

    public TraditionalFileProvider() {
        mWorkingDirectory = "/";
        Log.d(TAG, "Created: workingDir=/");
    }

    public TraditionalFileProvider(String workingDirectory) {
        mWorkingDirectory = workingDirectory;
        Log.d(TAG, "Created: workingDir=" + workingDirectory);
    }

    @Override
    public boolean exists(String path) {
        return new File(resolvePath(path)).exists();
    }

    @Override
    public boolean isFile(String path) {
        return new File(resolvePath(path)).isFile();
    }

    @Override
    public boolean isDirectory(String path) {
        return new File(resolvePath(path)).isDirectory();
    }

    @Override
    public boolean mkdir(String path) {
        return new File(resolvePath(path)).mkdir();
    }

    @Override
    public boolean mkdirs(String path) {
        return new File(resolvePath(path)).mkdirs();
    }

    @Override
    public boolean delete(String path) {
        return new File(resolvePath(path)).delete();
    }

    @Override
    public boolean deleteRecursively(String path) {
        File file = new File(resolvePath(path));
        return deleteRecursive(file);
    }

    private boolean deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        return file.delete();
    }

    @Override
    public boolean rename(String path, String newName) {
        File file = new File(resolvePath(path));
        File newFile = new File(file.getParent(), newName);
        return file.renameTo(newFile);
    }

    @Override
    public boolean move(String fromPath, String toPath) {
        File from = new File(resolvePath(fromPath));
        File to = new File(resolvePath(toPath));
        return from.renameTo(to);
    }

    @Override
    public boolean copy(String fromPath, String toPath) {
        try {
            File from = new File(resolvePath(fromPath));
            File to = new File(resolvePath(toPath));
            
            if (from.isDirectory()) {
                return copyDirectory(from, to);
            } else {
                return copyFile(from, to);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean copyFile(File from, File to) throws Exception {
        try (FileInputStream fis = new FileInputStream(from);
             FileOutputStream fos = new FileOutputStream(to)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            return true;
        }
    }

    private boolean copyDirectory(File from, File to) throws Exception {
        if (!to.exists()) {
            to.mkdirs();
        }
        
        File[] children = from.listFiles();
        if (children == null) return true;
        
        for (File child : children) {
            File newChild = new File(to, child.getName());
            if (child.isDirectory()) {
                copyDirectory(child, newChild);
            } else {
                copyFile(child, newChild);
            }
        }
        return true;
    }

    @Override
    public List<FileInfo> listFiles(String path) {
        String resolvedPath = resolvePath(path);
        Log.d(TAG, "listFiles: path=" + path + ", resolved=" + resolvedPath);
        List<FileInfo> result = new ArrayList<>();
        File dir = new File(resolvedPath);
        File[] files = dir.listFiles();
        if (files == null) {
            Log.w(TAG, "listFiles: listFiles() returned null, path may not be a directory or no permission");
            return result;
        }
        
        for (File file : files) {
            result.add(new FileInfo(
                    file.getName(),
                    file.getAbsolutePath(),
                    file.isDirectory(),
                    file.length(),
                    file.lastModified()
            ));
        }
        Log.d(TAG, "listFiles: found " + result.size() + " items");
        return result;
    }

    @Override
    public String read(String path, String encoding) {
        String resolvedPath = resolvePath(path);
        Log.d(TAG, "read: path=" + path + ", resolved=" + resolvedPath + ", encoding=" + encoding);
        try {
            String result = PFiles.read(resolvedPath, encoding);
            Log.d(TAG, "read: success, length=" + (result != null ? result.length() : 0));
            return result;
        } catch (Exception e) {
            Log.e(TAG, "read: error=" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String read(String path) {
        return read(resolvePath(path), "UTF-8");
    }

    @Override
    public byte[] readBytes(String path) {
        String resolvedPath = resolvePath(path);
        Log.d(TAG, "readBytes: path=" + path + ", resolved=" + resolvedPath);
        try {
            File file = new File(resolvedPath);
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                Log.d(TAG, "readBytes: success, size=" + data.length);
                return data;
            }
        } catch (Exception e) {
            Log.e(TAG, "readBytes: error=" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public InputStream openInputStream(String path) throws Exception {
        String resolvedPath = resolvePath(path);
        Log.d(TAG, "openInputStream: resolved=" + resolvedPath);
        return new BufferedInputStream(new FileInputStream(resolvedPath));
    }

    @Override
    public boolean write(String path, String content, String encoding) {
        String resolvedPath = resolvePath(path);
        Log.d(TAG, "write: path=" + path + ", resolved=" + resolvedPath + ", contentLen=" + content.length());
        try {
            PFiles.write(resolvedPath, content, encoding);
            Log.d(TAG, "write: success");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "write: error=" + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean write(String path, String content) {
        return write(resolvePath(path), content, "UTF-8");
    }

    @Override
    public boolean append(String path, String content, String encoding) {
        String resolvedPath = resolvePath(path);
        Log.d(TAG, "append: resolved=" + resolvedPath);
        try {
            PFiles.append(resolvedPath, content, encoding);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "append: error=" + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean writeBytes(String path, byte[] bytes) {
        String resolvedPath = resolvePath(path);
        Log.d(TAG, "writeBytes: resolved=" + resolvedPath + ", size=" + bytes.length);
        try (FileOutputStream fos = new FileOutputStream(resolvedPath)) {
            fos.write(bytes);
            Log.d(TAG, "writeBytes: success");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "writeBytes: error=" + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public OutputStream openOutputStream(String path) throws Exception {
        return openOutputStream(path, false);
    }

    @Override
    public OutputStream openOutputStream(String path, boolean append) throws Exception {
        return new BufferedOutputStream(new FileOutputStream(resolvePath(path), append));
    }

    @Override
    public long length(String path) {
        return new File(resolvePath(path)).length();
    }

    @Override
    public long lastModified(String path) {
        return new File(resolvePath(path)).lastModified();
    }

    @Override
    public String getName(String path) {
        return new File(resolvePath(path)).getName();
    }

    @Override
    public String getParent(String path) {
        return new File(resolvePath(path)).getParent();
    }

    @Override
    public String getExtension(String path) {
        return PFiles.getExtension(resolvePath(path));
    }

    @Override
    public boolean isAccessible(String path) {
        // 传统模式下，只要文件存在或父目录可写就认为可访问
        String resolvedPath = resolvePath(path);
        File file = new File(resolvedPath);
        boolean result;
        if (file.exists()) {
            result = file.canRead();
            Log.d(TAG, "isAccessible: path=" + resolvedPath + ", exists=true, canRead=" + result);
        } else {
            File parent = file.getParentFile();
            result = parent != null && parent.canWrite();
            Log.d(TAG, "isAccessible: path=" + resolvedPath + ", exists=false, parentWritable=" + result);
        }
        return result;
    }

    @Override
    public String getWorkingDirectory() {
        return mWorkingDirectory;
    }

    @Override
    public void setWorkingDirectory(String path) {
        Log.d(TAG, "setWorkingDirectory: " + path);
        mWorkingDirectory = path;
    }

    @Override
    public String resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return mWorkingDirectory;
        }
        
        File file = new File(path);
        if (file.isAbsolute()) {
            return path;
        }
        
        return new File(mWorkingDirectory, path).getAbsolutePath();
    }
}
