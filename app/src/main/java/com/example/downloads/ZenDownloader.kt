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

    /** Copies [input] to Downloads on a background thread; [onDone] is called there with (savedOk, finalBytes). */
    fun save(
        context: Context,
        fileName: String,
        mimeType: String,
        input: InputStream,
        onDone: (Boolean, Long) -> Unit
    ) {
        val app = context.applicationContext
        io.execute {
            var bytes = 0L
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
                            resolver.openOutputStream(target)!!.use { out -> bytes = src.copyTo(out) }
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
                        file.outputStream().use { out -> bytes = src.copyTo(out) }
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
}
