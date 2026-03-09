# KSP 完全迁移分析计划

## 一、当前状态

### 1.1 KAPT 注解处理器清单

| 注解处理器 | 版本 | KSP 支持 | 使用次数 | 文件数 | 状态 |
|-----------|------|---------|---------|--------|------|
| AndroidAnnotations | 4.8.0 | ❌ 不支持 | ~158 | 25 | 维护模式 |
| ButterKnife | 10.2.3 | ❌ 已废弃 | ~100 | 16 | 已停止维护 |
| Glide | 4.14.2 | ✅ **已迁移** | 无自定义 | - | KSP ✅ |

### 1.2 AndroidAnnotations 使用统计

| 注解类型 | 使用次数 | 说明 |
|---------|---------|------|
| @EActivity | 16 | 增强 Activity |
| @EFragment | 8 | 增强 Fragment |
| @EViewGroup | 1 | 增强 ViewGroup |
| @ViewById | 68 | 视图注入 |
| @AfterViews | 22 | 注入后回调 |
| @Click | 26 | 点击事件 |
| @UiThread | 16 | UI 线程执行 |
| @CheckedChange | 1 | 选中状态变化 |

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

### 2.1 AndroidAnnotations（最难）

**问题 1：无 KSP 支持**
- 官方未提供 KSP 支持，无迁移计划
- 项目处于维护模式，无重大更新

**问题 2：架构级注解**
- `@EActivity`/`@EFragment`/`@EViewGroup` 涉及组件生命周期
- 需要完全重构代码架构

**问题 3：生成类引用**
- 所有代码使用 `Activity_.class`（如 `SettingsActivity_.class`）
- 需要更新约 50+ 处 Intent 引用

**问题 4：@UiThread 注解**
- 16 处使用，需改用 `runOnUiThread`/`View.post`/协程

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

## 三、分阶段迁移路径

```
┌─────────────────────────────────────────────────────────────┐
│ 阶段 1: 移除 ButterKnife → ViewBinding                       │
│ 预估工作量：15-22 小时（2-3 个工作日）                        │
├─────────────────────────────────────────────────────────────┤
│ 任务清单：                                                   │
│                                                              │
│ 🟢 简单文件（2-3小时）：                                      │
│ □ AvatarView.java - 2 @BindView                             │
│ □ TextSizeSettingDialogBuilder.java - 2 @BindView           │
│ □ FunctionsKeyboardView.java - 2 @BindView                  │
│ □ ScriptLoopDialog.java - 3 @BindView                       │
│ □ DrawerMenuItemViewHolder.java - 4 @BindView               │
│ □ ExplorerProjectToolbar.java - 1 @BindView + 3 @OnClick    │
│ □ OptionListView.java - 2 @BindView                         │
│ □ OperationDialogBuilder.java - 2 @BindView                 │
│                                                              │
│ 🟡 中等文件（4-6小时）：                                      │
│ □ ShortcutCreateActivity.java - 3 @BindView + 1 @OnClick    │
│ □ TaskListRecyclerView.java - 内部 ViewHolder               │
│ □ FileChooseListView.java - 7 @BindView + 多事件            │
│ □ FindOrReplaceDialogBuilder.java - @OnCheckedChanged       │
│ □ CodeGenerateDialog.java - 内部 ViewHolder + 事件          │
│                                                              │
│ 🔴 复杂文件（6-10小时）：                                     │
│ □ CircularMenu.java - 15 处 @Optional 需判空处理            │
│ □ ExplorerView.java - 735 行，多内部 ViewHolder             │
│ □ CommunityWebView.java - @Optional 处理                    │
│                                                              │
│ 📦 清理工作：                                                 │
│ □ 移除 butterknife 依赖                                     │
│ □ 移除 kapt 'com.jakewharton:butterknife-compiler'         │
│ □ 全局搜索确认无遗漏                                         │
│                                                              │
│ ✅ 测试验证：                                                 │
│ □ 编译通过                                                   │
│ □ 功能回归测试                                               │
│ □ 内存泄漏检查（Dialog 生命周期）                            │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 阶段 2: 移除 AndroidAnnotations                              │
│ 预估工作量：8-12 天                                          │
├─────────────────────────────────────────────────────────────┤
│ 任务清单：                                                   │
│ □ 重构 16 个 Activity                                       │
│   - MainActivity                                            │
│   - EditActivity                                            │
│   - AboutActivity                                           │
│   - BuildActivity                                           │
│   - ProjectConfigActivity                                   │
│   - TimedTaskSettingActivity                                │
│   - LogActivity                                             │
│   - LoginActivity                                           │
│   - RegisterActivity                                        │
│   - WebActivity                                             │
│   - DocumentationActivity                                   │
│   - SettingsActivity                                        │
│   - ScriptWidgetSettingsActivity                            │
│   - ShortcutIconSelectActivity                              │
│   - TaskPrefEditActivity                                    │
│   - TaskerScriptEditActivity                                │
│                                                              │
│ □ 重构 8 个 Fragment                                        │
│   - DrawerFragment                                          │
│   - DocsFragment                                            │
│   - CommunityFragment                                       │
│   - MyScriptListFragment                                    │
│   - TaskManagerFragment                                     │
│   - DebugToolbarFragment                                    │
│   - NormalToolbarFragment                                   │
│   - SearchToolbarFragment                                   │
│                                                              │
│ □ 重构 1 个 ViewGroup                                       │
│   - EditorView (@EViewGroup)                                │
│                                                              │
│ □ 更新所有生成类引用                                         │
│   - SettingsActivity_.class → SettingsActivity.class        │
│   - 约 50+ 处 Intent 引用                                   │
│                                                              │
│ □ 处理 16 处 @UiThread                                      │
│   - JsDialog.java 全部重写                                  │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│ 阶段 3: 移除 KAPT，迁移 Glide 到 KSP                         │
│ 预估工作量：✅ 已完成                                        │
├─────────────────────────────────────────────────────────────┤
│ 任务清单：                                                   │
│ ✅ 升级 Glide                                                │
│   - implementation 'com.github.bumptech.glide:glide:4.14.2' │
│   - ksp 'com.github.bumptech.glide:ksp:4.14.2'              │
│                                                              │
│ ✅ 验证构建                                                   │
│   - BUILD SUCCESSFUL in 19s                                 │
│   - KAPT + KSP 共存验证成功                                  │
│                                                              │
│ ⏳ 待完成（需先移除 AA 和 ButterKnife）                       │
│ □ 移除 kotlin-kapt 插件                                     │
│ □ 清理 KAPT 相关配置                                         │
└─────────────────────────────────────────────────────────────┘
```

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
- AndroidAnnotations 迁移工作量大（8-12 天）
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
| 工作量 | 8-12 天 |
| 风险 | 高 |
| 收益 | 完全移除 KAPT，构建速度提升 |

