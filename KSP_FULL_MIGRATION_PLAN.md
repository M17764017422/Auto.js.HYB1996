# KSP 完全迁移分析计划

## 一、当前状态

### 1.1 KAPT 注解处理器清单

| 注解处理器 | 版本 | KSP 支持 | 使用次数 | 文件数 | 状态 |
|-----------|------|---------|---------|--------|------|
| AndroidAnnotations | 4.8.0 | ❌ 不支持 | ~158 | 25 | 维护模式 |
| ButterKnife | 10.2.3 | ❌ 已废弃 | ~100 | 16 | 已停止维护 |
| Glide | 4.14.2 | ✅ **已迁移** | 无自定义 | - | KSP ✅ |

### 1.2 AndroidAnnotations 使用统计

**注解类型统计**：

| 注解类型 | 使用次数 | 说明 |
|---------|---------|------|
| @EActivity | 16 | 增强 Activity |
| @EFragment | 8 | 增强 Fragment |
| @EViewGroup | 1 | 增强 ViewGroup (EditorView) |
| @ViewById | 68 | 视图注入 |
| @AfterViews | 22 | 注入后回调 |
| @Click | 26 | 点击事件 |
| @CheckedChange | 1 | 选中状态变化 (TimedTaskSettingActivity) |
| 生成类引用 | 28 | `Activity_.class` / `Activity_.intent()` |

**文件清单与复杂度**：

| 复杂度 | 文件 | @ViewById | @Click | @AfterViews | 代码行数 |
|--------|------|-----------|--------|-------------|----------|
| 🟢 简单 | AboutActivity | 1 | 6 | 1 | ~100 |
| 🟢 简单 | LogActivity | 1 | 1 | 1 | ~80 |
| 🟢 简单 | WebActivity | 1 | 0 | 1 | ~70 |
| 🟢 简单 | DocumentationActivity | 1 | 0 | 1 | ~60 |
| 🟢 简单 | ScriptWidgetSettingsActivity | 0 | 0 | 1 | ~80 |
| 🟢 简单 | TaskerScriptEditActivity | 1 | 0 | 1 | ~100 |
| 🟢 简单 | TaskPrefEditActivity | 0 | 1 | 1 | ~80 |
| 🟢 简单 | ShortcutIconSelectActivity | 1 | 0 | 1 | ~120 |
| 🟢 简单 | SearchToolbarFragment | 0 | 0 | 0 | ~150 |
| 🟢 简单 | NormalToolbarFragment | 0 | 0 | 0 | ~100 |
| 🟡 中等 | RegisterActivity | 4 | 1 | 1 | ~150 |
| 🟡 中等 | LoginActivity | 3 | 2 | 1 | ~150 |
| 🟡 中等 | CommunityFragment | 1 | 0 | 1 | ~200 |
| 🟡 中等 | DocsFragment | 1 | 0 | 1 | ~180 |
| 🟡 中等 | TaskManagerFragment | 3 | 0 | 1 | ~120 |
| 🟡 中等 | MyScriptListFragment | 1 | 0 | 1 | ~250 |
| 🟡 中等 | SettingsActivity | 0 | 0 | 1 | ~150 |
| 🟡 中等 | DebugToolbarFragment | 0 | 5 | 0 | ~200 |
| 🔴 复杂 | BuildActivity | 8 | 4 | 1 | ~300 |
| 🔴 复杂 | ProjectConfigActivity | 6 | 2 | 1 | ~280 |
| 🔴 复杂 | MainActivity | 3 | 2 | 1 | ~400 |
| 🔴 复杂 | DrawerFragment | 5 | 1 | 1 | ~518 |
| 🔴 复杂 | EditActivity | 1 | 0 | 1 | ~350 |
| 🔴 复杂 | TimedTaskSettingActivity | 17 | 2 | 1 | ~438 |
| 🔴 最复杂 | EditorView (@EViewGroup) | 9 | 0 | 1 | ~788 |

### 1.3 ButterKnife 使用统计

| 注解类型 | 使用次数 | 涉及文件 | 说明 |
|---------|---------|----------|------|
| @BindView | 57 | 16 | 视图绑定 |
| @OnClick | 34 | 10 | 点击事件 |
| @Optional | 15 | 2 | 可选绑定（无 ViewBinding 替代） |
| @OnCheckedChanged | 3 | 3 | 选中变化 |
| @OnTextChanged | 1 | 1 | 文本变化 |
| ButterKnife.bind() | 37 | 16 | 绑定调用 |

