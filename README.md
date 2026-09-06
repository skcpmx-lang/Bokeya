# বকেয়া — Bokeya

**ধার, বকেয়া, কিস্তি ও আয়-ব্যয়ের সহজ হিসাব।**

Bokeya is a native Android app for tracking what you owe and what people owe you — shop credit
(বাকি), personal debt, bank/NGO loans, EMIs, and everyday income and expenses. It is offline-first
and privacy-first: no ads, no analytics, no accounts, and no `INTERNET` permission. Every taka you
record stays on your phone.

- **Package:** `com.shohankhan.bokeya`
- **Version:** 1.0.0
- **Developer:** Shohan Khan · <helloiamshohan@gmail.com>

## Features

**Dashboard** — dynamic Bangla greeting, hero "মোট বকেয়া" card, আমি পাব / আমি দেব direction split,
financial-health indicator (a personal organization hint, never a credit score), আজকের হিসাব,
upcoming payments, overdue centre, and local rules-based insights computed on device.

**Accounts** — shops with item-level purchase tracking (name, quantity, unit, unit price),
personal debt and receivables tied to people, bank/NGO loans with generated installment schedules,
and EMIs with progress. Archive, pause, edit and delete are all supported.

**Money & payments** — a partial-payment engine with overpayment trimming, oldest-first allocation
across installments, payment methods (Cash, bKash, Nagad, Rocket, Card, Bank), and a payment
planner that buckets everything into overdue / today / this week / this month with a suggested
review order.

**Cashflow** — income and expenses with Bangla categories, month navigation, category breakdown,
unified transaction timeline, bulk select and delete, global search, calendar view with day
markers, goals and recurring entries.

**Reports & data** — on-device reports over any date range with PDF and CSV export, CSV import with
duplicate protection, and full JSON backup/restore with a schema version and a preview before a
destructive restore.

**Platform** — WorkManager daily digest and per-payment reminders (grouped, with snooze and
"পরিশোধ করেছি" actions, rescheduled after reboot), four Glance home-screen widgets, app shortcuts,
app lock with PIN + biometric, optional screenshot blocking, Material 3 dynamic theming with a
premium dark mode, and adaptive launcher icon and splash.

## Money handling

All amounts are stored as an integral number of **poisha** in a `@JvmInline value class Money`.
No financial value ever touches a floating-point type. Installment splits push the rounding
remainder into the final installment so a plan always sums back to the exact total.

## Architecture

Clean-ish architecture kept deliberately small:

```
core/       Money, formatting (Bangla digits, ৳ lakh/crore grouping, dates), schedule engine
domain/     enums, snapshots, InsightEngine (deterministic, local, no network)
data/       Room entities + DAOs, BokeyaRepository, DataStore settings
ui/         Compose theme, reusable components, Navigation, ViewModels, screens
notifications/ widget/ backup/ security/
```

Room runs with explicit migrations only — destructive fallback is intentionally disabled so a
schema change can never wipe someone's financial history.

## Build

```bash
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release-unsigned.apk
```

Requires JDK 17 and the Android SDK (compileSdk 35, minSdk 24). CI (`.github/workflows/build.yml`)
runs the tests, builds both variants, verifies the APKs with `aapt2` and `apksigner`, and publishes
them to the GitHub release.

### Release signing

No keystore is committed and none is invented. To produce a signed release, set these environment
variables before building — otherwise the release APK is produced unsigned:

```
BOKEYA_KEYSTORE_PATH
BOKEYA_KEYSTORE_PASSWORD
BOKEYA_KEY_ALIAS
BOKEYA_KEY_PASSWORD
```

## Privacy

Bokeya requests `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE` and `USE_BIOMETRIC`. It
does **not** request `INTERNET`. There is no telemetry, no crash reporting and no third-party SDK
with network access. Backups are written only where you choose via the system file picker.

Bokeya offers no financial advice, credit score, or loan approval — it is a personal record keeper.

## License

Copyright © Shohan Khan.
