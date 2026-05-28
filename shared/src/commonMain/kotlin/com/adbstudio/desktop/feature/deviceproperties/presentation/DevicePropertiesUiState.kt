package com.adbstudio.desktop.feature.deviceproperties.presentation

data class DevicePropertiesUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val data: List<Pair<String, String>> = emptyList(),
) {
    val filteredData: List<Pair<String, String>>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return data
            return data.filter { (key, value) ->
                key.lowercase().contains(query) || value.lowercase().contains(query)
            }
        }
}
