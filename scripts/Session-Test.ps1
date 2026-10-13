param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\session-test'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
try {
    Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
    Start-Sleep -Milliseconds 700
    Tap-CameraNode (Wait-CameraPreview)
    $nodes=@(Read-AppNodes)
    if($nodes.Description -contains '그리드 메뉴 열기') {Tap-AppLabel '그리드 메뉴 열기'}
    Tap-AppLabel '그리드 설정 열기'
    Tap-AppLabel '앱을 열 때 수동으로 켜기' -Scroll
    Start-Sleep -Milliseconds 500
    # Accept the runtime notification dialog for the feature being tested.
    Invoke-ProbeAdb shell uiautomator dump /data/local/tmp/chrynalogia-notification.xml | Out-Null
    [xml]$permission=(Invoke-ProbeAdb shell cat /data/local/tmp/chrynalogia-notification.xml) -join ''
    if($permission.SelectNodes('//node') | Where-Object {$_.text -match '^Chrynalogia에서 알림을'}) {
        $allow=$permission.SelectNodes('//node') | Where-Object {$_.text -eq '허용'} | Select-Object -First 1
        if(-not $allow){throw 'Notification allow button not found'}
        $allow.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]' | Out-Null
        Invoke-ProbeAdb shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2))
        Start-Sleep -Milliseconds 600
    }
    Assert-State 'Manual session starts and shows notification' {param($s) $s.runMode -eq 'manual' -and $s.sessionActive}
    $notification=Invoke-ProbeAdb shell dumpsys notification --noredact
    $notification | Select-String 'dev.chrynalogia.probe|Chrynalogia 실행 중' | Set-Content -Encoding utf8 (Join-Path $folder 'notification.txt')
    if(-not ($notification | Select-String 'Chrynalogia 실행 중')) {throw 'Manual notification missing'}
    Tap-AppLabel '삼성 카메라 열기'
    Assert-State 'Manual camera overlay' {param($s) $s.runMode -eq 'manual' -and $s.overlayAttached}
    Tap-AppLabel '그리드 메뉴 열기'
    Tap-AppLabel '오버레이 앱 끄기'
    Assert-State 'Manual toolbar stop' {param($s) $s.paused -and -not $s.overlayAttached -and -not $s.sessionActive}
    Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
    Start-Sleep -Milliseconds 600
    Assert-State 'Opening app restarts manual session' {param($s) $s.runMode -eq 'manual' -and $s.sessionActive}
    # Expand notifications and use the actual notification action, not a debug pause command.
    Invoke-ProbeAdb shell cmd statusbar expand-notifications
    Start-Sleep -Milliseconds 600
    Invoke-ProbeAdb shell uiautomator dump /data/local/tmp/chrynalogia-notification.xml | Out-Null
    [xml]$shade=(Invoke-ProbeAdb shell cat /data/local/tmp/chrynalogia-notification.xml) -join ''
    $stop=$shade.SelectNodes('//node') | Where-Object {$_.text -eq '끄기'} | Select-Object -First 1
    if(-not $stop) {
        $title=$shade.SelectNodes('//node') | Where-Object {$_.text -eq 'Chrynalogia 실행 중'} | Select-Object -First 1
        if(-not $title){throw 'Manual notification is not visible'}
        $title.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]' | Out-Null
        $x=[int](([int]$Matches[1]+[int]$Matches[3])/2);$y=[int](([int]$Matches[2]+[int]$Matches[4])/2)
        Invoke-ProbeAdb shell input swipe $x $y $x ([string]($y+180)) 350
        Invoke-ProbeAdb shell uiautomator dump /data/local/tmp/chrynalogia-notification.xml | Out-Null
        [xml]$shade=(Invoke-ProbeAdb shell cat /data/local/tmp/chrynalogia-notification.xml) -join ''
        $stop=$shade.SelectNodes('//node') | Where-Object {$_.text -eq '끄기'} | Select-Object -First 1
    }
    if(-not $stop){throw 'Notification stop action is not visible'}
    $stop.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]' | Out-Null
    Invoke-ProbeAdb shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2))
    Start-Sleep -Milliseconds 400
    Assert-State 'Notification stop action' {param($s) $s.paused -and -not $s.sessionActive}
} finally {
    Invoke-ProbeAdb shell cmd statusbar collapse
    # Restore always-on mode through the app UI.
    Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
    Start-Sleep -Milliseconds 600
    Tap-AppLabel '항상 켜기' -Scroll
    Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
    $checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
}
Write-Output "Passed $($checks.Count) session checks"
