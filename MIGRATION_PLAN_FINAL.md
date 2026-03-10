# Auto.js.HYB1996 迁移重构最终计划

> 创建日期: 2026-03-10
> 状态: 待执行
> 策略: 合并重构 + 混合架构

---

## 一、项目概述

### 1.1 迁移目标

| 目标 | 说明 |
|------|------|
| **KSP 迁移** | 完全移除 AndroidAnnotations 和 ButterKnife，使用 ViewBinding |
| **Material3 集成** | 引入 Jetpack Compose + Material3，实现现代化 UI |
| **构建优化** | 移除 KAPT，提升构建速度约 30% |

### 1.2 当前迁移进度

| 迁移类别 | 已完成 | 待完成 | 进度 |
|---------|--------|--------|------|
| KSP - Glide | ✅ 100% | - | 完成 |
| KSP - ButterKnife | ✅ 100% (7文件) | - | 完成 |
| KSP - AA 简单文件 | ✅ 100% (7文件) | - | 完成 |
| KSP - AA 生成类引用 | ✅ 100% (15类) | - | 完成 |
| KSP - AA 复杂文件 | 0% | 5文件 | **待执行** |
| Material3 批次 1-6 | ✅ 100% | - | 完成 |
| Material3 批次 7 | 0% | EditActivity集成 | **待执行** |

### 1.3 剩余复杂文件清单

| 文件 | AA注解数 | 代码行数 | 复杂度 | 预估工时 |
|------|----------|----------|--------|----------|
| MainActivity.java | 6 | ~300 | 中等 | 2-3小时 |
| DrawerFragment.java | 8 | ~400 | 中等 | 3-4小时 |
| EditActivity.java | 3 | ~250 | 低 | 1-2小时 |
| **EditorView.java** | 11 | 788 | **极高** | 4-6小时 |
| TimedTaskSettingActivity.java | 22 | ~400 | 中等 | 3-5小时 |

---

## 二、执行策略

### 2.1 合并重构 + 逐步验证策略

**核心理念**：合并重构提高效率，逐步验证确保安全

```
合并重构 = 减少重复工作（效率）
逐步验证 = 每批次独立测试（安全）

两者结合 = 高效 + 安全
```

**关键原则**：
1. 每批次完成后立即验证，不等待其他批次
2. 每批次完成后提交中间版本，方便回滚
3. 编译验证优先，确保代码可编译再进行功能测试
4. 合并重构保持效率，避免重复修改同一文件

### 2.2 合并重构策略

**EditActivity 合并处理**：同时完成 KSP 迁移和 Material3 集成

```
EditActivity 合并任务：
├── Java → Kotlin 转换
├── 移除 @EActivity 注解
├── 添加 ViewBinding
├── 添加 Compose setContent 入口
├── 集成 LogSheet 组件
└── 测试验证
```

### 2.3 混合架构策略

**EditorView 处理**：容器层 ViewBinding，核心组件保留

```
EditorView 混合架构：
├── 容器层: ViewBinding 迁移
│   ├── 移除 @EViewGroup
│   ├── 手动 inflate
│   └── 9 个 @ViewById → binding.xxx
│
├── CodeEditor: 保留传统实现
│   ├── 567 行复杂代码
│   ├── 语法高亮、断点、补全
│   └── 通过 binding.editor 访问
│
└── LogSheet: Material3 Compose（已完成）
    └── AndroidView 嵌入 ConsoleView
```

### 2.4 逐步验证流程

每个批次执行完成后，按以下流程验证：

```
┌─────────────────────────────────────────────────────────────┐
│ 步骤 1: 编译验证                                            │
│ └── 运行 ./gradlew compileCoolapkDebugKotlin               │
│     └── 成功 → 继续                                         │
│     └── 失败 → 修复后重试                                   │
├─────────────────────────────────────────────────────────────┤
│ 步骤 2: 功能验证                                            │
│ └── 运行 ./gradlew assembleCoolapkDebug                    │
│     └── 安装 APK 到测试设备                                 │
│     └── 执行批次对应的功能测试                               │
│     └── 成功 → 继续                                         │
│     └── 失败 → 修复后重试                                   │
├─────────────────────────────────────────────────────────────┤
│ 步骤 3: 提交中间版本                                        │
│ └── git add -A                                             │
│ └── git commit -m "feat: 批次X完成"                         │
│ └── git tag v0.85.XX-alpha                                 │
│ └── git push origin temp-test-branch                       │
│ └── git push origin v0.85.XX-alpha                         │
└─────────────────────────────────────────────────────────────┘
```

