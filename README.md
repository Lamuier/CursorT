# CursorUsage

> 在手机上随时查看 Cursor 订阅用量——无需第三方服务端，数据只留在你自己的设备里。

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-green.svg)

CursorUsage 是一款原生 Android 应用。你只需在应用内填入本机的 Cursor Access Token，它就会向 Cursor 官方接口拉取用量，并在本地解析、加密保存。没有账号体系，没有自建后端，所有数据不经过任何第三方。

---

## 功能特性

### 用量一目了然

- 总用量、套餐额度、计费周期、Credits、两个用量池（Cursor 模型 / 其他模型）与 On-demand 预算，集中呈现。
- 「概览 / 用量 / 账单 / 状态」四页签，带流畅切换动效；双环展示用量与计费周期进度。
- 「状态」页展示 Cursor 官方可用状态：总览、各组件、进行中的事件与近期历史，无需 Token。
- 环形图、进度条与关键数字均有轻量动画，信息密度高却不杂乱。

### 实时用量通知

- 常驻「用量监控」通知，随时看到当前用量进度。
- 用量达到 80% / 100% 时自动提醒一次，避免超额。
- 小米 HyperOS 3 超级岛也能显示用量。

### 桌面小组件

- 提供用量与状态两套 MD3 小组件，各有 2×1 迷你条与 4×3 详情卡；Android 12+ 跟随系统配色。状态小组件无需 Access Token。

### 应用快捷方式

- 长按图标直达「查看 Access Token」「管理账号」「设置」；「查看 Access Token」直接弹出指纹 / 锁屏验证，通过后明文展示 Token 并支持复制。

### 贴心体验

- 启动与回到前台自动刷新，前台每 5 分钟静默同步，也支持手动刷新。
- 仅 64 位；前台自动适配高刷新率屏幕，离开后释放。
- 浅色 / 深色跟随系统，支持无障碍与大屏布局。
- 内置 Windows 平台 Token 获取引导。

---

## 隐私与安全

- **数据不出本机**：用量仅向 Cursor 官方接口请求；可用状态仅向 Cursor 官方状态页请求。二者都不连接任何第三方分析/广告服务，Token 不发往任何自定义地址。
- **本地加密保存**：账号与 Token 经系统密钥库加密后保存在你的设备，不进入备份、不通过明文传输。
- **最小收集**：仅缓存解析后的用量数字，不保存任何原始响应。

完整说明见 [PRIVACY.md](PRIVACY.md)，第三方开源组件声明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

> ⚠️ Cursor 用量接口没有公开的稳定性承诺，接口变动时应用可能需要更新。

---

## 如何获取 Cursor Access Token

应用内点击「如何获取 Access Token」即可查看完整步骤。Windows 用户也可手动提取：

状态数据库通常位于 `%APPDATA%\Cursor\User\globalStorage\state.vscdb`，Token 对应的键为 `cursorAuth/accessToken`。

```powershell
# 已安装 sqlite3
sqlite3 "$env:APPDATA\Cursor\User\globalStorage\state.vscdb" "SELECT value FROM ItemTable WHERE key='cursorAuth/accessToken';" | Set-Clipboard

# 仅有 Python
python -c 'import os,sqlite3,json; from pathlib import Path; p=Path(os.environ["APPDATA"])/"Cursor"/"User"/"globalStorage"/"state.vscdb"; c=sqlite3.connect(p.as_uri()+"?mode=ro",uri=True); v=c.execute("select value from ItemTable where key=?",("cursorAuth/accessToken",)).fetchone()[0]; print(json.loads(v) if v.startswith("\"") else v,end="")' | Set-Clipboard
```

粘贴到应用后建议清空剪贴板：`Set-Clipboard -Value $null`。不要把 Token 发给任何人或存入代码仓库。

---

## 许可与免责声明

- **许可**：以 [MIT 许可证](LICENSE) 开源。
- **免责声明**：CursorUsage 是非官方、个人维护的工具，与 Cursor（Anysphere）无隶属关系。使用即表示你理解并自行承担相关风险。

---

*开发者与构建说明请见 [docs/technical.md](docs/technical.md)。*
