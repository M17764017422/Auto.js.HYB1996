# Material3 迁移日志

## 概述

记录 Auto.js.HYB1996 项目引入 Material3 和 Jetpack Compose 的迁移过程。

---

## 迁移目标

为项目添加现代化 UI 支持：
- Jetpack Compose 声明式 UI
- Material3 设计系统
- ModalBottomSheet 日志面板
- 动态颜色主题（Android 12+）

---

## 第一次尝试: 批次 1-6 执行

**日期**: 2026-03-10

### 完成的任务

#### 批次 1: Compose 依赖配置 ✅

修改 `app/build.gradle`：

```groovy
android {
    buildFeatures {
        compose true
        viewBinding true
        buildConfig true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = '1.5.15'
    }
}

dependencies {
    // Compose BOM
    implementation platform('androidx.compose:compose-bom:2024.10.01')
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.activity:activity-compose:1.9.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7'
    debugImplementation 'androidx.compose.ui:ui-tooling'
}
```

#### 批次 2: 主题文件移植 ✅

从 AutoX 移植主题文件，修改包名：

| 文件 | 源路径 | 目标路径 |
|------|--------|----------|
| Theme.kt | AutoX/common/.../theme/Theme.kt | app/.../material3/theme/Theme.kt |
| Color.kt | AutoX/common/.../theme/Color.kt | app/.../material3/theme/Color.kt |
| Type.kt | AutoX/common/.../theme/Type.kt | app/.../material3/theme/Type.kt |

**包名修改**: `com.aiselp.autox.ui.material3.theme` → `org.autojs.autojs.ui.material3.theme`

#### 批次 3: minSdk 升级 ✅

修改 `project-versions.json`：

```json
{
  "mini": 21,  // 从 19 升级到 21
  "target": 34,
  "compile": 34
}
```

**原因**: Compose 要求 minSdk >= 21

#### 批次 4: EditorModel.kt 创建 ✅

创建 ViewModel 管理编辑器状态：

```kotlin
// 路径: app/src/main/java/org/autojs/autojs/ui/edit/EditorModel.kt

class EditorModel : ViewModel() {
    var showLog by mutableStateOf(false)
    var running by mutableStateOf(false)
    var lastScriptFile by mutableStateOf<File?>(null)
    
    private val scriptListener = object : ScriptExecutionListener {
        override fun onStart(execution: ScriptExecution) { ... }
        override fun onSuccess(execution: ScriptExecution, result: Any?) { ... }
        override fun onException(execution: ScriptExecution, e: Throwable) { ... }
    }
    
    fun rerun() { ... }
    fun stopCurrentExecution() { ... }
}
```

**HYB1996 API 适配**:
- `ScriptExecutionListener` 来自 `com.stardust.autojs.execution`
- `AutoJs.getInstance().scriptEngineService` 用于执行脚本

#### 批次 5: LogSheet.kt 创建 ✅

创建 Material3 ModalBottomSheet 日志面板：

```kotlin
// 路径: app/src/main/java/org/autojs/autojs/ui/material3/components/LogSheet.kt

@Composable
fun LogSheet(
    consoleImpl: ConsoleImpl,
    viewModel: EditorModel = viewModel()
) {
    ModalBottomSheet(
        onDismissRequest = { viewModel.showLog = false },
        sheetState = sheetState
    ) {
        // 标题栏 + 操作按钮
        Row { ... }
        
        // ConsoleView 嵌入
        AndroidView(
            factory = { ctx -> ConsoleView(ctx).apply { ... } }
        )
    }
}
```

**HYB1996 API 适配**:
- `ConsoleView` 来自 `com.stardust.autojs.core.console.ConsoleView`
- `ConsoleImpl` 来自 `com.stardust.autojs.core.console.ConsoleImpl`
- `UiHandler` 来自 `com.stardust.util.UiHandler`

#### 批次 6: Kotlin 编译验证 ✅

```bash
.\gradlew :app:compileCoolapkDebugKotlin

BUILD SUCCESSFUL in 3m 36s
75 actionable tasks: 5 executed, 2 from cache, 68 up-to-date
```

---

## API 差异分析

### AutoX vs HYB1996

| 功能 | AutoX | HYB1996 |
|------|-------|---------|
| 脚本执行监听 | `BinderScriptListener` | `ScriptExecutionListener` |
| 执行器 | `EngineController` | `AutoJs.getInstance().scriptEngineService` |
| 任务信息 | `TaskInfo` | `ScriptExecution` |
| 控制台 | `BinderConsoleListener` | `ConsoleImpl` |
| 日志条目 | `LogEntry` | 直接使用 `ConsoleView` |

### 迁移适配要点

1. **脚本执行**: 使用 `AutoJs.getInstance().scriptEngineService.execute()`
2. **监听器**: 实现 `ScriptExecutionListener` 接口
3. **控制台**: 通过 `ConsoleImpl` 和 `ConsoleView` 组合使用
4. **停止脚本**: `execution.engine?.forceStop()`

---

## 创建的文件清单

