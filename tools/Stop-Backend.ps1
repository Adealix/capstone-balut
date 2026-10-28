param([int]$Port=8787)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskListener=Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if (-not $taskListener) { Write-Output 'Backend is already stopped.'; exit }
$taskProcess=Get-CimInstance Win32_Process -Filter "ProcessId=$($taskListener.OwningProcess)"
$taskParent=Get-CimInstance Win32_Process -Filter "ProcessId=$($taskProcess.ParentProcessId)"
$taskExpected=Join-Path $taskRoot 'backend\.venv\Scripts\python.exe'
if ($taskParent.CommandLine -notlike "*$taskExpected*uvicorn app.main:app*" -and $taskProcess.CommandLine -notlike "*$taskExpected*uvicorn app.main:app*") { throw 'Port belongs to a process not verified as this project backend. Nothing stopped.' }
Stop-Process -Id $taskProcess.ProcessId
if ($taskParent.CommandLine -like "*$taskExpected*uvicorn app.main:app*") { Stop-Process -Id $taskParent.ProcessId -ErrorAction SilentlyContinue }
Write-Output "Stopped this project's backend on port $Port."
