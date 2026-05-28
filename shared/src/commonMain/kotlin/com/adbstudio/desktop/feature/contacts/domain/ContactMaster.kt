package com.adbstudio.desktop.feature.contacts.domain

data class ContactMaster(
    val contactId: String,
    val displayName: String,
    val phoneNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val rawData: List<Map<String, String?>> = emptyList()
)
