# MicroMasters — Build & Release Checklist

The canonical build runs on **GitHub Actions** with the official Google Android SDK (the dev
sandbox is firewalled from Google hosts). Every push to the working branch produces a real,
installable APK.

## Toolchain (pinned)
- Gradle **8.9** (wrapper committed) · AGP **8.6.1** · Kotlin **1.9.24**
- compileSdk/targetSdk **34** · minSdk **26** · JDK **17**
- applicationId `com.micromasters.game` · label **MicroMasters**

## Get the current build (debug, installable)
1. Open **Actions → "Build MicroMasters APK" → latest green run**.
2. Download **Artifacts → `MicroMasters-apk`** (or **Releases → "MicroMasters — legfrissebb build" → `MicroMasters.apk`**).
3. On device: allow "install from unknown sources", install, launch.

## Local build (requires Android SDK + internet to Google)
```bash
./gradlew assembleDebug      # → app/build/outputs/apk/debug/app-debug.apk
```
Open in Android Studio and Run to deploy to an emulator/device.

## Debug release checklist (current)
- [x] CI green (`assembleDebug`) on the latest commit
- [x] APK renamed to `MicroMasters.apk`, uploaded as artifact + rolling release
- [x] Vector-only assets + adaptive icon (no raster), Unicode ≤7 emoji
- [x] Offline-safe: no INTERNET permission, schema-versioned save with guarded load/save
- [x] On-device crash reporter (`CrashActivity`) for field diagnostics
- [ ] Confirmed launch + first-session smoke test on a physical device

## Store release (Google Play) — steps when going live
1. **Keystore (once):**
   ```bash
   keytool -genkey -v -keystore upload.keystore -alias micromasters \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
   Store `upload.keystore` (base64) + passwords as **CI secrets**
   (`KEYSTORE_B64`, `KEYSTORE_PASS`, `KEY_ALIAS`, `KEY_PASS`) — never commit them.
2. **`app/build.gradle.kts`:** add a `release` `signingConfig` reading those env vars; keep
   `isMinifyEnabled = true` (R8) for release; verify `proguard-rules.pro`.
3. **Versioning:** bump `versionCode` (monotonic) + `versionName` each release.
4. **Build the AAB:** `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
5. **Pre-launch:** Play Console internal-testing track → fix pre-launch-report crashes/ANRs.
6. **Compliance:** privacy policy, data-safety form, content rating, target-API compliance.
7. **Promote:** internal → closed → open → production.

## Smoke test (every build)
1. Cold launch → Title → **JÁTSSZ** → World Select (no crash).
2. Enter Kitchen → **GYŰJTÉS** banks coins with pop + haptic.
3. Upgrades sheet → upgrade a unit (cost re-prices, more workers roam).
4. **Térkép** → conquer a territory.
5. Background ~1 min → reopen → offline earnings credited.
6. Daily reward claims once/day; reopen does not re-grant.
7. Settings → reset → returns to Title cleanly.
