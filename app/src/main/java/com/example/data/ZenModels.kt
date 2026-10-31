package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val accentThemeId: String,
    val sortOrder: Int,
    val isPrivate: Boolean = false
)

@Entity(tableName = "tabs")
data class TabEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val url: String,
    val isPinned: Boolean = false,
    val isEssential: Boolean = false,
    val isBookmarked: Boolean = false,
    val isUnloaded: Boolean = false,
    val isPrivate: Boolean = false,
    val sortOrder: Int = 0,
    val lastAccessed: Long = System.currentTimeMillis()
)

@Entity(tableName = "zen_mods")
data class ZenModEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean = false,
    val cssPayload: String = ""
)

@Entity(tableName = "firefox_extensions")
data class FirefoxExtensionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val author: String,
    val version: String,
    val description: String,
    val category: String,
    val permissions: String,
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = false,
    val allowInPrivate: Boolean = true,
    val runAt: String = "document_idle",
    val matchPatterns: String = "*://*/*",
    val amoSlug: String = "",
    val cssPayload: String = "",
    val jsPayload: String = ""
)

@Entity(tableName = "custom_search_engines")
data class CustomSearchEngineEntity(
    @PrimaryKey val id: String,
    val name: String,
    val searchUrlTemplate: String,
    val homeDomain: String
)

@Entity(tableName = "home_shortcuts")
data class HomeShortcutEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val sortOrder: Int = 0
)

@Entity(tableName = "history_entries")
data class HistoryEntryEntity(
    @PrimaryKey val id: String,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "download_items")
data class DownloadItemEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val url: String,
    val mimeType: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val totalBytes: Long = 0L,
    val status: String = "completed",
    val downloadedBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = 0L,
    val totalSegments: Int = 501,
    val completedSegments: Int = 0,
    val isResumable: Boolean = true,
    val isWifiOnly: Boolean = false,
    val speedLimitKbps: Long = 0L,
    val sourceDomain: String = "",
    val avgSpeedMbps: Float = 4.4f,
    val peakSpeedMbps: Float = 15.3f
)

data class DetectedMediaStream(
    val id: String = java.util.UUID.randomUUID().toString(),
    val resolution: String,
    val isBest: Boolean = false,
    val format: String = "HLS",
    val bitrate: String = "5.4 Mbps",
    val duration: String = "41m 43s",
    val segmentsCount: Int = 501,
    val estimatedSize: String = "1.6 GB",
    val url: String = "",
    val mimeType: String = "video/mp4"
)

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val domain: String,
    val url: String,
    val username: String,
    val password: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class SidebarPosition {
    LEFT, RIGHT
}

enum class SidebarWidthMode(val label: String, val widthDp: Int) {
    COMPACT("Compact", 240),
    BALANCED("Standard", 280),
    EXPANDED("Wide", 320)
}

enum class UrlBarPosition(val label: String) {
    BOTTOM("Bottom Toolbar"),
    TOP("Top Toolbar"),
    SIDEBAR_ONLY("Single Toolbar (Sidebar Only)")
}

enum class SingleToolbarNewTabBehavior(val label: String, val description: String) {
    CENTER_TOP_POPUP(
        "Center-Top Search Pop-Up",
        "Opens a floating search bar at the center-top with keyboard activated"
    ),
    FOCUS_SIDEBAR_SEARCH(
        "Focus Sidebar Search Bar",
        "Activates the search bar directly inside the side panel like a normal click"
    )
}

enum class HomeScreenStyle(val label: String, val description: String) {
    NORMAL("Normal", "Minimal search box and editable shortcuts grid"),
    ZEN("Zen", "Blank canvas with optional custom text positioned anywhere")
}

enum class SidebarColorMode(val label: String, val description: String) {
    NORMAL("Normal (Pitch Black / Graphite)", "Uses standard dark obsidian & pitch black surface"),
    CUSTOM("Custom Color", "Applies your selected color tint to the side panel")
}

