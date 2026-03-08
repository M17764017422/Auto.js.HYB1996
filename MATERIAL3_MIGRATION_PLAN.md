# Material3 UI 移植计划

> 创建日期: 2026-03-08
> 状态: 待执行
> 来源: AutoX, AutoX.js 对比分析

---

## 一、功能目标

### 1. 悬浮日志面板

**目标**: 在编辑器底部内嵌日志面板，无需切换页面查看脚本输出

**用户价值**:
- 提高调试效率
- 实时查看脚本运行日志
- 快捷操作：运行/停止/清空/打开完整日志

**核心功能**:
- 底部可拖拽弹出面板 (ModalBottomSheet)
- 实时日志显示 (ConsoleView)
- 运行状态旋转动画
- 脚本名称显示

### 2. ThemeColor 组件

**发现**: HYB1996 已存在完整实现，无需移植！

**已有组件**:
- `ThemeColorManagerCompat.java` - 主题色管理
- `ThemeColorMaterialDialogBuilder.java` - 对话框适配
- `ThemeColorImageViewCompat.java` - 图标着色
- `ThemeColorSwipeRefreshLayout.java` - 下拉刷新

**可选升级**: Material3 颜色体系（增强对比度、Android 12+ 动态颜色）

### 3. Material3 UI 现代化

**目标**: 渐进式引入现代 UI 风格

**用户价值**:
- 更好的视觉体验
- 深色模式优化
- 动画效果提升
- Android 12+ 动态颜色支持

---

## 二、架构分析

### AutoX Material3 架构

```
AutoX Material3 架构
├── common 模块
│   └── ui/material3/
│       ├── theme/
│       │   ├── Theme.kt      # AppTheme, M3Theme Composable
│       │   ├── Color.kt      # 完整颜色方案 (亮/暗/中对比/高对比)
│       │   └── Type.kt       # 排版系统
│       ├── components/
│       │   ├── DialogController.kt
│       │   ├── BackTopAppBar.kt
│       │   ├── Checkbox.kt
│       │   └── InputBox.kt
│       └── activity/
│           └── ErrorReportActivity.kt
│
└── codeeditor 模块
    └── EditActivity.kt
        ├── setContent { AppTheme { ... } }  # Compose 入口
        ├── AndroidView { webView }          # 原生 WebView 嵌入
        └── ModalBottomSheet { LogSheet() }  # Material3 底部面板
```

### 悬浮日志面板架构

```
AutoX EditActivity 架构:
┌─────────────────────────────────────────────────────────────┐
│  EditActivity.kt (Compose + Material3)                      │
│  ├── EditorAppManager (WebView 编辑器)                      │
│  ├── ConsoleImpl (日志核心实现)                             │
│  └── LogSheet() (ModalBottomSheet 悬浮日志面板)             │
│       ├── EditorModel (ViewModel 状态管理)                  │
│       │    ├── showLog: Boolean                             │
│       │    ├── running: Boolean                             │
│       │    └── lastScriptFile: File?                        │
│       └── ConsoleView (AndroidView 嵌入)                    │
│            └── EngineController.registerGlobalConsoleListener│
└─────────────────────────────────────────────────────────────┘

数据流:
EngineController --> BinderConsoleListener --> ConsoleImpl --> ConsoleView
       ↑                    (RxJava PublishSubject)
       │
ScriptEngineService (脚本执行日志)
```

### 功能对比表

| 功能 | HYB1996 | AutoX | AutoX.js |
|------|---------|-------|----------|
| 语法高亮 | ✅ | ✅ | ✅ |
| 代码补全 | ✅ | ✅ | ✅ |
| 括号匹配 | ✅ Kotlin | ✅ | ✅ |
| 自动缩进 | ✅ Kotlin | ✅ | ✅ |
| 撤销/重做 | ✅ Kotlin | ✅ | ✅ |
| 查找替换 | ✅ | ✅ | ✅ |
| 代码格式化 | ✅ | ✅ | ✅ |
| 断点调试 | ✅ | ✅ | ✅ |
| 主题切换 | ✅ Kotlin | ✅ | ✅ |
| 堆栈帧跳转 | ✅ **独有** | ❌ | ❌ |
| SAF 支持 | ✅ **完善** | 部分 | 部分 |
| WebView 编辑器 | ❌ | ✅ | ✅ |
| 插件系统 | ❌ | ✅ | ✅ |
| Compose UI | ❌ | ✅ | ✅ |
| 悬浮日志面板 | ❌ | ✅ | ✅ |

---

## 三、实施阶段

### 阶段一：基础环境配置 (1-2天)

**目标**: 为 HYB1996 添加 Compose 支持，不破坏现有功能

**修改文件**: `app/build.gradle`

```groovy
android {
    buildFeatures {
        buildConfig true
        viewBinding true
        compose true  // 新增
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = '1.5.15'  // Kotlin 1.9.25 对应版本
    }
}

dependencies {
    // Compose BOM 统一版本管理
    implementation platform('androidx.compose:compose-bom:2024.10.01')
    
    // Material3 核心依赖
    implementation 'androidx.compose.material3:material3'
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.ui:ui-tooling-preview'
    implementation 'androidx.activity:activity-compose:1.9.1'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7'
    
    // 调试工具
    debugImplementation 'androidx.compose.ui:ui-tooling'
}
```

