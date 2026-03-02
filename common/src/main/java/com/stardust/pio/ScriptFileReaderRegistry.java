package com.stardust.pio;

/**
 * 脚本文件读取器注册表
 * 用于在不同模块间共享文件读取功能
 */
public class ScriptFileReaderRegistry {

    private static ScriptFileReader sInstance;

    /**
     * 注册脚本文件读取器
     * @param reader 文件读取器实现
     */
    public static synchronized void register(ScriptFileReader reader) {
        sInstance = reader;
    }

    /**
     * 获取注册的脚本文件读取器
     * @return 文件读取器，如果未注册则返回 null
     */
    public static synchronized ScriptFileReader get() {
        return sInstance;
    }

    /**
     * 检查是否已注册文件读取器
     * @return 如果已注册返回 true，否则返回 false
     */
    public static synchronized boolean isRegistered() {
        return sInstance != null;
    }
}