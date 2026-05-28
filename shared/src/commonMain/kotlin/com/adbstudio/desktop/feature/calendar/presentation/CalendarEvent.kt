package com.adbstudio.desktop.feature.calendar.presentation

import com.adbstudio.desktop.adb.model.calendar.CalendarQueryType

sealed interface CalendarEvent {
    data object Refresh : CalendarEvent
    data class SetQueryType(val type: CalendarQueryType) : CalendarEvent
    data class SetSearchQuery(val query: String) : CalendarEvent
    data object ToggleOriginal : CalendarEvent
}
