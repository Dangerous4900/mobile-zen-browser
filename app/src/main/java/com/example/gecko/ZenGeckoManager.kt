package com.example.gecko

import android.content.Context
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.MediaSession
import org.mozilla.geckoview.StorageController
import java.util.concurrent.ConcurrentHashMap

data class GeckoMediaState(
    val hasMedia: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = ""
)

interface ZenGeckoCallback {
    fun onLocationChange(url: String, title: String?)
    fun onProgressChange(progress: Int, canGoBack: Boolean, canGoForward: Boolean)
    fun onTrackerBlocked(sourceUrl: String, category: String)
    fun onMediaStateChange(isActive: Boolean, isPlaying: Boolean, title: String, artwork: android.graphics.Bitmap? = null)
    fun onOpenNewTab(url: String)
}

object ZenGeckoManager {
    @Volatile
    private var runtime: GeckoRuntime? = null
    @Volatile private var appContext: Context? = null

    /** Set by the UI: called on the main thread when a page download finished or failed. */
    @Volatile var downloadSink: ((fileName: String, url: String, mimeType: String, bytes: Long, ok: Boolean) -> Unit)? = null

    /** Set by MainActivity: opens the system file chooser for <input type=file> and reports the chosen uris. */
    @Volatile var filePicker: ((multiple: Boolean, mimeTypes: Array<String>, onResult: (List<android.net.Uri>) -> Unit) -> Unit)? = null

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private val sessionPool = ConcurrentHashMap<String, GeckoSession>()
    private val tabSettings = ConcurrentHashMap<String, Boolean>() // tabId -> isDesktopMode
    val activeMediaSessions = ConcurrentHashMap<String, MediaSession>() // tabId -> MediaSession

