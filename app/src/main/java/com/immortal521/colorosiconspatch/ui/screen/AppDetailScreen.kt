package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.data.AppSettingsState
import com.immortal521.colorosiconspatch.data.IconFile
import com.immortal521.colorosiconspatch.data.IconPackage
import com.immortal521.colorosiconspatch.data.loadCachedIconBitmap
import com.immortal521.colorosiconspatch.data.loadCachedIconPackage
import com.immortal521.colorosiconspatch.ui.component.material.SegmentedColumn
import com.immortal521.colorosiconspatch.ui.component.material.LocalListItemShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val ICON_ROOT = "/data/adb/colorosiconspatch/uxicons"

@Composable
fun AppDetailScreen(
    packageName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val settings by AppSettingsState.settings.collectAsState()
    var appPackage by remember(packageName) { mutableStateOf<IconPackage?>(null) }
    var previews by remember(packageName) { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    val appLabel = remember(packageName) {
        runCatching { context.packageManager.getApplicationInfo(packageName, 0).loadLabel(context.packageManager).toString() }
            .getOrDefault(packageName)
    }
    val appSize = remember(packageName) {
        runCatching { File(context.packageManager.getApplicationInfo(packageName, 0).sourceDir).length() }
            .getOrDefault(0L)
    }

    LaunchedEffect(packageName) {
        withContext(Dispatchers.IO) {
            val loaded = loadCachedIconPackage(context, packageName)
            appPackage = loaded
            previews = loaded?.files?.associateNotNull { file ->
                loadCachedIconBitmap(context, packageName, file.name)?.let { file.name to it }
            }.orEmpty()
        }
    }

    MainScreenScaffold(
        title = appLabel,
        modifier = modifier,
        contentPadding = contentPadding,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        }
    ) { contentModifier ->
        Column(
            modifier = contentModifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SegmentedColumn(content = listOf({
                SegmentedListItem(
                    shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
                    colors = detailItemColors(),
                    content = { Text("应用包名") },
                    supportingContent = { Text(packageName) },
                    verticalAlignment = Alignment.CenterVertically
                )
            }, {
                SegmentedListItem(
                    shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(1, 2),
                    colors = detailItemColors(),
                    content = { Text("应用大小") },
                    supportingContent = { Text(formatBytes(appSize)) },
                    verticalAlignment = Alignment.CenterVertically
                )
            }))

            val groups = appPackage?.files.orEmpty().groupBy { categoryOf(it.name) }
            listOf("light", "dark", "monet", "mat", "other").forEach { category ->
                if (category != "other" && category !in settings.variants) return@forEach
                val entries = iconEntries(category, groups[category].orEmpty(), previews)
                if (entries.isNotEmpty()) {
                    Text(
                        categoryTitle(category),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                    )
                    if (category == "other") {
                        SegmentedColumn(
                            content = entries.map { entry ->
                                { MiscIconItem(entry) }
                            }
                        )
                    } else {
                        SegmentedColumn(
                            content = listOf {
                                IconGrid(category, entries)
                            }
                        )
                    }
                }
            }
            if (appPackage == null) {
                Text("暂无已下载的图标资源", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class IconEntry(
    val sizeName: String,
    val foreground: ImageBitmap? = null,
    val background: ImageBitmap? = null,
    val fileName: String
)

private fun iconEntries(category: String, files: List<IconFile>, previews: Map<String, ImageBitmap>): List<IconEntry> {
    val grouped = files.groupBy { iconSize(it.name) }
    return grouped.keys.sortedWith(compareBy { sizeOrder(it) }).mapNotNull { size ->
        val selected = grouped.getValue(size)
        val backgroundFile = selected.firstOrNull { it.name.startsWith("recbg") }
        val foregroundFile = selected.firstOrNull {
            when (category) {
                "light" -> it.name.startsWith("recfg")
                "dark" -> it.name.startsWith("rec_night")
                "monet" -> it.name.startsWith("monochrome")
                "mat" -> it.name.startsWith("mat")
                else -> false
            }
        }
        val direct = foregroundFile ?: if (category == "light") null else selected.firstOrNull()
        val foreground = direct?.let { previews[it.name] }
        val background = backgroundFile?.let { previews[it.name] }
        if (foreground == null && background == null) return@mapNotNull null
        IconEntry(
            sizeName = size,
            foreground = foreground,
            background = background,
            fileName = direct?.name ?: backgroundFile?.name ?: selected.first().name
        )
    }
}

@Composable
private fun IconGrid(category: String, entries: List<IconEntry>) {
    val bySize = entries.associateBy { it.sizeName }
    val fallback = bySize["1x1"]
    val unit = 50.dp
    SegmentedListItem(
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        colors = detailItemColors(),
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconSlot(category, bySize["2x2"] ?: fallback, "2x2", 2, 2, unit)
                    IconSlot(category, bySize["1x2"] ?: fallback, "1x2", 1, 2, unit)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconSlot(category, bySize["2x1"] ?: fallback, "2x1", 2, 1, unit)
                    IconSlot(category, fallback, "1x1", 1, 1, unit)
                }
            }
        },
        verticalAlignment = Alignment.CenterVertically
    )
}

@Composable
private fun MiscIconItem(entry: IconEntry) {
    SegmentedListItem(
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        colors = detailItemColors(),
        content = { Text(entry.fileName) },
        trailingContent = {
            IconSlot("other", entry, entry.sizeName, 1, 1, 50.dp)
        },
        verticalAlignment = Alignment.CenterVertically
    )
}

@Composable
private fun IconSlot(
    category: String,
    entry: IconEntry?,
    targetSize: String,
    columns: Int,
    rows: Int,
    unit: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .width(unit * columns)
            .height(unit * rows)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (entry == null) {
            Icon(
                Icons.Outlined.Image,
                contentDescription = "未适配",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        } else {
            val contentScale = when {
                targetSize == "1x2" -> ContentScale.FillWidth
                targetSize == "2x1" -> ContentScale.FillHeight
                else -> ContentScale.Fit
            }
            entry.background?.let {
                Image(
                    it,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.FillBounds
                )
            }
            entry.foreground?.let {
                Image(
                    it,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale,
                    colorFilter = if (category == "monet") ColorFilter.tint(MaterialTheme.colorScheme.primary) else null
                )
            }
        }
    }
}

@Composable
private fun detailItemColors() = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceBright,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

private fun categoryOf(name: String): String = when {
    name.startsWith("recbg") || name.startsWith("recfg") -> "light"
    name.startsWith("rec_night") -> "dark"
    name.startsWith("monochrome") -> "monet"
    name.startsWith("mat") -> "mat"
    else -> "other"
}

private fun iconSize(name: String): String = Regex("_(1x2|2x1|2x2)").find(name)?.groupValues?.get(1) ?: "1x1"

private fun sizeOrder(size: String): Int = listOf("1x1", "1x2", "2x1", "2x2").indexOf(size).let { if (it < 0) 99 else it }

private fun iconAspectRatio(size: String): Float = when (size) {
    "1x2" -> 240f / 820f
    "2x1" -> 820f / 240f
    else -> 1f
}

private fun categoryTitle(category: String): String = when (category) {
    "light" -> "浅色图标"
    "dark" -> "深色图标"
    "monet" -> "莫奈图标"
    "mat" -> "Material 图标"
    else -> "杂项图标"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(Locale.US, bytes / 1024f)
    else -> "%.1f MB".format(Locale.US, bytes / (1024f * 1024f))
}

private inline fun <T, R : Any> Iterable<T>.associateNotNull(transform: (T) -> Pair<String, R>?): Map<String, R> =
    buildMap { for (item in this@associateNotNull) transform(item)?.let { put(it.first, it.second) } }
