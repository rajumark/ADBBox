package com.adbstudio.desktop.feature.contacts.presentation

data class ContactsUiState(
    val selectedSerial: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val contacts: List<ContactMaster> = emptyList(),
    val selectedContact: ContactMaster? = null,
) {
    val filteredContacts: List<ContactMaster>
        get() {
            val query = searchQuery.trim().lowercase()
            if (query.isEmpty()) return contacts
            return contacts.filter { contact ->
                contact.displayName.lowercase().contains(query) ||
                    contact.contactId.lowercase().contains(query) ||
                    contact.numbers.any { it.lowercase().contains(query) }
            }
        }
}
