# 全栈启动：基础设施已由 Docker 拉起后，启动 5 个 Java 服务 + evaluate-2 双实例 + Gateway dual 负载 + 提示前端
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\project-env.ps1")

$projectRoot = Import-ProjectSecrets -ProjectRoot (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Write-DashscopeCoachHint
$logDir = Join-Path $projectRoot "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
Clear-JavaServiceEnvOverrides

& (Join-Path $PSScriptRoot "start-java-services.ps1")

# 子脚本返回后从 $PSScriptRoot 重新解析路径（避免 PowerShell 作用域污染）
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$logDir = Join-Path $projectRoot "logs"
$evaluate2Dir = Join-Path $projectRoot "jellystudy-parent\jellystudy-evaluate-service"
$evalJarRel = "target\jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar"
if (-not (Get-NetTCPConnection -LocalPort 8085 -State Listen -ErrorAction SilentlyContinue)) {
    if (-not (Test-Path (Join-Path $evaluate2Dir $evalJarRel))) { throw "Missing evaluate jar under $evaluate2Dir" }
    $agentPart = ""
    $agentJar = Join-Path $projectRoot "skywalking-agent\skywalking-agent.jar"
    if (Test-Path $agentJar) {
        $agentPart = "-javaagent:$agentJar -Dskywalking.collector.backend_service=127.0.0.1:11800 -Dskywalking.agent.service_name=jellystudy-evaluate-2 "
    }
    $eval2Java = "$agentPart-jar $evalJarRel"
    $eval2Cmd = New-JavaServiceCmd -ProjectRoot $projectRoot -WorkDir $evaluate2Dir -JavaArgs $eval2Java -ExtraEnv @{
        INSTANCE_ID = "evaluate-2"
        SERVER_PORT = "8085"
        DUBBO_PROTOCOL_PORT = "50055"
    }
    Start-Process cmd.exe -ArgumentList @("/c", $eval2Cmd) `
        -WorkingDirectory $evaluate2Dir `
        -RedirectStandardOutput (Join-Path $logDir "evaluate-2.out.log") `
        -RedirectStandardError (Join-Path $logDir "evaluate-2.err.log") `
        -WindowStyle Hidden
    Write-Host "Started evaluate-2 on 8085"
    Start-Sleep 25
}

# Gateway 使用 dual profile 轮询 8083/8085
Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
    Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
}
Start-Sleep 2
$gwDir = Join-Path $projectRoot "jellystudy-parent\jellystudy-gateway"
$gwJarRel = "target\jellystudy-gateway-1.0.0-SNAPSHOT.jar"
Clear-JavaServiceEnvOverrides
$gwCmd = New-JavaServiceCmd -ProjectRoot $projectRoot -WorkDir $gwDir -JavaArgs "-jar $gwJarRel --spring.profiles.active=dual" -ExtraEnv @{ SERVER_PORT = "8080" }
Start-Process cmd.exe -ArgumentList @("/c", $gwCmd) `
    -WorkingDirectory $gwDir `
    -RedirectStandardOutput (Join-Path $logDir "gateway-dual.out.log") `
    -RedirectStandardError (Join-Path $logDir "gateway-dual.err.log") `
    -WindowStyle Hidden
Write-Host "Gateway restarted with profile=dual (lb 8083+8085)"

Write-Host ""
Write-Host "========== 访问清单 =========="
Write-Host "前端:      http://127.0.0.1:9945/  (需另开: cd frontend && npx vite --host 127.0.0.1 --port 9945)"
Write-Host "Gateway:   http://127.0.0.1:8080/api/gateway/ping"
Write-Host "instance:  http://127.0.0.1:8083/api/evaluations/instance-info"
Write-Host "           http://127.0.0.1:8085/api/evaluations/instance-info"
Write-Host "Coach配置: http://127.0.0.1:8084/api/coach/config"
Write-Host "Knowledge: http://127.0.0.1:8081/api/knowledge-points/config"
Write-Host "QA配置:    http://127.0.0.1:8082/api/questions/config"
Write-Host "Nacos:     http://127.0.0.1:8848/nacos"
Write-Host "SkyWalking:http://127.0.0.1:8090/"
Write-Host "验证脚本:  powershell -File scripts\verify-z12-demo.ps1"
