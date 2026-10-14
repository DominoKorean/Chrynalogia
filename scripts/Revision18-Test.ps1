param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts/v18-checks'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
function Check([string]$Name,[bool]$Value){
 if(!$Value){Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder 'failure');throw "Failed: $Name"}
 $checks.Add([pscustomobject]@{Name=$Name;Passed=$true})
 $checks | ConvertTo-Json | Set-Content (Join-Path $folder 'ui-results.json')
}
function Flip-Check([int]$Physical){
 Send-ProbeCommand ui-orientation @('--ei','quarter',[string]$Physical) | Out-Null
 $before=(Get-CameraSnapshot).State
 foreach($label in @('좌우 반전','상하 반전')){
  $changeX=($Physical%2 -eq 0) -eq ($label -eq '좌우 반전')
  Tap-AppLabel $label
  $after=(Get-CameraSnapshot).State
  Check "Direction $Physical $label uses viewer axis" ($after.flipX -eq ($before.flipX -xor $changeX) -and $after.flipY -eq ($before.flipY -xor (-not $changeX)))
  Check "Direction $Physical $label keeps expanded menu" ($after.menuExpanded -and $after.menuBuildCount -eq $before.menuBuildCount)
  Tap-AppLabel $label
 }
}
$fixed=((Invoke-ProbeAdb shell wm fixed-to-user-rotation) -join '').Trim()
Start-ProbeTest
Invoke-ProbeTest {
 Invoke-ProbeAdb shell wm fixed-to-user-rotation enabled
 Invoke-ProbeAdb shell wm user-rotation lock 0
 Invoke-ProbeAdb shell am start --activity-clear-top -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Start-Sleep -Milliseconds 1200
 Tap-AppLabel '그리드';Start-Sleep -Milliseconds 400
 Tap-AppLabel '삼성 카메라 열기' -Scroll -Up
 Tap-AppLabel '그리드 메뉴 열기'
 $baseline=(Get-CameraSnapshot).State
 foreach($physical in @(0,1,2,3)){
  Flip-Check $physical
  Check "Sensor direction $physical keeps fixed coordinates" ((Get-CameraSnapshot).State.openerBounds -eq $baseline.openerBounds)
 }
 foreach($quarter in @(1,0,3,0,2,0)){
  Invoke-ProbeAdb shell wm user-rotation lock $quarter
  Send-ProbeCommand ui-orientation @('--ei','quarter',[string]((4-$quarter)%4)) | Out-Null
  Start-Sleep -Milliseconds 1000
  $state=(Get-CameraSnapshot).State
  Check "Display $quarter keeps the fixed anchor" ($state.displayQuarterTurn -eq $quarter -and $state.naturalOpenerBounds -eq $baseline.naturalOpenerBounds)
  Check "Display $quarter retains menu window" ($state.menuExpanded -and $state.menuBuildCount -eq $baseline.menuBuildCount)
  Flip-Check ((4-$quarter)%4)
  Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder "display-$quarter")
 }
 Tap-AppLabel '그리드 설정 열기'
 foreach($quarter in @(1,3,0)){
  Invoke-ProbeAdb shell wm user-rotation lock $quarter
  Send-ProbeCommand ui-orientation @('--ei','quarter',[string]((4-$quarter)%4)) | Out-Null
  Start-Sleep -Milliseconds 1000
  $state=(Get-CameraSnapshot).State
  Check "Popup display $quarter remains open" ($state.settingsPopupOpen -and $state.naturalOpenerBounds -eq $baseline.naturalOpenerBounds)
 }
 Tap-AppLabel '닫기'
 Write-Output "PASS: $($checks.Count) v18 UI checks"
} @(
 {Send-ProbeCommand ui-orientation @('--ei','quarter','-1') | Out-Null},
 {Invoke-ProbeAdb shell wm fixed-to-user-rotation $fixed | Out-Null},
 {Stop-ProbeTest}
)
