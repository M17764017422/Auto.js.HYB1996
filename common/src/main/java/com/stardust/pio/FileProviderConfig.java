package com.stardust.pio;

/**
 * 文件访问提供者配置接口
 * 用于获取运行时配置信息，避免对具体实现类的依赖
 */
public interface FileProviderConfig {
    
    /**
     * 获取 SAF 目录 URI
     */
    String getSafDirectoryUri();
    
    /**
     * 获取脚本目录路径
     */
    String getScriptDirPath();
    
    /**
     * 检查是否有 SAF 访问权限
     */
    boolean hasSafAccess();
    
    /**
     * 获取应用包名
     */
    String getPackageName();
}