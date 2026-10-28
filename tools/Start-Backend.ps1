param([ValidateSet('simulation','real')][string]$Mode='simulation',[int]$Port=8787)
$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskBackend=Join-Path $taskRoot 'backend'
$taskPython=Join-Path $taskBackend '.venv\Scripts\python.exe'
if (-not (Test-Path -LiteralPath $taskPython)) {
    python -m venv (Join-Path $taskBackend '.venv')
    & $taskPython -m pip install -r (Join-Path $taskBackend 'requirements.txt')
    if ($LASTEXITCODE -ne 0) { throw 'Backend installation failed.' }
}
$taskHealth=$null
try { $taskHealth=Invoke-RestMethod -Uri "http://127.0.0.1:$Port/health" -TimeoutSec 2 } catch {}
if ($taskHealth) {
    if ($taskHealth.mode -ne $Mode) { throw 'This port already serves a different operating mode.' }
    Write-Output "Backend already running on http://127.0.0.1:$Port ($Mode)."
    exit
}
$env:BALUT_MODE=$Mode
$env:BALUT_DATABASE=Join-Path $taskBackend "data\$Mode.sqlite3"
New-Item -ItemType Directory -Path (Join-Path $taskRoot 'evidence') -Force | Out-Null
# A new hidden console detaches from caller pipes. Logging goes directly to a file.
$taskService=Start-Process -FilePath $taskPython -ArgumentList "-m uvicorn app.main:app --host 127.0.0.1 --port $Port --no-access-log --log-config logging.json" -WorkingDirectory $taskBackend -WindowStyle Hidden -PassThru
$taskService.Id | Set-Content -LiteralPath (Join-Path $taskRoot 'evidence\backend-launcher.pid')
Write-Output "Started $Mode API at http://127.0.0.1:$Port. Emulator address: http://10.0.2.2:$Port"
