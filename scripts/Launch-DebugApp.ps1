$ErrorActionPreference = "Stop"

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adb)) {
    throw "adb.exe not found at $adb"
}

& $adb devices
& $adb shell monkey -p com.nextthing.app -c android.intent.category.LAUNCHER 1