**按文件复杂度分类**：

| 复杂度 | 文件数 | 文件列表 | 预估时间 |
|--------|--------|----------|----------|
| 🟢 简单 | 8 | AvatarView, TextSizeSettingDialogBuilder, FunctionsKeyboardView, ScriptLoopDialog, DrawerMenuItemViewHolder, ExplorerProjectToolbar, OptionListView, OperationDialogBuilder | 2-3小时 |
| 🟡 中等 | 5 | ShortcutCreateActivity, TaskListRecyclerView, FileChooseListView, FindOrReplaceDialogBuilder, CodeGenerateDialog | 4-6小时 |
| 🔴 复杂 | 3 | **CircularMenu** (15 @Optional), **ExplorerView** (735行, 多ViewHolder), CommunityWebView | 6-10小时 |

---

## 二、迁移难点分析

### 2.1 AndroidAnnotations → 标准生命周期（最难）

#### 技术难点分析

**难点 1：架构级注解重构**

```java
// AndroidAnnotations 写法
@EActivity(R.layout.activity_about)
public class AboutActivity extends BaseActivity {
    @ViewById(R.id.version) TextView mVersion;
    
    @AfterViews
    void setUpViews() {
        mVersion.setText("Version " + BuildConfig.VERSION_NAME);
    }
    
    @Click(R.id.github)
    void openGitHub() {
        IntentTool.browse(this, getString(R.string.my_github));
    }
}

// 标准写法
public class AboutActivity extends BaseActivity {
    private ActivityAboutBinding binding;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.version.setText("Version " + BuildConfig.VERSION_NAME);
        binding.github.setOnClickListener(v -> openGitHub());
    }
    
    private void openGitHub() {
        IntentTool.browse(this, getString(R.string.my_github));
    }
}
```

**难点 2：生成类引用更新（28 处）**

```java
// AndroidAnnotations 写法
startActivity(new Intent(this, SettingsActivity_.class));
LogActivity_.intent(this).start();
MainActivity_.intent(this).start();

// 标准写法
startActivity(new Intent(this, SettingsActivity.class));
startActivity(new Intent(this, LogActivity.class));
startActivity(new Intent(this, MainActivity.class));
```

**难点 3：Fragment @EFragment 重构**

```java
// AndroidAnnotations 写法
@EFragment(R.layout.fragment_drawer)
public class DrawerFragment extends Fragment {
    @ViewById(R.id.username) TextView mUserName;
    
    @AfterViews
    void setUpViews() { ... }
}

// 标准写法
public class DrawerFragment extends Fragment {
    private FragmentDrawerBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, 
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDrawerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // setUpViews() 内容移到这里
    }
}
```

**难点 4：@EViewGroup 重构（EditorView.java，788 行）**

```java
// AndroidAnnotations 写法
@EViewGroup(R.layout.editor_view)
public class EditorView extends FrameLayout {
    @ViewById(R.id.editor) CodeEditor mEditor;
    @ViewById(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    // ... 9 个 @ViewById
    
    @AfterViews
    void setupViews() { ... }
}

// 标准写法
public class EditorView extends FrameLayout {
    private EditorViewBinding binding;
    
    public EditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        binding = EditorViewBinding.inflate(LayoutInflater.from(context), this);
        initViews();
    }
    
    private void initViews() {
        // @AfterViews 内容移到这里
        // 访问视图：binding.editor, binding.drawerLayout
    }
}
```

**难点 5：@CheckedChange 事件处理**

```java
// AndroidAnnotations 写法 (TimedTaskSettingActivity.java)
@CheckedChange({R.id.daily_task_radio, R.id.weekly_task_radio, 
                R.id.disposable_task_radio, R.id.run_on_broadcast})
void onTimingMethodChanged(CompoundButton button, boolean isChecked) { ... }

// 标准写法
private void setupCheckedChangeListeners() {
    CompoundButton.OnCheckedChangeListener listener = (button, isChecked) -> {
        onTimingMethodChanged(button, isChecked);
    };
    binding.dailyTaskRadio.setOnCheckedChangeListener(listener);
    binding.weeklyTaskRadio.setOnCheckedChangeListener(listener);
    binding.disposableTaskRadio.setOnCheckedChangeListener(listener);
    binding.runOnBroadcast.setOnCheckedChangeListener(listener);
}
```

#### 文件迁移工作量估算

