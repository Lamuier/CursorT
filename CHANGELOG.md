# Changelog

## 1.0.0

初始公开版本（重品牌为 CursorUsage，作为本仓库唯一版本起点）。

- 查看 Cursor 订阅用量：总用量、套餐额度、计费周期、Credits、两个用量池（Cursor 模型 / 其他模型）与 On-demand 预算
- 主仪表盘「概览 / 用量 / 账单」三页签，胶囊式切换器带动效，支持下拉刷新与手动强制刷新
- 常驻「用量监控」Live Update 通知（Android 16 `Notification.ProgressStyle` + promoted ongoing），并适配小米 HyperOS 3 超级岛（`miui.focus.*` 仅作兼容层）
- 用量阈值提醒：用量达到 80% / 100% 各提醒一次，按计费周期起点去重
- 2×1 与 4×3 两种 MD3 桌面小组件，Android 12+ 跟随系统动态配色
- 仅打包 64 位 ABI：`arm64-v8a` 与 `x86_64`
- Alias 与 Access Token 使用 Android Keystore 不可导出密钥 + AES-GCM 加密保存在本机；Release 禁止应用数据备份、设备迁移及明文 HTTP
- 包名 `com.lamuier.cursorusage`，`versionCode=1`，`versionName=1.0.0`
