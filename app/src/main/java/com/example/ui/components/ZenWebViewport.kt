package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import com.example.data.OfflinePageEntity
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.BlockedTrackerEvent
import com.example.data.HomeScreenStyle
import com.example.data.HomeShortcutEntity
import com.example.data.TabEntity
import com.example.data.PasswordEntity
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.ActiveModalSheet
import com.example.ui.ZenBrowserUiState
import android.graphics.Canvas as AndroidCanvas
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt
private const val DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

class ZenBackgroundWebView(context: android.content.Context) : WebView(context) {

    var isBackgroundPlayAllowed: () -> Boolean = { true }

    override fun onWindowVisibilityChanged(visibility: Int) {
        if (visibility != android.view.View.VISIBLE && isBackgroundPlayAllowed()) {
            super.onWindowVisibilityChanged(android.view.View.VISIBLE)
        } else {
            super.onWindowVisibilityChanged(visibility)
        }
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        if (visibility != android.view.View.VISIBLE && isBackgroundPlayAllowed()) {
            super.onVisibilityChanged(changedView, android.view.View.VISIBLE)
        } else {
            super.onVisibilityChanged(changedView, visibility)
        }
    }

    override fun dispatchWindowVisibilityChanged(visibility: Int) {
        if (visibility != android.view.View.VISIBLE && isBackgroundPlayAllowed()) {
            super.dispatchWindowVisibilityChanged(android.view.View.VISIBLE)
        } else {
            super.dispatchWindowVisibilityChanged(visibility)
        }
    }

    override fun onPause() {
        if (!isBackgroundPlayAllowed()) {
            super.onPause()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        if (!hasWindowFocus && isBackgroundPlayAllowed()) {
            super.onWindowFocusChanged(true)
        } else {
            super.onWindowFocusChanged(hasWindowFocus)
        }
    }
}

object ZenWebViewManager {
    private val pool = mutableMapOf<String, WebView>()

    fun getOrCreate(context: android.content.Context, tabId: String, creator: (android.content.Context) -> WebView): Pair<WebView, Boolean> {
        val existing = pool[tabId]
        if (existing != null) {
            (existing.parent as? android.view.ViewGroup)?.removeView(existing)
            return Pair(existing, false)
        }
        val created = creator(context)
        pool[tabId] = created
        return Pair(created, true)
    }

    fun remove(tabId: String) {
        val wv = pool.remove(tabId)
        wv?.destroy()
    }

    fun get(tabId: String): WebView? = pool[tabId]

    fun getAllWebViews(): Map<String, WebView> = pool

    fun destroyAll() {
        pool.values.forEach { it.destroy() }
        pool.clear()
    }
}

class ZenWebViewController {
    var webView: WebView? = null
    var onGoBackFallback: (() -> Unit)? = null
    var onGoForwardFallback: (() -> Unit)? = null
    var onGoBackTriggered: (() -> Unit)? = null
    var onGetBackHistoryFallback: (() -> List<com.example.data.HistoryNavEntry>)? = null
    var onNavigateHistoryOffsetFallback: ((Int) -> Unit)? = null
    var lastHistoryNavTime: Long = 0L

    fun canGoBack(): Boolean {
        return (webView?.canGoBack() == true) || onGoBackFallback != null
    }

    fun canGoForward(): Boolean {
        return (webView?.canGoForward() == true) || onGoForwardFallback != null
    }

    fun goBack() {
        lastHistoryNavTime = System.currentTimeMillis()
        onGoBackTriggered?.invoke()
        val wv = webView
        if (wv != null && wv.canGoBack()) {
            val list = wv.copyBackForwardList()
            val curIdx = list.currentIndex
            if (curIdx > 0) {
                val curItem = list.getItemAtIndex(curIdx)
                val prevItem = list.getItemAtIndex(curIdx - 1)
                if (curIdx >= 2 && prevItem != null && curItem != null &&
                    (prevItem.url == curItem.url || prevItem.url.trimEnd('/') == curItem.url.trimEnd('/'))
                ) {
                    wv.goBackOrForward(-2)
                    return
                }
            }
            wv.goBack()
        } else {
            onGoBackFallback?.invoke()
        }
    }

    fun goForward() {
        lastHistoryNavTime = System.currentTimeMillis()
        val wv = webView
        if (wv != null && wv.canGoForward()) {
            wv.goForward()
        } else {
            onGoForwardFallback?.invoke()
        }
    }

    fun getBackHistory(): List<com.example.data.HistoryNavEntry> {
        val wv = webView
        val list = wv?.copyBackForwardList()
        if (list != null && list.size > 0) {
            val currentIndex = list.currentIndex
            val result = mutableListOf<com.example.data.HistoryNavEntry>()
            for (i in (currentIndex - 1) downTo 0) {
                val item = list.getItemAtIndex(i) ?: continue
                val steps = currentIndex - i
                val rawTitle = item.title?.trim()
                val rawUrl = item.url ?: ""
                val title = if (!rawTitle.isNullOrBlank()) rawTitle else ZenPrivacyEngine.extractHostDisplay(rawUrl)
                result.add(com.example.data.HistoryNavEntry(title = title, url = rawUrl, stepsBack = steps))
            }
            if (result.isNotEmpty()) return result
        }
        return onGetBackHistoryFallback?.invoke() ?: emptyList()
    }

    fun navigateToHistoryOffset(offset: Int) {
        lastHistoryNavTime = System.currentTimeMillis()
        onGoBackTriggered?.invoke()
        val wv = webView
        if (wv != null && wv.canGoBackOrForward(offset)) {
            wv.goBackOrForward(offset)
        } else if (offset == -1) {
            goBack()
        } else {
            onNavigateHistoryOffsetFallback?.invoke(offset)
        }
    }

    fun reload() {
        webView?.reload()
    }

    fun launchTranslatorPanel() {
        webView?.evaluateJavascript(
            """
            (function() {
                var oldFab = document.getElementById('__zen_translator_fab');
                if (oldFab) { oldFab.remove(); }
                if (typeof window.__zenOpenTranslator === 'function') {
                    window.__zenOpenTranslator();
                } else {
                    window.location.href = 'https://translate.google.com/translate?sl=auto&tl=en&u=' + encodeURIComponent(window.location.href);
                }
            })();
            """.trimIndent(),
            null
        )
    }

    fun launchUblockElementPicker() {
        webView?.evaluateJavascript(
            """
            (function() {
                if (typeof window.__zenStartUblockElementPicker === 'function') {
                    window.__zenStartUblockElementPicker();
                }
            })();
            """.trimIndent(),
            null
        )
    }

    fun executeExtensionAction(extension: com.example.data.FirefoxExtensionEntity) {
        when (extension.id) {
            "ff_zen_translator" -> launchTranslatorPanel()
            "ff_ublock_origin" -> launchUblockElementPicker()
            else -> if (extension.jsPayload.isNotBlank()) {
                webView?.evaluateJavascript(
                    "(function(){ try { ${extension.jsPayload} } catch(e){} })();",
                    null
                )
            }
        }
    }

    fun captureBackdropBitmap(): Bitmap? {
        val wv = webView ?: return null
        val w = wv.width
        val h = wv.height
        if (w <= 0 || h <= 0) return null
        return runCatching {
            val scale = 0.25f
            val bmpW = (w * scale).roundToInt().coerceAtLeast(1)
            val bmpH = (h * scale).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
            val canvas = AndroidCanvas(bitmap)
            canvas.scale(scale, scale)
            canvas.translate(-wv.scrollX.toFloat(), -wv.scrollY.toFloat())
            wv.draw(canvas)
            bitmap
        }.getOrNull()
    }

    var isMuted = false

    /** Explicit state commands are used by Android's system media controls. */
    fun setMediaPlaying(playing: Boolean) {
        webView?.evaluateJavascript(
            """
            (function() {
                window.__zenBypassPauseBlock = true;
                function controlDoc(doc) {
                    var ytp = doc.getElementById('movie_player') || doc.querySelector('.html5-video-player');
                    var ytpHandled = false;
                    if (ytp) {
                        try {
                            if (${playing} && typeof ytp.playVideo === 'function') {
                                ytp.playVideo();
                                ytpHandled = true;
                            } else if (!${playing} && typeof ytp.pauseVideo === 'function') {
                                ytp.pauseVideo();
                                ytpHandled = true;
                            }
                        } catch(e) {}
                    }

                    var els = Array.prototype.slice.call(doc.querySelectorAll('video, audio'));
                    els.forEach(function(m) {
                        if (${playing}) {
                            if (m.paused || m.ended) {
                                try { if (m.ended) m.currentTime = 0; } catch(e) {}
                                var p = m.play();
                                if (p && p.catch) p.catch(function() {});
                            }
                        } else if (!m.paused) {
                            m.pause();
                        }
                    });

                    if (!ytpHandled) {
                        if (${playing}) {
                            var anyStillPaused = els.length === 0 || els.some(function(m) { return m.paused; });
                            if (anyStillPaused) {
                                var playBtn = doc.querySelector('.ytp-play-button, .ytp-large-play-button, button[aria-label*="Play" i], button[title*="Play" i], [aria-label*="Play" i]');
                                if (playBtn) {
                                    var lbl = (playBtn.getAttribute('aria-label') || playBtn.title || '').toLowerCase();
                                    if (lbl.indexOf('pause') === -1) {
                                        playBtn.click();
                                    }
                                }
                            }
                        } else {
                            var anyStillPlaying = els.length === 0 || els.some(function(m) { return !m.paused; });
                            if (anyStillPlaying) {
                                var pauseBtn = doc.querySelector('.ytp-play-button[aria-label*="Pause" i], button[aria-label*="Pause" i], button[title*="Pause" i]');
                                if (pauseBtn) pauseBtn.click();
                            }
                        }
                    }

                    try {
                        var frames = doc.querySelectorAll('iframe');
                        for (var i = 0; i < frames.length; i++) {
                            try {
                                var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                                if (frameDoc) controlDoc(frameDoc);
                            } catch(e) {}
                        }
                    } catch(e) {}
                }
                controlDoc(document);
                setTimeout(function() { window.__zenBypassPauseBlock = false; }, 400);
            })();
            """.trimIndent(),
            null
        )
    }

    fun toggleMediaPlayPause() {
        webView?.evaluateJavascript(
            """
            (function() {
                var playing = false;
                function checkPlaying(doc) {
                    var ytp = doc.getElementById('movie_player') || doc.querySelector('.html5-video-player');
                    if (ytp && typeof ytp.getPlayerState === 'function') {
                        if (ytp.getPlayerState() === 1) playing = true;
                    }
                    var els = Array.prototype.slice.call(doc.querySelectorAll('video, audio'));
                    if (els.some(function(m) { return !m.paused && !m.ended; })) playing = true;
                    try {
                        var frames = doc.querySelectorAll('iframe');
                        for (var i = 0; i < frames.length; i++) {
                            try {
                                var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                                if (frameDoc) checkPlaying(frameDoc);
                            } catch(e) {}
                        }
                    } catch(e) {}
                }
                checkPlaying(document);

                window.__zenBypassPauseBlock = true;
                function applyToggle(doc, isPlaying) {
                    var ytp = doc.getElementById('movie_player') || doc.querySelector('.html5-video-player');
                    var ytpHandled = false;
                    if (ytp) {
                        try {
                            if (isPlaying && typeof ytp.pauseVideo === 'function') {
                                ytp.pauseVideo();
                                ytpHandled = true;
                            } else if (!isPlaying && typeof ytp.playVideo === 'function') {
                                ytp.playVideo();
                                ytpHandled = true;
                            }
                        } catch(e) {}
                    }

                    var els = Array.prototype.slice.call(doc.querySelectorAll('video, audio'));
                    els.forEach(function(m) {
                        if (isPlaying) {
                            if (!m.paused && !m.ended) m.pause();
                        } else if (m.paused || m.ended) {
                            try { if (m.ended) m.currentTime = 0; } catch(e) {}
                            var p = m.play();
                            if (p && p.catch) p.catch(function() {});
                        }
                    });

                    if (!ytpHandled) {
                        var selector = isPlaying
                            ? '.ytp-play-button[aria-label*="Pause" i], button[aria-label*="Pause" i], button[title*="Pause" i]'
                            : '.ytp-play-button, .ytp-large-play-button, button[aria-label*="Play" i], button[title*="Play" i]';
                        var btn = doc.querySelector(selector);
                        if (btn) btn.click();
                    }

                    try {
                        var frames = doc.querySelectorAll('iframe');
                        for (var i = 0; i < frames.length; i++) {
                            try {
                                var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                                if (frameDoc) applyToggle(frameDoc, isPlaying);
                            } catch(e) {}
                        }
                    } catch(e) {}
                }
                applyToggle(document, playing);
                setTimeout(function() { window.__zenBypassPauseBlock = false; }, 400);
            })();
            """.trimIndent(),
            null
        )
    }

