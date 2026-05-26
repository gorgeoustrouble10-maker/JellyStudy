@echo off
cd /d "%~dp0.."
echo skywalking up start > deploy-skywalking.log
docker compose -f docker-compose.core.yml up -d skywalking-oap skywalking-ui >> deploy-skywalking.log 2>&1
docker compose -f docker-compose.core.yml ps skywalking-oap skywalking-ui >> deploy-skywalking.log 2>&1
echo skywalking up done >> deploy-skywalking.log
