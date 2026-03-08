# Auto.js.HYB1996 编辑器与调试器移植升级报告

**日期**: 2026-03-08  
**版本**: v0.85.x → v0.86.0  
**移植来源**: AutoX, AutoX.js  
**构建变体**: coolapk (debug)

---

## 一、移植概述

本次移植从 AutoX 和 AutoX.js 项目中提取编辑器和调试器的改进，应用到 Auto.js.HYB1996 项目。主要工作包括：

1. 修复代码编辑器关键 Bug
2. 改进边界检查机制
3. 添加系统夜间模式检测
4. 将编辑器模块和 SAF 模块转换为 Kotlin
5. 调整调试器默认行为
6. 修复 Android 12+ 兼容性问题

---

## 二、修复的 Bug

### 2.1 BracketMatching 括号配对错误

**文件**: `app/src/main/java/org/autojs/autojs/ui/edit/editor/BracketMatching.java` → `BracketMatching.kt`

**问题描述**:  
`PAIR_RIGHT` 数组中第三个元素错误地使用了 `)` 而不是 `]`，导致方括号 `[]` 配对失败。

**修复前**:
```java
private static final char[] PAIR_RIGHT = {')', '}', ')'};
```

**修复后**:
```kotlin
private val PAIR_RIGHT = charArrayOf(')', '}', ']')
```

**影响**: 修复后 `[]` 括号配对功能正常工作。

---

### 2.2 CodeEditText 边界检查问题

**文件**: `app/src/main/java/org/autojs/autojs/ui/edit/editor/CodeEditText.java`

**问题描述**:  
原代码在 `drawText` 时仅依赖 try-catch 处理边界越界异常，不够健壮。

**修复方案**:
- 添加显式边界检查（AutoX.js 风格）
- 保留 try-catch 作为备用方案

**修改代码**:
```java
// 显式边界检查
if (visibleCharEnd > text.length()) {
    visibleCharEnd = text.length();
}
// try-catch 作为备用
try {
    canvas.drawText(text, previousColorPos, visibleCharEnd, paddingLeft + offsetX, lineBaseline, paint);
} catch (Exception e) {
    Log.e(LOG_TAG, e.toString());
}
```

---

### 2.3 FLAG_IMMUTABLE 崩溃（Android 12+）

**文件**: `app/src/main/java/org/autojs/autojs/external/foreground/ForegroundService.java`

**问题描述**:  
Android 12+ 要求 PendingIntent 必须显式指定 `FLAG_IMMUTABLE` 或 `FLAG_MUTABLE`，否则应用崩溃。

**错误日志**:
```
E/CrashHandler: Strongly consider using FLAG_IMMUTABLE, only use FLAG_MUTABLE if some functionality depends on the PendingIntent being mutable
at android.app.PendingIntent.checkFlags(PendingIntent.java:375)
at org.autojs.autojs.external.foreground.ForegroundService.buildNotification(ForegroundService.java:57)
```

**修复前**:
```java
PendingIntent contentIntent = PendingIntent.getActivity(this, 0, MainActivity_.intent(this).get(), 0);
```

**修复后**:
```java
int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M 
        ? PendingIntent.FLAG_IMMUTABLE 
        : 0;
PendingIntent contentIntent = PendingIntent.getActivity(this, 0, MainActivity_.intent(this).get(), flags);
```

---

## 三、新增功能

### 3.1 系统夜间模式检测

**新文件**: `app/src/main/java/org/autojs/autojs/ui/util/NightMode.kt`

**功能**: 检测系统是否启用夜间模式

```kotlin
package org.autojs.autojs.ui.util

import android.content.Context
import android.content.res.Configuration

fun Context.isSystemNightMode(): Boolean {
    val configuration = this.resources.configuration
    return (configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == 
           Configuration.UI_MODE_NIGHT_YES
}
```

**使用场景**: 编辑器主题自动切换，根据系统设置自动应用暗色主题。

---

## 四、语言转换（Java → Kotlin）

### 4.1 编辑器模块（11 个文件）

| 原文件 (Java) | 新文件 (Kotlin) | 说明 |
|--------------|----------------|------|
| `BracketMatching.java` | `BracketMatching.kt` | 括号配对工具，改为 object 单例 |
| `CodeCompletion.java` | `CodeCompletion.kt` | 代码补全项 |
| `CodeCompletions.java` | `CodeCompletions.kt` | 代码补全集合，添加 @JvmOverloads |
| `Symbols.java` | `Symbol.kt` | 符号补全，改名并改为 object 单例 |
| `AutoIndent.java` | `AutoIndent.kt` | 自动缩进 |
| `TextViewUndoRedo.java` | `TextViewUndoRedo.kt` | 撤销重做，修复属性 setter 冲突 |
| `Theme.java` | `Theme.kt` | 编辑器主题 |
| `Themes.java` | `Themes.kt` | 主题管理，改为 object 单例 |
| `JavaScriptHighlighter.java` | `JavaScriptHighlighter.kt` | JS 语法高亮 |
| `EditAction.java` | `EditAction.kt` | 编辑操作 |
| `TokenMapping.java` | `TokenMapping.kt` | Token 映射 |

