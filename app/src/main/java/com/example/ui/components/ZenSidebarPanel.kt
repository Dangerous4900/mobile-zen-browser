package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.data.MediaPlaybackState
import com.example.data.SidebarColorMode
import com.example.data.SidebarSectionType
import com.example.data.SidebarToolItem
import com.example.data.TabEntity
import com.example.data.TabSwipeAction
import com.example.data.TopToolbarToolItem
import com.example.data.UrlBarPosition
import com.example.data.WorkspaceEntity
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.ActiveModalSheet
import com.example.ui.ZenBrowserUiState
import kotlin.math.abs
import kotlin.math.roundToInt

fun workspaceIconForKey(key: String): ImageVector = when (key) {
    "code" -> Icons.Default.Code
    "science" -> Icons.Default.Science
    "palette" -> Icons.Default.Palette
    "grid" -> Icons.Default.GridView
    else -> Icons.Default.Spa
}

/**
 * Custom Sunglasses / Shade Glass Vector Icon for Private Tabs & Private Workspaces
 */
@Composable
fun ShadeGlassIcon(
    tint: Color = Color(0xFFF4F4F6),
    size: Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = w * 0.1f

        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.06f, h * 0.34f),
            size = Size(w * 0.38f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.56f, h * 0.34f),
            size = Size(w * 0.38f, h * 0.36f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.04f, h * 0.36f),
            end = Offset(w * 0.96f, h * 0.36f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        val path = Path().apply {
            moveTo(w * 0.06f, h * 0.36f)
            lineTo(w * 0.20f, h * 0.18f)
            moveTo(w * 0.94f, h * 0.36f)
            lineTo(w * 0.80f, h * 0.18f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ZenSidebarPanel(
    uiState: ZenBrowserUiState,
    onSelectWorkspace: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onCreateNewTab: () -> Unit,
    onNewTabSwipeGesture: (isLeftSwipe: Boolean) -> Unit,
    onTabSwipeGesture: (tab: TabEntity, isRightSwipe: Boolean) -> Unit,
    onUnloadTab: (TabEntity) -> Unit,
    onCloseTab: (String) -> Unit,
    onTogglePinTab: (TabEntity) -> Unit,
    onToggleBookmarkTab: (TabEntity) -> Unit,
    onToggleEssentialTab: (TabEntity) -> Unit,
    onDuplicateTab: (TabEntity) -> Unit = {},
    onSaveTabOffline: (TabEntity) -> Unit = {},
    onMoveTabToWorkspace: (TabEntity, String) -> Unit,
    onReorderTabs: (fromTabId: String, toIndex: Int, isPinnedSection: Boolean) -> Unit,
    onMergeTabsIntoSplitView: (primaryTabId: String, secondaryTabId: String) -> Unit,
    onConfirmSplitWithHomepage: () -> Unit,
    onCancelSplitSelection: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onGoBack: () -> Unit,
    onGoForward: () -> Unit,
    canGoBack: Boolean = uiState.canGoBack,
    canGoForward: Boolean = uiState.canGoForward,
    onGetBackHistory: () -> List<com.example.data.HistoryNavEntry> = { emptyList() },
    onNavigateHistoryOffset: (Int) -> Unit = {},
    onReload: () -> Unit,
    onGoHome: () -> Unit,
    onOpenGlance: () -> Unit = {},
    onClearSidebarSearchFocusRequest: () -> Unit,
    onOpenSheet: (ActiveModalSheet) -> Unit,
    onOpenExtensions: () -> Unit = {},
    onToggleSplitView: () -> Unit,
    onToggleReaderMode: () -> Unit,
    onToggleCompactMode: () -> Unit,
    onToggleDesktopMode: () -> Unit = {},
    onToggleMediaPlayPause: () -> Unit,
    onToggleMediaMute: () -> Unit = {},
    onDismissMediaBar: () -> Unit,
    onEnterPip: () -> Unit = {},
    onClearUnpinnedTabs: () -> Unit,
    onClearAllHistory: () -> Unit = {},
    onDeleteHistoryEntry: (String) -> Unit = {},
    viewportGraphicsLayer: androidx.compose.ui.graphics.layer.GraphicsLayer? = null,
    backdropImageBitmap: ImageBitmap? = null,
    softwareBlurredBackdrop: ImageBitmap? = null,
    modifier: Modifier = Modifier
) {
    if (uiState.isInPictureInPictureMode) return

    val sidebarCfg = uiState.sidebarConfig
    val enabledTools = sidebarCfg.enabledSidebarTools
    val uiScale = sidebarCfg.sidebarUiScale.coerceIn(0.75f, 1.35f)
    val baseDensity = LocalDensity.current
    val scaledSidebarDensity = remember(baseDensity, uiScale) {
        androidx.compose.ui.unit.Density(
            density = baseDensity.density * uiScale,
            fontScale = baseDensity.fontScale * uiScale
        )
    }
    val widthDp = (sidebarCfg.widthMode.widthDp * uiScale.coerceIn(0.85f, 1.18f)).dp
    var sidebarUrlQuery by remember(uiState.focusedTab?.url) {
        mutableStateOf(
            uiState.focusedTab?.url?.takeIf { it != "zen://home" } ?: ""
        )
    }
    var showEmptyAreaEditMenu by remember { mutableStateOf(false) }
    val sidebarSearchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val density = scaledSidebarDensity
    val rowHeightPx = with(density) { 46.dp.toPx() }

    // Hold-and-drag state for vertical tabs (supports both reordering and dropping onto another tab to open Split View)
    var draggingTabId by remember { mutableStateOf<String?>(null) }
    var draggingTabStartIndex by remember { mutableStateOf(-1) }
    var draggingIsPinnedSection by remember { mutableStateOf(false) }
    var dragAccumulatedY by remember { mutableFloatStateOf(0f) }
    var hoveredMergeTargetTabId by remember { mutableStateOf<String?>(null) }
    var hoveredReorderTargetIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(uiState.requestSidebarSearchFocus, uiState.isSidebarOpen) {
        if (uiState.requestSidebarSearchFocus && uiState.isSidebarOpen && sidebarCfg.urlBarPosition == UrlBarPosition.SIDEBAR_ONLY) {
            sidebarUrlQuery = ""
            runCatching {
                sidebarSearchFocusRequester.requestFocus()
                keyboardController?.show()
            }
            onClearSidebarSearchFocusRequest()
        }
    }

    // Resolve Side Panel Color (Normal vs Custom Color) + Blur + Opacity + Liquid Glass
    val baseSurfaceColor = if (sidebarCfg.sidebarColorMode == SidebarColorMode.CUSTOM) {
        Color(sidebarCfg.sidebarCustomColorHex)
    } else {
        Color(0xFF0A0A0E)
    }
    val isLiquidGlass = sidebarCfg.liquidGlassEnabled
    val blurFraction = sidebarCfg.glassBlurIntensity.coerceIn(0f, 1f)
    val opacityFraction = sidebarCfg.sidebarOpacity.coerceIn(0.05f, 1f)

    // Use the configured Side Panel Opacity directly so 5% is nearly transparent and 100% is solid
    val panelAlpha = if (isLiquidGlass) {
        (opacityFraction * 0.82f).coerceIn(0.05f, 0.96f)
    } else {
        opacityFraction
    }

    val panelShape = if (isLiquidGlass) RoundedCornerShape(18.dp) else RoundedCornerShape(0.dp)
    val panelBorderBrush = if (isLiquidGlass) {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f),
                baseSurfaceColor.copy(alpha = (panelAlpha + 0.15f).coerceIn(0.2f, 0.85f)),
                Color.White.copy(alpha = 0.16f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF222226), Color(0xFF222226))
        )
    }

    val currentWsIndex = remember(uiState.workspaces, uiState.activeWorkspaceId) {
        uiState.workspaces.indexOfFirst { it.id == uiState.activeWorkspaceId }.coerceAtLeast(0)
    }

    Surface(
        modifier = modifier
            .width(widthDp)
            .fillMaxHeight()
            .padding(if (isLiquidGlass) 6.dp else 0.dp)
            .clip(panelShape)
            .border(
                width = if (isLiquidGlass) 1.2.dp else 1.dp,
                brush = panelBorderBrush,
                shape = panelShape
            )
            .pointerInput(uiState.workspaces, currentWsIndex) {
                var accumulatedDragX = 0f
                var hasTriggeredSwipe = false
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDragX = 0f; hasTriggeredSwipe = false },
                    onDragEnd = { accumulatedDragX = 0f; hasTriggeredSwipe = false },
                    onDragCancel = { accumulatedDragX = 0f; hasTriggeredSwipe = false },
                    onHorizontalDrag = { change, dragAmount ->
                        accumulatedDragX += dragAmount
                        if (!hasTriggeredSwipe && kotlin.math.abs(accumulatedDragX) > 42f) {
                            hasTriggeredSwipe = true
                            change.consume()
                            if (accumulatedDragX < -42f) {
                                // Swiped Left (<-) -> Next Workspace or New Workspace
                                if (currentWsIndex < uiState.workspaces.lastIndex) {
                                    onSelectWorkspace(uiState.workspaces[currentWsIndex + 1].id)
                                } else {
                                    onOpenSheet(ActiveModalSheet.WORKSPACE_EDITOR)
                                }
                            } else if (accumulatedDragX > 42f) {
                                // Swiped Right (->) -> Previous Workspace or New Workspace
                                if (currentWsIndex > 0) {
                                    onSelectWorkspace(uiState.workspaces[currentWsIndex - 1].id)
                                } else {
                                    onOpenSheet(ActiveModalSheet.WORKSPACE_EDITOR)
                                }
                            }
                        }
                    }
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = { showEmptyAreaEditMenu = true }
            )
            .testTag("zen_sidebar_panel"),
        shape = panelShape,
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 1. Live Blurred Backdrop Layer directly inside the Side Panel Surface!
            // Because this layer lives inside ZenSidebarPanel's own Surface, it never desyncs or flickers off/on
            // during slide animations, tab switches, or WebView recompositions.
            if (blurFraction > 0.01f) {
                val blurPx = with(density) { (blurFraction * 36f).dp.toPx() }.coerceAtLeast(1f)
                var panelBoundsInRoot by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            val pos = coords.localToRoot(Offset.Zero)
                            panelBoundsInRoot = androidx.compose.ui.geometry.Rect(
                                offset = pos,
                                size = androidx.compose.ui.geometry.Size(
                                    coords.size.width.toFloat(),
                                    coords.size.height.toFloat()
                                )
                            )
                        }
                        .graphicsLayer {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    blurPx,
                                    blurPx,
                                    android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                        .drawWithContent {
                            val swBmp = softwareBlurredBackdrop
                            val rawBmp = backdropImageBitmap
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && swBmp != null) {
                                drawImage(
                                    image = swBmp,
                                    dstSize = androidx.compose.ui.unit.IntSize(
                                        size.width.roundToInt().coerceAtLeast(1),
                                        size.height.roundToInt().coerceAtLeast(1)
                                    )
                                )
                            } else {
                                // Draw fallback snapshot first (so even if a hardware WebView surface frame is skipped, it never flashes blank)
                                if (rawBmp != null) {
                                    drawImage(
                                        image = rawBmp,
                                        dstSize = androidx.compose.ui.unit.IntSize(
                                            size.width.roundToInt().coerceAtLeast(1),
                                            size.height.roundToInt().coerceAtLeast(1)
                                        )
                                    )
                                }
                                // Draw live recorded Compose & WebView viewport aligned to the Side Panel's exact screen coordinates
                                if (viewportGraphicsLayer != null) {
                                    val offsetX = -panelBoundsInRoot.left
                                    val offsetY = -panelBoundsInRoot.top
                                    drawContext.canvas.save()
                                    drawContext.canvas.translate(offsetX, offsetY)
                                    drawLayer(viewportGraphicsLayer)
                                    drawContext.canvas.restore()
                                }
                            }
                        }
                )
            }

            // 2. Side Panel Opacity Tint Layer (strictly controlled by the Side Panel Opacity slider: 5% -> 100%)
            if (isLiquidGlass) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = (0.10f + opacityFraction * 0.12f).coerceIn(0.05f, 0.28f)),
                                    baseSurfaceColor.copy(alpha = panelAlpha),
                                    baseSurfaceColor.copy(alpha = (panelAlpha + 0.08f).coerceAtMost(0.96f)),
                                    Color.White.copy(alpha = (0.06f + opacityFraction * 0.08f).coerceIn(0.03f, 0.20f))
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(800f, 1800f)
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(baseSurfaceColor.copy(alpha = panelAlpha))
                )
            }

            // Long-press on empty area of Side Panel -> "Edit Side Panel" popup menu
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                DropdownMenu(
                    expanded = showEmptyAreaEditMenu,
                    onDismissRequest = { showEmptyAreaEditMenu = false },
                    containerColor = Color(0xFF141417)
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Side Panel Layout & Tools") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Side Panel",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            showEmptyAreaEditMenu = false
                            onOpenSheet(ActiveModalSheet.SIDEBAR_LAYOUT_EDITOR)
                        },
                        modifier = Modifier.testTag("sidebar_empty_area_edit_menu_item")
                    )
                    DropdownMenuItem(
                        text = { Text("Customize Essentials Space") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            showEmptyAreaEditMenu = false
                            onOpenSheet(ActiveModalSheet.ESSENTIALS_MANAGER)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Zen Settings & Customization") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            showEmptyAreaEditMenu = false
                            onOpenSheet(ActiveModalSheet.SIDEBAR_STUDIO)
                        }
                    )
                }
            }

            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides scaledSidebarDensity
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                // Backward & Forward Navigation Controls Row on the Side Panel
                if (sidebarCfg.showSidebarNavControls && enabledTools.contains(SidebarToolItem.NAV_CONTROLS)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            sidebarCfg.enabledTopToolbarTools.forEach { tool ->
                                when (tool) {
                                    TopToolbarToolItem.BACK -> {
                                        var showBackHistoryMenu by remember { mutableStateOf(false) }
                                        var backHistoryItems by remember { mutableStateOf<List<com.example.data.HistoryNavEntry>>(emptyList()) }

                                        Box {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .combinedClickable(
                                                        enabled = canGoBack,
                                                        onClick = onGoBack,
                                                        onLongClick = {
                                                            val items = onGetBackHistory()
                                                            if (items.isNotEmpty()) {
                                                                backHistoryItems = items
                                                                showBackHistoryMenu = true
                                                            }
                                                        }
                                                    )
                                                    .testTag("sidebar_nav_back_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back (Hold for previous pages)",
                                                    tint = if (canGoBack) Color(0xFFF4F4F6) else Color(0xFF52525B),
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showBackHistoryMenu,
                                                onDismissRequest = { showBackHistoryMenu = false },
                                                modifier = Modifier
                                                    .background(Color(0xFF18181B))
                                                    .testTag("back_history_dropdown_menu")
                                            ) {
                                                Text(
                                                    text = "Previous Pages",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFA1A1AA),
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                                )
                                                HorizontalDivider(color = Color(0xFF27272A))
                                                backHistoryItems.forEach { entry ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                                                Text(
                                                                    text = entry.title,
                                                                    style = MaterialTheme.typography.bodyMedium,
                                                                    color = Color(0xFFF4F4F6),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                if (entry.url.isNotBlank()) {
                                                                    Text(
                                                                        text = entry.url,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = Color(0xFF71717A),
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        onClick = {
                                                            showBackHistoryMenu = false
                                                            onNavigateHistoryOffset(-entry.stepsBack)
                                                        },
                                                        modifier = Modifier.testTag("back_history_entry_${entry.stepsBack}")
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    TopToolbarToolItem.FORWARD -> {
                                        IconButton(
                                            onClick = onGoForward,
                                            enabled = canGoForward,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_forward_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Forward",
                                                tint = if (canGoForward) Color(0xFFF4F4F6) else Color(0xFF52525B),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.RELOAD -> {
                                        IconButton(
                                            onClick = onReload,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_reload_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Reload",
                                                tint = Color(0xFFD4D4D8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.HOME -> {
                                        IconButton(
                                            onClick = onGoHome,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_home_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Home,
                                                contentDescription = "Home Page",
                                                tint = Color(0xFFD4D4D8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.AT_A_GLANCE -> {
                                        IconButton(
                                            onClick = onOpenGlance,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_glance_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Visibility,
                                                contentDescription = "At a Glance",
                                                tint = Color(0xFF8AB4F8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.TRANSLATE -> {
                                        IconButton(
                                            onClick = { onOpenSheet(ActiveModalSheet.FIREFOX_EXTENSIONS) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_translate_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Translate,
                                                contentDescription = "Translate",
                                                tint = Color(0xFF34D399),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.READER_MODE -> {
                                        IconButton(
                                            onClick = onToggleReaderMode,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_reader_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.MenuBook,
                                                contentDescription = "Reader Mode",
                                                tint = if (uiState.isReaderModeEnabled) Color(0xFF8AB4F8) else Color(0xFFD4D4D8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.EXTENSIONS -> {
                                        IconButton(
                                            onClick = onOpenExtensions,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_extensions_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Extension,
                                                contentDescription = "Extensions",
                                                tint = Color(0xFFD4D4D8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.PRIVACY_SHIELD -> {
                                        IconButton(
                                            onClick = { onOpenSheet(ActiveModalSheet.PRIVACY_SHIELD) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_shield_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = "Privacy Shield",
                                                tint = Color(0xFFD4D4D8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.DESKTOP_MODE -> {
                                        IconButton(
                                            onClick = onToggleDesktopMode,
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("sidebar_nav_desktop_mode_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DesktopWindows,
                                                contentDescription = "Desktop Site",
                                                tint = if (uiState.isFocusedTabDesktopMode) Color(0xFF60A5FA) else Color(0xFFD4D4D8),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    TopToolbarToolItem.EDIT_LAYOUT -> {
                                        IconButton(
                                            onClick = { onOpenSheet(ActiveModalSheet.SIDEBAR_LAYOUT_EDITOR) },
                                            modifier = Modifier
                                                .size(30.dp)
                                                .testTag("sidebar_edit_layout_top_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Side Panel",
                                                tint = Color(0xFFA1A1AA),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!sidebarCfg.enabledTopToolbarTools.contains(TopToolbarToolItem.EDIT_LAYOUT)) {
                            IconButton(
                                onClick = { onOpenSheet(ActiveModalSheet.SIDEBAR_LAYOUT_EDITOR) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("sidebar_edit_layout_top_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Side Panel",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Single Toolbar search bar with top overhead space for the status bar & slim pill-rounded shape
                if (sidebarCfg.urlBarPosition == UrlBarPosition.SIDEBAR_ONLY &&
                    enabledTools.contains(SidebarToolItem.SEARCH_BAR)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF121216).copy(alpha = if (isLiquidGlass) 0.50f else 0.88f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isLiquidGlass) Color.White.copy(alpha = 0.24f) else Color(0xFF27272A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
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
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = sidebarUrlQuery,
                                onValueChange = { sidebarUrlQuery = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFF4F4F6)
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF4F4F6)),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        onNavigateUrl(sidebarUrlQuery)
                                        keyboardController?.hide()
                                    }
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (sidebarUrlQuery.isEmpty()) {
                                            Text(
                                                text = "Search or enter URL...",
                                                style = MaterialTheme.typography.bodySmall,
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
                                    .focusRequester(sidebarSearchFocusRequester)
                                    .testTag("sidebar_url_input")
                            )
                            if (sidebarUrlQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFF71717A),
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { sidebarUrlQuery = "" }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                }

                // Split Screen Tab Selection Banner when Split View is triggered
                AnimatedVisibility(visible = uiState.isSelectingSplitTabs) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF18181D),
                        border = BorderStroke(1.dp, Color(0xFF71717A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .testTag("split_selection_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Split View (${uiState.selectedSplitTabIds.size}/2 selected)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFF4F4F6),
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = onCancelSplitSelection,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel Split Selection",
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Tap a 2nd tab to open both in Split View, or split the selected tab with the Homepage.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                            Button(
                                onClick = onConfirmSplitWithHomepage,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF4F4F6),
                                    contentColor = Color(0xFF000000)
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                                    .testTag("split_with_homepage_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Splitscreen,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Split Selected Tab + Homepage",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Scrollable Top & Middle Area: Ordered dynamically by sidebarCfg.sectionOrder
                // Supports long-pressing any empty area to show "Edit Side Panel"
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = { showEmptyAreaEditMenu = true }
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    sidebarCfg.sectionOrder.forEach { section ->
                        when (section) {
                            SidebarSectionType.ESSENTIALS -> {
                                if (sidebarCfg.showEssentialGrid) {
                                    item(key = "section_essentials") {
                                        EssentialTabsGridSection(
                                            essentialTabs = uiState.essentialTabs,
                                            activeTabId = uiState.focusedTab?.id,
                                            isLiquidGlass = isLiquidGlass,
                                            workspaces = uiState.workspaces,
                                            onSelectTab = onSelectTab,
                                            onOpenEssentialsManager = { onOpenSheet(ActiveModalSheet.ESSENTIALS_MANAGER) },
                                            onUnloadTab = onUnloadTab,
                                            onCloseTab = onCloseTab,
                                            onTogglePinTab = onTogglePinTab,
                                            onToggleBookmarkTab = onToggleBookmarkTab,
                                            onToggleEssentialTab = onToggleEssentialTab,
                                            onDuplicateTab = onDuplicateTab,
                                            onMoveTabToWorkspace = onMoveTabToWorkspace,
                                            onSaveTabOffline = onSaveTabOffline
                                        )
                                    }
                                }
                            }

                            SidebarSectionType.PINNED -> {
                                if (sidebarCfg.showPinnedTabs && uiState.currentWorkspacePinnedTabs.isNotEmpty()) {
                                    item(key = "section_pinned_header") {
                                        SidebarSectionDivider(label = "Pinned")
                                    }
                                    itemsIndexed(
                                        items = uiState.currentWorkspacePinnedTabs,
                                        key = { _, it -> "pin_${it.id}" }
                                    ) { index, tab ->
                                        val isBeingDragged = draggingTabId == tab.id && draggingIsPinnedSection
                                        val isHoverMergeTarget = hoveredMergeTargetTabId == tab.id
                                        val splitSelectionOrder = uiState.selectedSplitTabIds.indexOf(tab.id)

                                        SwipeableVerticalTabRow(
                                            tab = tab,
                                            isSelected = tab.id == uiState.activeTabId || tab.id == uiState.secondarySplitTabId,
                                            isSplitSecondary = tab.id == uiState.secondarySplitTabId && uiState.isSplitViewEnabled,
                                            splitSelectionBadge = if (uiState.isSelectingSplitTabs && splitSelectionOrder >= 0) splitSelectionOrder + 1 else null,
                                            isBeingDragged = isBeingDragged,
                                            dragOffsetY = if (isBeingDragged) dragAccumulatedY else 0f,
                                            isHoverMergeTarget = isHoverMergeTarget,
                                            isLiquidGlass = isLiquidGlass,
                                            showFaviconBadge = sidebarCfg.showTabFaviconBadges,
                                            swipeRightAction = sidebarCfg.tabSwipeRightAction,
                                            swipeLeftAction = sidebarCfg.tabSwipeLeftAction,
                                            workspaces = uiState.workspaces,
                                            onSelect = { onSelectTab(tab.id) },
                                            onSwipeRight = { onTabSwipeGesture(tab, true) },
                                            onSwipeLeft = { onTabSwipeGesture(tab, false) },
                                            onUnload = { onUnloadTab(tab) },
                                            onClose = { onCloseTab(tab.id) },
                                            onDuplicate = { onDuplicateTab(tab) },
                                            onTogglePin = { onTogglePinTab(tab) },
                                            onToggleBookmark = { onToggleBookmarkTab(tab) },
                                            onToggleEssential = { onToggleEssentialTab(tab) },
                                            onMoveToWorkspace = { targetWsId -> onMoveTabToWorkspace(tab, targetWsId) },
                                            onSaveOffline = { onSaveTabOffline(tab) },
                                            onStartDrag = {
                                                draggingTabId = tab.id
                                                draggingTabStartIndex = index
                                                draggingIsPinnedSection = true
                                                dragAccumulatedY = 0f
                                                hoveredMergeTargetTabId = null
                                                hoveredReorderTargetIndex = null
                                            },
                                            onDragDeltaY = { dy ->
                                                dragAccumulatedY += dy
                                                val list = uiState.currentWorkspacePinnedTabs
                                                val rawOffsetRows = dragAccumulatedY / rowHeightPx
                                                val targetIdx = (draggingTabStartIndex + rawOffsetRows.roundToInt())
                                                    .coerceIn(0, list.lastIndex)
                                                val fractionalPart = abs(rawOffsetRows - rawOffsetRows.roundToInt())
                                                if (targetIdx != draggingTabStartIndex) {
                                                    if (fractionalPart < 0.34f) {
                                                        hoveredMergeTargetTabId = list[targetIdx].id
                                                        hoveredReorderTargetIndex = null
                                                    } else {
                                                        hoveredMergeTargetTabId = null
                                                        hoveredReorderTargetIndex = targetIdx
                                                    }
                                                } else {
                                                    hoveredMergeTargetTabId = null
                                                    hoveredReorderTargetIndex = null
                                                }
                                            },
                                            onEndDrag = {
                                                val draggedId = draggingTabId
                                                val mergeTarget = hoveredMergeTargetTabId
                                                val reorderIdx = hoveredReorderTargetIndex
                                                val fallbackIdx = (draggingTabStartIndex + (dragAccumulatedY / rowHeightPx).roundToInt())
                                                    .coerceIn(0, uiState.currentWorkspacePinnedTabs.lastIndex)
                                                draggingTabId = null
                                                dragAccumulatedY = 0f
                                                hoveredMergeTargetTabId = null
                                                hoveredReorderTargetIndex = null

                                                if (draggedId != null) {
                                                    if (mergeTarget != null && mergeTarget != draggedId) {
                                                        onMergeTabsIntoSplitView(draggedId, mergeTarget)
                                                    } else if (reorderIdx != null && reorderIdx != draggingTabStartIndex) {
                                                        onReorderTabs(draggedId, reorderIdx, true)
                                                    } else if (fallbackIdx != draggingTabStartIndex) {
                                                        onReorderTabs(draggedId, fallbackIdx, true)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            SidebarSectionType.OPEN_TABS -> {
                                if (enabledTools.contains(SidebarToolItem.NEW_TAB_BUTTON)) {
                                    item(key = "section_open_header") {
                                        NewTabSwipeableRow(
                                            openTabsCount = uiState.currentWorkspaceOpenTabs.size,
                                            showClearButton = enabledTools.contains(SidebarToolItem.CLEAR_TABS),
                                            isLiquidGlass = isLiquidGlass,
                                            onCreateNewTab = onCreateNewTab,
                                            onSwipeLeft = { onNewTabSwipeGesture(true) },
                                            onSwipeRight = { onNewTabSwipeGesture(false) },
                                            onClearUnpinnedTabs = onClearUnpinnedTabs
                                        )
                                    }
                                }

                                itemsIndexed(
                                    items = uiState.currentWorkspaceOpenTabs,
                                    key = { _, it -> "open_${it.id}" }
                                ) { index, tab ->
                                    val isBeingDragged = draggingTabId == tab.id && !draggingIsPinnedSection
                                    val isHoverMergeTarget = hoveredMergeTargetTabId == tab.id
                                    val splitSelectionOrder = uiState.selectedSplitTabIds.indexOf(tab.id)

                                    SwipeableVerticalTabRow(
                                        tab = tab,
                                        isSelected = tab.id == uiState.activeTabId || tab.id == uiState.secondarySplitTabId,
                                        isSplitSecondary = tab.id == uiState.secondarySplitTabId && uiState.isSplitViewEnabled,
                                        splitSelectionBadge = if (uiState.isSelectingSplitTabs && splitSelectionOrder >= 0) splitSelectionOrder + 1 else null,
                                        isBeingDragged = isBeingDragged,
                                        dragOffsetY = if (isBeingDragged) dragAccumulatedY else 0f,
                                        isHoverMergeTarget = isHoverMergeTarget,
                                        isLiquidGlass = isLiquidGlass,
                                        showFaviconBadge = sidebarCfg.showTabFaviconBadges,
                                        swipeRightAction = sidebarCfg.tabSwipeRightAction,
                                        swipeLeftAction = sidebarCfg.tabSwipeLeftAction,
                                        workspaces = uiState.workspaces,
                                        onSelect = { onSelectTab(tab.id) },
                                        onSwipeRight = { onTabSwipeGesture(tab, true) },
                                        onSwipeLeft = { onTabSwipeGesture(tab, false) },
                                        onUnload = { onUnloadTab(tab) },
                                        onClose = { onCloseTab(tab.id) },
                                        onDuplicate = { onDuplicateTab(tab) },
                                        onTogglePin = { onTogglePinTab(tab) },
                                        onToggleBookmark = { onToggleBookmarkTab(tab) },
                                        onToggleEssential = { onToggleEssentialTab(tab) },
                                        onMoveToWorkspace = { targetWsId -> onMoveTabToWorkspace(tab, targetWsId) },
                                        onSaveOffline = { onSaveTabOffline(tab) },
                                        onStartDrag = {
                                            draggingTabId = tab.id
                                            draggingTabStartIndex = index
                                            draggingIsPinnedSection = false
                                            dragAccumulatedY = 0f
                                            hoveredMergeTargetTabId = null
                                            hoveredReorderTargetIndex = null
                                        },
                                        onDragDeltaY = { dy ->
                                            dragAccumulatedY += dy
                                            val list = uiState.currentWorkspaceOpenTabs
                                            val rawOffsetRows = dragAccumulatedY / rowHeightPx
                                            val targetIdx = (draggingTabStartIndex + rawOffsetRows.roundToInt())
                                                .coerceIn(0, list.lastIndex)
                                            val fractionalPart = abs(rawOffsetRows - rawOffsetRows.roundToInt())
                                            if (targetIdx != draggingTabStartIndex) {
                                                if (fractionalPart < 0.34f) {
                                                    hoveredMergeTargetTabId = list[targetIdx].id
                                                    hoveredReorderTargetIndex = null
                                                } else {
                                                    hoveredMergeTargetTabId = null
                                                    hoveredReorderTargetIndex = targetIdx
                                                }
                                            } else {
                                                hoveredMergeTargetTabId = null
                                                hoveredReorderTargetIndex = null
                                            }
                                        },
                                        onEndDrag = {
                                            val draggedId = draggingTabId
                                            val mergeTarget = hoveredMergeTargetTabId
                                            val reorderIdx = hoveredReorderTargetIndex
                                            val fallbackIdx = (draggingTabStartIndex + (dragAccumulatedY / rowHeightPx).roundToInt())
                                                .coerceIn(0, uiState.currentWorkspaceOpenTabs.lastIndex)
                                            draggingTabId = null
                                            dragAccumulatedY = 0f
                                            hoveredMergeTargetTabId = null
                                            hoveredReorderTargetIndex = null

                                            if (draggedId != null) {
                                                if (mergeTarget != null && mergeTarget != draggedId) {
                                                    onMergeTabsIntoSplitView(draggedId, mergeTarget)
                                                } else if (reorderIdx != null && reorderIdx != draggingTabStartIndex) {
                                                    onReorderTabs(draggedId, reorderIdx, false)
                                                } else if (fallbackIdx != draggingTabStartIndex) {
                                                    onReorderTabs(draggedId, fallbackIdx, false)
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            SidebarSectionType.QUICK_TOOLS,
                            SidebarSectionType.WORKSPACES -> {
                                // Rendered in the bottom footer dock
                            }
                        }
                    }

                    // Dedicated empty-space target inside the scrollable list so long-pressing below tabs always opens the Edit Side Panel menu
                    item(key = "empty_space_long_press_target") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {},
                                    onLongClick = { showEmptyAreaEditMenu = true }
                                )
                                .testTag("sidebar_empty_space_area")
                        )
                    }
                }

                // Bottom Area:
                // - Media Bar (when media is playing, displayed right above the Split/Shield/Compact/Extensions bottom toolbar)
                // - Bottom Toolbar (customizable tools)
                // - Workspaces Dock
                SidebarBottomFooterBar(
                    workspaces = uiState.workspaces,
                    activeWorkspaceId = uiState.activeWorkspaceId,
                    mediaPlaybackState = uiState.mediaPlaybackState,
                    enabledTools = enabledTools,
                    showQuickToolsSection = sidebarCfg.sectionOrder.contains(SidebarSectionType.QUICK_TOOLS),
                    showWorkspacesSection = sidebarCfg.sectionOrder.contains(SidebarSectionType.WORKSPACES),
                    isSplitView = uiState.isSplitViewEnabled || uiState.isSelectingSplitTabs,
                    isReaderMode = uiState.isReaderModeEnabled,
                    isCompactMode = uiState.isCompactFullscreen,
                    isLiquidGlass = isLiquidGlass,
                    enabledExtensionsCount = uiState.firefoxExtensions.count { it.isInstalled && it.isEnabled },
                    onSelectWorkspace = onSelectWorkspace,
                    onManageWorkspaces = { onOpenSheet(ActiveModalSheet.WORKSPACE_EDITOR) },
                    onToggleSplit = onToggleSplitView,
                    onOpenPrivacy = { onOpenSheet(ActiveModalSheet.PRIVACY_SHIELD) },
                    onToggleReader = onToggleReaderMode,
                    onToggleCompact = onToggleCompactMode,
                    isDesktopMode = uiState.isFocusedTabDesktopMode,
                    onToggleDesktopMode = onToggleDesktopMode,
                    onOpenExtensions = onOpenExtensions,
                    onOpenLibrary = { onOpenSheet(ActiveModalSheet.LIBRARY) },
                    onOpenStudio = { onOpenSheet(ActiveModalSheet.SIDEBAR_STUDIO) },
                    onToggleMediaPlayPause = onToggleMediaPlayPause,
                    onSelectMediaTab = {
                        if (uiState.mediaPlaybackState.tabId.isNotBlank()) {
                            onSelectTab(uiState.mediaPlaybackState.tabId)
                        }
                    },
                    onDismissMediaBar = onDismissMediaBar,
                    onEnterPip = onEnterPip
                )
            }
            }
        }
    }
}

@Composable
private fun SidebarSectionDivider(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFA1A1AA),
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun EssentialTabsGridSection(
    essentialTabs: List<TabEntity>,
    activeTabId: String?,
    isLiquidGlass: Boolean,
    workspaces: List<WorkspaceEntity>,
    onSelectTab: (String) -> Unit,
    onOpenEssentialsManager: () -> Unit,
    onUnloadTab: (TabEntity) -> Unit,
    onCloseTab: (String) -> Unit,
    onTogglePinTab: (TabEntity) -> Unit,
    onToggleBookmarkTab: (TabEntity) -> Unit,
    onToggleEssentialTab: (TabEntity) -> Unit,
    onDuplicateTab: (TabEntity) -> Unit = {},
    onMoveTabToWorkspace: (TabEntity, String) -> Unit,
    onSaveTabOffline: (TabEntity) -> Unit = {}
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Essentials",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFA1A1AA)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onOpenEssentialsManager)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .testTag("manage_essentials_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Customize Essentials",
                    tint = Color(0xFFD4D4D8),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFD4D4D8)
                )
            }
        }

        if (essentialTabs.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxItemsInEachRow = 4
            ) {
                essentialTabs.forEach { tab ->
                    var showContextMenu by remember { mutableStateOf(false) }
                    val isSelected = tab.id == activeTabId
                    val monogram = tab.title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "Z"

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected && isLiquidGlass -> Color.White.copy(alpha = 0.22f)
                                isSelected -> Color(0xFF27272B)
                                isLiquidGlass -> Color.White.copy(alpha = 0.08f)
                                else -> Color(0xFF141417)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = when {
                                    isSelected && isLiquidGlass -> Color.White.copy(alpha = 0.55f)
                                    isSelected -> Color(0xFF52525B)
                                    isLiquidGlass -> Color.White.copy(alpha = 0.22f)
                                    else -> Color(0xFF222226)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .combinedClickable(
                                    onClick = { onSelectTab(tab.id) },
                                    onLongClick = { showContextMenu = true }
                                )
                                .alpha(if (tab.isUnloaded) 0.5f else 1f)
                                .testTag("essential_tab_${tab.id}")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (tab.isPrivate) {
                                    ShadeGlassIcon(tint = Color(0xFFF4F4F6), size = 15.dp)
                                } else if (tab.url == "zen://home") {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = tab.title,
                                        tint = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFD4D4D8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = monogram,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFD4D4D8),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        TabContextMenu(
                            expanded = showContextMenu,
                            tab = tab,
                            workspaces = workspaces,
                            onDismiss = { showContextMenu = false },
                            onUnload = { onUnloadTab(tab) },
                            onClose = { onCloseTab(tab.id) },
                            onDuplicate = { onDuplicateTab(tab) },
                            onToggleBookmark = { onToggleBookmarkTab(tab) },
                            onToggleEssential = { onToggleEssentialTab(tab) },
                            onTogglePin = { onTogglePinTab(tab) },
                            onMoveToWorkspace = { wsId -> onMoveTabToWorkspace(tab, wsId) },
                            onSaveOffline = { onSaveTabOffline(tab) }
                        )
                    }
                }

                Surface(
                    onClick = onOpenEssentialsManager,
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLiquidGlass) Color.White.copy(alpha = 0.06f) else Color(0xFF101013),
                    border = BorderStroke(
                        1.dp,
                        if (isLiquidGlass) Color.White.copy(alpha = 0.20f) else Color(0xFF222226)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("add_essential_tile_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Essential Site",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewTabSwipeableRow(
    openTabsCount: Int,
    showClearButton: Boolean,
    isLiquidGlass: Boolean,
    onCreateNewTab: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    onClearUnpinnedTabs: () -> Unit
) {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = dragOffsetX, label = "new_tab_swipe")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalDivider(
            color = if (isLiquidGlass) Color.White.copy(alpha = 0.16f) else Color(0xFF222226),
            thickness = 1.dp,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        Surface(
            onClick = onCreateNewTab,
            shape = RoundedCornerShape(8.dp),
            color = if (isLiquidGlass) Color.White.copy(alpha = 0.10f) else Color(0xFF141417),
            border = BorderStroke(
                1.dp,
                if (isLiquidGlass) Color.White.copy(alpha = 0.25f) else Color(0xFF222226)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetX < -75f) {
                                onSwipeLeft()
                            } else if (dragOffsetX > 75f) {
                                onSwipeRight()
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = { dragOffsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-140f, 140f)
                        }
                    )
                }
                .testTag("sidebar_new_tab_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Tab",
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Tab",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFF4F4F6)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShadeGlassIcon(
                        tint = Color(0xFFA1A1AA),
                        size = 13.dp
                    )
                    if (openTabsCount > 1 && showClearButton) {
                        IconButton(
                            onClick = onClearUnpinnedTabs,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("clear_unpinned_tabs_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear tabs",
                                tint = Color(0xFFA1A1AA),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableVerticalTabRow(
    tab: TabEntity,
    isSelected: Boolean,
    isSplitSecondary: Boolean,
    splitSelectionBadge: Int?,
    isBeingDragged: Boolean,
    dragOffsetY: Float,
    isHoverMergeTarget: Boolean,
    isLiquidGlass: Boolean,
    showFaviconBadge: Boolean,
    swipeRightAction: TabSwipeAction,
    swipeLeftAction: TabSwipeAction,
    workspaces: List<WorkspaceEntity>,
    onSelect: () -> Unit,
    onSwipeRight: () -> Unit,
    onSwipeLeft: () -> Unit,
    onUnload: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit = {},
    onTogglePin: () -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleEssential: () -> Unit,
    onMoveToWorkspace: (String) -> Unit,
    onSaveOffline: () -> Unit = {},
    onStartDrag: () -> Unit,
    onDragDeltaY: (Float) -> Unit,
    onEndDrag: () -> Unit
) {
    val host = ZenPrivacyEngine.extractHostDisplay(tab.url)
    val letter = tab.title.firstOrNull()?.uppercaseChar()?.toString() ?: "Z"
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(targetValue = dragOffsetX, label = "tab_swipe")
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isBeingDragged) 10f else 0f)
    ) {
        if (abs(animatedOffsetX) > 12f) {
            val isRight = animatedOffsetX > 0f
            val actionLabel = if (isRight) swipeRightAction.label else swipeLeftAction.label
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C1C20))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRight) Arrangement.Start else Arrangement.End
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA1A1AA)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                isHoverMergeTarget -> Color(0xFF27272A)
                isBeingDragged -> Color(0xFF2E2E34)
                splitSelectionBadge != null -> Color(0xFF27272B)
                isSelected && isLiquidGlass -> Color.White.copy(alpha = 0.18f)
                isSelected -> Color(0xFF222226)
                else -> Color.Transparent
            },
            border = when {
                isHoverMergeTarget -> BorderStroke(1.5.dp, Color(0xFFF4F4F6))
                isBeingDragged -> BorderStroke(1.dp, Color(0xFFA1A1AA))
                splitSelectionBadge != null -> BorderStroke(1.dp, Color(0xFFF4F4F6))
                isSplitSecondary -> BorderStroke(1.dp, Color(0xFF71717A))
                isSelected && isLiquidGlass -> BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                else -> null
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .offset {
                    IntOffset(
                        x = animatedOffsetX.roundToInt(),
                        y = dragOffsetY.roundToInt()
                    )
                }
                .clip(RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onSelect,
                    onLongClick = { showContextMenu = true }
                )
                .pointerInput(tab.id, swipeRightAction, swipeLeftAction) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (dragOffsetX > 80f) {
                                onSwipeRight()
                            } else if (dragOffsetX < -80f) {
                                onSwipeLeft()
                            }
                            dragOffsetX = 0f
                        },
                        onDragCancel = { dragOffsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX = (dragOffsetX + dragAmount).coerceIn(-160f, 160f)
                        }
                    )
                }
                .alpha(if (tab.isUnloaded) 0.48f else 1f)
                .testTag("vertical_tab_${tab.id}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showFaviconBadge) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isSelected && isLiquidGlass -> Color.White.copy(alpha = 0.25f)
                                    isSelected -> Color(0xFF3F3F46)
                                    isLiquidGlass -> Color.White.copy(alpha = 0.12f)
                                    else -> Color(0xFF18181C)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (splitSelectionBadge != null) {
                            Text(
                                text = splitSelectionBadge.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.Bold
                            )
                        } else if (tab.isPrivate) {
                            ShadeGlassIcon(
                                tint = Color(0xFFF4F4F6),
                                size = 13.dp
                            )
                        } else if (tab.url == "zen://home") {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = Color(0xFFD4D4D8),
                                modifier = Modifier.size(12.dp)
                            )
                        } else {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else if (tab.isPrivate) {
                    ShadeGlassIcon(tint = Color(0xFFF4F4F6), size = 14.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = if (isHoverMergeTarget) "Drop to Split with ${tab.title.ifBlank { host }}" else tab.title.ifBlank { host },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected || isHoverMergeTarget) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected || isHoverMergeTarget) Color(0xFFF4F4F6) else Color(0xFFD4D4D8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (tab.isUnloaded && !isHoverMergeTarget) {
                    Text(
                        text = "zzz",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                // Hold-and-drag grip handle: hold and drag vertically to reorder tabs or drop directly over another tab to open Split View
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .pointerInput(tab.id) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { onStartDrag() },
                                onDragEnd = { onEndDrag() },
                                onDragCancel = { onEndDrag() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDeltaY(dragAmount.y)
                                }
                            )
                        }
                        .testTag("tab_drag_handle_${tab.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isHoverMergeTarget) Icons.Default.Splitscreen else Icons.Default.DragIndicator,
                        contentDescription = "Hold and drag to move tab or drop over another tab to Split View",
                        tint = if (isBeingDragged || isHoverMergeTarget) Color(0xFFF4F4F6) else Color(0xFF71717A),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        TabContextMenu(
            expanded = showContextMenu,
            tab = tab,
            workspaces = workspaces,
            onDismiss = { showContextMenu = false },
            onUnload = onUnload,
            onClose = onClose,
            onDuplicate = onDuplicate,
            onToggleBookmark = onToggleBookmark,
            onToggleEssential = onToggleEssential,
            onTogglePin = onTogglePin,
            onMoveToWorkspace = onMoveToWorkspace,
            onSaveOffline = onSaveOffline
        )
    }
}

@Composable
private fun TabContextMenu(
    expanded: Boolean,
    tab: TabEntity,
    workspaces: List<WorkspaceEntity>,
    onDismiss: () -> Unit,
    onUnload: () -> Unit,
    onClose: () -> Unit,
    onDuplicate: () -> Unit = {},
    onToggleBookmark: () -> Unit,
    onToggleEssential: () -> Unit,
    onTogglePin: () -> Unit,
    onMoveToWorkspace: (String) -> Unit,
    onSaveOffline: () -> Unit = {}
) {
    var showWorkspaceSubMenu by remember { mutableStateOf(false) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            showWorkspaceSubMenu = false
            onDismiss()
        },
        containerColor = Color(0xFF141417)
    ) {
        if (!showWorkspaceSubMenu) {
            DropdownMenuItem(
                text = { Text("Duplicate Tab") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    onDuplicate()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text("Save Page for Offline") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CloudOff,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    onSaveOffline()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text("Unload Tab") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    onUnload()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text(if (tab.isPinned) "Unpin Tab" else "Pin Tab") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    onTogglePin()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text(if (tab.isEssential) "Remove from Essentials" else "Add to Essentials") },
                leadingIcon = {
                    Icon(
                        imageVector = if (tab.isEssential) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    onToggleEssential()
                    onDismiss()
                }
            )
            DropdownMenuItem(
                text = { Text(if (tab.isBookmarked) "Remove Bookmark" else "Bookmark Tab") },
                leadingIcon = {
                    Icon(
                        imageVector = if (tab.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    onToggleBookmark()
                    onDismiss()
                }
            )
            if (workspaces.size > 1) {
                DropdownMenuItem(
                    text = { Text("Move to Workspace...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DriveFileMove,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = { showWorkspaceSubMenu = true }
                )
            }
            HorizontalDivider(color = Color(0xFF222226))
            DropdownMenuItem(
                text = { Text("Close Tab") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                },
                onClick = {
                    onClose()
                    onDismiss()
                }
            )
        } else {
            workspaces.filter { it.id != tab.workspaceId }.forEach { ws ->
                DropdownMenuItem(
                    text = { Text("Move to ${ws.name}") },
                    leadingIcon = {
                        if (ws.isPrivate) {
                            ShadeGlassIcon(tint = Color(0xFFF4F4F6), size = 16.dp)
                        } else {
                            Icon(
                                imageVector = workspaceIconForKey(ws.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = {
                        onMoveToWorkspace(ws.id)
                        showWorkspaceSubMenu = false
                        onDismiss()
                    }
                )
            }
        }
    }
}

@Composable
private fun SidebarBottomFooterBar(
    workspaces: List<WorkspaceEntity>,
    activeWorkspaceId: String,
    mediaPlaybackState: MediaPlaybackState,
    enabledTools: List<SidebarToolItem>,
    showQuickToolsSection: Boolean,
    showWorkspacesSection: Boolean,
    isSplitView: Boolean,
    isReaderMode: Boolean,
    isCompactMode: Boolean,
    isLiquidGlass: Boolean,
    enabledExtensionsCount: Int,
    onSelectWorkspace: (String) -> Unit,
    onManageWorkspaces: () -> Unit,
    onToggleSplit: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onToggleReader: () -> Unit,
    onToggleCompact: () -> Unit,
    isDesktopMode: Boolean = false,
    onToggleDesktopMode: () -> Unit = {},
    onOpenExtensions: () -> Unit,
    onOpenLibrary: () -> Unit = {},
    onOpenStudio: () -> Unit,
    onToggleMediaPlayPause: () -> Unit,
    onToggleMediaMute: () -> Unit = {},
    onSelectMediaTab: () -> Unit,
    onDismissMediaBar: () -> Unit,
    onEnterPip: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Media Bar shown right above the Split/Shield/Compact bottom toolbar whenever media is active
        AnimatedVisibility(
            visible = mediaPlaybackState.isActive && enabledTools.contains(SidebarToolItem.MEDIA_BAR)
        ) {
            Surface(
                onClick = onSelectMediaTab,
                shape = RoundedCornerShape(50),
                color = if (isLiquidGlass) Color.White.copy(alpha = 0.16f) else Color(0xFF18181C),
                border = BorderStroke(
                    1.dp,
                    if (isLiquidGlass) Color.White.copy(alpha = 0.32f) else Color(0xFF2E2E36)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("sidebar_media_playback_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF27272A),
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { onToggleMediaMute() }
                                .testTag("sidebar_media_mute_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Mute or Unmute Media",
                                    tint = Color(0xFFF4F4F6),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = mediaPlaybackState.title.ifBlank { "Playing Media" },
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFF4F4F6),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (mediaPlaybackState.domain.isNotBlank()) {
                                Text(
                                    text = mediaPlaybackState.domain,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA1A1AA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            IconButton(
                                onClick = onEnterPip,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("sidebar_media_pip_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureInPicture,
                                    contentDescription = "Play in window",
                                    tint = Color(0xFFF4F4F6),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f)
                        ) {
                            IconButton(
                                onClick = onToggleMediaPlayPause,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("sidebar_media_play_pause_button")
                            ) {
                                Icon(
                                    imageVector = if (mediaPlaybackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (mediaPlaybackState.isPlaying) "Pause" else "Play",
                                    tint = Color(0xFFF4F4F6),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.06f)
                        ) {
                            IconButton(
                                onClick = onDismissMediaBar,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("sidebar_media_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop Media Bar",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            color = if (isLiquidGlass) Color.White.copy(alpha = 0.16f) else Color(0xFF222226),
            thickness = 1.dp
        )

        // Bottom Toolbar Row: Dynamically rendered in user's enabledSidebarTools order
        if (showQuickToolsSection) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                enabledTools.forEach { tool ->
                    when (tool) {
                        SidebarToolItem.SPLIT_VIEW -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.Splitscreen,
                                label = "Split View",
                                active = isSplitView,
                                onClick = onToggleSplit,
                                testTag = "quick_tool_split_view"
                            )
                        }
                        SidebarToolItem.PRIVACY_SHIELD -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.Shield,
                                label = "Shield",
                                active = false,
                                onClick = onOpenPrivacy,
                                testTag = "sidebar_shield_button"
                            )
                        }
                        SidebarToolItem.COMPACT_MODE -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.Fullscreen,
                                label = "Compact",
                                active = isCompactMode,
                                onClick = onToggleCompact,
                                testTag = "quick_tool_compact_mode"
                            )
                        }
                        SidebarToolItem.READER_MODE -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.MenuBook,
                                label = "Reader",
                                active = isReaderMode,
                                onClick = onToggleReader,
                                testTag = "quick_tool_reader_mode"
                            )
                        }
                        SidebarToolItem.FIREFOX_EXTENSIONS -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.Extension,
                                label = "Firefox Extensions",
                                active = enabledExtensionsCount > 0,
                                onClick = onOpenExtensions,
                                testTag = "quick_tool_firefox_extensions"
                            )
                        }
                        SidebarToolItem.LIBRARY -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.FolderSpecial,
                                label = "Library",
                                active = false,
                                onClick = onOpenLibrary,
                                testTag = "quick_tool_library"
                            )
                        }
                        SidebarToolItem.DESKTOP_MODE -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.DesktopWindows,
                                label = "Desktop",
                                active = isDesktopMode,
                                onClick = onToggleDesktopMode,
                                testTag = "quick_tool_desktop_mode"
                            )
                        }
                        SidebarToolItem.SETTINGS_BUTTON -> {
                            ToolbarIconToggle(
                                icon = Icons.Default.Tune,
                                label = "Settings",
                                active = false,
                                onClick = onOpenStudio,
                                testTag = "quick_tool_sidebar_studio"
                            )
                        }
                        else -> {}
                    }
                }
            }
        }

        // Bottom Workspaces Dock
        if (showWorkspacesSection) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLiquidGlass) Color.White.copy(alpha = 0.08f) else Color(0xFF101013))
                    .border(
                        1.dp,
                        if (isLiquidGlass) Color.White.copy(alpha = 0.22f) else Color(0xFF222226),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                workspaces.forEach { ws ->
                    val isSelected = ws.id == activeWorkspaceId
                    Surface(
                        onClick = { onSelectWorkspace(ws.id) },
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            isSelected && isLiquidGlass -> Color.White.copy(alpha = 0.22f)
                            isSelected -> Color(0xFF27272B)
                            else -> Color.Transparent
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                            .testTag("sidebar_ws_chip_${ws.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (ws.isPrivate) {
                                ShadeGlassIcon(
                                    tint = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFA1A1AA),
                                    size = 14.dp
                                )
                            } else {
                                Icon(
                                    imageVector = workspaceIconForKey(ws.iconKey),
                                    contentDescription = ws.name,
                                    tint = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFA1A1AA),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            if (workspaces.size <= 2) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ws.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFA1A1AA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                Surface(
                    onClick = onManageWorkspaces,
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("manage_workspaces_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Manage Workspaces",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolbarIconToggle(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (active) Color(0xFF27272B) else Color.Transparent,
        modifier = Modifier
            .size(32.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFFF4F4F6) else Color(0xFFA1A1AA),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
