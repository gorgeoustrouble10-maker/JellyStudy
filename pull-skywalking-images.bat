@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo Pulling SkyWalking images...
docker pull apache/skywalking-oap-server:9.7.0
docker pull apache/skywalking-ui:9.7.0
echo Done.
