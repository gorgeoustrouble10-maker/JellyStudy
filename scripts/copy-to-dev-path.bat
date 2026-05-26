@echo off
chcp 65001 >nul
set "SRC=%~dp0.."
set "DEST=C:\dev\jellystudy"
echo 将项目复制到英文路径，避免 Maven/Docker 中文路径乱码
echo 源: %SRC%
echo 目标: %DEST%
if not exist "C:\dev" mkdir "C:\dev"
if exist "%DEST%" (
    echo 目标已存在，请先手动删除或改名后再运行
    pause
    exit /b 1
)
xcopy "%SRC%" "%DEST%\" /E /I /H /Y /EXCLUDE:%~dp0copy-exclude.txt
echo.
echo 完成。请在 Cursor 中打开: %DEST%
pause
