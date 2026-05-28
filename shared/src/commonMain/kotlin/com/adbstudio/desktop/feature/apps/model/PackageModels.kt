package com.adbstudio.desktop.feature.apps.model

data class PackageInfo(
    val packageName: String,
    val versionName: String = "",
    val versionCode: Long = 0,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val installer: String = "",
    val firstInstallTime: String = "",
    val lastUpdateTime: String = "",
    val codePath: String = "",
    val dataDir: String = "",
    val primaryCpuAbi: String = "",
    val flags: String = "",
)

data class PackagePermission(
    val name: String,
    val type: PermissionType,
    val granted: Boolean,
)

enum class PermissionType {
    REQUESTED,
    INSTALL,
    RUNTIME,
}
