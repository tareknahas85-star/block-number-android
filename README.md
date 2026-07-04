# Block Number — حظر الأرقام

Android call-blocking app using the official `CallScreeningService` API (no root, no dialer replacement). Material Design 3 UI, Arabic + English (RTL).

## Features (v2.0)
- Block incoming calls from numbers **not in contacts** (toggle)
- **Local blacklist with wildcards** — `*` = any digits, `#` = one digit (e.g. `+9665*`)
- **Offline spam-number database** — blocks numbers with negative rating; updatable from a remote CSV (daily auto-update or manual)
- **Caller info notification** while ringing (spam rating/category, or unknown-caller notice)
- Optional notification when a call is blocked
- Blocked-call log with **block reason**; tap an entry to blacklist or copy the number
- Per-SIM control on dual-SIM devices
- Block hidden/private numbers
- Material 3 UI: bottom navigation (Home / Blacklist / Log), status card, dynamic color on Android 12+

## Spam database format
Hosted as static files (default: `https://raw.githubusercontent.com/tareknahas85-star/block-number-data/main`):
- `version.txt` — single line, e.g. `2026-07-04`
- `spamdb.csv` — `number,category,negative,positive,neutral,name` (one per line, `#` comments allowed)

The CSV is only downloaded when `version.txt` changes. A number is auto-blocked when `negative >= 2 × positive` and the "Block numbers with negative rating" toggle is on.

## Requirements
- Android 10 (API 29) or newer
- Permissions: `READ_CONTACTS`, `READ_PHONE_STATE`, `POST_NOTIFICATIONS` (13+), `INTERNET` + the **Call Screening role**

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
`ScreeningService` receives every incoming call before it rings and walks a decision chain: blocking off / SIM excluded → allow; hidden → block if enabled; **in contacts → always allow**; blacklist match → block; negative spam rating → block if enabled; not in contacts → block if enabled; otherwise allow and (optionally) show a caller-info notification. Every block is logged with its reason. Fail-open if contacts permission is missing.
