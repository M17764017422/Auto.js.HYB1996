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
| Gradle | 8.7 |
| AGP (Android Gradle Plugin) | 8.2.2 |
| JDK | 17 |
| Kotlin | 1.9.25 |
| KSP | 1.9.25-1.0.20 |

#### 3.1.2 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Rhino JavaScript Engine | 2.0.0-SNAPSHOT | JavaScript 运行时 (ES6+增强) |
| AndroidX AppCompat | 1.5.1 | 兼容性库 |
| Material Design | 1.7.0 | UI 组件 |
| ButterKnife | 10.2.3 | 视图绑定 (KAPT) |
| RxJava | 2.x | 响应式编程 |
| OpenCV | (内置) | 图像处理 |

#### 3.1.3 注解处理器

| 处理器 | 版本 | 处理方式 | 用途 |
|--------|------|----------|------|
| AndroidAnnotations | 4.8.0 | KAPT | 代码生成 |
| ButterKnife | 10.2.3 | KAPT | 视图绑定 |
| Glide | 4.15.1 | KAPT/KSP | 图片加载 |

> **注**: 项目采用 KAPT + KSP 共存策略，渐进式迁移中。ButterKnife 和 AndroidAnnotations 因不支持 KSP，继续使用 KAPT。

#### 3.1.4 AGP 8.x 兼容性配置

项目已升级至 AGP 8.2.2，需要特别注意以下兼容性配置：

##### 必需配置项

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `android.nonFinalResIds` | `false` | **必需**！ButterKnife 需要 final R.id |
| `android.nonTransitiveRClass` | `false` | 保持 R 类传递性 |
| `namespace` | 各模块配置 | AGP 8.x 强制要求 |
| `buildFeatures.buildConfig` | `true` | AGP 8.x 默认禁用 |

##### gradle.properties 关键配置

```properties
# ButterKnife 兼容性（必需）
android.nonFinalResIds=false
android.nonTransitiveRClass=false

# KAPT 兼容性
kapt.use.worker.api=false
kapt.include.compile.classpath=false
```

##### 各模块 namespace 配置

| 模块 | namespace |
|------|-----------|
| app | `org.autojs.autojs` |
| autojs | `com.stardust.autojs` |
| automator | `com.stardust.automator` |
| common | `com.stardust` |
| inrt | `com.stardust.auojs.inrt` |
| apkbuilder | `com.stardust.autojs.apkbuilder` |

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

## 6. 脚本录制功能

### 6.1 功能概述

Auto.js 提供了完整的操作录制与回放功能，允许用户录制屏幕上的触摸操作，然后自动生成可重复执行的脚本。该功能采用双模式设计，支持 Root 和无障碍两种录制方式。

### 6.2 架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                        录制入口                                  │
│  (悬浮窗按钮 / 音量键控制)                                        │
├─────────────────────────────────────────────────────────────────┤
│                    GlobalActionRecorder                         │
│                    (录制状态管理)                                 │
├───────────────────────────────┬─────────────────────────────────┤
│    Root 触摸录制模式           │     无障碍操作录制模式           │
│    TouchRecorder              │     AccessibilityActionRecorder │
│    ├── InputEventObserver     │     ├── AccessibilityService    │
│    ├── InputEventRecorder     │     ├── AccessibilityEvent      │
│    │   ├── AutoFileRecorder   │     └── AccessibilityAction     │
│    │   └── RootAutomatorRec   │         Converter               │
│    └── getevent -t (Shell)    │                                 │
├───────────────────────────────┴─────────────────────────────────┤
│                        输出格式                                  │
│  ┌─────────────────┐           ┌─────────────────┐              │
│  │ .auto 二进制文件 │           │  JS 代码字符串   │              │
│  │ (回放效率高)     │           │  (可读性好)      │              │
│  └─────────────────┘           └─────────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                        回放执行                                  │
│  engines.execAutoFile()         engines.execScript()            │
│         │                              │                        │
│         ▼                              ▼                        │
│  RootAutomatorEngine            JavaScriptEngine                │
│         │                              │                        │
│         ▼                              ▼                        │
│  root_automator (原生程序)       RootAutomator JS API           │
└─────────────────────────────────────────────────────────────────┘
```

### 6.3 两种录制模式对比

#### 6.3.1 Root 触摸录制模式

**原理**：通过 Shell 命令 `getevent -t` 监听底层输入事件，解析触摸坐标和时间间隔。

**核心类**：
- `InputEventObserver` - 监听输入事件
- `TouchRecorder` - 触摸录制器
- `InputEventToAutoFileRecorder` - 生成二进制 .auto 文件
- `InputEventToRootAutomatorRecorder` - 生成 JS 代码

**执行流程**：
```
getevent -t (Shell)
    ↓
