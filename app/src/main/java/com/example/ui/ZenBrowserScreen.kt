package com.example.ui

import com.example.data.TabEntity
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.BackSwipeSidebarMode
import com.example.data.HorizontalSwipeNavigationMode
import com.example.data.SidebarPosition
import com.example.data.TopToolbarToolItem
import com.example.data.UrlBarPosition
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.components.EssentialsManagerSheet
import com.example.ui.components.FirefoxExtensionsSheet
import com.example.ui.components.PasswordManagerSheet
import com.example.ui.components.PrivacyShieldSheet
import com.example.ui.components.SearchEngineSettingsSheet
import com.example.ui.components.ShadeGlassIcon
import com.example.ui.components.SidebarCustomizationStudioSheet
import com.example.ui.components.ZenExtensionFloatingPopup
import com.example.ui.components.ZenExtensionsMenuPopup
import com.example.ui.components.SidebarLayoutEditorSheet
import com.example.ui.components.SplitTabPickerSheet
import com.example.ui.components.StartPageCustomizerSheet
import com.example.ui.components.WorkspaceManagerSheet
import com.example.ui.components.ZenGlanceOverlay
import com.example.ui.components.ZenLibrarySheet
import com.example.ui.components.ZenModsStoreSheet
import com.example.ui.components.ZenSidebarPanel
import com.example.ui.components.ZenTutorialDialog
import com.example.ui.components.ZenWebPaneView
import com.example.ui.components.ZenWebViewController
import com.example.data.PasswordEntity
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun ZenBrowserScreen(
    uiState: ZenBrowserUiState,
    viewModel: ZenBrowserViewModel,
    onEnterPip: () -> Unit = {}
) {
    val primaryWebController = remember { ZenWebViewController() }
    val secondaryWebController = remember { ZenWebViewController() }
    val activeController = if (uiState.isSplitViewEnabled && uiState.focusedPane == FocusedPane.SECONDARY) {
        secondaryWebController
    } else {
        primaryWebController
    }

    // System media controls use explicit state commands so Android's Play and Pause
    // buttons cannot accidentally invert each other. Pages without media are left untouched.
    DisposableEffect(primaryWebController, secondaryWebController, uiState.mediaPlaybackState.isPlaying) {
        com.example.ZenMediaBus.onSetPlaying = { playing ->
            primaryWebController.setMediaPlaying(playing)
            secondaryWebController.setMediaPlaying(playing)
        }
        com.example.ZenMediaBus.onPlayPause = {
            val shouldPlay = !uiState.mediaPlaybackState.isPlaying
            primaryWebController.setMediaPlaying(shouldPlay)
            secondaryWebController.setMediaPlaying(shouldPlay)
        }
        com.example.ZenMediaBus.onStop = {
            primaryWebController.stopAllMedia()
            secondaryWebController.stopAllMedia()
            viewModel.dismissMediaPlaybackBar()
        }
        onDispose {
            com.example.ZenMediaBus.onSetPlaying = null
            com.example.ZenMediaBus.onPlayPause = null
            com.example.ZenMediaBus.onStop = null
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.onAppBackgroundedOrClosed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.isInPictureInPictureMode) {
        if (uiState.isInPictureInPictureMode) {
            viewModel.setSidebarOpen(false)
        }
        // Make the playing video fill the small PiP window; restore the page on exit.
        primaryWebController.setPipVideoMode(uiState.isInPictureInPictureMode)
        secondaryWebController.setPipVideoMode(uiState.isInPictureInPictureMode)
    }

    var showExtensionsMenuPopup by remember { mutableStateOf(false) }
    var activeExtensionPopup by remember { mutableStateOf<com.example.data.FirefoxExtensionEntity?>(null) }

    val handleSaveTabOffline: (TabEntity) -> Unit = remember(primaryWebController) {
        { tab ->
            if (tab.id == uiState.focusedTab?.id) {
                primaryWebController.capturePageHtmlForOffline { html ->
                    viewModel.saveTabForOffline(tab, html)
                }
            } else {
                viewModel.saveTabForOffline(tab, "")
            }
        }
    }

    // Predictive Back Handler:
    // Navigates back in web page history when history is available, or handles modals/sidebar.
    PredictiveBackHandler(enabled = true) { progress ->
        var swipeEdge = BackEventCompat.EDGE_LEFT
        try {
            progress.collect { backEvent ->
                swipeEdge = backEvent.swipeEdge
            }
            when {
                activeExtensionPopup != null -> activeExtensionPopup = null
                showExtensionsMenuPopup -> showExtensionsMenuPopup = false
                uiState.activeSheet != ActiveModalSheet.NONE -> viewModel.closeSheet()
                uiState.isCenterTopSearchPopupVisible -> viewModel.setCenterTopSearchPopupVisible(false)
                uiState.isFindInPageVisible -> viewModel.toggleFindInPage(false)
                uiState.isSidebarOpen -> viewModel.setSidebarOpen(false)
                uiState.isInPictureInPictureMode -> { /* Do not open sidebar in PiP */ }
                else -> {
                    val swipeMode = uiState.sidebarConfig.horizontalSwipeMode
                    if (swipeMode == HorizontalSwipeNavigationMode.SWIPE_LEFT_PANEL_RIGHT_BACK) {
                        if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
                            // User swiped leftwards from the right edge (<-): open side panel!
                            viewModel.setSidebarOpen(true)
                        } else {
                            // User swiped rightwards from the left edge (->): go back in history!
                            activeController.goBack()
                        }
                    } else {
                        if (swipeEdge == BackEventCompat.EDGE_RIGHT) {
                            // User swiped leftwards from the right edge (<-): go back in history!
                            activeController.goBack()
                        } else {
                            // User swiped rightwards from the left edge (->): open side panel!
                            viewModel.setSidebarOpen(true)
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
            // Gesture cancelled by user
        }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        if (uiState.statusBannerMessage != null) {
            delay(2200)
            viewModel.showBanner(null)
        }
    }

    if (uiState.isInPictureInPictureMode) {
        val activeTab = uiState.focusedTab ?: uiState.activePrimaryTab
        if (activeTab != null) {
            ZenWebPaneView(
                tab = activeTab,
                uiState = uiState,
                controller = primaryWebController,
                isFocusedPane = true,
                onFocusPane = {},
                onNavigateUrl = viewModel::navigateFocusedTab,
                onReloadUnloadedTab = { viewModel.selectTab(it.id) },
                onPageVisited = { url, title -> viewModel.onWebPageVisited(activeTab.id, url, title) },
                onProgressChanged = viewModel::updateNavigationProgress,
                onTrackerBlocked = viewModel::onTrackerBlocked,
                onMediaStateChanged = viewModel::updateMediaPlaybackState,
                onInstallExtensionFromAmo = viewModel::installExtensionFromAmoPage,
                onAddHomeShortcut = viewModel::addHomeShortcut,
                onUpdateHomeShortcut = viewModel::updateHomeShortcut,
                onDeleteHomeShortcut = viewModel::deleteHomeShortcut,
                onUpdateCustomTextPosition = { _, _ -> },
                onOpenSheet = viewModel::openSheet,
                onToggleSplitView = { viewModel.toggleSplitView() },
                onOpenGlance = viewModel::openGlance,
                onOpenInNewTab = { url, forceSwitch -> viewModel.openLinkInNewTab(url, forceSwitch = forceSwitch) },
                onOpenInPrivateTab = { url -> viewModel.openLinkInNewTab(url, isPrivate = true) },
                onUpdateSwitchToNewTabImmediately = viewModel::updateSwitchToNewTabImmediately,
                onPopTabHistory = viewModel::popTabHistory,
                onPopTabForward = viewModel::popTabForward,
                onPopTabHistoryOffset = viewModel::popTabHistoryOffset,
                onGetTabHistoryEntries = viewModel::getTabHistoryEntries,
                onOpenSidebar = {},
                onSavePassword = viewModel::insertPassword,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .testTag("zen_browser_root")
    ) {
        val isExpandedTablet = maxWidth >= 720.dp
        val sidebarPinnedOpen = isExpandedTablet && !uiState.isCompactFullscreen
        val sidebarCfg = uiState.sidebarConfig
        val density = LocalDensity.current
        val sidebarWidthPx = with(density) { sidebarCfg.widthMode.widthDp.dp.toPx() }
        val isRightSidebar = sidebarCfg.position == SidebarPosition.RIGHT

        val localView = androidx.compose.ui.platform.LocalView.current
        val viewportGraphicsLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()

        // Scale the backdrop blur dynamically with the Side Panel Blur Slider (0% = 0dp, 50% default = 18dp, 100% = 36dp)
        val targetBackdropBlur = (sidebarCfg.glassBlurIntensity * 36f).dp
        val popupBlurRadius by animateDpAsState(
            targetValue = if (uiState.isCenterTopSearchPopupVisible) targetBackdropBlur else 0.dp,
            label = "center_popup_backdrop_blur"
        )
        // Use the immediate target blur radius when the Side Panel is open so the blur never drops to 0 during animations or recompositions
        val activeSidebarBlurRadius = if (sidebarCfg.glassBlurIntensity > 0.01f) targetBackdropBlur else 0.dp

        // Captured viewport snapshot fallback so even when WebView hardware surfaces or static frames don't trigger a new layer record,
        // the Side Panel always has a guaranteed non-blank backdrop to blur without flickering on and off.
        var capturedViewportBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
        var softwareBlurredBackdrop by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

        LaunchedEffect(
            uiState.isSidebarOpen,
            sidebarCfg.glassBlurIntensity,
            uiState.focusedTab?.id,
            uiState.focusedTab?.url,
            uiState.pageLoadingProgress,
            sidebarCfg.homeScreenStyle,
            sidebarCfg.startPageGradientId,
            sidebarCfg.startPageImageUri,
            sidebarCfg.startPageBrightness
        ) {
            if ((uiState.isSidebarOpen || sidebarPinnedOpen) && sidebarCfg.glassBlurIntensity > 0.01f) {
                val captureFrame = {
                    val vw = localView.width
                    val vh = localView.height
                    if (vw > 0 && vh > 0) {
                        runCatching {
                            val scale = 0.35f
                            val bmpW = (vw * scale).roundToInt().coerceAtLeast(32)
                            val bmpH = (vh * scale).roundToInt().coerceAtLeast(32)
                            val rawBmp = android.graphics.Bitmap.createBitmap(bmpW, bmpH, android.graphics.Bitmap.Config.ARGB_8888)
                            val rawCanvas = android.graphics.Canvas(rawBmp)
                            rawCanvas.scale(scale, scale)
                            localView.draw(rawCanvas)
                            capturedViewportBitmap = rawBmp.asImageBitmap()

                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                                val blurScale = (0.24f - sidebarCfg.glassBlurIntensity * 0.16f).coerceIn(0.06f, 0.24f)
                                val smallW = (vw * blurScale).roundToInt().coerceAtLeast(16)
                                val smallH = (vh * blurScale).roundToInt().coerceAtLeast(16)
                                val smallBmp = android.graphics.Bitmap.createScaledBitmap(rawBmp, smallW, smallH, true)
                                val upBmp = android.graphics.Bitmap.createScaledBitmap(smallBmp, bmpW, bmpH, true)
                                val downBmp = android.graphics.Bitmap.createScaledBitmap(upBmp, smallW, smallH, true)
                                val finalBmp = android.graphics.Bitmap.createScaledBitmap(downBmp, bmpW, bmpH, true)
                                softwareBlurredBackdrop = finalBmp.asImageBitmap()
                            }
                        }
                    }
                }
                captureFrame()
                kotlinx.coroutines.delay(120)
                captureFrame()
            }
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .blur(popupBlurRadius)
                .drawWithContent {
                    viewportGraphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawContent()
                }
        ) {
            if (sidebarPinnedOpen && sidebarCfg.position == SidebarPosition.LEFT && !uiState.isInPictureInPictureMode) {
                ZenSidebarPanel(
                    uiState = uiState,
                    onSelectWorkspace = viewModel::selectWorkspace,
                    onSelectTab = viewModel::selectTab,
                    onCreateNewTab = viewModel::onNewTabButtonClicked,
                    onNewTabSwipeGesture = viewModel::handleNewTabSwipeGesture,
                    onTabSwipeGesture = viewModel::performTabSwipeAction,
                    onUnloadTab = viewModel::unloadTab,
                    onCloseTab = viewModel::closeTab,
                    onTogglePinTab = viewModel::togglePinTab,
                    onToggleBookmarkTab = viewModel::toggleBookmarkTab,
                    onToggleEssentialTab = viewModel::toggleEssentialTab,
                    onDuplicateTab = viewModel::duplicateTab,
                    onSaveTabOffline = handleSaveTabOffline,
                    onMoveTabToWorkspace = viewModel::moveTabToWorkspace,
                    onReorderTabs = viewModel::reorderTabs,
                    onMergeTabsIntoSplitView = viewModel::openSplitViewWithTabs,
                    onConfirmSplitWithHomepage = viewModel::confirmSplitSelectionWithHomepageIfSingle,
                    onCancelSplitSelection = viewModel::cancelSplitTabSelection,
                    onGoBack = { activeController.goBack() },
                    onGoForward = { activeController.goForward() },
                    canGoBack = activeController.canGoBack() || uiState.canGoBack,
                    canGoForward = activeController.canGoForward() || uiState.canGoForward,
                    onGetBackHistory = {
                        val fromWeb = activeController.getBackHistory()
                        if (fromWeb.isNotEmpty()) fromWeb else {
                            val curTabId = uiState.focusedTab?.id
                            if (curTabId != null) viewModel.getTabHistoryEntries(curTabId) else emptyList()
                        }
                    },
                    onNavigateHistoryOffset = { offset ->
                        activeController.navigateToHistoryOffset(offset)
                    },
                    onReload = { activeController.reload() },
                    onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                    onOpenGlance = { viewModel.openGlance(null) },
                    onNavigateUrl = viewModel::navigateFocusedTab,
                    onClearSidebarSearchFocusRequest = viewModel::clearSidebarSearchFocusRequest,
                    onOpenSheet = viewModel::openSheet,
                    onOpenExtensions = { showExtensionsMenuPopup = true },
                    onToggleSplitView = { viewModel.toggleSplitView() },
                    onToggleReaderMode = viewModel::toggleReaderMode,
                    onToggleCompactMode = viewModel::toggleCompactFullscreen,
                    onToggleDesktopMode = viewModel::toggleDesktopMode,
                    onToggleMediaPlayPause = {
                        val shouldPlay = !uiState.mediaPlaybackState.isPlaying
                        primaryWebController.setMediaPlaying(shouldPlay)
                        secondaryWebController.setMediaPlaying(shouldPlay)
                    },
                    onToggleMediaMute = {
                        val mute = !uiState.mediaPlaybackState.isMuted
                        viewModel.setMediaMuted(mute)
                        primaryWebController.setMediaMuted(mute)
                        secondaryWebController.setMediaMuted(mute)
                    },
                    onDismissMediaBar = {
                        primaryWebController.stopAllMedia()
                        secondaryWebController.stopAllMedia()
                        viewModel.dismissMediaPlaybackBar()
                    },
                    onEnterPip = onEnterPip,
                    onClearUnpinnedTabs = {
                        viewModel.clearBrowsingDataAndUnpinnedTabs {
                            primaryWebController.clearWebData()
                            secondaryWebController.clearWebData()
                        }
                    },
                    onClearAllHistory = viewModel::clearAllHistory,
                    onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                    viewportGraphicsLayer = viewportGraphicsLayer,
                    backdropImageBitmap = capturedViewportBitmap,
                    softwareBlurredBackdrop = softwareBlurredBackdrop
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top Toolbar (when configured to Top)
                if (!uiState.isInPictureInPictureMode && !uiState.isCompactFullscreen && sidebarCfg.urlBarPosition == UrlBarPosition.TOP) {
                    FirefoxToolbar(
                        uiState = uiState,
                        isTopBar = true,
                        suggestions = uiState.searchSuggestions,
                        onFetchSuggestions = viewModel::fetchSearchSuggestions,
                        onClearSuggestions = viewModel::clearSearchSuggestions,
                        onToggleSidebar = viewModel::toggleSidebar,
                        onNavigateUrl = { url ->
                            viewModel.clearSearchSuggestions()
                            viewModel.navigateFocusedTab(url)
                        },
                        onGoBack = { activeController.goBack() },
                        onGoForward = { activeController.goForward() },
                        canGoBack = activeController.canGoBack() || uiState.canGoBack,
                        canGoForward = activeController.canGoForward() || uiState.canGoForward,
                        onReload = { activeController.reload() },
                        onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                        onOpenPrivacy = { viewModel.openSheet(ActiveModalSheet.PRIVACY_SHIELD) },
                        onOpenExtensions = { showExtensionsMenuPopup = true },
                        onOpenExtensionPopup = { ext ->
                            activeExtensionPopup = ext
                            showExtensionsMenuPopup = false
                        },
                        onToggleReader = viewModel::toggleReaderMode,
                        onToggleDesktopMode = viewModel::toggleDesktopMode,
                        onEnterPip = onEnterPip
                    )
                } else if (!uiState.isInPictureInPictureMode && !uiState.isCompactFullscreen) {
                    // When not in fullscreen mode and there is no top toolbar (Single Toolbar / Sidebar Only mode, or Bottom Toolbar mode),
                    // provide minimum overhead so the webpage and UI elements are placed just below the status bar
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                    )
                }

                if (uiState.pageLoadingProgress in 1..99) {
                    LinearProgressIndicator(
                        progress = { uiState.pageLoadingProgress / 100f },
                        color = Color(0xFFE4E4E7),
                        trackColor = Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                    )
                }

                AnimatedVisibility(visible = uiState.isFindInPageVisible) {
                    FindInPageBar(
                        query = uiState.findInPageQuery,
                        onQueryChange = {
                            viewModel.updateFindInPageQuery(it)
                            activeController.findInPage(it)
                        },
                        onClose = { viewModel.toggleFindInPage(false) }
                    )
                }

                // Web Viewport Area
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    val totalHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val primaryTab = uiState.activePrimaryTab
                    val secondaryTab = uiState.activeSecondaryTab

                    val activeTabIds = remember(primaryTab?.id, secondaryTab?.id, uiState.isSplitViewEnabled) {
                        setOfNotNull(primaryTab?.id, if (uiState.isSplitViewEnabled) secondaryTab?.id else null)
                    }
                    val allCachedWebViews = com.example.ui.components.ZenWebViewManager.getAllWebViews()
                    val backgroundWebViews = remember(allCachedWebViews.keys.toSet(), activeTabIds) {
                        allCachedWebViews.filterKeys { !activeTabIds.contains(it) }
                    }

                    if (backgroundWebViews.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(1.dp)
                                .graphicsLayer { alpha = 0.001f }
                        ) {
                            backgroundWebViews.forEach { (bgTabId, bgWebView) ->
                                androidx.compose.runtime.key("bg_wv_$bgTabId") {
                                    androidx.compose.ui.viewinterop.AndroidView(
                                        factory = {
                                            (bgWebView.parent as? android.view.ViewGroup)?.removeView(bgWebView)
                                            bgWebView
                                        },
                                        update = { _ -> },
                                        modifier = Modifier.size(1.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (primaryTab != null) {
                        if (uiState.isSplitViewEnabled && secondaryTab != null) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .weight(uiState.splitRatio)
                                        .fillMaxWidth()
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = 1.5.dp,
                                            color = if (uiState.focusedPane == FocusedPane.PRIMARY) {
                                                Color(0xFFF4F4F6)
                                            } else {
                                                Color(0xFF222226)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    androidx.compose.runtime.key(primaryTab.id) {
                                        ZenWebPaneView(
                                            tab = primaryTab,
                                            uiState = uiState,
                                            controller = primaryWebController,
                                            isFocusedPane = uiState.focusedPane == FocusedPane.PRIMARY,
                                            onFocusPane = { viewModel.setFocusedPane(FocusedPane.PRIMARY) },
                                            onNavigateUrl = viewModel::navigateFocusedTab,
                                            onReloadUnloadedTab = { viewModel.selectTab(it.id) },
                                            onPageVisited = { url, title ->
                                                viewModel.onWebPageVisited(primaryTab.id, url, title)
                                            },
                                            onProgressChanged = viewModel::updateNavigationProgress,
                                            onTrackerBlocked = viewModel::onTrackerBlocked,
                                            onMediaStateChanged = viewModel::updateMediaPlaybackState,
                                            onInstallExtensionFromAmo = viewModel::installExtensionFromAmoPage,
                                            onAddHomeShortcut = viewModel::addHomeShortcut,
                                            onUpdateHomeShortcut = viewModel::updateHomeShortcut,
                                            onDeleteHomeShortcut = viewModel::deleteHomeShortcut,
                                            onUpdateCustomTextPosition = { x, y ->
                                                viewModel.updateStartPageAppearance(
                                                    zenCustomTextXFraction = x,
                                                    zenCustomTextYFraction = y
                                                )
                                            },
                                            onOpenSheet = viewModel::openSheet,
                                            onToggleSplitView = { viewModel.toggleSplitView() },
                                            onOpenGlance = viewModel::openGlance,
                                            onOpenInNewTab = { url, forceSwitch -> viewModel.openLinkInNewTab(url, forceSwitch = forceSwitch) },
                                            onOpenInPrivateTab = { url -> viewModel.openLinkInNewTab(url, isPrivate = true) },
                                            onUpdateSwitchToNewTabImmediately = viewModel::updateSwitchToNewTabImmediately,
                                            onPopTabHistory = viewModel::popTabHistory,
                                            onPopTabForward = viewModel::popTabForward,
                                            onPopTabHistoryOffset = viewModel::popTabHistoryOffset,
                                            onGetTabHistoryEntries = viewModel::getTabHistoryEntries,
                                            onOpenSidebar = if (uiState.isInPictureInPictureMode) ({}) else ({ viewModel.setSidebarOpen(true) }),
                                            onSavePassword = viewModel::insertPassword
                                        )
                                    }
                                }

                                SplitViewDividerBar(
                                    primaryTitle = primaryTab.title,
                                    secondaryTitle = secondaryTab.title,
                                    focusedPane = uiState.focusedPane,
                                    onDragDelta = { deltaY ->
                                        val deltaFraction = deltaY / totalHeightPx
                                        viewModel.setSplitRatio(uiState.splitRatio + deltaFraction)
                                    },
                                    onSwapPanes = viewModel::swapSplitPanes,
                                    onPickDifferentTab = { viewModel.openSheet(ActiveModalSheet.SPLIT_TAB_PICKER) },
                                    onCloseSplit = { viewModel.toggleSplitView() }
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f - uiState.splitRatio)
                                        .fillMaxWidth()
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = 1.5.dp,
                                            color = if (uiState.focusedPane == FocusedPane.SECONDARY) {
                                                Color(0xFFF4F4F6)
                                            } else {
                                                Color(0xFF222226)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                ) {
                                    androidx.compose.runtime.key(secondaryTab.id) {
                                        ZenWebPaneView(
                                            tab = secondaryTab,
                                            uiState = uiState,
                                            controller = secondaryWebController,
                                            isFocusedPane = uiState.focusedPane == FocusedPane.SECONDARY,
                                            onFocusPane = { viewModel.setFocusedPane(FocusedPane.SECONDARY) },
                                            onNavigateUrl = viewModel::navigateFocusedTab,
                                            onReloadUnloadedTab = { viewModel.selectTab(it.id) },
                                            onPageVisited = { url, title ->
                                                viewModel.onWebPageVisited(secondaryTab.id, url, title)
                                            },
                                            onProgressChanged = viewModel::updateNavigationProgress,
                                            onTrackerBlocked = viewModel::onTrackerBlocked,
                                            onMediaStateChanged = viewModel::updateMediaPlaybackState,
                                            onInstallExtensionFromAmo = viewModel::installExtensionFromAmoPage,
                                            onAddHomeShortcut = viewModel::addHomeShortcut,
                                            onUpdateHomeShortcut = viewModel::updateHomeShortcut,
                                            onDeleteHomeShortcut = viewModel::deleteHomeShortcut,
                                            onUpdateCustomTextPosition = { x, y ->
                                                viewModel.updateStartPageAppearance(
                                                    zenCustomTextXFraction = x,
                                                    zenCustomTextYFraction = y
                                                )
                                            },
                                            onOpenSheet = viewModel::openSheet,
                                            onToggleSplitView = { viewModel.toggleSplitView() },
                                            onOpenGlance = viewModel::openGlance,
                                            onOpenInNewTab = { url, forceSwitch -> viewModel.openLinkInNewTab(url, forceSwitch = forceSwitch) },
                                            onOpenInPrivateTab = { url -> viewModel.openLinkInNewTab(url, isPrivate = true) },
                                            onUpdateSwitchToNewTabImmediately = viewModel::updateSwitchToNewTabImmediately,
                                            onPopTabHistory = viewModel::popTabHistory,
                                            onPopTabForward = viewModel::popTabForward,
                                            onPopTabHistoryOffset = viewModel::popTabHistoryOffset,
                                            onGetTabHistoryEntries = viewModel::getTabHistoryEntries,
                                            onOpenSidebar = if (uiState.isInPictureInPictureMode) ({}) else ({ viewModel.setSidebarOpen(true) }),
                                            onSavePassword = viewModel::insertPassword
                                        )
                                    }
                                }
                            }
                        } else {
                            androidx.compose.runtime.key(primaryTab.id) {
                                ZenWebPaneView(
                                    tab = primaryTab,
                                    uiState = uiState,
                                    controller = primaryWebController,
                                    isFocusedPane = true,
                                    onFocusPane = {},
                                    onNavigateUrl = viewModel::navigateFocusedTab,
                                    onReloadUnloadedTab = { viewModel.selectTab(it.id) },
                                    onPageVisited = { url, title ->
                                        viewModel.onWebPageVisited(primaryTab.id, url, title)
                                    },
                                    onProgressChanged = viewModel::updateNavigationProgress,
                                    onTrackerBlocked = viewModel::onTrackerBlocked,
                                    onMediaStateChanged = viewModel::updateMediaPlaybackState,
                                    onInstallExtensionFromAmo = viewModel::installExtensionFromAmoPage,
                                    onAddHomeShortcut = viewModel::addHomeShortcut,
                                    onUpdateHomeShortcut = viewModel::updateHomeShortcut,
                                    onDeleteHomeShortcut = viewModel::deleteHomeShortcut,
                                    onUpdateCustomTextPosition = { x, y ->
                                        viewModel.updateStartPageAppearance(
                                            zenCustomTextXFraction = x,
                                            zenCustomTextYFraction = y
                                        )
                                    },
                                    onOpenSheet = viewModel::openSheet,
                                    onToggleSplitView = { viewModel.toggleSplitView() },
                                    onOpenGlance = viewModel::openGlance,
                                    onOpenInNewTab = { url, forceSwitch -> viewModel.openLinkInNewTab(url, forceSwitch = forceSwitch) },
                                    onOpenInPrivateTab = { url -> viewModel.openLinkInNewTab(url, isPrivate = true) },
                                    onUpdateSwitchToNewTabImmediately = viewModel::updateSwitchToNewTabImmediately,
                                    onPopTabHistory = viewModel::popTabHistory,
                                    onPopTabForward = viewModel::popTabForward,
                                    onPopTabHistoryOffset = viewModel::popTabHistoryOffset,
                                    onGetTabHistoryEntries = viewModel::getTabHistoryEntries,
                                    onOpenSidebar = if (uiState.isInPictureInPictureMode) ({}) else ({ viewModel.setSidebarOpen(true) }),
                                    onSavePassword = viewModel::insertPassword
                                )
                            }
                        }
                    }

                    // Left edge swipe detector: respects configured horizontalSwipeMode
                    val swipeMode = sidebarCfg.horizontalSwipeMode
                    if (!uiState.isSidebarOpen && !uiState.isInPictureInPictureMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(36.dp)
                                .pointerInput(swipeMode) {
                                    var accumulatedDrag = 0f
                                    var hasTriggered = false
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            accumulatedDrag = 0f
                                            hasTriggered = false
                                        },
                                        onDragEnd = {
                                            accumulatedDrag = 0f
                                            hasTriggered = false
                                        },
                                        onDragCancel = {
                                            accumulatedDrag = 0f
                                            hasTriggered = false
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            accumulatedDrag += dragAmount
                                            if (!hasTriggered && accumulatedDrag > 32f) {
                                                hasTriggered = true
                                                change.consume()
                                                if (swipeMode == HorizontalSwipeNavigationMode.SWIPE_LEFT_PANEL_RIGHT_BACK) {
                                                    // Swipe Right (->) navigates back!
                                                    activeController.goBack()
                                                } else {
                                                    // Swipe Right (->) opens side panel!
                                                    viewModel.setSidebarOpen(true)
                                                }
                                            }
                                        }
                                    )
                                }
                                .testTag("left_edge_swipe_zone")
                        )
                    }

                    // Right edge swipe detector: respects configured horizontalSwipeMode
                    if (!uiState.isSidebarOpen && !uiState.isInPictureInPictureMode) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(36.dp)
                                .pointerInput(swipeMode) {
                                    var accumulatedDrag = 0f
                                    var hasTriggered = false
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            accumulatedDrag = 0f
                                            hasTriggered = false
                                        },
                                        onDragEnd = {
                                            accumulatedDrag = 0f
                                            hasTriggered = false
                                        },
                                        onDragCancel = {
                                            accumulatedDrag = 0f
                                            hasTriggered = false
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            accumulatedDrag += dragAmount
                                            if (!hasTriggered && accumulatedDrag < -32f) {
                                                hasTriggered = true
                                                change.consume()
                                                if (swipeMode == HorizontalSwipeNavigationMode.SWIPE_LEFT_PANEL_RIGHT_BACK) {
                                                    // Swipe Left (<-) opens side panel!
                                                    viewModel.setSidebarOpen(true)
                                                } else {
                                                    // Swipe Left (<-) goes back!
                                                    activeController.goBack()
                                                }
                                            }
                                        }
                                    )
                                }
                                .testTag("right_edge_swipe_zone")
                        )
                    }

                    // Customizable Side Edge Bar (No Exit Compact floating button; just the adjustable bar unless hidden in Compact Mode)
                    val isRightSide = sidebarCfg.position == SidebarPosition.RIGHT
                    val showEdgeBar = !sidebarPinnedOpen && !uiState.isInPictureInPictureMode &&
                        !sidebarCfg.hideEdgeBarAlways &&
                        !(uiState.isCompactFullscreen && sidebarCfg.hideEdgeBarInCompactMode)

                    if (showEdgeBar) {
                        val barHeightDp = sidebarCfg.edgeBarLengthDp.dp
                        val barWidthDp = sidebarCfg.edgeBarWidthDp.dp
                        val maxOffsetPx = (constraints.maxHeight - with(androidx.compose.ui.platform.LocalDensity.current) { barHeightDp.toPx() })
                            .coerceAtLeast(0f)
                        val yOffsetPx = (maxOffsetPx * sidebarCfg.edgeBarVerticalOffsetFraction).roundToInt()

                        Box(
                            modifier = Modifier
                                .align(if (isRightSide) Alignment.TopEnd else Alignment.TopStart)
                                .offset { IntOffset(0, yOffsetPx) }
                                .width(barWidthDp)
                                .height(barHeightDp)
                                .clip(
                                    RoundedCornerShape(
                                        topStart = if (isRightSide) 4.dp else 0.dp,
                                        bottomStart = if (isRightSide) 4.dp else 0.dp,
                                        topEnd = if (isRightSide) 0.dp else 4.dp,
                                        bottomEnd = if (isRightSide) 0.dp else 4.dp
                                    )
                                )
                                .background(Color(0xFF52525B).copy(alpha = 0.72f))
                                .clickable { viewModel.toggleSidebar() }
                                .pointerInput(isRightSide) {
                                    detectHorizontalDragGestures { change, dragAmount ->
                                        change.consume()
                                        if ((!isRightSide && dragAmount > 16f) || (isRightSide && dragAmount < -16f)) {
                                            viewModel.setSidebarOpen(true)
                                        }
                                    }
                                }
                                .testTag("sidebar_edge_pill_handle")
                        )
                    }
                }

                // Bottom Toolbar (Default)
                if (!uiState.isInPictureInPictureMode && !uiState.isCompactFullscreen && sidebarCfg.urlBarPosition == UrlBarPosition.BOTTOM) {
                    FirefoxToolbar(
                        uiState = uiState,
                        isTopBar = false,
                        suggestions = uiState.searchSuggestions,
                        onFetchSuggestions = viewModel::fetchSearchSuggestions,
                        onClearSuggestions = viewModel::clearSearchSuggestions,
                        onToggleSidebar = viewModel::toggleSidebar,
                        onNavigateUrl = { url ->
                            viewModel.clearSearchSuggestions()
                            viewModel.navigateFocusedTab(url)
                        },
                        onGoBack = { activeController.goBack() },
                        onGoForward = { activeController.goForward() },
                        canGoBack = activeController.canGoBack() || uiState.canGoBack,
                        canGoForward = activeController.canGoForward() || uiState.canGoForward,
                        onReload = { activeController.reload() },
                        onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                        onOpenPrivacy = { viewModel.openSheet(ActiveModalSheet.PRIVACY_SHIELD) },
                        onOpenExtensions = { showExtensionsMenuPopup = true },
                        onOpenExtensionPopup = { ext ->
                            activeExtensionPopup = ext
                            showExtensionsMenuPopup = false
                        },
                        onToggleReader = viewModel::toggleReaderMode,
                        onToggleDesktopMode = viewModel::toggleDesktopMode,
                        onEnterPip = onEnterPip
                    )
                }
            }

            if (sidebarPinnedOpen && sidebarCfg.position == SidebarPosition.RIGHT && !uiState.isInPictureInPictureMode) {
                ZenSidebarPanel(
                    uiState = uiState,
                    onSelectWorkspace = viewModel::selectWorkspace,
                    onSelectTab = viewModel::selectTab,
                    onCreateNewTab = viewModel::onNewTabButtonClicked,
                    onNewTabSwipeGesture = viewModel::handleNewTabSwipeGesture,
                    onTabSwipeGesture = viewModel::performTabSwipeAction,
                    onUnloadTab = viewModel::unloadTab,
                    onCloseTab = viewModel::closeTab,
                    onTogglePinTab = viewModel::togglePinTab,
                    onToggleBookmarkTab = viewModel::toggleBookmarkTab,
                    onToggleEssentialTab = viewModel::toggleEssentialTab,
                    onDuplicateTab = viewModel::duplicateTab,
                    onSaveTabOffline = handleSaveTabOffline,
                    onMoveTabToWorkspace = viewModel::moveTabToWorkspace,
                    onReorderTabs = viewModel::reorderTabs,
                    onMergeTabsIntoSplitView = viewModel::openSplitViewWithTabs,
                    onConfirmSplitWithHomepage = viewModel::confirmSplitSelectionWithHomepageIfSingle,
                    onCancelSplitSelection = viewModel::cancelSplitTabSelection,
                    onGoBack = { activeController.goBack() },
                    onGoForward = { activeController.goForward() },
                    canGoBack = activeController.canGoBack() || uiState.canGoBack,
                    canGoForward = activeController.canGoForward() || uiState.canGoForward,
                    onGetBackHistory = {
                        val fromWeb = activeController.getBackHistory()
                        if (fromWeb.isNotEmpty()) fromWeb else {
                            val curTabId = uiState.focusedTab?.id
                            if (curTabId != null) viewModel.getTabHistoryEntries(curTabId) else emptyList()
                        }
                    },
                    onNavigateHistoryOffset = { offset ->
                        activeController.navigateToHistoryOffset(offset)
                    },
                    onReload = { activeController.reload() },
                    onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                    onOpenGlance = { viewModel.openGlance(null) },
                    onNavigateUrl = viewModel::navigateFocusedTab,
                    onClearSidebarSearchFocusRequest = viewModel::clearSidebarSearchFocusRequest,
                    onOpenSheet = viewModel::openSheet,
                    onOpenExtensions = { showExtensionsMenuPopup = true },
                    onToggleSplitView = { viewModel.toggleSplitView() },
                    onToggleReaderMode = viewModel::toggleReaderMode,
                    onToggleCompactMode = viewModel::toggleCompactFullscreen,
                    onToggleDesktopMode = viewModel::toggleDesktopMode,
                    onToggleMediaPlayPause = {
                        val shouldPlay = !uiState.mediaPlaybackState.isPlaying
                        primaryWebController.setMediaPlaying(shouldPlay)
                        secondaryWebController.setMediaPlaying(shouldPlay)
                    },
                    onToggleMediaMute = {
                        val mute = !uiState.mediaPlaybackState.isMuted
                        viewModel.setMediaMuted(mute)
                        primaryWebController.setMediaMuted(mute)
                        secondaryWebController.setMediaMuted(mute)
                    },
                    onDismissMediaBar = {
                        primaryWebController.stopAllMedia()
                        secondaryWebController.stopAllMedia()
                        viewModel.dismissMediaPlaybackBar()
                    },
                    onEnterPip = onEnterPip,
                    onClearUnpinnedTabs = {
                        viewModel.clearBrowsingDataAndUnpinnedTabs {
                            primaryWebController.clearWebData()
                            secondaryWebController.clearWebData()
                        }
                    },
                    viewportGraphicsLayer = viewportGraphicsLayer,
                    backdropImageBitmap = capturedViewportBitmap,
                    softwareBlurredBackdrop = softwareBlurredBackdrop
                )
            }
        }

        // Slide-over Blurred Sidebar Drawer on Mobile:
        // When the side panel is in focus, ONLY the side panel itself is dim/tinted/blurred;
        // the area beside and below it not occupied by the side panel remains completely normal (Color.Transparent)!
        if (!sidebarPinnedOpen && !uiState.isInPictureInPictureMode) {
            AnimatedVisibility(
                visible = uiState.isSidebarOpen,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.setSidebarOpen(false) }
                        )
                        .testTag("sidebar_scrim_backdrop")
                )
            }

            val isRight = sidebarCfg.position == SidebarPosition.RIGHT
            AnimatedVisibility(
                visible = uiState.isSidebarOpen,
                enter = slideInHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    initialOffsetX = { fullWidth -> if (isRight) fullWidth else -fullWidth }
                ),
                exit = slideOutHorizontally(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    targetOffsetX = { fullWidth -> if (isRight) fullWidth else -fullWidth }
                ),
                modifier = Modifier.align(if (isRight) Alignment.CenterEnd else Alignment.CenterStart)
            ) {
                ZenSidebarPanel(
                    uiState = uiState,
                    onSelectWorkspace = viewModel::selectWorkspace,
                    onSelectTab = viewModel::selectTab,
                    onCreateNewTab = viewModel::onNewTabButtonClicked,
                    onNewTabSwipeGesture = viewModel::handleNewTabSwipeGesture,
                    onTabSwipeGesture = viewModel::performTabSwipeAction,
                    onUnloadTab = viewModel::unloadTab,
                    onCloseTab = viewModel::closeTab,
                    onTogglePinTab = viewModel::togglePinTab,
                    onToggleBookmarkTab = viewModel::toggleBookmarkTab,
                    onToggleEssentialTab = viewModel::toggleEssentialTab,
                    onDuplicateTab = viewModel::duplicateTab,
                    onSaveTabOffline = handleSaveTabOffline,
                    onMoveTabToWorkspace = viewModel::moveTabToWorkspace,
                    onReorderTabs = viewModel::reorderTabs,
                    onMergeTabsIntoSplitView = viewModel::openSplitViewWithTabs,
                    onConfirmSplitWithHomepage = viewModel::confirmSplitSelectionWithHomepageIfSingle,
                    onCancelSplitSelection = viewModel::cancelSplitTabSelection,
                    onGoBack = { activeController.goBack() },
                    onGoForward = { activeController.goForward() },
                    canGoBack = activeController.canGoBack() || uiState.canGoBack,
                    canGoForward = activeController.canGoForward() || uiState.canGoForward,
                    onGetBackHistory = {
                        val fromWeb = activeController.getBackHistory()
                        if (fromWeb.isNotEmpty()) fromWeb else {
                            val curTabId = uiState.focusedTab?.id
                            if (curTabId != null) viewModel.getTabHistoryEntries(curTabId) else emptyList()
                        }
                    },
                    onNavigateHistoryOffset = { offset ->
                        activeController.navigateToHistoryOffset(offset)
                    },
                    onReload = { activeController.reload() },
                    onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                    onOpenGlance = { viewModel.openGlance(null) },
                    onNavigateUrl = viewModel::navigateFocusedTab,
                    onClearSidebarSearchFocusRequest = viewModel::clearSidebarSearchFocusRequest,
                    onOpenSheet = viewModel::openSheet,
                    onOpenExtensions = { showExtensionsMenuPopup = true },
                    onToggleSplitView = { viewModel.toggleSplitView() },
                    onToggleReaderMode = viewModel::toggleReaderMode,
                    onToggleCompactMode = viewModel::toggleCompactFullscreen,
                    onToggleDesktopMode = viewModel::toggleDesktopMode,
                    onToggleMediaPlayPause = {
                        val shouldPlay = !uiState.mediaPlaybackState.isPlaying
                        primaryWebController.setMediaPlaying(shouldPlay)
                        secondaryWebController.setMediaPlaying(shouldPlay)
                    },
                    onToggleMediaMute = {
                        val mute = !uiState.mediaPlaybackState.isMuted
                        viewModel.setMediaMuted(mute)
                        primaryWebController.setMediaMuted(mute)
                        secondaryWebController.setMediaMuted(mute)
                    },
                    onDismissMediaBar = {
                        primaryWebController.stopAllMedia()
                        secondaryWebController.stopAllMedia()
                        viewModel.dismissMediaPlaybackBar()
                    },
                    onEnterPip = onEnterPip,
                    onClearUnpinnedTabs = {
                        viewModel.clearBrowsingDataAndUnpinnedTabs {
                            primaryWebController.clearWebData()
                            secondaryWebController.clearWebData()
                        }
                    },
                    viewportGraphicsLayer = viewportGraphicsLayer,
                    backdropImageBitmap = capturedViewportBitmap,
                    softwareBlurredBackdrop = softwareBlurredBackdrop
                )
            }
        }

        // Single Toolbar Center-Top Search Pop-Up Overlay (activates search bar + keyboard on center-top)
        AnimatedVisibility(
            visible = uiState.isCenterTopSearchPopupVisible && !uiState.isInPictureInPictureMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CenterTopSearchPopupOverlay(
                isPrivate = uiState.isCurrentContextPrivate,
                suggestions = uiState.searchSuggestions,
                onFetchSuggestions = viewModel::fetchSearchSuggestions,
                onClearSuggestions = viewModel::clearSearchSuggestions,
                onNavigateUrl = { url ->
                    viewModel.clearSearchSuggestions()
                    viewModel.navigateFocusedTab(url)
                    viewModel.setCenterTopSearchPopupVisible(false)
                },
                onDismiss = {
                    viewModel.clearSearchSuggestions()
                    viewModel.setCenterTopSearchPopupVisible(false)
                }
            )
        }

        // First-Time / On-Demand Sidebar & Features Tutorial Dialog
        if (uiState.showTutorialDialog) {
            ZenTutorialDialog(
                onDismiss = viewModel::dismissTutorial
            )
        }

        val showStatusBanner = uiState.statusBannerMessage != null && run {
            val msg = uiState.statusBannerMessage ?: ""
            val isTabClose = msg.contains("Tab closed", ignoreCase = true)
            if (isTabClose && !uiState.sidebarConfig.showTabCloseNotification) return@run false
            if (!uiState.sidebarConfig.showActionNotifications) return@run false
            true
        }

        // Transient Toast Status Pill
        AnimatedVisibility(
            visible = showStatusBanner,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF141417),
                border = BorderStroke(1.dp, Color(0xFF2E2E34))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.statusBannerMessage ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFF4F4F6)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.showBanner(null) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss notification",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        when (uiState.activeSheet) {
            ActiveModalSheet.SIDEBAR_STUDIO -> {
                SidebarCustomizationStudioSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onOpenSheet = viewModel::openSheet,
                    onTriggerTutorial = viewModel::triggerTutorial,
                    onUpdatePosition = viewModel::updateSidebarPosition,
                    onUpdateWidthMode = viewModel::updateSidebarWidthMode,
                    onUpdateUiScale = viewModel::updateSidebarUiScale,
                    onUpdateUrlBarPosition = viewModel::updateUrlBarPosition,
                    onUpdateSingleToolbarNewTabBehavior = viewModel::updateSingleToolbarNewTabBehavior,
                    onUpdateHideStatusBarFullscreen = viewModel::updateHideStatusBarFullscreen,
                    onUpdateSidebarGlassAndColor = viewModel::updateSidebarGlassAndColor,
                    onUpdateGesturesAndSession = { right, left, newLeft, newRight, backSwipe, closePrivate, startup, hours ->
                        viewModel.updateGestureAndSessionConfig(
                            tabSwipeRight = right,
                            tabSwipeLeft = left,
                            newTabSwipeLeft = newLeft,
                            newTabSwipeRight = newRight,
                            closePrivateOnClose = closePrivate,
                            startupBehavior = startup,
                            homepageHoursThreshold = hours,
                            backSwipeSidebarMode = backSwipe
                        )
                    },
                    onUpdateEdgeBar = viewModel::updateEdgeBarSettings,
                    onToggleOption = viewModel::toggleSidebarOption,
                    onMoveSection = viewModel::moveSidebarSection,
                    onUpdateOpenLinksInGlanceAutomatically = viewModel::toggleOpenLinksInGlanceAutomatically,
                    onUpdateOpenInNewTab = viewModel::updateOpenInNewTab,
                    onUpdateOpenHistoryInNewTab = viewModel::updateOpenHistoryInNewTab,
                    onUpdateSwitchToNewTabImmediately = viewModel::updateSwitchToNewTabImmediately,
                    onUpdateNewTabPosition = viewModel::updateNewTabPosition,
                    onUpdateNotificationSettings = viewModel::updateNotificationSettings,
                    onUpdateFakeSearchConfig = { aiText, trigger, headline, snippet ->
                        viewModel.updateFakeSearchConfig(
                            customAiOverviewText = aiText,
                            fakeSearchQueryTrigger = trigger,
                            fakeSearchHeadline = headline,
                            fakeSearchSnippet = snippet
                        )
                    },
                    onOpenFakeSearch = { query ->
                        viewModel.createNewTab(initialInput = "zen://search?q=${android.net.Uri.encode(query)}", switchToTab = true)
                    },
                    onUpdateHistoryDeletionInterval = viewModel::updateHistoryDeletionInterval,
                    onUpdateHorizontalSwipeMode = viewModel::updateHorizontalSwipeMode,
                    onResetDefaults = viewModel::resetSidebarDefaults
                )
            }

            ActiveModalSheet.SIDEBAR_LAYOUT_EDITOR -> {
                SidebarLayoutEditorSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onToggleSidebarNavControls = viewModel::updateShowSidebarNavControls,
                    onToggleSidebarOption = viewModel::toggleSidebarOption,
                    onMoveSection = viewModel::moveSidebarSection,
                    onToggleToolItem = { tool, _ -> viewModel.toggleSidebarToolItem(tool) },
                    onMoveToolItem = viewModel::moveSidebarToolItem,
                    onToggleTopToolItem = { tool, _ -> viewModel.toggleTopToolbarToolItem(tool) },
                    onMoveTopToolItem = viewModel::moveTopToolbarToolItem
                )
            }

            ActiveModalSheet.ESSENTIALS_MANAGER -> {
                EssentialsManagerSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onAddEssentialSite = viewModel::addEssentialSite,
                    onRemoveEssentialSite = viewModel::removeEssentialSite
                )
            }

            ActiveModalSheet.START_PAGE_CUSTOMIZER -> {
                StartPageCustomizerSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onUpdateStartPage = viewModel::updateStartPageAppearance
                )
            }

            ActiveModalSheet.SEARCH_ENGINE_SETTINGS -> {
                SearchEngineSettingsSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onSelectNormalEngine = { id ->
                        viewModel.updatePrivacyToggle(normalSearchEngineId = id)
                    },
                    onSelectPrivateEngine = { id ->
                        viewModel.updatePrivacyToggle(privateSearchEngineId = id)
                    },
                    onAddCustomEngine = viewModel::addCustomSearchEngine,
                    onDeleteCustomEngine = viewModel::deleteCustomSearchEngine
                )
            }

            ActiveModalSheet.PRIVACY_SHIELD -> {
                PrivacyShieldSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onOpenSearchSettings = {
                        viewModel.openSheet(ActiveModalSheet.SEARCH_ENGINE_SETTINGS)
                    },
                    onOpenPasswordManager = {
                        viewModel.openSheet(ActiveModalSheet.PASSWORD_MANAGER)
                    },
                    onUpdatePrivacy = { blockTrackers, httpsOnly, stripParams, fingerprint, cookies ->
                        viewModel.updatePrivacyToggle(
                            blockTrackers = blockTrackers,
                            httpsEverywhere = httpsOnly,
                            stripParams = stripParams,
                            fingerprintProtection = fingerprint,
                            blockThirdPartyCookies = cookies
                        )
                    },
                    onUpdateAdBlockerLevel = { level ->
                        viewModel.updatePrivacyToggle(adBlockerLevel = level)
                    },
                    onUpdateHistoryDeletionInterval = viewModel::updateHistoryDeletionInterval,
                    onClearSession = {
                        viewModel.clearBrowsingDataAndUnpinnedTabs {
                            primaryWebController.clearWebData()
                            secondaryWebController.clearWebData()
                        }
                    }
                )
            }

            ActiveModalSheet.FIREFOX_EXTENSIONS -> {
                FirefoxExtensionsSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onInstallOrUninstall = viewModel::installOrUninstallFirefoxExtension,
                    onToggleEnabled = viewModel::toggleFirefoxExtensionEnabled,
                    onToggleAllowInPrivate = viewModel::toggleFirefoxExtensionPrivateAllowed,
                    onRunExtensionOnCurrentTab = { ext ->
                        viewModel.closeSheet()
                        viewModel.setSidebarOpen(false)
                        if (uiState.focusedTab?.url == "zen://home") {
                            viewModel.showBanner("Open a webpage first to use '${ext.name}'")
                        } else {
                            activeController.executeExtensionAction(ext)
                        }
                    },
                    onLaunchUblockElementPicker = {
                        viewModel.closeSheet()
                        viewModel.setSidebarOpen(false)
                        if (uiState.focusedTab?.url == "zen://home") {
                            viewModel.showBanner("Open a webpage first to use uBlock Element Picker")
                        } else {
                            activeController.launchUblockElementPicker()
                        }
                    },
                    onImportXpiFile = viewModel::importFirefoxXpiPackage,
                    onUpdateExtensionDetails = viewModel::updateFirefoxExtensionDetails,
                    onAddCustomExtension = viewModel::addCustomFirefoxExtension,
                    onInstallFromAmoUrl = { amoUrl ->
                        viewModel.installExtensionFromAmoPage(amoUrl, null)
                    },
                    onOpenAmoStoreInBrowser = {
                        viewModel.closeSheet()
                        viewModel.createNewTab("https://addons.mozilla.org/en-US/firefox/")
                    },
                    onOpenZenMods = {
                        viewModel.openSheet(ActiveModalSheet.ZEN_MODS)
                    },
                    onInstallUserscript = viewModel::installUserscript
                )
            }

            ActiveModalSheet.ZEN_MODS -> {
                ZenModsStoreSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onToggleMod = viewModel::toggleZenMod
                )
            }

            ActiveModalSheet.WORKSPACE_EDITOR -> {
                WorkspaceManagerSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onSelectWorkspace = viewModel::selectWorkspace,
                    onCreateWorkspace = viewModel::createWorkspace,
                    onDeleteWorkspace = viewModel::deleteWorkspace
                )
            }

            ActiveModalSheet.SPLIT_TAB_PICKER -> {
                SplitTabPickerSheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onPickTabForSplit = { tabId -> viewModel.toggleSplitView(tabId) }
                )
            }

            ActiveModalSheet.LIBRARY -> {
                ZenLibrarySheet(
                    uiState = uiState,
                    onDismiss = viewModel::closeSheet,
                    onNavigateUrl = { url -> viewModel.navigateFocusedTab(url) },
                    onOpenInNewTab = { url -> viewModel.createNewTab(initialInput = url, switchToTab = true) },
                    onToggleBookmark = viewModel::toggleBookmarkTab,
                    onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                    onClearAllHistory = viewModel::clearAllHistory,
                    onDeleteDownloadItem = viewModel::deleteDownloadItem,
                    onClearAllDownloads = viewModel::clearAllDownloads,
                    onDeleteOfflinePage = viewModel::deleteOfflinePage,
                    onClearAllOfflinePages = viewModel::clearAllOfflinePages,
                    onOpenOfflinePage = viewModel::openOfflinePage,
                    onSaveCurrentPageOffline = {
                        activeController.capturePageHtmlForOffline { html ->
                            viewModel.saveCurrentPageForOffline(uiState.focusedTab?.id ?: "", html)
                        }
                    }
                )
            }

            ActiveModalSheet.PASSWORD_MANAGER -> {
                PasswordManagerSheet(
                    uiState = uiState,
                    onClose = viewModel::closeSheet,
                    onInsertPassword = viewModel::insertPassword,
                    onDeletePassword = viewModel::deletePassword,
                    onClearAllPasswords = viewModel::clearAllPasswords
                )
            }

            ActiveModalSheet.NONE -> {}
        }

        // At a Glance Full Peek Card Overlay
        uiState.activeGlanceUrl?.let { glanceUrl ->
            ZenGlanceOverlay(
                glanceUrl = glanceUrl,
                onClose = viewModel::closeGlance,
                onPromoteToTab = { url ->
                    viewModel.closeGlance()
                    viewModel.createNewTab(url)
                },
                onOpenInSplitView = { url ->
                    viewModel.closeGlance()
                    viewModel.openSplitViewWithUrl(url)
                }
            )
        }

        // Extensions Menu Popup (listing installed extensions with logos)
        if (showExtensionsMenuPopup) {
            val activeTabUrl = uiState.focusedTab?.url ?: "zen://home"
            val activeDomain = remember(activeTabUrl) {
                ZenPrivacyEngine.extractHostDisplay(activeTabUrl)
            }
            ZenExtensionsMenuPopup(
                uiState = uiState,
                activeTabDomain = activeDomain,
                onDismiss = { showExtensionsMenuPopup = false },
                onSelectExtension = { ext ->
                    showExtensionsMenuPopup = false
                    activeExtensionPopup = ext
                },
                onToggleExtension = viewModel::toggleFirefoxExtensionEnabled,
                onOpenManageExtensions = {
                    viewModel.openSheet(ActiveModalSheet.FIREFOX_EXTENSIONS)
                },
                onSavePageOffline = {
                    activeController.capturePageHtmlForOffline { html ->
                        viewModel.saveCurrentPageForOffline(uiState.focusedTab?.id ?: "", html)
                    }
                }
            )
        }

        // Laptop-style Floating Window for selected extension options
        val currentExtPopup = activeExtensionPopup
        if (currentExtPopup != null) {
            val liveExt = uiState.firefoxExtensions.find { it.id == currentExtPopup.id } ?: currentExtPopup
            val activeTabUrl = uiState.focusedTab?.url ?: "zen://home"
            val activeDomain = remember(activeTabUrl) {
                ZenPrivacyEngine.extractHostDisplay(activeTabUrl)
            }
            ZenExtensionFloatingPopup(
                extension = liveExt,
                activeTabDomain = activeDomain,
                onDismiss = { activeExtensionPopup = null },
                onBackToMenu = {
                    activeExtensionPopup = null
                    showExtensionsMenuPopup = true
                },
                onToggleEnabled = {
                    viewModel.toggleFirefoxExtensionEnabled(liveExt)
                },
                onLaunchUblockElementPicker = {
                    activeExtensionPopup = null
                    activeController.launchUblockElementPicker()
                },
                onRunScript = { script ->
                    activeController.webView?.evaluateJavascript(
                        "(function(){ try { $script } catch(e){} })();",
                        null
                    )
                },
                onReloadTab = {
                    activeController.reload()
                },
                onOpenExtensionSettings = {
                    activeExtensionPopup = null
                    viewModel.openSheet(ActiveModalSheet.FIREFOX_EXTENSIONS)
                }
            )
        }
    }
}