### 2.5 版本号规划

| 批次 | 完成后标签 | 说明 |
|------|------------|------|
| 批次 1 | v0.85.16-alpha | MainActivity + DrawerFragment |
| 批次 2 | v0.85.17-alpha | TimedTaskSettingActivity |
| 批次 3 | v0.85.18-alpha | EditActivity + EditorView 合并重构 |
| 批次 4 | v0.85.19-alpha | Material3 主题升级 |
| 批次 5 | v0.85.20 | 最终版本（移除 KAPT） |

---

## 三、批次规划

### 批次 1：MainActivity + DrawerFragment 并行迁移（5-7小时）

#### 1.1 MainActivity.java 迁移

**路径**: `app/src/main/java/org/autojs/autojs/ui/main/MainActivity.java`

| 步骤 | 操作 | 工时 |
|------|------|------|
| 1.1.1 | 移除 `@EActivity` 注解 | 10分钟 |
| 1.1.2 | 添加 `ActivityMainBinding` | 15分钟 |
| 1.1.3 | 将 3 个 `@ViewById` 改为 `binding.xxx` | 20分钟 |
| 1.1.4 | 将 2 个 `@Click` 改为 `setOnClickListener` | 15分钟 |
| 1.1.5 | 将 `@AfterViews` 内容移到 `onCreate` | 20分钟 |
| 1.1.6 | **修改 activity_main.xml**：`<fragment>` → `FragmentContainerView` | 30分钟 |
| 1.1.7 | 动态加载 DrawerFragment | 20分钟 |
| 1.1.8 | 处理 EventBus 生命周期 | 15分钟 |
| 1.1.9 | 编译验证 | 15分钟 |

**关键代码变更**：

```java
// 迁移前
@EActivity(R.layout.activity_main)
public class MainActivity extends BaseActivity {
    @ViewById(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @ViewById(R.id.viewpager) ViewPager mViewPager;
    @ViewById(R.id.fab) FloatingActionButton mFab;
    
    @AfterViews
    void setUpViews() { ... }
    
    @Click(R.id.setting)
    void startSettingActivity() { ... }
}

// 迁移后
public class MainActivity extends BaseActivity {
    private ActivityMainBinding binding;
    private DrawerFragment drawerFragment;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 动态加载 DrawerFragment
        if (savedInstanceState == null) {
            drawerFragment = new DrawerFragment();
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.drawer_fragment_container, drawerFragment)
                .commit();
        }
        
        setUpViews();
        binding.setting.setOnClickListener(v -> startSettingActivity());
    }
}
```

**布局文件变更**：

```xml
<!-- activity_main.xml -->
<!-- 迁移前 -->
<fragment android:name="org.autojs.autojs.ui.main.drawer.DrawerFragment_"
    android:id="@+id/nav_view" />

<!-- 迁移后 -->
<androidx.fragment.app.FragmentContainerView
    android:id="@+id/nav_view"
    android:name="org.autojs.autojs.ui.main.drawer.DrawerFragment" />
```

#### 1.2 DrawerFragment.java 迁移

**路径**: `app/src/main/java/org/autojs/autojs/ui/main/drawer/DrawerFragment.java`

| 步骤 | 操作 | 工时 |
|------|------|------|
| 1.2.1 | 移除 `@EFragment` 注解 | 10分钟 |
| 1.2.2 | 添加 `FragmentDrawerBinding` | 15分钟 |
| 1.2.3 | 将 6 个 `@ViewById` 改为 `binding.xxx` | 30分钟 |
| 1.2.4 | 将 `@Click` 改为 `setOnClickListener` | 10分钟 |
| 1.2.5 | 重构 `onCreateView` 和 `onViewCreated` | 30分钟 |
| 1.2.6 | 处理 RxJava Disposable 管理 | 20分钟 |
| 1.2.7 | 处理 EventBus 注册/注销 | 15分钟 |
| 1.2.8 | 编译验证 | 15分钟 |

**关键代码变更**：

```java
// 迁移后
public class DrawerFragment extends Fragment {
    private FragmentDrawerBinding binding;
    private CompositeDisposable disposables = new CompositeDisposable();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDrawerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.avatar.setOnClickListener(v -> loginOrShowUserInfo());
        setUpViews();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        disposables.clear();
    }
}
```

