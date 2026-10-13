param([string]$Serial)
. "$PSScriptRoot\Device.ps1" -Serial $Serial
$folder = Join-Path $script:ProjectRoot ('artifacts\compare-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force $folder | Out-Null
try {
    foreach ($mode in @('pause', 'resume')) {
        Send-ProbeCommand $mode | Set-Content (Join-Path $folder "$mode-result.txt")
        Start-Sleep -Milliseconds 500
        Invoke-ProbeAdb shell screencap -p /data/local/tmp/chrynalogia-probe.png
        Invoke-ProbeAdb pull /data/local/tmp/chrynalogia-probe.png (Join-Path $folder "$mode.png")
    }
} finally {
    # This test is intended to end with the overlay enabled.
    Send-ProbeCommand resume
    Invoke-ProbeAdb shell rm -f /data/local/tmp/chrynalogia-probe.png
}
Write-Output "Overlay comparison saved: $folder"
