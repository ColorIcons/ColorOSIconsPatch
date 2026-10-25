package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private const val DEFAULT_INDEX_URL = GITHUB_INDEX_URL
private const val INDEX_CACHE = "icon-index.json"
private const val CONNECT_TIMEOUT_MS = 10_000
private const val READ_TIMEOUT_MS = 15_000
internal const val MAX_INDEX_BYTES = 8 * 1024 * 1024
internal const val MAX_INDEX_ENTRIES = 10_000
internal const val MAX_ICON_FILE_BYTES = 8L * 1024 * 1024
internal const val MAX_TOTAL_DOWNLOAD_BYTES = 256L * 1024 * 1024

internal const val ICON_STORAGE_ROOT = "/data/adb/ColorOSIconsPatch/uxicons"

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
        parseIndex(cache.readText(), supportedDownloadVariants().toSet()).second[packageName]
    }.getOrNull()
}

private val iconPreviewCache = ConcurrentHashMap<String, ImageBitmap>()

fun loadCachedIconBitmap(context: Context, packageName: String, fileName: String): ImageBitmap? {
    val cacheKey = "$packageName/$fileName"
    iconPreviewCache[cacheKey]?.let { return it }
    val target = "$ICON_STORAGE_ROOT/$packageName/$fileName"
    val cached = File.createTempFile("icon-preview-", ".png", context.cacheDir)
    return try {
        val result = com.topjohnwu.superuser.Shell.cmd(
            "cp ${shellQuote(target)} ${shellQuote(cached.absolutePath)}"
        ).exec()
        if (!result.isSuccess) return null
        BitmapFactory.decodeFile(cached.absolutePath)?.asImageBitmap()?.also {
            iconPreviewCache[cacheKey] = it
        }
    } finally {
        cached.delete()
    }
}

fun clearCachedIconBitmaps(packageName: String? = null) {
    if (packageName == null) {
        iconPreviewCache.clear()
    } else {
        iconPreviewCache.keys.removeIf { it.startsWith("$packageName/") }
    }
}

data class IconIndexLoadResult(
    val packages: Map<String, IconPackage>,
    val requiredFiles: List<IconFile> = emptyList(),
    val error: String? = null,
    val syncAllowed: Boolean = true
) {
    val adaptedPackages: Set<String>
        get() = packages.keys
}

fun loadIconIndex(
    context: Context,
    indexUrl: String = DEFAULT_INDEX_URL,
    enabledVariants: Set<String> = supportedDownloadVariants().toSet()
): IconIndexLoadResult {
    val cache = File(context.filesDir, INDEX_CACHE)
    val supportedVariants = enabledVariants.intersect(supportedDownloadVariants().toSet())
    return try {
        val json = downloadIndex(indexUrl)
        parseIndex(json, supportedVariants).let { (requiredFiles, packages) ->
            cache.writeText(json)
            IconIndexLoadResult(
                packages = packages,
                requiredFiles = requiredFiles
            )
        }
    } catch (error: Exception) {
        LogStore.error(context, "Icon index download failed: ${error.javaClass.simpleName}: ${error.message.orEmpty()}")
        if (!cache.isFile) {
            IconIndexLoadResult(
                emptyMap(),
                error = context.getString(com.immortal521.colorosiconspatch.R.string.icon_index_read_failed),
                syncAllowed = false
            )
        } else {
            try {
                parseIndex(cache.readText(), supportedVariants).let { (requiredFiles, packages) ->
                    IconIndexLoadResult(
                        packages = packages,
                        requiredFiles = requiredFiles,
                        error = context.getString(com.immortal521.colorosiconspatch.R.string.icon_index_cache_fallback)
                    )
                }
            } catch (cacheError: Exception) {
                LogStore.error(context, "Cached icon index is invalid: ${cacheError.javaClass.simpleName}: ${cacheError.message.orEmpty()}")
                IconIndexLoadResult(
                    emptyMap(),
                    error = context.getString(com.immortal521.colorosiconspatch.R.string.icon_index_read_failed),
                    syncAllowed = false
                )
            }
        }
    }
}

private fun downloadIndex(indexUrl: String): String {
    val url = URL(indexUrl)
    check(url.protocol.equals("https", ignoreCase = true)) { "Icon index must use HTTPS" }
    val connection = url.openConnection() as HttpURLConnection
    return try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.connect()
        check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        check(connection.contentLengthLong in -1..MAX_INDEX_BYTES.toLong()) { "Icon index is too large" }
        connection.inputStream.use { it.readBounded(MAX_INDEX_BYTES).toString(Charsets.UTF_8) }
    } finally {
        connection.disconnect()
    }
}

