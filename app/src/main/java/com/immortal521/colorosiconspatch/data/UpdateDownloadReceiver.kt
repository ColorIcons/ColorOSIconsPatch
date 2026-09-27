package com.immortal521.colorosiconspatch.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

class UpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (downloadId != prefs.getLong(KEY_DOWNLOAD_ID, -1L)) return

        val manager = context.getSystemService(DownloadManager::class.java)
        val query = manager.query(DownloadManager.Query().setFilterById(downloadId)) ?: return
        query.use { cursor ->
            if (!cursor.moveToFirst() || cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL) return
        }
        val uri = manager.getUriForDownloadedFile(downloadId) ?: return
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri("update", uri)
        })
    }

    companion object {
        private const val PREFS = "app_update"
        private const val KEY_DOWNLOAD_ID = "download_id"
        private const val KEY_FILENAME = "filename"

        fun rememberDownload(context: Context, id: Long, filename: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
                putLong(KEY_DOWNLOAD_ID, id)
                    .putString(KEY_FILENAME, filename)
            }
        }
    }
}
