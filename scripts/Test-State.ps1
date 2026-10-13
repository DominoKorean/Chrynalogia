function Find-ProbeSlider($Nodes, [string]$Label) {
    foreach($node in $Nodes) {
        if(-not $node.Visible -or $node.Description -ne $Label -or $node.Class -notmatch '(SeekBar|KitSlider)$' -or $node.Right -le $node.Left -or $node.Bottom -le $node.Top){continue}
        $blocked=$Nodes | Where-Object { $_.Visible -and $_.Clickable -and $_.Description -in @('그리드','라인','설정','Grid','Line','Settings') -and $node.Left -lt $_.Right -and $node.Right -gt $_.Left -and $node.Top -lt $_.Bottom -and $node.Bottom -gt $_.Top }
        if(-not $blocked){return $node}
    }
}
function Get-ProbeSliderPoint($Node, [int]$Value, [int]$Min, [int]$Max, [double]$DensityScale) {
    if ($Max -le $Min -or $DensityScale -le 0) { throw 'Invalid slider range or density' }
    $padding=12*$DensityScale
    $ratio=[Math]::Max(0.0,[Math]::Min(1.0,($Value-$Min)/[double]($Max-$Min)))
    [pscustomobject]@{X=[int][Math]::Round($Node.Left+$padding+$ratio*($Node.Right-$Node.Left-2*$padding));Y=[int][Math]::Round(($Node.Top+$Node.Bottom)/2)}
}
function Get-ProbeDisplay {
    $size=(Invoke-ProbeAdb shell wm size) -join "`n"
    $density=(Invoke-ProbeAdb shell wm density) -join "`n"
    $dimensions=[regex]::Matches($size,'(\d+)x(\d+)') | Select-Object -Last 1
    $dpi=[regex]::Matches($density,'\d+') | Select-Object -Last 1
    if (-not $dimensions -or -not $dpi) { throw 'Display metrics unavailable' }
    $w=[int]$dimensions.Groups[1].Value;$h=[int]$dimensions.Groups[2].Value
    $rotation=(Invoke-ProbeAdb shell dumpsys input) -join "`n"
    if ($rotation -match 'SurfaceOrientation:\s*[13]') { $temporary=$w;$w=$h;$h=$temporary }
    [pscustomobject]@{Width=$w;Height=$h;DensityScale=[int]$dpi.Value/160.0}
}
function Invoke-ProbeTest([scriptblock]$Body, [scriptblock[]]$Restore) {
    $failures=[System.Collections.Generic.List[System.Exception]]::new()
    try { & $Body } catch { $failures.Add($_.Exception) }
    foreach ($step in $Restore) { try { & $step } catch { $failures.Add($_.Exception) } }
    if ($failures.Count) { throw [System.AggregateException]::new('Test or restoration failed',$failures) }
}
function Start-ProbeTest {
    if ($script:ProbeTestDepth -gt 0) { $script:ProbeTestDepth++; return }
    Send-ProbeCommand backup-state | Out-Null
    $size=(Invoke-ProbeAdb shell wm size) -join "`n";$density=(Invoke-ProbeAdb shell wm density) -join "`n"
    $script:ProbeRestoreSize=if($size -match 'Override size: (\d+x\d+)'){$Matches[1]}else{'reset'}
    $script:ProbeRestoreDensity=if($density -match 'Override density: (\d+)'){$Matches[1]}else{'reset'}
    $script:ProbeRotation=(Invoke-ProbeAdb shell settings get system user_rotation) -join ''
    $script:ProbeAutoRotation=(Invoke-ProbeAdb shell settings get system accelerometer_rotation) -join ''
    $script:ProbeCameraBaseline=if(Get-Command Get-CameraSnapshot -ErrorAction SilentlyContinue){Get-CameraSnapshot}else{$null}
    $script:ProbeTestDepth=1
}
function Stop-ProbeTest {
    if ($script:ProbeTestDepth -le 0) { return }
    $script:ProbeTestDepth--
    if ($script:ProbeTestDepth -eq 0) {
        Invoke-ProbeTest {} @(
            { Invoke-ProbeAdb shell wm size $script:ProbeRestoreSize | Out-Null },
            { Invoke-ProbeAdb shell wm density $script:ProbeRestoreDensity | Out-Null },
            { if($script:ProbeRotation -eq 'null'){Invoke-ProbeAdb shell settings delete system user_rotation | Out-Null}else{Invoke-ProbeAdb shell settings put system user_rotation $script:ProbeRotation | Out-Null} },
            { if($script:ProbeAutoRotation -eq 'null'){Invoke-ProbeAdb shell settings delete system accelerometer_rotation | Out-Null}else{Invoke-ProbeAdb shell settings put system accelerometer_rotation $script:ProbeAutoRotation | Out-Null} },
            { Send-ProbeCommand restore-state | Out-Null },
            { Restore-ProbeCamera }
        )
    }
}
function Restore-ProbeCamera {
    $original=$script:ProbeCameraBaseline
    if(-not $original -or $original.State.foreground -ne 'com.sec.android.app.camera' -or $original.State.cameraMode -eq 'unknown'){return}
    $mode=$original.State.cameraMode
    $menu=Open-CameraMoreMenu
    $node=Find-CameraNode $menu 'shooting_mode_item_button_full' ('^'+[regex]::Escape($mode)+'$')
    if(-not $node){$node=Find-CameraNode $menu 'more_grid_list_text' ('^'+[regex]::Escape($mode)+'$')}
    Tap-CameraNode $node
    if($mode -eq '사진') {
        $suffix=@{'3:4'='NORMAL';'9:16'='WIDE';'1:1'='SQUARE';'FULL'='FULL'}[$original.State.previewRatio]
        if($suffix){$menu=Open-CameraRatioMenu;Tap-CameraNode (Find-CameraNode $menu 'quick_setting_button_main' "_PICTURE_RATIO_$suffix`$")}
    }
}
