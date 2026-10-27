package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.ZenDatabase
import com.example.data.ZenRepository
import com.example.ui.ZenBrowserScreen
import com.example.ui.ZenBrowserViewModel
import com.example.ui.theme.ZenBrowserTheme

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.util.Rational

class MainActivity : ComponentActivity() {

    private var viewModelRef: ZenBrowserViewModel? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    private val database: ZenDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ZenDatabase::class.java,
            "zen_browser.db"
        ).fallbackToDestructiveMigration(false).build()
    }

    private val repository: ZenRepository by lazy {
        ZenRepository(database.zenDao(), applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialExternalUrl = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString

        setContent {
            val browserViewModel: ZenBrowserViewModel = viewModel(
                factory = ZenBrowserViewModel.Factory(repository)
            )
            viewModelRef = browserViewModel
            val uiState by browserViewModel.uiState.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            applicationContext, android.Manifest.permission.POST_NOTIFICATIONS
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            LaunchedEffect(uiState.mediaPlaybackState.isPlaying) {
                if (uiState.mediaPlaybackState.isPlaying) {
                    acquireWakeLock()
                } else {
                    releaseWakeLock()
                }
            }

            LaunchedEffect(uiState.sidebarConfig.hideStatusBarFullscreen, uiState.isCompactFullscreen) {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                val hideStatusBar = uiState.sidebarConfig.hideStatusBarFullscreen || uiState.isCompactFullscreen
                if (hideStatusBar) {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
            }

            LaunchedEffect(initialExternalUrl) {
                if (!initialExternalUrl.isNullOrBlank()) {
                    browserViewModel.createNewTab(initialExternalUrl)
                }
            }

            // Publish playback to Android (MediaSession + foreground service) so media is detected,
            // controllable from the shade/lock screen/headset, and survives screen-off.
            val media = uiState.mediaPlaybackState
            LaunchedEffect(media.isActive, media.isPlaying, media.title, media.domain, uiState.sidebarConfig.backgroundPlayEnabled) {
                if (media.isActive && uiState.sidebarConfig.backgroundPlayEnabled) {
                    ZenMediaService.sync(applicationContext, media.isPlaying, media.title, media.domain)
                } else {
                    ZenMediaService.stop(applicationContext)
                }
            }

            LaunchedEffect(uiState.mediaPlaybackState.isPlaying, uiState.sidebarConfig.playInWindowPipEnabled) {
                if (canUsePip()) {
                    val shouldAutoEnter = uiState.mediaPlaybackState.isPlaying && uiState.sidebarConfig.playInWindowPipEnabled
                    setPictureInPictureParams(buildPipParams(autoEnter = shouldAutoEnter, isPlaying = uiState.mediaPlaybackState.isPlaying))
                }
            }

            ZenBrowserTheme(
                accentTheme = uiState.activeAccentTheme
            ) {
                ZenBrowserScreen(
                    uiState = uiState,
                    viewModel = browserViewModel,
                    onEnterPip = { enterPipManual() }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Keep page JS timers + audio running when the screen turns off / app is backgrounded.
        val media = viewModelRef?.uiState?.value?.mediaPlaybackState
        if (media?.isActive == true && viewModelRef?.uiState?.value?.sidebarConfig?.backgroundPlayEnabled != false) {
            com.example.ui.components.ZenWebViewManager.getAllWebViews().values.forEach { it.resumeTimers() }
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock == null) {
                val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
                wakeLock = powerManager.newWakeLock(
                    android.os.PowerManager.PARTIAL_WAKE_LOCK,
                    "ZenBrowser::MediaPlaybackWakeLock"
                ).apply {
                    setReferenceCounted(false)
                }
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L)
            }
        } catch (_: Exception) {}
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        if (isFinishing) ZenMediaService.stop(applicationContext)
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModelRef?.uiState?.value ?: return
        val media = state.mediaPlaybackState
        val pipEnabled = state.sidebarConfig.playInWindowPipEnabled
        if (canUsePip() && !isInPictureInPictureMode && media.isPlaying && pipEnabled) {
            viewModelRef?.setSidebarOpen(false)
            viewModelRef?.setPictureInPictureMode(true)
            try {
                val entered = enterPictureInPictureMode(buildPipParams(autoEnter = true, isPlaying = true))
                if (!entered && !isInPictureInPictureMode) {
                    viewModelRef?.setPictureInPictureMode(false)
                }
            } catch (_: Exception) {
                viewModelRef?.setPictureInPictureMode(false)
            }
        }
    }

    private fun canUsePip(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun buildPipParams(
        autoEnter: Boolean,
        isPlaying: Boolean = viewModelRef?.uiState?.value?.mediaPlaybackState?.isPlaying == true
    ): PictureInPictureParams {
        val b = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val actions = ArrayList<android.app.RemoteAction>()
            val rewPi = PendingIntent.getService(
                this, 103,
                Intent(this, ZenMediaService::class.java).setAction(ZenMediaService.ACTION_REWIND),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                android.app.RemoteAction(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_rew),
                    "Rewind 10s",
                    "Rewind 10 seconds",
                    rewPi
                )
            )

            val playPausePi = PendingIntent.getService(
                this, if (isPlaying) 102 else 101,
                Intent(this, ZenMediaService::class.java).setAction(if (isPlaying) ZenMediaService.ACTION_PAUSE else ZenMediaService.ACTION_PLAY),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                android.app.RemoteAction(
                    android.graphics.drawable.Icon.createWithResource(
                        this,
                        if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
                    ),
                    if (isPlaying) "Pause" else "Play",
                    if (isPlaying) "Pause" else "Play",
                    playPausePi
                )
            )

            val ffPi = PendingIntent.getService(
                this, 104,
                Intent(this, ZenMediaService::class.java).setAction(ZenMediaService.ACTION_FORWARD),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            actions.add(
                android.app.RemoteAction(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_ff),
                    "Forward 10s",
                    "Forward 10 seconds",
                    ffPi
                )
            )
            b.setActions(actions)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.setAutoEnterEnabled(autoEnter)
            b.setSeamlessResizeEnabled(true)
        }
        return b.build()
    }

    fun enterPipManual(): Boolean {
        if (!canUsePip() || isInPictureInPictureMode) return false
        val browserViewModel = viewModelRef ?: return false
        browserViewModel.setSidebarOpen(false)
        browserViewModel.setPictureInPictureMode(true)
        return try {
            val isPlaying = browserViewModel.uiState.value.mediaPlaybackState.isPlaying
            val entered = enterPictureInPictureMode(buildPipParams(autoEnter = false, isPlaying = isPlaying))
            if (!entered && !isInPictureInPictureMode) {
                browserViewModel.setPictureInPictureMode(false)
            }
            entered
        } catch (_: Exception) {
            browserViewModel.setPictureInPictureMode(false)
            false
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModelRef?.setPictureInPictureMode(isInPictureInPictureMode)
        if (isInPictureInPictureMode) {
            viewModelRef?.setSidebarOpen(false)
        }
    }
}
