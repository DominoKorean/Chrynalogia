param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts/v13-checks'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
function Check([string]$Name,[bool]$Value){if(!$Value){Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder 'ui-failure');throw "Failed: $Name"};$checks.Add([pscustomobject]@{Name=$Name;Passed=$true})}
function Choice([string]$Name){$item=Read-AppNodes | Where-Object {$_.Visible -and $_.Text -eq $Name -and $_.Class -eq 'android.widget.CheckedTextView'} | Select-Object -First 1;Tap-CameraNode $item}
function Shot([string]$Name){Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder $Name)}
function Settings-Tab {
 for($i=0;$i -lt 5;$i++){
  Tap-AppLabel '설정'
  if(Read-AppNodes | Where-Object {$_.Visible -and $_.Description -eq '설정' -and $_.Selected}){return}
  Start-Sleep -Milliseconds 250
 }
 throw 'Settings tab did not become selected'
}
function Rect-Parts([string]$Value){if($Value -notmatch '\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]'){throw "Invalid rectangle $Value"};return @([int]$Matches[1],[int]$Matches[2],[int]$Matches[3],[int]$Matches[4])}
$night=((Invoke-ProbeAdb shell cmd uimode night) -join '') -replace '^Night mode:\s*',''
Start-ProbeTest
Invoke-ProbeTest {
 Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Start-Sleep -Milliseconds 500
 Shot 'app-initial-light'
 Tap-AppLabel '그리드';Tap-AppLabel '황금 나선' -Scroll;Tap-AppLabel '삼성 카메라 열기' -Scroll -Up
 $base=(Get-CameraSnapshot).State
 Tap-AppLabel '그리드 메뉴 열기'
 $opened=(Get-CameraSnapshot).State
 $anchor=Rect-Parts $opened.openerBounds
 $native=Get-CameraSnapshot
 $right=($native.Nodes | Where-Object {$_.Visible -and $_.Id.EndsWith('/quick_setting_button_main')} | Measure-Object Right -Maximum).Maximum
 $frame=Rect-Parts $opened.contentWindowBounds
 Check 'Opener mirrors native right inset' ($anchor[0]-$frame[0] -eq $frame[2]-$right)
 foreach($label in @('그리드 90° 회전','좌우 반전','상하 반전')){Tap-AppLabel $label;$next=(Get-CameraSnapshot).State;Check "$label keeps menu registered" ($next.menuExpanded -and $next.menuBuildCount -eq $opened.menuBuildCount)}
 foreach($quarter in @(1,3,0,2)){
  Send-ProbeCommand ui-orientation @('--ei','quarter',[string]$quarter) | Out-Null
  $next=(Get-CameraSnapshot).State
  Check "Direction $quarter retains menu and manual geometry" ($next.menuExpanded -and $next.menuBuildCount -eq $opened.menuBuildCount -and $next.gridRotation -eq 1 -and $next.flipX -and $next.flipY)
  if($quarter % 2 -eq 1){$menu=Rect-Parts $next.menuBounds;$preview=Rect-Parts $next.bounds;Check "Direction $quarter keeps menu above preview" ($menu[3] -le $preview[1]);Check "Direction $quarter keeps menu clear of native buttons" ($menu[2] -le ($native.Nodes | Where-Object {$_.Visible -and $_.Id.EndsWith('/quick_setting_button_main')} | Measure-Object Left -Minimum).Minimum)}
 }
 Shot 'camera-portrait-final'
 Send-ProbeCommand ui-orientation @('--ei','quarter','1') | Out-Null
 Shot 'camera-landscape-locked-final'
 Tap-AppLabel '그리드 설정 열기';Tap-AppLabel '설정'
 Tap-AppLabel '카메라 메뉴에 앱 끄기 버튼 표시'
 $toggle=Read-AppNodes | Where-Object {$_.Text -eq '카메라 메뉴에 앱 끄기 버튼 표시'} | Select-Object -First 1
 Check 'Stop switch checked state updates in current tab' $toggle.Checked
 Shot 'popup-settings-light'
 $before=(Get-CameraSnapshot).State
 Send-ProbeCommand ui-orientation @('--ei','quarter','3') | Out-Null
 $next=(Get-CameraSnapshot).State
 Check 'Open popup survives orientation' ($next.settingsPopupOpen -and $next.menuExpanded -and $next.menuBuildCount -eq $before.menuBuildCount)
 Tap-AppLabel '앱 테마';Choice '시스템 기본값'
 Invoke-ProbeAdb shell cmd uimode night yes | Out-Null
 Start-Sleep -Milliseconds 600
 Shot 'popup-system-dark'
 $next=(Get-CameraSnapshot).State
 Check 'System dark transition keeps settings popup and menu' ($next.settingsPopupOpen -and $next.menuExpanded -and $next.appTheme -eq 'system' -and $next.menuBuildCount -eq $before.menuBuildCount)
 Invoke-ProbeAdb shell cmd uimode night no | Out-Null
 Start-Sleep -Milliseconds 500
 Shot 'popup-system-light'
 Check 'System light transition retains selected Settings tab' ([bool](Read-AppNodes | Where-Object {$_.Description -eq '설정' -and $_.Selected}))
 Invoke-ProbeAdb shell wm size 1080x2200 | Out-Null
 Start-Sleep -Milliseconds 900
 $next=(Get-CameraSnapshot).State
 Check 'Configuration resize retains popup and menu' ($next.settingsPopupOpen -and $next.menuExpanded -and $next.menuBuildCount -eq $before.menuBuildCount)
 Shot 'popup-resized'
 Invoke-ProbeAdb shell wm size reset | Out-Null
 Start-Sleep -Milliseconds 700
 Tap-AppLabel '닫기'
 Send-ProbeCommand ui-orientation @('--ei','quarter','-1') | Out-Null
 Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Start-Sleep -Milliseconds 500
 Settings-Tab;Tap-AppLabel '앱 테마' -Scroll;Choice '밝게'
 Start-Sleep -Milliseconds 500
 Settings-Tab;Shot 'app-settings-light'
 Tap-AppLabel '앱을 열 때 수동으로 켜기' -Scroll
 Tap-AppLabel '앱 백그라운드 실행 끄기' -Scroll
 $stopped=(Get-CameraSnapshot).State;Check 'Plain stop row stops manual session' ($stopped.paused -and !$stopped.sessionActive)
 Tap-AppLabel '언어' -Scroll -Up
 $activity=(Invoke-ProbeAdb shell dumpsys activity activities) -join "`n"
 Check 'Language row opens One UI app language activity' ($activity -match '(mResumedActivity|topResumedActivity)[=:].*AppLanguageSettingsActivity')
 Start-Sleep -Milliseconds 800
 Invoke-ProbeAdb shell screencap -p /data/local/tmp/chry-v13-language.png
 Invoke-ProbeAdb pull /data/local/tmp/chry-v13-language.png (Join-Path $folder 'oneui-language.png') | Out-Null
 Invoke-ProbeAdb shell rm /data/local/tmp/chry-v13-language.png
 Invoke-ProbeAdb shell input keyevent KEYCODE_BACK
 Start-Sleep -Milliseconds 500
 Check 'Returning from language preserves manual stop' (!(Get-CameraSnapshot).State.sessionActive)
 Tap-AppLabel '접근성에서 Chrynalogia 끄기' -Scroll
 $activity=(Invoke-ProbeAdb shell dumpsys activity activities) -join "`n"
 Check 'Access row opens public One UI accessibility settings' ($activity -match '(mResumedActivity|topResumedActivity)[=:].*AccessibilitySettingsActivity')
 Start-Sleep -Milliseconds 700
 Invoke-ProbeAdb shell screencap -p /data/local/tmp/chry-v13-access.png
 Invoke-ProbeAdb pull /data/local/tmp/chry-v13-access.png (Join-Path $folder 'oneui-accessibility.png') | Out-Null
 Invoke-ProbeAdb shell rm /data/local/tmp/chry-v13-access.png
 $checks | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 (Join-Path $folder 'ui-results.json')
 Write-Output "PASS: $($checks.Count) v13 UI checks"
} @(
 {Invoke-ProbeAdb shell cmd uimode night $night | Out-Null},
 {Send-ProbeCommand ui-orientation @('--ei','quarter','-1') | Out-Null},
 {Stop-ProbeTest}
)
