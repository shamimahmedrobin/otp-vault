# OTP Vault – Secure Android OTP Manager

**Your OTPs, Your Control.**

OTP Vault is a secure, **offline-first, manual-entry** OTP manager for Android. You type in the codes you want to keep for a few minutes; the app encrypts them on-device, shows a live countdown, and **deletes them automatically when they expire**.

> OTP Vault **never** reads SMS, never requests SMS permissions, never intercepts notifications, never connects to any financial account, and has **no internet permission, no cloud backend, no ads and no analytics**.

## Features

- **Manual OTP entry only** for bKash, Nagad, Rocket, CellFin, Upay, TAP, Bank, Email, Social Media, Shopping, Other — plus your own custom services
- **6-digit PIN** (stored only as a salted PBKDF2 verifier) with rate-limited attempts and optional **biometric unlock** (fingerprint / face, PIN fallback)
- **AES-256-GCM encryption** for OTP codes, phone numbers and notes, with a per-field random IV and a master key protected by the **Android Keystore**
- **Auto-expiry**: 5 min (default), 15 min, 30 min, 1 hour, 24 hours or custom; expired entries are hard-deleted by a WorkManager job (every 15 min) and on app startup
- **Home dashboard** with active / expiring-soon / services summary, search, service filters, masked codes by default, show/hide, copy (with timed clipboard clearing) and delete
- **History** (All / Active / Used / Expired filters) — expired rows are deleted, not kept as history
- **Encrypted local backup & restore** via the Storage Access Framework: PBKDF2-derived key + AES-GCM, integrity-checked, never uploaded automatically
- **Material 3** UI, light / dark / system themes, **English + বাংলা** localization
- `FLAG_SECURE` on the app window, database and keys excluded from Android Backup, auto-lock on background / inactivity

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture, Hilt DI |
| Database | Room (SQLite), Kotlin Flow |
| Security | Android Keystore, AES-256-GCM, PBKDF2-HMAC-SHA256 (120k iterations) |
| Background | WorkManager |
| Min / Target SDK | 26 (Android 8.0) / 35 |

Package: `com.otpvault.app`

## Project structure

```
app/src/main/java/com/otpvault/app/
  core/        crypto, security, database, preferences, common, util
  data/        local (entities, DAOs), repository, backup
  domain/      model, repository interfaces, usecases
  presentation/ navigation, theme, components, screens/*
  workers/     ExpiryCleanupWorker
  di/          AppModule (Hilt)
```

## Build

Requirements: Android Studio (Ladybug or newer) or a JDK 17 + Android SDK 35.

```bash
# Generate the Gradle wrapper once if it is missing:
gradle wrapper --gradle-version 8.9

# Debug build
./gradlew assembleDebug

# Unit tests (JVM)
./gradlew testDebugUnitTest

# Instrumented tests (device/emulator)
./gradlew connectedDebugAndroidTest
```

### Signed release APK

1. Create a keystore (keep it outside the repo):

```bash
keytool -genkeypair -v -keystore ~/otp-vault-release.jks -alias otpvault \
  -keyalg RSA -keysize 4096 -validity 10000
```

2. Create `keystore.properties` in the project root (git-ignored):

```properties
storeFile=/absolute/path/to/otp-vault-release.jks
storePassword=********
keyAlias=otpvault
keyPassword=********
```

3. Build:

```bash
./gradlew assembleRelease   # or bundleRelease for an AAB
```

Never commit a keystore or `keystore.properties`.

## Security notes

- The raw PIN is never stored; only `PBKDF2(pin, random salt)` is kept in DataStore.
- Encryption keys never leave the Android Keystore and are never exported in backups. Restoring a backup re-encrypts values with the device's fresh local key.
- Clipboard clearing clears the clip set by this app after 30 seconds; Android clipboard *history* controlled by keyboards/other apps cannot be erased by any app, and OTP Vault does not claim otherwise.
- See [PRIVACY.md](PRIVACY.md) for the full privacy policy.

## License

MIT — see repository for details.