#### 1.3 批次1验证清单

**编译验证**：
```bash
.\gradlew compileCoolapkDebugKotlin --parallel
```

**功能验证**：
- [ ] MainActivity 启动正常
- [ ] 侧边栏显示/隐藏正常
- [ ] ViewPager 切换正常
- [ ] 设置按钮点击正常
- [ ] 退出按钮点击正常

**提交中间版本**：
```bash
git add -A
git commit -m "feat: 批次1完成 - MainActivity和DrawerFragment迁移"
git tag v0.85.16-alpha
git push origin temp-test-branch
git push origin v0.85.16-alpha
```

---

### 批次 2：TimedTaskSettingActivity 迁移（3-5小时）

**路径**: `app/src/main/java/org/autojs/autojs/ui/timing/TimedTaskSettingActivity.java`

| 步骤 | 操作 | 工时 |
|------|------|------|
| 2.1 | 移除 `@EActivity` 注解 | 10分钟 |
| 2.2 | 添加 `ActivityTimedTaskSettingBinding` | 15分钟 |
| 2.3 | **迁移 17 个 `@ViewById`** | 1小时 |
| 2.4 | 将 `@CheckedChange` 改为 `setOnCheckedChangeListener` | 20分钟 |
| 2.5 | 将 `@Click` 改为 `setOnClickListener` | 15分钟 |
| 2.6 | 处理 ExpandableRelativeLayout 逻辑 | 30分钟 |
| 2.7 | 测试定时任务创建功能 | 30分钟 |

**@CheckedChange 迁移示例**：

```java
// 迁移前
@CheckedChange({R.id.daily_task_radio, R.id.weekly_task_radio, 
                R.id.disposable_task_radio, R.id.run_on_broadcast})
void onTimingMethodChanged(CompoundButton button, boolean isChecked) { ... }

// 迁移后
private void setupCheckedListeners() {
    CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
        onTimingMethodChanged(button, isChecked);
    };
    binding.dailyTaskRadio.setOnCheckedChangeListener(listener);
    binding.weeklyTaskRadio.setOnCheckedChangeListener(listener);
    binding.disposableTaskRadio.setOnCheckedChangeListener(listener);
    binding.runOnBroadcast.setOnCheckedChangeListener(listener);
}
```

#### 2.1 批次2验证清单

**编译验证**：
```bash
.\gradlew compileCoolapkDebugKotlin --parallel
```

**功能验证**：
- [ ] Activity 启动正常
- [ ] 每日任务类型选择正常
- [ ] 每周任务类型选择正常
- [ ] 一次性任务类型选择正常
- [ ] 广播任务类型选择正常
- [ ] 时间选择器功能正常
- [ ] 任务保存功能正常

**提交中间版本**：
```bash
git add -A
git commit -m "feat: 批次2完成 - TimedTaskSettingActivity迁移"
git tag v0.85.17-alpha
git push origin temp-test-branch
git push origin v0.85.17-alpha
```

---

### 批次 3：EditActivity + EditorView 合并重构（6-9小时）

**这是核心批次，同时完成 KSP 迁移和 Material3 集成**

#### 3.1 EditActivity.java 迁移

**路径**: `app/src/main/java/org/autojs/autojs/ui/edit/EditActivity.java`

| 步骤 | 操作 | 工时 |
|------|------|------|
| 3.1.1 | Java → Kotlin 转换 | 30分钟 |
| 3.1.2 | 移除 `@EActivity` 注解 | 10分钟 |
| 3.1.3 | 添加 `ActivityEditBinding` | 15分钟 |
| 3.1.4 | 添加 Compose `setContent` 入口 | 30分钟 |
| 3.1.5 | 集成 EditorModel ViewModel | 20分钟 |
| 3.1.6 | 集成 LogSheet 组件 | 30分钟 |

**EditActivity.kt 迁移后代码**：

```kotlin
// EditActivity.kt
class EditActivity : BaseActivity() {
    private lateinit var binding: ActivityEditBinding
    private val viewModel: EditorModel by viewModels()
    private lateinit var consoleImpl: ConsoleImpl
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Compose 容器 + AndroidView 嵌入传统视图
        setContent {
            AppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    // EditorView 作为 AndroidView 嵌入
                    AndroidView(
                        factory = { ctx ->
                            binding = ActivityEditBinding.inflate(LayoutInflater.from(ctx))
                            binding.root
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // 悬浮日志面板
                    LogSheet(consoleImpl, viewModel)
                }
            }
        }
    }
}
```

