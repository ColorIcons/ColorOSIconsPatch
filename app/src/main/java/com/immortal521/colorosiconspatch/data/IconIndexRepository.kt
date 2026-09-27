package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEFAULT_INDEX_URL = GITHUB_INDEX_URL
private const val INDEX_CACHE = "icon-index.json"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000

private const val PERSISTENT_ICONS = "/data/adb/colorosiconspatch/uxicons"

data class IconFile(
    val name: String,
    val path: String,
    val sha256: String,
    val size: Long
)

data class IconPackage(
    val packageName: String,
    val files: List<IconFile>
)

fun loadCachedIconPackage(context: Context, packageName: String): IconPackage? {
    val cache = File(context.filesDir, INDEX_CACHE)
    if (!cache.isFile) return null
    return runCatching {
        parseIndex(cache.readText(), DOWNLOAD_VARIANTS.toSet()).second[packageName]
    }.getOrNull()
}

fun loadCachedIconBitmap(context: Context, packageName: String, fileName: String): ImageBitmap? {
    val target = "$PERSISTENT_ICONS/$packageName/$fileName"
    val cached = File.createTempFile("icon-preview-", ".png", context.cacheDir)
    return try {
        val result = com.topjohnwu.superuser.Shell.cmd(
            "cp ${shellQuote(target)} ${shellQuote(cached.absolutePath)}"
        ).exec()
        if (!result.isSuccess) return null
        BitmapFactory.decodeFile(cached.absolutePath)?.asImageBitmap()
    } finally {
        cached.delete()
    }
}

data class IconIndexLoadResult(
    val packages: Map<String, IconPackage>,
    val requiredFiles: List<IconFile> = emptyList(),
    val error: String? = null
) {
    val adaptedPackages: Set<String>
        get() = packages.keys
}

fun loadIconIndex(
    context: Context,
    indexUrl: String = DEFAULT_INDEX_URL,
    enabledVariants: Set<String> = DOWNLOAD_VARIANTS.toSet()
): IconIndexLoadResult {
    val cache = File(context.filesDir, INDEX_CACHE)
    return try {
        val json = downloadIndex(indexUrl)
        cache.writeText(json)
        parseIndex(json, enabledVariants).let { (requiredFiles, packages) ->
            IconIndexLoadResult(
                packages = packages,
                requiredFiles = requiredFiles
            )
        }
    } catch (_: Exception) {
        if (!cache.isFile) {
            IconIndexLoadResult(emptyMap(), error = context.getString(com.immortal521.colorosiconspatch.R.string.icon_index_read_failed))
        } else {
            try {
                parseIndex(cache.readText(), enabledVariants).let { (requiredFiles, packages) ->
                    IconIndexLoadResult(
                        packages = packages,
                        requiredFiles = requiredFiles,
                        error = context.getString(com.immortal521.colorosiconspatch.R.string.icon_index_cache_fallback)
                    )
                }
            } catch (_: Exception) {
                IconIndexLoadResult(emptyMap(), error = context.getString(com.immortal521.colorosiconspatch.R.string.icon_index_read_failed))
            }
        }
    }
}

