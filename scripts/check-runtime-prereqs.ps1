# 启动前/提交前：密钥、打包安全、可选 Maven 测试
param(
    [switch]$RunMavenTests,
    [switch]$StrictSecrets
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
. (Join-Path $PSScriptRoot 'lib\project-env.ps1')

Import-ProjectSecrets -ProjectRoot $ProjectRoot | Out-Null
Write-Host '=== JellyStudy runtime prerequisites ===' -ForegroundColor Cyan

$ok = $true
Write-DashscopeCoachHint

if (-not (Test-DashscopeApiKeyConfigured)) {
    Write-Warning 'Coach/Evaluate Qianwen: DASHSCOPE_API_KEY not set (degraded AI mode)'
    if ($StrictSecrets) { $ok = $false }
}

$secretsFile = Join-Path $ProjectRoot 'local-secrets.bat'
if (-not (Test-Path $secretsFile)) {
    Write-Warning 'Missing local-secrets.bat — copy from local-secrets.bat.example'
    if ($StrictSecrets) { $ok = $false }
}

$trackedSecrets = git -C $ProjectRoot ls-files 'local-secrets.bat' 2>$null
if ($trackedSecrets) {
    Write-Warning 'local-secrets.bat is tracked by git — remove from index before push'
    $ok = $false
}

function Resolve-MavenCmd {
    if (Get-Command mvn -ErrorAction SilentlyContinue) { return 'mvn' }
    $ideaMvn = 'C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd'
    if (Test-Path $ideaMvn) { return $ideaMvn }
    throw 'Maven not found. Add mvn to PATH or install IntelliJ bundled Maven.'
}

if ($RunMavenTests) {
    $mvn = Resolve-MavenCmd
    Write-Host "Running Maven unit tests (subset) via $mvn ..." -ForegroundColor Cyan
    Push-Location (Join-Path $ProjectRoot 'jellystudy-parent')
    try {
        & $mvn -q install -pl jellystudy-common -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Maven install common failed exit $LASTEXITCODE" }
        $testSteps = @(
            @('jellystudy-common', 'JellystudyHealthControllerTest,ApiKeyAuthFilterTest'),
            @('jellystudy-coach-service', 'SocraticDialogueGuardTest,CoachAiTagsTest,CoachKnowledgeFilterTest,KnowledgeMasteryBuilderTest'),
            @('jellystudy-knowledge', 'KnowledgePointControllerTest,KnowledgePointServiceImplTest'),
            @('jellystudy-qa', 'QuestionControllerSearchTest,QuestionRankScoringTest')
        )
        foreach ($step in $testSteps) {
            & $mvn -q test -pl $step[0] "-Dtest=$($step[1])"
            if ($LASTEXITCODE -ne 0) { throw "Maven test failed for $($step[0]) exit $LASTEXITCODE" }
        }
        Write-Host 'Maven tests passed.' -ForegroundColor Green
    } finally {
        Pop-Location
    }
}

if (-not $ok) {
    throw 'Prerequisite check failed. Fix warnings above or omit -StrictSecrets.'
}
Write-Host 'Prerequisite check complete.' -ForegroundColor Green
