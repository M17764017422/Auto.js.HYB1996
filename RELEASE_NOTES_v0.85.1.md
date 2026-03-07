# Auto.js.HYB1996 v0.85.1 Release Notes

**发布日期**: 2026-03-08  
**版本类型**: 内测版 (Alpha)

---

## 🎉 重大更新

本次版本是一次**重大升级**，将 Rhino JavaScript 引擎从 1.7.14 升级至 2.0.0，同时升级构建工具链至 AGP 8.2.2，带来了更好的 ES6+ 特性支持和现代 Android 兼容性。

---

## 📦 版本信息

| 组件 | 原版本 | 新版本 |
|------|--------|--------|
| **Rhino** | 1.7.14-jdk7 | **2.0.0-SNAPSHOT** |
| **AGP** | 7.4.2 | **8.2.2** |
| **Gradle** | 8.2 | **8.7** |
| **Kotlin** | 1.8.22 | **1.9.25** |
| **targetSdk** | 28 (Android 9) | **34 (Android 14)** |
| **compileSdk** | 28 | **34** |
| **JDK** | 11 | **17** |

---

## ✨ 新增特性

### ES6+ JavaScript 支持

Rhino 2.0.0 带来了广泛的 ES6+ 特性支持（**实测支持率 92%**）：

| 特性 | 示例 | 状态 |
|------|------|------|
| 箭头函数 | `var add = (a, b) => a + b;` | ✅ 支持 |
| 模板字符串 | `` `Hello, ${name}!` `` | ✅ 支持 |
| let/const | `let x = 1; const PI = 3.14;` | ✅ 支持 |
| 解构赋值 | `var {name, age} = person;` | ✅ 支持 |
| Promise | `new Promise((r, j) => r(42));` | ✅ 支持 |
| Map/Set | `var map = new Map();` | ✅ 支持 |
| 生成器 | `function* gen() { yield 1; }` | ✅ 支持 |
| Object.assign/values/entries | `Object.assign({}, defaults);` | ✅ 支持 |
| Array.find/findIndex/includes | `[1,2,3].find(x => x > 1);` | ✅ 支持 |
| **展开运算符** | `var arr = [...other, 4];` | ✅ **支持** |
| **默认参数** | `function(a = 1) { }` | ✅ **支持** |
| **空值合并** | `value ?? 'default'` | ✅ **支持** |
| for...of | `for (var x of arr) { }` | ✅ 支持 |
| Symbol | `var sym = Symbol('test');` | ✅ 支持 |

### 不支持的 ES6+ 特性

| 特性 | 替代方案 |
|------|----------|
| class 关键字 | 构造函数 + 原型 |
| async/await | Promise.then() |
| 可选链 `obj?.prop` | `obj && obj.prop` |
| ES6 import/export | `load()` 或 `require()` |

---

## 🔧 Bug 修复

### Android 11+ TTS 初始化失败

**问题**: 在 Android 11+ 设备上，TTS `onInit` 回调始终返回 `status=-1` (ERROR)

**原因**: targetSdk 升级到 34 后，Android 11+ 强制要求声明包可见性才能发现系统服务

**修复**: 在 `AndroidManifest.xml` 添加包可见性声明

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
    <package android:name="com.termux" />
</queries>

<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
```

---

## ⚠️ 破坏性变更

### Java.extend API 变更

Rhino 2.0.0 移除了大写 `Java` 对象的 `extend` 方法，需要使用新的接口实现语法：

```javascript
// ❌ 旧写法 (Rhino 1.7.14) - 不再支持
var listener = Java.extend(TextToSpeech.OnInitListener, {
    onInit: function(status) { ... }
});

// ✅ 新写法 (Rhino 2.0.0) - 推荐方式
var listener = new TextToSpeech.OnInitListener({
    onInit: function(status) { ... }
});

// ✅ 兼容写法 - JavaAdapter 仍然支持
var listener = new JavaAdapter(TextToSpeech.OnInitListener, {
    onInit: function(status) { ... }
});
```

### JS-Java 类型比较

```javascript
var javaInt = java.lang.Integer.valueOf(42);

