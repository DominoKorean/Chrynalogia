param([string]$Serial)
. "$PSScriptRoot\Camera-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\mode-test'
New-Item -ItemType Directory -Force $folder | Out-Null
$results=[System.Collections.Generic.List[object]]::new()
Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
Start-Sleep -Milliseconds 600
$entries=@(
    @{Name='photo'; Label='사진'}, @{Name='video'; Label='동영상'},
    @{Name='portrait'; Label='인물 사진'}, @{Name='portrait-video'; Label='인물 동영상'},
    @{Name='pro'; Label='프로'}, @{Name='pro-video'; Label='프로 동영상'},
    @{Name='night'; Label='야간'}, @{Name='food'; Label='음식'},
    @{Name='panorama'; Label='파노라마'}, @{Name='super-slow-motion'; Label='슈퍼 슬로우 모션'},
    @{Name='slow-motion'; Label='슬로우 모션'}, @{Name='hyperlapse'; Label='하이퍼랩스'}
)
try {
    foreach ($entry in $entries) {
        $menu=Open-CameraMoreMenu
        if ($menu.State.overlayAttached) { throw 'The grid must hide on the More mode picker.' }
        if ($entry.Label -in @('사진','동영상')) {
            $node=Find-CameraNode $menu 'shooting_mode_item_button_full' ('^'+[regex]::Escape($entry.Label)+'$')
        } else {
            $node=Find-CameraNode $menu 'more_grid_list_text' ('^'+[regex]::Escape($entry.Label)+'$')
        }
        Tap-CameraNode $node
        $passed=$false
        for ($attempt=0; $attempt -lt 12; $attempt++) {
            $snapshot=Get-CameraSnapshot
            $preview=Find-CameraNode $snapshot 'camera_preview'
            if ($preview) {
                $bounds="[$($preview.Left),$($preview.Top)][$($preview.Right),$($preview.Bottom)]"
                $passed=$snapshot.State.overlayAttached -and $snapshot.State.locator -eq 'camera-preview-view' -and $snapshot.State.bounds -eq $bounds -and $snapshot.State.cameraMode -eq $entry.Label
                if ($passed) { break }
            }
            Start-Sleep -Milliseconds 250
        }
        Save-CameraSnapshot $snapshot (Join-Path $folder $entry.Name)
        $results.Add([pscustomobject]@{name=$entry.Name; label=$entry.Label; passed=$passed; bounds=$bounds; state=$snapshot.State})
        Write-Output "$($entry.Label): passed=$passed; mode=$($snapshot.State.cameraMode); bounds=$($snapshot.State.bounds); reason=$($snapshot.State.reason)"
    }
} finally {
    try {
        $menu=Open-CameraMoreMenu
        Tap-CameraNode (Find-CameraNode $menu 'shooting_mode_item_button_full' '^사진$')
    } catch { Write-Warning "Could not restore photo mode: $_" }
    $results | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
}
if ($results | Where-Object { -not $_.passed }) { throw 'One or more mode preview tests failed. Inspect artifacts/mode-test.' }
Write-Output 'PASS: All 12 native camera mode previews. No photos/videos were captured.'
