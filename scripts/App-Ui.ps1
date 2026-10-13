param([string]$Serial)
. "$PSScriptRoot\Camera-Ui.ps1" -Serial $Serial
. "$PSScriptRoot\Test-State.ps1"
function Read-AppNodes {
    Send-ProbeCommand capture | Out-Null
    $tree=(Invoke-ProbeAdb shell run-as dev.chrynalogia.probe cat files/app-ui.txt) -join "`n"
    foreach ($line in ($tree -split "`n")) {
        if ($line -match 'id=(\S+) bounds=\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\] visible=(true|false) clickable=(true|false) selected=(true|false) checked=(true|false) text=(.*?) desc=(.*)$') {
            [pscustomobject]@{ Class=($line.Trim() -split ' ')[0];Left=[int]$Matches[2];Top=[int]$Matches[3];Right=[int]$Matches[4];Bottom=[int]$Matches[5];Visible=$Matches[6] -eq 'true';Clickable=$Matches[7] -eq 'true';Selected=$Matches[8] -eq 'true';Checked=$Matches[9] -eq 'true';Text=$Matches[10];Description=$Matches[11] }
        }
    }
}
function Tap-AppLabel {
    param([string]$Label,[switch]$Scroll,[switch]$Up,[switch]$EnsureChecked)
    $display=Get-ProbeDisplay
    $sx=[int]($display.Width*0.79);$sy=[int]($display.Height*0.79);$sm=[int]($display.Height*0.53);$st=[int]($display.Height*0.34)
    for ($i=0;$i -lt 22;$i++) {
        $nodes=@(Read-AppNodes)
        $node=$nodes | Where-Object { $_.Visible -and $_.Clickable -and ($_.Text -eq $Label -or $_.Description -eq $Label) -and $_.Bottom-$_.Top -ge (30*$display.DensityScale) } | Select-Object -First 1
        if($node -and $Label -notin @('그리드','라인','설정','Grid','Line','Settings')) {
            $centerX=($node.Left+$node.Right)/2
            $bar=$nodes | Where-Object {$_.Visible -and $_.Clickable -and $_.Description -in @('그리드','라인','설정','Grid','Line','Settings') -and $centerX -ge $_.Left -and $centerX -le $_.Right} | Select-Object -First 1
            if($bar -and $node.Bottom -gt $bar.Top -and $node.Top -lt $bar.Bottom) {
                if($bar.Top-12-$node.Top -ge 80){$node.Bottom=$bar.Top-12}else{$node=$null}
            }
        }
        if ($node) { if(-not ($EnsureChecked -and ($node.Checked -or $node.Selected))){Tap-CameraNode $node};return }
        $state=(Get-CameraSnapshot).State
        if($state.foreground -notin @('com.sec.android.app.camera','dev.chrynalogia.probe')) {throw "App left foreground while looking for: $Label"}
        if (-not $Scroll) { if($i -lt 3){Start-Sleep -Milliseconds 250;continue};throw "UI label missing: $Label" }
        if ($Up) { Invoke-ProbeAdb shell input swipe $sx $st $sx $sm 500 }
        else { Invoke-ProbeAdb shell input swipe $sx $sy $sx $sm 500 }
        Start-Sleep -Milliseconds 700
    }
    throw "Scrolled UI label missing: $Label"
}
function Assert-State {
    param([string]$Name,[scriptblock]$Predicate)
    for($attempt=0;$attempt -lt 8;$attempt++) {
        $state=(Get-CameraSnapshot).State
        if (& $Predicate $state) {break}
        Start-Sleep -Milliseconds 250
    }
    if (-not (& $Predicate $state)) { $state | ConvertTo-Json -Depth 6 | Set-Content (Join-Path $folder 'failure.json');throw "Failed: $Name" }
    $checks.Add([pscustomobject]@{Name=$Name;Passed=$true;State=$state})
}
function Wait-CameraPreview {
    for($attempt=0;$attempt -lt 16;$attempt++) {
        $node=Find-CameraNode (Get-CameraSnapshot) 'camera_preview'
        if($node){return $node}
        Start-Sleep -Milliseconds 300
    }
    throw 'Camera preview did not become available'
}

