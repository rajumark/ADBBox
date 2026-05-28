package com.adbstudio.desktop.feature.messages.presentation

import com.adbstudio.desktop.adb.model.messages.QueryMessagesContentCommand

data class MessagesUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showOriginal: Boolean = false,
    val searchQuery: String = "",
    val data: List<Map<String, String>> = emptyList(),
) {
    val filteredData: List<Map<String, String>>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return data
            return data.filter { row ->
                row.any { (key, value) ->
                    if (key in searchKeys) {
                        value.lowercase().contains(query)
                    } else {
                        false
                    }
                }
            }
        }

    val columns: List<String>
        get() {
            if (data.isEmpty()) return emptyList()
            val keys = data.flatMap { it.keys }.distinct()
            return keys.sortedBy { key ->
                val index = QueryMessagesContentCommand.messagesColumnSortOrder.indexOf(key)
                if (index == -1) 9999 else index
            }
        }

    companion object {
        private val searchKeys = setOf("type", "date", "body", "date_sent", "address")
    }
}