| 复杂度 | 文件数 | 每个耗时 | 小计 |
|--------|--------|----------|------|
| 🟢 简单 | 10 | 30分钟-1小时 | 5-10 小时 |
| 🟡 中等 | 8 | 1-2小时 | 8-16 小时 |
| 🔴 复杂 | 6 | 2-4小时 | 12-24 小时 |
| 🔴 最复杂 (EditorView) | 1 | 4-6小时 | 4-6 小时 |
| **生成类引用更新** | 28 处 | 5分钟/处 | 2-3 小时 |
| **测试验证** | - | - | 8-12 小时 |

**AndroidAnnotations 迁移总计：39-71 小时（约 5-9 个工作日）**

### 2.2 ButterKnife → ViewBinding（中等）

#### 文件清单与工作量

| 文件 | 注解数 | 复杂度 | 迁移要点 |
|------|--------|--------|----------|
| AvatarView.java | 2 @BindView | 🟢 | 自定义View，`binding = XxxBinding.inflate()` |
| TextSizeSettingDialogBuilder.java | 2 @BindView | 🟢 | Dialog，`binding = XxxBinding.bind(view)` |
| FunctionsKeyboardView.java | 2 @BindView | 🟢 | 自定义View，同上 |
| ScriptLoopDialog.java | 3 @BindView | 🟢 | Dialog，同上 |
| DrawerMenuItemViewHolder.java | 4 @BindView | 🟢 | ViewHolder，`XxxBinding.bind(itemView)` |
| ExplorerProjectToolbar.java | 1 @BindView + 3 @OnClick | 🟢 | 自定义View，手动设置点击监听 |
| OptionListView.java | 2 @BindView | 🟢 | ViewHolder |
| OperationDialogBuilder.java | 2 @BindView | 🟢 | ViewHolder |
| ShortcutCreateActivity.java | 3 @BindView + 1 @OnClick | 🟡 | Dialog 中绑定 |
| TaskListRecyclerView.java | 3 @BindView + 1 @OnClick | 🟡 | 内部 ViewHolder 类 |
| FileChooseListView.java | 7 @BindView + 2 @OnClick + 1 @OnCheckedChanged | 🟡 | 多 ViewHolder + 事件监听 |
| FindOrReplaceDialogBuilder.java | 5 @BindView + 2 事件 | 🟡 | @OnCheckedChanged + @OnTextChanged |
| CodeGenerateDialog.java | 3 @BindView + 1 @OnCheckedChanged | 🟡 | 内部 ViewHolder + 事件 |
| **CircularMenu.java** | 13 @OnClick + 15 @Optional | 🔴 | 悬浮窗绑定，@Optional 无直接替代 |
| **ExplorerView.java** | 19 @BindView + 10 @OnClick | 🔴 | 多内部 ViewHolder，735 行代码 |
| CommunityWebView.java | 2 @OnClick + 2 @Optional | 🔴 | @Optional 处理 |

#### 技术难点与解决方案

**难点 1：@Optional 可选绑定（最大难点）**

```java
// ButterKnife 写法
@Optional
@OnClick(R.id.script_list)
void showScriptList() { ... }

// ViewBinding 替代方案
View scriptList = binding.getRoot().findViewById(R.id.script_list);
if (scriptList != null) {
    scriptList.setOnClickListener(v -> showScriptList());
}
```

**影响范围**：
- CircularMenu.java：15 处 @Optional
- CommunityWebView.java：2 处 @Optional

**难点 2：事件监听器迁移**

```java
// ButterKnife 写法
@OnCheckedChanged(R.id.checkbox_replace_all)
void syncWithReplaceCheckBox() { ... }

// ViewBinding 替代方案
binding.checkboxReplaceAll.setOnCheckedChangeListener((btn, isChecked) -> {
    syncWithReplaceCheckBox();
});
```

**难点 3：悬浮窗特殊绑定**

```java
// CircularMenu.java 特殊情况
ButterKnife.bind(CircularMenu.this, menu); // 在非 Activity/Fragment 中绑定

// ViewBinding 替代方案
CircularActionMenuBinding binding = CircularActionMenuBinding.bind(menu);
```

#### 迁移示例对比

**简单案例：AvatarView.java**

