package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.feature.apps.model.AppType
import com.adbstudio.desktop.feature.apps.presentation.AppsEvent
import com.adbstudio.desktop.feature.apps.presentation.AppsViewModel
import com.adbstudio.desktop.feature.apps.presentation.DetailsTab
import com.adbstudio.desktop.ui.component.SplitView

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
) {
    val state by viewModel.state.collectAsState()
    var showFilterMenu by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf<Pair<String, (() -> Unit)>?>(null) }

    SplitView(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        initialRatio = 0.4f,
        left = {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Packages",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Row {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        IconButton(onClick = { viewModel.onEvent(AppsEvent.Refresh) }) {
                            Text(text = "↻", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onEvent(AppsEvent.SetSearchQuery(it)) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    placeholder = { Text("Search packages...") },
                    singleLine = true,
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = showFilterMenu,
                    onExpandedChange = { showFilterMenu = it },
                ) {
                    OutlinedTextField(
                        value = state.appType.displayName,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFilterMenu) },
                        singleLine = true,
                    )
                    ExposedDropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                    ) {
                        AppType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    viewModel.onEvent(AppsEvent.SetFilter(type))
                                    showFilterMenu = false
                                },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (state.errorMessage != null) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = state.filteredPackages, key = { it }) { pkg ->
                        val isSelected = pkg == state.selectedPackage
                        val isForeground = pkg == state.foregroundPackage
                        val isPinned = state.pinnedPackages.contains(pkg)

                        ListItem(
                            headlineContent = {
                                Text(
                                    text = pkg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = when {
                                        isForeground -> MaterialTheme.colorScheme.primary
                                        isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                )
                            },
                            supportingContent = {
                                if (isForeground) {
                                    Text(
                                        text = "Foreground",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            leadingContent = {
                                if (isPinned) {
                                    Text(text = "📌", style = MaterialTheme.typography.labelMedium)
                                }
                            },
                            modifier = Modifier.combinedClickable(
                                onClick = { viewModel.onEvent(AppsEvent.SelectPackage(pkg)) },
                                onLongClick = { showActionMenu = pkg },
                            ),
                        )
                    }
                }
            }
        },
        right = {
            AppDetailsPanel(viewModel = viewModel)
        },
    )

    showActionMenu?.let { pkg ->
        AlertDialog(
            onDismissRequest = { showActionMenu = null },
            title = { Text(pkg) },
            text = {
                Column {
                    Text("Launch", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        viewModel.onEvent(AppsEvent.LaunchApp)
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Force Stop", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        showConfirmDialog = "Force stop $pkg?" to {
                            viewModel.onEvent(AppsEvent.ForceStopApp)
                        }
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Restart", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        viewModel.onEvent(AppsEvent.RestartApp)
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Uninstall", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        showConfirmDialog = "Uninstall $pkg?" to {
                            viewModel.onEvent(AppsEvent.UninstallApp)
                        }
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Clear Data", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        showConfirmDialog = "Clear data for $pkg?" to {
                            viewModel.onEvent(AppsEvent.ClearData)
                        }
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Enable", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        viewModel.onEvent(AppsEvent.EnableApp)
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Disable", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        viewModel.onEvent(AppsEvent.DisableApp)
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("App Settings", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        viewModel.onEvent(AppsEvent.OpenAppSettings)
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Pin / Unpin", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.TogglePin(pkg))
                        showActionMenu = null
                    }.padding(8.dp))
                    Text("Copy Package Name", modifier = Modifier.fillMaxWidth().clickable {
                        viewModel.onEvent(AppsEvent.SelectPackage(pkg))
                        viewModel.onEvent(AppsEvent.CopyPackageName)
                        showActionMenu = null
                    }.padding(8.dp))
                }
            },
            confirmButton = {},
        )
    }

    showConfirmDialog?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = { Text("Confirm") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = {
                    action()
                    showConfirmDialog = null
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun AppDetailsPanel(viewModel: AppsViewModel) {
    val state by viewModel.state.collectAsState()

    if (state.selectedPackage == null) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Select a package to see details",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = state.selectedPackage.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DetailsTab.entries.forEach { tab ->
                TextButton(
                    onClick = { viewModel.onEvent(AppsEvent.SetDetailsTab(tab)) },
                    enabled = state.detailsTab != tab,
                ) {
                    Text(tab.displayName, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (state.detailsTab) {
            DetailsTab.INFO -> InfoTab(state)
            DetailsTab.PERMISSIONS -> PermissionsTab(state)
            DetailsTab.DUMPSYS -> DumpsysTab(state)
            DetailsTab.PATHS -> PathsTab(state)
        }
    }
}

@Composable
private fun InfoTab(state: com.adbstudio.desktop.feature.apps.presentation.AppsUiState) {
    val info = state.packageInfo ?: return
    LazyColumn {
        item { InfoRow("Package", info.packageName) }
        item { InfoRow("Version", info.versionName) }
        item { InfoRow("Version Code", info.versionCode.toString()) }
        item { InfoRow("Min SDK", info.minSdk.toString()) }
        item { InfoRow("Target SDK", info.targetSdk.toString()) }
        item { InfoRow("Installer", info.installer) }
        item { InfoRow("First Install", info.firstInstallTime) }
        item { InfoRow("Last Update", info.lastUpdateTime) }
        item { InfoRow("Code Path", info.codePath) }
        item { InfoRow("Data Dir", info.dataDir) }
        item { InfoRow("CPU ABI", info.primaryCpuAbi) }
        item { InfoRow("Flags", info.flags) }
    }
}

@Composable
private fun PermissionsTab(state: com.adbstudio.desktop.feature.apps.presentation.AppsUiState) {
    if (state.permissions.isEmpty()) {
        Text("No permissions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val grouped = state.permissions.groupBy { it.type }
    LazyColumn {
        grouped.forEach { (type, perms) ->
            item {
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(perms) { perm ->
                ListItem(
                    headlineContent = { Text(perm.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    trailingContent = {
                        Text(
                            text = if (perm.granted) "Granted" else "Denied",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (perm.granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DumpsysTab(state: com.adbstudio.desktop.feature.apps.presentation.AppsUiState) {
    if (state.dumpsysSections.isEmpty()) {
        Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn {
        state.dumpsysSections.forEach { (section, content) ->
            item {
                Text(
                    text = section,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            item {
                Text(
                    text = content.take(500),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PathsTab(state: com.adbstudio.desktop.feature.apps.presentation.AppsUiState) {
    if (state.packagePaths.isEmpty()) {
        Text("No APK paths found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    LazyColumn {
        items(state.packagePaths) { path ->
            ListItem(headlineContent = { Text(path, maxLines = 2, overflow = TextOverflow.Ellipsis) })
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f),
        )
    }
}
