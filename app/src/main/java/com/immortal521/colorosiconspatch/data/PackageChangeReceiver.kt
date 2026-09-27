package com.immortal521.colorosiconspatch.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_REPLACED && intent.data?.schemeSpecificPart == context.packageName) {
            val prefs = context.getSharedPreferences("app_update", Context.MODE_PRIVATE)
            val downloadId = prefs.getLong("download_id", -1L)
            if (downloadId != -1L) {
                context.getSystemService(android.app.DownloadManager::class.java).remove(downloadId)
                prefs.edit().clear().apply()
            }
        }
        if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
            intent.action == Intent.ACTION_PACKAGE_REMOVED ||
            intent.action == Intent.ACTION_PACKAGE_REPLACED
        ) {
            context.sendBroadcast(Intent(ACTION_PACKAGE_SET_CHANGED).setPackage(context.packageName))
        }
    }

    companion object {
        const val ACTION_PACKAGE_SET_CHANGED =
            "com.immortal521.colorosiconspatch.PACKAGE_SET_CHANGED"
    }
}
