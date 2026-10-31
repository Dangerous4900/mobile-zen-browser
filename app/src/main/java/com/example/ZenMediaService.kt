package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.os.PowerManager

/** Bridge from the system (notification buttons, lock screen, Bluetooth) to the in-app web controllers. */
object ZenMediaBus {
    /** Explicit state commands avoid interpreting Android's Play and Pause as the same toggle. */
    @Volatile var onSetPlaying: ((Boolean) -> Unit)? = null
    @Volatile var onPlayPause: (() -> Unit)? = null
    @Volatile var onStop: (() -> Unit)? = null
    /** Seek to an absolute position (ms) / by a relative offset (ms) in the playing page media. */
    @Volatile var onSeekTo: ((Long) -> Unit)? = null
    @Volatile var onSeekBy: ((Long) -> Unit)? = null
    /** Request progress update from the web controller */
    @Volatile var onRequestProgress: (() -> Unit)? = null
}

/**
 * Foreground media service. It (1) registers a MediaSession so Android actually detects the playing
 * media (lock screen, notification shade, headset/Bluetooth buttons, Now Playing), and (2) keeps the
 * process alive with a wake lock so playback survives the screen turning off.
 */
class ZenMediaService : Service() {

    private var session: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var lastPlaying: Boolean? = null
    private var lastTitle: String? = null
    private var lastDomain: String? = null
    private var lastArtwork: Bitmap? = null
    private var positionMs: Long = PlaybackState.PLAYBACK_POSITION_UNKNOWN
    private var durationMs: Long = 0L
    private var stateRealtime: Long = 0L
    @Volatile private var userSeekTargetMs: Long = -1L
    @Volatile private var lastUserSeekRealtime: Long = 0L
    private val handler = Handler(Looper.getMainLooper())
    // Re-acquire the bounded wake lock for as long as playback continues; without this the lock
    // expired after 10 minutes and the phone slept mid-playback.
    private val renewWakeLock = object : Runnable {
        override fun run() {
            if (lastPlaying == true) {
                wakeLock?.acquire(WAKE_LOCK_MS)
                handler.postDelayed(this, WAKE_LOCK_RENEW_MS)
            }
        }
    }
    private val pollProgressRunnable = object : Runnable {
        override fun run() {
            if (lastPlaying == true) {
                ZenMediaBus.onRequestProgress?.invoke()
                handler.postDelayed(this, 1500L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createChannel()
        session = MediaSession(this, "ZenBrowserMedia").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { ZenMediaBus.onSetPlaying?.invoke(true) ?: ZenMediaBus.onPlayPause?.invoke() }
                override fun onPause() { ZenMediaBus.onSetPlaying?.invoke(false) ?: ZenMediaBus.onPlayPause?.invoke() }
                override fun onStop() { ZenMediaBus.onStop?.invoke() }
                override fun onSeekTo(pos: Long) {
                    recordSeekPosition(pos)
                    ZenMediaBus.onSeekTo?.invoke(pos)
                }
                override fun onRewind() {
                    recordRelativeSeek(-SKIP_MS)
                    ZenMediaBus.onSeekBy?.invoke(-SKIP_MS)
                }
                override fun onFastForward() {
                    recordRelativeSeek(SKIP_MS)
                    ZenMediaBus.onSeekBy?.invoke(SKIP_MS)
                }
                override fun onCustomAction(action: String, extras: android.os.Bundle?) {
                    when (action) {
                        ACTION_REWIND -> {
                            recordRelativeSeek(-SKIP_MS)
                            ZenMediaBus.onSeekBy?.invoke(-SKIP_MS)
                        }
                        ACTION_FORWARD -> {
                            recordRelativeSeek(SKIP_MS)
                            ZenMediaBus.onSeekBy?.invoke(SKIP_MS)
                        }
                    }
                }
            })
            setSessionActivity(
                PendingIntent.getActivity(
                    this@ZenMediaService, 0,
                    Intent(this@ZenMediaService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            isActive = true
        }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ZenBrowser:MediaPlayback").apply {
            setReferenceCounted(false)
        }
        // Must call startForeground promptly after startForegroundService().
        promote(buildNotification("Zen Browser", "", false))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> ZenMediaBus.onSetPlaying?.invoke(true) ?: ZenMediaBus.onPlayPause?.invoke()
            ACTION_PAUSE -> ZenMediaBus.onSetPlaying?.invoke(false) ?: ZenMediaBus.onPlayPause?.invoke()
            ACTION_PLAY_PAUSE -> ZenMediaBus.onPlayPause?.invoke()
            ACTION_STOP -> ZenMediaBus.onStop?.invoke()
            ACTION_REWIND -> {
                recordRelativeSeek(-SKIP_MS)
                ZenMediaBus.onSeekBy?.invoke(-SKIP_MS)
            }
            ACTION_FORWARD -> {
                recordRelativeSeek(SKIP_MS)
                ZenMediaBus.onSeekBy?.invoke(SKIP_MS)
            }
            ACTION_UPDATE -> update(
                intent.getBooleanExtra(EXTRA_PLAYING, false),
                intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                intent.getStringExtra(EXTRA_DOMAIN).orEmpty(),
                activeArtwork
            )
        }
        // Keep the media playback service alive if Android reclaims the service.
        return START_STICKY
    }

    fun update(playing: Boolean, title: String, domain: String, artwork: Bitmap? = null) {
        if (artwork != null) {
            lastArtwork = artwork
        }
        // Always (re)arm the wake lock first: this must not be skipped by the dedupe below.
        if (playing) {
            wakeLock?.acquire(WAKE_LOCK_MS)
            handler.removeCallbacks(renewWakeLock)
            handler.postDelayed(renewWakeLock, WAKE_LOCK_RENEW_MS)
            handler.removeCallbacks(pollProgressRunnable)
            handler.postDelayed(pollProgressRunnable, 1000L)
        } else {
            handler.removeCallbacks(renewWakeLock)
            handler.removeCallbacks(pollProgressRunnable)
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        val cleanTitle = title.ifBlank { "Zen Browser" }
        val cleanDomain = domain.ifBlank { "" }
        if (lastPlaying == playing && lastTitle == cleanTitle && lastDomain == cleanDomain && artwork == null) return
        val wasPlaying = lastPlaying
        lastPlaying = playing
        lastTitle = cleanTitle
        lastDomain = cleanDomain
        if (wasPlaying != playing) {
            val now = SystemClock.elapsedRealtime()
            // Freeze the extrapolated position at the moment of pause/resume so the seek bar doesn't jump.
            if (wasPlaying == true && positionMs >= 0) positionMs += now - stateRealtime
            stateRealtime = now
        }

        publishMetadata()
        applyPlaybackState()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(cleanTitle, cleanDomain, playing))
    }

    /** Called with the page's real position/duration so Android can show a seek bar and scrub. */
    fun progress(posMs: Long, durMs: Long) {
        val playing = lastPlaying ?: return
        var durChanged = false
        if (durMs > 0) {
            durChanged = durationMs == 0L || kotlin.math.abs(durMs - durationMs) > 1000L
            durationMs = durMs
            if (durChanged) publishMetadata()
        }

        val now = SystemClock.elapsedRealtime()

        // Guard against post-seek snapshot glitch: when the user explicitly scrubs or skips,
        // page timers / background intervals may report old pre-seek positions while the video
        // is demuxing. Ignore polls that haven't reached the seek target within a 3.5s window.
        if (userSeekTargetMs >= 0L) {
            val elapsedSinceSeek = now - lastUserSeekRealtime
            if (elapsedSinceSeek < 3500L) {
                val diffFromTarget = kotlin.math.abs(posMs - userSeekTargetMs)
                if (diffFromTarget > 3000L) {
                    return
                }
            }
            userSeekTargetMs = -1L
        }

        val expected = if (playing && positionMs >= 0 && stateRealtime > 0) {
            positionMs + (now - stateRealtime)
        } else {
            positionMs
        }
        val drift = kotlin.math.abs(expected - posMs)

        if (positionMs < 0 || drift > 2000L || durChanged) {
            positionMs = posMs
            stateRealtime = now
            applyPlaybackState()
        }
    }

    /** Update MediaSession's timeline immediately when the user scrubs the notification or PiP. */
    private fun recordSeekPosition(requestedMs: Long) {
        val maxPosition = if (durationMs > 0) (durationMs - 1).coerceAtLeast(0L) else Long.MAX_VALUE
        positionMs = requestedMs.coerceIn(0L, maxPosition)
        val now = SystemClock.elapsedRealtime()
        stateRealtime = now
        lastUserSeekRealtime = now
        userSeekTargetMs = positionMs
        applyPlaybackState()
    }

    /** Keep the system seek bar aligned with explicit +/-10 second commands. */
    private fun recordRelativeSeek(deltaMs: Long) {
        val now = SystemClock.elapsedRealtime()
        val current = if (lastPlaying == true && positionMs >= 0 && stateRealtime > 0) {
            positionMs + (now - stateRealtime)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        val target = (current + deltaMs).coerceAtLeast(0L)
        recordSeekPosition(if (durationMs > 0) target.coerceAtMost((durationMs - 1).coerceAtLeast(0L)) else target)
    }

    private fun publishMetadata() {
        val s = session ?: return
        val effectiveDuration = if (durationMs > 0) durationMs else 1000L * 60L * 30L
        val builder = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, lastTitle.orEmpty().ifBlank { "Playing media" })
            .putString(MediaMetadata.METADATA_KEY_ARTIST, lastDomain.orEmpty())
            .putLong(MediaMetadata.METADATA_KEY_DURATION, effectiveDuration)
        lastArtwork?.let { art ->
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, art)
            builder.putBitmap(MediaMetadata.METADATA_KEY_ART, art)
        }
        s.setMetadata(builder.build())
    }

