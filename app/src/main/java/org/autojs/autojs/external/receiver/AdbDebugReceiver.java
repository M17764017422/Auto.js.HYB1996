package org.autojs.autojs.external.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.stardust.autojs.execution.ExecutionConfig;
import com.stardust.autojs.execution.ScriptExecution;
import com.stardust.autojs.script.StringScriptSource;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.PFiles;

import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.BuildConfig;
import org.autojs.autojs.storage.FileProviderFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ADB 调试接口接收器
 * 支持 SAF (Storage Access Framework) 模式
 * 
 * 支持的命令:
 * - ACTION_RUN_SCRIPT: 运行脚本 (path 或 script 参数)
 * - ACTION_STOP_SCRIPT: 停止脚本 (id 参数)
 * - ACTION_STOP_ALL: 停止所有脚本
 * - ACTION_LIST_SCRIPTS: 列出运行中的脚本
 * - ACTION_PUSH_SCRIPT: 推送脚本到设备 (name, content 参数)
 * - ACTION_DELETE_SCRIPT: 删除脚本文件 (path 参数)
 * - ACTION_LIST_FILES: 列出脚本目录文件
 * - ACTION_READ_FILE: 读取文件内容 (path 参数)
 * - ACTION_MKDIR: 创建目录 (path 参数)
 * - ACTION_RENAME_FILE: 重命名/移动文件 (oldpath, newpath 参数)
 * - ACTION_GET_SCRIPT_OUTPUT: 获取脚本控制台输出 (id 参数, 可选 lines 参数)
 * - ACTION_PING: 检查应用状态
 * 
 * 使用方法:
 * adb shell am broadcast -a org.autojs.autojs.ACTION_RUN_SCRIPT --es script "toast('hello')"
 * adb shell am broadcast -a org.autojs.autojs.ACTION_RUN_SCRIPT --es path "/sdcard/脚本/test.js"
 * adb shell am broadcast -a org.autojs.autojs.ACTION_STOP_ALL
 * adb shell am broadcast -a org.autojs.autojs.ACTION_LIST_SCRIPTS
 */
public class AdbDebugReceiver extends BroadcastReceiver {

    private static final String TAG = "AdbDebugReceiver";

    // Actions
    public static final String ACTION_RUN_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.RUN_SCRIPT";
    public static final String ACTION_STOP_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.STOP_SCRIPT";
    public static final String ACTION_STOP_ALL = BuildConfig.APPLICATION_ID + ".adb.STOP_ALL";
    public static final String ACTION_LIST_SCRIPTS = BuildConfig.APPLICATION_ID + ".adb.LIST_SCRIPTS";
    public static final String ACTION_PUSH_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.PUSH_SCRIPT";
    public static final String ACTION_DELETE_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.DELETE_SCRIPT";
    public static final String ACTION_LIST_FILES = BuildConfig.APPLICATION_ID + ".adb.LIST_FILES";
    public static final String ACTION_READ_FILE = BuildConfig.APPLICATION_ID + ".adb.READ_FILE";
    public static final String ACTION_MKDIR = BuildConfig.APPLICATION_ID + ".adb.MKDIR";
    public static final String ACTION_RENAME_FILE = BuildConfig.APPLICATION_ID + ".adb.RENAME_FILE";
    public static final String ACTION_GET_SCRIPT_OUTPUT = BuildConfig.APPLICATION_ID + ".adb.GET_SCRIPT_OUTPUT";
    public static final String ACTION_PING = BuildConfig.APPLICATION_ID + ".adb.PING";

    // Extras
    public static final String EXTRA_SCRIPT = "script";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_OLD_PATH = "oldpath";
    public static final String EXTRA_NEW_PATH = "newpath";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_WORKING_DIRECTORY = "cwd";
    public static final String EXTRA_DELAY = "delay";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_BASE64 = "base64";  // 脚本内容是否为 base64 编码
    public static final String EXTRA_LINES = "lines";    // 读取的行数
    public static final String EXTRA_ENCODING = "encoding"; // 文件编码

