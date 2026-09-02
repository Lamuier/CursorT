# Privacy Policy

> This policy applies to the Cursor Assistant Android app (package `com.lamuier.cursorT`). [中文](PRIVACY.md) | **English**

**Effective date: 2026-08-19**

Cursor Assistant is local-first: it does not ship third-party analytics, ads, or crash SDKs, has no account system, and has no self-hosted backend. This is the full description of how data is handled.

The Chinese [PRIVACY.md](PRIVACY.md) is the canonical policy text.

## What we collect

**Nothing.** All data is produced and kept on your device. The developer cannot access it.

## Data the app processes

| Data | Purpose | Storage | Where it leaves the device |
| --- | --- | --- | --- |
| Cursor Access Token | Call official Cursor APIs for usage and cloud task lists | Encrypted with AndroidKeyStore (AES-GCM) in the app private directory | Sent only as a credential to official Cursor APIs |
| Account alias | Local account label | Encrypted with AndroidKeyStore (AES-GCM) | Does not leave the device |
| Usage summary (quota, percents, Credits, per-model tokens, Grok Bot weekly quota, and similar numbers) | UI, widgets, notifications | Encrypted AndroidKeyStore (AES-GCM) cache | Does not leave the device (parsed summary only; no raw response) |
| Cloud task summary (name, status, source, branch, PR link, line diffs, and similar) | Tasks tab | Encrypted AndroidKeyStore (AES-GCM) cache | Does not leave the device (parsed summary only; no raw response) |
| Cursor service status (component names, availability, incident summaries) | Status tab and widgets | Latest successful result cached in plaintext in the app private directory (public information, no token) | Requested only from the official Cursor status page; public status data briefly leaves the device to reach yours |

## Where data goes

- Network requests go only to official Cursor hosts: `api2.cursor.sh`, `cursor.com`, `status.cursor.com`
- The Access Token is used only for usage and task-list APIs (`api2.cursor.sh`, `cursor.com`). It is not sent with status-page requests or Custom Tabs
- Opening a task conversation or PR uses Chrome Custom Tabs to a checked `cursor.com` / `github.com` page. Those requests are made by the system browser with the browser’s own login, without this app’s Access Token
- All connections initiated by this app require HTTPS. Cleartext is forbidden. Redirects are rejected
- No data is sent to third-party services or a developer-operated server

## Data security

- Token and usage cache are written with the system keystore; keys do not leave secure hardware. Public service-status cache has no secrets and is stored in plaintext in the app private directory
- Cloud backup and device transfer are disabled (`allowBackup=false`). A new phone or cloud restore will not bring your token
- While a token is shown in plain text, `FLAG_SECURE` is enabled to block screenshots, recording, and Recents thumbnails
- Copied tokens (Android 13+) are marked sensitive so system clipboard previews show a placeholder

## Permissions

| Permission | Purpose |
| --- | --- |
| `INTERNET` | Request official Cursor usage, cloud task lists, and the official status page |
| `ACCESS_NETWORK_STATE` | Detect connectivity and avoid useless requests |
| `USE_BIOMETRIC` | Fingerprint / lock-screen verification before showing a token in plain text |
| `POST_NOTIFICATIONS` | Persistent usage monitor and threshold reminders |
| `RECEIVE_BOOT_COMPLETED` | Restore notification refresh scheduling after boot |
| `POST_PROMOTED_NOTIFICATIONS` | Android 16+ live notification progress style |

## Uninstall

Uninstalling the app deletes all local data (accounts, token, caches). Nothing remains.

## Changes

Material changes to this policy will be noted with the app update in [CHANGELOG.en.md](CHANGELOG.en.md) (canonical Chinese: [CHANGELOG.md](CHANGELOG.md)).

## Contact

Open a GitHub issue: <https://github.com/Lamuier/CursorT>
