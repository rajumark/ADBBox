@file:OptIn(ExperimentalMaterial3Api::class)

package com.adbstudio.desktop.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.feature.contacts.presentation.ContactsEvent
import com.adbstudio.desktop.feature.contacts.presentation.ContactsUiState
import com.adbstudio.desktop.feature.contacts.presentation.ContactsViewModel
import com.adbstudio.desktop.ui.component.SplitView

@Composable
fun ContactsScreen(
    viewModel: ContactsViewModel,
) {
    val state by viewModel.state.collectAsState()

    SplitView(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        initialRatio = 0.3f,
        left = {
            ContactsListPanel(
                state = state,
                onContactClick = { viewModel.onEvent(ContactsEvent.SelectContact(it)) },
                onSearchChange = { viewModel.onEvent(ContactsEvent.SetSearchQuery(it)) },
                onRefresh = { viewModel.onEvent(ContactsEvent.Refresh) },
            )
        },
        right = {
            ContactsDetailPanel(
                selectedContact = state.selectedContact,
                onSearchChange = { viewModel.onEvent(ContactsEvent.SetSearchQuery(it)) },
                onRefresh = { viewModel.onEvent(ContactsEvent.Refresh) },
            )
        },
    )
}

@Composable
private fun ContactsListPanel(
    state: ContactsUiState,
    onContactClick: (ContactMaster) -> Unit,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Contacts",
                style = MaterialTheme.typography.titleLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = androidx.compose.material3.Icons.Default.Refresh,
                        contentDescription = "Refresh",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            placeholder = { Text("Search in ${state.filteredContacts.size} contacts") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        if (state.actionMessage != null) {
            Text(
                text = state.actionMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = state.filteredContacts,
                key = { it.contactId },
            ) { contact ->
                ListItem(
                    leadingContent = {
                        Text(
                            text = contact.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.circle,
                                )
                                .wrapContentSize(align = Alignment.Center),
                        )
                    },
                    headlineContent = {
                        Text(
                            text = contact.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = contact.numbers.joinToString(separator = ", ") ?: "No number",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier
                        .clickable { onContactClick(contact) }
                        .padding(horizontal = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ContactsDetailPanel(
    selectedContact: ContactMaster?,
    onSearchChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    if (selectedContact == null) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Select a contact to see details",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = selectedContact.displayName,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onRefresh) {
                    Icon(
                        imageVector = androidx.compose.material3.Icons.Default.Refresh,
                        contentDescription = "Refresh",
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = "",
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            placeholder = { Text("Search in details...") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = selectedContact.rawData,
                key = { it.keys.firstOrNull() ?: "" },
            ) { entry ->
                entry.entries.sortedBy { it.key }.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp, horizontal = 8.dp),
                    ) {
                        Text(
                            text = key,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(120.dp),
                        )
                        Text(
                            text = value.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
