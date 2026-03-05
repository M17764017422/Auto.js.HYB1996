package org.autojs.autojs.external.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.stardust.autojs.engine.ScriptEngine;
import com.stardust.autojs.script.ScriptSource;
import com.stardust.pio.IFileProvider;
import com.stardust.pio.PFiles;
import com.stardust.pio.FileProviderFactory;

import org.autojs.autojs.BuildConfig;
import org.autojs.autojs.autojs.AutoJs;
import org.autojs.autojs.pluginclient.DevPluginResponseHandler;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ADB 调试接口接收器
 * 支持 SAF (Storage Access Framework) 模式
 * 
 * 命令处理委托给 DevPluginResponseHandler 统一处理
 * 
 * 使用方法:
 * adb shell am broadcast -n <package>/<class> -a <action> [--es param value]
 * 
 * 推荐使用 Base64 编码传输脚本内容，避免引号转义问题:
 * --es script <base64> --ez base64 true
 */
public class AdbDebugReceiver extends BroadcastReceiver {

    private static final String TAG = "AdbDebugReceiver";

    // Actions - 与 DevPluginResponseHandler 命令对应
    public static final String ACTION_RUN_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.RUN_SCRIPT";
    public static final String ACTION_STOP_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.STOP_SCRIPT";
    public static final String ACTION_STOP_ALL = BuildConfig.APPLICATION_ID + ".adb.STOP_ALL";
    public static final String ACTION_LIST_SCRIPTS = BuildConfig.APPLICATION_ID + ".adb.LIST_SCRIPTS";
    public static final String ACTION_SAVE_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.SAVE_SCRIPT";
    public static final String ACTION_PUSH_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.PUSH_SCRIPT";
    public static final String ACTION_RERUN_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.RERUN_SCRIPT";
    public static final String ACTION_DELETE_FILE = BuildConfig.APPLICATION_ID + ".adb.DELETE_FILE";
    public static final String ACTION_LIST_FILES = BuildConfig.APPLICATION_ID + ".adb.LIST_FILES";
    public static final String ACTION_READ_FILE = BuildConfig.APPLICATION_ID + ".adb.READ_FILE";
    public static final String ACTION_WRITE_FILE = BuildConfig.APPLICATION_ID + ".adb.WRITE_FILE";
    public static final String ACTION_MKDIR = BuildConfig.APPLICATION_ID + ".adb.MKDIR";
    public static final String ACTION_RENAME_FILE = BuildConfig.APPLICATION_ID + ".adb.RENAME_FILE";
    public static final String ACTION_PING = BuildConfig.APPLICATION_ID + ".adb.PING";
    public static final String ACTION_VERSION = BuildConfig.APPLICATION_ID + ".adb.VERSION";
    public static final String ACTION_GET_CONFIG = BuildConfig.APPLICATION_ID + ".adb.GET_CONFIG";

    // Extras
    public static final String EXTRA_SCRIPT = "script";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_BASE64 = "base64";
    public static final String EXTRA_JSON = "json";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_OLDPATH = "oldpath";
    public static final String EXTRA_NEWPATH = "newpath";
    public static final String EXTRA_KEY = "key";

    // 统一的命令处理器
    private static DevPluginResponseHandler sHandler = null;
    private static final Gson sGson = new Gson();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        // 初始化处理器（懒加载）
        if (sHandler == null) {
            File cacheDir = new File(context.getCacheDir(), "adb_debug");
            sHandler = new DevPluginResponseHandler(cacheDir);
        }

        boolean jsonOutput = getBooleanExtra(intent, EXTRA_JSON, false);
        String result;
        try {
            result = handleAction(intent, context);
        } catch (Exception e) {
            Log.e(TAG, "Error handling action: " + action, e);
            result = jsonOutput ? buildJsonError(e.getMessage()) : ("ERROR: " + e.getMessage());
        }