    private fun applyPlaybackState() {
        val s = session ?: return
        val playing = lastPlaying == true
        val pos = positionMs.coerceAtLeast(0L)
        val actions = PlaybackState.ACTION_PLAY or
            PlaybackState.ACTION_PAUSE or
            PlaybackState.ACTION_PLAY_PAUSE or
            PlaybackState.ACTION_STOP or
            PlaybackState.ACTION_SEEK_TO or
            PlaybackState.ACTION_REWIND or
            PlaybackState.ACTION_FAST_FORWARD
        val b = PlaybackState.Builder()
            .setActions(actions)
            .setState(
                if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                pos,
                if (playing) 1f else 0f,
                if (stateRealtime > 0) stateRealtime else SystemClock.elapsedRealtime()
            )
            .addCustomAction(PlaybackState.CustomAction.Builder(ACTION_REWIND, "Back 10 seconds", android.R.drawable.ic_media_rew).build())
            .addCustomAction(PlaybackState.CustomAction.Builder(ACTION_FORWARD, "Forward 10 seconds", android.R.drawable.ic_media_ff).build())
        s.setPlaybackState(b.build())
    }

    private fun buildNotification(title: String, domain: String, playing: Boolean): Notification {
        fun action(icon: Int, label: String, act: String, code: Int): Notification.Action {
            val pi = PendingIntent.getService(
                this, code, Intent(this, ZenMediaService::class.java).setAction(act),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return Notification.Action.Builder(Icon.createWithResource(this, icon), label, pi).build()
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val b = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(this, CHANNEL_ID) else Notification.Builder(this)
        val playPause = if (playing) action(android.R.drawable.ic_media_pause, "Pause", ACTION_PAUSE, 101)
        else action(android.R.drawable.ic_media_play, "Play", ACTION_PLAY, 102)
        b.setSmallIcon(R.drawable.ic_stat_zen_media)
            .setContentTitle(title.ifBlank { "Zen Browser" })
            .setContentText(domain)
            .setContentIntent(open)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(Notification.CATEGORY_TRANSPORT)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
        lastArtwork?.let { art ->
            b.setLargeIcon(art)
        }
        b.addAction(action(android.R.drawable.ic_media_rew, "Back 10 seconds", ACTION_REWIND, 103))
        b.addAction(playPause)
        b.addAction(action(android.R.drawable.ic_media_ff, "Forward 10 seconds", ACTION_FORWARD, 104))
        b.addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", ACTION_STOP, 105))
        b.setStyle(
            Notification.MediaStyle()
                .setMediaSession(session?.sessionToken)
                .setShowActionsInCompactView(0, 1, 2)
        )
        return b.build()
    }

    private fun promote(n: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Media playback", NotificationManager.IMPORTANCE_LOW).apply {
                    setShowBadge(false)
                }
            )
        }
    }

    override fun onDestroy() {
        instance = null
        handler.removeCallbacks(renewWakeLock)
        if (wakeLock?.isHeld == true) wakeLock?.release()
        session?.isActive = false
        session?.release()
        session = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_UPDATE = "com.example.media.UPDATE"
        const val ACTION_PLAY = "com.example.media.PLAY"
        const val ACTION_PAUSE = "com.example.media.PAUSE"
        const val ACTION_PLAY_PAUSE = "com.example.media.PLAY_PAUSE"
        const val ACTION_STOP = "com.example.media.STOP"
        const val ACTION_REWIND = "com.example.media.REWIND"
        const val ACTION_FORWARD = "com.example.media.FORWARD"
        private const val SKIP_MS = 10_000L
        const val EXTRA_PLAYING = "playing"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DOMAIN = "domain"
        private const val CHANNEL_ID = "zen_media"
        private const val NOTIF_ID = 4201
        private const val WAKE_LOCK_MS = 15 * 60 * 1000L
        private const val WAKE_LOCK_RENEW_MS = 5 * 60 * 1000L

        @Volatile private var instance: ZenMediaService? = null
        @Volatile var activeArtwork: Bitmap? = null

        /** Starts/updates the service. Safe to call repeatedly and from the background. */
        fun sync(context: Context, playing: Boolean, title: String, domain: String, artwork: Bitmap? = null) {
            if (artwork != null) {
                activeArtwork = artwork
            }
            val running = instance
            if (running != null) {
                running.update(playing, title, domain, artwork ?: activeArtwork)
                return
            }
            val i = Intent(context, ZenMediaService::class.java)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_PLAYING, playing)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_DOMAIN, domain)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i)
                } else {
                    context.startService(i)
                }
            } catch (e: Exception) {
                // Background-start restrictions: we'll retry on the next state change.
            }
        }

        /** Latest page position/duration (ms); duration 0 means live/unknown (no seek bar). */
        fun progress(posMs: Long, durMs: Long) {
            instance?.progress(posMs, durMs)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ZenMediaService::class.java))
        }
    }
}
