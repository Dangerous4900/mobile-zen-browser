package com.example.ui.components

import android.view.MotionEvent
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.BlockedTrackerEvent
import com.example.data.TabEntity
import com.example.gecko.ZenGeckoCallback
import com.example.gecko.ZenGeckoManager
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.ZenBrowserUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoView
import kotlin.math.abs
import kotlin.math.roundToInt

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
    onInstallExtensionFromAmo: (url: String, title: String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
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
            ZenGeckoManager.setFocusedTab(tab.id)
        }
        try {
            geckoSession.setActive(true)
        } catch (_: Throwable) {}
        onDispose {
            if (controller.activeTabId == tab.id) {
                controller.geckoSession = null
            }
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

    // Push enabled extensions / custom scripts / CSS to GeckoView pages
    val activeCss = uiState.getInjectedCssForTab(tab)
    val activeJs = uiState.getInjectedJsForTab(tab)
    LaunchedEffect(tab.id, tab.url, activeCss, activeJs) {
        ZenGeckoManager.pushExtensionAssets(activeCss, activeJs)
    }

    // Sync desktop vs mobile UA & viewport mode whenever toggled
    LaunchedEffect(tab.id, isDesktop) {
        ZenGeckoManager.setDesktopMode(context, tab.id, isDesktop)
    }

    // Push PiP video styling state into GeckoView pages
    LaunchedEffect(uiState.isInPictureInPictureMode) {
        ZenGeckoManager.pushPipMode(uiState.isInPictureInPictureMode)
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

                override fun onInstallAmoExtension(url: String, title: String?) {
                    onInstallExtensionFromAmo(url, title)
                }
            }
        )
        onDispose {}
    }

    val currentUiState by rememberUpdatedState(uiState)
    val currentHaptic by rememberUpdatedState(haptic)

    // Pull-to-refresh state when page is scrolled to the top
    var pullDistancePx by remember { mutableFloatStateOf(0f) }
    var isPullRefreshing by remember { mutableStateOf(false) }
    var hitThresholdHaptic by remember { mutableStateOf(false) }
    val pullTriggerPx = with(density) { 86.dp.toPx() }
    val edgeGuardPx = with(density) { 44.dp.toPx() }
    val touchSlopPx = with(density) { 18.dp.toPx() }

    val pullProgress = (pullDistancePx / pullTriggerPx).coerceIn(0f, 1.35f)
    val animatedPullOffset by animateFloatAsState(
        targetValue = when {
            isPullRefreshing -> pullTriggerPx * 0.62f
            pullDistancePx > 0f -> (pullDistancePx * 0.58f).coerceAtMost(pullTriggerPx * 0.92f)
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "gecko_pull_offset"
    )
    val animatedBadgeScale by animateFloatAsState(
        targetValue = when {
            isPullRefreshing -> 1f
            pullDistancePx > 8f -> (0.55f + (pullProgress * 0.5f)).coerceIn(0.55f, 1.08f)
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "gecko_pull_scale"
    )
    val infiniteSpin = rememberInfiniteTransition(label = "gecko_pull_spin")
    val refreshingRotation by infiniteSpin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 720, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gecko_pull_spin_deg"
    )

    LaunchedEffect(uiState.pageLoadingProgress) {
        if (isPullRefreshing && uiState.pageLoadingProgress >= 100) {
            delay(300)
            isPullRefreshing = false
        }
    }

    var geckoViewRef by remember { mutableStateOf<GeckoView?>(null) }
    // Page previews for the tabs overview: after a page finishes loading, and whenever the side panel opens.
    LaunchedEffect(tab.id, currentUiState.pageLoadingProgress >= 100, currentUiState.isSidebarOpen) {
        val gv = geckoViewRef ?: return@LaunchedEffect
        if (currentUiState.isInPictureInPictureMode || tab.url.isBlank()) return@LaunchedEffect
        if (tab.id != currentUiState.activePrimaryTab?.id) return@LaunchedEffect
        if (!currentUiState.isSidebarOpen) delay(1500)
        try {
            gv.capturePixels().accept({ bmp -> if (bmp != null) ZenTabThumbnails.put(tab.id, bmp) }, { _ -> })
        } catch (_: Throwable) {}
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .testTag("zen_gecko_viewport_container_${tab.id}")
    ) {
        AndroidView(
            factory = { ctx ->
                GeckoView(ctx).apply {
                    geckoViewRef = this
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    ZenGeckoManager.attachSessionToView(tab.id, this)
                    var downX = 0f
                    var downY = 0f
                    var eligibleTopPull = false
                    var pullEngaged = false
                    setOnTouchListener { view, event ->
                        when (event.actionMasked) {
                            MotionEvent.ACTION_DOWN -> {
                                ZenGeckoManager.noteUserTouch()
                                downX = event.x
                                downY = event.y
                                val vw = view.width.toFloat().coerceAtLeast(1f)
                                val outsideEdges = downX > edgeGuardPx && downX < (vw - edgeGuardPx)
                                if (!outsideEdges) {
                                    ZenGeckoManager.suppressContextMenuFor(1800L)
                                }
                                eligibleTopPull = !currentUiState.isInPictureInPictureMode &&
                                    !isPullRefreshing &&
                                    outsideEdges &&
                                    ZenGeckoManager.isTabAtTop(tab.id)
                                pullEngaged = false
                                hitThresholdHaptic = false
                                if (!isPullRefreshing) pullDistancePx = 0f
                            }
                            MotionEvent.ACTION_POINTER_DOWN -> {
                                eligibleTopPull = false
                                pullEngaged = false
                                pullDistancePx = 0f
                            }
                            MotionEvent.ACTION_MOVE -> {
                                if (eligibleTopPull && event.pointerCount == 1) {
                                    val dx = event.x - downX
                                    val dy = event.y - downY
                                    if (!pullEngaged) {
                                        if (dy > touchSlopPx && dy > abs(dx) * 1.45f && ZenGeckoManager.isTabAtTop(tab.id)) {
                                            pullEngaged = true
                                        } else if (abs(dx) > touchSlopPx || dy < -touchSlopPx) {
                                            eligibleTopPull = false
                                        }
                                    }
                                    if (pullEngaged) {
                                        val damped = (dy * 0.55f).coerceIn(0f, pullTriggerPx * 1.45f)
                                        pullDistancePx = damped
                                        if (damped >= pullTriggerPx && !hitThresholdHaptic) {
                                            hitThresholdHaptic = true
                                            currentHaptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                            }
                            MotionEvent.ACTION_UP -> {
                                ZenGeckoManager.noteUserTouch()
                                if (pullEngaged && pullDistancePx >= pullTriggerPx && !isPullRefreshing) {
                                    isPullRefreshing = true
                                    pullDistancePx = 0f
                                    ZenGeckoManager.requestSaveCurrentScroll()
                                    geckoSession.reload()
                                    scope.launch {
                                        delay(2600)
                                        isPullRefreshing = false
                                    }
                                } else {
                                    pullDistancePx = 0f
                                }
                                eligibleTopPull = false
                                pullEngaged = false
                                hitThresholdHaptic = false
                            }
                            MotionEvent.ACTION_CANCEL -> {
                                eligibleTopPull = false
                                pullEngaged = false
                                hitThresholdHaptic = false
                                pullDistancePx = 0f
                            }
                        }
                        false
                    }
                }
            },
            update = { gv ->
                ZenGeckoManager.attachSessionToView(tab.id, gv)
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("zen_geckoview_${tab.id}")
        )

        // Animated Scroll-to-Top Pull-to-Refresh Indicator Pill
        AnimatedVisibility(
            visible = (pullDistancePx > 8f || isPullRefreshing) && !uiState.isInPictureInPictureMode,
            enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.7f),
            exit = fadeOut(tween(180)) + scaleOut(targetScale = 0.7f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            val readyToReload = pullProgress >= 1f || isPullRefreshing
            Surface(
                shape = CircleShape,
                color = if (readyToReload) Color(0xFF1E293B) else Color(0xFF141418),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = Color.White
                ),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset { IntOffset(0, animatedPullOffset.roundToInt()) }
                    .size(44.dp)
                    .graphicsLayer {
                        scaleX = animatedBadgeScale
                        scaleY = animatedBadgeScale
                    }
                    .testTag("pull_to_refresh_indicator")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isPullRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer { rotationZ = refreshingRotation },
                            color = Color.White,
                            strokeWidth = 2.4.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (readyToReload) "Release to reload page" else "Pull to reload page",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    rotationZ = (pullProgress * 260f).coerceAtMost(340f)
                                }
                        )
                    }
                }
            }
        }

        val currentInstallTarget = remember(tab.url) {
            com.example.extensions.ZenExtensionCompatibility.resolveInstallTarget(tab.url)
        }
        val showAddToZenExtensionPill = !uiState.isInPictureInPictureMode &&
            currentInstallTarget !is com.example.extensions.ExtensionInstallTarget.Unsupported
        AnimatedVisibility(
            visible = showAddToZenExtensionPill,
            enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            val pillLabel = when (currentInstallTarget) {
                is com.example.extensions.ExtensionInstallTarget.UserscriptUrl -> "Install Userscript in Zen"
                else -> "Add Extension to Zen"
            }
            Surface(
                onClick = { onInstallExtensionFromAmo(tab.url, tab.title) },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1D4ED8),
                border = BorderStroke(1.dp, Color(0xFF93C5FD).copy(alpha = 0.65f)),
                shadowElevation = 8.dp,
                modifier = Modifier.testTag("amo_install_extension_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = pillLabel,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = pillLabel,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
