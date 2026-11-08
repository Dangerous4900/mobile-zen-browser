package com.example.downloads

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors

/**
 * Saves a downloaded response into the public Downloads folder.
 * Android 10+ uses MediaStore (no storage permission needed); older versions write to the Downloads directory.
 */
object ZenDownloader {
    private val io = Executors.newCachedThreadPool()

    fun fileNameFor(uri: String, headers: Map<String, String>?, mimeType: String?): String {
        val cd = headers?.entries?.firstOrNull { it.key.equals("content-disposition", true) }?.value
        var name: String? = null
        if (cd != null) {
            Regex("filename\\*\\s*=\\s*[^']*''([^;]+)", RegexOption.IGNORE_CASE).find(cd)?.let {
                name = runCatching { java.net.URLDecoder.decode(it.groupValues[1].trim('"'), "UTF-8") }.getOrNull()
            }
            if (name.isNullOrBlank()) {
                Regex("filename\\s*=\\s*\"?([^\";]+)\"?", RegexOption.IGNORE_CASE).find(cd)?.let {
                    name = it.groupValues[1].trim()
                }
            }
        }
        if (name.isNullOrBlank()) {
            name = runCatching { Uri.parse(uri).lastPathSegment }.getOrNull()?.takeIf { it.isNotBlank() }
        }
        var result = (name ?: "download").replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120)
        if (!result.contains('.')) {
            val ext = mimeType?.substringBefore(';')?.trim()?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            if (!ext.isNullOrBlank()) result += ".$ext"
        }
        return result
    }

    /**
     * Copies [input] to Downloads on a background thread.
     * [onProgress] gets (downloadedBytes, bytesPerSecond) about 3x per second; [onDone] gets (savedOk, finalBytes).
     */
    fun save(
        context: Context,
        fileName: String,
        mimeType: String,
        input: InputStream,
        onProgress: (Long, Long) -> Unit = { _, _ -> },
        onDone: (Boolean, Long) -> Unit
    ) {
        val app = context.applicationContext
        io.execute {
            var bytes = 0L
            val startedAt = System.currentTimeMillis()
            var lastTick = 0L
            var lastTickBytes = 0L
            fun copy(src: InputStream, out: java.io.OutputStream) {
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = src.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    bytes += n
                    val now = System.currentTimeMillis()
                    if (now - lastTick >= 350) {
                        val dt = (now - (if (lastTick == 0L) startedAt else lastTick)).coerceAtLeast(1)
                        val speed = (bytes - lastTickBytes) * 1000 / dt
                        lastTick = now
                        lastTickBytes = bytes
                        onProgress(bytes, speed)
                    }
                }
            }
            val ok = try {
                input.use { src ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val resolver = app.contentResolver
                        val values = ContentValues().apply {
                            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                            put(MediaStore.Downloads.MIME_TYPE, mimeType.ifBlank { "application/octet-stream" })
                            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                            put(MediaStore.Downloads.IS_PENDING, 1)
                        }
                        val target = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                            ?: throw java.io.IOException("Cannot create file")
                        try {
                            resolver.openOutputStream(target)!!.use { out -> copy(src, out) }
                            resolver.update(target, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                        } catch (t: Throwable) {
                            runCatching { resolver.delete(target, null, null) }
                            throw t
                        }
                    } else {
                        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        dir.mkdirs()
                        var file = File(dir, fileName)
                        var n = 1
                        while (file.exists()) {
                            val base = fileName.substringBeforeLast('.', fileName)
                            val ext = fileName.substringAfterLast('.', "")
                            file = File(dir, if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext")
                            n++
                        }
                        file.outputStream().use { out -> copy(src, out) }
                        android.media.MediaScannerConnection.scanFile(app, arrayOf(file.absolutePath), null, null)
                    }
                }
                true
            } catch (_: Throwable) {
                false
            }
            onDone(ok, bytes)
        }
    }

    /** Plain HTTP(S) download of [url] (used for "Download image"). Result goes through [save]. */
    fun saveFromUrl(
        context: Context,
        url: String,
        onStart: (fileName: String, mimeType: String, totalBytes: Long) -> Unit,
        onProgress: (Long, Long) -> Unit,
        onDone: (Boolean, Long) -> Unit
    ) {
        io.execute {
            try {
                val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) ZenBrowser")
                    instanceFollowRedirects = true
                }
                if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
                val mime = (conn.contentType ?: "").substringBefore(';').trim()
                val headers = HashMap<String, String>()
                conn.headerFields.forEach { (k, v) -> if (k != null) headers[k] = v.firstOrNull().orEmpty() }
                val name = fileNameFor(url, headers, mime)
                onStart(name, mime, conn.contentLengthLong)
                save(context, name, mime, conn.inputStream, onProgress, onDone)
            } catch (_: Throwable) {
                onDone(false, 0L)
            }
        }
    }

    // ---- Notification with a progress bar, like other browsers ----
    private const val CHANNEL_ID = "zen_downloads"

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    android.app.NotificationChannel(CHANNEL_ID, "Downloads", android.app.NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
    }

    fun notifyProgress(context: Context, id: String, fileName: String, done: Long, total: Long, speed: Long) {
        runCatching {
            ensureChannel(context)
            val b = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(fileName)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setContentText(
                    (if (total > 0) "${done * 100 / total}% • " else "") + formatBytes(done) +
                        (if (total > 0) " / ${formatBytes(total)}" else "") + " • ${formatBytes(speed)}/s"
                )
                .setProgress(if (total > 0) 100 else 0, if (total > 0) (done * 100 / total).toInt() else 0, total <= 0)
            androidx.core.app.NotificationManagerCompat.from(context).notify(id.hashCode(), b.build())
        }
    }

    fun notifyFinished(context: Context, id: String, fileName: String, ok: Boolean) {
        runCatching {
            ensureChannel(context)
            val open = android.app.PendingIntent.getActivity(
                context, id.hashCode(),
                android.content.Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            val b = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(if (ok) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                .setContentTitle(fileName)
                .setContentText(if (ok) "Download complete" else "Download failed")
                .setAutoCancel(true)
                .setContentIntent(open)
            androidx.core.app.NotificationManagerCompat.from(context).notify(id.hashCode(), b.build())
        }
    }

    fun formatBytes(b: Long): String = when {
        b >= 1L shl 30 -> String.format("%.2f GB", b / (1024.0 * 1024 * 1024))
        b >= 1L shl 20 -> String.format("%.1f MB", b / (1024.0 * 1024))
        b >= 1L shl 10 -> String.format("%.0f KB", b / 1024.0)
        else -> "$b B"
    }
}
