package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.content.Intent
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap


data class InstalledApp(
    val label: String,
    val packageName: String,
    val icon: androidx.compose.ui.graphics.ImageBitmap,
    val isAdapted: Boolean = false
)

fun loadInstalledApps(context: Context): List<InstalledApp> {
    val packageManager = context.packageManager
    val ownPackage = context.packageName

    val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

    return packageManager
        .queryIntentActivities(launcherIntent, 0)
        .asSequence()
        .map { it.activityInfo.applicationInfo }
        .filter { it.packageName != ownPackage }
        .distinctBy { it.packageName }
        .map { applicationInfo ->
            InstalledApp(
                label = applicationInfo.loadLabel(packageManager).toString(),
                packageName = applicationInfo.packageName,
                icon = applicationInfo.loadIcon(packageManager).toBitmap().asImageBitmap()
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
}
