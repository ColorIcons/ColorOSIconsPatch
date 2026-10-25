package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import org.xmlpull.v1.XmlPullParser


fun canReadInstalledApps(context: Context): Boolean =
    context.packageManager.getInstalledApplications(
        android.content.pm.PackageManager.ApplicationInfoFlags.of(0L)
    ).size > 1

data class InstalledApp(
    val label: String,
    val packageName: String,
    val installTime: Long,
    val size: Long,
    val icon: androidx.compose.ui.graphics.ImageBitmap,
    val isAdapted: Boolean = false,
    val hasNativeMonochrome: Boolean = false,
    val isSystem: Boolean = false,
    val userId: Int = 0
)

fun loadNativeMonochrome(context: Context, packageName: String): Drawable? = runCatching {
    val packageManager = context.packageManager
    val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    val resources = packageManager.getResourcesForApplication(applicationInfo)
    val iconIds = buildList {
        if (applicationInfo.icon != 0) add(applicationInfo.icon)
        packageManager.getLaunchIntentForPackage(packageName)?.component?.let { component ->
            packageManager.getActivityInfo(component, PackageManager.GET_META_DATA).icon
                .takeIf { it != 0 }?.let(::add)
        }
    }.distinct()
    iconIds.firstNotNullOfOrNull { iconId -> findMonochromeDrawable(resources, iconId, context) }
}.getOrNull()

fun hasNativeMonochrome(packageManager: PackageManager, applicationInfo: ApplicationInfo): Boolean =
    runCatching {
        val resources = packageManager.getResourcesForApplication(applicationInfo)
        val iconIds = buildList {
            if (applicationInfo.icon != 0) add(applicationInfo.icon)
            packageManager.getLaunchIntentForPackage(applicationInfo.packageName)?.component?.let { component ->
                packageManager.getActivityInfo(component, PackageManager.GET_META_DATA).icon
                    .takeIf { it != 0 }?.let(::add)
            }
        }.distinct()
        iconIds.any { findMonochromeDrawable(resources, it, null) != null }
    }.getOrDefault(false)

private fun findMonochromeDrawable(
    resources: Resources,
    iconId: Int,
    context: Context?
): Drawable? = runCatching {
    val parser = resources.getXml(iconId)
    var monochromeId = 0
    parser.use {
        var event = it.eventType
        var rootSeen = false
        var rootDepth = -1
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && !rootSeen) {
                rootSeen = true
                if (it.name != "adaptive-icon") break
                rootDepth = it.depth
            } else if (event == XmlPullParser.START_TAG && it.name == "monochrome" && it.depth == rootDepth + 1) {
                val attrs = resources.obtainAttributes(
                    android.util.Xml.asAttributeSet(it),
                    intArrayOf(android.R.attr.drawable)
                )
                monochromeId = try { attrs.getResourceId(0, 0) } finally { attrs.recycle() }
                break
            }
            event = it.next()
        }
    }
    if (monochromeId == 0) null else resources.getDrawable(monochromeId, context?.theme)
}.getOrNull()

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
                installTime = packageManager.getPackageInfo(
                    applicationInfo.packageName,
                    0
                ).firstInstallTime,
                size = File(applicationInfo.sourceDir).length(),
                icon = applicationInfo.loadIcon(packageManager).toBitmap().asImageBitmap(),
                isAdapted = applicationInfo.packageName in adaptedPackages,
                hasNativeMonochrome = hasNativeMonochrome(packageManager, applicationInfo),
                isSystem = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 ||
                        applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                userId = applicationInfo.uid / 100000
            )
        }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
        .toList()
}
