# Auto.js ADB 调试工具

ADB 调试接口，支持通过 ADB 命令行管理脚本运行。

## 功能特性

- ✅ 运行脚本（支持中文和特殊字符，Base64 编码传输）
- ✅ 运行脚本文件
- ✅ 停止指定脚本
- ✅ 停止所有脚本
- ✅ 列出运行中的脚本
- ✅ 推送脚本到设备
- ✅ 删除脚本文件
- ✅ 列出脚本目录文件
- ✅ 检查应用状态
- ✅ SAF (Storage Access Framework) 兼容

## 使用方法

### PowerShell (Windows)

```powershell
# 设置 ADB 路径
$env:PATH = "F:\AIDE\sdk\platform-tools;$env:PATH"

# 进入脚本目录
cd scripts

# 运行脚本内容
.\autojs-adb.ps1 run "toast('Hello World')"

# 运行带中文的脚本
.\autojs-adb.ps1 run "console.log('中文测试');toast('你好');"

# 运行脚本文件
.\autojs-adb.ps1 run -f "/sdcard/脚本/test.js"

# 列出运行中的脚本
.\autojs-adb.ps1 list

# 停止所有脚本
.\autojs-adb.ps1 stop-all

# 停止指定脚本
.\autojs-adb.ps1 stop 0

# 推送脚本到设备
.\autojs-adb.ps1 push myscript "toast('Hello')"

# 列出脚本目录
.\autojs-adb.ps1 files

# 列出指定目录
.\autojs-adb.ps1 files "/sdcard/脚本/支付宝"

# 删除脚本文件
.\autojs-adb.ps1 delete "/sdcard/脚本/test.js"

# 检查应用状态
.\autojs-adb.ps1 ping
```

### CMD (Windows)

```cmd
autojs-adb.bat run "toast('Hello')"
autojs-adb.bat run -f "/sdcard/脚本/test.js"
autojs-adb.bat list
autojs-adb.bat stop-all
autojs-adb.bat files
autojs-adb.bat ping
```

### Bash (Linux/Mac)

```bash
chmod +x autojs-adb.sh
./autojs-adb.sh run "toast('Hello')"
./autojs-adb.sh run -f "/sdcard/脚本/test.js"
./autojs-adb.sh list
./autojs-adb.sh stop-all
```

### 直接 ADB 命令

```bash
# 显式 Intent（推荐，Android 8+ 兼容）
adb shell am broadcast -n org.autojs.autojs.coolapk/org.autojs.autojs.external.receiver.AdbDebugReceiver -a org.autojs.autojs.coolapk.adb.RUN_SCRIPT --es script "toast('test')"

# 列出脚本
adb shell am broadcast -n org.autojs.autojs.coolapk/org.autojs.autojs.external.receiver.AdbDebugReceiver -a org.autojs.autojs.coolapk.adb.LIST_SCRIPTS

# 停止所有
adb shell am broadcast -n org.autojs.autojs.coolapk/org.autojs.autojs.external.receiver.AdbDebugReceiver -a org.autojs.autojs.coolapk.adb.STOP_ALL

# Ping
adb shell am broadcast -n org.autojs.autojs.coolapk/org.autojs.autojs.external.receiver.AdbDebugReceiver -a org.autojs.autojs.coolapk.adb.PING
```

## 命令详解

| 命令 | 参数 | 说明 |
|------|------|------|
| `run` | `<script>` | 运行脚本内容（自动 Base64 编码） |
| `run` | `-f <path>` | 运行指定路径的脚本文件 |
| `run` | `-f <path> -d <ms>` | 延迟运行脚本 |
| `stop` | `<id>` | 停止指定 ID 的脚本 |
| `stop-all` | - | 停止所有运行中的脚本 |
| `list` | - | 列出所有运行中的脚本 |
| `push` | `<name> <code>` | 推送脚本到设备（保存到 /sdcard/脚本/） |
| `delete` | `<path>` | 删除指定脚本文件 |
| `files` | `[path]` | 列出目录文件（默认 /sdcard/脚本/） |
| `ping` | - | 检查应用状态和权限模式 |

## 权限说明

应用需要存储权限才能访问文件。Android 11+ 需要以下权限之一：

1. **完全访问模式** - `MANAGE_EXTERNAL_STORAGE` 权限
2. **SAF 目录授权** - 在应用内授权脚本目录访问

使用 `ping` 命令可查看当前权限模式：
```
PONG: org.autojs.autojs.coolapk v0.80.1 (mode: 完全访问模式)
```

## 日志查看

查看 ADB 调试日志：
```bash
adb logcat -s AdbDebugReceiver
```

查看脚本控制台输出：
```bash
adb logcat -s AutoJS.Console
```

## 包名配置

默认使用 `org.autojs.autojs.coolapk` 包名。如需使用其他 flavor，修改脚本中的 `$PackageName` 变量：

- `org.autojs.autojs` - common flavor
- `org.autojs.autojs.coolapk` - coolapk flavor
- `org.autojs.autojs.github` - github flavor

## 技术细节

### Base64 编码

脚本内容使用 Base64 编码传输，解决中文和特殊字符的 Shell 转义问题。

### SAF 兼容

文件操作使用 `IFileProvider` 接口，支持：
- 传统 File API
- SAF (Storage Access Framework) 模式

### 显式 Intent

Android 8+ 对隐式广播有限制，使用显式 Intent 确保兼容性。

## 版本历史

- v0.80.2 - 添加 ADB 调试接口
  - 8 个命令支持
  - Base64 编码传输
  - SAF 兼容
  - 显式 Intent 支持
