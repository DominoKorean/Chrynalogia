param([string]$Serial)
. "$PSScriptRoot\Camera-Ui.ps1" -Serial $Serial
Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
Start-Sleep -Milliseconds 700
for ($attempt=0; $attempt -lt 10; $attempt++) {
    $snapshot=Get-CameraSnapshot
    $zoom=Find-CameraNode $snapshot 'zoom_lens_text_button_main' '^줌 1,|^Zoom 1,'
    if ($zoom) { break }
    $close=Find-CameraNode $snapshot 'quick_setting_close_button_image'
    if ($close) { Tap-CameraNode $close }
    else { Tap-CameraNode (Find-CameraNode $snapshot 'camera_preview') }
}
if (-not $zoom) { throw 'Normal 1x zoom button not found.' }
$before=$snapshot.State
if (-not $before.overlayAttached -or $before.locator -ne 'camera-preview-view') { throw 'Use the Samsung photo preview for this test.' }
$x=[int](($zoom.Left+$zoom.Right)/2); $y=[int](($zoom.Top+$zoom.Bottom)/2)
Invoke-ProbeAdb shell input swipe "$x" "$y" "$x" "$y" 700
Start-Sleep -Milliseconds 300
$afterSnapshot=Get-CameraSnapshot
$after=$afterSnapshot.State
$expanded=[bool](Find-CameraNode $afterSnapshot 'zoom_text_view')
$passed=$expanded -and $after.overlayAttached -and $before.bounds -eq $after.bounds
$folder=Join-Path $script:ProjectRoot 'artifacts\zoom-test'
Save-CameraSnapshot $afterSnapshot $folder
[pscustomobject]@{passed=$passed; zoomUiExpanded=$expanded; before=$before; after=$after} | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
if (-not $passed) { throw "Zoom regression failed: expanded=$expanded; before=$($before.bounds); after=$($after.bounds)" }
Write-Output "PASS: Zoom slider expanded; grid bounds unchanged: $($after.bounds)"
Write-Output 'ADB injected input checks UI/layout behavior; it does not replace real finger touch validation.'
