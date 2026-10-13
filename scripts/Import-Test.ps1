param([string]$Serial)
. "$PSScriptRoot\App-Ui.ps1" -Serial $Serial
$folder=Join-Path $script:ProjectRoot 'artifacts\import-test'
New-Item -ItemType Directory -Force $folder | Out-Null
$checks=[System.Collections.Generic.List[object]]::new()
$created=[System.Collections.Generic.List[string]]::new()
function Read-DocumentUi {
    Invoke-ProbeAdb shell uiautomator dump /data/local/tmp/chrynalogia-import.xml | Out-Null
    [xml]$tree=(Invoke-ProbeAdb shell cat /data/local/tmp/chrynalogia-import.xml) -join ''
    return $tree.SelectNodes('//node')
}
function Tap-DocumentNode($node) {
    if(-not $node){throw 'Document node missing'}
    $node.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]' | Out-Null
    if([int]$Matches[4] -gt 2205){throw 'Document target is behind the navigation bar; scroll it into view first.'}
    Invoke-ProbeAdb shell input tap ([int](([int]$Matches[1]+[int]$Matches[3])/2)) ([int](([int]$Matches[2]+[int]$Matches[4])/2))
    Start-Sleep -Milliseconds 600
}
function Pick-TestFile([string]$Name) {
    $nodes=Read-DocumentUi
    if(-not ($nodes | Where-Object {$_.text -eq '다음에서 열기:'})) {
        Tap-DocumentNode ($nodes | Where-Object {$_.'content-desc' -eq '루트 표시'} | Select-Object -First 1)
    }
    $nodes=Read-DocumentUi
    Tap-DocumentNode ($nodes | Where-Object {$_.text -match 'S23$|^내부 저장소$|^내부 저장공간$' -and $_.'resource-id' -eq 'android:id/title'} | Select-Object -First 1)
    # DocumentsUI remembers each directory's old scroll position.
    for($reset=0;$reset -lt 4;$reset++){Invoke-ProbeAdb shell input swipe 850 650 850 1800 500;Start-Sleep -Milliseconds 250}
    foreach($wanted in @('Download','000-Chrynalogia-probe',$Name)) {
        $found=$false
        for($i=0;$i -lt 12;$i++) {
            $nodes=Read-DocumentUi
            if(-not ($nodes | Where-Object {$_.package -match 'documentsui'})){throw 'Document picker is not foreground'}
            $escaped=[regex]::Escape($wanted)
            $node=$nodes | Where-Object {($_.text -eq $wanted -and $_.'resource-id' -eq 'android:id/title') -or $_.'content-desc' -match "^$escaped(?:,|$)"} | Select-Object -First 1
            if($node -and $node.bounds -match '\[\d+,\d+\]\[\d+,(\d+)\]' -and [int]$Matches[1] -le 2205) {Tap-DocumentNode $node;$found=$true;break}
            Invoke-ProbeAdb shell input swipe 850 1800 850 1300 500
            Start-Sleep -Milliseconds 600
        }
        if(-not $found){throw "Document entry not found: $wanted"}
    }
}
function Open-GridPopup {
    $nodes=@(Read-AppNodes)
    if($nodes.Description -contains '그리드 메뉴 열기'){Tap-AppLabel '그리드 메뉴 열기'}
    Tap-AppLabel '그리드 설정 열기'
}
try {
    & $script:AdbPath @script:DeviceArguments shell ls /sdcard/Download/000-Chrynalogia-probe 2>$null | Out-Null
    if($LASTEXITCODE -eq 0){throw 'Preserve existing test folder; choose another test name.'}
    Invoke-ProbeAdb shell mkdir /sdcard/Download/000-Chrynalogia-probe
    foreach($type in @('svg','png')) {
        $destination="/sdcard/Download/000-Chrynalogia-probe/Chrynalogia-test.$type"
        & $script:AdbPath @script:DeviceArguments shell ls $destination 2>$null | Out-Null
        if($LASTEXITCODE -eq 0){throw "Test destination already exists; preserve it: $destination"}
        Invoke-ProbeAdb push (Join-Path $script:ProjectRoot "artifacts\import-fixtures\Chrynalogia-test.$type") $destination | Out-Null
        $created.Add($destination)
    }
    $baseline=(Get-CameraSnapshot).State.importCount
    $index=0
    foreach($type in @('svg','png')) {
        Invoke-ProbeAdb shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -p com.sec.android.app.camera | Out-Null
        Tap-CameraNode (Wait-CameraPreview)
        Open-GridPopup
        Tap-AppLabel '파일에서 그리드 추가' -Scroll
        Pick-TestFile "Chrynalogia-test.$type"
        $index++
        Assert-State ("Import $type through system picker and return to camera") {param($s) $s.importCount -eq $baseline+$index -and $s.foreground -eq 'com.sec.android.app.camera' -and $s.overlayAttached -and $s.selectedGrids[0] -match '^file_'}
        Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder $type)
    }
    Open-GridPopup
    Tap-AppLabel '커스텀 파일 비율 유지' -Scroll
    Assert-State 'Imported asset aspect toggle' {param($s) $s.keepAspect}
    Tap-AppLabel '닫기'
    Save-CameraSnapshot (Get-CameraSnapshot) (Join-Path $folder 'keep-aspect')
    Open-GridPopup
    Tap-AppLabel 'Chrynalogia-test.svg 삭제' -Scroll
    Assert-State 'Delete imported SVG' {param($s) $s.importCount -eq $baseline+1}
    Tap-AppLabel 'Chrynalogia-test.png 삭제' -Scroll
    Assert-State 'Delete imported bitmap' {param($s) $s.importCount -eq $baseline}
    Tap-AppLabel '커스텀 파일 비율 유지'
    Tap-AppLabel '황금비 격자' -Scroll -Up
    Tap-AppLabel '흰색' -Scroll
    Tap-AppLabel '닫기'
    Tap-AppLabel '메뉴 닫기'
} finally {
    foreach($path in $created){Invoke-ProbeAdb shell rm -f $path}
    if($created.Count -gt 0){Invoke-ProbeAdb shell rmdir /sdcard/Download/000-Chrynalogia-probe}
    Invoke-ProbeAdb shell rm -f /data/local/tmp/chrynalogia-import.xml
    $checks | ConvertTo-Json -Depth 8 | Set-Content -Encoding utf8 (Join-Path $folder 'result.json')
}
Write-Output "Passed $($checks.Count) import checks"

