# Rhino 2.0.0 + AGP 8.2.2 升级报告

**升级日期**: 2026-03-08  
**项目**: Auto.js.HYB1996  
**分支**: temp-test-branch

---

## 一、升级概述

### 版本变更

| 组件 | 升级前 | 升级后 |
|------|--------|--------|
| Rhino | 1.7.14 | **2.0.0-SNAPSHOT** |
| AGP (Android Gradle Plugin) | 4.2.x | **8.2.2** |
| Gradle | 6.x | **8.5** |
| targetSdk | 28 (Android 9) | **34 (Android 14)** |
| compileSdk | 28 | **34** |
| JDK | 11 | **17** |

### 升级目标

1. 获取 Rhino 2.0.0 的 ES6+ 特性支持
2. 升级构建工具链以支持最新 Android 版本
3. 修复已知问题和安全漏洞

---

## 二、主要变更

### 2.1 Rhino 引擎变更

#### ES6+ 支持情况

**支持率**: 70.4% (50/71 项测试)

**支持特性**:
- 箭头函数
- 模板字符串
- let/const 块级作用域
- 解构赋值
- Promise
- Map/Set
- 生成器 (function*/yield)
- Object.assign/values/entries
- 数组方法 (find/filter/map 等)

**不支持特性**:
- 默认参数
- 扩展运算符 (...)
- class 关键字
- async/await
- 可选链 (?.)
- 空值合并 (??)
- ES6 import/export

#### API 变更

| 功能 | Rhino 1.7.14 | Rhino 2.0.0 |
|------|--------------|-------------|
| `Java.extend` | ✅ 支持 | ❌ 不支持 (使用 `java.extend`) |
| `JavaAdapter` | ✅ 支持 | ✅ 支持 (签名变更) |
| 接口实现 | `Java.extend(Interface, {...})` | `new Interface({...})` |
| `Packages.` 前缀 | ✅ 支持 | ⚠️ 需配置 |

### 2.2 构建系统变更

#### AGP 8.x 迁移

```groovy
// build.gradle 变更
android {
    namespace 'org.autojs.autojs'  // 必须声明 namespace
    
    buildFeatures {
        buildConfig true  // AGP 8.x 默认禁用，需显式启用
    }
}
```

#### AndroidManifest.xml 变更

```xml
<!-- Android 11+ 包可见性声明 (必需) -->
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

## 三、发现的问题及修复

### 3.1 TTS 初始化失败

**问题**: TTS `onInit` 返回 `status=-1` (ERROR)

**原因**: targetSdk 升级到 34 后，Android 11+ 强制要求声明包可见性

**修复**: 在 `AndroidManifest.xml` 添加:
```xml
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
</queries>
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
    tools:ignore="QueryAllPackagesPermission" />
```

**参考**: AutoX、AutoJs6 均使用相同方案

### 3.2 Java.extend 不可用

**问题**: `Java.extend` 报错 `extend 是 object 而非函数`

**原因**: Rhino 2.0.0 移除了大写 `Java` 对象的 `extend` 方法

**修复**: 使用新的接口实现语法:
```javascript
// 旧写法 (Rhino 1.7.14)
var listener = Java.extend(TextToSpeech.OnInitListener, {
    onInit: function(status) { ... }
});

// 新写法 (Rhino 2.0.0)
var listener = new TextToSpeech.OnInitListener({
    onInit: function(status) { ... }
});
```

### 3.3 JS-Java 类型比较问题

**问题**: `javaInteger === 1` 返回 `false`

**原因**: Java 对象与 JS 原始类型的严格相等比较

**修复**: 使用宽松相等或显式转换:
```javascript
// 方案1: 宽松相等
javaInt == 1  // true

