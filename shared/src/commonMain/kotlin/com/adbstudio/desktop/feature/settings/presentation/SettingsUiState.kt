package com.adbstudio.desktop.feature.settings.presentation

import com.adbstudio.desktop.feature.settings.data.SettingsCategory

data class SettingsUiState(
    val selectedSerial: String? = null,
    val categories: List<SettingsCategory> = emptyList(),
    val selectedCategory: SettingsCategory? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val actionMessage: String? = null,
) {
    val filteredItems: List<com.adbstudio.desktop.feature.settings.data.SettingsItem>
        get() {
            val category = selectedCategory ?: return emptyList()
            val query = searchQuery.trim().lowercase()
            return if (query.isEmpty()) category.items
            else category.items.filter {
                it.displayName.lowercase().contains(query) || it.intent.lowercase().contains(query)
            }
        }
}
