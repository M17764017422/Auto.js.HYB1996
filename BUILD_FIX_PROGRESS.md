# Auto.js.HYB1996 构建修复进度

## 当前状态: ✅ Release APK 构建成功，签名验证通过

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
- [x] 更新 SIGNATURE 常量为正确值
- [x] 推送修复 (v4.1.1-alpha4)
- [ ] 等待构建完成并测试

---

## 问题总结与经验教训

### 1. FLAG_IMMUTABLE 问题 ✅ 已修复
- **问题**: android-job 库不兼容 Android 12+
- **解决**: try-catch 捕获异常
- **教训**: 检查第三方库的维护状态和 Android 兼容性

### 2. SDK 版本不一致问题 ✅ 已修复
- **问题**: 部分模块硬编码 SDK 版本
- **解决**: 统一使用 `versions.compile` 变量
- **教训**: 全局搜索 `compileSdkVersion` 确保一致性

### 3. 签名验证问题 ✅ 已修复
- **问题**: Release 版本签名验证失败导致闪退
- **原因**: 
  - 代码中硬编码原始签名
  - APK v1/v2 签名方案差异导致 SHA-256 值不同
- **解决**: 更新 SIGNATURE 常量为 PackageManager 实际返回的值
- **教训**:
  - Fork 项目时需检查签名验证逻辑
  - Debug vs Release 行为差异可能来自 `BuildConfig.DEBUG` 条件
  - 使用 `numActivities` 和 `finishing` 标记追踪 Activity 生命周期
  - **关键**: `keytool -printcert` 返回 v2 签名，`PackageManager.GET_SIGNATURES` 返回 v1 签名

### 4. APK 签名方案差异 (重要发现)

#### 问题背景
Android APK 有多种签名方案：
| 方案 | 引入版本 | 说明 |
|------|----------|------|
| v1 (JAR Signing) | Android 1.0 | 传统签名，基于 META-INF |
| v2 (APK Signature Scheme) | Android 7.0 | 更快的验证速度 |
| v3 (APK Signature Scheme v3) | Android 9.0 | 支持密钥轮替 |

#### 签名值差异
| 获取方式 | 返回签名 | SHA-256 示例 |
|----------|----------|--------------|
| `keytool -printcert -jarfile xxx.apk` | v2 签名证书 | `F7BF336527...` |
| `PackageManager.GET_SIGNATURES` | v1 签名证书 | `F7BF335F6527...` |
| `PackageManager.GET_SIGNING_CERTIFICATES` (API 28+) | v2/v3 签名 | 与 keytool 相同 |

#### 关键代码
```java
// 旧方法 - 返回 v1 签名 (已废弃)
PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
Signature[] signatures = info.signatures;

// 新方法 - 返回 v2/v3 签名 (API 28+)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
    SigningInfo signingInfo = info.signingInfo;
    Signature[] signatures = signingInfo.getApkContentsSigners();
}
```

#### 经验总结
1. **计算签名常量时**，必须使用与应用代码相同的获取方式
2. **推荐做法**：运行应用打印实际签名值，而非用 keytool 计算
3. **调试方法**：添加日志 `Log.d(TAG, "Signature SHA: " + sha)` 获取正确值

### 5. 分析方法论
1. 对比 Debug vs Release 日志差异
2. 追踪 `numActivities` 变化定位 Activity 销毁时机
3. 搜索 `BuildConfig.DEBUG` 条件分支
4. 检查签名验证相关代码
5. 添加调试日志输出实际签名值

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

## 第六阶段: 签名验证修复 ✅

### 问题深入分析

#### APK 签名方案差异
Android APK 有多种签名方案：
- **v1 (JAR Signing)**: 传统签名，`PackageManager.GET_SIGNATURES` 返回此签名
- **v2/v3 (APK Signature Scheme)**: 新签名方案，`keytool -printcert` 读取此签名

#### 签名 SHA-256 差异
| 来源 | SHA-256 (hex) | Base64 |
|------|---------------|--------|
| keytool (APK v2) | `F7BF336527...` | `978zZSfKn48n...` |
| PackageManager (v1) | `F7BF335F6527...` | `978zX2Unyp+P...` |