// ❌ 严格相等比较 - 返回 false
javaInt === 42  // false (Java 对象 vs JS 原始类型)

// ✅ 宽松相等比较 - 返回 true
javaInt == 42   // true

// ✅ 显式转换 - 返回 true
javaInt.intValue() === 42  // true
```

### Java 异常捕获

```javascript
try {
    list.get(100);  // IndexOutOfBoundsException
} catch (e) {
    // ❌ 错误方式 - 返回 false
    e instanceof java.lang.IndexOutOfBoundsException  // false
    
    // ✅ 正确方式 - 通过 javaException 访问
    e.javaException instanceof java.lang.IndexOutOfBoundsException  // true
}
```

---

## 📋 兼容性测试结果

### 综合测试报告 (实测)

| 类别 | 通过/总数 | 支持率 |
|------|-----------|--------|
| ES6+ 语法 | 14/15 | 93% |
| Java 互操作 | 7/8 | 88% |
| 类型系统 | 2/3 | 67% |
| 异常处理 | 3/3 | 100% |
| 性能测试 | 3/3 | 100% |
| AutoJS API | 4/4 | 100% |
| **总计** | **33/36** | **92%** |

### 导入功能

| 测试项 | 结果 |
|--------|------|
| `importClass(java.io.File)` | ✅ 通过 |
| `importPackage(java.util)` | ✅ 通过 |
| `importClass("java.io.File")` | ✅ 通过 (AutoJs6 扩展) |

### 接口实现

| 测试项 | 结果 |
|--------|------|
| `new Interface({...})` | ✅ 通过 (推荐) |
| `JavaAdapter(Interface, {...})` | ✅ 通过 (兼容) |
| `Java.extend(Interface, {...})` | ❌ 不支持 (已弃用) |

### TTS 功能

| 测试项 | 修复前 | 修复后 |
|--------|--------|--------|
| TTS 初始化 | ❌ status=-1 | ✅ status=0 |
| TTS 朗读 | ❌ 失败 | ✅ 成功 |

---

## 📝 已知问题

1. **let 闭包捕获** - 在 for 循环中使用 let 声明的变量，闭包可能无法正确捕获每次迭代的值，建议使用 IIFE 或 let 外的其他方式
2. **Java.extend** - 大写 `Java.extend` 不可用，需使用 `new Interface()` 或 `JavaAdapter`
3. **module.exports** - 需要 Auto.js 模块加载器支持，当前版本可能不兼容使用 CommonJS 模块语法的脚本
4. **D8 警告** - 部分旧版依赖库产生字节码警告，不影响运行
5. **增量注解处理** - AndroidAnnotations 不支持增量处理，构建速度略受影响

---

## 📥 下载

| 架构 | 文件 |
|------|------|
| ARM (armeabi-v7a) | `app-coolapk-armeabi-v7a-debug.apk` |
| x86 | `app-coolapk-x86-debug.apk` |

---

## 📖 迁移指南

如果你的脚本使用了以下语法，需要进行适配：

### 1. Java.extend 调用

```javascript
// 查找所有 Java.extend 调用
// 替换为 new Interface({...}) 或 JavaAdapter
```

### 2. Java 类型严格比较

```javascript
// 查找所有 Java 对象的 === 比较
// 改用 == 或 .xxxValue() 方法
```

### 3. 异常处理

```javascript
// 查找 catch 块中的 instanceof 检查
// 改用 e.javaException instanceof ...
```

---

## 🔗 相关资源

- [完整升级报告](./UPGRADE_REPORT_RHINO2_AGP8.md)
- [Rhino 兼容性报告](./RHINO_UPGRADE_REPORT.md)
- [构建指南](../ISOLATED_BUILD_GUIDE.md)

---

## 🙏 致谢

- Rhino 团队 - JavaScript 引擎开发
- AutoJs6、AutoX 项目 - 参考实现
- TonyJiangWJ - 项目维护

---

**发布者**: iFlow CLI  
**发布时间**: 2026-03-08