enum class BackSwipeSidebarMode(val label: String, val description: String) {
    LEFT_SIDEBAR_RIGHT_BACK(
        "Left Edge: Open Side Panel • Right Edge: Go Back",
        "Swiping back from the left opens the side panel; swiping back from the right navigates back"
    ),
    SINGLE_SWIPE_SIDEBAR(
        "Single Swipe Opens Side Panel",
        "Swiping back from either edge opens the side panel immediately in one swipe"
    ),
    GO_BACK_THEN_SIDEBAR(
        "Go Back Until Start Page, Then Side Panel",
        "Navigates back in web history first; opens the side panel when at the start page"
    )
}

enum class HistoryDeletionInterval(val label: String, val intervalMillis: Long) {
    NEVER("Never (Keep Forever)", -1L),
    ONE_HOUR("Every 1 Hour", 3600_000L),
    TWENTY_FOUR_HOURS("Every 24 Hours", 86400_000L),
    SEVEN_DAYS("Every 7 Days", 7 * 86400_000L),
    ON_APP_CLOSE("On Browser Exit", 0L)
}

enum class SidebarToolItem(val id: String, val label: String) {
    NAV_CONTROLS("nav_controls", "Back & Forward Controls"),
    SEARCH_BAR("search_bar", "Sidebar Search / Address Bar"),
    NEW_TAB_BUTTON("new_tab_button", "+ New Tab Button"),
    MEDIA_BAR("media_bar", "Media Playback Bar"),
    SPLIT_VIEW("split_view", "Split View Button"),
    FIREFOX_EXTENSIONS("firefox_extensions", "Firefox Add-ons Button"),
    MEDIA_CAPTURE("media_capture", "Media Capture & Sniffer"),
    DOWNLOAD_MANAGER("download_manager", "Advance Download Manager (ADM)"),
    READER_MODE("reader_mode", "Reader Mode Button"),
    PRIVACY_SHIELD("privacy_shield", "Privacy Shield Button"),
    COMPACT_MODE("compact_mode", "Compact Mode Button"),
    CLEAR_TABS("clear_tabs", "Clear Unpinned Tabs Button"),
    LIBRARY("library", "Bookmarks, History & Downloads"),
    DESKTOP_MODE("desktop_mode", "Desktop Site Toggle"),
    SETTINGS_BUTTON("settings_button", "Customize / Settings Button")
}

enum class TopToolbarToolItem(val id: String, val label: String) {
    BACK("back", "Back Navigation"),
    FORWARD("forward", "Forward Navigation"),
    RELOAD("reload", "Reload Page"),
    HOME("home", "Home Button"),
    AT_A_GLANCE("at_a_glance", "At a Glance (Peek Mode)"),
    TRANSLATE("translate", "Translate Page"),
    READER_MODE("reader_mode", "Reader View"),
    EXTENSIONS("extensions", "Firefox Add-ons & Extensions"),
    PRIVACY_SHIELD("privacy_shield", "Tracking Protection Shield"),
    DESKTOP_MODE("desktop_mode", "Desktop Site Toggle"),
    EDIT_LAYOUT("edit_layout", "Edit Layout Button")
}

enum class SearchEngine(val id: String, val label: String, val searchUrlTemplate: String, val homeDomain: String) {
    DUCKDUCKGO("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q=%s", "duckduckgo.com"),
    GOOGLE("google", "Google", "https://www.google.com/search?q=%s", "google.com"),
    BRAVE("brave", "Brave Search", "https://search.brave.com/search?q=%s", "search.brave.com"),
    STARTPAGE("startpage", "Startpage", "https://www.startpage.com/do/dsearch?query=%s", "startpage.com"),
    BING("bing", "Bing", "https://www.bing.com/search?q=%s", "bing.com"),
    ECOSIA("ecosia", "Ecosia", "https://www.ecosia.org/search?q=%s", "ecosia.org"),
    QWANT("qwant", "Qwant", "https://www.qwant.com/?q=%s", "qwant.com"),
    WIKIPEDIA("wikipedia", "Wikipedia", "https://en.wikipedia.org/w/index.php?search=%s", "wikipedia.org")
}