差异原因：两种签名方案返回的证书信息略有不同。

#### 日志验证
```
D/DeveloperUtils: Current signature SHA: 978zX2Unyp+PJw02HL4K89vi+ppMuIIzvpG8wfmted0=
D/DeveloperUtils: Expected signature SHA: 978zZSfKn48nDTYcvgrz2+L6mky4gjO+kbzB+a153Q==
```

### 修复方案

**最终修复**: 更新 SIGNATURE 常量为 PackageManager 实际返回的值

**文件**: `common/src/main/java/com/stardust/util/DeveloperUtils.java`

```java
// 更新前 (keytool 计算的值)
private static final String SIGNATURE = "978zZSfKn48nDTYcvgrz2+L6mky4gjO+kbzB+a153Q==";

// 更新后 (PackageManager 返回的实际值)
private static final String SIGNATURE = "978zX2Unyp+PJw02HL4K89vi+ppMuIIzvpG8wfmted0=";
```

### 版本发布
- **Tag**: `v4.1.1-alpha4`
- **Commit**: `b60d8290`
- **状态**: 构建中

---

## 第七阶段: 隔离构建环境配置 ✅

### 创建文件
1. **ISOLATED_BUILD_GUIDE.md** - 隔离构建环境说明文档
2. **setup-isolated-env.ps1** - PowerShell 环境配置脚本

### 隔离环境结构
```
F:\AIDE\                     # 隔离环境根目录
├── sdk\                     # Android SDK
│   ├── build-tools\         # 28.0.3, 36.1.0
│   ├── platforms\           # android-28, android-36
│   └── platform-tools\      # adb, fastboot
├── gradle\distributions\    # Gradle 6.1.1
├── jbr\                     # JetBrains Runtime 17 (JDK)
├── maven-repo\              # 本地 Maven 仓库
├── .gradle\                 # Gradle 缓存
└── .android\                # Android 配置
```

### 配置脚本功能
- 自动设置环境变量 (ANDROID_SDK_ROOT, GRADLE_USER_HOME, JAVA_HOME)
- 自动检测 JDK (jbr/jdk-17/jdk-11)
- 创建必要目录
- 生成 Gradle init 脚本 (阿里云镜像)
- 支持 `-Offline` 离线构建模式
- 支持 `-Persist` 持久化环境变量

---

## 打包系统分析 ✅

### 系统架构
```
┌─────────────────────────────────────────────────────────────────────┐
│                    Auto.js 打包系统架构                              │
├─────────────────────────────────────────────────────────────────────┤
│  1. apkbuilder 模块 (Java 库)                                        │
│     └── ApkBuilder.java, ApkPackager.java, ManifestEditor.java      │
│                                                                     │
│  2. inrt 模块 (Android 应用)                                        │
│     └── 编译产物 inrt-*.apk 作为打包模板 (template.apk)             │
│                                                                     │
│  3. ApkBuilderPlugin (缺失!)                                        │
│     └── 包名: org.autojs.apkbuilderplugin (原作者已删除)            │
│                                                                     │
│  4. 主应用 (app 模块)                                               │
│     └── BuildActivity.java 调用 ApkBuilderPluginHelper              │
└─────────────────────────────────────────────────────────────────────┘
```

### 当前状态
| 组件 | 状态 | 说明 |
|------|------|------|
| inrt 模块 | ✅ 存在 | 已编译生成 APK |
| apkbuilder 模块 | ✅ 存在 | 打包工具库 |
| ApkBuilderPlugin | ❌ 缺失 | 原作者已删除，需自行构建 |
| inrt Release APK | ⚠️ 未签名 | 需要签名配置 |

### 注意事项
- 打包功能需要额外安装 ApkBuilderPlugin 插件
- 插件签名必须与主应用匹配
- `inrt-apk.zip` 中的 Release APK 未签名

---

## 构建记录