```java
// 迁移前
@BindView(R.id.icon_text) TextView mIconText;
@BindView(R.id.icon) RoundedImageView mIcon;

private void init() {
    inflate(getContext(), R.layout.avatar_view, this);
    ButterKnife.bind(this);
}

// 迁移后
private AvatarViewBinding binding;

private void init() {
    binding = AvatarViewBinding.inflate(LayoutInflater.from(getContext()), this);
    // 字段访问：mIconText → binding.iconText
    //          mIcon → binding.icon
}
```

**复杂案例：CircularMenu.java**

```java
// 迁移前 (15个 @Optional 方法)
@Optional @OnClick(R.id.script_list) void showScriptList() { ... }
@Optional @OnClick(R.id.record) void startRecord() { ... }
// ... 还有 13 个

// 迁移后
private void setupClickListeners(CircularActionMenuBinding binding) {
    View scriptList = binding.getRoot().findViewById(R.id.script_list);
    if (scriptList != null) {
        scriptList.setOnClickListener(v -> showScriptList());
    }
    // ... 逐个设置，共 15 处判空
}
```

#### 工作量估算

| 阶段 | 任务 | 时间 |
|------|------|------|
| 准备 | 启用 ViewBinding，分析布局文件 | 30分钟 |
| 简单文件 | 8 个文件迁移 | 2-3小时 |
| 中等文件 | 5 个文件迁移 + 事件监听 | 4-6小时 |
| 复杂文件 | ExplorerView (735行) | 3-4小时 |
| 复杂文件 | CircularMenu (@Optional处理) | 2-3小时 |
| 复杂文件 | CommunityWebView | 1小时 |
| 测试 | 编译验证 + 功能回归测试 | 2-4小时 |
| 清理 | 移除 ButterKnife 依赖 | 30分钟 |

**ButterKnife 迁移总计：15-22 小时（约 2-3 个工作日）**

### 2.3 Glide（✅ 已完成）

**状态：已成功迁移到 KSP**

~~**问题 1：KAPT+KSP 共存冲突**~~
- ~~之前的测试表明两者共存会导致处理器发现失败~~
- **已解决**：使用正确的 artifact `com.github.bumptech.glide:ksp:4.14.2`

~~**问题 2：版本升级**~~
- **已完成**：从 4.12.0 升级到 4.14.2
- API 无变更，平滑升级

**关键发现**：之前迁移失败是因为使用了错误的 artifact 名称：
- 错误：`ksp 'com.github.bumptech.glide:compiler:4.16.0'`
- 正确：`ksp 'com.github.bumptech.glide:ksp:4.14.2'`

---

## 三、渐进式迁移路径（简单优先）

### 策略说明

采用**渐进式迁移**策略，每次完成一批简单任务后验证功能，确保项目始终可运行。

