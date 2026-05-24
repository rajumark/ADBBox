package com.adbstudio.desktop.ui.component

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.device.PackageInfo

@Composable
fun PackageListView(
    packages: List<PackageInfo>,
    selectedPackage: PackageInfo?,
    onPackageSelected: (PackageInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val filtered = remember(packages, searchQuery) {
        if (searchQuery.isBlank()) {
            packages
        } else {
            packages.filter { it.packageName.contains(searchQuery, ignoreCase = true) }
        }
    }

    var focusedIndex by remember(filtered) { mutableStateOf(if (filtered.isNotEmpty()) 0 else -1) }

    LaunchedEffect(focusedIndex) {
        if (focusedIndex in filtered.indices) {
            listState.animateScrollToItem(focusedIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionDown -> {
                            if (filtered.isNotEmpty()) {
                                focusedIndex = if (focusedIndex < filtered.lastIndex) focusedIndex + 1 else 0
                                onPackageSelected(filtered[focusedIndex])
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            if (filtered.isNotEmpty()) {
                                focusedIndex = if (focusedIndex > 0) focusedIndex - 1 else filtered.lastIndex
                                onPackageSelected(filtered[focusedIndex])
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = { value ->
                    searchQuery = value
                    focusedIndex = if (filtered.isNotEmpty()) 0 else -1
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search ${packages.size} packages",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                itemsIndexed(filtered) { index, pkg ->
                    val isFocused = index == focusedIndex
                    val shape = when {
                        filtered.size == 1 -> RoundedCornerShape(8.dp)
                        index == 0 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        index == filtered.lastIndex -> RoundedCornerShape(
                            bottomStart = 8.dp, bottomEnd = 8.dp,
                        )
                        else -> RoundedCornerShape(2.dp)
                    }
                    val bgColor = if (isFocused) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(bgColor)
                            .clickable {
                                focusedIndex = index
                                onPackageSelected(pkg)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = pkg.packageName,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isFocused) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }

            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(8.dp),
                adapter = rememberScrollbarAdapter(listState),
            )
        }
    }
}
