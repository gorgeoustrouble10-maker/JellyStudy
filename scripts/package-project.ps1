# JellyStudy 打包脚本（PowerShell，避免 .bat LF 换行导致闪退）
$ErrorActionPreference = 'Stop'
$ProjectDir = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

Set-Location $ProjectDir
$PackageName = '32308117_jellystudy_temp'
$ZipName = '32308117_jellystudy.zip'
$TempDir = Join-Path $ProjectDir $PackageName
$ZipPath = Join-Path $ProjectDir $ZipName

Write-Host '========================================'
Write-Host '   JellyStudy Packaging (PowerShell)'
Write-Host '========================================'

Write-Host '[1/5] Cleaning target directories...'
Get-ChildItem -Path (Join-Path $ProjectDir 'jellystudy-parent') -Directory -Recurse -Filter 'target' -ErrorAction SilentlyContinue |
    ForEach-Object {
        Write-Host "  Removing $($_.FullName)"
        Remove-Item $_.FullName -Recurse -Force -ErrorAction SilentlyContinue
    }

Write-Host '[2/5] Cleaning frontend node_modules / dist...'
foreach ($sub in @('node_modules', 'dist')) {
    $p = Join-Path $ProjectDir "frontend\$sub"
    if (Test-Path $p) {
        Write-Host "  Removing $p"
        Remove-Item $p -Recurse -Force -ErrorAction SilentlyContinue
    }
}

Write-Host '[3/5] Preparing temp folder (not removing .vscode in source)...'
if (Test-Path $TempDir) { Remove-Item $TempDir -Recurse -Force }
New-Item -ItemType Directory -Path $TempDir | Out-Null

Write-Host '[4/5] Copying files (excluding secrets)...'
$secretExclude = @('local-secrets.bat', '.env')
$copyItems = @(
    @{ Src = 'jellystudy-parent'; Dst = 'jellystudy-parent' },
    @{ Src = 'frontend'; Dst = 'frontend' }
)
foreach ($item in $copyItems) {
    $src = Join-Path $ProjectDir $item.Src
    $dst = Join-Path $TempDir $item.Dst
    Write-Host "  Copying $($item.Src)..."
    robocopy $src $dst /E /XD target node_modules dist .git /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
    if ($LASTEXITCODE -ge 8) { throw "robocopy failed for $($item.Src) exit $LASTEXITCODE" }
}

$rootFiles = @(
    'init-database.sql',
    'docker-compose.yml',
    'docker-compose.core.yml',
    'docker-compose.services.yml',
    'docker-compose.skywalking.yml',
    'docker-compose.legacy-infra-only.yml',
    'docker',
    'start-docker-services.bat',
    'start-core.bat',
    'start-all-services.bat',
    'pull-core-images.bat',
    'package-project.bat',
    'submit-prep.bat',
    'nacos-config',
    'scripts',
    'docs',
    'README.md',
    'RUN_GUIDE.md',
    '.env.example'
)
foreach ($f in $rootFiles) {
    $src = Join-Path $ProjectDir $f
    $dst = Join-Path $TempDir $f
    if (-not (Test-Path $src)) {
        Write-Host "  Skip (missing): $f"
        continue
    }
    if (Test-Path $src -PathType Container) {
        New-Item -ItemType Directory -Force -Path $dst | Out-Null
        robocopy $src $dst /E /XD target node_modules dist .git /NFL /NDL /NJH /NJS /nc /ns /np | Out-Null
        if ($LASTEXITCODE -ge 8) { throw "robocopy failed for $f exit $LASTEXITCODE" }
    } else {
        if ($secretExclude -contains $f) {
            Write-Host "  Skip (secret): $f"
            continue
        }
        Copy-Item $src $dst -Force
    }
    Write-Host "  Copied $f"
}

foreach ($secretName in $secretExclude) {
    $secretPath = Join-Path $TempDir $secretName
    if (Test-Path $secretPath) {
        Remove-Item $secretPath -Force
        Write-Host "  Removed secret from package: $secretName"
    }
}

Get-ChildItem -LiteralPath $ProjectDir -File -Filter "*.md" | Where-Object {
    $_.Name -match 'Z12|JellyCoach|Redis与微服务'
} | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $TempDir $_.Name) -Force
    Write-Host "  Copied $($_.Name)"
}

Write-Host '[5/5] Creating ZIP...'
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
$stagingZip = Join-Path $env:TEMP "32308117_jellystudy_staging.zip"
if (Test-Path $stagingZip) { Remove-Item $stagingZip -Force }
Compress-Archive -Path (Join-Path $TempDir '*') -DestinationPath $stagingZip -Force
Move-Item $stagingZip $ZipPath -Force
Remove-Item $TempDir -Recurse -Force

# 校验关键路径
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($ZipPath)
$mustHave = @('nacos-config/jellystudy-qa.yaml', 'scripts/verify-z12-demo.ps1', 'docs/SUBMISSION_CHECKLIST.md')
foreach ($entry in $mustHave) {
    $found = $zip.Entries | Where-Object { $_.FullName -replace '\\','/' -eq $entry }
    if (-not $found) { Write-Warning "ZIP missing: $entry" } else { Write-Host "  ZIP OK: $entry" }
}
$forbidden = @('local-secrets.bat', '.env')
foreach ($bad in $forbidden) {
    $leak = $zip.Entries | Where-Object { $_.FullName -replace '\\','/' -eq $bad }
    if ($leak) { throw "ZIP must not contain secret file: $bad" }
}
$zip.Dispose()

$info = Get-Item $ZipPath
Write-Host ''
Write-Host '========================================'
Write-Host '         Packaging Complete!'
Write-Host '========================================'
Write-Host "ZIP: $($info.FullName)"
Write-Host ("Size: {0:N2} MB" -f ($info.Length / 1MB))
