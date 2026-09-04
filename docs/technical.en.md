# Cursor Assistant technical notes

For developers and contributors. This document covers the stack, build and release flow, and data-security implementation. The README is for end users and keeps only product and usage notes.

[中文](technical.md) | **English**

The Chinese [technical.md](technical.md) is canonical.

## Stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.2.10 (JVM 17 toolchain) |
| UI | Jetpack Compose + Material 3 (Compose BOM 2026.08.00) |
| Architecture | AndroidX Lifecycle (StateFlow / ViewModel), Activity Compose, Biometric, Browser Custom Tabs |
| Build | Android Gradle Plugin 9.2.1 · Gradle 9.5.1 |
| Platform | `compileSdk` / `targetSdk` = 37 · `minSdk` = 26 (Android 8.0) |
| Package | `com.lamuier.cursorT` |
| Version | `versionCode` = 17 · `versionName` = 2.5.3 |
| Size | Release uses R8 shrinking and resource shrinking. Dependencies are AndroidX (including Browser Custom Tabs) and Biometric only—no third-party networking or DI |

## Build and release

The project uses a single entry `build.ps1` (Android SDK 37 must be configured first):

```powershell
.\build.ps1                 # Debug build
.\build.ps1 -Install        # Debug build + adb install
.\build.ps1 -Release        # Signed Release package to dist/
.\build.ps1 -SetupSigning   # Configure / rebind Release signing
```

The script looks for the Android SDK in `local.properties` `sdk.dir`, then `ANDROID_HOME`, `ANDROID_SDK_ROOT`, and `%LOCALAPPDATA%\Android\Sdk`.

Gradle does not inherit the Windows system proxy. For a proxy, configure `%USERPROFILE%\.gradle\gradle.properties`:

```properties
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7897
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7897
```

Debug runs unit tests and Lint first, then produces `app\build\outputs\apk\debug\app-debug.apk`.

### Release signing and packaging

The keystore is not in the repo. Signing metadata defaults to `.signing\` at the project root (override with `-SigningRoot`). The first package run binds the local `debug.keystore` if nothing is configured yet. You can also configure explicitly:

```powershell
.\build.ps1 -SetupSigning                         # reuse debug.keystore
.\build.ps1 -SetupSigning -AdoptKeystore path.p12 -KeyAlias release -StorePassword ***
.\build.ps1 -SetupSigning -GenerateNewKey -ForceRebind   # new key (cannot overwrite old packages)
```

Day-to-day release:

```powershell
.\build.ps1 -Release
```

Clean is off by default (Windows often fails when `app\build` is locked). Add `-Clean` for a clean build, `-Online` to fetch dependencies, `-SkipChecks` to skip tests / Lint. The script checks the signing cert, zipalign, package name, version, permissions, and Manifest, and writes `dist\CursorT-v2.5.3-release.apk`.

### Data-security notes

```text
Android App (on device)
  ├─ Bearer Token  ──▶  https://api2.cursor.sh/...DashboardService/*
  │                      GetCurrentPeriodUsage / GetPlanInfo /
  │                      GetUsageLimitStatusAndActiveGrants /
  │                      GetAggregatedUsageEvents (per-model token totals; startDate/endDate for history)
  │                      GetSandUsageStatus (Grok Bot weekly quota)
  ├─ Cursor session ──▶  https://cursor.com/api/auth/stripe
  │                      https://cursor.com/api/background-composer/list (cloud tasks, including Grok Bot sources)
  ├─ Chrome Custom Tabs ──▶ https://cursor.com/agents?id=<bcId> (full conversation, browser cookies)
  │                      https://cursor.com/agents (official Agents page)
  │                      https://github.com/... (task PR, URL allowlist)
  └─ Public status (no token) ──▶  https://status.cursor.com/api/v2/summary.json
                              https://status.cursor.com/api/v2/incidents.json
```

- Alias and Access Token use a non-exportable Android Keystore key plus AES-GCM.
- The token is sent only to fixed official Cursor HTTPS hosts. Redirects are rejected. Custom service URLs are not allowed. Status-page requests do not carry the token. Custom Tabs are opened by the system browser and do not carry this app’s Access Token.
- The token is not written to `savedInstanceState`, logs, or usage / task caches. Release disables app backup, device transfer, and cleartext HTTP.
- Only parsed usage and task fields are cached (task cache is also encrypted per account + credential revision). Cursor raw responses are not stored. Conversations are not fetched in-app; the official website is opened instead. Task list requests include `include_sources` (including `BACKGROUND_COMPOSER_SOURCE_GROK_BOT`). After parsing `source`, tasks are grouped by repository / status / time / source. Tasks with a merged PR are omitted from the Tasks tab.
- Absolute times are formatted in the Settings display time zone with a GMT offset (default: follow system). Cache timestamps without a zone are read in the system zone at write time, then converted.
- UI language follows the system by default: English systems use `values-en`; everything else falls back to the default Chinese resources. Settings can force Simplified Chinese or English. Default `values/strings.xml` is Chinese.
- Custom Tabs URLs must pass an allowlist: `cursor.com/agents` (no query, or only `id=<bcId>`), or `github.com` HTTPS links with no query/fragment.
- Service status uses Statuspage public JSON (`/api/v2/summary.json` and `/api/v2/incidents.json`). HTML is not parsed and RSS is not subscribed. `summary.json` has overview, components, and unresolved incidents; `incidents.json` has recent history. Both are official, structured, unauthenticated APIs.
- Home screen widgets run in a separate process `:widgetProvider`. Usage and status widgets share one JobService refresh schedule (cache TTL 15 minutes). Placing only a status widget does not request usage APIs. Status requests do not carry a token.
