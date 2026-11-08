package com.example.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** In-memory page previews used by the tabs overview grid. */
object ZenTabThumbnails {
    private val cache = LruCache<String, Bitmap>(32)
    var version by mutableIntStateOf(0)
        private set

    fun get(tabId: String): Bitmap? = cache.get(tabId)

    fun put(tabId: String, source: Bitmap) {
        val targetW = 420
        val scaled = if (source.width > targetW) {
            val h = (source.height * (targetW.toFloat() / source.width)).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, targetW, h, true)
        } else source
        cache.put(tabId, scaled)
        version++
    }

    fun remove(tabId: String) {
        cache.remove(tabId)
        version++
    }
}