    // 存储运行中的脚本
    private static final ConcurrentHashMap<Integer, ScriptExecution> sRunningExecutions = new ConcurrentHashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        String result;
        try {
            if (ACTION_RUN_SCRIPT.equals(action)) {
                result = handleRunScript(context, intent);
            } else if (ACTION_STOP_SCRIPT.equals(action)) {
                result = handleStopScript(intent);
            } else if (ACTION_STOP_ALL.equals(action)) {
                result = handleStopAll();
            } else if (ACTION_LIST_SCRIPTS.equals(action)) {
                result = handleListScripts();
            } else if (ACTION_PUSH_SCRIPT.equals(action)) {
                result = handlePushScript(context, intent);
            } else if (ACTION_DELETE_SCRIPT.equals(action)) {
                result = handleDeleteScript(intent);
            } else if (ACTION_LIST_FILES.equals(action)) {
                result = handleListFiles(intent);
            } else if (ACTION_READ_FILE.equals(action)) {
                result = handleReadFile(intent);
            } else if (ACTION_MKDIR.equals(action)) {
                result = handleMkdir(intent);
            } else if (ACTION_RENAME_FILE.equals(action)) {
                result = handleRenameFile(intent);
            } else if (ACTION_GET_SCRIPT_OUTPUT.equals(action)) {
                result = handleGetScriptOutput(intent);
            } else if (ACTION_PING.equals(action)) {
                String modeStr = FileProviderFactory.getModeDescription();
                result = "PONG: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME + " (mode: " + modeStr + ")";
            } else {
                result = "Unknown action: " + action;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling action: " + action, e);
            result = "ERROR: " + e.getMessage();
        }

        // 返回结果
        Bundle extras = getResultExtras(true);
        extras.putString(EXTRA_RESULT, result);
        Log.d(TAG, "Result: " + result);
    }

    /**
     * 获取文件提供者
     */
    private IFileProvider getFileProvider() {
        return FileProviderFactory.getProvider();
    }

