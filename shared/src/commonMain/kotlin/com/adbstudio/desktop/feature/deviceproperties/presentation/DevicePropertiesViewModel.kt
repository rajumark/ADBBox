package com.adbstudio.desktop.feature.deviceproperties.presentation

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

class DevicePropertiesViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(DevicePropertiesUiState())
    val state: StateFlow<DevicePropertiesUiState> = _state.asStateFlow()

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
                        _state.value = DevicePropertiesUiState(
                            selectedSerial = null,
                            errorMessage = "No device selected",
                        )
                    }
                }
        }
    }

    fun onEvent(event: DevicePropertiesEvent) {
        when (event) {
            DevicePropertiesEvent.Refresh -> {
                val serial = _state.value.selectedSerial
                if (!serial.isNullOrBlank()) refreshInternal(serial, isManual = true)
            }
            is DevicePropertiesEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
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

            when (val result = adbManager.runShell(serial, "getprop")) {
                is AppResult.Success -> {
                    val parsed = result.value.lineSequence()
                        .map { it.trim() }
                        .filter { it.startsWith("[") && "]: [" in it }
                        .map { line ->
                            val keyEnd = line.indexOf("]: [")
                            val key = line.substring(1, keyEnd)
                            val value = line.substring(keyEnd + 4, line.length - 1)
                            key to value
                        }
                        .toList()
                    _state.value = _state.value.copy(
                        data = parsed,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }
        }
    }
}
