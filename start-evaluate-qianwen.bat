@echo off
chcp 65001 >nul
cd /d "%~dp0"
if exist "local-secrets.bat" call "local-secrets.bat"
if "%DASHSCOPE_API_KEY%"=="" (
    echo 请先创建 local-secrets.bat 或: set DASHSCOPE_API_KEY=sk-你的密钥
    pause
    exit /b 1
)
set EVALUATE_MODEL_TYPE=qianwen
echo 使用通义千问评估，端口 8083 ...
cd jellystudy-parent\jellystudy-evaluate-service
if exist "target\jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar" (
    java -jar target\jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar
) else (
    mvn spring-boot:run
)
