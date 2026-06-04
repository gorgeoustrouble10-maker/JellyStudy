@echo off
REM 提交前准备（双击运行）
cd /d "%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\submit-prep.ps1"
pause
