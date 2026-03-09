# KSP 迁移日志

## 概述

记录 Auto.js.HYB1996 项目从 KAPT 迁移到 KSP 的尝试过程。

---

## 第一次尝试: Glide KSP 迁移

**日期**: 2026-03-09

### 目标

将 Glide 注解处理器从 KAPT 迁移到 KSP，作为最简单的迁移起点。

### 初始状态

```groovy
// app/build.gradle
kapt 'com.github.bumptech.glide:compiler:4.15.1'
```

### 迁移步骤

#### 步骤 1: 直接替换为 KSP

```groovy
ksp 'com.github.bumptech.glide:compiler:4.15.1'
```

**结果**: 失败

```
e: [ksp] No providers found in processor classpath.
```

**分析**: Glide 4.12.0/4.15.1 不支持 KSP

#### 步骤 2: 升级 Glide 到 4.16.0

查阅 Glide 官方文档，4.14+ 版本开始支持 KSP。

```groovy
implementation 'com.github.bumptech.glide:glide:4.16.0'
ksp 'com.github.bumptech.glide:compiler:4.16.0'
```

**结果**: 失败

```
e: [ksp] No providers found in processor classpath.
e: Error occurred in KSP, check log for detail
```

#### 步骤 3: 禁用 KSP2

在 `gradle.properties` 中添加：

```properties
ksp.useKSP2=false
```

**结果**: 失败，同样的错误

#### 步骤 4: 验证 KSP 配置

```bash
./gradlew :app:dependencies --configuration kspCoolapkDebugKotlinProcessorClasspath
```

输出显示依赖已正确配置：

```
kspCoolapkDebugKotlinProcessorClasspath
\--- com.github.bumptech.glide:compiler:4.16.0
     \--- com.github.bumptech.glide:annotations:4.16.0
```

### 问题分析

1. **KSP 和 KAPT 共存冲突**: 项目同时使用 KAPT（AndroidAnnotations、ButterKnife）和 KSP，可能导致处理器发现机制冲突

2. **Glide KSP 入口点问题**: Glide compiler 可能未正确注册 KSP Service Provider

3. **版本兼容性**: 
   - Kotlin 1.9.25
   - KSP 1.9.25-1.0.20
   - AGP 8.2.2

### 最终决定

**回滚到 KAPT**，保持现状：

```groovy
implementation('com.github.bumptech.glide:glide:4.12.0', {
    exclude group: 'com.android.support'
})
kapt 'com.github.bumptech.glide:compiler:4.12.0'
```

构建验证成功：`BUILD SUCCESSFUL in 4m 43s`

---

## 第二次尝试: 修正 Artifact 名称

**日期**: 2026-03-09

### 问题根因发现

经过对 AutoX 项目的分析，发现了**失败的真正原因**：

**错误配置**（第一次尝试使用的）：
```groovy
ksp 'com.github.bumptech.glide:compiler:4.16.0'  // 错误！
```

**正确配置**：
```groovy
ksp 'com.github.bumptech.glide:ksp:4.14.2'  // 正确！
```

### Glide Artifact 说明

| Artifact | 用途 | 版本要求 |
|----------|------|----------|
| `com.github.bumptech.glide:compiler` | **KAPT 专用** | 4.x |
| `com.github.bumptech.glide:ksp` | **KSP 专用** | 4.14+ |

**关键发现**: Glide 的 KSP 支持使用**独立的 artifact**（`ksp`），而不是复用 `compiler` artifact。

### AutoX 项目验证

AutoX 项目已成功实现 KAPT + KSP + Compose 三方共存：

```kotlin
// AutoX/app/build.gradle.kts
plugins {
    id("kotlin-kapt")      // KAPT - ButterKnife
    id("com.google.devtools.ksp")  // KSP - Glide, Lifecycle
}

dependencies {
    // KAPT 依赖
    kapt("com.jakewharton:butterknife-compiler:10.2.3")
    
    // KSP 依赖 - 正确的 artifact
    ksp("com.github.bumptech.glide:ksp:4.14.2")
    
    // Compose 依赖
    implementation(platform(libs.compose.bom))
}
```

### 正确的迁移配置

