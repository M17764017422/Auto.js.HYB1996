# Auto.js.HYB1996 项目技术分析报告

## 1. 项目概述

### 1.1 项目简介

Auto.js.HYB1996 是一个基于原作者 hyb1996 开发的 Auto.js 开源版本的修改版。该项目是一个支持无障碍服务的 Android 平台 JavaScript IDE，其开发目标是类似于 JsBox 和 Workflow。项目基于 Mozilla Rhino JavaScript 引擎，为 Android 平台提供完整的 JavaScript 运行环境，支持无障碍服务自动化、图像处理、UI 操作等多种功能。

### 1.2 版本信息

| 项目 | 值 |
|------|-----|
| 版本名称 | 0.80.1 |
| 版本代码 | 801000 |
| 最低 SDK | 19 (Android 4.4) |
| 目标 SDK | 34 (Android 14) |
| 编译 SDK | 34 |
| Build Tools | 36.1.0 |

### 1.3 项目来源

- **原作者**: hyb1996
- **修改维护**: Fork 社区
- **许可证**: Mozilla Public License Version 2.0 (非商业使用)

---

## 2. 核心功能与技术特性

### 2.1 核心功能

| 功能模块 | 说明 |
|----------|------|
| 无障碍服务自动化 | 通过 Android 无障碍服务实现屏幕自动化操作 |
| UI 控件操作 | 提供强大的选择器 API，用于查找、遍历、操作屏幕控件 |
| JavaScript IDE | 支持代码补全、变量重命名、代码格式化、搜索替换 |
| APK 打包 | 支持将 JavaScript 脚本打包为独立 APK 文件 |
| Root 权限支持 | 提供更强大的屏幕点击、滑动、录制功能和 Shell 命令执行 |
| 图像处理 | 提供截屏、保存截图、图片找色、找图等功能 |
| Tasker 集成 | 可作为 Tasker 插件使用，结合 Tasker 完成工作流自动化 |
| 界面分析工具 | 类似 Android Studio 的 LayoutInspector，分析界面层次和控件信息 |
| 悬浮窗录制和运行 | 支持操作录制和回放功能 |

### 2.2 技术特性

| 特性 | 说明 |
|------|------|
| 多包名支持 | 支持 common/coolapk/github 三种 flavor，可同时安装 |
| 内存优化 | 修复大量内存泄漏问题 |
| 多架构支持 | 支持 armeabi-v7a 和 x86 架构 |
| Android 12+ 兼容 | 支持 FLAG_IMMUTABLE、SCHEDULE_EXACT_ALARM 等 |
| Android 11+ 存储 | 支持 MANAGE_EXTERNAL_STORAGE 和 SAF |

---

## 3. 技术栈与架构设计

### 3.1 技术栈

#### 3.1.1 构建系统

| 组件 | 版本 |
|------|------|
| Gradle | 7.5 |
| AGP (Android Gradle Plugin) | 7.4.2 |
| JDK | 17 |
| Kotlin | 1.8.22 |

#### 3.1.2 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Rhino JavaScript Engine | 1.7.14 | JavaScript 运行时 |
| AndroidX AppCompat | 1.5.1 | 兼容性库 |
| Material Design | 1.7.0 | UI 组件 |
| ButterKnife | 10.2.3 | 视图绑定 |
| RxJava | 2.x | 响应式编程 |
| OpenCV | (内置) | 图像处理 |

#### 3.1.3 注解处理器

| 处理器 | 版本 | 用途 |
|--------|------|------|
| AndroidAnnotations | 4.8.0 | 代码生成 |
| ButterKnife | 10.2.3 | 视图绑定 |

### 3.2 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                         App 模块                                 │
│  (主应用界面、设置、编辑器、项目管理)                              │
├─────────────────────────────────────────────────────────────────┤
│                       AutoJS 模块                                │
│  (JavaScript 运行时、API 实现、无障碍桥接)                        │
├─────────────────────────────────────────────────────────────────┤
│                      Automator 模块                              │
│  (自动化操作、UI 选择器、全局操作)                                │
├─────────────────────────────────────────────────────────────────┤
│                       Common 模块                                │
│  (公共工具、文件操作、权限管理)                                   │
├─────────────────────────────────────────────────────────────────┤
│                        Inrt 模块                                 │
│  (打包运行时、独立脚本执行)                                       │
├─────────────────────────────────────────────────────────────────┤
│                      ApkBuilder 模块                             │
│  (APK 打包工具、Manifest 编辑)                                   │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 模块依赖关系

```
app
 ├── autojs
 │    ├── common
 │    └── automator
 ├── inrt
 │    └── autojs
 └── apkbuilder
```

---

## 4. 代码结构与模块实现

