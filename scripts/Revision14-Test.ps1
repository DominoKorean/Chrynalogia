param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts/v14-checks'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
function Check([string]$Name,[bool]$Value){if(!$Value){Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder 'ui-failure');throw "Failed: $Name"};$checks.Add([pscustomobject]@{Name=$Name;Passed=$true});$checks | ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $folder 'ui-results.json')}
function Shot([string]$Name){Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder $Name)}
function System-Shot([string]$Name){Invoke-ProbeAdb shell screencap -p /data/local/tmp/chry-v14-system.png;Invoke-ProbeAdb pull /data/local/tmp/chry-v14-system.png (Join-Path $folder "$Name.png") | Out-Null;Invoke-ProbeAdb shell rm /data/local/tmp/chry-v14-system.png}
function System-Tap([string]$Text){
 # Camera diagnostics intentionally do not dump other apps' trees. The guide
 # already reports the next measured target; tapping that box also tests pass-through.
 $bounds=(Get-CameraSnapshot).State.accessibilityGuideBounds
 if($bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){throw "Guide target missing: $Text"}
 Invoke-ProbeAdb shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2))
 Start-Sleep -Milliseconds 700
}
function Execution {return Read-AppNodes | Where-Object {$_.Description -eq '앱 백그라운드 실행'} | Select-Object -First 1}
function Settings-Tab {
 for($i=0;$i -lt 5;$i++){
  Tap-AppLabel '설정'
  if(Read-AppNodes | Where-Object {$_.Visible -and $_.Description -eq '설정' -and $_.Selected}){return}
  Start-Sleep -Milliseconds 250
 }
 throw 'Settings tab did not become selected'
}
# Restore the original user backup before beginning a new reversible test session.
Send-ProbeCommand restore-state | Out-Null
Start-ProbeTest
Invoke-ProbeTest {
 Invoke-ProbeAdb shell am start --activity-clear-top -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Start-Sleep -Milliseconds 1500
 Tap-AppLabel '그리드';Tap-AppLabel '삼성 카메라 열기' -Scroll -Up
 Tap-AppLabel '그리드 메뉴 열기'
 Send-ProbeCommand ui-orientation @('--ei','quarter','0') | Out-Null
 $base=(Get-CameraSnapshot).State
 foreach($quarter in @(1,3,2,0)){
  Send-ProbeCommand ui-orientation @('--ei','quarter',[string]$quarter) | Out-Null
  Start-Sleep -Milliseconds 300
  $next=(Get-CameraSnapshot).State
  Check "Orientation $quarter keeps menu coordinates" ($next.menuBounds -eq $base.menuBounds -and $next.openerBounds -eq $base.openerBounds)
  Check "Orientation $quarter keeps preview and registered windows" ($next.bounds -eq $base.bounds -and $next.menuExpanded -and $next.menuBuildCount -eq $base.menuBuildCount)
 }
 Shot 'camera-stable-menu'
 $hide=Read-AppNodes | Where-Object {$_.Visible -and $_.Description -in @('그리드 숨기기','그리드 표시')} | Select-Object -First 1
 Tap-CameraNode $hide;Shot 'camera-eye-toggled'
 $hide=Read-AppNodes | Where-Object {$_.Visible -and $_.Description -in @('그리드 숨기기','그리드 표시')} | Select-Object -First 1
 Tap-CameraNode $hide
 foreach($label in @('그리드 90° 회전','좌우 반전','상하 반전')){
  Tap-AppLabel $label;$next=(Get-CameraSnapshot).State
  Check "$label retains menu position" ($next.menuBounds -eq $base.menuBounds -and $next.menuExpanded -and $next.menuBuildCount -eq $base.menuBuildCount)
 }
 Tap-AppLabel '그리드 설정 열기';Tap-AppLabel '설정'
 $before=(Get-CameraSnapshot).State
 foreach($quarter in @(1,2,3,0)){
  Send-ProbeCommand ui-orientation @('--ei','quarter',[string]$quarter) | Out-Null
  $next=(Get-CameraSnapshot).State
  Check "Open settings survives orientation $quarter" ($next.settingsPopupOpen -and $next.menuBounds -eq $before.menuBounds -and $next.openerBounds -eq $before.openerBounds -and $next.menuBuildCount -eq $before.menuBuildCount)
 }
 Tap-AppLabel '앱 테마';Tap-AppLabel '밝게';Start-Sleep -Milliseconds 450
 Tap-AppLabel '앱 테마';Shot 'theme-popup-light';Tap-AppLabel '어둡게';Start-Sleep -Milliseconds 450
 Tap-AppLabel '앱 테마';Shot 'theme-popup-dark';Tap-AppLabel '닫기'
 Check 'Theme update retains camera popup' ((Get-CameraSnapshot).State.settingsPopupOpen)
 Tap-AppLabel '닫기'
 Send-ProbeCommand ui-orientation @('--ei','quarter','-1') | Out-Null
 Invoke-ProbeAdb shell am start --activity-clear-top -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Start-Sleep -Milliseconds 500
 Settings-Tab;Tap-AppLabel '앱을 열 때 수동으로 켜기' -Scroll
 Start-Sleep -Milliseconds 700
 Tap-AppLabel '앱 백그라운드 실행' -Scroll
 $off=(Get-CameraSnapshot).State;$row=Execution
 Check 'Background toggle stops manual session immediately' ($off.paused -and !$off.sessionActive -and !$row.Checked -and $row.Text -match '꺼짐')
 Shot 'execution-off'
 Tap-AppLabel '앱 백그라운드 실행'
 Start-Sleep -Milliseconds 500
 $on=(Get-CameraSnapshot).State;$row=Execution
 Check 'Background toggle starts manual session immediately' ($on.sessionActive -and $row.Checked -and $row.Text -match '실행 중')
 Shot 'execution-on'
 Tap-AppLabel '앱 백그라운드 실행'
 Tap-AppLabel '접근성에서 Chrynalogia 끄기' -Scroll
 Start-Sleep -Milliseconds 800
 $activity=(Invoke-ProbeAdb shell dumpsys activity activities) -join "`n"
 Check 'Accessibility uses public Settings entry' ($activity -match 'topResumedActivity=.*com.android.settings/')
 Check 'Public accessibility page has non-touchable guide' ((Get-CameraSnapshot).State.accessibilityGuideBounds -ne 'none')
 System-Shot 'accessibility-guide-installed'
 System-Tap '설치된 앱'
 Check 'Installed apps highlights Chrynalogia entry' ((Get-CameraSnapshot).State.accessibilityGuideBounds -ne 'none')
 System-Shot 'accessibility-guide-app'
 System-Tap 'Chrynalogia'
 Check 'Service page highlights native switch' ((Get-CameraSnapshot).State.accessibilityGuideBounds -ne 'none')
 System-Shot 'accessibility-guide-switch'
 Invoke-ProbeAdb shell input keyevent KEYCODE_HOME
 Start-Sleep -Milliseconds 500
 Check 'Guide closes on leaving Settings' ((Get-CameraSnapshot).State.accessibilityGuideBounds -eq 'none')
 $checks | ConvertTo-Json -Depth 4 | Set-Content -Encoding utf8 (Join-Path $folder 'ui-results.json')
 Write-Output "PASS: $($checks.Count) v14 UI checks"
} @(
 {Send-ProbeCommand ui-orientation @('--ei','quarter','-1') | Out-Null},
 {Stop-ProbeTest}
)
