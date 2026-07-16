param(
    [int]$Port = 3000,
    [string]$PackageVersion = "4.32.0",
    [switch]$InstallOnly
)

$ErrorActionPreference = "Stop"

function Require-Command($Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' was not found. Please install Node.js 18+ and npm first."
    }
}

Require-Command "node"
Require-Command "npm"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$runtimeDir = Join-Path $repoRoot ".runtime\netease-api"
$packageDir = Join-Path $runtimeDir "node_modules\NeteaseCloudMusicApi"

if (-not (Test-Path $runtimeDir)) {
    New-Item -ItemType Directory -Path $runtimeDir | Out-Null
}

if (-not (Test-Path (Join-Path $runtimeDir "package.json"))) {
    Push-Location $runtimeDir
    try {
        npm init -y | Out-Host
    } finally {
        Pop-Location
    }
}

if (-not (Test-Path (Join-Path $packageDir "app.js"))) {
    Push-Location $runtimeDir
    try {
        npm install "NeteaseCloudMusicApi@$PackageVersion" | Out-Host
    } finally {
        Pop-Location
    }
}

if ($InstallOnly) {
    Write-Host "NeteaseCloudMusicApi installed at $packageDir"
    exit 0
}

$listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($listener) {
    Write-Host "Port $Port is already listening. NeteaseCloudMusicApi may already be running."
    Write-Host "Check: http://127.0.0.1:$Port/search?keywords=test&limit=1"
    exit 0
}

Write-Host "Starting NeteaseCloudMusicApi on http://127.0.0.1:$Port"
Write-Host "Press Ctrl+C to stop."
$env:PORT = "$Port"
Push-Location $packageDir
try {
    node app.js
} finally {
    Pop-Location
}