#### 3.2 EditorView.java 迁移（混合架构）

**路径**: `app/src/main/java/org/autojs/autojs/ui/edit/EditorView.java`

| 步骤 | 操作 | 工时 |
|------|------|------|
| 3.2.1 | 移除 `@EViewGroup` 注解 | 10分钟 |
| 3.2.2 | 修改构造函数，手动 inflate | 20分钟 |
| 3.2.3 | 添加 `EditorViewBinding` | 15分钟 |
| 3.2.4 | **迁移 9 个 `@ViewById`** | 45分钟 |
| 3.2.5 | 修改字段访问：`mEditor` → `binding.editor`（约80处） | 1小时 |
| 3.2.6 | 处理 Fragment Toolbar 切换逻辑 | 30分钟 |
| 3.2.7 | 确保 DebugBar 功能正常 | 30分钟 |
| 3.2.8 | 测试编辑器完整功能 | 1小时 |

**EditorView.java 迁移后代码**：

```java
// EditorView.java - 迁移后
public class EditorView extends FrameLayout {
    private EditorViewBinding binding;
    
    public EditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.editor_view, this);
        binding = EditorViewBinding.bind(this);
        init();
    }
    
    public EditorView(Context context) {
        this(context, null);
    }
    
    private void init() {
        // 原 @AfterViews 内容
        binding.editor.setOnTextChangeListener(...);
        binding.codeCompletionBar.setOnHintClickListener(this);
        binding.functionsKeyboard.setClickCallback(this);
        // ...
    }
    
    // 字段访问改为 getter
    public CodeEditor getEditor() {
        return binding.editor;
    }
    
    public DebugBar getDebugBar() {
        return binding.debugBar;
    }
}
```

**CodeEditor 保留说明**：
- 567 行复杂代码不做迁移
- 语法高亮、断点调试、自动补全保持不变
- 通过 `binding.editor` 访问所有功能

#### 3.3 批次3验证清单

**编译验证**：
```bash
.\gradlew compileCoolapkDebugKotlin --parallel
```

**功能验证**：
- [ ] EditActivity 启动正常
- [ ] 代码编辑功能正常（输入、删除、选择）
- [ ] 代码保存功能正常
- [ ] 语法高亮显示正常
- [ ] 断点调试功能正常
- [ ] 代码补全功能正常
- [ ] 日志面板显示/隐藏正常
- [ ] 日志面板运行按钮正常
- [ ] 日志面板清空按钮正常
- [ ] 日志面板打开完整日志正常

**提交中间版本**：
```bash
git add -A
git commit -m "feat: 批次3完成 - EditActivity和EditorView合并重构"
git tag v0.85.18-alpha
git push origin temp-test-branch
git push origin v0.85.18-alpha
```

---

### 批次 4：Material3 主题系统升级（4-5小时）

| 步骤 | 操作 | 工时 |
|------|------|------|
| 4.1 | 移植 m3_colors.xml（亮色） | 20分钟 |
| 4.2 | 移植 m3_colors.xml（暗色） | 15分钟 |
| 4.3 | 修改 themes.xml 主题继承 | 20分钟 |
| 4.4 | 测试亮色主题 | 30分钟 |
| 4.5 | 测试暗色主题 | 30分钟 |
| 4.6 | 测试所有界面主题一致性 | 1小时 |
| 4.7 | 修复兼容性问题 | 1-2小时 |

**主题继承变更**：

```xml
<!-- res/values/themes.xml -->
<!-- 迁移前 -->
<style name="AppTheme" parent="Theme.AppCompat.DayNight.NoActionBar">

<!-- 迁移后 -->
<style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
```

#### 4.1 批次4验证清单

**编译验证**：
```bash
.\gradlew compileCoolapkDebugKotlin --parallel
```

**功能验证**：
- [ ] 应用启动主题显示正常
- [ ] 亮色主题显示正常
- [ ] 暗色主题显示正常
- [ ] 主题切换功能正常
- [ ] MainActivity 界面主题一致
- [ ] EditActivity 界面主题一致
- [ ] SettingsActivity 界面主题一致
- [ ] 所有 Dialog 样式一致
- [ ] 状态栏颜色正确
- [ ] 导航栏颜色正确

**提交中间版本**：
```bash
git add -A
git commit -m "feat: 批次4完成 - Material3主题系统升级"
git tag v0.85.19-alpha
git push origin temp-test-branch
git push origin v0.85.19-alpha
```

