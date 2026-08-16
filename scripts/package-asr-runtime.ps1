param(
    [string]$Version = "v1"
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$modelsDir = Join-Path $repoRoot "app\src\main\assets\models"
$jniLibsDir = Join-Path $repoRoot "app\src\main\jniLibs"
$artifactDir = Join-Path $repoRoot "local-artifacts"
$stagingDir = Join-Path $artifactDir "asr-runtime-staging"
$archivePath = Join-Path $artifactDir "nextthing-asr-runtime-$Version.zip"
$checksumPath = "$archivePath.sha256"

if (-not (Test-Path -LiteralPath $modelsDir)) {
    throw "Missing models directory: $modelsDir"
}

if (-not (Test-Path -LiteralPath $jniLibsDir)) {
    throw "Missing jniLibs directory: $jniLibsDir"
}

New-Item -ItemType Directory -Path $artifactDir -Force | Out-Null

if (Test-Path -LiteralPath $stagingDir) {
    $resolved = (Resolve-Path -LiteralPath $stagingDir).Path
    if (-not $resolved.StartsWith($artifactDir, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw "Refusing to clean staging directory outside local-artifacts: $resolved"
    }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}

New-Item -ItemType Directory -Path (Join-Path $stagingDir "app\src\main\assets") -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $stagingDir "app\src\main") -Force | Out-Null

Copy-Item -LiteralPath $modelsDir -Destination (Join-Path $stagingDir "app\src\main\assets") -Recurse -Force
Copy-Item -LiteralPath $jniLibsDir -Destination (Join-Path $stagingDir "app\src\main") -Recurse -Force

if (Test-Path -LiteralPath $archivePath) {
    Remove-Item -LiteralPath $archivePath -Force
}
if (Test-Path -LiteralPath $checksumPath) {
    Remove-Item -LiteralPath $checksumPath -Force
}

Compress-Archive -LiteralPath (Join-Path $stagingDir "app") -DestinationPath $archivePath -CompressionLevel Optimal

$hash = Get-FileHash -Algorithm SHA256 -LiteralPath $archivePath
"$($hash.Hash.ToLowerInvariant())  $(Split-Path -Leaf $archivePath)" | Set-Content -Encoding ascii -LiteralPath $checksumPath

$archive = Get-Item -LiteralPath $archivePath
Write-Output "Created: $archivePath"
Write-Output ("Size: {0:N2} MB" -f ($archive.Length / 1MB))
Write-Output "SHA256: $($hash.Hash.ToLowerInvariant())"
