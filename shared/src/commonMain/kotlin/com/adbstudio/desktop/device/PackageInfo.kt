package com.adbstudio.desktop.device

data class PackageInfo(
    val packageName: String,
    val label: String = packageName,
    val iconLocalPath: String? = null,
    val versionName: String = "",
    val enabled: Boolean = true,
    val isSystem: Boolean = false,
)
