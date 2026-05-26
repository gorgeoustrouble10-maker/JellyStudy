@echo off
chcp 65001 >nul
cd /d "%~dp0\.."

docker ps --filter "name=jellystudy-mysql" --format "{{.Names}}" | findstr jellystudy-mysql >nul
if errorlevel 1 (
    echo Start MySQL first: docker compose -f docker-compose.core.yml up -d mysql
    exit /b 1
)

echo Importing demo data via container utf8mb4...
docker cp "scripts\clean-demo-data.sql" jellystudy-mysql:/tmp/clean-demo-data.sql
docker exec jellystudy-mysql sh -c "mysql -uroot -p123456 --default-character-set=utf8mb4 < /tmp/clean-demo-data.sql"
if errorlevel 1 exit /b 1

docker exec jellystudy-mysql mysql -uroot -p123456 --default-character-set=utf8mb4 -e "USE jellystudy; SELECT name, description FROM knowledge_point;"
echo Done. Refresh browser or restart 8081-8083 if needed.
