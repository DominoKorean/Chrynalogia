$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Test-State.ps1"
$checks = 0
function Check([bool]$Value, [string]$Name) { if (-not $Value) { throw "Failed: $Name" }; $script:checks++ }
$node = [pscustomobject]@{ Class='dev.chrynalogia.probe.KitSlider'; Description='Opacity'; Visible=$true; Left=100; Right=500; Top=200; Bottom=248 }
Check ($null -ne (Find-ProbeSlider @($node) 'Opacity')) 'current slider is found'
$point = Get-ProbeSliderPoint $node 50 0 100 1
Check ($point.X -eq 300 -and $point.Y -eq 224) 'midpoint'
$point = Get-ProbeSliderPoint $node 0 0 100 2
Check ($point.X -eq 124) 'density-aware inset'
$node.Visible=$false
Check ($null -eq (Find-ProbeSlider @($node) 'Opacity')) 'invisible slider is ignored'
$node.Visible=$true
$bar=[pscustomobject]@{Description='Line';Visible=$true;Clickable=$true;Left=80;Right=520;Top=220;Bottom=280}
Check ($null -eq (Find-ProbeSlider @($node,$bar) 'Opacity')) 'navigation-obscured slider is ignored'
$events = [System.Collections.Generic.List[string]]::new()
try { Invoke-ProbeTest { throw 'body error' } @({ $events.Add('first'); throw 'restore error' }, { $events.Add('last') }); throw 'expected failure' }
catch { Check ($events.Count -eq 2) 'all restoration steps execute'; Check ($_.Exception.ToString().Contains('body error') -and $_.Exception.ToString().Contains('restore error')) 'both errors survive' }
Get-ChildItem "$PSScriptRoot\*.ps1" | ForEach-Object { $tokens=$null; $errors=$null; [void][System.Management.Automation.Language.Parser]::ParseFile($_.FullName,[ref]$tokens,[ref]$errors); Check ($errors.Count -eq 0) "syntax $($_.Name)" }
Write-Output "PASS: $checks script checks"
