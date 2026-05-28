package com.adbstudio.desktop.feature.apps.presentation

import com.adbstudio.desktop.feature.apps.model.AppType
import com.adbstudio.desktop.feature.apps.model.PackageInfo
import com.adbstudio.desktop.feature.apps.model.PackagePermission

data class AppsUiState(
    val selectedSerial: String? = null,
    val packages: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val appType: AppType = AppType.ALL,
    val searchQuery: String = "",
    val selectedPackage: String? = null,
    val foregroundPackage: String? = null,
    val pinnedPackages: Set<String> = emptySet(),
    val packageInfo: PackageInfo? = null,
    val packagePaths: List<String> = emptyList(),
    val permissions: List<PackagePermission> = emptyList(),
    val dumpsysSections: Map<String, String> = emptyMap(),
    val isActionRunning: Boolean = false,
    val actionMessage: String? = null,
    val detailsTab: DetailsTab = DetailsTab.INFO,
) {
    val filteredPackages: List<String>
        get() {
            val query = searchQuery.trim().lowercase()
            val filtered = if (query.isEmpty()) packages
            else packages.filter { it.lowercase().contains(query) }

            val pinSet = pinnedPackages
            return filtered.sortedWith(
                compareByDescending<String> { pinSet.contains(it) }
                    .thenByDescending { it == foregroundPackage }
                    .thenBy { it },
            )
        }
}

enum class DetailsTab(val displayName: String) {
    INFO("Info"),
    PERMISSIONS("Permissions"),
    DUMPSYS("Full Info"),
    PATHS("Paths & APKs"),
}

