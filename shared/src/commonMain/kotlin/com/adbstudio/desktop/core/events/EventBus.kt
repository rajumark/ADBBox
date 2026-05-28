package com.adbstudio.desktop.core.events

import kotlinx.coroutines.flow.Flow

interface EventBus {
    suspend fun publish(event: AppEvent)
    fun events(): Flow<AppEvent>
}

