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

Write-Host '[4/5] Copying files...'
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
    'docker-compose.legacy-infra-only.yml',
    'docker',
    'start-docker-services.bat',
    'nacos-config',
    'scripts',
    'docs',
    '实验报告-第十二周-Z12.md',
    'start-core.bat',
    'start-all-services.bat',
    'pull-core-images.bat',
    'README.md',
    'RUN_GUIDE.md'
)
foreach ($f in $rootFiles) {
    $src = Join-Path $ProjectDir $f
    if (Test-Path $src) {
        Copy-Item $src (Join-Path $TempDir $f) -Force
        Write-Host "  Copied $f"
    }
}

Write-Host '[5/5] Creating ZIP...'
if (Test-Path $ZipPath) { Remove-Item $ZipPath -Force }
Compress-Archive -Path (Join-Path $TempDir '*') -DestinationPath $ZipPath -Force
Remove-Item $TempDir -Recurse -Force

$info = Get-Item $ZipPath
Write-Host ''
Write-Host '========================================'
Write-Host '         Packaging Complete!'
Write-Host '========================================'
Write-Host "ZIP: $($info.FullName)"
Write-Host ("Size: {0:N2} MB" -f ($info.Length / 1MB))
