package com.adbstudio.desktop.feature.devicesettings.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.core.error.toUserMessage
import com.adbstudio.desktop.core.result.AppResult
import com.adbstudio.desktop.device.DeviceRepository
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

class DeviceSettingsViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(DeviceSettingsUiState())
    val state: StateFlow<DeviceSettingsUiState> = _state.asStateFlow()

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
                        _state.value = DeviceSettingsUiState(
                            selectedSerial = null,
                            errorMessage = "No device selected",
                        )
                    }
                }
        }
    }

    fun onEvent(event: DeviceSettingsEvent) {
        when (event) {
            DeviceSettingsEvent.Refresh -> {
                val serial = _state.value.selectedSerial
                if (!serial.isNullOrBlank()) refreshInternal(serial, isManual = true)
            }
            is DeviceSettingsEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            is DeviceSettingsEvent.SetTab -> {
                _state.value = _state.value.copy(currentTab = event.tab)
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

            val global = fetchSettings(serial, "global")
            val system = fetchSettings(serial, "system")
            val secure = fetchSettings(serial, "secure")

            val firstError = global.error ?: system.error ?: secure.error

            _state.value = _state.value.copy(
                globalData = global.data,
                systemData = system.data,
                secureData = secure.data,
                isLoading = false,
                errorMessage = firstError,
            )
        }
    }

    private suspend fun fetchSettings(serial: String, type: String): SettingResult {
        return when (val result = adbManager.runShell(serial, "settings list $type")) {
            is AppResult.Success -> {
                val parsed = result.value.lineSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() && it.contains("=") }
                    .map { line ->
                        val idx = line.indexOf('=')
                        line.substring(0, idx) to line.substring(idx + 1)
                    }
                    .toList()
                SettingResult(parsed, null)
            }
            is AppResult.Error -> SettingResult(emptyList(), result.error.toUserMessage())
        }
    }

    private data class SettingResult(
        val data: List<Pair<String, String>>,
        val error: String?,
    )
}
