package com.adbstudio.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuBarScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.commander.CommanderAction
import com.adbstudio.desktop.commander.CommanderHost
import com.adbstudio.desktop.commander.CommanderRegistry
import com.adbstudio.desktop.device.DeviceManager
import com.adbstudio.desktop.device.PackageFilter
import com.adbstudio.desktop.device.PackageInfo
import com.adbstudio.desktop.device.PackageManager
import com.adbstudio.desktop.navigation.NavigationItem
import com.adbstudio.desktop.theme.ThemeMode
import com.adbstudio.desktop.ui.component.InstallState
import com.adbstudio.desktop.util.Preferences
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

fun main() = application {
    val adbManager = remember { AdbManager() }
    val prefs = remember { Preferences(adbManager.appDataDir) }
    val deviceManager = remember { DeviceManager(adbManager.adbPath) }
    val initialScreen = remember {
        NavigationItem.entries.find { it.name == prefs.lastScreen } ?: NavigationItem.Apps
    }
    val initialTheme = remember {
        try { ThemeMode.valueOf(prefs.themeMode) } catch (_: Exception) { ThemeMode.System }
    }
    val initialFilter = remember {
        try { PackageFilter.valueOf(prefs.packageFilter) } catch (_: Exception) { PackageFilter.User }
    }
    var commanderOpen by remember { mutableStateOf(false) }
    var lastShiftTime by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    val doubleShiftThresholdMs = 400.0
    var installState by remember { mutableStateOf<InstallState>(InstallState.Idle) }
    var apkToInstall by remember { mutableStateOf<String?>(null) }
    var batchMode by remember { mutableStateOf(false) }
    var selectedBatch by remember { mutableStateOf(setOf<PackageInfo>()) }
    var askBeforeUninstall by remember { mutableStateOf(prefs.askBeforeUninstall) }
    var askBeforeClearData by remember { mutableStateOf(prefs.askBeforeClearData) }

    val initialW = prefs.windowWidth.toIntOrNull() ?: 1200
    val initialH = prefs.windowHeight.toIntOrNull() ?: 800
    val initialPosX = prefs.windowX.toIntOrNull()
    val initialPosY = prefs.windowY.toIntOrNull()

    val windowState = rememberWindowState(
        position = if (initialPosX != null && initialPosY != null) {
            WindowPosition(initialPosX.dp, initialPosY.dp)
        } else {
            WindowPosition.PlatformDefault
        },
        size = DpSize(initialW.dp, initialH.dp),
    )

    LaunchedEffect(Unit) {
        while (true) {
            deviceManager.refresh()
            delay(3000)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            windowState.position to windowState.size
        }.collect { (pos, size) ->
            if (pos is WindowPosition.Absolute) {
                prefs.windowX = pos.x.value.toInt().toString()
                prefs.windowY = pos.y.value.toInt().toString()
            }
            prefs.windowWidth = size.width.value.toInt().toString()
            prefs.windowHeight = size.height.value.toInt().toString()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "ADBStudio",
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyUp &&
                (event.key == Key.ShiftLeft || event.key == Key.ShiftRight)
            ) {
                val now = TimeSource.Monotonic.markNow()
                val elapsed = (now - lastShiftTime).inWholeMilliseconds
                lastShiftTime = now
                if (elapsed < doubleShiftThresholdMs) {
                    commanderOpen = !commanderOpen
                }
                true
            } else {
                false
            }
        },
    ) {
        var themeMode by remember { mutableStateOf(initialTheme) }
        var navigationItem by remember (initialScreen) { mutableStateOf(initialScreen) }
        val commanderRegistry = remember { CommanderRegistry() }
        val packageManager = remember { PackageManager(adbManager.adbPath) }
        var selectedPackage by remember { mutableStateOf<PackageInfo?>(null) }
        var packageFilter by remember { mutableStateOf(initialFilter) }

        LaunchedEffect(navigationItem, deviceManager.selectedDeviceId, packageFilter) {
            val deviceId = deviceManager.selectedDeviceId
            if (deviceId != null) {
                packageManager.refresh(deviceId, packageFilter)
                if (navigationItem == NavigationItem.Apps) {
                    selectedPackage = null
                    while (true) {
                        delay(5000)
                        packageManager.refresh(deviceId, packageFilter)
                    }
                }
            }
        }

        LaunchedEffect(navigationItem) {
            prefs.lastScreen = navigationItem.name
        }

        LaunchedEffect(themeMode) {
            prefs.themeMode = themeMode.name
        }

        LaunchedEffect(packageFilter) {
            prefs.packageFilter = packageFilter.name
        }

        LaunchedEffect(askBeforeUninstall) {
            prefs.askBeforeUninstall = askBeforeUninstall
        }

        LaunchedEffect(askBeforeClearData) {
            prefs.askBeforeClearData = askBeforeClearData
        }

        LaunchedEffect(apkToInstall) {
            val apkFile = apkToInstall ?: return@LaunchedEffect
            val deviceId = deviceManager.selectedDeviceId ?: return@LaunchedEffect
            val name = File(apkFile).name
            installState = InstallState.Installing(fileName = name)
            installState = withContext(Dispatchers.IO) {
                runAdbInstall(adbManager.adbPath, deviceId, apkFile)
            }
            apkToInstall = null
        }

        LaunchedEffect(Unit) {
            NavigationItem.entries.forEach { item ->
                commanderRegistry.register(
                    CommanderAction(
                        label = item.displayName,
                        category = item.category,
                        action = { navigationItem = item },
                    ),
                )
            }

            commanderRegistry.registerAll(
                listOf(
                    CommanderAction("Connect Device…", "Device", "⌘⇧C"),
                    CommanderAction("Disconnect Device", "Device"),
                    CommanderAction("Screen Capture", "Device", "⌘⇧S"),
                    CommanderAction("Screen Record", "Device", "⌘⇧R"),
                    CommanderAction("Install APK…", "Device", "⌘⇧I"),
                    CommanderAction("Pull File…", "Device"),
                    CommanderAction("Push File…", "Device"),
                    CommanderAction("Restart Device", "Device"),
                    CommanderAction("About ADBStudio", "Help"),
                    CommanderAction("Check for Updates…", "Help"),
                    CommanderAction("Documentation", "Help"),
                    CommanderAction("Report Issue…", "Help"),
                    CommanderAction("System Theme", "Theme", action = { themeMode = ThemeMode.System }),
                    CommanderAction("Dark Theme", "Theme", action = { themeMode = ThemeMode.Dark }),
                    CommanderAction("Light Theme", "Theme", action = { themeMode = ThemeMode.Light }),
                ),
            )
        }

        MenuBar {
            NavigationMenu(
                current = navigationItem,
                onSelect = { navigationItem = it },
            )
            ThemeMenu(
                current = themeMode,
                onSelect = { themeMode = it },
            )
            DeviceMenu(deviceManager)
            HelpMenu()
        }

        CommanderHost(registry = commanderRegistry) {
            App(
                themeMode = themeMode,
                onThemeChange = { themeMode = it },
                navigationItem = navigationItem,
                adbManager = adbManager,
                selectedDevice = deviceManager.selectedDevice,
                packages = packageManager.packages,
                selectedPackage = selectedPackage,
                onPackageSelected = { selectedPackage = it },
                packageFilter = packageFilter,
                onFilterChange = { packageFilter = it },
                onInstallApk = {
                    val deviceId = deviceManager.selectedDeviceId
                    if (deviceId == null) return@App
                    val dialog = FileDialog(null as Frame?, "Select APK File", FileDialog.LOAD)
                    dialog.file = "*.apk"
                    dialog.isVisible = true
                    val dir = dialog.directory
                    val file = dialog.file
                    if (file == null) return@App
                    apkToInstall = File(dir, file).absolutePath
                },
                installState = installState,
                onInstallDismiss = { installState = InstallState.Idle },
                onCopyError = { text ->
                    Toolkit.getDefaultToolkit()
                        .systemClipboard
                        .setContents(StringSelection(text), null)
                },
                batchMode = batchMode,
                selectedBatch = selectedBatch,
                onBatchToggle = { pkg ->
                    selectedBatch = if (pkg in selectedBatch) {
                        selectedBatch - pkg
                    } else {
                        selectedBatch + pkg
                    }
                },
                onBatchCancel = {
                    selectedPackage = null
                    selectedBatch = emptySet()
                    batchMode = !batchMode
                },
                askBeforeUninstall = askBeforeUninstall,
                onAskBeforeUninstallChange = { askBeforeUninstall = it },
                askBeforeClearData = askBeforeClearData,
                onAskBeforeClearDataChange = { askBeforeClearData = it },
                onNavigateToApps = { navigationItem = NavigationItem.Apps },
                commanderOpen = commanderOpen,
                onCommanderDismiss = { commanderOpen = false },
                onCommanderAction = { action ->
                    action.action()
                    commanderOpen = false
                },
                commanderRegistry = commanderRegistry,
            )
        }
    }
}