### 4.1 App 模块目录结构

```
app/src/main/java/org/autojs/autojs/
├── accessibility/        # 无障碍服务相关
├── autojs/              # Auto.js 核心逻辑
│   ├── api/             # API 接口
│   ├── build/           # 构建相关
│   ├── key/             # 全局按键监听
│   └── record/          # 操作录制
├── external/            # 外部接口
│   ├── fileprovider/    # 文件提供者
│   ├── receiver/        # 广播接收器
│   ├── tasker/          # Tasker 集成
│   └── widget/          # 小组件
├── model/               # 数据模型
├── network/             # 网络功能
├── pluginclient/        # 插件客户端
├── storage/             # 存储功能
├── theme/               # 主题相关
├── timing/              # 定时任务
├── tool/                # 工具类
└── ui/                  # 用户界面
    ├── doc/             # 文档界面
    ├── edit/            # 编辑器界面
    ├── floating/        # 浮动窗口
    ├── main/            # 主界面
    └── settings/        # 设置界面
```

### 4.2 AutoJS 模块目录结构

```
autojs/src/main/java/com/stardust/autojs/
├── annotation/          # 注解定义
├── core/                # 核心功能
│   ├── accessibility/   # 无障碍服务核心
│   ├── console/         # 控制台实现
│   ├── graphics/        # 图形处理
│   ├── image/           # 图像处理
│   ├── permission/      # 权限管理
│   └── ui/              # UI 相关
├── engine/              # 脚本引擎管理
├── execution/           # 脚本执行管理
├── project/             # 项目管理
├── rhino/               # Rhino 封装
├── runtime/             # 运行时环境
│   └── api/             # API 实现
├── script/              # 脚本相关
└── util/                # 工具类
```

### 4.3 JavaScript 模块目录结构

```
autojs/src/main/assets/
├── init.js              # JavaScript 环境初始化
└── modules/
    ├── __app__.js       # app 模块
    ├── __automator__.js # automator 模块
    ├── __console__.js   # console 模块
    ├── __dialogs__.js   # dialogs 模块
    ├── __engines__.js   # engines 模块
    ├── __events__.js    # events 模块
    ├── __files__.js     # files 模块 (io)
    ├── __floaty__.js    # floaty 模块
    ├── __globals__.js   # 全局函数
    ├── __http__.js      # http 模块
    ├── __images__.js    # images 模块
    ├── __selector__.js  # selector 模块
    ├── __sensors__.js   # sensors 模块
    ├── __shell__.js     # shell 模块
    ├── __storages__.js  # storages 模块
    ├── __threads__.js   # threads 模块
    ├── __timers__.js    # timers 模块
    ├── __ui__.js        # ui 模块
    ├── __util__.js      # util 模块
    ├── __web__.js       # web 模块
    ├── __media__.js     # media 模块
    ├── __plugins__.js   # plugins 模块
    ├── promise.js       # Promise polyfill
    ├── lodash.js        # Lodash 工具库
    └── jvm-npm.js       # 模块加载系统
```

---

## 5. 脚本接口架构

### 5.1 双层架构设计

Auto.js.HYB1996 的脚本接口采用双层架构：

```
┌─────────────────────────────────────────────────────────────────┐
│                    JavaScript 运行环境                           │
├─────────────────────────────────────────────────────────────────┤
│  init.js                                                        │
│  ├── runtime.init()                                             │
│  ├── 全局函数 (toast, sleep, exit, etc.)                        │
│  ├── 核心模块 (app, console, dialogs, etc.)                     │
│  └── require() 模块加载系统                                      │
├─────────────────────────────────────────────────────────────────┤
│                    Java 层 API 实现                              │
├─────────────────────────────────────────────────────────────────┤
│  ScriptRuntime.java                                             │
│  ├── @ScriptVariable 暴露的变量                                  │
│  ├── 模块实现类 (AppUtils, Console, Files, etc.)                │
│  └── AccessibilityBridge 无障碍桥接                              │
├─────────────────────────────────────────────────────────────────┤
│                    Android 系统服务                              │
├─────────────────────────────────────────────────────────────────┤
│  AccessibilityService │ MediaProjection │ SensorManager         │
│  ClipboardManager │ Vibrator │ WindowManager                   │
└─────────────────────────────────────────────────────────────────┘
```

### 5.2 Java 层核心变量

通过 `@ScriptVariable` 注解暴露给 JavaScript 的变量：

