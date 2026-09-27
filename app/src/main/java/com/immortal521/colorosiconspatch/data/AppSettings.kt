package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.os.LocaleList
import android.app.LocaleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val PREFS = "app_settings"
private const val KEY_CHANNEL = "download_channel"
private const val KEY_CONCURRENCY = "download_concurrency"
private const val KEY_VARIANTS = "download_variants"
private const val KEY_THEME = "theme_mode"
private const val KEY_DYNAMIC = "dynamic_color"
private const val KEY_AUTO_CHECK_UPDATE = "auto_check_update"

const val CHANNEL_GITHUB = "github"
const val CHANNEL_CLOUDFLARE = "cloudflare"
const val GITHUB_INDEX_URL = "https://coloricons.github.io/icons/index.json"
const val CLOUDFLARE_INDEX_URL = "https://icons.immort.top/index.json"

val DOWNLOAD_VARIANTS = listOf("monet", "light", "dark", "mat")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val channel: String = CHANNEL_GITHUB,
    val concurrency: Int = 8,
    val variants: Set<String> = DOWNLOAD_VARIANTS.toSet(),
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val autoCheckUpdates: Boolean = false
) {
    val indexUrl: String
        get() = if (channel == CHANNEL_CLOUDFLARE) CLOUDFLARE_INDEX_URL else GITHUB_INDEX_URL
}

private fun Context.settingsPrefs() = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

fun loadAppSettings(context: Context): AppSettings {
    val prefs = context.settingsPrefs()
    val variants = prefs.getStringSet(KEY_VARIANTS, DOWNLOAD_VARIANTS.toSet())
        ?.intersect(DOWNLOAD_VARIANTS.toSet())
        ?.ifEmpty { DOWNLOAD_VARIANTS.toSet() }
        ?: DOWNLOAD_VARIANTS.toSet()
    return AppSettings(
        channel = prefs.getString(KEY_CHANNEL, CHANNEL_GITHUB) ?: CHANNEL_GITHUB,
        concurrency = prefs.getInt(KEY_CONCURRENCY, 8).coerceIn(1, 16),
        variants = variants,
        theme = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }
            .getOrDefault(ThemeMode.SYSTEM),
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC, true),
        autoCheckUpdates = prefs.getBoolean(KEY_AUTO_CHECK_UPDATE, false)
    )
}

fun saveAppSettings(context: Context, settings: AppSettings) {
    context.settingsPrefs().edit()
        .putString(KEY_CHANNEL, settings.channel)
        .putInt(KEY_CONCURRENCY, settings.concurrency)
        .putStringSet(KEY_VARIANTS, settings.variants)
        .putString(KEY_THEME, settings.theme.name)
        .putBoolean(KEY_DYNAMIC, settings.dynamicColor)
        .putBoolean(KEY_AUTO_CHECK_UPDATE, settings.autoCheckUpdates)
        .apply()
}

object AppSettingsState {
    private val _settings = MutableStateFlow<AppSettings>(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun load(context: Context) { _settings.value = loadAppSettings(context) }
    fun update(context: Context, transform: (AppSettings) -> AppSettings) {
        val next = transform(_settings.value)
        saveAppSettings(context, next)
        _settings.value = next
    }
}

fun setAppLanguage(context: Context, languageTag: String) {
    val localeManager = context.getSystemService(LocaleManager::class.java)
    localeManager.applicationLocales = LocaleList.forLanguageTags(languageTag)
}
