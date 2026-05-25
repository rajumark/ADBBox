package com.adbstudio.desktop.device

data class PackageInfo(
    val packageName: String,
    val label: String = packageName,
    val iconLocalPath: String? = null,
)
