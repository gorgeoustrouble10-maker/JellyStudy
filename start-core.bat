@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 启动基础设施（MySQL/Nacos/Redis/Mongo/RabbitMQ/SkyWalking）...
echo 全栈微服务请用: docker compose up -d --build  或  start-docker-services.bat
echo.
docker compose -f docker-compose.core.yml up -d
echo.
echo Nacos:  http://localhost:8848/nacos  (nacos/nacos)
echo MySQL:  localhost:3307  root/123456
echo Redis:  localhost:6379
echo SkyWalking: http://localhost:8090/
echo.
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