| 变量名 | 类型 | 说明 |
|--------|------|------|
| app | AppUtils | 应用操作相关 |
| console | Console | 控制台输出 |
| automator | SimpleActionAutomator | 自动化操作 |
| info | ActivityInfoProvider | 活动信息提供者 |
| ui | UI | UI 构建器 |
| dialogs | Dialogs | 对话框 |
| events | Events | 事件系统 |
| bridges | ScriptBridges | Java-JS 桥接 |
| loopers | Loopers | 消息循环 |
| timers | Timers | 定时器 |
| device | Device | 设备信息 |
| engines | Engines | 引擎管理 |
| threads | Threads | 线程管理 |
| floaty | Floaty | 悬浮窗 |
| colors | Colors | 颜色处理 |
| files | Files | 文件操作 |
| sensors | Sensors | 传感器 |
| media | Media | 多媒体 |
| plugins | Plugins | 插件系统 |
| images | Images | 图像处理 |

### 5.3 JavaScript 初始化流程

```javascript
// 1. 初始化运行时
runtime.init();

// 2. 重定向 importClass 支持字符串参数
global.importClass = function(pack) { ... };

// 3. 初始化基础模块
global.timers = require('__timers__.js')(runtime, global);
global.JSON = require('__json2__.js');
global.util = require('__util__.js');
global.device = runtime.device;
global.Promise = require('promise.js');

// 4. 设置 Java-JS 桥接
runtime.bridges.setBridges(require('__bridges__.js'));

// 5. 初始化全局函数
require("__globals__")(runtime, global);

// 6. 初始化一般模块
var modules = ['app', 'automator', 'console', 'dialogs', 'io', 'selector', 
               'shell', 'web', 'ui', "images", "threads", "events", "engines", 
               "RootAutomator", "http", "storages", "floaty", "sensors", 
               "media", "plugins", "continuation"];

// 7. 导入 Android 类
importClass(android.view.KeyEvent);
importClass(com.stardust.autojs.core.util.Shell);
Canvas = com.stardust.autojs.core.graphics.ScriptCanvas;
Image = com.stardust.autojs.core.image.ImageWrapper;

// 8. 重定向 require 支持相对路径
Module = require("jvm-npm.js");
require = Module.require;
```

### 5.4 全局函数

| 函数 | 说明 |
|------|------|
| toast(text) | 显示 Toast 提示 |
| toastLog(text) | 显示 Toast 并打印日志 |
| sleep(ms) | 阻塞延时 |
| exit() | 退出脚本 |
| setClip(text) | 设置剪贴板 |
| getClip() | 获取剪贴板 |
| currentPackage() | 获取当前包名 |
| currentActivity() | 获取当前 Activity |
| waitForActivity(activity) | 等待指定 Activity |
| waitForPackage(packageName) | 等待指定包名 |
| random(min, max) | 生成随机数 |
| setScreenMetrics(width, height) | 设置屏幕参数 |

---

## 6. JavaScript 兼容性

### 6.1 Rhino 1.7.14 支持

| 特性类别 | 支持情况 |
|----------|----------|
| ES5 全部语法 | ✅ 完全支持 |
| let/const | ✅ 支持 |
| 箭头函数 | ✅ 支持 |
| 模板字符串 | ✅ 支持 |
| 解构赋值 | ✅ 支持 |
| Promise | ✅ 支持 (polyfill) |
| Map/Set | ✅ 支持 |
| Object.values/entries | ✅ 支持 |
| 函数默认参数 | ❌ 不支持 |
| 剩余参数 (...) | ❌ 不支持 |
| 扩展运算符 (...) | ❌ 不支持 |
| class 类 | ❌ 不支持 |
| async/await | ❌ 不支持 |
| ES6 import/export | ❌ 不支持 (使用 CommonJS) |

### 6.2 模块系统

使用 CommonJS 规范：

```javascript
// 导出模块
module.exports = {
    add: function(a, b) { return a + b; }
};

// 导入模块
var myModule = require('./myModule');
```

---

## 7. 与其他 Auto.js 变体对比

### 7.1 模块对比

| 模块 | HYB1996 | Auto.js (TonyJiangWJ) | AutoJs6 | AutoX |
|------|---------|----------------------|---------|-------|
| 核心模块 (app, console 等) | ✅ | ✅ | ✅ | ✅ |
| $ocr (PaddleOCR) | ❌ | ✅ | ✅ | ❌ |
| $yolo (YOLO) | ❌ | ✅ | ✅ | ❌ |
| $tts (语音合成) | ❌ | ✅ | ✅ | ❌ |
| $shizuku | ❌ | ✅ | ✅ | ❌ |
| $mlKitOcr | ❌ | ✅ | ✅ | ❌ |

### 7.2 技术栈对比

