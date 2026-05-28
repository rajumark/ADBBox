package com.adbstudio.desktop.adb.model.lifecycle

data class LogEntry(
    val time: String? = null,
    val type: String? = null,
    val packageName: String? = null,
    val className: String? = null,
    val instanceId: String? = null,
    val taskRootPackage: String? = null,
    val taskRootClass: String? = null,
    val flags: String? = null
)
