# Taskoday Android update QA

The `qa` build type uses the same application ID and Release signing config as `release`, so QA version 6 can be upgraded to QA version 7 by Android. Use only the dedicated `Taskoday_Update_Isolated` AVD. Do not install these APKs over a personal phone or publish them to production.

QA builds use `https://10.0.2.2:8443/taskoday-test/version.json`, a QA-only TLS trust anchor, short test-only check intervals, and a separate `taskoday-test-api` base URL. The local HTTPS fixture binds to `127.0.0.1` on Windows; only the Android emulator gateway can reach it. No HarkServ QA endpoint or production manifest is changed.

## Build the compatible APK pair

Generate the local QA TLS identity once. Its private key stays under `%LOCALAPPDATA%\TaskodayQA\tls`; only its public certificate is included in the QA source set.

```powershell
python tools/create_taskoday_qa_tls_cert.py
.\gradlew.bat :app:assembleQa "-PtaskodayQaVersionCode=6" "-PtaskodayQaVersionName=1.0-beta05-qa"
Copy-Item app/build/outputs/apk/qa/app-qa.apk app/build/qa-artifacts/taskoday-qa-v6.apk -Force
.\gradlew.bat :app:assembleQa "-PtaskodayQaVersionCode=7" "-PtaskodayQaVersionName=1.0-beta06-qa"
Copy-Item app/build/outputs/apk/qa/app-qa.apk app/build/qa-artifacts/taskoday-qa-v7.apk -Force
```

Before testing, check both APKs with `aapt dump badging` and `apksigner verify --print-certs`; require package `com.harkinster.taskoday`, codes 6 and 7, and the same approved release signer.

## Local HTTPS test flow

Create a temporary state file containing `unavailable`, then start the fixture with the QA v7 APK and local TLS identity:

```powershell
$qa = Join-Path $env:LOCALAPPDATA 'TaskodayQA\tls'
$state = Join-Path $env:TEMP 'taskoday-qa-state.txt'
Set-Content -LiteralPath $state -Value 'unavailable' -NoNewline
python tools/taskoday_qa_update_server.py `
  --apk app/build/qa-artifacts/taskoday-qa-v7.apk `
  --certificate (Join-Path $qa 'taskoday-qa-emulator.crt.pem') `
  --private-key (Join-Path $qa 'taskoday-qa-emulator.key.pem') `
  --state-file $state `
  --throttle-bps 4194304
```

Install v6 only on the isolated AVD while the state is `unavailable`. Open Taskoday and seed non-personal test tasks/preferences. Then set the state file to `available`, wait at least five seconds, and relaunch Taskoday. Accept or cancel Android's installer dialog as needed. Verify the installed code is 7 and confirm local test data remains. Use a separate clean AVD snapshot to test denying the unknown-source permission and recovering through Settings.

The fixture accepts only the version manifest and the APK path it derives from the actual file digest. It emits a test-only changelog and never writes to production. Its TLS key remains outside the repository. Stop the fixture with Ctrl+C. The public production manifest remains untouched. The public QA path on HarkServ was checked and currently returns 404.
