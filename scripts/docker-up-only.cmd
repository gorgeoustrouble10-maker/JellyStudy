@echo off
chcp 65001 >nul
cd /d "%~dp0.."
echo [%date% %time%] docker up start > deploy-docker.log
docker compose -f docker-compose.core.yml up -d >> deploy-docker.log 2>&1
echo [%date% %time%] docker up done >> deploy-docker.log
