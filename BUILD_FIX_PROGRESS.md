# Auto.js.HYB1996 构建修复进度

## 当前状态: 构建中 (第四次修复 - try-catch 异常捕获)

---

## 第一阶段: 签名配置修复 ✅

### 1. Java 堆内存问题 ✅
- **问题**: GitHub Actions 构建时出现 `Java heap space` 错误
- **修复**: 修改 `gradle.properties`
  ```
  -Xms512m -Xmx1024m  →  -Xms1024m -Xmx4096m
  ```

### 2. Keystore 签名配置 ✅
- **问题**: `Keystore file not set for signing config release`
- **修复**: 修改 `app/build.gradle` 添加 fallback 逻辑
  - 当 release keystore 不存在时回退到 debug keystore
  - 支持从环境变量读取签名配置

### 3. GitHub Secrets 配置 ✅
- 创建了新的 release keystore:
  - 文件: `app/release-keystore.jks`
  - Alias: `autojs-release`
  - 密码: `autojs123456`
  - 证书: `CN=AutoJS Release, OU=Release, O=AutoJS, L=Beijing, ST=Beijing, C=CN`

### 4. 签名路径解析问题 ✅
- **问题**: `file()` 方法相对于 `app/` 目录解析，导致找不到 keystore
- **修复**: 添加 `resolveKeystoreFile` 函数智能解析路径

### 签名验证结果
- 证书类型: Release ✅
- 证书主题: `CN=AutoJS Release, OU=Release, O=AutoJS, L=Beijing, ST=Beijing, C=CN`

---

## 第二阶段: ADB 安装测试 ✅

### 连接方式
- **ADB 路径**: `F:\AIDE\platform-tools\adb.exe`
- **连接方式**: 无线调试 + 二维码配对
- **设备 IP**: 192.168.31.98
- **配对端口**: 42821, **连接端口**: 43341

### 安装结果
- Release APK 安装成功 ✅
- 应用启动后立即闪退 ❌

---

## 第三阶段: 闪退问题分析与修复 🔧

### 问题分析过程

#### 1. 日志收集
```bash
# 启动应用并收集日志
adb shell am start -n org.autojs.autojs/.ui.splash.SplashActivity
adb logcat -d -v time | grep -E "autojs|AutoJS|org.autojs"
```

#### 2. 关键错误发现
```
E/TransientBundleCompat: Targeting S+ (version 31 and above) requires that one of FLAG_IMMUTABLE or FLAG_MUTABLE be specified when creating a PendingIntent.
```

**错误来源**: `com.evernote:android-job:1.4.2` 库

#### 3. 崩溃链分析
1. 应用启动 → `TimedTaskScheduler.init()` 被调用
2. 触发 `JobManager.schedule()` 清理孤立 job
3. 创建 PendingIntent 时缺少 `FLAG_IMMUTABLE` (Android 12+ 强制要求)
4. 异常导致应用状态异常，MainActivity 被标记为 finishing
5. 应用闪退

### 修复内容

#### 修复 1: 更新 android-job 库
**文件**: `app/build.gradle`
```groovy
// 旧版本
implementation 'com.evernote:android-job:1.4.2'

// 新版本
implementation 'com.evernote:android-job:1.4.3'
implementation 'androidx.work:work-runtime:2.8.1'
```

#### 修复 2: 添加精确闹钟权限
**文件**: `app/src/main/AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM"/>
```

#### 修复 3: 升级 SDK 版本
**问题**: `androidx.work:work-runtime:2.8.1` 要求 `minCompileSdk 33`

**第一次尝试** (失败):
- 只更新了 `project-versions.json` 中的 `compile: 33`
- 构建失败: `:app:checkCommonDebugAarMetadata` 报错

**根本原因**: 6 个模块的 `compileSdkVersion` 硬编码为 31，未使用 `versions.compile` 变量

**最终修复**: 所有模块统一使用版本变量
```groovy
// 所有 build.gradle 文件
compileSdkVersion 31  →  compileSdkVersion versions.compile
```

**影响的模块**:
- `app/build.gradle`
- `common/build.gradle`
- `autojs/build.gradle`
- `automator/build.gradle`
- `inrt/build.gradle`
- `apkbuilder/build.gradle`

---

## 第四阶段: android-job FLAG_IMMUTABLE 最终修复 🔧

### 问题发现
- **测试结果**: 构建成功，但安装后仍然闪退
- **验证方法**: 检查日志发现 `TransientBundleCompat` 错误仍然存在
- **根本原因**: `android-job 1.4.3` 并未修复 `FLAG_IMMUTABLE` 问题，库已停止维护

### 最终修复方案
**文件**: `app/src/main/java/org/autojs/autojs/timing/TimedTaskScheduler.java`

添加 try-catch 捕获异常，防止应用启动崩溃：

