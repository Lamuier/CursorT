# Changelog

[中文](CHANGELOG.md) | **English**

The Chinese [CHANGELOG.md](CHANGELOG.md) is canonical.

## 2.5.0

> Version: 2.4.0 → 2.5.0. This release adds a display time zone and English UI. Backward-compatible user-visible capability, so a minor version bump under SemVer.

- Settings can switch the display time zone (default: follow system). Absolute times on screen include a GMT offset and are converted to the selected zone
- The Tasks tab no longer lists merged branches (`prStatus=merged` or `isPrMerged`)
- English UI: default resources are Simplified Chinese. Settings can choose Follow system / Simplified Chinese / English. Follow system shows English on English devices and falls back to Chinese otherwise
- English docs added (`README.en.md`, `PRIVACY.en.md`, `CHANGELOG.en.md`, `docs/technical.en.md`). Chinese docs remain primary
- `versionCode=14`, `versionName=2.5.0`

## 2.4.0

> Version: 2.3.0 → 2.4.0. This release adds Tasks grouping and includes cloud tasks started from Grok Bot. Backward-compatible user-visible capability, so a minor version bump under SemVer.

- Tasks tab can group by repository / status / time / source. Groups collapse, and the choice is stored on device
- Task list and relative times sort by latest activity / update / create descending
- Cloud task list requests include `include_sources` (including `BACKGROUND_COMPOSER_SOURCE_GROK_BOT`). Sources the website hides by default (Grok Bot / API / SDK) appear in the list
- Task cards show a source chip. Grok Bot tasks are highlighted, and the header shows the Grok Bot count
- `versionCode=13`, `versionName=2.4.0`

## 2.3.0

> Version: 2.2.1 → 2.3.0. This release adds Grok Bot weekly quota on Overview / Usage. Backward-compatible user-visible capability, so a minor version bump under SemVer.

- Grok Bot weekly quota: calls Cursor `DashboardService/GetSandUsageStatus` and shows weekly percent used plus reset countdown on Overview and Usage
- This quota is independent of monthly Cursor-model / other-model pools. Hidden for enterprise shared pools, when the feature is off, or when the cap is 0
- Failures of this API (including 401/403) do not affect spend or percent usage. Overage still uses existing on-demand
- Usage tab history token totals: switch calendar month vs billing cycle, both with previous/next (up to 12 windows), via `GetAggregatedUsageEvents` with `startDate` / `endDate`
- History windows estimate Cursor-model / other-model share from token cost (not official independent quota percents). Official two-pool percents remain current-cycle only. Failures do not affect current-cycle usage
- `versionCode=12`, `versionName=2.3.0`

## 2.2.1

> Version: 2.2.0 → 2.2.1. Overview quota cards and token cards layout polish. No new capability, no breaking change, so a patch bump under SemVer.

- Overview quota card is three columns: plan quota, own-pool spend, and third-party pool cost, aligned with the token card below. Spend is split by model—Composer / Grok / Auto in the own pool, other third-party models in the third-party pool. Shows “—” when token details are unavailable
- Input / output / cache token half-width cards merge into one full-width card with three equal columns
- `versionCode=11`, `versionName=2.2.1`

## 2.2.0

> Version: 2.1.0 → 2.2.0. Usage tab adds per-model token details for the current billing cycle. Backward-compatible user-visible capability, so a minor version bump under SemVer.

- Token usage: calls Cursor `DashboardService/GetAggregatedUsageEvents` and shows total input / output / cache tokens, total cost, and sorted per-model rows on Usage
- Token API failure does not affect spend or percent usage. Old caches without the field show a temporary-unavailable hint; a successful refresh writes the encrypted usage cache
- `versionCode=10`, `versionName=2.2.0`

## 2.1.0

> Version: 2.0.0 → 2.1.0. Adds the Tasks tab (list overview, Chrome Custom Tabs for the official conversation) and customizable tab order. Backward-compatible user-visible capability, so a minor version bump under SemVer.

- Tasks tab: Cursor cloud tasks (background agents) for the current account—name, status (creating / running / finished / error / expired), repo and branch, PR status (tappable), line diffs, files changed, model, and last activity
- Tapping a task opens the official Agents conversation in Chrome Custom Tabs. Full chat uses the browser login, so the app is not limited to the first prompt
- “Open Cursor Agents on the web” and “View PR” also use Custom Tabs after an allowlist check
- Main tab order is customizable in Settings (move up / down, restore default). The current tab moves with the order. Order is stored on device
- Task data reuses the existing Access Token session (same auth as billing). No extra credentials. Updates with silent foreground refresh and manual refresh. Encrypted on-device cache supports offline display
- Parsing is resilient: missing fields or new server enums fall back to placeholders without breaking other tabs
- `versionCode=9`, `versionName=2.1.0`

