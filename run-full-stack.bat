@echo off
chcp 65001 >nul
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File "%~dp0scripts\run-full-stack.ps1"
pause