```
┌─────────────────────────────────────────────────────────────┐
│ 批次 A: ButterKnife 简单文件（2-3小时）                       │
│ 目标：熟悉迁移流程，建立信心                                  │
├─────────────────────────────────────────────────────────────┤
│ □ AvatarView.java - 2 @BindView                             │
│ □ TextSizeSettingDialogBuilder.java - 2 @BindView           │
│ □ FunctionsKeyboardView.java - 2 @BindView                  │
│ □ ScriptLoopDialog.java - 3 @BindView                       │
│ □ DrawerMenuItemViewHolder.java - 4 @BindView               │
│ □ OptionListView.java - 2 @BindView                         │
│ □ OperationDialogBuilder.java - 2 @BindView                 │
│ □ ExplorerProjectToolbar.java - 1 @BindView + 3 @OnClick    │
│                                                              │
│ ✅ 验证：编译通过，相关功能测试                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 B: AndroidAnnotations 简单文件（3-5小时）                │
│ 目标：熟悉 AA 迁移模式                                       │
├─────────────────────────────────────────────────────────────┤
│ □ AboutActivity.java - 1 @ViewById + 6 @Click               │
│ □ LogActivity.java - 1 @ViewById + 1 @Click                 │
│ □ WebActivity.java - 1 @ViewById                            │
│ □ DocumentationActivity.java - 1 @ViewById                  │
│ □ ScriptWidgetSettingsActivity.java - 1 @AfterViews         │
│ □ TaskerScriptEditActivity.java - 1 @ViewById               │
│ □ TaskPrefEditActivity.java - 1 @Click                      │
│ □ ShortcutIconSelectActivity.java - 1 @ViewById             │
│                                                              │
│ ✅ 验证：Activity 启动测试，基础功能验证                      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 C: Fragment 简单迁移（2-3小时）                          │
│ 目标：Fragment 迁移模式                                      │
├─────────────────────────────────────────────────────────────┤
│ □ SearchToolbarFragment.java - 仅 @EFragment                │
│ □ NormalToolbarFragment.java - 仅 @EFragment                │
│ □ CommunityFragment.java - 1 @ViewById                      │
│ □ DocsFragment.java - 1 @ViewById                           │
│ □ TaskManagerFragment.java - 3 @ViewById                    │
│                                                              │
│ ✅ 验证：Fragment 切换测试，UI 显示验证                       │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 D: ButterKnife 中等文件（4-6小时）                       │
│ 目标：处理事件监听器迁移                                     │
├─────────────────────────────────────────────────────────────┤
│ □ ShortcutCreateActivity.java - 3 @BindView + 1 @OnClick    │
│ □ TaskListRecyclerView.java - 内部 ViewHolder               │
│ □ FileChooseListView.java - 7 @BindView + 多事件            │
│ □ FindOrReplaceDialogBuilder.java - @OnCheckedChanged       │
│ □ CodeGenerateDialog.java - 内部 ViewHolder + 事件          │
│                                                              │
│ ✅ 验证：文件选择、查找替换、代码生成功能                     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 E: AndroidAnnotations 中等文件（6-10小时）               │
│ 目标：用户认证和设置相关                                     │
├─────────────────────────────────────────────────────────────┤
│ □ RegisterActivity.java - 4 @ViewById + 1 @Click            │
│ □ LoginActivity.java - 3 @ViewById + 2 @Click               │
│ □ MyScriptListFragment.java - @EFragment                    │
│ □ SettingsActivity.java - @EActivity                        │
│ □ DebugToolbarFragment.java - 5 @Click                      │
│                                                              │
│ ✅ 验证：登录注册、设置功能测试                               │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 F: 引用更新（2-3小时）                                   │
│ 目标：更新所有生成类引用                                     │
├─────────────────────────────────────────────────────────────┤
│ □ 更新 28 处 Activity_.class / Activity_.intent()           │
│   - SettingsActivity_ → SettingsActivity                    │
│   - LogActivity_ → LogActivity                              │
│   - MainActivity_ → MainActivity                            │
│   - EditActivity_ → EditActivity                            │
│   - BuildActivity_ → BuildActivity                          │
│   - 其他引用                                                 │
│                                                              │
│ ✅ 验证：所有页面跳转正常                                     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 G: 核心功能文件（8-12小时）                              │
│ 目标：主要业务逻辑迁移                                       │
├─────────────────────────────────────────────────────────────┤
│ □ BuildActivity.java - 8 @ViewById + 4 @Click (~300行)      │
│ □ ProjectConfigActivity.java - 6 @ViewById + 2 @Click       │
│ □ MainActivity.java - 3 @ViewById + 2 @Click (~400行)       │
│ □ DrawerFragment.java - 5 @ViewById + 1 @Click (~518行)     │
│ □ EditActivity.java - 1 @ViewById (~350行)                  │
│ □ TimedTaskSettingActivity.java - 17 @ViewById + 事件       │
│                                                              │
│ ✅ 验证：主界面、编辑器、定时任务、打包功能                   │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 H: 复杂文件（8-12小时）                                  │
│ 目标：处理最复杂的情况                                       │
├─────────────────────────────────────────────────────────────┤
│ □ EditorView.java - @EViewGroup + 9 @ViewById (~788行)      │
│   - 需要重构自定义 ViewGroup 初始化逻辑                      │
│   - 涉及代码编辑器核心功能                                   │
│                                                              │
│ □ ButterKnife 复杂文件：                                     │
│   - CircularMenu.java - 15 处 @Optional 需判空处理          │
│   - ExplorerView.java - 735 行，多内部 ViewHolder           │
│   - CommunityWebView.java - @Optional 处理                  │
│                                                              │
│ ✅ 验证：编辑器完整功能、悬浮窗、文件浏览                     │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 批次 I: 清理与收尾（2-3小时）                                 │
│ 目标：移除废弃依赖，完成迁移                                 │
├─────────────────────────────────────────────────────────────┤
│ □ 移除 butterknife 依赖                                     │
│ □ 移除 androidannotations 依赖                              │
│ □ 移除 kapt 相关配置                                         │
│ □ 移除 kotlin-kapt 插件                                     │
│ □ 全局搜索确认无遗漏                                         │
│                                                              │
│ ✅ 最终验证：完整回归测试                                    │
└─────────────────────────────────────────────────────────────┘
```

