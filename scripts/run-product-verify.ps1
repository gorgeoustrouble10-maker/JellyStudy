# 一键：自检 + Maven 测试 + 编译 Coach + 重启 Coach + 健康检查
$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$ideaMvn = 'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd'
$mvn = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' }
       elseif (Test-Path $ideaMvn) { $ideaMvn }
       else { throw 'Maven not found' }

& (Join-Path $PSScriptRoot 'check-runtime-prereqs.ps1') -RunMavenTests

Write-Host '=== Compile coach-service ===' -ForegroundColor Cyan
Push-Location (Join-Path $ProjectRoot 'jellystudy-parent')
try {
    & $mvn -q package -pl jellystudy-coach-service -am -DskipTests
} finally { Pop-Location }

Write-Host '=== Restart Coach ===' -ForegroundColor Cyan
& (Join-Path $PSScriptRoot 'restart-coach.ps1')

Start-Sleep -Seconds 8
Write-Host '=== Coach health ===' -ForegroundColor Cyan
$health = Invoke-RestMethod -Uri 'http://127.0.0.1:8084/api/health' -TimeoutSec 10
$health | ConvertTo-Json -Depth 4

$config = Invoke-RestMethod -Uri 'http://127.0.0.1:8084/api/coach/config' -TimeoutSec 10
Write-Host "dashscopeConfigured: $($config.dashscopeConfigured)"

Write-Host '=== Frontend build ===' -ForegroundColor Cyan
Push-Location (Join-Path $ProjectRoot 'frontend')
try {
    if (-not (Test-Path 'node_modules')) { npm ci }
    npm run build
} finally { Pop-Location }

Write-Host '=== Product verify complete ===' -ForegroundColor Green