@Composable
private fun CenterTopSearchPopupOverlay(
    isPrivate: Boolean,
    suggestions: List<com.example.data.SearchSuggestionItem>,
    onFetchSuggestions: (String) -> Unit,
    onClearSuggestions: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(60)
        runCatching {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("center_top_search_popup_overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF121216).copy(alpha = 0.96f),
                border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                shadowElevation = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .testTag("center_top_search_popup_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPrivate) {
                        ShadeGlassIcon(tint = Color(0xFFF4F4F6), size = 16.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = {
                            query = it
                            onFetchSuggestions(it)
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFF4F4F6)
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF4F4F6)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                if (query.isNotBlank()) {
                                    keyboardController?.hide()
                                    onNavigateUrl(query)
                                }
                            }
                        ),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (query.isEmpty()) {
                                    Text(
                                        text = "Search or enter address...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF71717A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .testTag("center_top_search_input")
                    )
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                                onClearSuggestions()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (suggestions.isNotEmpty()) {
                SearchSuggestionsDropdown(
                    suggestions = suggestions,
                    onSelectSuggestion = { target ->
                        keyboardController?.hide()
                        onNavigateUrl(target)
                    }
                )
            }
        }
    }
}

@Composable
private fun FirefoxToolbar(
    uiState: ZenBrowserUiState,
    isTopBar: Boolean,
    suggestions: List<com.example.data.SearchSuggestionItem> = emptyList(),
    onFetchSuggestions: (String) -> Unit = {},
    onClearSuggestions: () -> Unit = {},
    onToggleSidebar: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    canGoBack: Boolean = uiState.canGoBack,
    canGoForward: Boolean = uiState.canGoForward,
    onReload: () -> Unit,
    onGoHome: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenExtensions: () -> Unit,
    onOpenExtensionPopup: (com.example.data.FirefoxExtensionEntity) -> Unit = {},
    onToggleReader: () -> Unit,
    onToggleDesktopMode: () -> Unit = {},
    onEnterPip: () -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val addressEditFocusRequester = remember { FocusRequester() }
    val focusedUrl = uiState.focusedTab?.url ?: "zen://home"
    val isPrivate = uiState.isCurrentContextPrivate
    var isEditingUrl by remember { mutableStateOf(false) }
    var textValue by remember(focusedUrl) {
        mutableStateOf(if (focusedUrl == "zen://home") "" else focusedUrl)
    }

    LaunchedEffect(isEditingUrl) {
        if (isEditingUrl) {
            delay(50)
            runCatching {
                addressEditFocusRequester.requestFocus()
                keyboardController?.show()
            }
        } else {
            onClearSuggestions()
        }
    }

    val insetModifier = if (isTopBar) {
        if (!uiState.isCompactFullscreen) {
            Modifier.windowInsetsPadding(WindowInsets.statusBars)
        } else {
            Modifier
        }
    } else {
        Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    }

    Column(
        modifier = insetModifier
            .fillMaxWidth()
            .background(Color(0xFF0A0A0D))
            .testTag("zen_floating_command_bar")
    ) {
        if (!isTopBar) {
            HorizontalDivider(color = Color(0xFF222226), thickness = 1.dp)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!isEditingUrl) {
                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_sidebar_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewSidebar,
                        contentDescription = "Sidebar",
                        tint = Color(0xFFD4D4D8),
                        modifier = Modifier.size(19.dp)
                    )
                }

                IconButton(
                    onClick = onGoBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) Color(0xFFD4D4D8) else Color(0xFF52525B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onGoForward,
                    enabled = canGoForward,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (canGoForward) Color(0xFFD4D4D8) else Color(0xFF52525B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    onClick = {
                        textValue = if (focusedUrl == "zen://home") "" else focusedUrl
                        isEditingUrl = true
                    },
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF141417),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("address_bar_pill")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenPrivacy,
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("address_shield_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Tracking Protection",
                                tint = if (uiState.privacyConfig.blockTrackersAndAds) Color(0xFFD4D4D8) else Color(0xFF71717A),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        if (isPrivate) {
                            ShadeGlassIcon(tint = Color(0xFFF4F4F6), size = 14.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                        } else if (focusedUrl.startsWith("https")) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        } else if (focusedUrl == "zen://home") {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = if (focusedUrl == "zen://home") {
                                "Search or enter URL..."
                            } else {
                                ZenPrivacyEngine.extractHostDisplay(focusedUrl)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (focusedUrl == "zen://home") Color(0xFF71717A) else Color(0xFFF4F4F6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (focusedUrl != "zen://home") {
                            IconButton(
                                onClick = onToggleReader,
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("cmd_reader_mode_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Reader View",
                                    tint = if (uiState.isReaderModeEnabled) Color(0xFFF4F4F6) else Color(0xFF71717A),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = onReload,
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("nav_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onGoHome,
                            modifier = Modifier
                                .size(26.dp)
                                .testTag("nav_home_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home Page",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                // Dedicated uBlock Origin quick button if installed
                val ublockExt = uiState.firefoxExtensions.find { it.id == "ff_ublock_origin" && it.isInstalled }
                if (ublockExt != null) {
                    IconButton(
                        onClick = { onOpenExtensionPopup(ublockExt) },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("toolbar_ublock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "uBlock Origin Quick Options",
                            tint = if (ublockExt.isEnabled) Color(0xFF34D399) else Color(0xFF71717A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenExtensions,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("toolbar_extensions_button")
                ) {
                    val ublockOn = uiState.firefoxExtensions.any { it.id == "ff_ublock_origin" && it.isInstalled && it.isEnabled }
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = "Firefox Add-ons & Extensions Menu",
                        tint = if (ublockOn) Color(0xFF34D399) else Color(0xFFD4D4D8),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (focusedUrl != "zen://home" && focusedUrl.isNotBlank()) {
                    val isDesktop = uiState.isFocusedTabDesktopMode
                    IconButton(
                        onClick = onToggleDesktopMode,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("toolbar_desktop_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DesktopWindows,
                            contentDescription = if (isDesktop) "Desktop Site: Active (Tap for Mobile)" else "Request Desktop Site",
                            tint = if (isDesktop) Color(0xFF60A5FA) else Color(0xFFD4D4D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (uiState.mediaPlaybackState.isActive || uiState.sidebarConfig.playInWindowPipEnabled) {
                    IconButton(
                        onClick = onEnterPip,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("toolbar_pip_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPicture,
                            contentDescription = "Play in window (PiP)",
                            tint = if (uiState.mediaPlaybackState.isPlaying) Color(0xFF60A5FA) else Color(0xFFD4D4D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Slim, non-clipped pill editing bar inside the 50.dp toolbar
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF141417),
                    border = BorderStroke(1.dp, Color(0xFF71717A)),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = textValue,
                            onValueChange = {
                                textValue = it
                                onFetchSuggestions(it)
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFF4F4F6)
                            ),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF4F4F6)),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(
                                onGo = {
                                    if (textValue.isNotBlank()) {
                                        onNavigateUrl(textValue)
                                    }
                                    isEditingUrl = false
                                    onClearSuggestions()
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (textValue.isEmpty()) {
                                        Text(
                                            text = "Search or enter URL...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF71717A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(addressEditFocusRequester)
                                .testTag("address_bar_edit_input")
                        )
                        IconButton(
                            onClick = {
                                textValue = ""
                                onClearSuggestions()
                                isEditingUrl = false
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isEditingUrl && suggestions.isNotEmpty()) {
            SearchSuggestionsDropdown(
                suggestions = suggestions,
                onSelectSuggestion = { target ->
                    onNavigateUrl(target)
                    isEditingUrl = false
                    onClearSuggestions()
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        }

        if (isTopBar) {
            HorizontalDivider(color = Color(0xFF222226), thickness = 1.dp)
        }
    }
}

@Composable
private fun SplitViewDividerBar(
    primaryTitle: String,
    secondaryTitle: String,
    focusedPane: FocusedPane,
    onDragDelta: (Float) -> Unit,
    onSwapPanes: () -> Unit,
    onPickDifferentTab: () -> Unit,
    onCloseSplit: () -> Unit
) {
    Surface(
        color = Color(0xFF101014),
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount)
                }
            }
            .testTag("split_view_divider_bar")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Drag to Resize Split View",
                    tint = Color(0xFFF4F4F6),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (focusedPane == FocusedPane.PRIMARY) {
                        "● $primaryTitle  /  $secondaryTitle  (Double-tap pane to focus)"
                    } else {
                        "$primaryTitle  /  ● $secondaryTitle  (Double-tap pane to focus)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD4D4D8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPickDifferentTab,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Splitscreen,
                        contentDescription = "Select split tabs in Side Panel",
                        tint = Color(0xFFD4D4D8),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onSwapPanes,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("swap_split_panes_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = "Swap panes",
                        tint = Color(0xFFD4D4D8),
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = onCloseSplit,
                    modifier = Modifier
                        .size(26.dp)
                        .testTag("close_split_view_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Split View",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FindInPageBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = Color(0xFF101014),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("find_in_page_bar")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Find in page") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("find_in_page_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onClose) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close find")
            }
        }
    }
}

@Composable
private fun SearchSuggestionsDropdown(
    suggestions: List<com.example.data.SearchSuggestionItem>,
    onSelectSuggestion: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF141418),
        border = BorderStroke(1.dp, Color(0xFF27272A)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .testTag("search_suggestions_dropdown")
    ) {
        LazyColumn(
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            items(suggestions) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectSuggestion(item.target) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when (item.type) {
                        com.example.data.SuggestionType.BOOKMARK -> Icons.Default.Bookmark
                        com.example.data.SuggestionType.HISTORY -> Icons.Default.History
                        com.example.data.SuggestionType.SEARCH_QUERY -> Icons.Default.Search
                    }
                    val iconTint = when (item.type) {
                        com.example.data.SuggestionType.BOOKMARK -> Color(0xFFF59E0B)
                        com.example.data.SuggestionType.HISTORY -> Color(0xFF60A5FA)
                        com.example.data.SuggestionType.SEARCH_QUERY -> Color(0xFFA1A1AA)
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = item.type.name,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF4F4F6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val sub = item.subtitle
                        if (!sub.isNullOrBlank()) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF222226), thickness = 0.5.dp)
            }
        }
    }
}