### 4.2 SAF 文件访问模块（5 个文件）

| 原文件 (Java) | 新文件 (Kotlin) | 说明 |
|--------------|----------------|------|
| `IFileProvider.java` | `IFileProvider.kt` | 文件提供者接口，FileInfo 改为 data class |
| `FileProviderFactory.java` | `FileProviderFactory.kt` | 文件提供者工厂，改为 object 单例 |
| `SafFileProviderImpl.java` | `SafFileProviderImpl.kt` | SAF 实现 |
| `TraditionalFileProvider.java` | `TraditionalFileProvider.kt` | 传统文件实现 |
| `StoragePermissionHelper.java` | `StoragePermissionHelper.kt` | 权限辅助类，改为 object 单例 |

### 4.3 Kotlin 转换注意事项

1. **object 单例模式**: 工具类转换为 Kotlin object，使用 `@JvmStatic` 注解保持 Java 兼容
2. **data class**: `IFileProvider.FileInfo` 转换为 data class，自动生成 equals/hashCode/toString
3. **属性访问**: Kotlin 属性对 Java 是私有的，需添加 getter 方法或使用 `@JvmField`
4. **可空性**: 添加 `!!` 或 `?` 处理可空类型
5. **平台声明冲突**: 避免手动 getter 与 Kotlin 自动生成的 getter 签名冲突

---

## 五、调试器修改

### 5.1 breakOnExceptions 默认值

**文件**: `autojs/src/main/java/org/stardust/autojs/core/debug/Debugger.java`

**修改**: 将 `breakOnExceptions` 默认值从 `true` 改为 `false`

```java
// 第 106 行
dim.setBreakOnExceptions(false);  // 原为 true
```

**原因**: 
- 默认在异常时中断会影响正常调试流程
- 用户可在需要时手动开启此选项
- 与 AutoX/AutoX.js 保持一致

---

## 六、编译错误修复记录

| 错误类型 | 文件 | 修复方法 |
|---------|------|---------|
| `sUri` 未定义 | FileProviderFactory.kt | 改为 `safUri` |
| writeBytes 类型不匹配 | SafFileProviderImpl.kt | 添加显式返回值 `true` |
| FileInfo 私有属性访问 | PFiles.java, AdbDebugReceiver.java, ExplorerFileProvider.java | 添加 getter 方法 |
| `CodeTextView` 未找到 | AutoIndent.kt | 改为 `CodeEditText` |
| Observable 类型不匹配 | Themes.kt | 使用 `toList().toObservable()` |
| Editable? 可空 | AutoIndent.kt | 添加 `!!` 断言 |
| setMaxHistorySize 平台冲突 | TextViewUndoRedo.kt | 使用属性 setter 替代方法 |
| Symbols 未找到 | EditorView.java | 改为 `Symbol` |
| getInsertText() 缺失 | CodeCompletion.kt | 添加显式方法 |
| getDebuggingLineBackgroundColor() 缺失 | Theme.kt | 使用 Kotlin 自动生成的 getter |
| SafFileProviderImpl 构造函数参数顺序 | FileProviderFactory.java | 调整参数顺序为 (treeUri, rootPath, context) |
| SafFileProviderImpl 重复文件 | app/storage/ | 删除 app 模块的重复文件，使用 common 模块 |

---

## 七、文件变更统计

### 7.1 新增文件
```
app/src/main/java/org/autojs/autojs/ui/util/NightMode.kt
```

### 7.2 删除文件
```
app/src/main/java/org/autojs/autojs/storage/SafFileProviderImpl.java  # 重复文件
```

