package com.immortal521.colorosiconspatch.data

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.content.edit

enum class CheckStatus {
    UNKNOWN,
    CHECKING,
    PASSED,
    FAILED
}

enum class RootImplementation(val displayName: String) {
    MAGISK("Magisk"),
    KERNELSU("KernelSU"),
    APATCH("APatch"),
    UNKNOWN("Root")
}

data class EnvironmentCheckResult(
    val root: CheckStatus = CheckStatus.UNKNOWN,
    val module: CheckStatus = CheckStatus.UNKNOWN,
    val implementation: RootImplementation = RootImplementation.UNKNOWN,
    val susfs: CheckStatus = CheckStatus.UNKNOWN,
    val xposed: CheckStatus = CheckStatus.UNKNOWN
) {
    val ready: Boolean
        get() = root == CheckStatus.PASSED &&
            module == CheckStatus.PASSED &&
            xposed == CheckStatus.PASSED
}

private const val PREFERENCES = "environment_check"
private const val ROOT_STATUS = "root_status"
private const val MODULE_STATUS = "module_status"
private const val ROOT_IMPLEMENTATION = "root_implementation"
private const val SUSFS_STATUS = "susfs_status"
private const val XPOSED_STATUS = "xposed_status"
private const val MODULE_ID = "ColorOSIconsPatch"

fun loadEnvironmentCheck(context: Context): EnvironmentCheckResult {
    val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    return EnvironmentCheckResult(
        root = readStatus(preferences.getString(ROOT_STATUS, null)),
        module = readStatus(preferences.getString(MODULE_STATUS, null)),
        implementation = readImplementation(preferences.getString(ROOT_IMPLEMENTATION, null)),
        susfs = readStatus(preferences.getString(SUSFS_STATUS, null)),
        xposed = readStatus(preferences.getString(XPOSED_STATUS, null))
    )
}

suspend fun checkRoot(context: Context): CheckStatus = withContext(Dispatchers.IO) {
    val shell = Shell.getShell()
    val implementation = detectRootImplementation()
    val status = if (shell.isRoot && implementation != RootImplementation.UNKNOWN) {
        CheckStatus.PASSED
    } else {
        CheckStatus.FAILED
    }
    val susfs = if (shell.isRoot && commandSucceeds("grep -q susfs /proc/filesystems || test -e /sys/fs/susfs")) {
        CheckStatus.PASSED
    } else {
        CheckStatus.FAILED
    }
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            putString(ROOT_STATUS, status.name)
                .putString(ROOT_IMPLEMENTATION, implementation.name)
                .putString(SUSFS_STATUS, susfs.name)
        }
    status
}

suspend fun checkModule(context: Context): CheckStatus = withContext(Dispatchers.IO) {
    val status = if (moduleExists(MODULE_ID)) CheckStatus.PASSED else CheckStatus.FAILED
    context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        .edit {
            putString(MODULE_STATUS, status.name)
        }
    status
}

fun detectRootImplementation(): RootImplementation {
    if (commandSucceeds("magisk -v || /data/adb/magisk/magisk -v")) return RootImplementation.MAGISK
    if (commandSucceeds("ksud -h || /data/adb/ksu/bin/ksud -h")) return RootImplementation.KERNELSU
    if (commandSucceeds("apd --help || /data/adb/ap/bin/apd --help || /data/adb/apd --help")) return RootImplementation.APATCH
    return RootImplementation.UNKNOWN
}

fun moduleExists(moduleId: String): Boolean {
    val moduleRoots = listOf(
        "/data/adb/modules/$moduleId",
        "/data/adb/modules_update/$moduleId",
        "/data/adb/modules/${moduleId.lowercase()}",
        "/data/adb/modules_update/${moduleId.lowercase()}"
    )
    if (moduleRoots.any { File(it, "module.prop").isFile }) return true
    return commandSucceeds(moduleRoots.joinToString(" || ") { "test -f '$it/module.prop'" })
}

fun commandSucceeds(command: String): Boolean = Shell.cmd(command).exec().isSuccess

private fun readStatus(value: String?): CheckStatus = try {
    value?.let(CheckStatus::valueOf) ?: CheckStatus.UNKNOWN
} catch (_: IllegalArgumentException) {
    CheckStatus.UNKNOWN
}

private fun readImplementation(value: String?): RootImplementation = try {
    value?.let(RootImplementation::valueOf) ?: RootImplementation.UNKNOWN
} catch (_: IllegalArgumentException) {
    RootImplementation.UNKNOWN
}
