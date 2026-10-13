param([string]$Serial)
. "$PSScriptRoot\Device.ps1" -Serial $Serial
$apk = Join-Path $script:ProjectRoot 'app\build\outputs\apk\debug\app-debug.apk'
if (-not (Test-Path $apk)) { throw 'Debug APK is missing. Build first.' }
Invoke-ProbeAdb install -r $apk
Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity
Write-Output 'In the Settings tab, grant overlay permission and enable Chrynalogia in Accessibility. Then tap Open Samsung Camera.'
