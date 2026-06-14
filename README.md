# eSIM Keeper

English | [简体中文](README.zh-CN.md)

![Android verification](https://github.com/chingluka0/esim-keeper/actions/workflows/android.yml/badge.svg)

eSIM Keeper is a free, offline-first Android app for tracking eSIM renewal dates, reminders, and balances.

## Screenshots

<p align="center">
  <img src="docs/images/home.jpg" width="220" />
  <img src="docs/images/home-dark.jpg" width="220" />
  <img src="docs/images/add-card.jpg" width="220" />
  <img src="docs/images/billing.jpg" width="220" />
</p>

<p align="center">
  <em>Card list (light / dark) · Add card · Number billing with import preset</em>
</p>

## Features

- Record eSIM name, phone number, country or region, balance, start date, and expiry date.
- Track fixed renewal cycles (the default rule) or a fixed expiry date, and mark a card as renewed.
- Filter cards by all, expiring soon, or expired, and show the remaining days with a progress bar.
- Record per-number billing rates, with built-in presets such as CTExcel and giffgaff.
- Sort cards by expiry date, creation time, or name.
- Export and import data as JSON backups.
- Read local SIM/eSIM information when the user grants phone permissions.
- Add renewal reminders to the system calendar without requesting calendar read/write permissions.
- English and Chinese UI resources, plus dark mode.
- Works offline and does not request network access.

## Number billing

Each card can record its own billing rates for outgoing calls, incoming calls,
sent SMS, received SMS, and data. Rates are free text (e.g. `£1/min`, `30p/SMS`,
`0.5p/MB`, `Free`), so any currency or note works.

The billing dialog also offers **Import preset**: pick a country (China for now)
to apply a common rate template. Built-in presets currently include CTExcel and
giffgaff; imported values are filled into the current card and can still be
edited by hand. More presets are planned — suggestions via Issues or Pull
Requests are welcome.

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

## Roadmap

- More number-billing presets (more countries, regions, carriers, and travel eSIMs).
- Better card display and sorting.
- More country/region detection rules.
- Smoother backup and import experience.

## Contributing & feedback

Ideas, questions, and suggestions are welcome — please open an Issue or a Pull
Request. This project started as a way to solve my own headaches managing
overseas numbers and renewal rules; I hope it helps you too.

## AI-assisted development

This project is built largely with AI assistance while I learn and improve it
along the way. Some of the code, design, and wording may still be rough — feedback
is very welcome, and thanks for your understanding.

## Acknowledgements

Thanks to fireflies1145 for contributing the PR that helped shape the card sorting, backup and restore, DataStore settings, and Android dependency upgrade work.

## Disclaimer

Inspired by SIMHUB. This app is an independent project and is not official, affiliated with, endorsed by, or sponsored by SIMHUB.
