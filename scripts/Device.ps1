param([string]$Serial)
$ErrorActionPreference = 'Stop'
$script:ProjectRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot\Test-State.ps1"
$script:AdbPath = Join-Path $script:ProjectRoot '.tools\android-sdk\platform-tools\adb.exe'
if (-not (Test-Path $script:AdbPath)) {
    $found = Get-Command adb -ErrorAction SilentlyContinue
    if (-not $found) { throw 'ADB is missing. Run scripts\Bootstrap.ps1 first.' }
    $script:AdbPath = $found.Source
}
$script:DeviceArguments = @()
if ($Serial) { $script:DeviceArguments = @('-s', $Serial) }
else {
    $lines = & $script:AdbPath devices
    $devices = @($lines | Where-Object { $_ -match '^\S+\s+device$' })
    if ($devices.Count -ne 1) {
        throw "Expected exactly one authorized device; found $($devices.Count). Check USB debugging/authorization, or pass -Serial."
    }
    $script:DeviceArguments = @('-s', ($devices[0] -split '\s+')[0])
}
function Invoke-ProbeAdb {
    & $script:AdbPath @script:DeviceArguments @args
    if ($LASTEXITCODE -ne 0) { throw "ADB failed: $($args -join ' ')" }
}
function Send-ProbeCommand {
    param([string]$Command, [string[]]$ExtraArguments = @())
    $result=Invoke-ProbeAdb shell am broadcast --receiver-foreground -a dev.chrynalogia.probe.DEBUG `
        -n dev.chrynalogia.probe/.DebugReceiver --es command $Command @ExtraArguments
    if (($result -join "`n") -match 'result=([1-9]\d*)') { throw "Probe command failed: $($result -join ' ')" }
    $result
}
