@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Starting JellyStudy infrastructure (MySQL, Nacos, Redis, SkyWalking)...
docker compose up -d
echo.
echo Wait 60-90 seconds for Nacos and SkyWalking to be ready.
echo Nacos:      http://localhost:8848/nacos
echo SkyWalking: http://localhost:8090
echo Redis:      localhost:6379
echo MySQL:      localhost:3306
pause
