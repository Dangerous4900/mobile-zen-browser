package com.example.gecko

import android.content.Context
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe persistent store mapping normalized page URLs to their last scrollY offset (CSS px).
 * Survives back/forward navigation, reloads, tab switching, and cold app restarts.
 */
object ZenScrollStore {
    private const val PREFS_NAME = "zen_scroll_prefs"
    private const val TS_PREFIX = "ts_"
    private const val MAX_ENTRIES = 250

    private val memoryCache = ConcurrentHashMap<String, Int>()

    fun normalizeUrl(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank() || trimmed == "about:blank" || trimmed.startsWith("zen://")) {
            return null
        }
        val lower = trimmed.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return null
        }
        val withoutFragment = trimmed.substringBefore('#')
        return if (withoutFragment.length > 8 && withoutFragment.endsWith("/")) {
            withoutFragment.dropLast(1)
        } else {
            withoutFragment
        }
    }

    fun saveScrollY(context: Context?, rawUrl: String, scrollY: Int) {
        val key = normalizeUrl(rawUrl) ?: return
        val clamped = scrollY.coerceAtLeast(0)
        memoryCache[key] = clamped
        val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
            .putInt(key, clamped)
            .putLong(TS_PREFIX + key, now)

        // Prune oldest entries periodically if we exceed MAX_ENTRIES
        val all = prefs.all
        val urlKeys = all.keys.filter { !it.startsWith(TS_PREFIX) }
        if (urlKeys.size > MAX_ENTRIES) {
            val toDrop = urlKeys
                .sortedBy { k -> (all[TS_PREFIX + k] as? Number)?.toLong() ?: 0L }
                .take(urlKeys.size - MAX_ENTRIES)
            toDrop.forEach { oldKey ->
                memoryCache.remove(oldKey)
                editor.remove(oldKey).remove(TS_PREFIX + oldKey)
            }
        }
        editor.apply()
    }

    fun getScrollY(context: Context?, rawUrl: String): Int {
        val key = normalizeUrl(rawUrl) ?: return 0
        memoryCache[key]?.let { return it }
        val prefs = context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) ?: return 0
        val stored = prefs.getInt(key, 0).coerceAtLeast(0)
        memoryCache[key] = stored
        return stored
    }

    fun clearAll(context: Context?) {
        memoryCache.clear()
        context?.applicationContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)?.edit()?.clear()?.apply()
    }
}
