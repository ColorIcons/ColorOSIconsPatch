package com.immortal521.colorosiconspatch.xposed

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.lang.ref.WeakReference

@SuppressLint("PrivateApi")
class MainModule : XposedModule() {
    companion object {
        private const val TAG = "ColorOSIconsPatch"
        private const val ACTION_REFRESH = "com.immortal521.colorosiconspatch.action.REFRESH_LAUNCHER_ICONS"
        private const val ACTION_PING = "com.immortal521.colorosiconspatch.action.CHECK_LSPOSED"
        private const val LAUNCHER_PACKAGE = "com.android.launcher"
        private const val LAUNCHER3_PACKAGE = "com.android.launcher3"

        private var launcherRef = WeakReference<Context>(null)
        private var receiver: BroadcastReceiver? = null
        private val hookedPackages = mutableSetOf<String>()
    }

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        writeLog(Log.INFO, "Module loaded in ${param.processName}")
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        val packageName = param.packageName
        if (packageName != LAUNCHER_PACKAGE && packageName != LAUNCHER3_PACKAGE) return
        synchronized(hookedPackages) {
            if (!hookedPackages.add(packageName)) return
        }

        try {
            val classLoader = param.defaultClassLoader
            val applicationName = param.applicationInfo.className
                ?.takeIf { it.isNotBlank() }
                ?: "android.app.Application"
            val applicationClass = classLoader.loadClass(applicationName)
            val attachBaseContext = findAttachBaseContext(applicationClass)
            hook(attachBaseContext).intercept { chain ->
                val result = chain.proceed()
                val context = chain.thisObject as? Context
                if (context != null) {
                    launcherRef = WeakReference(context)
                    registerReceiver(context)
                    writeLog(Log.INFO, "Launcher application captured: $packageName")
                }
                result
            }

            val launcherClassName = when (packageName) {
                LAUNCHER3_PACKAGE -> "com.android.launcher3.Launcher"
                else -> "com.android.launcher.Launcher"
            }
            val launcherClass = classLoader.loadClass(launcherClassName)
            val onCreate = launcherClass.getDeclaredMethod("onCreate", Bundle::class.java)
            hook(onCreate).intercept { chain ->
                val result = chain.proceed()
                val context = chain.thisObject as? Context
                if (context != null) {
                    launcherRef = WeakReference(context)
                    registerReceiver(context)
                }
                result
            }
            writeLog(Log.INFO, "Hooks installed for $packageName")
        } catch (error: Throwable) {
            writeLog(Log.ERROR, "Failed to hook $packageName", error)
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
        log(level, TAG, message, error)
        val context = launcherRef.get() ?: return
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
                putExtra("message", if (error == null) message else "$message: ${error.message}")
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
