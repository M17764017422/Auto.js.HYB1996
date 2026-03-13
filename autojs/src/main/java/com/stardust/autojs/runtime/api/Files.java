package com.stardust.autojs.runtime.api;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Log;

import com.stardust.autojs.runtime.ScriptRuntime;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.PFileInterface;
import com.stardust.pio.PFiles;
import com.stardust.pio.SafFileProviderImpl;
import com.stardust.pio.UncheckedIOException;
import com.stardust.pio.observe.PathObservable;
import com.stardust.pio.observe.SafPathObservable;
import com.stardust.util.Func1;

import com.stardust.pio.FileProviderFactory;

import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.typedarrays.NativeTypedArrayView;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件操作 API
 * 提供文件读写、复制、移动、监听等功能
 * 
 * 新增功能：
 * - copyWithProgress: 带进度回调的复制操作
 * - observe: 文件/目录变化监听
 * - getThumbnail: 获取图片缩略图
 */
public class Files {

    private static final String TAG = "AutoJS.Files";
    
    // 文件监听器缓存
    private static final Map<String, PathObservable> activeObservers = new HashMap<>();

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
        if (result == null) {
            throw new RuntimeException("File not found: " + resolvedPath);
        }
        Log.d(TAG + ".read", "result: length=" + result.length());
        return result;
    }


    public String read(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".read", "path=" + resolvedPath);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        String result = provider.read(resolvedPath);
        if (result == null) {
            throw new RuntimeException("File not found: " + resolvedPath);
        }
        Log.d(TAG + ".read", "result: length=" + result.length());
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

    public boolean writeBytes(String path, Object data) {
        byte[] bytes;
        if (data instanceof byte[]) {
            bytes = (byte[]) data;
        } else if (data instanceof NativeArray) {
            // JavaScript 数组
            NativeArray arr = (NativeArray) data;
            int len = (int) arr.getLength();
            bytes = new byte[len];
            for (int i = 0; i < len; i++) {
                Object val = arr.get(i, arr);
                if (val instanceof Number) {
                    bytes[i] = ((Number) val).byteValue();
                } else {
                    bytes[i] = 0;
                }
            }
            Log.d(TAG + ".writeBytes", "Converted NativeArray to byte[], length=" + len);
        } else if (data instanceof NativeTypedArrayView) {
            // ES6 类型化数组 (Uint8Array, Int8Array 等)
            NativeTypedArrayView<?> arr = (NativeTypedArrayView<?>) data;
            int len = arr.getArrayLength();
            bytes = new byte[len];
            for (int i = 0; i < len; i++) {
                Object val = arr.getArrayElement(i);
                if (val instanceof Number) {
                    bytes[i] = ((Number) val).byteValue();
                } else {
                    bytes[i] = 0;
                }
            }
            Log.d(TAG + ".writeBytes", "Converted NativeTypedArrayView to byte[], length=" + len);
        } else if (data != null && data.getClass().isArray()) {
            // 其他数组类型
            int len = java.lang.reflect.Array.getLength(data);
            bytes = new byte[len];
            for (int i = 0; i < len; i++) {
                Object val = java.lang.reflect.Array.get(data, i);
                if (val instanceof Number) {
                    bytes[i] = ((Number) val).byteValue();
                } else {
                    bytes[i] = 0;
                }
            }
            Log.d(TAG + ".writeBytes", "Converted Java array to byte[], length=" + len);
        } else {
            Log.w(TAG + ".writeBytes", "Unsupported data type: " + (data != null ? data.getClass() : "null"));
            return false;
        }
        // 直接调用 provider，避免递归调用自身
        String resolvedPath = path(path);
        Log.d(TAG + ".writeBytes", "path=" + resolvedPath + ", size=" + bytes.length);
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

    /**
     * 带进度回调的复制操作
     * 
     * @param pathFrom 源文件路径
     * @param pathTo 目标文件路径
     * @param options 选项对象，包含：
     *                - onProgress: function(bytesCopied) {} 进度回调
     *                - interval: 进度回调间隔（毫秒，默认500）
     * @return 是否成功
     * 
     * 示例：
     * files.copyWithProgress("/sdcard/a.zip", "/sdcard/b.zip", {
     *     onProgress: function(bytes) {
     *         console.log("已复制: " + bytes + " 字节");
     *     },
     *     interval: 200
     * });
     */
    public boolean copyWithProgress(String pathFrom, String pathTo, NativeObject options) {
        String from = path(pathFrom);
        String to = path(pathTo);
        
        long interval = 500;
        Object progressCallback = null;
        
        if (options != null) {
            Object intervalObj = options.get("interval", options);
            if (intervalObj instanceof Number) {
                interval = ((Number) intervalObj).longValue();
            }
            progressCallback = options.get("onProgress", options);
        }
        
        final long finalInterval = interval;
        final Object callback = progressCallback;
        
        Log.d(TAG + ".copyWithProgress", "from=" + from + ", to=" + to + ", interval=" + finalInterval);
        
        IFileProvider provider = FileProviderFactory.getProvider(from);
        
        if (provider instanceof SafFileProviderImpl) {
            SafFileProviderImpl safProvider = (SafFileProviderImpl) provider;
            return safProvider.copyWithProgress(
                from, 
                to, 
                finalInterval,
                callback != null ? bytesCopied -> {
                    try {
                        if (callback instanceof org.mozilla.javascript.Function) {
                            org.mozilla.javascript.Function fn = (org.mozilla.javascript.Function) callback;
                            fn.call(
                                org.mozilla.javascript.Context.getCurrentContext(),
                                fn.getParentScope(),
                                fn.getParentScope(),
                                new Object[]{bytesCopied}
                            );
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Progress callback error", e);
                    }
                    return kotlin.Unit.INSTANCE;
                } : null,
                null
            );
        } else {
            // 非 SAF 提供者，使用普通复制
            return provider.copy(from, to);
        }
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

    public String getParent(String path) {
        String resolvedPath = path(path);
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        return provider.getParent(resolvedPath);
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

    public boolean isDirectory(String path) {
        return isDir(path);
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

    // ==================== 新增 API ====================

    /**
     * 监听文件或目录变化
     * 
     * @param path 监听的路径
     * @param callback 变化回调函数
     * @return 监听器对象，可调用 close() 停止监听
     * 
     * 示例：
     * var observer = files.observe("/sdcard/test/", function() {
     *     console.log("文件变化了！");
     * });
     * // 停止监听
     * observer.close();
     */
    public ObserverWrapper observe(String path, final org.mozilla.javascript.Function callback) {
        String resolvedPath = path(path);
        Log.d(TAG + ".observe", "path=" + resolvedPath);
        
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        
        if (provider instanceof SafFileProviderImpl && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SafFileProviderImpl safProvider = (SafFileProviderImpl) provider;
            
            // 获取 treeUri 和 documentId
            try {
                // 使用反射获取 SafFileProviderImpl 的内部信息
                java.lang.reflect.Field treeUriField = SafFileProviderImpl.class.getDeclaredField("treeUri");
                treeUriField.setAccessible(true);
                android.net.Uri treeUri = (android.net.Uri) treeUriField.get(safProvider);
                
                // 获取 documentId
                java.lang.reflect.Method findDocumentIdMethod = SafFileProviderImpl.class.getDeclaredMethod("findDocumentId", String.class);
                findDocumentIdMethod.setAccessible(true);
                String documentId = (String) findDocumentIdMethod.invoke(safProvider, resolvedPath);
                
                if (documentId == null) {
                    Log.e(TAG + ".observe", "Cannot find documentId for path: " + resolvedPath);
                    return null;
                }
                
                SafPathObservable observable = new SafPathObservable(
                    resolvedPath,
                    treeUri,
                    documentId,
                    mRuntime.getUiHandler().getContext(),
                    1000
                );
                
                observable.addObserver(() -> {
                    try {
                        callback.call(
                            org.mozilla.javascript.Context.getCurrentContext(),
                            callback.getParentScope(),
                            callback.getParentScope(),
                            new Object[]{}
                        );
                    } catch (Exception e) {
                        Log.e(TAG, "Observer callback error", e);
                    }
                });
                
                // 缓存监听器
                synchronized (activeObservers) {
                    activeObservers.put(resolvedPath, observable);
                }
                
                return new ObserverWrapper(observable, resolvedPath);
                
            } catch (Exception e) {
                Log.e(TAG + ".observe", "Failed to create observer", e);
                return null;
            }
        } else {
            Log.w(TAG + ".observe", "Observer only supported for SAF paths on API 21+");
            return null;
        }
    }

    /**
     * 停止监听指定路径
     * 
     * @param path 要停止监听的路径
     * @return 是否成功停止
     */
    public boolean stopObserve(String path) {
        String resolvedPath = path(path);
        synchronized (activeObservers) {
            PathObservable observable = activeObservers.remove(resolvedPath);
            if (observable != null) {
                observable.close();
                Log.d(TAG + ".stopObserve", "Stopped observing: " + resolvedPath);
                return true;
            }
        }
        return false;
    }

    /**
     * 获取图片文件缩略图
     * 
     * @param path 图片文件路径
     * @param width 缩略图宽度
     * @param height 缩略图高度
     * @return Bitmap 对象，如果不支持则返回 null
     * 
     * 示例：
     * var thumb = files.getThumbnail("/sdcard/test.jpg", 200, 200);
     * if (thumb) {
     *     // 处理缩略图
     * }
     */
    public Bitmap getThumbnail(String path, int width, int height) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Log.w(TAG + ".getThumbnail", "Thumbnail requires API 21+");
            return null;
        }
        
        String resolvedPath = path(path);
        Log.d(TAG + ".getThumbnail", "path=" + resolvedPath + ", width=" + width + ", height=" + height);
        
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        
        if (provider instanceof SafFileProviderImpl) {
            SafFileProviderImpl safProvider = (SafFileProviderImpl) provider;
            return safProvider.getThumbnail(resolvedPath, width, height, null);
        } else {
            Log.w(TAG + ".getThumbnail", "Thumbnail only supported for SAF paths");
            return null;
        }
    }

    /**
     * 获取文件的 MIME 类型
     * 
     * @param path 文件路径（支持传统路径和 SAF 路径）
     * @return MIME 类型字符串，如 "image/jpeg"、"text/plain" 等，
     *         目录返回 "application/vnd.android.document/directory"
     * 
     * 示例：
     * var mime = files.getMimeType("/sdcard/test.jpg");  // "image/jpeg"
     * var dirMime = files.getMimeType("/sdcard/DCIM");   // "application/vnd.android.document/directory"
     */
    public String getMimeType(String path) {
        String resolvedPath = path(path);
        Log.d(TAG + ".getMimeType", "path=" + resolvedPath);
        
        IFileProvider provider = FileProviderFactory.getProvider(resolvedPath);
        String mimeType = provider.getMimeType(resolvedPath);
        
        Log.d(TAG + ".getMimeType", "mimeType=" + mimeType);
        return mimeType;
    }

    /**
     * 批量复制文件
     * 
     * @param operations 操作数组，每个元素是 [fromPath, toPath]
     * @param stopOnError 遇到错误是否停止（默认 false）
     * @return 批量操作结果对象 {successCount, failureCount, results: [{target, success, error}]}
     * 
     * 示例：
     * var result = files.copyBatch([
     *     ["/sdcard/a.txt", "/sdcard/backup/a.txt"],
     *     ["/sdcard/b.txt", "/sdcard/backup/b.txt"]
     * ]);
     * console.log("成功: " + result.successCount + ", 失败: " + result.failureCount);
     */
    public NativeObject copyBatch(NativeArray operations, boolean stopOnError) {
        Log.d(TAG + ".copyBatch", "operations=" + operations.size() + ", stopOnError=" + stopOnError);
        
        java.util.List<kotlin.Pair<String, String>> opList = new java.util.ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            Object item = operations.get(i);
            if (item instanceof NativeArray) {
                NativeArray pair = (NativeArray) item;
                if (pair.size() >= 2) {
                    String from = String.valueOf(pair.get(0));
                    String to = String.valueOf(pair.get(1));
                    opList.add(new kotlin.Pair<>(path(from), path(to)));
                }
            }
        }
        
        IFileProvider provider = FileProviderFactory.getProvider();
        IFileProvider.BatchResult result = provider.copyBatch(opList, stopOnError);
        return convertBatchResultToNativeObject(result);
    }

    public NativeObject copyBatch(NativeArray operations) {
        return copyBatch(operations, false);
    }

    /**
     * 批量移动文件
     * 
     * @param operations 操作数组，每个元素是 [fromPath, toPath]
     * @param stopOnError 遇到错误是否停止（默认 false）
     * @return 批量操作结果对象 {successCount, failureCount, results: [{target, success, error}]}
     * 
     * 示例：
     * var result = files.moveBatch([
     *     ["/sdcard/a.txt", "/sdcard/archive/a.txt"],
     *     ["/sdcard/b.txt", "/sdcard/archive/b.txt"]
     * ]);
     */
    public NativeObject moveBatch(NativeArray operations, boolean stopOnError) {
        Log.d(TAG + ".moveBatch", "operations=" + operations.size() + ", stopOnError=" + stopOnError);
        
        java.util.List<kotlin.Pair<String, String>> opList = new java.util.ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            Object item = operations.get(i);
            if (item instanceof NativeArray) {
                NativeArray pair = (NativeArray) item;
                if (pair.size() >= 2) {
                    String from = String.valueOf(pair.get(0));
                    String to = String.valueOf(pair.get(1));
                    opList.add(new kotlin.Pair<>(path(from), path(to)));
                }
            }
        }
        
        IFileProvider provider = FileProviderFactory.getProvider();
        IFileProvider.BatchResult result = provider.moveBatch(opList, stopOnError);
        return convertBatchResultToNativeObject(result);
    }

    public NativeObject moveBatch(NativeArray operations) {
        return moveBatch(operations, false);
    }

    /**
     * 批量删除文件
     * 
     * @param paths 要删除的文件路径数组
     * @param stopOnError 遇到错误是否停止（默认 false）
     * @return 批量操作结果对象 {successCount, failureCount, results: [{target, success, error}]}
     * 
     * 示例：
     * var result = files.deleteBatch(["/sdcard/a.txt", "/sdcard/b.txt"]);
     * console.log("删除成功: " + result.successCount);
     */
    public NativeObject deleteBatch(NativeArray paths, boolean stopOnError) {
        Log.d(TAG + ".deleteBatch", "paths=" + paths.size() + ", stopOnError=" + stopOnError);
        
        java.util.List<String> pathList = new java.util.ArrayList<>();
        for (int i = 0; i < paths.size(); i++) {
            pathList.add(path(String.valueOf(paths.get(i))));
        }
        
        IFileProvider provider = FileProviderFactory.getProvider();
        IFileProvider.BatchResult result = provider.deleteBatch(pathList, stopOnError);
        return convertBatchResultToNativeObject(result);
    }

    public NativeObject deleteBatch(NativeArray paths) {
        return deleteBatch(paths, false);
    }

    /**
     * 将 BatchResult 转换为 JavaScript 对象
     */
    private NativeObject convertBatchResultToNativeObject(IFileProvider.BatchResult result) {
        NativeObject obj = new NativeObject();
        obj.put("operationType", obj, result.getOperationType());
        obj.put("successCount", obj, result.getSuccessCount());
        obj.put("failureCount", obj, result.getFailureCount());
        obj.put("totalCount", obj, result.getTotalCount());
        obj.put("isAllSuccess", obj, result.isAllSuccess());
        
        NativeArray results = new NativeArray(result.getResults().size());
        for (int i = 0; i < result.getResults().size(); i++) {
            IFileProvider.BatchResult.OperationResult opResult = result.getResults().get(i);
            NativeObject item = new NativeObject();
            item.put("target", item, opResult.getTarget());
            item.put("success", item, opResult.getSuccess());
            item.put("error", item, opResult.getError());
            results.put(i, results, item);
        }
        obj.put("results", obj, results);
        
        return obj;
    }

    /**
     * 清除 SAF 缓存
     * 当文件系统状态发生变化时（如 SAF 授权变化）调用
     */
    public void clearSafCache() {
        SafFileProviderImpl.clearAllCaches();
        Log.d(TAG + ".clearSafCache", "SAF caches cleared");
    }

    /**
     * 监听器包装类，暴露给 JavaScript 使用
     */
    public static class ObserverWrapper {
        private final PathObservable observable;
        private final String path;
        private boolean closed = false;

        public ObserverWrapper(PathObservable observable, String path) {
            this.observable = observable;
            this.path = path;
        }

        /**
         * 关闭监听器
         */
        public void close() {
            if (closed) return;
            closed = true;
            observable.close();
            synchronized (activeObservers) {
                activeObservers.remove(path);
            }
        }

        /**
         * 是否正在监听
         */
        public boolean isObserving() {
            return !closed && observable.isObserving();
        }

        /**
         * 获取监听路径
         */
        public String getPath() {
            return path;
        }
    }
}