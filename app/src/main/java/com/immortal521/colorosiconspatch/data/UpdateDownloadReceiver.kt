package com.immortal521.colorosiconspatch.data

import android.app.DownloadManager
import android.content.Context
import androidx.core.content.edit
import java.io.File

object UpdateDownloadCleanup {
    private const val PREFS = "app_update"
    private const val KEY_DOWNLOAD_ID = "download_id"
    private const val KEY_FILENAME = "filename"
    private const val DEFAULT_FILENAME = "ColorOSIconsPatch-update.apk"

    fun rememberDownload(context: Context, id: Long, filename: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putLong(KEY_DOWNLOAD_ID, id)
                .putString(KEY_FILENAME, filename)
        }
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val downloadId = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId != -1L) {
            context.getSystemService(DownloadManager::class.java).remove(downloadId)
        }
        val filename = prefs.getString(KEY_FILENAME, DEFAULT_FILENAME) ?: DEFAULT_FILENAME
        File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
            filename
        ).delete()
        File(
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
            DEFAULT_FILENAME
        ).delete()
        prefs.edit { clear() }
    }
}
