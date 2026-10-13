param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\v12-checks'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
function Check-Ui([string]$Name,[bool]$Value){if(-not $Value){throw "Failed: $Name"};$checks.Add([pscustomobject]@{Name=$Name;Passed=$true})}
function Save-Screen([string]$Name){Invoke-ProbeAdb shell screencap -p /data/local/tmp/chry-v12.png;Invoke-ProbeAdb pull /data/local/tmp/chry-v12.png (Join-Path $folder ($Name+'.png')) | Out-Null}
function Set-Hex([string]$Value){Tap-AppLabel '라인 색상 HEX';Invoke-ProbeAdb shell input keyevent KEYCODE_MOVE_END KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL KEYCODE_DEL;Invoke-ProbeAdb shell input text $Value;Invoke-ProbeAdb shell input keyevent KEYCODE_ENTER;Start-Sleep -Milliseconds 350;if((Invoke-ProbeAdb shell dumpsys input_method | Select-String 'mInputShown=true')){Invoke-ProbeAdb shell input keyevent KEYCODE_BACK}}
function Set-Slider([string]$Label,[int]$Value,[int]$Min,[int]$Max){
 for($i=0;$i -lt 14;$i++){
  $node=@(Read-AppNodes)|Where-Object {$_.Description -eq $Label -and $_.Class -match '(SeekBar|KitSlider)$' -and $_.Visible -and $_.Bottom -lt 2020 -and $_.Bottom-$_.Top -ge 100}|Select-Object -First 1
  if($node){Start-Sleep -Milliseconds 500;$padding=12*2.625;$x=[int]($node.Left+$padding+($Value-$Min)/[double]($Max-$Min)*($node.Right-$node.Left-2*$padding));Invoke-ProbeAdb shell input tap $x ([int](($node.Top+$node.Bottom)/2));Start-Sleep -Milliseconds 300;return}
  Invoke-ProbeAdb shell input swipe 850 1850 850 1250 400;Start-Sleep -Milliseconds 700
 };throw "Missing slider: $Label"
}
Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
Start-Sleep -Milliseconds 900
$original=(Get-CameraSnapshot).State
if(-not $original.connected){throw 'Accessibility must be connected before this test'}
$original|ConvertTo-Json -Depth 5|Set-Content (Join-Path $folder 'test-baseline.json')
$names=@{thirds='3분할법';cross='중앙 십자선';golden_grid='황금비 격자';spiral='황금 나선';x='대각선 X자';diagonal='대각선법';triangle='황금 삼각형';rabatment='라바트망';dynamic='동적 대칭';custom='커스텀 직각 격자'}
$restoring=$false
try{
 Tap-AppLabel '그리드';Start-Sleep -Milliseconds 400
 $nodes=@(Read-AppNodes)
 Check-Ui 'Multi-selection description is inside the screen' ([bool]($nodes|Where-Object {$_.Visible -and $_.Text -eq '여러 그리드를 동시에 표시합니다.'}))
 Check-Ui 'Layout panel was removed' (-not ($nodes.Text -contains '배치'))
 Tap-AppLabel '다중 선택';Tap-AppLabel '3분할법' -EnsureChecked
 Assert-State 'Kit switch enables overlapping grids' {param($s) $s.selectedGrids -contains 'thirds'}
 Save-Screen 'grid-switch'
 Tap-AppLabel '다중 선택';Tap-AppLabel '커스텀 직각 격자' -Scroll
 # Both count rows are already measured before selection. Toggle off/on at the same scroll.
 Tap-AppLabel '커스텀 직각 격자'
 $before=@(Read-AppNodes)|Where-Object {$_.Text -eq '커스텀 파일'}|Select-Object -First 1
 Tap-AppLabel '커스텀 직각 격자'
 $after=@(Read-AppNodes)|Where-Object {$_.Text -eq '커스텀 파일'}|Select-Object -First 1
 Check-Ui 'Custom count expansion leaves the next panel fixed' ($null -ne $before -and $null -ne $after -and $before.Top -eq $after.Top)
 Save-Screen 'grid-custom'
 Tap-AppLabel '라인';Start-Sleep -Milliseconds 400
 $nodes=@(Read-AppNodes);$first=$nodes|Where-Object {$_.Text -eq '노란색'}|Select-Object -First 1
 Check-Ui 'Line tab starts at its first function' ($first.Top -lt 250)
 $hex=$nodes|Where-Object {$_.Class -eq 'android.widget.EditText' -and $_.Description -eq '라인 색상 HEX'}|Select-Object -First 1
 Check-Ui 'HEX editor displays exactly six digits' ($hex.Text -match '^[0-9A-F]{6}$')
 $custom=$nodes|Where-Object {$_.Text -eq '커스텀' -and $_.Class -eq 'android.widget.RadioButton'}|Select-Object -First 1
 Check-Ui 'Custom color is a selectable fourth radio choice' ($null -ne $custom -and $custom.Clickable)
 Set-Hex '172554'
 Assert-State 'Six-digit HEX selects and applies the custom color' {param($s) ($s.lineColor -band 0xffffff) -eq 0x172554}
 Save-Screen 'line-custom'
 Set-Hex 'FFFFFF';Tap-AppLabel '라인';Tap-AppLabel '흰색'
 Set-Slider '불투명도' 40 0 100;Set-Slider '선 두께' 24 3 60
 Assert-State 'Kit slider range touch updates both values' {param($s) [Math]::Abs($s.lineOpacity-0.4) -lt 0.04 -and [Math]::Abs($s.lineThickness-2.4) -lt 0.11}
 Save-Screen 'line-sliders-blur'
 Set-Slider '불투명도' ([int][Math]::Round($original.lineOpacity*100)) 0 100;Set-Slider '선 두께' ([int][Math]::Round($original.lineThickness*10)) 3 60
 Tap-AppLabel '설정';Start-Sleep -Milliseconds 400
 $nodes=@(Read-AppNodes);$language=$nodes|Where-Object {$_.Description -eq '언어' -and $_.Clickable}|Select-Object -First 1
 Check-Ui 'Short Settings tab also starts at its first function' ($language.Top -lt 250)
 Check-Ui 'Settings group headings are removed' (-not (@('앱 기본 설정','실행 방식','권한')|Where-Object {$nodes.Text -contains $_}))
 Save-Screen 'settings'
 Tap-AppLabel '그리드';Tap-AppLabel '라바트망' -Scroll -EnsureChecked
 Tap-AppLabel '그리드';Tap-AppLabel '삼성 카메라 열기'
 Wait-CameraPreview | Out-Null
 $closed=@(Read-AppNodes)|Where-Object {$_.Description -eq '그리드 메뉴 열기' -and $_.Clickable}|Select-Object -First 1
 Tap-AppLabel '그리드 메뉴 열기'
 $nodes=@(Read-AppNodes);$opened=$nodes|Where-Object {$_.Description -eq '메뉴 닫기' -and $_.Clickable}|Select-Object -First 1
 Check-Ui 'Camera app-logo opener stays anchored when expanded' ($closed.Left -eq $opened.Left -and $closed.Top -eq $opened.Top -and $closed.Right -eq $opened.Right -and $closed.Bottom -eq $opened.Bottom)
 $firstAction=$nodes|Where-Object {$_.Description -in @('그리드 숨기기','그리드 표시') -and $_.Clickable}|Select-Object -First 1
 Check-Ui 'Grouped camera panel opens below the app logo' ($firstAction.Top -ge $opened.Bottom)
 Save-Screen 'camera-menu'
 Tap-AppLabel '그리드 90° 회전'
 Assert-State 'Camera rotate button advances rabatment quarter turn' {param($s) $s.gridRotation -eq (($original.gridRotation+1)%4)}
 Save-Screen 'camera-rabatment-rotated'
 $bounds=(Get-CameraSnapshot).State.bounds
 Tap-AppLabel '그리드 설정 열기';Tap-AppLabel '라인'
 Assert-State 'Camera settings and tab navigation preserve preview bounds' {param($s) $s.settingsPopupOpen -and $s.overlayAttached -and $s.bounds -eq $bounds}
 Save-Screen 'camera-popup-line'
 Tap-AppLabel '닫기'
 while((Get-CameraSnapshot).State.gridRotation -ne $original.gridRotation){Tap-AppLabel '그리드 90° 회전'}
 Tap-AppLabel '메뉴 닫기'
}finally{
 $checks|ConvertTo-Json -Depth 7|Set-Content -Encoding utf8 (Join-Path $folder 'ui-checks.json')
}
Write-Output "Passed $($checks.Count) One UI checks"
