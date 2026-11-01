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
            val uiState by browserViewModel.uiState.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

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

            // Sync media playback with Android notification shade & lockscreen controls
            val media = uiState.mediaPlaybackState
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
        viewModelRef?.setSidebarOpen(false)
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
    }
}
