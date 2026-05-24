package com.adbstudio.desktop.commander

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CommanderDialog(
    registry: CommanderRegistry,
    onDismiss: () -> Unit,
    onActionSelected: (CommanderAction) -> Unit,
) {
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var selectedIndex by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    val filteredActions by remember {
        derivedStateOf {
            val q = query.text.trim().lowercase()
            if (q.isEmpty()) {
                registry.actions
            } else {
                registry.actions.filter { action ->
                    action.label.lowercase().contains(q) ||
                            action.category.lowercase().contains(q)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(filteredActions.size, selectedIndex) {
        if (filteredActions.isNotEmpty() && selectedIndex in filteredActions.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.Escape -> {
                            onDismiss()
                            true
                        }
                        Key.Enter -> {
                            if (filteredActions.isNotEmpty() && selectedIndex in filteredActions.indices) {
                                onActionSelected(filteredActions[selectedIndex])
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (filteredActions.isNotEmpty()) {
                                selectedIndex = if (selectedIndex > 0) selectedIndex - 1
                                else filteredActions.lastIndex
                            }
                            true
                        }
                        Key.DirectionDown -> {
                            if (filteredActions.isNotEmpty()) {
                                selectedIndex = if (selectedIndex < filteredActions.lastIndex) selectedIndex + 1
                                else 0
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .onFocusChanged { },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .width(520.dp)
                .heightIn(max = 480.dp),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                TextField(
                    value = query,
                    onValueChange = {
                        query = it
                        selectedIndex = 0
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = { Text("Search commands…") },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (filteredActions.isNotEmpty() && selectedIndex in filteredActions.indices) {
                                onActionSelected(filteredActions[selectedIndex])
                            }
                        },
                    ),
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredActions.isEmpty()) {
                    Text(
                        text = "No results found",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = "${filteredActions.size} results",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        itemsIndexed(filteredActions) { index, action ->
                            CommanderActionItem(
                                action = action,
                                isSelected = index == selectedIndex,
                                onClick = { onActionSelected(action) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommanderActionItem(
    action: CommanderAction,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = modifier
            .background(backgroundColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = action.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.5f),
                )
                if (action.shortcutHint.isNotEmpty()) {
                    Text(
                        text = "  ·  ${action.shortcutHint}",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}
