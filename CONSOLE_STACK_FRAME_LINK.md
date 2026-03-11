# 日志面板堆栈帧链接功能

## 功能概述

在日志面板（LogBottomSheet）中，错误日志的堆栈帧（如 `script.js:15`）显示为蓝色可点击链接，点击后可跳转到编辑器对应文件的指定行。

**关键特性**：
- 仅在日志面板（底部弹出）启用，不影响主日志界面（LogActivity）和悬浮窗
- 支持中文路径和 Unicode 字符
- 支持多种格式：`file.js:line` 和 `file.js:line:column`

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                     ConsoleView (核心组件)                    │
│  文件: autojs/.../console/ConsoleView.java                  │
│  职责: 日志显示、可点击链接开关、堆栈帧匹配                     │
└─────────────────────────────────────────────────────────────┘
      │
      │ setEnableStackFrameLinks(true/false)
      ▼
┌──────────────┬──────────────┬──────────────┐
│LogBottomSheet│  LogActivity │ConsoleFloaty │
│  启用链接 ✓  │  禁用链接 ✗  │  禁用链接 ✗  │
│  (底部面板)  │  (主日志界面) │  (悬浮窗)    │
└──────────────┴──────────────┴──────────────┘
```

## 核心代码

### 1. ConsoleView.java - 开关控制

**文件路径**: `autojs/src/main/java/com/stardust/autojs/core/console/ConsoleView.java`

```java
// 堆栈帧匹配正则表达式
private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
        "([^\\s:]+\\.js):(\\d+)(?::(\\d+))?"
);

// 链接颜色
private static final int LINK_COLOR = 0xFF2196F3;

// 开关字段（默认关闭）
private boolean mEnableStackFrameLinks = false;

// 设置方法
public void setEnableStackFrameLinks(boolean enabled) {
    mEnableStackFrameLinks = enabled;
}

// 创建可点击内容
private CharSequence createClickableContent(CharSequence content, int baseColor) {
    // 开关关闭时直接返回原内容
    if (!mEnableStackFrameLinks) {
        return content;
    }
    // ... 匹配和创建链接逻辑
}
```

### 2. LogBottomSheet.java - 启用链接

**文件路径**: `app/src/main/java/org/autojs/autojs/ui/log/LogBottomSheet.java`

```java
private void setupViews() {
    AutoJs autoJs = AutoJs.getInstance();
    if (autoJs != null) {
        binding.console.setConsole(autoJs.getGlobalConsole());
        binding.console.findViewById(R.id.input_container).setVisibility(View.GONE);
        
        // 启用堆栈帧链接（仅底部面板启用）
        binding.console.setEnableStackFrameLinks(true);
        
        // 设置点击监听器
        binding.console.setOnStackFrameClickListener((fileName, lineNumber, columnNumber) -> {
            if (mStackFrameClickListener != null) {
                mStackFrameClickListener.onStackFrameClick(fileName, lineNumber, columnNumber);
                dismiss();
            }
        });
    }
}
```

### 3. LogSheet.kt - Compose 版本

**文件路径**: `app/src/main/java/org/autojs/autojs/ui/material3/components/LogSheet.kt`

```kotlin
AndroidView(
    factory = { ctx ->
        ConsoleView(ctx).apply {
            findViewById<View>(R.id.input_container).visibility = View.GONE
            setConsole(consoleImpl)
            setEnableStackFrameLinks(true)  // 启用链接
        }
    },
    modifier = Modifier.fillMaxSize()
)
```

## 配置参数

### 正则表达式

| 参数 | 当前值 | 说明 |
|------|--------|------|
| 模式 | `([^\s:]+\.js):(\d+)(?::(\d+))?` | 匹配 `file.js:line` 或 `file.js:line:col` |
| 分组1 | 文件名 | 支持 Unicode（中文路径） |
| 分组2 | 行号 | 必需 |
| 分组3 | 列号 | 可选 |

### 颜色配置

| 参数 | 值 | 说明 |
|------|-----|------|
| `LINK_COLOR` | `0xFF2196F3` | Material Blue |

### 日志级别颜色

```java
static final SparseArray<Integer> COLORS = new SparseArrayEntries<Integer>()
    .entry(Log.VERBOSE, 0xdfc0c0c0)  // 灰色
    .entry(Log.DEBUG,   0xdfffffff)  // 白色
    .entry(Log.INFO,    0xff64dd17)  // 绿色
    .entry(Log.WARN,    0xff2962ff)  // 蓝色
    .entry(Log.ERROR,   0xffd50000)  // 红色
    .entry(Log.ASSERT,  0xffff534e)  // 橙红色
    .sparseArray();
