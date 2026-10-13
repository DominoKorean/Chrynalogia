param([string]$Serial)
. "$PSScriptRoot\Device.ps1" -Serial $Serial
function System-Nodes {
    Invoke-ProbeAdb shell uiautomator dump /data/local/tmp/chrynalogia-reconnect.xml | Out-Null
    [xml]$tree=(Invoke-ProbeAdb shell cat /data/local/tmp/chrynalogia-reconnect.xml) -join ''
    return $tree.SelectNodes('//node')
}
function Tap-SystemText([string]$Text) {
    $node=System-Nodes | Where-Object {$_.text -eq $Text} | Select-Object -First 1
    if(-not $node) {throw "Expected system label missing: $Text"}
    if($node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]'){throw 'Invalid node bounds'}
    Invoke-ProbeAdb shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2))
    Start-Sleep -Milliseconds 400
}
# Use the Settings UI to restart only the app's already-authorized service.
Invoke-ProbeAdb shell am start -a android.settings.ACCESSIBILITY_SETTINGS | Out-Null
Start-Sleep -Milliseconds 400
$current=System-Nodes
if(-not ($current | Where-Object {$_.text -eq 'Chrynalogia 바로가기'})) {
    if(-not ($current | Where-Object {$_.text -eq 'Activity Record'})) {Tap-SystemText '설치된 앱'}
    Tap-SystemText 'Chrynalogia'
}
$nodes=System-Nodes
if(-not ($nodes | Where-Object {$_.text -eq 'Chrynalogia'})) {throw 'Not on Chrynalogia accessibility page'}
$switch=$nodes | Where-Object {$_.class -eq 'android.widget.Switch'} | Select-Object -First 1
if(-not $switch) {throw 'Service switch missing'}
$switch.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]' | Out-Null
$x=[int](([int]$Matches[1]+[int]$Matches[3])/2);$y=[int](([int]$Matches[2]+[int]$Matches[4])/2)
if($switch.checked -eq 'true') {Invoke-ProbeAdb shell input tap $x $y;Start-Sleep -Milliseconds 250;Tap-SystemText '끄기'}
Invoke-ProbeAdb shell input tap $x $y
Start-Sleep -Milliseconds 300
Tap-SystemText '허용'
Start-Sleep -Milliseconds 600
Send-ProbeCommand capture
Invoke-ProbeAdb shell rm /data/local/tmp/chrynalogia-reconnect.xml