enum class TabSwipeAction(val label: String) {
    UNLOAD("Unload Tab"),
    CLOSE("Close Tab"),
    PIN("Pin / Unpin Tab"),
    BOOKMARK("Bookmark Tab"),
    ESSENTIAL("Add to Essentials"),
    NONE("Disabled")
}

enum class NewTabSwipeGestureAction(val label: String) {
    OPEN_PRIVATE_TAB("Open Private Tab"),
    OPEN_PRIVATE_WORKSPACE("Open Private Workspace"),
    OPEN_NORMAL_TAB("Open Normal Tab"),
    NONE("Disabled")
}

enum class NewTabPosition(val id: String, val label: String) {
    BOTTOM("bottom", "Bottom (Default)"),
    TOP("top", "Top")
}

enum class HorizontalSwipeNavigationMode(val id: String, val label: String, val description: String) {
    SWIPE_RIGHT_PANEL_LEFT_BACK(
        "right_panel_left_back",
        "Swipe Right: Side Panel • Swipe Left: Go Back (Default)",
        "Swiping right opens the side panel; swiping left navigates back in history"
    ),
    SWIPE_LEFT_PANEL_RIGHT_BACK(
        "left_panel_right_back",
        "Swipe Left: Side Panel • Swipe Right: Go Back",
        "Swiping left opens the side panel; swiping right navigates back in history"
    )
}

data class HistoryNavEntry(
    val title: String,
    val url: String,
    val stepsBack: Int
)

enum class StartupBehavior(val label: String, val description: String) {
    UNLOAD_AND_HOMEPAGE(
        "Unload All Tabs & Open Homepage",
        "Unloads background tabs into memory-saving state and starts on the Zen Start Page"
    ),
    RESUME_WHERE_LEFT_OFF(
        "Resume Where You Left Off",
        "Restores your last active workspace and focused tab immediately"
    ),
    HOMEPAGE_AFTER_HOURS(
        "Start at Homepage After Inactivity",
        "Resumes session unless the browser was closed longer than the configured hours"
    )
}

enum class StartPageGradientPreset(
    val id: String,
    val label: String,
    val topHex: Long,
    val midHex: Long,
    val bottomHex: Long
) {
    PITCH_BLACK_AMOLED("pitch_black", "Pitch Black AMOLED", 0xFF000000, 0xFF000000, 0xFF000000),
    OBSIDIAN_MONO("obsidian_mono", "Obsidian Monochrome", 0xFF141417, 0xFF0A0A0C, 0xFF000000),
    GRAPHITE_MIST("graphite_mist", "Graphite Mist", 0xFF27272A, 0xFF141416, 0xFF050506),
    SILVER_FOG("silver_fog", "Silver Fog", 0xFF3F3F46, 0xFF1C1C20, 0xFF08080A),
    MIDNIGHT_SLATE("midnight_slate", "Midnight Slate", 0xFF1E293B, 0xFF0F172A, 0xFF030508),
    DEEP_FOREST("deep_forest", "Deep Charcoal Pine", 0xFF14241F, 0xFF0D1714, 0xFF030504),
    WARM_ESPRESSO("warm_espresso", "Warm Espresso", 0xFF261C18, 0xFF16110F, 0xFF050403),
    COSMIC_ECLIPSE("cosmic_eclipse", "Cosmic Eclipse", 0xFF221A2E, 0xFF130F1C, 0xFF040306);

    companion object {
        fun fromId(id: String): StartPageGradientPreset =
            entries.find { it.id == id } ?: PITCH_BLACK_AMOLED
    }
}

enum class SidebarSectionType(val id: String, val label: String) {
    ESSENTIALS("essentials", "Essentials"),
    PINNED("pinned", "Pinned Tabs"),
    OPEN_TABS("open_tabs", "Tabs"),
    QUICK_TOOLS("quick_tools", "Split View & Bottom Tools"),
    WORKSPACES("workspaces", "Workspaces")
}

