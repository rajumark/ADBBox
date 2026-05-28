package com.adbstudio.desktop.feature.contacts.presentation

import com.adbstudio.desktop.adb.AdbManager
import com.adbstudio.desktop.adb.model.contacts.QueryContactsContentCommand
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

class ContactsViewModel(
    private val adbManager: AdbManager,
    private val deviceRepository: DeviceRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(ContactsUiState())
    val state: StateFlow<ContactsUiState> = _state.asStateFlow()

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

    fun onEvent(event: ContactsEvent) {
        when (event) {
            ContactsEvent.Refresh -> refreshInternal(_state.value.selectedSerial, isManual = true)
            is ContactsEvent.SetSearchQuery -> {
                _state.value = _state.value.copy(searchQuery = event.query)
            }
            is ContactsEvent.SelectContact -> {
                _state.value = _state.value.copy(selectedContact = event.contact)
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
                    contacts = emptyList(),
                    isLoading = false,
                    errorMessage = "No device selected",
                )
                return@launch
            }

            if (isManual) {
                _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            }

            when (val result = adbManager.run(QueryContactsContentCommand(serial))) {
                is AppResult.Success -> {
                    val contacts = buildContacts(result.value)
                    _state.value = _state.value.copy(
                        contacts = contacts,
                        isLoading = false,
                        errorMessage = null,
                    )
                }
                is AppResult.Error -> {
                    _state.value = _state.value.copy(
                        contacts = emptyList(),
                        isLoading = false,
                        errorMessage = result.error.toUserMessage(),
                    )
                }
            }
        }
    }

    private fun buildContacts(rows: List<Map<String, String>>): List<ContactMaster> {
        val grouped = rows
            .filter { it.containsKey(QueryContactsContentCommand.CONTACT_ID) }
            .groupBy { it[QueryContactsContentCommand.CONTACT_ID].orEmpty() }

        return grouped.map { (contactId, rowsForContact) ->
            val displayName = rowsForContact
                .firstOrNull { it[QueryContactsContentCommand.DISPLAY_NAME].orEmpty().isNotBlank() }
                ?.get(QueryContactsContentCommand.DISPLAY_NAME).orEmpty()

            val numbers = rowsForContact
                .filter { it[QueryContactsContentCommand.MIMETYPE] == QueryContactsContentCommand.MIMETYPE_PHONE_V2 }
                .mapNotNull { it[QueryContactsContentCommand.DATA1] }
                .distinct()

            ContactMaster(
                contactId = contactId,
                displayName = displayName.ifBlank { numbers.firstOrNull() ?: "Unknown" },
                numbers = numbers,
                rawData = rowsForContact,
            )
        }.sortedByDescending { it.contactId.toIntOrNull() ?: 0 }
    }
}