private fun runAdbInstall(adbPath: String, deviceId: String, apkFile: String): InstallState {
    return try {
        val process = ProcessBuilder(
            adbPath, "-s", deviceId, "install", "-r", apkFile,
        )
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        if (exitCode == 0 && output.contains("Success")) {
            InstallState.Success("App installed successfully on $deviceId.")
        } else {
            val lines = output.lines()
            val summary = if (lines.size <= 6) lines.joinToString("\n") else lines.take(6).joinToString("\n")
            InstallState.Error(summary = summary, fullLog = output.trim())
        }
    } catch (e: Exception) {
        val msg = e.message ?: "Unknown error"
        InstallState.Error(summary = msg, fullLog = msg)
    }
}

@Composable
private fun MenuBarScope.NavigationMenu(
    current: NavigationItem,
    onSelect: (NavigationItem) -> Unit,
) {
    Menu("Navigation") {
        Item("Apps") { onSelect(NavigationItem.Apps) }
        Item("Debug Info") { onSelect(NavigationItem.DebugInfo) }
        Item("Settings") { onSelect(NavigationItem.Settings) }
        Item("UI Inspector") { onSelect(NavigationItem.UiInspector) }
    }
}

@Composable
private fun MenuBarScope.ThemeMenu(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    Menu("Theme") {
        Item("System") { onSelect(ThemeMode.System) }
        Item("Dark") { onSelect(ThemeMode.Dark) }
        Item("Light") { onSelect(ThemeMode.Light) }
    }
}

@Composable
private fun MenuBarScope.DeviceMenu(deviceManager: DeviceManager) {
    val devices = deviceManager.devices
    val selectedId = deviceManager.selectedDeviceId
    val title = selectedId ?: "Device"
    Menu(title) {
        if (devices.isEmpty()) {
            Item("Refresh") { }
        } else {
            devices.forEach { device ->
                val isSelected = device.id == selectedId
                val prefix = if (isSelected) "✓ " else "  "
                Item("$prefix${device.id}") {
                    deviceManager.selectDevice(device.id)
                }
            }
            Separator()
            Item("Refresh") { }
        }
    }
}

@Composable
private fun MenuBarScope.HelpMenu() {
    Menu("Help") {
        Item("About ADBStudio") { }
        Item("Check for Updates…") { }
        Separator()
        Item("Documentation") { }
        Item("Report Issue…") { }
    }
}
