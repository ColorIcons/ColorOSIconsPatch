package com.immortal521.colorosiconspatch.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

private val launcherIconCache = ConcurrentHashMap<String, androidx.compose.ui.graphics.ImageBitmap>()
private val nativeMonochromeCache = ConcurrentHashMap<String, Boolean>()
private val installedAppCache = ConcurrentHashMap<String, InstalledApp>()

@Volatile
private var systemUiPackageManager: PackageManager? = null

private fun getSystemUiPackageManager(context: Context): PackageManager? {
    systemUiPackageManager?.let { return it }
    return synchronized(InstalledApp::class.java) {
        systemUiPackageManager ?: runCatching {
            context.createPackageContext(
                "com.android.systemui",
                Context.CONTEXT_IGNORE_SECURITY
            ).packageManager
        }.getOrNull()?.also { systemUiPackageManager = it }
    }
}

private fun oshinIcon(context: Context, packageName: String): androidx.compose.ui.graphics.ImageBitmap {
    val packageManager = context.packageManager
    val themedPackageManager = getSystemUiPackageManager(context)
    val drawable = runCatching {
        themedPackageManager?.getApplicationIcon(packageName)
            ?: throw PackageManager.NameNotFoundException()
    }.getOrElse {
        runCatching { packageManager.getApplicationIcon(packageName) }
            .getOrElse { packageManager.getApplicationInfo(packageName, 0).loadIcon(packageManager) }
    }
    val bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
        drawable.bitmap
    } else {
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            val canvas = Canvas(it)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
        }
    }

    var left = bitmap.width
    var top = bitmap.height
    var right = -1
    var bottom = -1
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    for (y in 0 until bitmap.height) {
        for (x in 0 until bitmap.width) {
            if (pixels[y * bitmap.width + x] ushr 24 != 0) {
                left = minOf(left, x)
                top = minOf(top, y)
                right = maxOf(right, x)
                bottom = maxOf(bottom, y)
            }
        }
    }
    val cropped = if (right >= left && bottom >= top) {
        Bitmap.createBitmap(bitmap, left, top, right - left + 1, bottom - top + 1)
    } else bitmap
    val maxSize = maxOf(cropped.width, cropped.height)
    val normalized = if (maxSize > 192) {
        val scale = 192f / maxSize
        Bitmap.createScaledBitmap(
            cropped,
            (cropped.width * scale).toInt().coerceAtLeast(1),
            (cropped.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else cropped
    return normalized.asImageBitmap()
}

private fun iconResourceIds(
    packageManager: PackageManager,
    applicationInfo: ApplicationInfo
): List<Int> = buildList {
    packageManager.getLaunchIntentForPackage(applicationInfo.packageName)?.component?.let { component ->
        packageManager.getActivityInfo(component, PackageManager.GET_META_DATA).icon
            .takeIf { it != 0 }?.let(::add)
    }
    if (applicationInfo.icon != 0) add(applicationInfo.icon)
}.distinct()

fun loadNativeMonochrome(context: Context, packageName: String): Drawable? = runCatching {
    val packageManager = context.packageManager
    val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
    val resources = packageManager.getResourcesForApplication(applicationInfo)
    iconResourceIds(packageManager, applicationInfo)
        .firstNotNullOfOrNull { iconId -> findMonochromeDrawable(resources, iconId, context) }
}.getOrNull()

fun hasNativeMonochrome(packageManager: PackageManager, applicationInfo: ApplicationInfo): Boolean =
    nativeMonochromeCache.getOrPut(applicationInfo.packageName) {
        runCatching {
            val resources = packageManager.getResourcesForApplication(applicationInfo)
            iconResourceIds(packageManager, applicationInfo)
                .any { findMonochromeDrawable(resources, it, null) != null }
        }.getOrDefault(false)
    }

fun clearInstalledAppCaches() {
    launcherIconCache.clear()
    nativeMonochromeCache.clear()
    installedAppCache.clear()
    systemUiPackageManager = null
}

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

suspend fun loadInstalledApps(
    context: Context,
    adaptedPackages: Set<String> = emptySet()
): List<InstalledApp> = coroutineScope {
    val packageManager = context.packageManager
    val ownPackage = context.packageName
    val applications = packageManager
        .getInstalledApplications(
            android.content.pm.PackageManager.ApplicationInfoFlags.of(0L)
        )
        .asSequence()
        .filter { it.packageName != ownPackage && it.enabled }
        .distinctBy { it.packageName }
        .toList()
    val dispatcher = Dispatchers.IO.limitedParallelism(4)

    applications.map { applicationInfo ->
        async(dispatcher) {
            val cached = installedAppCache[applicationInfo.packageName]
            val base = cached ?: InstalledApp(
                label = applicationInfo.loadLabel(packageManager).toString(),
                packageName = applicationInfo.packageName,
                installTime = packageManager.getPackageInfo(
                    applicationInfo.packageName,
                    0
                ).firstInstallTime,
                size = File(applicationInfo.sourceDir).length(),
                icon = launcherIconCache.getOrPut(applicationInfo.packageName) {
                    oshinIcon(context, applicationInfo.packageName)
                },
                hasNativeMonochrome = hasNativeMonochrome(packageManager, applicationInfo),
                isSystem = applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0 ||
                        applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0,
                userId = applicationInfo.uid / 100000
            ).also { installedAppCache[applicationInfo.packageName] = it }
            base.copy(isAdapted = applicationInfo.packageName in adaptedPackages)
        }
    }.awaitAll()
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
}
