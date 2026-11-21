package com.example.gecko

import android.content.Context
import org.json.JSONObject
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.MediaSession
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.WebExtension
import java.util.concurrent.ConcurrentHashMap

data class GeckoMediaState(
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = ""
)

data class ZenDownloadEvent(
    val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSec: Long,
    /** "downloading", "paused", "completed", "cancelled", or "failed" */
    val state: String
)

data class ZenContextInfo(val imageUrl: String?, val linkUrl: String?, val mediaUrl: String?, val title: String?)

interface ZenGeckoCallback {
    fun onLocationChange(url: String, title: String?)
    fun onProgressChange(progress: Int, canGoBack: Boolean, canGoForward: Boolean)
    fun onTrackerBlocked(sourceUrl: String, category: String)
    fun onMediaStateChange(isActive: Boolean, isPlaying: Boolean, title: String, artwork: android.graphics.Bitmap? = null)
    fun onOpenNewTab(url: String)
    fun onInstallAmoExtension(url: String, title: String?) {}
}

object ZenGeckoManager {
    @Volatile
    private var runtime: GeckoRuntime? = null
    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun getAppContext(): Context? = appContext

    /** Set by the UI: called on the main thread when a page download finished or failed. */
    @Volatile var downloadEvents: ((ZenDownloadEvent) -> Unit)? = null

    /** Set by the UI: long-press on an image / link / video. */
    @Volatile var contextMenuSink: ((ZenContextInfo) -> Unit)? = null

    @Volatile var isSidebarOpen: Boolean = false
    @Volatile private var suppressContextMenuUntil: Long = 0L

    fun suppressContextMenuFor(durationMs: Long = 1800L) {
        val target = System.currentTimeMillis() + durationMs
        if (target > suppressContextMenuUntil) {
            suppressContextMenuUntil = target
        }
    }

    fun onSidebarStateChanged(open: Boolean) {
        isSidebarOpen = open
        if (open) {
            suppressContextMenuFor(2000L)
        }
    }

    fun isContextMenuSuppressed(screenX: Int? = null): Boolean {
        if (isSidebarOpen) return true
        if (System.currentTimeMillis() < suppressContextMenuUntil) return true
        if (screenX != null) {
            val dm = appContext?.resources?.displayMetrics
            val density = dm?.density ?: 2.5f
            val edgePx = (72f * density).toInt()
            val widthPx = dm?.widthPixels ?: 1080
            if (screenX <= edgePx || (widthPx > 0 && screenX >= widthPx - edgePx)) {
                return true
            }
        }
        return false
    }

    private fun emitDownloadEvent(
        id: String,
        name: String,
        url: String,
        mime: String,
        done: Long,
        total: Long,
        speed: Long,
        state: String
    ) {
        val resolvedTotal = if (total > 0) total else if (state == "completed") done else 0L
        val ev = ZenDownloadEvent(id, name, url, mime, done, resolvedTotal, speed, state)
        mainHandler.post { downloadEvents?.invoke(ev) }
    }

    /** "Download image" / "Download link": plain HTTP download with progress, pause, and cancel. */
    fun downloadUrl(url: String, existingId: String? = null) {
        val ctx = appContext ?: return
        val id = existingId ?: ("dl_" + java.util.UUID.randomUUID().toString().take(8))
        var name = com.example.downloads.ZenDownloader.fileNameFor(url, null, null)
        var mime = ""
        var total = 0L
        emitDownloadEvent(id, name, url, mime, 0L, 0L, 0L, "downloading")
        com.example.downloads.ZenDownloader.saveFromUrl(
            context = ctx,
            url = url,
            downloadId = id,
            onStart = { n, m, t ->
                name = n; mime = m; total = t
                if (!com.example.downloads.ZenDownloader.isPaused(id) && !com.example.downloads.ZenDownloader.wasCancelled(id)) {
                    emitDownloadEvent(id, n, url, m, 0L, t, 0L, "downloading")
                    com.example.downloads.ZenDownloader.notifyProgress(ctx, id, n, 0L, t, 0L)
                }
            },
            onProgress = { done, speed ->
                if (!com.example.downloads.ZenDownloader.isPaused(id) && !com.example.downloads.ZenDownloader.wasCancelled(id)) {
                    emitDownloadEvent(id, name, url, mime, done, total, speed, "downloading")
                    com.example.downloads.ZenDownloader.notifyProgress(ctx, id, name, done, total, speed)
                }
            },
            onDone = { ok, bytes ->
                val cancelled = !ok && com.example.downloads.ZenDownloader.wasCancelled(id)
                val finalState = when {
                    ok -> "completed"
                    cancelled -> "cancelled"
                    else -> "failed"
                }
                emitDownloadEvent(id, name, url, mime, bytes, if (ok) bytes else total, 0L, finalState)
                com.example.downloads.ZenDownloader.notifyFinished(ctx, id, name, ok)
            },
            onStateChange = { state, done, t, speed ->
                val resolvedTotal = if (t > 0L) t else total
                emitDownloadEvent(id, name, url, mime, done, resolvedTotal, speed, state)
            }
        )
    }

    fun pauseDownload(id: String) {
        val ctx = appContext ?: return
        com.example.downloads.ZenDownloader.pause(ctx, id)
    }

    fun resumeDownload(id: String, fallbackUrl: String? = null): Boolean {
        val ctx = appContext ?: return false
        if (com.example.downloads.ZenDownloader.resume(ctx, id)) return true
        if (!fallbackUrl.isNullOrBlank() && (fallbackUrl.startsWith("http://") || fallbackUrl.startsWith("https://"))) {
            downloadUrl(fallbackUrl, existingId = id)
            return true
        }
        return false
    }

    fun cancelDownload(id: String) {
        val ctx = appContext ?: return
        com.example.downloads.ZenDownloader.cancel(ctx, id)
    }

    /** Set by MainActivity: opens the system file chooser for <input type=file> and reports the chosen uris. */
    @Volatile var filePicker: ((multiple: Boolean, mimeTypes: Array<String>, onResult: (List<android.net.Uri>) -> Unit) -> Unit)? = null

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val sessionPool = ConcurrentHashMap<String, GeckoSession>()
    private val aiPanelSessionPool = ConcurrentHashMap<String, GeckoSession>()
    private val aiPanelLoadedUrls = ConcurrentHashMap<String, String>()
    private val tabSettings = ConcurrentHashMap<String, Boolean>() // tabId -> isDesktopMode
    val activeMediaSessions = ConcurrentHashMap<String, MediaSession>() // tabId -> MediaSession
    private val tabAtTop = ConcurrentHashMap<String, Boolean>() // tabId -> scrollY <= threshold
    private val mutedTabs = ConcurrentHashMap<String, Boolean>() // tabId -> muted
    private val tabCallbacks = ConcurrentHashMap<String, ZenGeckoCallback>()
    private val attachedViews = ConcurrentHashMap<String, org.mozilla.geckoview.GeckoView>()
    private val attachedAiViews = ConcurrentHashMap<String, org.mozilla.geckoview.GeckoView>()
    private val installedGeckoExtensions = ConcurrentHashMap<String, WebExtension>()
    private val slugToGeckoId = ConcurrentHashMap<String, String>()
    private val extensionActions = ConcurrentHashMap<String, WebExtension.Action>()
    private val BUILT_IN_EXTENSION_IDS = setOf(
        "page-ux@zenbrowser.com",
        "adblock@zenbrowser.com",
        "video-bg-play@zenbrowser.com",
        "datasaver@zenbrowser.com"
    )
    @Volatile var onOpenNewTabRequested: ((String) -> Unit)? = null
    @Volatile var onSyncNativeExtension: ((id: String, name: String, slug: String, version: String, enabled: Boolean, allowPrivate: Boolean) -> Unit)? = null
    @Volatile private var pendingPopupCallback: ((GeckoSession?) -> Unit)? = null
    @Volatile private var pageUxPort: WebExtension.Port? = null
    @Volatile private var latestExtCss: String = ""
    @Volatile private var latestExtJs: String = ""
    @Volatile private var latestPipEnabled: Boolean = false
    @Volatile var isInPipMode: Boolean = false
    @Volatile var backgroundPlayEnabled: Boolean = true
    @Volatile private var pipTransitionAt: Long = 0L
    @Volatile private var activityPauseAt: Long = 0L
    @Volatile private var tabSwitchAt: Long = 0L
    @Volatile private var lastMediaPlayingAt: Long = 0L

