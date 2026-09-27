package com.immortal521.colorosiconspatch.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.rememberSliderState
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
import androidx.compose.ui.res.stringResource
import com.immortal521.colorosiconspatch.R
import kotlin.math.roundToInt
import com.immortal521.colorosiconspatch.data.AppSettingsState
import com.immortal521.colorosiconspatch.data.CHANNEL_CLOUDFLARE
import com.immortal521.colorosiconspatch.data.DOWNLOAD_VARIANTS
import com.immortal521.colorosiconspatch.data.ThemeMode
import com.immortal521.colorosiconspatch.data.setAppLanguage
import com.immortal521.colorosiconspatch.data.setPredictiveBackEnabled
import com.immortal521.colorosiconspatch.ui.component.material.ExpressiveToggleButton
import com.immortal521.colorosiconspatch.ui.component.material.LocalListItemShapes
import com.immortal521.colorosiconspatch.ui.component.material.SegmentedColumn
import com.immortal521.colorosiconspatch.ui.component.material.SegmentedDropdownItem
import com.immortal521.colorosiconspatch.ui.theme.effectiveFor
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
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
    val scope = rememberCoroutineScope()
    val languages = listOf(
        stringResource(R.string.system_default) to "",
        stringResource(R.string.simplified_chinese) to "zh-CN",
        stringResource(R.string.traditional_chinese) to "zh-TW",
        stringResource(R.string.english) to "en",
        stringResource(R.string.japanese) to "ja",
        stringResource(R.string.korean) to "ko",
        stringResource(R.string.spanish) to "es",
        stringResource(R.string.french) to "fr",
        stringResource(R.string.german) to "de",
        stringResource(R.string.russian) to "ru",
        stringResource(R.string.portuguese) to "pt"
    )

    SettingsPageScaffold(stringResource(R.string.settings), modifier, padding, null) {
        SegmentedColumn(content = listOf(
            {
                SwitchItem(Icons.Filled.SystemUpdate, stringResource(R.string.auto_check_updates), stringResource(R.string.auto_check_updates_summary), settings.autoCheckUpdates) {
                    AppSettingsState.update(context) { it.copy(autoCheckUpdates = !it.autoCheckUpdates) }
                }
            },
            {
                ActionItem(Icons.Filled.SystemUpdate, stringResource(R.string.check_updates), result ?: stringResource(R.string.check_updates_summary), onClick = {
                    checking = true
                    result = null
                    scope.launch {
                        result = withContext(Dispatchers.IO) { checkForUpdate(context) }
                        checking = false
                    }
                }) {
                    Button(
                        onClick = {
                            checking = true
                            result = null
                            scope.launch {
                                result = withContext(Dispatchers.IO) { checkForUpdate(context) }
                                checking = false
                            }
                        },
                        enabled = !checking
                    ) { Text(stringResource(if (checking) R.string.checking else R.string.check)) }
                }
            }
        ))
        if (checking) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))

        SegmentedColumn(content = listOf(
            { ArrowItem(Icons.Filled.Palette, stringResource(R.string.theme), stringResource(R.string.theme_summary)) { open(SettingsPage.THEME) } },
            {
                val languageIndex = languages.indexOfFirst { it.second == currentLanguageTag(context) }.coerceAtLeast(0)
                SegmentedDropdownItem(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.language),
                    summary = stringResource(R.string.language),
                    items = languages.map { it.first },
                    selectedIndex = languageIndex,
                    onItemSelected = { index -> setAppLanguage(context, languages[index].second) }
                )
            }
        ))

        SegmentedColumn(content = listOf(
            { ArrowItem(Icons.Filled.CloudDownload, stringResource(R.string.download_settings), stringResource(R.string.download_settings_summary)) { open(SettingsPage.DOWNLOAD) } }
        ))
    }
}

