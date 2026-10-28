$ErrorActionPreference='Stop'
$taskRoot=Split-Path $PSScriptRoot -Parent
$taskFrontend=Join-Path $taskRoot 'frontend'
$env:GRADLE_USER_HOME=Join-Path $PSScriptRoot 'gradle-cache'
if (Test-Path -LiteralPath 'D:\Android Studio\jbr') { $env:JAVA_HOME='D:\Android Studio\jbr' }
Push-Location $taskFrontend
try {
    & .\gradlew.bat --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
    if ($LASTEXITCODE -ne 0) { throw 'Android build/checks failed. Inspect the Gradle output.' }
} finally { Pop-Location }
