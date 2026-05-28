package com.adbstudio.desktop.feature.apps.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.`package`.ClearAppDataCommand
import com.adbstudio.desktop.adb.model.`package`.DisableAppCommand
import com.adbstudio.desktop.adb.model.`package`.DumpsysPackageCommand
import com.adbstudio.desktop.adb.model.`package`.EnableAppCommand
import com.adbstudio.desktop.adb.model.`package`.ForceStopAppCommand
import com.adbstudio.desktop.adb.model.`package`.GetForegroundAppCommand
import com.adbstudio.desktop.adb.model.`package`.GrantPermissionCommand
import com.adbstudio.desktop.adb.model.`package`.LaunchAppCommand
import com.adbstudio.desktop.adb.model.`package`.ListPackagesCommand
import com.adbstudio.desktop.adb.model.`package`.OpenAppSettingsCommand
import com.adbstudio.desktop.adb.model.`package`.PackagePathCommand
import com.adbstudio.desktop.adb.model.`package`.RevokePermissionCommand
import com.adbstudio.desktop.adb.model.`package`.UninstallAppCommand
import com.adbstudio.desktop.core.error.toUserMessage
import com.adbstudio.desktop.core.result.AppResult
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.apps.data.PinnedAppsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppsViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(AppsUiState(pinnedPackages = PinnedAppsManager.load()))
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    private var pollJob: Job? = null

    init {
        scope.launch {
            deviceRepository.state
                .map { it.selectedSerial }
                .distinctUntilChanged()
                .collect { serial ->
                    _state.value = _state.value.copy(selectedSerial = serial)
                    refreshInternal(serial, isManual = false)
                    startPolling(serial)
                }
        }
    }

    private fun startPolling(serial: String?) {
        pollJob?.cancel()
        if (serial.isNullOrBlank()) return
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                refreshForegroundApp(serial)
            }
        }
    }

    private suspend fun refreshForegroundApp(serial: String) {
        when (val result = adbManager.run(GetForegroundAppCommand(serial))) {
            is AppResult.Success -> {
                val newForeground = result.value
                if (newForeground != _state.value.foregroundPackage) {
                    _state.value = _state.value.copy(foregroundPackage = newForeground)
                }
            }
            is AppResult.Error -> {}
        }
    }

    fun onEvent(event: AppsEvent) {
        when (event) {
            AppsEvent.Refresh -> refreshInternal(_state.value.selectedSerial, isManual = true)
            is AppsEvent.SetFilter -> {
                _state.value = _state.value.copy(appType = event.appType)
                refreshInternal(_state.value.selectedSerial, isManual = false)
            }
            is AppsEvent.SetSearchQuery -> _state.value = _state.value.copy(searchQuery = event.query)
            is AppsEvent.SelectPackage -> {
                _state.value = _state.value.copy(
                    selectedPackage = event.packageName,
                    detailsTab = DetailsTab.INFO,
                    packageInfo = null,
                    packagePaths = emptyList(),
                    permissions = emptyList(),
                    dumpsysSections = emptyMap(),
                )
                if (event.packageName != null) loadPackageDetails(event.packageName)
            }
            is AppsEvent.TogglePin -> {
                val current = _state.value.pinnedPackages.toMutableSet()
                if (current.contains(event.packageName)) current.remove(event.packageName)
                else current.add(event.packageName)
                _state.value = _state.value.copy(pinnedPackages = current)
                PinnedAppsManager.save(current)
            }
            is AppsEvent.SetDetailsTab -> _state.value = _state.value.copy(detailsTab = event.tab)
            AppsEvent.DismissActionMessage -> _state.value = _state.value.copy(actionMessage = null)
            AppsEvent.LaunchApp -> executeAction("Launching") { serial, pkg ->
                adbManager.run(LaunchAppCommand(serial, pkg))
            }
            AppsEvent.ForceStopApp -> executeAction("Force stopping") { serial, pkg ->
                adbManager.run(ForceStopAppCommand(serial, pkg))
            }
            AppsEvent.RestartApp -> {
                val pkg = _state.value.selectedPackage ?: return
                val serial = _state.value.selectedSerial ?: return
                scope.launch {
                    _state.value = _state.value.copy(isActionRunning = true, actionMessage = "Restarting...")
                    adbManager.run(ForceStopAppCommand(serial, pkg))
                    adbManager.run(LaunchAppCommand(serial, pkg))
                    _state.value = _state.value.copy(isActionRunning = false, actionMessage = "App restarted")
                }
            }
            AppsEvent.UninstallApp -> executeAction("Uninstalling") { serial, pkg ->
                adbManager.run(UninstallAppCommand(serial, pkg))
            }
            AppsEvent.ClearData -> executeAction("Clearing data") { serial, pkg ->
                adbManager.run(ClearAppDataCommand(serial, pkg))
            }
            AppsEvent.EnableApp -> executeAction("Enabling") { serial, pkg ->
                adbManager.run(EnableAppCommand(serial, pkg))
            }
            AppsEvent.DisableApp -> executeAction("Disabling") { serial, pkg ->
                adbManager.run(DisableAppCommand(serial, pkg))
            }
            AppsEvent.OpenAppSettings -> executeAction("Opening settings") { serial, pkg ->
                adbManager.run(OpenAppSettingsCommand(serial, pkg))
            }
            AppsEvent.CopyPackageName -> {
                _state.value = _state.value.copy(actionMessage = "Package name copied")
            }
            AppsEvent.ViewOnPlayStore -> {
                _state.value = _state.value.copy(actionMessage = "Opening Play Store...")
            }
            AppsEvent.DownloadApk -> executeAction("Downloading APK") { serial, pkg ->
                adbManager.run(PackagePathCommand(serial, pkg))
            }
            AppsEvent.RefreshDetails -> {
                val pkg = _state.value.selectedPackage ?: return
                loadPackageDetails(pkg)
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun executeAction(action: String, block: suspend (serial: String, pkg: String) -> AppResult<Any>) {
        val pkg = _state.value.selectedPackage ?: return
        val serial = _state.value.selectedSerial ?: return
        scope.launch {
            _state.value = _state.value.copy(isActionRunning = true, actionMessage = "$action $pkg...")
            when (block(serial, pkg)) {
                is AppResult.Success -> _state.value = _state.value.copy(isActionRunning = false, actionMessage = "$action complete")
                is AppResult.Error -> _state.value = _state.value.copy(isActionRunning = false, actionMessage = "$action failed")
            }
        }
    }

    private fun refreshInternal(serial: String?, isManual: Boolean) {
        scope.launch {
            if (serial.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    packages = emptyList(),
                    isLoading = false,
                    errorMessage = "No device selected",
                )
                return@launch
            }

            if (isManual) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            when (val result = adbManager.run(ListPackagesCommand(serial, _state.value.appType.flag))) {
                is AppResult.Success -> {
                    val nextPackages = result.value.sorted()
                    if (nextPackages != _state.value.packages) {
                        _state.value = _state.value.copy(packages = nextPackages)
                    }
                    _state.value = _state.value.copy(isLoading = false, errorMessage = null)
                }

                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        packages = emptyList(),
                        isLoading = false,
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }
        }
        scope.launch {
            when (val result = adbManager.run(GetForegroundAppCommand(serial))) {
                is AppResult.Success -> _state.value = _state.value.copy(foregroundPackage = result.value)
                is AppResult.Error -> {}
            }
        }
    }

    private fun loadPackageDetails(packageName: String) {
        val serial = _state.value.selectedSerial ?: return
        scope.launch {
            when (val result = adbManager.run(DumpsysPackageCommand(serial, packageName))) {
                is AppResult.Success -> {
                    val info = parsePackageInfo(packageName, result.value)
                    val permissions = parsePermissions(result.value)
                    val sections = parseDumpsysSections(result.value)
                    _state.value = _state.value.copy(
                        packageInfo = info,
                        permissions = permissions,
                        dumpsysSections = sections,
                    )
                }
                is AppResult.Error -> {}
            }
        }
        scope.launch {
            when (val result = adbManager.run(PackagePathCommand(serial, packageName))) {
                is AppResult.Success -> _state.value = _state.value.copy(packagePaths = result.value)
                is AppResult.Error -> {}
            }
        }
    }

    private fun parsePackageInfo(packageName: String, dumpsys: String): com.adbstudio.desktop.feature.apps.model.PackageInfo {
        fun extract(key: String): String {
            val regex = Regex("$key\\s*=\\s*(.+)")
            return regex.find(dumpsys)?.groupValues?.get(1)?.trim() ?: ""
        }
        return com.adbstudio.desktop.feature.apps.model.PackageInfo(
            packageName = packageName,
            versionName = extract("versionName"),
            versionCode = extract("versionCode").toLongOrNull() ?: 0,
            minSdk = extract("minSdk").toIntOrNull() ?: 0,
            targetSdk = extract("targetSdk").toIntOrNull() ?: 0,
            installer = extract("installerPackageName"),
            firstInstallTime = extract("firstInstallTime"),
            lastUpdateTime = extract("lastUpdateTime"),
            codePath = extract("codePath"),
            dataDir = extract("dataDir"),
            primaryCpuAbi = extract("primaryCpuAbi"),
            flags = extract("flags"),
        )
    }

    private fun parsePermissions(dumpsys: String): List<com.adbstudio.desktop.feature.apps.model.PackagePermission> {
        val permissions = mutableListOf<com.adbstudio.desktop.feature.apps.model.PackagePermission>()
        val requestedSection = dumpsys.substringAfter("requested permissions:", "")
            .substringBefore("install permissions:", "")
        val installSection = dumpsys.substringAfter("install permissions:", "")
            .substringBefore("runtime permissions:", "")
        val runtimeSection = dumpsys.substringAfter("runtime permissions:", "")

        fun parseSection(section: String, type: com.adbstudio.desktop.feature.apps.model.PermissionType) {
            section.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.contains("=")) {
                    val granted = trimmed.contains("granted=true")
                    val name = trimmed.substringBefore(" ").trim()
                    if (name.isNotEmpty() && name.contains(".")) {
                        permissions.add(com.adbstudio.desktop.feature.apps.model.PackagePermission(name, type, granted))
                    }
                }
            }
        }
        parseSection(requestedSection, com.adbstudio.desktop.feature.apps.model.PermissionType.REQUESTED)
        parseSection(installSection, com.adbstudio.desktop.feature.apps.model.PermissionType.INSTALL)
        parseSection(runtimeSection, com.adbstudio.desktop.feature.apps.model.PermissionType.RUNTIME)
        return permissions
    }

    private fun parseDumpsysSections(dumpsys: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()
        var currentSection = "General"
        val currentContent = StringBuilder()

        dumpsys.lines().forEach { line ->
            if (line.endsWith(":") && line.trim().length < 80 && !line.startsWith(" ")) {
                if (currentContent.isNotEmpty()) {
                    sections[currentSection] = currentContent.toString().trim()
                    currentContent.clear()
                }
                currentSection = line.trim().removeSuffix(":")
            } else {
                currentContent.appendLine(line)
            }
        }
        if (currentContent.isNotEmpty()) {
            sections[currentSection] = currentContent.toString().trim()
        }
        return sections
    }

    companion object {
        private const val POLL_INTERVAL_MS = 2000L
    }
}
