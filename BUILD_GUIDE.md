# Auto.js 构建指南

## 目录
1. [环境准备](#环境准备)
2. [构建配置](#构建配置)
3. [签名配置](#签名配置)
4. [构建命令](#构建命令)
5. [构建产物](#构建产物)
6. [构建故障排除](#构建故障排除)

## 环境准备

### 系统要求
- **操作系统**: Windows 7/8/10/11, macOS, 或 Linux
- **内存**: 至少 8GB RAM (建议 16GB 或更多)
- **存储空间**: 至少 10GB 可用空间

### 必需软件

#### Java 开发环境
- **JDK 8** (推荐 OpenJDK 或 Oracle JDK)
  - 确保 `JAVA_HOME` 环境变量指向 JDK 安装目录
  - 验证命令: `java -version` 和 `javac -version`

#### Android 开发环境
- **Android Studio** 3.x 或更高版本 (包含 Android SDK)
- **Android SDK 版本**:
  - 编译 SDK 版本: API Level 28
  - 最低支持 SDK 版本: API Level 17
  - 目标 SDK 版本: API Level 28
- **Android SDK Build Tools**: 版本 28.0.3
- **Android NDK** (可选，如果需要编译原生代码)

#### Gradle 和构建工具
- **Gradle**: 4.10.2 (项目使用 Gradle Wrapper，无需单独安装)
- **Android Gradle Plugin**: 3.2.1

#### Git
- **Git**: 用于克隆项目和版本控制

### 环境变量配置
```bash
# 设置 JAVA_HOME 指向 JDK 安装目录
export JAVA_HOME=/path/to/jdk

# 将 JDK 的 bin 目录添加到 PATH
export PATH=$JAVA_HOME/bin:$PATH

# 设置 ANDROID_HOME 指向 Android SDK 目录 (如果未使用 Android Studio 默认路径)
export ANDROID_HOME=/path/to/android-sdk
```

## 构建配置

### 项目结构
Auto.js 采用多模块 Gradle 项目结构：
- `app`: 主应用程序模块 (application)
- `autojs`: JavaScript 运行时核心模块 (library)
- `automator`: 自动化操作模块 (library)
- `common`: 通用基础库模块 (library)
- `inrt`: 独立应用模块 (application)

### 模块依赖关系
```
app (application)
├── 依赖: :autojs, :automator, :common
├── 使用: Rhino JavaScript 引擎、Retrofit、Glide 等

autojs (library)
├── 依赖: :automator, :common
├── 使用: Rhino JavaScript 引擎、OkHttp、OpenCV 等

automator (library)
├── 依赖: :common
├── 提供: 控件自动化相关 API

common (library)
├── 无内部依赖
├── 提供: 通用工具类和基础功能

inrt (application)
├── 依赖: :autojs, :automator, :common
├── 与 app 模块有相似依赖结构
```

### 构建配置详情
- **Gradle 版本**: 4.10.2 (通过 Wrapper)
- **Android Gradle Plugin**: 3.2.1
- **Kotlin 版本**: 1.3.10
- **编译 SDK 版本**: 28
- **目标 SDK 版本**: 28
- **最低 SDK 版本**: 17
- **构建工具版本**: 28.0.3

### 多渠道构建
项目支持以下构建渠道：
- `common`: 通用渠道
- `coolapk`: 酷安渠道

### ABI 分包
项目构建以下架构的 APK:
- `armeabi-v7a`
- `x86`

### 特殊构建任务
- **inrt 模块**: 包含 `buildApkPlugin` 任务，用于构建 APK 打包插件
- **多 Dex 支持**: 启用 multidexEnabled 以支持大型应用
- **Java 8 支持**: 配置了 Java 1.8 的源码和目标兼容性

## 签名配置

### 当前签名状态
Auto.js 项目作为开源项目，不包含任何硬编码的签名配置，以确保安全性并避免暴露私钥信息。

### 开发构建
- **调试版本**: 使用 Android SDK 提供的默认调试密钥进行签名
- **开发环境**: 不需要额外的签名配置即可构建调试版本

### 发布构建签名配置
如果需要构建发布版本，需要自行配置签名信息。请参考以下步骤：

#### 1. 创建 Keystore 文件
使用 keytool 命令行工具创建密钥库文件：

```bash
keytool -genkey -v -keystore autojs-release-key.keystore -alias autojs_key_alias -keyalg RSA -keysize 2048 -validity 10000
```

执行该命令时，系统会提示您输入以下信息：
- **密钥库密码**: 设置一个强密码并记住它
- **重新输入密钥库密码**: 再次输入相同的密码
- **您的名字和姓氏**: 通常输入您的姓名或应用名称
- **组织单位名称**: 输入您的组织或团队名称
- **组织名称**: 输入您的公司或组织名称
- **城市或区域名称**: 输入您的城市名称
- **省份或州名称**: 输入您的省份或州名称
- **国家代码**: 输入两位字母的国家代码（如 CN 代表中国）
- **确认信息是否正确**: 检查信息无误后输入 'y'

#### 2. 验证密钥文件的有效性
创建密钥后，验证密钥文件是否正确生成：

```bash
# 查看密钥库中的条目
keytool -list -v -keystore autojs-release-key.keystore

# 查看特定别名的详细信息
keytool -list -v -keystore autojs-release-key.keystore -alias autojs_key_alias
```

验证时应确认以下信息：
- **密钥算法**: 应为 RSA
- **密钥大小**: 应为 2048 位或更大
- **有效期限**: 应为指定的天数（如 10000 天）
- **别名**: 应与构建配置中使用的别名一致
- **证书链长度**: 应显示正确的证书信息

#### 3. 配置签名信息
在 `app/build.gradle` 文件中添加以下配置：

```gradle
android {
    signingConfigs {
        release {
            storeFile file('path/to/your/keystore.jks')
            storePassword 'your_store_password'
            keyAlias 'your_key_alias'
            keyPassword 'your_key_password'
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            shrinkResources false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}
```

#### 4. 安全配置建议
- **安全存储**: 将 keystore 文件存储在安全位置，不要将其提交到版本控制系统
- **使用配置文件**: 使用环境变量或 `local.properties` 文件管理敏感信息：

```properties
# local.properties
MYAPP_RELEASE_STORE_FILE=../path/to/your/keystore.jks
MYAPP_RELEASE_STORE_PASSWORD=your_store_password
MYAPP_RELEASE_KEY_ALIAS=your_key_alias
MYAPP_RELEASE_KEY_PASSWORD=your_key_password
```

然后在 `build.gradle` 中引用：

```gradle
def keystoreProperties = new Properties()
def keystorePropertiesFile = rootProject.file('local.properties')
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(new FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        release {
            keyAlias keystoreProperties['MYAPP_RELEASE_KEY_ALIAS']
            keyPassword keystoreProperties['MYAPP_RELEASE_KEY_PASSWORD']
            storeFile keystoreProperties['MYAPP_RELEASE_STORE_FILE'] ? file(keystoreProperties['MYAPP_RELEASE_STORE_FILE']) : null
            storePassword keystoreProperties['MYAPP_RELEASE_STORE_PASSWORD']
        }
    }
    // ...
}
```

#### 5. 用于 GitHub Actions 的密钥准备
如果要在 GitHub Actions 中使用此密钥，需要将其转换为 Base64 格式：

```bash
# Linux/macOS
openssl base64 -in autojs-release-key.keystore -out autojs-release-key-base64.txt

# Windows (使用 Git Bash 或其他支持 base64 命令的环境)
certutil -encode autojs-release-key.keystore autojs-release-key-base64.txt
```

#### 使用 GitHub CLI 设置仓库 Secrets
使用 GitHub CLI 可以安全地设置仓库的 Actions Secrets，而无需在代码中暴露敏感信息：

```bash
# 设置 keystore 的 Base64 编码内容
gh secret set SIGNING_KEYSTORE_BASE64 -f autojs-release-key-base64.txt --repo <owner>/<repo>

# 或者直接提供值
gh secret set SIGNING_KEYSTORE_BASE64 -b'$(cat autojs-release-key-base64.txt)' --repo <owner>/<repo>

# 设置其他签名参数
gh secret set SIGNING_KEY_ALIAS -b'your_key_alias' --repo <owner>/<repo>
gh secret set SIGNING_KEY_PASSWORD -b'your_key_password' --repo <owner>/<repo>
gh secret set SIGNING_STORE_PASSWORD -b'your_store_password' --repo <owner>/<repo>
```

#### 手动设置 GitHub Secrets
1. 在 GitHub 仓库页面，转到 Settings > Secrets and variables > Actions
2. 点击 "New repository secret"
3. 添加以下 Secrets：
   - `SIGNING_KEYSTORE_BASE64`: Base64 编码的 keystore 文件内容
   - `SIGNING_KEY_ALIAS`: 签名密钥别名
   - `SIGNING_KEY_PASSWORD`: 签名密钥密码
   - `SIGNING_STORE_PASSWORD`: Keystore 密码
4. 点击 "Add secret" 保存每个 Secret

**重要安全提示**：
- 永远不要将密钥文件或敏感信息提交到版本控制系统
- 仅将 Base64 编码的密钥内容作为 GitHub Secrets 存储
- 定期轮换签名密钥以增强安全性
- 限制对 GitHub Secrets 的访问权限

## 构建命令

### 克隆项目
```bash
git clone https://github.com/hyb1996/Auto.js.git
cd Auto.js
```

### 基本构建命令
```bash
# 构建所有变体
./gradlew build

# 构建特定模块的所有变体
./gradlew :app:build
./gradlew :autojs:build
./gradlew :automator:build
./gradlew :common:build
./gradlew :inrt:build
```

### 构建特定版本
```bash
# 构建调试版本
./gradlew assembleDebug

# 构建发布版本
./gradlew assembleRelease

# 构建特定渠道的调试版本
./gradlew assembleCommonDebug
./gradlew assembleCoolapkDebug

# 构建特定渠道的发布版本
./gradlew assembleCommonRelease
./gradlew assembleCoolapkRelease

# 构建特定架构的 APK
./gradlew assembleCommonRelease --info
```

### 清理构建缓存
```bash
# 清理所有构建输出
./gradlew clean

# 清理特定模块
./gradlew :app:clean
```

### 运行测试
```bash
# 运行单元测试
./gradlew test

# 运行特定模块的测试
./gradlew :app:test
./gradlew :autojs:test
```

### 其他有用命令
```bash
# 查看所有可用任务
./gradlew tasks

# 构建并安装到设备
./gradlew installDebug
./gradlew installRelease

# 生成依赖报告
./gradlew app:dependencies
```

### Windows 系统命令
在 Windows 系统上，使用 `gradlew.bat` 替代 `./gradlew`：

```cmd
gradlew.bat build
gradlew.bat assembleRelease
gradlew.bat clean
```

## 构建产物

### APK 文件位置
构建完成后，APK 文件位于以下目录：

- **app 模块**:
  - 调试版本: `app/build/outputs/apk/debug/app-debug.apk`
  - 发布版本: `app/build/outputs/apk/release/app-{channel}-{abi}-release.apk`
    - `app-common-armeabi-v7a-release.apk`
    - `app-common-x86-release.apk`
    - `app-coolapk-armeabi-v7a-release.apk`
    - `app-coolapk-x86-release.apk`

- **inrt 模块**:
  - 调试版本: `inrt/build/outputs/apk/debug/inrt-debug.apk`
  - 发布版本: `inrt/build/outputs/apk/release/inrt-{abi}-release.apk`
    - `inrt-armeabi-v7a-release.apk`
    - `inrt-x86-release.apk`

### 其他构建产物
- **AAR 文件**: 各库模块的 AAR 文件位于 `build/outputs/aar/` 目录
- **插件文件**: 通过 `buildApkPlugin` 任务生成的打包插件位于 `common/release/` 目录
- **中间文件**: 各模块的 `build/` 目录包含编译、打包等中间文件

## 构建故障排除

### 常见问题及解决方案

#### 1. 内存不足错误
**问题**: `OutOfMemoryError` 或构建过程卡住
**解决方案**: 
- 在 `gradle.properties` 中增加内存配置：
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
```

#### 2. Gradle 下载缓慢
**问题**: Gradle Wrapper 下载缓慢或失败
**解决方案**:
- 配置 Gradle 镜像源或使用国内镜像
- 手动下载 Gradle 分发包到缓存目录

#### 3. Android SDK 路径错误
**问题**: `Android SDK not found` 错误
**解决方案**:
- 确保环境变量 `ANDROID_HOME` 或 `ANDROID_SDK_ROOT` 设置正确
- 或在 `local.properties` 中指定 SDK 路径：
```properties
sdk.dir=/path/to/android-sdk
```

#### 4. 依赖下载失败
**问题**: Gradle 同步时依赖下载失败
**解决方案**:
- 检查网络连接
- 在 `build.gradle` 中添加镜像仓库
- 清理 Gradle 缓存: `./gradlew clean` 和删除 `~/.gradle/caches/`

#### 5. 签名相关错误
**问题**: 构建发布版本时出现签名错误
**解决方案**:
- 确保已配置正确的签名信息
- 检查 keystore 文件路径和密码是否正确
- 确保 keyAlias 存在且有效

#### 6. Kotlin 版本冲突
**问题**: Kotlin 版本不兼容错误
**解决方案**:
- 确保项目中所有模块使用一致的 Kotlin 版本
- 检查 `build.gradle` 中的 `kotlin_version` 变量

#### 7. ABI 架构问题
**问题**: 构建特定架构时出错
**解决方案**:
- 检查 `build.gradle` 中的 splits 配置
- 确保 NDK 配置正确（如果需要原生库）

### 调试构建过程
```bash
# 启用详细输出
./gradlew build --info

# 启用调试模式
./gradlew build --debug

# 启用堆栈跟踪
./gradlew build --stacktrace
```

### 恢复默认状态
如果构建过程出现严重问题，可以执行以下操作恢复到干净状态：
```bash
# 清理所有构建输出
./gradlew clean

# 删除 Gradle 缓存
rm -rf ~/.gradle/caches/

# 重新同步项目
./gradlew build
```

## GitHub Actions 配置方案

### 可行性分析
Auto.js 项目完全支持使用 GitHub Actions 进行持续集成和构建，原因如下：

1. **项目兼容性**: Auto.js 是标准的 Android 项目，使用 Gradle 构建系统
2. **环境支持**: GitHub Actions 提供完整的 Android 构建环境
3. **构建复杂度**: 支持多模块、多渠道、多架构的复杂构建需求
4. **安全措施**: 可以使用 GitHub Secrets 安全地管理敏感信息
5. **制品管理**: 支持构建产物的上传和发布

### 工作流配置

项目包含以下 GitHub Actions 工作流配置：

#### 1. android-build.yml (基础构建)
- **触发条件**: 在 master 分支上的 push 和 pull_request 事件
- **功能**:
  - 设置 JDK 11 和 Android SDK 环境
  - 构建调试和发布版本的 APK
  - 运行单元测试
  - 上传构建产物作为制品

#### 2. android-release.yml (发布构建)
- **触发条件**: 在 master 分支上的 push 事件和标签创建事件
- **功能**:
  - 执行完整的构建和测试流程
  - 支持使用 GitHub Secrets 进行签名构建
  - 自动创建 GitHub Release
  - 上传构建产物到 Release

### 使用 GitHub Actions 进行签名构建

如果需要使用 GitHub Actions 构建签名的 APK，需要配置以下 GitHub Secrets:

1. **SIGNING_KEYSTORE_BASE64**: Base64 编码的 keystore 文件内容
2. **SIGNING_KEY_ALIAS**: 签名密钥别名
3. **SIGNING_KEY_PASSWORD**: 签名密钥密码
4. **SIGNING_STORE_PASSWORD**: Keystore 密码

配置步骤：
1. 将 keystore 文件转换为 Base64 格式：
   ```bash
   openssl base64 -in your-keystore.jks -out keystore-base64.txt
   ```
2. 将 Base64 内容复制到 `SIGNING_KEYSTORE_BASE64` Secret
3. 将其他签名信息分别配置到对应的 Secrets

### 本地测试 GitHub Actions
可以使用以下工具在本地测试 GitHub Actions 配置：
- **act**: 一个命令行工具，可以在本地运行 GitHub Actions
- 配置文件位于 `.github/workflows/` 目录

### 构建产物管理
GitHub Actions 会自动将构建产物作为制品上传，包括：
- 调试版本 APK
- 无签名发布版本 APK
- 签名发布版本 APK（如果配置了签名）
- 测试报告
- 构建日志