param([string]$Serial)
. "$PSScriptRoot\Camera-Ui.ps1" -Serial $Serial
Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
Start-Sleep -Milliseconds 700
for ($attempt=0; $attempt -lt 10; $attempt++) {
    $snapshot=Get-CameraSnapshot
    $entry=Find-CameraNode $snapshot 'quick_setting_entry_button'
    if ($entry) { break }
    $close=Find-CameraNode $snapshot 'quick_setting_close_button_image'
    if ($close) { Tap-CameraNode $close }
    else { Tap-CameraNode (Find-CameraNode $snapshot 'camera_preview') }
}
if (-not $entry) { throw 'Quick controls entry button not found.' }
$before=$snapshot.State
if (-not $before.overlayAttached -or $before.locator -ne 'camera-preview-view') { throw 'Use the Samsung photo preview for this test.' }
Tap-CameraNode $entry
$afterSnapshot=Get-CameraSnapshot
$after=$afterSnapshot.State
$expanded=[bool](Find-CameraNode $afterSnapshot 'quick_setting_main_list')
$passed=$expanded -and $after.overlayAttached -and $before.bounds -eq $after.bounds
$folder=Join-Path $script:ProjectRoot 'artifacts\quick-controls-test'
Save-CameraSnapshot $afterSnapshot $folder
[pscustomobject]@{passed=$passed; panelExpanded=$expanded; before=$before; after=$after} | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
if (-not $passed) { throw "Quick controls regression failed: expanded=$expanded; before=$($before.bounds); after=$($after.bounds)" }
Write-Output "PASS: Quick controls expanded; grid remains attached at $($after.bounds)"
# Leave the panel open for visual verification. No camera settings are changed.