    fun getOrCreateRuntime(context: Context): GeckoRuntime {
        appContext = context.applicationContext
        return runtime ?: synchronized(this) {
            runtime ?: run {
                val settings = GeckoRuntimeSettings.Builder()
                    .aboutConfigEnabled(true)
                    .consoleOutput(true)
                    .contentBlocking(
                        ContentBlocking.Settings.Builder()
                            .antiTracking(ContentBlocking.AntiTracking.DEFAULT)
                            .safeBrowsing(ContentBlocking.SafeBrowsing.DEFAULT)
                            .enhancedTrackingProtectionLevel(ContentBlocking.EtpLevel.STRICT)
                            .cookieBehavior(ContentBlocking.CookieBehavior.ACCEPT_NON_TRACKERS)
                            .build()
                    )
                    .build()
                GeckoRuntime.create(context.applicationContext, settings).also { rt ->
                    runtime = rt
                    try {
                        rt.webExtensionController.ensureBuiltIn(
                            "resource://android/assets/extensions/video_bg_play/",
                            "video-bg-play@zenbrowser.com"
                        ).accept({ }, { })
                    } catch (_: Throwable) {
                        try {
                            rt.webExtensionController.installBuiltIn(
                                "resource://android/assets/extensions/video_bg_play/"
                            ).accept({ }, { })
                        } catch (_: Throwable) {}
                    }
                }
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
            val settings = GeckoSessionSettings.Builder()
                .usePrivateMode(isPrivate)
                .useTrackingProtection(true)
                .suspendMediaWhenInactive(false)
                .userAgentMode(
                    if (isDesktop) GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
                    else GeckoSessionSettings.USER_AGENT_MODE_MOBILE
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
        var currentCanBack = false
        var currentCanForward = false
        var currentTitle: String? = null

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
                if (shouldBlockPopup(uri)) return GeckoResult.fromValue(null)
                lastPopupAt = System.currentTimeMillis()
                callback.onOpenNewTab(uri)
                return null
            }

            // Tab-under / redirect ads: block navigations to ad hosts and app-launching schemes the user didn't tap.
            override fun onLoadRequest(
                s: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
                val uri = request.uri
                val userInitiated = System.currentTimeMillis() - lastUserTouchAt < USER_GESTURE_WINDOW_MS
                val scheme = runCatching { android.net.Uri.parse(uri).scheme?.lowercase() }.getOrNull()
                val web = scheme == "http" || scheme == "https" || scheme == "about" || scheme == "data" || scheme == "blob" || scheme == "resource" || scheme == "moz-extension"
                val host = runCatching { android.net.Uri.parse(uri).host }.getOrNull()
                val deny = (!web && !userInitiated) ||
                    (web && !userInitiated && com.example.privacy.ZenAdBlockList.isBlockedHost(host))
                return if (deny) GeckoResult.fromValue(org.mozilla.geckoview.AllowOrDeny.DENY) else null
            }
        }

        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(s: GeckoSession, url: String) {
                callback.onProgressChange(15, currentCanBack, currentCanForward)
            }

            override fun onPageStop(s: GeckoSession, success: Boolean) {
                callback.onProgressChange(100, currentCanBack, currentCanForward)
            }

            override fun onProgressChange(s: GeckoSession, progress: Int) {
                callback.onProgressChange(progress, currentCanBack, currentCanForward)
            }
        }

        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(s: GeckoSession, title: String?) {
                currentTitle = title
            }

            // Anything the page serves as a download (attachments, archives, apk, pdf "save", ...).
            override fun onExternalResponse(s: GeckoSession, response: org.mozilla.geckoview.WebResponse) {
                val ctx = appContext ?: return
                val body = response.body ?: return
                val headers = response.headers
                val mime = headers.entries.firstOrNull { it.key.equals("content-type", true) }?.value?.substringBefore(';')?.trim().orEmpty()
                val name = com.example.downloads.ZenDownloader.fileNameFor(response.uri, headers, mime)
                com.example.downloads.ZenDownloader.save(ctx, name, mime, body) { ok, bytes ->
                    mainHandler.post { downloadSink?.invoke(name, response.uri, mime, bytes, ok) }
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
                callback.onMediaStateChange(isActive = true, isPlaying = true, title = currentTitle ?: "Media")
            }

            override fun onPause(s: GeckoSession, mediaSession: MediaSession) {
                activeMediaSessions[tabId] = mediaSession
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

    private const val USER_GESTURE_WINDOW_MS = 1500L
    @Volatile var lastUserTouchAt = 0L
    @Volatile private var lastPopupAt = 0L

    fun noteUserTouch() { lastUserTouchAt = System.currentTimeMillis() }

    /** A page may open a new tab only right after a real tap, never to an ad host, and not repeatedly. */
    private fun shouldBlockPopup(uri: String): Boolean {
        val now = System.currentTimeMillis()
        val host = runCatching { android.net.Uri.parse(uri).host }.getOrNull()
        return now - lastUserTouchAt > USER_GESTURE_WINDOW_MS ||
            now - lastPopupAt < 1000L ||
            com.example.privacy.ZenAdBlockList.isBlockedHost(host)
    }

    fun getSession(tabId: String): GeckoSession? = sessionPool[tabId]

    fun removeSession(tabId: String) {
        activeMediaSessions.remove(tabId)
        sessionLoadedUrls.remove(tabId)
        sessionPool.remove(tabId)?.let { session ->
            if (session.isOpen) {
                session.close()
            }
        }
    }

    fun setDesktopMode(context: Context, tabId: String, desktop: Boolean) {
        tabSettings[tabId] = desktop
        sessionPool[tabId]?.let { session ->
            session.settings.userAgentMode = if (desktop) {
                GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            } else {
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE
            }
            session.reload()
        }
    }

    @Volatile private var lastUserPauseAt = 0L

    fun setMediaPlaying(tabId: String, playing: Boolean) {
        if (!playing) lastUserPauseAt = System.currentTimeMillis()
        val ms = activeMediaSessions[tabId] ?: activeMediaSessions.values.firstOrNull()
        if (ms != null) {
            if (playing) ms.play() else ms.pause()
        }
    }

    fun seekMediaTo(tabId: String, posMs: Long) {
        val ms = activeMediaSessions[tabId] ?: activeMediaSessions.values.firstOrNull()
        if (ms != null) {
            ms.seekTo(posMs / 1000.0, false)
        }
    }

    fun seekMediaBy(tabId: String, deltaMs: Long) {
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
        val ms = activeMediaSessions[tabId] ?: activeMediaSessions.values.firstOrNull()
        if (ms != null) {
            ms.muteAudio(muted)
        }
    }

    fun clearAllData(context: Context) {
        val rt = getOrCreateRuntime(context)
        rt.storageController.clearData(StorageController.ClearFlags.ALL)
    }

    fun installWebExtension(
        context: Context,
        uri: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        val rt = getOrCreateRuntime(context)
        rt.webExtensionController.install(uri).accept(
            { ext ->
                onComplete(true, ext?.metaData?.name ?: "Extension")
            },
            { error ->
                onComplete(false, error?.message)
            }
        )
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
                    "datasaver@zenbrowser"
                ).accept({ }, { })
            } catch (_: Throwable) {
                try {
                    rt.webExtensionController.installBuiltIn(
                        "resource://android/assets/zen-datasaver/"
                    ).accept({ }, { })
                } catch (_: Throwable) {}
            }
        } else {
            try {
                rt.webExtensionController.ensureBuiltIn(
                    "resource://android/assets/zen-datasaver/",
                    "datasaver@zenbrowser"
                ).accept({ ext ->
                    if (ext != null) {
                        rt.webExtensionController.uninstall(ext)
                    }
                }, { })
            } catch (_: Throwable) {}
        }
    }

    fun destroyAll() {
        activeMediaSessions.clear()
        sessionPool.values.forEach { session ->
            if (session.isOpen) session.close()
        }
        sessionPool.clear()
    }
}
