$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $java = Get-Command java -ErrorAction Stop
    $env:JAVA_HOME = Split-Path (Split-Path $java.Source -Parent) -Parent
}
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools\gradle-home'
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Android build/lint failed.' }
} finally { Pop-Location }