### 渐进式迁移优势

| 优势 | 说明 |
|------|------|
| **低风险** | 每批次独立验证，问题早发现早修复 |
| **可中断** | 随时可暂停，不影响项目正常使用 |
| **渐进学习** | 从简单到复杂，逐步掌握迁移技巧 |
| **信心建立** | 完成简单任务后更有动力继续 |

### 批次工作量汇总

| 批次 | 内容 | 工作量 | 累计 |
|------|------|--------|------|
| A | ButterKnife 简单 | 2-3小时 | 2-3小时 |
| B | AA 简单 Activity | 3-5小时 | 5-8小时 |
| C | AA 简单 Fragment | 2-3小时 | 7-11小时 |
| D | ButterKnife 中等 | 4-6小时 | 11-17小时 |
| E | AA 中等 | 6-10小时 | 17-27小时 |
| F | 引用更新 | 2-3小时 | 19-30小时 |
| G | 核心功能 | 8-12小时 | 27-42小时 |
| H | 复杂文件 | 8-12小时 | 35-54小时 |
| I | 清理收尾 | 2-3小时 | 37-57小时 |
| **测试** | 各批次验证 | 8-12小时 | **45-69小时** |

---

## 四、关键问题清单

### 4.1 技术问题

| 问题 | 影响 | 解决方案 | 状态 |
|------|------|----------|------|
| ~~KAPT+KSP 共存~~ | ~~处理器发现失败~~ | ~~必须完全移除 KAPT 后再迁移~~ | ✅ 已解决（可共存） |
| @Optional 无替代 | 15 处需要处理 | 运行时判空或多布局方案 | 待处理 |
| 生成类引用 | 50+ 处需要更新 | 全局搜索替换 | 待处理 |
| Dialog 生命周期 | 内存泄漏风险 | 手动管理 ViewBinding | 待处理 |

### 4.2 代码变更

| 变更类型 | 数量 | 风险 |
|---------|------|------|
| Activity 重构 | 16 | 高 |
| Fragment 重构 | 8 | 中 |
| ViewHolder 重构 | 10 | 中 |
| Dialog 重构 | 6 | 高 |
| 自定义 View 重构 | 4 | 低 |

### 4.3 测试覆盖

| 测试类型 | 范围 |
|---------|------|
| 功能测试 | 所有 Activity/Fragment |
| UI 测试 | 所有 Dialog 和 ViewHolder |
| 内存测试 | Dialog 生命周期 |
| 回归测试 | 全量功能验证 |

---

## 五、建议方案

### 5.1 短期方案（推荐）

**保持现状**
- KAPT 可以正常工作
- AndroidAnnotations 和 ButterKnife 无安全风险
- Glide 已成功迁移到 KSP

**理由**：
- 迁移总工作量大（7-12 天）
- 风险高，需要全面回归测试
- 收益有限（主要是构建速度提升）

### 5.2 中期方案

**渐进迁移 - 先迁移 ButterKnife**

| 项目 | 详情 |
|------|------|
| 工作量 | 15-22 小时（2-3 个工作日） |
| 风险 | 中 |
| 收益 | 移除废弃依赖，减少 KAPT 处理量 |
| 前置条件 | ViewBinding 已启用 |

**迁移步骤**：
1. 启用 ViewBinding（已启用）
2. 按复杂度分批迁移文件
3. 处理 @Optional 可选绑定
4. 移除 ButterKnife 依赖
5. 功能回归测试

### 5.3 长期方案

**完全重构 - 移除 AndroidAnnotations**

| 项目 | 详情 |
|------|------|
| 工作量 | 39-71 小时（5-9 个工作日） |
| 风险 | 高 |
| 收益 | 完全移除 KAPT，构建速度提升 |

**重构内容**：
1. 16 个 Activity - 移除 @EActivity，改用 ViewBinding
2. 8 个 Fragment - 移除 @EFragment
3. 1 个 ViewGroup - 重构 EditorView (788行)
4. 28 处生成类引用 - `Activity_.class` → `Activity.class`
5. 1 处 @CheckedChange - 改用标准监听器

### 5.4 总体工作量汇总

