package com.immortal521.colorosiconspatch.data

import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val MODULE_ID = "ColorOSIconsPatch"
private const val MODULE_VERSION = "0.4.0"
private const val MODULE_VERSION_CODE = "000040"
private const val MODULE_ASSET_ROOT = "module"
private const val PERSISTENT_ICONS = "/data/adb/ColorOSIconsPatch/uxicons"
private const val PUBLIC_ZIP = "/sdcard/Download/ColorOSIconsPatch.zip"

enum class ModuleOperationStatus {
    SUCCESS,
    MANUAL_INSTALL_REQUIRED,
    FAILED
}

data class ModuleOperationResult(
    val status: ModuleOperationStatus,
    val message: String,
    val zip: File? = null
)

/** Builds the fixed module template and installs it through the detected root manager. */
suspend fun prepareAndInstallModule(context: Context): ModuleOperationResult =
    withContext(Dispatchers.IO) {
        try {
            val zip = File(context.cacheDir, "$MODULE_ID.zip")
            createModuleZip(context, zip)
            when (val installer = detectInstaller()) {
                RootImplementation.APATCH -> ModuleOperationResult(
                    ModuleOperationStatus.MANUAL_INSTALL_REQUIRED,
                    context.getString(com.immortal521.colorosiconspatch.R.string.apatch_import_module, PUBLIC_ZIP),
                    copyZipToPublicStorage(zip)
                )
                RootImplementation.MAGISK,
                RootImplementation.KERNELSU -> {
                    val install = installModule(context, zip, installer)
                    if (install.first == 0 && moduleExists(MODULE_ID)) {
                        ensurePersistentIconDirectory(context)
                        zip.delete()
                        ModuleOperationResult(ModuleOperationStatus.SUCCESS, context.getString(com.immortal521.colorosiconspatch.R.string.module_installed), null)
                    } else {
                        ModuleOperationResult(
                            ModuleOperationStatus.FAILED,
                            install.second.ifBlank { context.getString(com.immortal521.colorosiconspatch.R.string.module_install_failed) },
                            zip
                        )
                    }
                }
                RootImplementation.UNKNOWN -> ModuleOperationResult(
                    ModuleOperationStatus.FAILED,
                    context.getString(com.immortal521.colorosiconspatch.R.string.root_manager_missing)
                )
            }
        } catch (error: Exception) {
            ModuleOperationResult(ModuleOperationStatus.FAILED, error.message ?: context.getString(com.immortal521.colorosiconspatch.R.string.module_prepare_failed))
        }
    }

private fun createModuleZip(context: Context, output: File) {
    val temporary = File(output.parentFile, "${output.name}.tmp")
    if (temporary.exists()) temporary.delete()
    try {
        ZipOutputStream(temporary.outputStream().buffered()).use { zip ->
            addText(zip, "module.prop", moduleProp())
            addAsset(zip, context, "customize.sh")
            addAsset(zip, context, "action.sh")
            addAsset(zip, context, "post-fs-data.sh")
            addAsset(zip, context, "service.sh")
            addAsset(zip, context, "uninstall.sh")
        }
        if (!temporary.renameTo(output)) throw IOException(context.getString(com.immortal521.colorosiconspatch.R.string.save_module_failed))
    } finally {
        if (temporary.exists()) temporary.delete()
    }
}

private fun moduleProp(): String = """
    id=$MODULE_ID
    name=ColorOS Icons Patch
    version=$MODULE_VERSION
    versionCode=$MODULE_VERSION_CODE
    author=immort521
    description=ColorOS Icons Patch
""".trimIndent() + "\n"

private fun addAsset(zip: ZipOutputStream, context: Context, name: String) {
    context.assets.open("$MODULE_ASSET_ROOT/$name").use { input ->
        zip.putNextEntry(ZipEntry(name))
        input.copyTo(zip)
        zip.closeEntry()
    }
}

private fun addText(zip: ZipOutputStream, name: String, content: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(content.toByteArray(Charsets.UTF_8))
    zip.closeEntry()
}

private fun detectInstaller(): RootImplementation = detectRootImplementation()

private fun installModule(context: Context, zip: File, implementation: RootImplementation): Pair<Int, String> {
    val command = when (implementation) {
        RootImplementation.MAGISK -> "magisk --install-module ${shellQuote(zip.absolutePath)}"
        RootImplementation.KERNELSU -> "ksud module install ${shellQuote(zip.absolutePath)}"
        else -> error(context.getString(com.immortal521.colorosiconspatch.R.string.unsupported_root_install))
    }
    val result = Shell.cmd(command).exec()
    return result.code to (result.out + result.err).joinToString("\n")
}

private fun copyZipToPublicStorage(zip: File): File? {
    val result = Shell.cmd(
        "mkdir -p /sdcard/Download && cp ${shellQuote(zip.absolutePath)} ${shellQuote(PUBLIC_ZIP)}"
    ).exec()
    return if (result.isSuccess) File(PUBLIC_ZIP) else zip
}

private fun ensurePersistentIconDirectory(context: Context) {
    val result = Shell.cmd(
        "mkdir -p ${shellQuote(PERSISTENT_ICONS)} && chmod 0755 ${shellQuote(PERSISTENT_ICONS)}"
    ).exec()
    check(result.isSuccess) { result.err.joinToString("\n").ifBlank { context.getString(com.immortal521.colorosiconspatch.R.string.persistent_icon_dir_failed) } }
}

private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"
