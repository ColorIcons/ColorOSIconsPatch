package com.immortal521.colorosiconspatch.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

enum class CheckStatus {
    UNKNOWN,
    CHECKING,
    PASSED,
    FAILED
}

data class EnvironmentCheckResult(
    val root: CheckStatus = CheckStatus.UNKNOWN,
    val module: CheckStatus = CheckStatus.UNKNOWN
) {
    val ready: Boolean
        get() = root == CheckStatus.PASSED && module == CheckStatus.PASSED
}

private const val PREFERENCES = "environment_check"
private const val ROOT_STATUS = "root_status"
private const val MODULE_STATUS = "module_status"
private const val MODULE_ID = "ColorOSIconsPatch"

fun loadEnvironmentCheck(context: Context): EnvironmentCheckResult {
    val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    return EnvironmentCheckResult(
        root = readStatus(preferences.getString(ROOT_STATUS, null)),
        module = readStatus(preferences.getString(MODULE_STATUS, null))
    )
}

suspend fun checkRoot(context: Context): CheckStatus = withContext(Dispatchers.IO) {
    val status = if (hasRootAccess()) CheckStatus.PASSED else CheckStatus.FAILED
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putString(ROOT_STATUS, status.name)
        .apply()
    status
}

suspend fun checkModule(context: Context): CheckStatus = withContext(Dispatchers.IO) {
    val status = if (hasKernelSuModule()) CheckStatus.PASSED else CheckStatus.FAILED
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .edit()
        .putString(MODULE_STATUS, status.name)
        .apply()
    status
}

private fun hasRootAccess(): Boolean {
    val result = runSu("id") ?: return false
    return result.first == 0 && result.second.contains("uid=0")
}

private fun hasKernelSuModule(): Boolean {
    val moduleRoots = listOf(
        "/data/adb/modules/$MODULE_ID",
        "/data/adb/modules_update/$MODULE_ID",
        "/data/adb/modules/colorosiconspatch",
        "/data/adb/modules_update/colorosiconspatch"
    )
    if (moduleRoots.any { File(it, "module.prop").isFile }) return true

    val checks = moduleRoots.joinToString(" || ") { "test -f '$it/module.prop'" }
    return runSu(checks)?.first == 0
}

private fun runSu(command: String): Pair<Int, String>? {
    return try {
        val process = ProcessBuilder("su", "-c", command)
            .redirectErrorStream(true)
            .start()
        if (!process.waitFor(3, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }
        process.exitValue() to process.inputStream.bufferedReader().use { it.readText() }
    } catch (_: Exception) {
        null
    }
}

private fun readStatus(value: String?): CheckStatus = try {
    value?.let(CheckStatus::valueOf) ?: CheckStatus.UNKNOWN
} catch (_: IllegalArgumentException) {
    CheckStatus.UNKNOWN
}
