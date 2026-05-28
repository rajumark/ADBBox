package com.adbstudio.desktop.feature.lifecycle.presentation

sealed interface LifecycleEvent {
    data object Refresh : LifecycleEvent
}
