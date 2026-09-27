package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.data.InstalledApp

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    apps: List<InstalledApp>?,
    indexError: String? = null,
    contentPadding: PaddingValues = PaddingValues(),
    onAppClick: (InstalledApp) -> Unit = {}
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var sortByPackage by rememberSaveable { mutableStateOf(false) }
    var showSystemApps by rememberSaveable { mutableStateOf(true) }
    var showOtherUserApps by rememberSaveable { mutableStateOf(false) }
    var sortMenuVisible by rememberSaveable { mutableStateOf(false) }
    var filterMenuVisible by rememberSaveable { mutableStateOf(false) }
    val visibleApps = apps?.asSequence()
        ?.filter {
            (it.userId == 0 || showOtherUserApps) &&
                (showSystemApps || !it.isSystem)
        }
        ?.filter {
            searchText.isBlank() || it.label.contains(searchText, ignoreCase = true) ||
                it.packageName.contains(searchText, ignoreCase = true)
        }
        ?.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) {
            if (sortByPackage) it.packageName else it.label
        })
        ?.toList()

    MainScreenScaffold(
        title = "应用",
        contentPadding = contentPadding,
        modifier = modifier,
        scrollable = false,
        actions = {
            Box {
                IconButton(onClick = { sortMenuVisible = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序方式")
                }
                DropdownMenu(
                    expanded = sortMenuVisible,
                    onDismissRequest = { sortMenuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("按名称排序") },
                        leadingIcon = if (!sortByPackage) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            sortByPackage = false
                            sortMenuVisible = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("按包名排序") },
                        leadingIcon = if (sortByPackage) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            sortByPackage = true
                            sortMenuVisible = false
                        }
                    )
                }
            }
            Box {
                IconButton(onClick = { filterMenuVisible = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "应用筛选")
                }
                DropdownMenu(
                    expanded = filterMenuVisible,
                    onDismissRequest = { filterMenuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("显示系统软件") },
                        leadingIcon = if (showSystemApps) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = { showSystemApps = !showSystemApps }
                    )
                    DropdownMenuItem(
                        text = { Text("显示其他用户空间软件") },
                        leadingIcon = if (showOtherUserApps) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = { showOtherUserApps = !showOtherUserApps }
                    )
                }
            }
        },
    ) { contentModifier ->
        Column(modifier = contentModifier) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
                placeholder = { Text("搜索应用或包名") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions.Default,
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除搜索")
                        }
                    }
                }
            )
            when (val currentApps = visibleApps) {
                null -> Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator()
                }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    item {
                        indexError?.let {
                            Text(
                                text = it,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    itemsIndexed(currentApps, key = { _, app -> app.packageName }) { index, app ->
                        AppRow(app, index = index, count = currentApps.size, onClick = { onAppClick(app) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, index: Int, count: Int, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        content = { Text(app.label, maxLines = 1) },
        supportingContent = {
            Text(
                app.packageName,
                maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Image(
                bitmap = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(48.dp)
            )
        },
        trailingContent = {
            Text(
                text = if (app.isAdapted) "已适配" else "未适配",
                style = MaterialTheme.typography.labelMedium,
                color = if (app.isAdapted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