InputEventObserver.onInputEvent()
    ↓
InputEvent.parse() → 解析事件字符串
    ↓
InputEventRecorder.recordInputEvent()
    ↓
生成录制文件/代码
```

**生成的 JS 代码示例**：
```javascript
var ra = new RootAutomator();
ra.setScreenMetrics(1080, 1920);
ra.touchX(500);
ra.touchY(800);
ra.sendSync();
sleep(100);
ra.exit();
```

**使用条件**：
| 条件 | 要求 |
|------|------|
| Root 权限 | **必须** |
| 无障碍服务 | 不需要 |
| 录制触摸手势 | ✅ 支持 |
| 录制滑动操作 | ✅ 支持 |
| 精确时间间隔 | ✅ 支持 |

#### 6.3.2 无障碍操作录制模式

**原理**：通过无障碍服务监听 AccessibilityEvent，转换用户操作为脚本代码。

**核心类**：
- `AccessibilityActionRecorder` - 无障碍操作录制器
- `AccessibilityActionConverter` - 事件转脚本转换器

**监听的事件类型**：
```java
AccessibilityEvent.TYPE_VIEW_CLICKED      // 点击
AccessibilityEvent.TYPE_VIEW_LONG_CLICKED // 长按
AccessibilityEvent.TYPE_VIEW_SCROLLED     // 滚动
AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED // 文本输入
```

**生成的 JS 代码示例**：
```javascript
while(!click(500, 800));      // 点击
while(!longClick(100, 200));  // 长按
while(!input(0, "文本"));      // 输入文本
```

**使用条件**：
| 条件 | 要求 |
|------|------|
| Root 权限 | 不需要 |
| 无障碍服务 | **必须开启** |
| 录制触摸手势 | ❌ 不支持 |
| 录制滑动操作 | ❌ 不支持 |
| 录制点击操作 | ✅ 支持 |
| 录制文本输入 | ✅ 支持 |

### 6.4 .auto 二进制文件格式

当录制设置选择 `binary` 输出时，生成 `.auto` 二进制文件：

```
┌────────────────────────────────────┐
│ 文件头 (256 字节)                   │
├────────────────────────────────────┤
│ 魔数: 0x00B87B6D (4字节)            │
│ 版本号: 1 (4字节)                    │
│ 屏幕宽度 (4字节)                     │
│ 屏幕高度 (4字节)                     │
│ 填充: 240字节                        │
├────────────────────────────────────┤
│ 事件数据 (变长)                      │
│ - 0x01: DATA_TYPE_SLEEP            │
│   ├── byte: type                   │
│   └── int: milliseconds            │
│ - 0x02: DATA_TYPE_TOUCH_X          │
│   ├── byte: type                   │
│   └── int: x coordinate            │
│ - 0x03: DATA_TYPE_TOUCH_Y          │
│   ├── byte: type                   │
│   └── int: y coordinate            │
│ - 0x04: DATA_TYPE_EVENT            │
│   ├── byte: type                   │
│   ├── short: event type            │
│   ├── short: event code            │
│   └── int: event value             │
│ - 0x05: DATA_TYPE_EVENT_SYNC_REPORT│
│   └── byte: type                   │
└────────────────────────────────────┘
```

### 6.5 录制文件回放机制

#### 6.5.1 execAutoFile() 执行流程

```java
// Engines.java
public ScriptExecution execAutoFile(String path, ExecutionConfig config) {
    return mEngineService.execute(new AutoFileSource(path), config);
}

// AutoFileSource.java
public class AutoFileSource extends ScriptSource {
    public static final String ENGINE = AutoFileSource.class.getName() + ".Engine";
    // 标识使用 RootAutomatorEngine 执行
}

// AutoJs.java - 引擎注册
mScriptEngineManager.registerEngine(AutoFileSource.ENGINE, 
    () -> new RootAutomatorEngine(mContext));
