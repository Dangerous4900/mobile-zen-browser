package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.os.Build
import android.util.Rational

class MainActivity : ComponentActivity() {

    private var viewModelRef: ZenBrowserViewModel? = null

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

            LaunchedEffect(uiState.mediaPlaybackState.isPlaying, uiState.sidebarConfig.playInWindowPipEnabled) {
                if (canUsePip()) {
                    val auto = uiState.mediaPlaybackState.isPlaying && uiState.sidebarConfig.playInWindowPipEnabled
                    setPictureInPictureParams(buildPipParams(autoEnter = auto))
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

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // On Android 12+ setAutoEnterEnabled handles this; entering twice makes PiP flicker.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) enterPipIfPlaying()
    }

    private fun canUsePip(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun buildPipParams(autoEnter: Boolean): PictureInPictureParams {
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(autoEnter)
                    setSeamlessResizeEnabled(true)
                }
            }
            .build()
    }

    private fun enterPipIfPlaying(): Boolean {
        val state = viewModelRef?.uiState?.value ?: return false
        val media = state.mediaPlaybackState
        val pipEnabled = state.sidebarConfig.playInWindowPipEnabled
        if (!(media.isPlaying && pipEnabled)) return false
        return enterPipNow()
    }

    fun enterPipManual(): Boolean = enterPipNow()

    /**
     * Enters PiP. UI state is flipped only in onPictureInPictureModeChanged, so a refused request
     * (PiP disabled in system settings, unsupported device) can no longer leave the UI stuck blank.
     */
    private fun enterPipNow(): Boolean {
        if (!canUsePip() || isInPictureInPictureMode) return false
        viewModelRef?.setSidebarOpen(false)
        return try {
            enterPictureInPictureMode(buildPipParams(autoEnter = false))
        } catch (e: IllegalStateException) {
            false
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModelRef?.setPictureInPictureMode(isInPictureInPictureMode)
    }
}
