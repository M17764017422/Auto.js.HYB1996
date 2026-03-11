package com.stardust.io;

import com.stardust.pio.PFile;
import com.stardust.pio.PFiles;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.FileProviderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.stardust.pio.PFiles.closeSilently;

public class Zip {

    public static void unzip(InputStream stream, File dir) throws IOException {
        unzip(stream, dir.getAbsolutePath());
    }
    
    /**
     * 解压 ZIP 文件到指定目录（支持 SAF 模式）
     * @param stream ZIP 文件输入流
     * @param destDir 目标目录路径
     * @throws IOException IO 异常
     */
    public static void unzip(InputStream stream, String destDir) throws IOException {
        OutputStream fos = null;
        ZipInputStream zis = null;
        IFileProvider provider = FileProviderFactory.getProvider(destDir);
        
        try {
            zis = new ZipInputStream(stream);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryPath = destDir;
                if (!entryPath.endsWith("/") && !entryPath.endsWith(File.separator)) {
                    entryPath += "/";
                }
                entryPath += entry.getName();
                
                if (entry.isDirectory()) {
                    // 创建目录
                    provider.mkdirs(entryPath);
                } else {
                    // 确保父目录存在
                    String parentPath = provider.getParent(entryPath);
                    if (parentPath != null && !provider.exists(parentPath)) {
                        provider.mkdirs(parentPath);
                    }
                    // 写入文件
                    try {
                        fos = provider.openOutputStream(entryPath);
                    } catch (Exception e) {
                        throw new IOException("Failed to open output stream for: " + entryPath, e);
                    }
                    if (fos != null) {
                        PFiles.write(zis, fos, false);
                        fos.close();
                        fos = null;
                    }
                    zis.closeEntry();
                }
            }
        } finally {
            closeSilently(fos);
            closeSilently(stream);
            closeSilently(zis);
        }
    }

    public static void unzip(File zipFile, File dir) throws IOException {
        unzip(zipFile, dir.getAbsolutePath());
    }
    
    /**
     * 解压 ZIP 文件到指定目录（支持 SAF 模式）
     * @param zipFile ZIP 文件
     * @param destDir 目标目录路径
     * @throws IOException IO 异常
     */
    public static void unzip(File zipFile, String destDir) throws IOException {
        // 对于 ZIP 文件本身，使用传统方式读取（ZIP 文件通常不在 SAF 目录）
        unzip(new FileInputStream(zipFile), destDir);
    }

}
