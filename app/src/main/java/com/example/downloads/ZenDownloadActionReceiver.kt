package com.example.downloads

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ZenDownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val downloadId = intent?.getStringExtra(EXTRA_DOWNLOAD_ID)?.takeIf { it.isNotBlank() } ?: return
        when (intent.action) {
            ACTION_PAUSE -> ZenDownloader.pause(context, downloadId)
            ACTION_RESUME -> ZenDownloader.resume(context, downloadId)
            ACTION_CANCEL -> ZenDownloader.cancel(context, downloadId)
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.example.downloads.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.downloads.ACTION_RESUME"
        const val ACTION_CANCEL = "com.example.downloads.ACTION_CANCEL"
        const val EXTRA_DOWNLOAD_ID = "download_id"
    }
}
