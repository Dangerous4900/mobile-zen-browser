package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Language
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInBrowser
import com.example.data.PasswordEntity
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.BackSwipeSidebarMode
import com.example.data.DownloadItemEntity
import com.example.data.FirefoxExtensionEntity
import com.example.data.HistoryDeletionInterval
import com.example.data.HistoryEntryEntity
import com.example.data.OfflinePageEntity
import com.example.data.HomeScreenStyle
import com.example.data.HorizontalSwipeNavigationMode
import com.example.data.NewTabPosition
import com.example.data.NewTabSwipeGestureAction
import com.example.data.SearchEngine
import com.example.data.SidebarColorMode
import com.example.data.SidebarPosition
import com.example.data.SidebarSectionType
import com.example.data.SidebarToolItem
import com.example.data.SidebarWidthMode
import com.example.data.SingleToolbarNewTabBehavior
import com.example.data.StartPageGradientPreset
import com.example.data.StartupBehavior
import com.example.data.TabEntity
import com.example.data.TabSwipeAction
import com.example.data.TopToolbarToolItem
import com.example.data.UrlBarPosition
import com.example.data.WorkspaceEntity
import com.example.data.ZenModEntity
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.ActiveModalSheet
import com.example.ui.ZenBrowserUiState
import com.example.ui.theme.ZenAccentTheme
import kotlin.math.roundToInt

/**
 * Interactive Tutorial Dialog shown on first launch and triggerable anytime from Settings.
 */
