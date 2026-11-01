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

    private val sessionPool = ConcurrentHashMap<String, GeckoSession>()
    private val tabSettings = ConcurrentHashMap<String, Boolean>() // tabId -> isDesktopMode
    val activeMediaSessions = ConcurrentHashMap<String, MediaSession>() // tabId -> MediaSession

    fun getOrCreateRuntime(context: Context): GeckoRuntime {
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
                GeckoRuntime.create(context.applicationContext, settings).also {
                    runtime = it
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
            session
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
                callback.onOpenNewTab(uri)
                return null
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
                val now = System.currentTimeMillis()
                if (now < pipKeepPlayingUntil && now - lastUserPauseAt > 1200L) {
                    // Automatic pause caused by entering PiP, not by the user: undo it and keep reporting "playing".
                    runCatching { s.setActive(true); mediaSession.play() }
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

    /** While now < this, a pause the page/engine did on its own (window shrank, lost focus) is undone. */
    @Volatile var pipKeepPlayingUntil = 0L
    @Volatile private var lastUserPauseAt = 0L

    fun holdPlaybackForPip(tabId: String? = null, durationMs: Long = 6000L) {
        pipKeepPlayingUntil = System.currentTimeMillis() + durationMs
        // Only the playing tab: activating every background session would wake them all up.
        val targets = if (!tabId.isNullOrBlank()) listOfNotNull(sessionPool[tabId]) else activeMediaSessions.keys.mapNotNull { sessionPool[it] }
        targets.forEach { runCatching { it.setActive(true) } }
    }

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

    fun destroyAll() {
        activeMediaSessions.clear()
        sessionPool.values.forEach { session ->
            if (session.isOpen) session.close()
        }
        sessionPool.clear()
    }
}
