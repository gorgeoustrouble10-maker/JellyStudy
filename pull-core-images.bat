@echo off
chcp 65001 >nul
echo ========================================
echo  逐个拉取镜像（网络不稳可多次运行）
echo ========================================
cd /d "%~dp0"

echo [1/3] Redis (通常已有)...
docker pull redis:7-alpine
if %errorlevel% neq 0 goto retry

echo [2/3] MySQL 8.0 (较大，请耐心等待)...
docker pull mysql:8.0
if %errorlevel% neq 0 goto retry

echo [3/3] Nacos 2.2.3...
docker pull nacos/nacos-server:v2.2.3
if %errorlevel% neq 0 goto retry

echo.
echo 全部镜像拉取完成！
echo 下一步: start-core.bat
pause
exit /b 0

:retry
echo.
echo 某一步失败，请检查网络后重新运行本脚本。
pause
exit /b 1
