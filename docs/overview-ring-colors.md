# 概览圆环配色

记录概览页双环的颜色规则，方便后续改 UI 或排查「圆环怎么又变色了」。产品行为见 README；本文只写实现约定。

**中文** | [English](overview-ring-colors.en.md)

相关代码：

| 职责 | 位置 |
| --- | --- |
| 健康度阈值 | `UsageCalculations.level()` |
| 圆环绘制与取色 | `DashboardScreen.kt`（`OverviewHeroUsageRing` / `ringColor` / `UsageRing`） |
| 语义色与配色方案 | `ui/theme/Color.kt`（`PulseSemantic`、`chartColorsFor()`） |
| 分列 / 合并偏好 | `OverviewUsageRingMode`，保存在本机 SharedPreferences |
| 分列自动折叠 | `OverviewRingCollapse` |

圆环样式可在设置里切换。默认分列；本周期若自有池已用尽而三方仍有额度，会临时改成合并，下个周期再回到分列。

## 结构

双环同心，从 12 点方向顺时针画：

| 环 | 含义 | 颜色来源 |
| --- | --- | --- |
| 外环 | 用量进度 | 健康度语义色；分列时可能再拆成两条叠画弧 |
| 内环 | 账单周期进度 | 始终主题 `primary`，不跟健康度走 |
| 底轨 | 未走完的弧 | 外环 `surfaceVariant`；内环同色、透明度 `0.55` |

弧本身分两层：先铺一层 16% 透明度、1.5 倍线宽的光晕，再画从 `60%` 透明度渐变到实色的主弧。

## 健康度 → 颜色

用量先算 `UsageLevel`，再映射到当前 `PulseChartColors`：

| 用量 | 级别 | 圆环色 |
| --- | --- | --- |
| `< 80%`，且不超周期进度 | `Healthy` | `chartColors.healthy` |
| `≥ 80%`，**或**用量进度超过周期进度 | `Warning` | `chartColors.warning` |
| `≥ 90%` | `Critical` | `chartColors.critical` |
| `≥ 100%` | `Exhausted` | 同样用 `chartColors.critical` |
| 百分比未知 | — | `onSurfaceVariant` |

「超过周期进度」指用量百分比已经大于账单周期走过的百分比，用来提前标黄。

## 各配色方案的语义色

健康度三色几乎全仓共用，只有极光 / 余烬微调绿和黄。`chart1` / `chart2` / `chart3` 分别取主题 `primary` / `tertiary` / `secondary`。

| 配色 | 正常 `healthy` | 警告 `warning` | 紧张/用尽 `critical` |
| --- | --- | --- | --- |
| Pulse / 系统 / 紫罗兰 | `#10B981` | `#F59E0B` | `#EF4444` |
| 极光 | `#14B8A6` | `#F59E0B` | `#EF4444` |
| 余烬 | `#22C55E` | `#FBBF24` | `#EF4444` |

内环周期色随主题主色，不随健康度：

| 配色 | 浅色 `primary` | 深色 `primary` |
| --- | --- | --- |
| Pulse / 系统 | `#2563EB` | `#60A5FA` |
| 极光 | `#0F766E` | `#2DD4BF` |
| 余烬 | `#B45309` | `#FBBF24` |
| 紫罗兰 | `#4F46E5` | `#A5B4FC` |

## 分列 vs 合并

**分列（默认）**

- 外环自有池：按自有池健康度上色。
- 外环三方池：按三方池健康度上色；**若与自有池撞色，改用 `chart3`（主题 secondary）**，避免两条弧分不清。
- 中心数字下方的「自有池」标签跟自有池同色。
- 两条弧都有进度时会 3 秒反向呼吸：透明度在 `0.08`～`1` 之间对打；较短一侧永远叠在上面。

**合并**

- 外环只画一条总用量弧，颜色按总用量健康度。
- 中心说明文字也用同一色。
- 图例只保留「用量」和「周期 %」。

图例色点跟对应弧一致：自有 / 三方 / 总用量跟外环，周期百分比跟内环主色。
