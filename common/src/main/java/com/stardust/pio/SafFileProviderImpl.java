package com.stardust.pio;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.text.TextUtils;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.RequiresApi;

/**
 * SAF (Storage Access Framework) 文件访问实现
 * 适用于用户授权特定目录的场景
 */
public class SafFileProviderImpl implements IFileProvider {

    private static final String TAG = "SafFileProvider";

    private final Uri mTreeUri;
    private String mWorkingDirectory;
    private final String mRootPath;
    private final Context mContext;

    /**
     * @param treeUri SAF 授权的目录 URI
     * @param rootPath 对应的虚拟根路径，如 "/sdcard/脚本"
     */
    public SafFileProviderImpl(Uri treeUri, String rootPath) {
        this(treeUri, rootPath, null);
    }

    /**
     * @param treeUri SAF 授权的目录 URI
     * @param rootPath 对应的虚拟根路径，如 "/sdcard/脚本"
     * @param context 上下文（用于访问 ContentResolver）
     */
    public SafFileProviderImpl(Uri treeUri, String rootPath, Context context) {
        mTreeUri = treeUri;
        mRootPath = rootPath;
        mWorkingDirectory = rootPath;
        mContext = context;
        Log.i(TAG, "Created: treeUri=" + treeUri + ", rootPath=" + rootPath);
    }