// 方案2: 显式转换
javaInt.intValue() === 1  // true
```

### 3.4 Java 异常捕获变更

**问题**: `e instanceof java.lang.XxxException` 返回 `false`

**原因**: Rhino 2.0.0 将 Java 异常包装为 `JavaException`

**修复**: 通过 `e.javaException` 访问原始异常:
```javascript
try {
    // Java 代码
} catch (e) {
    // 错误方式
    e instanceof java.lang.IOException  // false
    
    // 正确方式
    e.javaException instanceof java.lang.IOException  // true
}
```

---

## 四、兼容性测试结果

### 4.1 导入功能测试

| 测试项 | 结果 | 说明 |
|--------|------|------|
| `importClass(java.io.File)` | ✅ 通过 | 传统导入 |
| `importPackage(java.util)` | ✅ 通过 | 包导入 |
| `importClass("java.io.File")` | ✅ 通过 | 字符串参数 (AutoJs6 扩展) |
| `Packages.java.io.File` | ❌ 不支持 | 需额外配置 |

### 4.2 接口实现测试

| 测试项 | 结果 | 说明 |
|--------|------|------|
| `new Interface({...})` | ✅ 通过 | 推荐方式 |
| `JavaAdapter(Interface, {...})` | ✅ 通过 | 兼容方式 |
| `Java.extend(Interface, {...})` | ❌ 不支持 | 已弃用 |

### 4.3 TTS 功能测试

| 测试项 | 修复前 | 修复后 |
|--------|--------|--------|
| TTS 初始化 | ❌ status=-1 | ✅ status=0 |
| TTS 朗读 | ❌ 失败 | ✅ 成功 |
| 默认引擎发现 | ❌ 无法发现 | ✅ org.nobody.sgtts |

---

## 五、迁移指南

### 5.1 脚本迁移检查清单

- [ ] 检查 `Java.extend` 调用，改为 `new Interface({...})`
- [ ] 检查 Java 类型严格相等比较，改用 `==` 或 `.xxxValue()`
- [ ] 检查异常处理中的 `instanceof`，改用 `e.javaException`
- [ ] 检查 `module.exports` 是否被支持 (需 Auto.js 模块加载器)

### 5.2 新特性可用

```javascript
// 箭头函数
var add = (a, b) => a + b;

// 模板字符串
var msg = `Hello, ${name}!`;

// let/const
let x = 1;
const PI = 3.14;

// 解构赋值
var {name, age} = person;
var [first, second] = array;

// Promise (如果环境支持)
new Promise((resolve, reject) => {
    resolve(42);
});

// Map/Set
var map = new Map();
var set = new Set();
```

### 5.3 不支持特性替代方案

```javascript
// ❌ 默认参数
function foo(a = 1) { }

// ✅ 替代方案
function foo(a) {
    a = a !== undefined ? a : 1;
}

// ❌ 扩展运算符
var arr = [...other]

// ✅ 替代方案
var arr = other.slice();

// ❌ async/await
async function foo() { await bar(); }

// ✅ 替代方案 (Promise)
function foo() {
    return new Promise(function(resolve) {
        bar(function() { resolve(); });
    });
}
```

---

## 六、构建配置参考

### 6.1 环境变量

```powershell
$env:JAVA_HOME = "F:\AIDE\jbr"
$env:GRADLE_USER_HOME = "F:\AIDE\.gradle"
$env:TEMP = "F:\AIDE\tmp"
$env:TMP = "F:\AIDE\tmp"
```

### 6.2 构建命令

```powershell
cd K:\msys64\home\ms900\Auto.js.HYB1996
.\gradlew.bat assembleCoolapkDebug --parallel
```

### 6.3 输出位置

```
app\build\outputs\apk\coolapk\debug\app-coolapk-armeabi-v7a-debug.apk
```

---

## 七、参考资源

- Rhino 2.0.0 源码: `K:\msys64\home\ms900\Rhino-For-AutoJs`
- 构建指南: `K:\msys64\home\ms900\ISOLATED_BUILD_GUIDE.md`
- 项目 AGENTS.md: `K:\msys64\home\ms900\Auto.js\AGENTS.md`

---

## 八、后续工作

1. [ ] 完善模块加载器支持 (`require`/`module.exports`)
2. [ ] 测试更多 AutoJs6 扩展 API 兼容性
3. [ ] 性能基准测试对比
4. [ ] 文档更新

---

**报告编写**: iFlow CLI  
**最后更新**: 2026-03-08
