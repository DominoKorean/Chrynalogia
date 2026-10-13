param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\feature-test'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
try {
    Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
    Start-Sleep -Milliseconds 700
    Send-ProbeCommand resume | Out-Null
    $preview=Find-CameraNode (Get-CameraSnapshot) 'camera_preview'
    Tap-CameraNode $preview
    $existing=(Get-CameraSnapshot).State
    if($existing.settingsPopupOpen) { Tap-AppLabel '닫기' }
    $nodes=@(Read-AppNodes)
    if($nodes.Description -contains '메뉴 닫기') { Tap-AppLabel '메뉴 닫기' }
    Tap-AppLabel '그리드 메뉴 열기'
    Tap-AppLabel '그리드 설정 열기'
    $stack=@(Read-AppNodes) | Where-Object {$_.Text -eq '여러 그리드 겹치기' -and $_.Visible} | Select-Object -First 1
    if($stack.Checked) {Tap-AppLabel '여러 그리드 겹치기'}
    Assert-State 'Camera stays focused while settings are open' {param($s) $s.settingsPopupOpen -and $s.foreground -eq 'com.sec.android.app.camera' -and $s.overlayAttached}
    $names=@('황금비','3분할법','중앙 십자선','황금비 격자','황금 나선','대각선 X자','대각선법','황금 삼각형','라바트망','동적 대칭','커스텀 직각 격자')
    $ids=@('golden_ratio','thirds','cross','golden_grid','spiral','x','diagonal','triangle','rabatment','dynamic','custom')
    for($i=0;$i -lt $names.Count;$i++) {
        Tap-AppLabel $names[$i] -Scroll
        $wanted=$ids[$i]
        Assert-State ('Select '+$wanted) {param($s) $s.selectedGrids.Count -eq 1 -and $s.selectedGrids[0] -eq $wanted}
    }
    Tap-AppLabel '여러 그리드 겹치기' -Scroll -Up
    Tap-AppLabel '3분할법' -Scroll
    Tap-AppLabel '중앙 십자선' -Scroll
    Assert-State 'Multiple grids' {param($s) $s.selectedGrids -contains 'thirds' -and $s.selectedGrids -contains 'cross' -and $s.selectedGrids.Count -ge 2}
    Tap-AppLabel '여러 그리드 겹치기' -Scroll -Up
    Assert-State 'Single selection after disabling stacking' {param($s) $s.selectedGrids.Count -eq 1}
    Tap-AppLabel '황금비 격자' -Scroll
    Tap-AppLabel '노란색' -Scroll
    Assert-State 'Yellow preset without outline' {param($s) $s.lineColor -eq -11686 -and -not $s.outline}
    Tap-AppLabel '흰색'
    Assert-State 'White preset' {param($s) $s.lineColor -eq -1 -and -not $s.outline}
    Tap-AppLabel '검은 아웃라인' -Scroll
    Assert-State 'Outline toggle' {param($s) $s.outline}
    Tap-AppLabel '커스텀 파일 비율 유지' -Scroll -Up
    Assert-State 'Preserve aspect preference' {param($s) $s.keepAspect}
    Tap-AppLabel '커스텀 파일 비율 유지'
    Tap-AppLabel '닫기'
    Assert-State 'Camera retained after closing popup' {param($s) -not $s.settingsPopupOpen -and $s.overlayAttached}
    Tap-AppLabel '그리드 회전'
    Assert-State 'Rotate grid' {param($s) $s.gridRotation -eq 1}
    Tap-AppLabel '그리드 좌우 반전'
    Assert-State 'Horizontal flip' {param($s) $s.flipX}
    Tap-AppLabel '그리드 상하 반전'
    Assert-State 'Vertical flip' {param($s) $s.flipY}
    Tap-AppLabel '그리드 숨기기'
    Assert-State 'Hide retains controls' {param($s) $s.gridHidden -and $s.controlsAttached}
    Tap-AppLabel '그리드 표시'
    Assert-State 'Show grid' {param($s) -not $s.gridHidden}
    Save-CameraSnapshot (Get-CameraSnapshot) $folder
    # Restore layout transformations and collapse controls before camera regressions.
    Tap-AppLabel '그리드 좌우 반전';Tap-AppLabel '그리드 상하 반전'
    for($i=0;$i -lt 3;$i++) { Tap-AppLabel '그리드 회전' }
    Tap-AppLabel '메뉴 닫기'
} finally {
    $checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
}
Write-Output "Passed $($checks.Count) feature checks"

