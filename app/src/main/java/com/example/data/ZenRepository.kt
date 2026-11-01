package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

private val Context.dataStore by preferencesDataStore(name = "zen_browser_settings")

class ZenRepository(
    private val zenDao: ZenDao,
    private val context: Context? = null
) {
    val workspaces: Flow<List<WorkspaceEntity>> = zenDao.getAllWorkspaces()
    val tabs: Flow<List<TabEntity>> = zenDao.getAllTabs()
    val mods: Flow<List<ZenModEntity>> = zenDao.getAllMods()
    val firefoxExtensions: Flow<List<FirefoxExtensionEntity>> = zenDao.getAllFirefoxExtensions()
    val customSearchEngines: Flow<List<CustomSearchEngineEntity>> = zenDao.getAllCustomSearchEngines()
    val homeShortcuts: Flow<List<HomeShortcutEntity>> = zenDao.getAllHomeShortcuts()
    val history: Flow<List<HistoryEntryEntity>> = zenDao.getAllHistory()
    val downloads: Flow<List<DownloadItemEntity>> = zenDao.getAllDownloads()
    val offlinePages: Flow<List<OfflinePageEntity>> = zenDao.getAllOfflinePages()
    // Passwords are stored encrypted (Android Keystore AES-GCM) and decrypted only at this boundary.
    val passwords: Flow<List<PasswordEntity>> = zenDao.getAllPasswords()
        .onStart { migratePlaintextPasswords() }
        .map { list -> list.map { it.copy(password = ZenCrypto.decrypt(it.password)) } }

    private object Keys {
        val SIDEBAR_POSITION = stringPreferencesKey("sidebar_position")
        val SIDEBAR_WIDTH = stringPreferencesKey("sidebar_width")
        val URL_BAR_POSITION = stringPreferencesKey("url_bar_position")
        val SINGLE_TOOLBAR_NEW_TAB_BEHAVIOR = stringPreferencesKey("single_toolbar_new_tab_behavior")
        val HIDE_STATUS_BAR_FULLSCREEN = booleanPreferencesKey("hide_status_bar_fullscreen")
        val HAS_COMPLETED_TUTORIAL = booleanPreferencesKey("has_completed_tutorial_v1")
        val COMPACT_MODE = booleanPreferencesKey("compact_mode")
        val SHOW_ESSENTIALS = booleanPreferencesKey("show_essentials")
        val SHOW_PINNED = booleanPreferencesKey("show_pinned")
        val SHOW_WORKSPACE_STRIP = booleanPreferencesKey("show_workspace_strip")
        val SHOW_FAVICONS = booleanPreferencesKey("show_favicons")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity_v3")
        val SIDEBAR_OPACITY = floatPreferencesKey("sidebar_opacity")
        val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val SIDEBAR_COLOR_MODE = stringPreferencesKey("sidebar_color_mode")
        val SIDEBAR_CUSTOM_COLOR_HEX = longPreferencesKey("sidebar_custom_color_hex")
        val BORDER_GLOW = booleanPreferencesKey("border_glow")
        val SECTION_ORDER = stringPreferencesKey("section_order")
        val ENABLED_SIDEBAR_TOOLS = stringPreferencesKey("enabled_sidebar_tools")
        val ENABLED_TOP_TOOLBAR_TOOLS = stringPreferencesKey("enabled_top_toolbar_tools")
        val SHOW_SIDEBAR_NAV_CONTROLS = booleanPreferencesKey("show_sidebar_nav_controls")
        val BACK_SWIPE_SIDEBAR_MODE = stringPreferencesKey("back_swipe_sidebar_mode")
        val OPEN_IN_NEW_TAB = booleanPreferencesKey("open_in_new_tab")
        val OPEN_HISTORY_IN_NEW_TAB = booleanPreferencesKey("open_history_in_new_tab")
        val SWITCH_TO_NEW_TAB_IMMEDIATELY = booleanPreferencesKey("switch_to_new_tab_immediately")
        val SHOW_TAB_CLOSE_NOTIFICATION = booleanPreferencesKey("show_tab_close_notification")
        val SHOW_ACTION_NOTIFICATIONS = booleanPreferencesKey("show_action_notifications")
        val CUSTOM_AI_OVERVIEW_TEXT = stringPreferencesKey("custom_ai_overview_text")
        val FAKE_SEARCH_QUERY_TRIGGER = stringPreferencesKey("fake_search_query_trigger")
        val FAKE_SEARCH_HEADLINE = stringPreferencesKey("fake_search_headline")
        val FAKE_SEARCH_SNIPPET = stringPreferencesKey("fake_search_snippet")
        val BACKGROUND_PLAY_ENABLED = booleanPreferencesKey("background_play_enabled")
        val DATA_SAVER_ENABLED = booleanPreferencesKey("data_saver_enabled")
        val PLAY_IN_WINDOW_PIP_ENABLED = booleanPreferencesKey("play_in_window_pip_enabled")
        val SIDEBAR_UI_SCALE = floatPreferencesKey("sidebar_ui_scale")

        val TAB_SWIPE_RIGHT = stringPreferencesKey("tab_swipe_right")
        val TAB_SWIPE_LEFT = stringPreferencesKey("tab_swipe_left")
        val NEW_TAB_SWIPE_LEFT = stringPreferencesKey("new_tab_swipe_left")
        val NEW_TAB_SWIPE_RIGHT = stringPreferencesKey("new_tab_swipe_right")
        val CLOSE_PRIVATE_ON_CLOSE = booleanPreferencesKey("close_private_on_close")
        val STARTUP_BEHAVIOR = stringPreferencesKey("startup_behavior_v2")
        val HOMEPAGE_HOURS_THRESHOLD = intPreferencesKey("homepage_hours_threshold")
        val LAST_SESSION_TIMESTAMP = longPreferencesKey("last_session_timestamp")

        val HOME_SCREEN_STYLE = stringPreferencesKey("home_screen_style")
        val ZEN_CUSTOM_TEXT = stringPreferencesKey("zen_custom_text")
        val ZEN_CUSTOM_TEXT_COLOR = longPreferencesKey("zen_custom_text_color")
        val ZEN_CUSTOM_TEXT_X = floatPreferencesKey("zen_custom_text_x")
        val ZEN_CUSTOM_TEXT_Y = floatPreferencesKey("zen_custom_text_y")
        val START_PAGE_GRADIENT = stringPreferencesKey("start_page_gradient")
        val START_PAGE_IMAGE_URI = stringPreferencesKey("start_page_image_uri")
        val START_PAGE_BLUR_ENABLED = booleanPreferencesKey("start_page_blur_enabled")
        val START_PAGE_BLUR_RADIUS = floatPreferencesKey("start_page_blur_radius")
        val START_PAGE_BRIGHTNESS = floatPreferencesKey("start_page_brightness")

        val EDGE_BAR_WIDTH = floatPreferencesKey("edge_bar_width")
        val EDGE_BAR_LENGTH = floatPreferencesKey("edge_bar_length")
        val EDGE_BAR_OFFSET = floatPreferencesKey("edge_bar_offset")
        val HIDE_EDGE_BAR_COMPACT = booleanPreferencesKey("hide_edge_bar_compact")
        val HIDE_EDGE_BAR_ALWAYS = booleanPreferencesKey("hide_edge_bar_always")

        val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
        val AD_BLOCKER_LEVEL = stringPreferencesKey("ad_blocker_level")
        val HTTPS_EVERYWHERE = booleanPreferencesKey("https_everywhere")
        val STRIP_PARAMS = booleanPreferencesKey("strip_params")
        val FINGERPRINT_PROTECTION = booleanPreferencesKey("fingerprint_protection")
        val BLOCK_COOKIES = booleanPreferencesKey("block_cookies")
        val NORMAL_SEARCH_ENGINE = stringPreferencesKey("normal_search_engine_id")
        val PRIVATE_SEARCH_ENGINE = stringPreferencesKey("private_search_engine_id")
        val TOTAL_TRACKERS_BLOCKED = intPreferencesKey("total_trackers_blocked_v2")
        val TOTAL_PARAMS_STRIPPED = intPreferencesKey("total_params_stripped_v2")
        val TOTAL_HTTPS_UPGRADES = intPreferencesKey("total_https_upgrades_v2")
        val OPEN_LINKS_IN_GLANCE_AUTO = booleanPreferencesKey("open_links_in_glance_auto")
        val HISTORY_DELETION_INTERVAL = stringPreferencesKey("history_deletion_interval")
        val LAST_ACTIVE_WORKSPACE_ID = stringPreferencesKey("last_active_workspace_id")
        val LAST_ACTIVE_TAB_ID = stringPreferencesKey("last_active_tab_id")
    }

    val sidebarConfigFlow: Flow<SidebarConfig>? = context?.dataStore?.data?.map { prefs ->
        val orderString = prefs[Keys.SECTION_ORDER]
        val parsedOrder = if (!orderString.isNullOrBlank()) {
            val list = orderString.split(",").mapNotNull { id ->
                SidebarSectionType.entries.find { it.id == id }
            }
            list.takeIf { it.isNotEmpty() }
        } else null

        val toolsString = prefs[Keys.ENABLED_SIDEBAR_TOOLS]
        val parsedTools = if (toolsString != null) {
            if (toolsString.isBlank()) {
                emptyList()
            } else {
                toolsString.split(",").mapNotNull { id ->
                    SidebarToolItem.entries.find { it.id == id }
                }
            }
        } else null

        val topToolsString = prefs[Keys.ENABLED_TOP_TOOLBAR_TOOLS]
        val parsedTopTools = if (topToolsString != null) {
            if (topToolsString.isBlank()) {
                emptyList()
            } else {
                topToolsString.split(",").mapNotNull { id ->
                    TopToolbarToolItem.entries.find { it.id == id }
                }
            }
        } else null

        SidebarConfig(
            position = prefs[Keys.SIDEBAR_POSITION]?.let {
                runCatching { SidebarPosition.valueOf(it) }.getOrNull()
            } ?: SidebarPosition.LEFT,
            widthMode = prefs[Keys.SIDEBAR_WIDTH]?.let {
                runCatching { SidebarWidthMode.valueOf(it) }.getOrNull()
            } ?: SidebarWidthMode.BALANCED,
            urlBarPosition = prefs[Keys.URL_BAR_POSITION]?.let {
                runCatching { UrlBarPosition.valueOf(it) }.getOrNull()
            } ?: UrlBarPosition.SIDEBAR_ONLY,
            singleToolbarNewTabBehavior = prefs[Keys.SINGLE_TOOLBAR_NEW_TAB_BEHAVIOR]?.let {
                runCatching { SingleToolbarNewTabBehavior.valueOf(it) }.getOrNull()
            } ?: SingleToolbarNewTabBehavior.CENTER_TOP_POPUP,
            hideStatusBarFullscreen = prefs[Keys.HIDE_STATUS_BAR_FULLSCREEN] ?: false,
            hasCompletedTutorial = prefs[Keys.HAS_COMPLETED_TUTORIAL] ?: false,
            compactModeAutoHide = prefs[Keys.COMPACT_MODE] ?: false,
            showEssentialGrid = prefs[Keys.SHOW_ESSENTIALS] ?: true,
            showPinnedTabs = prefs[Keys.SHOW_PINNED] ?: true,
            showWorkspaceStripInBottomBar = prefs[Keys.SHOW_WORKSPACE_STRIP] ?: false,
            showTabFaviconBadges = prefs[Keys.SHOW_FAVICONS] ?: true,
            showSidebarNavControls = prefs[Keys.SHOW_SIDEBAR_NAV_CONTROLS] ?: true,
            backSwipeSidebarMode = prefs[Keys.BACK_SWIPE_SIDEBAR_MODE]?.let {
                runCatching { BackSwipeSidebarMode.valueOf(it) }.getOrNull()
            } ?: BackSwipeSidebarMode.GO_BACK_THEN_SIDEBAR,
            sidebarUiScale = (prefs[Keys.SIDEBAR_UI_SCALE] ?: 1.0f).coerceIn(0.75f, 1.35f),
            glassBlurIntensity = prefs[Keys.GLASS_INTENSITY] ?: 0.50f,
            sidebarOpacity = prefs[Keys.SIDEBAR_OPACITY] ?: 0.85f,
            liquidGlassEnabled = prefs[Keys.LIQUID_GLASS_ENABLED] ?: false,
            sidebarColorMode = prefs[Keys.SIDEBAR_COLOR_MODE]?.let {
                runCatching { SidebarColorMode.valueOf(it) }.getOrNull()
            } ?: SidebarColorMode.NORMAL,
            sidebarCustomColorHex = prefs[Keys.SIDEBAR_CUSTOM_COLOR_HEX] ?: 0xFF182232L,
            borderGlowEnabled = prefs[Keys.BORDER_GLOW] ?: false,
            tabSwipeRightAction = prefs[Keys.TAB_SWIPE_RIGHT]?.let {
                runCatching { TabSwipeAction.valueOf(it) }.getOrNull()
            } ?: TabSwipeAction.UNLOAD,
            tabSwipeLeftAction = prefs[Keys.TAB_SWIPE_LEFT]?.let {
                runCatching { TabSwipeAction.valueOf(it) }.getOrNull()
            } ?: TabSwipeAction.CLOSE,
            newTabSwipeLeftAction = prefs[Keys.NEW_TAB_SWIPE_LEFT]?.let {
                runCatching { NewTabSwipeGestureAction.valueOf(it) }.getOrNull()
            } ?: NewTabSwipeGestureAction.OPEN_PRIVATE_TAB,
            newTabSwipeRightAction = prefs[Keys.NEW_TAB_SWIPE_RIGHT]?.let {
                runCatching { NewTabSwipeGestureAction.valueOf(it) }.getOrNull()
            } ?: NewTabSwipeGestureAction.OPEN_PRIVATE_WORKSPACE,
            closePrivateOnAppClose = prefs[Keys.CLOSE_PRIVATE_ON_CLOSE] ?: true,
            startupBehavior = prefs[Keys.STARTUP_BEHAVIOR]?.let {
                runCatching { StartupBehavior.valueOf(it) }.getOrNull()
            } ?: StartupBehavior.RESUME_WHERE_LEFT_OFF,
            homepageAfterHoursThreshold = prefs[Keys.HOMEPAGE_HOURS_THRESHOLD] ?: 4,
            lastSessionTimestamp = prefs[Keys.LAST_SESSION_TIMESTAMP] ?: System.currentTimeMillis(),
            homeScreenStyle = prefs[Keys.HOME_SCREEN_STYLE]?.let {
                runCatching { HomeScreenStyle.valueOf(it) }.getOrNull()
            } ?: HomeScreenStyle.NORMAL,
            zenCustomText = prefs[Keys.ZEN_CUSTOM_TEXT] ?: "",
            zenCustomTextColorHex = prefs[Keys.ZEN_CUSTOM_TEXT_COLOR] ?: 0xFFF4F4F6,
            zenCustomTextXFraction = prefs[Keys.ZEN_CUSTOM_TEXT_X] ?: 0.5f,
            zenCustomTextYFraction = prefs[Keys.ZEN_CUSTOM_TEXT_Y] ?: 0.45f,
            startPageGradientId = prefs[Keys.START_PAGE_GRADIENT] ?: StartPageGradientPreset.PITCH_BLACK_AMOLED.id,
            startPageImageUri = prefs[Keys.START_PAGE_IMAGE_URI]?.takeIf { it.isNotBlank() },
            startPageBlurEnabled = prefs[Keys.START_PAGE_BLUR_ENABLED] ?: false,
            startPageBlurRadius = prefs[Keys.START_PAGE_BLUR_RADIUS] ?: 14f,
            startPageBrightness = prefs[Keys.START_PAGE_BRIGHTNESS] ?: 1.0f,
            edgeBarWidthDp = prefs[Keys.EDGE_BAR_WIDTH] ?: 6f,
            edgeBarLengthDp = prefs[Keys.EDGE_BAR_LENGTH] ?: 96f,
            edgeBarVerticalOffsetFraction = prefs[Keys.EDGE_BAR_OFFSET] ?: 0.5f,
            hideEdgeBarInCompactMode = prefs[Keys.HIDE_EDGE_BAR_COMPACT] ?: false,
            hideEdgeBarAlways = prefs[Keys.HIDE_EDGE_BAR_ALWAYS] ?: false,
            openLinksInGlanceAutomatically = prefs[Keys.OPEN_LINKS_IN_GLANCE_AUTO] ?: false,
            openInNewTab = prefs[Keys.OPEN_IN_NEW_TAB] ?: false,
            openHistoryInNewTab = prefs[Keys.OPEN_HISTORY_IN_NEW_TAB] ?: true,
            switchToNewTabImmediately = prefs[Keys.SWITCH_TO_NEW_TAB_IMMEDIATELY] ?: false,
            showTabCloseNotification = prefs[Keys.SHOW_TAB_CLOSE_NOTIFICATION] ?: true,
            showActionNotifications = prefs[Keys.SHOW_ACTION_NOTIFICATIONS] ?: true,
            customAiOverviewText = prefs[Keys.CUSTOM_AI_OVERVIEW_TEXT] ?: "",
            fakeSearchQueryTrigger = prefs[Keys.FAKE_SEARCH_QUERY_TRIGGER] ?: "? : who is the main character",
            fakeSearchHeadline = prefs[Keys.FAKE_SEARCH_HEADLINE] ?: "",
            fakeSearchSnippet = prefs[Keys.FAKE_SEARCH_SNIPPET] ?: "",
            backgroundPlayEnabled = prefs[Keys.BACKGROUND_PLAY_ENABLED] ?: true,
            dataSaverEnabled = prefs[Keys.DATA_SAVER_ENABLED] ?: false,
            playInWindowPipEnabled = prefs[Keys.PLAY_IN_WINDOW_PIP_ENABLED] ?: true,
            sectionOrder = parsedOrder ?: SidebarConfig().sectionOrder,
            enabledSidebarTools = parsedTools ?: SidebarConfig().enabledSidebarTools,
            enabledTopToolbarTools = parsedTopTools ?: SidebarConfig().enabledTopToolbarTools
        )
    }

    val privacyConfigFlow: Flow<PrivacyConfig>? = context?.dataStore?.data?.map { prefs ->
        PrivacyConfig(
            blockTrackersAndAds = prefs[Keys.BLOCK_TRACKERS] ?: true,
            adBlockerLevel = prefs[Keys.AD_BLOCKER_LEVEL]?.let {
                runCatching { AdBlockerLevel.valueOf(it) }.getOrNull()
            } ?: AdBlockerLevel.NORMAL,
            httpsEverywhere = prefs[Keys.HTTPS_EVERYWHERE] ?: true,
            stripTrackingParams = prefs[Keys.STRIP_PARAMS] ?: true,
            fingerprintProtection = prefs[Keys.FINGERPRINT_PROTECTION] ?: true,
            blockThirdPartyCookies = prefs[Keys.BLOCK_COOKIES] ?: true,
            normalSearchEngineId = prefs[Keys.NORMAL_SEARCH_ENGINE] ?: SearchEngine.DUCKDUCKGO.id,
            privateSearchEngineId = prefs[Keys.PRIVATE_SEARCH_ENGINE] ?: SearchEngine.DUCKDUCKGO.id,
            historyDeletionInterval = prefs[Keys.HISTORY_DELETION_INTERVAL]?.let {
                runCatching { HistoryDeletionInterval.valueOf(it) }.getOrNull()
            } ?: HistoryDeletionInterval.NEVER,
            totalTrackersBlocked = prefs[Keys.TOTAL_TRACKERS_BLOCKED] ?: 0,
            totalParamsStripped = prefs[Keys.TOTAL_PARAMS_STRIPPED] ?: 0,
            totalHttpsUpgrades = prefs[Keys.TOTAL_HTTPS_UPGRADES] ?: 0
        )
    }

    suspend fun saveSidebarConfig(config: SidebarConfig) {
        context?.dataStore?.edit { prefs ->
            prefs[Keys.SIDEBAR_POSITION] = config.position.name
            prefs[Keys.SIDEBAR_WIDTH] = config.widthMode.name
            prefs[Keys.URL_BAR_POSITION] = config.urlBarPosition.name
            prefs[Keys.SINGLE_TOOLBAR_NEW_TAB_BEHAVIOR] = config.singleToolbarNewTabBehavior.name
            prefs[Keys.HIDE_STATUS_BAR_FULLSCREEN] = config.hideStatusBarFullscreen
            prefs[Keys.HAS_COMPLETED_TUTORIAL] = config.hasCompletedTutorial
            prefs[Keys.COMPACT_MODE] = config.compactModeAutoHide
            prefs[Keys.SHOW_ESSENTIALS] = config.showEssentialGrid
            prefs[Keys.SHOW_PINNED] = config.showPinnedTabs
            prefs[Keys.SHOW_WORKSPACE_STRIP] = config.showWorkspaceStripInBottomBar
            prefs[Keys.SHOW_FAVICONS] = config.showTabFaviconBadges
            prefs[Keys.SHOW_SIDEBAR_NAV_CONTROLS] = config.showSidebarNavControls
            prefs[Keys.BACK_SWIPE_SIDEBAR_MODE] = config.backSwipeSidebarMode.name
            prefs[Keys.SIDEBAR_UI_SCALE] = config.sidebarUiScale
            prefs[Keys.GLASS_INTENSITY] = config.glassBlurIntensity
            prefs[Keys.SIDEBAR_OPACITY] = config.sidebarOpacity
            prefs[Keys.LIQUID_GLASS_ENABLED] = config.liquidGlassEnabled
            prefs[Keys.SIDEBAR_COLOR_MODE] = config.sidebarColorMode.name
            prefs[Keys.SIDEBAR_CUSTOM_COLOR_HEX] = config.sidebarCustomColorHex
            prefs[Keys.BORDER_GLOW] = config.borderGlowEnabled
            prefs[Keys.TAB_SWIPE_RIGHT] = config.tabSwipeRightAction.name
            prefs[Keys.TAB_SWIPE_LEFT] = config.tabSwipeLeftAction.name
            prefs[Keys.NEW_TAB_SWIPE_LEFT] = config.newTabSwipeLeftAction.name
            prefs[Keys.NEW_TAB_SWIPE_RIGHT] = config.newTabSwipeRightAction.name
            prefs[Keys.CLOSE_PRIVATE_ON_CLOSE] = config.closePrivateOnAppClose
            prefs[Keys.STARTUP_BEHAVIOR] = config.startupBehavior.name
            prefs[Keys.HOMEPAGE_HOURS_THRESHOLD] = config.homepageAfterHoursThreshold
            prefs[Keys.LAST_SESSION_TIMESTAMP] = config.lastSessionTimestamp
            prefs[Keys.HOME_SCREEN_STYLE] = config.homeScreenStyle.name
            prefs[Keys.ZEN_CUSTOM_TEXT] = config.zenCustomText
            prefs[Keys.ZEN_CUSTOM_TEXT_COLOR] = config.zenCustomTextColorHex
            prefs[Keys.ZEN_CUSTOM_TEXT_X] = config.zenCustomTextXFraction
            prefs[Keys.ZEN_CUSTOM_TEXT_Y] = config.zenCustomTextYFraction
            prefs[Keys.START_PAGE_GRADIENT] = config.startPageGradientId
            prefs[Keys.START_PAGE_IMAGE_URI] = config.startPageImageUri ?: ""
            prefs[Keys.START_PAGE_BLUR_ENABLED] = config.startPageBlurEnabled
            prefs[Keys.START_PAGE_BLUR_RADIUS] = config.startPageBlurRadius
            prefs[Keys.START_PAGE_BRIGHTNESS] = config.startPageBrightness
            prefs[Keys.EDGE_BAR_WIDTH] = config.edgeBarWidthDp
            prefs[Keys.EDGE_BAR_LENGTH] = config.edgeBarLengthDp
            prefs[Keys.EDGE_BAR_OFFSET] = config.edgeBarVerticalOffsetFraction
            prefs[Keys.HIDE_EDGE_BAR_COMPACT] = config.hideEdgeBarInCompactMode
            prefs[Keys.HIDE_EDGE_BAR_ALWAYS] = config.hideEdgeBarAlways
            prefs[Keys.OPEN_LINKS_IN_GLANCE_AUTO] = config.openLinksInGlanceAutomatically
            prefs[Keys.OPEN_IN_NEW_TAB] = config.openInNewTab
            prefs[Keys.OPEN_HISTORY_IN_NEW_TAB] = config.openHistoryInNewTab
            prefs[Keys.SWITCH_TO_NEW_TAB_IMMEDIATELY] = config.switchToNewTabImmediately
            prefs[Keys.SHOW_TAB_CLOSE_NOTIFICATION] = config.showTabCloseNotification
            prefs[Keys.SHOW_ACTION_NOTIFICATIONS] = config.showActionNotifications
            prefs[Keys.CUSTOM_AI_OVERVIEW_TEXT] = config.customAiOverviewText
            prefs[Keys.FAKE_SEARCH_QUERY_TRIGGER] = config.fakeSearchQueryTrigger
            prefs[Keys.FAKE_SEARCH_HEADLINE] = config.fakeSearchHeadline
            prefs[Keys.FAKE_SEARCH_SNIPPET] = config.fakeSearchSnippet
            prefs[Keys.BACKGROUND_PLAY_ENABLED] = config.backgroundPlayEnabled
            prefs[Keys.DATA_SAVER_ENABLED] = config.dataSaverEnabled
            prefs[Keys.PLAY_IN_WINDOW_PIP_ENABLED] = config.playInWindowPipEnabled
            prefs[Keys.SECTION_ORDER] = config.sectionOrder.joinToString(",") { it.id }
            prefs[Keys.ENABLED_SIDEBAR_TOOLS] = config.enabledSidebarTools.joinToString(",") { it.id }
            prefs[Keys.ENABLED_TOP_TOOLBAR_TOOLS] = config.enabledTopToolbarTools.joinToString(",") { it.id }
        }
    }

    suspend fun savePrivacyConfig(config: PrivacyConfig) {
        context?.dataStore?.edit { prefs ->
            prefs[Keys.BLOCK_TRACKERS] = config.blockTrackersAndAds
            prefs[Keys.AD_BLOCKER_LEVEL] = config.adBlockerLevel.name
            prefs[Keys.HTTPS_EVERYWHERE] = config.httpsEverywhere
            prefs[Keys.STRIP_PARAMS] = config.stripTrackingParams
            prefs[Keys.FINGERPRINT_PROTECTION] = config.fingerprintProtection
            prefs[Keys.BLOCK_COOKIES] = config.blockThirdPartyCookies
            prefs[Keys.NORMAL_SEARCH_ENGINE] = config.normalSearchEngineId
            prefs[Keys.PRIVATE_SEARCH_ENGINE] = config.privateSearchEngineId
            prefs[Keys.HISTORY_DELETION_INTERVAL] = config.historyDeletionInterval.name
            prefs[Keys.TOTAL_TRACKERS_BLOCKED] = config.totalTrackersBlocked
            prefs[Keys.TOTAL_PARAMS_STRIPPED] = config.totalParamsStripped
            prefs[Keys.TOTAL_HTTPS_UPGRADES] = config.totalHttpsUpgrades
        }
    }

    suspend fun pruneHistoryOlderThan(cutoffTimestamp: Long) {
        zenDao.deleteHistoryOlderThan(cutoffTimestamp)
    }

    suspend fun saveLastSessionState(workspaceId: String, tabId: String) {
        context?.dataStore?.edit { prefs ->
            if (workspaceId.isNotBlank()) prefs[Keys.LAST_ACTIVE_WORKSPACE_ID] = workspaceId
            if (tabId.isNotBlank()) prefs[Keys.LAST_ACTIVE_TAB_ID] = tabId
        }
    }

    suspend fun getLastSessionState(): Pair<String?, String?> {
        val prefs = context?.dataStore?.data?.first() ?: return Pair(null, null)
        return Pair(prefs[Keys.LAST_ACTIVE_WORKSPACE_ID], prefs[Keys.LAST_ACTIVE_TAB_ID])
    }

    suspend fun seedDefaultsIfEmpty() {
        val currentWorkspaces = zenDao.getAllWorkspaces().first()
        if (currentWorkspaces.isEmpty()) {
            val defaultWorkspaces = listOf(
                WorkspaceEntity(
                    id = "ws_default",
                    name = "Default",
                    iconKey = "spa",
                    accentThemeId = "carbon",
                    sortOrder = 0,
                    isPrivate = false
                )
            )
            zenDao.insertWorkspaces(defaultWorkspaces)

            // By default, Essentials space has nothing; only start with a clean homepage tab
            val defaultTabs = listOf(
                TabEntity(
                    id = "tab_open_1",
                    workspaceId = "ws_default",
                    title = "New Tab",
                    url = "zen://home",
                    isPinned = false,
                    isEssential = false,
                    sortOrder = 0,
                    lastAccessed = System.currentTimeMillis()
                )
            )
            zenDao.insertTabs(defaultTabs)
        }

        val currentShortcuts = zenDao.getAllHomeShortcuts().first()
        if (currentShortcuts.isEmpty()) {
            val defaultShortcuts = listOf(
                HomeShortcutEntity("sc_1", "DuckDuckGo", "https://duckduckgo.com", 0),
                HomeShortcutEntity("sc_2", "Wikipedia", "https://en.m.wikipedia.org", 1),
                HomeShortcutEntity("sc_3", "Hacker News", "https://news.ycombinator.com", 2),
                HomeShortcutEntity("sc_4", "MDN", "https://developer.mozilla.org", 3),
                HomeShortcutEntity("sc_5", "GitHub", "https://github.com/zen-browser/desktop", 4),
                HomeShortcutEntity("sc_6", "EFF", "https://www.eff.org", 5),
                HomeShortcutEntity("sc_7", "arXiv", "https://arxiv.org", 6),
                HomeShortcutEntity("sc_8", "Startpage", "https://www.startpage.com", 7)
            )
            zenDao.insertHomeShortcuts(defaultShortcuts)
        }

        val currentMods = zenDao.getAllMods().first()
        if (currentMods.isEmpty()) {
            val defaultMods = listOf(
                ZenModEntity(
                    id = "mod_superpins",
                    title = "Compact Essentials Grid",
                    author = "zen-core",
                    description = "Renders essential tabs in a compact monochrome icon grid at the top of the vertical sidebar.",
                    category = "Sidebar",
                    isEnabled = true,
                    cssPayload = ""
                ),
                ZenModEntity(
                    id = "mod_amoled_web",
                    title = "Monochrome Dark Pages",
                    author = "zen-core",
                    description = "Applies a dark monochrome stylesheet to web pages.",
                    category = "Appearance",
                    isEnabled = false,
                    cssPayload = "html, body { background-color: #000000 !important; color: #e4e4e7 !important; } a { color: #d4d4d8 !important; }"
                ),
                ZenModEntity(
                    id = "mod_zero_annoyances",
                    title = "Hide Cookie Notices",
                    author = "privacy",
                    description = "Hides common cookie banners and consent overlays on websites.",
                    category = "Privacy",
                    isEnabled = false,
                    cssPayload = "#onetrust-consent-sdk, #CybotCookiebotDialog, .cc-window { display: none !important; }"
                ),
                ZenModEntity(
                    id = "mod_serif_typography",
                    title = "Clean Article Spacing",
                    author = "typesetter",
                    description = "Adjusts line-height for easier reading on article pages.",
                    category = "Appearance",
                    isEnabled = false,
                    cssPayload = "article, p { line-height: 1.7 !important; }"
                )
            )
            zenDao.insertMods(defaultMods)
        } else {
            // Migrate existing mod_zero_annoyances if it had broad [class*='cookie'] / [class*='consent'] rules that hid page content
            currentMods.find { it.id == "mod_zero_annoyances" && it.cssPayload.contains("[class*='cookie']") }?.let { mod ->
                zenDao.updateMod(
                    mod.copy(
                        isEnabled = false,
                        cssPayload = "#onetrust-consent-sdk, #CybotCookiebotDialog, .cc-window { display: none !important; }"
                    )
                )
            }
        }

        val uBlockCssPayload = """
            .adsbygoogle, ins.adsbygoogle, [id^="div-gpt-ad"], [id^="google_ads_iframe"],
            iframe[src*="doubleclick.net"], iframe[src*="googlesyndication.com"],
            iframe[src*="adnxs.com"], iframe[src*="rubiconproject.com"],
            iframe[src*="criteo."], iframe[src*="taboola."], iframe[src*="outbrain."],
            iframe[src*="amazon-adsystem.com"], iframe[src*="moatads.com"],
            iframe[id*="google_ads"], iframe[name*="google_ads"],
            .taboola-widget, #taboola-below-article-thumbnails, .OUTBRAIN, [data-ob-mark],
            .ytp-ad-overlay-container, .ytp-ad-message-container, .ytp-ad-image-overlay,
            .ytp-ad-text-overlay, .ytp-ad-player-overlay, .ytp-ad-action-interstitial,
            .ytp-ad-module, .ytp-ad-overlay-slot, .ytp-ad-player-overlay-flyout-cta,
            .sparkles-light-cta, .ytp-ad-badge,
            ytd-ad-slot-renderer, ytm-promoted-sparkles-web-renderer, ytm-promoted-video-renderer,
            ytd-in-feed-ad-layout-renderer, ytd-banner-promo-renderer, ytd-display-ad-renderer,
            ytd-statement-banner-renderer, ytd-CompanionSlotRenderer, ytm-companion-ad-renderer,
            #masthead-ad, #player-ads, .video-ads,
            [data-ad-slot], [data-ad-client], [data-ad-unit], [data-google-query-id],
            .ad-slot, .ad-banner, .ad-container, .ad-wrapper, .advertisement-banner,
            .sponsored-content, .dfp-ad, .gpt-ad, amp-ad, amp-embed[type="taboola"],
            [class*="ad-banner" i], [class*="ad-container" i], [class*="sponsored-post" i],
            [class*="promoted-item" i], [data-ad], [aria-label*="advertisement" i],
            [aria-label*="sponsored" i], [id^="ad_"], [class^="ad_"] {
                display: none !important;
                height: 0 !important;
                min-height: 0 !important;
                max-height: 0 !important;
                overflow: hidden !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }
        """.trimIndent()

        val uBlockJsPayload = """
            (function() {
                if (window.__zenUblockActive) return;
                window.__zenUblockActive = true;
                var host = location.hostname || 'page';
                if (localStorage.getItem('__zen_ublock_whitelist_' + host) === '1') return;

                // Apply per-site custom element picker rules saved by the user
                try {
                    var customRules = JSON.parse(localStorage.getItem('__zen_ublock_custom_' + host) || '[]');
                    if (customRules.length > 0) {
                        var st = document.createElement('style');
                        st.id = '__zen_ublock_custom_style';
                        st.textContent = customRules.join(', ') + ' { display: none !important; }';
                        (document.head || document.documentElement || document.body).appendChild(st);
                    }
                } catch(e) {}

                // Block popup ad windows and ad-redirect window.open calls
                var origOpen = window.open;
                window.open = function(url) {
                    var str = String(url || '').toLowerCase();
                    if (str.indexOf('doubleclick') !== -1 || str.indexOf('adservice') !== -1 || str.indexOf('popads') !== -1 || str.indexOf('onclickads') !== -1 || str.indexOf('propellerads') !== -1 || str.indexOf('exoclick') !== -1 || str.indexOf('googlesyndication') !== -1 || str.indexOf('adnxs') !== -1 || str.indexOf('taboola') !== -1 || str.indexOf('outbrain') !== -1) {
                        return null;
                    }
                    return origOpen ? origOpen.apply(window, arguments) : null;
                };

                // Active DOM & YouTube Video Ad Pruner
                function sweepAds() {
                    try {
                        // Auto-skip and fast-forward any YouTube video pre-roll / mid-roll ad
                        var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .videoAdUiSkipButton');
                        if (skipBtn) { skipBtn.click(); }
                        var adShowing = document.querySelector('.ad-showing, .ad-interrupting');
                        if (adShowing) {
                            var v = document.querySelector('video');
                            if (v && !isNaN(v.duration) && v.duration > 0 && v.duration < 180) {
                                v.currentTime = v.duration;
                            }
                        }
                        // Remove ad iframes & empty ad slots
                        document.querySelectorAll('ins.adsbygoogle, [id^="div-gpt-ad"], [id^="google_ads_iframe"], ytd-ad-slot-renderer, ytm-promoted-sparkles-web-renderer, ytm-companion-ad-renderer, amp-ad, [class*="ad-banner" i], [class*="ad-container" i], [data-ad]').forEach(function(el) {
                            el.style.setProperty('display', 'none', 'important');
                        });
                    } catch(err) {}
                }
                sweepAds();
                setInterval(sweepAds, 400);
                try {
                    var observer = new MutationObserver(function() { sweepAds(); });
                    observer.observe(document.documentElement || document.body, { childList: true, subtree: true });
                } catch(obsErr) {}

                // Interactive Element Picker ("Zap / Block Element on Page") for uBlock Origin
                window.__zenStartUblockElementPicker = function() {
                    if (document.getElementById('__zen_ublock_picker_banner')) return;
                    var banner = document.createElement('div');
                    banner.id = '__zen_ublock_picker_banner';
                    banner.style.cssText = 'position:fixed;bottom:16px;left:16px;right:16px;z-index:2147483647;background:#141418;color:#f4f4f6;border:1px solid #ef4444;border-radius:12px;padding:12px 14px;font-family:system-ui,sans-serif;font-size:12px;display:flex;justify-content:space-between;align-items:center;box-shadow:0 10px 28px rgba(0,0,0,0.85);';
                    banner.innerHTML = '<span>🛡️ <b>uBlock Element Picker:</b> Tap any annoying element on the page to block it</span><button id="__zen_ublock_picker_cancel" style="background:#27272a;color:#f4f4f6;border:1px solid #3f3f46;border-radius:6px;padding:4px 10px;font-size:11px;cursor:pointer;">Done</button>';
                    document.documentElement.appendChild(banner);

                    function onPickClick(e) {
                        if (banner.contains(e.target)) return;
                        e.preventDefault();
                        e.stopPropagation();
                        var el = e.target;
                        if (el && el !== document.body && el !== document.documentElement) {
                            var selector = el.tagName.toLowerCase();
                            if (el.id) {
                                selector += '#' + CSS.escape(el.id);
                            } else if (el.className && typeof el.className === 'string') {
                                var cls = el.className.trim().split(/\s+/).filter(Boolean).slice(0, 2);
                                if (cls.length) selector += '.' + cls.map(function(c){ return CSS.escape(c); }).join('.');
                            }
                            el.style.setProperty('display', 'none', 'important');
                            try {
                                var list = JSON.parse(localStorage.getItem('__zen_ublock_custom_' + host) || '[]');
                                if (list.indexOf(selector) === -1) list.push(selector);
                                localStorage.setItem('__zen_ublock_custom_' + host, JSON.stringify(list));
                            } catch(err) {}
                        }
                        cleanup();
                    }
                    function cleanup() {
                        document.removeEventListener('click', onPickClick, true);
                        if (banner.parentNode) banner.parentNode.removeChild(banner);
                    }
                    banner.querySelector('#__zen_ublock_picker_cancel').addEventListener('click', function(ev) {
                        ev.stopPropagation();
                        cleanup();
                    });
                    document.addEventListener('click', onPickClick, true);
                };
            })();
        """.trimIndent()

        val translatorCssPayload = """
            #__zen_translator_fab { display: none !important; }
            #__zen_translator_panel {
                position: fixed !important;
                top: 64px !important;
                right: 16px !important;
                width: 292px !important;
                max-width: calc(100vw - 32px) !important;
                z-index: 2147483647 !important;
                background: rgba(14, 14, 18, 0.97) !important;
                color: #f4f4f6 !important;
                border: 1px solid #3f3f46 !important;
                border-radius: 14px !important;
                padding: 14px !important;
                font-family: system-ui, -apple-system, sans-serif !important;
                font-size: 12px !important;
                box-shadow: 0 12px 32px rgba(0,0,0,0.8) !important;
                backdrop-filter: blur(14px) !important;
            }
        """.trimIndent()

        val translatorJsPayload = """
            (function() {
                var oldFab = document.getElementById('__zen_translator_fab');
                if (oldFab) { oldFab.remove(); }
                if (window.__zenTranslatorReady) return;
                window.__zenTranslatorReady = true;
                var langs = [
                    { code: 'en', name: 'English' },
                    { code: 'es', name: 'Spanish' },
                    { code: 'fr', name: 'French' },
                    { code: 'de', name: 'German' },
                    { code: 'ar', name: 'Arabic' },
                    { code: 'hi', name: 'Hindi' },
                    { code: 'zh-CN', name: 'Chinese (Simplified)' },
                    { code: 'ja', name: 'Japanese' },
                    { code: 'ko', name: 'Korean' },
                    { code: 'pt', name: 'Portuguese' },
                    { code: 'ru', name: 'Russian' },
                    { code: 'it', name: 'Italian' },
                    { code: 'tr', name: 'Turkish' }
                ];
                window.__zenOpenTranslator = function() {
                    var existing = document.getElementById('__zen_translator_panel');
                    if (existing) {
                        existing.style.display = (existing.style.display === 'none') ? 'block' : 'none';
                        return;
                    }
                    var savedLang = localStorage.getItem('__zen_target_lang') || 'en';
                    var panel = document.createElement('div');
                    panel.id = '__zen_translator_panel';
                    panel.style.display = 'block';
                    var optionsHtml = langs.map(function(l) {
                        return '<option value="' + l.code + '"' + (l.code === savedLang ? ' selected' : '') + '>' + l.name + '</option>';
                    }).join('');
                    panel.innerHTML =
                        '<div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">' +
                        '<strong style="font-size:13px;color:#f4f4f6;">🌐 Zen Inbuilt Translator</strong>' +
                        '<span id="__zen_tr_close" style="cursor:pointer;color:#a1a1aa;font-size:15px;padding:2px 6px;">✕</span>' +
                        '</div>' +
                        '<div style="margin-bottom:8px;color:#a1a1aa;font-size:11px;">Target Language:</div>' +
                        '<select id="__zen_tr_lang" style="width:100%;padding:6px 8px;border-radius:8px;background:#18181c;color:#f4f4f6;border:1px solid #3f3f46;margin-bottom:10px;font-size:12px;">' +
                        optionsHtml +
                        '</select>' +
                        '<div id="__zen_tr_result" style="display:none;padding:8px;border-radius:8px;background:#18181c;border:1px solid #27272a;color:#e4e4e7;margin-bottom:8px;max-height:120px;overflow-y:auto;font-size:11.5px;"></div>' +
                        '<div style="display:flex;gap:6px;">' +
                        '<button id="__zen_tr_sel_btn" style="flex:1;padding:7px 8px;border-radius:8px;border:1px solid #3f3f46;background:#27272a;color:#f4f4f6;font-weight:600;font-size:11px;cursor:pointer;">Translate Selection</button>' +
                        '<button id="__zen_tr_page_btn" style="flex:1;padding:7px 8px;border-radius:8px;border:none;background:#f4f4f6;color:#000000;font-weight:600;font-size:11px;cursor:pointer;">Translate Full Page</button>' +
                        '</div>';
                    document.documentElement.appendChild(panel);
                    panel.querySelector('#__zen_tr_close').addEventListener('click', function() {
                        panel.style.display = 'none';
                    });
                    var langSelect = panel.querySelector('#__zen_tr_lang');
                    langSelect.addEventListener('change', function() {
                        localStorage.setItem('__zen_target_lang', langSelect.value);
                    });
                    var resBox = panel.querySelector('#__zen_tr_result');
                    function translateSnippet(text, targetLang) {
                        resBox.style.display = 'block';
                        resBox.textContent = 'Translating...';
                        var url = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + encodeURIComponent(targetLang) + '&dt=t&q=' + encodeURIComponent(text.slice(0, 1200));
                        fetch(url).then(function(r) { return r.json(); }).then(function(data) {
                            if (data && data[0]) {
                                var out = data[0].map(function(part) { return part[0]; }).join('');
                                resBox.textContent = out;
                            } else {
                                resBox.textContent = 'Could not translate text.';
                            }
                        }).catch(function() {
                            window.location.href = 'https://translate.google.com/translate?sl=auto&tl=' + encodeURIComponent(targetLang) + '&u=' + encodeURIComponent(window.location.href);
                        });
                    }
                    panel.querySelector('#__zen_tr_sel_btn').addEventListener('click', function() {
                        var sel = (window.getSelection && window.getSelection().toString().trim()) || '';
                        if (!sel) {
                            var mainText = (document.querySelector('article, main, p') || document.body).innerText.trim().slice(0, 500);
                            translateSnippet(mainText, langSelect.value);
                        } else {
                            translateSnippet(sel, langSelect.value);
                        }
                    });
                    panel.querySelector('#__zen_tr_page_btn').addEventListener('click', function() {
                        var tl = langSelect.value || 'en';
                        var paragraphs = Array.prototype.slice.call(document.querySelectorAll('h1, h2, h3, p, li')).filter(function(el) {
                            return el.innerText && el.innerText.trim().length > 12 && !el.getAttribute('data-zen-translated');
                        }).slice(0, 28);
                        if (paragraphs.length > 0) {
                            resBox.style.display = 'block';
                            resBox.textContent = 'Translating page inline (' + paragraphs.length + ' blocks)...';
                            var combined = paragraphs.map(function(el) { return el.innerText.trim().replace(/\n+/g, ' '); }).join('\n|||\n');
                            var api = 'https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=' + encodeURIComponent(tl) + '&dt=t&q=' + encodeURIComponent(combined.slice(0, 3500));
                            fetch(api).then(function(r) { return r.json(); }).then(function(data) {
                                if (data && data[0]) {
                                    var fullTranslated = data[0].map(function(part) { return part[0]; }).join('');
                                    var parts = fullTranslated.split(/\s*\|\|\|\s*/);
                                    paragraphs.forEach(function(el, idx) {
                                        if (parts[idx]) {
                                            el.innerText = parts[idx];
                                            el.setAttribute('data-zen-translated', 'true');
                                        }
                                    });
                                    resBox.textContent = 'Page translated to ' + tl.toUpperCase() + '!';
                                }
                            }).catch(function() {
                                window.location.href = 'https://translate.google.com/translate?sl=auto&tl=' + encodeURIComponent(tl) + '&u=' + encodeURIComponent(window.location.href);
                            });
                        } else {
                            window.location.href = 'https://translate.google.com/translate?sl=auto&tl=' + encodeURIComponent(tl) + '&u=' + encodeURIComponent(window.location.href);
                        }
                    });
                };
            })();
        """.trimIndent()

        val currentExtensions = zenDao.getAllFirefoxExtensions().first()
        if (currentExtensions.isEmpty()) {
            val defaultFirefoxExtensions = listOf(
                FirefoxExtensionEntity(
                    id = "ff_ublock_origin",
                    name = "uBlock Origin",
                    author = "Raymond Hill (gorhill)",
                    version = "1.64.0",
                    description = "An efficient wide-spectrum content blocker enabled by default. Blocks ads, trackers, popups, and supports interactive Element Picker.",
                    category = "Privacy & Security",
                    permissions = "Access your data for all websites, Block content on any page, Privacy, Storage, WebNavigation",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_start",
                    matchPatterns = "*://*/*",
                    amoSlug = "ublock-origin",
                    cssPayload = uBlockCssPayload,
                    jsPayload = uBlockJsPayload
                ),
                FirefoxExtensionEntity(
                    id = "ff_privacy_badger",
                    name = "Privacy Badger",
                    author = "EFF (Electronic Frontier Foundation)",
                    version = "2024.7.17",
                    description = "Automatically learns to block invisible trackers and non-consensual tracking cookies as you browse.",
                    category = "Privacy & Security",
                    permissions = "Access your data for all websites, Block trackers, Privacy",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_start",
                    matchPatterns = "*://*/*",
                    amoSlug = "privacy-badger",
                    cssPayload = "",
                    jsPayload = "console.log('Zen: Privacy Badger active');"
                ),
                FirefoxExtensionEntity(
                    id = "ff_video_bg_play",
                    name = "Video Background Play Fix",
                    author = "JanH (Mozilla Recommended)",
                    version = "1.6.1",
                    description = "Prevents websites (like YouTube and Vimeo) from pausing videos when switching tabs or minimizing, and forces HTML5 inline + fullscreen playback.",
                    category = "Media & Video",
                    permissions = "Access your data on youtube.com, vimeo.com, and all websites",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_start",
                    matchPatterns = "*://*/*",
                    amoSlug = "video-background-play-fix",
                    cssPayload = "",
                    jsPayload = """
                        Object.defineProperty(document, 'hidden', { value: false, configurable: true });
                        Object.defineProperty(document, 'visibilityState', { value: 'visible', configurable: true });
                        window.addEventListener('visibilitychange', function(e) { e.stopImmediatePropagation(); }, true);
                        document.querySelectorAll('video').forEach(function(v) {
                            v.setAttribute('playsinline', '');
                            v.setAttribute('webkit-playsinline', '');
                        });
                    """.trimIndent()
                ),
                FirefoxExtensionEntity(
                    id = "ff_dark_reader",
                    name = "Dark Reader",
                    author = "Dark Reader Ltd",
                    version = "4.9.96",
                    description = "Applies a pitch-black AMOLED dark theme to bright websites while preserving video players and images.",
                    category = "Appearance",
                    permissions = "Access your data for all websites",
                    isInstalled = true,
                    isEnabled = false,
                    allowInPrivate = true,
                    runAt = "document_start",
                    matchPatterns = "*://*/*",
                    amoSlug = "darkreader",
                    cssPayload = "html, body { background-color: #050507 !important; color: #e4e4e7 !important; border-color: #27272a !important; } article, section, main, nav, header, footer, aside { background-color: transparent !important; color: inherit !important; } a { color: #93c5fd !important; } video, img, canvas, picture { filter: none !important; }",
                    jsPayload = ""
                ),
                FirefoxExtensionEntity(
                    id = "ff_sponsorblock",
                    name = "SponsorBlock for YouTube",
                    author = "Ajay Ramachandran",
                    version = "5.9.5",
                    description = "Automatically skips video ads, intros, and annoying overlays on video streaming sites.",
                    category = "Media & Video",
                    permissions = "Access your data on youtube.com and video sites",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_idle",
                    matchPatterns = "*://*.youtube.com/*, *://youtu.be/*",
                    amoSlug = "sponsorblock",
                    cssPayload = ".ytp-ad-overlay-container, ytd-InFeedAdLayoutRenderer, ytd-promoted-sparkles-web-renderer { display: none !important; }",
                    jsPayload = "if (!window.__zenSponsorBlockTimer) { window.__zenSponsorBlockTimer = setInterval(function(){ var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button'); if (skip) { skip.click(); } }, 1000); }"
                ),
                FirefoxExtensionEntity(
                    id = "ff_clearurls",
                    name = "ClearURLs",
                    author = "Kevin R.",
                    version = "1.27.3",
                    description = "Automatically removes tracking elements from links and cleans outbound redirect URLs on the page.",
                    category = "Privacy & Security",
                    permissions = "Access your data for all websites",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_idle",
                    matchPatterns = "*://*/*",
                    amoSlug = "clearurls",
                    cssPayload = "",
                    jsPayload = "document.querySelectorAll('a[href]').forEach(function(a){ try { var u = new URL(a.href); ['utm_source','utm_medium','utm_campaign','fbclid','gclid','msclkid'].forEach(function(p){ u.searchParams.delete(p); }); a.href = u.toString(); } catch(e){} });"
                ),
                FirefoxExtensionEntity(
                    id = "ff_enhancer_youtube",
                    name = "Enhancer for YouTube",
                    author = "Maxime RF",
                    version = "2.0.126",
                    description = "Adds theater-mode cinema framing, hides distracting Shorts shelves, and improves mobile video playback controls.",
                    category = "Media & Video",
                    permissions = "Access your data on youtube.com",
                    isInstalled = false,
                    isEnabled = false,
                    allowInPrivate = true,
                    runAt = "document_idle",
                    matchPatterns = "*://*.youtube.com/*",
                    amoSlug = "enhancer-for-youtube",
                    cssPayload = "ytm-reel-shelf-renderer, ytd-reel-shelf-renderer, ytd-rich-shelf-renderer[is-shorts] { display: none !important; } video { object-fit: contain !important; }",
                    jsPayload = ""
                ),
                FirefoxExtensionEntity(
                    id = "ff_violentmonkey",
                    name = "Violentmonkey Userscripts",
                    author = "Violentmonkey Team",
                    version = "2.21.0",
                    description = "Open-source userscript manager supporting Greasemonkey / Tampermonkey GM_* APIs and custom page automation.",
                    category = "Productivity",
                    permissions = "Access your data for all websites, Unlimited storage",
                    isInstalled = false,
                    isEnabled = false,
                    allowInPrivate = false,
                    runAt = "document_end",
                    matchPatterns = "*://*/*",
                    amoSlug = "violentmonkey",
                    cssPayload = "",
                    jsPayload = "window.GM_info = { scriptHandler: 'Violentmonkey-Zen', version: '2.21.0' };"
                ),
                FirefoxExtensionEntity(
                    id = "ff_return_dislike",
                    name = "Return YouTube Dislike",
                    author = "Dmitry Selivanov",
                    version = "3.0.0.17",
                    description = "Restores clean engagement metrics and declutters video player action bars.",
                    category = "Media & Video",
                    permissions = "Access your data on youtube.com",
                    isInstalled = false,
                    isEnabled = false,
                    allowInPrivate = true,
                    runAt = "document_idle",
                    matchPatterns = "*://*.youtube.com/*",
                    amoSlug = "return-youtube-dislikes",
                    cssPayload = "ytd-StatementBannerRenderer, ytd-clarification-renderer { display: none !important; }",
                    jsPayload = ""
                ),
                FirefoxExtensionEntity(
                    id = "ff_consent_o_matic",
                    name = "Consent-O-Matic",
                    author = "CAVI Aarhus University",
                    version = "1.1.3",
                    description = "Automatically dismisses GDPR and cookie consent pop-ups and restores page scrolling.",
                    category = "Privacy & Security",
                    permissions = "Access your data for all websites",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_end",
                    matchPatterns = "*://*/*",
                    amoSlug = "consent-o-matic",
                    cssPayload = "#onetrust-consent-sdk, #CybotCookiebotDialog, .fc-consent-root, .cc-window { display: none !important; }",
                    jsPayload = ""
                ),
                FirefoxExtensionEntity(
                    id = "ff_zen_translator",
                    name = "Zen Inbuilt Page Translator",
                    author = "zen-core",
                    version = "2.5.0",
                    description = "On-demand page & selection translator. Tap 'Open Translator' on this extension to translate the active page or highlighted text into 13+ languages.",
                    category = "Productivity",
                    permissions = "Access your data for all websites, Translate page text",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_idle",
                    matchPatterns = "*://*/*",
                    amoSlug = "zen-page-translator",
                    cssPayload = translatorCssPayload,
                    jsPayload = translatorJsPayload
                ),
                FirefoxExtensionEntity(
                    id = "ff_bitwarden",
                    name = "Bitwarden Password Manager",
                    author = "Bitwarden Inc.",
                    version = "2024.10.0",
                    description = "Highlights secure login fields and prevents third-party scripts from snooping on password inputs.",
                    category = "Privacy & Security",
                    permissions = "Access your data for all websites, Clipboard access",
                    isInstalled = false,
                    isEnabled = false,
                    allowInPrivate = false,
                    runAt = "document_idle",
                    matchPatterns = "*://*/*",
                    amoSlug = "bitwarden-password-manager",
                    cssPayload = "input[type='password']:focus { border-color: #a1a1aa !important; box-shadow: 0 0 0 1px #f4f4f6 !important; }",
                    jsPayload = "document.querySelectorAll('input[type=\"password\"]').forEach(function(inp){ inp.setAttribute('autocomplete', 'current-password'); });"
                )
            )
            zenDao.insertFirefoxExtensions(defaultFirefoxExtensions)
        } else {
            // Ensure uBlock Origin is installed, enabled by default, and upgraded to v1.64.0 with full cosmetic + element picker support
            val existingUblock = currentExtensions.find { it.id == "ff_ublock_origin" }
            if (existingUblock == null) {
                zenDao.insertFirefoxExtension(
                    FirefoxExtensionEntity(
                        id = "ff_ublock_origin",
                        name = "uBlock Origin",
                        author = "Raymond Hill (gorhill)",
                        version = "1.64.0",
                        description = "An efficient wide-spectrum content blocker enabled by default. Blocks ads, trackers, popups, and supports interactive Element Picker.",
                        category = "Privacy & Security",
                        permissions = "Access your data for all websites, Block content on any page, Privacy, Storage, WebNavigation",
                        isInstalled = true,
                        isEnabled = true,
                        allowInPrivate = true,
                        runAt = "document_start",
                        matchPatterns = "*://*/*",
                        amoSlug = "ublock-origin",
                        cssPayload = uBlockCssPayload,
                        jsPayload = uBlockJsPayload
                    )
                )
            } else if (!existingUblock.isInstalled || !existingUblock.isEnabled || existingUblock.version != "1.64.0" || existingUblock.cssPayload != uBlockCssPayload || existingUblock.matchPatterns != "*://*/*") {
                zenDao.updateFirefoxExtension(
                    existingUblock.copy(
                        version = "1.64.0",
                        description = "An efficient wide-spectrum content blocker enabled by default. Blocks ads, trackers, popups, and supports interactive Element Picker.",
                        isInstalled = true,
                        isEnabled = true,
                        allowInPrivate = true,
                        matchPatterns = "*://*/*",
                        cssPayload = uBlockCssPayload,
                        jsPayload = uBlockJsPayload
                    )
                )
            }

            val existingTranslator = currentExtensions.find { it.id == "ff_zen_translator" }
            if (existingTranslator == null) {
                zenDao.insertFirefoxExtension(
                    FirefoxExtensionEntity(
                        id = "ff_zen_translator",
                        name = "Zen Inbuilt Page Translator",
                        author = "zen-core",
                        version = "2.5.0",
                        description = "On-demand page & selection translator. Tap 'Open Translator' on this extension to translate the active page or highlighted text into 13+ languages.",
                        category = "Productivity",
                        permissions = "Access your data for all websites, Translate page text",
                        isInstalled = true,
                        isEnabled = true,
                        allowInPrivate = true,
                        runAt = "document_idle",
                        matchPatterns = "*://*/*",
                        amoSlug = "zen-page-translator",
                        cssPayload = translatorCssPayload,
                        jsPayload = translatorJsPayload
                    )
                )
            } else if (existingTranslator.cssPayload.contains("bottom: 18px") || existingTranslator.jsPayload.contains("__zen_translator_fab")) {
                zenDao.updateFirefoxExtension(
                    existingTranslator.copy(
                        version = "2.5.0",
                        description = "On-demand page & selection translator. Tap 'Open Translator' on this extension to translate the active page or highlighted text into 13+ languages.",
                        cssPayload = translatorCssPayload,
                        jsPayload = translatorJsPayload
                    )
                )
            }

            val existingBadger = currentExtensions.find { it.id == "ff_privacy_badger" }
            if (existingBadger == null) {
                zenDao.insertFirefoxExtension(
                    FirefoxExtensionEntity(
                        id = "ff_privacy_badger",
                        name = "Privacy Badger",
                        author = "EFF (Electronic Frontier Foundation)",
                        version = "2024.7.17",
                        description = "Automatically learns to block invisible trackers and non-consensual tracking cookies as you browse.",
                        category = "Privacy & Security",
                        permissions = "Access your data for all websites, Block trackers, Privacy",
                        isInstalled = true,
                        isEnabled = true,
                        allowInPrivate = true,
                        runAt = "document_start",
                        matchPatterns = "*://*/*",
                        amoSlug = "privacy-badger",
                        cssPayload = "",
                        jsPayload = "console.log('Zen: Privacy Badger active');"
                    )
                )
            }

            currentExtensions.find { it.id == "ff_sponsorblock" && it.jsPayload.contains("adVideo.currentTime") }?.let { sb ->
                zenDao.updateFirefoxExtension(
                    sb.copy(
                        cssPayload = ".ytp-ad-overlay-container, ytd-InFeedAdLayoutRenderer, ytd-promoted-sparkles-web-renderer { display: none !important; }",
                        jsPayload = "if (!window.__zenSponsorBlockTimer) { window.__zenSponsorBlockTimer = setInterval(function(){ var skip = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button'); if (skip) { skip.click(); } }, 1000); }"
                    )
                )
            }
            currentExtensions.find { it.id == "ff_consent_o_matic" && (it.cssPayload.contains("body { overflow: auto") || it.jsPayload.isNotBlank()) }?.let { cm ->
                zenDao.updateFirefoxExtension(
                    cm.copy(
                        cssPayload = "#onetrust-consent-sdk, #CybotCookiebotDialog, .fc-consent-root, .cc-window { display: none !important; }",
                        jsPayload = ""
                    )
                )
            }
        }
    }

    suspend fun insertWorkspace(workspace: WorkspaceEntity) = zenDao.insertWorkspace(workspace)
    suspend fun updateWorkspace(workspace: WorkspaceEntity) = zenDao.updateWorkspace(workspace)
    suspend fun deleteWorkspace(workspaceId: String) {
        zenDao.deleteTabsForWorkspace(workspaceId)
        zenDao.deleteWorkspace(workspaceId)
    }

    suspend fun deletePrivateTabsAndWorkspaces() {
        zenDao.deleteAllPrivateTabs()
        zenDao.deletePrivateWorkspaces()
    }

    suspend fun unloadAllActiveTabs() = zenDao.unloadAllActiveTabs()

    suspend fun insertTab(tab: TabEntity) = zenDao.insertTab(tab)
    suspend fun updateTab(tab: TabEntity) = zenDao.updateTab(tab)
    suspend fun updateTabs(tabs: List<TabEntity>) = zenDao.updateTabs(tabs)
    suspend fun deleteTab(tabId: String) = zenDao.deleteTabById(tabId)
    suspend fun clearUnpinnedTabs() = zenDao.clearAllUnpinnedTabs()
    suspend fun updateMod(mod: ZenModEntity) = zenDao.updateMod(mod)

    suspend fun insertFirefoxExtension(extension: FirefoxExtensionEntity) =
        zenDao.insertFirefoxExtension(extension)

    suspend fun updateFirefoxExtension(extension: FirefoxExtensionEntity) =
        zenDao.updateFirefoxExtension(extension)

    suspend fun deleteFirefoxExtension(extensionId: String) =
        zenDao.deleteFirefoxExtension(extensionId)

    suspend fun insertCustomSearchEngine(engine: CustomSearchEngineEntity) =
        zenDao.insertCustomSearchEngine(engine)

    suspend fun deleteCustomSearchEngine(engineId: String) =
        zenDao.deleteCustomSearchEngine(engineId)

    suspend fun insertHomeShortcut(shortcut: HomeShortcutEntity) =
        zenDao.insertHomeShortcut(shortcut)

    suspend fun updateHomeShortcut(shortcut: HomeShortcutEntity) =
        zenDao.updateHomeShortcut(shortcut)

    suspend fun deleteHomeShortcut(shortcutId: String) =
        zenDao.deleteHomeShortcut(shortcutId)

    // History and Downloads operations
    suspend fun insertHistoryEntry(entry: HistoryEntryEntity) =
        zenDao.insertHistoryEntry(entry)

    suspend fun deleteHistoryEntry(id: String) =
        zenDao.deleteHistoryEntry(id)

    suspend fun clearAllHistory() =
        zenDao.clearAllHistory()

    suspend fun insertDownloadItem(item: DownloadItemEntity) =
        zenDao.insertDownloadItem(item)

    suspend fun deleteDownloadItem(id: String) =
        zenDao.deleteDownloadItem(id)

    suspend fun clearAllDownloads() =
        zenDao.clearAllDownloads()

    // Offline Pages operations
    suspend fun insertOfflinePage(page: OfflinePageEntity) =
        zenDao.insertOfflinePage(page)

    suspend fun getOfflinePageById(id: String) =
        zenDao.getOfflinePageById(id)

    suspend fun getOfflinePageByUrl(url: String) =
        zenDao.getOfflinePageByUrl(url)

    suspend fun deleteOfflinePage(id: String) =
        zenDao.deleteOfflinePage(id)

    suspend fun clearAllOfflinePages() =
        zenDao.clearAllOfflinePages()

    // Password Manager operations
    suspend fun getPasswordsForDomain(domain: String) =
        zenDao.getPasswordsForDomain(domain).map { it.copy(password = ZenCrypto.decrypt(it.password)) }

    suspend fun insertPassword(password: PasswordEntity) {
        val encrypted = ZenCrypto.encrypt(password.password) ?: return
        zenDao.insertPassword(password.copy(password = encrypted, updatedAt = System.currentTimeMillis()))
    }

    private var passwordMigrationDone = false

    /** One-time: encrypts any legacy plaintext rows left over from earlier versions. */
    private suspend fun migratePlaintextPasswords() {
        if (passwordMigrationDone) return
        passwordMigrationDone = true
        zenDao.getAllPasswords().first()
            .filter { !ZenCrypto.isEncrypted(it.password) && it.password.isNotEmpty() }
            .forEach { row ->
                ZenCrypto.encrypt(row.password)?.let { zenDao.insertPassword(row.copy(password = it)) }
            }
    }

    suspend fun deletePassword(id: String) =
        zenDao.deletePassword(id)

    suspend fun clearAllPasswords() =
        zenDao.clearAllPasswords()
}
