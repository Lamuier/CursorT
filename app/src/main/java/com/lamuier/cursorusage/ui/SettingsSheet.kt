package com.lamuier.cursorusage.ui

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lamuier.cursorusage.R
import com.lamuier.cursorusage.data.NotificationSettings
import com.lamuier.cursorusage.data.PercentDisplayMode
import com.lamuier.cursorusage.data.ThemeSettings
import com.lamuier.cursorusage.ui.theme.ColorPalette
import com.lamuier.cursorusage.ui.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSheet(
    settings: ThemeSettings,
    notificationSettings: NotificationSettings,
    onDismiss: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onLiveUpdatesToggle: (Boolean) -> Unit,
    onThresholdRemindersToggle: (Boolean) -> Unit,
    percentDisplayMode: PercentDisplayMode,
    onPercentDisplayModeChange: (PercentDisplayMode) -> Unit,
    onManageAccount: () -> Unit,
) {
    val systemPaletteAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        stringResource(R.string.settings_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭设置")
                }
            }

            // ---- 外观 ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.appearance_title))
                Text(
                    stringResource(R.string.appearance_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ThemeModeOption(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.theme_mode_system),
                        icon = Icons.Outlined.PhoneAndroid,
                        selected = settings.themeMode == ThemeMode.System,
                        description = "跟随系统深浅色",
                        onClick = { onThemeModeChange(ThemeMode.System) },
                    )
                    ThemeModeOption(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.theme_mode_light),
                        icon = Icons.Outlined.LightMode,
                        selected = settings.themeMode == ThemeMode.Light,
                        description = "始终使用浅色",
                        onClick = { onThemeModeChange(ThemeMode.Light) },
                    )
                    ThemeModeOption(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.theme_mode_dark),
                        icon = Icons.Outlined.DarkMode,
                        selected = settings.themeMode == ThemeMode.Dark,
                        description = "始终使用深色",
                        onClick = { onThemeModeChange(ThemeMode.Dark) },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ColorPalette.entries.forEach { palette ->
                        val enabled = palette != ColorPalette.System || systemPaletteAvailable
                        PaletteOption(
                            palette = palette,
                            selected = settings.colorPalette == palette,
                            enabled = enabled,
                            supporting = when {
                                palette == ColorPalette.System && !systemPaletteAvailable ->
                                    "需要 Android 12 及以上"
                                palette == ColorPalette.System ->
                                    "跟随壁纸动态配色"
                                else -> null
                            },
                            onClick = {
                                if (enabled) onPaletteChange(palette)
                            },
                        )
                    }
                }
            }

            HorizontalDivider()

            // ---- 通知 ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.notification_section_title))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingToggleRow(
                        label = stringResource(R.string.notification_live_updates_label),
                        description = stringResource(R.string.notification_live_updates_description),
                        checked = notificationSettings.liveUpdatesEnabled,
                        onCheckedChange = onLiveUpdatesToggle,
                    )
                    SettingToggleRow(
                        label = stringResource(R.string.notification_threshold_reminders_label),
                        description = stringResource(R.string.notification_threshold_reminders_description),
                        checked = notificationSettings.thresholdRemindersEnabled,
                        onCheckedChange = onThresholdRemindersToggle,
                    )
                    Text(
                        stringResource(R.string.notification_display_mode_title),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PercentDisplayModeSelector(
                        selected = percentDisplayMode,
                        onSelected = onPercentDisplayModeChange,
                    )
                }
            }

            HorizontalDivider()

            // ---- 账号 ----
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.accounts_section_title))
                SettingNavRow(
                    label = stringResource(R.string.accounts_manage_label),
                    description = stringResource(R.string.accounts_manage_description),
                    icon = Icons.Outlined.AccountCircle,
                    onClick = onManageAccount,
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun HorizontalDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

@Composable
private fun SettingNavRow(
    label: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "›",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PercentDisplayModeSelector(
    selected: PercentDisplayMode,
    onSelected: (PercentDisplayMode) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PercentDisplayMode.entries.forEach { mode ->
            val label = when (mode) {
                PercentDisplayMode.Used -> stringResource(R.string.notification_display_mode_used)
                PercentDisplayMode.Remaining ->
                    stringResource(R.string.notification_display_mode_remaining)
            }
            PercentModeOption(
                modifier = Modifier.weight(1f),
                label = label,
                selected = selected == mode,
                onClick = { onSelected(mode) },
            )
        }
    }
}

@Composable
private fun PercentModeOption(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        modifier = modifier
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = label
            }
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = border,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun ThemeModeOption(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    description: String,
    onClick: () -> Unit,
) {
    val border = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        modifier = modifier
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = "$label，$description"
            }
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = border,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun PaletteOption(
    palette: ColorPalette,
    selected: Boolean,
    enabled: Boolean,
    supporting: String?,
    onClick: () -> Unit,
) {
    val border = when {
        selected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = buildString {
                    append("色板 ${palette.displayName}")
                    if (supporting != null) append("，$supporting")
                    if (!enabled) append("，不可用")
                }
            }
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = border,
        contentColor = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(palette.previewSwatch.copy(alpha = if (enabled) 1f else 0.4f))
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.surface,
                        shape = CircleShape,
                    ),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    palette.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                supporting?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (enabled) 1f else 0.6f,
                        ),
                    )
                }
            }
            if (selected) {
                Text(
                    "已选",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