```java
public static void init(@NotNull Context context) {
    try {
        JobManager.create(context).addJobCreator(tag -> { ... });
        new JobRequest.Builder(JOB_TAG_CHECK_TASKS)
                .setPeriodic(TimeUnit.MINUTES.toMillis(20))
                .build()
                .scheduleAsync();
        checkTasks(context, true);
    } catch (Exception e) {
        // android-job library has FLAG_IMMUTABLE compatibility issues on Android 12+
        Log.e(LOG_TAG, "Failed to initialize TimedTaskScheduler: " + e.getMessage());
    }
}
```

**影响**:
- 定时任务功能在 Android 12+ 上暂时不可用
- 应用可以正常启动和运行
- TODO: 后续迁移到 WorkManager

---

## 文件修改汇总

| 文件 | 修改内容 |
|------|----------|
| `gradle.properties` | JVM 内存: 512m/1024m → 1024m/4096m |
| `app/build.gradle` | 签名路径解析 + android-job 1.4.3 + work-runtime |
| `app/src/main/AndroidManifest.xml` | 添加 SCHEDULE_EXACT_ALARM 权限 |
| `project-versions.json` | compile/target: 31 → 33 |
| `common/build.gradle` | compileSdkVersion → versions.compile |
| `autojs/build.gradle` | compileSdkVersion → versions.compile |
| `automator/build.gradle` | compileSdkVersion → versions.compile |
| `inrt/build.gradle` | compileSdkVersion → versions.compile |
| `apkbuilder/build.gradle` | compileSdkVersion → versions.compile |
| `.github/workflows/android.yml` | 签名配置调试步骤 |
| `TimedTaskScheduler.java` | try-catch 捕获 FLAG_IMMUTABLE 异常 |

---

## 经验总结

### 1. 添加依赖前检查 SDK 要求
```bash
# 确认依赖的 minCompileSdk 要求
# 例如 androidx.work:work-runtime:2.8.1 要求 SDK 33
```

### 2. SDK 版本更新需全局检查
```bash
# 搜索所有 compileSdkVersion 硬编码
grep -r "compileSdkVersion" --include="*.gradle"
```

### 3. 统一使用版本变量
- 所有模块应使用 `versions.compile` 而非硬编码
- 只需修改 `project-versions.json` 一处即可全局生效

### 4. Android 12+ (API 31+) 兼容性要点
- PendingIntent 必须指定 `FLAG_IMMUTABLE` 或 `FLAG_MUTABLE`
- 精确闹钟需要 `SCHEDULE_EXACT_ALARM` 权限
- 过时的库 (如 android-job 1.4.2) 可能不兼容

### 5. 推送前先设置标签
- 工作流配置了 tag 触发构建
- 应先创建 tag 再推送，避免多次触发

---

## 构建记录

| Commit | 状态 | 说明 |
|--------|------|------|
| `10cc0192` | ✅ | 签名配置修复 |
| `31b11e66` | ❌ | android-job 更新 (SDK 版本不匹配) |
| `f153c920` | ❌ | SDK 版本更新 (仅 project-versions.json) |
| `34bc2bf6` | ✅ | 所有模块统一使用 versions.compile |
| `320b0485` | 🔄 | try-catch 捕获 FLAG_IMMUTABLE 异常 |

---

## 第五阶段: Debug vs Release 差异分析 ✅

### 问题现象
- **Debug APK**: 正常启动运行
- **Release APK**: 启动后立即闪退

### 分析过程

#### 1. 对比 Activity 数量变化
| 版本 | 时间点 | numActivities | 说明 |
|------|--------|---------------|------|
| Debug | 权限对话框出现 | 2 | MainActivity + GrantPermissionsActivity |
| Release | 权限对话框出现 | 1 | 只有 GrantPermissionsActivity |

#### 2. 关键日志发现
Release 版本:
```
20:00:07.827 - numActivities=2 (MainActivity 启动)
20:00:07.969 - MainActivity_ t5438 f}} (finishing 标记!)
20:00:08.123 - numActivities=1 (MainActivity 已销毁)
```

Debug 版本:
```
20:13:59.466 - numActivities=2 (MainActivity 启动)
20:13:59.995 - numActivities=2 (权限对话框出现，MainActivity 仍在)
```

#### 3. 根本原因定位
**文件**: `app/src/main/java/org/autojs/autojs/ui/main/MainActivity.java:260-263`

```java
@Override
protected void onStart() {
    super.onStart();
    if (!BuildConfig.DEBUG) {
        DeveloperUtils.verifyApk(this, R.string.dex_crcs);
    }
}
```

**文件**: `common/src/main/java/com/stardust/util/DeveloperUtils.java`