```groovy
// app/build.gradle

android {
    buildFeatures {
        compose true  // 可选
        viewBinding true
        buildConfig true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = '1.5.15'
    }
}

dependencies {
    // Glide - KSP 版本
    implementation 'com.github.bumptech.glide:glide:4.14.2'
    ksp 'com.github.bumptech.glide:ksp:4.14.2'
    
    // KAPT 保留（AndroidAnnotations, ButterKnife）
    kapt "org.androidannotations:androidannotations:4.8.0"
    kapt 'com.jakewharton:butterknife-compiler:10.2.3'
}
```

### 三方共存可行性

| 组件 | 状态 | 说明 |
|------|------|------|
| KAPT + KSP | ✅ 兼容 | 两者可同时使用 |
| KAPT + Compose | ✅ 兼容 | 无直接冲突 |
| KSP + Compose | ✅ 兼容 | 版本需匹配 |
| **KAPT + KSP + Compose** | ✅ **兼容** | AutoX 已验证 |

---

## 后续迁移策略

### 方案 A: 完全移除 KAPT 后再迁移

1. 将 AndroidAnnotations 替换为 ViewBinding（工作量：~120 处注解）
2. 将 ButterKnife 替换为 ViewBinding（工作量：~158 处注解）
3. 移除 `kotlin-kapt` 插件
4. 重新尝试 Glide KSP 迁移

**预估工作量**: 3-5 天

### 方案 B: 保持现状

- KAPT 和 KSP 可以共存
- Glide 保持 KAPT
- 其他新项目/模块使用 KSP

### 方案 C: 等待上游更新

- 等待 Glide 或 KSP 后续版本修复兼容性问题
- 关注 https://github.com/bumptech/glide/issues

---

## 环境信息

| 组件 | 版本 |
|------|------|
| Kotlin | 1.9.25 |
| KSP | 1.9.25-1.0.20 |
| AGP | 8.2.2 |
| Gradle | 8.7 |
| JDK | 17 (JetBrains Runtime) |
| Glide (尝试前) | 4.12.0 |
| Glide (尝试版本) | 4.16.0 |

---

## 参考链接

- [Glide KSP 支持](https://github.com/bumptech/glide/issues/4951)
- [KSP 官方文档](https://kotlinlang.org/docs/ksp-overview.html)
- [KAPT 到 KSP 迁移指南](https://kotlinlang.org/docs/ksp-quickstart.html#migrate-from-kapt)

---

## 变更记录

| 日期 | 操作 | 结果 |
|------|------|------|
| 2026-03-09 | Glide 4.15.1 → KSP (错误 artifact) | 失败 |
| 2026-03-09 | Glide 4.16.0 → KSP (错误 artifact) | 失败 |
| 2026-03-09 | 禁用 KSP2 | 失败 |
| 2026-03-09 | 回滚到 KAPT | 成功 |
| 2026-03-09 | 发现真正原因：artifact 名称错误 | 分析完成 |
| 2026-03-09 | Glide 4.14.2 → KSP (正确 artifact: `ksp`) | **成功** |

---

## 结论

### 第一次尝试失败原因

之前 Glide KSP 迁移失败的**真正原因**是使用了错误的 artifact 名称：
- 错误：`ksp 'com.github.bumptech.glide:compiler:x.x.x'`
- 正确：`ksp 'com.github.bumptech.glide:ksp:4.14.2'`

### 可行性确认

- **KAPT + KSP + Compose 三方共存**：可行（AutoX 已验证）
- **Glide KSP 迁移**：✅ **成功**（需使用正确的 artifact）
- **AndroidAnnotations/ButterKnife**：继续保持 KAPT（不支持 KSP）

### 当前状态

```groovy
// Glide - KSP
implementation 'com.github.bumptech.glide:glide:4.14.2'
ksp 'com.github.bumptech.glide:ksp:4.14.2'

// AndroidAnnotations/ButterKnife - KAPT
kapt "org.androidannotations:androidannotations:4.8.0"
kapt 'com.jakewharton:butterknife-compiler:10.2.3'
```

**KAPT 和 KSP 共存验证成功！BUILD SUCCESSFUL in 19s**
