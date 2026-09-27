package com.immortal521.colorosiconspatch.data

import android.content.Context
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
        val directory = File(context.filesDir, LOG_DIRECTORY).apply { mkdirs() }
        val zipFile = File(context.cacheDir, "coloros-icons-patch-logs.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
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
