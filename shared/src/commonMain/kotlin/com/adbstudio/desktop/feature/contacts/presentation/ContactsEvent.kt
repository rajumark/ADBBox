package com.adbstudio.desktop.feature.contacts.presentation

sealed interface ContactsEvent {
    data object Refresh : ContactsEvent
    data class SetSearchQuery(val query: String) : ContactsEvent
    data class SelectContact(val contact: ContactMaster?) : ContactsEvent
}
