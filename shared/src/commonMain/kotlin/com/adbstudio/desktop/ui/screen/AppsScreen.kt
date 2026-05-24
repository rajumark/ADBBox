package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.device.PackageContextAction
import com.adbstudio.desktop.device.PackageFilter
import com.adbstudio.desktop.device.PackageInfo
import com.adbstudio.desktop.ui.component.AdaptiveListDetail
import com.adbstudio.desktop.ui.component.AppDetailsContent
import com.adbstudio.desktop.ui.component.PackageListView

@Composable
fun AppsScreen(
    packages: List<PackageInfo>,
    selectedPackage: PackageInfo?,
    onPackageSelected: (PackageInfo) -> Unit,
    packageFilter: PackageFilter,
    onFilterChange: (PackageFilter) -> Unit,
    onInstallApk: () -> Unit,
    onPackageContextAction: (PackageContextAction, String) -> Unit,
    askBeforeUninstall: Boolean,
    askBeforeClearData: Boolean,
    batchMode: Boolean,
    selectedBatch: Set<PackageInfo>,
    onBatchToggle: (PackageInfo) -> Unit,
    onBatchCancel: () -> Unit,
    onBackToPackageList: () -> Unit,
) {
    var pendingUninstallPkg by remember { mutableStateOf<PackageInfo?>(null) }
    var pendingClearDataPkg by remember { mutableStateOf<PackageInfo?>(null) }

    pendingUninstallPkg?.let { pkg ->
        if (askBeforeUninstall) {
            AlertDialog(
                onDismissRequest = { pendingUninstallPkg = null },
                title = { Text("Uninstall ${pkg.packageName}", style = MaterialTheme.typography.titleSmall) },
                text = {
                    Text(
                        "Are you sure you want to uninstall '${pkg.packageName}'? This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        pendingUninstallPkg = null
                        onPackageContextAction(PackageContextAction.Uninstall, pkg.packageName)
                    }) { Text("Uninstall") }
                },
                dismissButton = {
                    Button(onClick = { pendingUninstallPkg = null }) { Text("Cancel") }
                },
            )
        } else {
            LaunchedEffect(pkg) {
                pendingUninstallPkg = null
                onPackageContextAction(PackageContextAction.Uninstall, pkg.packageName)
            }
        }
    }

    pendingClearDataPkg?.let { pkg ->
        if (askBeforeClearData) {
            AlertDialog(
                onDismissRequest = { pendingClearDataPkg = null },
                title = { Text("Clear Data for ${pkg.packageName}", style = MaterialTheme.typography.titleSmall) },
                text = {
                    Text(
                        "Are you sure you want to clear all data for '${pkg.packageName}'? This will reset the app to its original state.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        pendingClearDataPkg = null
                        onPackageContextAction(PackageContextAction.ClearData, pkg.packageName)
                    }) { Text("Clear") }
                },
                dismissButton = {
                    Button(onClick = { pendingClearDataPkg = null }) { Text("Cancel") }
                },
            )
        } else {
            LaunchedEffect(pkg) {
                pendingClearDataPkg = null
                onPackageContextAction(PackageContextAction.ClearData, pkg.packageName)
            }
        }
    }

    fun handleAction(action: PackageContextAction, packageName: String) {
        val pkg = packages.find { it.packageName == packageName } ?: return
        when (action) {
            PackageContextAction.Uninstall -> pendingUninstallPkg = pkg
            PackageContextAction.ClearData -> pendingClearDataPkg = pkg
            else -> onPackageContextAction(action, packageName)
        }
    }

    AdaptiveListDetail(
        modifier = Modifier.fillMaxSize(),
        showDetail = selectedPackage != null && !batchMode,
        onBackToList = onBackToPackageList,
        listContent = {
            PackageListView(
                packages = packages,
                selectedPackage = selectedPackage,
                onPackageSelected = onPackageSelected,
                packageFilter = packageFilter,
                onFilterChange = onFilterChange,
                onInstallApk = onInstallApk,
                onPackageContextAction = { action, name -> handleAction(action, name) },
                batchMode = batchMode,
                selectedBatch = selectedBatch,
                onBatchToggle = onBatchToggle,
                onBatchCancel = onBatchCancel,
                modifier = Modifier.fillMaxSize().padding(top = 8.dp),
            )
        },
        detailContent = { onBack ->
            if (selectedPackage != null) {
                AppDetailsContent(
                    pkg = selectedPackage,
                    showBack = true,
                    onBack = onBack,
                    onAction = { action -> handleAction(action, selectedPackage.packageName) },
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Select an app to view details",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}
