package com.adbstudio.desktop.feature.calendar.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.calendar.CalendarQueryType
import com.adbstudio.desktop.adb.model.calendar.QueryCalendarContentCommand
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

class CalendarViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

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

    fun onEvent(event: CalendarEvent) {
        when (event) {
            CalendarEvent.Refresh -> refreshInternal(_state.value.selectedSerial, isManual = true)
            is CalendarEvent.SetQueryType -> {
                _state.value = _state.value.copy(queryType = event.type)
                refreshInternal(_state.value.selectedSerial, isManual = false)
            }
            is CalendarEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            CalendarEvent.ToggleOriginal -> {
                _state.value = _state.value.copy(showOriginal = !_state.value.showOriginal)
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

            val command = QueryCalendarContentCommand(
                serial = serial,
                queryType = _state.value.queryType,
                showOriginal = _state.value.showOriginal,
            )

            when (val result = adbManager.run(command)) {
                is AppResult.Success -> {
                    _state.value = _state.value.copy(
                        data = result.value,
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
