@echo off
chcp 65001 >nul
echo 拉取 SkyWalking（可选，体积大，报告需要时再运行）
docker pull apache/skywalking-oap-server:9.7.0
docker pull apache/skywalking-ui:9.7.0
echo 完成后运行: docker compose up -d skywalking-oap skywalking-ui
pause
