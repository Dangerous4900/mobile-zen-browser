package com.example.ui

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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.SwapVert
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
import com.example.data.SidebarPosition
import com.example.data.UrlBarPosition
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.components.EssentialsManagerSheet
import com.example.ui.components.FirefoxExtensionsSheet
import com.example.ui.components.PrivacyShieldSheet
import com.example.ui.components.SearchEngineSettingsSheet
import com.example.ui.components.ShadeGlassIcon
import com.example.ui.components.SidebarCustomizationStudioSheet
import com.example.ui.components.SidebarLayoutEditorSheet
import com.example.ui.components.SplitTabPickerSheet
import com.example.ui.components.StartPageCustomizerSheet
import com.example.ui.components.WorkspaceManagerSheet
import com.example.ui.components.ZenLibrarySheet
import com.example.ui.components.ZenModsStoreSheet
import com.example.ui.components.ZenSidebarPanel
import com.example.ui.components.ZenTutorialDialog
import com.example.ui.components.ZenWebPaneView
import com.example.ui.components.ZenWebViewController
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

@Composable
fun ZenBrowserScreen(
    uiState: ZenBrowserUiState,
    viewModel: ZenBrowserViewModel
) {
    val primaryWebController = remember { ZenWebViewController() }
    val secondaryWebController = remember { ZenWebViewController() }
    val activeController = if (uiState.isSplitViewEnabled && uiState.focusedPane == FocusedPane.SECONDARY) {
        secondaryWebController
    } else {
        primaryWebController
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

    // Predictive Back Handler: detects whether the user swiped back from the LEFT edge or RIGHT edge!
    // - Swiping from the LEFT edge opens the Side Panel in one swipe.
    // - Swiping from the RIGHT edge navigates the page back (or opens the Side Panel if at the start page / in Single Swipe mode).
    PredictiveBackHandler(enabled = true) { progress ->
        var swipeEdge = BackEventCompat.EDGE_LEFT
        try {
            progress.collect { backEvent ->
                swipeEdge = backEvent.swipeEdge
            }
            when {
                uiState.activeSheet != ActiveModalSheet.NONE -> viewModel.closeSheet()
                uiState.isCenterTopSearchPopupVisible -> viewModel.setCenterTopSearchPopupVisible(false)
                uiState.isSidebarOpen -> viewModel.setSidebarOpen(false)
                else -> {
                    when (uiState.sidebarConfig.backSwipeSidebarMode) {
                        BackSwipeSidebarMode.LEFT_SIDEBAR_RIGHT_BACK -> {
                            if (swipeEdge == BackEventCompat.EDGE_LEFT) {
                                // Swiped back from the LEFT -> Open Side Panel immediately in one swipe
                                viewModel.setSidebarOpen(true)
                            } else {
                                // Swiped back from the RIGHT (or 3-button nav bar) -> Page goes back if possible, otherwise open Side Panel
                                if (uiState.canGoBack) {
                                    activeController.goBack()
                                } else {
                                    viewModel.setSidebarOpen(true)
                                }
                            }
                        }

                        BackSwipeSidebarMode.SINGLE_SWIPE_SIDEBAR -> {
                            viewModel.setSidebarOpen(true)
                        }

                        BackSwipeSidebarMode.GO_BACK_THEN_SIDEBAR -> {
                            if (uiState.isCompactFullscreen) {
                                viewModel.setSidebarOpen(true)
                            } else if (uiState.canGoBack) {
                                activeController.goBack()
                            } else {
                                viewModel.setSidebarOpen(true)
                            }
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
            if (sidebarPinnedOpen && sidebarCfg.position == SidebarPosition.LEFT) {
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
                    onMoveTabToWorkspace = viewModel::moveTabToWorkspace,
                    onReorderTabs = viewModel::reorderTabs,
                    onMergeTabsIntoSplitView = viewModel::openSplitViewWithTabs,
                    onConfirmSplitWithHomepage = viewModel::confirmSplitSelectionWithHomepageIfSingle,
                    onCancelSplitSelection = viewModel::cancelSplitTabSelection,
                    onGoBack = { activeController.goBack() },
                    onGoForward = { activeController.goForward() },
                    onReload = { activeController.reload() },
                    onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                    onNavigateUrl = viewModel::navigateFocusedTab,
                    onClearSidebarSearchFocusRequest = viewModel::clearSidebarSearchFocusRequest,
                    onOpenSheet = viewModel::openSheet,
                    onToggleSplitView = { viewModel.toggleSplitView() },
                    onToggleReaderMode = viewModel::toggleReaderMode,
                    onToggleCompactMode = viewModel::toggleCompactFullscreen,
                    onToggleMediaPlayPause = {
                        primaryWebController.toggleMediaPlayPause()
                        secondaryWebController.toggleMediaPlayPause()
                    },
                    onDismissMediaBar = {
                        primaryWebController.stopAllMedia()
                        secondaryWebController.stopAllMedia()
                        viewModel.dismissMediaPlaybackBar()
                    },
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Top Toolbar (when configured to Top)
                if (!uiState.isCompactFullscreen && sidebarCfg.urlBarPosition == UrlBarPosition.TOP) {
                    FirefoxToolbar(
                        uiState = uiState,
                        isTopBar = true,
                        onToggleSidebar = viewModel::toggleSidebar,
                        onNavigateUrl = viewModel::navigateFocusedTab,
                        onGoBack = { activeController.goBack() },
                        onGoForward = { activeController.goForward() },
                        onReload = { activeController.reload() },
                        onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                        onOpenPrivacy = { viewModel.openSheet(ActiveModalSheet.PRIVACY_SHIELD) },
                        onOpenExtensions = { viewModel.openSheet(ActiveModalSheet.FIREFOX_EXTENSIONS) },
                        onToggleReader = viewModel::toggleReaderMode
                    )
                } else if (!uiState.isCompactFullscreen) {
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
                                        onToggleSplitView = { viewModel.toggleSplitView() }
                                    )
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
                                        onToggleSplitView = { viewModel.toggleSplitView() }
                                    )
                                }
                            }
                        } else {
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
                                onToggleSplitView = { viewModel.toggleSplitView() }
                            )
                        }
                    }

                    // Customizable Side Edge Bar (No Exit Compact floating button; just the adjustable bar unless hidden in Compact Mode)
                    val isRightSide = sidebarCfg.position == SidebarPosition.RIGHT
                    val showEdgeBar = !sidebarPinnedOpen &&
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
                if (!uiState.isCompactFullscreen && sidebarCfg.urlBarPosition == UrlBarPosition.BOTTOM) {
                    FirefoxToolbar(
                        uiState = uiState,
                        isTopBar = false,
                        onToggleSidebar = viewModel::toggleSidebar,
                        onNavigateUrl = viewModel::navigateFocusedTab,
                        onGoBack = { activeController.goBack() },
                        onGoForward = { activeController.goForward() },
                        onReload = { activeController.reload() },
                        onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                        onOpenPrivacy = { viewModel.openSheet(ActiveModalSheet.PRIVACY_SHIELD) },
                        onOpenExtensions = { viewModel.openSheet(ActiveModalSheet.FIREFOX_EXTENSIONS) },
                        onToggleReader = viewModel::toggleReaderMode
                    )
                }
            }

            if (sidebarPinnedOpen && sidebarCfg.position == SidebarPosition.RIGHT) {
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
                    onMoveTabToWorkspace = viewModel::moveTabToWorkspace,
                    onReorderTabs = viewModel::reorderTabs,
                    onMergeTabsIntoSplitView = viewModel::openSplitViewWithTabs,
                    onConfirmSplitWithHomepage = viewModel::confirmSplitSelectionWithHomepageIfSingle,
                    onCancelSplitSelection = viewModel::cancelSplitTabSelection,
                    onGoBack = { activeController.goBack() },
                    onGoForward = { activeController.goForward() },
                    onReload = { activeController.reload() },
                    onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                    onNavigateUrl = viewModel::navigateFocusedTab,
                    onClearSidebarSearchFocusRequest = viewModel::clearSidebarSearchFocusRequest,
                    onOpenSheet = viewModel::openSheet,
                    onToggleSplitView = { viewModel.toggleSplitView() },
                    onToggleReaderMode = viewModel::toggleReaderMode,
                    onToggleCompactMode = viewModel::toggleCompactFullscreen,
                    onToggleMediaPlayPause = {
                        primaryWebController.toggleMediaPlayPause()
                        secondaryWebController.toggleMediaPlayPause()
                    },
                    onDismissMediaBar = {
                        primaryWebController.stopAllMedia()
                        secondaryWebController.stopAllMedia()
                        viewModel.dismissMediaPlaybackBar()
                    },
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
        if (!sidebarPinnedOpen) {
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
                    onMoveTabToWorkspace = viewModel::moveTabToWorkspace,
                    onReorderTabs = viewModel::reorderTabs,
                    onMergeTabsIntoSplitView = viewModel::openSplitViewWithTabs,
                    onConfirmSplitWithHomepage = viewModel::confirmSplitSelectionWithHomepageIfSingle,
                    onCancelSplitSelection = viewModel::cancelSplitTabSelection,
                    onGoBack = { activeController.goBack() },
                    onGoForward = { activeController.goForward() },
                    onReload = { activeController.reload() },
                    onGoHome = { viewModel.navigateFocusedTab("zen://home") },
                    onNavigateUrl = viewModel::navigateFocusedTab,
                    onClearSidebarSearchFocusRequest = viewModel::clearSidebarSearchFocusRequest,
                    onOpenSheet = viewModel::openSheet,
                    onToggleSplitView = { viewModel.toggleSplitView() },
                    onToggleReaderMode = viewModel::toggleReaderMode,
                    onToggleCompactMode = viewModel::toggleCompactFullscreen,
                    onToggleMediaPlayPause = {
                        primaryWebController.toggleMediaPlayPause()
                        secondaryWebController.toggleMediaPlayPause()
                    },
                    onDismissMediaBar = {
                        primaryWebController.stopAllMedia()
                        secondaryWebController.stopAllMedia()
                        viewModel.dismissMediaPlaybackBar()
                    },
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
            visible = uiState.isCenterTopSearchPopupVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            CenterTopSearchPopupOverlay(
                isPrivate = uiState.isCurrentContextPrivate,
                onNavigateUrl = viewModel::navigateFocusedTab,
                onDismiss = { viewModel.setCenterTopSearchPopupVisible(false) }
            )
        }

        // First-Time / On-Demand Sidebar & Features Tutorial Dialog
        if (uiState.showTutorialDialog) {
            ZenTutorialDialog(
                onDismiss = viewModel::dismissTutorial
            )
        }

        // Transient Toast Status Pill
        AnimatedVisibility(
            visible = uiState.statusBannerMessage != null,
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
                Text(
                    text = uiState.statusBannerMessage ?: "",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFF4F4F6),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
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
                    onUpdateWorkspaceTheme = viewModel::updateWorkspaceTheme,
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
                    onMoveToolItem = viewModel::moveSidebarToolItem
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
                    onUpdatePrivacy = { blockTrackers, httpsOnly, stripParams, fingerprint, cookies ->
                        viewModel.updatePrivacyToggle(
                            blockTrackers = blockTrackers,
                            httpsEverywhere = httpsOnly,
                            stripParams = stripParams,
                            fingerprintProtection = fingerprint,
                            blockThirdPartyCookies = cookies
                        )
                    },
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
                        viewModel.createNewTab("https://addons.mozilla.org/en-US/android/")
                    },
                    onOpenZenMods = {
                        viewModel.openSheet(ActiveModalSheet.ZEN_MODS)
                    }
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
                    onToggleBookmark = viewModel::toggleBookmarkTab,
                    onDeleteHistoryEntry = viewModel::deleteHistoryEntry,
                    onClearAllHistory = viewModel::clearAllHistory,
                    onDeleteDownloadItem = viewModel::deleteDownloadItem,
                    onClearAllDownloads = viewModel::clearAllDownloads
                )
            }

            ActiveModalSheet.NONE -> {}
        }
    }
}

