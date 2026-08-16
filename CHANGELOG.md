# Changelog

## 1.1.0

- 适配 Android 17（API 37）：`compileSdk`/`targetSdk` 升级至 37，Build-Tools 显式固定 37.0.0
- 构建链升级：Gradle 8.13 → 9.5.1、AGP 8.13.2 → 9.2.1，迁移至 AGP 内置 Kotlin（2.2.10，移除 `kotlin-android` 插件，`kotlinOptions` 迁移为 `kotlin.compilerOptions`），Compose 编译器插件同步至 2.2.10；`settings.gradle.kts` 增加阿里云镜像（本地无法直连 Google/Maven Central）；`build.ps1` SDK 检查更新为 `android-37.0`，targetSdk 校验更新为 37
- Release 签名切换为外部正式证书（PKCS12，证书 SHA-256 `5342F38C…6FF3`），支持 `-AdoptKeystore` 绑定既有密钥库；签名资产目录由 `%LOCALAPPDATA%\CursorUsage\signing` 迁移至仓库内 `.signing/`（已加入 `.gitignore`，密码经 DPAPI 加密存储）
- `build.ps1` 修复：`-SetupSigning` 首次自动配置签名后元数据文件变量为空导致校验失败的问题；生成密钥流程中 `Remove-Item Env:` 在受限环境下的执行兼容问题
- 计费周期重置时间精确到分钟：概览、周期卡片、常驻通知与桌面小组件的重置倒计时由整数天改为「天 + 小时 / 小时 + 分」精确展示，周期结束时间显示具体时刻；周期与用量百分比统一精确到两位小数（如 `35.42%`），覆盖仪表盘与桌面小组件；周期范围标签同年省略年份（`07-01 — 07-31 15:41`，跨年保留），避免长文本显示不全；周期时间解析兼容纯日期格式；页面停留期间周期百分比与倒计时每 5 秒本地走动（仅重算时间，不触发网络请求，切后台自动暂停）
- 移除 `x86_64` ABI 支持，仅打包 `arm64-v8a`
- `versionCode=2`，`versionName=1.1.0`

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
