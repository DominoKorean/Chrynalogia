param([string]$Serial, [string]$OutputFolder)
. "$PSScriptRoot\Camera-Ui.ps1" -Serial $Serial
Start-ProbeTest
Invoke-ProbeTest {
$display=Get-ProbeDisplay
$swipeX=[int]($display.Width*0.79);$swipeBottom=[int]($display.Height*0.79);$swipeMiddle=[int]($display.Height*0.53);$swipeTop=[int]($display.Height*0.34)
$folder=if ($OutputFolder) { $OutputFolder } else { Join-Path $script:ProjectRoot 'artifacts\ratio-test' }
New-Item -ItemType Directory -Force $folder | Out-Null
$results=[System.Collections.Generic.List[object]]::new()
$menu=Open-CameraRatioMenu
$original=($menu.Nodes | Where-Object { $_.Text -match '_PICTURE_RATIO_' -and $_.Selected } | Select-Object -First 1).Text
if (-not $original) { throw 'Original photo aspect ratio is unknown; cannot restore it safely.' }
try {
    foreach ($entry in @(
        @{Name='3-4'; Suffix='NORMAL'; Expected=4.0/3.0},
        @{Name='9-16'; Suffix='WIDE'; Expected=16.0/9.0},
        @{Name='1-1'; Suffix='SQUARE'; Expected=1.0},
        @{Name='FULL'; Suffix='FULL'; Expected=0.0}
    )) {
        $selectionMatches=$false
        for ($selectionAttempt=0; $selectionAttempt -lt 3; $selectionAttempt++) {
            $menu=Open-CameraRatioMenu
            $option=Find-CameraNode $menu 'quick_setting_button_main' "_PICTURE_RATIO_$($entry.Suffix)$"
            Tap-CameraNode $option
            # Preview/menu animations take longer with a large logical framebuffer.
            Start-Sleep -Milliseconds 1200
            $selectedMenu=Open-CameraRatioMenu
            $selectedOption=Find-CameraNode $selectedMenu 'quick_setting_button_main' "_PICTURE_RATIO_$($entry.Suffix)$"
            $selectionMatches=$selectedOption -and $selectedOption.Selected
            if ($selectionMatches) { break }
        }
        $passed=$false
        for ($attempt=0; $attempt -lt 12; $attempt++) {
            $snapshot=Get-CameraSnapshot
            $preview=Find-CameraNode $snapshot 'camera_preview'
            $root=($snapshot.Nodes | Where-Object Id -eq 'null' | Select-Object -First 1)
            if ($preview) {
                $width=$preview.Right-$preview.Left; $height=$preview.Bottom-$preview.Top
                $aspect=[Math]::Max($width,$height)/[double][Math]::Min($width,$height)
                $expectedBounds="[$($preview.Left),$($preview.Top)][$($preview.Right),$($preview.Bottom)]"
                # Full may be pillarboxed or clipped after a logical display override.
                # Confirm the selected Full option and a preview that fills one axis.
                $ratioMatches=if ($entry.Expected -eq 0) { $width -ge ($root.Right-$root.Left)*0.90 -or $height -ge ($root.Bottom-$root.Top)*0.90 }
                    else { [Math]::Abs($aspect-$entry.Expected) -lt 0.025 }
                $passed=$selectionMatches -and $ratioMatches -and $snapshot.State.overlayAttached -and $snapshot.State.bounds -eq $expectedBounds -and $snapshot.State.locator -eq 'camera-preview-view'
                if ($passed) { break }
            }
            Start-Sleep -Milliseconds 250
        }
        Save-CameraSnapshot $snapshot (Join-Path $folder $entry.Name)
        $results.Add([pscustomobject]@{name=$entry.Name; passed=$passed; selectedOption=$selectedOption.Text; selectionConfirmed=$selectionMatches; aspect=$aspect; previewBounds=$expectedBounds; state=$snapshot.State})
        if (-not $passed) { throw "Ratio test failed: $($entry.Name)" }
        Write-Output "PASS: $($entry.Name) preview=$expectedBounds"
    }
} finally {
    $results | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
    $restored=$false
    for ($restoreAttempt=0; $restoreAttempt -lt 4; $restoreAttempt++) {
        try {
            $menu=Open-CameraRatioMenu
            $originalNode=Find-CameraNode $menu 'quick_setting_button_main' ([regex]::Escape($original)+'$')
            Tap-CameraNode $originalNode
            Start-Sleep -Milliseconds 1000
            $verify=Open-CameraRatioMenu
            $verifiedNode=Find-CameraNode $verify 'quick_setting_button_main' ([regex]::Escape($original)+'$')
            $restored=$verifiedNode -and $verifiedNode.Selected
            if ($restored) { break }
        } catch { Start-Sleep -Milliseconds 400 }
    }
    [pscustomobject]@{originalOption=$original; restored=$restored} |
        ConvertTo-Json | Set-Content -Encoding utf8 (Join-Path $folder 'restore-result.json')
    if (-not $restored) { throw 'Could not restore the original camera ratio; test results are saved separately.' }
    Write-Output "Restored original ratio: $original"
}

} @({ Stop-ProbeTest })
