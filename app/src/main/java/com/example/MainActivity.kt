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
import androidx.compose.runtime.setValue
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
    private var pipWindow by androidx.compose.runtime.mutableStateOf(false)
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var resumeMediaAfterPip = false

    /** GeckoView/WebView can pause the page when the window shrinks; keep nudging playback until it sticks. */
    private fun resumeMediaSoon() {
        if (!resumeMediaAfterPip) return
        listOf(100L, 400L, 900L, 1800L, 3000L).forEach { delayMs ->
            mainHandler.postDelayed({
                if (!resumeMediaAfterPip) return@postDelayed
                val st = viewModelRef?.uiState?.value ?: return@postDelayed
                val id = st.mediaPlaybackState.tabId.ifBlank { st.focusedTab?.id.orEmpty() }
                if (id.isNotBlank()) {
                    com.example.gecko.ZenGeckoManager.setMediaPlaying(id, true)
                    com.example.ui.components.ZenWebViewManager.get(id)?.let { wv ->
                        wv.resumeTimers()
                        wv.evaluateJavascript("document.querySelectorAll('video,audio').forEach(function(m){if(m.paused&&!m.ended){m.play().catch(function(){});}})", null)
                    }
                }
            }, delayMs)
        }
    }

    private val database: ZenDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ZenDatabase::class.java,
            "zen_browser.db"
        ).fallbackToDestructiveMigration(true).build()
    }

    private val repository: ZenRepository by lazy {
        ZenRepository(database.zenDao(), applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.privacy.ZenAdBlockList.load(applicationContext)

        val initialExternalUrl = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.dataString

        setContent {
            val browserViewModel: ZenBrowserViewModel = viewModel(
                factory = ZenBrowserViewModel.Factory(repository)
            )
            viewModelRef = browserViewModel
            // Keep collecting while STOPPED too: in PiP the activity can be stopped, and a lifecycle-gated
            // collector then freezes on the last pre-PiP state (sidebar open, full layout) inside the tiny window.
            val uiState by browserViewModel.uiState.collectAsStateWithLifecycle(
                minActiveState = androidx.lifecycle.Lifecycle.State.CREATED
            )

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
            LaunchedEffect(media.isActive, media.isPlaying, media.title, media.domain, media.artwork, uiState.sidebarConfig.backgroundPlayEnabled) {
                if (media.isActive && uiState.sidebarConfig.backgroundPlayEnabled) {
                    ZenMediaService.sync(applicationContext, media.isPlaying, media.title, media.domain, media.artwork)
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
                // The window's real PiP state is Compose snapshot state set synchronously from the
                // Activity callbacks, so the PiP layout can never lag behind the ViewModel flow.
                val winCfg = androidx.compose.ui.platform.LocalConfiguration.current
                val tinyWindow = winCfg.screenHeightDp < 260 || winCfg.screenWidthDp < 240
                val screenState = if (pipWindow || tinyWindow) {
                    uiState.copy(isInPictureInPictureMode = true, isSidebarOpen = false)
                } else {
                    if (uiState.isInPictureInPictureMode) {
                        uiState.copy(isInPictureInPictureMode = false, isSidebarOpen = false)
                    } else uiState
                }
                ZenBrowserScreen(
                    uiState = screenState,
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

    override fun onPause() {
        super.onPause()
        // Entering PiP pauses the activity; make sure nothing can pop the panel open meanwhile.
        if (isInPictureInPictureMode || pipWindow) viewModelRef?.setSidebarOpen(false)
        if (resumeMediaAfterPip) resumeMediaSoon()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        if (isFinishing) ZenMediaService.stop(applicationContext)
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        viewModelRef?.onLeavingApp()
        val state = viewModelRef?.uiState?.value ?: return
        val media = state.mediaPlaybackState
        val pipEnabled = state.sidebarConfig.playInWindowPipEnabled
        if (canUsePip() && !isInPictureInPictureMode && media.isPlaying && pipEnabled) {
            resumeMediaAfterPip = true
            viewModelRef?.setSidebarOpen(false)
            pipWindow = true
            viewModelRef?.setPictureInPictureMode(true)
            try {
                val entered = enterPictureInPictureMode(buildPipParams(autoEnter = true, isPlaying = true))
                if (!entered && !isInPictureInPictureMode) {
                    pipWindow = false
                    viewModelRef?.setPictureInPictureMode(false)
                }
            } catch (_: Exception) {
                pipWindow = false
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
        browserViewModel.onLeavingApp()
        resumeMediaAfterPip = browserViewModel.uiState.value.mediaPlaybackState.isPlaying
        pipWindow = true
        browserViewModel.setPictureInPictureMode(true)
        return try {
            val isPlaying = browserViewModel.uiState.value.mediaPlaybackState.isPlaying
            val entered = enterPictureInPictureMode(buildPipParams(autoEnter = false, isPlaying = isPlaying))
            if (!entered && !isInPictureInPictureMode) {
                pipWindow = false
                browserViewModel.setPictureInPictureMode(false)
            }
            entered
        } catch (_: Exception) {
            pipWindow = false
            browserViewModel.setPictureInPictureMode(false)
            false
        }
    }

    // Android 12+: fires as the transition starts, before the window shrinks, so the sidebar is
    // gone from the very first PiP frame instead of showing at half width inside the PiP window.
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.S)
    override fun onPictureInPictureUiStateChanged(pipState: android.app.PictureInPictureUiState) {
        super.onPictureInPictureUiStateChanged(pipState)
        if (pipState.isTransitioningToPip) {
            viewModelRef?.onLeavingApp()
            if (viewModelRef?.uiState?.value?.mediaPlaybackState?.isPlaying == true) resumeMediaAfterPip = true
            pipWindow = true
            viewModelRef?.setSidebarOpen(false)
            viewModelRef?.setPictureInPictureMode(true)
        }
    }

    // Window resizes are handled here (configChanges); re-sync in case a PiP callback was missed.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // One-directional on purpose: during the enter transition the system still reports "not PiP"
        // while we've already switched layouts. Clearing here made the layout flap and stopped media.
        // Leaving PiP is handled only by onPictureInPictureModeChanged(false).
        if (isInPictureInPictureMode && !pipWindow) {
            pipWindow = true
            viewModelRef?.setPictureInPictureMode(true)
            viewModelRef?.setSidebarOpen(false)
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipWindow = isInPictureInPictureMode
        viewModelRef?.setPictureInPictureMode(isInPictureInPictureMode)
        // Always ensure sidebar is kept closed during PiP and when maximizing from PiP
        viewModelRef?.setSidebarOpen(false)
        resumeMediaSoon()
        if (!isInPictureInPictureMode) mainHandler.postDelayed({ resumeMediaAfterPip = false }, 3500L)
        val curMedia = viewModelRef?.uiState?.value?.mediaPlaybackState
        if (curMedia?.isPlaying == true) {
            val activeTabId = curMedia.tabId.ifBlank {
                viewModelRef?.uiState?.value?.focusedTab?.id
            }
            if (!activeTabId.isNullOrBlank()) {
                com.example.gecko.ZenGeckoManager.setMediaPlaying(activeTabId, true)
            }
        }
    }
}
