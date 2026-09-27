package com.immortal521.colorosiconspatch.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.immortal521.colorosiconspatch.R
import com.immortal521.colorosiconspatch.data.LogCategory
import com.immortal521.colorosiconspatch.data.LogEntry
import com.immortal521.colorosiconspatch.data.LogLevel
import com.immortal521.colorosiconspatch.data.LogStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var category by remember { mutableStateOf(LogCategory.RUNTIME) }
    var query by remember { mutableStateOf("") }
    var selectedLevels by remember { mutableStateOf(LogLevel.entries.toSet()) }
    var entries by remember { mutableStateOf(emptyList<LogEntry>()) }
    var showFilter by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    val saveLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) {
                val zip = LogStore.zip(context)
                context.contentResolver.openOutputStream(uri)
                    ?.use { output -> zip.inputStream().use { it.copyTo(output) } }
            }
        }

    fun reload() {
        entries = LogStore.read(context, category, selectedLevels, query)
    }
    LaunchedEffect(category, query, selectedLevels) {
        entries =
            withContext(Dispatchers.IO) { LogStore.read(context, category, selectedLevels, query) }
    }

    MainScreenScaffold(
        title = stringResource(R.string.logs),
        scrollable = false,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    stringResource(R.string.back)
                )
            }
        },
        actions = {
            IconButton(onClick = { showFilter = true }) {
                Icon(
                    Icons.Filled.FilterList,
                    stringResource(R.string.log_filter)
                )
            }
            IconButton(onClick = {
                val file = LogStore.zip(context)
                val uri =
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"; putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                ); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, null))
            }) { Icon(Icons.Filled.IosShare, stringResource(R.string.log_share)) }
            IconButton(onClick = { saveLauncher.launch("coloros-icons-patch-logs.zip") }) {
                Icon(
                    Icons.Filled.Save,
                    stringResource(R.string.log_save)
                )
            }
            IconButton(onClick = { showMore = true }) {
                Icon(
                    Icons.Filled.MoreVert,
                    stringResource(R.string.log_more)
                )
            }
            DropdownMenu(expanded = showFilter, onDismissRequest = { showFilter = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.log_runtime)) },
                    onClick = { category = LogCategory.RUNTIME; showFilter = false })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.log_lsposed)) },
                    onClick = { category = LogCategory.LSPOSED; showFilter = false })
                LogLevel.entries.forEach { level ->
                    DropdownMenuItem(
                        text = { Text("${if (level in selectedLevels) "✓ " else ""}${level.name}") },
                        onClick = {
                            selectedLevels =
                                if (level in selectedLevels) selectedLevels - level else selectedLevels + level
                        })
                }
            }
            DropdownMenu(expanded = showMore, onDismissRequest = { showMore = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.log_scroll_top)) },
                    leadingIcon = { Icon(Icons.Filled.VerticalAlignTop, null) },
                    onClick = {
                        showMore = false; if (entries.isNotEmpty()) scope.launch {
                        listState.animateScrollToItem(
                            0
                        )
                    }
                    })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.log_scroll_bottom)) },
                    leadingIcon = { Icon(Icons.Filled.VerticalAlignBottom, null) },
                    onClick = {
                        showMore = false; if (entries.isNotEmpty()) scope.launch {
                        listState.animateScrollToItem(
                            entries.lastIndex
                        )
                    }
                    })
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.log_clear)) },
                    leadingIcon = { Icon(Icons.Filled.DeleteSweep, null) },
                    onClick = { LogStore.clear(context, category); showMore = false; reload() })
            }
        }
    ) { modifier ->
        Column(
            modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.log_search)) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (category == LogCategory.RUNTIME) stringResource(
                        R.string.log_runtime
                    ) else stringResource(R.string.log_lsposed),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(entries) { entry ->
                    Text(
                        "${entry.timestamp}  ${entry.level.name}  ${entry.message}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
