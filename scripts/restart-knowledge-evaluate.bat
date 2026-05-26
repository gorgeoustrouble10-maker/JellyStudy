@echo off
chcp 65001 >nul
set "MVN=C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot\bin\java.exe"
set "BASE=%~dp0..\jellystudy-parent"
set "LOG=%~dp0..\logs"

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8081 " ^| findstr LISTENING') do taskkill /F /PID %%p >nul 2>&1
for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8083 " ^| findstr LISTENING') do taskkill /F /PID %%p >nul 2>&1
timeout /t 2 /nobreak >nul

cd /d "%BASE%"
call "%MVN%" clean package -DskipTests -pl jellystudy-common,jellystudy-knowledge,jellystudy-evaluate-service -am -q
if errorlevel 1 exit /b 1

if not exist "%LOG%" mkdir "%LOG%"
start "" /B "%JAVA%" -jar "%BASE%\jellystudy-knowledge\target\jellystudy-knowledge-1.0.0-SNAPSHOT.jar" 1>>"%LOG%\knowledge.out.log" 2>>"%LOG%\knowledge.err.log"
start "" /B "%JAVA%" -jar "%BASE%\jellystudy-evaluate-service\target\jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar" 1>>"%LOG%\evaluate.out.log" 2>>"%LOG%\evaluate.err.log"
echo 8081 / 8083 已重新编译并启动，日志: %LOG%
