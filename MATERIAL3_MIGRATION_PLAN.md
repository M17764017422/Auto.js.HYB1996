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

### 阶段一：基础环境配置

**目标**: 为 HYB1996 添加 Compose 支持，不破坏现有功能

#### 1.1 详细工作量分析

| 任务 | 文件 | 行数 | 耗时 | 风险 |
|------|------|------|------|------|
| 添加 Compose 依赖 | `app/build.gradle` | ~20行 | 15分钟 | 低 |
| Gradle Sync | - | - | 5分钟 | 低 |
| 创建目录结构 | - | - | 5分钟 | 无 |
| 移植 Theme.kt | 新建 | ~105行 | 20分钟 | 低 |
| 移植 Color.kt | 新建 | ~210行 | 20分钟 | 低 |
| 移植 Type.kt | 新建 | ~5行 | 5分钟 | 低 |
| 调整包名导入 | 3个文件 | - | 10分钟 | 低 |
| 编译验证 | - | - | 10分钟 | 低 |

**阶段一总计：1.5 小时**

#### 1.2 build.gradle 修改

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

#### 1.3 新建目录结构

```
app/src/main/java/org/autojs/autojs/ui/material3/
├── theme/
│   ├── Theme.kt       # ~105行，直接复制自 AutoX
│   ├── Color.kt       # ~210行，直接复制自 AutoX
│   └── Type.kt        # ~5行，直接复制自 AutoX
└── components/
    └── (阶段二添加)
```

#### 1.4 源文件分析（AutoX）

| 文件 | 路径 | 行数 | 复杂度 |
|------|------|------|--------|
| Theme.kt | `AutoX/common/.../theme/Theme.kt` | 105 | 简单，纯配置 |
| Color.kt | `AutoX/common/.../theme/Color.kt` | 210 | 简单，颜色定义 |
| Type.kt | `AutoX/common/.../theme/Type.kt` | 5 | 极简单 |

**关键发现**：主题文件可直接复制，仅需修改包名

---

### 阶段二：悬浮日志面板移植

**目标**: 在编辑器中实现 Material3 悬浮日志面板

#### 2.1 现有代码分析

**HYB1996 EditActivity 现状**：
- 文件：`app/src/main/java/org/autojs/autojs/ui/edit/EditActivity.java`
- 代码量：~250 行 Java
- 框架：AndroidAnnotations `@EActivity`
- 核心依赖：EditorView（788行），EditorMenu

**AutoX EditActivity 参考**：
- 文件：`AutoX/codeeditor/.../EditActivity.kt`
- 代码量：~220 行 Kotlin
- 框架：Compose + Material3
- 核心组件：EditorAppManager，LogSheet

**好消息**：ConsoleView 已存在于 HYB1996
- 路径：`autojs/src/main/java/com/stardust/autojs/core/console/ConsoleView.java`
- 代码量：~239 行
- 可直接复用，无需移植

#### 2.2 详细工作量分析

| 任务 | 文件 | 工作量 | 风险 | 说明 |
|------|------|--------|------|------|
| 创建 EditorModel.kt | 新建 | 30分钟 | 低 | ~40行，简单 ViewModel |
| 创建 LogSheet.kt | 新建 | 1小时 | 中 | ~70行，复制自 AutoX |
| 添加菜单项 | `menu_editor.xml` | 10分钟 | 低 | 1行 XML |
| 修改 EditorMenu.java | 现有 | 30分钟 | 低 | 添加菜单处理逻辑 |
| **EditActivity 重构** | 现有 | **3-5小时** | **高** | 见下方分析 |
| 集成测试 | - | 1小时 | 中 | 功能验证 |

**阶段二总计：6-8 小时**

#### 2.3 关键技术难点：EditActivity 重构

**问题**：EditActivity 使用 `@EActivity` 注解

```java
// 现状：AndroidAnnotations 注解
@EActivity(R.layout.activity_edit)
public class EditActivity extends BaseActivity {
    @ViewById(R.id.editor_view) EditorView mEditorView;
    @AfterViews void setUpViews() { ... }
}
```

**方案 A：完全重构为 Kotlin（推荐）**
- 工作量：3-5 小时
- 风险：中
- 收益：代码现代化，便于后续维护
- 步骤：
  1. Java → Kotlin 转换
  2. 移除 @EActivity，改用 ViewBinding
  3. 添加 Compose setContent 入口
  4. 集成 LogSheet 组件

**方案 B：混合方案（快速）**
- 工作量：1-2 小时
- 风险：低
- 限制：保留 @EActivity，Compose 作为叠加层
- 实现：在 `activity_edit.xml` 中添加 `ComposeView`

```xml
<!-- activity_edit.xml 添加 -->
<androidx.compose.ui.platform.ComposeView
    android:id="@+id/compose_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```java