@Composable
private fun ThemeSettings(modifier: Modifier, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by AppSettingsState.settings.collectAsState()
    val isDark = when (settings.theme) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    SettingsPageScaffold(stringResource(R.string.theme), modifier, padding, onBack) {
        ThemePreview(
            modifier = Modifier.fillMaxWidth(),
            isDark = isDark,
            keyColor = settings.keyColor,
            paletteStyle = settings.paletteStyle,
            colorSpec = settings.colorSpec
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ColorSeedButton(0, settings.keyColor == 0, isDark, settings.paletteStyle, settings.colorSpec) { AppSettingsState.update(context) { it.copy(keyColor = 0) } } }
            items(listOf(0xFFF44336.toInt(), 0xFF2196F3.toInt(), 0xFF009688.toInt(), 0xFFFF9800.toInt(), 0xFF9C27B0.toInt())) { color ->
                ColorSeedButton(color, settings.keyColor == color, isDark, settings.paletteStyle, settings.colorSpec) { AppSettingsState.update(context) { it.copy(keyColor = color) } }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
            listOf(
                Triple(Icons.Filled.Brightness4, stringResource(R.string.theme_system), ThemeMode.SYSTEM),
                Triple(Icons.Filled.Brightness7, stringResource(R.string.theme_light), ThemeMode.LIGHT),
                Triple(Icons.Filled.Brightness3, stringResource(R.string.theme_dark), ThemeMode.DARK)
            ).forEachIndexed { index, (icon, label, mode) ->
                ExpressiveToggleButton(
                    checked = settings.theme == mode,
                    onCheckedChange = { checked ->
                        if (checked) AppSettingsState.update(context) { it.copy(theme = mode) }
                    },
                    modifier = Modifier.weight(1f),
                    shapes = when (index) {
                        0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        2 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                ) {
                    Icon(icon, contentDescription = label)
                }
            }
        }

        SegmentedColumn(
            modifier = Modifier.fillMaxWidth(),
            content = listOf(
                {
                    val styles = PaletteStyle.entries
                    SegmentedDropdownItem(
                        icon = Icons.Filled.Style,
                        title = stringResource(R.string.palette_style),
                        items = styles.map { it.name },
                        selectedIndex = styles.indexOf(settings.paletteStyle),
                        onItemSelected = { index ->
                            AppSettingsState.update(context) { it.copy(paletteStyle = styles[index]) }
                        }
                    )
                },
                {
                    val specs = ColorSpec.SpecVersion.entries
                    SegmentedDropdownItem(
                        icon = Icons.Filled.DesignServices,
                        title = stringResource(R.string.color_spec),
                        items = specs.map { it.name },
                        selectedIndex = specs.indexOf(settings.colorSpec).coerceAtLeast(0),
                        onItemSelected = { index ->
                            AppSettingsState.update(context) { it.copy(colorSpec = specs[index]) }
                        }
                    )
                }
            )
        )

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            SegmentedColumn(content = listOf({
                SwitchItem(
                    icon = Icons.Filled.MenuOpen,
                    title = stringResource(R.string.predictive_back),
                    summary = stringResource(R.string.predictive_back_summary),
                    checked = settings.predictiveBack
                ) {
                    val enabled = !settings.predictiveBack
                    AppSettingsState.update(context) { it.copy(predictiveBack = enabled) }
                    setPredictiveBackEnabled(context, enabled)
                    (context as? android.app.Activity)?.recreate()
                }
            }))
        }
    }
}

@Composable
private fun ColorSeedButton(
    color: Int,
    selected: Boolean,
    isDark: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val systemWallpaperScheme = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else MaterialTheme.colorScheme
    val scheme = rememberDynamicColorScheme(
        seedColor = if (color == 0) systemWallpaperScheme.primary else Color(color),
        isDark = isDark,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle)
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainer
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(48.dp)) {
                drawArc(scheme.primaryContainer, 180f, 180f, true)
                drawArc(scheme.tertiaryContainer, 0f, 180f, true)
            }
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).border(2.dp, scheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(24.dp).clip(CircleShape).background(scheme.primary)) {
                        Icon(Icons.Filled.Palette, null, tint = scheme.onPrimary, modifier = Modifier.align(Alignment.Center).size(16.dp))
                    }
                }
            }
            AnimatedVisibility(visible = !selected) {
                Box(Modifier.size(20.dp).background(scheme.primary, CircleShape))
            }
        }
    }
}

