param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\lifecycle-test'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
$originalRotation=(Invoke-ProbeAdb shell wm user-rotation) -join ''
$originalRotation | Set-Content -Encoding utf8 (Join-Path $folder 'original-rotation.txt')
try {
    Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity -f 0x04000000 | Out-Null
    Start-Sleep -Milliseconds 600
    Tap-AppLabel '앱을 열 때 수동으로 켜기' -Scroll
    Assert-State 'Manual session starts from settings' {param($s) $s.runMode -eq 'manual' -and $s.sessionActive}
    Tap-AppLabel '백그라운드 실행 끄기'
    Assert-State 'Stop on app screen' {param($s) -not $s.sessionActive -and $s.paused}
    Invoke-ProbeAdb shell wm user-rotation lock 1
    Start-Sleep -Milliseconds 1400
    Assert-State 'Rotation does not restart a stopped manual session' {param($s) $s.runMode -eq 'manual' -and -not $s.sessionActive -and $s.paused}
    Invoke-ProbeAdb shell wm user-rotation lock 0
    Start-Sleep -Milliseconds 1000
    Assert-State 'Portrait recreation remains stopped' {param($s) -not $s.sessionActive -and $s.paused}
    Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
    Start-Sleep -Milliseconds 600
    Assert-State 'Explicit app launch resumes manual mode' {param($s) $s.sessionActive}
} finally {
    if($originalRotation -match 'lock\s+(\d)'){Invoke-ProbeAdb shell wm user-rotation lock $Matches[1]}
    else {Invoke-ProbeAdb shell wm user-rotation free}
    Start-Sleep -Milliseconds 700
    Tap-AppLabel '항상 켜기' -Scroll -EnsureChecked
    $checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
}
Write-Output "Passed $($checks.Count) lifecycle checks"
