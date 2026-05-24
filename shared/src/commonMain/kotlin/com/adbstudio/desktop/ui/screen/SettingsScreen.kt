package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.theme.ThemeMode

private enum class SettingsPage(val label: String) {
    Theme("Theme"),
    Alert("Alert"),
}

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    askBeforeUninstall: Boolean,
    onAskBeforeUninstallChange: (Boolean) -> Unit,
    askBeforeClearData: Boolean,
    onAskBeforeClearDataChange: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var selectedPage by remember { mutableStateOf(SettingsPage.Theme) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(top = 16.dp),
            ) {
                SettingsPage.entries.forEach { page ->
                    val isSelected = page == selectedPage
                    val bgColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        Color.Transparent
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .clickable { selectedPage = page }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = page.label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            ),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.fillMaxHeight().width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // Content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp),
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(16.dp))

                when (selectedPage) {
                    SettingsPage.Theme -> ThemePage(
                        current = themeMode,
                        onSelect = onThemeChange,
                    )
                    SettingsPage.Alert -> AlertPage(
                        askBeforeUninstall = askBeforeUninstall,
                        onAskBeforeUninstallChange = onAskBeforeUninstallChange,
                        askBeforeClearData = askBeforeClearData,
                        onAskBeforeClearDataChange = onAskBeforeClearDataChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemePage(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Column {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
        ) {
            ThemeMode.entries.forEachIndexed { index, mode ->
                val isSelected = mode == current
                val shape = when {
                    index == 0 -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
                    index == ThemeMode.entries.lastIndex -> RoundedCornerShape(
                        topEnd = 8.dp, bottomEnd = 8.dp,
                    )
                    else -> RoundedCornerShape(0.dp)
                }
                val bgColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val textColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Box(
                    modifier = Modifier
                        .clip(shape)
                        .background(bgColor)
                        .clickable { onSelect(mode) }
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = when (mode) {
                            ThemeMode.System -> "System"
                            ThemeMode.Dark -> "Dark"
                            ThemeMode.Light -> "Light"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertPage(
    askBeforeUninstall: Boolean,
    onAskBeforeUninstallChange: (Boolean) -> Unit,
    askBeforeClearData: Boolean,
    onAskBeforeClearDataChange: (Boolean) -> Unit,
) {
    Column {
        Text(
            text = "Confirmations",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(12.dp))

        SettingsToggleItem(
            label = "Ask before uninstall",
            checked = askBeforeUninstall,
            onCheckedChange = onAskBeforeUninstallChange,
        )

        Spacer(Modifier.height(4.dp))

        SettingsToggleItem(
            label = "Ask before clear data",
            checked = askBeforeClearData,
            onCheckedChange = onAskBeforeClearDataChange,
        )
    }
}

@Composable
private fun SettingsToggleItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
