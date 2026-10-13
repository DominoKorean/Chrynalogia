param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\style-test'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
$densityText=(Invoke-ProbeAdb shell wm density) -join "`n"
$effective=[regex]::Matches($densityText,'\d+') | Select-Object -Last 1
$testDensity=[int]$effective.Value/160.0
function Set-AppSlider([string]$Label,[int]$Value,[int]$Min,[int]$Max) {
    for($i=0;$i -lt 18;$i++) {
        $node=@(Read-AppNodes) | Where-Object {$_.Visible -and $_.Description -eq $Label -and $_.Bottom-$_.Top -ge 80} | Select-Object -First 1
        if($node) {
            $padding=16*$testDensity
            $x=[int]($node.Left+$padding+($Value-$Min)/[double]($Max-$Min)*($node.Right-$node.Left-2*$padding))
            Invoke-ProbeAdb shell input tap $x ([int](($node.Top+$node.Bottom)/2))
            Start-Sleep -Milliseconds 400
            return
        }
        Invoke-ProbeAdb shell input swipe 850 1850 850 1250 500
        Start-Sleep -Milliseconds 600
    }
    throw "Slider missing: $Label"
}
try {
    Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
    Tap-CameraNode (Wait-CameraPreview)
    if(@(Read-AppNodes).Description -contains '그리드 메뉴 열기'){Tap-AppLabel '그리드 메뉴 열기'}
    Tap-AppLabel '그리드 설정 열기'
    Tap-AppLabel '커스텀 직각 격자' -Scroll -EnsureChecked
    Set-AppSlider '세로 칸 수' 5 1 16
    Set-AppSlider '가로 칸 수' 7 1 16
    Assert-State 'Custom grid cell counts' {param($s) $s.columns -eq 5 -and $s.rows -eq 7 -and $s.selectedGrids[0] -eq 'custom'}
    Tap-AppLabel '노란색' -Scroll
    $field=$null
    for($attempt=0;$attempt -lt 4;$attempt++) {
        $field=@(Read-AppNodes) | Where-Object {$_.Description -eq '라인 색상 HEX' -and $_.Visible -and $_.Bottom-$_.Top -ge 80} | Select-Object -First 1
        if($field){break}
        Invoke-ProbeAdb shell input swipe 850 1800 850 1450 500
        Start-Sleep -Milliseconds 600
    }
    if(-not $field){throw 'Color editor is not visible'}
    Tap-CameraNode $field
    Assert-State 'Keyboard does not close camera settings' {param($s) $s.settingsPopupOpen -and $s.overlayAttached}
    Invoke-ProbeAdb shell input keyevent KEYCODE_MOVE_END KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL
    Invoke-ProbeAdb shell input text 3EA6FF
    Invoke-ProbeAdb shell input keyevent KEYCODE_ENTER
    Start-Sleep -Milliseconds 600
    Assert-State 'HEX input applies inside camera popup' {param($s) ($s.lineColor -band 0xffffff) -eq 0x3ea6ff -and $s.settingsPopupOpen -and $s.foreground -eq 'com.sec.android.app.camera'}
    Tap-AppLabel '검은 아웃라인' -Scroll
    Set-AppSlider '불투명도 (%)' 40 0 100
    Set-AppSlider '선 두께 (0.1dp)' 24 3 60
    Assert-State 'Line opacity and thickness sliders' {param($s) [Math]::Abs($s.lineOpacity-0.4) -lt 0.025 -and [Math]::Abs($s.lineThickness-2.4) -lt 0.11}
    Tap-AppLabel '닫기'
    Save-CameraSnapshot (Get-CameraSnapshot) $folder
    Tap-AppLabel '그리드 설정 열기'
    Set-AppSlider '세로 칸 수' 3 1 16
    Set-AppSlider '가로 칸 수' 3 1 16
    Tap-AppLabel '흰색' -Scroll
    Set-AppSlider '불투명도 (%)' 90 0 100
    Set-AppSlider '선 두께 (0.1dp)' 12 3 60
    Tap-AppLabel '황금비 격자' -Scroll -Up -EnsureChecked
    Tap-AppLabel '닫기'
    Tap-AppLabel '메뉴 닫기'
    Assert-State 'Restore default line and grid settings' {param($s) $s.selectedGrids[0] -eq 'golden_grid' -and -not $s.outline -and $s.lineColor -eq -1 -and [Math]::Abs($s.lineThickness-1.2) -lt 0.11}
} finally {$checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')}
Write-Output "Passed $($checks.Count) style checks"