---

### 批次 5：清理与测试（3-4小时）

| 步骤 | 操作 | 工时 |
|------|------|------|
| 5.1 | 移除 `butterknife` 依赖 | 15分钟 |
| 5.2 | 移除 `androidannotations` 依赖 | 15分钟 |
| 5.3 | 移除 `kotlin-kapt` 插件 | 10分钟 |
| 5.4 | 清理 kapt 相关配置 | 10分钟 |
| 5.5 | 全局搜索确认无 `Activity_` 引用遗漏 | 30分钟 |
| 5.6 | **完整功能回归测试** | 1.5小时 |
| 5.7 | APK 体积对比 | 15分钟 |
| 5.8 | 构建速度对比 | 15分钟 |

#### 5.1 批次5验证清单

**编译验证**：
```bash
.\gradlew assembleCoolapkDebug --parallel
```

**完整功能回归测试**：
- [ ] 应用启动正常
- [ ] MainActivity 所有 Tab 切换正常
- [ ] 侧边栏所有功能正常
- [ ] 编辑器完整功能正常
- [ ] 日志面板功能正常
- [ ] 定时任务创建/编辑/删除正常
- [ ] 设置页面功能正常
- [ ] 登录/注册功能正常
- [ ] 文件浏览功能正常
- [ ] 脚本打包功能正常
- [ ] 悬浮窗功能正常
- [ ] 无障碍服务正常
- [ ] 亮色/暗色主题切换正常

**性能验证**：
- [ ] 构建速度提升约 30%
- [ ] APK 体积增量 < 4MB
- [ ] 无内存泄漏

**提交最终版本**：
```bash
git add -A
git commit -m "feat: 迁移完成 - 移除KAPT，完成KSP和Material3集成"
git tag v0.85.20
git push origin temp-test-branch
git push origin v0.85.20
```

---

## 四、工作量汇总

### 4.1 批次工时

| 批次 | 内容 | 工时 | 可并行 |
|------|------|------|--------|
| 1 | MainActivity + DrawerFragment | 5-7小时 | ✅ 可并行 |
| 2 | TimedTaskSettingActivity | 3-5小时 | ✅ 可与批次1并行 |
| 3 | EditActivity + EditorView（合并重构） | 6-9小时 | ❌ 核心批次 |
| 4 | Material3 主题升级 | 4-5小时 | ✅ 可与批次2并行 |
| 5 | 清理与测试 | 3-4小时 | ❌ 最后执行 |
| **总计** | - | **21-30小时** | - |

### 4.2 并行执行时间线

```
Day 1 (8小时)
├── 批次 1: MainActivity + DrawerFragment (5-7小时)
├── 批次 2: TimedTaskSettingActivity (并行, 3-5小时)
└── 批次 4: Material3 主题升级 (并行, 4-5小时)

Day 2 (8小时)
├── 批次 3: EditActivity + EditorView 合并重构 (6-9小时)
└── 继续测试验证

Day 3 (6-8小时)
├── 批次 5: 清理与测试 (3-4小时)
├── 问题修复
└── 最终验证
```

**优化后总耗时：约 22-25 小时（2.5-3 天）**

---

## 五、风险控制

### 5.1 高风险问题

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| EditorView inflate 时机错误 | 编辑器崩溃 | 在构造函数最后调用 init() |
| CodeEditor 功能丢失 | 核心功能失效 | 使用 AndroidView 嵌入保留 |
| Fragment 切换异常 | 页面显示错误 | 保持原有 Fragment 事务逻辑 |
| 调试器断点功能失效 | 调试不可用 | 仔细迁移断点回调接口 |

### 5.2 中风险问题

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| EventBus 生命周期 | 内存泄漏 | 确保 register/unregister 配对 |
| RxJava 订阅泄漏 | 内存泄漏 | 使用 CompositeDisposable 统一管理 |
| ExpandableRelativeLayout 替换 | 动画效果丢失 | 使用 Material3 AnimatedVisibility |
| 权限请求回调 | 功能异常 | 兼容 ActivityResultContracts |

### 5.3 低风险问题

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 状态保存/恢复 | 状态丢失 | 手动处理 onSaveInstanceState |
| TimePicker 样式差异 | 视觉不一致 | Material3 样式统一 |
| 主题颜色不一致 | 视觉不一致 | 手动同步 XML 和 Compose 颜色 |

