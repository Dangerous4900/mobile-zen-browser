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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.ActiveModalSheet
import com.example.ui.ZenBrowserUiState
import android.graphics.Canvas as AndroidCanvas
import java.io.ByteArrayInputStream
import kotlin.math.roundToInt

class ZenWebViewController {
    var webView: WebView? = null

    fun goBack() {
        webView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun goForward() {
        webView?.let { if (it.canGoForward()) it.goForward() }
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

    fun toggleMediaPlayPause() {
        webView?.evaluateJavascript(
            """
            (function() {
                var media = document.querySelector('video, audio');
                if (media) {
                    if (media.paused) { media.play(); } else { media.pause(); }
                }
            })();
            """.trimIndent(),
            null
        )
    }

    fun stopAllMedia() {
        webView?.evaluateJavascript(
            """
            (function() {
                document.querySelectorAll('video, audio').forEach(function(m) { m.pause(); });
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
    onOpenInNewTab: (url: String) -> Unit = {},
    onOpenInPrivateTab: (url: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (tab.url == "zen://home" || tab.url.isBlank() || tab.isUnloaded) {
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
    val currentPrivacyCfg by rememberUpdatedState(privacyCfg)
    val currentCustomCss by rememberUpdatedState(customCss)
    val currentCustomJs by rememberUpdatedState(customJs)
    val currentReaderMode by rememberUpdatedState(readerMode)



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

    // Track URLs known to the WebView so asynchronous Room updates or HTTP redirects never trigger a reload loop
    val knownWebViewUrls = remember(tab.id) { mutableSetOf(tab.url) }
    var lastLoadedExternalUrl by remember(tab.id) { mutableStateOf(tab.url) }
    // Link long-press context menu state (Open in New Tab, Open in Private Tab, Copy Link Address, etc.)
    var longPressedLinkUrl by remember(tab.id) { mutableStateOf<String?>(null) }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    val webView = remember(tab.id) {
        var touchStartY = 0f
        var touchStartX = 0f
        var pullEngageY = 0f
        var startedAtTopOfPage = false
        var isPullingDownToRefresh = false

        WebView(context).apply {
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
            setOnTouchListener { v, event ->
                val wv = v as? WebView ?: return@setOnTouchListener false
                when (event.actionMasked) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        touchStartY = event.rawY
                        touchStartX = event.rawX
                        pullEngageY = event.rawY
                        startedAtTopOfPage = (wv.scrollY <= 8 && !wv.canScrollVertically(-1))
                        isPullingDownToRefresh = false
                        isPullingGestureActive = false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        if (isPullingDownToRefresh) {
                            // Once the pull-down refresh gesture is engaged, keep holding it!
                            // It will NEVER disappear until the user lets go of the screen, and follows slow pulls up/down smoothly.
                            isPullingGestureActive = true
                            val rawPull = (event.rawY - pullEngageY).coerceAtLeast(10f)
                            pullDownDistancePx = (rawPull * 0.58f).coerceIn(10f, pullRefreshThresholdPx * 1.65f)
                            // Consume the touch event so WebView doesn't bounce/overscroll or cancel the hold
                            return@setOnTouchListener true
                        } else {
                            val isAtTopNow = (wv.scrollY <= 8 && !wv.canScrollVertically(-1))
                            if (!startedAtTopOfPage && isAtTopNow) {
                                // User scrolled up to the very top while holding finger down — anchor from here
                                startedAtTopOfPage = true
                                touchStartY = event.rawY
                                touchStartX = event.rawX
                            } else if (startedAtTopOfPage && !isAtTopNow) {
                                startedAtTopOfPage = false
                            }

                            if (startedAtTopOfPage && isAtTopNow) {
                                val dy = event.rawY - touchStartY
                                val dx = kotlin.math.abs(event.rawX - touchStartX)
                                if (dy > 12f && dy > dx * 1.15f) {
                                    isPullingDownToRefresh = true
                                    isPullingGestureActive = true
                                    pullEngageY = event.rawY - 12f
                                    val rawPull = (event.rawY - pullEngageY).coerceAtLeast(10f)
                                    pullDownDistancePx = (rawPull * 0.58f).coerceIn(10f, pullRefreshThresholdPx * 1.65f)
                                    return@setOnTouchListener true
                                }
                            }
                        }
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
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
                        } else if (!isPullRefreshing) {
                            pullDownDistancePx = 0f
                        }
                    }
                }
                false
            }
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = false
                allowFileAccess = true
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                setSupportMultipleWindows(false)
                cacheMode = WebSettings.LOAD_DEFAULT
                // Clean standard Mobile Chrome User-Agent without "; wv" so websites & video players serve standard mobile web
                val rawUa = userAgentString ?: ""
                if (rawUa.isNotBlank()) {
                    userAgentString = rawUa
                        .replace("; wv)", ")")
                        .replace("Version/4.0 ", "")
                }
            }
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, !privacyCfg.blockThirdPartyCookies)

            addJavascriptInterface(
                object {
                    @JavascriptInterface
                    fun onMediaUpdate(isPlaying: Boolean, title: String, host: String) {
                        post {
                            onMediaStateChanged(
                                true,
                                isPlaying,
                                title.ifBlank { tab.title },
                                host.ifBlank { ZenPrivacyEngine.extractHostDisplay(tab.url) },
                                tab.id
                            )
                        }
                    }

                    @JavascriptInterface
                    fun onInstallAmoExtension(amoUrl: String, addonTitle: String) {
                        post {
                            onInstallExtensionFromAmo(amoUrl, addonTitle)
                        }
                    }
                },
                "ZenMediaBridge"
            )
        }
    }

    DisposableEffect(webView, isFocusedPane) {
        if (isFocusedPane) {
            controller.webView = webView
        }
        onDispose {
            if (controller.webView === webView) {
                controller.webView = null
            }
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

    val mediaAndAmoScript = remember {
        """
        (function() {
            var oldFab = document.getElementById('__zen_translator_fab');
            if (oldFab) { oldFab.remove(); }

            function enhanceVideos() {
                document.querySelectorAll('video').forEach(function(v) {
                    if (!v.hasAttribute('playsinline')) {
                        v.setAttribute('playsinline', '');
                    }
                    if (!v.hasAttribute('webkit-playsinline')) {
                        v.setAttribute('webkit-playsinline', '');
                    }
                });
            }
            enhanceVideos();

            if (!window.__zenMediaHooked) {
                window.__zenMediaHooked = true;
                document.addEventListener('loadedmetadata', function(e) {
                    if (e.target && e.target.tagName === 'VIDEO') {
                        enhanceVideos();
                    }
                }, true);
                document.addEventListener('play', function(e) {
                    if (e.target && (e.target.tagName === 'VIDEO' || e.target.tagName === 'AUDIO')) {
                        if (window.ZenMediaBridge) {
                            window.ZenMediaBridge.onMediaUpdate(true, document.title || '', location.hostname || '');
                        }
                    }
                }, true);
                document.addEventListener('pause', function(e) {
                    if (e.target && (e.target.tagName === 'VIDEO' || e.target.tagName === 'AUDIO')) {
                        if (window.ZenMediaBridge) {
                            window.ZenMediaBridge.onMediaUpdate(false, document.title || '', location.hostname || '');
                        }
                    }
                }, true);
            }

            if (location.hostname.indexOf('addons.mozilla.org') !== -1 && !window.__zenAmoHooked) {
                window.__zenAmoHooked = true;
                document.addEventListener('click', function(e) {
                    var target = e.target ? e.target.closest('a.AMInstallButton-button, a[href*=".xpi"], button.AMInstallButton-button') : null;
                    if (target && window.ZenMediaBridge && window.ZenMediaBridge.onInstallAmoExtension) {
                        e.preventDefault();
                        e.stopPropagation();
                        window.ZenMediaBridge.onInstallAmoExtension(location.href, document.title || '');
                        target.textContent = 'Installed in Zen';
                    }
                }, true);
            }
        })();
        """.trimIndent()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: return null
                            val isMainFrame = request.isForMainFrame
                            // Block third-party ad & tracker requests when either Privacy Shield OR uBlock Origin is active
                            val isUblockActive = currentUiState.firefoxExtensions.any {
                                it.id == "ff_ublock_origin" && it.isInstalled && it.isEnabled
                            }
                            if (!isMainFrame && (currentPrivacyCfg.blockTrackersAndAds || isUblockActive)) {
                                val lowerUrl = reqUrl.lowercase()
                                // First check if this request is a known ad or tracker endpoint
                                val blockedEvent = ZenPrivacyEngine.matchTrackerRequest(reqUrl)
                                if (blockedEvent != null) {
                                    view?.post { onTrackerBlocked(blockedEvent) }
                                    val mimeType = if (reqUrl.contains(".js")) "application/javascript" else "text/plain"
                                    return WebResourceResponse(
                                        mimeType,
                                        "utf-8",
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                // If uBlock Origin or ad blocking is active, intercept any ad server / ad path request
                                if (ZenPrivacyEngine.isAdOrTrackerUrl(reqUrl)) {
                                    val adEvent = BlockedTrackerEvent(
                                        domain = ZenPrivacyEngine.extractHostDisplay(reqUrl),
                                        category = "Ad Network"
                                    )
                                    view?.post { onTrackerBlocked(adEvent) }
                                    val mimeType = if (reqUrl.contains(".js")) "application/javascript" else "text/plain"
                                    return WebResourceResponse(
                                        mimeType,
                                        "utf-8",
                                        ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                                // Preserve actual video streams & CDN scripts that did not match any ad/tracker rule
                                val isEssentialMediaStream = lowerUrl.contains("googlevideo.com/videoplayback") &&
                                    !lowerUrl.contains("&adformat=") &&
                                    !lowerUrl.contains("&ctier=l")
                                if (isEssentialMediaStream) {
                                    return super.shouldInterceptRequest(view, request)
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
                            if ((currentPrivacyCfg.blockTrackersAndAds || isUblockActive) && !request.hasGesture()) {
                                if (ZenPrivacyEngine.isAdOrTrackerUrl(targetUrl)) {
                                    val adRedirect = BlockedTrackerEvent(
                                        domain = ZenPrivacyEngine.extractHostDisplay(targetUrl),
                                        category = "Ad Network"
                                    )
                                    view?.post { onTrackerBlocked(adRedirect) }
                                    return true
                                }
                            }
                            // Intercept direct .xpi Firefox extension downloads and install them into Zen
                            if (targetUrl.lowercase().contains(".xpi")) {
                                onInstallExtensionFromAmo(view?.url ?: targetUrl, view?.title)
                                return true
                            }
                            if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                knownWebViewUrls.add(targetUrl)
                                lastLoadedExternalUrl = targetUrl
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
                                knownWebViewUrls.add(url)
                                lastLoadedExternalUrl = url
                                onPageVisited(url, view?.title)
                            }
                            if (isFocusedPane && view != null) {
                                onProgressChanged(view.progress, view.canGoBack(), view.canGoForward())
                            }
                        }

                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            if (!url.isNullOrBlank() && url != "about:blank") {
                                knownWebViewUrls.add(url)
                                lastLoadedExternalUrl = url
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
                            if (isFocusedPane && view != null) {
                                onProgressChanged(view.progress.coerceAtLeast(10), view.canGoBack(), view.canGoForward())
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // Only reset pull-to-refresh if the user is NOT actively holding the pull gesture
                            if (!isPullingGestureActive) {
                                isPullRefreshing = false
                                pullDownDistancePx = 0f
                            }
                            if (!url.isNullOrBlank() && url != "about:blank") {
                                knownWebViewUrls.add(url)
                                lastLoadedExternalUrl = url
                                onPageVisited(url, view?.title)
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
                            if (isFocusedPane && view != null) {
                                onProgressChanged(100, view.canGoBack(), view.canGoForward())
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (isFocusedPane && view != null) {
                                onProgressChanged(newProgress, view.canGoBack(), view.canGoForward())
                            }
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            val currentUrl = view?.url ?: return
                            if (currentUrl.isNotBlank() && currentUrl != "about:blank") {
                                knownWebViewUrls.add(currentUrl)
                                lastLoadedExternalUrl = currentUrl
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

                        override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                            request ?: return
                            val allowed = request.resources.filter {
                                it == android.webkit.PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID ||
                                    it == android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                                    it == android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE
                            }
                            if (allowed.isNotEmpty()) {
                                request.grant(allowed.toTypedArray())
                            } else {
                                request.deny()
                            }
                        }
                    }

                    knownWebViewUrls.add(tab.url)
                    lastLoadedExternalUrl = tab.url
                    loadUrl(tab.url)
                }
            },
            update = { view ->
                // Only call loadUrl when tab.url was explicitly changed by the user from outside the WebView
                // (e.g. URL bar or shortcut), and is NOT an internal redirect or SPA history update already known to this WebView
                val target = tab.url
                if (target != "zen://home" &&
                    target.isNotBlank() &&
                    target != lastLoadedExternalUrl &&
                    target != view.url &&
                    target != view.originalUrl &&
                    !knownWebViewUrls.contains(target)
                ) {
                    knownWebViewUrls.add(target)
                    lastLoadedExternalUrl = target
                    view.loadUrl(target)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("zen_webview_${tab.id}")
        )

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

        // Link Long-Press Context Menu Overlay (Open in new tab, Open in private tab, Copy link, Share)
        longPressedLinkUrl?.let { linkUrl ->
            val hostDisplay = ZenPrivacyEngine.extractHostDisplay(linkUrl)
            AlertDialog(
                onDismissRequest = { longPressedLinkUrl = null },
                containerColor = Color(0xFF141418),
                shape = RoundedCornerShape(16.dp),
                title = {
                    Column {
                        Text(
                            text = "Link Options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F6)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = linkUrl,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = {
                                onOpenInNewTab(linkUrl)
                                longPressedLinkUrl = null
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E24),
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
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E24),
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
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E24),
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
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E1E24),
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
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = { longPressedLinkUrl = null },
                        modifier = Modifier.testTag("link_action_cancel_button")
                    ) {
                        Text("Cancel", color = Color(0xFFA1A1AA))
                    }
                }
            )
        }
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