@Composable
private fun ThemePreview(
    modifier: Modifier = Modifier,
    isDark: Boolean,
    keyColor: Int,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion
) {
    val context = LocalContext.current
    val systemScheme = if (isDark) {
        androidx.compose.material3.dynamicDarkColorScheme(context)
    } else {
        androidx.compose.material3.dynamicLightColorScheme(context)
    }
    val scheme = rememberDynamicColorScheme(
        seedColor = if (keyColor == 0) systemScheme.primary else Color(keyColor),
        isDark = isDark,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle)
    )
    Surface(
        modifier = modifier,
        color = scheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .aspectRatio(0.52f)
                    .padding(vertical = 14.dp, horizontal = 7.dp)
                    .border(1.dp, scheme.outlineVariant, RoundedCornerShape(14.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.theme_preview_title),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(scheme.secondaryContainer, RoundedCornerShape(7.dp))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(scheme.surfaceBright, RoundedCornerShape(7.dp))
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(scheme.surfaceContainerHigh, RoundedCornerShape(7.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Brightness4, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun DownloadSettings(modifier: Modifier, padding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by AppSettingsState.settings.collectAsState()
    SettingsPageScaffold(stringResource(R.string.download_settings), modifier, padding, onBack) {
        Text(stringResource(R.string.download_channel_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        SegmentedColumn(content = listOf({
            SegmentedDropdownItem(
                title = stringResource(R.string.download_channel_title),
                summary = stringResource(R.string.download_channel_summary),
                items = listOf("GitHub", "Cloudflare"),
                selectedIndex = if (settings.channel == CHANNEL_CLOUDFLARE) 1 else 0,
                onItemSelected = { index ->
                    AppSettingsState.update(context) {
                        it.copy(channel = if (index == 1) CHANNEL_CLOUDFLARE else "github")
                    }
                }
            )
        }))
        Text(stringResource(R.string.download_performance_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        SegmentedColumn(content = listOf({
            val sliderState = rememberSliderState(
                value = settings.concurrency.toFloat(),
                steps = 21,
                trackRange = 2f..24f
            )
            SegmentedListItem(
                shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
                colors = settingsItemColors(),
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.concurrency_title))
                                Text(
                                    stringResource(R.string.concurrency_summary_short),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                sliderState.value.roundToInt().toString(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            state = sliderState,
                            onValueChangeFinished = {
                                AppSettingsState.update(context) {
                                    it.copy(concurrency = sliderState.value.roundToInt().coerceIn(2, 24))
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                verticalAlignment = Alignment.CenterVertically
            )
        }))
        Text(stringResource(R.string.icon_types_section), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, bottom = 6.dp))
        SegmentedColumn(
            content = DOWNLOAD_VARIANTS.map { variant ->
                {
                    SwitchItem(
                        title = variant.uppercase(),
                        summary = stringResource(R.string.download_variant_summary, variant.uppercase()),
                        checked = variant in settings.variants,
                        onClick = {
                            AppSettingsState.update(context) { current ->
                                val next = if (variant in current.variants) {
                                    current.variants - variant
                                } else {
                                    current.variants + variant
                                }
                                current.copy(variants = next.takeIf { it.isNotEmpty() } ?: current.variants)
                            }
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun SettingsPageScaffold(title: String, modifier: Modifier, padding: PaddingValues, onBack: (() -> Unit)?, content: @Composable () -> Unit) {
    MainScreenScaffold(
        modifier = modifier,
        title = title,
        contentPadding = padding,
        navigationIcon = onBack?.let { callback -> { IconButton(onClick = callback) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } } } ?: {}
    ) { contentModifier ->
        Column(
            contentModifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) { content() }
    }
}

@Composable
private fun settingsItemColors() = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceBright,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
    supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun ActionItem(
    icon: ImageVector,
    title: String,
    summary: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        colors = settingsItemColors(),
        content = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Box(contentAlignment = Alignment.Center) { Icon(icon, title) } },
        trailingContent = { Box(contentAlignment = Alignment.Center) { trailing() } },
        verticalAlignment = Alignment.CenterVertically
    )
}

@Composable
private fun ArrowItem(icon: ImageVector, title: String, summary: String, onClick: () -> Unit) {
    SegmentedListItem(
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        colors = settingsItemColors(),
        content = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(icon, title) },
        onClick = onClick,
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
        verticalAlignment = Alignment.CenterVertically
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
        SegmentedListItem(
            shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
            colors = settingsItemColors(),
            content = { Text(title) },
            supportingContent = { Text(summary) },
            leadingContent = { Icon(icon, title) },
            onClick = onExpand,
            trailingContent = { Text(value, color = MaterialTheme.colorScheme.primary) },
            verticalAlignment = Alignment.CenterVertically
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
    SegmentedListItem(
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        colors = settingsItemColors(),
        content = { Text(title) },
        onClick = onClick,
        leadingContent = { RadioButton(selected = selected, onClick = onClick) },
        verticalAlignment = Alignment.CenterVertically
    )
}

@Composable
private fun SwitchItem(icon: ImageVector? = null, title: String, summary: String, checked: Boolean, onClick: () -> Unit) {
    SegmentedListItem(
        shapes = LocalListItemShapes.current ?: ListItemDefaults.segmentedShapes(0, 1),
        colors = settingsItemColors(),
        content = { Text(title) },
        supportingContent = { Text(summary) },
        onClick = onClick,
        leadingContent = icon?.let { { Box(contentAlignment = Alignment.Center) { Icon(it, title) } } },
        trailingContent = { Box(contentAlignment = Alignment.Center) { Switch(checked = checked, onCheckedChange = { onClick() }) } },
        verticalAlignment = Alignment.CenterVertically
    )
}

private fun currentLanguageTag(context: android.content.Context): String =
    context.resources.configuration.locales[0]?.toLanguageTag().orEmpty()

private fun checkForUpdate(context: android.content.Context): String = runCatching {
    val connection = URL("https://api.github.com/repos/immortal521/ColorIconsPatch/releases/latest")
        .openConnection() as HttpURLConnection
    connection.connectTimeout = 8_000
    connection.readTimeout = 8_000
    try {
        if (connection.responseCode == 404) context.getString(R.string.no_release) else context.getString(R.string.release_available)
    } finally { connection.disconnect() }
}.getOrElse { context.getString(R.string.update_check_failed, it.message ?: context.getString(R.string.network_unavailable)) }
