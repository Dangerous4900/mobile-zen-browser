package com.example.mediacapture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

enum class MediaKind {
    VIDEO, AUDIO, SUBTITLE
}

enum class MediaCaptureSort {
    DEFAULT, QUALITY, SIZE
}

data class DetectedMedia(
    val id: String,
    val url: String,
    val kind: MediaKind,
    val format: String,
    val mimeType: String? = null,
    val label: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val bandwidthBitsPerSec: Long? = null,
    val durationSeconds: Double? = null,
    val sizeBytes: Long? = null,
    val isLive: Boolean = false,
    val hasAudio: Boolean? = null,
    val groupUrl: String? = null,
    val pageUrl: String = "",
    val detectedAtMillis: Long = System.currentTimeMillis(),
    val isBest: Boolean = false,
    val codecs: String? = null,
    val channels: Int? = null
) {
    val displayResolution: String
        get() {
            if (label != null && label.contains("p", ignoreCase = true)) return label
            if (height != null && height > 0) return "${height}p"
            return when {
                bandwidthBitsPerSec != null && bandwidthBitsPerSec > 4_000_000 -> "1080p HD"
                bandwidthBitsPerSec != null && bandwidthBitsPerSec > 2_000_000 -> "720p HD"
                bandwidthBitsPerSec != null && bandwidthBitsPerSec > 1_000_000 -> "480p"
                else -> format.uppercase()
            }
        }

    val displayBitrate: String
        get() {
            if (bandwidthBitsPerSec != null && bandwidthBitsPerSec > 0) {
                val kbps = bandwidthBitsPerSec / 1000
                return "$kbps kbps"
            }
            return ""
        }

    val estimatedBytes: Long?
        get() {
            if (sizeBytes != null && sizeBytes > 0) return sizeBytes
            if (durationSeconds != null && durationSeconds > 0 && bandwidthBitsPerSec != null && bandwidthBitsPerSec > 0) {
                return ((durationSeconds * bandwidthBitsPerSec) / 8).toLong()
            }
            return null
        }
}

object MediaCaptureStore {
    private val tabMedia = ConcurrentHashMap<String, MutableStateFlow<List<DetectedMedia>>>()
    private val seenUrls = ConcurrentHashMap<String, MutableSet<String>>()

    fun observe(tabId: String): StateFlow<List<DetectedMedia>> {
        return tabMedia.getOrPut(tabId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    fun getItems(tabId: String): List<DetectedMedia> {
        return tabMedia[tabId]?.value ?: emptyList()
    }

    fun count(tabId: String): Int {
        return tabMedia[tabId]?.value?.size ?: 0
    }

    fun markSeen(tabId: String, url: String): Boolean {
        val set = seenUrls.getOrPut(tabId) { ConcurrentHashMap.newKeySet() }
        return set.add(url)
    }

    fun add(tabId: String, media: DetectedMedia) {
        val flow = tabMedia.getOrPut(tabId) { MutableStateFlow(emptyList()) }
        val current = flow.value
        if (current.any { it.url == media.url }) return

        val updated = (current + media).toMutableList()
        // Determine best video stream
        val videos = updated.filter { it.kind == MediaKind.VIDEO }
        val bestVideo = videos.maxByOrNull {
            (it.height ?: 0) * 10_000_000L + (it.bandwidthBitsPerSec ?: 0L)
        }

        val marked = updated.map { item ->
            if (bestVideo != null && item.id == bestVideo.id) {
                item.copy(isBest = true)
            } else {
                item.copy(isBest = false)
            }
        }
        flow.value = marked
    }

    fun clearTab(tabId: String) {
        tabMedia.remove(tabId)
        seenUrls.remove(tabId)
    }

    fun clearAll() {
        tabMedia.clear()
        seenUrls.clear()
    }
}