private fun downloadIndex(indexUrl: String): String {
    val connection = URL(indexUrl).openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.connect()
        check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun parseIndex(
    json: String,
    enabledVariants: Set<String>
): Pair<List<IconFile>, Map<String, IconPackage>> {
    val root = JSONObject(json)
    val requiredFiles = buildList {
        addFiles(root.optJSONArray("required_files"))
    }
    val packages = root.getJSONObject("packages")
    val packageMap = buildMap {
        val packageKeys = packages.keys()
        while (packageKeys.hasNext()) {
            val packageName = packageKeys.next()
            val packageJson = packages.getJSONObject(packageName)
            val files = buildList {
                addFiles(packageJson.optJSONArray("required"))
                val variants = packageJson.optJSONObject("variants") ?: return@buildList
                val variantKeys = variants.keys()
                while (variantKeys.hasNext()) {
                    val variant = variantKeys.next()
                    if (variant in enabledVariants) {
                        addFiles(variants.optJSONArray(variant))
                    }
                }
            }
            if (files.isNotEmpty()) put(packageName, IconPackage(packageName, files.distinctBy { it.name }))
        }
    }
    return requiredFiles to packageMap
}

private fun MutableList<IconFile>.addFiles(array: org.json.JSONArray?) {
    if (array == null) return
    for (index in 0 until array.length()) {
        val file = array.getJSONObject(index)
        add(
            IconFile(
                name = file.getString("file"),
                path = file.getString("path"),
                sha256 = file.getString("sha256"),
                size = file.optLong("size", 0L)
            )
        )
    }
}

data class IconSyncPlan(
    val updates: List<IconFileUpdate>,
    val staleFiles: List<String>
) {
    val totalChanges: Int get() = updates.size + staleFiles.size
    val totalBytes: Long get() = updates.sumOf { it.file.size }
    val affectedApps: Int
        get() = (updates.map { it.packageName } + staleFiles.map {
            it.substringAfter(PERSISTENT_ICONS).trimStart('/').substringBefore('/')
        })
            .filter(String::isNotBlank)
            .distinct()
            .size
}

data class IconFileUpdate(
    val packageName: String,
    val file: IconFile,
    val target: String,
    val size: Long
)

data class IconSyncProgress(
    val completedApps: Int,
    val totalApps: Int,
    val currentApp: String? = null,
    val currentFile: String? = null
)

data class IconSyncResult(val downloaded: Int, val removed: Int)

suspend fun buildIconSyncPlan(
    installedPackages: Set<String>,
    index: IconIndexLoadResult
): IconSyncPlan {
    val keep = mutableSetOf<String>()
    val checksums = readLocalChecksums()
    val updates = buildList {
        for (requiredFile in index.requiredFiles) {
            val target = "$PERSISTENT_ICONS/${requiredFile.name}"
            keep += target
            if (checksums[target] != requiredFile.sha256) {
                add(IconFileUpdate(REQUIRED_PACKAGE, requiredFile, target, requiredFile.size))
            }
        }
        for (packageName in installedPackages.intersect(index.packages.keys)) {
            for (iconFile in index.packages.getValue(packageName).files) {
                val target = "$PERSISTENT_ICONS/$packageName/${iconFile.name}"
                keep += target
                if (checksums[target] != iconFile.sha256) {
                    add(IconFileUpdate(packageName, iconFile, target, iconFile.size))
                }
            }
        }
    }
    return IconSyncPlan(updates, (checksums.keys - keep).toList())
}

suspend fun syncIconResources(
    context: Context,
    plan: IconSyncPlan,
    onProgress: (IconSyncProgress) -> Unit
): IconSyncResult = coroutineScope {
    val updatesByApp = plan.updates.groupBy { it.packageName }
    val staleByApp = plan.staleFiles.groupBy {
        it.substringAfter(PERSISTENT_ICONS).trimStart('/').substringBefore('/')
    }
    val apps = (updatesByApp.keys + staleByApp.keys).filter(String::isNotBlank).distinct()
    val totalApps = apps.size
    val completedMutex = Mutex()
    var completedApps = 0
    val downloadDispatcher = Dispatchers.IO.limitedParallelism(
        loadAppSettings(context).concurrency
    )

    val results = apps.map { packageName ->
        async(downloadDispatcher) {
            val updates = updatesByApp[packageName].orEmpty()
            val staleFiles = staleByApp[packageName].orEmpty()
            onProgress(IconSyncProgress(completedApps, totalApps, packageName))
            var downloadedForApp = 0
            var removedForApp = 0

            for (update in updates) {
                val temporary = File.createTempFile("icon-", ".png", context.cacheDir)
                try {
                    onProgress(IconSyncProgress(completedApps, totalApps, packageName, update.file.name))
                    downloadFile(
                        update.file.path,
                        temporary,
                        loadAppSettings(context).indexUrl.removeSuffix("/index.json")
                    )
                    check(sha256(temporary) == update.file.sha256) {
                        context.getString(com.immortal521.colorosiconspatch.R.string.icon_checksum_failed, update.file.path)
                    }
                    installRootFile(context, temporary, update.target)
                    downloadedForApp++
                } finally {
                    temporary.delete()
                }
            }
            for (staleFile in staleFiles) {
                check(runRoot("rm -f ${shellQuote(staleFile)}").first == 0) {
                    context.getString(com.immortal521.colorosiconspatch.R.string.delete_old_icons_failed)
                }
                removedForApp++
            }
            val completed = completedMutex.withLock { ++completedApps }
            onProgress(IconSyncProgress(completed, totalApps, packageName))
            IconSyncResult(downloadedForApp, removedForApp)
        }
    }.awaitAll()
    IconSyncResult(results.sumOf { it.downloaded }, results.sumOf { it.removed })
}

private const val REQUIRED_PACKAGE = "__required__"

private fun readLocalChecksums(): Map<String, String> {
    val result = runRoot(
        "find ${shellQuote(PERSISTENT_ICONS)} -type f -exec sha256sum {} +"
    )
    if (result.first != 0) return emptyMap()
    return result.second.lineSequence().mapNotNull { line ->
        val separator = line.indexOf("  ")
        if (separator <= 0) return@mapNotNull null
        val checksum = line.substring(0, separator).trim()
        val path = line.substring(separator + 2).trim()
        if (checksum.length == 64 && path.isNotEmpty()) path to checksum else null
    }.toMap()
}

private fun installRootFile(context: Context, source: File, target: String) {
    val command = "mkdir -p ${shellQuote(target.substringBeforeLast('/'))} && " +
        "cp ${shellQuote(source.absolutePath)} ${shellQuote(target)} && " +
        "chmod 0644 ${shellQuote(target)}"
    check(runRoot(command).first == 0) { context.getString(com.immortal521.colorosiconspatch.R.string.write_icons_failed) }
}

private fun removeStaleRootFiles(keep: Set<String>, installedPackages: Set<String>) {
    if (installedPackages.isEmpty()) return
    val packagePaths = installedPackages.joinToString(" ") {
        shellQuote("$PERSISTENT_ICONS/$it")
    }
    val staleCondition = keep.joinToString(" && ") {
        "[ \"\$file\" != ${shellQuote(it)} ]"
    }.ifBlank { "true" }
    val command = "find $packagePaths -type f -name '*.png' -print0 | while IFS= read -r -d '' file; do " +
        "if $staleCondition; then rm -f \"\$file\"; fi; done"
    runRoot(command)
}

private fun downloadFile(path: String, output: File, baseUrl: String) {
    val connection = URL("$baseUrl/$path").openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        connection.inputStream.use { input -> output.outputStream().use { input.copyTo(it) } }
    } finally {
        connection.disconnect()
    }
}

private fun sha256(file: File): String {
    val digest = java.security.MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

private fun runRoot(command: String): Pair<Int, String> {
    return try {
        val result = com.topjohnwu.superuser.Shell.cmd(command).exec()
        result.code to (result.out + result.err).joinToString("\n")
    } catch (error: Exception) {
        1 to (error.message ?: "")
    }
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
