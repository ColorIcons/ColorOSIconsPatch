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

private const val PERSISTENT_ICONS = "/data/adb/colorosiconspatch/uxicons"

data class IconFile(
    val name: String,
    val path: String,
    val sha256: String
)

data class IconPackage(
    val packageName: String,
    val files: List<IconFile>
)

data class IconIndexLoadResult(
    val packages: Map<String, IconPackage>,
    val error: String? = null
) {
    val adaptedPackages: Set<String>
        get() = packages.keys
}

fun loadIconIndex(context: Context): IconIndexLoadResult {
    val cache = File(context.filesDir, INDEX_CACHE)
    return try {
        val json = downloadIndex()
        cache.writeText(json)
        IconIndexLoadResult(parsePackages(json))
    } catch (_: Exception) {
        if (!cache.isFile) {
            IconIndexLoadResult(emptyMap(), "图标索引读取失败")
        } else {
            try {
                IconIndexLoadResult(parsePackages(cache.readText()), "在线索引读取失败，当前使用缓存")
            } catch (_: Exception) {
                IconIndexLoadResult(emptyMap(), "图标索引读取失败")
            }
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
        check(connection.responseCode in 200..299) { "HTTP ${connection.responseCode}" }
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun parsePackages(json: String): Map<String, IconPackage> {
    val packages = JSONObject(json).getJSONObject("packages")
    return buildMap {
        val packageKeys = packages.keys()
        while (packageKeys.hasNext()) {
            val packageName = packageKeys.next()
            val packageJson = packages.getJSONObject(packageName)
            val files = buildList {
                addFiles(packageJson.optJSONArray("required"))
                val variants = packageJson.optJSONObject("variants") ?: return@buildList
                val variantKeys = variants.keys()
                while (variantKeys.hasNext()) {
                    addFiles(variants.optJSONArray(variantKeys.next()))
                }
            }
            if (files.isNotEmpty()) put(packageName, IconPackage(packageName, files.distinctBy { it.name }))
        }
    }
}

private fun MutableList<IconFile>.addFiles(array: org.json.JSONArray?) {
    if (array == null) return
    for (index in 0 until array.length()) {
        val file = array.getJSONObject(index)
        add(
            IconFile(
                name = file.getString("file"),
                path = file.getString("path"),
                sha256 = file.getString("sha256")
            )
        )
    }
}

suspend fun syncIconResources(
    context: Context,
    installedPackages: Set<String>,
    index: IconIndexLoadResult
): IconSyncResult {
    val filesToKeep = mutableSetOf<String>()
    var downloaded = 0
    var skipped = 0

    for (packageName in installedPackages.intersect(index.packages.keys)) {
        val iconPackage = index.packages.getValue(packageName)
        for (iconFile in iconPackage.files) {
            val target = "$PERSISTENT_ICONS/$packageName/${iconFile.name}"
            filesToKeep += target
            if (remoteFileMatches(target, iconFile.sha256)) {
                skipped++
                continue
            }
            val temporary = File(context.cacheDir, "icon-$packageName-${iconFile.name}")
            downloadFile(iconFile.path, temporary)
            check(sha256(temporary) == iconFile.sha256) { "图标校验失败：${iconFile.path}" }
            installRootFile(temporary, target)
            temporary.delete()
            downloaded++
        }
    }

    removeStaleRootFiles(filesToKeep, installedPackages)
    return IconSyncResult(downloaded, skipped)
}

data class IconSyncResult(val downloaded: Int, val skipped: Int)

private fun remoteFileMatches(path: String, sha256: String): Boolean {
    val result = runRoot("sha256sum ${shellQuote(path)}")
    return result.first == 0 && result.second.trim().startsWith(sha256)
}

private fun installRootFile(source: File, target: String) {
    val command = "mkdir -p ${shellQuote(target.substringBeforeLast('/'))} && " +
        "cp ${shellQuote(source.absolutePath)} ${shellQuote(target)} && " +
        "chmod 0644 ${shellQuote(target)}"
    check(runRoot(command).first == 0) { "无法写入图标资源" }
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

private fun downloadFile(path: String, output: File) {
    val connection = URL("https://immortal521.github.io/icons/$path").openConnection() as HttpURLConnection
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
        1 to (error.message ?: "无法执行 Root 命令")
    }
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