@Composable
fun ZenTutorialDialog(
    onDismiss: () -> Unit
) {
    val steps = listOf(
        Triple(
            "Welcome to Zen Browser",
            "Edge-Aware Gestures & Navigation Controls",
            "• Swipe back once from the LEFT edge (or tap the edge handle) to open your frosted / Liquid Glass Side Panel.\n• Swipe back from the RIGHT edge to navigate back in page history.\n• Use Back, Forward, Reload, and Home controls at the top of the Side Panel, plus the sleek slim search bar."
        ),
        Triple(
            "Smart Tab Unloading & Reordering",
            "Slide, Hold-to-Drag & Auto-Switch",
            "• Slide a tab RIGHT to unload it and save memory — Zen automatically switches to your next loaded tab, or returns to the Home Screen if all tabs are unloaded.\n• Hold and drag any tab to reorder it, or drop one tab onto another to open them together in Split View.\n• Swipe LEFT or RIGHT on '+ New Tab' for Private Tabs or Private Workspaces."
        ),
        Triple(
            "Split Screen & Customizable Side Panel",
            "Dual-Pane Browsing & Layout Editor",
            "• Tap Split View to pick 2 tabs in the Side Panel (or 1 tab + Homepage), drag the center divider to resize, and double-tap either pane to focus it.\n• Long-press any empty area in the Side Panel (or tap Edit) to add, remove, or reorder tools and sections (Essentials starts clean and empty by default)."
        ),
        Triple(
            "Inbuilt Translator, Add-ons & Home Screen",
            "Extensions, Blur, Opacity & Brightness",
            "• Open Extensions and tap 'Zen Inbuilt Page Translator' ('Open Translator') anytime you want to translate a page or highlighted text into 13+ languages.\n• Adjust Side Panel Blur & Opacity independently in Settings without dimming the rest of the screen.\n• Long-press the Home Screen to customize Normal/Zen styles, wallpaper blur, and brightness."
        )
    )
    var currentStep by remember { mutableIntStateOf(0) }
    val (title, subtitle, body) = steps[currentStep]

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF101014),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewSidebar,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF4F4F6),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA1A1AA)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD4D4D8)
                )
                Text(
                    text = "Step ${currentStep + 1} of ${steps.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (currentStep < steps.lastIndex) {
                        currentStep++
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF4F4F6),
                    contentColor = Color(0xFF000000)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("tutorial_next_or_done_button")
            ) {
                Text(if (currentStep < steps.lastIndex) "Next" else "Get Started")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("tutorial_skip_button")
            ) {
                Text("Skip", color = Color(0xFFA1A1AA))
            }
        },
        modifier = Modifier.testTag("zen_tutorial_dialog")
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SidebarCustomizationStudioSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onOpenSheet: (ActiveModalSheet) -> Unit,
    onTriggerTutorial: () -> Unit,
    onUpdatePosition: (SidebarPosition) -> Unit,
    onUpdateWidthMode: (SidebarWidthMode) -> Unit,
    onUpdateUiScale: (Float) -> Unit = {},
    onUpdateUrlBarPosition: (UrlBarPosition) -> Unit,
    onUpdateSingleToolbarNewTabBehavior: (SingleToolbarNewTabBehavior) -> Unit,
    onUpdateHideStatusBarFullscreen: (Boolean) -> Unit,
    onUpdateSidebarGlassAndColor: (
        blurIntensity: Float?,
        sidebarOpacity: Float?,
        liquidGlassEnabled: Boolean?,
        colorMode: SidebarColorMode?,
        customColorHex: Long?
    ) -> Unit,
    onUpdateGesturesAndSession: (
        tabSwipeRight: TabSwipeAction?,
        tabSwipeLeft: TabSwipeAction?,
        newTabSwipeLeft: NewTabSwipeGestureAction?,
        newTabSwipeRight: NewTabSwipeGestureAction?,
        backSwipeMode: BackSwipeSidebarMode?,
        closePrivateOnClose: Boolean?,
        startupBehavior: StartupBehavior?,
        homepageHoursThreshold: Int?
    ) -> Unit,
    onUpdateEdgeBar: (
        widthDp: Float?,
        lengthDp: Float?,
        verticalOffsetFraction: Float?,
        hideInCompactMode: Boolean?,
        hideAlways: Boolean?
    ) -> Unit,
    onToggleOption: (
        compactAutoHide: Boolean?,
        showEssentials: Boolean?,
        showPinned: Boolean?,
        showWorkspaceStrip: Boolean?,
        showFavicons: Boolean?,
        borderGlow: Boolean?,
        glassIntensity: Float?
    ) -> Unit,
    onMoveSection: (SidebarSectionType, Int) -> Unit,
    onUpdateWorkspaceTheme: (WorkspaceEntity, String) -> Unit = { _, _ -> },
    onUpdateOpenLinksInGlanceAutomatically: (Boolean) -> Unit = {},
    onUpdateOpenInNewTab: (Boolean) -> Unit = {},
    onUpdateOpenHistoryInNewTab: (Boolean) -> Unit = {},
    onUpdateSwitchToNewTabImmediately: (Boolean) -> Unit = {},
    onUpdateNewTabPosition: (NewTabPosition) -> Unit = {},
    onUpdateNotificationSettings: (showTabCloseNotification: Boolean?, showActionNotifications: Boolean?) -> Unit = { _, _ -> },
    onUpdateFakeSearchConfig: (overviewText: String?, trigger: String?, headline: String?, snippet: String?) -> Unit = { _, _, _, _ -> },
    onOpenFakeSearch: (String) -> Unit = {},
    onUpdateHistoryDeletionInterval: (HistoryDeletionInterval) -> Unit = {},
    onToggleTopToolItem: (TopToolbarToolItem, Boolean) -> Unit = { _, _ -> },
    onMoveTopToolItem: (TopToolbarToolItem, Int) -> Unit = { _, _ -> },
    onUpdateHorizontalSwipeMode: (HorizontalSwipeNavigationMode) -> Unit = {},
    onResetDefaults: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cfg = uiState.sidebarConfig
    var settingsSearchQuery by remember { mutableStateOf("") }

    var localBlur by remember(cfg.glassBlurIntensity) { mutableFloatStateOf(cfg.glassBlurIntensity) }
    var localOpacity by remember(cfg.sidebarOpacity) { mutableFloatStateOf(cfg.sidebarOpacity) }
    var localEdgeWidth by remember(cfg.edgeBarWidthDp) { mutableFloatStateOf(cfg.edgeBarWidthDp) }
    var localEdgeLength by remember(cfg.edgeBarLengthDp) { mutableFloatStateOf(cfg.edgeBarLengthDp) }
    var localEdgeOffset by remember(cfg.edgeBarVerticalOffsetFraction) { mutableFloatStateOf(cfg.edgeBarVerticalOffsetFraction) }

    var localAiOverviewText by remember(cfg.customAiOverviewText) { mutableStateOf(cfg.customAiOverviewText) }
    var localFakeTrigger by remember(cfg.fakeSearchQueryTrigger) { mutableStateOf(cfg.fakeSearchQueryTrigger) }
    var localFakeHeadline by remember(cfg.fakeSearchHeadline) { mutableStateOf(cfg.fakeSearchHeadline) }
    var localFakeSnippet by remember(cfg.fakeSearchSnippet) { mutableStateOf(cfg.fakeSearchSnippet) }

    fun matchesSearch(vararg terms: String): Boolean {
        if (settingsSearchQuery.isBlank()) return true
        val q = settingsSearchQuery.trim().lowercase()
        return terms.any { it.lowercase().contains(q) }
    }

    val sidebarColorPalette = listOf(
        "Midnight Navy" to 0xFF162238L,
        "Deep Forest" to 0xFF142822L,
        "Royal Plum" to 0xFF261834L,
        "Crimson Wine" to 0xFF2E161CL,
        "Warm Espresso" to 0xFF281D18L,
        "Arctic Slate" to 0xFF1E293BL,
        "Cyber Teal" to 0xFF112A30L,
        "Steel Graphite" to 0xFF27272AL
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("sidebar_studio_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = Color(0xFFF4F4F6)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Zen Settings & Customization",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Liquid Glass, side panel color, extensions, gestures & homescreen",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }
                    IconButton(
                        onClick = onResetDefaults,
                        modifier = Modifier.testTag("reset_sidebar_defaults_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset Defaults"
                        )
                    }
                }
            }

            // Search Bar in Settings Page
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF141418),
                    border = BorderStroke(
                        1.dp,
                        if (settingsSearchQuery.isNotEmpty()) Color(0xFF8AB4F8) else Color(0xFF27272A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_search_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Settings",
                            tint = if (settingsSearchQuery.isNotEmpty()) Color(0xFF8AB4F8) else Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        androidx.compose.foundation.text.BasicTextField(
                            value = settingsSearchQuery,
                            onValueChange = { settingsSearchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFF4F4F6)),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF8AB4F8)),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    if (settingsSearchQuery.isEmpty()) {
                                        Text(
                                            text = "Search settings (blur, toolbar, gestures...)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF71717A)
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (settingsSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { settingsSearchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Customization Shortcuts + Firefox Extensions + Tutorial Trigger
            item {
                SectionCard(title = "QUICK CUSTOMIZATION & ADD-ONS") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onOpenSheet(ActiveModalSheet.ESSENTIALS_MANAGER) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_essentials_manager_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Essentials", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { onOpenSheet(ActiveModalSheet.START_PAGE_CUSTOMIZER) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_start_page_customizer_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Homescreen", maxLines = 1)
                        }
                    }
                    OutlinedButton(
                        onClick = { onOpenSheet(ActiveModalSheet.FIREFOX_EXTENSIONS) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_firefox_extensions_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Firefox Extensions & Add-ons (${uiState.firefoxExtensions.count { it.isInstalled && it.isEnabled }} active)")
                    }
                    OutlinedButton(
                        onClick = { onOpenSheet(ActiveModalSheet.SEARCH_ENGINE_SETTINGS) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_search_engine_settings_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Search Engines (Normal & Private)")
                    }
                    OutlinedButton(
                        onClick = { onOpenSheet(ActiveModalSheet.SIDEBAR_LAYOUT_EDITOR) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_sidebar_layout_editor_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ViewSidebar, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Side Panel Items & Sections")
                    }
                    OutlinedButton(
                        onClick = onTriggerTutorial,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("trigger_sidebar_tutorial_button")
                    ) {
                        Icon(imageVector = Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Sidebar & Features Tutorial")
                    }
                }
            }

            // Side Panel Blur (Default 50%), Opacity, Liquid Glass & Side Panel Color Section
            if (matchesSearch("blur", "opacity", "liquid glass", "glass", "color", "tint", "theme")) {
                item {
                    SectionCard(title = "SIDE PANEL BLUR, OPACITY, LIQUID GLASS & COLOR") {
                        Text(
                            text = "Side Panel Blur Intensity: ${(localBlur * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF4F4F6)
                        )
                        Slider(
                            value = localBlur,
                            onValueChange = {
                                localBlur = it
                                onUpdateSidebarGlassAndColor(it, null, null, null, null)
                            },
                            valueRange = 0f..1f,
                            modifier = Modifier.testTag("sidebar_blur_slider")
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Side Panel Opacity: ${(localOpacity * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF4F4F6)
                        )
                        Slider(
                            value = localOpacity,
                            onValueChange = {
                                localOpacity = it
                                onUpdateSidebarGlassAndColor(null, it, null, null, null)
                            },
                            valueRange = 0.05f..1f,
                            modifier = Modifier.testTag("sidebar_opacity_slider")
                        )

                    SettingSwitchRow(
                        title = "Liquid Glass Effect",
                        subtitle = "Refractive translucent glass with specular rim highlights and floating rounded edges",
                        checked = cfg.liquidGlassEnabled,
                        onCheckedChange = {
                            onUpdateSidebarGlassAndColor(null, null, it, null, null)
                        },
                        testTag = "toggle_liquid_glass"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Side Panel Color",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SidebarColorMode.entries.forEach { mode ->
                            val selected = cfg.sidebarColorMode == mode
                            Surface(
                                onClick = {
                                    onUpdateSidebarGlassAndColor(null, null, null, mode, null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF27272A) else Color(0xFF141417),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sidebar_color_mode_${mode.name.lowercase()}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = if (mode == SidebarColorMode.NORMAL) "Normal" else "Custom Color",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF4F4F6)
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA),
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }

                    if (cfg.sidebarColorMode == SidebarColorMode.CUSTOM) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Select Side Panel Color Tint",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            sidebarColorPalette.forEach { (label, hex) ->
                                val isSelected = cfg.sidebarCustomColorHex == hex
                                Surface(
                                    onClick = {
                                        onUpdateSidebarGlassAndColor(null, null, null, SidebarColorMode.CUSTOM, hex)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(hex),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFF3F3F46)
                                    ),
                                    modifier = Modifier.testTag("sidebar_custom_color_$hex")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(hex))
                                                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFF4F4F6)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Fullscreen & Toolbar Behavior (including Open in New Tab Switch behavior)
            if (matchesSearch("toolbar", "fullscreen", "status bar", "switch to new tab", "open in new tab", "new tab", "tab switch", "immediately", "link")) {
                item {
                    SectionCard(title = "TOOLBAR & FULLSCREEN") {
                    SettingSwitchRow(
                        title = "Fullscreen Mode (Hide Status Bar)",
                        subtitle = "Hides the system status bar for full-height AMOLED browsing",
                        checked = cfg.hideStatusBarFullscreen,
                        onCheckedChange = onUpdateHideStatusBarFullscreen,
                        testTag = "toggle_fullscreen_hide_status_bar"
                    )

                    SettingSwitchRow(
                        title = "Open in New Tab",
                        subtitle = "Automatically open clicked links in a new tab directly without needing to open the link options menu",
                        checked = cfg.openInNewTab,
                        onCheckedChange = onUpdateOpenInNewTab,
                        testTag = "toggle_open_in_new_tab"
                    )

                    SettingSwitchRow(
                        title = "Open History in New Tab",
                        subtitle = "When a webpage in browsing history is clicked, open it in a new tab",
                        checked = cfg.openHistoryInNewTab,
                        onCheckedChange = onUpdateOpenHistoryInNewTab,
                        testTag = "toggle_open_history_in_new_tab"
                    )

                    SettingSwitchRow(
                        title = "Switch to New Tab Immediately",
                        subtitle = "Automatically switch to new tabs when opening links instead of opening in background",
                        checked = cfg.switchToNewTabImmediately,
                        onCheckedChange = onUpdateSwitchToNewTabImmediately,
                        testTag = "toggle_switch_to_new_tab_immediately"
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "New Tab Placement",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Text(
                        text = "Choose whether newly opened tabs appear at the bottom or top of your tab list",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF71717A)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NewTabPosition.entries.forEach { pos ->
                            FilterChip(
                                selected = cfg.newTabPosition == pos,
                                onClick = { onUpdateNewTabPosition(pos) },
                                label = { Text(pos.label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("new_tab_pos_${pos.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Toolbar Placement",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        UrlBarPosition.entries.forEach { urlPos ->
                            FilterChip(
                                selected = cfg.urlBarPosition == urlPos,
                                onClick = { onUpdateUrlBarPosition(urlPos) },
                                label = { Text(urlPos.label) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("url_pos_${urlPos.name.lowercase()}")
                            )
                        }
                    }

                    if (cfg.urlBarPosition == UrlBarPosition.SIDEBAR_ONLY) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "When '+ New Tab' is Clicked in Single Toolbar Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFA1A1AA)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SingleToolbarNewTabBehavior.entries.forEach { behavior ->
                                Surface(
                                    onClick = { onUpdateSingleToolbarNewTabBehavior(behavior) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (cfg.singleToolbarNewTabBehavior == behavior) Color(0xFF27272A) else Color(0xFF141417),
                                    border = BorderStroke(
                                        1.dp,
                                        if (cfg.singleToolbarNewTabBehavior == behavior) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("single_toolbar_new_tab_${behavior.name.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = behavior.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF4F4F6)
                                        )
                                        Text(
                                            text = behavior.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA1A1AA)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // At a Glance & History Interval
            item {
                SectionCard(title = "AT A GLANCE PREVIEWS") {
                    SettingSwitchRow(
                        title = "Open Links in At a Glance Automatically",
                        subtitle = "Opens tapped web links in a peek preview card instead of full navigation",
                        checked = cfg.openLinksInGlanceAutomatically,
                        onCheckedChange = onUpdateOpenLinksInGlanceAutomatically,
                        testTag = "toggle_auto_glance_links"
                    )
                }
            }

            // Notifications & Status Banners
            if (matchesSearch("notification", "notifications", "banner", "toast", "close tab", "actions", "pill")) {
                item {
                    SectionCard(title = "NOTIFICATIONS & STATUS BANNERS") {
                        SettingSwitchRow(
                            title = "Tab Close Notifications",
                            subtitle = "Show notification banner pill when closing a tab and switching",
                            checked = cfg.showTabCloseNotification,
                            onCheckedChange = { checked ->
                                onUpdateNotificationSettings(checked, null)
                            },
                            testTag = "toggle_tab_close_notification"
                        )

                        SettingSwitchRow(
                            title = "Action & Tab Status Banners",
                            subtitle = "Show notification banners for actions (tab unloads, background tabs, duplicate, split view, etc.)",
                            checked = cfg.showActionNotifications,
                            onCheckedChange = { checked ->
                                onUpdateNotificationSettings(null, checked)
                            },
                            testTag = "toggle_action_notifications"
                        )
                    }
                }
            }

            // Fake Google Search & AI Overview Customization
            if (matchesSearch("fake", "google", "search", "ai overview", "overview", "sadiq", "aura", "main character", "gemini", "custom text", "edit", "ai", "text", "custom")) {
                item {
                    SectionCard(title = "FAKE GOOGLE SEARCH & AI OVERVIEW") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF4285F4), Color(0xFFEC4899), Color(0xFFF59E0B))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Custom AI Overview Generator",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF4F4F6)
                                    )
                                    Text(
                                        text = "Edit text displayed in the Google Gemini AI Overview card",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "AI Overview Text (Appears in AI Overview)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF8AB4F8),
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = localAiOverviewText,
                            onValueChange = {
                                localAiOverviewText = it
                                onUpdateFakeSearchConfig(it, null, null, null)
                            },
                            placeholder = {
                                Text(
                                    text = "Sadiq is the main character and why ask the obvious when you can see his aura... (Leave blank to use default)",
                                    color = Color(0xFF71717A),
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .testTag("input_custom_ai_overview_text"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF18181E),
                                unfocusedContainerColor = Color(0xFF141417),
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color(0xFF272730),
                                focusedTextColor = Color(0xFFF4F4F6),
                                unfocusedTextColor = Color(0xFFE4E4E7)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (localAiOverviewText.isBlank()) "Default text active" else "${localAiOverviewText.length} characters",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (localAiOverviewText.isBlank()) Color(0xFF8AB4F8) else Color(0xFFA1A1AA)
                            )
                            if (localAiOverviewText.isNotBlank()) {
                                TextButton(
                                    onClick = {
                                        localAiOverviewText = ""
                                        onUpdateFakeSearchConfig("", null, null, null)
                                    },
                                    modifier = Modifier.testTag("reset_ai_overview_text_btn")
                                ) {
                                    Text("Reset to Default", fontSize = 12.sp, color = Color(0xFFF87171))
                                }
                            }
                        }

                        // Quick Presets
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val auraText = "Sadiq is the main character and why ask the obvious when you can see his aura. I mean even I am in awe how his personality, aura and also being so handsome and cute as a cat. also he uses arch btw."
                                    localAiOverviewText = auraText
                                    onUpdateFakeSearchConfig(auraText, null, null, null)
                                },
                                label = { Text("Aura Canon", fontSize = 11.sp) },
                                modifier = Modifier.testTag("preset_aura_canon")
                            )
                            FilterChip(
                                selected = false,
                                onClick = {
                                    val customText = "AI Overview: Verified main character aura index exceeds 9000%. System telemetry confirms Arch Linux mastery, peak charisma, and undeniable protagonist energy."
                                    localAiOverviewText = customText
                                    onUpdateFakeSearchConfig(customText, null, null, null)
                                },
                                label = { Text("Protagonist Telemetry", fontSize = 11.sp) },
                                modifier = Modifier.testTag("preset_protagonist")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Live Preview Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_overview_live_preview_card"),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF1E2024),
                            border = BorderStroke(1.dp, Color(0xFF3C4043))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF8AB4F8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Live AI Overview Preview",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF8AB4F8)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = localAiOverviewText.trim().ifBlank {
                                        "Sadiq is the main character and why ask the obvious when you can see his aura. I mean even I am in awe how his personality, aura and also being so handsome and cute as a cat. also he uses arch btw."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE8EAED),
                                    lineHeight = 18.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Search Query Trigger",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFA1A1AA)
                        )
                        OutlinedTextField(
                            value = localFakeTrigger,
                            onValueChange = {
                                localFakeTrigger = it
                                onUpdateFakeSearchConfig(null, it, null, null)
                            },
                            placeholder = { Text("? : who is the main character", color = Color(0xFF71717A)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fake_search_trigger"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF18181E),
                                unfocusedContainerColor = Color(0xFF141417),
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color(0xFF272730),
                                focusedTextColor = Color(0xFFF4F4F6),
                                unfocusedTextColor = Color(0xFFE4E4E7)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Text(
                            text = "Searching this query in the URL bar launches the fake Google search page.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF71717A)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "First Result Headline (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFA1A1AA)
                        )
                        OutlinedTextField(
                            value = localFakeHeadline,
                            onValueChange = {
                                localFakeHeadline = it
                                onUpdateFakeSearchConfig(null, null, it, null)
                            },
                            placeholder = { Text("How to I be of help to the main character...", color = Color(0xFF71717A)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_fake_search_headline"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF18181E),
                                unfocusedContainerColor = Color(0xFF141417),
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color(0xFF272730),
                                focusedTextColor = Color(0xFFF4F4F6),
                                unfocusedTextColor = Color(0xFFE4E4E7)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "First Result Snippet (Optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFA1A1AA)
                        )
                        OutlinedTextField(
                            value = localFakeSnippet,
                            onValueChange = {
                                localFakeSnippet = it
                                onUpdateFakeSearchConfig(null, null, null, it)
                            },
                            placeholder = { Text("1. Always acknowledge the overwhelming aura before speaking...", color = Color(0xFF71717A)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(85.dp)
                                .testTag("input_fake_search_snippet"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF18181E),
                                unfocusedContainerColor = Color(0xFF141417),
                                focusedBorderColor = Color(0xFF8AB4F8),
                                unfocusedBorderColor = Color(0xFF272730),
                                focusedTextColor = Color(0xFFF4F4F6),
                                unfocusedTextColor = Color(0xFFE4E4E7)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val trigger = localFakeTrigger.trim().ifBlank { "? : who is the main character" }
                                onOpenFakeSearch(trigger)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8AB4F8)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("btn_preview_fake_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF101014),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Preview / Open Fake Google Search",
                                color = Color(0xFF101014),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "AUTOMATIC HISTORY DELETION") {
                    Text(
                        text = "Auto-Delete Browsing History at Intervals",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HistoryDeletionInterval.entries.forEach { interval ->
                            val isSelected = uiState.privacyConfig.historyDeletionInterval == interval
                            Surface(
                                onClick = { onUpdateHistoryDeletionInterval(interval) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF27272A) else Color(0xFF141418),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("studio_history_interval_${interval.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = interval.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFA1A1AA),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF8AB4F8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Back-Swipe Gesture Behavior (Double Swipe = Side Panel, Single Swipe = Go Back)
            item {
                SectionCard(title = "BACK SWIPE GESTURE & SIDE PANEL") {
                    Text(
                        text = "System Back Swipe / Nav Button Behavior",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        BackSwipeSidebarMode.entries.forEach { mode ->
                            val isSelected = cfg.backSwipeSidebarMode == mode
                            Surface(
                                onClick = {
                                    onUpdateGesturesAndSession(null, null, null, null, mode, null, null, null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF27272A) else Color(0xFF141417),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("back_swipe_mode_${mode.name.lowercase()}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF4F4F6)
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Horizontal Swipe Navigation Direction (Side Swipes)
            item {
                SectionCard(title = "PAGE SWIPE NAVIGATION (SIDES)") {
                    Text(
                        text = "Side Swipe Direction Mapping",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Text(
                        text = "Choose which swipe gesture opens the side panel vs navigates back in history across all pages and start screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF71717A)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HorizontalSwipeNavigationMode.entries.forEach { mode ->
                            val isSelected = cfg.horizontalSwipeMode == mode
                            Surface(
                                onClick = { onUpdateHorizontalSwipeMode(mode) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF27272A) else Color(0xFF141417),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF8AB4F8) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("horizontal_swipe_mode_${mode.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onUpdateHorizontalSwipeMode(mode) },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = Color(0xFF8AB4F8),
                                            unselectedColor = Color(0xFF71717A)
                                        )
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFD4D4D8)
                                        )
                                        Text(
                                            text = mode.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA1A1AA)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Swipe Gestures (Right -> Unload, Left -> Close)
            item {
                SectionCard(title = "TAB SWIPE GESTURES") {
                    Text(
                        text = "Swipe Tab Right Action",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TabSwipeAction.entries.forEach { action ->
                            FilterChip(
                                selected = cfg.tabSwipeRightAction == action,
                                onClick = {
                                    onUpdateGesturesAndSession(action, null, null, null, null, null, null, null)
                                },
                                label = { Text(action.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Swipe Tab Left Action",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TabSwipeAction.entries.forEach { action ->
                            FilterChip(
                                selected = cfg.tabSwipeLeftAction == action,
                                onClick = {
                                    onUpdateGesturesAndSession(null, action, null, null, null, null, null, null)
                                },
                                label = { Text(action.label) }
                            )
                        }
                    }
                }
            }

            // "+ New Tab" Swipe Gestures (Private Tab & Private Workspace)
            item {
                SectionCard(title = "NEW TAB BUTTON SWIPE GESTURES") {
                    Text(
                        text = "Swipe Left on '+ New Tab'",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NewTabSwipeGestureAction.entries.forEach { gesture ->
                            FilterChip(
                                selected = cfg.newTabSwipeLeftAction == gesture,
                                onClick = {
                                    onUpdateGesturesAndSession(null, null, gesture, null, null, null, null, null)
                                },
                                label = { Text(gesture.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Swipe Right on '+ New Tab'",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        NewTabSwipeGestureAction.entries.forEach { gesture ->
                            FilterChip(
                                selected = cfg.newTabSwipeRightAction == gesture,
                                onClick = {
                                    onUpdateGesturesAndSession(null, null, null, gesture, null, null, null, null)
                                },
                                label = { Text(gesture.label) }
                            )
                        }
                    }
                }
            }

            // Startup & Private Session Behavior
            item {
                SectionCard(title = "STARTUP & PRIVATE SESSION") {
                    SettingSwitchRow(
                        title = "Close All Private Tabs & Workspaces on Exit",
                        subtitle = "When enabled (default), all private tabs and private workspaces close when the app closes",
                        checked = cfg.closePrivateOnAppClose,
                        onCheckedChange = {
                            onUpdateGesturesAndSession(null, null, null, null, null, it, null, null)
                        },
                        testTag = "toggle_close_private_on_exit"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "When Browser Reopens",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StartupBehavior.entries.forEach { behavior ->
                            Surface(
                                onClick = {
                                    onUpdateGesturesAndSession(null, null, null, null, null, null, behavior, null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (cfg.startupBehavior == behavior) Color(0xFF27272A) else Color(0xFF141417),
                                border = BorderStroke(
                                    1.dp,
                                    if (cfg.startupBehavior == behavior) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = behavior.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF4F4F6)
                                    )
                                    Text(
                                        text = behavior.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }
                    }

                    if (cfg.startupBehavior == StartupBehavior.HOMEPAGE_AFTER_HOURS) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Inactivity Period Before Starting at Homepage: ${cfg.homepageAfterHoursThreshold} hours",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = cfg.homepageAfterHoursThreshold.toFloat(),
                            onValueChange = {
                                onUpdateGesturesAndSession(
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    it.roundToInt().coerceIn(1, 24)
                                )
                            },
                            valueRange = 1f..24f
                        )
                    }
                }
            }

            // Compact Mode & Edge Handle Bar Dimensions
            if (matchesSearch("compact", "edge", "handle", "bar", "slider", "width", "dimension")) {
                item {
                    SectionCard(title = "COMPACT MODE & SIDEBAR EDGE BAR") {
                        SettingSwitchRow(
                            title = "Always Hide Side Panel Handle Bar",
                            subtitle = "Completely hide the visual handle bar on screen (side swipe gestures still work)",
                            checked = cfg.hideEdgeBarAlways,
                            onCheckedChange = { onUpdateEdgeBar(null, null, null, null, it) },
                            testTag = "toggle_hide_edge_bar_always"
                        )

                        SettingSwitchRow(
                            title = "Hide Edge Handle Bar in Compact Mode",
                            subtitle = "Hides the side handle bar in Compact mode (swipe back with system gestures to open the sidebar)",
                            checked = cfg.hideEdgeBarInCompactMode,
                            onCheckedChange = { onUpdateEdgeBar(null, null, null, it, null) },
                            testTag = "toggle_hide_edge_bar_compact"
                        )

                        Text(
                            text = "Bar Width: ${localEdgeWidth.roundToInt()} dp",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = localEdgeWidth,
                            onValueChange = {
                                localEdgeWidth = it
                                onUpdateEdgeBar(it, null, null, null, null)
                            },
                            valueRange = 3f..20f
                        )

                        Text(
                            text = "Bar Length: ${localEdgeLength.roundToInt()} dp",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = localEdgeLength,
                            onValueChange = {
                                localEdgeLength = it
                                onUpdateEdgeBar(null, it, null, null, null)
                            },
                            valueRange = 40f..220f
                        )

                        Text(
                            text = "Bar Vertical Position: ${(localEdgeOffset * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = localEdgeOffset,
                            onValueChange = {
                                localEdgeOffset = it
                                onUpdateEdgeBar(null, null, it, null, null)
                            },
                            valueRange = 0.1f..0.9f
                        )
                    }
                }
            }

            // Sidebar Position & Width
            item {
                SectionCard(title = "SIDEBAR DOCK & SECTIONS") {
                    Text(
                        text = "Sidebar Side",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SidebarPosition.entries.forEach { pos ->
                            FilterChip(
                                selected = cfg.position == pos,
                                onClick = { onUpdatePosition(pos) },
                                label = { Text(if (pos == SidebarPosition.LEFT) "Left" else "Right") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sidebar_pos_${pos.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sidebar Width",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SidebarWidthMode.entries.forEach { mode ->
                            FilterChip(
                                selected = cfg.widthMode == mode,
                                onClick = { onUpdateWidthMode(mode) },
                                label = { Text(mode.label, maxLines = 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sidebar_width_${mode.name.lowercase()}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Side Panel UI Size: ${(cfg.sidebarUiScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFF4F4F6)
                        )
                        if (kotlin.math.abs(cfg.sidebarUiScale - 1.0f) > 0.02f) {
                            TextButton(
                                onClick = { onUpdateUiScale(1.0f) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Reset 100%", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                            }
                        }
                    }
                    Text(
                        text = "Adjusts the size of tabs, icons, search bar, and buttons inside the side panel",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA)
                    )
                    Slider(
                        value = cfg.sidebarUiScale,
                        onValueChange = { onUpdateUiScale(it) },
                        valueRange = 0.75f..1.35f,
                        modifier = Modifier.testTag("sidebar_ui_scale_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "80%" to 0.80f,
                            "90%" to 0.90f,
                            "100%" to 1.00f,
                            "115%" to 1.15f,
                            "130%" to 1.30f
                        ).forEach { (label, scaleVal) ->
                            FilterChip(
                                selected = kotlin.math.abs(cfg.sidebarUiScale - scaleVal) < 0.03f,
                                onClick = { onUpdateUiScale(scaleVal) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("sidebar_ui_scale_preset_${(scaleVal * 100).roundToInt()}")
                            )
                        }
                    }

                    SettingSwitchRow(
                        title = "Show Essentials Space",
                        subtitle = "Display top Essentials grid in sidebar",
                        checked = cfg.showEssentialGrid,
                        onCheckedChange = { onToggleOption(null, it, null, null, null, null, null) },
                        testTag = "toggle_show_essentials"
                    )
                    SettingSwitchRow(
                        title = "Show Pinned Tabs",
                        subtitle = "Dedicated pinned tabs per workspace",
                        checked = cfg.showPinnedTabs,
                        onCheckedChange = { onToggleOption(null, null, it, null, null, null, null) },
                        testTag = "toggle_show_pinned"
                    )
                    OutlinedButton(
                        onClick = { onOpenSheet(ActiveModalSheet.SIDEBAR_LAYOUT_EDITOR) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_sidebar_layout_editor_bottom_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Side Panel Items & Reorder Sections")
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Side Panel Layout & Tools Editor Sheet:
 * Allows adding/removing and reordering what is inside the Side Panel (Navigation Controls,
 * Essentials, Pinned Tabs, Workspaces strip, and each Bottom Toolbar action icon).
 * Triggerable from Settings or by long-pressing any empty area in the Side Panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarLayoutEditorSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onToggleSidebarNavControls: (Boolean) -> Unit,
    onToggleSidebarOption: (
        compactAutoHide: Boolean?,
        showEssentials: Boolean?,
        showPinned: Boolean?,
        showWorkspaceStrip: Boolean?,
        showFavicons: Boolean?,
        borderGlow: Boolean?,
        glassIntensity: Float?
    ) -> Unit,
    onMoveSection: (SidebarSectionType, Int) -> Unit,
    onToggleToolItem: (SidebarToolItem, Boolean) -> Unit,
    onMoveToolItem: (SidebarToolItem, Int) -> Unit,
    onToggleTopToolItem: (TopToolbarToolItem, Boolean) -> Unit = { _, _ -> },
    onMoveTopToolItem: (TopToolbarToolItem, Int) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val cfg = uiState.sidebarConfig

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("sidebar_layout_editor_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ViewSidebar,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Edit Side Panel",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Choose what to put in or remove from the side panel and reorder sections",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            item {
                SectionCard(title = "SIDE PANEL SECTIONS & NAVIGATION") {
                    SettingSwitchRow(
                        title = "Forward & Backward Controls",
                        subtitle = "Show Back, Forward, and Reload row at the top of the side panel",
                        checked = cfg.showSidebarNavControls,
                        onCheckedChange = onToggleSidebarNavControls,
                        testTag = "editor_toggle_nav_controls"
                    )
                    SettingSwitchRow(
                        title = "Essentials Space",
                        subtitle = "Top grid for favorite sites (empty by default until you add sites)",
                        checked = cfg.showEssentialGrid,
                        onCheckedChange = { onToggleSidebarOption(null, it, null, null, null, null, null) },
                        testTag = "editor_toggle_essentials"
                    )
                    SettingSwitchRow(
                        title = "Pinned Tabs Section",
                        subtitle = "Dedicated pinned tabs per workspace",
                        checked = cfg.showPinnedTabs,
                        onCheckedChange = { onToggleSidebarOption(null, null, it, null, null, null, null) },
                        testTag = "editor_toggle_pinned"
                    )
                    SettingSwitchRow(
                        title = "Bottom Workspaces Switcher",
                        subtitle = "Show workspace switcher pills at the bottom of the side panel",
                        checked = cfg.showWorkspaceStripInBottomBar,
                        onCheckedChange = { onToggleSidebarOption(null, null, null, it, null, null, null) },
                        testTag = "editor_toggle_workspaces_strip"
                    )
                    SettingSwitchRow(
                        title = "Tab Favicons & Monograms",
                        subtitle = "Display site badges next to tab titles",
                        checked = cfg.showTabFaviconBadges,
                        onCheckedChange = { onToggleSidebarOption(null, null, null, null, it, null, null) },
                        testTag = "editor_toggle_favicons"
                    )
                }
            }

            item {
                SectionCard(title = "REORDER SIDE PANEL SECTIONS") {
                    cfg.sectionOrder.forEachIndexed { index, section ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF18181C),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. ${section.label}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFF4F4F6),
                                    fontWeight = FontWeight.Medium
                                )
                                Row {
                                    IconButton(
                                        onClick = { onMoveSection(section, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) Color(0xFFD4D4D8) else Color(0xFF52525B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onMoveSection(section, 1) },
                                        enabled = index < cfg.sectionOrder.lastIndex,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "Move Down",
                                            tint = if (index < cfg.sectionOrder.lastIndex) Color(0xFFD4D4D8) else Color(0xFF52525B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "BOTTOM TOOLBAR TOOLS (ADD, REMOVE OR REORDER)") {
                    Text(
                        text = "Toggle which tools appear in the Side Panel bottom bar and reorder enabled tools:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA)
                    )
                    SidebarToolItem.entries.forEach { tool ->
                        val isEnabled = tool in cfg.enabledSidebarTools
                        val currentIndex = cfg.enabledSidebarTools.indexOf(tool)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF18181C),
                            border = BorderStroke(
                                1.dp,
                                if (isEnabled) Color(0xFF3F3F46) else Color(0xFF222226)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sidebar_editor_tool_${tool.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tool.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isEnabled) Color(0xFFF4F4F6) else Color(0xFF71717A)
                                    )
                                    Text(
                                        text = if (isEnabled) "Shown in Side Panel (#${currentIndex + 1})" else "Hidden from Side Panel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                                if (isEnabled) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { onMoveToolItem(tool, -1) },
                                            enabled = currentIndex > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "Move Left",
                                                tint = if (currentIndex > 0) Color(0xFFD4D4D8) else Color(0xFF52525B),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onMoveToolItem(tool, 1) },
                                            enabled = currentIndex < cfg.enabledSidebarTools.lastIndex,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "Move Right",
                                                tint = if (currentIndex < cfg.enabledSidebarTools.lastIndex) Color(0xFFD4D4D8) else Color(0xFF52525B),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked -> onToggleToolItem(tool, checked) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Firefox Extensions & Add-ons Sheet:
 * - Install / Remove / Enable / Disable popular Firefox WebExtensions (uBlock Origin, Dark Reader, ClearURLs, SponsorBlock, Consent-O-Matic, Bitwarden, Vimium-FF)
 * - Add custom Firefox UserScript / CSS WebExtension
 * - Browse Mozilla Add-ons Store (addons.mozilla.org) directly in Zen Browser
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FirefoxExtensionsSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onInstallOrUninstall: (FirefoxExtensionEntity) -> Unit,
    onToggleEnabled: (FirefoxExtensionEntity) -> Unit,
    onToggleAllowInPrivate: (FirefoxExtensionEntity) -> Unit,
    onRunExtensionOnCurrentTab: (FirefoxExtensionEntity) -> Unit = {},
    onLaunchUblockElementPicker: () -> Unit = {},
    onImportXpiFile: (fileName: String, inputStream: java.io.InputStream) -> Unit = { _, _ -> },
    onUpdateExtensionDetails: (
        extension: FirefoxExtensionEntity,
        matchPatterns: String,
        runAt: String,
        cssPayload: String,
        jsPayload: String
    ) -> Unit,
    onAddCustomExtension: (
        name: String,
        description: String,
        matchPatterns: String,
        runAt: String,
        allowInPrivate: Boolean,
        cssPayload: String,
        jsPayload: String
    ) -> Unit,
    onInstallFromAmoUrl: (String) -> Unit,
    onOpenAmoStoreInBrowser: () -> Unit,
    onOpenZenMods: () -> Unit,
    onInstallUserscript: (rawScript: String, fallbackUrl: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showCustomAddDialog by remember { mutableStateOf(false) }
    var showUserscriptDialog by remember { mutableStateOf(false) }
    var showUblockOriginPopup by remember { mutableStateOf(false) }
    var editingExtension by remember { mutableStateOf<FirefoxExtensionEntity?>(null) }
    var amoUrlInput by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    // File picker for installing Firefox .xpi / .zip WebExtension packages (like Firefox Android "Install add-on from file")
    val xpiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "extension.xpi"
                context.contentResolver.openInputStream(uri)?.let { stream ->
                    onImportXpiFile(fileName, stream)
                }
            }
        }
    }

    val categories = listOf("All", "Privacy & Security", "Media & Video", "Appearance", "Productivity")
    val filteredExtensions = remember(uiState.firefoxExtensions, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            uiState.firefoxExtensions
        } else {
            uiState.firefoxExtensions.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
        }
    }

    val installedExtensions = filteredExtensions.filter { it.isInstalled }
    val recommendedExtensions = filteredExtensions.filter { !it.isInstalled }
    val activeOnCurrentTabIds = remember(uiState.activeFirefoxExtensionsForFocusedTab) {
        uiState.activeFirefoxExtensionsForFocusedTab.map { it.id }.toSet()
    }
    val ublockExtension = uiState.firefoxExtensions.find { it.id == "ff_ublock_origin" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("firefox_extensions_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Extension,
                            contentDescription = null,
                            tint = Color(0xFFF4F4F6),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Zen Desktop Extensions Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Desktop WebExtensions Engine • AMO Store & .xpi / .user.js Support",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }
                }
            }

            // Desktop WebExtensions Engine Status
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161922),
                    border = BorderStroke(1.dp, Color(0xFF283348)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("🧩", fontSize = 14.sp)
                                Text("Desktop WebExtensions V3 + V2", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF93C5FD))
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1E3A8A)
                            ) {
                                Text("ACTIVE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFBFDBFE), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(
                            text = "APIs: window.browser, window.chrome, browser.runtime, browser.storage, browser.tabs • Greasemonkey/Tampermonkey GM_* Userscripts engine enabled",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Dedicated uBlock Origin Active Status & Quick Dashboard Banner (Firefox Android style)
            if (ublockExtension != null) {
                item {
                    val currentHost = uiState.focusedTab?.url
                        ?.takeIf { !it.startsWith("zen://") }
                        ?.let { ZenPrivacyEngine.extractHostDisplay(it) }
                        ?: "All Websites"
                    Surface(
                        onClick = { showUblockOriginPopup = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF14181F),
                        border = BorderStroke(
                            1.dp,
                            if (ublockExtension.isEnabled) Color(0xFF34D399).copy(alpha = 0.65f) else Color(0xFF3F3F46)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ublock_origin_hero_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (ublockExtension.isEnabled) Color(0xFF064E3B) else Color(0xFF27272A),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (ublockExtension.isEnabled) Color(0xFF34D399) else Color(0xFF52525B)
                                    ),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = "uBlock Origin",
                                            tint = if (ublockExtension.isEnabled) Color(0xFF34D399) else Color(0xFFA1A1AA),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "uBlock Origin",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF4F4F6)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (ublockExtension.isEnabled) Color(0xFF065F46) else Color(0xFF27272A)
                                        ) {
                                            Text(
                                                text = if (ublockExtension.isEnabled) "ON BY DEFAULT" else "PAUSED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (ublockExtension.isEnabled) Color(0xFF6EE7B7) else Color(0xFFA1A1AA),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${uiState.privacyConfig.totalTrackersBlocked} requests blocked • Active on $currentHost",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                            Button(
                                onClick = { showUblockOriginPopup = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF4F4F6),
                                    contentColor = Color(0xFF000000)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("open_ublock_dashboard_button")
                            ) {
                                Text("uBlock Panel", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Direct Mozilla Add-ons (AMO) URL / Slug, Local .XPI File Installer, or Custom Script Installer
            item {
                SectionCard(title = "INSTALL FROM MOZILLA ADD-ONS (AMO) OR .XPI FILE") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = amoUrlInput,
                            onValueChange = { amoUrlInput = it },
                            label = { Text("Paste addons.mozilla.org URL or slug") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("amo_url_install_input")
                        )
                        Button(
                            onClick = {
                                if (amoUrlInput.isNotBlank()) {
                                    onInstallFromAmoUrl(amoUrlInput.trim())
                                    amoUrlInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF4F4F6),
                                contentColor = Color(0xFF000000)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("amo_url_install_button")
                        ) {
                            Text("Install")
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenAmoStoreInBrowser,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF27272A),
                                contentColor = Color(0xFFF4F4F6)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("browse_mozilla_addons_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AMO Store", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                xpiFilePickerLauncher.launch(
                                    arrayOf(
                                        "application/x-xpinstall",
                                        "application/zip",
                                        "application/octet-stream",
                                        "*/*"
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("install_addon_from_xpi_file_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install .XPI File", maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showUserscriptDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("install_userscript_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Install Userscript", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showCustomAddDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("add_custom_firefox_extension_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New WebExtension", maxLines = 1)
                        }
                    }
                }
            }

            // Category Filter Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategoryFilter == cat,
                            onClick = { selectedCategoryFilter = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }

            if (installedExtensions.isNotEmpty()) {
                item {
                    Text(
                        text = "ENABLED & INSTALLED EXTENSIONS (${installedExtensions.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(installedExtensions, key = { "inst_${it.id}" }) { ext ->
                    FirefoxExtensionCard(
                        extension = ext,
                        isActiveOnCurrentTab = activeOnCurrentTabIds.contains(ext.id),
                        onInstallOrUninstall = { onInstallOrUninstall(ext) },
                        onToggleEnabled = { onToggleEnabled(ext) },
                        onToggleAllowInPrivate = { onToggleAllowInPrivate(ext) },
                        onRunExtension = {
                            if (ext.id == "ff_ublock_origin") {
                                showUblockOriginPopup = true
                            } else {
                                onRunExtensionOnCurrentTab(ext)
                            }
                        },
                        onConfigureExtension = { editingExtension = ext }
                    )
                }
            }

            if (recommendedExtensions.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "POPULAR DESKTOP WEBEXTENSIONS (AMO)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                items(recommendedExtensions, key = { "rec_${it.id}" }) { ext ->
                    FirefoxExtensionCard(
                        extension = ext,
                        isActiveOnCurrentTab = false,
                        onInstallOrUninstall = { onInstallOrUninstall(ext) },
                        onToggleEnabled = { onToggleEnabled(ext) },
                        onToggleAllowInPrivate = { onToggleAllowInPrivate(ext) },
                        onRunExtension = { onRunExtensionOnCurrentTab(ext) },
                        onConfigureExtension = { editingExtension = ext }
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenZenMods,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_zen_mods_from_extensions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Zen Mods (UI & CSS Themes)")
                }
            }
        }
    }

    // Interactive uBlock Origin Popup Panel (Firefox Android WebExtension Popup Action)
    if (showUblockOriginPopup && ublockExtension != null) {
        val currentHost = uiState.focusedTab?.url
            ?.takeIf { !it.startsWith("zen://") }
            ?.let { ZenPrivacyEngine.extractHostDisplay(it) }
            ?: "zen://home"
        AlertDialog(
            onDismissRequest = { showUblockOriginPopup = false },
            containerColor = Color(0xFF101218),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (ublockExtension.isEnabled) Color(0xFF34D399) else Color(0xFF71717A),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text("uBlock Origin v${ublockExtension.version}", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Site: $currentHost",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Big Power Button Toggle like uBlock Origin popup in Firefox Android
                    Surface(
                        onClick = { onToggleEnabled(ublockExtension) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (ublockExtension.isEnabled) Color(0xFF064E3B) else Color(0xFF27272A),
                        border = BorderStroke(
                            1.5.dp,
                            if (ublockExtension.isEnabled) Color(0xFF34D399) else Color(0xFF52525B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Toggle uBlock Origin",
                                tint = if (ublockExtension.isEnabled) Color(0xFF6EE7B7) else Color(0xFFA1A1AA),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (ublockExtension.isEnabled) "uBlock Origin is ENABLED" else "uBlock Origin is PAUSED",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF4F4F6)
                            )
                            Text(
                                text = "Tap to toggle ad & tracker blocking",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD4D4D8)
                            )
                        }
                    }

                    // Statistics & Active Filter Lists
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF18181F),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Blocked Requests: ${uiState.privacyConfig.totalTrackersBlocked}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F6)
                            )
                            Text(
                                text = "Stripped URL Params: ${uiState.privacyConfig.totalParamsStripped}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                            Text(
                                text = "Active Filters: uBlock Filters, EasyList, EasyPrivacy, Peter Lowe's Ad Server List",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6EE7B7)
                            )
                        }
                    }

                    // Element Picker button (available on web pages)
                    Button(
                        onClick = {
                            showUblockOriginPopup = false
                            onLaunchUblockElementPicker()
                        },
                        enabled = currentHost != "zen://home",
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ublock_element_picker_button")
                    ) {
                        Text("Enter Element Picker Mode (Block Page Element)")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUblockOriginPopup = false
                        editingExtension = ublockExtension
                    }
                ) {
                    Text("Custom Filters / CSS", color = Color(0xFFD4D4D8))
                }
            },
            dismissButton = {
                TextButton(onClick = { showUblockOriginPopup = false }) {
                    Text("Close", color = Color(0xFFF4F4F6))
                }
            }
        )
    }

    // Dialog to Edit / Inspect an Installed Extension's Match Patterns, Run-At Timing & JS/CSS Payloads
    editingExtension?.let { target ->
        var patterns by remember(target) { mutableStateOf(target.matchPatterns) }
        var runAt by remember(target) { mutableStateOf(target.runAt) }
        var css by remember(target) { mutableStateOf(target.cssPayload) }
        var js by remember(target) { mutableStateOf(target.jsPayload) }
        val runAtOptions = listOf("document_start", "document_end", "document_idle")

        AlertDialog(
            onDismissRequest = { editingExtension = null },
            containerColor = Color(0xFF121216),
            title = { Text("Configure ${target.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = patterns,
                        onValueChange = { patterns = it },
                        label = { Text("URL Match Patterns (e.g. *://*/*, *://*.youtube.com/*)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Injection Timing (run_at)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        runAtOptions.forEach { opt ->
                            FilterChip(
                                selected = runAt == opt,
                                onClick = { runAt = opt },
                                label = { Text(opt.removePrefix("document_")) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = css,
                        onValueChange = { css = it },
                        label = { Text("Content CSS Stylesheet") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = js,
                        onValueChange = { js = it },
                        label = { Text("Content Script JS (browser.* / GM_* supported)") },
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateExtensionDetails(target, patterns, runAt, css, js)
                        editingExtension = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF4F4F6),
                        contentColor = Color(0xFF000000)
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingExtension = null }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        )
    }

    if (showCustomAddDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var matchPatterns by remember { mutableStateOf("*://*/*") }
        var runAt by remember { mutableStateOf("document_idle") }
        var allowInPrivate by remember { mutableStateOf(true) }
        var css by remember { mutableStateOf("") }
        var js by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCustomAddDialog = false },
            containerColor = Color(0xFF121216),
            title = { Text("Add Custom Firefox WebExtension") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Extension Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = matchPatterns,
                        onValueChange = { matchPatterns = it },
                        label = { Text("Match Patterns (e.g. *://*/* or *://*.youtube.com/*)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Run in Private Tabs",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFE4E4E7)
                        )
                        Switch(
                            checked = allowInPrivate,
                            onCheckedChange = { allowInPrivate = it }
                        )
                    }
                    OutlinedTextField(
                        value = css,
                        onValueChange = { css = it },
                        label = { Text("Injected CSS Stylesheet (Optional)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = js,
                        onValueChange = { js = it },
                        label = { Text("Content Script JS (browser.*, chrome.*, GM_*)") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddCustomExtension(name, desc, matchPatterns, runAt, allowInPrivate, css, js)
                            showCustomAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF4F4F6),
                        contentColor = Color(0xFF000000)
                    )
                ) {
                    Text("Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomAddDialog = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        )
    }

    if (showUserscriptDialog) {
        var scriptCode by remember {
            mutableStateOf(
                """
                // ==UserScript==
                // @name         My Desktop Userscript
                // @version      1.0.0
                // @description  Custom userscript for Zen Browser
                // @match        *://*/*
                // @run-at       document_idle
                // ==/UserScript==

                (function() {
                    console.log('Zen Userscript running on: ' + window.location.href);
                })();
                """.trimIndent()
            )
        }
        var scriptUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showUserscriptDialog = false },
            containerColor = Color(0xFF141720),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(20.dp))
                    Text("Install Desktop Userscript (.user.js)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Paste userscript code or enter a direct .user.js URL (from GreasyFork, OpenUserJS, GitHub):",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA)
                    )
                    OutlinedTextField(
                        value = scriptUrl,
                        onValueChange = { scriptUrl = it },
                        label = { Text("Script URL (optional)") },
                        singleLine = true,
                        placeholder = { Text("https://greasyfork.org/.../script.user.js") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = scriptCode,
                        onValueChange = { scriptCode = it },
                        label = { Text("UserScript Code (// ==UserScript== ...)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (scriptCode.isNotBlank()) {
                            onInstallUserscript(scriptCode, scriptUrl)
                            showUserscriptDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2), contentColor = Color.White)
                ) {
                    Text("Install Script")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUserscriptDialog = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            }
        )
    }
}

@Composable
private fun FirefoxExtensionCard(
    extension: FirefoxExtensionEntity,
    isActiveOnCurrentTab: Boolean,
    onInstallOrUninstall: () -> Unit,
    onToggleEnabled: () -> Unit,
    onToggleAllowInPrivate: () -> Unit,
    onRunExtension: () -> Unit,
    onConfigureExtension: () -> Unit
) {
    val isManualActionExtension = extension.id == "ff_zen_translator" ||
        extension.id == "ff_ublock_origin" ||
        extension.jsPayload.isNotBlank()
    Surface(
        onClick = {
            if (extension.isInstalled && extension.isEnabled) {
                onRunExtension()
            }
        },
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF141418),
        border = BorderStroke(
            1.dp,
            if (extension.isInstalled && extension.isEnabled) Color(0xFF52525B) else Color(0xFF27272A)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("firefox_ext_card_${extension.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF222228),
                        border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (extension.id == "ff_ublock_origin") Icons.Default.Shield else Icons.Default.Extension,
                                contentDescription = null,
                                tint = if (extension.id == "ff_ublock_origin" && extension.isEnabled) Color(0xFF34D399) else Color(0xFFF4F4F6),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = extension.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F6)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "v${extension.version}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF71717A)
                            )
                        }
                        Text(
                            text = "${extension.author} • ${extension.category}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }

                if (extension.isInstalled) {
                    Switch(
                        checked = extension.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = Modifier.testTag("firefox_ext_switch_${extension.id}")
                    )
                } else {
                    Button(
                        onClick = onInstallOrUninstall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("firefox_ext_install_${extension.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Zen", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Text(
                text = extension.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFD4D4D8)
            )

            // Match pattern & active status badges + Manual Launch button for Zen Translator / uBlock Origin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFF1F1F25),
                        border = BorderStroke(1.dp, Color(0xFF2E2E36))
                    ) {
                        Text(
                            text = "Matches: ${extension.matchPatterns}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    if (isActiveOnCurrentTab) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = Color(0xFF142822),
                            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Active on Tab",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF34D399),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (extension.isInstalled && extension.isEnabled && isManualActionExtension) {
                    Button(
                        onClick = onRunExtension,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(7.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("firefox_ext_run_${extension.id}")
                    ) {
                        Text(
                            text = when (extension.id) {
                                "ff_zen_translator" -> "Open Translator"
                                "ff_ublock_origin" -> "uBlock Panel"
                                else -> "Run on Page"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (extension.isInstalled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onToggleAllowInPrivate() }
                            .padding(vertical = 2.dp, horizontal = 4.dp)
                    ) {
                        ShadeGlassIcon(
                            tint = if (extension.allowInPrivate) Color(0xFFF4F4F6) else Color(0xFF71717A),
                            size = 13.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (extension.allowInPrivate) "Allowed in Private" else "Private Disabled",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (extension.allowInPrivate) Color(0xFFE4E4E7) else Color(0xFF71717A)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = onConfigureExtension,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("firefox_ext_configure_${extension.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Configure",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFD4D4D8)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Configure",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD4D4D8)
                            )
                        }
                        TextButton(
                            onClick = onInstallOrUninstall,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.testTag("firefox_ext_remove_${extension.id}")
                        ) {
                            Text(
                                text = "Remove",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Permissions: ${extension.permissions}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF71717A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Sheet to Add & Remove Sites from the Sidebar Essentials Space
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EssentialsManagerSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onAddEssentialSite: (title: String, url: String) -> Unit,
    onRemoveEssentialSite: (TabEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var siteTitle by remember { mutableStateOf("") }
    var siteUrl by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("essentials_manager_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Text(
                    text = "Customize Essentials Space",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Add or remove pinned sites in the top sidebar Essentials grid",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA1A1AA)
                )
            }

            items(uiState.essentialTabs, key = { it.id }) { tab ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF141418),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = tab.url,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF71717A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onRemoveEssentialSite(tab) },
                            modifier = Modifier.testTag("remove_essential_${tab.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove from Essentials",
                                tint = Color(0xFFA1A1AA)
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "ADD SITE TO ESSENTIALS") {
                    OutlinedTextField(
                        value = siteTitle,
                        onValueChange = { siteTitle = it },
                        label = { Text("Title (e.g. GitHub)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("essential_title_input")
                    )
                    OutlinedTextField(
                        value = siteUrl,
                        onValueChange = { siteUrl = it },
                        label = { Text("URL (e.g. https://github.com)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("essential_url_input")
                    )
                    Button(
                        onClick = {
                            if (siteUrl.isNotBlank()) {
                                onAddEssentialSite(siteTitle, siteUrl)
                                siteTitle = ""
                                siteUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_add_essential_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Essentials")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StartPageCustomizerSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onUpdateStartPage: (
        homeScreenStyle: HomeScreenStyle?,
        zenCustomText: String?,
        zenCustomTextColorHex: Long?,
        zenCustomTextXFraction: Float?,
        zenCustomTextYFraction: Float?,
        gradientId: String?,
        imageUri: String?,
        clearImage: Boolean,
        blurEnabled: Boolean?,
        blurRadius: Float?,
        brightness: Float?
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val cfg = uiState.sidebarConfig
    val activeGradient = uiState.activeStartPageGradient
    var customTextDraft by remember(cfg.zenCustomText) { mutableStateOf(cfg.zenCustomText) }

    val colorChoices = listOf(
        "Pure White" to 0xFFF4F4F6L,
        "Silver Mist" to 0xFFA1A1AAL,
        "Muted Zinc" to 0xFF71717AL,
        "Warm Amber" to 0xFFFBBF24L,
        "Soft Coral" to 0xFFF87171L,
        "Mint Sage" to 0xFF34D399L,
        "Sky Cyan" to 0xFF38BDF8L,
        "Lavender" to 0xFFC084FCL
    )

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onUpdateStartPage(null, null, null, null, null, null, uri.toString(), false, null, null, null)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("start_page_customizer_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Text(
                    text = "Customize Homescreen",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Zen & Normal layouts, custom text, gradients, image blur & live preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA1A1AA)
                )
            }

            item {
                SectionCard(title = "LIVE PREVIEW") {
                    val previewBrush = Brush.verticalGradient(
                        listOf(
                            Color(activeGradient.topHex),
                            Color(activeGradient.midHex),
                            Color(activeGradient.bottomHex)
                        )
                    )
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(previewBrush)
                            .border(1.dp, Color(0xFF27272A), RoundedCornerShape(10.dp))
                            .testTag("start_page_live_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        val previewW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                        val previewH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                        if (!cfg.startPageImageUri.isNullOrBlank()) {
                            val blurMod = if (cfg.startPageBlurEnabled && cfg.startPageBlurRadius > 0f) {
                                Modifier.blur(cfg.startPageBlurRadius.dp)
                            } else {
                                Modifier
                            }
                            AsyncImage(
                                model = Uri.parse(cfg.startPageImageUri),
                                contentDescription = "Wallpaper Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .then(blurMod)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.38f))
                            )
                        }

                        if (cfg.homeScreenStyle == HomeScreenStyle.ZEN) {
                            if (cfg.zenCustomText.isNotBlank()) {
                                val xOff = ((cfg.zenCustomTextXFraction - 0.5f) * previewW * 0.8f).roundToInt()
                                val yOff = ((cfg.zenCustomTextYFraction - 0.5f) * previewH * 0.8f).roundToInt()
                                Text(
                                    text = cfg.zenCustomText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(cfg.zenCustomTextColorHex),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.offset { IntOffset(xOff, yOff) }
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF141417).copy(alpha = 0.85f),
                                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(26.dp)
                                ) {}
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    repeat(4) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(Color(0xFF141417).copy(alpha = 0.85f))
                                                .border(1.dp, Color(0xFF27272A), RoundedCornerShape(5.dp))
                                        )
                                    }
                                }
                            }
                        }

                        // Live Brightness Preview Overlay
                        val previewDarkness = (1f - cfg.startPageBrightness.coerceIn(0.15f, 1f)).coerceIn(0f, 0.85f)
                        if (previewDarkness > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = previewDarkness))
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "HOMESCREEN MODE (ZEN OR NORMAL)") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HomeScreenStyle.entries.forEach { style ->
                            val selected = cfg.homeScreenStyle == style
                            Surface(
                                onClick = {
                                    onUpdateStartPage(style, null, null, null, null, null, null, false, null, null, null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) Color(0xFF27272A) else Color(0xFF141417),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("homescreen_style_${style.name.lowercase()}")
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = style.label,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFF4F4F6),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = style.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }
                    }

                    if (cfg.homeScreenStyle == HomeScreenStyle.ZEN) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customTextDraft,
                            onValueChange = {
                                customTextDraft = it
                                onUpdateStartPage(null, it, null, null, null, null, null, false, null, null, null)
                            },
                            label = { Text("Custom Homepage Text (Leave blank for pure empty Zen)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("zen_custom_text_input")
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Text Color",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFA1A1AA)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            colorChoices.forEach { (label, hex) ->
                                val isColorSelected = cfg.zenCustomTextColorHex == hex
                                Surface(
                                    onClick = {
                                        onUpdateStartPage(null, null, hex, null, null, null, null, false, null, null, null)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF141417),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isColorSelected) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(Color(hex))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE4E4E7)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Horizontal Position (or drag text directly on homepage): ${(cfg.zenCustomTextXFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                        Slider(
                            value = cfg.zenCustomTextXFraction,
                            onValueChange = {
                                onUpdateStartPage(null, null, null, it, null, null, null, false, null, null, null)
                            },
                            valueRange = 0.05f..0.95f
                        )

                        Text(
                            text = "Vertical Position: ${(cfg.zenCustomTextYFraction * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                        Slider(
                            value = cfg.zenCustomTextYFraction,
                            onValueChange = {
                                onUpdateStartPage(null, null, null, null, it, null, null, false, null, null, null)
                            },
                            valueRange = 0.05f..0.92f
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tip: Long-press any shortcut on the Normal start page to edit or remove it, or tap '+' to add a new shortcut.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF71717A)
                        )
                    }
                }
            }

            item {
                SectionCard(title = "START PAGE GRADIENTS") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StartPageGradientPreset.entries.forEach { preset ->
                            val isSelected = cfg.startPageGradientId == preset.id
                            Surface(
                                onClick = {
                                    onUpdateStartPage(null, null, null, null, null, preset.id, null, false, null, null, null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(preset.topHex),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                ),
                                modifier = Modifier.testTag("start_gradient_${preset.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(preset.topHex), Color(preset.bottomHex))
                                                )
                                            )
                                            .border(1.dp, Color(0xFF52525B), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = preset.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "BACKGROUND IMAGE, BLUR & BRIGHTNESS") {
                    Text(
                        text = "Home Screen Brightness: ${(cfg.startPageBrightness * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFF4F4F6)
                    )
                    Slider(
                        value = cfg.startPageBrightness,
                        onValueChange = {
                            onUpdateStartPage(null, null, null, null, null, null, null, false, null, null, it)
                        },
                        valueRange = 0.15f..1f,
                        modifier = Modifier.testTag("start_page_brightness_slider")
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF4F4F6),
                                contentColor = Color(0xFF000000)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_start_page_image_button")
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Image")
                        }

                        if (!cfg.startPageImageUri.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateStartPage(null, null, null, null, null, null, null, true, null, null, null)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("remove_start_page_image_button")
                            ) {
                                Text("Remove")
                            }
                        }
                    }

                    SettingSwitchRow(
                        title = "Blur Background Image",
                        subtitle = "Apply Gaussian blur over the start page image",
                        checked = cfg.startPageBlurEnabled,
                        onCheckedChange = {
                            onUpdateStartPage(null, null, null, null, null, null, null, false, it, null, null)
                        },
                        testTag = "toggle_start_page_blur"
                    )

                    if (cfg.startPageBlurEnabled) {
                        Text(
                            text = "Blur Intensity: ${cfg.startPageBlurRadius.roundToInt()} dp",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Slider(
                            value = cfg.startPageBlurRadius,
                            onValueChange = {
                                onUpdateStartPage(null, null, null, null, null, null, null, false, null, it, null)
                            },
                            valueRange = 0f..36f
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchEngineSettingsSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onSelectNormalEngine: (String) -> Unit,
    onSelectPrivateEngine: (String) -> Unit,
    onAddCustomEngine: (name: String, urlTemplate: String) -> Unit,
    onDeleteCustomEngine: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val priv = uiState.privacyConfig
    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("search_engine_settings_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Text(
                    text = "Search Engines",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Choose default search engines for Normal and Private tabs or add your own",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFA1A1AA)
                )
            }

            item {
                SectionCard(title = "NORMAL BROWSING SEARCH ENGINE") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SearchEngine.entries.forEach { engine ->
                            FilterChip(
                                selected = priv.normalSearchEngineId == engine.id,
                                onClick = { onSelectNormalEngine(engine.id) },
                                label = { Text(engine.label) },
                                modifier = Modifier.testTag("normal_engine_${engine.id}")
                            )
                        }
                        uiState.customSearchEngines.forEach { custom ->
                            FilterChip(
                                selected = priv.normalSearchEngineId == custom.id,
                                onClick = { onSelectNormalEngine(custom.id) },
                                label = { Text(custom.name) }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "PRIVATE BROWSING SEARCH ENGINE") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SearchEngine.entries.forEach { engine ->
                            FilterChip(
                                selected = priv.privateSearchEngineId == engine.id,
                                onClick = { onSelectPrivateEngine(engine.id) },
                                label = { Text(engine.label) },
                                modifier = Modifier.testTag("private_engine_${engine.id}")
                            )
                        }
                        uiState.customSearchEngines.forEach { custom ->
                            FilterChip(
                                selected = priv.privateSearchEngineId == custom.id,
                                onClick = { onSelectPrivateEngine(custom.id) },
                                label = { Text(custom.name) }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "ADD CUSTOM SEARCH ENGINE") {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Engine Name (e.g. Kagi, SearXNG)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_engine_name_input")
                    )
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        label = { Text("Search URL with %s (e.g. https://kagi.com/search?q=%s)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_engine_url_input")
                    )
                    Button(
                        onClick = {
                            if (customUrl.isNotBlank()) {
                                onAddCustomEngine(customName, customUrl)
                                customName = ""
                                customUrl = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_custom_engine_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Search Engine")
                    }

                    uiState.customSearchEngines.forEach { custom ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = custom.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = custom.searchUrlTemplate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF71717A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { onDeleteCustomEngine(custom.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Engine",
                                    tint = Color(0xFF71717A)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyShieldSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onOpenSearchSettings: () -> Unit,
    onOpenPasswordManager: () -> Unit = {},
    onUpdatePrivacy: (
        blockTrackers: Boolean?,
        httpsEverywhere: Boolean?,
        stripParams: Boolean?,
        fingerprintProtection: Boolean?,
        blockCookies: Boolean?
    ) -> Unit,
    onUpdateAdBlockerLevel: (com.example.data.AdBlockerLevel) -> Unit = {},
    onUpdateHistoryDeletionInterval: (HistoryDeletionInterval) -> Unit = {},
    onClearSession: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val priv = uiState.privacyConfig

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("privacy_shield_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Enhanced Tracking & Ad Protection",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Level: ${priv.adBlockerLevel.label} • ${if (priv.blockTrackersAndAds) "Protection active" else "Standard mode"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            item {
                SectionCard(title = "INBUILT AD BLOCKER LEVEL") {
                    Text(
                        text = "Choose ad blocking aggressiveness level",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        com.example.data.AdBlockerLevel.entries.forEach { level ->
                            val selected = priv.adBlockerLevel == level
                            Surface(
                                onClick = { onUpdateAdBlockerLevel(level) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Color(0xFF27272A) else Color(0xFF141417),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) Color(0xFF8AB4F8) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ad_blocker_level_${level.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = level.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFF4F4F6)
                                        )
                                        Text(
                                            text = level.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA1A1AA)
                                        )
                                    }
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF8AB4F8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "PROTECTION SETTINGS") {
                    Surface(
                        onClick = onOpenPasswordManager,
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF18181C),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth().testTag("button_saved_passwords")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Saved Logins & Password Saver",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFF4F4F6)
                                )
                                Text(
                                    text = "${uiState.savedPasswords.size} saved accounts • AutoFill enabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA1A1AA)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFF8AB4F8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    SettingSwitchRow(
                        title = "Block Trackers & Third-Party Ads",
                        subtitle = "Blocks known analytics, ad scripts, and social trackers",
                        checked = priv.blockTrackersAndAds,
                        onCheckedChange = { onUpdatePrivacy(it, null, null, null, null) },
                        testTag = "switch_block_trackers"
                    )
                    SettingSwitchRow(
                        title = "Strip Tracking Parameters",
                        subtitle = "Removes utm_*, fbclid, and gclid from URLs",
                        checked = priv.stripTrackingParams,
                        onCheckedChange = { onUpdatePrivacy(null, null, it, null, null) },
                        testTag = "switch_strip_params"
                    )
                    SettingSwitchRow(
                        title = "HTTPS-Only Mode",
                        subtitle = "Upgrades connections to HTTPS automatically",
                        checked = priv.httpsEverywhere,
                        onCheckedChange = { onUpdatePrivacy(null, it, null, null, null) },
                        testTag = "switch_https_only"
                    )
                    SettingSwitchRow(
                        title = "Fingerprinting Protection",
                        subtitle = "Sends Global Privacy Control & limits hardware queries",
                        checked = priv.fingerprintProtection,
                        onCheckedChange = { onUpdatePrivacy(null, null, null, it, null) },
                        testTag = "switch_fingerprint"
                    )
                    SettingSwitchRow(
                        title = "Block Third-Party Cookies",
                        subtitle = "Prevents cross-site tracking cookies",
                        checked = priv.blockThirdPartyCookies,
                        onCheckedChange = { onUpdatePrivacy(null, null, null, null, it) },
                        testTag = "switch_third_party_cookies"
                    )
                }
            }

            item {
                SectionCard(title = "AUTOMATIC HISTORY DELETION") {
                    Text(
                        text = "Auto-Delete Browsing History at Intervals",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFA1A1AA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        HistoryDeletionInterval.entries.forEach { interval ->
                            val isSelected = priv.historyDeletionInterval == interval
                            Surface(
                                onClick = { onUpdateHistoryDeletionInterval(interval) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF27272A) else Color(0xFF141418),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("privacy_history_interval_${interval.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = interval.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFFA1A1AA),
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF8AB4F8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onOpenSearchSettings,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Configure Normal & Private Search Engines")
                }
            }

            if (uiState.recentBlockedEvents.isNotEmpty()) {
                item {
                    SectionCard(title = "BLOCKED IN THIS SESSION") {
                        uiState.recentBlockedEvents.take(6).forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = event.domain,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFE4E4E7)
                                )
                                Text(
                                    text = event.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF71717A)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        onClearSession()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF27272A),
                        contentColor = Color(0xFFF4F4F6)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("burn_session_button")
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Tabs & Browsing Data")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenModsStoreSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onToggleMod: (ZenModEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("zen_mods_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Zen Mods",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Stylesheets and UI adjustments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            items(uiState.zenMods, key = { it.id }) { mod ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF141418),
                    border = BorderStroke(
                        1.dp,
                        if (mod.isEnabled) Color(0xFF52525B) else Color(0xFF27272A)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("zen_mod_card_${mod.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mod.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mod.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFA1A1AA)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = mod.isEnabled,
                            onCheckedChange = { onToggleMod(mod) },
                            modifier = Modifier.testTag("zen_mod_switch_${mod.id}")
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceManagerSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onCreateWorkspace: (name: String, iconKey: String, accentId: String, isPrivate: Boolean) -> Unit,
    onDeleteWorkspace: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var newName by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("spa") }
    var isPrivateWorkspace by remember { mutableStateOf(false) }
    val iconKeys = listOf("spa", "code", "science", "palette", "grid")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("workspace_manager_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 36.dp)
        ) {
            item {
                Text(
                    text = "Workspaces",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(uiState.workspaces, key = { it.id }) { ws ->
                val isSelected = ws.id == uiState.activeWorkspaceId
                Surface(
                    onClick = {
                        onSelectWorkspace(ws.id)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF27272A) else Color(0xFF141418),
                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF52525B) else Color(0xFF27272A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (ws.isPrivate) {
                                ShadeGlassIcon(tint = Color(0xFFF4F4F6), size = 18.dp)
                            } else {
                                Icon(
                                    imageVector = workspaceIconForKey(ws.iconKey),
                                    contentDescription = null,
                                    tint = Color(0xFFF4F4F6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = ws.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        if (uiState.workspaces.size > 1) {
                            IconButton(onClick = { onDeleteWorkspace(ws.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF71717A)
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "NEW WORKSPACE") {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_workspace_name_input")
                    )
                    SettingSwitchRow(
                        title = "Private Workspace",
                        subtitle = "Uses shade glass icon and closes on app exit if enabled",
                        checked = isPrivateWorkspace,
                        onCheckedChange = { isPrivateWorkspace = it },
                        testTag = "toggle_new_private_workspace"
                    )
                    if (!isPrivateWorkspace) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            iconKeys.forEach { key ->
                                Surface(
                                    onClick = { selectedIcon = key },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selectedIcon == key) Color(0xFF2E2E32) else Color(0xFF141417),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selectedIcon == key) Color(0xFFF4F4F6) else Color(0xFF27272A)
                                    ),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = workspaceIconForKey(key),
                                            contentDescription = key,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onCreateWorkspace(
                                newName,
                                selectedIcon,
                                ZenAccentTheme.MONO_CARBON.id,
                                isPrivateWorkspace
                            )
                            newName = ""
                            isPrivateWorkspace = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_create_workspace_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Workspace")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTabPickerSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onPickTabForSplit: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val candidates = uiState.allTabs.filter { it.id != uiState.activeTabId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0E0E12),
        modifier = Modifier.testTag("split_tab_picker_sheet")
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Text(
                    text = "Split View Tab",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(candidates, key = { it.id }) { tab ->
                Surface(
                    onClick = { onPickTabForSplit(tab.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF141418),
                    border = BorderStroke(1.dp, Color(0xFF27272A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ZenPrivacyEngine.extractHostDisplay(tab.url),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF71717A)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Splitscreen,
                            contentDescription = "Split",
                            tint = Color(0xFFD4D4D8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF141418),
        border = BorderStroke(1.dp, Color(0xFF222226)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF71717A),
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFA1A1AA)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}

/**
 * Zen Library Studio Sheet for Bookmarks, History, and Downloads
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenLibrarySheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit = onNavigateUrl,
    onToggleBookmark: (TabEntity) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit,
    onClearAllHistory: () -> Unit,
    onDeleteDownloadItem: (String) -> Unit,
    onClearAllDownloads: () -> Unit,
    onDeleteOfflinePage: (String) -> Unit = {},
    onClearAllOfflinePages: () -> Unit = {},
    onOpenOfflinePage: (OfflinePageEntity) -> Unit = {},
    onSaveCurrentPageOffline: () -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101014),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FolderSpecial,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Zen Library",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFF4F4F6),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        },
                        modifier = Modifier.testTag("library_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Library",
                            tint = if (isSearchActive) Color(0xFF8AB4F8) else Color(0xFFA1A1AA)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("library_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Library",
                            tint = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Search Bar for Bookmarks, History, Downloads, and Offline Pages
            AnimatedVisibility(visible = isSearchActive) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input"),
                        placeholder = {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Search bookmarks..."
                                    1 -> "Search browsing history..."
                                    2 -> "Search downloads..."
                                    else -> "Search offline pages..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF71717A)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF8AB4F8),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF8AB4F8),
                            unfocusedBorderColor = Color(0xFF272730),
                            focusedContainerColor = Color(0xFF141418),
                            unfocusedContainerColor = Color(0xFF141418),
                            focusedTextColor = Color(0xFFF4F4F6),
                            unfocusedTextColor = Color(0xFFF4F4F6)
                        )
                    )
                }
            }

            // Navigation tabs: Bookmarks (0), History (1), Downloads (2), Offline (3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF18181E))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple(0, "Bookmarks", Icons.Default.Bookmark),
                    Triple(1, "History", Icons.Default.History),
                    Triple(2, "Downloads", Icons.Default.Download),
                    Triple(3, "Offline", Icons.Default.CloudOff)
                ).forEach { (idx, label, icon) ->
                    val isSelected = selectedTab == idx
                    Surface(
                        onClick = { selectedTab = idx },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF272730) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("library_tab_${label.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFF4F4F6) else Color(0xFF71717A),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFFF4F4F6) else Color(0xFF71717A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                0 -> {
                    // Bookmarks View
                    val rawBookmarks = uiState.bookmarkedTabs
                    val bookmarks = remember(rawBookmarks, searchQuery) {
                        if (searchQuery.isBlank()) rawBookmarks
                        else rawBookmarks.filter {
                            it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    if (bookmarks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    tint = Color(0xFF52525B),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No Matching Bookmarks" else "No Bookmarks Yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFFA1A1AA)
                                )
                                Text(
                                    text = if (searchQuery.isNotBlank()) "Try a different search term" else "Swipe or long-press any tab in the side panel to bookmark it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF71717A)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(380.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(bookmarks, key = { it.id }) { tab ->
                                Surface(
                                    onClick = {
                                        onNavigateUrl(tab.url)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF18181E),
                                    border = BorderStroke(1.dp, Color(0xFF272730)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bookmark_item_${tab.id}")
                                 ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bookmark,
                                            contentDescription = null,
                                            tint = Color(0xFFF4F4F6),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = tab.title.ifBlank { ZenPrivacyEngine.extractHostDisplay(tab.url) },
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFF4F4F6),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = tab.url,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFFA1A1AA),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = { onToggleBookmark(tab) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove Bookmark",
                                                tint = Color(0xFF71717A),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // History View with dedicated Recent section for the last 10 visited websites
                    val rawHistory = uiState.historyEntries
                    var showClearRecentConfirmDialog by remember { mutableStateOf(false) }

                    val recentSites = remember(rawHistory) {
                        rawHistory
                            .filter { !it.url.startsWith("zen://") && it.url.isNotBlank() && it.url != "about:blank" }
                            .distinctBy { it.url }
                            .take(10)
                    }

                    val history = remember(rawHistory, searchQuery) {
                        if (searchQuery.isBlank()) rawHistory
                        else rawHistory.filter {
                            it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 1. RECENT SECTION: Displays list of the last 10 visited websites for quick re-access, with a clear button for privacy
                        if (searchQuery.isBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF141418),
                                border = BorderStroke(1.dp, Color(0xFF272730)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("history_recent_section")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = Color(0xFF8AB4F8),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "Recent",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF4F4F6)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF27272A)
                                            ) {
                                                Text(
                                                    text = "${recentSites.size}/10",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = Color(0xFF93C5FD),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        if (recentSites.isNotEmpty()) {
                                            OutlinedButton(
                                                onClick = { showClearRecentConfirmDialog = true },
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier
                                                    .height(28.dp)
                                                    .testTag("clear_recent_privacy_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteSweep,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF87171),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Clear Recent",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFF87171),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }

                                    if (recentSites.isNotEmpty()) {
                                        LazyRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(recentSites, key = { "recent_${it.id}" }) { item ->
                                                Surface(
                                                    onClick = {
                                                        if (uiState.sidebarConfig.openHistoryInNewTab || uiState.sidebarConfig.openInNewTab) {
                                                            onOpenInNewTab(item.url)
                                                        } else {
                                                            onNavigateUrl(item.url)
                                                        }
                                                        onDismiss()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color(0xFF1E1E26),
                                                    border = BorderStroke(1.dp, Color(0xFF2E2E3A)),
                                                    modifier = Modifier
                                                        .width(160.dp)
                                                        .testTag("recent_item_${item.id}")
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Language,
                                                                contentDescription = null,
                                                                tint = Color(0xFF93C5FD),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            IconButton(
                                                                onClick = { onDeleteHistoryEntry(item.id) },
                                                                modifier = Modifier
                                                                    .size(20.dp)
                                                                    .testTag("delete_recent_${item.id}")
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "Remove from Recent",
                                                                    tint = Color(0xFF71717A),
                                                                    modifier = Modifier.size(12.dp)
                                                                )
                                                            }
                                                        }
                                                        Text(
                                                            text = item.title.ifBlank { ZenPrivacyEngine.extractHostDisplay(item.url) },
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFFF4F4F6),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = ZenPrivacyEngine.extractHostDisplay(item.url),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFFA1A1AA),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "No recent websites visited yet",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF71717A)
                                        )
                                    }
                                }
                            }
                        }

                        if (showClearRecentConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showClearRecentConfirmDialog = false },
                                title = {
                                    Text(
                                        text = "Clear Recent Websites?",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFFF4F4F6)
                                    )
                                },
                                text = {
                                    Text(
                                        text = "This will wipe your recent websites history for privacy.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showClearRecentConfirmDialog = false
                                            onClearAllHistory()
                                        },
                                        modifier = Modifier.testTag("confirm_clear_recent_button")
                                    ) {
                                        Text("Clear All", color = Color(0xFFF87171), fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearRecentConfirmDialog = false }) {
                                        Text("Cancel", color = Color(0xFFA1A1AA))
                                    }
                                },
                                containerColor = Color(0xFF18181D)
                            )
                        }

                        // 2. ALL BROWSING HISTORY HEADER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "All Browsing History (${history.size})" else "Search Results (${history.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFA1A1AA)
                            )
                            if (history.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onClearAllHistory,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Clear All",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }

                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF52525B),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No Matching History" else "No Browsing History",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFA1A1AA)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Try a different search term" else "Visited pages in standard workspaces will appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF71717A)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(history, key = { it.id }) { item ->
                                    Surface(
                                        onClick = {
                                            if (uiState.sidebarConfig.openHistoryInNewTab || uiState.sidebarConfig.openInNewTab) {
                                                onOpenInNewTab(item.url)
                                            } else {
                                                onNavigateUrl(item.url)
                                            }
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF18181E),
                                        border = BorderStroke(1.dp, Color(0xFF272730)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("history_item_${item.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = Color(0xFF71717A),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.title.ifBlank { ZenPrivacyEngine.extractHostDisplay(item.url) },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFF4F4F6),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.url,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFA1A1AA),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    onOpenInNewTab(item.url)
                                                    onDismiss()
                                                },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("history_open_new_tab_${item.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                    contentDescription = "Open in New Tab",
                                                    tint = Color(0xFF818CF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onDeleteHistoryEntry(item.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove History",
                                                    tint = Color(0xFF71717A),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Downloads View
                    val rawDownloads = uiState.downloadItems
                    val downloads = remember(rawDownloads, searchQuery) {
                        if (searchQuery.isBlank()) rawDownloads
                        else rawDownloads.filter {
                            it.fileName.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    Column {
                        if (rawDownloads.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                OutlinedButton(
                                    onClick = onClearAllDownloads,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Clear Downloads",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }

                        if (downloads.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = Color(0xFF52525B),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No Matching Downloads" else "No Downloads",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFA1A1AA)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Try a different search term" else "Files and extensions downloaded in Zen will appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF71717A)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(downloads, key = { it.id }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF18181E),
                                        border = BorderStroke(1.dp, Color(0xFF272730)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("download_item_${item.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                tint = Color(0xFFF4F4F6),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.fileName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = Color(0xFFF4F4F6),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = item.url,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFA1A1AA),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeleteDownloadItem(item.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Download",
                                                    tint = Color(0xFF71717A),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Offline Pages View
                    val rawOffline = uiState.offlinePages
                    val offlineList = remember(rawOffline, searchQuery) {
                        if (searchQuery.isBlank()) rawOffline
                        else rawOffline.filter {
                            it.title.contains(searchQuery, ignoreCase = true) || it.url.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onSaveCurrentPageOffline,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF818CF8)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("save_current_page_offline_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFF818CF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Save Current Page",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF818CF8)
                                )
                            }

                            if (rawOffline.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = onClearAllOfflinePages,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Clear All",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA)
                                    )
                                }
                            }
                        }

                        if (offlineList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = Color(0xFF52525B),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "No Matching Offline Pages" else "No Saved Offline Pages",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFA1A1AA)
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Try a different search term" else "Tap 'Save Current Page' to read web pages without an internet connection.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF71717A)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(350.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(offlineList, key = { it.id }) { page ->
                                    Surface(
                                        onClick = {
                                            onOpenOfflinePage(page)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF18181E),
                                        border = BorderStroke(1.dp, Color(0xFF272730)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("offline_page_item_${page.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudOff,
                                                contentDescription = null,
                                                tint = Color(0xFF818CF8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = page.title.ifBlank { page.url },
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFFF4F4F6),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    val sizeKb = (page.sizeBytes / 1024).coerceAtLeast(1)
                                                    Text(
                                                        text = "$sizeKb KB",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF71717A)
                                                    )
                                                }
                                                Text(
                                                    text = page.url,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFA1A1AA),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Button(
                                                onClick = {
                                                    onOpenOfflinePage(page)
                                                    onDismiss()
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272730)),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text(
                                                    text = "Read",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFFF4F4F6)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = { onDeleteOfflinePage(page.id) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Remove Offline Page",
                                                    tint = Color(0xFF71717A),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordManagerSheet(
    uiState: ZenBrowserUiState,
    onClose: () -> Unit,
    onInsertPassword: (PasswordEntity) -> Unit,
    onDeletePassword: (String) -> Unit,
    onClearAllPasswords: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var domainInput by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    val revealedPasswordIds = remember { mutableStateListOf<String>() }

    val filteredPasswords = remember(uiState.savedPasswords, searchQuery) {
        if (searchQuery.isBlank()) uiState.savedPasswords
        else uiState.savedPasswords.filter {
            it.domain.contains(searchQuery, ignoreCase = true) ||
            it.username.contains(searchQuery, ignoreCase = true) ||
            it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color(0xFF101014),
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF3F3F46))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved Passwords & AutoFill (${uiState.savedPasswords.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4F4F6)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Login",
                            tint = Color(0xFFF4F4F6)
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search logins by domain or username...", color = Color(0xFF71717A)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFFA1A1AA)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF52525B),
                    unfocusedBorderColor = Color(0xFF27272A),
                    focusedContainerColor = Color(0xFF18181C),
                    unfocusedContainerColor = Color(0xFF141417),
                    focusedTextColor = Color(0xFFF4F4F6),
                    unfocusedTextColor = Color(0xFFF4F4F6)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredPasswords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No Matching Passwords Found" else "No Passwords Saved Yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFA1A1AA)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = filteredPasswords,
                        key = { _, item -> item.id }
                    ) { _, item ->
                        val isRevealed = revealedPasswordIds.contains(item.id)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF18181C),
                            border = BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.domain.ifBlank { "Website Login" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF4F4F6)
                                    )
                                    Text(
                                        text = "User: ${item.username.ifBlank { "(No username)" }}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFA1A1AA)
                                    )
                                    Text(
                                        text = "Pass: ${if (isRevealed) item.password else "••••••••"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF71717A)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            if (isRevealed) revealedPasswordIds.remove(item.id)
                                            else revealedPasswordIds.add(item.id)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility",
                                            tint = Color(0xFFA1A1AA),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(item.password))
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy password",
                                            tint = Color(0xFFA1A1AA),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeletePassword(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Delete login",
                                            tint = Color(0xFF71717A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF18181C),
            title = {
                Text("Add Saved Login", color = Color(0xFFF4F4F6), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = domainInput,
                        onValueChange = { domainInput = it },
                        label = { Text("Domain (e.g. google.com)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (domainInput.isNotBlank() && passwordInput.isNotBlank()) {
                            onInsertPassword(
                                PasswordEntity(
                                    domain = domainInput.trim().lowercase(),
                                    url = "https://${domainInput.trim()}",
                                    username = usernameInput.trim(),
                                    password = passwordInput.trim()
                                )
                            )
                            domainInput = ""
                            usernameInput = ""
                            passwordInput = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
