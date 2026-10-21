package com.immortal521.colorosiconspatch.xposed

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import com.highcapable.yukihookapi.annotation.xposed.YukiHookLibXposedEntry
import com.highcapable.yukihookapi.hook.factory.encase
import com.highcapable.yukihookapi.hook.log.YLog
import com.highcapable.yukihookapi.hook.xposed.YukiHookXposedModule
import com.highcapable.yukihookapi.hook.xposed.bridge.event.registerFrameworkEvents
import com.highcapable.yukihookapi.hook.xposed.bridge.event.v101.onModuleLoaded
import java.lang.ref.WeakReference

@YukiHookLibXposedEntry(
    minApiVersion = 102,
    targetApiVersion = 102,
    scope = ["com.android.launcher", "com.android.launcher3"]
)
@SuppressLint("PrivateApi")
class MainModule : YukiHookXposedModule {
    companion object {
        private const val TAG = "ColorOSIconsPatch"
        private const val ACTION_REFRESH = "com.immortal521.colorosiconspatch.action.REFRESH_LAUNCHER_ICONS"
        private const val ACTION_PING = "com.immortal521.colorosiconspatch.action.CHECK_LSPOSED"
        private const val LAUNCHER_PACKAGE = "com.android.launcher"
        private const val LAUNCHER3_PACKAGE = "com.android.launcher3"

        private var launcherRef = WeakReference<Context>(null)
        private var receiver: BroadcastReceiver? = null
        private val hookedPackages = mutableSetOf<String>()
        private val pendingLogs = ArrayDeque<PendingLog>()
        private const val MAX_PENDING_LOGS = 100

        private data class PendingLog(val level: Int, val message: String)
    }

    override fun onInit() {
        registerFrameworkEvents {
            onModuleLoaded { param ->
                writeLog(Log.INFO, "Module loaded in ${param.processName}")
            }
        }
    }

    override fun onHook() = encase {
        loadApp(LAUNCHER_PACKAGE, LAUNCHER3_PACKAGE) {
            val targetPackage = packageName
            synchronized(hookedPackages) {
                if (!hookedPackages.add(targetPackage)) return@loadApp
            }

            try {
                val classLoader = requireNotNull(hostClassLoader)
                val applicationName = appInfo.className
                    ?.takeIf { it.isNotBlank() }
                    ?: "android.app.Application"
                val applicationClass = classLoader.loadClass(applicationName)
                findAttachBaseContext(applicationClass).intercept {
                    val result = proceed()
                    instanceOrNull<Context>()?.let { context ->
                        launcherRef = WeakReference(context)
                        registerReceiver(context)
                        flushPendingLogs(context)
                        writeLog(Log.INFO, "Launcher application captured: $targetPackage")
                    }
                    result
                }

                val launcherClassName = when (targetPackage) {
                    LAUNCHER3_PACKAGE -> "com.android.launcher3.Launcher"
                    else -> "com.android.launcher.Launcher"
                }
                val launcherClass = classLoader.loadClass(launcherClassName)
                launcherClass.getDeclaredMethod("onCreate", Bundle::class.java).intercept {
                    val result = proceed()
                    instanceOrNull<Context>()?.let { context ->
                        launcherRef = WeakReference(context)
                        registerReceiver(context)
                        flushPendingLogs(context)
                    }
                    result
                }
                writeLog(Log.INFO, "Hooks installed for $targetPackage")
            } catch (error: Throwable) {
                writeLog(Log.ERROR, "Failed to hook $targetPackage", error)
            }
        }
    }

    private fun findAttachBaseContext(type: Class<*>): java.lang.reflect.Method {
        var current: Class<*>? = type
        while (current != null) {
            try {
                return current.getDeclaredMethod("attachBaseContext", Context::class.java)
            } catch (_: NoSuchMethodException) {
                current = current.superclass
            }
        }
        throw NoSuchMethodException("attachBaseContext on ${type.name}")
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver(context: Context) {
        if (receiver != null) return
        val refreshReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    ACTION_PING -> setResultCode(android.app.Activity.RESULT_OK)
                    ACTION_REFRESH -> refreshLauncher()
                }
            }
        }
        try {
            context.registerReceiver(
                refreshReceiver,
                IntentFilter().apply {
                    addAction(ACTION_PING)
                    addAction(ACTION_REFRESH)
                },
                Context.RECEIVER_EXPORTED
            )
            receiver = refreshReceiver
            writeLog(Log.INFO, "Refresh receiver registered")
        } catch (_: Throwable) {
            writeLog(Log.ERROR, "Failed to register refresh receiver")
        }
    }

    private fun refreshLauncher() {
        val context = launcherRef.get() ?: return
        try {
            val appStateClass = context.classLoader.loadClass("com.android.launcher3.LauncherAppState")
            val getInstance = appStateClass.getMethod("getInstance", Context::class.java)
            val appState = getInstance.invoke(null, context)
            val refresh = appStateClass.getDeclaredMethod("refreshAndReloadLauncher")
            refresh.isAccessible = true
            refresh.invoke(appState)
            writeLog(Log.INFO, "Launcher icons refreshed")
        } catch (_: NoSuchMethodException) {
            refreshWithModelFallback(context)
        } catch (error: Throwable) {
            writeLog(Log.ERROR, "Failed to refresh launcher icons", error)
        }
    }

    private fun writeLog(level: Int, message: String, error: Throwable? = null) {
        val fullMessage = if (error == null) message else "$message: ${error.message}"
        when (level) {
            Log.ERROR -> YLog.error(message, error, TAG)
            Log.WARN -> YLog.warn(message, error, TAG)
            Log.DEBUG -> YLog.debug(message, error, TAG)
            else -> YLog.info(message, error, TAG)
        }
        val context = launcherRef.get()
        if (context == null) {
            synchronized(pendingLogs) {
                if (pendingLogs.size == MAX_PENDING_LOGS) pendingLogs.removeFirst()
                pendingLogs.addLast(PendingLog(level, fullMessage))
            }
            return
        }
        sendLog(context, level, fullMessage)
    }

    private fun flushPendingLogs(context: Context) {
        val logs = synchronized(pendingLogs) {
            pendingLogs.toList().also { pendingLogs.clear() }
        }
        logs.forEach { sendLog(context, it.level, it.message) }
    }

    private fun sendLog(context: Context, level: Int, message: String) {
        runCatching {
            context.sendBroadcast(Intent("com.immortal521.colorosiconspatch.action.LSPOSED_LOG").apply {
                setPackage("com.immortal521.colorosiconspatch")
                putExtra(
                    "level", when (level) {
                        Log.ERROR -> "ERROR"
                        Log.WARN -> "WARN"
                        Log.DEBUG -> "DEBUG"
                        else -> "INFO"
                    }
                )
                putExtra("message", message)
            })
        }
    }

    private fun refreshWithModelFallback(context: Context) {
        try {
            val appStateClass = context.classLoader.loadClass("com.android.launcher3.LauncherAppState")
            val appState = appStateClass.getMethod("getInstance", Context::class.java).invoke(null, context)
            val model = appStateClass.getMethod("getModel").invoke(appState)
            model.javaClass.getMethod("forceReload").invoke(model)
            writeLog(Log.INFO, "Launcher model reloaded")
        } catch (error: Throwable) {
            writeLog(Log.ERROR, "Launcher model reload failed", error)
        }
    }
}
