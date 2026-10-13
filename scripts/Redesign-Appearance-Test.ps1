param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
Start-ProbeTest
Invoke-ProbeTest {
$display=Get-ProbeDisplay
$swipeX=[int]($display.Width*0.79);$swipeBottom=[int]($display.Height*0.79);$swipeMiddle=[int]($display.Height*0.53);$swipeTop=[int]($display.Height*0.34)
$folder=Join-Path $script:ProjectRoot 'artifacts\redesign-checks'
$checks=[System.Collections.Generic.List[object]]::new()
function Choose-Appearance([string]$Label) {
 $choice=@(Read-AppNodes) | Where-Object {$_.Visible -and $_.Text -eq $Label -and $_.Class -eq 'android.widget.CheckedTextView'} | Select-Object -First 1
 if(-not $choice){throw "Appearance choice missing: $Label"}
 Tap-CameraNode $choice
 Start-Sleep -Milliseconds 750
}
try {
 Tap-AppLabel '그리드 메뉴 열기'
 Tap-AppLabel '그리드 설정 열기'
 Tap-AppLabel '설정'
 $bounds=(Get-CameraSnapshot).State.bounds
 Tap-AppLabel '앱 테마'
 Choose-Appearance '밝게'
 Assert-State 'Camera popup applies light theme without moving grid' {param($s) $s.appTheme -eq 'light' -and $s.settingsPopupOpen -and $s.overlayAttached -and $s.bounds -eq $bounds}
 Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder 'popup-light')
 Tap-AppLabel '언어'
 Choose-Appearance 'English'
 Assert-State 'Camera popup applies English and remains available' {param($s) $s.appLanguage -eq 'en' -and $s.settingsPopupOpen -and $s.overlayAttached}
 if(-not (@(Read-AppNodes).Description -contains 'Language')){throw 'Camera popup language did not update'}
 Tap-AppLabel 'Language'
 Choose-Appearance 'System default'
 Assert-State 'Camera popup restores system language' {param($s) $s.appLanguage -eq '' -and $s.settingsPopupOpen -and $s.overlayAttached}
 Tap-AppLabel '앱 테마'
 Choose-Appearance '시스템 기본값'
 Tap-AppLabel '닫기'
 if(@(Read-AppNodes).Description -contains '메뉴 닫기'){Tap-AppLabel '메뉴 닫기'}
 Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Tap-AppLabel '설정'
 Tap-AppLabel '앱을 열 때 수동으로 켜기' -Scroll
 Assert-State 'Manual session starts from redesigned settings' {param($s) $s.runMode -eq 'manual' -and $s.sessionActive}
 Tap-AppLabel '백그라운드 실행 끄기' -Scroll
 Assert-State 'Manual session stops from redesigned settings' {param($s) $s.paused -and -not $s.sessionActive}
 Tap-AppLabel '설정'
 Tap-AppLabel '앱 테마'
 Choose-Appearance '밝게'
 Assert-State 'Theme recreation preserves stopped manual session' {param($s) $s.appTheme -eq 'light' -and $s.paused -and -not $s.sessionActive}
 Tap-AppLabel '언어'
 Choose-Appearance 'English'
 Assert-State 'Language recreation preserves stopped manual session' {param($s) $s.appLanguage -eq 'en' -and $s.paused -and -not $s.sessionActive}
 Tap-AppLabel 'Language'
 Choose-Appearance 'System default'
 Tap-AppLabel '앱 테마'
 Choose-Appearance '시스템 기본값'
 Tap-AppLabel '항상 켜기' -Scroll
 Assert-State 'Original always-on mode and appearance restored' {param($s) $s.runMode -eq 'always' -and $s.sessionActive -and $s.appTheme -eq 'system' -and $s.appLanguage -eq ''}
 Tap-AppLabel '그리드'
 Tap-AppLabel '삼성 카메라 열기'
 Wait-CameraPreview | Out-Null
 Assert-State 'Camera overlay works after language/theme/session transitions' {param($s) $s.overlayAttached -and $s.controlsAttached}
} finally {
 $checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'appearance-checks.json')
 Invoke-ProbeAdb shell rm -f /data/local/tmp/chrynalogia-appearance.xml
}
Write-Output "Passed $($checks.Count) appearance and session checks"

} @({ Stop-ProbeTest })
