package com.adbstudio.desktop.device

enum class PermissionGroup { Runtime, Requested, Install }

data class PermissionInfo(
    val permission: String,
    val granted: Boolean,
    val group: PermissionGroup,
)
