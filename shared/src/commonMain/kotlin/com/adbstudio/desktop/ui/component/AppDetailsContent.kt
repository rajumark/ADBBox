package com.adbstudio.desktop.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.device.PackageContextAction
import com.adbstudio.desktop.device.PackageInfo
import com.adbstudio.desktop.device.PermissionInfo

private enum class AppDetailTab(val label: String) {
    Basic("Basic"),
    Components("Components"),
    Permissions("Permissions"),
    Data("Data"),
    Files("Files"),
}

@Composable
fun AppDetailsContent(
    pkg: PackageInfo,
    showBack: Boolean,
    onBack: () -> Unit,
    onAction: (PackageContextAction) -> Unit,
    onFetchPermissions: suspend (String) -> List<PermissionInfo> = { emptyList() },
    onGrantPermission: suspend (String, String) -> Unit = { _, _ -> },
    onRevokePermission: suspend (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(AppDetailTab.Basic) }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        if (showBack) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        text = pkg.packageName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.Start)
                .padding(start = 12.dp),
        ) {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 0.dp,
                divider = {},
            ) {
                AppDetailTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        },
                    )
                }
            }
        }

        when (selectedTab) {
            AppDetailTab.Basic -> AppDetailsBasicPage(
                modifier = Modifier.weight(1f),
            )
            AppDetailTab.Components -> AppDetailsComponentsPage(
                modifier = Modifier.weight(1f),
            )
            AppDetailTab.Permissions -> AppDetailsPermissionsPage(
                packageName = pkg.packageName,
                onFetchPermissions = onFetchPermissions,
                onGrantPermission = onGrantPermission,
                onRevokePermission = onRevokePermission,
                modifier = Modifier.weight(1f),
            )
            AppDetailTab.Data -> AppDetailsDataPage(
                modifier = Modifier.weight(1f),
            )
            AppDetailTab.Files -> AppDetailsFilesPage(
                modifier = Modifier.weight(1f),
            )
        }
    }
}