| Commit/Tag | 状态 | 说明 |
|------------|------|------|
| `10cc0192` | ✅ | 签名配置修复 |
| `31b11e66` | ❌ | android-job 更新 (SDK 版本不匹配) |
| `f153c920` | ❌ | SDK 版本更新 (仅 project-versions.json) |
| `34bc2bf6` | ✅ | 所有模块统一使用 versions.compile |
| `320b0485` | ✅ | try-catch 捕获 FLAG_IMMUTABLE 异常 |
| `d25b5756` | ✅ | 添加签名调试日志 |
| `4a5daf90` | ✅ | 更新 SIGNATURE (第一次尝试，值不正确) |
| `b60d8290` / `v4.1.1-alpha4` | ✅ | 修正 SIGNATURE 为 PackageManager 返回值 |

---

## 最终验证结果 ✅

### 签名验证日志
```
D/DeveloperUtils: Current signature SHA: 978zX2Unyp+PJw02HL4K89vi+ppMuIIzvpG8wfmted0=
D/DeveloperUtils: Expected signature SHA: 978zX2Unyp+PJw02HL4K89vi+ppMuIIzvpG8wfmted0=
```

### Activity 状态
```
mResumedActivity: ActivityRecord{...MainActivity_}
numActivities=2 (MainActivity + 权限对话框)
无 finishing 标记
```

### 结论
- ✅ Release APK 正常启动
- ✅ 签名验证通过
- ✅ MainActivity 正常运行
- ✅ 权限请求正常处理

---

## 后续待办

- [ ] 迁移到 WorkManager 替代 android-job
- [ ] 创建 ApkBuilderPlugin 项目恢复打包功能
- [ ] 考虑移除或改进签名验证逻辑（开源项目意义不大）
- [ ] **彻底清理 Git 历史中的敏感文件** (重要!)
  - 问题: 在升级 Rhino 过程中，意外将敏感文件提交到 Git 历史
  - 涉及文件:
    - `release-keystore-base64.txt` (签名密钥 Base64)
    - `logs_*/` 目录 (构建日志)
    - `未确认 *.crdownload` (临时文件)
  - 当前状态: 已从最新提交中移除，但历史记录中仍存在
  - 清理方法: 使用 `git filter-repo` 或 BFG Repo-Cleaner 彻底删除
  - 清理命令参考:
    ```bash
    # 使用 git-filter-repo
    git filter-repo --path release-keystore-base64.txt --invert-paths
    git filter-repo --path-glob 'logs_*' --invert-paths
    
    # 或使用 BFG (更快)
    bfg --delete-files release-keystore-base64.txt
    ```
  - 注意: 清理后所有 commit hash 会改变，需要强制推送

---

## 第八阶段: Rhino 引擎升级 ✅

### 升级完成
- **原版本**: Rhino 1.7.7.2 (本地 JAR)
- **新版本**: Rhino 1.7.14 (Maven Central)
- **Maven 坐标**: `org.mozilla:rhino:1.7.14`

### 修改内容

**文件**: `autojs/build.gradle`
```groovy
// 旧配置
api files('libs/rhino-1.7.7.2.jar')

// 新配置
api 'org.mozilla:rhino:1.7.14'
```

### 版本对比

| 项目 | Rhino 版本 | 来源方式 | JDK 要求 |
|------|-----------|----------|----------|
| Auto.js (TonyJiangWJ) | 1.7.14-jdk7 + 1.9.1 | 本地 JAR + Maven | JDK 7+ / 11+ |
| Auto.js.HYB1996 | 1.7.14 ✅ | Maven Central | JDK 8+ |
| AutoX | 1.8.1 | Maven Central | JDK 11+ |

### 新增 ES6+ 特性支持

| 特性 | 1.7.7.2 (旧) | 1.7.14 (新) |
|------|--------------|-------------|
| Promise | ⚠️ 基础 | ✅ 完整支持 |
| BigInt | ❌ | ✅ |
| 模板字符串 | ✅ | ✅ |
| 箭头函数 | ✅ | ✅ |
| let/const | ✅ | ✅ |
| globalThis | ❌ | ✅ |
| Object.values/entries | ❌ | ✅ |
| Exponential operator `**` | ❌ | ✅ |
| for-of loop (Java Iterable) | ❌ | ✅ |

