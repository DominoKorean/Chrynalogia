param([switch]$AcceptSdkLicenses)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$tools = Join-Path $projectRoot '.tools'
$sdk = Join-Path $tools 'android-sdk'
$manager = Join-Path $tools 'commandline\cmdline-tools\bin\sdkmanager.bat'
New-Item -ItemType Directory -Force $tools | Out-Null
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $java = Get-Command java -ErrorAction Stop
    $env:JAVA_HOME = Split-Path (Split-Path $java.Source -Parent) -Parent
}
if (-not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) { throw 'Install a JDK 17 or newer first.' }
if (-not (Test-Path $manager)) {
    $url = 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip'
    $zip = Join-Path $tools 'commandline-tools.zip'
    Invoke-WebRequest -Uri $url -OutFile $zip
    Expand-Archive -LiteralPath $zip -DestinationPath (Join-Path $tools 'commandline') -Force
}
if ($AcceptSdkLicenses) {
    1..100 | ForEach-Object { 'y' } | & $manager "--sdk_root=$sdk" --licenses
} else { & $manager "--sdk_root=$sdk" --licenses }
if ($LASTEXITCODE -ne 0) { throw 'SDK licenses were not accepted.' }
& $manager "--sdk_root=$sdk" 'platform-tools' 'platforms;android-37.0' 'build-tools;36.0.0'
if ($LASTEXITCODE -ne 0) { throw 'SDK installation failed.' }
"sdk.dir=$($sdk.Replace('\','/').Replace(':','\:'))" | Set-Content (Join-Path $projectRoot 'local.properties')
Write-Output "SDK ready: $sdk"
