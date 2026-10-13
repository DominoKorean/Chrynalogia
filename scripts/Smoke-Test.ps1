param([string]$Serial)
. "$PSScriptRoot\Device.ps1" -Serial $Serial
Start-ProbeTest
Invoke-ProbeTest {
$display=Get-ProbeDisplay
$swipeX=[int]($display.Width*0.79);$swipeBottom=[int]($display.Height*0.79);$swipeMiddle=[int]($display.Height*0.53);$swipeTop=[int]($display.Height*0.34)
$checks = [System.Collections.Generic.List[object]]::new()
function Assert-Overlay {
    param([string]$Name, [bool]$Expected)
    $state = $null
    for ($attempt = 0; $attempt -lt 10; $attempt++) {
        Send-ProbeCommand capture | Out-Null
        $json = Invoke-ProbeAdb shell run-as dev.chrynalogia.probe cat files/probe-status.json
        $state = ($json -join "`n") | ConvertFrom-Json
        if ($state.connected -and $state.overlayAttached -eq $Expected) { break }
        Start-Sleep -Milliseconds 300
    }
    $passed = $state.connected -and $state.overlayAttached -eq $Expected
    $checks.Add([pscustomobject]@{ name=$Name; passed=$passed; state=$state })
    if (-not $passed) { throw "Failed: $Name. State: $($state | ConvertTo-Json -Compress)" }
    Write-Output "PASS: $Name"
}
try {
    Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
    Assert-Overlay 'Camera foreground shows overlay' $true
    Send-ProbeCommand pause | Out-Null
    Assert-Overlay 'Pause removes overlay' $false
    Send-ProbeCommand resume | Out-Null
    Assert-Overlay 'Resume restores overlay' $true
    Invoke-ProbeAdb shell input keyevent KEYCODE_HOME
    Assert-Overlay 'Home hides overlay' $false
    Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
    Assert-Overlay 'Returning to camera restores overlay' $true
    Invoke-ProbeAdb shell cmd statusbar expand-notifications
    Assert-Overlay 'Notification shade hides overlay' $false
    Invoke-ProbeAdb shell cmd statusbar collapse
    Assert-Overlay 'Closing notification shade restores overlay' $true
} finally {
    Invoke-ProbeAdb shell cmd statusbar collapse
    Send-ProbeCommand resume | Out-Null
    $folder = Join-Path $script:ProjectRoot 'artifacts'
    New-Item -ItemType Directory -Force $folder | Out-Null
    $checks | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $folder 'smoke-test.json')
}
Write-Output 'These checks verify app/window state. Inspect screenshots and use real finger input to verify visibility and touch delivery.'

} @({ Stop-ProbeTest })
