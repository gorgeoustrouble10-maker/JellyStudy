# 一键：重建 Redis → Docker 全栈 → Maven 编译 → 后端三服务 + 前端
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root

$secretsBat = Join-Path $root "local-secrets.bat"
if (Test-Path $secretsBat) {
    Get-Content $secretsBat -Encoding UTF8 | ForEach-Object {
        if ($_ -match '^\s*set\s+(\w+)=(.*)$') {
            Set-Item -Path "env:$($Matches[1])" -Value $Matches[2].Trim()
        }
    }
}

$env:MYSQL_PASSWORD = if ($env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD } else { "123456" }
$env:REDIS_PASSWORD = if ($env:REDIS_PASSWORD) { $env:REDIS_PASSWORD } else { "jellystudy_redis" }
$env:NACOS_USERNAME = if ($env:NACOS_USERNAME) { $env:NACOS_USERNAME } else { "nacos" }
$env:NACOS_PASSWORD = if ($env:NACOS_PASSWORD) { $env:NACOS_PASSWORD } else { "nacos" }

Write-Host "=== [1/6] Docker down ===" -ForegroundColor Cyan
docker compose -f docker-compose.core.yml down 2>&1 | Out-Host

Write-Host "=== [2/6] Remove Redis volume (fresh password) ===" -ForegroundColor Cyan
$vols = docker volume ls -q | Select-String "jellystudy-redis-data"
foreach ($v in $vols) {
    Write-Host "  docker volume rm $v"
    docker volume rm $v 2>&1 | Out-Host
}

Write-Host "=== [3/6] Docker up (MySQL + Nacos auth + Redis + SkyWalking) ===" -ForegroundColor Cyan
docker compose -f docker-compose.core.yml up -d 2>&1 | Out-Host
Start-Sleep -Seconds 25

Write-Host "=== [4/6] SkyWalking Java Agent ===" -ForegroundColor Cyan
& (Join-Path $PSScriptRoot "download-skywalking-agent.ps1")

Write-Host "=== [5/6] Maven package ===" -ForegroundColor Cyan
$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
    foreach ($p in @(
        "$env:MAVEN_HOME\bin\mvn.cmd",
        "C:\Program Files\Apache\maven\bin\mvn.cmd",
        "$env:USERPROFILE\scoop\apps\maven\current\bin\mvn.cmd"
    )) {
        if (Test-Path $p) { $mvn = $p; break }
    }
}
$bundledMvn = Join-Path $root "tools\apache-maven-3.9.6\bin\mvn.cmd"
if (-not $mvn -and (Test-Path $bundledMvn)) { $mvn = $bundledMvn }
if (-not $mvn) { throw "未找到 mvn，请安装 Maven 或运行 scripts 下载 tools/apache-maven-3.9.6" }
Set-Location (Join-Path $root "jellystudy-parent")
& $mvn clean package -DskipTests -q
if ($LASTEXITCODE -ne 0) { throw "Maven 编译失败" }
Set-Location $root

$agentJar = Join-Path $root "skywalking-agent\skywalking-agent.jar"

function Start-JellyService($name, $jarDir, $jarName, $port, $swName) {
    $jar = Join-Path $root "jellystudy-parent\$jarDir\target\$jarName"
    if (-not (Test-Path $jar)) { throw "缺少 JAR: $jar" }
    $argList = @("-jar", $jar)
    if (Test-Path $agentJar) {
        $argList = @(
            "-javaagent:$agentJar",
            "-Dskywalking.collector.backend_service=127.0.0.1:11800",
            "-Dskywalking.agent.service_name=$swName"
        ) + $argList
    }
    Write-Host "  启动 $name (:$port) ..."
    Start-Process -FilePath "java" -ArgumentList $argList `
        -WorkingDirectory (Join-Path $root "jellystudy-parent\$jarDir") `
        -WindowStyle Minimized
}

Write-Host "=== [6/6] 启动 Java 服务 + 前端 ===" -ForegroundColor Cyan
Get-Process -Name "java" -ErrorAction SilentlyContinue | Where-Object {
    $_.Path -like "*"
} | Out-Null

Start-JellyService "knowledge" "jellystudy-knowledge" "jellystudy-knowledge-1.0.0-SNAPSHOT.jar" 8081 "jellystudy-knowledge"
Start-Sleep -Seconds 12
Start-JellyService "evaluate" "jellystudy-evaluate-service" "jellystudy-evaluate-service-1.0.0-SNAPSHOT.jar" 8083 "jellystudy-evaluate"
Start-Sleep -Seconds 12
Start-JellyService "qa" "jellystudy-qa" "jellystudy-qa-1.0.0-SNAPSHOT.jar" 8082 "jellystudy-qa"

Set-Location (Join-Path $root "frontend")
if (-not (Test-Path "node_modules")) { npm install 2>&1 | Out-Host }
Start-Process -FilePath "npm" -ArgumentList "run", "dev" -WorkingDirectory (Join-Path $root "frontend") -WindowStyle Minimized

Write-Host ""
Write-Host "完成。访问:" -ForegroundColor Green
Write-Host "  前端     http://127.0.0.1:9945"
Write-Host "  Nacos    http://127.0.0.1:8848/nacos  (nacos / nacos)"
Write-Host "  SkyWalking http://127.0.0.1:8090"
Write-Host "  API      http://127.0.0.1:8082/api/questions"