    /** Seeks the page's main media element to an absolute position in milliseconds. */
    fun seekMediaTo(positionMs: Long) = runSeek("t = $positionMs / 1000;")

    /** Seeks the page's main media element by a relative offset in milliseconds. */
    fun seekMediaBy(deltaMs: Long) = runSeek("t = (m.currentTime || 0) + ($deltaMs / 1000);")

    private fun runSeek(computeTarget: String) {
        webView?.evaluateJavascript(
            """
            (function() {
                function collect(doc, out) {
                    var ytp = doc.getElementById('movie_player') || doc.querySelector('.html5-video-player');
                    if (ytp && !out.__ytp) out.__ytp = ytp;
                    doc.querySelectorAll('video, audio').forEach(function(m) { out.push(m); });
                    try {
                        var frames = doc.querySelectorAll('iframe');
                        for (var i = 0; i < frames.length; i++) {
                            try {
                                var fd = frames[i].contentDocument || frames[i].contentWindow.document;
                                if (fd) collect(fd, out);
                            } catch(e) {}
                        }
                    } catch(e) {}
                }
                var all = [];
                collect(document, all);
                var seekable = all.filter(function(m) { return isFinite(m.duration) && m.duration > 0; });
                var m = seekable.filter(function(x) { return !x.paused; })[0] || seekable[0];
                if (!m && !all.__ytp) return;
                var dur = (m && isFinite(m.duration) && m.duration > 0) ? m.duration : (all.__ytp && typeof all.__ytp.getDuration === 'function' ? all.__ytp.getDuration() : 3600);
                var t = 0;
                if (!m) m = { currentTime: (all.__ytp && typeof all.__ytp.getCurrentTime === 'function' ? all.__ytp.getCurrentTime() : 0), duration: dur };
                $computeTarget
                t = Math.max(0, Math.min(t, dur - 0.25));
                window.__zenLastSeekTime = Date.now();
                window.__zenSeekTargetSec = t;
                if (all.__ytp && typeof all.__ytp.seekTo === 'function') {
                    try { all.__ytp.seekTo(t, true); } catch(e) {}
                } else if (m && 'currentTime' in m) {
                    try { m.currentTime = t; } catch(e) {}
                }
            })();
            """.trimIndent(),
            null
        )
    }

    /** Mutes/unmutes every media element and Web Audio context on the page (never clicks site buttons). */
    fun setMediaMuted(muted: Boolean) {
        isMuted = muted
        webView?.evaluateJavascript(
            """
            (function() {
                window.__zenIsMuted = $muted;
                if (typeof window.__zenApplyMute === 'function') {
                    try { window.__zenApplyMute($muted); } catch(e) {}
                }
                function applyMuteToDoc(doc) {
                    try {
                        var ytp = (doc.getElementById && doc.getElementById('movie_player')) || doc.querySelector('.html5-video-player');
                        if (ytp) {
                            if ($muted && typeof ytp.mute === 'function') ytp.mute();
                            else if (!$muted && typeof ytp.unMute === 'function') ytp.unMute();
                        }
                    } catch(e) {}
                    doc.querySelectorAll('video, audio').forEach(function(m) {
                        try {
                            m.muted = $muted;
                            m.defaultMuted = $muted;
                            if ($muted) {
                                if (m.volume > 0) m.__zenSavedVol = m.volume;
                                m.volume = 0;
                            } else {
                                m.volume = (m.__zenSavedVol !== undefined && m.__zenSavedVol > 0) ? m.__zenSavedVol : 1;
                            }
                        } catch(e) {}
                    });
                    try {
                        var frames = doc.querySelectorAll('iframe');
                        for (var i = 0; i < frames.length; i++) {
                            try {
                                var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                                if (frameDoc) applyMuteToDoc(frameDoc);
                            } catch(e) {}
                        }
                    } catch(e) {}
                }
                applyMuteToDoc(document);
            })();
            """.trimIndent(),
            null
        )
    }

    /** Re-applies the remembered mute state after navigation (the page JS flag is lost on load). */
    fun reapplyMuteState() {
        if (isMuted) setMediaMuted(true)
    }

    /** Stretches the playing video over the viewport while in the tiny PiP window. */
    fun setPipVideoMode(enabled: Boolean) {
        webView?.evaluateJavascript(
            """
            (function() {
                var id = '__zen_pip_style';
                var old = document.getElementById(id);
                if (old) old.remove();
                document.querySelectorAll('[data-zen-pip]').forEach(function(e) { e.removeAttribute('data-zen-pip'); });
                if (!$enabled) return;

                function findAndPip(doc) {
                    var vids = Array.prototype.slice.call(doc.querySelectorAll('video'));
                    var v = vids.filter(function(x) { return !x.paused; })[0] || vids.sort(function(a, b) { return b.clientWidth * b.clientHeight - a.clientWidth * a.clientHeight; })[0];
                    if (v) {
                        v.setAttribute('data-zen-pip', '1');
                        return true;
                    }
                    try {
                        var frames = doc.querySelectorAll('iframe');
                        for (var i = 0; i < frames.length; i++) {
                            try {
                                var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                                if (frameDoc && findAndPip(frameDoc)) return true;
                            } catch(e) {}
                        }
                    } catch(e) {}
                    return false;
                }
                findAndPip(document);

                var st = document.createElement('style');
                st.id = id;
                st.textContent = 'html,body{background:#000!important;overflow:hidden!important}' +
                    'video[data-zen-pip],.html5-main-video[data-zen-pip]{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;' +
                    'max-width:none!important;max-height:none!important;object-fit:contain!important;z-index:2147483647!important;background:#000!important}' +
                    '.html5-video-player{position:fixed!important;inset:0!important;width:100vw!important;height:100vh!important;z-index:2147483646!important;max-width:none!important;max-height:none!important}';
                document.documentElement.appendChild(st);
            })();
            """.trimIndent(),
            null
        )
    }

    fun stopAllMedia() {
        isMuted = false
        webView?.evaluateJavascript(
            """
            (function() {
                window.__zenBypassPauseBlock = true;
                window.__zenApplyMute && window.__zenApplyMute(false);
                var mediaElements = document.querySelectorAll('video, audio');
                mediaElements.forEach(function(media) {
                    media.muted = false;
                    media.pause();
                    media.currentTime = 0;
                    try {
                        if (media.srcObject) {
                            var tracks = media.srcObject.getTracks();
                            tracks.forEach(function(t) { t.stop(); });
                        }
                    } catch(e) {}
                    media.src = '';
                    media.load();
                });
                if (window.__zenAudioContexts) {
                    window.__zenAudioContexts.forEach(function(ctx) {
                        if (ctx.state === 'suspended') ctx.resume();
                    });
                }
                setTimeout(function() { window.__zenBypassPauseBlock = false; }, 300);
            })();
            """.trimIndent(),
            null
        )
    }

    fun findInPage(query: String) {
        val view = webView ?: return
        if (query.isBlank()) {
            view.clearMatches()
        } else {
            view.findAllAsync(query)
        }
    }

