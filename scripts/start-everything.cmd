@echo off
chcp 65001 >nul
cd /d "%~dp0.."
echo [%date% %time%] start-everything > start-everything.log

echo [1] Docker compose up -d >> start-everything.log 2>&1
docker compose -f docker-compose.core.yml up -d >> start-everything.log 2>&1

echo [2] done >> start-everything.log
