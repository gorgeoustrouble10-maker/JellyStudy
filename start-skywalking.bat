@echo off
cd /d "%~dp0"
echo ============================================
echo JellyStudy - Start SkyWalking on port 8090
echo ============================================
echo.

echo [1/3] Pull images first time may take 5-15 min...
docker pull apache/skywalking-oap-server:9.7.0
if errorlevel 1 goto fail
docker pull apache/skywalking-ui:9.7.0
if errorlevel 1 goto fail

echo.
echo [2/3] Start containers...
docker compose -f docker-compose.core.yml up -d skywalking-oap skywalking-ui
if errorlevel 1 goto fail

echo.
echo [3/3] Wait 90s for OAP to be ready...
timeout /t 90 /nobreak >nul

docker compose -f docker-compose.core.yml ps skywalking-oap skywalking-ui
echo.
echo Open UI: http://127.0.0.1:8090/
echo If still down, run: docker logs jellystudy-skywalking-oap --tail 30
pause
exit /b 0

:fail
echo.
echo FAILED. Make sure Docker Desktop is running, then run this bat again.
pause
exit /b 1