@Composable
private fun CenterTopSearchPopupOverlay(
    isPrivate: Boolean,
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
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF121216).copy(alpha = 0.96f),
            border = BorderStroke(1.dp, Color(0xFF3F3F46)),
            shadowElevation = 14.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 8.dp, start = 16.dp, end = 16.dp)
                .widthIn(max = 520.dp)
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
                    onValueChange = { query = it },
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
                        onClick = { query = "" },
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
    }
}

@Composable
private fun FirefoxToolbar(
    uiState: ZenBrowserUiState,
    isTopBar: Boolean,
    onToggleSidebar: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    onReload: () -> Unit,
    onGoHome: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenExtensions: () -> Unit,
    onToggleReader: () -> Unit
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
                    enabled = uiState.canGoBack,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("nav_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (uiState.canGoBack) Color(0xFFD4D4D8) else Color(0xFF52525B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onGoForward,
                    enabled = uiState.canGoForward,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("nav_forward_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Forward",
                        tint = if (uiState.canGoForward) Color(0xFFD4D4D8) else Color(0xFF52525B),
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

                IconButton(
                    onClick = onOpenExtensions,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("toolbar_extensions_button")
                ) {
                    val ublockOn = uiState.firefoxExtensions.any { it.id == "ff_ublock_origin" && it.isInstalled && it.isEnabled }
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = "Firefox Add-ons & uBlock Origin",
                        tint = if (ublockOn) Color(0xFF34D399) else Color(0xFFD4D4D8),
                        modifier = Modifier.size(18.dp)
                    )
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
                            onValueChange = { textValue = it },
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
