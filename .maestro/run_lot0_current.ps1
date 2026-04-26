param(
    [switch]$InstallDebug
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$emulator = Join-Path $env:LOCALAPPDATA "Android\Sdk\emulator\emulator.exe"
$maestro = Join-Path $projectRoot ".maestro-cli\maestro\bin\maestro.bat"

$flows = @(
    ".maestro\flows\01-splash.yaml",
    ".maestro\flows\02-home.yaml",
    ".maestro\flows\03-missions.yaml",
    ".maestro\flows\04-quetes.yaml",
    ".maestro\flows\05-profil.yaml"
)

function Ensure-Device {
    & $adb start-server | Out-Null
    $devices = & $adb devices
    if ($devices -notmatch "device$") {
        Write-Host "No Android device detected. Starting emulator..."
        Start-Process -FilePath $emulator -ArgumentList "-avd Pixel_3a_API_30_x86" | Out-Null
        $deadline = (Get-Date).AddMinutes(3)
        do {
            Start-Sleep -Seconds 5
            $devices = & $adb devices
            $ready = $devices | Select-String "device$"
            if ($ready) { break }
        } while ((Get-Date) -lt $deadline)
        if (-not $ready) {
            throw "No ADB device available after 3 minutes."
        }
    }

    $bootDeadline = (Get-Date).AddMinutes(2)
    do {
        $boot = (& $adb shell getprop sys.boot_completed).Trim()
        if ($boot -eq "1") { break }
        Start-Sleep -Seconds 3
    } while ((Get-Date) -lt $bootDeadline)
    if ($boot -ne "1") {
        throw "Android emulator/device is not fully booted."
    }
}

function Wake-Device {
    & $adb shell input keyevent 224 | Out-Null
    & $adb shell input keyevent 82 | Out-Null
}

if ($InstallDebug) {
    Write-Host "Installing debug APK..."
    Push-Location $projectRoot
    try {
        & ".\gradlew" ":app:installDebug"
    } finally {
        Pop-Location
    }
}

Ensure-Device

Push-Location $projectRoot
try {
    foreach ($flow in $flows) {
        Wake-Device
        Write-Host "Running $flow"
        & $maestro test $flow --test-output-dir "docs\visual\current"
    }
} finally {
    Pop-Location
}

Write-Host "Lot 0 screenshots generated in docs\visual\current\screenshots"
