@echo off
chcp 65001 >nul
cd /d "%~dp0.."
echo [%date% %time%] docker deploy start > deploy-docker.log
docker compose -f docker-compose.core.yml down >> deploy-docker.log 2>&1
for /f %%v in ('docker volume ls -q ^| findstr jellystudy-redis-data') do docker volume rm %%v >> deploy-docker.log 2>&1
docker compose -f docker-compose.core.yml up -d >> deploy-docker.log 2>&1
echo [%date% %time%] docker deploy done >> deploy-docker.log
