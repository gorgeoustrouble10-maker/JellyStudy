@echo off
chcp 65001 >nul
set "BASE=%~dp0"
set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot\bin\java.exe"
set "LOG=%BASE%logs"
set "PARENT=%BASE%jellystudy-parent"

echo [1/5] Docker: MySQL + Nacos + Redis ...
cd /d "%BASE%"
docker compose -f docker-compose.core.yml up -d
timeout /t 8 /nobreak >nul

echo [2/5] Maven compile (skip tests) ...
cd /d "%PARENT%"
call "%MVN%" -q package -DskipTests -pl jellystudy-common,jellystudy-knowledge,jellystudy-qa,jellystudy-evaluate-service -am
if errorlevel 1 (
    echo 编译失败，请检查磁盘与 JDK
    pause
    exit /b 1
)

echo [3/5] 停止旧 Java 进程 (8081-8083) ...
for %%p in (8081 8082 8083) do for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":%%p " ^| findstr LISTENING') do taskkill /F /PID %%a >nul 2>&1
timeout /t 2 /nobreak >nul

if not exist "%LOG%" mkdir "%LOG%"
echo [4/5] 启动 8081 / 8082 / 8083 ...
start "knowledge-8081" /B "%JAVA%" -jar "%PARENT%\jellystudy-knowledge\target\jellystudy-knowledge-1.0.0-SNAPSHOT.jar" 1>>"%LOG%\knowledge.out.log" 2>>"%LOG%\knowledge.err.log"
start "qa-8082" /B "%JAVA%" -jar "%PARENT%\jellystudy-qa\target\jellystudy-qa-1.0.0-SNAPSHOT.jar" 1>>"%LOG%\qa.out.log" 2>>"%LOG%\qa.err.log"
start "evaluate-8083" /B "%JAVA%" -jar "%PARENT%\jellystudy-evaluate-service\target\jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar" 1>>"%LOG%\evaluate.out.log" 2>>"%LOG%\evaluate.err.log"

echo 等待服务就绪 ...
timeout /t 25 /nobreak >nul

echo [5/5] 前端 (新窗口) ...
start "frontend-9945" cmd /k "cd /d %BASE%frontend && npm run dev"

echo.
echo ========================================
echo 知识点  http://127.0.0.1:8081
echo 问答    http://127.0.0.1:8082
echo 评估    http://127.0.0.1:8083
echo 前端    http://127.0.0.1:9945
echo Nacos   http://127.0.0.1:8848/nacos
echo 日志    %LOG%
echo ========================================
pause
