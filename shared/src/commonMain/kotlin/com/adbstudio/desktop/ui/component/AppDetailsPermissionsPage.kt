package com.adbstudio.desktop.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adbstudio.desktop.device.PermissionGroup
import com.adbstudio.desktop.device.PermissionInfo
import kotlinx.coroutines.launch

@Composable
fun AppDetailsPermissionsPage(
    packageName: String,
    onFetchPermissions: suspend (String) -> List<PermissionInfo>,
    onGrantPermission: suspend (String, String) -> Unit,
    onRevokePermission: suspend (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var allPermissions by remember { mutableStateOf<List<PermissionInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedGroup by remember { mutableStateOf(PermissionGroup.Runtime) }
    var searchQuery by remember { mutableStateOf("") }
    var refreshTrigger by remember { mutableStateOf(0) }
    var operationInProgress by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(packageName, refreshTrigger) {
        isLoading = true
        errorMessage = null
        operationInProgress = false
        try {
            allPermissions = onFetchPermissions(packageName)
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load permissions"
        }
        isLoading = false
    }

    val filteredPermissions = remember(allPermissions, selectedGroup, searchQuery) {
        allPermissions.filter { perm ->
            perm.group == selectedGroup &&
                (searchQuery.isEmpty() || perm.permission.contains(searchQuery, ignoreCase = true))
        }
    }

    val runtimePermissions = remember(allPermissions) {
        allPermissions.filter { it.group == PermissionGroup.Runtime }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search permissions", style = MaterialTheme.typography.bodySmall) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(16.dp))
                },
                singleLine = true,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodySmall,
            )
            if (selectedGroup == PermissionGroup.Runtime && !operationInProgress) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        val ungranted = runtimePermissions.filter { !it.granted }.map { it.permission }
                        if (ungranted.isNotEmpty()) {
                            operationInProgress = true
                            scope.launch {
                                ungranted.forEach { p ->
                                    try { onGrantPermission(packageName, p) } catch (_: Exception) {}
                                }
                                refreshTrigger++
                                operationInProgress = false
                            }
                        }
                    },
                    enabled = runtimePermissions.any { !it.granted },
                ) {
                    Text("Grant All", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(
                    onClick = {
                        val granted = runtimePermissions.filter { it.granted }.map { it.permission }
                        if (granted.isNotEmpty()) {
                            operationInProgress = true
                            scope.launch {
                                granted.forEach { p ->
                                    try { onRevokePermission(packageName, p) } catch (_: Exception) {}
                                }
                                refreshTrigger++
                                operationInProgress = false
                            }
                        }
                    },
                    enabled = runtimePermissions.any { it.granted },
                ) {
                    Text("Revoke All", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (operationInProgress) {
                Spacer(Modifier.width(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        SecondaryScrollableTabRow(
            selectedTabIndex = PermissionGroup.entries.indexOf(selectedGroup),
            edgePadding = 0.dp,
            divider = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp)
                .height(36.dp),
        ) {
            PermissionGroup.entries.forEach { group ->
                val count = allPermissions.count { it.group == group }
                Tab(
                    selected = selectedGroup == group,
                    onClick = { selectedGroup = group },
                    text = {
                        Text(
                            "${group.name} ($count)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                filteredPermissions.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No ${selectedGroup.name.lowercase()} permissions found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredPermissions, key = { it.permission }) { perm ->
                            PermissionRow(
                                permission = perm,
                                canToggle = selectedGroup == PermissionGroup.Runtime && !operationInProgress,
                                onToggle = { grant ->
                                    scope.launch {
                                        try {
                                            if (grant) onGrantPermission(packageName, perm.permission)
                                            else onRevokePermission(packageName, perm.permission)
                                        } catch (_: Exception) {}
                                        refreshTrigger++
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    permission: PermissionInfo,
    canToggle: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val checked = permission.granted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (canToggle) Modifier.clickable { onToggle(!checked) }
                else Modifier
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (canToggle) {
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle(it) },
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
        } else if (permission.group == PermissionGroup.Install) {
            Icon(
                imageVector = if (checked) Icons.Default.CheckCircle else Icons.Default.CheckCircle,
                contentDescription = if (checked) "Granted" else "Denied",
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
        } else {
            Spacer(Modifier.width(30.dp))
        }
        Text(
            text = permission.permission,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
