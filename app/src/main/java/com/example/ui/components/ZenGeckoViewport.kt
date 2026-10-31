package com.example.ui.components

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.BlockedTrackerEvent
import com.example.data.TabEntity
import com.example.gecko.ZenGeckoCallback
import com.example.gecko.ZenGeckoManager
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.ZenBrowserUiState
import org.mozilla.geckoview.GeckoView

@android.annotation.SuppressLint("ClickableViewAccessibility")
@Composable
fun ZenGeckoViewPort(
    tab: TabEntity,
    uiState: ZenBrowserUiState,
    controller: ZenWebViewController,
    isFocusedPane: Boolean,
    onPageVisited: (url: String, title: String?) -> Unit,
    onProgressChanged: (progress: Int, canBack: Boolean, canForward: Boolean) -> Unit,
    onTrackerBlocked: (BlockedTrackerEvent) -> Unit,
    onMediaStateChanged: (isActive: Boolean, isPlaying: Boolean, title: String, domain: String, tabId: String, artwork: android.graphics.Bitmap?) -> Unit,
    onOpenInNewTab: (url: String, forceSwitch: Boolean?) -> Unit,
    onOpenSidebar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDesktop = uiState.desktopModeTabIds.contains(tab.id)

    val geckoSession = remember(tab.id) {
        ZenGeckoManager.getOrCreateSession(
            context = context,
            tabId = tab.id,
            isPrivate = tab.isPrivate || (uiState.activeWorkspace?.isPrivate == true),
            isDesktop = isDesktop
        )
    }

    DisposableEffect(tab.id, isFocusedPane) {
        if (isFocusedPane) {
            controller.geckoSession = geckoSession
            controller.activeTabId = tab.id
        }
        onDispose {
            if (controller.activeTabId == tab.id) {
                controller.geckoSession = null
            }
        }
    }

    // Bind URL changes - only load when target is a new, valid URL (never reload if already loaded or about:blank)
    LaunchedEffect(tab.url, tab.id) {
        val target = tab.url.trim()
        if (target.isNotBlank() && !target.startsWith("zen://") && target != "about:blank") {
            val currentLoaded = ZenGeckoManager.getCurrentSessionUrl(tab.id)
            if (currentLoaded == null || currentLoaded != target) {
                ZenGeckoManager.setCurrentSessionUrl(tab.id, target)
                geckoSession.loadUri(target)
            }
        }
    }

    // Attach delegates
    DisposableEffect(tab.id, geckoSession) {
        ZenGeckoManager.attachDelegates(
            session = geckoSession,
            tabId = tab.id,
            callback = object : ZenGeckoCallback {
                override fun onLocationChange(url: String, title: String?) {
                    onPageVisited(url, title)
                }

                override fun onProgressChange(progress: Int, canGoBack: Boolean, canGoForward: Boolean) {
                    controller.canGoBackGecko = canGoBack
                    controller.canGoForwardGecko = canGoForward
                    onProgressChanged(progress, canGoBack, canGoForward)
                }

                override fun onTrackerBlocked(sourceUrl: String, category: String) {
                    onTrackerBlocked(
                        BlockedTrackerEvent(
                            domain = ZenPrivacyEngine.extractHostDisplay(sourceUrl),
                            category = category
                        )
                    )
                }

                override fun onMediaStateChange(isActive: Boolean, isPlaying: Boolean, title: String, artwork: android.graphics.Bitmap?) {
                    onMediaStateChanged(isActive, isPlaying, title, ZenPrivacyEngine.extractHostDisplay(tab.url), tab.id, artwork)
                }

                override fun onOpenNewTab(url: String) {
                    onOpenInNewTab(url, null)
                }
            }
        )
        onDispose {}
    }

    val currentUiState by androidx.compose.runtime.rememberUpdatedState(uiState)
    val currentOnOpenSidebar by androidx.compose.runtime.rememberUpdatedState(onOpenSidebar)

    AndroidView(
        factory = { ctx ->
            GeckoView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setSession(geckoSession)
                var touchStartX = 0f
                var touchStartY = 0f
                setOnTouchListener { _, event ->
                    if (currentUiState.isInPictureInPictureMode) return@setOnTouchListener false
                    val densityMetrics = ctx.resources.displayMetrics
                    val edgeThresholdPx = 28f * densityMetrics.density
                    val screenWidthPx = densityMetrics.widthPixels.toFloat()

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            if (currentUiState.isInPictureInPictureMode) return@setOnTouchListener false
                            touchStartX = event.rawX
                            touchStartY = event.rawY
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (currentUiState.isInPictureInPictureMode) return@setOnTouchListener false
                        }
                        MotionEvent.ACTION_CANCEL -> {
                            touchStartX = 0f
                            touchStartY = 0f
                            return@setOnTouchListener false
                        }
                        MotionEvent.ACTION_UP -> {
                            if (currentUiState.isInPictureInPictureMode) {
                                touchStartX = 0f
                                touchStartY = 0f
                                return@setOnTouchListener false
                            }
                            val isLeftEdge = touchStartX <= edgeThresholdPx
                            val isRightEdge = touchStartX >= (screenWidthPx - edgeThresholdPx)
                            if (isLeftEdge || isRightEdge) {
                                val dx = event.rawX - touchStartX
                                val dy = kotlin.math.abs(event.rawY - touchStartY)
                                val horizontalDistance = kotlin.math.abs(dx)
                                if (horizontalDistance > 60f && horizontalDistance > dy * 1.35f) {
                                    if (isLeftEdge && dx > 40f) {
                                        if (!currentUiState.isInPictureInPictureMode) {
                                            currentOnOpenSidebar()
                                        }
                                    } else if (isRightEdge && dx < -40f) {
                                        if (!currentUiState.isInPictureInPictureMode) {
                                            currentOnOpenSidebar()
                                        }
                                    }
                                }
                            }
                            touchStartX = 0f
                            touchStartY = 0f
                        }
                    }
                    false
                }
            }
        },
        update = { gv ->
            if (gv.session != geckoSession) {
                gv.setSession(geckoSession)
            }
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("zen_geckoview_${tab.id}")
    )
}
