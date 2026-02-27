@echo off
echo 开始自动推送修改的构建配置文件...

echo 检查 Git 状态...
git status

echo 添加修改的文件到暂存区...
git add .github/workflows/android.yml
git add .github/workflows/android-test.yml
git add build.gradle
git add app/build.gradle
git add autojs/build.gradle
git add common/build.gradle
git add automator/build.gradle
git add inrt/build.gradle
git add gradle.properties
git add gradle/wrapper/gradle-wrapper.properties
git add local.properties

REM 检查是否有更改
git diff --cached --quiet
if errorlevel 1 (
    echo 检测到更改，创建提交...
    
    git commit -m "feat: 更新构建配置并添加 GitHub Actions 工作流
    
- 修复依赖问题，替换已停用的 JCenter 仓库
- 添加 MavenCentral 和阿里云镜像加速
- 解决 RootShell 库依赖冲突
- 添加 GitHub Actions 自动构建工作流
- 更新 Gradle 版本和内存配置"
    
    if errorlevel 1 (
        echo 提交失败。
        pause
        exit /b 1
    ) else (
        echo 提交成功！
        echo 推送到远程仓库...
        git push origin main
        
        if errorlevel 1 (
            echo 推送失败，请检查错误信息。
            pause
            exit /b 1
        ) else (
            echo 推送成功！
        )
    )
) else (
    echo 没有检测到更改，无需提交。
)

echo 脚本执行完成。
pause