$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $taskRoot 'backend')
try { & .\.venv\Scripts\python.exe -m unittest discover -s tests -v; if ($LASTEXITCODE -ne 0) { throw 'Backend tests failed.' } }
finally { Pop-Location }
