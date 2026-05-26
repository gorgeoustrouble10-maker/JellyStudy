@echo off
chcp 65001 >nul
echo ========================================
echo   JellyStudy Docker 全栈（推荐入口）
echo ========================================
echo.
echo [1] 编译全部 Java 模块...
call mvn -f jellystudy-parent\pom.xml package -DskipTests -q
if errorlevel 1 (
    echo Maven 编译失败
    exit /b 1
)
echo.
echo [2] 启动基础设施 + 五服务 + Gateway + SkyWalking Agent...
docker compose up -d --build
if errorlevel 1 (
    echo Docker 启动失败
    exit /b 1
)
echo.
echo [3] 等待 Nacos 就绪并导入配置...
timeout /t 15 /nobreak >nul
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\import-nacos-config.ps1
echo.
echo ========== 访问清单 ==========
echo Gateway:    http://localhost:8080/api/evaluations/instance-info
echo evaluate-1: http://localhost:8083/api/evaluations/instance-info
echo evaluate-2: http://localhost:8085/api/evaluations/instance-info
echo Coach:      http://localhost:8084/api/coach/config
echo SkyWalking: http://localhost:8090/
echo 验证:       powershell -File scripts\verify-z12-demo.ps1
echo.
echo 说明: 所有 Java 容器已挂载 SkyWalking Agent，Trace 可在 UI 查看
