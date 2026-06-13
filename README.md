# eSIM Keeper

![Android verification](https://github.com/chingluka0/esim-keeper/actions/workflows/android.yml/badge.svg)

eSIM Keeper is a free, offline-first Android app for tracking eSIM renewal dates, reminders, and balances.

## Features

- Record eSIM name, phone number, country or region, balance, start date, and expiry date.
- Track fixed renewal cycles and mark a card as renewed.
- Filter cards by all, expiring soon, expired, or long-term renewal.
- Read local SIM/eSIM information when the user grants phone permissions.
- Add renewal reminders to the system calendar without requesting calendar read/write permissions.
- English and Chinese UI resources.
- Works offline and does not request network access.

## Privacy

The app stores card records locally on the device with Room/SQLite.

Requested permissions:

- `READ_PHONE_STATE`: used only when importing local SIM/eSIM information.
- `READ_PHONE_NUMBERS`: used only when the system exposes a phone number for local SIM/eSIM import.

The app does not request `INTERNET` permission.

## Build

Requirements:

- JDK 17
- Android SDK with compile SDK 35

Build and test:

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Verification

The repository includes a GitHub Actions workflow that runs on every push and pull request:

- `testDebugUnitTest`
- `assembleDebug`

To verify locally that the APK does not request network permission:

```bash
aapt dump permissions app/build/outputs/apk/debug/app-debug.apk
```

Expected permissions are limited to phone-state import permissions and Android's generated app-internal receiver permission. There should be no `android.permission.INTERNET`.

## Disclaimer

Inspired by SIMHUB. This app is an independent project and is not official, affiliated with, endorsed by, or sponsored by SIMHUB.