data class MediaPlaybackState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val isMuted: Boolean = false,
    val title: String = "",
    val domain: String = "",
    val tabId: String = "",
    val artworkUrl: String = "",
    val artwork: android.graphics.Bitmap? = null
)

data class SidebarConfig(
    val position: SidebarPosition = SidebarPosition.LEFT,
    val widthMode: SidebarWidthMode = SidebarWidthMode.BALANCED,
    val urlBarPosition: UrlBarPosition = UrlBarPosition.SIDEBAR_ONLY,
    val singleToolbarNewTabBehavior: SingleToolbarNewTabBehavior = SingleToolbarNewTabBehavior.CENTER_TOP_POPUP,
    val hideStatusBarFullscreen: Boolean = false,
    val hasCompletedTutorial: Boolean = false,
    val compactModeAutoHide: Boolean = false,
    val showEssentialGrid: Boolean = true,
    val showPinnedTabs: Boolean = true,
    val showWorkspaceStripInBottomBar: Boolean = false,
    val showTabFaviconBadges: Boolean = true,
    // Side Panel Navigation Controls & Back Swipe Gesture Mode (Go back in web history first, then open side panel)
    val showSidebarNavControls: Boolean = true,
    val backSwipeSidebarMode: BackSwipeSidebarMode = BackSwipeSidebarMode.GO_BACK_THEN_SIDEBAR,
    val horizontalSwipeMode: HorizontalSwipeNavigationMode = HorizontalSwipeNavigationMode.SWIPE_RIGHT_PANEL_LEFT_BACK,
    val openInNewTab: Boolean = false,
    val openHistoryInNewTab: Boolean = true,
    val switchToNewTabImmediately: Boolean = false,
    val newTabPosition: NewTabPosition = NewTabPosition.BOTTOM,
    val showTabCloseNotification: Boolean = true,
    val showActionNotifications: Boolean = true,
    val customAiOverviewText: String = "",
    val fakeSearchQueryTrigger: String = "? : who is the main character",
    val fakeSearchHeadline: String = "",
    val fakeSearchSnippet: String = "",
    val backgroundPlayEnabled: Boolean = true,
    val playInWindowPipEnabled: Boolean = true,
    // Side Panel UI Size Scale (0.75f = 75% Compact to 1.35f = 135% Large, Default 1.0f = 100%)
    val sidebarUiScale: Float = 1.0f,
    // Side Panel Blur (Default 50% = 0.5f), Opacity (Default 85% = 0.85f), Liquid Glass mode, and Side Panel Color
    val glassBlurIntensity: Float = 0.50f,
    val sidebarOpacity: Float = 0.85f,
    val liquidGlassEnabled: Boolean = false,
    val sidebarColorMode: SidebarColorMode = SidebarColorMode.NORMAL,
    val sidebarCustomColorHex: Long = 0xFF182232,
    val borderGlowEnabled: Boolean = false,
    // Tab swipe actions (Default: right -> UNLOAD, left -> CLOSE)
    val tabSwipeRightAction: TabSwipeAction = TabSwipeAction.UNLOAD,
    val tabSwipeLeftAction: TabSwipeAction = TabSwipeAction.CLOSE,
    // "+ New Tab" button swipe actions (Default: left -> Private Tab, right -> Private Workspace)
    val newTabSwipeLeftAction: NewTabSwipeGestureAction = NewTabSwipeGestureAction.OPEN_PRIVATE_TAB,
    val newTabSwipeRightAction: NewTabSwipeGestureAction = NewTabSwipeGestureAction.OPEN_PRIVATE_WORKSPACE,
    // Session & Lifecycle settings
    val closePrivateOnAppClose: Boolean = true,
    val startupBehavior: StartupBehavior = StartupBehavior.RESUME_WHERE_LEFT_OFF,
    val homepageAfterHoursThreshold: Int = 4,
    val lastSessionTimestamp: Long = System.currentTimeMillis(),
    // Start Page / Homescreen Customization
    val homeScreenStyle: HomeScreenStyle = HomeScreenStyle.NORMAL,
    val zenCustomText: String = "",
    val zenCustomTextColorHex: Long = 0xFFF4F4F6,
    val zenCustomTextXFraction: Float = 0.5f,
    val zenCustomTextYFraction: Float = 0.45f,
    val startPageGradientId: String = StartPageGradientPreset.PITCH_BLACK_AMOLED.id,
    val startPageImageUri: String? = null,
    val startPageBlurEnabled: Boolean = false,
    val startPageBlurRadius: Float = 14f,
    val startPageBrightness: Float = 1.0f,
    // Compact Mode & Edge Bar Customization
    val edgeBarWidthDp: Float = 6f,
    val edgeBarLengthDp: Float = 96f,
    val edgeBarVerticalOffsetFraction: Float = 0.5f,
    val hideEdgeBarInCompactMode: Boolean = false,
    val hideEdgeBarAlways: Boolean = false,
    val openLinksInGlanceAutomatically: Boolean = false,
    val sectionOrder: List<SidebarSectionType> = listOf(
        SidebarSectionType.ESSENTIALS,
        SidebarSectionType.PINNED,
        SidebarSectionType.OPEN_TABS,
        SidebarSectionType.QUICK_TOOLS,
        SidebarSectionType.WORKSPACES
    ),
    val enabledSidebarTools: List<SidebarToolItem> = SidebarToolItem.entries.toList(),
    val enabledTopToolbarTools: List<TopToolbarToolItem> = listOf(
        TopToolbarToolItem.BACK,
        TopToolbarToolItem.FORWARD,
        TopToolbarToolItem.RELOAD,
        TopToolbarToolItem.HOME,
        TopToolbarToolItem.AT_A_GLANCE,
        TopToolbarToolItem.TRANSLATE,
        TopToolbarToolItem.EDIT_LAYOUT
    )
)