**新建目录结构**:
```
app/src/main/java/org/autojs/autojs/ui/material3/
├── theme/
│   ├── Theme.kt       # AppTheme Composable
│   ├── Color.kt       # 颜色定义
│   └── Type.kt        # 排版系统
└── components/
    ├── DialogController.kt   # 对话框组件
    └── LogSheet.kt           # 日志面板组件
```

---

### 阶段二：悬浮日志面板移植 (3-5天)

**目标**: 在编辑器中实现 Material3 悬浮日志面板

**需要修改的文件**:

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `EditActivity.java` → `EditActivity.kt` | 重构 | 启用 Compose |
| `EditorMenu.java` | 修改 | 添加"显示日志"菜单 |
| `menu_editor.xml` | 修改 | 添加菜单项 |
| `LogSheet.kt` | 新建 | Compose 日志面板 |
| `EditorModel.kt` | 新建 | ViewModel 状态管理 |

**核心实现 LogSheet.kt**:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogSheet(
    consoleImpl: ConsoleImpl,
    viewModel: EditorModel
) {
    if (!viewModel.showLog) return
    
    ModalBottomSheet(
        onDismissRequest = { viewModel.showLog = false },
        sheetState = rememberModalBottomSheetState(false),
        dragHandle = null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = viewModel.lastScriptFile?.name ?: "日志",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row {
                // 运行按钮（带旋转动画）
                IconButton(onClick = { viewModel.rerun() }) {
                    if (viewModel.running) {
                        val infiniteTransition = rememberInfiniteTransition()
                        val rotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing)
                            )
                        )
                        Icon(
                            modifier = Modifier.rotate(rotation),
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "运行中"
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "运行"
                        )
                    }
                }
                
                // 清除按钮
                IconButton(onClick = { consoleImpl.clear() }) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "清空")
                }
                
                // 打开日志 Activity 按钮
                IconButton(onClick = { viewModel.openLogActivity() }) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = "打开日志")
                }
            }
        }
        
        Column(modifier = Modifier.height(500.dp)) {
            AndroidView(
                factory = { context ->
                    ConsoleView(context).apply {
                        findViewById<View>(R.id.input_container).visibility = View.GONE
                        setConsole(consoleImpl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
```

**EditorModel.kt ViewModel**:

```kotlin
class EditorModel : ViewModel() {
    var showLog by mutableStateOf(false)
        private set
    
    var running by mutableStateOf(false)
        private set
    
    var lastScriptFile by mutableStateOf<File?>(null)
        private set
    
    fun toggleLog() {
        showLog = !showLog
    }
    
    fun setRunning(isRunning: Boolean) {
        running = isRunning
    }
    
    fun setScriptFile(file: File?) {
        lastScriptFile = file
    }
    
    fun rerun() {
        // 重新运行脚本逻辑
    }
    
    fun openLogActivity() {
        // 打开完整日志 Activity
    }
}
```

---

### 阶段三：主题系统升级 (2-3天)

**目标**: 升级到 Material3 主题系统

**移植文件**:

| 源文件 | 目标路径 |
|--------|----------|
| `AutoX/app/res/values/m3_colors.xml` | `HYB1996/app/res/values/m3_colors.xml` |
| `AutoX/app/res/values-night/m3_colors.xml` | `HYB1996/app/res/values-night/m3_colors.xml` |
| `AutoX/app/res/values/theme_overlays.xml` | `HYB1996/app/res/values/theme_overlays.xml` |
| `AutoX/common/.../theme/Theme.kt` | `HYB1996/app/.../material3/theme/Theme.kt` |
| `AutoX/common/.../theme/Color.kt` | `HYB1996/app/.../material3/theme/Color.kt` |
| `AutoX/common/.../theme/Type.kt` | `HYB1996/app/.../material3/theme/Type.kt` |

**修改主题继承**:

```xml
<!-- res/values/themes.xml -->
<!-- 从 -->
<style name="AppTheme" parent="Theme.AppCompat.DayNight.NoActionBar">

<!-- 改为 -->
<style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
```

**Theme.kt 核心实现**:

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // Android 12+ 动态取色
    content: @Composable() () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 动态颜色
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) 
            else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }
    
    // 自动更新状态栏/导航栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
        }
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

---

### 阶段四：持续优化 (后续迭代)

**可渐进替换的组件**:

| 优先级 | 组件 | 现状 | 移植后 |
|--------|------|------|--------|
| 高 | Dialog 组件 | MaterialDialogs 库 | Material3 BasicAlertDialog |
| 高 | 底部面板 | 无 | ModalBottomSheet |
| 中 | 设置页面 | RecyclerView + XML | LazyColumn + Compose |
| 中 | 文件列表 | RecyclerView | LazyColumn |
| 低 | 主界面 | XML Layout | Compose Scaffold |

---

## 四、混用策略详解

### 1. Activity 级别混用

```kotlin
// 方案 A：全新 Compose Activity
class NewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                // 纯 Compose UI
            }
        }
    }
}

// 方案 B：混合布局
class HybridActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AndroidView(factory = { 
                    // 嵌入现有 XML 布局
                    layoutInflater.inflate(R.layout.existing_layout, null)
                })
                // 叠加 Compose 组件
                LogSheet(consoleImpl, viewModel)
            }
        }
    }
}
```

### 2. Fragment 级别混用

```kotlin
class ComposeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?, 
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    // Compose UI
                }
            }
        }
    }
}
```

### 3. XML 布局中嵌入 Compose

```xml
<!-- res/layout/fragment_hybrid.xml -->
<LinearLayout>
    <androidx.compose.ui.platform.ComposeView
        android:id="@+id/compose_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
    
    <!-- 传统 View -->
    <RecyclerView ... />
