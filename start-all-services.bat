@echo off
chcp 65001 >nul
echo ==============================================
echo JellyStudy 服务启动脚本
echo ==============================================
echo.

set "BASE_DIR=%~dp0"
if exist "%BASE_DIR%local-secrets.bat" call "%BASE_DIR%local-secrets.bat"
set REDIS_PASSWORD=jellystudy_redis
set NACOS_USERNAME=nacos
set NACOS_PASSWORD=nacos

echo [Step 1] 启动 Docker（含 Nacos 认证 + SkyWalking）...
docker compose -f "%BASE_DIR%docker-compose.core.yml" up -d
echo   Nacos: http://localhost:8848/nacos  用户 nacos / nacos
echo   SkyWalking: http://localhost:8090
echo   Redis: localhost:6379  密码 jellystudy_redis
timeout /t 20 /nobreak >nul

echo [Step 2] 编译项目...
cd "%BASE_DIR%jellystudy-parent"
call mvn clean package -DskipTests -q
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b 1
)
echo 编译成功！
echo.

echo [Step 3] 启动知识点服务 (8081)...
start "jellystudy-knowledge" cmd /k "cd /d %BASE_DIR%jellystudy-parent\jellystudy-knowledge && mvn spring-boot:run"
timeout /t 15 /nobreak >nul

echo [Step 4] 启动评估服务 (8083)...
start "jellystudy-evaluate" cmd /k "cd /d %BASE_DIR%jellystudy-parent\jellystudy-evaluate-service && mvn spring-boot:run"
timeout /t 15 /nobreak >nul

echo [Step 5] 启动问答服务 (8082)...
start "jellystudy-qa" cmd /k "cd /d %BASE_DIR%jellystudy-parent\jellystudy-qa && mvn spring-boot:run"

echo.
echo ==============================================
echo 服务启动中！
echo ==============================================
echo 知识点: http://localhost:8081
echo 问答:   http://localhost:8082
echo 评估:   http://localhost:8083
echo 前端:   cd frontend ^&^& npm run dev  -^> http://127.0.0.1:9945
echo.
pause
