# 下载 SkyWalking Java Agent 到项目 skywalking-agent 目录
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$agentDir = Join-Path $root "skywalking-agent"
if (Test-Path (Join-Path $agentDir "skywalking-agent.jar")) {
    Write-Host "Agent already exists: $agentDir\skywalking-agent.jar"
    exit 0
}
# 国内推荐华为云镜像；失败再试 GitHub / Apache
$mirrors = @(
    "https://mirrors.huaweicloud.com/apache/skywalking/java-agent/9.1.0/apache-skywalking-java-agent-9.1.0.tgz",
    "https://github.com/apache/skywalking-java/releases/download/v9.1.0/apache-skywalking-java-agent-9.1.0.tgz",
    "https://archive.apache.org/dist/skywalking/java-agent/9.1.0/apache-skywalking-java-agent-9.1.0.tgz"
)
$tgz = Join-Path $env:TEMP "skywalking-agent.tgz"
Write-Host "Downloading SkyWalking Java Agent 9.1.0..."
$ok = $false
foreach ($zipUrl in $mirrors) {
    Write-Host "  Try: $zipUrl"
    try {
        curl.exe -L --retry 2 -o $tgz $zipUrl
        if ((Test-Path $tgz) -and (Get-Item $tgz).Length -gt 20000000) { $ok = $true; break }
    } catch { }
}
if (-not $ok) { throw "Download failed from all mirrors" }
New-Item -ItemType Directory -Force -Path $agentDir | Out-Null
tar -xzf $tgz -C $agentDir --strip-components=1
if (Test-Path (Join-Path $agentDir "skywalking-agent.jar")) {
    Write-Host "OK: $agentDir\skywalking-agent.jar"
} else {
    Write-Host "Extract done. Check: $agentDir"
}
