# Auto.js.HYB1996 调试指南

## 目录
1. [ADB 调试接口](#1-adb-调试接口)
2. [日志系统](#2-日志系统)
3. [常见问题排查](#3-常见问题排查)
4. [Debug vs Release 差异](#4-debug-vs-release-差异)
5. [SAF 模式调试](#5-saf-模式调试)
6. [签名验证调试](#6-签名验证调试)
7. [脚本调试技巧](#7-脚本调试技巧)

---

## 1. ADB 调试接口

### 1.0 ADB 环境配置

#### ADB 路径

| 来源 | 路径 |
|------|------|
| 隔离环境 | `F:\AIDE\sdk\platform-tools\adb.exe` |
| 系统 PATH | 可能已有其他 ADB（如 Android Studio） |

#### 设置 ADB 环境变量

**临时设置（当前会话）**:
```cmd
# 使用隔离环境 ADB
set PATH=F:\AIDE\sdk\platform-tools;%PATH%

# 验证
adb version
```

**PowerShell 临时设置**:
```powershell
$env:PATH = "F:\AIDE\sdk\platform-tools;" + $env:PATH
adb version
```

**永久设置（注册表）**:
```powershell
# 添加到用户 PATH
[Environment]::SetEnvironmentVariable(
    "PATH",
    "F:\AIDE\sdk\platform-tools;" + [Environment]::GetEnvironmentVariable("PATH", "User"),
    "User"
)
```

#### 验证 ADB 配置

```cmd
# 查看 ADB 版本和路径
adb version
where adb

# 预期输出（隔离环境）
# Android Debug Bridge version 1.0.41
# F:\AIDE\sdk\platform-tools\adb.exe
```

#### 无线调试连接

```cmd
# 使用完整路径连接（ADB 未加入 PATH 时）
F:\AIDE\sdk\platform-tools\adb.exe connect 192.168.31.98:38991

# 或使用已配置 PATH 的方式
adb connect 192.168.31.98:38991

# 验证连接
adb devices
# 输出: 192.168.31.98:38991    device
```

**首次配对**（设备显示配对码时）:
```cmd
adb pair 192.168.31.98:配对端口
# 输入设备上显示的配对码
```

### 1.1 支持的命令

| 命令 | 功能 | 示例 |
|------|------|------|
| `RUN_SCRIPT` | 运行脚本 | `adb shell am broadcast -a org.autojs.AUTOJS_ADB --es command RUN_SCRIPT --es path "/sdcard/test.js"` |
| `STOP_SCRIPT` | 停止脚本 | `--es command STOP_SCRIPT` |
| `STOP_ALL` | 停止所有脚本 | `--es command STOP_ALL` |
| `LIST_SCRIPTS` | 列出运行中脚本 | `--es command LIST_SCRIPTS` |
| `PUSH_SCRIPT` | 推送脚本 | `--es command PUSH_SCRIPT --es content "base64编码内容" --es name "test.js"` |
| `DELETE_SCRIPT` | 删除脚本 | `--es command DELETE_SCRIPT --es path "/sdcard/test.js"` |
| `LIST_FILES` | 列出文件 | `--es command LIST_FILES --es path "/sdcard"` |
| `PING` | 测试连接 | `--es command PING` |

### 1.2 使用脚本工具

项目提供了跨平台的 ADB 调试脚本：

```powershell
# PowerShell
.\scripts\autojs-adb.ps1 -Command RUN_SCRIPT -Path "/sdcard/test.js"

# Batch
.\scripts\autojs-adb.bat RUN_SCRIPT "/sdcard/test.js"

# Bash
./scripts/autojs-adb.sh RUN_SCRIPT "/sdcard/test.js"
```

### 1.3 Base64 编码传输

对于包含中文或特殊字符的内容，使用 Base64 编码：

```powershell
# PowerShell Base64 编码
$content = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("toast('你好')"))
adb shell am broadcast -a org.autojs.AUTOJS_ADB --es command PUSH_SCRIPT --es content $content --es name "test.js"
```

---

## 2. 日志系统

### 2.1 日志 TAG 规范

应用使用统一的 TAG 前缀便于过滤：

| 模块 | TAG 格式 | 示例 |
|------|----------|------|
| 文件操作 | `AutoJS.Files.*` | `AutoJS.Files.open`, `AutoJS.Files.read` |
| 控制台 | `AutoJS.Console.*` | `AutoJS.Console.log`, `AutoJS.Console.error` |
| SAF | `SafFileProvider` | `SafFileProvider.readBytes` |
| FileProvider 工厂 | `FileProviderFactory` | `FileProviderFactory.Mode` |
| 开发工具 | `DeveloperUtils` | `DeveloperUtils.Signature` |

### 2.2 日志过滤命令

```bash
# 过滤所有 AutoJS 日志
adb logcat | grep "AutoJS\."

# 过滤文件操作日志
adb logcat | grep "AutoJS\.Files"

# 过滤控制台日志
adb logcat | grep "AutoJS\.Console"

# 过滤错误级别日志
adb logcat *:E | grep -E "autojs|AutoJS|org\.autojs"

# 过滤特定方法
adb logcat | grep "AutoJS\.Files\.read"
adb logcat | grep "AutoJS\.Console\.error"
```

### 2.3 脚本日志到 Logcat

脚本中的 `console.log()` 等方法会自动输出到 Logcat：

```javascript
// 脚本代码
console.log("调试信息");
console.error("错误信息");

// Logcat 输出
// D/AutoJS.Console.log: 调试信息
// E/AutoJS.Console.error: 错误信息
```

### 2.4 实时日志监控

```bash
# 清除旧日志后开始监控
adb logcat -c && adb logcat -v time | grep -E "AutoJS|org\.autojs"

# 保存日志到文件
adb logcat -v time > debug_log.txt
```

---

## 3. 常见问题排查

### 3.1 应用启动闪退

#### 排查步骤

1. **收集崩溃日志**
   ```bash
   adb logcat -c
   adb shell am start -n org.autojs.autojs/.ui.splash.SplashActivity
   adb logcat -d -v time | grep -E "FATAL|AndroidRuntime|autojs"
   ```

2. **检查常见原因**

   | 错误信息 | 原因 | 解决方案 |
   |----------|------|----------|
   | `FLAG_IMMUTABLE` | android-job 不兼容 Android 12+ | 已修复，使用 try-catch |
   | `Signature verification failed` | 签名验证失败 | 更新 SIGNATURE 常量 |
   | `Permission denied` | 存储权限问题 | 检查 MANAGE_EXTERNAL_STORAGE 权限 |

3. **Activity 状态追踪**
   ```bash
   # 监控 Activity 数量变化
   adb logcat | grep "numActivities"
   
   # 检查 finishing 标记
   adb logcat | grep "finishing"
   ```

### 3.2 文件访问失败

#### 错误示例
```
Permission to access file: /storage/emulated/0/脚本/test.js is denied
```

#### 排查步骤

1. **检查存储模式**
   ```bash
   adb logcat | grep "FileProviderFactory"
   # 输出: Mode: SAF_DIRECTORY 或 Mode: FULL_ACCESS
   ```

2. **检查权限状态**
   ```bash
   adb shell appops get org.autojs.autojs MANAGE_EXTERNAL_STORAGE
   # 输出: allow 或 deny
   ```

3. **检查文件路径**
   - 完全访问模式：任意外部存储路径
   - SAF 模式：仅在授权目录内

### 3.3 脚本执行错误

#### 排查步骤

1. **查看脚本错误日志**
   ```bash
   adb logcat | grep -E "Rhino|JavaScript|ScriptException"
   ```

2. **常见错误类型**

   | 错误 | 原因 | 解决方案 |
   |------|------|----------|
   | `TypeError: ... is not a function` | API 调用方式错误 | 检查 API 文档 |
   | `ReferenceError: ... is not defined` | 变量未定义 | 检查变量声明 |
   | `JavaException` | Java 层异常 | 查看 Java 堆栈 |

---

## 4. Debug vs Release 差异

### 4.1 主要差异

| 项目 | Debug 版本 | Release 版本 |
|------|-----------|--------------|
| 签名验证 | 跳过 | 执行 |
| 日志输出 | 详细 | 精简 |
| 代码优化 | 无 | ProGuard/R8 |
| 调试信息 | 包含 | 移除 |

### 4.2 签名验证逻辑

**代码位置**: `MainActivity.java:260-263`
```java
@Override
protected void onStart() {
    super.onStart();
    if (!BuildConfig.DEBUG) {
        DeveloperUtils.verifyApk(this, R.string.dex_crcs);
    }
}
```

### 4.3 签名问题排查

1. **查看当前签名 SHA**
   ```bash
   adb logcat | grep "DeveloperUtils"
   # 输出: Current signature SHA: xxx
   # 输出: Expected signature SHA: xxx
   ```

2. **手动验证签名**
   ```powershell
   # 从 APK 提取签名
   keytool -printcert -jarfile app-release.apk
   
   # 计算 SHA-256 Base64
   # 注意：PackageManager.GET_SIGNATURES 返回 v1 签名
   # keytool 返回 v2 签名，两者可能不同
   ```

---

## 5. SAF 模式调试

### 5.1 SAF 模式识别

```bash
adb logcat | grep "FileProviderFactory"
```

输出示例：
```
I/FileProviderFactory: Mode: SAF_DIRECTORY
I/SafFileProvider: Created: treeUri=content://com.android.externalstorage.documents/tree/...
```

### 5.2 SAF 文件操作日志

```bash
adb logcat | grep "SafFileProvider"
```

输出示例：
```
D/SafFileProvider: readBytes: path=/storage/emulated/0/脚本/test.js
D/SafFileProvider: readBytes: success, size=1234 bytes
D/SafFileProvider: write: path=/storage/emulated/0/脚本/output.txt
D/SafFileProvider: write: success, size=567 bytes
```

### 5.3 SAF 常见错误

| 错误 | 原因 | 解决方案 |
|------|------|----------|
| `findDocumentId: part not found` | 路径不在 SAF 授权目录内 | 检查授权目录 |
| `EPERM (Operation not permitted)` | 无访问权限 | 重新授权目录 |
| `Callable returned null` | 文件不存在或路径错误 | 检查文件路径 |

### 5.4 应用私有目录

SAF 模式下，应用私有目录自动使用 TraditionalFileProvider：

```
/data/user/0/org.autojs.autojs/files/  → TraditionalFileProvider
/storage/emulated/0/脚本/              → SafFileProviderImpl
```

---

## 6. 签名验证调试

### 6.1 查看签名信息

```bash
# 查看签名验证日志
adb logcat | grep "DeveloperUtils"
```

### 6.2 签名不匹配问题

**现象**: Release 版本启动后立即退出

**排查**:
1. 检查日志中的签名 SHA 值
2. 对比 Current vs Expected
3. 更新 `DeveloperUtils.java` 中的 SIGNATURE 常量

### 6.3 APK 签名方案差异

| 获取方式 | 返回签名 | API 级别 |
|----------|----------|----------|
| `keytool -printcert` | v2 签名 | - |
| `GET_SIGNATURES` | v1 签名 | 已废弃 |
| `GET_SIGNING_CERTIFICATES` | v2/v3 签名 | API 28+ |

**注意**: 计算签名常量时，必须使用与应用代码相同的获取方式。

---

## 7. 脚本调试技巧

### 7.1 控制台调试

```javascript
// 基础日志
console.log("信息日志");
console.info("信息");
console.warn("警告");
console.error("错误");

// 断言
console.assert(condition, "断言失败");

// 计时
console.time("操作");
// ... 代码
console.timeEnd("操作");
```

### 7.2 异常捕获

```javascript
try {
    // 可能出错的代码
    files.read("/sdcard/notexist.txt");
} catch (e) {
    console.error("错误: " + e.message);
    console.error("堆栈: " + e.stack);
}
```

### 7.3 调试工具函数

```javascript
// 打印对象结构
function inspect(obj, depth) {
    depth = depth || 0;
    if (depth > 3) return "...";
    if (obj === null) return "null";
    if (obj === undefined) return "undefined";
    if (typeof obj !== "object") return String(obj);
    
    var result = "{\n";
    for (var key in obj) {
        result += "  ".repeat(depth + 1) + key + ": " + inspect(obj[key], depth + 1) + "\n";
    }
    result += "  ".repeat(depth) + "}";
    return result;
}

// 使用
console.log(inspect(someObject));
```

### 7.4 远程调试

通过 ADB 广播执行调试代码：

```bash
# 执行简单调试脚本
adb shell am broadcast -a org.autojs.AUTOJS_ADB \
    --es command PUSH_SCRIPT \
    --es content "Y29uc29sZS5sb2coJ2RlYnVnJyk7" \
    --es name "debug.js"
# content 是 console.log('debug'); 的 Base64 编码
```

---

## 附录

### A. 常用 ADB 命令

```bash
# 安装 APK
adb install app-debug.apk

# 启动应用
adb shell am start -n org.autojs.autojs/.ui.splash.SplashActivity

# 强制停止
adb shell am force-stop org.autojs.autojs

# 清除数据
adb shell pm clear org.autojs.autojs

# 查看权限
adb shell dumpsys package org.autojs.autojs | grep permission

# 截屏
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### B. 日志级别对照

| 方法 | Android Log | 级别 |
|------|-------------|------|
| `console.verbose()` | `Log.v()` | 最低 |
| `console.log()` | `Log.d()` | 低 |
| `console.info()` | `Log.i()` | 中 |
| `console.warn()` | `Log.w()` | 高 |
| `console.error()` | `Log.e()` | 最高 |

### C. 版本兼容性

| Android 版本 | API 级别 | 注意事项 |
|--------------|----------|----------|
| Android 10 | 29 | 分区存储开始引入 |
| Android 11 | 30 | 强制分区存储，需要 MANAGE_EXTERNAL_STORAGE |
| Android 12 | 31 | PendingIntent 需要 FLAG_IMMUTABLE |
| Android 13 | 33 | 通知权限变化 |

---

## 8. 开发构建环境配置

### 8.1 隔离环境目录结构

```
F:\AIDE\
├── jbr\                    # JDK 17 (JetBrains Runtime)
│   └── bin\java.exe
├── sdk\                    # Android SDK
│   ├── build-tools\        # 28.0.3, 36.1.0
│   ├── platforms\          # android-28, android-36
│   └── platform-tools\     # adb, fastboot
├── gradle\distributions\   # Gradle 安装目录
├── .gradle\                # Gradle 缓存
├── .android\               # Android 配置
└── maven-repo\             # 本地 Maven 仓库
```

### 8.2 环境变量配置

| 变量名 | 值 | 说明 |
|--------|-----|------|
| `JAVA_HOME` | `F:\AIDE\jbr` | JDK 17 根目录 |
| `ANDROID_SDK_ROOT` | `F:\AIDE\sdk` | Android SDK 根目录 |
| `GRADLE_USER_HOME` | `F:\AIDE\.gradle` | Gradle 用户目录 |
| `USERPROFILE` | `F:\AIDE\.userprofile` | 用户配置目录 |
| `TEMP` / `TMP` | `F:\AIDE\.temp` | 临时文件目录 |

### 8.3 环境验证命令

```cmd
# 验证环境变量
echo %ANDROID_SDK_ROOT%
echo %GRADLE_USER_HOME%
echo %JAVA_HOME%

# 验证 JDK 版本
java -version
# 预期: openjdk version "17.0.7"

# 验证 SDK 路径
dir %ANDROID_SDK_ROOT%\platforms
```

### 8.4 镜像加速配置

**重要：镜像必须放在仓库配置的最前面！**

**项目级 build.gradle**:
```groovy
repositories {
    // 阿里云镜像（必须放在最前面）
    maven { url 'https://maven.aliyun.com/repository/google' }
    maven { url 'https://maven.aliyun.com/repository/public' }
    maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
    
    // 备用镜像
    maven { url 'https://mirrors.cloud.tencent.com/nexus/repository/maven-public/' }
    
    // 原始仓库（最后兜底）
    google()
    mavenCentral()
}
```

**Gradle Wrapper 镜像** (gradle-wrapper.properties):
```properties
# 阿里云镜像下载 Gradle
distributionUrl=https\://mirrors.aliyun.com/macports/distfiles/gradle/gradle-7.5-all.zip
```

### 8.5 Gradle 配置 (gradle.properties)

```properties
# 隔离环境配置
org.gradle.user.home=F:/AIDE/.gradle
org.gradle.jvmargs=-Xms1024m -Xmx4096m -Duser.home=F:/AIDE

# 构建优化
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.daemon=true

# 注意：AGP 4.2.x 不支持 configuration-cache，必须禁用
```

### 8.6 AGP/Gradle 版本兼容性

| AGP 版本 | 兼容 Gradle 版本 | configuration-cache |
|----------|------------------|---------------------|
| 4.2.x | 6.7.1 - 7.5 | ❌ 不支持 |
| 7.0.x | 7.0+ | ⚠️ 实验性 |
| 7.2+ | 7.3.3+ | ✅ 支持 |
| 8.0+ | 8.0+ | ✅ 默认启用 |

**当前项目**：AGP 7.4.2 + Gradle 7.5

### 8.7 构建命令

```cmd
# 激活环境
call F:\AIDE\setup-env.bat

# 在线构建（首次下载依赖）
gradlew.bat assembleDebug

# 离线构建（使用缓存）
gradlew.bat assembleDebug --offline

# 清理构建
gradlew.bat clean

# 查看依赖树
gradlew.bat dependencies
```

### 8.8 镜像地址速查表

| 镜像源 | 地址 | 用途 |
|--------|------|------|
| 阿里云 Google | `maven.aliyun.com/repository/google` | Google Android 库 |
| 阿里云 Public | `maven.aliyun.com/repository/public` | 通用 Maven 库 |
| 阿里云 Central | `maven.aliyun.com/repository/central` | Maven Central |
| 阿里云 Gradle Plugin | `maven.aliyun.com/repository/gradle-plugin` | Gradle 插件 |
| 阿里云 JitPack | `maven.aliyun.com/repository/jitpack` | JitPack 镜像 |
| 腾讯云 | `mirrors.cloud.tencent.com/nexus/repository/maven-public/` | 备用镜像 |
| Gradle 发行版 | `mirrors.aliyun.com/macports/distfiles/gradle/` | Gradle 下载 |

### 8.9 隔离环境检查清单

构建前确认：
- ✅ `ANDROID_SDK_ROOT` 指向隔离环境
- ✅ `GRADLE_USER_HOME` 指向隔离环境
- ✅ `user.home` 设置为隔离路径
- ✅ 仓库配置：镜像在最前面
- ✅ C: 盘没有新的 Gradle/Android 文件
- ✅ Gradle/AGP 版本兼容

---

## 附录

### A. 常用 ADB 命令

```bash
# 安装 APK
adb install app-debug.apk

# 启动应用
adb shell am start -n org.autojs.autojs/.ui.splash.SplashActivity

# 强制停止
adb shell am force-stop org.autojs.autojs

# 清除数据
adb shell pm clear org.autojs.autojs

# 查看权限
adb shell dumpsys package org.autojs.autojs | grep permission

# 截屏
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png
```

### B. 日志级别对照

| 方法 | Android Log | 级别 |
|------|-------------|------|
| `console.verbose()` | `Log.v()` | 最低 |
| `console.log()` | `Log.d()` | 低 |
| `console.info()` | `Log.i()` | 中 |
| `console.warn()` | `Log.w()` | 高 |
| `console.error()` | `Log.e()` | 最高 |

### C. 版本兼容性

| Android 版本 | API 级别 | 注意事项 |
|--------------|----------|----------|
| Android 10 | 29 | 分区存储开始引入 |
| Android 11 | 30 | 强制分区存储，需要 MANAGE_EXTERNAL_STORAGE |
| Android 12 | 31 | PendingIntent 需要 FLAG_IMMUTABLE |
| Android 13 | 33 | 通知权限变化 |

### D. 项目版本信息

| 组件 | 版本 |
|------|------|
| AGP | 7.4.2 |
| Gradle | 7.5 |
| JDK | 17 (JetBrains Runtime) |
| Kotlin | 1.7.10 |
| Rhino | 1.7.14 |
| compileSdk | 33 |
| targetSdk | 33 |

---

更新时间: 2026-03-04