    fun isTabAtTop(tabId: String): Boolean = tabAtTop[tabId] ?: true

    fun noteActivityPause() {
        activityPauseAt = System.currentTimeMillis()
    }

    fun noteTabSwitch() {
        tabSwitchAt = System.currentTimeMillis()
        keepBackgroundSessionsActive()
    }

    fun noteUserPauseIntent() {
        lastUserPauseAt = System.currentTimeMillis()
    }

    fun noteUserMediaPause() {
        noteUserPauseIntent()
    }

    fun getOpenSessionTabIds(): Set<String> =
        sessionPool.filterValues { it.isOpen }.keys.toSet()

    fun onPipModeChanged(enabled: Boolean) {
        isInPipMode = enabled
        pipTransitionAt = System.currentTimeMillis()
        pushPipMode(enabled)
        keepBackgroundSessionsActive()
    }

    fun setFocusedTab(tabId: String) {
        val rt = runtime ?: return
        val focusedSession = sessionPool[tabId] ?: return
        sessionPool.forEach { (id, sess) ->
            if (sess.isOpen) {
                runCatching { rt.webExtensionController.setTabActive(sess, id == tabId) }
            }
        }
        runCatching { focusedSession.setActive(true) }
    }

    fun attachSessionToView(tabId: String, view: org.mozilla.geckoview.GeckoView) {
        val session = sessionPool[tabId] ?: return
        val prev = attachedViews[tabId]
        if (prev != null && prev !== view) {
            runCatching {
                if (prev.session === session) {
                    prev.releaseSession()
                }
            }
        }
        if (view.session !== session) {
            runCatching {
                if (view.session != null) {
                    view.releaseSession()
                }
            }
            view.setSession(session)
        }
        attachedViews[tabId] = view
        runCatching { session.setActive(true) }
        setFocusedTab(tabId)
    }

    private fun sendPageUxMessage(payload: JSONObject) {
        mainHandler.post {
            runCatching { pageUxPort?.postMessage(payload) }
        }
    }

    fun requestSaveCurrentScroll() {
        sendPageUxMessage(JSONObject().put("type", "saveScrollNow"))
    }

    fun pushPipMode(enabled: Boolean) {
        latestPipEnabled = enabled
        sendPageUxMessage(JSONObject().put("type", "setPipMode").put("enabled", enabled))
    }

    fun pushExtensionAssets(css: String, js: String) {
        latestExtCss = css
        latestExtJs = js
        val shim = com.example.privacy.ZenPrivacyEngine.buildDesktopWebExtensionShimScript()
        sendPageUxMessage(
            JSONObject()
                .put("type", "extensionPayload")
                .put("css", css)
                .put("js", js)
                .put("shim", shim)
        )
    }

    fun executeScriptOnActivePage(script: String) {
        if (script.isBlank()) return
        sendPageUxMessage(
            JSONObject()
                .put("type", "evalScript")
                .put("js", script)
        )
    }

    private fun handlePageUxNativeMessage(
        message: Any,
        sender: WebExtension.MessageSender? = null
    ): GeckoResult<Any>? {
        val obj = message as? JSONObject ?: return null
        when (obj.optString("type")) {
            "checkAiSidePanel" -> {
                val isAi = isAiPanelSession(sender?.session)
                return GeckoResult.fromValue(JSONObject().put("isAiSidePanel", isAi))
            }
            "getScroll" -> {
                val url = obj.optString("url")
                val y = ZenScrollStore.getScrollY(appContext, url)
                return GeckoResult.fromValue(JSONObject().put("y", y))
            }
            "saveScroll" -> {
                val url = obj.optString("url")
                val y = obj.optInt("y", 0)
                ZenScrollStore.saveScrollY(appContext, url, y)
            }
            "topState" -> {
                val url = obj.optString("url")
                val atTop = obj.optBoolean("atTop", true)
                val normalized = ZenScrollStore.normalizeUrl(url)
                sessionLoadedUrls.entries.forEach { (tid, loadedUrl) ->
                    if (ZenScrollStore.normalizeUrl(loadedUrl) == normalized) {
                        tabAtTop[tid] = atTop
                    }
                }
            }
            "userMediaPauseIntent" -> {
                noteUserPauseIntent()
            }
            "mediaState" -> {
                val url = obj.optString("url")
                val isActive = obj.optBoolean("isActive", false)
                val isPlaying = obj.optBoolean("isPlaying", false)
                val title = obj.optString("title", "Media")
                val now = System.currentTimeMillis()
                if (isPlaying) {
                    lastMediaPlayingAt = now
                }
                val inPipTransition = (now - pipTransitionAt) < 3800L
                val inTabSwitchTransition = (now - tabSwitchAt) < 2500L
                val inBackgroundTransition = backgroundPlayEnabled && (now - activityPauseAt) < 4000L
                val userPaused = (now - lastUserPauseAt) < 2500L
                val suppressTransientPause = !isPlaying && isActive && !userPaused &&
                    (now - lastMediaPlayingAt < 6500L) &&
                    (inPipTransition || inTabSwitchTransition || inBackgroundTransition)
                val normalized = ZenScrollStore.normalizeUrl(url)
                val matchedEntry = sessionLoadedUrls.entries.firstOrNull {
                    ZenScrollStore.normalizeUrl(it.value) == normalized
                }
                val tid = matchedEntry?.key
                if (tid != null && !activeMediaSessions.containsKey(tid) && !suppressTransientPause) {
                    mainHandler.post {
                        tabCallbacks[tid]?.onMediaStateChange(isActive, isPlaying, title, null)
                    }
                }
            }
            "installAmoExtension" -> {
                val extUrl = obj.optString("url").trim()
                val pageUrl = obj.optString("pageUrl").trim()
                val extTitle = obj.optString("title").takeIf { it.isNotBlank() }
                if (extUrl.isNotBlank()) {
                    val normPage = ZenScrollStore.normalizeUrl(pageUrl.ifBlank { extUrl })
                    val normExt = ZenScrollStore.normalizeUrl(extUrl)
                    val matchedEntry = sessionLoadedUrls.entries.firstOrNull {
                        val n = ZenScrollStore.normalizeUrl(it.value)
                        n == normPage || n == normExt
                    }
                    val cb = matchedEntry?.key?.let { tabCallbacks[it] } ?: tabCallbacks.values.firstOrNull()
                    if (cb != null) {
                        mainHandler.post { cb.onInstallAmoExtension(extUrl, extTitle) }
                    }
                }
            }
        }
        return null
    }