### 后续可选升级

- [ ] 评估升级到 Rhino 1.8.1 (需要 JDK 11+)
- [ ] 评估升级到 Rhino 1.9.1 (需要 JDK 11+，支持函数默认参数、扩展运算符)

---
更新时间: 2026-03-02 02:25

---

## 经验教训: Git 提交失误记录

### 事件概述
在 Rhino 升级过程中，由于多次使用 `git commit --amend` 修改提交，意外将以下文件包含到 Git 历史中：

| 文件 | 类型 | 风险等级 |
|------|------|----------|
| `release-keystore-base64.txt` | 签名密钥 | 🔴 高 |
| `logs_58933971828/` 等 | 构建日志 | 🟡 中 |
| `未确认 *.crdownload` | 临时文件 | 🟢 低 |

### 根本原因
1. `git add -A` 将所有未跟踪文件加入暂存区
2. `git commit --amend` 将敏感文件包含到提交中
3. 强制推送后，敏感文件被永久记录在 Git 历史中

### 预防措施
1. **始终检查 `.gitignore`** - 确保敏感文件模式已被忽略
2. **避免使用 `git add -A`** - 改用 `git add <具体文件>`
3. **提交前检查** - 使用 `git status` 和 `git diff --cached`
4. **敏感文件检测** - 考虑添加 pre-commit hook 检测敏感文件

### 已采取的补救措施
- 从最新提交中移除所有敏感文件
- 更新 `.gitignore` 添加更多忽略规则:
  ```
  # Release keystore - never commit release keystores
  app/release-keystore.jks
  *.jks
  !app/debug/*.jks
  
  # Build logs
  logs_*.zip
  logs_*/
  
  # Temporary files
  *.crdownload
  release-keystore-base64.txt
  ```
- 远程仓库当前分支已更新，敏感文件不在工作目录中

---

## 第九阶段: Android 11+ 存储权限支持 ✅

### 问题背景
Android 11 (API 30) 引入分区存储 (Scoped Storage)，应用无法直接访问外部存储。
- 原项目缺少 `MANAGE_EXTERNAL_STORAGE` 权限声明
- 应用启动后只能看到目录，无法看到文件

### 解决方案

#### 方案对比

| 方案 | 权限类型 | 兼容性 | 安全性 |
|------|----------|--------|--------|
| 完全访问 | MANAGE_EXTERNAL_STORAGE | ✅ 100% | 中等 |
| SAF 目录授权 | Storage Access Framework | ⚠️ 有限 | 高 |

#### 实现内容

**新增文件 (6个)**:

| 文件 | 模块 | 说明 |
|------|------|------|
| `IFileProvider.java` | common | 统一文件访问接口 |
| `TraditionalFileProvider.java` | common | 传统 File API 实现 |
| `StoragePermissionHelper.java` | app | 权限管理帮助类 |
| `SafFileProvider.java` | app | SAF 文件操作封装 |
| `SafFileProviderImpl.java` | app | SAF IFileProvider 实现 |
| `FileProviderFactory.java` | app | 文件提供者工厂 |

**修改文件 (5个)**:

| 文件 | 修改内容 |
|------|----------|
| `AndroidManifest.xml` | 添加 `MANAGE_EXTERNAL_STORAGE` 权限 |
| `MainActivity.java` | 新权限检查逻辑，支持两种授权方式 |
| `Pref.java` | 添加 SAF URI 存储方法 |
| `values/strings.xml` | 权限对话框文字 |
| `values-zh/strings.xml` | 中文权限对话框文字 |

### 权限策略

```
┌─────────────────────────────────────────────────────────┐
│                    Android 11+ 权限选择                   │
├─────────────────────────────────────────────────────────┤
│  [完全访问] ← 推荐                                        │
│     └─ MANAGE_EXTERNAL_STORAGE                          │
│     └─ JS 脚本 files.* API 完全兼容                      │
│                                                         │
│  [选择目录] ← 实验功能                                    │
│     └─ SAF 目录授权                                      │
│     └─ UI 可浏览文件                                     │
│     └─ JS 脚本文件操作受限                                │
└─────────────────────────────────────────────────────────┘
```

