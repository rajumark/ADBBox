package com.adbstudio.desktop.core.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SimpleEventBus : EventBus {
    private val _events = MutableSharedFlow<AppEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override suspend fun publish(event: AppEvent) {
        _events.emit(event)
    }

    override fun events(): Flow<AppEvent> = _events.asSharedFlow()
}