| 阶段 | 任务 | 工作量 | 风险 | 收益 |
|------|------|--------|------|------|
| ✅ 已完成 | Glide → KSP | 已完成 | 无 | 构建提速 |
| 阶段 1 | ButterKnife → ViewBinding | 2-3 天 | 中 | 移除废弃依赖 |
| 阶段 2 | AndroidAnnotations 移除 | 5-9 天 | 高 | 移除 KAPT |
| 阶段 3 | 移除 KAPT 插件 | 1 小时 | 低 | 构建提速 |
| **总计** | **完全迁移到 KSP** | **7-12 天** | **高** | **完全移除 KAPT** |

### 5.5 收益分析

| 指标 | 当前 (KAPT) | 迁移后 (KSP) | 提升 |
|------|-------------|--------------|------|
| 注解处理时间 | ~30秒 | ~10秒 | ~66% |
| 增量构建 | 较慢 | 更快 | 显著 |
| 代码生成 | Java Stub | 直接处理 | 更准确 |
| 维护成本 | 依赖废弃库 | 现代化架构 | 降低 |

---

## 六、环境信息

| 组件 | 版本 |
|------|------|
| Kotlin | 1.9.25 |
| KSP | 1.9.25-1.0.20 |
| AGP | 8.2.2 |
| Gradle | 8.7 |
| JDK | 17 (JetBrains Runtime) |
| ViewBinding | 已启用 |

---

## 七、参考链接

