# Auto.js.HYB1996 构建修复进度

## 当前状态: 构建成功，签名问题待修复

### 已完成的修复

#### 1. Java 堆内存问题 ✅
- **问题**: GitHub Actions 构建时出现 `Java heap space` 错误
- **修复**: 修改 `gradle.properties`
  ```
  -Xms512m -Xmx1024m  →  -Xms1024m -Xmx4096m
  ```

#### 2. Keystore 签名配置 ✅
- **问题**: `Keystore file not set for signing config release`
- **修复**: 修改 `app/build.gradle` 添加 fallback 逻辑
  - 当 release keystore 不存在时回退到 debug keystore
  - 支持从环境变量读取签名配置

#### 3. GitHub Secrets 配置 ✅
- 创建了新的 release keystore:
  - 文件: `app/release-keystore.jks`
  - Alias: `autojs-release`
  - 密码: `autojs123456`
  - 证书: `CN=AutoJS Release, OU=Release, O=AutoJS, L=Beijing, ST=Beijing, C=CN`
- 已上传到 GitHub Secrets:
  - `SIGNING_KEYSTORE_BASE64` - keystore 的 base64 编码
  - `SIGNING_KEY_ALIAS` = `autojs-release`
  - `SIGNING_KEY_PASSWORD` = `autojs123456`
  - `SIGNING_STORE_PASSWORD` = `autojs123456`

### 当前问题: Release APK 仍使用 Debug 签名

#### 问题分析
1. APK 检查结果:
   - 文件大小: 17.6 MB
   - 结构: 完整，可正常解压
   - 签名证书: `CN=AutoJS Debug, OU=Debug` (应该是 Release)

2. 根本原因:
   - `build.gradle` 第 74-80 行的条件判断:
     ```groovy
     if (signingKeystorePassword && signingKeystoreFile && file(signingKeystoreFile).exists()) {
         signingConfig signingConfigs.release
     } else {
         signingConfig signingConfigs.debug
     }
     ```
   - Gradle 配置阶段评估时，keystore 文件可能尚未从 base64 解码
   - 导致 `file(signingKeystoreFile).exists()` 返回 false，回退到 debug 签名

#### 待修复方案 ~~已修复~~

#### 问题根源分析
1. **路径解析问题**: `app/build.gradle` 中的 `file()` 方法相对于 `app/` 目录解析，但环境变量 `KEYSTORE_FILE=app/release-keystore.jks` 是相对于项目根目录的路径
2. **条件判断不健壮**: `file(signingKeystoreFile).exists()` 可能因路径解析问题返回 false

#### 修复内容 (2026-03-01)

**1. `app/build.gradle` 修改:**
- 添加 `resolveKeystoreFile` 函数，智能解析 keystore 文件路径：
  - 支持绝对路径
  - 支持相对于项目根目录的路径
  - 支持相对于 app 模块的路径
- 预先计算 `useReleaseSigning` 变量
- 添加调试日志输出签名配置状态

**2. `.github/workflows/android.yml` 修改:**
- 添加 "Debug signing config" 步骤，在构建前验证：
  - Keystore 文件是否存在
  - 环境变量是否正确设置
- 改进 keystore 解码步骤的日志输出

### 构建记录

| 版本 | 状态 | 签名 | 备注 |
|------|------|------|------|
| v1.0.0-test.5 | 成功 | Debug | 签名配置未生效 |

### 文件修改记录

1. `gradle.properties` - 增加 JVM 内存
2. `app/build.gradle` - 添加签名配置 fallback 逻辑
3. `.github/workflows/android.yml` - 添加环境变量传递
4. `app/release-keystore.jks` - 新建的 release keystore

### 下一步

- [x] 修复签名问题，确保 release 构建使用正确的 release keystore
- [ ] 重新构建并发布 v1.0.0-test.6 验证修复

---
更新时间: 2026-03-01