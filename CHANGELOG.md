# Changelog

## 未归档

- 新增「任务」页签：查看当前账号下的 Cursor 云端任务（后台智能体），展示任务名、执行状态（创建中 / 运行中 / 已完成 / 已出错 / 已过期）、仓库与分支、PR 状态（可跳转）、增删行数、改动文件数、所用模型与最近活跃时间
- 点击任务可进入对话详情：查看用户与智能体的聊天记录，并在未过期任务中发送跟进消息；运行中的任务会在详情页自动刷新
- 修复任务对话详情只显示创建时的第一条用户消息，后续助手回复与跟进消息无法展示
- 任务数据复用现有 Access Token 的会话认证获取（与账单接口同源），无需新增凭据；随前台静默刷新与手动刷新同步更新，本机加密缓存支持离线回显
- 数据解析容错：接口字段缺失或服务端枚举变化时降级为占位展示，不影响其余页签

## 2.0.0

> 版本判定：1.3.2 → 2.0.0。本版包含更换应用包名等不向后兼容变更，已装用户无法覆盖升级、本机数据不迁移，按 SemVer 定为主版本（原计划的 1.4.0 未发布，直接升级为 2.0.0）。

**破坏性变动（升级前必读）**

- 应用包名由 `com.lamuier.cursorusage` 更换为 `com.lamuier.cursorT`，Android 将其视为全新应用：**无法覆盖安装旧包**，须先手动卸载旧版再安装本版
- 旧包的本机数据不会迁移到新包：加密保存的账号（Access Token）、小组件与通知偏好、本地缓存等随卸载一并清除；本应用 `allowBackup=false`，系统备份/恢复同样不可用，安装新版后需重新添加 Access Token
- 已放置的用量/状态桌面小组件、快捷方式（Shortcut）与通知权限均需在卸载重装后重新配置
- 应用显示名称由「CursorUsage」更名为「Cursor助手」（启动器、顶栏与启动页同步变更）

**其他变更**

- 新增「状态」页签：展示 Cursor 官方可用状态（总览、各服务组件、进行中的事件、计划维护与近期历史），数据来自 Statuspage 公开 JSON API（`status.cursor.com/api/v2`），无需 Access Token
- 新增状态桌面小组件：2×1 迷你条与 4×3 详情卡，与用量小组件共用刷新调度；无需 Access Token，未添加账号时也可使用
- 用量加载失败时仍可查看状态页，便于对照官方故障说明
- 点击事件卡片或「打开官方状态页」仅允许跳转到 `status.cursor.com` / `stspg.io`
- 品牌统一：GitHub 仓库更名为 `Lamuier/CursorT`，Release 产物改用 `CursorT-v*` 命名（旧 `CursorUsage-v*` 产物随打包自动清理），代码标识符与主题名同步为 `CursorT` 前缀
- `versionCode=8`，`versionName=2.0.0`

## 1.3.2（灰度测试版）

> 灰度测试发布（GitHub Release 标记为 Pre-release，不占用 Latest），经实际验证后转为正式版。

- 合规：新增 `PRIVACY.md`（完整隐私政策：数据清单、去向、安全措施、权限用途、卸载行为）与 `THIRD_PARTY_NOTICES.md`（Apache-2.0 / EPL-2.0 第三方组件许可声明），README「隐私与安全」章节附链接
- 修复：网络请求 User-Agent 版本号硬编码 `1.10` 与实际版本脱钩，改用 `BuildConfig.VERSION_NAME` 自动跟随 versionName（debug 构建带 `-debug` 后缀）
- 构建：`build.ps1 -Release` 导出产物后自动清理 `dist/` 中旧版本产物（仅匹配 `CursorUsage-v*` 命名模式的 APK / mapping / sha256 / build.json），dist 只保留最新版本
- 构建：`compose.ui.tooling.preview` 依赖从 `implementation` 移至 `debugImplementation`（预览注解无需进入 Release 编译类路径）
- `versionCode=6`，`versionName=1.3.2`

## 1.3.1

- 安全加固：Access Token 明文展示期间（Shortcut「查看 Access Token」对话框与账号管理面板的展示区）临时启用 `FLAG_SECURE`，关闭展示后自动恢复，防止最近任务缩略图与截屏/录屏捕获明文 Token
- 安全加固：复制 Access Token 改用系统 `ClipboardManager` 并在 Android 13+ 标记 `EXTRA_IS_SENSITIVE`，系统剪贴板预览仅显示敏感内容占位而不展示明文（新增 `SensitiveContent` 工具）
- 安全加固：Gradle wrapper 增加 `distributionSha256Sum` 完整性 pin（哈希与官方 services.gradle.org 发布值一致），镜像分发被污染时构建直接失败
- 安全加固：`KeystoreCrypto` 解密路径改为只使用既有密钥，密钥缺失时立即报错，不再静默新建密钥掩盖密钥被删的异常状态
- 依赖维护：Compose BOM `2024.12.01` → `2026.08.00`、core-ktx `1.15.0` → `1.19.0`、lifecycle `2.8.7` → `2.11.0`、activity-compose `1.9.3` → `1.13.0`、fragment-ktx `1.8.5` → `1.9.0`（均为最新稳定版，含安全修复）

## 1.3.0

- 新增长按图标静态 App Shortcuts（Android 8.0+）：「查看 Access Token」不进账号面板，直接弹出设备验证（指纹 / 面部 / 锁屏密码），通过后对话框内明文展示 Token 并支持一键复制，无账号时转入添加流程；另有「管理 Cursor 账号」「应用设置」两个直达入口；快捷方式图标沿用新品牌视觉（暖米白圆底 + 蓝色 glyph）
- `MainActivity` 改为 `singleTask` 启动模式并处理 `onNewIntent`，应用已在后台时点击 Shortcut 同样生效（冷启动经 `onCreate` 解析 intent action）
- 新增 `ShortcutAction` 模型统一解析 Shortcut intent action（`com.lamuier.cursorusage.action.SHORTCUT_*`）
- `versionCode=4`，`versionName=1.3.0`

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