```

#### 6.5.2 RootAutomatorEngine 执行原理

```java
public void execute(String autoFile) {
    // 1. 复制 root_automator 可执行文件到缓存目录
    mExecutablePath = getExecutablePath(mContext);
    
    // 2. 通过 su 执行命令
    String[] commands = {
        "chmod 755 " + mExecutablePath,
        "\"" + mExecutablePath + "\" \"" + autoFile + "\" -d \"" + mDeviceNameOrPath + "\" &",
        "echo $!",  // 获取进程 PID
        "exit",
        "exit"
    };
    
    // 3. 执行 Shell 命令
    mProcess = Runtime.getRuntime().exec("su");
    // ... 执行命令并等待
}
```

#### 6.5.3 root_automator 原生程序

`root_automator` 是一个原生 C 程序，负责：
1. 读取 `.auto` 二进制文件
2. 解析事件数据
3. 通过 `/dev/input/eventX` 设备文件注入触摸事件
4. 实现精确的时间控制和事件同步

### 6.6 录制设置选项

| 设置项 | 选项 | 说明 |
|--------|------|------|
| Root 录制输出格式 | `binary` | 生成 .auto 文件，回放效率高 |
| | `js` | 生成 JS 代码，可读性好 |
| 录制提示 | 开启/关闭 | 录制开始时显示 Toast 提示 |
| 音量键控制录制 | 开启/关闭 | 使用音量键开始/停止录制 |
| 录制超时 | 10 分钟 | 自动停止录制 |

### 6.7 录制状态管理

```java
// Recorder.java - 录制状态定义
int STATE_NOT_START = 0;  // 未开始
int STATE_RECORDING = 1;  // 录制中
int STATE_PAUSED   = 2;   // 已暂停
int STATE_STOPPED  = 3;   // 已停止

// 状态转换
STATE_NOT_START → STATE_RECORDING → STATE_STOPPED
                      ↕ STATE_PAUSED
