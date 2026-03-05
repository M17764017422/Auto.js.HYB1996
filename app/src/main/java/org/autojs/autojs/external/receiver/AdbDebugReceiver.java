package org.autojs.autojs.external.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

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
import java.util.List;
import java.util.Set;

/**
 * ADB 调试接口接收器
 * 支持 SAF (Storage Access Framework) 模式
 * 
 * 命令处理委托给 DevPluginResponseHandler 统一处理
 * 
 * 使用方法:
 * adb shell am broadcast -a org.autojs.autojs.adb.RUN_SCRIPT --es script "toast('hello')"
 * adb shell am broadcast -a org.autojs.autojs.adb.RUN_SCRIPT --es path "/sdcard/脚本/test.js"
 * adb shell am broadcast -a org.autojs.autojs.adb.STOP_ALL
 * adb shell am broadcast -a org.autojs.autojs.adb.LIST_SCRIPTS
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
    public static final String ACTION_DELETE_SCRIPT = BuildConfig.APPLICATION_ID + ".adb.DELETE_SCRIPT";
    public static final String ACTION_LIST_FILES = BuildConfig.APPLICATION_ID + ".adb.LIST_FILES";
    public static final String ACTION_READ_FILE = BuildConfig.APPLICATION_ID + ".adb.READ_FILE";
    public static final String ACTION_MKDIR = BuildConfig.APPLICATION_ID + ".adb.MKDIR";
    public static final String ACTION_RENAME_FILE = BuildConfig.APPLICATION_ID + ".adb.RENAME_FILE";
    public static final String ACTION_PING = BuildConfig.APPLICATION_ID + ".adb.PING";

    // Extras
    public static final String EXTRA_SCRIPT = "script";
    public static final String EXTRA_PATH = "path";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_RESULT = "result";
    public static final String EXTRA_BASE64 = "base64";

    // 统一的命令处理器
    private static DevPluginResponseHandler sHandler = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);

        // 初始化处理器（懒加载）
        if (sHandler == null) {
            File cacheDir = new File(context.getCacheDir(), "adb_debug");
            sHandler = new DevPluginResponseHandler(cacheDir);
        }

        String result;
        try {
            result = handleAction(intent);
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
     * 统一命令处理入口
     * 将 Intent 转换为 JSON 格式，委托给 DevPluginResponseHandler 处理
     */
    private String handleAction(Intent intent) {
        String action = intent.getAction();

        // 处理不需要 JSON 转换的简单命令
        if (ACTION_PING.equals(action)) {
            String modeStr = FileProviderFactory.getModeDescription();
            return "PONG: " + BuildConfig.APPLICATION_ID + " v" + BuildConfig.VERSION_NAME + " (mode: " + modeStr + ")";
        }

        if (ACTION_LIST_SCRIPTS.equals(action)) {
            return handleListScripts();
        }

        if (ACTION_STOP_ALL.equals(action)) {
            return handleStopAll();
        }

        // 转换为 JSON 格式，委托给 DevPluginResponseHandler
        if (ACTION_RUN_SCRIPT.equals(action)) {
            return handleRunScript(intent);
        }

        if (ACTION_STOP_SCRIPT.equals(action)) {
            return handleStopScript(intent);
        }

        if (ACTION_SAVE_SCRIPT.equals(action) || ACTION_PUSH_SCRIPT.equals(action)) {
            return handleSaveScript(intent);
        }

        if (ACTION_RERUN_SCRIPT.equals(action)) {
            return handleRerunScript(intent);
        }

        // 文件操作命令（扩展功能，不通过 DevPluginResponseHandler）
        if (ACTION_DELETE_SCRIPT.equals(action)) {
            return handleFileOperation("delete", intent);
        }

        if (ACTION_LIST_FILES.equals(action)) {
            return handleFileOperation("list", intent);
        }

        if (ACTION_READ_FILE.equals(action)) {
            return handleFileOperation("read", intent);
        }

        if (ACTION_MKDIR.equals(action)) {
            return handleFileOperation("mkdir", intent);
        }

        if (ACTION_RENAME_FILE.equals(action)) {
            return handleFileOperation("rename", intent);
        }

        return "Unknown action: " + action;
    }

    /**
     * 运行脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleRunScript(Intent intent) {
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
                return "ERROR: Invalid base64 encoding";
            }
        }

        // 如果指定了路径，从文件读取
        if (!TextUtils.isEmpty(path) && TextUtils.isEmpty(script)) {
            try {
                script = FileProviderFactory.getProvider().read(path);
                if (script == null) {
                    return "ERROR: Failed to read file: " + path;
                }
                if (TextUtils.isEmpty(name)) {
                    name = new File(path).getName();
                }
            } catch (Exception e) {
                return "ERROR: Cannot read file: " + e.getMessage();
            }
        }

        if (TextUtils.isEmpty(script)) {
            return "ERROR: Missing 'script' or 'path' parameter";
        }

        // 构建JSON并委托给处理器
        JsonObject data = new JsonObject();
        data.addProperty("id", TextUtils.isEmpty(id) ? "adb_" + System.currentTimeMillis() : id);
        data.addProperty("script", script);
        data.addProperty("name", TextUtils.isEmpty(name) ? "" : name);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("run", data));

        boolean success = sHandler.handle(command);
        return success ? "OK: Script started" : "ERROR: Failed to start script";
    }

    /**
     * 停止脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleStopScript(Intent intent) {
        String id = intent.getStringExtra(EXTRA_ID);
        if (TextUtils.isEmpty(id)) {
            return "ERROR: Missing 'id' parameter";
        }

        JsonObject data = new JsonObject();
        data.addProperty("id", id);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("stop", data));

        boolean success = sHandler.handle(command);
        return success ? "OK: Script stopped, id=" + id : "ERROR: Failed to stop script";
    }

    /**
     * 保存脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleSaveScript(Intent intent) {
        String script = intent.getStringExtra(EXTRA_SCRIPT);
        String name = intent.getStringExtra(EXTRA_NAME);
        boolean isBase64 = getBooleanExtra(intent, EXTRA_BASE64, false);

        if (TextUtils.isEmpty(script)) {
            return "ERROR: Missing 'script' parameter";
        }

        // 解码 base64
        if (isBase64) {
            try {
                byte[] decoded = Base64.decode(script, Base64.DEFAULT);
                script = new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return "ERROR: Invalid base64 encoding";
            }
        }

        JsonObject data = new JsonObject();
        data.addProperty("script", script);
        data.addProperty("name", TextUtils.isEmpty(name) ? "" : name);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("save", data));

        boolean success = sHandler.handle(command);
        return success ? "OK: Script saved" : "ERROR: Failed to save script";
    }

    /**
     * 重运行脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleRerunScript(Intent intent) {
        String script = intent.getStringExtra(EXTRA_SCRIPT);
        String id = intent.getStringExtra(EXTRA_ID);
        String name = intent.getStringExtra(EXTRA_NAME);

        if (TextUtils.isEmpty(script)) {
            return "ERROR: Missing 'script' parameter";
        }

        JsonObject data = new JsonObject();
        data.addProperty("id", TextUtils.isEmpty(id) ? "adb_" + System.currentTimeMillis() : id);
        data.addProperty("script", script);
        data.addProperty("name", TextUtils.isEmpty(name) ? "" : name);

        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("rerun", data));

        boolean success = sHandler.handle(command);
        return success ? "OK: Script rerun" : "ERROR: Failed to rerun script";
    }

    /**
     * 列出运行中的脚本
     */
    private String handleListScripts() {
        try {
            Set<ScriptEngine> engines = AutoJs.getInstance()
                    .getScriptEngineService().getEngines();
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
            return "OK: No running scripts";
        }
    }

    /**
     * 停止所有脚本 - 委托给 DevPluginResponseHandler
     */
    private String handleStopAll() {
        JsonObject command = new JsonObject();
        command.addProperty("type", "command");
        command.add("data", buildCommandData("stopAll", new JsonObject()));

        boolean success = sHandler.handle(command);
        return success ? "OK: All scripts stopped" : "ERROR: Failed to stop all scripts";
    }

    /**
     * 文件操作（扩展功能）
     */
    private String handleFileOperation(String operation, Intent intent) {
        // 这些操作使用 FileProvider 直接处理
        IFileProvider provider = FileProviderFactory.getProvider();
        String path = intent.getStringExtra(EXTRA_PATH);

        if ("delete".equals(operation)) {
            if (TextUtils.isEmpty(path)) return "ERROR: Missing 'path' parameter";
            if (!provider.exists(path)) return "ERROR: File not found: " + path;
            boolean deleted = provider.delete(path);
            return deleted ? "OK: File deleted: " + path : "ERROR: Failed to delete";
        }

        if ("list".equals(operation)) {
            if (TextUtils.isEmpty(path)) {
                path = PFiles.getSdcardPath() + "/脚本";
            }
            if (!provider.exists(path)) return "ERROR: Directory not found: " + path;
            java.util.List<IFileProvider.FileInfo> files = provider.listFiles(path);
            if (files == null || files.isEmpty()) {
                return "OK: Empty directory: " + path;
            }
            StringBuilder sb = new StringBuilder("OK: Files in " + path + ":\n");
            for (IFileProvider.FileInfo f : files) {
                sb.append("  ").append(f.isDirectory ? "[D] " : "[F] ").append(f.name).append("\n");
            }
            return sb.toString().trim();
        }

        if ("read".equals(operation)) {
            if (TextUtils.isEmpty(path)) return "ERROR: Missing 'path' parameter";
            if (!provider.exists(path)) return "ERROR: File not found: " + path;
            String content = provider.read(path);
            return content != null ? "OK: " + content : "ERROR: Failed to read file";
        }

        if ("mkdir".equals(operation)) {
            if (TextUtils.isEmpty(path)) return "ERROR: Missing 'path' parameter";
            boolean created = provider.mkdirs(path);
            return created ? "OK: Directory created: " + path : "ERROR: Failed to create directory";
        }

        if ("rename".equals(operation)) {
            String oldPath = intent.getStringExtra("oldpath");
            String newPath = intent.getStringExtra("newpath");
            if (TextUtils.isEmpty(oldPath) || TextUtils.isEmpty(newPath)) {
                return "ERROR: Missing 'oldpath' or 'newpath' parameter";
            }
            boolean renamed = provider.rename(oldPath, provider.getName(newPath));
            return renamed ? "OK: Renamed to " + newPath : "ERROR: Failed to rename";
        }

        return "ERROR: Unknown file operation: " + operation;
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
}
