@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo [1] 启动 SkyWalking OAP + UI ...
docker compose -f docker-compose.skywalking.yml up -d
echo UI: http://localhost:8090
echo.
if not exist "skywalking-agent\skywalking-agent.jar" (
    echo [提示] 未找到 Agent，请先运行: powershell -ExecutionPolicy Bypass -File scripts\download-skywalking-agent.ps1
    pause
    exit /b 1
)
set "SW_OPTS=-javaagent:%~dp0skywalking-agent\skywalking-agent.jar -Dskywalking.agent.service_name=%~2 -Dskywalking.collector.backend_service=127.0.0.1:11800"
echo [2] 用带 Agent 的方式启动各服务（请在新终端分别执行，或修改 start-all-services.bat 加入 %%SW_OPTS%%）
echo 示例:
echo   set JAVA_TOOL_OPTIONS=%SW_OPTS%
echo   cd jellystudy-parent\jellystudy-qa ^&^& mvn spring-boot:run
pause
