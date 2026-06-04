# 将 nacos-config/*.yaml 推送到 Nacos 配置中心（第十二周作业一）
$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$nacosHost = if ($env:NACOS_HOST) { $env:NACOS_HOST } else { "127.0.0.1" }
$nacosPort = if ($env:NACOS_PORT) { $env:NACOS_PORT } else { "8848" }
$user = if ($env:NACOS_USERNAME) { $env:NACOS_USERNAME } else { "nacos" }
$pass = if ($env:NACOS_PASSWORD) { $env:NACOS_PASSWORD } else { "nacos" }
$base = "http://${nacosHost}:${nacosPort}/nacos/v1/cs/configs"
$configDir = Join-Path $root "nacos-config"

function Publish-NacosConfig($dataId, $filePath) {
    $content = Get-Content $filePath -Raw -Encoding UTF8
    $body = @{
        dataId  = $dataId
        group   = "DEFAULT_GROUP"
        content = $content
        type    = "yaml"
        username = $user
        password = $pass
    }
    $resp = Invoke-RestMethod -Method Post -Uri $base -Body $body
    if ($resp -eq "true") {
        Write-Host "OK  $dataId"
    } else {
        Write-Warning "FAIL $dataId -> $resp"
    }
}

Write-Host "Nacos: $base (user=$user)"
Publish-NacosConfig "jellystudy-evaluate-service.yaml" (Join-Path $configDir "jellystudy-evaluate-service.yaml")
Publish-NacosConfig "jellystudy-coach-service.yaml" (Join-Path $configDir "jellystudy-coach-service.yaml")
Publish-NacosConfig "jellystudy-knowledge.yaml" (Join-Path $configDir "jellystudy-knowledge.yaml")
Publish-NacosConfig "jellystudy-qa.yaml" (Join-Path $configDir "jellystudy-qa.yaml")
Write-Host "Done. 验证:"
Write-Host "  GET http://127.0.0.1:8083/api/evaluations/instance-info"
Write-Host "  GET http://127.0.0.1:8084/api/coach/config"
Write-Host "  GET http://127.0.0.1:8081/api/knowledge-points/config"
Write-Host "  GET http://127.0.0.1:8082/api/questions/config"
