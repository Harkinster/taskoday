# Maestro flows - baseline screenshots

## Prerequisites
- Android emulator running.
- Debug app installed (`com.example.taskoday`).

## Commands
Install/update app:

```powershell
.\gradlew :app:installDebug
```

Run all baseline captures:

```powershell
.\.maestro-cli\maestro\bin\maestro.bat test .maestro\flows --test-output-dir docs\visual\current
```

Recommended (sequential + device wake handling):

```powershell
.\.maestro\run_lot0_current.ps1
```

With APK install before captures:

```powershell
.\.maestro\run_lot0_current.ps1 -InstallDebug
```

Run one flow only:

```powershell
.\.maestro-cli\maestro\bin\maestro.bat test .maestro\flows\02-home.yaml --test-output-dir docs\visual\current
```

## Notes
- The bottom navigation taps in `03/04/05` are calibrated for `Pixel_3a_API_30_x86` (1080x2220).
- If you use another emulator/device size, update the `point` values in those flows.
