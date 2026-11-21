package com.example.downloads

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Saves a downloaded response into the public Downloads folder.
 * Android 10+ uses MediaStore (no storage permission needed); older versions write to the Downloads directory.
 */
object ZenDownloader {
    private val io = Executors.newCachedThreadPool()

    private class ActiveDownload(
        val id: String,
        @Volatile var fileName: String,
        val url: String,
        @Volatile var mimeType: String,
        @Volatile var totalBytes: Long,
        @Volatile var currentBytes: Long = 0L,
        @Volatile var inputStream: InputStream? = null,
        @Volatile var connection: HttpURLConnection? = null,
        @Volatile var onStateChange: ((state: String, done: Long, total: Long, speed: Long) -> Unit)? = null
    ) {
        val paused = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)
        val lock = Object()
    }

    private val activeDownloads = ConcurrentHashMap<String, ActiveDownload>()
    private val cancelledIds = ConcurrentHashMap<String, Long>()

    fun isActive(id: String): Boolean = activeDownloads.containsKey(id)

    fun isPaused(id: String): Boolean = activeDownloads[id]?.paused?.get() == true

    fun wasCancelled(id: String): Boolean =
        activeDownloads[id]?.cancelled?.get() == true || cancelledIds.containsKey(id)

    fun pause(context: Context, id: String) {
        val app = context.applicationContext
        val ctrl = activeDownloads[id] ?: return
        if (ctrl.cancelled.get()) return
        ctrl.paused.set(true)
        synchronized(ctrl.lock) { ctrl.lock.notifyAll() }
        notifyProgress(app, id, ctrl.fileName, ctrl.currentBytes, ctrl.totalBytes, 0L, isPaused = true)
        ctrl.onStateChange?.invoke("paused", ctrl.currentBytes, ctrl.totalBytes, 0L)
    }

    fun resume(context: Context, id: String): Boolean {
        val app = context.applicationContext
        val ctrl = activeDownloads[id] ?: return false
        if (ctrl.cancelled.get()) return false
        ctrl.paused.set(false)
        synchronized(ctrl.lock) { ctrl.lock.notifyAll() }
        notifyProgress(app, id, ctrl.fileName, ctrl.currentBytes, ctrl.totalBytes, 0L, isPaused = false)
        ctrl.onStateChange?.invoke("downloading", ctrl.currentBytes, ctrl.totalBytes, 0L)
        return true
    }

    fun cancel(context: Context, id: String) {
        val app = context.applicationContext
        cancelledIds[id] = System.currentTimeMillis()
        val ctrl = activeDownloads[id]
        if (ctrl != null) {
            ctrl.cancelled.set(true)
            ctrl.paused.set(false)
            synchronized(ctrl.lock) { ctrl.lock.notifyAll() }
            io.execute {
                runCatching { ctrl.inputStream?.close() }
                runCatching { ctrl.connection?.disconnect() }
            }
            ctrl.onStateChange?.invoke("cancelled", ctrl.currentBytes, ctrl.totalBytes, 0L)
        }
        cancelNotification(app, id)
    }

    fun cancelNotification(context: Context, id: String) {
        runCatching {
            androidx.core.app.NotificationManagerCompat.from(context).cancel(id.hashCode())
        }
    }

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
        onDone: (Boolean, Long) -> Unit,
        downloadId: String? = null,
        url: String = "",
        totalBytes: Long = 0L,
        onStateChange: ((state: String, done: Long, total: Long, speed: Long) -> Unit)? = null
    ) {
        val id = downloadId ?: ("dl_" + java.util.UUID.randomUUID().toString().take(8))
        val ctrl = activeDownloads.getOrPut(id) {
            ActiveDownload(id, fileName, url, mimeType, totalBytes, onStateChange = onStateChange)
        }.apply {
            this.fileName = fileName
            this.mimeType = mimeType
            if (totalBytes > 0L) this.totalBytes = totalBytes
            this.inputStream = input
            if (onStateChange != null) this.onStateChange = onStateChange
        }
        saveInternal(context, ctrl, input, onProgress, onDone)
    }

    private fun saveInternal(
        context: Context,
        ctrl: ActiveDownload,
        input: InputStream,
        onProgress: (Long, Long) -> Unit,
        onDone: (Boolean, Long) -> Unit
    ) {
        val app = context.applicationContext
        io.execute {
            var bytes = 0L
            val startedAt = System.currentTimeMillis()
            var lastTick = 0L
            var lastTickBytes = 0L
            fun waitIfPaused() {
                var pausedOnce = false
                while (ctrl.paused.get() && !ctrl.cancelled.get()) {
                    pausedOnce = true
                    synchronized(ctrl.lock) {
                        if (ctrl.paused.get() && !ctrl.cancelled.get()) {
                            runCatching { ctrl.lock.wait(300) }
                        }
                    }
                }
                if (pausedOnce) {
                    lastTick = System.currentTimeMillis()
                    lastTickBytes = bytes
                }
                if (ctrl.cancelled.get()) {
                    throw java.io.InterruptedIOException("Download cancelled")
                }
            }
            fun copy(src: InputStream, out: java.io.OutputStream) {
                val buf = ByteArray(64 * 1024)
                while (true) {
                    waitIfPaused()
                    val n = src.read(buf)
                    if (n < 0) break
                    waitIfPaused()
                    out.write(buf, 0, n)
                    bytes += n
                    ctrl.currentBytes = bytes
                    val now = System.currentTimeMillis()
                    if (now - lastTick >= 350 && !ctrl.paused.get() && !ctrl.cancelled.get()) {
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
                            put(MediaStore.Downloads.DISPLAY_NAME, ctrl.fileName)
                            put(MediaStore.Downloads.MIME_TYPE, ctrl.mimeType.ifBlank { "application/octet-stream" })
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
                        var file = File(dir, ctrl.fileName)
                        var n = 1
                        while (file.exists()) {
                            val base = ctrl.fileName.substringBeforeLast('.', ctrl.fileName)
                            val ext = ctrl.fileName.substringAfterLast('.', "")
                            file = File(dir, if (ext.isEmpty()) "$base ($n)" else "$base ($n).$ext")
                            n++
                        }
                        try {
                            file.outputStream().use { out -> copy(src, out) }
                            android.media.MediaScannerConnection.scanFile(app, arrayOf(file.absolutePath), null, null)
                        } catch (t: Throwable) {
                            runCatching { file.delete() }
                            throw t
                        }
                    }
                }
                true
            } catch (_: Throwable) {
                false
            } finally {
                runCatching { ctrl.connection?.disconnect() }
                activeDownloads.remove(ctrl.id)
            }
            if (!ctrl.cancelled.get()) {
                cancelledIds.remove(ctrl.id)
            }
            onDone(ok, bytes)
        }
    }

    /** Plain HTTP(S) download of [url] (used for "Download image" or resuming URL downloads). Result goes through [save]. */
    fun saveFromUrl(
        context: Context,
        url: String,
        onStart: (fileName: String, mimeType: String, totalBytes: Long) -> Unit,
        onProgress: (Long, Long) -> Unit,
        onDone: (Boolean, Long) -> Unit,
        downloadId: String? = null,
        onStateChange: ((state: String, done: Long, total: Long, speed: Long) -> Unit)? = null
    ) {
        val id = downloadId ?: ("dl_" + java.util.UUID.randomUUID().toString().take(8))
        cancelledIds.remove(id)
        val ctrl = ActiveDownload(
            id = id,
            fileName = fileNameFor(url, null, null),
            url = url,
            mimeType = "",
            totalBytes = 0L,
            onStateChange = onStateChange
        )
        activeDownloads[id] = ctrl
        io.execute {
            try {
                if (ctrl.cancelled.get()) throw java.io.InterruptedIOException("Download cancelled")
                val conn = (java.net.URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) ZenBrowser")
                    instanceFollowRedirects = true
                }
                ctrl.connection = conn
                if (conn.responseCode !in 200..299) throw java.io.IOException("HTTP ${conn.responseCode}")
                val mime = (conn.contentType ?: "").substringBefore(';').trim()
                val headers = HashMap<String, String>()
                conn.headerFields.forEach { (k, v) -> if (k != null) headers[k] = v.firstOrNull().orEmpty() }
                val name = fileNameFor(url, headers, mime)
                val total = conn.contentLengthLong
                ctrl.fileName = name
                ctrl.mimeType = mime
                ctrl.totalBytes = total
                ctrl.inputStream = conn.inputStream
                onStart(name, mime, total)
                saveInternal(context, ctrl, conn.inputStream, onProgress, onDone)
            } catch (_: Throwable) {
                activeDownloads.remove(id)
                onDone(false, 0L)
            }
        }
    }

    // ---- Notification with a progress bar, Pause/Resume & Cancel actions ----
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

    fun notifyProgress(
        context: Context,
        id: String,
        fileName: String,
        done: Long,
        total: Long,
        speed: Long,
        isPaused: Boolean = false
    ) {
        if (wasCancelled(id)) return
        runCatching {
            ensureChannel(context)
            val toggleAction = if (isPaused) {
                ZenDownloadActionReceiver.ACTION_RESUME
            } else {
                ZenDownloadActionReceiver.ACTION_PAUSE
            }
            val toggleIntent = Intent(context, ZenDownloadActionReceiver::class.java).apply {
                action = toggleAction
                putExtra(ZenDownloadActionReceiver.EXTRA_DOWNLOAD_ID, id)
            }
            val togglePending = PendingIntent.getBroadcast(
                context,
                (id + "_toggle").hashCode(),
                toggleIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val cancelIntent = Intent(context, ZenDownloadActionReceiver::class.java).apply {
                action = ZenDownloadActionReceiver.ACTION_CANCEL
                putExtra(ZenDownloadActionReceiver.EXTRA_DOWNLOAD_ID, id)
            }
            val cancelPending = PendingIntent.getBroadcast(
                context,
                (id + "_cancel").hashCode(),
                cancelIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val progressText = buildString {
                if (isPaused) append("Paused • ")
                if (total > 0) append("${(done * 100 / total).coerceIn(0, 100)}% • ")
                append(formatBytes(done))
                if (total > 0) append(" / ${formatBytes(total)}")
                if (!isPaused && speed > 0) append(" • ${formatBytes(speed)}/s")
            }

            val b = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(
                    if (isPaused) android.R.drawable.ic_media_pause
                    else android.R.drawable.stat_sys_download
                )
                .setContentTitle(fileName)
                .setOnlyAlertOnce(true)
                .setOngoing(!isPaused)
                .setContentText(progressText)
                .setProgress(
                    if (total > 0) 100 else 0,
                    if (total > 0) ((done * 100 / total).toInt()).coerceIn(0, 100) else 0,
                    total <= 0 && !isPaused
                )
                .addAction(
                    if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                    if (isPaused) "Resume" else "Pause",
                    togglePending
                )
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Cancel",
                    cancelPending
                )
            androidx.core.app.NotificationManagerCompat.from(context).notify(id.hashCode(), b.build())
        }
    }

    fun notifyFinished(context: Context, id: String, fileName: String, ok: Boolean) {
        if (!ok && wasCancelled(id)) {
            cancelNotification(context, id)
            cancelledIds.remove(id)
            return
        }
        cancelledIds.remove(id)
        runCatching {
            ensureChannel(context)
            val open = PendingIntent.getActivity(
                context, id.hashCode(),
                Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val b = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(if (ok) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error)
                .setContentTitle(fileName)
                .setContentText(if (ok) "Download complete" else "Download failed")
                .setAutoCancel(true)
                .setOngoing(false)
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
