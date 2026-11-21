package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.ZenDatabase
import com.example.data.ZenRepository
import com.example.ui.ZenBrowserScreen
import com.example.ui.ZenBrowserViewModel
import com.example.ui.theme.ZenBrowserTheme

class MainActivity : FragmentActivity() {

    private var viewModelRef: ZenBrowserViewModel? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var biometricErrorMessage by mutableStateOf<String?>(null)
    private var downEdgeCandidate: Int = com.example.ui.BACK_EDGE_NONE
    private var recentCancelledSwipeEdge: Int = com.example.ui.BACK_EDGE_NONE
    private var recentCancelledSwipeAtMs: Long = 0L

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (isInPictureInPictureMode) {
            downEdgeCandidate = com.example.ui.BACK_EDGE_NONE
            recentCancelledSwipeEdge = com.example.ui.BACK_EDGE_NONE
            recentCancelledSwipeAtMs = 0L
            return super.dispatchTouchEvent(ev)
        }
        if (ev != null) {
            val edgePx = 48f * resources.displayMetrics.density
            val windowWidth = window?.decorView?.width?.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels
            when (ev.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    val x = ev.rawX
                    downEdgeCandidate = when {
                        x <= edgePx -> androidx.activity.BackEventCompat.EDGE_LEFT
                        x >= (windowWidth - edgePx) -> androidx.activity.BackEventCompat.EDGE_RIGHT
                        else -> com.example.ui.BACK_EDGE_NONE
                    }
                    if (downEdgeCandidate != com.example.ui.BACK_EDGE_NONE) {
                        com.example.gecko.ZenGeckoManager.suppressContextMenuFor(1800L)
                    }
                }
                android.view.MotionEvent.ACTION_CANCEL -> {
                    if (downEdgeCandidate != com.example.ui.BACK_EDGE_NONE) {
                        recentCancelledSwipeEdge = downEdgeCandidate
                        recentCancelledSwipeAtMs = android.os.SystemClock.uptimeMillis()
                    }
                    downEdgeCandidate = com.example.ui.BACK_EDGE_NONE
                }
                android.view.MotionEvent.ACTION_UP -> {
                    downEdgeCandidate = com.example.ui.BACK_EDGE_NONE
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    fun consumeRecentCancelledSwipeEdge(maxAgeMs: Long = 1500L): Int {
        val edge = recentCancelledSwipeEdge
        val age = android.os.SystemClock.uptimeMillis() - recentCancelledSwipeAtMs
        recentCancelledSwipeEdge = com.example.ui.BACK_EDGE_NONE
        recentCancelledSwipeAtMs = 0L
        return if (edge != com.example.ui.BACK_EDGE_NONE && age in 0..maxAgeMs) {
            edge
        } else {
            com.example.ui.BACK_EDGE_NONE
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

    private var pendingFileResult: ((List<android.net.Uri>) -> Unit)? = null
    private val fileChooser = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        pendingFileResult?.invoke(uris ?: emptyList())
        pendingFileResult = null
    }

    private val pipActionReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> ZenMediaBus.onPlayPause?.invoke()
                ACTION_PIP_MUTE -> ZenMediaBus.onToggleMute?.invoke()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pipFilter = android.content.IntentFilter().apply {
            addAction(ACTION_PIP_PLAY_PAUSE)
            addAction(ACTION_PIP_MUTE)
        }
        ContextCompat.registerReceiver(this, pipActionReceiver, pipFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        com.example.gecko.ZenGeckoManager.filePicker = { _, types, onResult ->
            pendingFileResult?.invoke(emptyList())
            pendingFileResult = onResult
            try { fileChooser.launch(types) } catch (_: Exception) { pendingFileResult = null; onResult(emptyList()) }
        }
        enableEdgeToEdge()
        com.example.gecko.ZenGeckoManager.init(applicationContext)
        com.example.privacy.ZenAdBlockList.load(applicationContext)

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

            // File access: asked once on first launch (media read on Android 13+, storage on older versions).
            val filePermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { _ -> }
            LaunchedEffect(Unit) {
                val prefs = getSharedPreferences("zen_prefs", MODE_PRIVATE)
                if (!prefs.getBoolean("asked_file_permission", false)) {
                    val wanted = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                            android.Manifest.permission.READ_MEDIA_IMAGES,
                            android.Manifest.permission.READ_MEDIA_VIDEO,
                            android.Manifest.permission.READ_MEDIA_AUDIO
                        )
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        else -> arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    }
                    val missing = wanted.filter {
                        ContextCompat.checkSelfPermission(applicationContext, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    prefs.edit().putBoolean("asked_file_permission", true).apply()
                    if (missing.isNotEmpty()) filePermissionLauncher.launch(missing.toTypedArray())
                }
            }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
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

            // Sync Picture-in-Picture auto-enter parameters & RemoteActions on Android 8+ / 12+
            val shouldAutoPip = uiState.mediaPlaybackState.isPlaying && uiState.sidebarConfig.playInWindowPipEnabled
            LaunchedEffect(
                shouldAutoPip,
                uiState.mediaPlaybackState.isPlaying,
                uiState.mediaPlaybackState.isMuted,
                uiState.isInPictureInPictureMode
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
                ) {
                    runCatching {
                        setPictureInPictureParams(
                            buildPictureInPictureParams(
                                autoEnter = shouldAutoPip,
                                isPlaying = uiState.mediaPlaybackState.isPlaying,
                                isMuted = uiState.mediaPlaybackState.isMuted
                            )
                        )
                    }
                }
            }

            // Sync media playback with Android notification shade & lockscreen controls
            val media = uiState.mediaPlaybackState
            LaunchedEffect(uiState.sidebarConfig.backgroundPlayEnabled) {
                com.example.gecko.ZenGeckoManager.backgroundPlayEnabled = uiState.sidebarConfig.backgroundPlayEnabled
            }
            LaunchedEffect(media.isActive, media.isPlaying, media.title, media.domain, media.artwork, uiState.sidebarConfig.backgroundPlayEnabled) {
                if (media.isActive && uiState.sidebarConfig.backgroundPlayEnabled) {
                    ZenMediaService.sync(applicationContext, media.isPlaying, media.title, media.domain, media.artwork)
                } else {
                    ZenMediaService.stop(applicationContext)
                }
            }

            // Automatically launch BiometricPrompt when app is locked
            LaunchedEffect(uiState.isAppLocked) {
                if (uiState.isAppLocked) {
                    promptBiometricUnlock()
                }
            }

            ZenBrowserTheme(
                accentTheme = uiState.activeAccentTheme
            ) {
                ZenBrowserScreen(
                    uiState = uiState,
                    viewModel = browserViewModel,
                    onTriggerBiometricUnlock = { promptBiometricUnlock() },
                    biometricErrorMessage = biometricErrorMessage
                )
            }
        }
    }

    fun promptBiometricUnlock(onSuccess: (() -> Unit)? = null) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    biometricErrorMessage = null
                    viewModelRef?.setAppLocked(false)
                    onSuccess?.invoke()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        biometricErrorMessage = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    biometricErrorMessage = "Authentication failed. Please try again."
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Zen Browser")
            .setSubtitle("Authenticate to access your tabs and browsing history")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            biometricErrorMessage = e.localizedMessage ?: "Biometric prompt error"
        }
    }

    override fun onStop() {
        super.onStop()
        // Keep page JS timers + audio running when the screen turns off / app is backgrounded.
        val media = viewModelRef?.uiState?.value?.mediaPlaybackState
        if (media?.isActive == true && viewModelRef?.uiState?.value?.sidebarConfig?.backgroundPlayEnabled != false) {
            com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            com.example.ui.components.ZenWebViewManager.getAllWebViews().values.forEach { it.resumeTimers() }
            mainHandler.postDelayed({
                com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            }, 180L)
            mainHandler.postDelayed({
                com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            }, 450L)
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
        com.example.gecko.ZenGeckoManager.noteActivityPause()
        viewModelRef?.suppressSidebarDuringTransition(2500L)
        viewModelRef?.setSidebarOpen(false)
        val state = viewModelRef?.uiState?.value
        if (state?.mediaPlaybackState?.isActive == true &&
            (state.sidebarConfig.backgroundPlayEnabled || isInPictureInPictureMode)
        ) {
            com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            mainHandler.postDelayed({
                com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            }, 160L)
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipActionReceiver) }
        mainHandler.removeCallbacksAndMessages(null)
        if (isFinishing) ZenMediaService.stop(applicationContext)
        super.onDestroy()
        releaseWakeLock()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        val state = viewModelRef?.uiState?.value
        val shouldEnterPip = state?.mediaPlaybackState?.isPlaying == true &&
            state.sidebarConfig.playInWindowPipEnabled &&
            !isInPictureInPictureMode
        // On Android 12+, setAutoEnterEnabled(true) enters PiP automatically without a double transition
        if (shouldEnterPip &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            runCatching {
                enterPictureInPictureMode(
                    buildPictureInPictureParams(
                        autoEnter = true,
                        isPlaying = state?.mediaPlaybackState?.isPlaying == true,
                        isMuted = state?.mediaPlaybackState?.isMuted == true
                    )
                )
            }
        }
        viewModelRef?.onLeavingApp()
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun buildPictureInPictureParams(
        autoEnter: Boolean,
        isPlaying: Boolean,
        isMuted: Boolean
    ): android.app.PictureInPictureParams {
        val playPauseIntent = android.app.PendingIntent.getBroadcast(
            this,
            901,
            Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val muteIntent = android.app.PendingIntent.getBroadcast(
            this,
            902,
            Intent(ACTION_PIP_MUTE).setPackage(packageName),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseAction = android.app.RemoteAction(
            android.graphics.drawable.Icon.createWithResource(
                this,
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (isPlaying) "Pause" else "Play",
            if (isPlaying) "Pause media" else "Play media",
            playPauseIntent
        )
        val muteAction = android.app.RemoteAction(
            android.graphics.drawable.Icon.createWithResource(
                this,
                if (isMuted) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode
            ),
            if (isMuted) "Unmute" else "Mute",
            if (isMuted) "Unmute media" else "Mute media",
            muteIntent
        )
        val builder = android.app.PictureInPictureParams.Builder()
            .setAspectRatio(android.util.Rational(16, 9))
            .setActions(listOf(playPauseAction, muteAction))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(autoEnter)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        downEdgeCandidate = com.example.ui.BACK_EDGE_NONE
        recentCancelledSwipeEdge = com.example.ui.BACK_EDGE_NONE
        recentCancelledSwipeAtMs = 0L
        com.example.gecko.ZenGeckoManager.onPipModeChanged(isInPictureInPictureMode)
        viewModelRef?.suppressSidebarDuringTransition(2600L)
        viewModelRef?.setPictureInPictureMode(isInPictureInPictureMode)
        val shouldKeepMedia = viewModelRef?.shouldAutoResumeMediaOnPip() == true
        if (shouldKeepMedia) {
            com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            mainHandler.postDelayed({
                com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            }, 150L)
            mainHandler.postDelayed({
                com.example.gecko.ZenGeckoManager.keepBackgroundSessionsActive()
            }, 420L)
        }
    }

    companion object {
        private const val ACTION_PIP_PLAY_PAUSE = "com.example.ACTION_PIP_PLAY_PAUSE"
        private const val ACTION_PIP_MUTE = "com.example.ACTION_PIP_MUTE"
    }
}