```java
public static void verifyApk(Activity activity, final int crcRes) {
    sExecutor.execute(new Runnable() {
        @Override
        public void run() {
            if (!checkSignature(a)) {
                a.finish();  // 签名验证失败 → 关闭 Activity
                return;
            }
        }
    });
}

public static boolean checkSignature(Context context, String packageName) {
    String sha = getSignatureSHA(context, packageName);
    return SIGNATURE.equals(sha);  // 比对硬编码的签名
}

private static final String SIGNATURE = "nPNPcy4Lk/eP6fLvZitP0VPbHdFCbKua77m59vis5fA=";
```

### 问题根源
1. Release 版本调用 `DeveloperUtils.verifyApk()` 进行签名验证
2. 验证方法比对 APK 签名与硬编码的 `SIGNATURE` 常量
3. 新构建使用自定义 keystore 签名，与原始签名不匹配
4. 验证失败 → `activity.finish()` → MainActivity 被销毁 → 应用闪退

### 签名对比
| 版本 | 签名摘要 | 说明 |
|------|----------|------|
| 原始签名 | `nPNPcy4Lk/eP6fLvZitP0VPbHdFCbKua77m59vis5fA=` | 硬编码在代码中 |
| Debug APK | `5affef64` | debug keystore 签名 |
| Release APK | `f70f37d` | 自定义 release keystore 签名 |

### 解决方案

**方案 A (推荐)**: 禁用签名验证
```java
// DeveloperUtils.java - checkSignature 方法
public static boolean checkSignature(Context context, String packageName) {
    // Allow custom signatures for forked builds
    return true;
}
```

**方案 B**: 更新签名常量
```java
// 需要计算新签名的 SHA-256 Base64 值
private static final String SIGNATURE = "新的签名SHA256值";
```

### 当前修复状态
- [x] 添加调试日志输出当前签名 SHA 值
- [ ] 禁用签名验证或更新签名常量
- [ ] 等待构建完成并测试

---

## 问题总结与经验教训

### 1. FLAG_IMMUTABLE 问题 (已修复)
- **问题**: android-job 库不兼容 Android 12+
- **解决**: try-catch 捕获异常
- **教训**: 检查第三方库的维护状态和 Android 兼容性

### 2. SDK 版本不一致问题 (已修复)
- **问题**: 部分模块硬编码 SDK 版本
- **解决**: 统一使用 `versions.compile` 变量
- **教训**: 全局搜索 `compileSdkVersion` 确保一致性

### 3. 签名验证问题 (进行中)
- **问题**: Release 版本签名验证失败导致闪退
- **原因**: 代码中硬编码原始签名，自定义签名不匹配
- **教训**: 
  - Fork 项目时需检查签名验证逻辑
  - Debug vs Release 行为差异可能来自 `BuildConfig.DEBUG` 条件
  - 使用 `numActivities` 和 `finishing` 标记追踪 Activity 生命周期

### 4. 分析方法论
1. 对比 Debug vs Release 日志差异
2. 追踪 `numActivities` 变化定位 Activity 销毁时机
3. 搜索 `BuildConfig.DEBUG` 条件分支
4. 检查签名验证相关代码

---

## 文件修改汇总

| 文件 | 修改内容 |
|------|----------|
| `gradle.properties` | JVM 内存: 512m/1024m → 1024m/4096m |
| `app/build.gradle` | 签名路径解析 + android-job 1.4.3 + work-runtime |
| `app/src/main/AndroidManifest.xml` | 添加 SCHEDULE_EXACT_ALARM 权限 |
| `project-versions.json` | compile/target: 31 → 33 |
| `common/build.gradle` | compileSdkVersion → versions.compile |
| `autojs/build.gradle` | compileSdkVersion → versions.compile |
| `automator/build.gradle` | compileSdkVersion → versions.compile |
| `inrt/build.gradle` | compileSdkVersion → versions.compile |
| `apkbuilder/build.gradle` | compileSdkVersion → versions.compile |
| `.github/workflows/android.yml` | 签名配置调试步骤 |
| `TimedTaskScheduler.java` | try-catch 捕获 FLAG_IMMUTABLE 异常 |
| `DeveloperUtils.java` | 添加签名调试日志 + 禁用签名验证 (待推送) |

---

## 构建记录

| Commit | 状态 | 说明 |
|--------|------|------|
| `10cc0192` | ✅ | 签名配置修复 |
| `31b11e66` | ❌ | android-job 更新 (SDK 版本不匹配) |
| `f153c920` | ❌ | SDK 版本更新 (仅 project-versions.json) |
| `34bc2bf6` | ✅ | 所有模块统一使用 versions.compile |
| `320b0485` | ✅ | try-catch 捕获 FLAG_IMMUTABLE 异常 |
| `d25b5756` | 🔄 | 添加签名调试日志 |
| 待推送 | ⏳ | 禁用签名验证 |

---

## 下一步

- [ ] 推送禁用签名验证的修改
- [ ] 等待构建完成
- [ ] 下载并安装新的 Release APK 进行测试
- [ ] 验证修复是否生效
- [ ] 后续: 迁移到 WorkManager 替代 android-job

---
更新时间: 2026-03-01