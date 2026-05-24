package com.adbstudio.desktop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.adbstudio.desktop.commander.CommanderAction
import com.adbstudio.desktop.commander.CommanderDialog
import com.adbstudio.desktop.commander.CommanderRegistry
import com.adbstudio.desktop.device.DeviceInfo
import com.adbstudio.desktop.device.PackageContextAction
import com.adbstudio.desktop.device.PackageFilter
import com.adbstudio.desktop.device.PackageInfo
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.AdbStudioTheme
import com.adbstudio.desktop.theme.ThemeMode
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.ui.component.InstallApkDialog
import com.adbstudio.desktop.ui.component.InstallState
import com.adbstudio.desktop.ui.screen.AppsScreen
import com.adbstudio.desktop.ui.screen.DebugInfoScreen
import com.adbstudio.desktop.ui.screen.SettingsScreen
import com.adbstudio.desktop.ui.screen.UiInspectorScreen

@Composable
fun App(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    navigationItem: NavigationItem,
    adbManager: AdbManager,
    selectedDevice: DeviceInfo?,
    packages: List<PackageInfo>,
    selectedPackage: PackageInfo?,
    onPackageSelected: (PackageInfo) -> Unit,
    packageFilter: PackageFilter,
    onFilterChange: (PackageFilter) -> Unit,
    onInstallApk: () -> Unit,
    installState: InstallState,
    onInstallDismiss: () -> Unit,
    onCopyError: (String) -> Unit,
    batchMode: Boolean,
    selectedBatch: Set<PackageInfo>,
    onBatchToggle: (PackageInfo) -> Unit,
    onBatchCancel: () -> Unit,
    onPackageContextAction: (PackageContextAction, String) -> Unit,
    onBackToPackageList: () -> Unit,
    askBeforeUninstall: Boolean,
    onAskBeforeUninstallChange: (Boolean) -> Unit,
    askBeforeClearData: Boolean,
    onAskBeforeClearDataChange: (Boolean) -> Unit,
    onNavigateToApps: () -> Unit,
    commanderOpen: Boolean,
    onCommanderDismiss: () -> Unit,
    onCommanderAction: (CommanderAction) -> Unit,
    commanderRegistry: CommanderRegistry,
) {
    AdbStudioTheme(themeMode = themeMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (navigationItem) {
                    NavigationItem.Apps -> AppsScreen(
                        packages = packages,
                        selectedPackage = selectedPackage,
                        onPackageSelected = onPackageSelected,
                        packageFilter = packageFilter,
                        onFilterChange = onFilterChange,
                        onInstallApk = onInstallApk,
                        onPackageContextAction = onPackageContextAction,
                        onBackToPackageList = onBackToPackageList,
                        askBeforeUninstall = askBeforeUninstall,
                        askBeforeClearData = askBeforeClearData,
                        batchMode = batchMode,
                        selectedBatch = selectedBatch,
                        onBatchToggle = onBatchToggle,
                        onBatchCancel = onBatchCancel,
                    )
                    NavigationItem.DebugInfo -> DebugInfoScreen(
                        adbManager = adbManager,
                        selectedDevice = selectedDevice,
                    )
                    NavigationItem.Settings -> SettingsScreen(
                        themeMode = themeMode,
                        onThemeChange = onThemeChange,
                        askBeforeUninstall = askBeforeUninstall,
                        onAskBeforeUninstallChange = onAskBeforeUninstallChange,
                        askBeforeClearData = askBeforeClearData,
                        onAskBeforeClearDataChange = onAskBeforeClearDataChange,
                        onBack = onNavigateToApps,
                    )
                    NavigationItem.UiInspector -> UiInspectorScreen()
                }
            }

            AnimatedVisibility(
                visible = commanderOpen,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                CommanderDialog(
                    registry = commanderRegistry,
                    onDismiss = onCommanderDismiss,
                    onActionSelected = onCommanderAction,
                )
            }

            InstallApkDialog(
                state = installState,
                onDismiss = onInstallDismiss,
                onCopyError = onCopyError,
            )
        }
    }
}