    fun clearWebData() {
        webView?.apply {
            clearCache(true)
            clearHistory()
            clearFormData()
        }
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    fun capturePageHtmlForOffline(onCaptured: (String) -> Unit) {
        val wv = webView
        if (wv != null) {
            wv.evaluateJavascript("(function() { return document.documentElement.outerHTML; })();") { raw ->
                val html = if (raw != null && raw != "null") {
                    try {
                        org.json.JSONTokener(raw).nextValue().toString()
                    } catch (e: Exception) {
                        raw.removeSurrounding("\"").replace("\\\"", "\"").replace("\\n", "\n")
                    }
                } else ""
                onCaptured(html)
            }
        } else {
            onCaptured("")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun ZenWebPaneView(
    tab: TabEntity,
    uiState: ZenBrowserUiState,
    controller: ZenWebViewController,
    isFocusedPane: Boolean,
    onFocusPane: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onReloadUnloadedTab: (TabEntity) -> Unit,
    onPageVisited: (url: String, title: String?) -> Unit,
    onProgressChanged: (progress: Int, canBack: Boolean, canForward: Boolean) -> Unit,
    onTrackerBlocked: (BlockedTrackerEvent) -> Unit,
    onMediaStateChanged: (isActive: Boolean, isPlaying: Boolean, title: String, domain: String, tabId: String) -> Unit,
    onInstallExtensionFromAmo: (url: String, title: String?) -> Unit = { _, _ -> },
    onAddHomeShortcut: (title: String, url: String) -> Unit,
    onUpdateHomeShortcut: (HomeShortcutEntity, String, String) -> Unit,
    onDeleteHomeShortcut: (String) -> Unit,
    onUpdateCustomTextPosition: (Float, Float) -> Unit,
    onOpenSheet: (ActiveModalSheet) -> Unit,
    onToggleSplitView: () -> Unit,
    onOpenInNewTab: (url: String, forceSwitch: Boolean?) -> Unit = { _, _ -> },
    onOpenInPrivateTab: (url: String) -> Unit = {},
    onOpenGlance: (url: String) -> Unit = {},
    onUpdateSwitchToNewTabImmediately: (Boolean) -> Unit = {},
    onPopTabHistory: (String, String?) -> String? = { _, _ -> null },
    onPopTabForward: (String, String?) -> String? = { _, _ -> null },
    onPopTabHistoryOffset: (String, Int, String?) -> String? = { _, _, _ -> null },
    onGetTabHistoryEntries: (String) -> List<com.example.data.HistoryNavEntry> = { emptyList() },
    onOpenSidebar: () -> Unit = {},
    onSavePassword: (PasswordEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (tab.url.startsWith("zen://search")) {
        DisposableEffect(tab.id, isFocusedPane) {
            if (isFocusedPane) {
                controller.webView = null
                controller.onGoBackFallback = {
                    val prevUrl = onPopTabHistory(tab.id, tab.url)
                    if (!prevUrl.isNullOrBlank() && prevUrl != tab.url) {
                        onNavigateUrl(prevUrl)
                    } else {
                        onNavigateUrl("zen://home")
                    }
                }
                onProgressChanged(100, true, false)
            }
            onDispose {
                if (controller.webView == null) {
                    controller.onGoBackFallback = null
                }
            }
        }
        val queryParam = runCatching {
            Uri.parse(tab.url).getQueryParameter("q")
        }.getOrNull() ?: "? : who is the main character"
        ZenGoogleSearchResultView(
            query = queryParam,
            customAiOverviewText = uiState.sidebarConfig.customAiOverviewText,
            customHeadline = uiState.sidebarConfig.fakeSearchHeadline,
            customSnippet = uiState.sidebarConfig.fakeSearchSnippet,
            onNavigateUrl = onNavigateUrl,
            onOpenInGlance = onOpenGlance,
            modifier = modifier
        )
        return
    }

    if (tab.url.startsWith("zen://offline")) {
        val pageId = runCatching { Uri.parse(tab.url).getQueryParameter("id") }.getOrNull() ?: ""
        val offlinePage = uiState.offlinePages.find { it.id == pageId }

        DisposableEffect(tab.id, isFocusedPane) {
            if (isFocusedPane) {
                controller.webView = null
                controller.onGoBackFallback = {
                    val prevUrl = onPopTabHistory(tab.id, tab.url)
                    if (!prevUrl.isNullOrBlank() && prevUrl != tab.url) {
                        onNavigateUrl(prevUrl)
                    } else {
                        onNavigateUrl("zen://home")
                    }
                }
                onProgressChanged(100, true, false)
            }
            onDispose {
                if (controller.webView == null) {
                    controller.onGoBackFallback = null
                }
            }
        }

        if (offlinePage != null) {
            ZenOfflinePageRenderer(
                page = offlinePage,
                onLoadLiveSite = { onNavigateUrl(offlinePage.url) },
                onNavigateUrl = onNavigateUrl,
                modifier = modifier
            )
        } else {
            Box(
                modifier = modifier.fillMaxSize().background(Color(0xFF101014)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        tint = Color(0xFF71717A),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Offline Page Not Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFF4F4F6)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "This saved page may have been deleted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onNavigateUrl("zen://home") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272730))
                    ) {
                        Text("Return Home", color = Color(0xFFF4F4F6))
                    }
                }
            }
        }
        return
    }

    if (tab.url == "zen://home" || tab.url.isBlank() || tab.isUnloaded) {
        DisposableEffect(tab.id, isFocusedPane) {
            if (isFocusedPane) {
                controller.webView = null
                controller.onGoBackFallback = null
                onProgressChanged(100, false, false)
            }
            onDispose {}
        }
        ZenHomeStartPage(
            uiState = uiState,
            isPrivateTab = tab.isPrivate || (uiState.activeWorkspace?.isPrivate == true),
            onFocusPane = onFocusPane,
            onNavigateUrl = onNavigateUrl,
            onAddHomeShortcut = onAddHomeShortcut,
            onUpdateHomeShortcut = onUpdateHomeShortcut,
            onDeleteHomeShortcut = onDeleteHomeShortcut,
            onUpdateCustomTextPosition = onUpdateCustomTextPosition,
            onOpenStartPageCustomizer = { onOpenSheet(ActiveModalSheet.START_PAGE_CUSTOMIZER) },
            onOpenSidebar = onOpenSidebar,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val privacyCfg = uiState.privacyConfig
    val readerMode = uiState.isReaderModeEnabled
    val customCss = remember(uiState.firefoxExtensions, uiState.zenMods, tab.url, tab.isPrivate) {
        uiState.getInjectedCssForTab(tab)
    }
    val customJs = remember(uiState.firefoxExtensions, tab.url, tab.isPrivate) {
        uiState.getInjectedJsForTab(tab)
    }

    val currentUiState by rememberUpdatedState(uiState)
    // Pooled WebViews can outlive the composition that created their touch listener.
    val currentOnOpenSidebar by rememberUpdatedState(onOpenSidebar)
    val currentPrivacyCfg by rememberUpdatedState(privacyCfg)
    val currentCustomCss by rememberUpdatedState(customCss)
    val currentCustomJs by rememberUpdatedState(customJs)
    val currentReaderMode by rememberUpdatedState(readerMode)
    val isDesktopMode = uiState.desktopModeTabIds.contains(tab.id)
    val defaultMobileUa = remember(tab.id) {
        val rawUa = WebSettings.getDefaultUserAgent(context) ?: "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        rawUa.replace("; wv)", ")").replace("Version/4.0 ", "")
    }



    // Pull-to-refresh state when user is at the top-most position of a web page and pulls down
    var pullDownDistancePx by remember(tab.id) { mutableStateOf(0f) }
    var isPullingGestureActive by remember(tab.id) { mutableStateOf(false) }
    var isPullRefreshing by remember(tab.id) { mutableStateOf(false) }
    val pullRefreshThresholdPx = with(density) { 82.dp.toPx() }

    LaunchedEffect(uiState.pageLoadingProgress) {
        // Only dismiss when page finishes loading AND the user is not currently holding their finger down
        if (uiState.pageLoadingProgress >= 100 && isPullRefreshing && !isPullingGestureActive) {
            kotlinx.coroutines.delay(250)
            if (!isPullingGestureActive) {
                isPullRefreshing = false
                pullDownDistancePx = 0f
            }
        }
    }

    // Track HTML5 Video Fullscreen custom view & callback
    var fullscreenCustomView by remember(tab.id) { mutableStateOf<android.view.View?>(null) }
    var fullscreenCustomViewCallback by remember(tab.id) {
        mutableStateOf<WebChromeClient.CustomViewCallback?>(null)
    }

    // Track URLs and navigation states so asynchronous Room updates, link clicks, redirects, and back/forward never trigger a reload loop
    val knownInternalUrls = remember(tab.id) { mutableSetOf(tab.url) }
    var lastProcessedTabUrl by remember(tab.id) { mutableStateOf(tab.url) }
    var lastVisitedInternalUrl by remember(tab.id) { mutableStateOf<String?>(null) }
    // Link long-press context menu state (Open in New Tab, Open in Private Tab, Copy Link Address, etc.)
    var longPressedLinkUrl by remember(tab.id) { mutableStateOf<String?>(null) }
    var detectedLanguageCode by remember(tab.id, tab.url) { mutableStateOf<String?>(null) }
    var showTranslateBanner by remember(tab.id, tab.url) { mutableStateOf(false) }
    var detectedSavePassword by remember(tab.id, tab.url) { mutableStateOf<com.example.data.PasswordEntity?>(null) }
    var detectedAutofillPassword by remember(tab.id, tab.url) { mutableStateOf<com.example.data.PasswordEntity?>(null) }
    var showAutofillPrompt by remember(tab.id, tab.url) { mutableStateOf(false) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    val scrollPositionsByUrl = remember(tab.id) { mutableMapOf<String, Int>() }
    val scrollPrefs = remember {
        context.getSharedPreferences("zen_scroll_prefs", android.content.Context.MODE_PRIVATE)
    }
    var isUserInteractingWithScroll by remember(tab.id) { mutableStateOf(false) }

    val (webView, isNewlyCreated) = remember(tab.id) {
        ZenWebViewManager.getOrCreate(context, tab.id) { ctx ->
            var touchStartY = 0f
            var touchStartX = 0f
            var pullEngageY = 0f
            var startedAtTopOfPage = false
            var isPullingDownToRefresh = false
            var hasTriggeredHorizontalSwipe = false

            ZenBackgroundWebView(ctx).apply {
                // Without this Android demotes the renderer when the app is backgrounded/screen-off,
                // which silences or kills page audio after a while.
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false)
                }
                isBackgroundPlayAllowed = { currentUiState.sidebarConfig.backgroundPlayEnabled }
                isLongClickable = true
                setOnLongClickListener { wv ->
                    val hitResult = (wv as? WebView)?.hitTestResult
                    if (hitResult != null) {
                        val linkUrl = when (hitResult.type) {
                            WebView.HitTestResult.SRC_ANCHOR_TYPE,
                            WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> hitResult.extra
                            else -> null
                        }
                        if (!linkUrl.isNullOrBlank()) {
                            longPressedLinkUrl = linkUrl
                            return@setOnLongClickListener true
                        }
                    }
                    false
                }
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                isFocusable = true
                isFocusableInTouchMode = true
                isScrollbarFadingEnabled = true
                overScrollMode = android.view.View.OVER_SCROLL_ALWAYS
                var pendingScrollKey = ""
                var pendingScrollY = 0
                val persistScrollRunnable = Runnable {
                    if (pendingScrollKey.isNotEmpty()) scrollPrefs.edit().putInt(pendingScrollKey, pendingScrollY).apply()
                }
                setOnScrollChangeListener { _, _, _, scrollY, _ ->
                    val url = this.url ?: tab.url
                    if (url.isNotBlank() && scrollY > 0) {
                        scrollPositionsByUrl[url] = scrollY
                        scrollPositionsByUrl[url.substringBefore('#').trimEnd('/')] = scrollY
                        // Debounced: writing SharedPreferences on every scroll frame is wasteful.
                        removeCallbacks(persistScrollRunnable)
                        pendingScrollKey = "scroll_" + url.hashCode()
                        pendingScrollY = scrollY
                        postDelayed(persistScrollRunnable, 400)
                    }
                }
                setOnTouchListener { v, event ->
                    val wv = v as? WebView ?: return@setOnTouchListener false
                    if (currentUiState.isInPictureInPictureMode) {
                        return@setOnTouchListener false
                    }
                    if (event.actionMasked == android.view.MotionEvent.ACTION_CANCEL) {
                        // Android takes over this gesture for home/back/PiP. Abort it completely:
                        // CANCEL must never finish an edge swipe or trigger pull-to-refresh.
                        wv.postDelayed({ isUserInteractingWithScroll = false }, 350)
                        isPullingDownToRefresh = false
                        isPullingGestureActive = false
                        startedAtTopOfPage = false
                        hasTriggeredHorizontalSwipe = false
                        touchStartX = 0f
                        touchStartY = 0f
                        pullEngageY = 0f
                        if (!isPullRefreshing) pullDownDistancePx = 0f
                        return@setOnTouchListener false
                    }
                    val densityMetrics = ctx.resources.displayMetrics
                    val edgeThresholdPx = 28f * densityMetrics.density
                    val screenWidthPx = densityMetrics.widthPixels.toFloat()

                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            isUserInteractingWithScroll = true
                            touchStartY = event.rawY
                            touchStartX = event.rawX
                            pullEngageY = event.rawY
                            startedAtTopOfPage = (wv.scrollY <= 8 && !wv.canScrollVertically(-1))
                            isPullingDownToRefresh = false
                            isPullingGestureActive = false
                            hasTriggeredHorizontalSwipe = false
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            isUserInteractingWithScroll = true
                            if (isPullingDownToRefresh) {
                                isPullingGestureActive = true
                                val rawPull = (event.rawY - pullEngageY).coerceAtLeast(10f)
                                pullDownDistancePx = (rawPull * 0.58f).coerceIn(10f, pullRefreshThresholdPx * 1.65f)
                                return@setOnTouchListener true
                            } else {
                                val isAtTopNow = (wv.scrollY <= 8 && !wv.canScrollVertically(-1))
                                if (!startedAtTopOfPage && isAtTopNow) {
                                    startedAtTopOfPage = true
                                    touchStartY = event.rawY
                                    touchStartX = event.rawX
                                } else if (startedAtTopOfPage && !isAtTopNow) {
                                    startedAtTopOfPage = false
                                }

                                if (startedAtTopOfPage && isAtTopNow) {
                                    val dy = event.rawY - touchStartY
                                    val dx = kotlin.math.abs(event.rawX - touchStartX)
                                    if (dy > 16f && dy > dx * 1.25f) {
                                        isPullingDownToRefresh = true
                                        isPullingGestureActive = true
                                        pullEngageY = event.rawY - 16f
                                        val rawPull = (event.rawY - pullEngageY).coerceAtLeast(10f)
                                        pullDownDistancePx = (rawPull * 0.58f).coerceIn(10f, pullRefreshThresholdPx * 1.65f)
                                        return@setOnTouchListener true
                                    }
                                }
                            }
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            wv.postDelayed({ isUserInteractingWithScroll = false }, 350)
                            val wasPulling = isPullingDownToRefresh
                            isPullingDownToRefresh = false
                            isPullingGestureActive = false
                            startedAtTopOfPage = false
                            if (wasPulling) {
                                if (pullDownDistancePx >= pullRefreshThresholdPx) {
                                    isPullRefreshing = true
                                    pullDownDistancePx = pullRefreshThresholdPx * 0.78f
                                    wv.reload()
                                } else {
                                    isPullRefreshing = false
                                    pullDownDistancePx = 0f
                                }
                                return@setOnTouchListener true
                            } else {
                                val finalDx = event.rawX - touchStartX
                                val finalDy = event.rawY - touchStartY
                                val absFinalDx = kotlin.math.abs(finalDx)
                                val absFinalDy = kotlin.math.abs(finalDy)
                                val startedFromLeftEdge = touchStartX <= edgeThresholdPx
                                val startedFromRightEdge = touchStartX >= (screenWidthPx - edgeThresholdPx)

                                // ACTION_CANCEL means the system took the gesture (home / PiP / back swipe).
                                // Treating it as a finished edge swipe opened the side panel while entering PiP.
                                val wasCancelled = event.actionMasked == android.view.MotionEvent.ACTION_CANCEL
                                if (!wasCancelled && (startedFromLeftEdge || startedFromRightEdge) && absFinalDx > 60f && absFinalDx > absFinalDy * 1.35f) {
                                    hasTriggeredHorizontalSwipe = true
                                    val swipeMode = currentUiState.sidebarConfig.horizontalSwipeMode
                                    if (swipeMode == com.example.data.HorizontalSwipeNavigationMode.SWIPE_LEFT_PANEL_RIGHT_BACK) {
                                        if (finalDx < -60f && startedFromRightEdge) {
                                            wv.post { currentOnOpenSidebar() }
                                            return@setOnTouchListener true
                                        } else if (finalDx > 60f && startedFromLeftEdge) {
                                            wv.post { controller.goBack() }
                                            return@setOnTouchListener true
                                        }
                                    } else {
                                        if (finalDx < -60f && startedFromRightEdge) {
                                            wv.post { controller.goBack() }
                                            return@setOnTouchListener true
                                        } else if (finalDx > 60f && startedFromLeftEdge) {
                                            wv.post { currentOnOpenSidebar() }
                                            return@setOnTouchListener true
                                        }
                                    }
                                }
                            }
                            if (!isPullRefreshing) {
                                pullDownDistancePx = 0f
                            }
                            hasTriggeredHorizontalSwipe = false
                        }
                    }
                    false
                }
                setOnScrollChangeListener { v, _, scrollY, _, _ ->
                val currentWv = v as? WebView ?: return@setOnScrollChangeListener
                val currentUrl = currentWv.url ?: tab.url
                if (currentUrl.isNotBlank() && currentUrl != "about:blank" && currentUrl != "zen://home") {
                    if (scrollY > 0 || isUserInteractingWithScroll) {
                        scrollPositionsByUrl[currentUrl] = scrollY
                    }
                }
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = false
                allowFileAccess = false
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                setSupportMultipleWindows(false)
                cacheMode = WebSettings.LOAD_DEFAULT
                userAgentString = if (isDesktopMode) DESKTOP_USER_AGENT else defaultMobileUa
            }
            CookieManager.getInstance().setAcceptCookie(true)
            val blockCookies = privacyCfg.blockThirdPartyCookies || privacyCfg.adBlockerLevel == com.example.data.AdBlockerLevel.HIGH
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !blockCookies)

            val webViewSelf = this
            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onMediaUpdate(hasMedia: Boolean, isPlaying: Boolean, title: String, host: String) {
                        post {
                            onMediaStateChanged(
                                hasMedia,
                                isPlaying,
                                title.ifBlank { tab.title },
                                host.ifBlank { ZenPrivacyEngine.extractHostDisplay(tab.url) },
                                tab.id
                            )
                        }
                    }

                    @JavascriptInterface
                    fun onMediaProgress(positionMs: Long, durationMs: Long) {
                        // Only the tab that owns the media bar may drive the system seek bar.
                        val owner = currentUiState.mediaPlaybackState.tabId
                        if (owner.isNotBlank() && owner != tab.id) return
                        com.example.ZenMediaService.progress(positionMs, durationMs)
                    }

                    @JavascriptInterface
                    fun onInstallAmoExtension(amoUrl: String, addonTitle: String) {
                        val host = try { android.net.Uri.parse(amoUrl).host?.lowercase() } catch (e: Exception) { null }
                        val isAmo = host == "addons.mozilla.org" || host?.endsWith(".addons.mozilla.org") == true
                        if (!isAmo) return
                        post {
                            onInstallExtensionFromAmo(amoUrl, addonTitle)
                        }
                    }
                },
                "ZenMediaBridge"
            )

            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onSavePassword(domain: String, pageUrl: String, user: String, pass: String) {
                        post {
                            // Never trust the page-supplied domain: use the real host of the WebView's current URL.
                            val realHost = try { android.net.Uri.parse(webViewSelf.url ?: "").host?.lowercase() } catch (e: Exception) { null }
                            if (realHost.isNullOrBlank() || !domain.equals(realHost, ignoreCase = true)) return@post
                            if (domain.isNotBlank() && pass.isNotBlank()) {
                                detectedSavePassword = PasswordEntity(
                                    id = "pwd_${java.util.UUID.randomUUID().toString().take(8)}",
                                    domain = domain,
                                    url = pageUrl.ifBlank { tab.url },
                                    username = user,
                                    password = pass
                                )
                            }
                        }
                    }
                },
                "ZenAutofillBridge"
            )
        }
    }
}

    val currentTabUrl by rememberUpdatedState(tab.url)
    DisposableEffect(webView, isFocusedPane, tab.id) {
        if (isFocusedPane) {
            controller.webView = webView
            controller.onGoBackTriggered = {
                isUserInteractingWithScroll = false
                val wv = controller.webView
                val curUrl = wv?.url ?: currentTabUrl
                val curY = wv?.scrollY ?: 0
                if (curUrl.isNotBlank() && curY > 0) {
                    scrollPositionsByUrl[curUrl] = curY
                    scrollPositionsByUrl[curUrl.substringBefore('#').trimEnd('/')] = curY
                    scrollPrefs.edit().putInt("scroll_" + curUrl.hashCode(), curY).apply()
                }
            }
            controller.onGoBackFallback = {
                val wvUrl = controller.webView?.url
                val activeUrl = if (!wvUrl.isNullOrBlank()) wvUrl else currentTabUrl
                val prevUrl = onPopTabHistory(tab.id, activeUrl)
                if (!prevUrl.isNullOrBlank() && prevUrl != activeUrl) {
                    lastProcessedTabUrl = prevUrl
                    controller.webView?.loadUrl(prevUrl)
                    onNavigateUrl(prevUrl)
                }
            }
            controller.onGoForwardFallback = {
                val wvUrl = controller.webView?.url
                val activeUrl = if (!wvUrl.isNullOrBlank()) wvUrl else currentTabUrl
                val nextUrl = onPopTabForward(tab.id, activeUrl)
                if (!nextUrl.isNullOrBlank() && nextUrl != activeUrl) {
                    lastProcessedTabUrl = nextUrl
                    controller.webView?.loadUrl(nextUrl)
                    onNavigateUrl(nextUrl)
                }
            }
            controller.onNavigateHistoryOffsetFallback = { offset ->
                val wvUrl = controller.webView?.url
                val activeUrl = if (!wvUrl.isNullOrBlank()) wvUrl else currentTabUrl
                val targetUrl = onPopTabHistoryOffset(tab.id, offset, activeUrl)
                if (!targetUrl.isNullOrBlank() && targetUrl != activeUrl) {
                    lastProcessedTabUrl = targetUrl
                    controller.webView?.loadUrl(targetUrl)
                    onNavigateUrl(targetUrl)
                }
            }
            controller.onGetBackHistoryFallback = {
                onGetTabHistoryEntries(tab.id)
            }
        }
        onDispose {
            if (controller.webView === webView) {
                controller.webView = null
                controller.onGoBackFallback = null
                controller.onGoForwardFallback = null
                controller.onGoBackTriggered = null
                controller.onNavigateHistoryOffsetFallback = null
                controller.onGetBackHistoryFallback = null
            }
        }
    }

    val restoreScrollForUrl: (WebView?, String?) -> Unit = { wv, targetUrl ->
        val url = targetUrl?.takeIf { it.isNotBlank() } ?: wv?.url ?: currentTabUrl
        val inMemY = scrollPositionsByUrl[url] ?: scrollPositionsByUrl[url.substringBefore('#').trimEnd('/')]
        val savedY = scrollPrefs.getInt("scroll_" + url.hashCode(), 0)
        val targetY = if (inMemY != null && inMemY > 0) inMemY else savedY
        if (targetY > 0 && wv != null) {
            wv.post {
                if (!isUserInteractingWithScroll) {
                    wv.scrollTo(0, targetY)
                }
            }
            wv.postDelayed({
                if (!isUserInteractingWithScroll && wv.scrollY < (targetY * 0.7f).toInt()) {
                    wv.scrollTo(0, targetY)
                    wv.evaluateJavascript("window.scrollTo({top: $targetY, behavior: 'instant'});", null)
                }
            }, 300)
        }
    }

    LaunchedEffect(readerMode, customCss, customJs, privacyCfg.fingerprintProtection) {
        val script = ZenPrivacyEngine.buildFingerprintProtectionScript(
            fingerprintProtection = privacyCfg.fingerprintProtection,
            readerModeActive = readerMode,
            customCssPayload = customCss,
            customJsPayload = customJs
        )
        webView.evaluateJavascript(script, null)
    }

    LaunchedEffect(uiState.findInPageQuery, uiState.isFindInPageVisible) {
        if (isFocusedPane) {
            if (uiState.isFindInPageVisible && uiState.findInPageQuery.isNotBlank()) {
                webView.findAllAsync(uiState.findInPageQuery)
            } else {
                webView.clearMatches()
            }
        }
    }

    LaunchedEffect(isDesktopMode) {
        val targetUa = if (isDesktopMode) DESKTOP_USER_AGENT else defaultMobileUa
        if (webView.settings.userAgentString != targetUa) {
            webView.settings.userAgentString = targetUa
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            if (isDesktopMode) {
                val script = ZenPrivacyEngine.buildDesktopModeSpoofScript(true)
                if (script.isNotBlank()) {
                    webView.evaluateJavascript(script, null)
                }
            }
            webView.evaluateJavascript(ZenPrivacyEngine.buildDesktopWebExtensionShimScript(), null)
            if (tab.url.startsWith("http://") || tab.url.startsWith("https://")) {
                webView.reload()
            }
        }
    }

    val mediaAndAmoScript = remember {
        """
        (function() {
            var oldFab = document.getElementById('__zen_translator_fab');
            if (oldFab) { oldFab.remove(); }

            function enhanceDocVideos(doc) {
                doc.querySelectorAll('video, audio').forEach(function(v) {
                    if (!v.hasAttribute('playsinline')) {
                        v.setAttribute('playsinline', '');
                    }
                    if (!v.hasAttribute('webkit-playsinline')) {
                        v.setAttribute('webkit-playsinline', '');
                    }
                });
                try {
                    var frames = doc.querySelectorAll('iframe');
                    for (var i = 0; i < frames.length; i++) {
                        try {
                            var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                            if (frameDoc) enhanceDocVideos(frameDoc);
                        } catch(e) {}
                    }
                } catch(e) {}
            }
            function enhanceVideos() {
                enhanceDocVideos(document);
            }
            enhanceVideos();

            if (!window.__zenMediaHooked) {
                window.__zenMediaHooked = true;

                var userInteractionTime = Date.now();
                document.addEventListener('click', function() { userInteractionTime = Date.now(); }, true);
                document.addEventListener('touchstart', function() { userInteractionTime = Date.now(); }, true);
                document.addEventListener('keydown', function() { userInteractionTime = Date.now(); }, true);

                // Background Audio/Video playback: prevent web pages from pausing when backgrounded, switching tabs, or screen off
                try {
                    var origAddEventListener = EventTarget.prototype.addEventListener;
                    EventTarget.prototype.addEventListener = function(type, listener, options) {
                        if (type === 'visibilitychange' || type === 'webkitvisibilitychange' || type === 'pagehide' || type === 'blur' || type === 'focusout' || type === 'freeze') {
                            return;
                        }
                        return origAddEventListener.call(this, type, listener, options);
                    };
                } catch(e) {}

                try {
                    Object.defineProperty(document, 'hidden', { get: function() { return false; }, configurable: true });
                    Object.defineProperty(document, 'webkitHidden', { get: function() { return false; }, configurable: true });
                    Object.defineProperty(document, 'visibilityState', { get: function() { return 'visible'; }, configurable: true });
                    Object.defineProperty(document, 'webkitVisibilityState', { get: function() { return 'visible'; }, configurable: true });
                    window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                    document.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                    window.addEventListener('pagehide', function(e) { e.stopImmediatePropagation(); }, true);
                    window.addEventListener('blur', function(e) { e.stopImmediatePropagation(); }, true);
                } catch(e) {}

                try {
                    window.open = function() { return null; };
                    window.showModalDialog = function() { return null; };
                } catch(e) {}

                try {
                    var origPause = HTMLMediaElement.prototype.pause;
                    HTMLMediaElement.prototype.pause = function() {
                        if (window.__zenBypassPauseBlock) {
                            return origPause.apply(this, arguments);
                        }
                        var stack = new Error().stack || '';
                        var timeSinceInteraction = Date.now() - userInteractionTime;
                        if (timeSinceInteraction > 900 || stack.indexOf('visibility') !== -1 || stack.indexOf('blur') !== -1 || stack.indexOf('pagehide') !== -1 || document.hidden) {
                            return;
                        }
                        return origPause.apply(this, arguments);
                    };
                } catch(e) {}

                // ---- Mute machinery (installed at page load so late-created media/AudioContexts are covered) ----
                window.__zenIsMuted = false;
                window.__zenAudioContexts = window.__zenAudioContexts || [];
                try {
                    ['AudioContext', 'webkitAudioContext'].forEach(function(name) {
                        var Orig = window[name];
                        if (!Orig || Orig.__zenWrapped) return;
                        var Wrapped = function() {
                            var ctx = new (Function.prototype.bind.apply(Orig, [null].concat(Array.prototype.slice.call(arguments))))();
                            window.__zenAudioContexts.push(ctx);
                            if (window.__zenIsMuted) { try { ctx.suspend(); } catch(e) {} }
                            return ctx;
                        };
                        Wrapped.prototype = Orig.prototype;
                        Wrapped.__zenWrapped = true;
                        window[name] = Wrapped;
                    });
                } catch(e) {}
                function applyMuteToMedia(root) {
                    try {
                        var ytp = (root.getElementById && root.getElementById('movie_player')) || root.querySelector('.html5-video-player');
                        if (ytp) {
                            if (window.__zenIsMuted && typeof ytp.mute === 'function') ytp.mute();
                            else if (!window.__zenIsMuted && typeof ytp.unMute === 'function') ytp.unMute();
                        }
                    } catch(e) {}
                    root.querySelectorAll('video, audio').forEach(function(m) {
                        try {
                            if (m.muted !== window.__zenIsMuted) m.muted = window.__zenIsMuted;
                            m.defaultMuted = window.__zenIsMuted;
                            if (window.__zenIsMuted) {
                                if (m.volume > 0) m.__zenSavedVol = m.volume;
                                m.volume = 0;
                            } else if (m.__zenSavedVol !== undefined) {
                                m.volume = m.__zenSavedVol > 0 ? m.__zenSavedVol : 1;
                            }
                        } catch(e) {}
                    });
                    try {
                        var frames = root.querySelectorAll('iframe');
                        for (var i = 0; i < frames.length; i++) {
                            try {
                                var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                                if (frameDoc) applyMuteToMedia(frameDoc);
                            } catch(e) {}
                        }
                    } catch(e) {}
                }
                window.__zenApplyMute = function(muted) {
                    window.__zenIsMuted = !!muted;
                    applyMuteToMedia(document);
                    if (window.__zenAudioContexts) {
                        window.__zenAudioContexts.forEach(function(ctx) {
                            try { muted ? ctx.suspend() : ctx.resume(); } catch(e) {}
                        });
                    }
                };
                ['play', 'playing', 'volumechange', 'loadedmetadata'].forEach(function(ev) {
                    document.addEventListener(ev, function(e) {
                        var t = e.target;
                        if (t && (t.tagName === 'VIDEO' || t.tagName === 'AUDIO') && window.__zenIsMuted) {
                            try {
                                t.muted = true;
                                t.defaultMuted = true;
                                t.volume = 0;
                            } catch(e) {}
                        }
                    }, true);
                });

                function checkAndUpdateMediaState() {
                    var isPlaying = false;
                    var hasMedia = false;
                    if (window.__zenIsMuted) applyMuteToMedia(document);

                    var progressMedia = null;
                    function checkDoc(doc) {
                        var allMedia = doc.querySelectorAll('video, audio');
                        if (allMedia.length > 0) hasMedia = true;
                        allMedia.forEach(function(m) {
                            if (isFinite(m.duration) && m.duration > 0 && (!progressMedia || (progressMedia.paused && !m.paused))) {
                                progressMedia = m;
                            }
                            if (!m.hasAttribute('playsinline')) m.setAttribute('playsinline', '');
                            if (!m.hasAttribute('webkit-playsinline')) m.setAttribute('webkit-playsinline', '');
                            if (!m.paused && m.currentTime > 0 && !m.ended) {
                                isPlaying = true;
                            }
                        });
                        try {
                            var frames = doc.querySelectorAll('iframe');
                            for (var i = 0; i < frames.length; i++) {
                                try {
                                    var frameDoc = frames[i].contentDocument || frames[i].contentWindow.document;
                                    if (frameDoc) checkDoc(frameDoc);
                                } catch(e) {}
                            }
                        } catch(e) {}
                    }
                    checkDoc(document);

                    var curPosMs = 0;
                    var curDurMs = 0;
                    var ytp = document.getElementById('movie_player') || document.querySelector('.html5-video-player');
                    if (ytp && typeof ytp.getCurrentTime === 'function' && typeof ytp.getDuration === 'function') {
                        var yCur = ytp.getCurrentTime() || 0;
                        var yDur = ytp.getDuration() || 0;
                        curPosMs = Math.round(yCur * 1000);
                        curDurMs = Math.round(yDur * 1000);
                    } else if (progressMedia) {
                        curPosMs = Math.round((progressMedia.currentTime || 0) * 1000);
                        curDurMs = Math.round((progressMedia.duration || 0) * 1000);
                    }

                    // Protect against reporting stale pre-seek time during active seek transitions
                    if (window.__zenSeekTargetSec !== undefined) {
                        var timeSinceSeek = Date.now() - (window.__zenLastSeekTime || 0);
                        if (timeSinceSeek < 3000) {
                            var targetMs = Math.round(window.__zenSeekTargetSec * 1000);
                            if (Math.abs(curPosMs - targetMs) > 2500) {
                                curPosMs = targetMs;
                            } else {
                                delete window.__zenSeekTargetSec;
                            }
                        } else {
                            delete window.__zenSeekTargetSec;
                        }
                    }

                    if (window.ZenMediaBridge) {
                        window.ZenMediaBridge.onMediaUpdate(hasMedia, isPlaying, document.title || '', location.hostname || '');
                        if (window.ZenMediaBridge.onMediaProgress) {
                            window.ZenMediaBridge.onMediaProgress(curPosMs, curDurMs);
                        }
                    }
                }

                setInterval(checkAndUpdateMediaState, 2000);

                document.addEventListener('loadedmetadata', function(e) {
                    if (e.target && (e.target.tagName === 'VIDEO' || e.target.tagName === 'AUDIO')) {
                        enhanceVideos();
                    }
                }, true);
                document.addEventListener('play', checkAndUpdateMediaState, true);
                document.addEventListener('pause', checkAndUpdateMediaState, true);
                document.addEventListener('seeked', function() {
                    delete window.__zenSeekTargetSec;
                    setTimeout(checkAndUpdateMediaState, 150);
                }, true);
            }

            if (location.hostname.indexOf('addons.mozilla.org') !== -1 && !window.__zenAmoHooked) {
                window.__zenAmoHooked = true;
                document.addEventListener('click', function(e) {
                    var target = e.target ? e.target.closest('a.AMInstallButton-button, a[href*=".xpi"], button.AMInstallButton-button, .InstallButtonWrapper a, .InstallButtonWrapper button, .AMOPrompt .Button, a.Button--action') : null;
                    if (target && window.ZenMediaBridge && window.ZenMediaBridge.onInstallAmoExtension) {
                        e.preventDefault();
                        e.stopPropagation();
                        window.ZenMediaBridge.onInstallAmoExtension(location.href, document.title || '');
                        target.textContent = '✓ Installed in Zen';
                    }
                }, true);
            }

            if ((location.hostname.indexOf('greasyfork.org') !== -1 || location.hostname.indexOf('openuserjs.org') !== -1) && !window.__zenUserscriptHooked) {
                window.__zenUserscriptHooked = true;
                document.addEventListener('click', function(e) {
                    var target = e.target ? e.target.closest('a[href*=".user.js"], a.install-link') : null;
                    if (target && window.ZenMediaBridge && window.ZenMediaBridge.onInstallAmoExtension) {
                        e.preventDefault();
                        e.stopPropagation();
                        window.ZenMediaBridge.onInstallAmoExtension(target.href || location.href, document.title || '');
                        target.textContent = '✓ Installed in Zen';
                    }
                }, true);
            }

            // Form password listener for local password saving & autofill
            if (!window.__zenAutofillHooked) {
                window.__zenAutofillHooked = true;
                function attachAutofillListeners() {
                    try {
                        var forms = document.forms || [];
                        for (var i = 0; i < forms.length; i++) {
                            var f = forms[i];
                            if (!f.__zenHooked) {
                                f.__zenHooked = true;
                                f.addEventListener('submit', function(e) {
                                    var passInput = this.querySelector('input[type="password"]');
                                    if (passInput && passInput.value) {
                                        var userInput = this.querySelector('input[type="text"], input[type="email"], input[name*="user"], input[name*="login"]');
                                        var userVal = userInput ? userInput.value || '' : '';
                                        var passVal = passInput.value;
                                        if (window.ZenAutofillBridge) {
                                            window.ZenAutofillBridge.onSavePassword(location.hostname || '', location.href || '', userVal, passVal);
                                        }
                                    }
                                }, true);
                            }
                        }
                    } catch(e) {}
                }
                attachAutofillListeners();
                document.addEventListener('DOMContentLoaded', attachAutofillListeners);
                document.addEventListener('change', attachAutofillListeners, true);
            }

            // Automatic scroll position restoration for in-page link navigation & browser history
            try {
                if ('scrollRestoration' in history) {
                    history.scrollRestoration = 'auto';
                }
            } catch(e) {}
        })();
        """.trimIndent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        androidx.compose.runtime.key(tab.id) {
            AndroidView(
            factory = {
                webView.apply {
                    (parent as? android.view.ViewGroup)?.removeView(this)
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: return null
                            val isMainFrame = request.isForMainFrame
                            val lowerUrl = reqUrl.lowercase()

                            // Whitelist essential site media streams, CDNs, fonts, and YouTube resources so video/page loading is smooth
                            val isEssentialSiteResource = lowerUrl.contains("googlevideo.com") ||
                                lowerUrl.contains("ytimg.com") ||
                                lowerUrl.contains("youtube.com") ||
                                lowerUrl.contains("youtu.be") ||
                                lowerUrl.contains("fonts.gstatic.com") ||
                                lowerUrl.contains("fonts.googleapis.com") ||
                                (lowerUrl.contains("youtube.com") &&
                                    !lowerUrl.contains("/pagead/") &&
                                    !lowerUrl.contains("doubleclick") &&
                                    !lowerUrl.contains("/api/stats/ads"))
                            if (isEssentialSiteResource) {
                                return super.shouldInterceptRequest(view, request)
                            }

                            val adLevel = currentPrivacyCfg.adBlockerLevel
                            val isUblockActive = currentUiState.firefoxExtensions.any {
                                it.id == "ff_ublock_origin" && it.isInstalled && it.isEnabled
                            }
                            val shouldBlockAds = adLevel != com.example.data.AdBlockerLevel.OFF || currentPrivacyCfg.blockTrackersAndAds || isUblockActive

                            if (!isMainFrame && shouldBlockAds) {
                                val blockedEvent = ZenPrivacyEngine.matchTrackerRequest(reqUrl)
                                val isAggressiveHigh = adLevel == com.example.data.AdBlockerLevel.HIGH && (
                                    lowerUrl.contains("google-analytics.com") ||
                                    lowerUrl.contains("googletagmanager.com") ||
                                    lowerUrl.contains("/analytics") ||
                                    lowerUrl.contains("/telemetry") ||
                                    lowerUrl.contains("facebook.net/en_us/fbevents.js") ||
                                    lowerUrl.contains("doubleclick.net") ||
                                    lowerUrl.contains("scorecardresearch.com")
                                )
                                if (blockedEvent != null || ZenPrivacyEngine.isAdOrTrackerUrl(reqUrl) || isAggressiveHigh) {
                                    val event = blockedEvent ?: BlockedTrackerEvent(
                                        domain = ZenPrivacyEngine.extractHostDisplay(reqUrl),
                                        category = if (isAggressiveHigh) "Telemetry/Tracker" else "Ad Network"
                                    )
                                    view?.post { onTrackerBlocked(event) }
                                    val isJson = lowerUrl.contains("json") || lowerUrl.contains("/pagead/") || lowerUrl.contains("/api/stats/ads") || lowerUrl.contains("youtube.com")
                                    val mimeType = if (isJson) "application/json" else if (reqUrl.contains(".js")) "application/javascript" else "text/plain"
                                    val responseBody = if (isJson) "{}" else ""
                                    return WebResourceResponse(
                                        mimeType,
                                        "utf-8",
                                        ByteArrayInputStream(responseBody.toByteArray(Charsets.UTF_8))
                                    )
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false
                            // Block main-frame popup redirects to ad servers
                            val isUblockActive = currentUiState.firefoxExtensions.any {
                                it.id == "ff_ublock_origin" && it.isInstalled && it.isEnabled
                            }
                            val shouldBlockAdRedirects = currentPrivacyCfg.adBlockerLevel != com.example.data.AdBlockerLevel.OFF || currentPrivacyCfg.blockTrackersAndAds || isUblockActive
                            if (shouldBlockAdRedirects) {
                                if (ZenPrivacyEngine.isAdOrPopupRedirect(targetUrl)) {
                                    val adRedirect = BlockedTrackerEvent(
                                        domain = ZenPrivacyEngine.extractHostDisplay(targetUrl),
                                        category = "Blocked Ad Popup Redirect"
                                    )
                                    view?.post { onTrackerBlocked(adRedirect) }
                                    return true
                                }
                            }
                            // Intercept direct .xpi, .crx, or .user.js desktop extension downloads and install them into Zen
                            if (targetUrl.lowercase().contains(".xpi") || targetUrl.lowercase().contains(".crx") ||
                                targetUrl.lowercase().endsWith(".user.js") || targetUrl.lowercase().contains(".user.js?")
                            ) {
                                onInstallExtensionFromAmo(view?.url ?: targetUrl, view?.title)
                                return true
                            }
                            if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                val curUrl = view?.url ?: tab.url
                                val curY = view?.scrollY ?: 0
                                if (curUrl.isNotBlank() && curY > 0) {
                                    scrollPositionsByUrl[curUrl] = curY
                                }
                                if (currentUiState.sidebarConfig.openLinksInGlanceAutomatically && request?.hasGesture() == true) {
                                    onOpenGlance(targetUrl)
                                    return true
                                }
                                if (currentUiState.sidebarConfig.openInNewTab && request?.hasGesture() == true) {
                                    val forceSwitch = if (currentUiState.sidebarConfig.switchToNewTabImmediately) true else false
                                    onOpenInNewTab(targetUrl, forceSwitch)
                                    return true
                                }
                                knownInternalUrls.add(targetUrl)
                                lastVisitedInternalUrl = targetUrl
                                // Let WebView natively handle all HTTP/HTTPS navigations, redirects, and OAuth/login flows without interrupting
                                return false
                            }
                            if (targetUrl.startsWith("zen://")) {
                                onNavigateUrl(targetUrl)
                                return true
                            }
                            // Ignore custom external app schemes (intent://, market://, mailto:, tel:) so WebView doesn't show ERR_UNKNOWN_URL_SCHEME
                            return true
                        }

                        override fun doUpdateVisitedHistory(
                            view: WebView?,
                            url: String?,
                            isReload: Boolean
                        ) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            if (!url.isNullOrBlank() && url != "about:blank") {
                                knownInternalUrls.add(url)
                                lastVisitedInternalUrl = url
                                onPageVisited(url, view?.title)
                                restoreScrollForUrl(view, url)
                            }
                            if (isFocusedPane && view != null) {
                                val canBackEffective = view.canGoBack() || (tab.url != "zen://home" && tab.url.isNotBlank())
                                onProgressChanged(view.progress, canBackEffective, view.canGoForward())
                            }
                        }

                        override fun onPageCommitVisible(view: WebView?, url: String?) {
                            super.onPageCommitVisible(view, url)
                            if (!url.isNullOrBlank() && url != "about:blank") {
                                restoreScrollForUrl(view, url)
                            }
                            if (isDesktopMode) {
                                val desktopScript = ZenPrivacyEngine.buildDesktopModeSpoofScript(true)
                                if (desktopScript.isNotBlank()) {
                                    view?.evaluateJavascript(desktopScript, null)
                                }
                            }
                            view?.evaluateJavascript(ZenPrivacyEngine.buildDesktopWebExtensionShimScript(), null)
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isUserInteractingWithScroll = false
                            if (!url.isNullOrBlank() && url != "about:blank") {
                                knownInternalUrls.add(url)
                                lastVisitedInternalUrl = url
                                onPageVisited(url, view?.title)
                            }
                            // Inject ad-blocking & anti-fingerprinting script early at page start using latest dynamic payloads
                            val currentUrl = url ?: tab.url
                            val activeTab = tab.copy(url = currentUrl)
                            val dynamicCss = currentUiState.getInjectedCssForTab(activeTab)
                            val dynamicJs = currentUiState.getInjectedJsForTab(activeTab)
                            val earlyScript = ZenPrivacyEngine.buildFingerprintProtectionScript(
                                fingerprintProtection = currentPrivacyCfg.fingerprintProtection,
                                readerModeActive = currentReaderMode,
                                customCssPayload = dynamicCss,
                                customJsPayload = dynamicJs
                            )
                            view?.evaluateJavascript(earlyScript, null)
                            view?.evaluateJavascript(mediaAndAmoScript, null)
                            view?.evaluateJavascript(ZenPrivacyEngine.buildDesktopWebExtensionShimScript(), null)
                            if (isDesktopMode) {
                                val desktopScript = ZenPrivacyEngine.buildDesktopModeSpoofScript(true)
                                if (desktopScript.isNotBlank()) {
                                    view?.evaluateJavascript(desktopScript, null)
                                }
                            }
                            if (isFocusedPane && view != null) {
                                val canBackEffective = view.canGoBack() || (tab.url != "zen://home" && tab.url.isNotBlank())
                                onProgressChanged(view.progress.coerceAtLeast(10), canBackEffective, view.canGoForward())
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (controller.isMuted) {
                                view?.evaluateJavascript("window.__zenApplyMute && window.__zenApplyMute(true);", null)
                            }
                            // Only reset pull-to-refresh if the user is NOT actively holding the pull gesture
                            if (!isPullingGestureActive) {
                                isPullRefreshing = false
                                pullDownDistancePx = 0f
                            }
                            if (!url.isNullOrBlank() && url != "about:blank") {
                                knownInternalUrls.add(url)
                                lastVisitedInternalUrl = url
                                onPageVisited(url, view?.title)
                                restoreScrollForUrl(view, url)
                            }
                            val currentUrl = url ?: tab.url
                            val activeTab = tab.copy(url = currentUrl)
                            val dynamicCss = currentUiState.getInjectedCssForTab(activeTab)
                            val dynamicJs = currentUiState.getInjectedJsForTab(activeTab)
                            val script = ZenPrivacyEngine.buildFingerprintProtectionScript(
                                fingerprintProtection = currentPrivacyCfg.fingerprintProtection,
                                readerModeActive = currentReaderMode,
                                customCssPayload = dynamicCss,
                                customJsPayload = dynamicJs
                            )
                            view?.evaluateJavascript(script, null)
                            view?.evaluateJavascript(mediaAndAmoScript, null)
                            view?.evaluateJavascript(ZenPrivacyEngine.buildDesktopWebExtensionShimScript(), null)
                            if (isDesktopMode) {
                                val desktopScript = ZenPrivacyEngine.buildDesktopModeSpoofScript(true)
                                if (desktopScript.isNotBlank()) {
                                    view?.evaluateJavascript(desktopScript, null)
                                }
                            }

                            // Detect webpage language for automatic translation prompt
                            if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
                                view?.evaluateJavascript(
                                    """
                                    (function() {
                                        var lang = document.documentElement.lang || document.querySelector('meta[http-equiv="content-language"]')?.content || '';
                                        if (!lang) {
                                            var og = document.querySelector('meta[property="og:locale"]');
                                            if (og) lang = og.content;
                                        }
                                        if (lang) {
                                            lang = lang.replace(/[-_].*$/, '').toLowerCase();
                                        }
                                        return lang || '';
                                    })();
                                    """.trimIndent()
                                ) { res ->
                                    val detected = res?.trim()?.removeSurrounding("\"")?.lowercase() ?: ""
                                    if (detected.isNotBlank() && detected != "null" && detected != "en") {
                                        detectedLanguageCode = detected
                                        showTranslateBanner = true
                                    }
                                }
                            }

                            if (currentUrl.startsWith("http://") || currentUrl.startsWith("https://")) {
                                val currentDomain = ZenPrivacyEngine.extractHostDisplay(currentUrl)
                                val pageHost = try { android.net.Uri.parse(currentUrl).host?.lowercase() } catch (e: Exception) { null }
                                val matchingPassword = currentUiState.savedPasswords.find {
                                    val d = it.domain.lowercase()
                                    pageHost != null && (pageHost == d || pageHost.endsWith(".$d") || it.domain.equals(currentDomain, ignoreCase = true))
                                }
                                if (matchingPassword != null) {
                                    detectedAutofillPassword = matchingPassword
                                    showAutofillPrompt = true
                                }
                            }

                            restoreScrollForUrl(view, currentUrl)

                            if (isFocusedPane && view != null) {
                                val canBackEffective = view.canGoBack() || (tab.url != "zen://home" && tab.url.isNotBlank())
                                onProgressChanged(100, canBackEffective, view.canGoForward())
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (isFocusedPane && view != null) {
                                val canBackEffective = view.canGoBack() || (tab.url != "zen://home" && tab.url.isNotBlank())
                                onProgressChanged(newProgress, canBackEffective, view.canGoForward())
                            }
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            val currentUrl = view?.url ?: return
                            if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
                                knownInternalUrls.add(currentUrl)
                                lastVisitedInternalUrl = currentUrl
                                onPageVisited(currentUrl, title)
                            }
                        }

                        override fun getDefaultVideoPoster(): Bitmap {
                            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                        }

                        override fun onShowCustomView(
                            view: android.view.View?,
                            callback: CustomViewCallback?
                        ) {
                            if (fullscreenCustomView != null) {
                                callback?.onCustomViewHidden()
                                return
                            }
                            fullscreenCustomView = view
                            fullscreenCustomViewCallback = callback
                        }

                        override fun onHideCustomView() {
                            fullscreenCustomView = null
                            fullscreenCustomViewCallback?.onCustomViewHidden()
                            fullscreenCustomViewCallback = null
                        }

                        override fun onCreateWindow(
                            view: WebView?,
                            isDialog: Boolean,
                            isUserGesture: Boolean,
                            resultMsg: android.os.Message?
                        ): Boolean {
                            return false
                        }

                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            request ?: return
                            val allowed = request.resources.filter {
                                // DRM playback only. Camera/mic are denied until a per-site prompt exists.
                                it == android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
                            }
                            if (allowed.isNotEmpty()) {
                                request.grant(allowed.toTypedArray())
                            } else {
                                request.deny()
                            }
                        }
                    }

                    if (isNewlyCreated) {
                        knownInternalUrls.add(tab.url)
                        lastProcessedTabUrl = tab.url
                        loadUrl(tab.url)
                    }
                }
            },
            update = { view ->
                val target = tab.url
                if (target != lastProcessedTabUrl) {
                    lastProcessedTabUrl = target
                    val currentWvUrl = view.url
                    val isAlreadyShowing = (target == currentWvUrl) ||
                        (target == view.originalUrl) ||
                        (currentWvUrl != null && (target.trimEnd('/') == currentWvUrl.trimEnd('/')))
                    if (!isAlreadyShowing && target != "zen://home" && target.isNotBlank()) {
                        val curUrl = currentWvUrl ?: target
                        val curY = view.scrollY
                        if (curUrl.isNotBlank() && curY > 0) {
                            scrollPositionsByUrl[curUrl] = curY
                        }
                        knownInternalUrls.add(target)
                        view.loadUrl(target)
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("zen_webview_${tab.id}")
        )
        }

        // Top Pull-to-Refresh Indicator when user pulls down from the top-most position of a webpage:
        // Only shows a reload icon with smooth scale, alpha, drop-down, and rotation animations as you pull down,
        // and stays visible as long as you hold it until you let go!
        if (isPullingGestureActive || pullDownDistancePx > 4f || isPullRefreshing) {
            val progressFraction = (pullDownDistancePx / pullRefreshThresholdPx).coerceIn(0f, 1.45f)
            val normalizedProgress = progressFraction.coerceIn(0f, 1f)
            val indicatorOffsetY = with(density) {
                (pullDownDistancePx.coerceAtMost(pullRefreshThresholdPx * 1.35f) - 20.dp.toPx()).roundToInt()
            }.coerceAtLeast(10)

            val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pull_refresh_spin")
            val continuousSpinAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(
                        durationMillis = 720,
                        easing = androidx.compose.animation.core.LinearEasing
                    ),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Restart
                ),
                label = "pull_refresh_continuous_spin"
            )

            val iconScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isPullRefreshing) {
                    1f
                } else {
                    (0.55f + normalizedProgress * 0.45f + (progressFraction - 1f).coerceAtLeast(0f) * 0.2f).coerceIn(0.5f, 1.15f)
                },
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                ),
                label = "pull_refresh_scale"
            )

            val iconAlpha = if (isPullRefreshing) {
                1f
            } else {
                (normalizedProgress * 1.25f).coerceIn(0.2f, 1f)
            }

            val iconRotation = if (isPullRefreshing) {
                continuousSpinAngle
            } else {
                progressFraction * 300f
            }

            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = Color(0xFF141418).copy(alpha = 0.92f * iconAlpha),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (normalizedProgress >= 1f || isPullRefreshing) {
                        Color(0xFFF4F4F6).copy(alpha = iconAlpha)
                    } else {
                        Color(0xFF3F3F46).copy(alpha = iconAlpha)
                    }
                ),
                shadowElevation = (6f * iconAlpha).dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, indicatorOffsetY) }
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                        alpha = iconAlpha
                    }
                    .testTag("webview_pull_to_refresh_indicator")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Pull to Refresh",
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                rotationZ = iconRotation
                            }
                    )
                }
            }
        }

        // Fullscreen HTML5 Video Container when user taps Fullscreen on any video player
        fullscreenCustomView?.let { customVideoView ->
            androidx.activity.compose.BackHandler {
                fullscreenCustomView = null
                fullscreenCustomViewCallback?.onCustomViewHidden()
                fullscreenCustomViewCallback = null
            }
            AndroidView(
                factory = { ctx ->
                    android.widget.FrameLayout(ctx).apply {
                        setBackgroundColor(android.graphics.Color.BLACK)
                        (customVideoView.parent as? android.view.ViewGroup)?.removeView(customVideoView)
                        addView(
                            customVideoView,
                            android.widget.FrameLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .testTag("zen_fullscreen_video_container")
            )
        }

        // Webpage Language Detection & Translation Menu (styled identically to the Side Panel with blur & transparency)
        if (showTranslateBanner && detectedLanguageCode != null) {
            val sidebarCfg = uiState.sidebarConfig
            val isLiquidGlass = sidebarCfg.liquidGlassEnabled
            val panelAlpha = if (isLiquidGlass) {
                (sidebarCfg.sidebarOpacity * 0.85f).coerceIn(0.20f, 0.95f)
            } else {
                sidebarCfg.sidebarOpacity.coerceIn(0.40f, 1f)
            }
            val bannerShape = RoundedCornerShape(16.dp)
            val bannerBorderBrush = if (isLiquidGlass) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.40f),
                        Color(0xFF1E1E24).copy(alpha = panelAlpha),
                        Color.White.copy(alpha = 0.16f)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF3F3F46), Color(0xFF27272A))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
                    .testTag("zen_translate_banner")
            ) {
                Surface(
                    shape = bannerShape,
                    color = Color(0xFF0F0F14).copy(alpha = panelAlpha),
                    border = BorderStroke(1.2.dp, bannerBorderBrush),
                    shadowElevation = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(bannerShape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF34D399).copy(alpha = 0.18f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Translate",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Page is in ${getLanguageDisplayName(detectedLanguageCode)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F6)
                            )
                            Text(
                                text = "Translate this webpage to English?",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                controller.launchTranslatorPanel()
                                showTranslateBanner = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLiquidGlass) Color.White.copy(alpha = 0.20f) else Color(0xFF27272A),
                                contentColor = Color(0xFF34D399)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.40f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("translate_page_action_button")
                        ) {
                            Text(
                                text = "Translate",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showTranslateBanner = false },
                            modifier = Modifier.size(28.dp).testTag("translate_dismiss_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Save Password Prompt Banner
        detectedSavePassword?.let { pwd ->
            val bannerShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
                    .testTag("zen_save_password_banner")
            ) {
                Surface(
                    shape = bannerShape,
                    color = Color(0xFF0F0F14).copy(alpha = 0.95f),
                    border = BorderStroke(1.2.dp, Color(0xFF60A5FA).copy(alpha = 0.6f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth().clip(bannerShape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF60A5FA).copy(alpha = 0.18f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Save Password",
                                    tint = Color(0xFF60A5FA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Save Password for ${pwd.domain}?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F6)
                            )
                            if (pwd.username.isNotBlank()) {
                                Text(
                                    text = "Account: ${pwd.username}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA1A1AA)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSavePassword(pwd)
                                detectedSavePassword = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("save_password_confirm_button")
                        ) {
                            Text(
                                text = "Save",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { detectedSavePassword = null },
                            modifier = Modifier.size(28.dp).testTag("save_password_dismiss_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Autofill Password Suggestion Banner
        if (showAutofillPrompt && detectedAutofillPassword != null && detectedSavePassword == null) {
            val pwd = detectedAutofillPassword!!
            val bannerShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
                    .testTag("zen_autofill_password_banner")
            ) {
                Surface(
                    shape = bannerShape,
                    color = Color(0xFF0F0F14).copy(alpha = 0.95f),
                    border = BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.6f)),
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth().clip(bannerShape)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF34D399).copy(alpha = 0.18f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Autofill Login",
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Autofill login for ${pwd.domain}?",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F6)
                            )
                            Text(
                                text = if (pwd.username.isNotBlank()) "User: ${pwd.username}" else "Saved password available",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cleanPass = org.json.JSONObject.quote(pwd.password)
                                val cleanUser = org.json.JSONObject.quote(pwd.username)
                                webView.evaluateJavascript(
                                    """
                                    (function() {
                                        var passInput = document.querySelector('input[type="password"]');
                                        if (passInput) {
                                            passInput.value = $cleanPass;
                                            passInput.dispatchEvent(new Event('input', { bubbles: true }));
                                            passInput.dispatchEvent(new Event('change', { bubbles: true }));
                                            var form = passInput.form || document;
                                            var userInput = form.querySelector('input[type="text"], input[type="email"], input[name*="user"], input[name*="login"]');
                                            if (userInput) {
                                                userInput.value = $cleanUser;
                                                userInput.dispatchEvent(new Event('input', { bubbles: true }));
                                                userInput.dispatchEvent(new Event('change', { bubbles: true }));
                                            }
                                        }
                                    })();
                                    """.trimIndent(),
                                    null
                                )
                                showAutofillPrompt = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("autofill_confirm_button")
                        ) {
                            Text(
                                text = "Autofill",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { showAutofillPrompt = false },
                            modifier = Modifier.size(28.dp).testTag("autofill_dismiss_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Link Long-Press Context Menu Overlay (Side Panel blur & transparency styling, no header/URL, no cancel button)
        longPressedLinkUrl?.let { linkUrl ->
            Dialog(
                onDismissRequest = { longPressedLinkUrl = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                val sidebarCfg = uiState.sidebarConfig
                val isLiquidGlass = sidebarCfg.liquidGlassEnabled
                val panelAlpha = if (isLiquidGlass) {
                    (sidebarCfg.sidebarOpacity * 0.85f).coerceIn(0.20f, 0.95f)
                } else {
                    sidebarCfg.sidebarOpacity.coerceIn(0.40f, 1f)
                }
                val panelShape = RoundedCornerShape(18.dp)
                val panelBorderBrush = if (isLiquidGlass) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color(0xFF1E1E24).copy(alpha = panelAlpha),
                            Color.White.copy(alpha = 0.16f)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF3F3F46), Color(0xFF27272A))
                    )
                }

                Surface(
                    shape = panelShape,
                    color = Color(0xFF0F0F14).copy(alpha = panelAlpha),
                    border = BorderStroke(width = 1.2.dp, brush = panelBorderBrush),
                    shadowElevation = 24.dp,
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .fillMaxWidth(0.85f)
                        .clip(panelShape)
                        .testTag("zen_link_options_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            onClick = {
                                onOpenGlance(linkUrl)
                                longPressedLinkUrl = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLiquidGlass) Color.White.copy(alpha = 0.08f) else Color(0xFF1E1E24).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isLiquidGlass) Color.White.copy(alpha = 0.18f) else Color(0xFF2A2A32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_action_at_a_glance")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color(0xFF8AB4F8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Open in At a Glance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF8AB4F8),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                onOpenInNewTab(linkUrl, null)
                                longPressedLinkUrl = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLiquidGlass) Color.White.copy(alpha = 0.06f) else Color(0xFF1E1E24).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isLiquidGlass) Color.White.copy(alpha = 0.12f) else Color(0xFF2A2A32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_action_open_new_tab")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color(0xFFF4F4F6),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Open in New Tab",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF4F4F6),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                onOpenInPrivateTab(linkUrl)
                                longPressedLinkUrl = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLiquidGlass) Color.White.copy(alpha = 0.06f) else Color(0xFF1E1E24).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isLiquidGlass) Color.White.copy(alpha = 0.12f) else Color(0xFF2A2A32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_action_open_private_tab")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShadeGlassIcon(
                                    tint = Color(0xFFF4F4F6),
                                    size = 18.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Open in Private Tab",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF4F4F6),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(linkUrl))
                                longPressedLinkUrl = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLiquidGlass) Color.White.copy(alpha = 0.06f) else Color(0xFF1E1E24).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isLiquidGlass) Color.White.copy(alpha = 0.12f) else Color(0xFF2A2A32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_action_copy_link")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color(0xFFD4D4D8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Copy Link Address",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD4D4D8)
                                )
                            }
                        }

                        Surface(
                            onClick = {
                                onNavigateUrl(linkUrl)
                                longPressedLinkUrl = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLiquidGlass) Color.White.copy(alpha = 0.06f) else Color(0xFF1E1E24).copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isLiquidGlass) Color.White.copy(alpha = 0.12f) else Color(0xFF2A2A32)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("link_action_open_here")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFFD4D4D8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Open in Current Tab",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFD4D4D8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun getLanguageDisplayName(code: String?): String {
    return when (code?.lowercase()?.take(2)) {
        "es" -> "Spanish"
        "fr" -> "French"
        "de" -> "German"
        "it" -> "Italian"
        "pt" -> "Portuguese"
        "ja" -> "Japanese"
        "zh" -> "Chinese"
        "ko" -> "Korean"
        "ru" -> "Russian"
        "ar" -> "Arabic"
        "hi" -> "Hindi"
        "nl" -> "Dutch"
        "tr" -> "Turkish"
        "pl" -> "Polish"
        "sv" -> "Swedish"
        "uk" -> "Ukrainian"
        "vi" -> "Vietnamese"
        "th" -> "Thai"
        "id" -> "Indonesian"
        else -> code?.uppercase() ?: "another language"
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ZenHomeStartPage(
    uiState: ZenBrowserUiState,
    isPrivateTab: Boolean,
    onFocusPane: () -> Unit = {},
    onNavigateUrl: (String) -> Unit,
    onAddHomeShortcut: (title: String, url: String) -> Unit,
    onUpdateHomeShortcut: (HomeShortcutEntity, String, String) -> Unit,
    onDeleteHomeShortcut: (String) -> Unit,
    onUpdateCustomTextPosition: (Float, Float) -> Unit,
    onOpenStartPageCustomizer: () -> Unit,
    onOpenSidebar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var searchInput by remember { mutableStateOf("") }
    var showLongPressMenu by remember { mutableStateOf(false) }
    var longPressOffset by remember { mutableStateOf(Offset.Zero) }
    var editingShortcut by remember { mutableStateOf<HomeShortcutEntity?>(null) }
    var showAddShortcutDialog by remember { mutableStateOf(false) }

    val cfg = uiState.sidebarConfig
    val gradientPreset = uiState.activeStartPageGradient
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(gradientPreset.topHex),
            Color(gradientPreset.midHex),
            Color(gradientPreset.bottomHex)
        )
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBrush)
            .pointerInput(cfg.horizontalSwipeMode) {
                var totalDrag = 0f
                var triggered = false
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f; triggered = false },
                    onDragEnd = { totalDrag = 0f; triggered = false },
                    onDragCancel = { totalDrag = 0f; triggered = false },
                    onHorizontalDrag = { change, dragAmount ->
                        totalDrag += dragAmount
                        if (!triggered) {
                            val openThresholdCrossed = if (cfg.horizontalSwipeMode == com.example.data.HorizontalSwipeNavigationMode.SWIPE_LEFT_PANEL_RIGHT_BACK) {
                                totalDrag < -45f // Swipe left opens side panel
                            } else {
                                totalDrag > 45f  // Swipe right opens side panel
                            }
                            if (openThresholdCrossed) {
                                triggered = true
                                change.consume()
                                onOpenSidebar()
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onFocusPane() },
                    onLongPress = { offset ->
                        longPressOffset = offset
                        showLongPressMenu = true
                    }
                )
            }
            .testTag("zen_start_page"),
        contentAlignment = Alignment.Center
    ) {
        val parentWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val parentHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        // Optional Custom Wallpaper Image with optional Gaussian Blur
        if (!cfg.startPageImageUri.isNullOrBlank()) {
            val blurModifier = if (cfg.startPageBlurEnabled && cfg.startPageBlurRadius > 0f) {
                Modifier.blur(cfg.startPageBlurRadius.dp)
            } else {
                Modifier
            }
            AsyncImage(
                model = Uri.parse(cfg.startPageImageUri),
                contentDescription = "Start Page Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
            )
        }

        // Home Screen Brightness Overlay (controlled by startPageBrightness in Homescreen Settings)
        val brightness = cfg.startPageBrightness.coerceIn(0.15f, 1.5f)
        if (brightness < 0.99f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (1f - brightness).coerceIn(0f, 0.85f)))
            )
        } else if (brightness > 1.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = ((brightness - 1f) * 0.45f).coerceIn(0f, 0.35f)))
            )
        }

        // Long-press "Customize" Context Menu anchored at exact press location on the Home Screen
        val anchorX = if (longPressOffset == Offset.Zero) {
            (parentWidthPx * 0.5f).roundToInt()
        } else {
            longPressOffset.x.roundToInt().coerceIn(24, (parentWidthPx - 140f).roundToInt().coerceAtLeast(24))
        }
        val anchorY = if (longPressOffset == Offset.Zero) {
            120
        } else {
            longPressOffset.y.roundToInt().coerceIn(24, (parentHeightPx - 80f).roundToInt().coerceAtLeast(24))
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(anchorX, anchorY) }
        ) {
            DropdownMenu(
                expanded = showLongPressMenu,
                onDismissRequest = { showLongPressMenu = false },
                containerColor = Color(0xFF141417)
            ) {
                DropdownMenuItem(
                    text = { Text("Customize") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Customize Start Page",
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = {
                        showLongPressMenu = false
                        onOpenStartPageCustomizer()
                    },
                    modifier = Modifier.testTag("start_page_customize_menu_item")
                )
            }
        }

        if (cfg.homeScreenStyle == HomeScreenStyle.ZEN) {
            // ZEN HOMESCREEN MODE: Blank canvas with optional custom text in any color positioned anywhere (and draggable)
            if (cfg.zenCustomText.isNotBlank()) {
                val xOffset = ((cfg.zenCustomTextXFraction - 0.5f) * parentWidthPx * 0.85f).roundToInt()
                val yOffset = ((cfg.zenCustomTextYFraction - 0.5f) * parentHeightPx * 0.85f).roundToInt()
                Text(
                    text = cfg.zenCustomText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(cfg.zenCustomTextColorHex),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .offset { IntOffset(xOffset, yOffset) }
                        .pointerInput(parentWidthPx, parentHeightPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val nextX = (cfg.zenCustomTextXFraction + dragAmount.x / parentWidthPx).coerceIn(0.05f, 0.95f)
                                val nextY = (cfg.zenCustomTextYFraction + dragAmount.y / parentHeightPx).coerceIn(0.05f, 0.92f)
                                onUpdateCustomTextPosition(nextX, nextY)
                            }
                        }
                        .testTag("zen_home_custom_text")
                )
            }
        } else {
            // NORMAL HOMESCREEN MODE:
            // - "Z" logo and "zen" wordmark removed
            // - Blank search input (no "Search with DuckDuckGo..." placeholder)
            // - Editable & removable shortcuts grid
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp),
                contentPadding = PaddingValues(vertical = 48.dp)
            ) {
                // Clean Search Box with Blank Placeholder
                item {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { searchInput = it },
                        placeholder = {
                            Text(
                                text = "",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            if (isPrivateTab) {
                                ShadeGlassIcon(tint = Color(0xFFD4D4D8), size = 16.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (searchInput.isNotBlank()) {
                                    onNavigateUrl(searchInput)
                                }
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF52525B),
                            unfocusedBorderColor = Color(0xFF27272A),
                            focusedContainerColor = Color(0xFF121215).copy(alpha = 0.88f),
                            unfocusedContainerColor = Color(0xFF0E0E11).copy(alpha = 0.82f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_page_search_box")
                    )
                }

                // Editable & Removable Shortcuts Grid
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        maxItemsInEachRow = 4
                    ) {
                        uiState.homeShortcuts.forEach { shortcut ->
                            var showShortcutMenu by remember { mutableStateOf(false) }

                            Box(modifier = Modifier.weight(1f)) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .combinedClickable(
                                            onClick = { onNavigateUrl(shortcut.url) },
                                            onLongClick = { showShortcutMenu = true }
                                        )
                                        .padding(vertical = 6.dp)
                                        .testTag("speed_dial_${shortcut.title.lowercase().replace(" ", "_")}")
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF121215).copy(alpha = 0.88f),
                                        border = BorderStroke(1.dp, Color(0xFF222226)),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = shortcut.title.firstOrNull()?.uppercaseChar()?.toString() ?: "Z",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = Color(0xFFE4E4E7),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = shortcut.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                DropdownMenu(
                                    expanded = showShortcutMenu,
                                    onDismissRequest = { showShortcutMenu = false },
                                    containerColor = Color(0xFF141417)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit Shortcut") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            showShortcutMenu = false
                                            editingShortcut = shortcut
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Remove Shortcut") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            showShortcutMenu = false
                                            onDeleteHomeShortcut(shortcut.id)
                                        }
                                    )
                                }
                            }
                        }

                        // "+" Tile to Add a New Shortcut
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showAddShortcutDialog = true }
                                .padding(vertical = 6.dp)
                                .testTag("add_home_shortcut_tile")
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0E0E11).copy(alpha = 0.75f),
                                border = BorderStroke(1.dp, Color(0xFF222226)),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Shortcut",
                                        tint = Color(0xFF71717A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF71717A)
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit Existing Shortcut Dialog
    editingShortcut?.let { target ->
        var editTitle by remember(target) { mutableStateOf(target.title) }
        var editUrl by remember(target) { mutableStateOf(target.url) }
        AlertDialog(
            onDismissRequest = { editingShortcut = null },
            containerColor = Color(0xFF141417),
            title = { Text("Edit Shortcut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        label = { Text("URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateHomeShortcut(target, editTitle, editUrl)
                        editingShortcut = null
                    }
                ) {
                    Text("Save", color = Color(0xFFF4F4F6))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteHomeShortcut(target.id)
                        editingShortcut = null
                    }
                ) {
                    Text("Remove", color = Color(0xFFA1A1AA))
                }
            }
        )
    }

    // Add New Shortcut Dialog
    if (showAddShortcutDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddShortcutDialog = false },
            containerColor = Color(0xFF141417),
            title = { Text("Add Shortcut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Title") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newUrl,
                        onValueChange = { newUrl = it },
                        label = { Text("URL") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newUrl.isNotBlank()) {
                            onAddHomeShortcut(newTitle, newUrl)
                            showAddShortcutDialog = false
                        }
                    }
                ) {
                    Text("Add", color = Color(0xFFF4F4F6))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddShortcutDialog = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ZenOfflinePageRenderer(
    page: OfflinePageEntity,
    onLoadLiveSite: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().background(Color(0xFF101014))) {
        // Offline Header Banner
        Surface(
            color = Color(0xFF1A1A22),
            border = BorderStroke(1.dp, Color(0xFF2E2E38)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("offline_banner_header")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = "Offline Mode",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "OFFLINE SAVED PAGE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF818CF8),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val sizeKb = (page.sizeBytes / 1024).coerceAtLeast(1)
                            Text(
                                text = "• $sizeKb KB",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF71717A)
                            )
                        }
                        Text(
                            text = page.title.ifBlank { page.url },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF4F4F6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onLoadLiveSite,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF818CF8),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("load_live_site_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Load Live Site",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // WebView displaying stored HTML content
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = false
                        allowContentAccess = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                onNavigateUrl(url)
                                return true
                            }
                            return false
                        }
                    }
                    loadDataWithBaseURL(page.url, page.htmlContent, "text/html", "utf-8", null)
                }
            },
            update = { view ->
                view.loadDataWithBaseURL(page.url, page.htmlContent, "text/html", "utf-8", null)
            },
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}



