# CursorUsage 技术说明

面向开发者与贡献者。本文档覆盖技术栈、构建与发布流程，以及数据安全实现的要点。README 面向终端用户，仅保留功能与使用说明。

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.0.21（JVM 17 toolchain） |
| UI | Jetpack Compose + Material 3（Compose BOM 2024.12.01） |
| 架构构件 | AndroidX Lifecycle（StateFlow / ViewModel）、Activity Compose、Biometric |
| 构建 | Android Gradle Plugin 9.2.1 · Gradle 9.5.1 |
| 平台 | `compileSdk` / `targetSdk` = 37 · `minSdk` = 26（Android 8.0） |
| 包名 | `com.lamuier.cursorusage` |
| 版本 | `versionCode` = 4 · `versionName` = 1.3.0 |
| 体积 | Release 开启 R8 混淆与资源压缩，仅引入 AndroidX 与 Biometric，无第三方网络 / 依赖注入框架 |

## 构建与发布

工程使用统一入口 `build.ps1`（需先配置 Android SDK 37）：

```powershell
.\build.ps1                 # Debug 构建
.\build.ps1 -Install        # Debug 构建 + adb 安装
.\build.ps1 -Release        # Release 签名打包到 dist/
.\build.ps1 -SetupSigning   # 配置 / 重绑 Release 签名
```

脚本依次从 `local.properties` 的 `sdk.dir`、`ANDROID_HOME`、`ANDROID_SDK_ROOT` 与 `%LOCALAPPDATA%\Android\Sdk` 查找 Android SDK。

Gradle 不继承 Windows 系统代理；需走代理时，在 `%USERPROFILE%\.gradle\gradle.properties` 配置：

```properties
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7897
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7897
```

Debug 默认先跑单元测试与 Lint，再产出 `app\build\outputs\apk\debug\app-debug.apk`。

### Release 签名与打包

密钥库不进仓库，签名元数据默认位于 `%LOCALAPPDATA%\CursorUsage\signing`。首次打包若尚未配置，会自动绑定本机 `debug.keystore`；也可显式配置：

```powershell
.\build.ps1 -SetupSigning                         # 默认沿用 debug.keystore
.\build.ps1 -SetupSigning -AdoptKeystore path.p12 -KeyAlias release -StorePassword ***
.\build.ps1 -SetupSigning -GenerateNewKey -ForceRebind   # 新钥（无法覆盖旧包）
```

日常发版：

```powershell
.\build.ps1 -Release
```

默认不做 clean（避免 Windows 上 `app\build` 被占用导致失败）；需要干净构建加 `-Clean`，联网拉依赖加 `-Online`，跳过测试 / Lint 加 `-SkipChecks`。脚本会校验签名证书、zipalign、包名、版本、权限与 Manifest，输出 `dist\CursorUsage-v1.1.0-release.apk`。

### 数据安全实现要点

```text
Android App（本机）
  ├─ Bearer Token  ──▶  https://api2.cursor.sh/...DashboardService/*
  └─ Cursor 会话    ──▶  https://cursor.com/api/auth/stripe
```

- Alias 与 Access Token 使用 Android Keystore 不可导出密钥 + AES-GCM 加密保存。
- Token 仅发往固定 Cursor 官方 HTTPS 域名，禁止重定向，不允许自定义服务地址。
- Token 不进入 `savedInstanceState`、日志或用量缓存；Release 禁止应用数据备份、设备迁移及明文 HTTP。
- 仅缓存解析后的用量字段，不保存 Cursor 原始响应。
