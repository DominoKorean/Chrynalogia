$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME -or -not (Test-Path (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
    $java = Get-Command java -ErrorAction Stop
    $env:JAVA_HOME = Split-Path (Split-Path $java.Source -Parent) -Parent
}
$env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools\gradle-home'
$signing = Join-Path $projectRoot '.signing'
$keystore = Join-Path $signing 'chrynalogia-release.p12'
$passwordFile = Join-Path $signing 'password.txt'
$buildTools = Join-Path $projectRoot '.tools\android-sdk\build-tools\36.0.0'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
$signer = Join-Path $buildTools 'apksigner.bat'
$aligner = Join-Path $buildTools 'zipalign.exe'
foreach ($tool in @($keytool,$signer,$aligner)) {
    if (-not (Test-Path -LiteralPath $tool)) { throw "Required tool missing: $tool" }
}
New-Item -ItemType Directory -Force $signing | Out-Null
# Keep the private key and its password out of Git and accessible only to this account.
$identity = [System.Security.Principal.WindowsIdentity]::GetCurrent().Name
& icacls.exe $signing /inheritance:r /grant:r "${identity}:(OI)(CI)F" | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Unable to protect release signing directory' }
if ((Test-Path -LiteralPath $keystore) -xor (Test-Path -LiteralPath $passwordFile)) {
    throw 'Signing files are incomplete. Restore the existing key/password pair; do not replace the release key.'
}
if (-not (Test-Path -LiteralPath $keystore)) {
    $bytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    $secret = [Convert]::ToBase64String($bytes)
    [System.IO.File]::WriteAllText($passwordFile,$secret,[System.Text.UTF8Encoding]::new($false))
    & $keytool -genkeypair -keystore $keystore -storetype PKCS12 -alias chrynalogia -keyalg RSA -keysize 3072 -validity 36500 -dname 'CN=Chrynalogia' -storepass:file $passwordFile -keypass:file $passwordFile
    if ($LASTEXITCODE -ne 0) { throw 'Release key generation failed' }
}
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:assembleRelease :app:testDebugUnitTest :app:lintRelease --offline --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Release build or verification failed' }
    $metadata = Get-Content 'app\build\outputs\apk\release\output-metadata.json' -Raw | ConvertFrom-Json
    if ($metadata.elements.Count -ne 1) { throw 'Expected one universal release APK' }
    $version = $metadata.elements[0].versionName
    if ($version -notmatch '^\d+\.\d+\.\d+$') { throw 'Release version must use major.minor.patch' }
    $unsigned = Join-Path $projectRoot ('app\build\outputs\apk\release\'+$metadata.elements[0].outputFile)
    $aligned = Join-Path $signing 'aligned.apk'
    $artifacts = Join-Path $projectRoot 'artifacts'
    New-Item -ItemType Directory -Force $artifacts | Out-Null
    $output = Join-Path $artifacts "Chrynalogia-v$version.apk"
    try {
        & $aligner -P 16 -f 4 $unsigned $aligned
        if ($LASTEXITCODE -ne 0) { throw 'APK alignment failed' }
        # PKCS12 uses the store password for the key; do not consume this file twice.
        & $signer sign --ks $keystore --ks-key-alias chrynalogia --ks-pass "file:$passwordFile" --v4-signing-enabled false --out $output $aligned
        if ($LASTEXITCODE -ne 0) { throw 'APK signing failed' }
        & $signer verify --verbose --print-certs $output
        if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
        $hash = (Get-FileHash -LiteralPath $output -Algorithm SHA256).Hash
        "$hash  Chrynalogia-v$version.apk" | Set-Content -LiteralPath "$output.sha256" -Encoding ascii
        Write-Output "Release APK: $output"
    } finally {
        if (Test-Path -LiteralPath $aligned) { Remove-Item -LiteralPath $aligned -Force }
    }
} finally { Pop-Location }
