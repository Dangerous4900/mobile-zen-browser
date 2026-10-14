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

class MainActivity : ComponentActivity() {

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

            ZenBrowserTheme(
                accentTheme = uiState.activeAccentTheme
            ) {
                ZenBrowserScreen(
                    uiState = uiState,
                    viewModel = browserViewModel
                )
            }
        }
    }
}
