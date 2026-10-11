# Taskoday Android updates and Release signing

## Current signing identity

The repository is configured to read Release credentials from the user Gradle properties `taskodayKeystorePath`, `taskodayKeystorePassword`, `taskodayKeyAlias`, and `taskodayKeyPassword` (or the matching `TASKODAY_*` environment variables). The configured keystore path exists outside this repository. Its certificate matches the already signed `release/beta03/Taskoday-1.0-beta03.apk` certificate:

`4783e3e7f20c4bfec5e6b22fb0e3cfcaeb0dfddca3ebc380929c7fb1243a666b`

That public fingerprint is pinned in BuildConfig as an independent APK signer trust anchor. The app also checks that the manifest signer fingerprint matches the pin. Do not rotate or replace this key: Android will reject an update signed by a different certificate. Back up the keystore and its passwords using an encrypted password manager and offline encrypted backup. Never put the keystore or passwords in Git or on HarkServ.

`applicationId` is `com.harkinster.taskoday` (the Kotlin namespace remains `com.example.taskoday`). Current `versionCode` is 6 and `versionName` is `1.0-beta05`.

## Build and inspect a signed APK

From PowerShell in the repository root:

```powershell
.\gradlew.bat :app:assembleRelease
```

Gradle uses the existing user-level key configuration. The APK is at `app\build\outputs\apk\release\app-release.apk`. Inspect its certificate and package/version before any distribution:

```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk\build-tools\37.0.0"
& "$sdk\apksigner.bat" verify --verbose --print-certs app\build\outputs\apk\release\app-release.apk
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\aapt.exe" dump badging app\build\outputs\apk\release\app-release.apk
```

The SHA-256 certificate must equal the pin above. To export only the public certificate (keytool prompts for the keystore password; it is not written into the command history):

```powershell
keytool -exportcert -keystore "<configured-keystore-path>" -alias "<configured-key-alias>" -rfc -file taskoday-release-cert.pem
keytool -printcert -file taskoday-release-cert.pem
```

## Publication

No publication was performed. The referenced `ops/android-distribution/publish-windows.ps1` and the server integration guide are not present in this checkout, so the server-specific publication arguments cannot be verified here. Once the deployment tools are restored, inspect their `-WhatIf`/validation behavior and invoke the documented explicit publish command with the validated APK. Do not upload the keystore. Configure the server's expected signer fingerprint as the public SHA-256 above. Confirm `version.json` remains unavailable until the explicit publish step.

The phone connected during this integration is a Debug development device. Do not install the Release APK over it. Test a Release upgrade using a separate emulator/device with both APKs signed by the same certificate.
