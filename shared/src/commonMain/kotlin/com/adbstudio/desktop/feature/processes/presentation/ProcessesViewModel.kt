package com.adbstudio.desktop.feature.processes.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.processes.PsCommand
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

class ProcessesViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ProcessesUiState())
    val state: StateFlow<ProcessesUiState> = _state.asStateFlow()

    init {
        scope.launch {
            deviceRepository.state
                .map { it.selectedSerial }
                .distinctUntilChanged()
                .collect { serial ->
                    _state.value = _state.value.copy(selectedSerial = serial)
                    refreshInternal(serial, isManual = false)
                }
        }
    }

    fun onEvent(event: ProcessesEvent) {
        when (event) {
            ProcessesEvent.Refresh -> refreshInternal(_state.value.selectedSerial, isManual = true)
            is ProcessesEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            ProcessesEvent.ToggleShowAll -> {
                _state.value = _state.value.copy(showAll = !_state.value.showAll)
                refreshInternal(_state.value.selectedSerial, isManual = false)
            }
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun refreshInternal(serial: String?, isManual: Boolean) {
        scope.launch {
            if (serial.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    data = emptyList(),
                    isLoading = false,
                    errorMessage = "No device selected",
                )
                return@launch
            }

            if (isManual) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            val command = PsCommand(
                serial = serial,
                showAll = _state.value.showAll,
            )

            when (val result = adbManager.run(command)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        data = result.value.entries,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        data = emptyList(),
                        isLoading = false,
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }
        }
    }
}
