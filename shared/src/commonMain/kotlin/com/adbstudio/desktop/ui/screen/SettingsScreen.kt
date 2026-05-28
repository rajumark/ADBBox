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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.feature.settings.data.SettingsCategory
import com.adbstudio.desktop.feature.settings.presentation.SettingsEvent
import com.adbstudio.desktop.feature.settings.presentation.SettingsUiState
import com.adbstudio.desktop.feature.settings.presentation.SettingsViewModel
import com.adbstudio.desktop.ui.component.SplitView

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
) {
    val state by viewModel.state.collectAsState()

    SplitView(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        initialRatio = 0.3f,
        left = {
            CategoryPanel(
                categories = state.categories,
                selectedCategory = state.selectedCategory,
                onCategorySelect = { viewModel.onEvent(SettingsEvent.SelectCategory(it)) },
            )
        },
        right = {
            SettingsItemsPanel(
                state = state,
                onSearchChange = { viewModel.onEvent(SettingsEvent.SetSearchQuery(it)) },
                onSettingClick = { viewModel.onEvent(SettingsEvent.OpenSetting(it)) },
            )
        },
    )
}

@Composable
private fun CategoryPanel(
    categories: List<SettingsCategory>,
    selectedCategory: SettingsCategory?,
    onCategorySelect: (SettingsCategory) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(categories) { index, category ->
                val isSelected = category.id == selectedCategory?.id
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface

                ListItem(
                    headlineContent = {
                        Text(
                            text = category.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = "${category.items.size} settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    leadingContent = {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
                    modifier = Modifier
                        .background(bgColor)
                        .clickable { onCategorySelect(category) },
                )
            }
        }
    }
}

@Composable
private fun SettingsItemsPanel(
    state: SettingsUiState,
    onSearchChange: (String) -> Unit,
    onSettingClick: (com.adbstudio.desktop.feature.settings.data.SettingsItem) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = state.selectedCategory?.displayName ?: "Settings",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), strokeWidth = 2.dp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            placeholder = { Text("Search settings...") },
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.selectedCategory == null) {
            Text(
                text = "Select a category",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            return
        }

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

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = state.filteredItems,
                key = { it.id },
            ) { item ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = item.displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = item.intent,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    modifier = Modifier.clickable { onSettingClick(item) },
                )
            }
        }
    }
}
