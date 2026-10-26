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

            LaunchedEffect(uiState.mediaPlaybackState.isPlaying, uiState.mediaPlaybackState.isActive, uiState.sidebarConfig.playInWindowPipEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val isPlaying = uiState.mediaPlaybackState.isPlaying || uiState.mediaPlaybackState.isActive
                    val pipEnabled = uiState.sidebarConfig.playInWindowPipEnabled
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .setAutoEnterEnabled(isPlaying && pipEnabled)
                        .build()
                    setPictureInPictureParams(params)
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
        enterPipIfPlaying()
    }

    private fun enterPipIfPlaying(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val state = viewModelRef?.uiState?.value
            val isPlaying = state?.mediaPlaybackState?.isPlaying == true || state?.mediaPlaybackState?.isActive == true
            val pipEnabled = state?.sidebarConfig?.playInWindowPipEnabled ?: true
            if (isPlaying && pipEnabled) {
                viewModelRef?.setSidebarOpen(false)
                viewModelRef?.setPictureInPictureMode(true)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                setAutoEnterEnabled(true)
                            }
                        }
                        .build()
                    enterPictureInPictureMode(params)
                }, 50)
                return true
            }
        }
        return false
    }

    fun enterPipManual(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModelRef?.setSidebarOpen(false)
            viewModelRef?.setPictureInPictureMode(true)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }, 50)
            return true
        }
        return false
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        viewModelRef?.setPictureInPictureMode(isInPictureInPictureMode)
    }
}