| 文件 | 路径 | 行数 |
|------|------|------|
| Theme.kt | app/.../material3/theme/Theme.kt | ~105 |
| Color.kt | app/.../material3/theme/Color.kt | ~210 |
| Type.kt | app/.../material3/theme/Type.kt | ~5 |
| LogSheet.kt | app/.../material3/components/LogSheet.kt | ~170 |
| EditorModel.kt | app/.../ui/edit/EditorModel.kt | ~90 |

---

## 待完成任务

### 批次 7: EditActivity 集成（与 KSP 冲突）

需要在 `EditActivity.java` 中：
1. 添加 Compose 内容
2. 集成 LogSheet
3. 连接 EditorMenu

**冲突说明**: EditActivity 使用 AndroidAnnotations 注解，需要先完成 KSP 迁移

### 依赖关系

```
Material3 批次 7 (EditActivity)
    ↓ 依赖
KSP 批次 D (复杂 Activity 迁移)
    ↓ 需要
先完成 ButterKnife 简单文件 (批次 A-C)
```

---

## 编译状态

| 组件 | 状态 | 说明 |
|------|------|------|
| Kotlin 编译 | ✅ 通过 | Material3 代码语法正确 |
| Java 编译 | ❌ 失败 | AndroidAnnotations 生成类缺失 |
| 完整构建 | ❌ 失败 | 依赖 KSP 迁移完成 |

---

## 环境信息

| 组件 | 版本 |
|------|------|
| Kotlin | 1.9.25 |
| Compose BOM | 2024.10.01 |
| Material3 | (BOM 管理) |
| Activity Compose | 1.9.1 |
| ViewModel Compose | 2.8.7 |
| minSdk | 21 |
| targetSdk | 34 |

---

## 下一步

1. **完成 KSP 迁移批次 A-C** - ButterKnife 和简单 AA 文件
2. **完成 KSP 迁移批次 D** - EditActivity 迁移
3. **继续 Material3 批次 7** - EditActivity Compose 集成
4. **完整构建验证**

---

## 变更记录

| 日期 | 操作 | 结果 |
|------|------|------|
| 2026-03-10 | 创建 Material3 主题文件 | 成功 |
| 2026-03-10 | 创建 EditorModel.kt | 成功 |
| 2026-03-10 | 创建 LogSheet.kt | 成功 |
| 2026-03-10 | 升级 minSdk 19→21 | 成功 |
| 2026-03-10 | Kotlin 编译验证 | 通过 |
| 2026-03-10 | 完整构建验证 | 失败（预期：依赖 KSP） |
| 2026-03-10 | ButterKnife 迁移完成 | 7 文件迁移 |
| 2026-03-10 | AA 引用修复 | 5 类完成，~10 类待处理 |

---

## 当前状态总览

### Material3 迁移进度

```
批次 1: Compose 依赖  ████████████ 完成
批次 2: 主题文件      ████████████ 完成
批次 3: minSdk 升级   ████████████ 完成
批次 4: EditorModel   ████████████ 完成
批次 5: LogSheet      ████████████ 完成
批次 6: Kotlin 验证   ████████████ 完成
批次 7: EditActivity  ░░░░░░░░░░░░ 阻塞（依赖 KSP）
```

**进度**: 75% (批次 1-6 完成)

### KSP 迁移进度

```
批次 A: ButterKnife   ████████████ 完成 (7 文件)
批次 B: 简单 AA       ████░░░░░░░░ 进行中
批次 C: 简单 Fragment ░░░░░░░░░░░░ 待执行
批次 D: EditActivity  ░░░░░░░░░░░░ 待执行
批次 E-I: 复杂文件    ░░░░░░░░░░░░ 待执行
```

**进度**: ~15% (ButterKnife 完成)

### 编译状态

| 组件 | 状态 | 备注 |
|------|------|------|
| Kotlin 编译 | ✅ 通过 | Material3 代码正确 |
| Java 编译 | ❌ 失败 | AA 生成类引用未完成 |
| 完整构建 | ❌ 失败 | 需完成 KSP 迁移 |

### 阻塞关系

```
Material3 批次 7 (EditActivity Compose 集成)
        ↓ 需要先完成
KSP 批次 D (EditActivity AA 迁移)
        ↓ 需要先完成
KSP 批次 B-C (简单 AA 文件迁移)
        ↓ 需要先完成
AA 生成类引用修复 (当前阻塞点)
```

### 剩余工作量估算

| 任务 | 预估时间 |
|------|----------|
| AA 生成类引用修复 (~10 类) | 1-2 小时 |
| inflate 方法签名修复 (~3 处) | 30 分钟 |
| 简单 AA Activity 迁移 | 2-3 小时 |
| 简单 AA Fragment 迁移 | 1-2 小时 |
| EditActivity 迁移 | 1-2 小时 |
| Material3 批次 7 | 1-2 小时 |
| **总计** | **6-10 小时** |

---

## 参考资源

- [AutoX EditActivity.kt](../AutoX/codeeditor/src/main/java/com/aiselp/autojs/codeeditor/EditActivity.kt)
- [AutoX Theme.kt](../AutoX/common/src/main/java/com/aiselp/autox/ui/material3/theme/Theme.kt)
- [Material3 官方文档](https://developer.android.com/jetpack/androidx/releases/material3)
- [Compose 官方文档](https://developer.android.com/jetpack/compose)
