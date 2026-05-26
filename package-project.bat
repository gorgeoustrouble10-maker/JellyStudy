@echo off

chcp 65001 >nul

cd /d "%~dp0"

echo Running packager via PowerShell...

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\package-project.ps1"

echo.

pause

