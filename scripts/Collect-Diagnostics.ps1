param([string]$Serial)
. "$PSScriptRoot\Device.ps1" -Serial $Serial
$folder = Join-Path $script:ProjectRoot ('artifacts\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $folder | Out-Null
Send-ProbeCommand capture | Set-Content -Encoding utf8 (Join-Path $folder 'capture-result.txt')
foreach ($name in @('probe-status.json', 'probe-events.jsonl', 'camera-ui.txt', 'app-ui.txt')) {
    # run-as limits file access to this debuggable app's private data.
    $content = & $script:AdbPath @script:DeviceArguments shell run-as dev.chrynalogia.probe cat "files/$name" 2>&1
    if ($LASTEXITCODE -ne 0) { $content = "Unavailable ($name): $content" }
    $content | Set-Content -Encoding utf8 (Join-Path $folder $name)
}
Invoke-ProbeAdb shell getprop ro.product.model | Set-Content (Join-Path $folder 'device.txt')
Invoke-ProbeAdb shell getprop ro.build.version.release | Add-Content (Join-Path $folder 'device.txt')
Invoke-ProbeAdb shell getprop ro.build.version.sdk | Add-Content (Join-Path $folder 'device.txt')
Invoke-ProbeAdb shell getprop ro.build.version.oneui | Add-Content (Join-Path $folder 'device.txt')
Invoke-ProbeAdb logcat -d -v threadtime 'CameraGridProbe:I' 'InputDispatcher:W' 'AndroidRuntime:E' '*:S' |
    Set-Content -Encoding utf8 (Join-Path $folder 'logcat.txt')
Invoke-ProbeAdb shell dumpsys window windows | Set-Content -Encoding utf8 (Join-Path $folder 'windows.txt')
Invoke-ProbeAdb shell dumpsys accessibility | Set-Content -Encoding utf8 (Join-Path $folder 'accessibility.txt')
# adb screencap + pull preserves PNG bytes even in Windows PowerShell 5.1.
Invoke-ProbeAdb shell screencap -p /data/local/tmp/chrynalogia-probe.png
Invoke-ProbeAdb pull /data/local/tmp/chrynalogia-probe.png (Join-Path $folder 'screen.png')
Invoke-ProbeAdb shell rm /data/local/tmp/chrynalogia-probe.png
Write-Output "Diagnostics saved: $folder"
