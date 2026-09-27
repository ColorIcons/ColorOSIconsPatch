package com.immortal521.colorosiconspatch.data

import android.annotation.SuppressLint
import android.content.Context
import java.io.File
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap


data class InstalledApp(
    val label: String,
    val packageName: String,
    val installTime: Long,
    val size: Long,
    val icon: androidx.compose.ui.graphics.ImageBitmap,
    val isAdapted: Boolean = false,
    val isSystem: Boolean = false,
    val userId: Int = 0
)

fun loadInstalledApps(
    context: Context,
    adaptedPackages: Set<String> = emptySet()
): List<InstalledApp> {
    val packageManager = context.packageManager
    val ownPackage = context.packageName

    return packageManager
        .getInstalledApplications(
            android.content.pm.PackageManager.ApplicationInfoFlags.of(0L)
        )
        .asSequence()
        .filter { it.packageName != ownPackage && it.enabled }
        .distinctBy { it.packageName }
        .map { applicationInfo ->
            InstalledApp(
                label = applicationInfo.loadLabel(packageManager).toString(),
                packageName = applicationInfo.packageName,
                installTime = packageManager.getPackageInfo(applicationInfo.packageName, 0).firstInstallTime,
                size = File(applicationInfo.sourceDir).length(),
                icon = applicationInfo.loadIcon(packageManager).toBitmap().asImageBitmap(),
                isAdapted = applicationInfo.packageName in adaptedPackages,
                isSystem = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 ||
                    applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                userId = applicationInfo.uid / 100000
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
}
