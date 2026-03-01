package org.autojs.autojs.storage;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * SAF (Storage Access Framework) 文件操作封装
 * 提供 SAF 方式下的文件读写操作
 */
public class SafFileProvider {

    private final Context mContext;
    private final Uri mTreeUri;

    public SafFileProvider(Context context, Uri treeUri) {
        mContext = context.getApplicationContext();
        mTreeUri = treeUri;
    }

    /**
     * 列出目录下的文件和子目录
     * @param relativePath 相对于授权目录的路径，如 "脚本" 或 "脚本/子目录"
     * @return 文件信息列表
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public List<FileInfo> listFiles(@Nullable String relativePath) {
        List<FileInfo> result = new ArrayList<>();
        
        Uri directoryUri = getDocumentUri(relativePath);
        if (directoryUri == null) {
            return result;
        }

        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                mTreeUri, DocumentsContract.getTreeDocumentId(mTreeUri));
        
        // 如果有相对路径，需要先找到对应的目录
        if (relativePath != null && !relativePath.isEmpty()) {
            childrenUri = findDirectoryChildrenUri(relativePath);
            if (childrenUri == null) {
                return result;
            }
        }

        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };

        try (Cursor cursor = mContext.getContentResolver().query(
                childrenUri, projection, null, null, null)) {
            if (cursor == null) {
                return result;
            }

            while (cursor.moveToNext()) {
                String documentId = cursor.getString(0);
                String name = cursor.getString(1);
                String mimeType = cursor.getString(2);
                long size = cursor.getLong(3);
                long lastModified = cursor.getLong(4);

                boolean isDirectory = mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR);
                
                result.add(new FileInfo(name, relativePath != null ? relativePath + "/" + name : name, 
                        isDirectory, size, lastModified));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * 查找目录的子文档 URI
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private Uri findDirectoryChildrenUri(String relativePath) {
        String[] parts = relativePath.split("/");
        String currentDocumentId = DocumentsContract.getTreeDocumentId(mTreeUri);
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, currentDocumentId);
            String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, 
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE};
            
            boolean found = false;
            try (Cursor cursor = mContext.getContentResolver().query(
                    childrenUri, projection, null, null, null)) {
                if (cursor == null) {
                    return null;
                }
                
                while (cursor.moveToNext()) {
                    String docId = cursor.getString(0);
                    String name = cursor.getString(1);
                    String mimeType = cursor.getString(2);
                    
                    if (name.equals(part) && mimeType.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                        currentDocumentId = docId;
                        found = true;
                        break;
                    }
                }
            }
            
            if (!found) {
                return null;
            }
        }
        
        return DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, currentDocumentId);
    }

    /**
     * 获取文档 URI
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private Uri getDocumentUri(@Nullable String relativePath) {
        if (relativePath == null || relativePath.isEmpty()) {
            return mTreeUri;
        }
        
        String documentId = findDocumentId(relativePath);
        if (documentId == null) {
            return null;
        }
        
        return DocumentsContract.buildDocumentUriUsingTree(mTreeUri, documentId);
    }

    /**
     * 查找文档 ID
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private String findDocumentId(String relativePath) {
        String[] parts = relativePath.split("/");
        String currentDocumentId = DocumentsContract.getTreeDocumentId(mTreeUri);
        
        for (String part : parts) {
            if (part.isEmpty()) continue;
            
            Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, currentDocumentId);
            String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, 
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME};
            
            boolean found = false;
            try (Cursor cursor = mContext.getContentResolver().query(
                    childrenUri, projection, null, null, null)) {
                if (cursor == null) {
                    return null;
                }
                
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
            
            if (!found) {
                return null;
            }
        }
        
        return currentDocumentId;
    }

    /**
     * 读取文件内容
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    public InputStream readFile(String relativePath) throws FileNotFoundException {
        Uri documentUri = getDocumentUri(relativePath);
        if (documentUri == null) {
            throw new FileNotFoundException("File not found: " + relativePath);
        }
        
        return mContext.getContentResolver().openInputStream(documentUri);
    }

    /**
     * 写入文件内容
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    public OutputStream writeFile(String relativePath) throws FileNotFoundException {
        Uri documentUri = getDocumentUri(relativePath);
        if (documentUri == null) {
            // 文件不存在，尝试创建
            documentUri = createFile(relativePath);
            if (documentUri == null) {
                throw new FileNotFoundException("Cannot create file: " + relativePath);
            }
        }
        
        return mContext.getContentResolver().openOutputStream(documentUri, "wt");
    }

    /**
     * 创建文件
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    public Uri createFile(String relativePath) {
        int lastSlash = relativePath.lastIndexOf('/');
        String parentPath = lastSlash > 0 ? relativePath.substring(0, lastSlash) : "";
        String fileName = lastSlash > 0 ? relativePath.substring(lastSlash + 1) : relativePath;
        
        String parentDocumentId;
        if (parentPath.isEmpty()) {
            parentDocumentId = DocumentsContract.getTreeDocumentId(mTreeUri);
        } else {
            parentDocumentId = findDocumentId(parentPath);
            if (parentDocumentId == null) {
                // 尝试创建父目录
                parentDocumentId = createDirectory(parentPath);
                if (parentDocumentId == null) {
                    return null;
                }
            }
        }
        
        try {
            return DocumentsContract.createDocument(
                    mContext.getContentResolver(),
                    DocumentsContract.buildDocumentUriUsingTree(mTreeUri, parentDocumentId),
                    "application/x-javascript",
                    fileName
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建目录
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    public String createDirectory(String relativePath) {
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
                if (newDirUri == null) {
                    return null;
                }
                currentDocumentId = DocumentsContract.getDocumentId(newDirUri);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        
        return currentDocumentId;
    }

    /**
     * 查找子文档 ID
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private String findChildDocumentId(String parentDocumentId, String name, boolean isDirectory) {
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(mTreeUri, parentDocumentId);
        String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID, 
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE};
        
        try (Cursor cursor = mContext.getContentResolver().query(
                childrenUri, projection, null, null, null)) {
            if (cursor == null) {
                return null;
            }
            
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

    /**
     * 删除文件或目录
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean delete(String relativePath) {
        Uri documentUri = getDocumentUri(relativePath);
        if (documentUri == null) {
            return false;
        }
        
        try {
            return DocumentsContract.deleteDocument(mContext.getContentResolver(), documentUri);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 检查文件是否存在
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean exists(String relativePath) {
        return findDocumentId(relativePath) != null;
    }

    /**
     * 文件信息类
     */
    public static class FileInfo {
        public final String name;
        public final String relativePath;
        public final boolean isDirectory;
        public final long size;
        public final long lastModified;

        public FileInfo(String name, String relativePath, boolean isDirectory, long size, long lastModified) {
            this.name = name;
            this.relativePath = relativePath;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
        }
    }
}