| 项目 | AGP | Gradle | JDK | Kotlin | Rhino |
|------|-----|--------|-----|--------|-------|
| HYB1996 | 7.4.2 | 7.5 | 17 | 1.8.22 | 1.7.14 |
| Auto.js (TonyJiangWJ) | 7.x | 7.x | 17 | 1.9.0 | 1.7.14+1.9.1 |
| AutoJs6 | 8.x | 8.x | 17 | 2.0.0 | 1.7.14 |
| AutoX | 7.x | 7.x | 17 | 1.9.0 | 1.7.14 |

### 7.3 特色功能对比

| 功能 | HYB1996 | Auto.js (TonyJiangWJ) | AutoJs6 | AutoX |
|------|---------|----------------------|---------|-------|
| 多包名 Flavor | ✅ | ✅ (多分支) | ❌ | ❌ |
| SAF 存储支持 | ✅ | ✅ | ✅ | ❌ |
| WebDAV 同步 | ✅ | ❌ | ❌ | ❌ |
| CI/CD 自动构建 | ✅ | ✅ | ✅ | ❌ |
| AI 推理 | ❌ | ✅ | ✅ | ❌ |

---

## 8. 构建与发布

### 8.1 构建配置

#### Product Flavors

| Flavor | 包名 | 说明 |
|--------|------|------|
| common | org.autojs.autojs | 标准版本 |
| coolapk | org.autojs.autojs.coolapk | 酷安渠道 |
| github | org.autojs.autojs.github | GitHub 版本 |

#### 签名配置

```groovy
signingConfigs {
    debug {
        storeFile file('debug/autojs-debug.jks')
        storePassword 'autojs123'
        keyAlias 'autojs-debug'
    }
    release {
        storeFile releaseKeystoreFile ?: debug keystore
        // 从环境变量读取签名配置
    }
}
```

### 8.2 CI/CD 流程

```
GitHub Actions Workflow:
├── 触发条件: tag 推送 (v*)
├── 环境准备: JDK 17 + Android SDK
├── 版本同步: 从 tag 读取版本信息
├── 签名配置: 从 Secrets 读取 keystore
├── 构建: assembleCoolapkRelease
├── 发布: GitHub Release
└── 同步: WebDAV 上传到坚果云
```

### 8.3 版本号规则

- **Git Tag**: `v0.80.1`
- **versionName**: `0.80.1` (去掉 v 前缀)
- **versionCode**: `801000` (计算公式: major*1000000 + minor*10000 + patch*1000)

---

## 9. 版本演进记录

| 版本 | 日期 | 主要更新 |
|------|------|----------|
| v0.80.1 | 2026-03-03 | 首个正式发布版本 |
| v4.1.1-alpha13 | 2026-03-03 | 编辑器功能增强 |
| v4.1.1-alpha2 | 2026-03-03 | AGP 7.4.2 + 多包名 flavor |

### 9.1 主要改进历程

1. **构建系统升级**: AGP 4.2.2 → 7.4.2，支持 JDK 17
2. **多包名支持**: 实现三个 flavor 可同时安装
3. **存储权限适配**: 支持 Android 11+ 的 MANAGE_EXTERNAL_STORAGE 和 SAF
4. **编辑器改进**: 修复 MIUI 长按崩溃，添加注释切换、双指缩放
5. **Bug 修复**: WindowLeaked、FLAG_IMMUTABLE 等兼容性问题
6. **CI/CD 完善**: GitHub Actions 自动构建发布

---

## 10. 总结

### 10.1 项目特点

Auto.js.HYB1996 是一个功能完善的 Android 自动化脚本平台，具有以下特点：

1. **完整的 JavaScript 运行环境**: 基于 Rhino 1.7.14，支持大部分 ES6 特性
2. **丰富的自动化 API**: 涵盖无障碍服务、图像处理、文件操作等
3. **良好的兼容性**: 支持 Android 4.4 到 Android 14
4. **灵活的构建系统**: 支持多渠道、多架构打包
5. **完善的 CI/CD**: 自动构建、签名、发布

### 10.2 与其他变体的定位差异

| 项目 | 定位 |
|------|------|
| HYB1996 | 稳定可靠的基础版本，适合日常使用 |
| Auto.js (TonyJiangWJ) | 功能最全，包含 AI 推理能力 |
| AutoJs6 | 最新架构，持续更新 |
| AutoX | 轻量级，专注核心功能 |

### 10.3 后续发展方向

1. **AI 功能移植**: 从 TonyJiangWJ 版本移植 OCR、YOLO 等功能
2. **KSP 迁移**: 替代已过时的 KAPT
3. **ViewBinding**: 替代 ButterKnife 和 AndroidAnnotations
4. **Kotlin 化**: 逐步将 Java 代码迁移到 Kotlin
5. **现代化架构**: 引入 Jetpack Compose、Hilt 等

---

*报告生成时间: 2026-03-03*
