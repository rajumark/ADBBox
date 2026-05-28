package com.adbstudio.desktop.feature.contacts.presentation

data class ContactMaster(
    val contactId: String,
    val displayName: String,
    val numbers: List<String>,
    val rawData: List<Map<String, String>>,
)
