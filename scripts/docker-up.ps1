# Docker 全栈一键启动（Windows）
$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Set-Location $root

Write-Host "=== [1/3] Maven package ===" -ForegroundColor Cyan
& mvn -f jellystudy-parent\pom.xml package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

Write-Host "=== [2/3] docker compose up ===" -ForegroundColor Cyan
docker compose up -d --build
if ($LASTEXITCODE -ne 0) { throw "docker compose failed" }

Write-Host "=== [3/3] Nacos config import ===" -ForegroundColor Cyan
Start-Sleep 20
& (Join-Path $PSScriptRoot "import-nacos-config.ps1")

Write-Host ""
Write-Host "Gateway:    http://127.0.0.1:8080" -ForegroundColor Green
Write-Host "SkyWalking: http://127.0.0.1:8090/" -ForegroundColor Green
Write-Host "Verify:     powershell -File scripts\verify-z12-demo.ps1" -ForegroundColor Yellow
