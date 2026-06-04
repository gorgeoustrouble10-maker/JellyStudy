# 第十二周演示验证脚本（Nacos + 双实例 + Coach 配置）
$ErrorActionPreference = "Continue"

Write-Host "=== 1. Nacos 配置导入 ===" -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "import-nacos-config.ps1")

Write-Host "`n=== 2. 评估 instance-info (8083) ===" -ForegroundColor Cyan
try {
    $r1 = Invoke-RestMethod "http://127.0.0.1:8083/api/evaluations/instance-info" -TimeoutSec 8
    $r1 | ConvertTo-Json -Compress
} catch { Write-Warning $_ }

Write-Host "`n=== 3. 评估 instance-info via Gateway (轮询) ===" -ForegroundColor Cyan
1..4 | ForEach-Object {
    try {
        $r = Invoke-RestMethod "http://127.0.0.1:8080/api/evaluations/instance-info" -TimeoutSec 8
        Write-Host "  try $_ -> instanceId=$($r.instanceId) port=$($r.serverPort)"
    } catch { Write-Warning "  try $_ failed" }
}

Write-Host "`n=== 4. Coach Nacos 配置 ===" -ForegroundColor Cyan
try {
    $c = Invoke-RestMethod "http://127.0.0.1:8084/api/coach/config" -TimeoutSec 8
    $c | ConvertTo-Json -Compress
} catch { Write-Warning "Coach config: $_" }

Write-Host "`n=== 4b. Knowledge Nacos 配置 ===" -ForegroundColor Cyan
try {
    $k = Invoke-RestMethod "http://127.0.0.1:8081/api/knowledge-points/config" -TimeoutSec 8
    $k | ConvertTo-Json -Compress
} catch { Write-Warning "Knowledge config: $_" }

Write-Host "`n=== 4c. QA Nacos 配置 ===" -ForegroundColor Cyan
try {
    $q = Invoke-RestMethod "http://127.0.0.1:8082/api/questions/config" -TimeoutSec 8
    $q | ConvertTo-Json -Compress
} catch { Write-Warning "QA config: $_" }

Write-Host "`n=== 5. evaluate-2 双实例 (8085) ===" -ForegroundColor Cyan
try {
    $r2 = Invoke-RestMethod "http://127.0.0.1:8085/api/evaluations/instance-info" -TimeoutSec 5
    $r2 | ConvertTo-Json -Compress
} catch { Write-Host "  8085 not running (Docker: docker compose up -d; local: start-full-stack.ps1)" }

Write-Host "`n=== 6. Deep health check (components) ===" -ForegroundColor Cyan
@(
    @{ name = "knowledge"; url = "http://127.0.0.1:8081/api/health" },
    @{ name = "qa"; url = "http://127.0.0.1:8082/api/health" },
    @{ name = "evaluate"; url = "http://127.0.0.1:8083/api/health" },
    @{ name = "coach"; url = "http://127.0.0.1:8084/api/health" },
    @{ name = "gateway-evaluate"; url = "http://127.0.0.1:8080/api/health/evaluate" }
) | ForEach-Object {
    try {
        $h = Invoke-RestMethod $_.url -TimeoutSec 8
        $comp = if ($h.components) { ($h.components.PSObject.Properties | ForEach-Object { "$($_.Name)=$($_.Value)" }) -join ", " } else { "n/a" }
        Write-Host "  $($_.name): status=$($h.status) components=[$comp]"
    } catch { Write-Warning "  $($_.name) health failed" }
}

Write-Host "`nTip: Docker stack = docker compose up -d | Local dual Gateway = start-full-stack.ps1" -ForegroundColor Yellow
