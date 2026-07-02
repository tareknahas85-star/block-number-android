# Block Number — حظر الأرقام

Android app that automatically blocks incoming calls from numbers **not in your contacts**, using the official `CallScreeningService` API (no root, no dialer replacement).

## Features
- Blocks any incoming call whose number is not saved in contacts
- Per-SIM control on dual-SIM devices (e.g. SIM 1 blocks unknown callers, SIM 2 receives everything)
- Optional blocking of hidden/private numbers
- Log of blocked calls (number + time), with clear option
- On/off toggle
- Arabic + English UI (RTL supported)

## Requirements
- Android 10 (API 29) or newer
- Permissions: `READ_CONTACTS` + the **Call Screening role** (requested on first launch)

## Build
### GitHub Actions (recommended)
Push to GitHub — the `Build APK` workflow produces `app-debug.apk` as an artifact.

### Locally
```bash
gradle assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```
Requires JDK 17 and Android SDK (compileSdk 35).

## How it works
`ScreeningService` receives every incoming call before it rings. It looks the number up via `ContactsContract.PhoneLookup`; if not found, the call is rejected silently and recorded in a local SQLite log.  If contacts permission is missing, calls are allowed (fail-open) to avoid blocking legitimate calls. On dual-SIM devices, the UI shows one switch per SIM (via `TelecomManager` phone accounts) and the service checks the incoming call's `PhoneAccountHandle` against the per-SIM setting.
