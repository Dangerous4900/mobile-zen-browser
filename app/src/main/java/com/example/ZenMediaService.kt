package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

/** Bridge from the system (notification buttons, lock screen, Bluetooth) to the in-app web controllers. */
object ZenMediaBus {
    /** Explicit state commands avoid interpreting Android's Play and Pause as the same toggle. */
    @Volatile var onSetPlaying: ((Boolean) -> Unit)? = null
    @Volatile var onPlayPause: (() -> Unit)? = null
    @Volatile var onStop: (() -> Unit)? = null
}

/**
 * Foreground media service. It (1) registers a MediaSession so Android actually detects the playing
 * media (lock screen, notification shade, headset/Bluetooth buttons, Now Playing), and (2) keeps the
 * process alive with a wake lock so playback survives the screen turning off.
 */
class ZenMediaService : Service() {

    private var session: MediaSession? = null
    private var wakeLock: PowerManager.WakeLock? = null

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
            })
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
            ACTION_PLAY_PAUSE -> ZenMediaBus.onPlayPause?.invoke()
            ACTION_STOP -> ZenMediaBus.onStop?.invoke()
            ACTION_UPDATE -> update(
                intent.getBooleanExtra(EXTRA_PLAYING, false),
                intent.getStringExtra(EXTRA_TITLE).orEmpty(),
                intent.getStringExtra(EXTRA_DOMAIN).orEmpty()
            )
        }
        return START_NOT_STICKY
    }

    fun update(playing: Boolean, title: String, domain: String) {
        val s = session ?: return
        s.setMetadata(
            MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title.ifBlank { "Playing media" })
                .putString(MediaMetadata.METADATA_KEY_ARTIST, domain)
                .build()
        )
        s.setPlaybackState(
            PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP)
                .setState(
                    if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                    if (playing) 1f else 0f
                )
                .build()
        )
        // Renewing a bounded wake lock means it can never leak if the page dies.
        if (playing) wakeLock?.acquire(WAKE_LOCK_MS) else if (wakeLock?.isHeld == true) wakeLock?.release()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(title, domain, playing))
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
        return b.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title.ifBlank { "Zen Browser" })
            .setContentText(domain)
            .setContentIntent(open)
            .setOngoing(playing)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .addAction(
                if (playing) action(android.R.drawable.ic_media_pause, "Pause", ACTION_PLAY_PAUSE, 1)
                else action(android.R.drawable.ic_media_play, "Play", ACTION_PLAY_PAUSE, 1)
            )
            .addAction(action(android.R.drawable.ic_menu_close_clear_cancel, "Stop", ACTION_STOP, 2))
            .apply {
                setStyle(
                    Notification.MediaStyle()
                        .setMediaSession(session?.sessionToken)
                        .setShowActionsInCompactView(0, 1)
                )
            }
            .build()
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
        if (wakeLock?.isHeld == true) wakeLock?.release()
        session?.isActive = false
        session?.release()
        session = null
        super.onDestroy()
    }

    companion object {
        const val ACTION_UPDATE = "com.example.media.UPDATE"
        const val ACTION_PLAY_PAUSE = "com.example.media.PLAY_PAUSE"
        const val ACTION_STOP = "com.example.media.STOP"
        const val EXTRA_PLAYING = "playing"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DOMAIN = "domain"
        private const val CHANNEL_ID = "zen_media"
        private const val NOTIF_ID = 4201
        private const val WAKE_LOCK_MS = 10 * 60 * 1000L

        @Volatile private var instance: ZenMediaService? = null

        /** Starts/updates the service. Safe to call repeatedly and from the background. */
        fun sync(context: Context, playing: Boolean, title: String, domain: String) {
            val running = instance
            if (running != null) {
                running.update(playing, title, domain)
                return
            }
            val i = Intent(context, ZenMediaService::class.java)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_PLAYING, playing)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_DOMAIN, domain)
            try {
                context.startForegroundService(i)
            } catch (e: Exception) {
                // Background-start restrictions: we'll retry on the next state change.
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ZenMediaService::class.java))
        }
    }
}
