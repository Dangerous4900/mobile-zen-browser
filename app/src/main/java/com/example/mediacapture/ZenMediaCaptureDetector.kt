package com.example.mediacapture

import android.net.Uri
import java.util.UUID

object ZenMediaCaptureDetector {

    private val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mkv", "mov", "m4v", "3gp", "flv", "avi", "wmv", "m3u8", "mpd")
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "oga", "opus", "weba")
    private val SUBTITLE_EXTENSIONS = setOf("vtt", "srt")

    /**
     * JavaScript scanner injected into pages to find media sources from DOM elements.
     */
    const val JS_MEDIA_SCANNER = """
        (function() {
            try {
                var found = [];
                var addMedia = function(url, kind, label) {
                    if (!url || url.indexOf('http') !== 0) return;
                    if (found.some(function(x) { return x.url === url; })) return;
                    found.push({ url: url, kind: kind, label: label || '' });
                };

                // Scan <video> and <audio> tags
                var vids = document.querySelectorAll('video, audio');
                for (var i = 0; i < vids.length; i++) {
                    var el = vids[i];
                    var isAudio = el.tagName.toLowerCase() === 'audio';
                    var kind = isAudio ? 'AUDIO' : 'VIDEO';
                    if (el.src) addMedia(el.src, kind, el.title || '');
                    if (el.currentSrc) addMedia(el.currentSrc, kind, el.title || '');

                    var sources = el.querySelectorAll('source');
                    for (var s = 0; s < sources.length; s++) {
                        var src = sources[s];
                        var res = src.getAttribute('res') || src.getAttribute('size') || src.getAttribute('label') || '';
                        addMedia(src.src, kind, res);
                    }
                    var tracks = el.querySelectorAll('track');
                    for (var t = 0; t < tracks.length; t++) {
                        addMedia(tracks[t].src, 'SUBTITLE', tracks[t].label || tracks[t].srclang || '');
                    }
                }

                // Scan meta tags (og:video, twitter:player:stream)
                var ogVids = document.querySelectorAll("meta[property='og:video'], meta[property='og:video:url'], meta[name='twitter:player:stream']");
                for (var m = 0; m < ogVids.length; m++) {
                    addMedia(ogVids[m].content, 'VIDEO', 'OpenGraph Video');
                }

                if (found.length > 0) {
                    return JSON.stringify(found);
                }
            } catch(e) {}
            return '[]';
        })();
    """

    fun onUrlObserved(tabId: String, pageUrl: String, rawUrl: String, mimeType: String? = null) {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return
        val scheme = uri.scheme?.lowercase() ?: return
        if (scheme != "http" && scheme != "https") return

        val path = uri.path?.lowercase() ?: ""
        val ext = path.substringAfterLast('.', "").substringBefore('?')

        val isVideoExt = ext in VIDEO_EXTENSIONS || mimeType?.startsWith("video/") == true
        val isAudioExt = ext in AUDIO_EXTENSIONS || mimeType?.startsWith("audio/") == true
        val isSubExt = ext in SUBTITLE_EXTENSIONS || mimeType?.contains("vtt") == true || mimeType?.contains("subrip") == true

        if (!isVideoExt && !isAudioExt && !isSubExt) return
        if (!MediaCaptureStore.markSeen(tabId, rawUrl)) return

        val kind = when {
            isSubExt -> MediaKind.SUBTITLE
            isAudioExt -> MediaKind.AUDIO
            else -> MediaKind.VIDEO
        }

        val format = when {
            ext.isNotBlank() -> ext.uppercase()
            mimeType != null -> mimeType.substringAfter('/').substringBefore(';').uppercase()
            else -> "MP4"
        }

        val height = when {
            rawUrl.contains("1080") -> 1080
            rawUrl.contains("720") -> 720
            rawUrl.contains("480") -> 480
            rawUrl.contains("360") -> 360
            rawUrl.contains("240") -> 240
            else -> null
        }

        val bandwidth: Long? = when (height) {
            1080 -> 4_500_000L
            720 -> 2_500_000L
            480 -> 1_200_000L
            360 -> 600_000L
            else -> if (kind == MediaKind.AUDIO) 256_000L else 2_000_000L
        }

        val media = DetectedMedia(
            id = UUID.randomUUID().toString(),
            url = rawUrl,
            kind = kind,
            format = format,
            mimeType = mimeType,
            label = height?.let { "${it}p" },
            height = height,
            bandwidthBitsPerSec = bandwidth,
            pageUrl = pageUrl
        )

        MediaCaptureStore.add(tabId, media)
    }

    fun parseJsScannerResults(tabId: String, pageUrl: String, jsonResults: String) {
        if (jsonResults.isBlank() || jsonResults == "[]") return
        runCatching {
            val arr = org.json.JSONArray(jsonResults)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val url = obj.getString("url")
                val kindStr = obj.optString("kind", "VIDEO")
                val label = obj.optString("label", "")
                val kind = when (kindStr) {
                    "AUDIO" -> MediaKind.AUDIO
                    "SUBTITLE" -> MediaKind.SUBTITLE
                    else -> MediaKind.VIDEO
                }
                if (!MediaCaptureStore.markSeen(tabId, url)) continue

                val ext = url.substringBefore('?').substringAfterLast('.', "mp4").uppercase()
                val height = when {
                    label.contains("1080") || url.contains("1080") -> 1080
                    label.contains("720") || url.contains("720") -> 720
                    label.contains("480") || url.contains("480") -> 480
                    label.contains("360") || url.contains("360") -> 360
                    else -> null
                }

                val item = DetectedMedia(
                    id = UUID.randomUUID().toString(),
                    url = url,
                    kind = kind,
                    format = ext,
                    label = if (label.isNotBlank()) label else height?.let { "${it}p" },
                    height = height,
                    pageUrl = pageUrl
                )
                MediaCaptureStore.add(tabId, item)
            }
        }
    }
}
