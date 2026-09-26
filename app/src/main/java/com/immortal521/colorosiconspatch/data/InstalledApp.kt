package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap


data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: androidx.compose.ui.graphics.ImageBitmap,
    val isAdapted: Boolean = false
)

fun loadInstalledApps(
    context: Context,
    adaptedPackages: Set<String> = emptySet()
): List<InstalledApp> {
    val packageManager = context.packageManager
    val ownPackage = context.packageName

    return packageManager
        .getInstalledApplications(
            android.content.pm.PackageManager.ApplicationInfoFlags.of(
                android.content.pm.PackageManager.MATCH_ALL.toLong()
            )
        )
        .asSequence()
        .filter { it.packageName != ownPackage && it.enabled }
        .distinctBy { it.packageName }
        .map { applicationInfo ->
            InstalledApp(
                label = applicationInfo.loadLabel(packageManager).toString(),
                packageName = applicationInfo.packageName,
                icon = applicationInfo.loadIcon(packageManager).toBitmap().asImageBitmap(),
                isAdapted = applicationInfo.packageName in adaptedPackages
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
}