## 2.0.0

> Version: 1.3.2 → 2.0.0. Includes a package-name change and other breaking changes. Installed users cannot overlay-upgrade; on-device data is not migrated. Major bump under SemVer (planned 1.4.0 was not shipped).

**Breaking changes (read before upgrading)**

- Package name changed from `com.lamuier.cursorusage` to `com.lamuier.cursorT`. Android treats this as a new app: **you cannot overlay-install over the old package**. Uninstall the old app first
- Old-package on-device data is not migrated: encrypted accounts (Access Token), widget and notification prefs, and local caches are removed with uninstall. This app sets `allowBackup=false`, so system backup/restore also does not apply. Add the Access Token again after install
- Placed usage/status widgets, shortcuts, and notification permission must be set up again after reinstall
- Display name changed from “CursorUsage” to “Cursor Assistant” (launcher, top bar, and splash)

**Other changes**

- Status tab: official Cursor availability (overview, components, active incidents, scheduled maintenance, recent history) from Statuspage public JSON (`status.cursor.com/api/v2`). No Access Token
- Status home-screen widgets: 2×1 mini bar and 4×3 detail card, sharing refresh scheduling with usage widgets. No Access Token; usable without an account
- Usage load failure still allows the Status tab so you can read official incident copy
- Incident cards and “Open official status page” may only open `status.cursor.com` / `stspg.io`
- Branding: GitHub repo renamed to `Lamuier/CursorT`. Release artifacts use `CursorT-v*` (old `CursorUsage-v*` artifacts are cleaned on package). Code identifiers and theme names use the `CursorT` prefix
- `versionCode=8`, `versionName=2.0.0`

## 1.3.2 (preview)

> Preview GitHub Release (marked Pre-release, not Latest). Promoted to stable after real-device verification.

- Compliance: added `PRIVACY.md` and `THIRD_PARTY_NOTICES.md`; README Privacy section links to them
- Fix: network User-Agent no longer hardcodes `1.10`; uses `BuildConfig.VERSION_NAME` (debug builds append `-debug`)
- Build: `build.ps1 -Release` cleans old `CursorUsage-v*` artifacts in `dist/`
- Build: `compose.ui.tooling.preview` moved from `implementation` to `debugImplementation`
- `versionCode=6`, `versionName=1.3.2`

## 1.3.1

- Security: `FLAG_SECURE` while an Access Token is shown in plain text
- Security: copy Access Token via `ClipboardManager` with `EXTRA_IS_SENSITIVE` on Android 13+
- Security: Gradle wrapper `distributionSha256Sum` pin
- Security: `KeystoreCrypto` decrypt uses an existing key only; missing keys fail immediately
- Dependencies: Compose BOM `2024.12.01` → `2026.08.00`, plus other AndroidX updates

## 1.3.0

- Static app shortcuts (Android 8.0+): View Access Token, Manage Cursor account, Settings
- `MainActivity` is `singleTask` and handles `onNewIntent` so shortcuts work when the app is already in the background
- `ShortcutAction` parses shortcut intent actions
- `versionCode=4`, `versionName=1.3.0`

## 1.2.0

- Redrawn app icon (AI usage visual)
- Android 17 Live Update uses `Notification.MetricStyle` on API 37+; API 36 keeps `ProgressStyle`
- Live Update semantic colors on API 37+
- Network security config enables ECH on API 37+ and forbids cleartext
- `build.ps1` Release packaging fixes
- `versionCode=3`, `versionName=1.2.0`

## 1.1.0

- Android 17 (API 37): `compileSdk` / `targetSdk` 37
- Build chain: Gradle 8.13 → 9.5.1, AGP 8.13.2 → 9.2.1, built-in Kotlin 2.2.10
- Release signing switched to an external certificate; signing assets live in `.signing/`
- Billing-cycle reset countdown precise to the minute; percents to two decimals
- Dropped `x86_64`; `arm64-v8a` only
- `versionCode=2`, `versionName=1.1.0`

## 1.0.0

Initial public release (rebranded as CursorUsage; version origin for this repo).

- Cursor subscription usage: total, plan quota, billing cycle, Credits, two pools, on-demand budget
- Dashboard Overview / Usage / Billing tabs
- Persistent usage-monitor Live Update plus Xiaomi HyperOS 3 Dynamic Island
- Threshold reminders at 80% / 100%
- 2×1 and 4×3 Material 3 widgets
- 64-bit ABIs: `arm64-v8a` and `x86_64`
- Alias and Access Token encrypted with Android Keystore + AES-GCM
- Package `com.lamuier.cursorusage`, `versionCode=1`, `versionName=1.0.0`
