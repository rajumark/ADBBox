package com.adbstudio.desktop.ui.component

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.device.PackageContextAction
import com.adbstudio.desktop.device.PackageFilter
import com.adbstudio.desktop.device.PackageInfo
import com.adbstudio.desktop.util.loadIconPainter

@Composable
fun PackageListView(
    packages: List<PackageInfo>,
    selectedPackage: PackageInfo?,
    onPackageSelected: (PackageInfo) -> Unit,
    packageFilter: PackageFilter,
    onFilterChange: (PackageFilter) -> Unit,
    onInstallApk: () -> Unit,
    onPackageContextAction: (PackageContextAction, String) -> Unit,
    batchMode: Boolean,
    selectedBatch: Set<PackageInfo>,
    onBatchToggle: (PackageInfo) -> Unit,
    onBatchCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPackage by remember { mutableStateOf<PackageInfo?>(null) }

    val filtered = remember(packages, searchQuery) {
        if (searchQuery.isBlank()) {
            packages
        } else {
            packages.filter {
                it.packageName.contains(searchQuery, ignoreCase = true) ||
                it.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    var focusedIndex by remember(filtered) { mutableStateOf(if (filtered.isNotEmpty()) 0 else -1) }

    LaunchedEffect(focusedIndex) {
        if (focusedIndex in filtered.indices) {
            listState.animateScrollToItem(focusedIndex)
        }
    }

    if (showFilterDialog) {
        PackageFilterDialog(
            current = packageFilter,
            onSelect = { filter ->
                showFilterDialog = false
                onFilterChange(filter)
            },
            onDismiss = { showFilterDialog = false },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && !batchMode) {
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
        if (batchMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${selectedBatch.size} Selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = onBatchCancel) { Text("Cancel") }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { value ->
                                    searchQuery = value
                                    focusedIndex = if (filtered.isNotEmpty()) 0 else -1
                                },
                                modifier = Modifier.fillMaxWidth(),
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
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (showMenu) {
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Types") },
                                onClick = {
                                    showMenu = false
                                    showFilterDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Install App") },
                                onClick = {
                                    showMenu = false
                                    onInstallApk()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Batch Operation") },
                                onClick = {
                                    showMenu = false
                                    onBatchCancel()
                                },
                            )
                        }
                    }
                }
            }
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
                    val bgColor = if (isFocused && !batchMode) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }

                    if (batchMode) {
                        val batchIconPainter = remember(pkg.iconLocalPath) { loadIconPainter(pkg.iconLocalPath) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(bgColor)
                                .clickable { onBatchToggle(pkg) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = pkg in selectedBatch,
                                onCheckedChange = { onBatchToggle(pkg) },
                            )
                            Spacer(Modifier.width(8.dp))
                            if (batchIconPainter != null) {
                                Image(
                                    painter = batchIconPainter,
                                    contentDescription = pkg.label,
                                    modifier = Modifier.size(20.dp).padding(end = 6.dp),
                                )
                            }
                            Text(
                                text = pkg.label,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        PackageListItem(
                            pkg = pkg,
                            isFocused = isFocused,
                            shape = shape,
                            bgColor = bgColor,
                            showContextMenu = showContextMenu,
                            contextMenuPackage = contextMenuPackage,
                            onFocus = {
                                focusedIndex = index
                                onPackageSelected(pkg)
                            },
                            onContextMenu = {
                                contextMenuPackage = pkg
                                showContextMenu = true
                            },
                            onDismissContextMenu = {
                                showContextMenu = false
                                contextMenuPackage = null
                            },
                            onAction = { action ->
                                showContextMenu = false
                                contextMenuPackage = null
                                onPackageContextAction(action, pkg.packageName)
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

private val contextMenuItems = listOf(
    "Open" to PackageContextAction.Open,
    "Force Stop" to PackageContextAction.ForceStop,
    "Restart" to PackageContextAction.Restart,
    "Uninstall" to PackageContextAction.Uninstall,
    "Clear Data" to PackageContextAction.ClearData,
    "Enable" to PackageContextAction.Enable,
    "Disable" to PackageContextAction.Disable,
    "Home" to PackageContextAction.Home,
    null,
    "Copy" to PackageContextAction.Copy,
    "Open App Info" to PackageContextAction.OpenAppInfo,
    "View at Playstore" to PackageContextAction.ViewAtPlaystore,
    "View at Desktop" to PackageContextAction.ViewAtDesktop,
    "Find online" to PackageContextAction.FindOnline,
)

@Composable
private fun PackageListItem(
    pkg: PackageInfo,
    isFocused: Boolean,
    shape: RoundedCornerShape,
    bgColor: Color,
    showContextMenu: Boolean,
    contextMenuPackage: PackageInfo?,
    onFocus: () -> Unit,
    onContextMenu: () -> Unit,
    onDismissContextMenu: () -> Unit,
    onAction: (PackageContextAction) -> Unit,
) {
    var showMoreSubmenu by remember { mutableStateOf(false) }

    val iconPainter = remember(pkg.iconLocalPath) {
        loadIconPainter(pkg.iconLocalPath)
    }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(bgColor)
                .clickable { onFocus() }
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconPainter != null) {
                Image(
                    painter = iconPainter,
                    contentDescription = pkg.label,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                )
            } else {
                Spacer(Modifier.size(24.dp).padding(end = 8.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pkg.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
                Text(
                    text = pkg.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isFocused) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }

            Box {
                IconButton(
                    onClick = onContextMenu,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                DropdownMenu(
                    expanded = showContextMenu && contextMenuPackage == pkg,
                    onDismissRequest = {
                        showMoreSubmenu = false
                        onDismissContextMenu()
                    },
                ) {
                    if (showMoreSubmenu) {
                        DropdownMenuItem(
                            text = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                            onClick = { showMoreSubmenu = false },
                        )
                        contextMenuItems.drop(5).forEach { item ->
                            if (item == null) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            } else {
                                val (label, action) = item
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        showMoreSubmenu = false
                                        onAction(action)
                                    },
                                )
                            }
                        }
                    } else {
                        contextMenuItems.take(5).filterNotNull().forEach { item ->
                            val (label, action) = item
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    showMoreSubmenu = false
                                    onAction(action)
                                },
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("More", modifier = Modifier.weight(1f))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            },
                            onClick = { showMoreSubmenu = true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PackageFilterDialog(
    current: PackageFilter,
    onSelect: (PackageFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Package Types",
                style = MaterialTheme.typography.titleSmall,
            )
        },
        text = {
            Column {
                PackageFilter.entries.forEachIndexed { index, filter ->
                    val shape = when {
                        PackageFilter.entries.size == 1 -> RoundedCornerShape(8.dp)
                        index == 0 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        index == PackageFilter.entries.lastIndex -> RoundedCornerShape(
                            bottomStart = 8.dp, bottomEnd = 8.dp,
                        )
                        else -> RoundedCornerShape(2.dp)
                    }
                    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(bgColor)
                            .clickable { onSelect(filter) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = filter == current,
                            onClick = { onSelect(filter) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            filter.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}
