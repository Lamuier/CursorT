# Cursor Assistant

> Check your Cursor subscription usage on your phone—no third-party backend. Data stays on your device.

[中文](README.md) | **English**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)

Cursor Assistant is a native Android app. Paste your Cursor Access Token in the app; it fetches usage from official Cursor APIs, then parses and encrypts the result locally. There is no account system and no self-hosted backend. Data never goes through a third party.

Chinese is the primary language of this repository and the default app locale. English UI is available in Settings, or automatically when the system language is English.

---

## Features

### Usage at a glance

- Total usage, billing cycle, Credits, two usage pools (Cursor models / other models), Grok Bot weekly quota, and on-demand budget in one place. Plan quota mix is on the Usage tab.
- The Usage tab also shows billing-cycle token totals: input / output / cache tokens and cost by model. You can look back by calendar month or past billing cycles, with pool share estimated from token cost. Grok Bot weekly quota is a separate section and is not mixed into monthly pools.
- Five tabs—Overview, Usage, Billing, Tasks, Status—with animated switching. Tab order is customizable in Settings. Dual rings show usage and billing-cycle progress.
- The Tasks tab lists Cursor cloud tasks (background agents), grouped by repository / status / time / source. Merged branches are hidden. Sources that the website hides by default (Grok Bot, API, SDK, and others) are included. Tapping a task opens the official Agents page in Chrome Custom Tabs for the full conversation.
- The Status tab shows official Cursor availability: overview, components, active incidents, and recent history. No token required.
- Rings, progress bars, and key numbers use light motion without clutter.

### Live usage notifications

- A persistent usage-monitor notification shows current progress.
- One-shot alerts at 80% / 100% usage.
- Xiaomi HyperOS 3 Dynamic Island can show usage as well.

### Home screen widgets

- Usage and status Material 3 widgets, each with a 2×1 mini bar and a 4×3 detail card. Android 12+ follows system colors. Status widgets do not need an Access Token.

### App shortcuts

- Long-press the icon for View Access Token, Manage account, and Settings. View Access Token prompts fingerprint / lock-screen verification, then shows the token in plain text with copy support.

### Everyday details

- Auto-refresh on launch and when returning to the foreground; silent sync every 5 minutes in the foreground; manual refresh is also available.
- 64-bit only. High refresh rate is used in the foreground and released when you leave.
- Light / dark follow the system. Accessibility and large-screen layouts are supported. Tab order, display time zone, and UI language can be changed in Settings. Default copy is Simplified Chinese; Follow system uses English on English devices and falls back to Chinese otherwise.
- Built-in Windows guide for extracting a Token.

---

## Privacy and security

- **Data stays on device**: Usage is requested only from official Cursor APIs; availability is requested only from the official Cursor status page. Neither talks to analytics or ads. The token is never sent to a custom URL.
- **Encrypted locally**: Account and token are stored with the system keystore on your device. They are not backed up and are not transmitted in the clear.
- **Minimal cache**: Only parsed usage numbers and task summaries are cached. Raw API responses are not saved. Conversations are viewed on the official website; the app does not fetch chat content.

Full details: [PRIVACY.en.md](PRIVACY.en.md) (canonical Chinese: [PRIVACY.md](PRIVACY.md)). Third-party notices: [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

> Cursor usage APIs have no public stability guarantee. The app may need an update when APIs change.

---

## How to get a Cursor Access Token

In the app, tap “How to get an Access Token” for the full steps. Windows users can also extract it manually:

The state database is usually at `%APPDATA%\Cursor\User\globalStorage\state.vscdb`. The key is `cursorAuth/accessToken`.

```powershell
# sqlite3 installed
sqlite3 "$env:APPDATA\Cursor\User\globalStorage\state.vscdb" "SELECT value FROM ItemTable WHERE key='cursorAuth/accessToken';" | Set-Clipboard

# Python only
python -c 'import os,sqlite3,json; from pathlib import Path; p=Path(os.environ["APPDATA"])/"Cursor"/"User"/"globalStorage"/"state.vscdb"; c=sqlite3.connect(p.as_uri()+"?mode=ro",uri=True); v=c.execute("select value from ItemTable where key=?",("cursorAuth/accessToken",)).fetchone()[0]; print(json.loads(v) if v.startswith("\"") else v,end="")' | Set-Clipboard
```

After pasting into the app, clear the clipboard: `Set-Clipboard -Value $null`. Do not send the token to anyone or commit it to a repository.

---

## License and disclaimer

- **License**: Open source under the [MIT License](LICENSE).
- **Disclaimer**: Cursor Assistant is an unofficial, personally maintained tool and is not affiliated with Cursor (Anysphere). Use is at your own risk.

---

*Build and contributor notes: [docs/technical.en.md](docs/technical.en.md) (canonical Chinese: [docs/technical.md](docs/technical.md)).*
