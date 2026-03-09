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

| 注解类型 | 使用次数 | 说明 |
|---------|---------|------|
| @BindView | 47 | 视图绑定 |
| @OnClick | 33 | 点击事件 |
| @Optional | 15 | 可选绑定 |
| @OnCheckedChanged | 4 | 选中变化 |
| @OnTextChanged | 1 | 文本变化 |

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

### 2.2 ButterKnife（中等）

**问题 1：@Optional 可选绑定**
- ViewBinding 所有视图都必须存在于布局中
- 15 处 `@Optional` 需要运行时判空或多布局方案

**问题 2：ViewHolder 重构**
- 10 个 ViewHolder 需要修改构造函数签名
- 需要持有 ViewBinding 引用

**问题 3：Dialog 生命周期**
- 6 个 Dialog 类需要手动管理 ViewBinding
- 需要在 dismiss 时释放引用避免内存泄漏

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
│ 阶段 1: 移除 ButterKnife                                     │
│ 预估工作量：10-15 天                                         │
├─────────────────────────────────────────────────────────────┤
│ 任务清单：                                                   │
│ □ 迁移 10 个 RecyclerView.ViewHolder                        │
│   - ExplorerItemViewHolder (ExplorerView.java)              │
│   - ExplorerPageViewHolder (ExplorerView.java)              │
│   - CategoryViewHolder (ExplorerView.java)                  │
│   - DrawerMenuItemViewHolder                                │
│   - TaskViewHolder (TaskListRecyclerView.java)              │
│   - FileChooseListView ViewHolder x2                        │
│   - CodeGenerateDialog ViewHolder                           │
│   - OperationDialogBuilder ViewHolder                       │
│   - OptionListView ViewHolder                               │
│                                                              │
│ □ 迁移 6 个 Dialog/DialogBuilder 类                         │
│   - ScriptLoopDialog                                        │
│   - FindOrReplaceDialogBuilder                              │
│   - TextSizeSettingDialogBuilder                            │
│   - ManualDialog                                            │
│   - CodeGenerateDialog                                      │
│   - OperationDialogBuilder                                  │
│                                                              │
│ □ 处理 15 处 @Optional 可选绑定                              │
│   - CircularMenu.java (13 处)                               │
│   - CommunityWebView.java (2 处)                            │
│                                                              │
│ □ 迁移 4 个自定义 View                                       │
│   - AvatarView                                              │
│   - FunctionsKeyboardView                                   │
│   - ExplorerProjectToolbar                                  │
│   - OptionListView                                          │
│                                                              │
│ □ 处理 33 处 @OnClick 事件                                   │
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
- Glide KAPT 版本稳定

**理由**：
- 迁移工作量大（20-30 天）
- 风险高，需要全面回归测试
- 收益有限（主要是构建速度提升）

### 5.2 中期方案

**渐进迁移**
1. 先迁移 ButterKnife 到 ViewBinding
   - 工作量：10-15 天
   - 风险：中
   - 收益：移除废弃依赖

2. 评估 AndroidAnnotations 替代方案
   - 考虑使用 Hilt/Dagger 依赖注入
   - 考虑使用协程替代 @UiThread

### 5.3 长期方案

**完全重构**
1. 移除 AndroidAnnotations
   - 使用 ViewBinding + 标准生命周期
   - 重构所有 Activity/Fragment

2. 移除 KAPT
   - 迁移 Glide 到 KSP
   - 清理所有 KAPT 配置

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

---

## 九、结论

### 已完成

- **Glide KSP 迁移**：✅ 成功（使用正确的 artifact `ksp:4.14.2`）
- **KAPT + KSP 共存**：✅ 验证可行

### 待完成

完全迁移到 KSP 还需要移除 AndroidAnnotations 和 ButterKnife 两个不支持 KSP 的库。这是一个较大的重构工作，预估需要 20-30 天的开发时间加上完整的回归测试。

### 当前配置

```groovy
// Glide - KSP ✅
implementation 'com.github.bumptech.glide:glide:4.14.2'
ksp 'com.github.bumptech.glide:ksp:4.14.2'

// AndroidAnnotations/ButterKnife - KAPT (保留)
kapt "org.androidannotations:androidannotations:4.8.0"
kapt 'com.jakewharton:butterknife-compiler:10.2.3'
```

**建议**：短期内保持现状，中期可考虑先迁移 ButterKnife，长期规划完全重构。
