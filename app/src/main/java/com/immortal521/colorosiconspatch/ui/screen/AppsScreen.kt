package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.R
import com.immortal521.colorosiconspatch.data.InstalledApp

@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppsScreen(
    modifier: Modifier = Modifier,
    apps: List<InstalledApp>?,
    indexError: String? = null,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),

    onAppClick: (InstalledApp) -> Unit = {}
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var sortBy by rememberSaveable { mutableStateOf(AppSort.NAME) }
    var showSystemApps by rememberSaveable { mutableStateOf(false) }
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
        ?.sortedWith(
            when (sortBy) {
                AppSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
                AppSort.PACKAGE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.packageName }
                AppSort.RECENT -> compareByDescending { it.installTime }
            }
        )
        ?.toList()

    MainScreenScaffold(
        title = stringResource(R.string.apps),
        contentPadding = contentPadding,
        modifier = modifier,
        scrollable = false,
        actions = {
            Box {
                IconButton(onClick = { sortMenuVisible = true }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(R.string.sort)
                    )
                }
                DropdownMenu(
                    expanded = sortMenuVisible,
                    onDismissRequest = { sortMenuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_name)) },
                        leadingIcon = if (sortBy == AppSort.NAME) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            sortBy = AppSort.NAME
                            sortMenuVisible = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_package)) },
                        leadingIcon = if (sortBy == AppSort.PACKAGE) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            sortBy = AppSort.PACKAGE
                            sortMenuVisible = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.sort_recent)) },
                        leadingIcon = if (sortBy == AppSort.RECENT) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = {
                            sortBy = AppSort.RECENT
                            sortMenuVisible = false
                        }
                    )
                }
            }
            Box {
                IconButton(onClick = { filterMenuVisible = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.filter_apps)
                    )
                }
                DropdownMenu(
                    expanded = filterMenuVisible,
                    onDismissRequest = { filterMenuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.show_system_apps)) },
                        leadingIcon = if (showSystemApps) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null,
                        onClick = { showSystemApps = !showSystemApps }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.show_other_users)) },
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
                placeholder = { Text(stringResource(R.string.search_apps)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions.Default,
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_search)
                            )
                        }
                    }
                }
            )
            when (visibleApps) {
                null -> LoadingIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )

                else -> {
                    val pullToRefreshState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        modifier = Modifier.weight(1f),
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        state = pullToRefreshState,
                        indicator = {
                            PullToRefreshDefaults.LoadingIndicator(
                                modifier = Modifier.align(Alignment.TopCenter),
                                isRefreshing = isRefreshing,
                                state = pullToRefreshState
                            )
                        }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 16.dp
                            )
                        ) {
                            item {
                                indexError?.let {
                                    Text(
                                        text = it,
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 8.dp
                                        ),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            itemsIndexed(
                                visibleApps,
                                key = { _, app -> app.packageName }) { index, app ->
                                AppRow(
                                    app,
                                    index = index,
                                    count = visibleApps.size,
                                    onClick = { onAppClick(app) })
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class AppSort { NAME, PACKAGE, RECENT }

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
                text = stringResource(if (app.isAdapted) R.string.adapted else R.string.not_adapted),
                style = MaterialTheme.typography.labelMedium,
                color = if (app.isAdapted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}