// EditActivity.java 添加
ComposeView composeView = findViewById(R.id.compose_view);
composeView.setContent(() -> {
    AppTheme.INSTANCE.invoke(() -> {
        LogSheet(consoleImpl, viewModel);
        return null;
    });
});
```

#### 2.4 需要修改的文件清单

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `EditActivity.java` | 重构/修改 | 方案A完全重构，方案B添加ComposeView |
| `EditorMenu.java` | 修改 | 添加"显示日志"菜单处理 |
| `menu_editor.xml` | 修改 | 添加菜单项 |
| `LogSheet.kt` | 新建 | Compose 日志面板（~70行） |
| `EditorModel.kt` | 新建 | ViewModel 状态管理（~40行） |

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

### 阶段三：主题系统升级

**目标**: 升级到 Material3 主题系统

#### 3.1 详细工作量分析

| 任务 | 文件 | 工作量 | 风险 |
|------|------|--------|------|
| 移植 m3_colors.xml | 新建 | 20分钟 | 低 |
| 移植 m3_colors.xml (night) | 新建 | 10分钟 | 低 |
| 移植 theme_overlays.xml | 新建 | 10分钟 | 低 |
| 修改主题继承 | `themes.xml` | 15分钟 | 中 |
| 测试亮色主题 | - | 30分钟 | 低 |
| 测试暗色主题 | - | 30分钟 | 低 |
| 测试所有界面 | - | 1小时 | 中 |
| 修复兼容性问题 | - | 1-2小时 | 中 |

**阶段三总计：4-5 小时**

#### 3.2 移植文件清单

| 源文件 | 目标路径 | 说明 |
|--------|----------|------|
| `AutoX/app/res/values/m3_colors.xml` | `HYB1996/app/res/values/m3_colors.xml` | 亮色颜色资源 |
| `AutoX/app/res/values-night/m3_colors.xml` | `HYB1996/app/res/values-night/m3_colors.xml` | 暗色颜色资源 |
| `AutoX/app/res/values/theme_overlays.xml` | `HYB1996/app/res/values/theme_overlays.xml` | 对比度变体 |

#### 3.3 主题继承修改

```xml
<!-- res/values/themes.xml -->
<!-- 从 -->
<style name="AppTheme" parent="Theme.AppCompat.DayNight.NoActionBar">

<!-- 改为 -->
<style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
```

#### 3.4 兼容性风险点

| 问题 | 影响 | 解决方案 |
|------|------|----------|
| ThemeColor 组件冲突 | 现有主题色管理失效 | 保持 ThemeColorManager 兼容 |
| MaterialDialogs 库 | 样式不一致 | 渐进替换为 M3 Dialog |
| 状态栏颜色 | 部分页面异常 | 逐个 Activity 测试 |
| 深色模式 | 颜色对比度问题 | 手动调整颜色值 |

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

## 三、工作量汇总

### 3.1 各阶段工作量

| 阶段 | 任务 | 工作量 | 风险 | 前置条件 |
|------|------|--------|------|----------|
| 阶段一 | Compose 环境配置 | 1.5 小时 | 低 | 无 |
| 阶段二 | 悬浮日志面板 | 6-8 小时 | 中高 | 阶段一完成 |
| 阶段三 | 主题系统升级 | 4-5 小时 | 中 | 阶段一完成 |
| **总计** | **核心功能** | **11.5-14.5 小时** | **中** | - |

### 3.2 方案对比

| 方案 | 工作量 | 风险 | 收益 | 推荐场景 |
|------|--------|------|------|----------|
| 方案 A（完全重构） | 11.5-14.5 小时 | 中高 | 代码现代化 | 计划长期维护 |
| 方案 B（混合方案） | 8-11 小时 | 低 | 快速实现 | 快速验证功能 |

### 3.3 简单任务优先执行顺序

| 批次 | 任务 | 工作量 | 可独立验证 |
|------|------|--------|-----------|
| 1 | 添加 Compose 依赖 | 15分钟 | ✅ 编译通过 |
| 2 | 移植主题文件 | 1小时 | ✅ 编译通过 |
| 3 | 创建 EditorModel.kt | 30分钟 | ✅ 编译通过 |
| 4 | 创建 LogSheet.kt | 1小时 | ✅ 编译通过 |
| 5 | 添加菜单项 | 10分钟 | ✅ 菜单显示 |
| 6 | 修改 EditorMenu | 30分钟 | ✅ 菜单响应 |
| 7 | EditActivity 集成 | 3-5小时 | ✅ 日志面板显示 |
| 8 | 主题升级 | 4-5小时 | ✅ 界面样式 |
| 9 | 测试验证 | 2小时 | ✅ 功能正常 |

### 3.4 APK 体积影响

| 组件 | 增量 |
|------|------|
| Compose Runtime | ~1.5 MB |
| Material3 | ~0.5 MB |
| Compose UI | ~0.8 MB |
| 其他依赖 | ~0.5 MB |
| **总计增量** | **~3-4 MB** |

**缓解措施**：启用 R8 代码压缩和资源压缩

---

## 四、关键技术决策

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

## 九、与 KSP 计划并行执行协调

### 9.1 可并行任务

| Material3 批次 | KSP 批次 | 并行状态 | 说明 |
|---------------|----------|----------|------|
| 1-2: Compose 配置 | A-F: 全部简单/中等 | ✅ 可并行 | 无文件冲突 |
| 3-4: 新建文件 | A-F: 全部简单/中等 | ✅ 可并行 | 无文件冲突 |
| 5-6: 菜单修改 | A-F: 全部简单/中等 | ✅ 可并行 | 无文件冲突 |

### 9.2 需协调任务

| Material3 批次 | KSP 批次 | 冲突文件 | 解决方案 |
|---------------|----------|----------|----------|
| 7: EditActivity 集成 | G: EditActivity 重构 | `EditActivity.java` | **合并重构** |

### 9.3 合并重构 EditActivity

**一次性完成以下工作**：

```
合并任务清单：
□ Java → Kotlin 转换
□ 移除 @EActivity 注解
□ 添加 ViewBinding
□ 添加 Compose setContent 入口
□ 创建 EditorModel.kt
□ 创建 LogSheet.kt
□ 集成 LogSheet 组件
□ 测试验证

