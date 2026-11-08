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
        try {
            geckoSession.setActive(true)
        } catch (_: Throwable) {}
        onDispose {
            if (controller.activeTabId == tab.id) {
                controller.geckoSession = null
            }
            // Keep background session active so media playback continues when switching tabs
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                if (geckoSession.isOpen) {
                    try {
                        geckoSession.setActive(true)
                    } catch (_: Throwable) {}
                }
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

    var geckoViewRef by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<GeckoView?>(null) }
    // Page previews for the tabs overview: after a page finishes loading, and whenever the side panel opens.
    androidx.compose.runtime.LaunchedEffect(tab.id, currentUiState.pageLoadingProgress >= 100, currentUiState.isSidebarOpen) {
        val gv = geckoViewRef ?: return@LaunchedEffect
        if (currentUiState.isInPictureInPictureMode || tab.url.isBlank()) return@LaunchedEffect
        if (tab.id != currentUiState.activePrimaryTab?.id) return@LaunchedEffect
        if (!currentUiState.isSidebarOpen) kotlinx.coroutines.delay(1500)
        try {
            gv.capturePixels().accept({ bmp -> if (bmp != null) ZenTabThumbnails.put(tab.id, bmp) }, { _ -> })
        } catch (_: Throwable) {}
    }

    AndroidView(
        factory = { ctx ->
            GeckoView(ctx).apply {
                geckoViewRef = this
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setSession(geckoSession)
                var touchStartX = 0f
                var touchStartY = 0f
                var swipeHandled = false
                // Honors Settings > horizontal swipe mode: one edge opens the side panel, the other goes back.
                fun handleEdgeSwipe(fromLeftEdge: Boolean) {
                    val panelOnLeft = currentUiState.sidebarConfig.horizontalSwipeMode ==
                        com.example.data.HorizontalSwipeNavigationMode.SWIPE_RIGHT_PANEL_LEFT_BACK
                    if (fromLeftEdge == panelOnLeft) currentOnOpenSidebar()
                    else post { runCatching { geckoSession.goBack() } }
                }
                // Gesture navigation reserves the screen edges for "back" and cancels our swipe (ACTION_CANCEL),
                // so ask the system to leave a band of each edge to us (Android caps this at 200dp tall).
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    addOnLayoutChangeListener { v, l, t, r, b, _, _, _, _ ->
                        val d = v.resources.displayMetrics.density
                        val w = r - l
                        val h = b - t
                        val band = (24 * d).toInt()
                        val bandH = minOf(h, (200 * d).toInt())
                        val top = (h - bandH) / 2
                        v.systemGestureExclusionRects = listOf(
                            android.graphics.Rect(0, top, band, top + bandH),
                            android.graphics.Rect(w - band, top, w, top + bandH)
                        )
                    }
                }
                setOnTouchListener { _, event ->
                    if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_UP) {
                        ZenGeckoManager.noteUserTouch()
                    }
                    if (currentUiState.isInPictureInPictureMode) return@setOnTouchListener false
                    val densityMetrics = ctx.resources.displayMetrics
                    val edgeThresholdPx = 44f * densityMetrics.density
                    val screenWidthPx = densityMetrics.widthPixels.toFloat()

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            if (currentUiState.isInPictureInPictureMode) return@setOnTouchListener false
                            touchStartX = event.rawX
                            touchStartY = event.rawY
                            swipeHandled = false
                        }
                        MotionEvent.ACTION_MOVE -> {
                            if (currentUiState.isInPictureInPictureMode) return@setOnTouchListener false
                            // Open as soon as the swipe is clearly horizontal, instead of waiting for the finger to lift
                            // (the page or the system may consume or cancel the gesture before ACTION_UP).
                            if (!swipeHandled && touchStartX > 0f) {
                                val dx = event.rawX - touchStartX
                                val dy = kotlin.math.abs(event.rawY - touchStartY)
                                val fromLeft = touchStartX <= edgeThresholdPx && dx > 48f
                                val fromRight = touchStartX >= (screenWidthPx - edgeThresholdPx) && dx < -48f
                                if ((fromLeft || fromRight) && kotlin.math.abs(dx) > dy * 1.35f) {
                                    swipeHandled = true
                                    handleEdgeSwipe(fromLeft)
                                }
                            }
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
                            if (swipeHandled) {
                                swipeHandled = false
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
                                        if (!currentUiState.isInPictureInPictureMode) handleEdgeSwipe(true)
                                    } else if (isRightEdge && dx < -40f) {
                                        if (!currentUiState.isInPictureInPictureMode) handleEdgeSwipe(false)
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
            try {
                geckoSession.setActive(true)
            } catch (_: Throwable) {}
        },
        modifier = modifier
            .fillMaxSize()
            .testTag("zen_geckoview_${tab.id}")
    )
}
