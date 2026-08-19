# 第三方开源许可声明 / Third-Party Notices

Cursor助手 本体以 [MIT 许可证](LICENSE) 开源。Release 分发的 APK 中包含以下第三方开源组件：

## Apache License 2.0

以下组件以 Apache License 2.0 发布（版权各自归其贡献者所有）：

- **Kotlin 标准库与协程**（JetBrains s.r.o.）
- **AndroidX / Jetpack Compose**：
  - `androidx.core:core-ktx`
  - `androidx.lifecycle:lifecycle-*`
  - `androidx.activity:activity-compose`
  - `androidx.compose:*`（ui、material3、material-icons）
  - `androidx.biometric:biometric`
  - `androidx.fragment:fragment-ktx`

Apache License 2.0 全文：<https://www.apache.org/licenses/LICENSE-2.0>

> 依据 Apache License 2.0 第 4 条c项：本分发以目标代码形式提供，许可声明此处置于仓库源码中；任何源码形式（含 README 等 NOTICE 信息，如有）可于对应组件上游仓库获取。

## Eclipse Public License 2.0（仅开发测试，不随 APK 分发）

- **JUnit 4**（`junit:junit`，版权 JUnit 团队）——仅用于本地单元测试，不包含在 Release APK 内

Eclipse Public License 2.0 全文：<https://www.eclipse.org/legal/epl-2.0/>

## 声明

以上组件按「按原样」基础分发，各自遵循其原许可证条款；与 Cursor助手（MIT）本身分发的许可互相独立。本声明如需更新，随依赖变更在 [CHANGELOG.md](CHANGELOG.md) 记录。