```

### 6.8 启动录制的方式

1. **悬浮窗录制按钮**
   - 点击悬浮窗的录制按钮
   - 自动检测 Root 权限并选择录制模式

2. **音量键控制**
   - 在设置中开启"音量键控制录制"
   - 按音量下键开始/停止录制

3. **录制超时自动停止**
   - 默认 10 分钟后自动停止
   - 防止长时间录制占用资源

### 6.9 录制完成后的处理

```java
// GlobalActionRecorder.java
@Override
public void onStop() {
    if (!mDiscard) {
        String code = getCode();
        if (code != null)
            handleRecordedScript(code);  // JS 代码 → 显示保存对话框
        else
            handleRecordedFile(getPath()); // .auto 文件 → 导入到脚本目录
    }
}
```

### 6.10 两种模式的选择建议

| 场景 | 推荐模式 | 原因 |
|------|----------|------|
| 有 Root 权限 | Root 触摸录制 | 支持完整手势录制，效率高 |
| 无 Root 权限 | 无障碍录制 | 仅需无障碍服务即可使用 |
| 需要录制滑动 | Root 触摸录制 | 无障碍模式不支持滑动手势 |
| 需要编辑脚本 | 选择 JS 输出格式 | 可直接编辑生成的代码 |
| 追求回放效率 | 选择 binary 输出格式 | 原生程序执行，效率最高 |

### 6.11 相关示例脚本

| 脚本 | 路径 | 功能 |
|------|------|------|
| 运行录制文件.js | sample/脚本引擎/ | 选择并运行 .auto 录制文件 |
| 运行脚本文件.js | sample/脚本引擎/ | 选择并运行 .js 脚本文件 |

---

## 7. Rhino 引擎与构建系统升级记录

### 7.1 升级概述

2026年3月7日，项目完成 Rhino JavaScript 引擎从 1.7.14 到 2.0.0-SNAPSHOT 的升级，同时升级构建系统至 AGP 8.2.2。

| 项目 | 升级前 | 升级后 |
|------|--------|--------|
| Rhino 版本 | 1.7.14-jdk7 | 2.0.0-SNAPSHOT |
| AGP 版本 | 7.4.2 | 8.2.2 |
| Gradle 版本 | 8.2 | 8.7 |
| Kotlin 版本 | 1.8.22 | 1.9.25 |
| KSP | 无 | 1.9.25-1.0.20 |
| JAR 大小 | ~900KB | ~1.7MB |

### 7.2 升级过程中遇到的问题

#### 7.2.1 AGP 7.4.2 D8 与 Rhino JAR 不兼容

**问题现象**：
```
NullPointerException at D8 desugaring
```

**原因分析**：
- Rhino 2.0.0 的 `rhino-all` JAR 包含 JLine FFM 类（Java 22 字节码）
- AGP 7.4.2 的 D8 无法处理高版本字节码

**解决方案**：
1. 使用 `rhino` 核心模块（Java 11 字节码）
2. 或升级至 AGP 8.x（推荐）

#### 7.2.2 AGP 8.x R.id 非 final 导致 ButterKnife 失败

**问题现象**：
```
error: @BindView fields must not be private or static
Element ... invalidated by R.class change
```

**原因分析**：
- AGP 8.x 默认将 R.id 设为非 final
- ButterKnife @BindView 需要 final R.id

**解决方案**：
```properties
# gradle.properties
android.nonFinalResIds=false
```

#### 7.2.3 AGP 8.x KAPT 找不到 AndroidManifest.xml

**问题现象**：
```
Could not find the AndroidManifest.xml file
```

**解决方案**：
```groovy
// app/build.gradle
javaCompileOptions {
    annotationProcessorOptions {
        arguments = [
            "resourcePackageName": "org.autojs.autojs"
        ]
    }
}
```

#### 7.2.5 Java 字节码版本不兼容

**问题现象**：
```
Unsupported class file major version 66
```

**原因分析**：
- `rhino-all` 模块包含 JLine 依赖，使用 Java 22 编译（字节码版本 66）
- Android Jetifier 不支持高于 Java 11 的字节码版本

**解决方案**：
使用 `rhino` 核心模块而非 `rhino-all`。Rhino 项目结构：
- `rhino` - 核心模块，无外部依赖，Java 11 字节码
- `rhino-all` - Shadow JAR，包含 JLine（终端库），Java 22 字节码

#### 7.2.6 ShellContextFactory 类缺失

**问题现象**：
```
class com.stardust.autojs.rhino.AndroidContextFactory, unresolved supertypes: ShellContextFactory
```

**原因分析**：
- `ShellContextFactory` 位于 `rhino-tools` 模块
- `rhino-tools` 不包含在核心 `rhino.jar` 中

**解决方案**：
修改 `AndroidContextFactory.java`，从继承 `ShellContextFactory` 改为继承 `ContextFactory`：

```java
// 修改前
import org.mozilla.javascript.tools.shell.ShellContextFactory;
public class AndroidContextFactory extends ShellContextFactory { ... }

// 修改后
import org.mozilla.javascript.ContextFactory;
public class AndroidContextFactory extends ContextFactory { ... }
```

#### 7.2.7 WrapFactory 方法变为 final

**问题现象**：
```
'wrap' in 'WrapFactory' is final and cannot be overridden
'wrapAsJavaObject' in 'WrapFactory' is final and cannot be overridden
```

**原因分析**：
Rhino 2.0.0 引入 `TypeInfo` 类型系统，API 签名变更：
- `wrap(Class<?>)` 方法变为 `final`，委托给 `wrap(TypeInfo)` 方法
- `wrapAsJavaObject(Class<?>)` 方法变为 `final`，委托给 `wrapAsJavaObject(TypeInfo)` 方法

**解决方案**：
修改 `RhinoJavaScriptEngine.kt`，重写 `TypeInfo` 版本：

```kotlin
import org.mozilla.javascript.lc.type.TypeInfo