```

## 扩展指南

### 修改链接颜色

编辑 `ConsoleView.java`:

```java
// 修改为其他颜色
private static final int LINK_COLOR = 0xFFFF5722;  // 深橙色
```

### 修改匹配模式

编辑 `ConsoleView.java`:

```java
// 更严格的匹配：要求前面有空格或行首
private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
        "(?:^|\\s)([^\\s:]+\\.js):(\\d+)(?::(\\d+))?"
);

// 或只匹配错误日志中的堆栈帧（在 createClickableContent 中检查）
```

### 在其他界面启用链接

```java
// 在任意使用 ConsoleView 的地方
consoleView.setEnableStackFrameLinks(true);
consoleView.setOnStackFrameClickListener((fileName, line, col) -> {
    // 处理点击事件
});
```

### 添加更多文件类型支持

```java
// 匹配 .js, .ts, .java 文件
private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
        "([^\\s:]+\\.(js|ts|java)):(\\d+)(?::(\\d+))?"
);
```

## 布局文件说明

### LogBottomSheet 布局 (Java)

**文件路径**: `app/src/main/res/layout/bottom_sheet_log.xml`

```
┌────────────────────────────────────────────┐
│                  [拖拽手柄]                  │  4dp × 40dp, 居中
├────────────────────────────────────────────┤
│  [脚本名称/标题]           [日志] [清除]     │  Header 48dp
├────────────────────────────────────────────┤
│                                            │
│              ConsoleView                   │  400dp 高度
│           (日志显示区域)                    │
│                                            │
│                                            │
└────────────────────────────────────────────┘
```

**布局结构**:

| 组件 | ID | 尺寸 | 说明 |
|------|-----|------|------|
| 根布局 | - | match_parent | LinearLayout, 垂直方向 |
| 拖拽手柄 | - | 40dp × 4dp | 圆角灰色条, 可拖拽 |
| 标题文本 | `tv_title` | 0dp (weight=1) | 脚本名称, 粗体 16sp |
| 打开日志按钮 | `btn_open_full` | 40dp × 40dp | 跳转到全屏日志界面 |
| 清除按钮 | `btn_clear` | 40dp × 40dp | 清空日志 |
| ConsoleView | `console` | match_parent × 400dp | 日志显示组件 |

**自定义属性**:

```xml
<com.stardust.autojs.core.console.ConsoleView
    android:id="@+id/console"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:color_debug="@color/console_debug"
    app:color_verbose="@color/console_verbose"/>
```

| 属性 | 说明 |
|------|------|
| `app:color_debug` | Debug 级别日志颜色 |
| `app:color_verbose` | Verbose 级别日志颜色 |
| `app:color_info` | Info 级别日志颜色 |
| `app:color_warn` | Warn 级别日志颜色 |
| `app:color_error` | Error 级别日志颜色 |
| `app:color_assert` | Assert 级别日志颜色 |

**相关 Drawable**:

| 文件 | 用途 |
|------|------|
| `bg_bottom_sheet_rounded.xml` | 底部面板圆角背景 |
| `bg_drag_handle.xml` | 拖拽手柄背景 |
| `ic_logcat.xml` | 日志图标 |
| `ic_clear.xml` | 清除图标 |

## 相关文件

| 文件 | 用途 |
|------|------|
| `autojs/.../console/ConsoleView.java` | 核心日志显示组件 |
| `autojs/.../console/ConsoleImpl.java` | 日志后端实现 |
| `app/.../ui/log/LogBottomSheet.java` | 底部日志面板 (Java) |
| `app/.../res/layout/bottom_sheet_log.xml` | 底部日志面板布局 |
| `app/.../ui/log/LogActivity.java` | 主日志界面 |
| `app/.../ui/material3/components/LogSheet.kt` | 底部日志面板 (Compose) |

## 版本历史

- **v0.85.2** - 添加堆栈帧链接功能，支持中文路径
- **v0.85.2** - 添加开关控制，仅在日志面板启用
