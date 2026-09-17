# Overview ring colors

Color rules for the Overview concentric rings, so later UI changes can check the contract instead of re-deriving it from code. Product behavior lives in the README; this note covers implementation only.

[中文](overview-ring-colors.md) | **English**

The Chinese [overview-ring-colors.md](overview-ring-colors.md) is canonical.

Related code:

| Role | Location |
| --- | --- |
| Health thresholds | `UsageCalculations.level()` |
| Ring drawing and color lookup | `DashboardScreen.kt` (`OverviewHeroUsageRing` / `ringColor` / `UsageRing`) |
| Semantic colors and palettes | `ui/theme/Color.kt` (`PulseSemantic`, `chartColorsFor()`) |
| Split / combined preference | `OverviewUsageRingMode`, stored in on-device SharedPreferences |
| Auto-collapse from split | `OverviewRingCollapse` |

Ring style is switched in Settings. Split is the default. If the own pool is exhausted this cycle while the third-party pool still has quota, the ring temporarily switches to combined and reverts next cycle.

## Structure

Two concentric rings, drawn clockwise from 12 o’clock:

| Ring | Meaning | Color source |
| --- | --- | --- |
| Outer | Usage progress | Health semantic color; split mode may overlay two arcs |
| Inner | Billing-cycle progress | Always theme `primary`, not health-based |
| Track | Unfilled remainder | Outer uses `surfaceVariant`; inner uses the same color at `0.55` alpha |

Each progress arc is two layers: a 16% alpha glow at 1.5× stroke width, then a main stroke that gradients from 60% alpha to solid.

## Health → color

Usage is classified as `UsageLevel`, then mapped through the current `PulseChartColors`:

| Usage | Level | Ring color |
| --- | --- | --- |
| `< 80%`, and not ahead of cycle progress | `Healthy` | `chartColors.healthy` |
| `≥ 80%`, **or** usage ahead of cycle progress | `Warning` | `chartColors.warning` |
| `≥ 90%` | `Critical` | `chartColors.critical` |
| `≥ 100%` | `Exhausted` | also `chartColors.critical` |
| Percent unknown | — | `onSurfaceVariant` |

“Ahead of cycle progress” means usage percent is already greater than elapsed billing-cycle percent, so the ring turns amber early.

## Semantic colors by palette

The three health colors are shared across most palettes. Aurora and Ember only tweak green and amber. `chart1` / `chart2` / `chart3` come from theme `primary` / `tertiary` / `secondary`.

| Palette | Healthy | Warning | Critical / exhausted |
| --- | --- | --- | --- |
| Pulse / System / Violet | `#10B981` | `#F59E0B` | `#EF4444` |
| Aurora | `#14B8A6` | `#F59E0B` | `#EF4444` |
| Ember | `#22C55E` | `#FBBF24` | `#EF4444` |

Inner-ring cycle color follows theme primary, not health:

| Palette | Light `primary` | Dark `primary` |
| --- | --- | --- |
| Pulse / System | `#2563EB` | `#60A5FA` |
| Aurora | `#0F766E` | `#2DD4BF` |
| Ember | `#B45309` | `#FBBF24` |
| Violet | `#4F46E5` | `#A5B4FC` |

## Split vs combined

**Split (default)**

- Outer own-pool arc uses own-pool health color.
- Outer third-party arc uses third-party health color; **if that would match the own-pool color, it falls back to `chart3` (theme secondary)** so the two arcs stay distinguishable.
- The “own pool” caption under the center value uses the own-pool color.
- When both arcs have progress, they reverse-breathe over 3 seconds between alpha `0.08` and `1`. The shorter arc is always drawn on top.

**Combined**

- The outer ring is a single total-usage arc colored by total-usage health.
- The center caption uses the same color.
- The legend only shows usage and cycle percent.

Legend dots match their arcs: own / third-party / total usage follow the outer ring; cycle percent follows inner-ring primary.
