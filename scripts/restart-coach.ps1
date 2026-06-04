# 仅重启 Coach (8084)，启动前自动 call local-secrets.bat（千问密钥）
$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\project-env.ps1")

$root = Import-ProjectSecrets
Write-DashscopeCoachHint
Clear-JavaServiceEnvOverrides

$logDir = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$coachDir = Join-Path $root "jellystudy-parent\jellystudy-coach-service"
$jarRel = "target\jellystudy-coach-service-1.0.0-SNAPSHOT.jar"
if (-not (Test-Path (Join-Path $coachDir $jarRel))) {
    throw "Missing $jarRel — run: tools\apache-maven-3.9.6\bin\mvn.cmd -f jellystudy-parent\pom.xml package -pl jellystudy-coach-service -am -DskipTests"
}

Stop-ListenerOnPort 8084
Start-Sleep 2

$agentJar = Join-Path $root "skywalking-agent\skywalking-agent.jar"
$agentPart = ""
if (Test-Path $agentJar) {
    $agentPart = "-javaagent:`"$agentJar`" -Dskywalking.collector.backend_service=127.0.0.1:11800 -Dskywalking.agent.service_name=jellystudy-coach "
}

$javaArgs = "$agentPart-jar $jarRel"
$cmd = New-JavaServiceCmd -ProjectRoot $root -WorkDir $coachDir -JavaArgs $javaArgs -ExtraEnv @{ SERVER_PORT = "8084" }

Start-Process cmd.exe -ArgumentList @("/c", $cmd) `
    -WorkingDirectory $coachDir `
    -RedirectStandardOutput (Join-Path $logDir "coach.out.log") `
    -RedirectStandardError (Join-Path $logDir "coach.err.log") `
    -WindowStyle Hidden

Write-Host "Coach starting on 8084 (logs: logs\coach.out.log)"
$deadline = (Get-Date).AddSeconds(90)
while ((Get-Date) -lt $deadline) {
    if (Get-NetTCPConnection -LocalPort 8084 -State Listen -ErrorAction SilentlyContinue) {
        Write-Host "Coach ready: http://127.0.0.1:8084/api/coach/config"
        exit 0
    }
    Start-Sleep 2
}
Write-Warning "Coach did not bind 8084 within 90s — check logs\coach.err.log"
exit 1