合并工作量: 8-12小时
```

**合并后代码结构**：

```kotlin
// EditActivity.kt - 合并重构后
class EditActivity : BaseActivity() {
    private lateinit var binding: ActivityEditBinding
    private val viewModel: EditorModel by viewModels()
    private lateinit var consoleImpl: ConsoleImpl
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 方案 A: 完全 Compose
        setContent {
            AppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // EditorView 作为 AndroidView 嵌入
                    AndroidView(factory = { binding.root })
                    // 悬浮日志面板
                    LogSheet(consoleImpl, viewModel)
                }
            }
        }
        
        // 或者 方案 B: ViewBinding + Compose 叠加
        // binding = ActivityEditBinding.inflate(layoutInflater)
        // setContentView(binding.root)
        // binding.composeView.setContent { AppTheme { LogSheet(...) } }
    }
}
```

### 9.4 并行执行时间对比

| 执行方式 | 总耗时 | 说明 |
|----------|--------|------|
| Material3 单独执行 | 11.5-14.5小时 | 不含 KSP 迁移 |
| KSP 单独执行 | 45-69小时 | 不含 Material3 |
| 串行执行合计 | 56-83小时 | 两计划依次执行 |
| 并行执行合计 | 37-57小时 | 独立任务同时进行 |
| **节省** | **19-26小时** | 约 35% |

### 9.5 推荐执行顺序

```
┌─────────────────────────────────────────────────────────────┐
│ 阶段 1: 并行执行 (19-30小时)                                 │
├─────────────────────────────────────────────────────────────┤
│ Material3: 批次 1-6              KSP: 批次 A-F              │
│ 3.5小时                         19-30小时                   │
│                                                              │
│ ⏱️ 实际耗时: max(3.5h, 19-30h) = 19-30小时                  │
│                                                              │
│ Material3 任务在此阶段可快速完成，等待 KSP 简单任务          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 阶段 2: 合并重构 (8-12小时)                                  │
├─────────────────────────────────────────────────────────────┤
│ EditActivity 合并重构                                        │
│ - 移除 @EActivity + ViewBinding (来自 KSP)                  │
│ - Compose + LogSheet (来自 Material3)                       │
│                                                              │
│ 💡 这是两计划唯一冲突点，合并后一次性完成                     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 阶段 3: 收尾并行 (10-15小时)                                 │
├─────────────────────────────────────────────────────────────┤
│ Material3: 批次 8-9             KSP: 批次 H-I               │
│ 6-7小时                        10-15小时                    │
│                                                              │
│ ⏱️ 实际耗时: max(6-7h, 10-15h) = 10-15小时                  │
│                                                              │
│ Material3 主题升级与 KSP 复杂文件处理可并行                   │
└─────────────────────────────────────────────────────────────┘
```

### 9.6 协调建议

| 阶段 | 建议执行方式 |
|------|-------------|
| 阶段 1 | 可两人并行，或同一人先完成 Material3（快）再做 KSP |
| 阶段 2 | 必须合并执行，避免重复工作 |
| 阶段 3 | 可并行，但需确保 EditorView 重构（KSP）先完成 |

---

## 十、变更记录

| 日期 | 操作 | 说明 |
|------|------|------|
| 2026-03-08 | 创建计划 | 初始版本 |
| 2026-03-10 | 深度分析工作量 | 添加详细任务分解 |
| 2026-03-10 | 添加源码分析 | AutoX EditActivity、ConsoleView 分析 |
| 2026-03-10 | 更新工作量估算 | 总计 11.5-14.5 小时 |
| 2026-03-10 | 添加方案对比 | 方案A完全重构 vs 方案B混合 |
| 2026-03-10 | 添加与 KSP 并行执行协调 | 合并 EditActivity 重构 |

---

*文档结束*