---

## 六、成功标准

### 6.1 功能验证

- [ ] 编译通过，无错误
- [ ] 所有 Activity 可正常启动
- [ ] 编辑器功能完整（语法高亮、断点、补全）
- [ ] 定时任务创建功能正常
- [ ] 日志面板显示/隐藏正常
- [ ] 亮色/暗色主题切换正常

### 6.2 性能指标

- [ ] 构建速度提升约 30%
- [ ] APK 体积增量 < 4MB
- [ ] 无内存泄漏

### 6.3 代码质量

- [ ] 无废弃依赖（butterknife, androidannotations）
- [ ] 无 KAPT 插件
- [ ] 代码风格一致

---

## 七、技术要点速查

### 7.1 @EActivity → ViewBinding

```java
// 迁移前
@EActivity(R.layout.activity_xxx)
public class XxxActivity extends BaseActivity {
    @ViewById(R.id.view) View mView;
    @Click(R.id.button) void onClick() { }
    @AfterViews void init() { }
}

// 迁移后
public class XxxActivity extends BaseActivity {
    private ActivityXxxBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityXxxBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.button.setOnClickListener(v -> onClick());
        init();
    }
}
```

### 7.2 @EFragment → ViewBinding

```java
// 迁移前
@EFragment(R.layout.fragment_xxx)
public class XxxFragment extends Fragment {
    @ViewById(R.id.view) View mView;
    @AfterViews void init() { }
}

// 迁移后
public class XxxFragment extends Fragment {
    private FragmentXxxBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentXxxBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
```

### 7.3 @EViewGroup → ViewBinding

```java
// 迁移前
@EViewGroup(R.layout.view_xxx)
public class XxxView extends FrameLayout {
    @ViewById(R.id.view) View mView;
    @AfterViews void init() { }
}

// 迁移后
public class XxxView extends FrameLayout {
    private ViewXxxBinding binding;
    
    public XxxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        inflate(context, R.layout.view_xxx, this);
        binding = ViewXxxBinding.bind(this);
        init();
    }
}
```

### 7.4 @CheckedChange 迁移

```java
// 迁移前
@CheckedChange({R.id.radio1, R.id.radio2})
void onCheckedChanged(CompoundButton button, boolean isChecked) { }

// 迁移后
private void setupListeners() {
    CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
        onCheckedChanged(button, isChecked);
    };
    binding.radio1.setOnCheckedChangeListener(listener);
    binding.radio2.setOnCheckedChangeListener(listener);
}
```

---

## 八、变更记录

| 日期 | 操作 | 状态 |
|------|------|------|
| 2026-03-10 | 创建最终迁移计划 | 完成 |
| 2026-03-10 | 制定合并重构策略 | 完成 |
| 2026-03-10 | 制定混合架构策略 | 完成 |
| 2026-03-10 | 细化5个批次规划 | 完成 |
| 2026-03-10 | 添加逐步验证策略 | 完成 |
| 2026-03-10 | 添加各批次验证清单 | 完成 |
| 2026-03-10 | 添加版本号规划 | 完成 |

---

## 九、参考资源

### 已有文档

- `KSP_FULL_MIGRATION_PLAN.md` - KSP 完整迁移分析
- `KSP_MIGRATION_LOG.md` - KSP 迁移日志
- `MATERIAL3_MIGRATION_PLAN.md` - Material3 迁移计划
- `MATERIAL3_MIGRATION_LOG.md` - Material3 迁移日志

### 已完成组件

- `Theme.kt` - Material3 主题配置
- `Color.kt` - 颜色定义
- `Type.kt` - 字体样式
- `LogSheet.kt` - 日志面板组件
- `EditorModel.kt` - 编辑器 ViewModel

### 关键文件路径

| 组件 | 路径 |
|------|------|
| MainActivity | `app/src/main/java/org/autojs/autojs/ui/main/MainActivity.java` |
| DrawerFragment | `app/src/main/java/org/autojs/autojs/ui/main/drawer/DrawerFragment.java` |
| EditActivity | `app/src/main/java/org/autojs/autojs/ui/edit/EditActivity.java` |
| EditorView | `app/src/main/java/org/autojs/autojs/ui/edit/EditorView.java` |
| TimedTaskSettingActivity | `app/src/main/java/org/autojs/autojs/ui/timing/TimedTaskSettingActivity.java` |

---

*文档结束*
