# Auto.js.HYB1996 构建修复进度

## 当前状态
- 分支: `temp-test-branch`
- 最新标签: `v0.85.7-alpha`
- 工作目录: `K:\msys64\home\ms900\Auto.js.HYB1996`

---

## 已完成的修复

### 1. Gradle 8.x AAR 依赖解析问题 (v0.85.2-alpha 失败)
**问题**: `flatDir` 仓库在 `dependencyResolutionManagement` 模式下不生效
**修复**: 将 AAR 依赖改为显式文件依赖
- `autojs/build.gradle`:
  - `opencv-3.4.3.aar` -> `api files('libs/opencv-3.4.3.aar')`
  - `libtermexec-release.aar` -> `api files('libs/libtermexec-release.aar')`
  - `emulatorview-release.aar` -> `api files('libs/emulatorview-release.aar')`
  - `term-debug.aar` -> `api files('libs/term-debug.aar')`
- 移除了 `flatDir { dirs 'libs' }` 仓库配置

### 2. JitPack 依赖问题 (v0.85.3-alpha, v0.85.4-alpha 失败)
**问题**: jcenter 废弃后部分依赖找不到
**修复**: 更新为 JitPack 格式
- `recyclerview-flexibledivider` -> `com.github.yqritc:RecyclerView-FlexibleDivider:1.4.0`
- `AVLoadingIndicatorView` -> `com.github.HarlonWang:AVLoadingIndicatorView:2.1.3`
- `expandablerecyclerview` -> `com.github.thoughtbot:expandable-recycler-view:1.4`
- `Flurry Analytics` 更新为 `14.0.0` (Maven Central)

### 3. Flurry 仓库冲突 (v0.85.4-alpha 失败)
**问题**: Flurry JFrog 仓库对 JitPack 路径返回错误响应
**修复**: 移除 Flurry JFrog 仓库，使用 Maven Central

### 4. hyb1996 库无法下载 (v0.85.5-alpha, v0.85.6-alpha 失败)
**问题**: JitPack 上 hyb1996 的库无法下载
**修复**: 使用本地缓存的 AAR 文件
- `EnhancedFloaty-0.31.aar` -> `autojs/libs/`
- `settingscompat-1.1.5.aar` -> `common/libs/` (后移除)
- `MutableTheme-1.0.0.aar` -> `app/libs/` (后移除)
- `android-multi-level-listview-1.1.aar` -> `app/libs/`

### 5. 移除未使用依赖 (v0.85.7-alpha)
**修复**: 移除未使用的库，用系统 API 替代
- 移除 `MutableTheme-1.0.0.aar` (未被使用)
- 移除 `settingscompat-1.1.5.aar`，替代方案：
  - `SettingsCompat.canDrawOverlays()` -> `Settings.canDrawOverlays()`
  - `SettingsCompat.manageWriteSettings()` -> `Settings.ACTION_MANAGE_WRITE_SETTINGS`
- 简化 `FloatingPermission.java`，移除 MIUI V10 特殊处理

---

## 当前本地 AAR 依赖

| 文件 | 位置 | 用途 | 来源 |
|------|------|------|------|
| `opencv-3.4.3.aar` | autojs/libs/ | 图像处理 | 原项目自带 |
| `libtermexec-release.aar` | autojs/libs/ | 终端执行 | 原项目自带 |
| `emulatorview-release.aar` | autojs/libs/ | 终端视图 | 原项目自带 |
| `term-debug.aar` | autojs/libs/ | 终端调试 | 原项目自带 |
| `rhino-all-2.0.0-SNAPSHOT.jar` | autojs/libs/ | JS引擎 | 本地构建 |
| `EnhancedFloaty-0.31.aar` | autojs/libs/ | 悬浮窗 | 本地缓存 |
| `android-multi-level-listview-1.1.aar` | app/libs/ | 多级列表 | 本地缓存 |

---

## 备份文件
位置: `K:\msys64\home\ms900\backup_libs_AutoJS\`
- `settingscompat-1.1.5.aar`
- `android-multi-level-listview-1.1.aar`

---

## 仓库配置 (settings.gradle)
```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url "https://jitpack.io" }
        maven { url 'https://oss.sonatype.org/content/repositories/snapshots/' }
    }
}
```

---

## 构建命令

### 本地构建 (隔离环境)
```powershell
$env:JAVA_HOME = "F:\AIDE\jbr"
$env:GRADLE_USER_HOME = "F:\AIDE\.gradle"
$env:TEMP = "F:\AIDE\tmp"
$env:TMP = "F:\AIDE\tmp"
.\gradlew assembleCoolapkDebug --parallel
```

### GitHub Actions
- 触发: 推送标签 `v*.*.*`
- 工作流: `.github/workflows/android-ci.yml`

---

## 版本管理规则
- 格式: `v<major>.<minor>.<patch>[-<stage>]`
- 预发布阶段: `alpha` → `beta` → `rc`
- 大版本号不变时，补丁号自动+1
- 当前版本: `v0.85.x` 系列

---

## 待检查问题
- [ ] GitHub Actions v0.85.7-alpha 构建结果
- [ ] 可能还需要处理的其他 JitPack 依赖问题

---

*最后更新: 2026-03-09*
