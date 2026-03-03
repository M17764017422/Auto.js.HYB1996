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
    public static final String ACTION_PING = BuildConfig.APPLICATION_ID + ".adb.PING";

    // Extras
    public static final String EXTRA_SCRIPT = "script";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_WORKING_DIRECTORY = "cwd";
    public static final String EXTRA_DELAY = "delay";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_BASE64 = "base64";  // 脚本内容是否为 base64 编码

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
     * 清理已完成的执行记录
     */
    public static void cleanupExecution(int id) {
        sRunningExecutions.remove(id);
    }
}