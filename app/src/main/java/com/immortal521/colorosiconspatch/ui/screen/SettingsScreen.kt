package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.immortal521.colorosiconspatch.data.AppSettingsState
import com.immortal521.colorosiconspatch.data.CHANNEL_CLOUDFLARE
import com.immortal521.colorosiconspatch.data.DOWNLOAD_VARIANTS
import com.immortal521.colorosiconspatch.data.ThemeMode
import com.immortal521.colorosiconspatch.data.setAppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
enum class SettingsPage { ROOT, THEME, DOWNLOAD }

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    onOpen: (SettingsPage) -> Unit = {}
) {
    SettingsRoot(modifier, contentPadding, onOpen)
}

@Composable
fun ThemeSettingsScreen(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(), onBack: () -> Unit) {
    ThemeSettings(modifier, padding, onBack)
}

@Composable
fun DownloadSettingsScreen(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(), onBack: () -> Unit) {
    DownloadSettings(modifier, padding, onBack)
}

@Composable
private fun SettingsRoot(modifier: Modifier, padding: PaddingValues, open: (SettingsPage) -> Unit) {
    val context = LocalContext.current
    val settings by AppSettingsState.settings.collectAsState()
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var languageMenu by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val languages = listOf("系统默认" to "", "简体中文" to "zh-CN", "English" to "en")

    SettingsPageScaffold("设置", modifier, padding, null) {
        SettingsGroup {
            SwitchItem(
                icon = Icons.Filled.SystemUpdate,
                title = "启动时自动检查",
                summary = "应用启动时检查 GitHub Releases",
                checked = settings.autoCheckUpdates
            ) {
                AppSettingsState.update(context) { it.copy(autoCheckUpdates = !it.autoCheckUpdates) }
            }
            ActionItem(
                icon = Icons.Filled.SystemUpdate,
                title = "检查应用更新",
                summary = result ?: "手动检查 GitHub Releases 是否有新版本",
                trailing = {
                    Button(
                        onClick = {
                            checking = true
                            result = null
                            scope.launch {
                                result = withContext(Dispatchers.IO) { checkForUpdate() }
                                checking = false
                            }
                        },
                        enabled = !checking
                    ) { Text(if (checking) "检查中" else "检查") }
                }
            )
            if (checking) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
        }

        SettingsGroup {
            ArrowItem(Icons.Filled.Palette, "主题", "颜色、深色模式和动态取色") { open(SettingsPage.THEME) }
            DropdownItem(
                icon = Icons.Filled.Language,
                title = "语言",
                summary = "应用显示语言",
                value = languages.firstOrNull { it.second == currentLanguageTag(context) }?.first ?: "系统默认",
                expanded = languageMenu,
                onExpand = { languageMenu = true },
                onDismiss = { languageMenu = false },
                options = languages.map { it.first },
                onSelected = { index ->
                    setAppLanguage(context, languages[index].second)
                    languageMenu = false
                }
            )
        }

        SettingsGroup {
            ArrowItem(Icons.Filled.CloudDownload, "下载设置", "下载通道、并发数和图标类型") {
                open(SettingsPage.DOWNLOAD)
            }
        }
    }
}

@Composable
private fun ThemeSettings(modifier: Modifier, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by AppSettingsState.settings.collectAsState()
    SettingsPageScaffold("主题", modifier, padding, onBack) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ColorOS Icons", style = MaterialTheme.typography.headlineSmall)
                Text("主题预览", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary).forEach { color ->
                        Spacer(Modifier.size(48.dp).background(color, RoundedCornerShape(14.dp)))
                    }
                }
            }
        }
        SettingsGroup(title = "外观") {
            ThemeMode.values().forEach { mode ->
                RadioItem(
                    title = when (mode) { ThemeMode.SYSTEM -> "跟随系统"; ThemeMode.LIGHT -> "浅色"; ThemeMode.DARK -> "深色" },
                    selected = settings.theme == mode
                ) { AppSettingsState.update(context) { it.copy(theme = mode) } }
            }
            SwitchItem(
                title = "动态取色",
                summary = "使用系统壁纸生成 Material You 配色",
                checked = settings.dynamicColor
            ) { AppSettingsState.update(context) { it.copy(dynamicColor = !it.dynamicColor) } }
        }
    }
}

@Composable
private fun DownloadSettings(modifier: Modifier, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by AppSettingsState.settings.collectAsState()
    SettingsPageScaffold("下载设置", modifier, padding, onBack) {
        SettingsGroup(title = "下载通道") {
            RadioItem("GitHub", settings.channel != CHANNEL_CLOUDFLARE) {
                AppSettingsState.update(context) { it.copy(channel = "github") }
            }
            RadioItem("Cloudflare", settings.channel == CHANNEL_CLOUDFLARE) {
                AppSettingsState.update(context) { it.copy(channel = CHANNEL_CLOUDFLARE) }
            }
        }
        SettingsGroup(title = "下载性能") {
            ListItem(
                headlineContent = { Text("并发数") },
                supportingContent = { Text("同时下载的资源数量：${settings.concurrency}") },
                trailingContent = { Text(settings.concurrency.toString(), color = MaterialTheme.colorScheme.primary) }
            )
            Slider(
                value = settings.concurrency.toFloat(),
                onValueChange = { value -> AppSettingsState.update(context) { it.copy(concurrency = value.toInt().coerceIn(1, 16)) } },
                valueRange = 1f..16f,
                steps = 14,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun SettingsPageScaffold(title: String, modifier: Modifier, padding: PaddingValues, onBack: (() -> Unit)?, content: @Composable () -> Unit) {
    MainScreenScaffold(
        modifier = modifier,
        title = title,
        contentPadding = padding,
        navigationIcon = onBack?.let { callback -> { IconButton(onClick = callback) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } } } ?: {}
    ) { contentModifier ->
        Column(
            contentModifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) { content() }
    }
}

@Composable
private fun SettingsGroup(title: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        title?.let { Text(it, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceBright,
            shape = RoundedCornerShape(20.dp)
        ) { Column { content() } }
    }
}

@Composable
private fun ActionItem(icon: ImageVector, title: String, summary: String, trailing: @Composable () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, title) },
        trailingContent = trailing
    )
}

@Composable
private fun ArrowItem(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, title) },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun DropdownItem(
    icon: ImageVector,
    title: String,
    summary: String,
    value: String,
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    options: List<String>,
    onSelected: (Int) -> Unit
) {
    Column {
        ListItem(
            headlineContent = { Text(title) },
            supportingContent = { Text(summary) },
            leadingContent = { Icon(icon, title) },
            trailingContent = { Text(value, color = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable(onClick = onExpand)
        )
        if (expanded) {
            DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(index) })
                }
            }
        }
    }
}

@Composable
private fun RadioItem(title: String, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SwitchItem(icon: ImageVector? = null, title: String, summary: String, checked: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = icon?.let { { Icon(it, title) } },
        trailingContent = { Switch(checked = checked, onCheckedChange = { onClick() }) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

private fun currentLanguageTag(context: android.content.Context): String =
    context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()

private fun checkForUpdate(): String = runCatching {
    val connection = URL("https://api.github.com/repos/immortal521/ColorIconsPatch/releases/latest")
        .openConnection() as HttpURLConnection
    connection.connectTimeout = 8_000
    connection.readTimeout = 8_000
    try {
        if (connection.responseCode == 404) "暂无发布版本" else "发现可用发布版本，请前往项目页面查看"
    } finally { connection.disconnect() }
}.getOrElse { "检查失败：${it.message ?: "网络不可用"}" }
