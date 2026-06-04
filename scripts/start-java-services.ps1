$ErrorActionPreference = "Stop"
. (Join-Path $PSScriptRoot "lib\project-env.ps1")

$root = Import-ProjectSecrets -ProjectRoot (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
Write-DashscopeCoachHint
Clear-JavaServiceEnvOverrides

$agentJar = Join-Path $root "skywalking-agent\skywalking-agent.jar"
$logDir = Join-Path $root "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Wait-ForPort($port, $timeoutSec = 150) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $conn = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($conn) { return $true }
        Start-Sleep 2
    }
    return $false
}

function Wait-ForNacos($timeoutSec = 120) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-RestMethod "http://127.0.0.1:8848/nacos/v1/console/health/readiness" -TimeoutSec 3 | Out-Null
            Write-Host "Nacos ready"
            return $true
        } catch { Start-Sleep 3 }
    }
    Write-Warning "Nacos not ready within ${timeoutSec}s — services may fail on first attempt"
    return $false
}

Wait-ForNacos 120 | Out-Null

function Start-ServiceJar($name, $dir, $jar, $swName, $port = $null, $extraEnv = @{}) {
    $jarRel = "target\$jar"
    $workDir = Join-Path $root "jellystudy-parent\$dir"
    if (-not (Test-Path (Join-Path $workDir $jarRel))) { throw "Missing $(Join-Path $workDir $jarRel)" }
    if ($port) {
        $existing = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
        if ($existing) {
            Write-Host "Port $port already in use, skipping duplicate start for $name"
            return
        }
    }
    $agentArgs = @()
    if (Test-Path $agentJar) {
        $agentArgs = @(
            "-javaagent:`"$agentJar`"",
            "-Dskywalking.collector.backend_service=127.0.0.1:11800",
            "-Dskywalking.agent.service_name=$swName"
        )
    }
    $outLog = Join-Path $logDir "$name.out.log"
    $errLog = Join-Path $logDir "$name.err.log"

    $envVars = @{ SERVER_PORT = "$port" }
    foreach ($kv in $extraEnv.GetEnumerator()) {
        $envVars[$kv.Key] = $kv.Value
    }
    $javaCmd = ($agentArgs + @("-jar", $jarRel)) -join " "
    $fullCmd = New-JavaServiceCmd -ProjectRoot $root -WorkDir $workDir -JavaArgs $javaCmd -ExtraEnv $envVars

    Start-Process cmd.exe -ArgumentList @("/c", $fullCmd) `
        -WorkingDirectory $workDir `
        -RedirectStandardOutput $outLog -RedirectStandardError $errLog `
        -WindowStyle Hidden
    Write-Host "Started $name (logs: logs\$name.out.log)"
    if ($port) {
        if (Wait-ForPort $port 180) {
            Write-Host "$name ready on port $port"
        } else {
            Write-Warning "$name did not bind port $port within 180s — check logs\$name.err.log"
        }
    }
}

Get-Process java -ErrorAction SilentlyContinue | Where-Object {
    $cmd = (Get-CimInstance Win32_Process -Filter "ProcessId=$($_.Id)" -ErrorAction SilentlyContinue).CommandLine
    $cmd -match 'jellystudy'
} | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep 3

Start-ServiceJar "knowledge" "jellystudy-knowledge" "jellystudy-knowledge-1.0.0-SNAPSHOT.jar" "jellystudy-knowledge" 8081 @{}
Start-ServiceJar "evaluate" "jellystudy-evaluate-service" "jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar" "jellystudy-evaluate" 8083 @{ INSTANCE_ID = "evaluate-1"; DUBBO_PROTOCOL_PORT = "50053" }
Start-ServiceJar "coach" "jellystudy-coach-service" "jellystudy-coach-service-1.0.0-SNAPSHOT.jar" "jellystudy-coach" 8084 @{}
Start-ServiceJar "qa" "jellystudy-qa" "jellystudy-qa-1.0.0-SNAPSHOT.jar" "jellystudy-qa" 8082 @{}
Start-ServiceJar "gateway" "jellystudy-gateway" "jellystudy-gateway-1.0.0-SNAPSHOT.jar" "jellystudy-gateway" 8080 @{}
Write-Host "All Java services + Gateway (8080) started."
