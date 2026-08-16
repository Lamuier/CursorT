# Changelog

## 未归档

（暂无）

## 1.2.0

- 重绘应用图标，主视觉改为「AI 工具用量」：中性圆形容器 + 蓝色液体（`#2563EB` 渐变）填充至 75% 液位，液体中心浸入 AI 星形（sparkle）；去除品牌化图形与品牌配色，暖米白底（`#F2F1ED` 系）+ 淡蓝光晕；同步更新前景、背景与单色（主题图标）层，legacy 层自动生效
- 图标精修：容器环由 4.5px 粗黑描边改为 2.5px 蓝色渐变（`#93C5FD`→`#3B82F6`→`#1E3A8A`）+ 顶部柔光高光弧；液体更饱和（`#60A5FA`→`#3B82F6`→`#1D4ED8`）+ 表面张力高光弧线；AI 星形更大 + 半透明光晕层；背景加中心柔光与右下微阴影增加景深；单色层同步细化
- 采纳 Android 17 新特性：常驻「用量监控」Live Update 在 API 37+ 换用 `Notification.MetricStyle` 三指标模板（用量 % 关键指标 · 重置倒计时 · Credits），AOD / 锁屏 / 状态栏同步展示，倒计时基于 `TimeDifference` 随系统走动；API 36 保持 `ProgressStyle`
- Live Update 语义颜色（API 37+）：用量指标与正文按档位着色（<80% 蓝 INFO / ≥80% 橙 CAUTION / ≥100% 红 DANGER），80% / 100% 阈值提醒分别套 CAUTION / DANGER
- 网络安全配置接入 ECH（加密 Client Hello）：新增 `network_security_config`（API 37+ 经 `xml-v37` 启用 `domainEncryption`），TLS 握手加密 SNI；同时以 `base-config` 显式禁止明文流量
- `build.ps1` 修复 Release 打包：其一，`Invoke-Assemble` 返回值偶发混入瞬态输出（如 lint-cache 清理）污染 `$apk`，且单元素管道结果直接 `[-1]` 会按字符索引取到 `"k"`，现以 `@()` 包裹整个管道后取 `.apk` 路径元素；其二，Release 校验改为白名单放行 `networkSecurityConfig`（APK 内资源 ID 已数字化，改在源码层校验指向 `@xml/network_security_config` 且两份配置均禁明文）
- `versionCode=3`，`versionName=1.2.0`

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