### 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    文件访问架构                              │
├─────────────────────────────────────────────────────────────┤
│                     IFileProvider (接口)                     │
│                           │                                 │
│            ┌──────────────┴──────────────┐                  │
│            ▼                              ▼                  │
│  TraditionalFileProvider         SafFileProviderImpl        │
│    (传统 File API)                 (SAF 实现)               │
│            ▲                              ▲                  │
│            │                              │                  │
│            └──────────────┬──────────────┘                  │
│                           ▼                                 │
│                  FileProviderFactory                        │
│                    (自动选择)                                │
└─────────────────────────────────────────────────────────────┘
```

### 提交记录

| Commit | 说明 |
|--------|------|
| `6a6bec3d` | feat: add Android 11+ storage permission support with SAF option |

### 已知限制

#### SAF 模式下 JS API 兼容性

| API | 传统模式 | SAF 模式 |
|-----|----------|----------|
| `files.read()` | ✅ | ⚠️ 需重构 |
| `files.write()` | ✅ | ⚠️ 需重构 |
| `files.listDir()` | ✅ | ⚠️ 需重构 |
| `files.exists()` | ✅ | ⚠️ 需重构 |
| 所有 PFiles 方法 | ✅ | ❌ 需重构 |

**原因**: 项目中有 154 处使用传统 File API，需要统一改为使用 IFileProvider 接口。

---

## 后续工作规划

### 高优先级

| 任务 | 状态 | 说明 |
|------|------|------|
| 重构 PFiles.java | 待开始 | 154 处文件操作需改用 IFileProvider |
| JS files API 适配 | 待开始 | 依赖 PFiles 重构 |
| Git 历史清理 | 待处理 | 删除敏感文件历史记录 |

### 中优先级

| 任务 | 状态 | 说明 |
|------|------|------|
| WorkManager 迁移 | 待处理 | 替代废弃的 android-job |
| ApkBuilderPlugin 构建 | 待处理 | 恢复打包功能 |
| 签名验证优化 | 待处理 | 考虑移除或改进 |

### 低优先级

| 任务 | 状态 | 说明 |
|------|------|------|
| Rhino 1.8.1 升级评估 | 待评估 | 需要 JDK 11+ |
| 代码规范化 | 待处理 | 统一代码风格 |

---

## PFiles.java 重构计划

### 重构范围

涉及 154 处传统 File API 调用，分布如下：

| 模块 | 文件数 | 说明 |
|------|--------|------|
| common | 4 | PFiles.java, PFile.java, PReadableTextFile.java, PWritableTextFile.java |
| autojs | 10 | ScriptRuntime.java, Files.java, Images.java 等 |
| app | 15+ | ScriptOperations.java, EditorView.java 等 |

### 重构策略

**方案 A: 渐进式重构 (推荐)**
1. 在 PFiles 中添加静态 IFileProvider 字段
2. 逐步修改方法使用 IFileProvider
3. 保持原有方法签名兼容
4. 完成后移除传统 File API 代码

**方案 B: 包装层**
1. 保持 PFiles 原有实现
2. 新增 PFilesEx 使用 IFileProvider
3. Files.java 根据权限模式选择实现

### 预计工作量

| 阶段 | 工作内容 | 预计改动 |
|------|----------|----------|
| 1 | PFiles 核心方法 | ~30 处 |
| 2 | PReadableTextFile/PWritableTextFile | ~15 处 |
| 3 | autojs 模块适配 | ~40 处 |
| 4 | app 模块适配 | ~70 处 |

### 风险评估

- **兼容性风险**: 中 - 需确保 JS 脚本行为一致
- **回归风险**: 高 - 需充分测试所有文件操作
- **性能风险**: 低 - SAF 模式下略有性能损失

---
更新时间: 2026-03-02 03:30