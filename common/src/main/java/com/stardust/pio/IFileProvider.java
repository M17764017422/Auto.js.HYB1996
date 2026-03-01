package com.stardust.pio;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * 统一文件访问接口
 * 支持传统 File API 和 SAF (Storage Access Framework) 两种实现
 */
public interface IFileProvider {

    /**
     * 检查文件或目录是否存在
     */
    boolean exists(String path);

    /**
     * 检查是否为文件
     */
    boolean isFile(String path);

    /**
     * 检查是否为目录
     */
    boolean isDirectory(String path);

    /**
     * 创建目录
     */
    boolean mkdir(String path);

    /**
     * 创建目录（包含父目录）
     */
    boolean mkdirs(String path);

    /**
     * 删除文件
     */
    boolean delete(String path);

    /**
     * 递归删除目录
     */
    boolean deleteRecursively(String path);

    /**
     * 重命名文件或目录
     */
    boolean rename(String path, String newName);

    /**
     * 移动文件或目录
     */
    boolean move(String fromPath, String toPath);

    /**
     * 复制文件
     */
    boolean copy(String fromPath, String toPath);

    /**
     * 列出目录内容
     */
    List<FileInfo> listFiles(String path);

    /**
     * 读取文件内容为字符串
     */
    String read(String path, String encoding);

    /**
     * 读取文件内容为字符串（UTF-8）
     */
    String read(String path);

    /**
     * 读取文件为字节数组
     */
    byte[] readBytes(String path);

    /**
     * 获取文件输入流
     */
    InputStream openInputStream(String path) throws Exception;

    /**
     * 写入字符串到文件
     */
    boolean write(String path, String content, String encoding);

    /**
     * 写入字符串到文件（UTF-8）
     */
    boolean write(String path, String content);

    /**
     * 追加字符串到文件
     */
    boolean append(String path, String content, String encoding);

    /**
     * 写入字节数组到文件
     */
    boolean writeBytes(String path, byte[] bytes);

    /**
     * 获取文件输出流
     */
    OutputStream openOutputStream(String path) throws Exception;

    /**
     * 获取文件输出流（追加模式）
     */
    OutputStream openOutputStream(String path, boolean append) throws Exception;

    /**
     * 获取文件大小
     */
    long length(String path);

    /**
     * 获取最后修改时间
     */
    long lastModified(String path);

    /**
     * 获取文件名
     */
    String getName(String path);

    /**
     * 获取父目录路径
     */
    String getParent(String path);

    /**
     * 获取文件扩展名
     */
    String getExtension(String path);

    /**
     * 检查路径是否在授权范围内
     */
    boolean isAccessible(String path);

    /**
     * 获取当前工作目录
     */
    String getWorkingDirectory();

    /**
     * 设置当前工作目录
     */
    void setWorkingDirectory(String path);

    /**
     * 解析相对路径为绝对路径
     */
    String resolvePath(String path);

    /**
     * 文件信息类
     */
    class FileInfo {
        public final String name;
        public final String path;
        public final boolean isDirectory;
        public final long size;
        public final long lastModified;

        public FileInfo(String name, String path, boolean isDirectory, long size, long lastModified) {
            this.name = name;
            this.path = path;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
        }

        @Override
        public String toString() {
            return "FileInfo{" +
                    "name='" + name + '\'' +
                    ", path='" + path + '\'' +
                    ", isDirectory=" + isDirectory +
                    ", size=" + size +
                    ", lastModified=" + lastModified +
                    '}';
        }
    }
}