### 7.3 转换文件（Java → Kotlin）
```
# 编辑器模块 (11)
app/src/main/java/org/autojs/autojs/ui/edit/editor/BracketMatching.kt
app/src/main/java/org/autojs/autojs/model/autocomplete/CodeCompletion.kt
app/src/main/java/org/autojs/autojs/model/autocomplete/CodeCompletions.kt
app/src/main/java/org/autojs/autojs/model/autocomplete/Symbol.kt
app/src/main/java/org/autojs/autojs/ui/edit/editor/AutoIndent.kt
app/src/main/java/org/autojs/autojs/ui/edit/editor/TextViewUndoRedo.kt
app/src/main/java/org/autojs/autojs/ui/edit/theme/Theme.kt
app/src/main/java/org/autojs/autojs/ui/edit/theme/Themes.kt
app/src/main/java/org/autojs/autojs/ui/edit/editor/JavaScriptHighlighter.kt
app/src/main/java/org/autojs/autojs/ui/edit/editor/EditAction.kt
app/src/main/java/org/autojs/autojs/ui/edit/editor/TokenMapping.kt

# SAF 模块 (5)
common/src/main/java/com/stardust/pio/IFileProvider.kt
common/src/main/java/com/stardust/pio/FileProviderFactory.kt
common/src/main/java/com/stardust/pio/SafFileProviderImpl.kt
common/src/main/java/com/stardust/pio/TraditionalFileProvider.kt
app/src/main/java/org/autojs/autojs/storage/StoragePermissionHelper.kt
```

### 7.4 修改文件
```
app/src/main/java/org/autojs/autojs/ui/edit/editor/CodeEditText.java
app/src/main/java/org/autojs/autojs/external/foreground/ForegroundService.java
app/src/main/java/org/autojs/autojs/storage/FileProviderFactory.java
app/src/main/java/org/autojs/autojs/model/explorer/ExplorerFileProvider.java
app/src/main/java/org/autojs/autojs/external/receiver/AdbDebugReceiver.java
app/src/main/java/org/autojs/autojs/ui/edit/editor/EditorView.java
common/src/main/java/com/stardust/pio/PFiles.java
autojs/src/main/java/org/stardust/autojs/core/debug/Debugger.java
```

---

## 八、测试验证

### 8.1 构建验证
```
./gradlew assembleCoolapkDebug --parallel
BUILD SUCCESSFUL in 1m 14s
117 actionable tasks: 14 executed, 103 up-to-date
```

### 8.2 设备测试
- **设备**: Android 12 (API 31), arm64-v8a
- **连接**: ADB 无线调试 `192.168.31.98:43225`
- **包名**: `org.autojs.autojs.coolapk`

### 8.3 功能测试

| 测试项 | 结果 |
|-------|------|
| 应用启动 | ✅ 通过 |
| SAF 文件访问 | ✅ 通过 |
| ADB 调试命令 | ✅ 通过 |
| 脚本执行 | ✅ 通过 |
| Toast 显示 | ✅ 通过 |
| 主题切换 | ✅ 通过 |

### 8.4 ADB 调试命令测试
```powershell
# 版本查询
F:\AIDE\sdk\platform-tools\adb.exe shell "am broadcast -n org.autojs.autojs.coolapk/org.autojs.autojs.external.receiver.AdbDebugReceiver -a org.autojs.autojs.coolapk.adb.VERSION"
# 结果: Version: (810100), Package: org.autojs.autojs.coolapk, Mode: SAF 目录授权模式

# 脚本执行
$script='toast("测试成功!");'; $b64=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($script))
F:\AIDE\sdk\platform-tools\adb.exe shell "am broadcast -n org.autojs.autojs.coolapk/org.autojs.autojs.external.receiver.AdbDebugReceiver -a org.autojs.autojs.coolapk.adb.PUSH_SCRIPT --es script $b64 --ez base64 true"
# 结果: OK: Script pushed and started, id=adb_1772968869137
```

---

## 九、兼容性说明

### 9.1 Android 版本兼容
- **最低版本**: Android 5.0 (API 21)
- **目标版本**: Android 13 (API 33)
- **测试版本**: Android 12 (API 31)

### 9.2 SAF 模式兼容
- 完全访问模式 (MANAGE_EXTERNAL_STORAGE)
- SAF 目录授权模式
- 传统模式 (Android 10 及以下)

### 9.3 Java-Kotlin 互操作
- 所有公共 API 保持 Java 兼容
- 使用 `@JvmStatic`, `@JvmOverloads`, `@JvmField` 注解
- getter 方法命名遵循 JavaBean 规范

---

## 十、后续建议

1. **单元测试**: 为 Kotlin 转换的核心模块添加单元测试
2. **性能测试**: 对比 Kotlin 版本与原 Java 版本的性能
3. **代码审查**: 检查是否还有其他 PendingIntent 相关代码需要修复
4. **文档更新**: 更新 API 文档，反映 Kotlin 转换后的变化

---

## 十一、参考文档

- [AutoX_CodeEditor_Porting_Guide.md](../../AutoX_CodeEditor_Porting_Guide.md)
- [DEBUG_GUIDE.md](./DEBUG_GUIDE.md)
- [ISOLATED_BUILD_GUIDE.md](./ISOLATED_BUILD_GUIDE.md)

---

**报告生成时间**: 2026-03-08 19:25  
**构建版本**: 810100  
**签名**: Debug (未签名验证)
