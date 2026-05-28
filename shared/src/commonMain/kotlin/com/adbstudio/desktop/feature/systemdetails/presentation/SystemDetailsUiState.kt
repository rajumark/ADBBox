package com.adbstudio.desktop.feature.systemdetails.presentation

data class SystemDetailsUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val currentTab: DetailsTab = DetailsTab.Features,
    val features: List<String> = emptyList(),
    val permissions: List<String> = emptyList(),
    val libraries: List<String> = emptyList(),
) {
    val activeData: List<String>
        get() = when (currentTab) {
            DetailsTab.Features -> features
            DetailsTab.Permissions -> permissions
            DetailsTab.Libraries -> libraries
        }

    val filteredData: List<String>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return activeData
            return activeData.filter { it.lowercase().contains(query) }
        }
}