        // 返回结果
        Bundle extras = getResultExtras(true);
        extras.putString(EXTRA_RESULT, result);
        Log.d(TAG, "Result: " + result);
    }

    /**
     * 统一命令处理入口
     * 将 Intent 转换为 JSON 格式，委托给 DevPluginResponseHandler 处理
     */
    private String handleAction(Intent intent, Context context) {
        String action = intent.getAction();
        boolean jsonOutput = getBooleanExtra(intent, EXTRA_JSON, false);

        // 基础命令
        if (ACTION_PING.equals(action)) {
            return handlePing(jsonOutput);
        }

        if (ACTION_VERSION.equals(action)) {
            return handleVersion(jsonOutput);
        }

        if (ACTION_GET_CONFIG.equals(action)) {
            return handleGetConfig(intent, jsonOutput);
        }

        if (ACTION_LIST_SCRIPTS.equals(action)) {
            return handleListScripts(jsonOutput);
        }

        if (ACTION_STOP_ALL.equals(action)) {
            return handleStopAll(jsonOutput);
        }

        // 脚本命令 - 委托给 DevPluginResponseHandler
        if (ACTION_RUN_SCRIPT.equals(action)) {
            return handleRunScript(intent, jsonOutput);
        }

        if (ACTION_STOP_SCRIPT.equals(action)) {
            return handleStopScript(intent, jsonOutput);
        }

        if (ACTION_SAVE_SCRIPT.equals(action)) {
            return handleSaveScript(intent, jsonOutput);
        }

        if (ACTION_PUSH_SCRIPT.equals(action)) {
            return handlePushScript(intent, jsonOutput);
        }

        if (ACTION_RERUN_SCRIPT.equals(action)) {
            return handleRerunScript(intent, jsonOutput);
        }

        // 文件操作命令
        if (ACTION_DELETE_FILE.equals(action)) {
            return handleFileDelete(intent, jsonOutput);
        }

        if (ACTION_LIST_FILES.equals(action)) {
            return handleFileList(intent, jsonOutput);
        }

        if (ACTION_READ_FILE.equals(action)) {
            return handleFileRead(intent, jsonOutput);
        }

        if (ACTION_WRITE_FILE.equals(action)) {
            return handleFileWrite(intent, jsonOutput);
        }

        if (ACTION_MKDIR.equals(action)) {
            return handleMkdir(intent, jsonOutput);
        }

        if (ACTION_RENAME_FILE.equals(action)) {
            return handleRenameFile(intent, jsonOutput);
        }

        String errorMsg = "Unknown action: " + action;
        return jsonOutput ? buildJsonError(errorMsg) : errorMsg;
    }

    /**
     * PING 命令
     */
    private String handlePing(boolean jsonOutput) {
        String modeStr = FileProviderFactory.getModeDescription();
        String message = BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME + " (mode: " + modeStr + ")";
        return jsonOutput ? buildJsonSuccess("pong", message) : "PONG: " + message;
    }

    /**
     * VERSION 命令
     */
    private String handleVersion(boolean jsonOutput) {
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("package", BuildConfig.APPLICATION_ID);
            result.addProperty("versionName", BuildConfig.VERSION_NAME);
            result.addProperty("versionCode", BuildConfig.VERSION_CODE);
            result.addProperty("debug", BuildConfig.DEBUG);
            result.addProperty("fileMode", FileProviderFactory.getModeDescription());
            return sGson.toJson(result);
        }
        return "Version: " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n" +
               "Package: " + BuildConfig.APPLICATION_ID + "\n" +
               "Mode: " + FileProviderFactory.getModeDescription();
    }

    /**
     * GET_CONFIG 命令 - 获取配置信息
     */
    private String handleGetConfig(Intent intent, boolean jsonOutput) {
        String key = intent.getStringExtra(EXTRA_KEY);
        
        // 使用 FileProvider 获取实际工作目录，确保 SAF 模式兼容
        IFileProvider provider = FileProviderFactory.getProvider();
        
        JsonObject config = new JsonObject();
        config.addProperty("scriptDir", provider.getWorkingDirectory());
        config.addProperty("fileMode", FileProviderFactory.getModeDescription());
        config.addProperty("versionName", BuildConfig.VERSION_NAME);
        config.addProperty("packageName", BuildConfig.APPLICATION_ID);
        
        if (!TextUtils.isEmpty(key)) {
            if (config.has(key)) {
                String value = config.get(key).getAsString();
                return jsonOutput ? buildJsonSuccess("config", value) : value;
            }
            return jsonOutput ? buildJsonError("Key not found: " + key) : "ERROR: Key not found: " + key;
        }
        
        return jsonOutput ? sGson.toJson(config) : config.toString();
    }

    /**
     * 运行脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleRunScript(Intent intent, boolean jsonOutput) {
        String script = intent.getStringExtra(EXTRA_SCRIPT);
        String path = intent.getStringExtra(EXTRA_PATH);
        String name = intent.getStringExtra(EXTRA_NAME);
        String id = intent.getStringExtra(EXTRA_ID);
        boolean isBase64 = getBooleanExtra(intent, EXTRA_BASE64, false);

        // 解码 base64
        if (isBase64 && !TextUtils.isEmpty(script)) {
            try {
                byte[] decoded = Base64.decode(script, Base64.DEFAULT);
                script = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return jsonOutput ? buildJsonError("Invalid base64 encoding") : "ERROR: Invalid base64 encoding";
            }
        }

        // 如果指定了路径，从文件读取
        if (!TextUtils.isEmpty(path) && TextUtils.isEmpty(script)) {
            try {
                script = FileProviderFactory.getProvider().read(path);
                if (script == null) {
                    return jsonOutput ? buildJsonError("Failed to read file: " + path) : "ERROR: Failed to read file: " + path;
                }
                if (TextUtils.isEmpty(name)) {
                    name = new File(path).getName();
                }
            } catch (Exception e) {
                return jsonOutput ? buildJsonError("Cannot read file: " + e.getMessage()) : "ERROR: Cannot read file: " + e.getMessage();
            }
        }

        if (TextUtils.isEmpty(script)) {
            return jsonOutput ? buildJsonError("Missing 'script' or 'path' parameter") : "ERROR: Missing 'script' or 'path' parameter";
        }

        String scriptId = TextUtils.isEmpty(id) ? "adb_" + System.currentTimeMillis() : id;
        String scriptName = TextUtils.isEmpty(name) ? "" : name;

        // 构建JSON并委托给处理器
        JsonObject data = new JsonObject();
        data.addProperty("id", scriptId);
        data.addProperty("script", script);
        data.addProperty("name", scriptName);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("run", data));

        boolean success = sHandler.handle(command);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("id", scriptId);
            result.addProperty("name", scriptName);
            return sGson.toJson(result);
        }
        return success ? "OK: Script started, id=" + scriptId : "ERROR: Failed to start script";
    }

    /**
     * 停止脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleStopScript(Intent intent, boolean jsonOutput) {
        String id = intent.getStringExtra(EXTRA_ID);
        if (TextUtils.isEmpty(id)) {
            return jsonOutput ? buildJsonError("Missing 'id' parameter") : "ERROR: Missing 'id' parameter";
        }

        JsonObject data = new JsonObject();
        data.addProperty("id", id);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("stop", data));

        boolean success = sHandler.handle(command);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("id", id);
            return sGson.toJson(result);
        }
        return success ? "OK: Script stopped, id=" + id : "ERROR: Failed to stop script";
    }

    /**
     * 推送并运行脚本 - 委托给 DevPluginResponseHandler
     */
    private String handlePushScript(Intent intent, boolean jsonOutput) {
        String script = intent.getStringExtra(EXTRA_SCRIPT);
        String name = intent.getStringExtra(EXTRA_NAME);
        String id = intent.getStringExtra(EXTRA_ID);
        boolean isBase64 = getBooleanExtra(intent, EXTRA_BASE64, false);

        if (TextUtils.isEmpty(script)) {
            return jsonOutput ? buildJsonError("Missing 'script' parameter") : "ERROR: Missing 'script' parameter";
        }

        // 解码 base64
        if (isBase64) {
            try {
                byte[] decoded = Base64.decode(script, Base64.DEFAULT);
                script = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return jsonOutput ? buildJsonError("Invalid base64 encoding") : "ERROR: Invalid base64 encoding";
            }
        }

        String scriptId = TextUtils.isEmpty(id) ? "adb_" + System.currentTimeMillis() : id;
        String scriptName = TextUtils.isEmpty(name) ? "" : name;

        // 构建JSON并委托给处理器（使用 run 命令）
        JsonObject data = new JsonObject();
        data.addProperty("id", scriptId);
        data.addProperty("script", script);
        data.addProperty("name", scriptName);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("run", data));

        boolean success = sHandler.handle(command);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("id", scriptId);
            result.addProperty("name", scriptName);
            return sGson.toJson(result);
        }
        return success ? "OK: Script pushed and started, id=" + scriptId : "ERROR: Failed to start script";
    }

    /**
     * 保存脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleSaveScript(Intent intent, boolean jsonOutput) {
        String script = intent.getStringExtra(EXTRA_SCRIPT);
        String name = intent.getStringExtra(EXTRA_NAME);
        boolean isBase64 = getBooleanExtra(intent, EXTRA_BASE64, false);

        if (TextUtils.isEmpty(script)) {
            return jsonOutput ? buildJsonError("Missing 'script' parameter") : "ERROR: Missing 'script' parameter";
        }

        // 解码 base64
        if (isBase64) {
            try {
                byte[] decoded = Base64.decode(script, Base64.DEFAULT);
                script = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return jsonOutput ? buildJsonError("Invalid base64 encoding") : "ERROR: Invalid base64 encoding";
            }
        }

        String scriptName = TextUtils.isEmpty(name) ? "untitled.js" : name;

        JsonObject data = new JsonObject();
        data.addProperty("script", script);
        data.addProperty("name", scriptName);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("save", data));

        boolean success = sHandler.handle(command);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("name", scriptName);
            return sGson.toJson(result);
        }
        return success ? "OK: Script saved: " + scriptName : "ERROR: Failed to save script";
    }

    /**
     * 重运行脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleRerunScript(Intent intent, boolean jsonOutput) {
        String script = intent.getStringExtra(EXTRA_SCRIPT);
        String id = intent.getStringExtra(EXTRA_ID);
        String name = intent.getStringExtra(EXTRA_NAME);
        boolean isBase64 = getBooleanExtra(intent, EXTRA_BASE64, false);

        if (TextUtils.isEmpty(script)) {
            return jsonOutput ? buildJsonError("Missing 'script' parameter") : "ERROR: Missing 'script' parameter";
        }

        // 解码 base64
        if (isBase64) {
            try {
                byte[] decoded = Base64.decode(script, Base64.DEFAULT);
                script = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return jsonOutput ? buildJsonError("Invalid base64 encoding") : "ERROR: Invalid base64 encoding";
            }
        }

        String scriptId = TextUtils.isEmpty(id) ? "adb_" + System.currentTimeMillis() : id;
        String scriptName = TextUtils.isEmpty(name) ? "" : name;

        JsonObject data = new JsonObject();
        data.addProperty("id", scriptId);
        data.addProperty("script", script);
        data.addProperty("name", scriptName);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("rerun", data));

        boolean success = sHandler.handle(command);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("id", scriptId);
            return sGson.toJson(result);
        }
        return success ? "OK: Script rerun, id=" + scriptId : "ERROR: Failed to rerun script";
    }

    /**
     * 列出运行中的脚本
     */
    private String handleListScripts(boolean jsonOutput) {
        try {
            Set<ScriptEngine> engines = AutoJs.getInstance()
                    .getScriptEngineService().getEngines();
            
            if (jsonOutput) {
                JsonObject result = new JsonObject();
                result.addProperty("success", true);
                JsonArray scriptsArray = new JsonArray();
                
                if (engines != null && !engines.isEmpty()) {
                    result.addProperty("count", engines.size());
                    for (ScriptEngine engine : engines) {
                        JsonObject scriptInfo = new JsonObject();
                        scriptInfo.addProperty("id", engine.getId());
                        Object source = engine.getTag(ScriptEngine.TAG_SOURCE);
                        if (source instanceof ScriptSource) {
                            scriptInfo.addProperty("source", ((ScriptSource) source).getName());
                        }
                        scriptsArray.add(scriptInfo);
                    }
                } else {
                    result.addProperty("count", 0);
                }
                result.add("scripts", scriptsArray);
                return sGson.toJson(result);
            }
            
            if (engines == null || engines.isEmpty()) {
                return "OK: No running scripts";
            }

            StringBuilder sb = new StringBuilder("OK: Running scripts (" + engines.size() + "):\n");
            for (ScriptEngine engine : engines) {
                sb.append("  id=").append(engine.getId());
                Object source = engine.getTag(ScriptEngine.TAG_SOURCE);
                if (source instanceof ScriptSource) {
                    sb.append(", source=").append(((ScriptSource) source).getName());
                }
                sb.append("\n");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            return jsonOutput ? buildJsonError(e.getMessage()) : "ERROR: " + e.getMessage();
        }
    }

    /**
     * 停止所有脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleStopAll(boolean jsonOutput) {
        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("stopAll", new JsonObject()));

        boolean success = sHandler.handle(command);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            return sGson.toJson(result);
        }
        return success ? "OK: All scripts stopped" : "ERROR: Failed to stop all scripts";
    }

    // ==================== 文件操作命令 ====================

    /**
     * 删除文件
     */
    private String handleFileDelete(Intent intent, boolean jsonOutput) {
        IFileProvider provider = FileProviderFactory.getProvider();
        String path = intent.getStringExtra(EXTRA_PATH);
        
        if (TextUtils.isEmpty(path)) {
            return jsonOutput ? buildJsonError("Missing 'path' parameter") : "ERROR: Missing 'path' parameter";
        }
        if (!provider.exists(path)) {
            return jsonOutput ? buildJsonError("File not found: " + path) : "ERROR: File not found: " + path;
        }
        
        boolean deleted = provider.delete(path);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", deleted);
            result.addProperty("path", path);
            return sGson.toJson(result);
        }
        return deleted ? "OK: File deleted: " + path : "ERROR: Failed to delete";
    }

    /**
     * 列出目录内容
     */
    private String handleFileList(Intent intent, boolean jsonOutput) {
        IFileProvider provider = FileProviderFactory.getProvider();
        String path = intent.getStringExtra(EXTRA_PATH);
        
        if (TextUtils.isEmpty(path)) {
            path = provider.getWorkingDirectory();
        }
        
        if (!provider.exists(path)) {
            return jsonOutput ? buildJsonError("Directory not found: " + path) : "ERROR: Directory not found: " + path;
        }
        
        List<IFileProvider.FileInfo> files = provider.listFiles(path);
        
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", true);
            result.addProperty("path", path);
            JsonArray filesArray = new JsonArray();
            
            if (files != null) {
                for (IFileProvider.FileInfo f : files) {
                    JsonObject fileInfo = new JsonObject();
                    fileInfo.addProperty("name", f.name);
                    fileInfo.addProperty("isDirectory", f.isDirectory);
                    fileInfo.addProperty("size", f.size);
                    filesArray.add(fileInfo);
                }
            }
            result.add("files", filesArray);
            return sGson.toJson(result);
        }
        
        if (files == null || files.isEmpty()) {
            return "OK: Empty directory: " + path;
        }
        
        StringBuilder sb = new StringBuilder("OK: Files in " + path + ":\n");
        for (IFileProvider.FileInfo f : files) {
            sb.append("  ").append(f.isDirectory ? "[D] " : "[F] ").append(f.name);
            if (!f.isDirectory) {
                sb.append(" (").append(formatSize(f.size)).append(")");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * 读取文件内容
     */
    private String handleFileRead(Intent intent, boolean jsonOutput) {
        IFileProvider provider = FileProviderFactory.getProvider();
        String path = intent.getStringExtra(EXTRA_PATH);
        
        if (TextUtils.isEmpty(path)) {
            return jsonOutput ? buildJsonError("Missing 'path' parameter") : "ERROR: Missing 'path' parameter";
        }
        if (!provider.exists(path)) {
            return jsonOutput ? buildJsonError("File not found: " + path) : "ERROR: File not found: " + path;
        }
        
        String content = provider.read(path);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", content != null);
            result.addProperty("path", path);
            result.addProperty("content", content != null ? content : "");
            return sGson.toJson(result);
        }
        return content != null ? "OK: " + content : "ERROR: Failed to read file";
    }

    /**
     * 写入文件内容
     */
    private String handleFileWrite(Intent intent, boolean jsonOutput) {
        IFileProvider provider = FileProviderFactory.getProvider();
        String path = intent.getStringExtra(EXTRA_PATH);
        String content = intent.getStringExtra(EXTRA_CONTENT);
        boolean isBase64 = getBooleanExtra(intent, EXTRA_BASE64, false);
        
        if (TextUtils.isEmpty(path)) {
            return jsonOutput ? buildJsonError("Missing 'path' parameter") : "ERROR: Missing 'path' parameter";
        }
        if (TextUtils.isEmpty(content)) {
            return jsonOutput ? buildJsonError("Missing 'content' parameter") : "ERROR: Missing 'content' parameter";
        }
        
        // 解码 base64
        if (isBase64) {
            try {
                byte[] decoded = Base64.decode(content, Base64.DEFAULT);
                content = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return jsonOutput ? buildJsonError("Invalid base64 encoding") : "ERROR: Invalid base64 encoding";
            }
        }
        
        boolean success = provider.write(path, content);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", success);
            result.addProperty("path", path);
            return sGson.toJson(result);
        }
        return success ? "OK: File written: " + path : "ERROR: Failed to write file";
    }

    /**
     * 创建目录
     */
    private String handleMkdir(Intent intent, boolean jsonOutput) {
        IFileProvider provider = FileProviderFactory.getProvider();
        String path = intent.getStringExtra(EXTRA_PATH);
        
        if (TextUtils.isEmpty(path)) {
            return jsonOutput ? buildJsonError("Missing 'path' parameter") : "ERROR: Missing 'path' parameter";
        }
        
        boolean created = provider.mkdirs(path);
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", created);
            result.addProperty("path", path);
            return sGson.toJson(result);
        }
        return created ? "OK: Directory created: " + path : "ERROR: Failed to create directory";
    }

    /**
     * 重命名文件
     */
    private String handleRenameFile(Intent intent, boolean jsonOutput) {
        IFileProvider provider = FileProviderFactory.getProvider();
        String oldPath = intent.getStringExtra(EXTRA_OLDPATH);
        String newPath = intent.getStringExtra(EXTRA_NEWPATH);
        
        if (TextUtils.isEmpty(oldPath) || TextUtils.isEmpty(newPath)) {
            return jsonOutput ? buildJsonError("Missing 'oldpath' or 'newpath' parameter") : "ERROR: Missing 'oldpath' or 'newpath' parameter";
        }
        
        boolean renamed = provider.rename(oldPath, provider.getName(newPath));
        if (jsonOutput) {
            JsonObject result = new JsonObject();
            result.addProperty("success", renamed);
            result.addProperty("oldPath", oldPath);
            result.addProperty("newPath", newPath);
            return sGson.toJson(result);
        }
        return renamed ? "OK: Renamed to " + newPath : "ERROR: Failed to rename";
    }

    /**
     * 构建命令数据
     */
    private JsonObject buildCommandData(String command, JsonObject params) {
        JsonObject data = new JsonObject();
        data.addProperty("command", command);
        for (String key : params.keySet()) {
            data.add(key, params.get(key));
        }
        return data;
    }

    /**
     * 兼容 boolean 和 string 类型的 extra
     */
    private boolean getBooleanExtra(Intent intent, String name, boolean defaultValue) {
        boolean value = intent.getBooleanExtra(name, defaultValue);
        if (!value) {
            String strValue = intent.getStringExtra(name);
            if ("true".equalsIgnoreCase(strValue)) {
                value = true;
            }
        }
        return value;
    }

    /**
     * 构建 JSON 成功响应
     */
    private String buildJsonSuccess(String key, String value) {
        JsonObject result = new JsonObject();
        result.addProperty("success", true);
        result.addProperty(key, value);
        return sGson.toJson(result);
    }

    /**
     * 构建 JSON 错误响应
     */
    private String buildJsonError(String message) {
        JsonObject result = new JsonObject();
        result.addProperty("success", false);
        result.addProperty("error", message);
        return sGson.toJson(result);
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format(Locale.US, "%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024));
        return String.format(Locale.US, "%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