**重构内容**：
1. 16 个 Activity - 移除 @EActivity，改用标准生命周期
2. 8 个 Fragment - 移除 @EFragment
3. 1 个 ViewGroup - 移除 @EViewGroup
4. 50+ 处生成类引用 - `Activity_.class` → `Activity.class`
5. 16 处 @UiThread - 改用 `runOnUiThread`/协程

### 5.4 总体工作量汇总

| 阶段 | 任务 | 工作量 | 风险 |
|------|------|--------|------|
| ✅ 已完成 | Glide → KSP | 已完成 | 无 |
| 阶段 1 | ButterKnife → ViewBinding | 2-3 天 | 中 |
| 阶段 2 | AndroidAnnotations 移除 | 8-12 天 | 高 |
| 阶段 3 | 移除 KAPT 插件 | 1 小时 | 低 |
| **总计** | **完全迁移到 KSP** | **10-15 天** | **高** |

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

---

## 九、结论

### 已完成

- **Glide KSP 迁移**：✅ 成功（使用正确的 artifact `ksp:4.14.2`）
- **KAPT + KSP 共存**：✅ 验证可行

### 待完成

| 阶段 | 任务 | 工作量 | 优先级 |
|------|------|--------|--------|
| 阶段 1 | ButterKnife → ViewBinding | 2-3 天 | 中（库已废弃） |
| 阶段 2 | AndroidAnnotations 移除 | 8-12 天 | 低（维护模式） |
| 阶段 3 | 移除 KAPT 插件 | 1 小时 | 低 |

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

### 推荐执行顺序

1. **短期**：保持现状，KAPT + KSP 共存工作正常
2. **中期**：优先迁移 ButterKnife（工作量小，库已废弃）
3. **长期**：评估 AndroidAnnotations 迁移收益后再决定

### 风险提示

- ButterKnife 迁移需注意 @Optional 处理
- AndroidAnnotations 迁移涉及架构重构，需全面测试
- Dialog 生命周期需注意内存泄漏
