param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts/v15-checks'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
function Check([string]$Name,[bool]$Value){
 if(!$Value){Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder 'ui-failure');throw "Failed: $Name"}
 $checks.Add([pscustomobject]@{Name=$Name;Passed=$true})
 $checks | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $folder 'ui-results.json')
}
function Parts([string]$Box){
 if($Box -notmatch '\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]'){throw "Invalid box: $Box"}
 return @([int]$Matches[1],[int]$Matches[2],[int]$Matches[3],[int]$Matches[4])
}
function Project([string]$Box,[int]$Quarter){
 $b=Parts $Box;$l=$b[0];$t=$b[1];$r=$b[2];$bottom=$b[3]
 switch($Quarter){
  1 {return "[$t,$($naturalWidth-$r)][$bottom,$($naturalWidth-$l)]"}
  2 {return "[$($naturalWidth-$r),$($naturalHeight-$bottom)][$($naturalWidth-$l),$($naturalHeight-$t)]"}
  3 {return "[$($naturalHeight-$bottom),$l][$($naturalHeight-$t),$r]"}
  default {return $Box}
 }
}
function Shot([string]$Name){Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder $Name)}
function Turn([int]$Quarter){
 # This changes the actual display, while the second input reproduces the corresponding physical sensor angle.
 Invoke-ProbeAdb shell wm user-rotation lock $Quarter
 Send-ProbeCommand ui-orientation @('--ei','quarter',[string]((4-$Quarter)%4)) | Out-Null
 Start-Sleep -Milliseconds 1000
}
function Verify-Anchors([string]$Name,[int]$Quarter,$Baseline){
 $s=(Get-CameraSnapshot).State
 Check "$Name actual display rotation" ($s.displayQuarterTurn -eq $Quarter)
 Check "$Name retains natural anchor and menu" ($s.naturalOpenerBounds -eq $Baseline.naturalOpenerBounds -and $s.naturalMenuBounds -eq $Baseline.naturalMenuBounds)
 Check "$Name projects opener to the same physical corner" ($s.openerBounds -eq (Project $Baseline.naturalOpenerBounds $Quarter))
 Check "$Name projects the grouped menu" ($s.menuBounds -eq (Project $Baseline.naturalMenuBounds $Quarter))
 Check "$Name keeps windows and upright icons" ($s.menuExpanded -and $s.menuBuildCount -eq $Baseline.menuBuildCount -and [Math]::Abs([Math]::IEEERemainder($s.iconRotation,360)) -lt 1)
 $actual=Read-AppNodes | Where-Object {$_.Visible -and $_.Clickable -and $_.Description -eq '메뉴 닫기'} | Select-Object -First 1
 Check "$Name actual opener view matches projected window" ($actual -and "[$($actual.Left),$($actual.Top)][$($actual.Right),$($actual.Bottom)]" -eq $s.openerBounds)
}
$fixed=((Invoke-ProbeAdb shell wm fixed-to-user-rotation) -join '').Trim()
Start-ProbeTest
Invoke-ProbeTest {
 Invoke-ProbeAdb shell wm fixed-to-user-rotation enabled
 Invoke-ProbeAdb shell wm user-rotation lock 0
 Invoke-ProbeAdb shell am start --activity-clear-top -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Start-Sleep -Milliseconds 1200
 Tap-AppLabel '그리드';Tap-AppLabel '삼성 카메라 열기' -Scroll -Up
 Turn 0
 Tap-AppLabel '그리드 메뉴 열기'
 $base=(Get-CameraSnapshot).State
 $frame=Parts $base.contentWindowBounds;$naturalWidth=$frame[2]-$frame[0];$naturalHeight=$frame[3]-$frame[1]
 $nativeRight=((Get-CameraSnapshot).Nodes | Where-Object {$_.Visible -and $_.Id.EndsWith('/quick_setting_button_main')} | Measure-Object Right -Maximum).Maximum
 Check 'Initial natural margin matches stable Samsung chrome' ((Parts $base.naturalOpenerBounds)[0] -eq $naturalWidth-$nativeRight)
 Shot 'portrait-baseline'
 $index=0
 foreach($quarter in @(1,0,3,0,2,0,1,3,0)){
  Turn $quarter;Verify-Anchors "Cycle $index direction $quarter" $quarter $base
  Shot "cycle-$index-$quarter";$index++
 }
 Tap-AppLabel '그리드 설정 열기';Tap-AppLabel '설정'
 foreach($quarter in @(1,3,0)){
  Turn $quarter;Verify-Anchors "Popup direction $quarter" $quarter $base
  Check "Popup direction $quarter stays open on selected tab" ((Get-CameraSnapshot).State.settingsPopupOpen -and [bool](Read-AppNodes | Where-Object {$_.Visible -and $_.Description -eq '설정' -and $_.Selected}))
  Shot "popup-$quarter"
 }
 Tap-AppLabel '닫기'
 # Also verify opening the camera directly while already landscape.
 Invoke-ProbeAdb shell input keyevent KEYCODE_HOME
 foreach($quarter in @(1,3)){
  Turn $quarter
  Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
  Start-Sleep -Milliseconds 1000
  Tap-AppLabel '그리드 메뉴 열기'
  $fresh=(Get-CameraSnapshot).State
  Check "Fresh landscape $quarter calibrates original natural margin" ($fresh.naturalOpenerBounds -eq $base.naturalOpenerBounds)
  Verify-Anchors "Fresh landscape $quarter" $quarter $fresh
  Shot "fresh-landscape-$quarter"
  Invoke-ProbeAdb shell input keyevent KEYCODE_HOME
 }
 Turn 0
 Invoke-ProbeAdb shell am start --activity-clear-top -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Start-Sleep -Milliseconds 800
 Tap-AppLabel '설정';Tap-AppLabel '접근성에서 Chrynalogia 끄기' -Scroll
 Start-Sleep -Milliseconds 600
 $activity=(Invoke-ProbeAdb shell dumpsys activity activities) -join "`n"
 Check 'Access row still opens public Settings' ($activity -match 'topResumedActivity=.*com.android.settings/')
 $windows=(Invoke-ProbeAdb shell dumpsys window windows) -join "`n"
 Check 'Settings opens without any Chrynalogia overlay' ($windows -notmatch 'Chrynalogia-(AccessibilityGuide|CameraControls|CameraMenu|CameraSettings)')
 Invoke-ProbeAdb shell screencap -p /data/local/tmp/chry-v15-settings.png
 Invoke-ProbeAdb pull /data/local/tmp/chry-v15-settings.png (Join-Path $folder 'accessibility-no-overlay.png') | Out-Null
 Invoke-ProbeAdb shell rm /data/local/tmp/chry-v15-settings.png
 Write-Output "PASS: $($checks.Count) v15 UI checks"
} @(
 {Send-ProbeCommand ui-orientation @('--ei','quarter','-1') | Out-Null},
 {Invoke-ProbeAdb shell wm fixed-to-user-rotation $fixed | Out-Null},
 {Stop-ProbeTest}
)
