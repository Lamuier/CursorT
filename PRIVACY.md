# 隐私政策 / Privacy Policy

> 本文件适用于 Cursor助手 Android 应用（包名 `com.lamuier.cursorT`）。

**生效日期：2026-08-19**

Cursor助手 是一款本地优先的应用：不内置任何第三方统计、广告、崩溃收集 SDK，没有账号体系，没有自建服务端。以下是数据处理的完整说明。

## 我们收集哪些数据

**不收集。** 所有数据均产生并保留在你的设备上，开发者无法接触。

## 应用处理的数据

| 数据 | 用途 | 存储方式 | 离开设备的去向 |
| --- | --- | --- | --- |
| Cursor Access Token | 调用 Cursor 官方接口查询用量与云端任务列表 | AndroidKeyStore（AES-GCM）加密后存于应用私有目录 | 仅在请求 Cursor 官方接口时作为认证凭据发出 |
| 账号别名 | 本地标识账号 | AndroidKeyStore（AES-GCM）加密存储 | 不离开设备 |
| 用量摘要（额度、百分比、Credits 等数字） | 界面展示、小组件、通知 | AndroidKeyStore（AES-GCM）加密缓存 | 不离开设备（缓存为解析后的摘要，不含原始响应） |
| 云端任务摘要（任务名、状态、分支、PR 链接、增删行数等） | 「任务」页展示 | AndroidKeyStore（AES-GCM）加密缓存 | 不离开设备（缓存为解析后的摘要，不含原始响应） |
| Cursor 服务状态（组件名、可用状态、事件摘要） | 「状态」页与桌面小组件展示 | 应用私有目录明文缓存最近一次成功结果（公开信息，不含 Token） | 仅向 Cursor 官方状态页请求；响应中的公开状态数据会短暂离开设备到达本机 |

## 数据去向

- 网络请求仅指向 Cursor 官方域名：`api2.cursor.sh`、`cursor.com`、`status.cursor.com`
- Access Token 只用于用量与任务列表接口（`api2.cursor.sh`、`cursor.com`），不会随状态页请求或 Custom Tabs 发送
- 打开任务对话或查看 PR 时，应用通过 Chrome Custom Tabs 打开已校验的 `cursor.com` / `github.com` 页面；这些请求由系统浏览器发出，使用浏览器自身的登录态，不携带本应用 Access Token
- 全部由本应用发起的连接强制 HTTPS，禁止明文传输，拒绝重定向
- 不向任何第三方服务、开发者个人服务器发送数据

## 数据安全

- Token 与用量缓存经系统密钥库加密后落盘，密钥不出安全硬件；公开的服务状态缓存不含敏感信息，明文保存在应用私有目录
- 云备份与设备迁移已排除（`allowBackup=false`）：换机、云恢复不会带走你的 Token
- Token 明文展示期间自动启用 `FLAG_SECURE`，阻止截屏 / 录屏 / 最近任务缩略图捕获
- 复制 Token 时（Android 13+）剪贴板标记为敏感内容，系统预览仅显示占位

## 权限用途

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 请求 Cursor 官方用量、云端任务列表与官方状态页 |
| `ACCESS_NETWORK_STATE` | 判断网络可用性，避免无效请求 |
| `USE_BIOMETRIC` | 查看明文 Token 前的指纹 / 锁屏验证 |
| `POST_NOTIFICATIONS` | 用量监控常驻通知与阈值提醒 |
| `RECEIVE_BOOT_COMPLETED` | 开机后恢复通知刷新调度 |
| `POST_PROMOTED_NOTIFICATIONS` | Android 16+ 实时通知进度样式 |

## 卸载

卸载应用即删除全部本地数据（账号、Token、缓存），无任何残留。

## 变更

本政策如有实质变更，将随应用更新在 [CHANGELOG.md](CHANGELOG.md) 中注明。

## 联系

通过 GitHub 仓库 issue 联系开发者：<https://github.com/Lamuier/CursorT>