    private fun installOrEnsurePageUxExtension(rt: GeckoRuntime) {
        val attachDelegate: (WebExtension?) -> Unit = { ext ->
            if (ext != null) {
                runCatching { rt.webExtensionController.setAllowedInPrivateBrowsing(ext, true) }
                runCatching {
                    ext.setMessageDelegate(object : WebExtension.MessageDelegate {
                        override fun onMessage(
                            nativeApp: String,
                            message: Any,
                            sender: WebExtension.MessageSender
                        ): GeckoResult<Any>? {
                            return handlePageUxNativeMessage(message, sender)
                        }

                        override fun onConnect(port: WebExtension.Port) {
                            pageUxPort = port
                            port.setDelegate(object : WebExtension.PortDelegate {
                                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                                    handlePageUxNativeMessage(message)
                                }

                                override fun onDisconnect(port: WebExtension.Port) {
                                    if (pageUxPort === port) pageUxPort = null
                                }
                            })
                            val shim = com.example.privacy.ZenPrivacyEngine.buildDesktopWebExtensionShimScript()
                            runCatching {
                                port.postMessage(
                                    JSONObject()
                                        .put("type", "extensionPayload")
                                        .put("css", latestExtCss)
                                        .put("js", latestExtJs)
                                        .put("shim", shim)
                                )
                                if (mutedTabs.values.any { it }) {
                                    port.postMessage(JSONObject().put("type", "setMuted").put("muted", true))
                                }
                                if (latestPipEnabled) {
                                    port.postMessage(JSONObject().put("type", "setPipMode").put("enabled", true))
                                }
                            }
                        }
                    }, "zenPageUx")
                }
            }
        }

