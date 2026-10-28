param(
    [ValidatePattern('^[A-Za-z0-9_-]{1,30}$')][string]$Session='tutorial',
    [ValidateRange(1025,65535)][int]$Port=8789
)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskBackend=Join-Path $taskRoot 'backend'
$taskPython=Join-Path $taskBackend '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $taskPython)) { throw 'Install the backend environment using the project setup guide first.' }
if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "Port $Port is already in use. Reuse the existing practice server, or stop it with Ctrl+C in its own terminal before restarting." }
$taskOldMode=$env:BALUT_MODE
$taskOldDatabase=$env:BALUT_DATABASE
$env:BALUT_MODE='simulation'
$env:BALUT_DATABASE=Join-Path $taskBackend "data\manual-$Session.sqlite3"
Write-Output "Practice database: $env:BALUT_DATABASE"
Write-Output "Emulator address: http://10.0.2.2:$Port"
Write-Output 'Leave this window open. Press Ctrl+C here to stop only this practice server.'
Push-Location $taskBackend
try { & $taskPython -m uvicorn app.main:app --host 127.0.0.1 --port $Port --no-access-log }
finally {
    Pop-Location
    $env:BALUT_MODE=$taskOldMode
    $env:BALUT_DATABASE=$taskOldDatabase
}
