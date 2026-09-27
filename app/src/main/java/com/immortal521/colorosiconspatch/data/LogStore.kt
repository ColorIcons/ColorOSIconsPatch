package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val LOG_DIRECTORY = "logs"
private const val RUNTIME_FILE = "runtime.log"
private const val LSPOSED_FILE = "lsposed.log"
const val ACTION_LSPOSED_LOG = "com.immortal521.colorosiconspatch.action.LSPOSED_LOG"
const val EXTRA_LOG_LEVEL = "level"
const val EXTRA_LOG_MESSAGE = "message"

enum class LogCategory(val fileName: String) {
    RUNTIME(RUNTIME_FILE),
    LSPOSED(LSPOSED_FILE)
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val category: LogCategory,
    val message: String
) {
    fun encode(): String = listOf(timestamp, level.name, category.name, message).joinToString("\t")

    companion object {
        fun decode(line: String): LogEntry? {
            val parts = line.split('\t', limit = 4)
            if (parts.size != 4) return null
            return runCatching {
                LogEntry(
                    parts[0],
                    LogLevel.valueOf(parts[1]),
                    LogCategory.valueOf(parts[2]),
                    parts[3]
                )
            }.getOrNull()
        }
    }
}

object LogStore {
    fun debug(context: Context, message: String) =
        append(context, LogCategory.RUNTIME, LogLevel.DEBUG, message)

    fun info(context: Context, message: String) =
        append(context, LogCategory.RUNTIME, LogLevel.INFO, message)

    fun warn(context: Context, message: String) =
        append(context, LogCategory.RUNTIME, LogLevel.WARN, message)

    fun error(context: Context, message: String) =
        append(context, LogCategory.RUNTIME, LogLevel.ERROR, message)

    private val lock = Any()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun append(context: Context, category: LogCategory, level: LogLevel, message: String) {
        val entry = LogEntry(dateFormat.format(Date()), level, category, message.replace('\n', ' '))
        synchronized(lock) {
            logFile(context, category).apply {
                parentFile?.mkdirs()
                appendText(entry.encode() + "\n", StandardCharsets.UTF_8)
            }
        }
    }

    fun read(
        context: Context,
        category: LogCategory? = null,
        levels: Set<LogLevel> = LogLevel.entries.toSet(),
        query: String = ""
    ): List<LogEntry> = synchronized(lock) {
        LogCategory.entries.asSequence()
            .filter { category == null || it == category }
            .flatMap { readFile(logFile(context, it)).asSequence() }
            .filter {
                it.level in levels && (query.isBlank() || it.message.contains(
                    query,
                    true
                ) || it.timestamp.contains(query, true))
            }
            .toList()
    }

    fun clear(context: Context, category: LogCategory? = null) {
        synchronized(lock) {
            LogCategory.entries
                .filter { category == null || it == category }
                .forEach { logFile(context, it).delete() }
        }
    }

    fun zip(context: Context): File = synchronized(lock) {
        File(context.filesDir, LOG_DIRECTORY).mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val zipFile = File(context.cacheDir, "ColorOSIconsPatch-$timestamp.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zip.putNextEntry(ZipEntry("environment.txt"))
            zip.write(environment(context).toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
            LogCategory.entries.forEach { category ->
                val file = logFile(context, category)
                if (!file.exists()) return@forEach
                zip.putNextEntry(ZipEntry(category.fileName))
                FileInputStream(file).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        zipFile
    }

    fun exportFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "ColorOSIconsPatch-$timestamp.zip"
    }

    private fun environment(context: Context): String {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val check = loadEnvironmentCheck(context)
        return buildString {
            appendLine("app_name=ColorOS Icons Patch")
            appendLine("app_version=${packageInfo.versionName.orEmpty()}")
            appendLine("app_version_code=${packageInfo.longVersionCode}")
            appendLine("android_version=${Build.VERSION.RELEASE}")
            appendLine("android_sdk=${Build.VERSION.SDK_INT}")
            appendLine("device_manufacturer=${Build.MANUFACTURER}")
            appendLine("device_model=${Build.MODEL}")
            appendLine("rom_version=${Build.DISPLAY}")
            appendLine("root_status=${check.root}")
            appendLine("root_manager=${check.implementation.displayName}")
            appendLine("root_version=${check.rootVersion}")
            appendLine("module_status=${check.module}")
            appendLine("susfs_status=${check.susfs}")
            appendLine("lsposed_status=${check.xposed}")
            appendLine("lsposed_service_active=${XposedServiceState.active}")
        }
    }


    private fun logFile(context: Context, category: LogCategory) =
        File(File(context.filesDir, LOG_DIRECTORY), category.fileName)

    private fun readFile(file: File): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return BufferedReader(
            InputStreamReader(
                FileInputStream(file),
                StandardCharsets.UTF_8
            )
        ).useLines { lines ->
            lines.mapNotNull(LogEntry::decode).toList()
        }
    }
}