private inner class WrapFactory : org.mozilla.javascript.WrapFactory() {

    override fun wrap(cx: Context, scope: Scriptable, obj: Any?, staticType: TypeInfo): Any? {
        return when {
            obj is String -> runtime.bridges.toString(obj.toString())
            staticType.is(UiObjectCollection::class.java) -> runtime.bridges.asArray(obj)
            else -> super.wrap(cx, scope, obj, staticType)
        }
    }

    override fun wrapAsJavaObject(cx: Context?, scope: Scriptable, javaObject: Any?, staticType: TypeInfo): Scriptable? {
        return if (javaObject is View) {
            ViewExtras.getNativeView(scope, javaObject, staticType.asClass(), runtime)
        } else {
            super.wrapAsJavaObject(cx, scope, javaObject, staticType)
        }
    }
}
```

**关键 API 变更**：
| 旧 API | 新 API |
|--------|--------|
| `staticType == Class` | `staticType.is(Class)` |
| `staticType` (作为 Class) | `staticType.asClass()` |

### 7.3 Rhino 2.0.0 新增特性

| 特性 | 1.7.14 | 2.0.0 |
|------|--------|-------|
| 可选链 `?.` | ❌ | ✅ |
| 空值合并 `??` | ❌ | ✅ |
| `globalThis` | ❌ | ✅ |
| 箭头函数 | ✅ | ✅ |
| 模板字符串 | ✅ | ✅ |
| `let`/`const` | ✅ | ✅ |
| 解构赋值 | ✅ | ✅ |
| Promise | ✅ | ✅ |

### 7.4 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `build.gradle` | 修改 | AGP 8.2.2, Kotlin 1.9.25, KSP 插件 |
| `gradle.properties` | 修改 | 新增 AGP 8.x 兼容配置 |
| `gradle/wrapper/gradle-wrapper.properties` | 修改 | Gradle 8.7 |
| `app/build.gradle` | 修改 | KSP 插件, buildConfig, namespace |
| `autojs/libs/rhino-1.7.14-jdk7.jar` | 删除 | 避免重复类 |
| `autojs/libs/rhino-2.0.0-SNAPSHOT.jar` | 新增 | Rhino 2.0.0 核心模块 |
| `autojs/.../AndroidContextFactory.java` | 修改 | 继承 ContextFactory |
| `autojs/.../RhinoJavaScriptEngine.kt` | 修改 | 使用 TypeInfo API |

### 7.5 升级经验总结

| 陷阱 | 解决方案 |
|------|----------|
| 使用 `rhino-all` 导致字节码不兼容 | 使用 `rhino` 核心模块 |
| 继承 `ShellContextFactory` | 改为继承 `ContextFactory` |
| 重写 `wrap(Class)` 方法 | 重写 `wrap(TypeInfo)` 方法 |
| 保留旧 JAR 文件 | 删除旧 JAR，避免资源冲突 |
| AGP 8.x R.id 非 final | 设置 `android.nonFinalResIds=false` |
| KAPT 找不到 Manifest | 配置 `resourcePackageName` |
| buildConfig 丢失 | 启用 `buildFeatures.buildConfig` |
| Gradle 使用 C: 盘缓存 | 设置 TEMP/TMP 环境变量到非 C 盘 |

**详细升级报告**: 参见 `UPGRADE_REPORT_RHINO2_AGP8.md`

---

## 8. JavaScript 兼容性

### 8.1 Rhino 2.0.0-SNAPSHOT 支持

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
| 可选链 `?.` | ✅ **新增** |
| 空值合并 `??` | ✅ **新增** |
| `globalThis` | ✅ **新增** |
| 函数默认参数 | ❌ 不支持 |
| 剩余参数 (...) | ❌ 不支持 |
| 扩展运算符 (...) | ❌ 不支持 |
| class 类 | ❌ 不支持 |
| async/await | ❌ 不支持 |
| ES6 import/export | ❌ 不支持 (使用 CommonJS) |

### 8.2 模块系统

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

## 9. 与其他 Auto.js 变体对比

### 9.1 模块对比

| 模块 | HYB1996 | Auto.js (TonyJiangWJ) | AutoJs6 | AutoX |
|------|---------|----------------------|---------|-------|
| 核心模块 (app, console 等) | ✅ | ✅ | ✅ | ✅ |
| $ocr (PaddleOCR) | ❌ | ✅ | ✅ | ❌ |
| $yolo (YOLO) | ❌ | ✅ | ✅ | ❌ |
| $tts (语音合成) | ❌ | ✅ | ✅ | ❌ |
| $shizuku | ❌ | ✅ | ✅ | ❌ |
| $mlKitOcr | ❌ | ✅ | ✅ | ❌ |

### 9.2 技术栈对比

| 项目 | AGP | Gradle | JDK | Kotlin | Rhino | KSP |
|------|-----|--------|-----|--------|-------|-----|
| HYB1996 | **8.2.2** | **8.7** | 17 | **1.9.25** | **2.0.0-SNAPSHOT** | ✅ |
| Auto.js (TonyJiangWJ) | 7.x | 7.x | 17 | 1.9.0 | 1.7.14+1.9.1 | ❌ |
| AutoJs6 | 8.x | 8.x | 17 | 2.0.0 | 1.7.14 | ❌ |
| AutoX | 8.5.0 | 8.7 | 17 | 1.9.25 | 1.7.14 | ✅ |

### 9.3 特色功能对比

| 功能 | HYB1996 | Auto.js (TonyJiangWJ) | AutoJs6 | AutoX |
|------|---------|----------------------|---------|-------|
| 多包名 Flavor | ✅ | ✅ (多分支) | ❌ | ❌ |
| SAF 存储支持 | ✅ | ✅ | ✅ | ❌ |
| WebDAV 同步 | ✅ | ❌ | ❌ | ❌ |
| CI/CD 自动构建 | ✅ | ✅ | ✅ | ❌ |
| AI 推理 | ❌ | ✅ | ✅ | ❌ |

---

## 10. 构建与发布

### 10.1 构建配置

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

### 10.2 CI/CD 流程

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

### 10.3 版本号规则

- **Git Tag**: `v0.80.1`
- **versionName**: `0.80.1` (去掉 v 前缀)
- **versionCode**: `801000` (计算公式: major*1000000 + minor*10000 + patch*1000)

---

## 11. 版本演进记录

| 版本 | 日期 | 主要更新 |
|------|------|----------|
| v2.0.0-rhino2-agp8 | 2026-03-07 | **AGP 升级至 8.2.2，Gradle 8.7，Kotlin 1.9.25，KSP 支持** |
| v2.0.0-rhino-debug | 2026-03-06 | **Rhino 升级至 2.0.0-SNAPSHOT，支持可选链、空值合并** |
| v0.80.1 | 2026-03-03 | 首个正式发布版本 |
| v4.1.1-alpha13 | 2026-03-03 | 编辑器功能增强 |
| v4.1.1-alpha2 | 2026-03-03 | AGP 7.4.2 + 多包名 flavor |

### 11.1 主要改进历程

1. **构建系统升级**: AGP 7.4.2 → 8.2.2，Gradle 8.2 → 8.7，Kotlin 1.8.22 → 1.9.25
2. **KSP 支持**: 新增 KSP 插件，支持 KAPT + KSP 共存
3. **Rhino 引擎升级**: 1.7.14 → 2.0.0-SNAPSHOT，获得 ES6+ 新特性（可选链、空值合并等）
4. **多包名支持**: 实现三个 flavor 可同时安装
5. **存储权限适配**: 支持 Android 11+ 的 MANAGE_EXTERNAL_STORAGE 和 SAF
5. **编辑器改进**: 修复 MIUI 长按崩溃，添加注释切换、双指缩放
6. **Bug 修复**: WindowLeaked、FLAG_IMMUTABLE 等兼容性问题
7. **CI/CD 完善**: GitHub Actions 自动构建发布

---

## 12. 总结

### 12.1 项目特点

Auto.js.HYB1996 是一个功能完善的 Android 自动化脚本平台，具有以下特点：

1. **完整的 JavaScript 运行环境**: 基于 Rhino 1.7.14，支持大部分 ES6 特性
2. **丰富的自动化 API**: 涵盖无障碍服务、图像处理、文件操作等
3. **良好的兼容性**: 支持 Android 4.4 到 Android 14
4. **灵活的构建系统**: 支持多渠道、多架构打包
5. **完善的 CI/CD**: 自动构建、签名、发布

### 12.2 与其他变体的定位差异

| 项目 | 定位 |
|------|------|
| HYB1996 | 稳定可靠的基础版本，适合日常使用 |
| Auto.js (TonyJiangWJ) | 功能最全，包含 AI 推理能力 |
| AutoJs6 | 最新架构，持续更新 |
| AutoX | 轻量级，专注核心功能 |

### 12.3 后续发展方向

1. **AI 功能移植**: 从 TonyJiangWJ 版本移植 OCR、YOLO 等功能
2. **KSP 迁移**: 替代已过时的 KAPT
3. **ViewBinding**: 替代 ButterKnife 和 AndroidAnnotations
4. **Kotlin 化**: 逐步将 Java 代码迁移到 Kotlin
5. **现代化架构**: 引入 Jetpack Compose、Hilt 等

---

*报告生成时间: 2026-03-06*  
*最后更新: Rhino 升级至 2.0.0-SNAPSHOT*