        try {
            rt.webExtensionController.ensureBuiltIn(
                "resource://android/assets/extensions/zen_page_ux/",
                "page-ux@zenbrowser.com"
            ).accept({ attachDelegate(it) }, { })
        } catch (_: Throwable) {
            try {
                rt.webExtensionController.installBuiltIn(
                    "resource://android/assets/extensions/zen_page_ux/"
                ).accept({ attachDelegate(it) }, { })
            } catch (_: Throwable) {}
        }
    }

    private fun extractSlugFromAmoListingUrl(listingUrl: String?): String {
        if (listingUrl.isNullOrBlank()) return ""
        val path = runCatching { android.net.Uri.parse(listingUrl).path }.getOrNull().orEmpty()
        val segments = path.split('/').filter { it.isNotBlank() }
        val addonIdx = segments.indexOfFirst { it.equals("addon", ignoreCase = true) }
        return if (addonIdx >= 0 && addonIdx + 1 < segments.size) segments[addonIdx + 1].lowercase() else ""
    }

    private fun createExtensionPopupSession(rt: GeckoRuntime): GeckoResult<GeckoSession> {
        val cb = pendingPopupCallback
        pendingPopupCallback = null
        val popupSession = GeckoSession()
        popupSession.open(rt)
        runCatching { popupSession.setActive(true) }
        popupSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (uri.isNotBlank() && uri != "about:blank") {
                    mainHandler.post { onOpenNewTabRequested?.invoke(uri) }
                }
                return null
            }
        }
        mainHandler.post { cb?.invoke(popupSession) }
        return GeckoResult.fromValue(popupSession)
    }

    private fun registerUserExtension(rt: GeckoRuntime, ext: WebExtension, slugHint: String = "") {
        if (ext.id in BUILT_IN_EXTENSION_IDS || ext.isBuiltIn) return
        installedGeckoExtensions[ext.id] = ext
        val amoSlug = slugHint.ifBlank { extractSlugFromAmoListingUrl(ext.metaData?.amoListingUrl) }.lowercase()
        if (amoSlug.isNotBlank()) slugToGeckoId[amoSlug] = ext.id
        ext.metaData?.name?.trim()?.lowercase()?.takeIf { it.isNotBlank() }?.let { extNameKey ->
            slugToGeckoId[extNameKey] = ext.id
        }
        runCatching {
            ext.setTabDelegate(object : WebExtension.TabDelegate {
                override fun onNewTab(
                    source: WebExtension,
                    createDetails: WebExtension.CreateTabDetails
                ): GeckoResult<GeckoSession>? {
                    val targetUrl = createDetails.url?.trim().orEmpty()
                    if (targetUrl.isNotBlank() && targetUrl != "about:blank") {
                        mainHandler.post { onOpenNewTabRequested?.invoke(targetUrl) }
                    }
                    return null
                }

                override fun onOpenOptionsPage(source: WebExtension) {
                    val optionsUrl = source.metaData?.optionsPageUrl?.trim().orEmpty()
                    if (optionsUrl.isNotBlank()) {
                        mainHandler.post { onOpenNewTabRequested?.invoke(optionsUrl) }
                    }
                }
            })
        }
        runCatching {
            ext.setActionDelegate(object : WebExtension.ActionDelegate {
                override fun onBrowserAction(
                    extension: WebExtension,
                    session: GeckoSession?,
                    action: WebExtension.Action
                ) {
                    if (session == null || !extensionActions.containsKey(extension.id)) {
                        extensionActions[extension.id] = action
                    }
                }

                override fun onPageAction(
                    extension: WebExtension,
                    session: GeckoSession?,
                    action: WebExtension.Action
                ) {
                    extensionActions.putIfAbsent(extension.id, action)
                }

                override fun onTogglePopup(
                    extension: WebExtension,
                    action: WebExtension.Action
                ): GeckoResult<GeckoSession>? {
                    extensionActions[extension.id] = action
                    return createExtensionPopupSession(rt)
                }

                override fun onOpenPopup(
                    extension: WebExtension,
                    action: WebExtension.Action
                ): GeckoResult<GeckoSession>? {
                    extensionActions[extension.id] = action
                    return createExtensionPopupSession(rt)
                }
            })
        }
        val md = ext.metaData
        if (md != null) {
            val resolvedName = md.name?.takeIf { it.isNotBlank() } ?: ext.id
            val resolvedSlug = amoSlug.ifBlank { ext.id }
            val resolvedVer = md.version?.takeIf { it.isNotBlank() } ?: "1.0.0"
            mainHandler.post {
                onSyncNativeExtension?.invoke(
                    ext.id,
                    resolvedName,
                    resolvedSlug,
                    resolvedVer,
                    md.enabled,
                    md.allowedInPrivateBrowsing
                )
            }
        }
    }

    fun getOrCreateRuntime(context: Context): GeckoRuntime {
        appContext = context.applicationContext
        return runtime ?: synchronized(this) {
            runtime ?: run {
                val settings = GeckoRuntimeSettings.Builder()
                    .aboutConfigEnabled(true)
                    .consoleOutput(false)
                    .extensionsWebAPIEnabled(true)
                    .extensionsProcessEnabled(true)
                    .contentBlocking(
                        ContentBlocking.Settings.Builder()
                            .antiTracking(ContentBlocking.AntiTracking.STRICT)
                            .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                            .cookieBannerHandlingMode(ContentBlocking.CookieBannerMode.COOKIE_BANNER_MODE_REJECT_OR_ACCEPT)
                            .cookieBannerGlobalRulesEnabled(true)
                            .cookieBannerGlobalRulesSubFramesEnabled(true)
                            .queryParameterStrippingEnabled(true)
                            .queryParameterStrippingPrivateBrowsingEnabled(true)
                            .strictSocialTrackingProtection(true)
                            .cookiePurging(true)
                            .build()
                    )
                    .build()
                GeckoRuntime.create(context.applicationContext, settings).also { rt ->
                    runtime = rt
                    runCatching { rt.webExtensionController.enableExtensionProcessSpawning() }
                    runCatching {
                        rt.webExtensionController.setPromptDelegate(object : org.mozilla.geckoview.WebExtensionController.PromptDelegate {
                            override fun onInstallPrompt(extension: WebExtension): GeckoResult<AllowOrDeny>? {
                                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                            }

                            override fun onInstallPrompt(
                                extension: WebExtension,
                                permissions: Array<out String>,
                                origins: Array<out String>
                            ): GeckoResult<AllowOrDeny>? {
                                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                            }

                            override fun onUpdatePrompt(
                                currentlyInstalled: WebExtension,
                                updatedExtension: WebExtension,
                                newPermissions: Array<out String>,
                                newOrigins: Array<out String>
                            ): GeckoResult<AllowOrDeny>? {
                                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                            }

                            override fun onOptionalPrompt(
                                extension: WebExtension,
                                permissions: Array<out String>,
                                origins: Array<out String>
                            ): GeckoResult<AllowOrDeny>? {
                                return GeckoResult.fromValue(AllowOrDeny.ALLOW)
                            }
                        })
                    }
                    runCatching {
                        rt.webExtensionController.setAddonManagerDelegate(object : org.mozilla.geckoview.WebExtensionController.AddonManagerDelegate {
                            override fun onInstalled(extension: WebExtension) {
                                registerUserExtension(rt, extension)
                            }

                            override fun onReady(extension: WebExtension) {
                                registerUserExtension(rt, extension)
                            }

                            override fun onEnabled(extension: WebExtension) {
                                registerUserExtension(rt, extension)
                            }

                            override fun onDisabled(extension: WebExtension) {
                                registerUserExtension(rt, extension)
                            }

                            override fun onOptionalPermissionsChanged(extension: WebExtension) {
                                registerUserExtension(rt, extension)
                            }

                            override fun onUninstalled(extension: WebExtension) {
                                installedGeckoExtensions.remove(extension.id)
                                extensionActions.remove(extension.id)
                                slugToGeckoId.entries.removeIf { it.value == extension.id }
                            }
                        })
                    }
                    try {
                        rt.webExtensionController.ensureBuiltIn(
                            "resource://android/assets/extensions/video_bg_play/",
                            "video-bg-play@zenbrowser.com"
                        ).accept({ ext ->
                            if (ext != null) rt.webExtensionController.setAllowedInPrivateBrowsing(ext, true)
                        }, { })
                    } catch (_: Throwable) {
                        try {
                            rt.webExtensionController.installBuiltIn(
                                "resource://android/assets/extensions/video_bg_play/"
                            ).accept({ ext ->
                                if (ext != null) rt.webExtensionController.setAllowedInPrivateBrowsing(ext, true)
                            }, { })
                        } catch (_: Throwable) {}
                    }
                    installOrEnsurePageUxExtension(rt)
                    if (isAdBlockActive) {
                        installOrEnsureAdBlockExtension(rt)
                    }
                    runCatching {
                        rt.webExtensionController.list().accept({ extensions ->
                            extensions?.forEach { ext ->
                                registerUserExtension(rt, ext)
                            }
                        }, { })
                    }
                }
            }
        }
    }

    @Volatile
    private var isAdBlockActive = true

    private fun installOrEnsureAdBlockExtension(rt: GeckoRuntime) {
        try {
            rt.webExtensionController.ensureBuiltIn(
                "resource://android/assets/extensions/zen_adblock/",
                "adblock@zenbrowser.com"
            ).accept({ ext ->
                if (ext != null) rt.webExtensionController.setAllowedInPrivateBrowsing(ext, true)
            }, { })
        } catch (_: Throwable) {
            try {
                rt.webExtensionController.installBuiltIn(
                    "resource://android/assets/extensions/zen_adblock/"
                ).accept({ ext ->
                    if (ext != null) rt.webExtensionController.setAllowedInPrivateBrowsing(ext, true)
                }, { })
            } catch (_: Throwable) {}
        }
    }

    fun setAdBlockEnabled(context: Context, enabled: Boolean) {
        isAdBlockActive = enabled
        val rt = getOrCreateRuntime(context)
        if (enabled) {
            installOrEnsureAdBlockExtension(rt)
        } else {
            runCatching {
                rt.webExtensionController.list().accept({ extensions ->
                    extensions?.firstOrNull { it.id == "adblock@zenbrowser.com" }?.let { ext ->
                        rt.webExtensionController.uninstall(ext)
                    }
                }, { })
            }
        }
    }

    fun getOrCreateSession(
        context: Context,
        tabId: String,
        isPrivate: Boolean = false,
        isDesktop: Boolean = false
    ): GeckoSession {
        return sessionPool.getOrPut(tabId) {
            tabSettings[tabId] = isDesktop
            val settings = GeckoSessionSettings.Builder()
                .usePrivateMode(isPrivate)
                .useTrackingProtection(true)
                .suspendMediaWhenInactive(false)
                .userAgentMode(
                    if (isDesktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
                )
                .viewportMode(
                    if (isDesktop) GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
                    else GeckoSessionSettings.VIEWPORT_MODE_MOBILE
                )
                .build()
            val session = GeckoSession(settings)
            session.open(getOrCreateRuntime(context))
            try {
                session.setActive(true)
            } catch (_: Throwable) {}
            session
        }
    }

    fun isAiPanelSession(session: GeckoSession?): Boolean {
        if (session == null) return false
        return aiPanelSessionPool.values.any { it === session }
    }

    fun getOrCreateAiPanelSession(
        context: Context,
        providerId: String,
        initialUrl: String
    ): GeckoSession {
        val existing = aiPanelSessionPool[providerId]
        if (existing != null && existing.isOpen) {
            return existing
        }
        val settings = GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .suspendMediaWhenInactive(false)
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .build()
        val session = GeckoSession(settings)
        session.open(getOrCreateRuntime(context))
        runCatching { session.setActive(true) }
        aiPanelSessionPool[providerId] = session
        if (initialUrl.isNotBlank()) {
            aiPanelLoadedUrls[providerId] = initialUrl
            session.loadUri(initialUrl)
        }
        return session
    }

    fun getAiPanelCurrentUrl(providerId: String): String? = aiPanelLoadedUrls[providerId]

    fun attachAiSessionToView(
        providerId: String,
        session: GeckoSession,
        view: org.mozilla.geckoview.GeckoView
    ) {
        attachedAiViews.entries.removeIf { it.value === view && it.key != providerId }
        val prev = attachedAiViews[providerId]
        if (prev != null && prev !== view) {
            runCatching {
                if (prev.session === session) {
                    prev.releaseSession()
                }
            }
        }
        if (view.session !== session) {
            runCatching {
                if (view.session != null) {
                    view.releaseSession()
                }
            }
            view.setSession(session)
        }
        attachedAiViews[providerId] = view
        runCatching { session.setActive(true) }
    }

    fun releaseAiSessionFromView(
        providerId: String,
        view: org.mozilla.geckoview.GeckoView
    ) {
        attachedAiViews.entries.removeIf { it.value === view || it.key == providerId && it.value === view }
        runCatching {
            if (view.session != null) {
                view.releaseSession()
            }
        }
    }

    fun attachAiPanelDelegates(
        session: GeckoSession,
        providerId: String,
        onLocationChange: (String) -> Unit,
        onProgressChange: (progress: Int, canGoBack: Boolean) -> Unit,
        onOpenInNewTab: (String) -> Unit
    ) {
        var canGoBackNow = false
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                canGoBackNow = canGoBack
                onProgressChange(100, canGoBackNow)
            }

            override fun onLocationChange(
                s: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                if (!url.isNullOrBlank() && url != "about:blank") {
                    aiPanelLoadedUrls[providerId] = url
                    onLocationChange(url)
                }
            }

            override fun onNewSession(s: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (uri.isNotBlank() && uri != "about:blank") {
                    val host = runCatching { android.net.Uri.parse(uri).host?.lowercase().orEmpty() }.getOrDefault("")
                    val isAuthHost = host.contains("accounts.google.") ||
                        host.contains("appleid.apple.") ||
                        host.contains("login.microsoftonline.") ||
                        host.contains("auth0.") ||
                        host.contains("auth.openai.") ||
                        host.contains("chatgpt.com") ||
                        host.contains("claude.ai") ||
                        host.contains("gemini.google.") ||
                        host.contains("perplexity.ai") ||
                        host.contains("grok.com") ||
                        host.contains("x.com")
                    if (isAuthHost) {
                        mainHandler.post { s.loadUri(uri) }
                    } else {
                        mainHandler.post { onOpenInNewTab(uri) }
                    }
                }
                return null
            }

            override fun onLoadRequest(
                s: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                val uri = request.uri
                val scheme = runCatching { android.net.Uri.parse(uri).scheme?.lowercase() }.getOrNull()
                val web = scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob"
                return if (!web) GeckoResult.fromValue(AllowOrDeny.DENY) else null
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                onProgressChange(15, canGoBackNow)
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                onProgressChange(100, canGoBackNow)
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                onProgressChange(progress, canGoBackNow)
            }
        }

        session.promptDelegate = object : GeckoSession.PromptDelegate {
            override fun onPopupPrompt(
                s: GeckoSession,
                prompt: GeckoSession.PromptDelegate.PopupPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? =
                GeckoResult.fromValue(prompt.confirm(AllowOrDeny.DENY))

            override fun onFilePrompt(
                s: GeckoSession,
                prompt: GeckoSession.PromptDelegate.FilePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val ctx = appContext
                val picker = filePicker
                if (ctx == null || picker == null) return GeckoResult.fromValue(prompt.dismiss())
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                val multiple = prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
                val types = prompt.mimeTypes?.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")
                mainHandler.post {
                    picker(multiple, types) { uris ->
                        result.complete(
                            when {
                                uris.isEmpty() -> prompt.dismiss()
                                multiple -> prompt.confirm(ctx, uris.toTypedArray())
                                else -> prompt.confirm(ctx, uris.first())
                            }
                        )
                    }
                }
                return result
            }
        }
    }

    fun keepBackgroundSessionsActive() {
        sessionPool.values.forEach { s ->
            if (s.isOpen) {
                try {
                    s.setActive(true)
                } catch (_: Throwable) {}
            }
        }
    }

    private val sessionLoadedUrls = ConcurrentHashMap<String, String>()

    fun getCurrentSessionUrl(tabId: String): String? = sessionLoadedUrls[tabId]

    fun setCurrentSessionUrl(tabId: String, url: String) {
        if (url.isNotBlank() && url != "about:blank" && !url.startsWith("zen://")) {
            sessionLoadedUrls[tabId] = url
        }
    }

    fun attachDelegates(
        session: GeckoSession,
        tabId: String,
        callback: ZenGeckoCallback
    ) {
        tabCallbacks[tabId] = callback
        var currentCanBack = false
        var currentCanForward = false
        var currentTitle: String? = null

        session.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(s: GeckoSession, scrollX: Int, scrollY: Int) {
                tabAtTop[tabId] = scrollY <= 8
            }
        }

        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(s: GeckoSession, canGoBack: Boolean) {
                currentCanBack = canGoBack
                callback.onProgressChange(100, currentCanBack, currentCanForward)
            }

            override fun onCanGoForward(s: GeckoSession, canGoForward: Boolean) {
                currentCanForward = canGoForward
                callback.onProgressChange(100, currentCanBack, currentCanForward)
            }

            override fun onLocationChange(
                s: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                if (!url.isNullOrBlank() && url != "about:blank") {
                    sessionLoadedUrls[tabId] = url
                    callback.onLocationChange(url, currentTitle)
                }
            }

            override fun onNewSession(s: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (isAdBlockActive && shouldBlockPopup(uri)) {
                    callback.onTrackerBlocked(uri.ifBlank { "popup" }, "Blocked Popup")
                    return GeckoResult.fromValue(null)
                }
                lastPopupAt = System.currentTimeMillis()
                callback.onOpenNewTab(uri)
                return null
            }

            // Tab-under / redirect ads: block navigations to ad/popup hosts and unrequested app-launching schemes.
            override fun onLoadRequest(
                s: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                val uri = request.uri
                val lowerUri = uri.lowercase()
                val cleanPath = lowerUri.substringBefore('?').substringBefore('#')
                val scheme = runCatching { android.net.Uri.parse(uri).scheme?.lowercase() }.getOrNull()
                if (scheme == "https" &&
                    (cleanPath.endsWith(".xpi") || cleanPath.endsWith(".user.js"))
                ) {
                    mainHandler.post { callback.onInstallAmoExtension(uri, currentTitle) }
                    return GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY)
                }
                val userInitiated = System.currentTimeMillis() - lastUserTouchAt < USER_GESTURE_WINDOW_MS
                val web = scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob" || scheme == "resource" || scheme == "moz-extension"
                val host = runCatching { android.net.Uri.parse(uri).host }.getOrNull()
                val isBlockedAd = isAdBlockActive && (
                    com.example.privacy.ZenAdBlockList.isBlockedHost(host) ||
                        com.example.privacy.ZenPrivacyEngine.isAdOrTrackerUrl(uri) ||
                        com.example.privacy.ZenPrivacyEngine.isAdOrPopupRedirect(uri, userInitiated = userInitiated)
                    )
                val deny = (!web && !userInitiated) || isBlockedAd
                if (isBlockedAd) {
                    callback.onTrackerBlocked(uri, "Blocked Ad")
                }
                return if (deny) GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY) else null
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                tabAtTop[tabId] = true
                callback.onProgressChange(15, currentCanBack, currentCanForward)
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                callback.onProgressChange(100, currentCanBack, currentCanForward)
                val shim = com.example.privacy.ZenPrivacyEngine.buildDesktopWebExtensionShimScript()
                sendPageUxMessage(
                    JSONObject()
                        .put("type", "extensionPayload")
                        .put("css", latestExtCss)
                        .put("js", latestExtJs)
                        .put("shim", shim)
                )
                val savedY = sessionLoadedUrls[tabId]?.let { ZenScrollStore.getScrollY(appContext, it) } ?: 0
                if (savedY > 2) {
                    sendPageUxMessage(JSONObject().put("type", "restoreScroll").put("y", savedY))
                }
                if (mutedTabs[tabId] == true) {
                    sendPageUxMessage(JSONObject().put("type", "setMuted").put("muted", true))
                }
                if (latestPipEnabled) {
                    sendPageUxMessage(JSONObject().put("type", "setPipMode").put("enabled", true))
                }
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                callback.onProgressChange(progress, currentCanBack, currentCanForward)
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(s: GeckoSession, title: String?) {
                currentTitle = title
            }

            // Anything the page serves as a download (extensions .xpi, archives, apk, pdf "save", ...).
            override fun onExternalResponse(s: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                val ctx = appContext ?: return
                val body = response.body ?: return
                val headers = response.headers
                val mime = headers.entries.firstOrNull { it.key.equals("content-type", true) }?.value?.substringBefore(';')?.trim().orEmpty()
                val total = headers.entries.firstOrNull { it.key.equals("content-length", true) }?.value?.trim()?.toLongOrNull() ?: 0L
                val name = com.example.downloads.ZenDownloader.fileNameFor(response.uri, headers, mime)
                val lowerUri = response.uri.lowercase()
                val lowerName = name.lowercase()
                val isExtensionPackage = mime.equals("application/x-xpinstall", ignoreCase = true) ||
                    lowerName.endsWith(".xpi") ||
                    lowerUri.substringBefore('?').substringBefore('#').endsWith(".xpi") ||
                    lowerName.endsWith(".user.js") ||
                    lowerUri.substringBefore('?').substringBefore('#').endsWith(".user.js")
                if (isExtensionPackage) {
                    runCatching { body.close() }
                    mainHandler.post { callback.onInstallAmoExtension(response.uri, currentTitle) }
                    return
                }
                val id = "dl_" + java.util.UUID.randomUUID().toString().take(8)
                emitDownloadEvent(id, name, response.uri, mime, 0L, total, 0L, "downloading")
                com.example.downloads.ZenDownloader.notifyProgress(ctx, id, name, 0L, total, 0L)
                com.example.downloads.ZenDownloader.save(
                    context = ctx,
                    fileName = name,
                    mimeType = mime,
                    input = body,
                    downloadId = id,
                    url = response.uri,
                    totalBytes = total,
                    onProgress = { done, speed ->
                        if (!com.example.downloads.ZenDownloader.isPaused(id) && !com.example.downloads.ZenDownloader.wasCancelled(id)) {
                            emitDownloadEvent(id, name, response.uri, mime, done, total, speed, "downloading")
                            com.example.downloads.ZenDownloader.notifyProgress(ctx, id, name, done, total, speed)
                        }
                    },
                    onDone = { ok, bytes ->
                        val cancelled = !ok && com.example.downloads.ZenDownloader.wasCancelled(id)
                        val finalState = when {
                            ok -> "completed"
                            cancelled -> "cancelled"
                            else -> "failed"
                        }
                        emitDownloadEvent(id, name, response.uri, mime, bytes, if (ok) bytes else total, 0L, finalState)
                        com.example.downloads.ZenDownloader.notifyFinished(ctx, id, name, ok)
                    },
                    onStateChange = { state, done, t, speed ->
                        emitDownloadEvent(id, name, response.uri, mime, done, if (t > 0L) t else total, speed, state)
                    }
                )
            }

            override fun onContextMenu(
                s: GeckoSession,
                screenX: Int,
                screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement
            ) {
                if (isContextMenuSuppressed(screenX)) return
                val image = if (element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE) element.srcUri else null
                val media = if (element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_VIDEO ||
                    element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_AUDIO) element.srcUri else null
                if (image == null && media == null && element.linkUri == null) return
                mainHandler.post {
                    if (!isContextMenuSuppressed(screenX)) {
                        contextMenuSink?.invoke(ZenContextInfo(image, element.linkUri, media, element.title ?: element.altText))
                    }
                }
            }
        }

        session.promptDelegate = object : GeckoSession.PromptDelegate {
            // Pop-up windows the engine would otherwise ask about: always deny.
            override fun onPopupPrompt(
                s: GeckoSession,
                prompt: GeckoSession.PromptDelegate.PopupPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? =
                GeckoResult.fromValue(prompt.confirm(org.mozilla.geckoview.AllowOrDeny.DENY))

            // Suppress unsolicited JS alert() popups (scam/virus/ad-blocker traps).
            override fun onAlertPrompt(
                s: GeckoSession,
                prompt: GeckoSession.PromptDelegate.AlertPrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val unsolicited = System.currentTimeMillis() - lastUserTouchAt > 1200L
                return if (isAdBlockActive && unsolicited) {
                    GeckoResult.fromValue(prompt.dismiss())
                } else {
                    super.onAlertPrompt(s, prompt)
                }
            }

            // <input type="file"> (uploads).
            override fun onFilePrompt(
                s: GeckoSession,
                prompt: GeckoSession.PromptDelegate.FilePrompt
            ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? {
                val ctx = appContext
                val picker = filePicker
                if (ctx == null || picker == null) return GeckoResult.fromValue(prompt.dismiss())
                val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
                val multiple = prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE
                val types = prompt.mimeTypes?.takeIf { it.isNotEmpty() } ?: arrayOf("*/*")
                mainHandler.post {
                    picker(multiple, types) { uris ->
                        result.complete(
                            when {
                                uris.isEmpty() -> prompt.dismiss()
                                multiple -> prompt.confirm(ctx, uris.toTypedArray())
                                else -> prompt.confirm(ctx, uris.first())
                            }
                        )
                    }
                }
                return result
            }
        }

        session.contentBlockingDelegate = object : ContentBlocking.Delegate {
            override fun onContentBlocked(s: GeckoSession, event: ContentBlocking.BlockEvent) {
                val cat = event.antiTrackingCategory
                val categoryName = when {
                    (cat and ContentBlocking.AntiTracking.FINGERPRINTING) != 0 -> "Fingerprinting Tracker"
                    (cat and ContentBlocking.AntiTracking.CRYPTOMINING) != 0 -> "Cryptominer"
                    (cat and ContentBlocking.AntiTracking.SOCIAL) != 0 -> "Social Tracker"
                    (cat and ContentBlocking.AntiTracking.AD) != 0 -> "Ad Tracker"
                    else -> "Blocked Tracker"
                }
                callback.onTrackerBlocked(event.uri, categoryName)
            }
        }

        session.mediaSessionDelegate = object : MediaSession.Delegate {
            override fun onMetadata(
                s: GeckoSession,
                mediaSession: MediaSession,
                meta: MediaSession.Metadata
            ) {
                activeMediaSessions[tabId] = mediaSession
                val title = meta.title ?: currentTitle ?: "Media"
                val artImage = meta.artwork
                if (artImage != null) {
                    try {
                        artImage.getBitmap(512).accept(
                            { bmp ->
                                callback.onMediaStateChange(isActive = true, isPlaying = true, title = title, artwork = bmp)
                            },
                            {
                                callback.onMediaStateChange(isActive = true, isPlaying = true, title = title, artwork = null)
                            }
                        )
                    } catch (e: Exception) {
                        callback.onMediaStateChange(isActive = true, isPlaying = true, title = title, artwork = null)
                    }
                } else {
                    callback.onMediaStateChange(isActive = true, isPlaying = true, title = title, artwork = null)
                }
            }

            override fun onPlay(s: GeckoSession, mediaSession: MediaSession) {
                activeMediaSessions[tabId] = mediaSession
                lastMediaPlayingAt = System.currentTimeMillis()
                if (mutedTabs[tabId] == true) mediaSession.muteAudio(true)
                callback.onMediaStateChange(isActive = true, isPlaying = true, title = currentTitle ?: "Media")
            }

            override fun onPause(s: GeckoSession, mediaSession: MediaSession) {
                activeMediaSessions[tabId] = mediaSession
                val now = System.currentTimeMillis()
                val inPipTransition = (now - pipTransitionAt) < 3800L
                val inTabSwitchTransition = (now - tabSwitchAt) < 2500L
                val inBackgroundTransition = backgroundPlayEnabled && (now - activityPauseAt) < 4000L
                val recentMedia = (now - lastMediaPlayingAt) < 6500L
                val userRequestedPause = (now - lastUserPauseAt) < 2500L
                if (!userRequestedPause && recentMedia && (inPipTransition || inTabSwitchTransition || inBackgroundTransition)) {
                    keepBackgroundSessionsActive()
                    mainHandler.post {
                        runCatching { s.setActive(true) }
                        runCatching { mediaSession.play() }
                        sendPageUxMessage(JSONObject().put("type", "mediaCommand").put("action", "play"))
                    }
                    callback.onMediaStateChange(isActive = true, isPlaying = true, title = currentTitle ?: "Media")
                    return
                }
                callback.onMediaStateChange(isActive = true, isPlaying = false, title = currentTitle ?: "Media")
            }

            override fun onStop(s: GeckoSession, mediaSession: MediaSession) {
                activeMediaSessions.remove(tabId)
                callback.onMediaStateChange(isActive = false, isPlaying = false, title = "")
            }

            override fun onPositionState(
                s: GeckoSession,
                mediaSession: MediaSession,
                state: MediaSession.PositionState
            ) {
                activeMediaSessions[tabId] = mediaSession
                val durSec = state.duration
                val posSec = state.position
                if (durSec > 0.0) {
                    val durMs = (durSec * 1000.0).toLong()
                    val posMs = (posSec * 1000.0).toLong()
                    com.example.ZenMediaService.progress(posMs, durMs)
                }
            }
        }
    }

    private const val USER_GESTURE_WINDOW_MS = 800L
    @Volatile var lastUserTouchAt = 0L
    @Volatile private var lastPopupAt = 0L

    fun noteUserTouch() { lastUserTouchAt = System.currentTimeMillis() }

    /** A page may open a new tab only right after a real tap, never to an ad/popup URL or blank window, and not repeatedly. */
    private fun shouldBlockPopup(uri: String): Boolean {
        val now = System.currentTimeMillis()
        val trimmed = uri.trim().lowercase()
        if (trimmed.isEmpty() ||
            trimmed == "about:blank" ||
            trimmed.startsWith("javascript:") ||
            trimmed.startsWith("data:") ||
            trimmed.startsWith("blob:")
        ) {
            return true
        }
        val host = runCatching { android.net.Uri.parse(uri).host }.getOrNull()
        return now - lastUserTouchAt > USER_GESTURE_WINDOW_MS ||
            now - lastPopupAt < 3000L ||
            com.example.privacy.ZenAdBlockList.isBlockedHost(host) ||
            com.example.privacy.ZenPrivacyEngine.isAdOrPopupRedirect(uri, userInitiated = false)
    }

    fun getSession(tabId: String): GeckoSession? = sessionPool[tabId]

    fun removeSession(tabId: String) {
        activeMediaSessions.remove(tabId)
        sessionLoadedUrls.remove(tabId)
        tabCallbacks.remove(tabId)
        tabAtTop.remove(tabId)
        mutedTabs.remove(tabId)
        tabSettings.remove(tabId)
        attachedViews.remove(tabId)?.let { view ->
            runCatching {
                if (view.session != null) {
                    view.releaseSession()
                }
            }
        }
        sessionPool.remove(tabId)?.let { session ->
            if (session.isOpen) {
                session.close()
            }
        }
    }

    fun setDesktopMode(context: Context, tabId: String, desktop: Boolean) {
        val previous = tabSettings[tabId]
        tabSettings[tabId] = desktop
        sessionPool[tabId]?.let { session ->
            val targetUa = if (desktop) {
                GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            }
            val targetViewport = if (desktop) {
                GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.VIEWPORT_MODE_MOBILE
            }
            val changed = (previous != null && previous != desktop) ||
                session.settings.userAgentMode != targetUa ||
                session.settings.viewportMode != targetViewport
            session.settings.userAgentMode = targetUa
            session.settings.viewportMode = targetViewport
            if (changed) {
                session.reload()
            }
        }
    }

    @Volatile private var lastUserPauseAt = 0L

    fun setMediaPlaying(tabId: String, playing: Boolean) {
        if (!playing) lastUserPauseAt = System.currentTimeMillis()
        val ms = activeMediaSessions[tabId] ?: activeMediaSessions.values.firstOrNull()
        if (ms != null) {
            if (playing) ms.play() else ms.pause()
        } else {
            sendPageUxMessage(
                JSONObject()
                    .put("type", "mediaCommand")
                    .put("action", if (playing) "play" else "pause")
            )
        }
    }

    fun seekMediaTo(tabId: String, posMs: Long) {
        val ms = activeMediaSessions[tabId] ?: activeMediaSessions.values.firstOrNull()
        if (ms != null) {
            ms.seekTo(posMs / 1000.0, false)
        }
    }

    fun seekMediaBy(tabId: String, deltaMs: Long) {
        if (pageUxPort != null) {
            sendPageUxMessage(
                JSONObject()
                    .put("type", "mediaCommand")
                    .put("action", "seekBy")
                    .put("deltaMs", deltaMs)
            )
            return
        }
        val ms = activeMediaSessions[tabId] ?: activeMediaSessions.values.firstOrNull()
        if (ms != null) {
            if (deltaMs > 0) {
                ms.seekForward()
            } else {
                ms.seekBackward()
            }
        }
    }

    fun setMediaMuted(tabId: String, muted: Boolean) {
        mutedTabs[tabId] = muted
        val ms = activeMediaSessions[tabId] ?: activeMediaSessions.values.firstOrNull()
        if (ms != null) {
            ms.muteAudio(muted)
        }
        sendPageUxMessage(JSONObject().put("type", "setMuted").put("muted", muted))
    }

    fun clearAllData(context: Context) {
        val rt = getOrCreateRuntime(context)
        rt.storageController.clearData(StorageController.ClearFlags.ALL)
        ZenScrollStore.clearAll(context)
    }

    fun resolveInstalledExtension(slugOrIdOrName: String): WebExtension? {
        val raw = slugOrIdOrName.trim()
        if (raw.isBlank()) return null
        val lower = raw.lowercase()
        val mappedId = slugToGeckoId[lower]
        if (mappedId != null) {
            installedGeckoExtensions[mappedId]?.let { return it }
        }
        installedGeckoExtensions[raw]?.let { return it }
        val normalizedNeedle = lower.replace("_", "-")
        return installedGeckoExtensions.values.firstOrNull { ext ->
            val extName = ext.metaData?.name?.trim()?.lowercase().orEmpty()
            val amoSlug = extractSlugFromAmoListingUrl(ext.metaData?.amoListingUrl)
            ext.id.equals(raw, ignoreCase = true) ||
                extName == lower ||
                amoSlug == lower ||
                amoSlug == normalizedNeedle
        }
    }

    fun hasNativeExtension(slugOrIdOrName: String): Boolean =
        resolveInstalledExtension(slugOrIdOrName) != null

    fun hasNativePopupOrOptions(slugOrIdOrName: String): Boolean {
        val ext = resolveInstalledExtension(slugOrIdOrName) ?: return false
        return extensionActions.containsKey(ext.id) || !ext.metaData?.optionsPageUrl.isNullOrBlank()
    }

    fun getNativeOptionsUrl(slugOrIdOrName: String): String? =
        resolveInstalledExtension(slugOrIdOrName)?.metaData?.optionsPageUrl?.takeIf { it.isNotBlank() }

    fun openNativeExtensionPopup(
        slugOrIdOrName: String,
        onSessionReady: (GeckoSession?) -> Unit
    ): Boolean {
        val ext = resolveInstalledExtension(slugOrIdOrName) ?: return false
        val action = extensionActions[ext.id]
        if (action != null) {
            pendingPopupCallback = onSessionReady
            mainHandler.post {
                val ok = runCatching { action.click() }.isSuccess
                if (!ok) {
                    val cb = pendingPopupCallback
                    pendingPopupCallback = null
                    cb?.invoke(null)
                }
            }
            return true
        }
        val optionsUrl = ext.metaData?.optionsPageUrl?.takeIf { it.isNotBlank() }
        if (optionsUrl != null) {
            mainHandler.post { onOpenNewTabRequested?.invoke(optionsUrl) }
            onSessionReady(null)
            return true
        }
        return false
    }

    fun openNativeExtensionOptions(slugOrIdOrName: String): Boolean {
        val optionsUrl = getNativeOptionsUrl(slugOrIdOrName) ?: return false
        mainHandler.post { onOpenNewTabRequested?.invoke(optionsUrl) }
        return true
    }

    fun installWebExtension(
        context: Context,
        uri: String,
        slug: String = "",
        onComplete: (Boolean, String?) -> Unit
    ) {
        val rt = getOrCreateRuntime(context)
        mainHandler.post {
            runCatching {
                rt.webExtensionController.install(uri).accept(
                    { ext ->
                        if (ext != null) {
                            registerUserExtension(rt, ext, slugHint = slug)
                            onComplete(true, ext.metaData?.name ?: "Extension")
                        } else {
                            onComplete(false, "No extension returned")
                        }
                    },
                    { error ->
                        onComplete(false, error?.message)
                    }
                )
            }.onFailure { err ->
                onComplete(false, err.message)
            }
        }
    }

    fun installWebExtensionFromUri(
        uri: String,
        slug: String = "",
        installMethod: String = org.mozilla.geckoview.WebExtensionController.INSTALLATION_METHOD_MANAGER,
        onComplete: (Boolean, String?, String?) -> Unit
    ) {
        val ctx = appContext ?: run {
            onComplete(false, null, "Gecko runtime not ready")
            return
        }
        val rt = getOrCreateRuntime(ctx)
        mainHandler.post {
            runCatching {
                rt.webExtensionController.install(uri, installMethod).accept(
                    { ext ->
                        if (ext != null) {
                            registerUserExtension(rt, ext, slugHint = slug)
                            onComplete(true, ext.id, ext.metaData?.name ?: "Extension")
                        } else {
                            onComplete(false, null, "No extension returned")
                        }
                    },
                    { error ->
                        onComplete(false, null, error?.message)
                    }
                )
            }.onFailure { err ->
                onComplete(false, null, err.message)
            }
        }
    }

    fun installWebExtensionFromFile(
        context: Context,
        file: java.io.File,
        slug: String = "",
        onComplete: (Boolean, String?, String?) -> Unit
    ) {
        init(context)
        val fileUri = android.net.Uri.fromFile(file).toString()
        installWebExtensionFromUri(
            uri = fileUri,
            slug = slug,
            installMethod = org.mozilla.geckoview.WebExtensionController.INSTALLATION_METHOD_FROM_FILE,
            onComplete = onComplete
        )
    }

    fun setNativeWebExtensionEnabled(slugOrId: String, enabled: Boolean) {
        val rt = runtime ?: return
        val ext = resolveInstalledExtension(slugOrId) ?: return
        mainHandler.post {
            runCatching {
                val op = if (enabled) {
                    rt.webExtensionController.enable(ext, org.mozilla.geckoview.WebExtensionController.EnableSource.USER)
                } else {
                    rt.webExtensionController.disable(ext, org.mozilla.geckoview.WebExtensionController.EnableSource.USER)
                }
                op.accept({ updated ->
                    if (updated != null) registerUserExtension(rt, updated, slugHint = slugOrId)
                }, { })
            }
        }
    }

    fun setNativeWebExtensionAllowedInPrivate(slugOrId: String, allowed: Boolean) {
        val rt = runtime ?: return
        val ext = resolveInstalledExtension(slugOrId) ?: return
        mainHandler.post {
            runCatching {
                rt.webExtensionController.setAllowedInPrivateBrowsing(ext, allowed).accept({ updated ->
                    if (updated != null) registerUserExtension(rt, updated, slugHint = slugOrId)
                }, { })
            }
        }
    }

    fun uninstallNativeWebExtension(slugOrId: String) {
        val rt = runtime ?: return
        val ext = resolveInstalledExtension(slugOrId) ?: return
        installedGeckoExtensions.remove(ext.id)
        extensionActions.remove(ext.id)
        slugToGeckoId.entries.removeIf { it.value == ext.id }
        mainHandler.post {
            runCatching { rt.webExtensionController.uninstall(ext) }
        }
    }

    @Volatile
    private var isDataSaverActive = false

    fun setDataSaver(context: Context, enabled: Boolean) {
        isDataSaverActive = enabled
        val rt = getOrCreateRuntime(context)
        if (enabled) {
            try {
                rt.webExtensionController.ensureBuiltIn(
                    "resource://android/assets/zen-datasaver/",
                    "datasaver@zenbrowser.com"
                ).accept({ }, { })
            } catch (_: Throwable) {
                try {
                    rt.webExtensionController.installBuiltIn(
                        "resource://android/assets/zen-datasaver/"
                    ).accept({ }, { })
                } catch (_: Throwable) {}
            }
        } else {
            runCatching {
                rt.webExtensionController.list().accept({ extensions ->
                    extensions?.firstOrNull { it.id == "datasaver@zenbrowser.com" }?.let { ext ->
                        rt.webExtensionController.uninstall(ext)
                    }
                }, { })
            }
        }
    }

    fun destroyAll() {
        activeMediaSessions.clear()
        sessionPool.values.forEach { session ->
            if (session.isOpen) session.close()
        }
        sessionPool.clear()
        aiPanelSessionPool.values.forEach { session ->
            if (session.isOpen) session.close()
        }
        aiPanelSessionPool.clear()
        aiPanelLoadedUrls.clear()
        attachedAiViews.clear()
    }
}
