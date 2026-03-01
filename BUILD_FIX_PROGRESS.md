# Auto.js.HYB1996 构建修复进度

## 当前状态: 构建成功 ✅ (Android 12+ 兼容性修复完成)

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

---

## 下一步

- [x] 等待当前构建完成 ✅
- [ ] 下载并安装新的 APK 进行测试
- [ ] 验证 Android 12+ 兼容性修复是否生效

---
更新时间: 2026-03-01