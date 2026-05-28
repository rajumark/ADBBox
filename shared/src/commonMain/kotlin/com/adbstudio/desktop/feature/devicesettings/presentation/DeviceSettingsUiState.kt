package com.adbstudio.desktop.feature.devicesettings.presentation

data class DeviceSettingsUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val currentTab: SettingsTab = SettingsTab.Global,
    val globalData: List<Pair<String, String>> = emptyList(),
    val systemData: List<Pair<String, String>> = emptyList(),
    val secureData: List<Pair<String, String>> = emptyList(),
) {
    val activeData: List<Pair<String, String>>
        get() = when (currentTab) {
            SettingsTab.Global -> globalData
            SettingsTab.System -> systemData
            SettingsTab.Secure -> secureData
            SettingsTab.All -> (globalData + systemData + secureData).distinct()
        }

    val filteredData: List<Pair<String, String>>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return activeData
            return activeData.filter { (key, value) ->
                key.lowercase().contains(query) || value.lowercase().contains(query)
            }
        }
}