</LinearLayout>
```

---

## 五、文件移植清单

### 从 AutoX 移植

| 源文件 | 目标路径 | 说明 |
|--------|----------|------|
| `AutoX/common/.../theme/Theme.kt` | `HYB1996/app/.../material3/theme/` | 主题定义 |
| `AutoX/common/.../theme/Color.kt` | `HYB1996/app/.../material3/theme/` | 颜色方案 |
| `AutoX/common/.../theme/Type.kt` | `HYB1996/app/.../material3/theme/` | 排版系统 |
| `AutoX/app/res/values/m3_colors.xml` | `HYB1996/app/res/values/` | 颜色资源 |
| `AutoX/app/res/values-night/m3_colors.xml` | `HYB1996/app/res/values-night/` | 夜间颜色 |
| `AutoX/app/res/values/theme_overlays.xml` | `HYB1996/app/res/values/` | 对比度变体 |

### 新建文件

| 文件路径 | 说明 |
|----------|------|
| `app/.../ui/material3/components/LogSheet.kt` | 悬浮日志面板组件 |
| `app/.../ui/edit/EditorModel.kt` | ViewModel 状态管理 |

### 修改文件

| 文件路径 | 改动说明 |
|----------|----------|
| `app/build.gradle` | 添加 Compose 依赖 |
| `app/.../ui/edit/EditActivity.java` | 重构为 Kotlin，集成 Compose |
| `app/.../ui/edit/EditorMenu.java` | 添加"显示日志"菜单处理 |
| `app/res/menu/menu_editor.xml` | 添加菜单项 |
| `app/res/values/themes.xml` | 修改主题继承 |

---

## 六、风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| APK 体积增加 | +3-5MB | 启用 R8 压缩 |
| Kotlin 学习曲线 | 开发效率 | 渐进式迁移，保留 Java 代码 |
| 主题冲突 | UI 不一致 | 手动同步 XML 和 Compose 颜色 |
| AndroidAnnotations 兼容 | 重构工作 | 混用策略，逐步替换 |
| Material3 兼容性 | UI 组件冲突 | 测试所有界面 |

---

## 七、参考资源

### 源项目路径

- **AutoX**: `K:\msys64\home\ms900\AutoX`
- **AutoX.js**: `K:\msys64\home\ms900\AutoX.js`
- **Auto.js.HYB1996**: `K:\msys64\home\ms900\Auto.js.HYB1996`

### 关键参考文件

**悬浮日志面板**:
- `AutoX/codeeditor/src/main/java/com/aiselp/autojs/codeeditor/EditActivity.kt`

**Material3 主题**:
- `AutoX/common/src/main/java/com/aiselp/autox/ui/material3/theme/Theme.kt`
- `AutoX/common/src/main/java/com/aiselp/autox/ui/material3/theme/Color.kt`

**ThemeColor 组件（已存在）**:
- `HYB1996/app/src/main/java/org/autojs/autojs/theme/ThemeColorManagerCompat.java`

---

## 八、执行检查清单

### 阶段一检查项
- [ ] `app/build.gradle` 添加 Compose 依赖
- [ ] Sync Gradle 成功
- [ ] 创建 `ui/material3/theme/` 目录
- [ ] 移植 Theme.kt, Color.kt, Type.kt

### 阶段二检查项
- [ ] 创建 LogSheet.kt
- [ ] 创建 EditorModel.kt
- [ ] 修改 EditActivity 集成 Compose
- [ ] 添加"显示日志"菜单
- [ ] 测试日志面板显示/隐藏
- [ ] 测试日志实时更新
- [ ] 测试清空/打开完整日志

### 阶段三检查项
- [ ] 移植 m3_colors.xml
- [ ] 修改主题继承
- [ ] 测试亮色/暗色模式切换
- [ ] 测试 Android 12+ 动态颜色
- [ ] 测试所有界面主题一致性

### 阶段四检查项
- [ ] Dialog 组件替换
- [ ] 设置页面 Compose 化
- [ ] 文件列表优化
- [ ] 全面测试

---

*文档结束*
