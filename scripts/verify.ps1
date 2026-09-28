param(
    [string]$Java21Home = 'D:\Java\JDK-21\jdk-21.0.11+10'
)

$ErrorActionPreference = 'Stop'
$workspacePath = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path

if (-not (Test-Path (Join-Path $Java21Home 'bin\java.exe'))) {
    throw "Java 21 not found: $Java21Home"
}

$env:JAVA_HOME = $Java21Home
$env:Path = "$Java21Home\bin;$env:Path"

Write-Host '[1/4] Backend tests'
Push-Location (Join-Path $workspacePath 'backend')
try { & mvn -B test; if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed' } } finally { Pop-Location }

Write-Host '[2/4] Admin checks and build'
Push-Location (Join-Path $workspacePath 'admin-web')
try {
    & pnpm type-check; if ($LASTEXITCODE -ne 0) { throw 'Admin type check failed' }
    & pnpm test; if ($LASTEXITCODE -ne 0) { throw 'Admin tests failed' }
    & pnpm build; if ($LASTEXITCODE -ne 0) { throw 'Admin build failed' }
} finally { Pop-Location }

Write-Host '[3/4] Miniapp checks and tests'
Push-Location (Join-Path $workspacePath 'miniapp')
try {
    & pnpm type-check; if ($LASTEXITCODE -ne 0) { throw 'Miniapp type check failed' }
    & pnpm test; if ($LASTEXITCODE -ne 0) { throw 'Miniapp tests failed' }
    & pnpm build:npm; if ($LASTEXITCODE -ne 0) { throw 'Miniapp NPM build failed' }
    $tdesignButton = Join-Path $workspacePath 'miniapp\miniprogram\miniprogram_npm\tdesign-miniprogram\button\button.js'
    if (-not (Test-Path $tdesignButton)) { throw 'Miniapp TDesign build artifact is missing' }
} finally { Pop-Location }

Write-Host '[4/4] Runtime health check'
try {
    $health = Invoke-RestMethod -Uri 'http://127.0.0.1:8081/api/v1/health' -TimeoutSec 5
    if ($health.data.status -ne 'UP') { throw 'Backend health status is not UP' }
    Write-Host 'Backend health: UP'
} catch {
    Write-Warning 'Backend is not running; build and automated tests already passed.'
}

Write-Host 'All static checks, automated tests, and builds completed.'
