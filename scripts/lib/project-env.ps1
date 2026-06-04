# 共享：加载 local-secrets.bat、校验千问密钥、构造带 secrets 的 cmd 启动命令
function Import-ProjectSecrets {
    param([string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path)

    $secretsBat = Join-Path $ProjectRoot "local-secrets.bat"
    if (Test-Path $secretsBat) {
        Get-Content $secretsBat -Encoding UTF8 | ForEach-Object {
            if ($_ -match '^\s*set\s+(\w+)=(.*)$') {
                Set-Item -Path "env:$($Matches[1])" -Value $Matches[2].Trim()
            }
        }
        Write-Host "Loaded secrets from local-secrets.bat"
    } else {
        Write-Warning "local-secrets.bat not found — copy local-secrets.bat.example and set DASHSCOPE_API_KEY"
    }

    if (-not $env:REDIS_PASSWORD) { $env:REDIS_PASSWORD = "jellystudy_redis" }
    if (-not $env:NACOS_USERNAME) { $env:NACOS_USERNAME = "nacos" }
    if (-not $env:NACOS_PASSWORD) { $env:NACOS_PASSWORD = "nacos" }
    if (-not $env:MYSQL_PASSWORD) { $env:MYSQL_PASSWORD = "123456" }

    return $ProjectRoot
}

function Test-DashscopeApiKeyConfigured {
    return -not [string]::IsNullOrWhiteSpace($env:DASHSCOPE_API_KEY)
}

function Write-DashscopeCoachHint {
    if (Test-DashscopeApiKeyConfigured) {
        Write-Host "DASHSCOPE_API_KEY: configured (Coach/Socratic & Qianwen evaluate can call DashScope)" -ForegroundColor Green
    } else {
        Write-Warning "DASHSCOPE_API_KEY is empty — Coach Socratic/quiz will use template fallback; set key in local-secrets.bat"
    }
}

function Clear-JavaServiceEnvOverrides {
    Remove-Item Env:SERVER_PORT -ErrorAction SilentlyContinue
    Remove-Item Env:INSTANCE_ID -ErrorAction SilentlyContinue
    Remove-Item Env:DUBBO_PROTOCOL_PORT -ErrorAction SilentlyContinue
}

function New-JavaServiceCmd {
    param(
        [Parameter(Mandatory = $true)][string]$ProjectRoot,
        [Parameter(Mandatory = $true)][string]$WorkDir,
        [Parameter(Mandatory = $true)][string]$JavaArgs,
        [hashtable]$ExtraEnv = @{}
    )

    $secretsBat = Join-Path $ProjectRoot "local-secrets.bat"
    $prefix = ""
    if (Test-Path $secretsBat) {
        $prefix = "call `"$secretsBat`" && "
    }

    $envChain = @()
    if ($ExtraEnv) {
        foreach ($kv in $ExtraEnv.GetEnumerator()) {
            $envChain += "set $($kv.Key)=$($kv.Value)"
        }
    }
    $envPart = if ($envChain.Count) { ($envChain -join " && ") + " && " } else { "" }

    return "${prefix}cd /d `"$WorkDir`" && ${envPart}java ${JavaArgs}"
}

function Stop-ListenerOnPort {
    param([int]$Port)
    Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | ForEach-Object {
        Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped process on port $Port (PID $($_.OwningProcess))"
    }
}
