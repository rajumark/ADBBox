package com.adbstudio.desktop.feature.processes.presentation

data class ProcessesUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showAll: Boolean = true,
    val searchQuery: String = "",
    val data: List<Map<String, String>> = emptyList(),
) {
    val filteredData: List<Map<String, String>>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return data
            return data.filter { row ->
                row.values.any { value ->
                    value.lowercase().contains(query)
                }
            }
        }

    val columns: List<String>
        get() {
            if (data.isEmpty()) return emptyList()
            val keys = data.flatMap { it.keys }.distinct()
            return keys.sortedBy { key ->
                val index = com.adbstudio.desktop.adb.model.processes.PsCommand.columnSortOrder.indexOf(key)
                if (index == -1) 9999 else index
            }
        }
}
