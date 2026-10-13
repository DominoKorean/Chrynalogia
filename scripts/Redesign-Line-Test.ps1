param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
Start-ProbeTest
Invoke-ProbeTest {
$display=Get-ProbeDisplay
$swipeX=[int]($display.Width*0.79);$swipeBottom=[int]($display.Height*0.79);$swipeMiddle=[int]($display.Height*0.53);$swipeTop=[int]($display.Height*0.34)
$folder=Join-Path $script:ProjectRoot 'artifacts\redesign-checks'
$checks=[System.Collections.Generic.List[object]]::new()
$original=(Get-CameraSnapshot).State
$pixelScale=(Get-ProbeDisplay).DensityScale
function Set-LineSlider([string]$Label,[int]$Value,[int]$Min,[int]$Max) {
 for($i=0;$i -lt 14;$i++) {
  $node=Find-ProbeSlider @(Read-AppNodes) $Label
  if($node -and $node.Bottom -lt ($display.Height*0.87) -and $node.Bottom-$node.Top -ge (36*$display.DensityScale)){
    $point=Get-ProbeSliderPoint $node $Value $Min $Max $display.DensityScale
    Invoke-ProbeAdb shell input tap $point.X $point.Y;Start-Sleep -Milliseconds 300;return
  }
  Invoke-ProbeAdb shell input swipe $swipeX $swipeBottom $swipeX $swipeMiddle 400
  Start-Sleep -Milliseconds 700
 }
 throw "Line slider missing: $Label"
}
try {
 Invoke-ProbeAdb shell am start -n dev.chrynalogia.probe/.MainActivity | Out-Null
 Tap-AppLabel '라인'
 $hue=@(Read-AppNodes) | Where-Object {$_.Visible -and $_.Clickable -and $_.Description -eq '커스텀'} | Select-Object -First 1
 Tap-CameraNode $hue
 Assert-State 'Touch hue picker updates custom color' {param($s) ($s.lineColor -band 0xff0000) -eq 0 -and (($s.lineColor -shr 8) -band 255) -gt 240 -and ($s.lineColor -band 255) -gt 240}
 $sv=@(Read-AppNodes) | Where-Object {$_.Visible -and $_.Clickable -and $_.Description -eq '라인 색상 HEX' -and $_.Class -ne 'android.widget.EditText'} | Select-Object -First 1
 Tap-CameraNode $sv
 Assert-State 'Touch saturation/value picker updates custom color' {param($s) (($s.lineColor -shr 16) -band 255) -ge 50 -and (($s.lineColor -shr 8) -band 255) -le 150 -and ($s.lineColor -band 255) -le 150}
 Set-LineSlider '불투명도' 40 0 100
 Set-LineSlider '선 두께' 24 3 60
 Assert-State 'Redesigned opacity and thickness sliders work' {param($s) [Math]::Abs($s.lineOpacity-0.4) -lt 0.04 -and [Math]::Abs($s.lineThickness-2.4) -lt 0.11}
 Set-LineSlider '불투명도' ([int][Math]::Round($original.lineOpacity*100)) 0 100
 Set-LineSlider '선 두께' ([int][Math]::Round($original.lineThickness*10)) 3 60
 Tap-AppLabel '라인'
 Tap-AppLabel '흰색'
 Assert-State 'Original line style restored' {param($s) $s.lineColor -eq -1 -and -not $s.outline -and [Math]::Abs($s.lineOpacity-$original.lineOpacity) -lt 0.04 -and [Math]::Abs($s.lineThickness-$original.lineThickness) -lt 0.11}
 Tap-AppLabel '그리드'
 Tap-AppLabel '삼성 카메라 열기'
 Wait-CameraPreview | Out-Null
} finally {$checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'line-checks.json')}
Write-Output "Passed $($checks.Count) line input checks"

} @({ Stop-ProbeTest })
