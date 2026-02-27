# PowerShell 脚本：自动提交并推送修改的构建配置文件

Write-Host "开始自动推送修改的构建配置文件..." -ForegroundColor Green

# 检查 Git 状态
Write-Host "检查 Git 状态..." -ForegroundColor Yellow
git status

# 添加所有修改的文件
Write-Host "添加修改的文件到暂存区..." -ForegroundColor Yellow
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

# 检查是否有需要提交的更改
$hasChanges = git diff --cached --quiet
if ($LASTEXITCODE -ne 0 -or (git diff HEAD --name-only).Trim() -ne "") {
    Write-Host "检测到更改，创建提交..." -ForegroundColor Yellow
    
    # 创建提交
    $commitMessage = "feat: 更新构建配置并添加 GitHub Actions 工作流
    
    - 修复依赖问题，替换已停用的 JCenter 仓库
    - 添加 MavenCentral 和阿里云镜像加速
    - 解决 RootShell 库依赖冲突
    - 添加 GitHub Actions 自动构建工作流
    - 更新 Gradle 版本和内存配置"
    
    git commit -m $commitMessage
    
    if ($?) {
        Write-Host "提交成功！" -ForegroundColor Green
        
        # 推送到远程仓库
        Write-Host "推送到远程仓库..." -ForegroundColor Yellow
        git push origin main
        
        if ($?) {
            Write-Host "推送成功！" -ForegroundColor Green
        } else {
            Write-Host "推送失败，请检查错误信息。" -ForegroundColor Red
        }
    } else {
        Write-Host "提交失败。" -ForegroundColor Red
    }
} else {
    Write-Host "没有检测到更改，无需提交。" -ForegroundColor Yellow
}

Write-Host "脚本执行完成。" -ForegroundColor Green