internal fun validateIndexSize(json: String) {
    require(json.toByteArray(Charsets.UTF_8).size <= MAX_INDEX_BYTES) { "Icon index is too large" }
}

internal fun parseIndex(
    json: String,
    enabledVariants: Set<String>
): Pair<List<IconFile>, Map<String, IconPackage>> {
    validateIndexSize(json)
    val root = JSONObject(json)
    val requiredFiles = buildList {
        addFiles(root.optJSONArray("required_files"))
    }
    val packages = root.getJSONObject("packages")
    val packageMap = buildMap {
        val packageKeys = packages.keys()
        while (packageKeys.hasNext()) {
            val packageName = packageKeys.next()
            if (!isSafePackageName(packageName)) continue
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
            if (files.isNotEmpty()) put(
                packageName,
                IconPackage(packageName, files.distinctBy { it.name })
            )
        }
    }
    val entryCount = requiredFiles.size + packageMap.values.sumOf { it.files.size }
    check(entryCount <= MAX_INDEX_ENTRIES) { "Icon index has too many entries" }
    return requiredFiles to packageMap
}

private fun MutableList<IconFile>.addFiles(array: org.json.JSONArray?) {
    if (array == null) return
    for (index in 0 until array.length()) {
        val file = array.getJSONObject(index)
        val name = file.getString("file")
        val path = file.getString("path")
        val size = file.optLong("size", -1L)
        check(isSafeIconFileName(name)) { "Invalid icon file name" }
        check(isSafeIconDownloadPath(path)) { "Invalid icon download path" }
        check(file.getString("sha256").matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid icon checksum" }
        check(size in 0..MAX_ICON_FILE_BYTES) { "Invalid icon file size" }
        if (filterSupportedIconFileName(name)) {
            add(IconFile(name, path, file.getString("sha256").lowercase(), size))
        }
    }
}

data class IconSyncPlan(
    val updates: List<IconFileUpdate>,
    val staleFiles: List<String>,
    val moves: List<IconFileMove> = emptyList()
) {
    val totalChanges: Int get() = updates.size + staleFiles.size + moves.size
    val totalBytes: Long get() = updates.sumOf { it.file.size }
    val affectedApps: Int
        get() = (updates.map { it.packageName } + moves.map { it.packageName } + staleFiles.map {
            it.substringAfter(ICON_STORAGE_ROOT).trimStart('/').substringBefore('/')
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

data class IconFileMove(
    val packageName: String,
    val source: String,
    val target: String
)

data class IconSyncProgress(
    val completedApps: Int,
    val totalApps: Int,
    val currentApp: String? = null,
    val currentFile: String? = null
)

data class IconSyncResult(val downloaded: Int, val removed: Int)

fun buildIconSyncPlan(
    installedPackages: Set<String>,
    index: IconIndexLoadResult,
    monochromeSources: Set<String> = emptySet(),
    nativeMonochromePackages: Set<String> = emptySet(),
    checksums: Map<String, String> = readLocalChecksums()
): IconSyncPlan {
    require(index.syncAllowed) { "Icon index is invalid; refusing to plan file changes" }
    val keep = mutableSetOf<String>()
    val moves = mutableListOf<IconFileMove>()
    var totalBytes = 0L
    val updates = buildList {
        fun addPackageFile(packageName: String, file: IconFile, target: String) {
            check(isSafeIconFileName(file.name)) { "Invalid icon file name" }
            check(isSafeIconDownloadPath(file.path)) { "Invalid icon download path" }
            check(file.size in 0..MAX_ICON_FILE_BYTES) { "Invalid icon file size" }
            keep += target
            if (checksums[target] != file.sha256) {
                totalBytes += file.size
                check(totalBytes <= MAX_TOTAL_DOWNLOAD_BYTES) { "Icon download plan is too large" }
                add(IconFileUpdate(packageName, file, target, file.size))
            }
        }
        for (requiredFile in index.requiredFiles) {
            check(isSafeIconFileName(requiredFile.name)) { "Invalid required icon file name" }
            check(isSafeIconDownloadPath(requiredFile.path)) { "Invalid icon download path" }
            check(requiredFile.size in 0..MAX_ICON_FILE_BYTES) { "Invalid icon file size" }
            val target = "$ICON_STORAGE_ROOT/${requiredFile.name}"
            keep += target
            if (checksums[target] != requiredFile.sha256) {
                totalBytes += requiredFile.size
                check(totalBytes <= MAX_TOTAL_DOWNLOAD_BYTES) { "Icon download plan is too large" }
                add(IconFileUpdate(REQUIRED_PACKAGE, requiredFile, target, requiredFile.size))
            }
        }
        for (packageName in installedPackages.intersect(index.packages.keys)) {
            check(isSafePackageName(packageName)) { "Invalid package name" }
            val files = index.packages.getValue(packageName).files
            val downloaded = files.filter { it.name.startsWith("monochrome") }
            val nativeAvailable = packageName in nativeMonochromePackages
            val preferNative = "native:$packageName" in monochromeSources
            val useNative = nativeAvailable && (preferNative || downloaded.isEmpty())
            for (iconFile in files.filterNot { it.name.startsWith("monochrome") }) {
                addPackageFile(packageName, iconFile, "$ICON_STORAGE_ROOT/$packageName/${iconFile.name}")
            }
            for (iconFile in downloaded) {
                val normal = "$ICON_STORAGE_ROOT/$packageName/${iconFile.name}"
                val parked = "$ICON_STORAGE_ROOT/$packageName/${downloadedMonochromeFileName(iconFile.name)}"
                val target = if (useNative) parked else normal
                val alternate = if (useNative) normal else parked
                keep += target
                when {
                    checksums[target] == iconFile.sha256 -> Unit
                    checksums[alternate] == iconFile.sha256 -> moves += IconFileMove(packageName, alternate, target)
                    else -> {
                        totalBytes += iconFile.size
                        check(totalBytes <= MAX_TOTAL_DOWNLOAD_BYTES) { "Icon download plan is too large" }
                        add(IconFileUpdate(packageName, iconFile.copy(name = target.substringAfterLast('/')), target, iconFile.size))
                    }
                }
            }
            // The selected source is represented by the active filename; both names use the index hash.
        }
    }
    return IconSyncPlan(
        updates,
        (checksums.keys - keep - moves.mapTo(mutableSetOf()) { it.source }).toList(),
        moves
    )
}

suspend fun syncIconResources(
    context: Context,
    plan: IconSyncPlan,
    onProgress: (IconSyncProgress) -> Unit
): IconSyncResult = coroutineScope {
    validateSyncPlan(plan)
    val updatesByApp = plan.updates.groupBy { it.packageName }
    val movesByApp = plan.moves.groupBy { it.packageName }
    val staleByApp = plan.staleFiles.groupBy {
        it.substringAfter(ICON_STORAGE_ROOT).trimStart('/').substringBefore('/')
    }
    val apps = (updatesByApp.keys + movesByApp.keys + staleByApp.keys).filter(String::isNotBlank).distinct()
    val totalApps = apps.size
    val completedMutex = Mutex()
    var completedApps = 0
    val downloadDispatcher = Dispatchers.IO.limitedParallelism(
        loadAppSettings(context).concurrency
    )

    val results = apps.map { packageName ->
        async(downloadDispatcher) {
            val updates = updatesByApp[packageName].orEmpty()
            val moves = movesByApp[packageName].orEmpty()
            val staleFiles = staleByApp[packageName].orEmpty()
            if (moves.isNotEmpty() || updates.isNotEmpty() || staleFiles.isNotEmpty()) {
                clearCachedIconBitmaps(packageName)
            }
            onProgress(IconSyncProgress(completedApps, totalApps, packageName))
            var downloadedForApp = 0
            var removedForApp = 0

            for (move in moves) {
                moveRootFile(context, move.source, move.target)
            }
            for (update in updates) {
                check(isSafePersistentIconPath(update.target)) { "Invalid icon target" }
                check(isSafePackageName(update.packageName) || update.packageName == REQUIRED_PACKAGE) {
                    "Invalid icon package"
                }
                check(update.file.size in 0..MAX_ICON_FILE_BYTES) { "Invalid icon file size" }
                val temporary = File.createTempFile("icon-", ".png", context.cacheDir)
                try {
                    onProgress(
                        IconSyncProgress(
                            completedApps,
                            totalApps,
                            packageName,
                            update.file.name
                        )
                    )
                    downloadFile(
                        update.file.path,
                        temporary,
                        loadAppSettings(context).indexUrl.removeSuffix("/index.json")
                    )
                    check(sha256(temporary) == update.file.sha256) {
                        context.getString(
                            com.immortal521.colorosiconspatch.R.string.icon_checksum_failed,
                            update.file.path
                        )
                    }
                    installRootFile(context, temporary, update.target)
                    downloadedForApp++
                } finally {
                    temporary.delete()
                }
            }
            val completed = completedMutex.withLock { ++completedApps }
            onProgress(IconSyncProgress(completed, totalApps, packageName))
            IconSyncResult(downloadedForApp, removedForApp)
        }
    }.awaitAll()

    // A failed download must never be followed by deletion of existing icons.
    for (staleFile in plan.staleFiles) {
        check(isSafePersistentIconPath(staleFile)) { "Invalid stale icon path" }
        val result = runRoot(rootFileCommand("rm -f ${shellQuote(staleFile)}", staleFile))
        check(result.first == 0) {
            val output = result.second.trim()
            context.getString(com.immortal521.colorosiconspatch.R.string.delete_old_icons_failed) +
                    " (exit ${result.first})" +
                    output.takeIf { it.isNotEmpty() }?.let { ": $it" }.orEmpty()
        }
    }
    IconSyncResult(results.sumOf { it.downloaded }, plan.staleFiles.size)
}


private const val REQUIRED_PACKAGE = "__required__"

private fun readLocalChecksums(): Map<String, String> {
    val result = runRoot(
        "find ${shellQuote(ICON_STORAGE_ROOT)} -type f -exec sha256sum {} +"
    )
    if (result.first != 0) return emptyMap()
    return result.second.lineSequence().mapNotNull { line ->
        val match = Regex("^([0-9a-fA-F]{64})\\s+(.+)$").matchEntire(line.trim())
            ?: return@mapNotNull null
        match.groupValues[2].trim() to match.groupValues[1].lowercase()
    }.toMap()
}

internal fun moveRootFile(context: Context, source: String, target: String) {
    check(isSafePersistentIconPath(source) && isSafePersistentIconPath(target)) {
        "Invalid icon move"
    }
    val command = "if [ -f ${shellQuote(source)} ]; then " +
            "mv -f ${shellQuote(source)} ${shellQuote(target)}; " +
            "elif [ -f ${shellQuote(target)} ]; then true; " +
            "else echo 'Missing source: $source' >&2; false; fi"
    val result = runRoot(command)
    check(result.first == 0) {
        val output = result.second.trim()
        "${context.getString(com.immortal521.colorosiconspatch.R.string.write_icons_failed)} " +
                "(move $source -> $target, exit ${result.first})" +
                output.takeIf { it.isNotEmpty() }?.let { ": $it" }.orEmpty()
    }
}

suspend fun switchMonochromeFiles(
    context: Context,
    packageName: String,
    files: List<IconFile>,
    useNative: Boolean
) = withContext(Dispatchers.IO) {
    check(isSafePackageName(packageName)) { "Invalid icon package" }
    for (file in files.filter { it.name.startsWith("monochrome") }) {
        check(isSafeIconFileName(file.name)) { "Invalid monochrome file name" }
        val normal = "$ICON_STORAGE_ROOT/$packageName/${activeMonochromeFileName(file.name)}"
        val parked = "$ICON_STORAGE_ROOT/$packageName/${downloadedMonochromeFileName(file.name)}"
        val source = if (useNative) normal else parked
        val target = if (useNative) parked else normal
        moveRootFile(context, source, target)
    }
}

private fun installRootFile(context: Context, source: File, target: String) {
    check(isSafePersistentIconPath(target)) { "Invalid icon target" }
    val parent = target.substringBeforeLast('/')
    val staged = shellQuote("$target.tmp")
    val command = "[ -d ${shellQuote(ICON_STORAGE_ROOT)} ] && " +
            "[ ! -L /data ] && [ ! -L /data/adb ] && " +
            "[ ! -L /data/adb/ColorOSIconsPatch ] && " +
            "[ ! -L ${shellQuote(ICON_STORAGE_ROOT)} ] && " +
            "mkdir -p -- ${shellQuote(parent)} && " +
            "[ ! -L ${shellQuote(parent)} ] && [ ! -L ${shellQuote(target)} ] && " +
            "rm -f -- $staged && " +
            "cp -- ${shellQuote(source.absolutePath)} $staged && " +
            "chmod 0644 -- $staged && " +
            "mv -f -- $staged ${shellQuote(target)}"
    check(runRoot(command).first == 0) { context.getString(com.immortal521.colorosiconspatch.R.string.write_icons_failed) }
}

private fun downloadFile(path: String, output: File, baseUrl: String) {
    check(isSafeIconDownloadPath(path)) { "Invalid icon download path" }
    val base = URL(baseUrl)
    check(base.protocol.equals("https", ignoreCase = true)) { "Icon downloads must use HTTPS" }
    val url = URL(base, "${base.path.trimEnd('/')}/$path")
    check(url.host == base.host && url.port == base.port) { "Icon download escaped its host" }
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.connect()
        check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        check(connection.contentLengthLong in -1..MAX_ICON_FILE_BYTES) { "Icon file is too large" }
        connection.inputStream.use { input -> output.outputStream().use { input.copyBoundedTo(it, MAX_ICON_FILE_BYTES) } }
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

internal fun downloadedMonochromeFileName(name: String): String =
    name.removeSuffix(".png") + "_download.png"

internal fun activeMonochromeFileName(name: String): String =
    name.removeSuffix("_download.png").let { base ->
        if (base.endsWith(".png")) base else "$base.png"
    }

internal fun isSafeIconFileName(value: String): Boolean =
    value.isNotEmpty() && value.length <= 255 &&
            value != "." && value != ".." &&
            value.none { it == '/' || it == '\\' || it == '\u0000' || it == '\n' || it == '\r' || it == '?' || it == '#' }

internal fun isSafePackageName(value: String): Boolean =
    value.matches(Regex("[A-Za-z][A-Za-z0-9_]*(?:\\.[A-Za-z][A-Za-z0-9_]*)+"))

internal fun isSafeIconDownloadPath(value: String): Boolean =
    value.isNotEmpty() && !value.startsWith('/') && !value.startsWith('\\') &&
            !value.contains('%') &&
            !value.any { it == '\u0000' || it == '\n' || it == '\r' || it == '?' || it == '#' || it == '\\' } &&
            value.split('/').all { it.isNotEmpty() && it != "." && it != ".." && it.length <= 255 }

internal fun isSafePersistentIconPath(value: String): Boolean {
    if (!value.startsWith("$ICON_STORAGE_ROOT/")) return false
    val parts = value.removePrefix("$ICON_STORAGE_ROOT/").split('/')
    return parts.size in 1..2 && parts.all(::isSafeIconFileName) &&
            (parts.size == 1 || isSafePackageName(parts[0]))
}

internal fun validateSyncPlan(plan: IconSyncPlan) {
    require(plan.totalBytes <= MAX_TOTAL_DOWNLOAD_BYTES) { "Icon download plan is too large" }
    plan.updates.forEach { update ->
        require(isSafePersistentIconPath(update.target)) { "Invalid icon target" }
        require(isSafePackageName(update.packageName) || update.packageName == REQUIRED_PACKAGE) {
            "Invalid icon package"
        }
        require(isSafeIconFileName(update.file.name) && update.file.size in 0..MAX_ICON_FILE_BYTES) {
            "Invalid icon file metadata"
        }
    }
    plan.staleFiles.forEach { require(isSafePersistentIconPath(it)) { "Invalid stale icon path" } }
    plan.moves.forEach { move ->
        require(isSafePackageName(move.packageName)) { "Invalid icon package" }
        require(isSafePersistentIconPath(move.source) && isSafePersistentIconPath(move.target)) {
            "Invalid icon move"
        }
        require(move.source.substringBeforeLast('/') == move.target.substringBeforeLast('/')) {
            "Icon moves must stay in one package directory"
        }
    }
}

private fun rootFileCommand(operation: String, path: String): String {
    check(isSafePersistentIconPath(path)) { "Invalid persistent icon path" }
    val parent = path.substringBeforeLast('/')
    return "[ -d ${shellQuote(ICON_STORAGE_ROOT)} ] && " +
            "[ ! -L /data ] && [ ! -L /data/adb ] && " +
            "[ ! -L /data/adb/ColorOSIconsPatch ] && " +
            "[ ! -L ${shellQuote(ICON_STORAGE_ROOT)} ] && " +
            "[ ! -L ${shellQuote(parent)} ] && [ ! -L ${shellQuote(path)} ] && $operation"
}

private fun java.io.InputStream.readBounded(maxBytes: Int): ByteArray {
    val output = java.io.ByteArrayOutputStream()
    copyBoundedTo(output, maxBytes.toLong())
    return output.toByteArray()
}

private fun java.io.InputStream.copyBoundedTo(output: java.io.OutputStream, maxBytes: Long) {
    val buffer = ByteArray(8192)
    var total = 0L
    while (true) {
        val count = read(buffer)
        if (count < 0) return
        total += count
        check(total <= maxBytes) { "Downloaded file is too large" }
        output.write(buffer, 0, count)
    }
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
