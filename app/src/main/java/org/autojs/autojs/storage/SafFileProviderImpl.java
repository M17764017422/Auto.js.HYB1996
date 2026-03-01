package org.autojs.autojs.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import com.stardust.pio.IFileProvider;

import java.io.ByteArrayOutputStream;
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

    private final Context mContext;
    private final Uri mTreeUri;
    private String mWorkingDirectory;
    private final String mRootPath;

    /**
     * @param context 上下文
     * @param treeUri SAF 授权的目录 URI
     * @param rootPath 对应的虚拟根路径，如 "/sdcard/脚本"
     */
    public SafFileProviderImpl(Context context, Uri treeUri, String rootPath) {
        mContext = context.getApplicationContext();
        mTreeUri = treeUri;
        mRootPath = rootPath;
        mWorkingDirectory = rootPath;
    }

    @Override
    public boolean exists(String path) {
        String documentId = findDocumentId(path);
        return documentId != null;
    }

    @Override
    public boolean isFile(String path) {
        String documentId = findDocumentId(path);
        if (documentId == null) return false;
        
        String mimeType = getMimeType(documentId);
        return mimeType != null && !mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
    }

    @Override
    public boolean isDirectory(String path) {
        String documentId = findDocumentId(path);
        if (documentId == null) return false;
        
        String mimeType = getMimeType(documentId);
        return mimeType != null && mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
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
        
        try {
            return DocumentsContract.deleteDocument(mContext.getContentResolver(), documentUri);
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
        // SAF 不直接支持重命名，需要复制+删除
        // 这里暂时返回 false，后续可以实现
        return false;
    }

    @Override
    public boolean move(String fromPath, String toPath) {
        // SAF 不直接支持移动
        return false;
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
        List<FileInfo> result = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return result;
        }
        
        Uri childrenUri = getChildrenUri(path);
        if (childrenUri == null) return result;
        
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };
        
        try (Cursor cursor = mContext.getContentResolver().query(
                childrenUri, projection, null, null, null)) {
            if (cursor == null) return result;
            
            while (cursor.moveToNext()) {
                String name = cursor.getString(1);
                String mimeType = cursor.getString(2);
                long size = cursor.getLong(3);
                long lastModified = cursor.getLong(4);
                
                boolean isDir = mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
                String childPath = path.endsWith("/") ? path + name : path + "/" + name;
                
                result.add(new FileInfo(name, childPath, isDir, size, lastModified));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return result;
    }

    @Override
    public String read(String path, String encoding) {
        byte[] data = readBytes(path);
        if (data == null) return null;
        try {
            return new String(data, encoding);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String read(String path) {
        return read(path, "UTF-8");
    }

    @Override
    public byte[] readBytes(String path) {
        try (InputStream is = openInputStream(path)) {
            if (is == null) return null;
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public InputStream openInputStream(String path) throws Exception {
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) {
            throw new Exception("File not found: " + path);
        }
        return mContext.getContentResolver().openInputStream(documentUri);
    }

    @Override
    public boolean write(String path, String content, String encoding) {
        try {
            byte[] data = content.getBytes(encoding);
            return writeBytes(path, data);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean write(String path, String content) {
        return write(path, content, "UTF-8");
    }

    @Override
    public boolean append(String path, String content, String encoding) {
        // SAF 不直接支持追加，需要读取原有内容后重新写入
        String existing = read(path, encoding);
        if (existing == null) existing = "";
        return write(path, existing + content, encoding);
    }

    @Override
    public boolean writeBytes(String path, byte[] bytes) {
        try (OutputStream os = openOutputStream(path)) {
            if (os == null) return false;
            os.write(bytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public OutputStream openOutputStream(String path) throws Exception {
        return openOutputStream(path, false);
    }

    @Override
    public OutputStream openOutputStream(String path, boolean append) throws Exception {
        Uri documentUri = getDocumentUri(path);
        
        if (documentUri == null) {
            // 文件不存在，创建新文件
            documentUri = createFile(path);
            if (documentUri == null) {
                throw new Exception("Cannot create file: " + path);
            }
        }
        
        if (append) {
            // SAF 不支持追加模式，需要读取现有内容
            throw new Exception("SAF does not support append mode directly");
        }
        
        return mContext.getContentResolver().openOutputStream(documentUri, "wt");
    }

    @Override
    public long length(String path) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return 0;
        
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) return 0;
        
        String[] projection = {DocumentsContract.Document.COLUMN_SIZE};
        try (Cursor cursor = mContext.getContentResolver().query(
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
        
        Uri documentUri = getDocumentUri(path);
        if (documentUri == null) return 0;
        
        String[] projection = {DocumentsContract.Document.COLUMN_LAST_MODIFIED};
        try (Cursor cursor = mContext.getContentResolver().query(
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
        if (path == null) return false;
        
        String resolvedPath = resolvePath(path);
        return resolvedPath.startsWith(mRootPath);
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
        if (relativePath.isEmpty()) {
            return DocumentsContract.getTreeDocumentId(mTreeUri);
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
            try (Cursor cursor = mContext.getContentResolver().query(
                    childrenUri, projection, null, null, null)) {
                if (cursor == null) return null;
                
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String name = cursor.getString(1);
                    
                    if (name.equals(part)) {
                        currentDocumentId = docId;
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) return null;
        }
        
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
        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(mTreeUri, documentId);
        String[] projection = {DocumentsContract.Document.COLUMN_MIME_TYPE};
        
        try (Cursor cursor = mContext.getContentResolver().query(
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
                    mContext.getContentResolver(),
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
                        mContext.getContentResolver(),
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
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, parentDocumentId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        
        try (Cursor cursor = mContext.getContentResolver().query(
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
