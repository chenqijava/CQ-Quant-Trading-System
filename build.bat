@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set start_time=%time%
echo 开始构建项目 [开始时间: %start_time%]

echo.
echo [1/4] 检查Git状态...
git status
if errorlevel 1 goto :error_git_status

echo.
echo [2/4] 构建前端项目...
cd web
if errorlevel 1 goto :error_web_dir

call npm run build
if errorlevel 1 goto :error_web_build

echo.
echo [3/4] 构建后端项目...
cd ../api
if errorlevel 1 goto :error_api_dir

@REM call ./gradlew assemble
@REM if errorlevel 1 goto :error_api_build

echo.
echo [4/4] Git提交和推送...
cd ..

set /p commit_msg="请输入提交信息: "
if "!commit_msg!"=="" (
    echo 未输入提交信息，使用默认提交信息
    set commit_msg=自动构建提交 - %date% %time%
)

git add .
if errorlevel 1 goto :error_git_add

git commit -m "!commit_msg!"
if errorlevel 1 goto :error_git_commit

git push
if errorlevel 1 goto :error_git_push

set end_time=%time%
echo.
echo ✓ 所有项目构建完成并已推送到远程仓库！
echo 开始时间: %start_time%
echo 结束时间: %end_time%
echo 提交信息: !commit_msg!
pause
exit /b 0

:error_git_status
echo 错误: Git状态检查失败，请确保当前目录是Git仓库
goto :end_error

:error_web_dir
echo 错误: 无法进入web目录，请检查目录是否存在
goto :end_error

:error_web_build
echo 错误: 前端构建失败，请检查npm配置
goto :end_error

:error_api_dir
echo 错误: 无法进入api目录，请检查目录是否存在
goto :end_error

:error_api_build
echo 错误: 后端构建失败，请检查Gradle配置
goto :end_error

:error_git_add
echo 错误: Git添加文件失败
goto :end_error

:error_git_commit
echo 错误: Git提交失败
goto :end_error

:error_git_push
echo 错误: Git推送失败，请检查网络连接或权限
goto :end_error

:end_error
pause
exit /b 1