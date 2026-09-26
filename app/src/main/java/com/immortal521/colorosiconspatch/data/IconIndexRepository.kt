package com.immortal521.colorosiconspatch.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val INDEX_URL = "https://immortal521.github.io/icons/index.json"
private const val INDEX_CACHE = "icon-index.json"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000

data class IconIndexLoadResult(
    val adaptedPackages: Set<String>,
    val error: String? = null
)

fun loadIconIndex(context: Context): IconIndexLoadResult {
    val cache = File(context.filesDir, INDEX_CACHE)
    return try {
        val json = downloadIndex()
        cache.writeText(json)
        IconIndexLoadResult(parseAdaptedPackages(json))
    } catch (error: Exception) {
        if (cache.isFile) {
            try {
                IconIndexLoadResult(
                    parseAdaptedPackages(cache.readText()),
                    "在线索引读取失败，当前使用缓存"
                )
            } catch (cacheError: Exception) {
                IconIndexLoadResult(emptySet(), "图标索引读取失败")
            }
        } else {
            IconIndexLoadResult(emptySet(), "图标索引读取失败")
        }
    }
}

private fun downloadIndex(): String {
    val connection = URL(INDEX_URL).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.connect()
        if (connection.responseCode !in 200..299) {
            error("HTTP ${connection.responseCode}")
        }
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun parseAdaptedPackages(json: String): Set<String> {
    val packages = JSONObject(json).optJSONObject("packages") ?: return emptySet()
    return buildSet {
        val keys = packages.keys()
        while (keys.hasNext()) {
            val packageName = keys.next()
            val packageIndex = packages.optJSONObject(packageName) ?: continue
            val hasRequiredFiles = packageIndex.optJSONArray("required")?.length()?.let { it > 0 } == true
            val variants = packageIndex.optJSONObject("variants")
            val hasVariantFiles = variants?.keys()?.asSequence()?.any { variant ->
                variants.optJSONArray(variant)?.length()?.let { it > 0 } == true
            } == true
            if (hasRequiredFiles || hasVariantFiles) add(packageName)
        }
    }
}
