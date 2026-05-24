package com.adbstudio.desktop.commander

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.toMutableStateList

class CommanderRegistry {
    private val _actions = mutableListOf<CommanderAction>().toMutableStateList()

    val actions: List<CommanderAction> get() = _actions

    fun register(action: CommanderAction) {
        _actions.add(action)
    }

    fun registerAll(actions: List<CommanderAction>) {
        _actions.addAll(actions)
    }

    fun clear() {
        _actions.clear()
    }
}

val LocalCommanderRegistry = staticCompositionLocalOf { CommanderRegistry() }
