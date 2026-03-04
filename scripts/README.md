# Auto.js ADB 调试工具

ADB 调试接口，支持通过 ADB 命令行管理脚本运行。

## 功能特性

### 脚本运行
- ✅ 运行脚本（支持中文和特殊字符，Base64 编码传输）
- ✅ 运行脚本文件
- ✅ 停止指定脚本
- ✅ 停止所有脚本
- ✅ 列出运行中的脚本
- ✅ 获取脚本控制台输出

### 文件管理
- ✅ 推送脚本到设备
- ✅ 读取文件内容
- ✅ 删除脚本文件
- ✅ 重命名/移动文件
- ✅ 创建目录
- ✅ 列出脚本目录文件
- ✅ 递归列出文件
- ✅ 树形显示目录结构

### 系统功能
- ✅ 检查应用状态
- ✅ 实时查看应用日志
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

### Python (跨平台)

```bash
# 运行脚本内容
python autojs-adb.py run "toast('Hello World')"

# 运行带中文的脚本
python autojs-adb.py run "console.log('中文测试');toast('你好');"

# 运行脚本文件
python autojs-adb.py run -f "/sdcard/脚本/test.js"

# 延迟运行脚本
python autojs-adb.py run -f "/sdcard/脚本/test.js" -d 1000

# 列出运行中的脚本
python autojs-adb.py list

# 停止所有脚本
python autojs-adb.py stop-all

# 停止指定脚本
python autojs-adb.py stop 0

# 推送脚本到设备
python autojs-adb.py push myscript "toast('Hello')"

# 列出脚本目录
python autojs-adb.py files

# 递归列出所有文件
python autojs-adb.py files -r

# 树形显示目录结构
python autojs-adb.py files --tree

# 列出指定目录
python autojs-adb.py files "/sdcard/脚本/支付宝"
python autojs-adb.py files -r "/sdcard/脚本/支付宝"
python autojs-adb.py files --tree "/sdcard/脚本/支付宝"

# 删除脚本文件
python autojs-adb.py delete "/sdcard/脚本/test.js"

# 读取文件内容
python autojs-adb.py read "/sdcard/脚本/test.js"

# 读取二进制文件（返回 base64）
python autojs-adb.py read "/sdcard/test.png" --base64

# 创建目录
python autojs-adb.py mkdir "/sdcard/脚本/新目录"

# 重命名/移动文件
python autojs-adb.py rename "/sdcard/脚本/old.js" "/sdcard/脚本/new.js"

# 获取脚本输出日志
python autojs-adb.py output 12345
python autojs-adb.py output 12345 --lines 100

# 检查应用状态
python autojs-adb.py ping

# 实时查看应用日志
python autojs-adb.py logcat

# 清除日志缓冲区后查看
python autojs-adb.py logcat -c
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
| `output` | `[id]` | 获取脚本控制台输出（可选指定ID） |
| `output` | `[id] -l <lines>` | 指定读取的日志行数 |
| `push` | `<name> <code>` | 推送脚本到设备（保存到 /sdcard/脚本/） |
| `delete` | `<path>` | 删除指定脚本文件 |
| `files` | `[path]` | 列出目录文件（默认 /sdcard/脚本/） |
| `files` | `-r [path]` | 递归列出所有文件 |
| `files` | `--tree [path]` | 树形显示目录结构 |
| `read` | `<path>` | 读取文件内容 |
| `read` | `<path> --base64` | 读取文件返回 Base64 编码 |
| `mkdir` | `<path>` | 创建目录（支持递归创建） |
| `rename` | `<oldpath> <newpath>` | 重命名/移动文件或目录 |
| `ping` | - | 检查应用状态和权限模式 |
| `logcat` | `[-c]` | 实时查看应用日志（Python 版本独有） |

## 权限说明

应用需要存储权限才能访问文件。Android 11+ 需要以下权限之一：

1. **完全访问模式** - `MANAGE_EXTERNAL_STORAGE` 权限
2. **SAF 目录授权** - 在应用内授权脚本目录访问

使用 `ping` 命令可查看当前权限模式：
```
PONG: org.autojs.autojs.coolapk v0.80.2-debug (mode: SAF 目录授权模式)
```

## 日志查看

查看应用日志（推荐使用 --pid 过滤）：
```bash
# 获取应用 PID 并查看日志
adb logcat -d -v time --pid=$(adb shell pidof org.autojs.autojs.coolapk)

# Windows PowerShell
$pid = (adb shell pidof org.autojs.autojs.coolapk).Trim(); adb logcat -d -v time --pid=$pid
```

查看 ADB 调试日志：
```bash
adb logcat -s AdbDebugReceiver:D
```

查看脚本控制台输出：
```bash
adb logcat -s AutoJS.Console:*
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

## 测试示例

### 运行脚本文件

```bash
# 发送广播运行脚本
adb shell am broadcast -n org.autojs.autojs.coolapk/org.autojs.autojs.external.receiver.AdbDebugReceiver -a org.autojs.autojs.coolapk.adb.RUN_SCRIPT --es path "/storage/emulated/0/脚本/adb_test.js"

# 返回结果
Broadcast completed: result=0, extras: Bundle[mParcelledData.dataSize=164]
```

### 日志输出

```
D/AdbDebugReceiver: Result: OK: Script started, id=1, path=/storage/emulated/0/脚本/adb_test.js
D/AutoJS.Console.log: ADB Test Success!
D/GlobalConsole: [Tmp.js]运行结束，用时0.019000秒
```

### SAF 模式文件读取

```
I/FileProviderFactory: Mode: SAF_DIRECTORY
D/SafFileProvider: read: path=/storage/emulated/0/脚本/adb_test.js, encoding=UTF-8
D/SafFileProvider: read: success, length=140 chars
```

## 版本历史

- v0.80.2 - 添加 ADB 调试接口
  - **脚本运行**: run, stop, stop-all, list, output
  - **文件管理**: push, read, delete, mkdir, rename, files
  - **系统功能**: ping, logcat
  - Base64 编码传输
  - SAF 兼容
  - 显式 Intent 支持
  - 已测试验证所有功能
