package com.adbstudio.desktop.feature.inspector.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.inspector.GetCurrentActivityCommand
import com.adbstudio.desktop.core.error.toUserMessage
import com.adbstudio.desktop.core.result.AppResult
import com.adbstudio.desktop.device.DeviceRepository
import com.adbstudio.desktop.feature.inspector.data.WindowDumpParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class UiInspectorViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(UiInspectorUiState())
    val state: StateFlow<UiInspectorUiState> = _state.asStateFlow()

    init {
        scope.launch {
            deviceRepository.state
                .map { it.selectedSerial }
                .distinctUntilChanged()
                .collect { serial ->
                    _state.value = _state.value.copy(selectedSerial = serial)
                    if (!serial.isNullOrBlank()) {
                        refreshInternal(serial, isManual = false)
                    } else {
                        _state.value = UiInspectorUiState(
                            selectedSerial = null,
                            errorMessage = "No device selected",
                        )
                    }
                }
        }
    }

    fun onEvent(event: UiInspectorEvent) {
        when (event) {
            UiInspectorEvent.Refresh -> {
                val serial = _state.value.selectedSerial
                if (!serial.isNullOrBlank()) {
                    refreshInternal(serial, isManual = true)
                }
            }
            is UiInspectorEvent.SelectNode -> {
                _state.value = _state.value.copy(
                    selectedNodeId = event.nodeId,
                    nodeTraversalIndex = event.nodeId?.let { id ->
                        _state.value.nodes.indexOfFirst { it.id == id }
                    } ?: -1,
                )
            }
            is UiInspectorEvent.SetLayerDepth -> {
                _state.value = _state.value.copy(layerDepth = event.depth)
            }
            is UiInspectorEvent.SetNodeTraversalIndex -> {
                val index = event.index
                val nodeId = if (index in _state.value.nodes.indices) {
                    _state.value.nodes[index].id
                } else null
                _state.value = _state.value.copy(
                    nodeTraversalIndex = index,
                    selectedNodeId = nodeId,
                )
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun refreshInternal(serial: String, isManual: Boolean) {
        scope.launch {
            if (isManual) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            // Fetch activity in parallel
            val activityDeferred = scope.launch {
                when (val result = adbManager.run(GetCurrentActivityCommand(serial))) {
                    is AppResult.Success -> {
                        _state.value = _state.value.copy(activityName = result.value)
                    }
                    is AppResult.Error -> {
                        _state.value = _state.value.copy(activityName = null)
                    }
                }
            }

            // Fetch fragment info in parallel
            val fragmentDeferred = scope.launch {
                val fragmentText = fetchCurrentFragment(serial)
                _state.value = _state.value.copy(fragmentInfo = fragmentText)
            }

            // Step 1: Dump UI hierarchy to device temp
            when (val dumpResult = adbManager.runShell(serial, "uiautomator dump /data/local/tmp/window_dump.xml")) {
                is AppResult.Error -> {
                    activityDeferred.join()
                    fragmentDeferred.join()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "UI dump failed: ${dumpResult.error.toUserMessage()}",
                    )
                    return@launch
                }
                is AppResult.Success -> { /* continue */ }
            }

            // Step 2: Pull XML file
            val xmlBytes = when (val pullXmlResult = adbManager.pullFile(serial, "/data/local/tmp/window_dump.xml")) {
                is AppResult.Error -> {
                    activityDeferred.join()
                    fragmentDeferred.join()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to pull XML: ${pullXmlResult.error.toUserMessage()}",
                    )
                    return@launch
                }
                is AppResult.Success -> pullXmlResult.value
            }

            // Step 3: Capture screenshot on device
            when (val screenshotResult = adbManager.runShell(serial, "screencap -p /data/local/tmp/screenshot.png")) {
                is AppResult.Error -> {
                    activityDeferred.join()
                    fragmentDeferred.join()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Screenshot failed: ${screenshotResult.error.toUserMessage()}",
                    )
                    return@launch
                }
                is AppResult.Success -> { /* continue */ }
            }

            // Step 4: Pull screenshot
            val screenshotBytes = when (val pullScreenshotResult = adbManager.pullFile(serial, "/data/local/tmp/screenshot.png")) {
                is AppResult.Error -> {
                    activityDeferred.join()
                    fragmentDeferred.join()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to pull screenshot: ${pullScreenshotResult.error.toUserMessage()}",
                    )
                    return@launch
                }
                is AppResult.Success -> pullScreenshotResult.value
            }

            activityDeferred.join()
            fragmentDeferred.join()

            val xmlText = xmlBytes.decodeToString()
            val nodes = WindowDumpParser.parse(xmlText)

            _state.value = _state.value.copy(
                xmlContent = xmlText,
                screenshotBytes = screenshotBytes,
                nodes = nodes,
                selectedNodeId = null,
                nodeTraversalIndex = -1,
                layerDepth = -1,
                isLoading = false,
                errorMessage = null,
            )
        }
    }

    private suspend fun fetchCurrentFragment(serial: String): String? {
        // Priority 1: NavBackStackEntry (Jetpack Navigation)
        when (val result = adbManager.runShell(serial, "dumpsys activity top | grep NavBackStackEntry")) {
            is AppResult.Success -> {
                val lines = result.value.lines().filter { it.contains("NavBackStackEntry") }
                if (lines.isNotEmpty()) {
                    return lines.lastOrNull()?.trim()?.substringAfter("NavBackStackEntry")?.trim() ?: "Jetpack Nav"
                }
            }
            is AppResult.Error -> { /* fallback */ }
        }

        // Priority 2: General fragment grep
        when (val result = adbManager.runShell(serial, "dumpsys activity top | grep -i Fragment")) {
            is AppResult.Success -> {
                val lines = result.value.lines()
                    .filter { it.contains("Fragment", ignoreCase = true) }
                    .map { it.trim() }
                if (lines.isNotEmpty()) {
                    // Return the last/most specific fragment line
                    return lines.lastOrNull()?.take(120)
                }
            }
            is AppResult.Error -> { /* fallback */ }
        }

        return null
    }
}