    /**
     * 运行脚本
     * 支持 path (文件路径) 或 script (脚本内容) 参数
     * 支持 base64 编码的脚本内容
     * 兼容 SAF 模式
     */
    private String handleRunScript(Context context, Intent intent) {
        String scriptContent = intent.getStringExtra(EXTRA_SCRIPT);
        String path = intent.getStringExtra(EXTRA_PATH);
        String workingDir = intent.getStringExtra(EXTRA_WORKING_DIRECTORY);
        long delay = intent.getLongExtra(EXTRA_DELAY, 0);
        // Support both boolean and string type for base64 parameter
        boolean isBase64 = intent.getBooleanExtra(EXTRA_BASE64, false);
        if (!isBase64) {
            String base64Str = intent.getStringExtra(EXTRA_BASE64);
            isBase64 = "true".equalsIgnoreCase(base64Str);
        }

        if (TextUtils.isEmpty(scriptContent) && TextUtils.isEmpty(path)) {
            return "ERROR: Missing 'script' or 'path' parameter";
        }

        // 解码 base64 编码的脚本内容
        if (isBase64 && !TextUtils.isEmpty(scriptContent)) {
            try {
                byte[] decoded = Base64.decode(scriptContent, Base64.DEFAULT);
                scriptContent = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return "ERROR: Invalid base64 encoding: " + e.getMessage();
            }
        }

        // 如果指定了路径，从文件读取
        if (!TextUtils.isEmpty(path) && TextUtils.isEmpty(scriptContent)) {
            IFileProvider provider = getFileProvider();
            
            if (!provider.exists(path)) {
                return "ERROR: File not found: " + path;
            }
            
            // 使用 FileProvider 读取文件内容（支持 SAF）
            try {
                scriptContent = provider.read(path);
                if (scriptContent == null) {
                    return "ERROR: Failed to read file: " + path;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading file: " + path, e);
                return "ERROR: Cannot read file: " + e.getMessage();
            }
            
            // 设置工作目录
            if (TextUtils.isEmpty(workingDir)) {
                workingDir = provider.getParent(path);
            }
        }

        // 执行脚本内容
        ExecutionConfig config = new ExecutionConfig();
        if (!TextUtils.isEmpty(workingDir)) {
            config.setWorkingDirectory(workingDir);
        }
        if (delay > 0) {
            config.setDelay(delay);
        }

        StringScriptSource source = new StringScriptSource(scriptContent);
        ScriptExecution execution = AutoJs.getInstance().getScriptEngineService().execute(source, config);

        if (execution != null) {
            sRunningExecutions.put(execution.getId(), execution);
            String pathInfo = path != null ? ", path=" + path : "";
            return "OK: Script started, id=" + execution.getId() + pathInfo;
        } else {
            return "ERROR: Failed to start script";
        }
    }

    /**
     * 停止指定脚本
     */
    private String handleStopScript(Intent intent) {
        int id = intent.getIntExtra(EXTRA_ID, -1);
        if (id == -1) {
            return "ERROR: Missing 'id' parameter";
        }

        ScriptExecution execution = sRunningExecutions.remove(id);
        if (execution != null) {
            execution.getEngine().forceStop();
            return "OK: Script stopped, id=" + id;
        } else {
            return "ERROR: Script not found, id=" + id;
        }
    }

    /**
     * 停止所有脚本（包括非本 Receiver 启动的脚本）
     */
    private String handleStopAll() {
        // 先清理本地追踪
        int tracked = sRunningExecutions.size();
        sRunningExecutions.clear();
        
        // 调用全局停止所有脚本
        try {
            int stopped = AutoJs.getInstance().getScriptEngineService().stopAll();
            return "OK: Stopped " + stopped + " scripts (tracked: " + tracked + ")";
        } catch (Exception e) {
            return "OK: Stopped " + tracked + " tracked scripts (global stop failed: " + e.getMessage() + ")";
        }
    }

    /**
     * 列出运行中的脚本（包括非本 Receiver 启动的脚本）
     */
    private String handleListScripts() {
        try {
            Set<com.stardust.autojs.engine.ScriptEngine> engines = 
                AutoJs.getInstance().getScriptEngineService().getEngines();
            if (engines == null || engines.isEmpty()) {
                return "OK: No running scripts";
            }

            StringBuilder sb = new StringBuilder("OK: Running scripts (" + engines.size() + "):\n");
            for (com.stardust.autojs.engine.ScriptEngine engine : engines) {
                sb.append("  id=").append(engine.getId());
                Object source = engine.getTag(com.stardust.autojs.engine.ScriptEngine.TAG_SOURCE);
                if (source instanceof com.stardust.autojs.script.ScriptSource) {
                    sb.append(", source=").append(((com.stardust.autojs.script.ScriptSource) source).getName());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Error listing scripts", e);
            // 回退到本地追踪
            if (sRunningExecutions.isEmpty()) {
                return "OK: No running scripts";
            }

            StringBuilder sb = new StringBuilder("OK: Running scripts (" + sRunningExecutions.size() + " tracked):\n");
            for (Integer id : sRunningExecutions.keySet()) {
                ScriptExecution execution = sRunningExecutions.get(id);
                sb.append("  id=").append(id);
                if (execution.getSource() != null) {
                    sb.append(", source=").append(execution.getSource().toString());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        }
    }

    /**
     * 推送脚本到设备
     * 兼容 SAF 模式
     */
    private String handlePushScript(Context context, Intent intent) {
        String name = intent.getStringExtra(EXTRA_NAME);
        String content = intent.getStringExtra(EXTRA_CONTENT);
        String path = intent.getStringExtra(EXTRA_PATH);
        // Support both boolean and string type for base64 parameter
        boolean isBase64 = intent.getBooleanExtra(EXTRA_BASE64, false);
        if (!isBase64) {
            String base64Str = intent.getStringExtra(EXTRA_BASE64);
            isBase64 = "true".equalsIgnoreCase(base64Str);
        }

        if (TextUtils.isEmpty(name) && TextUtils.isEmpty(path)) {
            return "ERROR: Missing 'name' or 'path' parameter";
        }
        if (TextUtils.isEmpty(content)) {
            return "ERROR: Missing 'content' parameter";
        }

        // 解码 base64 编码的内容
        if (isBase64) {
            try {
                byte[] decoded = Base64.decode(content, Base64.DEFAULT);
                content = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return "ERROR: Invalid base64 encoding: " + e.getMessage();
            }
        }

        IFileProvider provider = getFileProvider();
        
        // 确定目标路径
        String targetPath;
        if (!TextUtils.isEmpty(path)) {
            targetPath = path;
        } else {
            // 默认保存到脚本目录
            String scriptDir = PFiles.getSdcardPath() + "/脚本";
            // 确保目录存在
            if (!provider.exists(scriptDir)) {
                provider.mkdirs(scriptDir);
            }
            // 确保文件名以 .js 结尾
            if (!name.endsWith(".js")) {
                name = name + ".js";
            }
            targetPath = scriptDir + "/" + name;
        }

        // 确保父目录存在
        String parentDir = provider.getParent(targetPath);
        if (!TextUtils.isEmpty(parentDir) && !provider.exists(parentDir)) {
            provider.mkdirs(parentDir);
        }

        // 使用 FileProvider 写入文件（支持 SAF）
        try {
            boolean success = provider.write(targetPath, content);
            if (success) {
                return "OK: Script saved to " + targetPath;
            } else {
                return "ERROR: Failed to write file";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing file: " + targetPath, e);
            return "ERROR: Failed to write file: " + e.getMessage();
        }
    }

    /**
     * 删除脚本文件
     * 兼容 SAF 模式
     */
    private String handleDeleteScript(Intent intent) {
        String path = intent.getStringExtra(EXTRA_PATH);
        if (TextUtils.isEmpty(path)) {
            return "ERROR: Missing 'path' parameter";
        }

        IFileProvider provider = getFileProvider();
        
        if (!provider.exists(path)) {
            return "ERROR: File not found: " + path;
        }

        // 使用 FileProvider 删除文件（支持 SAF）
        try {
            boolean success = provider.delete(path);
            if (success) {
                return "OK: File deleted: " + path;
            } else {
                return "ERROR: Failed to delete file: " + path;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + path, e);
            return "ERROR: Failed to delete file: " + e.getMessage();
        }
    }

    /**
     * 列出脚本目录文件
     * 兼容 SAF 模式
     */
    private String handleListFiles(Intent intent) {
        String path = intent.getStringExtra(EXTRA_PATH);
        
        IFileProvider provider = getFileProvider();
        
        // 默认列出脚本目录
        if (TextUtils.isEmpty(path)) {
            path = PFiles.getSdcardPath() + "/脚本";
        }

        if (!provider.exists(path)) {
            return "ERROR: Directory not found: " + path;
        }
        if (!provider.isDirectory(path)) {
            return "ERROR: Not a directory: " + path;
        }

        // 使用 FileProvider 列出文件（支持 SAF）
        try {
            List<IFileProvider.FileInfo> files = provider.listFiles(path);
            if (files == null || files.isEmpty()) {
                return "OK: Empty directory: " + path;
            }

            StringBuilder sb = new StringBuilder("OK: Files in ");
            sb.append(path).append(":\n");
            for (IFileProvider.FileInfo file : files) {
                sb.append("  ");
                if (file.isDirectory) {
                    sb.append("[D] ");
                } else {
                    sb.append("[F] ");
                }
                sb.append(file.name);
                if (!file.isDirectory) {
                    sb.append(" (").append(formatSize(file.size)).append(")");
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Error listing files: " + path, e);
            return "ERROR: Failed to list files: " + e.getMessage();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }

    /**
     * 读取文件内容
     * 支持 base64 编码返回（用于二进制文件）
     * 兼容 SAF 模式
     */
    private String handleReadFile(Intent intent) {
        String path = intent.getStringExtra(EXTRA_PATH);
        boolean returnBase64 = intent.getBooleanExtra(EXTRA_BASE64, false);
        if (!returnBase64) {
            String base64Str = intent.getStringExtra(EXTRA_BASE64);
            returnBase64 = "true".equalsIgnoreCase(base64Str);
        }
        
        if (TextUtils.isEmpty(path)) {
            return "ERROR: Missing 'path' parameter";
        }

        IFileProvider provider = getFileProvider();
        
        if (!provider.exists(path)) {
            return "ERROR: File not found: " + path;
        }
        
        if (provider.isDirectory(path)) {
            return "ERROR: Path is a directory: " + path;
        }

        try {
            if (returnBase64) {
                // 返回 base64 编码内容
                byte[] bytes = provider.readBytes(path);
                if (bytes == null) {
                    return "ERROR: Failed to read file: " + path;
                }
                String base64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
                return "OK: " + base64;
            } else {
                // 返回文本内容
                String content = provider.read(path);
                if (content == null) {
                    return "ERROR: Failed to read file: " + path;
                }
                return "OK: " + content;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + path, e);
            return "ERROR: Failed to read file: " + e.getMessage();
        }
    }

    /**
     * 创建目录
     * 支持递归创建（类似 mkdir -p）
     * 兼容 SAF 模式
     */
    private String handleMkdir(Intent intent) {
        String path = intent.getStringExtra(EXTRA_PATH);
        
        if (TextUtils.isEmpty(path)) {
            return "ERROR: Missing 'path' parameter";
        }

        IFileProvider provider = getFileProvider();
        
        // 检查是否已存在
        if (provider.exists(path)) {
            if (provider.isDirectory(path)) {
                return "OK: Directory already exists: " + path;
            } else {
                return "ERROR: Path exists but is a file: " + path;
            }
        }

        try {
            boolean success = provider.mkdirs(path);
            if (success) {
                return "OK: Directory created: " + path;
            } else {
                return "ERROR: Failed to create directory: " + path;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating directory: " + path, e);
            return "ERROR: Failed to create directory: " + e.getMessage();
        }
    }

    /**
     * 重命名/移动文件或目录
     * 兼容 SAF 模式
     */
    private String handleRenameFile(Intent intent) {
        String oldPath = intent.getStringExtra(EXTRA_OLD_PATH);
        String newPath = intent.getStringExtra(EXTRA_NEW_PATH);
        
        if (TextUtils.isEmpty(oldPath)) {
            return "ERROR: Missing 'oldpath' parameter";
        }
        if (TextUtils.isEmpty(newPath)) {
            return "ERROR: Missing 'newpath' parameter";
        }

        IFileProvider provider = getFileProvider();
        
        if (!provider.exists(oldPath)) {
            return "ERROR: Source not found: " + oldPath;
        }
        
        // 检查目标是否已存在
        if (provider.exists(newPath)) {
            return "ERROR: Destination already exists: " + newPath;
        }
        
        // 确保目标父目录存在
        String parentDir = provider.getParent(newPath);
        if (!TextUtils.isEmpty(parentDir) && !provider.exists(parentDir)) {
            provider.mkdirs(parentDir);
        }

        try {
            // 获取新旧路径的父目录
            String oldParent = provider.getParent(oldPath);
            String newParent = provider.getParent(newPath);
            String newName = provider.getName(newPath);
            
            boolean success;
            if (oldParent != null && oldParent.equals(newParent)) {
                // 同目录重命名，使用 rename 方法
                success = provider.rename(oldPath, newName);
            } else {
                // 跨目录移动，使用 move 方法
                success = provider.move(oldPath, newPath);
            }
            
            if (success) {
                return "OK: Renamed " + oldPath + " -> " + newPath;
            } else {
                return "ERROR: Failed to rename file";
            }
        } catch (Exception e) {
            Log.e(TAG, "Error renaming file: " + oldPath + " -> " + newPath, e);
            return "ERROR: Failed to rename file: " + e.getMessage();
        }
    }

    /**
     * 获取脚本的控制台输出日志
     * 从 Logcat 读取指定脚本的输出
     */
    private String handleGetScriptOutput(Intent intent) {
        int id = intent.getIntExtra(EXTRA_ID, -1);
        int lines = intent.getIntExtra(EXTRA_LINES, 50);
        if (lines <= 0) lines = 50;
        if (lines > 500) lines = 500; // 限制最大行数

        // 首先检查脚本是否正在运行
        Set<com.stardust.autojs.engine.ScriptEngine> engines = null;
        try {
            engines = AutoJs.getInstance().getScriptEngineService().getEngines();
        } catch (Exception e) {
            Log.e(TAG, "Error getting engines", e);
        }
        
        boolean found = false;
        String scriptName = null;
        if (engines != null) {
            for (com.stardust.autojs.engine.ScriptEngine engine : engines) {
                if (engine.getId() == id) {
                    found = true;
                    Object source = engine.getTag(com.stardust.autojs.engine.ScriptEngine.TAG_SOURCE);
                    if (source instanceof com.stardust.autojs.script.ScriptSource) {
                        scriptName = ((com.stardust.autojs.script.ScriptSource) source).getName();
                    }
                    break;
                }
            }
        }
        
        // 如果指定了 ID 但脚本不在运行中
        if (id != -1 && !found) {
            return "ERROR: Script not running, id=" + id;
        }

        // 从 logcat 读取日志
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -v time -t " + lines);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            
            StringBuilder sb = new StringBuilder();
            String line;
            boolean hasOutput = false;
            
            while ((line = reader.readLine()) != null) {
                // 过滤 AutoJS 相关日志
                if (line.contains("AutoJS.Console") || 
                    line.contains("GlobalConsole") ||
                    line.contains("AdbDebugReceiver")) {
                    
                    // 如果指定了脚本 ID，进一步过滤
                    if (id != -1 && scriptName != null) {
                        if (line.contains(scriptName) || line.contains("[" + scriptName + "]") ||
                            line.contains("id=" + id)) {
                            sb.append(line).append("\n");
                            hasOutput = true;
                        }
                    } else {
                        sb.append(line).append("\n");
                        hasOutput = true;
                    }
                }
            }
            
            reader.close();
            process.waitFor();
            
            if (!hasOutput) {
                return "OK: No output logs found";
            }
            
            return "OK: Script output:\n" + sb.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Error reading logcat", e);
            return "ERROR: Failed to read logs: " + e.getMessage();
        }
    }

    /**
     * 清理已完成的执行记录
     */
    public static void cleanupExecution(int id) {
        sRunningExecutions.remove(id);
    }
}