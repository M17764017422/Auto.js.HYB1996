package com.stardust.pio;

import java.io.InputStream;

/**
 * 脚本文件读取器接口
 * 用于支持 SAF 模式下的文件读取
 */
public interface ScriptFileReader {

    /**
     * 读取文件内容为字符串
     * @param filePath 文件路径
     * @return 文件内容
     * @throws Exception 如果读取失败
     */
    String read(String filePath) throws Exception;

    /**
     * 读取文件内容为字符串
     * @param filePath 文件路径
     * @param encoding 字符编码
     * @return 文件内容
     * @throws Exception 如果读取失败
     */
    String read(String filePath, String encoding) throws Exception;

    /**
     * 获取文件输入流
     * @param filePath 文件路径
     * @return 文件输入流
     * @throws Exception 如果获取失败
     */
    InputStream openInputStream(String filePath) throws Exception;
}