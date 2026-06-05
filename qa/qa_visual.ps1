param(
    [string] $FlowPath = ".maestro\taskoday_visual.yaml"
)

$ErrorActionPreference = "Stop"

cd C:\Users\DrHarkinster\AndroidStudioProjects\taskoday

$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-21.0.5.11-hotspot"
$env:GRADLE_USER_HOME="$env:USERPROFILE\.gradle"
$env:PATH="$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;C:\maestro\bin;$env:PATH"

function Invoke-Checked {
    param(
        [Parameter(Mandatory = $true)]
        [string] $FilePath,
        [Parameter(ValueFromRemainingArguments = $true)]
        [string[]] $Arguments
    )

    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$FilePath a echoue avec le code $LASTEXITCODE."
    }
}

Write-Host "JAVA_HOME = $env:JAVA_HOME"
java -version

if (!(Test-Path "$env:JAVA_HOME\bin\jlink.exe")) {
    throw "jlink.exe introuvable dans JAVA_HOME. Verifie le JDK."
}

Write-Host "jlink.exe trouve."

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($null -eq $adbCommand) {
    throw "adb introuvable. Verifie le SDK Android ou ajoute platform-tools au PATH."
}

$maestroCommand = Get-Command maestro -ErrorAction SilentlyContinue
if ($null -eq $maestroCommand) {
    throw "maestro introuvable. Verifie l'installation dans C:\maestro\bin."
}

Invoke-Checked adb version
Invoke-Checked maestro --version

Invoke-Checked .\gradlew.bat --no-daemon :app:assembleDebug --console=plain

Invoke-Checked adb devices
Invoke-Checked adb install -r app\build\outputs\apk\debug\app-debug.apk

Remove-Item -Recurse -Force qa\screenshots -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force qa\screenshots | Out-Null

Write-Host "Flow Maestro = $FlowPath"
Invoke-Checked maestro test $FlowPath

Write-Host "Screenshots generes dans qa/screenshots/"
Get-ChildItem qa\screenshots -File -ErrorAction SilentlyContinue | Select-Object FullName, Length, LastWriteTime
