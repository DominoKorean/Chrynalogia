param([string]$Serial)
. "$PSScriptRoot\Device.ps1" -Serial $Serial
function Get-CameraSnapshot {
    Send-ProbeCommand capture | Out-Null
    $stateText = Invoke-ProbeAdb shell run-as dev.chrynalogia.probe cat files/probe-status.json
    $state = ($stateText -join "`n") | ConvertFrom-Json
    $tree = (Invoke-ProbeAdb shell run-as dev.chrynalogia.probe cat files/camera-ui.txt) -join "`n"
    $nodes = foreach ($line in ($tree -split "`n")) {
        if ($line -match 'id=(\S+) bounds=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\] visible=(true|false) clickable=(true|false)(?: selected=(true|false) checked=(true|false))? text=(.*?) desc=(.*)$') {
            [pscustomobject]@{
                Id=$Matches[1]; Left=[int]$Matches[2]; Top=[int]$Matches[3]
                Right=[int]$Matches[4]; Bottom=[int]$Matches[5]
                Visible=$Matches[6] -eq 'true'; Clickable=$Matches[7] -eq 'true'
                Selected=$Matches[8] -eq 'true'; Checked=$Matches[9] -eq 'true'
                Text=$Matches[10]; Description=$Matches[11]; Raw=$line
            }
        }
    }
    return [pscustomobject]@{ State=$state; Tree=$tree; Nodes=@($nodes) }
}
function Find-CameraNode {
    param($Snapshot, [string]$Id, [string]$LabelPattern='.*')
    return ($Snapshot.Nodes | Where-Object {
        $_.Visible -and ($_.Id.EndsWith("/$Id")) -and ($_.Text -match $LabelPattern -or $_.Description -match $LabelPattern -or ($_.Text + ' ' + $_.Description) -match $LabelPattern)
    } | Select-Object -First 1)
}
function Tap-CameraNode {
    param($Node)
    if (-not $Node) { throw 'Requested camera UI node not found' }
    $x=[int](($Node.Left+$Node.Right)/2); $y=[int](($Node.Top+$Node.Bottom)/2)
    Invoke-ProbeAdb shell input tap "$x" "$y"
    Start-Sleep -Milliseconds 650
}
function Open-CameraQuickControls {
    $snapshot=Get-CameraSnapshot
    if ($snapshot.State.foreground -ne 'com.sec.android.app.camera') {
        Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
        Start-Sleep -Milliseconds 650
        $snapshot=Get-CameraSnapshot
    }
    if (Find-CameraNode $snapshot 'quick_setting_main_list') { return $snapshot }
    $entry=Find-CameraNode $snapshot 'quick_setting_entry_button'
    if (-not $entry) {
        # Focus inside the measured preview to close a transient zoom/submenu UI.
        $preview=Find-CameraNode $snapshot 'camera_preview'
        if ($preview) { Tap-CameraNode $preview }
        else { Invoke-ProbeAdb shell input keyevent KEYCODE_BACK; Start-Sleep -Milliseconds 350 }
        for ($attempt=0; $attempt -lt 8; $attempt++) {
            $snapshot=Get-CameraSnapshot
            if (Find-CameraNode $snapshot 'quick_setting_main_list') { return $snapshot }
            $entry=Find-CameraNode $snapshot 'quick_setting_entry_button'
            if ($entry) { break }
            Start-Sleep -Milliseconds 300
        }
    }
    Tap-CameraNode $entry
    $snapshot=Get-CameraSnapshot
    if (-not (Find-CameraNode $snapshot 'quick_setting_main_list')) { throw 'Quick controls did not open' }
    return $snapshot
}
function Open-CameraRatioMenu {
    $current=Get-CameraSnapshot
    if ((Find-CameraNode $current 'quick_setting_sub_list') -and ($current.Nodes | Where-Object { $_.Visible -and $_.Text -match '_RATIO_' })) {
        return $current
    }
    $snapshot=Open-CameraQuickControls
    $ratio=Find-CameraNode $snapshot 'quick_setting_button_main' '^비율 비율$|ratio|aspect'
    Tap-CameraNode $ratio
    $snapshot=Get-CameraSnapshot
    if (-not (Find-CameraNode $snapshot 'quick_setting_sub_list')) { throw 'Ratio submenu did not open' }
    return $snapshot
}
function Save-CameraSnapshot {
    param($Snapshot, [string]$Folder)
    New-Item -ItemType Directory -Force $Folder | Out-Null
    $Snapshot.State | ConvertTo-Json -Depth 6 | Set-Content -Encoding utf8 (Join-Path $Folder 'state.json')
    $Snapshot.Tree | Set-Content -Encoding utf8 (Join-Path $Folder 'camera-ui.txt')
    Invoke-ProbeAdb shell screencap -p /data/local/tmp/chrynalogia-probe.png
    Invoke-ProbeAdb pull /data/local/tmp/chrynalogia-probe.png (Join-Path $Folder 'screen.png') | Out-Null
    Invoke-ProbeAdb shell rm /data/local/tmp/chrynalogia-probe.png
}
function Open-CameraMoreMenu {
    for ($attempt=0; $attempt -lt 10; $attempt++) {
        $snapshot=Get-CameraSnapshot
        if (Find-CameraNode $snapshot 'more_grid_list_view') { return $snapshot }
        $more=Find-CameraNode $snapshot 'shooting_mode_item_button_full' '^더보기$|^More$'
        $back=Find-CameraNode $snapshot 'instant_mode_back_button'
        if ($more) { Tap-CameraNode $more }
        elseif ($back) { Tap-CameraNode $back }
        elseif ($snapshot.State.foreground -ne 'com.sec.android.app.camera') {
            Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
            Start-Sleep -Milliseconds 600
        } else { Start-Sleep -Milliseconds 300 }
    }
    Save-CameraSnapshot $snapshot (Join-Path $script:ProjectRoot 'artifacts\more-menu-failure')
    throw 'More modes menu did not open; inspect artifacts/more-menu-failure.'
}
