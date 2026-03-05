# SAF 文件操作调试指南

## 概述

本文档记录了 SAF (Storage Access Framework) 文件操作相关的修复和调试方法。

## 日志标签

所有文件操作相关类使用统一的日志标签：

| 类名 | TAG | 说明 |
|------|-----|------|
| `SafFileProviderImpl` | `SafFileProvider` | SAF 模式文件操作 |
| `TraditionalFileProvider` | `TraditionalFileProvider` | 传统 File API 模式 |
| `FileProviderFactory` | `FileProviderFactory` | Provider 工厂类 |
| `PFiles` | `PFiles` | 文件操作工具类 |

## 日志监控命令

### 监控所有文件操作日志
```bash
adb logcat -s SafFileProvider:* TraditionalFileProvider:* FileProviderFactory:* PFiles:*
```

### 仅监控 SAF 操作
```bash
adb logcat -s SafFileProvider:*
```

### 清空日志后监控
```bash
adb logcat -c && adb logcat -s SafFileProvider:* TraditionalFileProvider:* FileProviderFactory:* PFiles:*
```

### 详细日志（包含 VERBOSE 级别）
```bash
adb logcat -v time SafFileProvider:V TraditionalFileProvider:V FileProviderFactory:V PFiles:V *:S
```

## 权限模式

FileProviderFactory 支持以下权限模式：

| 模式 | 常量 | 说明 |
|------|------|------|
| 完全访问 | `MODE_FULL_ACCESS` | Android 11+ 拥有 MANAGE_EXTERNAL_STORAGE 权限 |
| SAF 目录授权 | `MODE_SAF_DIRECTORY` | 通过 SAF 授权特定目录访问 |
| 传统模式 | `MODE_LEGACY` | Android 10 及以下传统存储权限 |
| 未知 | `MODE_UNKNOWN` | 无有效权限 |

## 已修复的问题

### 1. 占位符实现修复

| 方法 | 原问题 | 修复方案 |
|------|--------|----------|
| `SafFileProviderImpl.rename()` | 返回 `false` (占位符) | 使用 `DocumentsContract.renameDocument()` |
| `SafFileProviderImpl.move()` | 返回 `false` (占位符) | 复制 + 删除实现 |

### 2. SAF 兼容性修复

| 文件 | 方法 | 修复 |
|------|------|------|
| `ScriptOperations.java` | `deleteWithoutConfirm()` | 使用 `PFiles.removeDir()` |
| `ScriptOperations.java` | `newDirectory()` | 使用 `PFiles.mkdirs()` |
| `ExplorerFileItem.java` | `rename()` | 使用 `PFiles.rename()` |
| `ExplorerDirPage.java` | `rename()` | 使用 `PFiles.rename()` |
| `ExplorerProjectPage.java` | `rename()` | 使用 `PFiles.rename()` |
| `FileObservable.java` | `delete()` | 使用 `PFiles.remove()` |
| `ProjectTemplate.java` | 目录创建 | 使用 `PFiles.mkdirs()` |
| `PFiles.java` | `mkdirs(String)` | 添加 SAF 支持 |
| `PFiles.java` | `rename()` | 添加 SAF 支持 |
| `PFiles.java` | `move()` | 添加 SAF 支持 |
| `PFiles.java` | `remove()` | 添加 SAF 支持 |
| `PFiles.java` | `removeDir()` | 添加 SAF 支持 |

## 已添加日志的方法

### TraditionalFileProvider
- `exists()`, `isFile()`, `isDirectory()`
- `mkdir()`, `mkdirs()`, `delete()`, `deleteRecursively()`
- `rename()`, `move()`, `copy()`
- `listFiles()`, `read()`, `readBytes()`, `write()`, `writeBytes()`
- `openInputStream()`, `openOutputStream()`
- `isAccessible()`, `setWorkingDirectory()`

### SafFileProviderImpl
- 所有公共方法均已添加日志
- 辅助方法 `findDocumentId()` 有详细日志

### PFiles
- `mkdirs()`, `remove()`, `removeDir()`
- `rename()`, `move()`

### FileProviderFactory
- `setConfig()`, `getCurrentMode()`, `getProvider()`
- `refresh()`, `isAppPrivatePath()`

## 常见问题排查

### 1. 文件操作失败

检查日志中的错误信息：
```bash
adb logcat -s SafFileProvider:E TraditionalFileProvider:E PFiles:E
```

常见错误：
- `documentUri is null` - 文件不存在或路径错误
- `Context is null` - GlobalAppContext 未初始化
- `part not found` - SAF 目录中找不到指定文件

### 2. 权限模式检测

检查当前权限模式：
```bash
adb logcat -s FileProviderFactory:I | grep -E "(Mode:|getCurrentMode)"
```

### 3. Provider 选择问题

检查 Provider 选择日志：
```bash
adb logcat -s FileProviderFactory:D | grep "getProvider"
```

### 4. SAF 授权丢失

如果 SAF 授权丢失，需要重新授权：
1. 打开应用设置
2. 清除应用数据或重新安装
3. 重新授予目录访问权限

## 代码架构

```
文件操作调用链：
UI 操作 (ExplorerView 等)
    ↓
业务逻辑 (ScriptOperations, ExplorerFileItem 等)
    ↓
工具类
    ↓
Provider 工厂
    ↓
具体实现 (SafFileProviderImpl 或 TraditionalFileProvider)
    ↓
系统 API (DocumentsContract 或 File)
```

## 测试用例

### SAF 测试目录

建议在 `/sdcard/脚本/SAF_TEST_FULL/` 目录下进行测试：

```
SAF_TEST_FULL/
├── test.txt          # 测试文件
├── test_copy.txt     # 复制测试
├── to_move.txt       # 移动测试
├── renamed.txt       # 重命名测试
└── subdir1/          # 子目录测试
    └── subdir2/
```

### 测试脚本

```javascript
// 测试 SAF 文件操作
var testDir = "/sdcard/脚本/SAF_TEST_FULL";

// 测试写入
files.write(testDir + "/test.txt", "Hello SAF");
console.log("写入测试: " + files.exists(testDir + "/test.txt"));

// 测试读取
var content = files.read(testDir + "/test.txt");
console.log("读取测试: " + content);

// 测试复制
files.copy(testDir + "/test.txt", testDir + "/test_copy.txt");
console.log("复制测试: " + files.exists(testDir + "/test_copy.txt"));

// 测试重命名
files.rename(testDir + "/test_copy.txt", "renamed.txt");
console.log("重命名测试: " + files.exists(testDir + "/renamed.txt"));

// 测试删除
files.remove(testDir + "/renamed.txt");
console.log("删除测试: " + !files.exists(testDir + "/renamed.txt"));
```

## 相关文件路径

```
common/src/main/java/com/stardust/pio/
├── IFileProvider.java          # 文件访问接口
├── SafFileProviderImpl.java    # SAF 实现
├── TraditionalFileProvider.java # 传统实现
├── FileProviderFactory.java    # Provider 工厂
├── FileProviderConfig.java     # 配置接口
└── PFiles.java                 # 文件操作工具类

app/src/main/java/org/autojs/autojs/
├── storage/
│   └── SafFileProviderImpl.java  # App 模块 SAF 实现
├── ui/common/
│   └── ScriptOperations.java     # 脚本操作
└── model/explorer/
    ├── ExplorerFileItem.java     # 文件项
    ├── ExplorerDirPage.java      # 目录页
    └── ExplorerProjectPage.java  # 项目页
```
