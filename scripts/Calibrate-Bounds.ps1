param(
    [Parameter(Mandatory = $true)][int]$Left,
    [Parameter(Mandatory = $true)][int]$Top,
    [Parameter(Mandatory = $true)][int]$Right,
    [Parameter(Mandatory = $true)][int]$Bottom,
    [string]$Serial
)
. "$PSScriptRoot\Device.ps1" -Serial $Serial
Send-ProbeCommand bounds @('--ei', 'left', "$Left", '--ei', 'top', "$Top", '--ei', 'right', "$Right", '--ei', 'bottom', "$Bottom")