- [KSP 官方文档](https://kotlinlang.org/docs/ksp-overview.html)
- [KAPT 到 KSP 迁移指南](https://kotlinlang.org/docs/ksp-quickstart.html#migrate-from-kapt)
- [AndroidAnnotations GitHub](https://github.com/androidannotations/androidannotations)
- [ButterKnife 官方声明（已废弃）](https://github.com/JakeWharton/butterknife)
- [Glide KSP 支持](https://github.com/bumptech/glide/issues/4951)
- [ViewBinding 官方指南](https://developer.android.com/topic/libraries/view-binding)

---

## 八、变更记录

| 日期 | 操作 | 结果 |
|------|------|------|
| 2026-03-09 | Glide 4.15.1 → KSP (错误 artifact) | 失败 |
| 2026-03-09 | Glide 4.16.0 → KSP (错误 artifact) | 失败 |
| 2026-03-09 | 创建完整迁移计划 | 完成 |
| 2026-03-09 | Glide 4.14.2 → KSP (正确 artifact) | **成功** ✅ |
| 2026-03-09 | 发布 v0.85.3-alpha 测试版 | **成功** ✅ |
| 2026-03-10 | 完善 ButterKnife 迁移分析 | 完成 |
| 2026-03-10 | 添加文件复杂度分类和迁移示例 | 完成 |
| 2026-03-10 | 更新工作量估算（15-22小时） | 完成 |
| 2026-03-10 | 深度分析 AndroidAnnotations 迁移 | 完成 |
| 2026-03-10 | 统计 25 个文件、28 处生成类引用 | 完成 |
| 2026-03-10 | 更新总工作量（55-94小时，7-12天） | 完成 |
| 2026-03-10 | 调整为渐进式迁移策略（9批次） | 完成 |
| 2026-03-10 | 简单任务优先，每批次独立验证 | 完成 |
| 2026-03-10 | 添加与 Material3 并行执行协调 | 完成 |

---

## 九、结论

### 已完成

- **Glide KSP 迁移**：✅ 成功（使用正确的 artifact `ksp:4.14.2`）
- **KAPT + KSP 共存**：✅ 验证可行

### 渐进式迁移工作量

| 批次 | 内容 | 工作量 | 可暂停 |
|------|------|--------|--------|
| A | ButterKnife 简单文件 | 2-3小时 | ✅ |
| B | AA 简单 Activity | 3-5小时 | ✅ |
| C | AA 简单 Fragment | 2-3小时 | ✅ |
| D | ButterKnife 中等文件 | 4-6小时 | ✅ |
| E | AA 中等文件 | 6-10小时 | ✅ |
| F | 引用更新 | 2-3小时 | ✅ |
| G | 核心功能文件 | 8-12小时 | ✅ |
| H | 复杂文件 | 8-12小时 | ✅ |
| I | 清理收尾 | 2-3小时 | - |
| **总计** | **渐进式迁移** | **45-69小时 (6-9天)** | - |

### 当前配置

```groovy
// Glide - KSP ✅
implementation 'com.github.bumptech.glide:glide:4.14.2'
ksp 'com.github.bumptech.glide:ksp:4.14.2'

// AndroidAnnotations - KAPT (保留)
kapt "org.androidannotations:androidannotations:4.8.0"

// ButterKnife - KAPT (建议迁移)
kapt 'com.jakewharton:butterknife-compiler:10.2.3'
```

### 推荐执行策略

| 阶段 | 建议 | 理由 |
|------|------|------|
| 批次 A-C | **立即可执行** | 简单任务，风险低，建立信心 |
| 批次 D-E | **条件允许时执行** | 中等难度，需一定经验 |
| 批次 F-I | **规划后执行** | 核心功能，需完整测试 |

### 渐进式迁移优势

| 优势 | 说明 |
|------|------|
| 低风险 | 每批次独立验证，问题早发现 |
| 可中断 | 随时可暂停，不影响项目使用 |
| 渐进学习 | 从简单到复杂，逐步掌握 |
| 信心建立 | 完成简单任务后更有动力 |

### 风险提示

| 风险类型 | 描述 | 缓解措施 |
|---------|------|----------|
| 功能回归 | 重构可能引入 bug | 每批次验证 |
| 生命周期问题 | Fragment/Dialog 内存泄漏 | 代码审查 |
| 生成类引用遗漏 | Activity_.class 未更新 | 批次 F 集中处理 |
| EditorView 复杂性 | 788 行核心代码 | 批次 H 单独处理 |

### 决策建议

| 场景 | 建议 |
|------|------|
| 项目稳定运行中 | 保持现状，或执行批次 A-C |
| 有空闲时间 | 按批次渐进迁移 |
| 计划大规模重构 | 一并迁移，减少技术债 |
| 构建速度瓶颈 | 完成全部迁移后移除 KAPT |

---

## 十、与 Material3 计划并行执行协调

### 10.1 可并行任务

| KSP 批次 | Material3 批次 | 并行状态 | 说明 |
|----------|---------------|----------|------|
| A: ButterKnife 简单 | 1-2: Compose 配置 | ✅ 可并行 | 无文件冲突 |
| A: ButterKnife 简单 | 3-4: 新建文件 | ✅ 可并行 | 无文件冲突 |
| B: AA 简单 Activity | 1-6: 全部准备 | ✅ 可并行 | 无文件冲突 |
| C: AA 简单 Fragment | 1-6: 全部准备 | ✅ 可并行 | 无文件冲突 |
| D: ButterKnife 中等 | 1-6: 全部准备 | ✅ 可并行 | 无文件冲突 |
| E: AA 中等文件 | 1-6: 全部准备 | ✅ 可并行 | 无文件冲突 |
| F: 引用更新 | 1-6: 全部准备 | ✅ 可并行 | 无文件冲突 |

### 10.2 需协调任务

| KSP 批次 | Material3 批次 | 冲突文件 | 解决方案 |
|----------|---------------|----------|----------|
| G: EditActivity | 7: EditActivity | `EditActivity.java` | **合并重构** |

### 10.3 合并重构 EditActivity

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

合并工作量: 8-12小时（vs 分开执行 6-10小时重复）
```

### 10.4 并行执行时间对比

| 执行方式 | 总耗时 | 说明 |
|----------|--------|------|
| 串行执行 | 56-83小时 (7-11天) | 两计划依次执行 |
| 并行执行 | 37-57小时 (5-7天) | 独立任务同时进行 |
| **节省** | **19-26小时 (2-4天)** | 约 35% |

### 10.5 推荐执行顺序

```
┌─────────────────────────────────────────────────────────────┐
│ 阶段 1: 并行执行 (19-30小时)                                 │
├─────────────────────────────────────────────────────────────┤
│ KSP: 批次 A-F                    Material3: 批次 1-6        │
│ 19-30小时                       3.5小时                     │
│                                                              │
│ ⏱️ 实际耗时: max(19-30h, 3.5h) = 19-30小时                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 阶段 2: 合并重构 (8-12小时)                                  │
├─────────────────────────────────────────────────────────────┤
│ EditActivity 合并重构                                        │
│ - KSP: 移除 @EActivity + ViewBinding                        │
│ - Material3: Compose + LogSheet                             │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 阶段 3: 收尾并行 (10-15小时)                                 │
├─────────────────────────────────────────────────────────────┤
│ KSP: 批次 H-I                   Material3: 批次 8-9         │
│ 10-15小时                       6-7小时                     │
│                                                              │
│ ⏱️ 实际耗时: max(10-15h, 6-7h) = 10-15小时                  │
└─────────────────────────────────────────────────────────────┘
```
