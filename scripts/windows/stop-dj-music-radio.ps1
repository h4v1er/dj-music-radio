param(
    [switch] $KeepMiddleware
)

$ErrorActionPreference = "Continue"
$ProgressPreference = "SilentlyContinue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$projectPorts = @(3000, 5173, 8080, 8081, 8082, 8083, 8084, 9000)
$middlewarePorts = @(6379, 8848)
$projectTasks = @(
    "Codex-DJMusicRadio-frontend",
    "Codex-DJMusicRadio-module-chat",
    "Codex-DJMusicRadio-module-rec",
    "Codex-DJMusicRadio-module-music",
    "Codex-DJMusicRadio-module-user",
    "Codex-DJMusicRadio-gateway",
    "Codex-DJMusicRadio-netease-api",
    "DJMusicRadio-frontend",
    "DJMusicRadio-module-chat"
)
$middlewareTasks = @("CodexNacosStandalone", "CodexRedis")

function Write-Step($message) {
    Write-Host ""
    Write-Host "==> $message" -ForegroundColor Cyan
}

function Stop-TaskIfExists($taskName) {
    $task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    if ($task) {
        Stop-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
        Write-Host "Stopped task: $taskName"
    }
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

function Stop-ServiceIfPresent($serviceName) {
    $svc = Get-Service -Name $serviceName -ErrorAction SilentlyContinue
    if ($svc -and $svc.Status -ne "Stopped") {
        Stop-Service -Name $serviceName -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped service: $serviceName"
    }
}

Write-Host "DJ Music Radio stop all" -ForegroundColor Green

Write-Step "Stop project scheduled tasks"
foreach ($taskName in $projectTasks) { Stop-TaskIfExists $taskName }

Write-Step "Clean project ports"
Stop-Ports $projectPorts

if (-not $KeepMiddleware) {
    Write-Step "Stop middleware"
    foreach ($taskName in $middlewareTasks) { Stop-TaskIfExists $taskName }
    Stop-Ports $middlewarePorts
    Stop-ServiceIfPresent "RabbitMQ"
    Stop-ServiceIfPresent "MySQL80"
} else {
    Write-Step "Keep middleware running"
}

Write-Step "Remaining related listening ports"
$ports = if ($KeepMiddleware) { $projectPorts } else { $projectPorts + $middlewarePorts + @(3306, 5672, 15672) }
Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
    Where-Object { $_.LocalPort -in $ports } |
    Select-Object LocalAddress,LocalPort,State,OwningProcess |
    Sort-Object LocalPort |
    Format-Table -AutoSize

Write-Host "Stop completed." -ForegroundColor Green
