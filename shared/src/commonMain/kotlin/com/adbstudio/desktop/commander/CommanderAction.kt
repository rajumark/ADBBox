package com.adbstudio.desktop.commander

data class CommanderAction(
    val label: String,
    val category: String,
    val shortcutHint: String = "",
    val action: () -> Unit = {},
)