enum class AdBlockerLevel(val id: String, val label: String, val description: String) {
    OFF("off", "Off", "No ad blocking; all ads and trackers allowed"),
    NORMAL("normal", "Normal", "Standard ad and tracker domain blocking (recommended)"),
    HIGH("high", "High", "Aggressive blocking including third-party scripts, ads, and cookies");

    companion object {
        fun fromId(id: String): AdBlockerLevel =
            entries.find { it.id.equals(id, ignoreCase = true) } ?: NORMAL
    }
}

data class PrivacyConfig(
    val blockTrackersAndAds: Boolean = true,
    val adBlockerLevel: AdBlockerLevel = AdBlockerLevel.HIGH,
    val httpsEverywhere: Boolean = true,
    val stripTrackingParams: Boolean = true,
    val fingerprintProtection: Boolean = true,
    val blockThirdPartyCookies: Boolean = true,
    val normalSearchEngineId: String = SearchEngine.DUCKDUCKGO.id,
    val privateSearchEngineId: String = SearchEngine.DUCKDUCKGO.id,
    val historyDeletionInterval: HistoryDeletionInterval = HistoryDeletionInterval.NEVER,
    val totalTrackersBlocked: Int = 0,
    val totalParamsStripped: Int = 0,
    val totalHttpsUpgrades: Int = 0
)

data class BlockedTrackerEvent(
    val domain: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "offline_pages")
data class OfflinePageEntity(
    @PrimaryKey val id: String,
    val title: String,
    val url: String,
    val htmlContent: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sizeBytes: Long = 0L
)

enum class SuggestionType {
    SEARCH_QUERY,
    HISTORY,
    BOOKMARK
}

data class SearchSuggestionItem(
    val title: String,
    val subtitle: String? = null,
    val target: String,
    val type: SuggestionType = SuggestionType.SEARCH_QUERY
)
