param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\redesign-checks'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
function Save-RedesignScreen([string]$Name) {
 Invoke-ProbeAdb shell screencap -p /data/local/tmp/chrynalogia-redesign.png
 Invoke-ProbeAdb pull /data/local/tmp/chrynalogia-redesign.png (Join-Path $folder ($Name+'.png')) | Out-Null
}
function Tap-Choice([string]$Label) {
 Invoke-ProbeAdb shell uiautomator dump /data/local/tmp/chrynalogia-redesign.xml | Out-Null
 [xml]$dialog=(Invoke-ProbeAdb shell cat /data/local/tmp/chrynalogia-redesign.xml) -join ''
 $choice=$dialog.SelectNodes('//node') | Where-Object {$_.text -eq $Label -and $_.'resource-id' -eq 'android:id/text1'} | Select-Object -First 1
 if(-not $choice){throw "Dialog choice missing: $Label"}
 if($choice.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){throw 'Invalid choice bounds'}
 Invoke-ProbeAdb shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2))
 Start-Sleep -Milliseconds 650
}
function Set-Editor([string]$Description,[string]$Value,[switch]$Scroll) {
 Tap-AppLabel $Description -Scroll:$Scroll
 Invoke-ProbeAdb shell input keyevent KEYCODE_MOVE_END KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL
 Invoke-ProbeAdb shell input text $Value
 Invoke-ProbeAdb shell input keyevent KEYCODE_ENTER
 Start-Sleep -Milliseconds 350
}
Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
for($attempt=0;$attempt -lt 12;$attempt++) {
    $original=(Get-CameraSnapshot).State
    if($original.connected -and $null -ne $original.lineThickness){break}
    Start-Sleep -Milliseconds 300
}
if(-not $original.connected -or $null -eq $original.lineThickness){throw 'Wait for the accessibility service before changing settings'}
$original | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $folder 'original-state.json') -Encoding utf8
try {
 Tap-AppLabel '그리드'
 $nodes=@(Read-AppNodes)
 if($nodes.Description -contains '황금비'){throw 'Retired golden ratio is still listed'}
 $left=$nodes | Where-Object {$_.Description -eq '3분할법' -and $_.Clickable} | Select-Object -First 1
 $right=$nodes | Where-Object {$_.Description -eq '중앙 십자선' -and $_.Clickable} | Select-Object -First 1
 if(-not $left -or -not $right -or $left.Top -ne $right.Top -or $left.Right -ge $right.Left){throw 'Two-column grid cards do not align'}
 Assert-State 'Two-column cards and retired item removed' {param($s) $s.connected}
 Save-RedesignScreen 'grid'
 Tap-AppLabel '라인'
 Save-RedesignScreen 'line'
 Tap-AppLabel '검은색'
 Tap-AppLabel '자동 대비 아웃라인' -Scroll
 Assert-State 'Black line uses a white contrast outline' {param($s) $s.lineColor -eq -16777216 -and $s.outline -and $s.outlineColor -eq -1}
 Tap-AppLabel '라인'
 Tap-AppLabel '흰색'
 Assert-State 'Color preset turns outline off' {param($s) $s.lineColor -eq -1 -and -not $s.outline}
 Tap-AppLabel '자동 대비 아웃라인' -Scroll
 Assert-State 'White line uses a black contrast outline' {param($s) $s.outline -and $s.outlineColor -eq -16777216}
 Tap-AppLabel '라인'
 Set-Editor '라인 색상 HEX' '172554'
 Assert-State 'HEX input accepts a dark custom color' {param($s) ($s.lineColor -band 0xffffff) -eq 0x172554 -and $s.outlineColor -eq -1}
 Tap-AppLabel '라인'
 Tap-AppLabel '흰색'
 Tap-AppLabel '설정'
 Save-RedesignScreen 'settings'
 Tap-AppLabel '앱 테마'
 Tap-Choice '밝게'
 Assert-State 'App light theme saved' {param($s) $s.appTheme -eq 'light'}
 Save-RedesignScreen 'settings-light'
 Tap-AppLabel '앱 테마'
 Tap-Choice '어둡게'
 Assert-State 'App dark theme saved' {param($s) $s.appTheme -eq 'dark'}
 Tap-AppLabel '언어'
 Tap-Choice 'English'
 Assert-State 'English per-app language saved' {param($s) $s.appLanguage -eq 'en'}
 if(-not (@(Read-AppNodes).Description -contains 'Language')){throw 'English UI is not localized'}
 Save-RedesignScreen 'settings-english'
 Tap-AppLabel 'Language'
 Tap-Choice '한국어'
 Assert-State 'Korean per-app language saved' {param($s) $s.appLanguage -eq 'ko'}
 Tap-AppLabel '언어'
 Tap-Choice '시스템 기본값'
 Assert-State 'System language restored' {param($s) $s.appLanguage -eq ''}
 Tap-AppLabel '앱 테마'
 Tap-Choice '시스템 기본값'
 Assert-State 'System theme restored' {param($s) $s.appTheme -eq 'system'}
 Tap-AppLabel '그리드'
 Tap-AppLabel '커스텀 직각 격자' -Scroll -EnsureChecked
 Set-Editor '세로 칸 수' '5' -Scroll
 Invoke-ProbeAdb shell input keyevent KEYCODE_BACK
 Set-Editor '가로 칸 수' '7' -Scroll
 Invoke-ProbeAdb shell input keyevent KEYCODE_BACK
 Assert-State 'Selected custom card expands editable counts' {param($s) $s.selectedGrids[0] -eq 'custom' -and $s.columns -eq 5 -and $s.rows -eq 7}
 Set-Editor '세로 칸 수' '3' -Scroll
 Invoke-ProbeAdb shell input keyevent KEYCODE_BACK
 Set-Editor '가로 칸 수' '3' -Scroll
 Invoke-ProbeAdb shell input keyevent KEYCODE_BACK
 Tap-AppLabel '그리드'
 Tap-AppLabel '3분할법' -EnsureChecked
 Tap-AppLabel '삼성 카메라 열기'
 $preview=Wait-CameraPreview
 Assert-State 'Camera overlay remains attached with new UI' {param($s) $s.overlayAttached -and $s.controlsAttached}
 $bounds=(Get-CameraSnapshot).State.bounds
 Tap-AppLabel '그리드 메뉴 열기'
 $items=@(Read-AppNodes) | Where-Object {$_.Clickable -and $_.Visible -and $_.Description -in @('그리드 90° 회전','좌우 반전','상하 반전','그리드 설정 열기')}
 if($items.Count -ne 4 -or @($items.Left | Select-Object -Unique).Count -ne 1){throw 'Camera controls are not a vertical menu'}
 Save-RedesignScreen 'camera-menu'
 Tap-AppLabel '그리드 숨기기'
 Assert-State 'New camera menu toggles grid visibility' {param($s) $s.gridHidden}
 Tap-AppLabel '그리드 표시'
 Tap-AppLabel '그리드 설정 열기'
 Assert-State 'New tabbed camera popup holds preview bounds' {param($s) $s.settingsPopupOpen -and $s.overlayAttached -and $s.bounds -eq $bounds}
 Tap-AppLabel '라인'
 Set-Editor '라인 색상 HEX' '3EA6FF' -Scroll
 Assert-State 'Keyboard and custom color retain the camera popup' {param($s) $s.settingsPopupOpen -and $s.overlayAttached -and ($s.lineColor -band 0xffffff) -eq 0x3ea6ff -and $s.bounds -eq $bounds}
 Save-RedesignScreen 'camera-popup-line'
 Tap-AppLabel '라인'
 Tap-AppLabel '흰색'
 Tap-AppLabel '닫기'
 Tap-AppLabel '메뉴 닫기'
 Assert-State 'Camera controls collapse after popup closes' {param($s) $s.overlayAttached -and -not $s.settingsPopupOpen}
} finally {
 $checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'ui-checks.json')
 Invoke-ProbeAdb shell rm -f /data/local/tmp/chrynalogia-redesign.png /data/local/tmp/chrynalogia-redesign.xml
}
Write-Output "Passed $($checks.Count) redesign checks"
