param([string]$Serial, [string[]]$Profiles)
. "$PSScriptRoot\Camera-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\screen-size-test'
New-Item -ItemType Directory -Force $folder | Out-Null
$sizeText=(Invoke-ProbeAdb shell wm size) -join "`n"
$densityText=(Invoke-ProbeAdb shell wm density) -join "`n"
$restoreSize=if ($sizeText -match 'Override size: (\d+x\d+)') { $Matches[1] } else { 'reset' }
$restoreDensity=if ($densityText -match 'Override density: (\d+)') { $Matches[1] } else { 'reset' }
[pscustomobject]@{originalSize=$sizeText; originalDensity=$densityText; restoreSize=$restoreSize; restoreDensity=$restoreDensity} |
    ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $folder 'original-display.json')
$results=[System.Collections.Generic.List[object]]::new()
if ($Profiles -and (Test-Path (Join-Path $folder 'result.json'))) {
    $previous=Get-Content (Join-Path $folder 'result.json') -Raw | ConvertFrom-Json
    foreach ($item in @($previous)) {
        if ($item.profile -notin $Profiles) { $results.Add($item) }
    }
}
try {
    foreach ($profile in @(
        @{Name='compact-720x1600'; Size='720x1600'; Density=320},
        @{Name='short-720x1280'; Size='720x1280'; Density=280},
        @{Name='large-1440x3088'; Size='1440x3088'; Density=480}
    )) {
        if ($Profiles -and $profile.Name -notin $Profiles) { continue }
        Write-Output "Testing logical display $($profile.Size), density $($profile.Density) on the same S23"
        Invoke-ProbeAdb shell wm size $profile.Size
        Invoke-ProbeAdb shell wm density "$($profile.Density)"
        Start-Sleep -Milliseconds 800
        Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
        Start-Sleep -Milliseconds 700
        $snapshot=Get-CameraSnapshot
        $photo=Find-CameraNode $snapshot 'shooting_mode_item_button_full' '^사진$'
        if ($photo) { Tap-CameraNode $photo }
        $profileFolder=Join-Path $folder $profile.Name
        & "$PSScriptRoot\Ratio-Test.ps1" -Serial $Serial -OutputFolder $profileFolder
        $ratioResults=Get-Content (Join-Path $profileFolder 'result.json') -Raw | ConvertFrom-Json
        $passed=@($ratioResults).Count -eq 4 -and -not ($ratioResults | Where-Object { -not $_.passed })
        $results.Add([pscustomobject]@{profile=$profile.Name; size=$profile.Size; density=$profile.Density; passed=$passed; ratios=$ratioResults})
        if (-not $passed) { throw "Screen size profile failed: $($profile.Name)" }
    }
} finally {
    Invoke-ProbeAdb shell wm size $restoreSize
    Invoke-ProbeAdb shell wm density $restoreDensity
    $results | ConvertTo-Json -Depth 9 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
    Invoke-ProbeAdb shell wm size
    Invoke-ProbeAdb shell wm density
    Write-Output 'Restored original display settings. These tests simulate layout sizes; they do not test a different device or camera build.'
}