    private Context getContext() {
        if (mContext != null) {
            return mContext;
        }
        try {
            return (Context) Class.forName("com.stardust.app.GlobalAppContext")
                    .getMethod("get")
                    .invoke(null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get GlobalAppContext", e);
            return null;
        }
    }

    @Override
    public boolean exists(String path) {
        String documentId = findDocumentId(path);
        boolean result = documentId != null;
        Log.d(TAG, "exists: path=" + path + ", result=" + result);
        return result;
    }

    @Override
    public boolean isFile(String path) {
        String documentId = findDocumentId(path);
        if (documentId == null) {
            Log.d(TAG, "isFile: path=" + path + ", result=false (not found)");
            return false;
        }
        
        String mimeType = getMimeType(documentId);
        boolean result = mimeType != null && !mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
        Log.d(TAG, "isFile: path=" + path + ", mimeType=" + mimeType + ", result=" + result);
        return result;
    }

    @Override
    public boolean isDirectory(String path) {
        String documentId = findDocumentId(path);
        if (documentId == null) {
            Log.d(TAG, "isDirectory: path=" + path + ", result=false (not found)");
            return false;
        }
        
        String mimeType = getMimeType(documentId);
        boolean result = mimeType != null && mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
        Log.d(TAG, "isDirectory: path=" + path + ", mimeType=" + mimeType + ", result=" + result);
        return result;
    }

    @Override
    public boolean mkdir(String path) {
        return createDirectory(path) != null;
    }

    @Override
    public boolean mkdirs(String path) {
        return createDirectory(path) != null;
    }

    @Override
    public boolean delete(String path) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }
        
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) return false;
        
        Context context = getContext();
        if (context == null) return false;
        
        try {
            return DocumentsContract.deleteDocument(context.getContentResolver(), documentUri);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteRecursively(String path) {
        // SAF 不直接支持递归删除，需要手动实现
        return deleteRecursive(path);
    }

    private boolean deleteRecursive(String path) {
        if (!isDirectory(path)) {
            return delete(path);
        }
        
        List<FileInfo> children = listFiles(path);
        for (FileInfo child : children) {
            if (child.isDirectory) {
                deleteRecursive(child.path);
            } else {
                delete(child.path);
            }
        }
        return delete(path);
    }

    @Override
    public boolean rename(String path, String newName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "rename: API level < 21");
            return false;
        }
        
        Context context = getContext();
        if (context == null) {
            Log.e(TAG, "rename: context is null");
            return false;
        }
        
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) {
            Log.w(TAG, "rename: documentUri is null for path=" + path);
            return false;
        }
        
        try {
            // Android N (API 24) 及以上使用 DocumentsContract.renameDocument
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri newUri = DocumentsContract.renameDocument(
                    context.getContentResolver(), documentUri, newName);
                boolean success = newUri != null;
                Log.d(TAG, "rename: path=" + path + ", newName=" + newName + ", success=" + success);
                return success;
            } else {
                // 低版本使用复制+删除方式
                String parentPath = getParent(path);
                String newPath = parentPath + "/" + newName;
                boolean success = copy(path, newPath);
                if (success) {
                    success = delete(path);
                }
                Log.d(TAG, "rename (fallback): path=" + path + ", newName=" + newName + ", success=" + success);
                return success;
            }
        } catch (Exception e) {
            Log.e(TAG, "rename failed: " + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean move(String fromPath, String toPath) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "move: API level < 21");
            return false;
        }
        
        // 确保目标目录存在
        String parentPath = getParent(toPath);
        if (!TextUtils.isEmpty(parentPath) && !exists(parentPath)) {
            mkdirs(parentPath);
        }
        
        // 使用复制+删除方式实现移动
        boolean success = copy(fromPath, toPath);
        if (success) {
            success = delete(fromPath);
        }
        Log.d(TAG, "move: from=" + fromPath + ", to=" + toPath + ", success=" + success);
        return success;
    }

    @Override
    public boolean copy(String fromPath, String toPath) {
        try {
            Uri fromUri = getDocumentUri(fromPath);
            if (fromUri == null) return false;
            
            // 读取源文件
            byte[] data = readBytes(fromPath);
            if (data == null) return false;
            
            // 创建目标文件并写入
            return writeBytes(toPath, data);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public List<FileInfo> listFiles(String path) {
        Log.d(TAG, "listFiles: path=" + path);
        List<FileInfo> result = new ArrayList<>();
        
        Context context = getContext();
        if (context == null) {
            Log.w(TAG, "listFiles: context is null");
            return result;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG, "listFiles: API level < 21, returning empty list");
            return result;
        }
        
        Uri childrenUri = getChildrenUri(path);
        if (childrenUri == null) {
            Log.e(TAG, "listFiles: getChildrenUri returned null for path=" + path);
            return result;
        }
        
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };
        
        try (Cursor cursor = context.getContentResolver().query(
                childrenUri, projection, null, null, null)) {
            if (cursor == null) {
                Log.e(TAG, "listFiles: query returned null cursor");
                return result;
            }
            
            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                String mimeType = cursor.getString(2);
                long size = cursor.getLong(3);
                long lastModified = cursor.getLong(4);
                
                boolean isDir = mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
                String childPath = path.endsWith("/") ? path + name : path + "/" + name;
                
                result.add(new FileInfo(name, childPath, isDir, size, lastModified));
                Log.v(TAG, "listFiles: found " + (isDir ? "dir" : "file") + ": " + name);
            }
            Log.d(TAG, "listFiles: found " + result.size() + " items");
        } catch (Exception e) {
            Log.e(TAG, "listFiles: error=" + e.getMessage(), e);
        }
        
        return result;
    }

    @Override
    public String read(String path, String encoding) {
        Log.d(TAG, "read: path=" + path + ", encoding=" + encoding);
        byte[] data = readBytes(path);
        if (data == null) {
            Log.e(TAG, "read: failed to read bytes from " + path);
            return null;
        }
        try {
            String result = new String(data, encoding);
            Log.d(TAG, "read: success, length=" + result.length() + " chars");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "read: encoding error=" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String read(String path) {
        return read(path, "UTF-8");
    }

    @Override
    public byte[] readBytes(String path) {
        Log.d(TAG, "readBytes: path=" + path);
        try (InputStream is = openInputStream(path)) {
            if (is == null) {
                Log.e(TAG, "readBytes: openInputStream returned null");
                return null;
            }
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            byte[] result = bos.toByteArray();
            Log.d(TAG, "readBytes: success, size=" + result.length + " bytes");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "readBytes: error=" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public InputStream openInputStream(String path) throws Exception {
        Log.d(TAG, "openInputStream: path=" + path);
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) {
            Log.e(TAG, "openInputStream: documentUri is null for path=" + path);
            throw new Exception("File not found: " + path);
        }
        
        Context context = getContext();
        if (context == null) {
            throw new Exception("Context is null");
        }
        
        Log.d(TAG, "openInputStream: documentUri=" + documentUri);
        return context.getContentResolver().openInputStream(documentUri);
    }

    @Override
    public boolean write(String path, String content, String encoding) {
        Log.d(TAG, "write: path=" + path + ", contentLength=" + content.length() + ", encoding=" + encoding);
        try {
            byte[] data = content.getBytes(encoding);
            return writeBytes(path, data);
        } catch (Exception e) {
            Log.e(TAG, "write: encoding error=" + e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean write(String path, String content) {
        return write(path, content, "UTF-8");
    }

    @Override
    public boolean append(String path, String content, String encoding) {
        Log.d(TAG, "append: path=" + path);
        // SAF 不直接支持追加，需要读取原有内容后重新写入
        String existing = read(path, encoding);
        if (existing == null) existing = "";
        return write(path, existing + content, encoding);
    }

    @Override
    public boolean append(String path, String content) {
        return append(path, content, "UTF-8");
    }

    @Override
    public boolean writeBytes(String path, byte[] bytes) {
        Log.d(TAG, "writeBytes: path=" + path + ", size=" + bytes.length + " bytes");
        try (OutputStream os = openOutputStream(path)) {
            if (os == null) {
                Log.e(TAG, "writeBytes: openOutputStream returned null");
                return false;
            }
            os.write(bytes);
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
        Log.d(TAG, "openOutputStream: path=" + path + ", append=" + append);
        Uri documentUri = getDocumentUri(path);
        
        Context context = getContext();
        if (context == null) {
            throw new Exception("Context is null");
        }
        
        if (documentUri == null) {
            // 文件不存在，创建新文件
            Log.d(TAG, "openOutputStream: file not found, creating new file");
            documentUri = createFile(path);
            if (documentUri == null) {
                Log.e(TAG, "openOutputStream: failed to create file: " + path);
                throw new Exception("Cannot create file: " + path);
            }
            Log.d(TAG, "openOutputStream: created new file, uri=" + documentUri);
        }
        
        if (append) {
            // SAF 不直接支持追加模式，使用 AppendOutputStream 实现
            Log.d(TAG, "openOutputStream: using AppendOutputStream for append mode");
            return new AppendOutputStream(context, documentUri, path);
        }
        
        return context.getContentResolver().openOutputStream(documentUri, "wt");
    }

    /**
     * 支持 SAF 追加模式的 OutputStream
     * 在 close() 时读取现有内容，合并新内容后写入
     */
    private class AppendOutputStream extends ByteArrayOutputStream {
        private final Context mContext;
        private final Uri mDocumentUri;
        private final String mPath;
        
        public AppendOutputStream(Context context, Uri documentUri, String path) {
            mContext = context;
            mDocumentUri = documentUri;
            mPath = path;
        }
        
        @Override
        public void close() throws IOException {
            super.close();
            
            // 读取现有内容
            byte[] existingData = new byte[0];
            try (InputStream is = mContext.getContentResolver().openInputStream(mDocumentUri)) {
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, len);
                    }
                    existingData = baos.toByteArray();
                }
            } catch (Exception e) {
                Log.w(TAG, "AppendOutputStream: failed to read existing content: " + e.getMessage());
            }
            
            // 合并内容并写入
            try (OutputStream os = mContext.getContentResolver().openOutputStream(mDocumentUri, "wt")) {
                if (os != null) {
                    os.write(existingData);
                    os.write(toByteArray());
                    Log.d(TAG, "AppendOutputStream: wrote " + (existingData.length + size()) + " bytes to " + mPath);
                }
            } catch (Exception e) {
                Log.e(TAG, "AppendOutputStream: failed to write: " + e.getMessage());
                throw new IOException("Failed to write appended content", e);
            }
        }
    }

    @Override
    public long length(String path) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
        
        Context context = getContext();
        if (context == null) return 0;
        
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) return 0;
        
        String[] projection = {DocumentsContract.Document.COLUMN_SIZE};
        try (Cursor cursor = context.getContentResolver().query(
                documentUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public long lastModified(String path) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
        
        Context context = getContext();
        if (context == null) return 0;
        
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) return 0;
        
        String[] projection = {DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        try (Cursor cursor = context.getContentResolver().query(
                documentUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public String getName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    @Override
    public String getParent(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash > 0 ? path.substring(0, lastSlash) : mRootPath;
    }

    @Override
    public String getExtension(String path) {
        String name = getName(path);
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(lastDot + 1) : "";
    }

    @Override
    public boolean isAccessible(String path) {
        // 检查路径是否在授权目录范围内
        if (path == null) {
            Log.d(TAG, "isAccessible: path is null, returning false");
            return false;
        }
        
        String resolvedPath = resolvePath(path);
        boolean result = resolvedPath.startsWith(mRootPath);
        Log.d(TAG, "isAccessible: path=" + path + ", resolved=" + resolvedPath 
                + ", root=" + mRootPath + ", result=" + result);
        return result;
    }

    @Override
    public String getWorkingDirectory() {
        return mWorkingDirectory;
    }

    @Override
    public void setWorkingDirectory(String path) {
        mWorkingDirectory = path;
    }

    @Override
    public String resolvePath(String path) {
        if (path == null || path.isEmpty()) {
            return mWorkingDirectory;
        }
        
        if (path.startsWith("/")) {
            return path;
        }
        
        return mWorkingDirectory.endsWith("/") 
                ? mWorkingDirectory + path 
                : mWorkingDirectory + "/" + path;
    }

    // ==================== SAF 辅助方法 ====================

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private String findDocumentId(String path) {
        String relativePath = getRelativePath(path);
        Log.v(TAG, "findDocumentId: path=" + path + ", relativePath=" + relativePath);
        
        Context context = getContext();
        if (context == null) return null;
        
        if (relativePath.isEmpty()) {
            String rootId = DocumentsContract.getTreeDocumentId(mTreeUri);
            Log.v(TAG, "findDocumentId: root path, returning rootId=" + rootId);
            return rootId;
        }
        
        String[] parts = relativePath.split("/");
        String currentDocumentId = DocumentsContract.getTreeDocumentId(mTreeUri);
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                    mTreeUri, currentDocumentId);
            String[] projection = {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
            };
            
            boolean found = false;
            try (Cursor cursor = context.getContentResolver().query(
                    childrenUri, projection, null, null, null)) {
                if (cursor == null) {
                    Log.e(TAG, "findDocumentId: query returned null cursor for part=" + part);
                    return null;
                }
                
                int count = 0;
                while (cursor.moveToNext()) {
                    count++;
                    String docId = cursor.getString(0);
                    String name = cursor.getString(1);
                    Log.v(TAG, "findDocumentId: checking child[" + count + "] name=" + name + ", docId=" + docId);
                    
                    if (name.equals(part)) {
                        currentDocumentId = docId;
                        found = true;
                        Log.v(TAG, "findDocumentId: found part=" + part + ", docId=" + docId);
                        break;
                    }
                }
                Log.d(TAG, "findDocumentId: queried " + count + " children for part=" + part + ", found=" + found);
            } catch (Exception e) {
                Log.e(TAG, "findDocumentId: query exception for part=" + part + ", error=" + e.getMessage(), e);
                return null;
            }
            
            if (!found) {
                Log.w(TAG, "findDocumentId: part not found: " + part + " in path=" + path);
                return null;
            }
        }
        
        Log.v(TAG, "findDocumentId: result=" + currentDocumentId);
        return currentDocumentId;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private Uri getDocumentUri(String path) {
        String documentId = findDocumentId(path);
        if (documentId == null) return null;
        return DocumentsContract.buildDocumentUriUsingTree(mTreeUri, documentId);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private Uri getChildrenUri(String path) {
        String documentId = findDocumentId(path);
        if (documentId == null) return null;
        return DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, documentId);
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private String getMimeType(String documentId) {
        Context context = getContext();
        if (context == null) return null;
        
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(mTreeUri, documentId);
        String[] projection = {DocumentsContract.Document.COLUMN_MIME_TYPE};
        
        try (Cursor cursor = context.getContentResolver().query(
                documentUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private Uri createFile(String path) {
        Context context = getContext();
        if (context == null) return null;
        
        String parentPath = getParent(path);
        String fileName = getName(path);
        
        String parentDocumentId = findDocumentId(parentPath);
        if (parentDocumentId == null) {
            // 尝试创建父目录
            parentDocumentId = createDirectory(parentPath);
            if (parentDocumentId == null) return null;
        }
        
        Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(mTreeUri, parentDocumentId);
        
        try {
            return DocumentsContract.createDocument(
                    context.getContentResolver(),
                    parentUri,
                    "application/x-javascript",
                    fileName
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private String createDirectory(String path) {
        Context context = getContext();
        if (context == null) return null;
        
        String relativePath = getRelativePath(path);
        if (relativePath.isEmpty()) {
            return DocumentsContract.getTreeDocumentId(mTreeUri);
        }
        
        String[] parts = relativePath.split("/");
        String currentDocumentId = DocumentsContract.getTreeDocumentId(mTreeUri);
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            // 先检查是否已存在
            String existingId = findChildDocumentId(currentDocumentId, part, true);
            if (existingId != null) {
                currentDocumentId = existingId;
                continue;
            }
            
            // 创建目录
            Uri parentUri = DocumentsContract.buildDocumentUriUsingTree(mTreeUri, currentDocumentId);
            try {
                Uri newDirUri = DocumentsContract.createDocument(
                        context.getContentResolver(),
                        parentUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        part
                );
                if (newDirUri == null) return null;
                currentDocumentId = DocumentsContract.getDocumentId(newDirUri);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        return currentDocumentId;
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private String findChildDocumentId(String parentDocumentId, String name, boolean isDirectory) {
        Context context = getContext();
        if (context == null) return null;
        
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, parentDocumentId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        
        try (Cursor cursor = context.getContentResolver().query(
                childrenUri, projection, null, null, null)) {
            if (cursor == null) return null;
            
            while (cursor.moveToNext()) {
                String docId = cursor.getString(0);
                String docName = cursor.getString(1);
                String mimeType = cursor.getString(2);
                
                if (docName.equals(name)) {
                    boolean isDir = mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
                    if (isDirectory == isDir) {
                        return docId;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private String getRelativePath(String path) {
        String resolvedPath = resolvePath(path);
        if (resolvedPath.startsWith(mRootPath)) {
            String relative = resolvedPath.substring(mRootPath.length());
            return relative.startsWith("/") ? relative.substring(1) : relative;
        }
        return resolvedPath;
    }
}