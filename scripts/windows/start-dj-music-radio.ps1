param(
    [switch] $NoClean,
    [int] $TimeoutSeconds = 90
)

$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$repo = "D:\projects\dj-music-radio"
$projectPorts = @(3000, 5173, 8080, 8081, 8082, 8083, 8084)
$middlewarePorts = @(3306, 5672, 6379, 8848, 15672)
$legacyTasks = @("DJMusicRadio-frontend", "DJMusicRadio-module-chat")
$middlewareTasks = @("CodexRedis", "CodexNacosStandalone")
$appTasks = @(
    "Codex-DJMusicRadio-netease-api",
    "Codex-DJMusicRadio-gateway",
    "Codex-DJMusicRadio-module-user",
    "Codex-DJMusicRadio-module-music",
    "Codex-DJMusicRadio-module-rec",
    "Codex-DJMusicRadio-module-chat",
    "Codex-DJMusicRadio-frontend"
)

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message" -ForegroundColor Cyan
}

function Stop-TaskIfExists($taskName) {
    $task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if ($task) {
        Stop-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    }
}

function Start-TaskIfExists($taskName) {
    $task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if (-not $task) {
        Write-Host "Task not found: $taskName" -ForegroundColor Yellow
        return
    }
    Start-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    Write-Host "Started task: $taskName"
}

function Stop-Ports($ports) {
    foreach ($port in $ports) {
        Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
            Select-Object -ExpandProperty OwningProcess -Unique |
            Where-Object { $_ } |
            ForEach-Object {
                Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue
                Write-Host "Cleaned port $port process PID $_"
            }
    }
}

function Test-Port($port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port($port, $name, $timeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($timeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-Port $port) {
            Write-Host ("{0,-16} : OK  :{1}" -f $name, $port) -ForegroundColor Green
            return $true
        }
        Start-Sleep -Seconds 2
    }
    Write-Host ("{0,-16} : TIMEOUT  :{1}" -f $name, $port) -ForegroundColor Yellow
    return $false
}

function Start-ServiceIfPresent($serviceName) {
    $svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if (-not $svc) {
        Write-Host "Service not found: $serviceName" -ForegroundColor Yellow
        return
    }
    if ($svc.Status -ne "Running") {
        Start-Service -Name $serviceName
        Write-Host "Started service: $serviceName"
    } else {
        Write-Host "Service already running: $serviceName"
    }
}

Write-Host "DJ Music Radio start all" -ForegroundColor Green
Write-Host "Repo: $repo"

if (-not (Test-Path $repo)) {
    Write-Host "Repo not found: $repo" -ForegroundColor Red
    exit 1
}

Write-Step "Clean old project tasks and ports"
foreach ($taskName in $legacyTasks) { Stop-TaskIfExists $taskName }
foreach ($taskName in $appTasks) { Stop-TaskIfExists $taskName }
if (-not $NoClean) {
    Stop-Ports $projectPorts
}

Write-Step "Start middleware"
Start-ServiceIfPresent "MySQL80"
Start-ServiceIfPresent "RabbitMQ"

foreach ($taskName in $middlewareTasks) {
    Start-TaskIfExists $taskName
}

Wait-Port 3306 "MySQL" 30 | Out-Null
Wait-Port 5672 "RabbitMQ" 30 | Out-Null
Wait-Port 6379 "Redis" 30 | Out-Null
Wait-Port 8848 "Nacos" 60 | Out-Null

Write-Step "Start project services"
foreach ($taskName in $appTasks) {
    Start-TaskIfExists $taskName
    Start-Sleep -Seconds 2
}

Write-Step "Wait for ports"
$checks = @(
    [PSCustomObject]@{ Port = 3000; Name = "Netease API" }
    [PSCustomObject]@{ Port = 8080; Name = "Gateway" }
    [PSCustomObject]@{ Port = 8081; Name = "module-chat" }
    [PSCustomObject]@{ Port = 8082; Name = "module-music" }
    [PSCustomObject]@{ Port = 8083; Name = "module-rec" }
    [PSCustomObject]@{ Port = 8084; Name = "module-user" }
    [PSCustomObject]@{ Port = 5173; Name = "Frontend" }
)

$allOk = $true
foreach ($check in $checks) {
    if (-not (Wait-Port $check.Port $check.Name $TimeoutSeconds)) {
        $allOk = $false
    }
}

Write-Step "URLs"
Write-Host "Frontend:   http://127.0.0.1:5173/"
Write-Host "Gateway:    http://127.0.0.1:8080/"
Write-Host "Nacos:      http://127.0.0.1:8848/nacos/"
Write-Host "RabbitMQ:   http://127.0.0.1:15672/   guest / guest"
Write-Host "NeteaseAPI: http://127.0.0.1:3000/"

Write-Step "Current listening ports"
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in ($projectPorts + $middlewarePorts) } |
    Select-Object LocalAddress,LocalPort,State,OwningProcess |
    Sort-Object LocalPort |
    Format-Table -AutoSize

$exitCode = 0
$finalMessage = "Start completed."
$finalColor = "Green"

if (-not $allOk) {
    $exitCode = 1
    $finalMessage = "Some ports are not ready. Check logs under D:\projects\dj-music-radio\logs."
    $finalColor = "Yellow"
}

Write-Host $finalMessage -ForegroundColor $finalColor
exit $exitCode
