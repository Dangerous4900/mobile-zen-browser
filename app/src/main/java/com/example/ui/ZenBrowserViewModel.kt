package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AdBlockerLevel
import com.example.data.BlockedTrackerEvent
import com.example.data.BackSwipeSidebarMode
import com.example.data.CustomSearchEngineEntity
import com.example.data.DownloadItemEntity
import com.example.data.FirefoxExtensionEntity
import com.example.data.HistoryDeletionInterval
import com.example.data.HistoryEntryEntity
import com.example.data.HistoryNavEntry
import com.example.data.OfflinePageEntity
import com.example.data.PasswordEntity
import com.example.data.HomeScreenStyle
import com.example.data.HomeShortcutEntity
import com.example.data.HorizontalSwipeNavigationMode
import com.example.data.MediaPlaybackState
import com.example.data.NewTabPosition
import com.example.data.NewTabSwipeGestureAction
import com.example.data.PrivacyConfig
import com.example.data.SidebarColorMode
import com.example.data.SidebarConfig
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
import com.example.data.ZenRepository
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.theme.ZenAccentTheme
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActiveModalSheet {
    NONE,
    SIDEBAR_STUDIO,
    SIDEBAR_LAYOUT_EDITOR,
    PRIVACY_SHIELD,
    ZEN_MODS,
    FIREFOX_EXTENSIONS,
    WORKSPACE_EDITOR,
    SPLIT_TAB_PICKER,
    ESSENTIALS_MANAGER,
    START_PAGE_CUSTOMIZER,
    SEARCH_ENGINE_SETTINGS,
    LIBRARY,
    PASSWORD_MANAGER
}

enum class FocusedPane {
    PRIMARY,
    SECONDARY
}

data class ZenBrowserUiState(
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val activeWorkspaceId: String = "ws_default",
    val allTabs: List<TabEntity> = emptyList(),
    val activeTabId: String = "tab_open_1",
    val secondarySplitTabId: String? = null,
    val isSplitViewEnabled: Boolean = false,
    val isSelectingSplitTabs: Boolean = false,
    val selectedSplitTabIds: List<String> = emptyList(),
    val splitRatio: Float = 0.5f,
    val focusedPane: FocusedPane = FocusedPane.PRIMARY,
    val isSidebarOpen: Boolean = false,
    val isCompactFullscreen: Boolean = false,
    val isReaderModeEnabled: Boolean = false,
    val isPrivateGlanceMode: Boolean = false,
    val activeGlanceUrl: String? = null,
    val isCenterTopSearchPopupVisible: Boolean = false,
    val requestSidebarSearchFocus: Boolean = false,
    val showTutorialDialog: Boolean = false,
    val mediaPlaybackState: MediaPlaybackState = MediaPlaybackState(),
    val homeShortcuts: List<HomeShortcutEntity> = emptyList(),
    val historyEntries: List<HistoryEntryEntity> = emptyList(),
    val downloadItems: List<DownloadItemEntity> = emptyList(),
    val offlinePages: List<OfflinePageEntity> = emptyList(),
    val savedPasswords: List<PasswordEntity> = emptyList(),
    val sidebarConfig: SidebarConfig = SidebarConfig(),
    val privacyConfig: PrivacyConfig = PrivacyConfig(),
    val customSearchEngines: List<CustomSearchEngineEntity> = emptyList(),
    val zenMods: List<ZenModEntity> = emptyList(),
    val firefoxExtensions: List<FirefoxExtensionEntity> = emptyList(),
    val recentBlockedEvents: List<BlockedTrackerEvent> = emptyList(),
    val activeSheet: ActiveModalSheet = ActiveModalSheet.NONE,
    val pageLoadingProgress: Int = 100,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val findInPageQuery: String = "",
    val isFindInPageVisible: Boolean = false,
    val isInPictureInPictureMode: Boolean = false,
    val statusBannerMessage: String? = null,
    val desktopModeTabIds: Set<String> = emptySet(),
    val searchSuggestions: List<com.example.data.SearchSuggestionItem> = emptyList()
) {
    val isFocusedTabDesktopMode: Boolean
        get() = desktopModeTabIds.contains(focusedTab?.id)

    val activeWorkspace: WorkspaceEntity?
        get() = workspaces.find { it.id == activeWorkspaceId } ?: workspaces.firstOrNull()

    val activeAccentTheme: ZenAccentTheme
        get() = ZenAccentTheme.fromId(activeWorkspace?.accentThemeId ?: "carbon")

    val essentialTabs: List<TabEntity>
        get() = allTabs.filter { it.isEssential }.sortedBy { it.sortOrder }

    val bookmarkedTabs: List<TabEntity>
        get() = allTabs.filter { it.isBookmarked }.sortedByDescending { it.lastAccessed }

    val currentWorkspacePinnedTabs: List<TabEntity>
        get() = allTabs.filter { it.workspaceId == activeWorkspaceId && it.isPinned && !it.isEssential }
            .sortedBy { it.sortOrder }

    val currentWorkspaceOpenTabs: List<TabEntity>
        get() = allTabs.filter { it.workspaceId == activeWorkspaceId && !it.isPinned && !it.isEssential }
            .sortedBy { it.sortOrder }

    val activePrimaryTab: TabEntity?
        get() = allTabs.find { it.id == activeTabId }
            ?: currentWorkspaceOpenTabs.firstOrNull()
            ?: currentWorkspacePinnedTabs.firstOrNull()
            ?: essentialTabs.firstOrNull()

    val activeSecondaryTab: TabEntity?
        get() = secondarySplitTabId?.let { id -> allTabs.find { it.id == id } }

    val focusedTab: TabEntity?
        get() = if (isSplitViewEnabled && focusedPane == FocusedPane.SECONDARY && activeSecondaryTab != null) {
            activeSecondaryTab
        } else {
            activePrimaryTab
        }

    val isCurrentContextPrivate: Boolean
        get() = (focusedTab?.isPrivate == true) ||
            (activeWorkspace?.isPrivate == true) ||
            isPrivateGlanceMode

    val activeSearchEngineLabel: String
        get() {
            val engineId = if (isCurrentContextPrivate) {
                privacyConfig.privateSearchEngineId
            } else {
                privacyConfig.normalSearchEngineId
            }
            return ZenPrivacyEngine.resolveSearchEngineLabel(engineId, customSearchEngines)
        }

    val activeStartPageGradient: StartPageGradientPreset
        get() = StartPageGradientPreset.fromId(sidebarConfig.startPageGradientId)

    val activeFirefoxExtensionsForFocusedTab: List<FirefoxExtensionEntity>
        get() {
            val url = focusedTab?.url ?: "zen://home"
            val privateCtx = isCurrentContextPrivate
            return firefoxExtensions.filter { ext ->
                ext.isInstalled &&
                    ext.isEnabled &&
                    (!privateCtx || ext.allowInPrivate) &&
                    matchesExtensionUrlPattern(url, ext.matchPatterns)
            }
        }

    fun getInjectedCssForTab(tab: TabEntity): String {
        val url = tab.url
        val isPriv = tab.isPrivate || (activeWorkspace?.isPrivate == true)
        val activeExts = firefoxExtensions.filter { ext ->
            ext.isInstalled &&
                ext.isEnabled &&
                (!isPriv || ext.allowInPrivate) &&
                matchesExtensionUrlPattern(url, ext.matchPatterns)
        }
        return buildList {
            zenMods.filter { it.isEnabled && it.cssPayload.isNotBlank() }
                .forEach { add(it.cssPayload) }
            activeExts.filter { it.cssPayload.isNotBlank() }
                .forEach { add(it.cssPayload) }
        }.joinToString("\n")
    }

    fun getInjectedJsForTab(tab: TabEntity): String {
        val url = tab.url
        val isPriv = tab.isPrivate || (activeWorkspace?.isPrivate == true)
        val activeExts = firefoxExtensions.filter { ext ->
            ext.isInstalled &&
                ext.isEnabled &&
                (!isPriv || ext.allowInPrivate) &&
                matchesExtensionUrlPattern(url, ext.matchPatterns)
        }
        return activeExts
            .filter { it.jsPayload.isNotBlank() }
            .joinToString("\n") { it.jsPayload }
    }

    val activeInjectedCss: String
        get() = buildList {
            zenMods.filter { it.isEnabled && it.cssPayload.isNotBlank() }
                .forEach { add(it.cssPayload) }
            activeFirefoxExtensionsForFocusedTab
                .filter { it.cssPayload.isNotBlank() }
                .forEach { add(it.cssPayload) }
        }.joinToString("\n")

    val activeInjectedJs: String
        get() = activeFirefoxExtensionsForFocusedTab
            .filter { it.jsPayload.isNotBlank() }
            .joinToString("\n") { it.jsPayload }

    fun matchesExtensionUrlPattern(url: String, patterns: String): Boolean {
        if (url.isBlank() || url == "about:blank" || url.startsWith("zen://")) return false
        val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme !in setOf("http", "https")) return false

        val candidates = patterns.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (candidates.isEmpty()) return true
        val host = uri.host?.lowercase() ?: return false
        val fullUrl = url.lowercase()
        val pathAndQuery = (uri.rawPath.orEmpty().ifEmpty { "/" } +
            (uri.rawQuery?.let { "?$it" } ?: "")).lowercase()

        fun globMatches(value: String, glob: String): Boolean {
            val regex = buildString {
                append('^')
                glob.forEach { ch ->
                    if (ch == '*') append(".*") else append(Regex.escape(ch.toString()))
                }
                append('$')
            }
            return runCatching { Regex(regex, RegexOption.IGNORE_CASE).matches(value) }.getOrDefault(false)
        }

        return candidates.any { raw ->
            val pattern = raw.lowercase()
            if (pattern == "*" || pattern == "<all_urls>" || pattern == "*://*/*") return@any true

            // Tampermonkey/Greasemonkey @include entries are URL globs, not WebExtension match patterns.
            if (!pattern.contains("://")) {
                return@any globMatches(fullUrl, pattern)
            }

            val schemePattern = pattern.substringBefore("://")
            if (schemePattern != "*" && schemePattern != scheme) return@any false
            val afterScheme = pattern.substringAfter("://")
            val patternHost = afterScheme.substringBefore('/').substringBefore(':')
            val patternPath = afterScheme.substringAfter('/', "/*").let { "/$it" }
            val hostMatches = when {
                patternHost == "*" -> true
                patternHost.startsWith("*.") -> {
                    val domain = patternHost.removePrefix("*.")
                    host == domain || host.endsWith(".$domain")
                }
                else -> host == patternHost
            }
            hostMatches && globMatches(pathAndQuery, patternPath)
        }
    }
}

private data class TransientSessionState(
    val activeWorkspaceId: String = "ws_default",
    val activeTabId: String = "tab_open_1",
    val secondarySplitTabId: String? = null,
    val isSplitViewEnabled: Boolean = false,
    val isSelectingSplitTabs: Boolean = false,
    val selectedSplitTabIds: List<String> = emptyList(),
    val splitRatio: Float = 0.5f,
    val focusedPane: FocusedPane = FocusedPane.PRIMARY,
    val isSidebarOpen: Boolean = false,
    val isCompactFullscreen: Boolean = false,
    val isReaderModeEnabled: Boolean = false,
    val isPrivateGlanceMode: Boolean = false,
    val activeGlanceUrl: String? = null,
    val isCenterTopSearchPopupVisible: Boolean = false,
    val requestSidebarSearchFocus: Boolean = false,
    val showTutorialDialog: Boolean = false,
    val mediaPlaybackState: MediaPlaybackState = MediaPlaybackState(),
    val recentBlockedEvents: List<BlockedTrackerEvent> = emptyList(),
    val activeSheet: ActiveModalSheet = ActiveModalSheet.NONE,
    val pageLoadingProgress: Int = 100,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val findInPageQuery: String = "",
    val isFindInPageVisible: Boolean = false,
    val isInPictureInPictureMode: Boolean = false,
    val statusBannerMessage: String? = null,
    val desktopModeTabIds: Set<String> = emptySet(),
    val searchSuggestions: List<com.example.data.SearchSuggestionItem> = emptyList()
)

class ZenBrowserViewModel(
    private val repository: ZenRepository
) : ViewModel() {

    private val sessionState = MutableStateFlow(TransientSessionState())
    private val localSidebarConfig = MutableStateFlow(SidebarConfig())
    private val localPrivacyConfig = MutableStateFlow(PrivacyConfig())

    init {
        viewModelScope.launch {
            repository.seedDefaultsIfEmpty()
            applyStartupLifecycleRules()
            applyHistoryDeletionPolicy()
        }
        repository.sidebarConfigFlow?.let { flow ->
            viewModelScope.launch {
                flow.collect { config ->
                    localSidebarConfig.value = config
                }
            }
        }
        repository.privacyConfigFlow?.let { flow ->
            viewModelScope.launch {
                flow.collect { config ->
                    localPrivacyConfig.value = config
                }
            }
        }
    }

    val uiState: StateFlow<ZenBrowserUiState> = combine(
        repository.workspaces,
        repository.tabs,
        combine(
            combine(repository.mods, repository.firefoxExtensions, repository.history) { mods, extensions, hist ->
                Triple(mods, extensions, hist)
            },
            combine(repository.downloads, repository.offlinePages, repository.passwords) { dls, offline, pass ->
                Triple(dls, offline, pass)
            }
        ) { (mods, extensions, hist), (dls, offline, pass) ->
            Sextuple(mods, extensions, hist, dls, offline, pass)
        },
        localSidebarConfig,
        combine(
            localPrivacyConfig,
            repository.customSearchEngines,
            repository.homeShortcuts,
            sessionState
        ) { priv, customEngines, shortcuts, sess ->
            tuple4(priv, customEngines, shortcuts, sess)
        }
    ) { workspaces, tabs, (mods, extensions, hist, dls, offline, pass), sidebarCfg, (privacyCfg, customEngines, shortcuts, session) ->
        val resolvedWorkspaceId = if (workspaces.any { it.id == session.activeWorkspaceId }) {
            session.activeWorkspaceId
        } else {
            workspaces.firstOrNull { !it.isPrivate }?.id
                ?: workspaces.firstOrNull()?.id
                ?: "ws_default"
        }

        val resolvedPrimaryTabId = if (tabs.any { it.id == session.activeTabId }) {
            session.activeTabId
        } else {
            tabs.firstOrNull { it.workspaceId == resolvedWorkspaceId && !it.isEssential }?.id
                ?: tabs.firstOrNull()?.id
                ?: ""
        }

        ZenBrowserUiState(
            workspaces = workspaces,
            activeWorkspaceId = resolvedWorkspaceId,
            allTabs = tabs,
            activeTabId = resolvedPrimaryTabId,
            secondarySplitTabId = session.secondarySplitTabId?.takeIf { id -> tabs.any { it.id == id } },
            isSplitViewEnabled = session.isSplitViewEnabled && session.secondarySplitTabId != null,
            isSelectingSplitTabs = session.isSelectingSplitTabs,
            selectedSplitTabIds = session.selectedSplitTabIds.filter { id -> tabs.any { it.id == id } },
            splitRatio = session.splitRatio,
            focusedPane = session.focusedPane,
            isSidebarOpen = session.isSidebarOpen,
            isCompactFullscreen = session.isCompactFullscreen,
            isReaderModeEnabled = session.isReaderModeEnabled,
            isPrivateGlanceMode = session.isPrivateGlanceMode,
            activeGlanceUrl = session.activeGlanceUrl,
            isCenterTopSearchPopupVisible = session.isCenterTopSearchPopupVisible,
            requestSidebarSearchFocus = session.requestSidebarSearchFocus,
            showTutorialDialog = session.showTutorialDialog,
            mediaPlaybackState = session.mediaPlaybackState,
            homeShortcuts = shortcuts,
            historyEntries = hist,
            downloadItems = dls,
            offlinePages = offline,
            savedPasswords = pass,
            sidebarConfig = sidebarCfg,
            privacyConfig = privacyCfg,
            customSearchEngines = customEngines,
            zenMods = mods,
            firefoxExtensions = extensions,
            recentBlockedEvents = session.recentBlockedEvents,
            activeSheet = session.activeSheet,
            pageLoadingProgress = session.pageLoadingProgress,
            canGoBack = session.canGoBack,
            canGoForward = session.canGoForward,
            findInPageQuery = session.findInPageQuery,
            isFindInPageVisible = session.isFindInPageVisible,
            isInPictureInPictureMode = session.isInPictureInPictureMode,
            statusBannerMessage = session.statusBannerMessage,
            desktopModeTabIds = session.desktopModeTabIds,
            searchSuggestions = session.searchSuggestions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZenBrowserUiState()
    )

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private fun <A, B, C, D> tuple4(a: A, b: B, c: C, d: D) = Quad(a, b, c, d)
    private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
    private fun <A, B, C, D, E> tuple5(a: A, b: B, c: C, d: D, e: E) = Quint(a, b, c, d, e)
    private data class Sextuple<A, B, C, D, E, F>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E, val sixth: F)

    private suspend fun applyStartupLifecycleRules() {
        val cfg = repository.sidebarConfigFlow?.first() ?: localSidebarConfig.value
        val now = System.currentTimeMillis()
        val elapsedMillis = now - cfg.lastSessionTimestamp
        val thresholdMillis = cfg.homepageAfterHoursThreshold * 3600_000L

        if (!cfg.hasCompletedTutorial) {
            sessionState.update { it.copy(showTutorialDialog = true) }
        }

        if (cfg.closePrivateOnAppClose) {
            repository.deletePrivateTabsAndWorkspaces()
        }

        val shouldStartAtHomepageAndUnload = when (cfg.startupBehavior) {
            StartupBehavior.UNLOAD_AND_HOMEPAGE -> true
            StartupBehavior.RESUME_WHERE_LEFT_OFF -> false
            StartupBehavior.HOMEPAGE_AFTER_HOURS -> elapsedMillis >= thresholdMillis
        }

        if (shouldStartAtHomepageAndUnload) {
            repository.unloadAllActiveTabs()
            val tabs = repository.tabs.first()
            val existingHome = tabs.firstOrNull { it.url == "zen://home" && !it.isEssential && !it.isPrivate }
            if (existingHome != null) {
                sessionState.update {
                    it.copy(
                        activeWorkspaceId = existingHome.workspaceId,
                        activeTabId = existingHome.id
                    )
                }
            } else {
                val defaultWsId = repository.workspaces.first().firstOrNull { !it.isPrivate }?.id ?: "ws_default"
                val homeTab = TabEntity(
                    id = "tab_${UUID.randomUUID().toString().take(8)}",
                    workspaceId = defaultWsId,
                    title = "New Tab",
                    url = "zen://home",
                    isPinned = false,
                    isEssential = false
                )
                repository.insertTab(homeTab)
                sessionState.update {
                    it.copy(
                        activeWorkspaceId = defaultWsId,
                        activeTabId = homeTab.id
                    )
                }
            }
        } else {
            val (savedWsId, savedTabId) = repository.getLastSessionState()
            val allTabs = repository.tabs.first()
            val targetTab = if (!savedTabId.isNullOrBlank()) allTabs.firstOrNull { it.id == savedTabId && !it.isPrivate } else null
            if (targetTab != null) {
                if (targetTab.isUnloaded) {
                    repository.updateTab(
                        targetTab.copy(
                            isUnloaded = false,
                            lastAccessed = System.currentTimeMillis()
                        )
                    )
                }
                sessionState.update {
                    it.copy(
                        activeWorkspaceId = savedWsId ?: targetTab.workspaceId,
                        activeTabId = targetTab.id
                    )
                }
            }
        }
    }

    fun onAppBackgroundedOrClosed() {
        val cfg = localSidebarConfig.value
        val curWs = sessionState.value.activeWorkspaceId
        val curTab = sessionState.value.activeTabId
        viewModelScope.launch {
            if (cfg.closePrivateOnAppClose) {
                repository.deletePrivateTabsAndWorkspaces()
            }
            // Do not unload tabs when exiting the app so user's active tabs remain loaded
            val updated = cfg.copy(lastSessionTimestamp = System.currentTimeMillis())
            localSidebarConfig.value = updated
            repository.saveSidebarConfig(updated)
            if (curWs.isNotBlank() && curTab.isNotBlank()) {
                repository.saveLastSessionState(curWs, curTab)
            }
        }
    }

    fun triggerTutorial() {
        sessionState.update {
            it.copy(
                showTutorialDialog = true,
                activeSheet = ActiveModalSheet.NONE
            )
        }
    }

    fun dismissTutorial() {
        sessionState.update { it.copy(showTutorialDialog = false) }
        updateSidebarConfig { it.copy(hasCompletedTutorial = true) }
    }

    fun toggleSidebar() {
        if (sessionState.value.isInPictureInPictureMode) return
        if (!sessionState.value.isSidebarOpen && System.currentTimeMillis() < suppressSidebarOpenUntil) return
        sessionState.update { it.copy(isSidebarOpen = !it.isSidebarOpen) }
    }

    private var suppressSidebarOpenUntil = 0L

    /** Called when the user starts leaving the app (home/PiP gesture): stray gesture events must not open the panel. */
    fun onLeavingApp() {
        suppressSidebarOpenUntil = System.currentTimeMillis() + 1500L
        sessionState.update { it.copy(isSidebarOpen = false) }
    }

    fun setSidebarOpen(open: Boolean) {
        if (open && (sessionState.value.isInPictureInPictureMode || System.currentTimeMillis() < suppressSidebarOpenUntil)) return
        sessionState.update {
            it.copy(
                isSidebarOpen = open,
                requestSidebarSearchFocus = if (!open) false else it.requestSidebarSearchFocus
            )
        }
    }

    fun clearSidebarSearchFocusRequest() {
        sessionState.update { it.copy(requestSidebarSearchFocus = false) }
    }

    fun setCenterTopSearchPopupVisible(visible: Boolean) {
        sessionState.update { it.copy(isCenterTopSearchPopupVisible = visible) }
    }

    fun calculateNewTabSortOrder(): Int {
        val state = uiState.value
        val openTabs = state.currentWorkspaceOpenTabs
        return if (state.sidebarConfig.newTabPosition == NewTabPosition.TOP) {
            (openTabs.minOfOrNull { it.sortOrder } ?: 0) - 1
        } else {
            (openTabs.maxOfOrNull { it.sortOrder } ?: 0) + 1
        }
    }

    fun onNewTabButtonClicked() {
        val cfg = localSidebarConfig.value
        if (cfg.urlBarPosition == UrlBarPosition.SIDEBAR_ONLY) {
            val state = uiState.value
            val effectivePrivate = state.activeWorkspace?.isPrivate == true
            val targetSortOrder = calculateNewTabSortOrder()
            val newTab = TabEntity(
                id = "tab_${UUID.randomUUID().toString().take(8)}",
                workspaceId = state.activeWorkspaceId,
                title = if (effectivePrivate) "Private Tab" else "New Tab",
                url = "zen://home",
                isPinned = false,
                isEssential = false,
                isPrivate = effectivePrivate,
                sortOrder = targetSortOrder,
                lastAccessed = System.currentTimeMillis()
            )
            viewModelScope.launch {
                repository.insertTab(newTab)
                if (cfg.singleToolbarNewTabBehavior == SingleToolbarNewTabBehavior.CENTER_TOP_POPUP) {
                    sessionState.update {
                        it.copy(
                            activeTabId = newTab.id,
                            isSidebarOpen = false,
                            isCenterTopSearchPopupVisible = true
                        )
                    }
                } else {
                    sessionState.update {
                        it.copy(
                            activeTabId = newTab.id,
                            isSidebarOpen = true,
                            requestSidebarSearchFocus = true
                        )
                    }
                }
            }
        } else {
            createNewTab("zen://home")
        }
    }

    fun selectWorkspace(workspaceId: String) {
        val currentTabs = uiState.value.allTabs
        val firstWorkspaceTab = currentTabs
            .filter { it.workspaceId == workspaceId && !it.isEssential }
            .maxByOrNull { it.lastAccessed }
            ?: currentTabs.firstOrNull { it.isEssential }

        sessionState.update {
            it.copy(
                activeWorkspaceId = workspaceId,
                activeTabId = firstWorkspaceTab?.id ?: it.activeTabId
            )
        }
    }

    fun createWorkspace(
        name: String,
        iconKey: String,
        accentThemeId: String,
        isPrivate: Boolean = false
    ) {
        val trimmed = name.trim().ifEmpty { if (isPrivate) "Private Space" else "Workspace" }
        val newId = "ws_${UUID.randomUUID().toString().take(8)}"
        val order = uiState.value.workspaces.size
        val workspace = WorkspaceEntity(
            id = newId,
            name = trimmed,
            iconKey = if (isPrivate) "private" else iconKey,
            accentThemeId = accentThemeId,
            sortOrder = order,
            isPrivate = isPrivate
        )
        val starterTab = TabEntity(
            id = "tab_${UUID.randomUUID().toString().take(8)}",
            workspaceId = newId,
            title = if (isPrivate) "Private Tab" else "New Tab",
            url = "zen://home",
            isPinned = false,
            isEssential = false,
            isPrivate = isPrivate
        )
        viewModelScope.launch {
            repository.insertWorkspace(workspace)
            repository.insertTab(starterTab)
            sessionState.update {
                it.copy(
                    activeWorkspaceId = newId,
                    activeTabId = starterTab.id,
                    activeSheet = ActiveModalSheet.NONE,
                    statusBannerMessage = if (isPrivate) "Opened Private Workspace" else null
                )
            }
        }
    }

    fun openOrSwitchToPrivateWorkspace() {
        val existingPrivateWs = uiState.value.workspaces.firstOrNull { it.isPrivate }
        if (existingPrivateWs != null) {
            selectWorkspace(existingPrivateWs.id)
            showBanner("Switched to Private Workspace")
        } else {
            createWorkspace(
                name = "Private",
                iconKey = "private",
                accentThemeId = ZenAccentTheme.MONO_OBSIDIAN.id,
                isPrivate = true
            )
        }
    }

    fun updateWorkspaceTheme(workspace: WorkspaceEntity, newAccentId: String) {
        viewModelScope.launch {
            repository.updateWorkspace(workspace.copy(accentThemeId = newAccentId))
        }
    }

    fun deleteWorkspace(workspaceId: String) {
        if (uiState.value.workspaces.size <= 1) return
        viewModelScope.launch {
            repository.deleteWorkspace(workspaceId)
        }
    }

    fun selectTab(tabId: String) {
        val state = uiState.value
        val targetTab = state.allTabs.find { it.id == tabId } ?: return

        // If user is currently in Split Screen Tab Selection Mode in the side panel:
        if (state.isSelectingSplitTabs) {
            toggleTabForSplitSelection(tabId)
            return
        }

        val targetWsId = if (!targetTab.isEssential) targetTab.workspaceId else state.activeWorkspaceId
        viewModelScope.launch {
            repository.saveLastSessionState(targetWsId, tabId)
            repository.updateTab(
                targetTab.copy(
                    isUnloaded = false,
                    lastAccessed = System.currentTimeMillis()
                )
            )
        }

        sessionState.update { sess ->
            val updatedWorkspaceId = if (!targetTab.isEssential) targetTab.workspaceId else sess.activeWorkspaceId
            if (sess.isSplitViewEnabled && sess.focusedPane == FocusedPane.SECONDARY) {
                sess.copy(
                    secondarySplitTabId = tabId,
                    isSidebarOpen = false
                )
            } else {
                sess.copy(
                    activeWorkspaceId = updatedWorkspaceId,
                    activeTabId = tabId,
                    isSidebarOpen = false
                )
            }
        }
    }

    fun toggleTabForSplitSelection(tabId: String) {
        val currentSelected = sessionState.value.selectedSplitTabIds.toMutableList()
        if (currentSelected.contains(tabId)) {
            currentSelected.remove(tabId)
        } else {
            if (currentSelected.size >= 2) {
                currentSelected.removeAt(0)
            }
            currentSelected.add(tabId)
        }

        if (currentSelected.size == 2) {
            // Two tabs selected -> immediately open Split View with those two tabs
            openSplitViewWithTabs(currentSelected[0], currentSelected[1])
        } else {
            sessionState.update {
                it.copy(selectedSplitTabIds = currentSelected)
            }
        }
    }

    fun confirmSplitSelectionWithHomepageIfSingle() {
        val state = uiState.value
        val selected = state.selectedSplitTabIds
        if (selected.isEmpty()) {
            val primaryId = state.activeTabId
            createHomepageAndOpenSplit(primaryId)
        } else if (selected.size == 1) {
            createHomepageAndOpenSplit(selected[0])
        } else {
            openSplitViewWithTabs(selected[0], selected[1])
        }
    }

    fun cancelSplitTabSelection() {
        sessionState.update {
            it.copy(
                isSelectingSplitTabs = false,
                selectedSplitTabIds = emptyList()
            )
        }
    }

    private fun createHomepageAndOpenSplit(primaryTabId: String) {
        val state = uiState.value
        val maxSort = state.currentWorkspaceOpenTabs.maxOfOrNull { it.sortOrder } ?: 0
        val homeSplitTab = TabEntity(
            id = "tab_${UUID.randomUUID().toString().take(8)}",
            workspaceId = state.activeWorkspaceId,
            title = "New Tab",
            url = "zen://home",
            isPinned = false,
            isEssential = false,
            isPrivate = state.activeWorkspace?.isPrivate == true,
            sortOrder = maxSort + 1
        )
        viewModelScope.launch {
            repository.insertTab(homeSplitTab)
            sessionState.update {
                it.copy(
                    activeTabId = primaryTabId,
                    secondarySplitTabId = homeSplitTab.id,
                    isSplitViewEnabled = true,
                    isSelectingSplitTabs = false,
                    selectedSplitTabIds = emptyList(),
                    focusedPane = FocusedPane.SECONDARY,
                    activeSheet = ActiveModalSheet.NONE,
                    isSidebarOpen = false,
                    statusBannerMessage = "Split View opened with Homepage"
                )
            }
        }
    }

    fun openSplitViewWithTabs(primaryTabId: String, secondaryTabId: String) {
        if (primaryTabId == secondaryTabId) return
        val state = uiState.value
        val primaryTab = state.allTabs.find { it.id == primaryTabId }
        val secondaryTab = state.allTabs.find { it.id == secondaryTabId }

        viewModelScope.launch {
            primaryTab?.let {
                repository.updateTab(it.copy(isUnloaded = false, lastAccessed = System.currentTimeMillis()))
            }
            secondaryTab?.let {
                repository.updateTab(it.copy(isUnloaded = false, lastAccessed = System.currentTimeMillis()))
            }
        }

        sessionState.update {
            it.copy(
                activeTabId = primaryTabId,
                secondarySplitTabId = secondaryTabId,
                isSplitViewEnabled = true,
                isSelectingSplitTabs = false,
                selectedSplitTabIds = emptyList(),
                focusedPane = FocusedPane.SECONDARY,
                activeSheet = ActiveModalSheet.NONE,
                isSidebarOpen = false,
                statusBannerMessage = "Split View active • Double-tap any side to focus"
            )
        }
    }

    fun reorderTabs(fromTabId: String, toIndex: Int, isPinnedSection: Boolean) {
        val state = uiState.value
        val sectionTabs = (if (isPinnedSection) state.currentWorkspacePinnedTabs else state.currentWorkspaceOpenTabs)
            .toMutableList()
        val fromIndex = sectionTabs.indexOfFirst { it.id == fromTabId }
        if (fromIndex == -1) return
        val clampedTo = toIndex.coerceIn(0, sectionTabs.lastIndex)
        if (fromIndex == clampedTo) return

        val moved = sectionTabs.removeAt(fromIndex)
        sectionTabs.add(clampedTo, moved)

        val updatedTabs = sectionTabs.mapIndexed { idx, tab ->
            tab.copy(sortOrder = idx)
        }
        viewModelScope.launch {
            repository.updateTabs(updatedTabs)
        }
    }

    fun createNewTab(
        initialInput: String = "zen://home",
        pinToWorkspace: Boolean = false,
        isPrivate: Boolean = false,
        switchToTab: Boolean = true
    ) {
        val state = uiState.value
        val effectivePrivate = isPrivate || (state.activeWorkspace?.isPrivate == true)
        val sanitized = ZenPrivacyEngine.resolveInputToUrl(
            rawInput = initialInput,
            privacyConfig = state.privacyConfig,
            isPrivateContext = effectivePrivate,
            customEngines = state.customSearchEngines,
            fakeSearchTrigger = state.sidebarConfig.fakeSearchQueryTrigger
        )
        val title = when {
            sanitized.finalUrl == "zen://home" -> if (effectivePrivate) "Private Tab" else "New Tab"
            else -> ZenPrivacyEngine.extractHostDisplay(sanitized.finalUrl)
        }
        val targetSortOrder = calculateNewTabSortOrder()
        val newTab = TabEntity(
            id = "tab_${UUID.randomUUID().toString().take(8)}",
            workspaceId = state.activeWorkspaceId,
            title = title,
            url = sanitized.finalUrl,
            isPinned = pinToWorkspace,
            isEssential = false,
            isPrivate = effectivePrivate,
            sortOrder = targetSortOrder,
            lastAccessed = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertTab(newTab)
            recordSanitizationMetrics(sanitized.strippedParamsCount, sanitized.upgradedToHttps)
            if (switchToTab) {
                sessionState.update { sess ->
                    if (sess.isSplitViewEnabled && sess.focusedPane == FocusedPane.SECONDARY) {
                        sess.copy(
                            secondarySplitTabId = newTab.id,
                            isSidebarOpen = false,
                            statusBannerMessage = if (effectivePrivate) "New Private Tab" else null
                        )
                    } else {
                        sess.copy(
                            activeTabId = newTab.id,
                            isSidebarOpen = false,
                            statusBannerMessage = if (effectivePrivate) "New Private Tab" else null
                        )
                    }
                }
            } else {
                sessionState.update { sess ->
                    sess.copy(
                        statusBannerMessage = "Tab opened in background • '$title'"
                    )
                }
            }
        }
    }

    fun openLinkInNewTab(
        url: String,
        isPrivate: Boolean = false,
        forceSwitch: Boolean? = null
    ) {
        val shouldSwitch = forceSwitch ?: uiState.value.sidebarConfig.switchToNewTabImmediately
        createNewTab(
            initialInput = url,
            isPrivate = isPrivate,
            switchToTab = shouldSwitch
        )
    }

    fun updateSwitchToNewTabImmediately(enabled: Boolean) {
        updateSidebarConfig { it.copy(switchToNewTabImmediately = enabled) }
    }

    fun updateNewTabPosition(position: NewTabPosition) {
        updateSidebarConfig { it.copy(newTabPosition = position) }
    }

    fun duplicateTab(tab: TabEntity) {
        val state = uiState.value
        val maxSort = (state.allTabs.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val duplicatedTab = TabEntity(
            id = "tab_${UUID.randomUUID().toString().take(8)}",
            workspaceId = tab.workspaceId,
            title = tab.title,
            url = tab.url,
            isPinned = false,
            isEssential = false,
            isBookmarked = tab.isBookmarked,
            isUnloaded = false,
            isPrivate = tab.isPrivate,
            sortOrder = maxSort,
            lastAccessed = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertTab(duplicatedTab)
            sessionState.update {
                it.copy(
                    activeTabId = duplicatedTab.id,
                    statusBannerMessage = "Duplicated '${tab.title.ifBlank { "Tab" }}'"
                )
            }
        }
    }

    fun deleteHistoryEntry(id: String) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(id)
            showBanner("Removed history entry")
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            showBanner("Browsing history cleared")
        }
    }

    fun addDownloadItem(fileName: String, url: String, mimeType: String, totalBytes: Long) {
        viewModelScope.launch {
            repository.insertDownloadItem(
                DownloadItemEntity(
                    id = "dl_${UUID.randomUUID().toString().take(8)}",
                    fileName = fileName,
                    url = url,
                    mimeType = mimeType,
                    timestamp = System.currentTimeMillis(),
                    totalBytes = totalBytes,
                    status = "completed"
                )
            )
            showBanner("Downloaded $fileName")
        }
    }

    fun deleteDownloadItem(id: String) {
        viewModelScope.launch {
            repository.deleteDownloadItem(id)
            showBanner("Removed download item")
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
            showBanner("Downloads list cleared")
        }
    }

    fun saveCurrentPageForOffline(tabId: String, htmlContent: String) {
        val state = uiState.value
        val tab = state.allTabs.find { it.id == tabId } ?: state.focusedTab ?: return
        if (tab.url.isBlank() || tab.url.startsWith("zen://")) {
            showBanner("Cannot save system page for offline reading")
            return
        }
        val cleanTitle = tab.title.ifBlank { ZenPrivacyEngine.extractHostDisplay(tab.url) }
        val bytes = htmlContent.toByteArray(Charsets.UTF_8)
        val pageId = "off_${UUID.randomUUID().toString().take(8)}"
        val page = OfflinePageEntity(
            id = pageId,
            title = cleanTitle,
            url = tab.url,
            htmlContent = htmlContent,
            timestamp = System.currentTimeMillis(),
            sizeBytes = bytes.size.toLong()
        )
        viewModelScope.launch {
            repository.insertOfflinePage(page)
            showBanner("Saved '$cleanTitle' for offline reading")
        }
    }

    fun saveTabForOffline(tab: TabEntity, htmlContent: String = "") {
        if (tab.url.isBlank() || tab.url.startsWith("zen://")) {
            showBanner("Cannot save system page for offline reading")
            return
        }
        val cleanTitle = tab.title.ifBlank { ZenPrivacyEngine.extractHostDisplay(tab.url) }
        val bytes = htmlContent.toByteArray(Charsets.UTF_8)
        val pageId = "off_${UUID.randomUUID().toString().take(8)}"
        val page = OfflinePageEntity(
            id = pageId,
            title = cleanTitle,
            url = tab.url,
            htmlContent = htmlContent,
            timestamp = System.currentTimeMillis(),
            sizeBytes = if (bytes.isNotEmpty()) bytes.size.toLong() else tab.url.length.toLong()
        )
        viewModelScope.launch {
            repository.insertOfflinePage(page)
            showBanner("Saved '$cleanTitle' for offline reading")
        }
    }

    fun deleteOfflinePage(id: String) {
        viewModelScope.launch {
            repository.deleteOfflinePage(id)
            showBanner("Removed offline page")
        }
    }

    fun insertPassword(password: PasswordEntity) {
        viewModelScope.launch {
            repository.insertPassword(password)
            showBanner("Saved login details for ${password.domain}")
        }
    }

    fun deletePassword(id: String) {
        viewModelScope.launch {
            repository.deletePassword(id)
            showBanner("Removed password")
        }
    }

    fun clearAllPasswords() {
        viewModelScope.launch {
            repository.clearAllPasswords()
            showBanner("Cleared all saved passwords")
        }
    }

    fun clearAllOfflinePages() {
        viewModelScope.launch {
            repository.clearAllOfflinePages()
            showBanner("Cleared offline pages")
        }
    }

    fun openOfflinePage(page: OfflinePageEntity) {
        val targetUrl = "zen://offline?id=${page.id}"
        createNewTab(initialInput = targetUrl, switchToTab = true)
    }

    fun handleNewTabSwipeGesture(isLeftSwipe: Boolean) {
        val cfg = localSidebarConfig.value
        val action = if (isLeftSwipe) cfg.newTabSwipeLeftAction else cfg.newTabSwipeRightAction
        when (action) {
            NewTabSwipeGestureAction.OPEN_PRIVATE_TAB -> createNewTab(
                initialInput = "zen://home",
                isPrivate = true
            )
            NewTabSwipeGestureAction.OPEN_PRIVATE_WORKSPACE -> openOrSwitchToPrivateWorkspace()
            NewTabSwipeGestureAction.OPEN_NORMAL_TAB -> createNewTab(
                initialInput = "zen://home",
                isPrivate = false
            )
            NewTabSwipeGestureAction.NONE -> {}
        }
    }

    fun performTabSwipeAction(tab: TabEntity, isRightSwipe: Boolean) {
        val cfg = localSidebarConfig.value
        val action = if (isRightSwipe) cfg.tabSwipeRightAction else cfg.tabSwipeLeftAction
        when (action) {
            TabSwipeAction.UNLOAD -> unloadTab(tab)
            TabSwipeAction.CLOSE -> closeTab(tab.id)
            TabSwipeAction.PIN -> togglePinTab(tab)
            TabSwipeAction.BOOKMARK -> toggleBookmarkTab(tab)
            TabSwipeAction.ESSENTIAL -> toggleEssentialTab(tab)
            TabSwipeAction.NONE -> {}
        }
    }

    fun unloadTab(tab: TabEntity) {
        val state = uiState.value
        val activeWorkspaceTabs = state.allTabs.filter {
            it.id != tab.id &&
                !it.isUnloaded &&
                (it.workspaceId == state.activeWorkspaceId || it.isEssential)
        }
        val fallbackNonUnloadedTab = activeWorkspaceTabs
            .filter { !it.isEssential }
            .maxByOrNull { it.lastAccessed }
            ?: activeWorkspaceTabs.maxByOrNull { it.lastAccessed }
            ?: state.allTabs.filter { it.id != tab.id && !it.isUnloaded }.maxByOrNull { it.lastAccessed }

        viewModelScope.launch {
            com.example.ui.components.ZenWebViewManager.remove(tab.id)
            repository.updateTab(tab.copy(isUnloaded = true))

            val isPrimaryBeingUnloaded = state.activeTabId == tab.id
            val isSecondaryBeingUnloaded = state.isSplitViewEnabled && state.secondarySplitTabId == tab.id

            if (isPrimaryBeingUnloaded || isSecondaryBeingUnloaded) {
                if (fallbackNonUnloadedTab != null) {
                    sessionState.update { sess ->
                        val nextWsId = if (!fallbackNonUnloadedTab.isEssential) {
                            fallbackNonUnloadedTab.workspaceId
                        } else {
                            sess.activeWorkspaceId
                        }
                        if (isSecondaryBeingUnloaded && !isPrimaryBeingUnloaded) {
                            val altSecondary = activeWorkspaceTabs.firstOrNull { it.id != sess.activeTabId }
                            if (altSecondary != null) {
                                sess.copy(
                                    secondarySplitTabId = altSecondary.id,
                                    statusBannerMessage = "Tab unloaded • Switched to '${altSecondary.title}'"
                                )
                            } else {
                                sess.copy(
                                    secondarySplitTabId = null,
                                    isSplitViewEnabled = false,
                                    focusedPane = FocusedPane.PRIMARY,
                                    statusBannerMessage = "Tab unloaded • Switched to '${state.activePrimaryTab?.title ?: "Active Tab"}'"
                                )
                            }
                        } else {
                            sess.copy(
                                activeWorkspaceId = nextWsId,
                                activeTabId = fallbackNonUnloadedTab.id,
                                statusBannerMessage = "Tab unloaded • Switched to '${fallbackNonUnloadedTab.title}'"
                            )
                        }
                    }
                } else {
                    // All tabs are unloaded -> go to Home Screen
                    val effectivePrivate = state.activeWorkspace?.isPrivate == true
                    val existingHomeTab = state.allTabs.firstOrNull {
                        it.id != tab.id &&
                            it.url == "zen://home" &&
                            it.workspaceId == state.activeWorkspaceId &&
                            !it.isEssential
                    }
                    if (existingHomeTab != null) {
                        repository.updateTab(
                            existingHomeTab.copy(
                                isUnloaded = false,
                                lastAccessed = System.currentTimeMillis()
                            )
                        )
                        sessionState.update { sess ->
                            if (isSecondaryBeingUnloaded && !isPrimaryBeingUnloaded) {
                                sess.copy(
                                    secondarySplitTabId = existingHomeTab.id,
                                    statusBannerMessage = "All tabs unloaded • Returned to Home Screen"
                                )
                            } else {
                                sess.copy(
                                    activeTabId = existingHomeTab.id,
                                    statusBannerMessage = "All tabs unloaded • Returned to Home Screen"
                                )
                            }
                        }
                    } else {
                        val maxSortOrder = (state.allTabs.maxOfOrNull { it.sortOrder } ?: 0) + 1
                        val homeTab = TabEntity(
                            id = "tab_${UUID.randomUUID().toString().take(8)}",
                            workspaceId = state.activeWorkspaceId,
                            title = if (effectivePrivate) "Private Tab" else "New Tab",
                            url = "zen://home",
                            isPinned = false,
                            isEssential = false,
                            isUnloaded = false,
                            isPrivate = effectivePrivate,
                            sortOrder = maxSortOrder,
                            lastAccessed = System.currentTimeMillis()
                        )
                        repository.insertTab(homeTab)
                        sessionState.update { sess ->
                            if (isSecondaryBeingUnloaded && !isPrimaryBeingUnloaded) {
                                sess.copy(
                                    secondarySplitTabId = homeTab.id,
                                    statusBannerMessage = "All tabs unloaded • Returned to Home Screen"
                                )
                            } else {
                                sess.copy(
                                    activeTabId = homeTab.id,
                                    statusBannerMessage = "All tabs unloaded • Returned to Home Screen"
                                )
                            }
                        }
                    }
                }
            } else {
                showBanner("Tab unloaded to save memory")
            }
        }
    }

    fun toggleBookmarkTab(tab: TabEntity) {
        viewModelScope.launch {
            val next = !tab.isBookmarked
            repository.updateTab(tab.copy(isBookmarked = next))
            showBanner(if (next) "Bookmarked '${tab.title}'" else "Removed bookmark")
        }
    }

    fun moveTabToWorkspace(tab: TabEntity, targetWorkspaceId: String) {
        val targetWs = uiState.value.workspaces.find { it.id == targetWorkspaceId } ?: return
        viewModelScope.launch {
            repository.updateTab(
                tab.copy(
                    workspaceId = targetWorkspaceId,
                    isEssential = false,
                    isPrivate = tab.isPrivate || targetWs.isPrivate
                )
            )
            showBanner("Moved tab to ${targetWs.name}")
        }
    }

    fun addEssentialSite(title: String, rawUrl: String) {
        val state = uiState.value
        val sanitized = ZenPrivacyEngine.resolveInputToUrl(
            rawInput = rawUrl,
            privacyConfig = state.privacyConfig,
            isPrivateContext = false,
            customEngines = state.customSearchEngines
        )
        val cleanTitle = title.trim().ifEmpty {
            ZenPrivacyEngine.extractHostDisplay(sanitized.finalUrl)
        }
        val essentialTab = TabEntity(
            id = "tab_ess_${UUID.randomUUID().toString().take(8)}",
            workspaceId = state.activeWorkspaceId,
            title = cleanTitle,
            url = sanitized.finalUrl,
            isPinned = false,
            isEssential = true,
            lastAccessed = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertTab(essentialTab)
            showBanner("Added '$cleanTitle' to Essentials")
        }
    }

    fun removeEssentialSite(tab: TabEntity) {
        viewModelScope.launch {
            repository.deleteTab(tab.id)
            showBanner("Removed '${tab.title}' from Essentials")
        }
    }

    fun addHomeShortcut(title: String, rawUrl: String) {
        val cleanUrl = rawUrl.trim().let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        }
        val cleanTitle = title.trim().ifEmpty { ZenPrivacyEngine.extractHostDisplay(cleanUrl) }
        val shortcut = HomeShortcutEntity(
            id = "sc_${UUID.randomUUID().toString().take(8)}",
            title = cleanTitle,
            url = cleanUrl,
            sortOrder = uiState.value.homeShortcuts.size
        )
        viewModelScope.launch {
            repository.insertHomeShortcut(shortcut)
        }
    }

    fun updateHomeShortcut(shortcut: HomeShortcutEntity, newTitle: String, newUrl: String) {
        val cleanUrl = newUrl.trim().let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        }
        val cleanTitle = newTitle.trim().ifEmpty { ZenPrivacyEngine.extractHostDisplay(cleanUrl) }
        viewModelScope.launch {
            repository.updateHomeShortcut(shortcut.copy(title = cleanTitle, url = cleanUrl))
        }
    }

    fun deleteHomeShortcut(shortcutId: String) {
        viewModelScope.launch {
            repository.deleteHomeShortcut(shortcutId)
        }
    }

    fun updateMediaPlaybackState(
        isActive: Boolean,
        isPlaying: Boolean,
        title: String,
        domain: String,
        tabId: String
    ) {
        sessionState.update { current ->
            val previous = current.mediaPlaybackState
            val effectiveTabId = tabId.ifBlank { previous.tabId }.ifBlank { current.activeTabId }

            // If another tab reports idle/no media while we have an active media session on a different tab,
            // ignore the idle report so background tabs or newly selected tabs don't kill playback state.
            if (previous.isActive && previous.tabId.isNotBlank() &&
                effectiveTabId.isNotBlank() && previous.tabId != effectiveTabId && !isPlaying
            ) {
                return@update current
            }

            // Keep the media bar/session active when paused or when DOM polling temporarily misses the element.
            val resolvedActive = when {
                isActive -> true
                isPlaying -> true
                previous.isActive && (previous.tabId == effectiveTabId || previous.tabId.isBlank()) -> true
                else -> false
            }

            // Stabilize title to avoid title jitter causing repeated notification re-draws
            val resolvedTitle = when {
                title.isNotBlank() && !title.equals("YouTube", ignoreCase = true) && !title.equals(domain, ignoreCase = true) -> title
                previous.title.isNotBlank() && !previous.title.equals("YouTube", ignoreCase = true) && !previous.title.equals(domain, ignoreCase = true) -> previous.title
                title.isNotBlank() -> title
                previous.title.isNotBlank() -> previous.title
                else -> domain.ifBlank { "Zen Browser" }
            }

            val resolvedDomain = domain.ifBlank { previous.domain }

            if (previous.isActive == resolvedActive &&
                previous.isPlaying == isPlaying &&
                previous.title == resolvedTitle &&
                previous.domain == resolvedDomain &&
                previous.tabId == effectiveTabId
            ) {
                return@update current
            }

            current.copy(
                mediaPlaybackState = MediaPlaybackState(
                    isActive = resolvedActive,
                    isPlaying = isPlaying,
                    isMuted = if (previous.tabId.isNotBlank() && effectiveTabId.isNotBlank() && previous.tabId != effectiveTabId) false else previous.isMuted,
                    title = resolvedTitle,
                    domain = resolvedDomain,
                    tabId = effectiveTabId
                )
            )
        }
    }

    fun setMediaMuted(muted: Boolean) {
        sessionState.update { it.copy(mediaPlaybackState = it.mediaPlaybackState.copy(isMuted = muted)) }
    }

    fun dismissMediaPlaybackBar() {
        sessionState.update {
            it.copy(mediaPlaybackState = MediaPlaybackState(isActive = false, isPlaying = false))
        }
    }

    fun setPictureInPictureMode(inPip: Boolean) {
        if (sessionState.value.isInPictureInPictureMode == inPip) return
        sessionState.update {
            it.copy(
                isInPictureInPictureMode = inPip,
                isSidebarOpen = if (inPip) false else it.isSidebarOpen,
                activeSheet = if (inPip) ActiveModalSheet.NONE else it.activeSheet,
                isCenterTopSearchPopupVisible = if (inPip) false else it.isCenterTopSearchPopupVisible,
                requestSidebarSearchFocus = if (inPip) false else it.requestSidebarSearchFocus
            )
        }
    }

    fun updateBackgroundPlayEnabled(enabled: Boolean) {
        updateSidebarConfig { it.copy(backgroundPlayEnabled = enabled) }
    }

    private val tabHistoryStacks = mutableMapOf<String, ArrayDeque<String>>()
    private val tabForwardStacks = mutableMapOf<String, ArrayDeque<String>>()

    fun pushTabHistory(tabId: String, url: String) {
        if (url.isBlank() || url == "about:blank") return
        val stack = tabHistoryStacks.getOrPut(tabId) { ArrayDeque() }
        if (stack.lastOrNull() != url) {
            stack.addLast(url)
            if (stack.size > 50) stack.removeFirst()
        }
    }

    fun popTabHistory(tabId: String, currentUrl: String? = null): String? {
        val stack = tabHistoryStacks[tabId] ?: return null
        if (currentUrl != null) {
            while (stack.isNotEmpty() && stack.last() == currentUrl) {
                stack.removeLast()
            }
        }
        if (stack.isEmpty()) return null
        val targetUrl = stack.removeLast()
        if (!currentUrl.isNullOrBlank() && currentUrl != "about:blank") {
            val fwdStack = tabForwardStacks.getOrPut(tabId) { ArrayDeque() }
            if (fwdStack.lastOrNull() != currentUrl) {
                fwdStack.addLast(currentUrl)
                if (fwdStack.size > 50) fwdStack.removeFirst()
            }
        }
        return targetUrl
    }

    fun popTabForward(tabId: String, currentUrl: String? = null): String? {
        val fwdStack = tabForwardStacks[tabId] ?: return null
        if (currentUrl != null) {
            while (fwdStack.isNotEmpty() && fwdStack.last() == currentUrl) {
                fwdStack.removeLast()
            }
        }
        if (fwdStack.isEmpty()) return null
        val targetUrl = fwdStack.removeLast()
        if (!currentUrl.isNullOrBlank() && currentUrl != "about:blank") {
            pushTabHistory(tabId, currentUrl)
        }
        return targetUrl
    }

    fun canTabHistoryGoBack(tabId: String): Boolean {
        return tabHistoryStacks[tabId]?.isNotEmpty() == true
    }

    fun canTabForwardGoForward(tabId: String): Boolean {
        return tabForwardStacks[tabId]?.isNotEmpty() == true
    }

    fun getTabHistoryEntries(tabId: String): List<HistoryNavEntry> {
        val stack = tabHistoryStacks[tabId] ?: return emptyList()
        return stack.toList().reversed().mapIndexed { index, url ->
            HistoryNavEntry(
                title = ZenPrivacyEngine.extractHostDisplay(url),
                url = url,
                stepsBack = index + 1
            )
        }
    }

    fun popTabHistoryOffset(tabId: String, offset: Int, currentUrl: String? = null): String? {
        if (offset >= 0) return null
        val steps = kotlin.math.abs(offset)
        var targetUrl: String? = null
        for (i in 0 until steps) {
            val popped = popTabHistory(tabId, if (i == 0) currentUrl else targetUrl) ?: break
            targetUrl = popped
        }
        return targetUrl
    }

    fun updatePlayInWindowPipEnabled(enabled: Boolean) {
        updateSidebarConfig { it.copy(playInWindowPipEnabled = enabled) }
    }

    fun navigateFocusedTab(rawInput: String) {
        val state = uiState.value
        val targetTab = state.focusedTab
        val isPrivate = state.isCurrentContextPrivate
        val sanitized = ZenPrivacyEngine.resolveInputToUrl(
            rawInput = rawInput,
            privacyConfig = state.privacyConfig,
            isPrivateContext = isPrivate,
            customEngines = state.customSearchEngines,
            fakeSearchTrigger = state.sidebarConfig.fakeSearchQueryTrigger
        )

        recordSanitizationMetrics(sanitized.strippedParamsCount, sanitized.upgradedToHttps)

        sessionState.update {
            it.copy(
                isCenterTopSearchPopupVisible = false,
                requestSidebarSearchFocus = false,
                isSidebarOpen = false
            )
        }

        if (targetTab == null) {
            createNewTab(sanitized.finalUrl, isPrivate = isPrivate)
            return
        }

        val previousUrl = targetTab.url
        if (previousUrl.isNotBlank() && previousUrl != "about:blank" && previousUrl != sanitized.finalUrl) {
            pushTabHistory(targetTab.id, previousUrl)
            tabForwardStacks[targetTab.id]?.clear()
        }

        val updatedTitle = if (sanitized.finalUrl == "zen://home") {
            if (targetTab.isPrivate) "Private Tab" else "New Tab"
        } else {
            ZenPrivacyEngine.extractHostDisplay(sanitized.finalUrl)
        }

        viewModelScope.launch {
            repository.updateTab(
                targetTab.copy(
                    url = sanitized.finalUrl,
                    title = updatedTitle,
                    isUnloaded = false,
                    lastAccessed = System.currentTimeMillis()
                )
            )
        }
    }

    fun onWebPageVisited(tabId: String, url: String, title: String?) {
        val state = uiState.value
        val tab = state.allTabs.find { it.id == tabId } ?: return
        val cleanTitle = title?.takeIf { it.isNotBlank() && !it.startsWith("http") }
            ?: ZenPrivacyEngine.extractHostDisplay(url)

        val tabWorkspace = state.workspaces.find { it.id == tab.workspaceId }
        val isPrivateContext = tab.isPrivate || (tabWorkspace?.isPrivate == true)

        if (tab.url != url || tab.title != cleanTitle || tab.isUnloaded) {
            val previousUrl = tab.url
            if (previousUrl.isNotBlank() && previousUrl != "about:blank" && previousUrl != url) {
                pushTabHistory(tabId, previousUrl)
            }

            viewModelScope.launch {
                repository.updateTab(
                    tab.copy(
                        url = url,
                        title = cleanTitle,
                        isUnloaded = false,
                        lastAccessed = System.currentTimeMillis()
                    )
                )

                // User Requirement: Private tab and private workspace do NOT save history!
                if (!isPrivateContext && !url.startsWith("zen://") && url != "about:blank") {
                    repository.insertHistoryEntry(
                        HistoryEntryEntity(
                            id = "hist_${UUID.randomUUID().toString().take(8)}",
                            url = url,
                            title = cleanTitle,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    fun togglePinTab(tab: TabEntity) {
        viewModelScope.launch {
            val updated = tab.copy(
                isPinned = !tab.isPinned,
                isEssential = false
            )
            repository.updateTab(updated)
            showBanner(if (updated.isPinned) "Pinned tab" else "Unpinned tab")
        }
    }

    fun toggleEssentialTab(tab: TabEntity) {
        viewModelScope.launch {
            val updated = tab.copy(
                isEssential = !tab.isEssential,
                isPinned = false
            )
            repository.updateTab(updated)
            showBanner(if (updated.isEssential) "Added to Essentials" else "Removed from Essentials")
        }
    }

    fun closeTab(tabId: String) {
        val state = uiState.value
        val remainingTabs = state.allTabs.filter { it.id != tabId }
        val remainingOpenTabsInWs = remainingTabs.filter { !it.isUnloaded && (it.workspaceId == state.activeWorkspaceId || it.isEssential) }
        val remainingOpenTabsAnywhere = remainingTabs.filter { !it.isUnloaded }
        val fallbackOpenTab = remainingOpenTabsInWs.filter { !it.isEssential }.maxByOrNull { it.lastAccessed }
            ?: remainingOpenTabsInWs.maxByOrNull { it.lastAccessed }
            ?: remainingOpenTabsAnywhere.maxByOrNull { it.lastAccessed }
            ?: remainingTabs.firstOrNull { it.workspaceId == state.activeWorkspaceId }
            ?: remainingTabs.firstOrNull()

        tabHistoryStacks.remove(tabId)
        tabForwardStacks.remove(tabId)
        com.example.ui.components.ZenWebViewManager.remove(tabId)
        sessionState.update { it.copy(desktopModeTabIds = it.desktopModeTabIds - tabId) }
        viewModelScope.launch {
            repository.deleteTab(tabId)
            val isPrimaryClosed = (state.activeTabId == tabId) || (state.activePrimaryTab?.id == tabId)
            val isSecondaryClosed = state.isSplitViewEnabled && state.secondarySplitTabId == tabId

            if (fallbackOpenTab != null) {
                if (fallbackOpenTab.isUnloaded) {
                    repository.updateTab(
                        fallbackOpenTab.copy(
                            isUnloaded = false,
                            lastAccessed = System.currentTimeMillis()
                        )
                    )
                }
                // There is another open tab available -> switch to that tab
                sessionState.update { sess ->
                    val nextPrimary = if (isPrimaryClosed) fallbackOpenTab.id else sess.activeTabId
                    val nextSecondary = if (isSecondaryClosed) null else sess.secondarySplitTabId
                    val updatedMedia = if (sess.mediaPlaybackState.tabId == tabId) {
                        MediaPlaybackState(isActive = false)
                    } else sess.mediaPlaybackState
                    val nextWs = if (!fallbackOpenTab.isEssential) fallbackOpenTab.workspaceId else sess.activeWorkspaceId

                    val showCloseNotification = state.sidebarConfig.showTabCloseNotification && state.sidebarConfig.showActionNotifications
                    sess.copy(
                        activeWorkspaceId = nextWs,
                        activeTabId = nextPrimary,
                        secondarySplitTabId = nextSecondary,
                        isSplitViewEnabled = sess.isSplitViewEnabled && nextSecondary != null,
                        mediaPlaybackState = updatedMedia,
                        statusBannerMessage = if (showCloseNotification) "Tab closed • Switched to '${fallbackOpenTab.title}'" else null
                    )
                }
            } else {
                // No other open tabs -> switch to Home Screen!
                val existingHomeTab = remainingTabs.firstOrNull {
                    it.url == "zen://home" && (it.workspaceId == state.activeWorkspaceId || !it.isEssential)
                }
                if (existingHomeTab != null) {
                    repository.updateTab(
                        existingHomeTab.copy(
                            isUnloaded = false,
                            lastAccessed = System.currentTimeMillis()
                        )
                    )
                    sessionState.update { sess ->
                        val showCloseNotification = state.sidebarConfig.showTabCloseNotification && state.sidebarConfig.showActionNotifications
                        sess.copy(
                            activeTabId = existingHomeTab.id,
                            secondarySplitTabId = null,
                            isSplitViewEnabled = false,
                            statusBannerMessage = if (showCloseNotification) "Tab closed • Returned to Home Screen" else null
                        )
                    }
                } else {
                    createNewTab("zen://home")
                }
            }
        }
    }

    fun toggleSplitView(secondaryTabId: String? = null) {
        val state = uiState.value
        // If explicit secondaryTabId was passed (e.g. from SplitTabPickerSheet), open immediately:
        if (secondaryTabId != null) {
            openSplitViewWithTabs(state.activeTabId, secondaryTabId)
            return
        }

        // If Split View is already enabled, toggle it off:
        if (state.isSplitViewEnabled) {
            sessionState.update {
                it.copy(
                    isSplitViewEnabled = false,
                    isSelectingSplitTabs = false,
                    selectedSplitTabIds = emptyList(),
                    focusedPane = FocusedPane.PRIMARY
                )
            }
            return
        }

        // If already in split-tab selection mode inside the side panel, confirm with 1 tab + Homepage:
        if (state.isSelectingSplitTabs) {
            confirmSplitSelectionWithHomepageIfSingle()
            return
        }

        // Open the side panel in Split Tab Selection Mode with the current tab pre-selected as Tab 1
        val initialSelection = listOfNotNull(state.activePrimaryTab?.id)
        sessionState.update {
            it.copy(
                isSidebarOpen = true,
                isSelectingSplitTabs = true,
                selectedSplitTabIds = initialSelection,
                activeSheet = ActiveModalSheet.NONE,
                statusBannerMessage = "Select a 2nd tab to split, or tap 'Split with Homepage'"
            )
        }
    }

    fun setSplitRatio(ratio: Float) {
        sessionState.update { it.copy(splitRatio = ratio.coerceIn(0.20f, 0.80f)) }
    }

    fun setFocusedPane(pane: FocusedPane) {
        sessionState.update {
            it.copy(
                focusedPane = pane,
                statusBannerMessage = if (pane == FocusedPane.PRIMARY) "Top pane focused" else "Bottom pane focused"
            )
        }
    }

    fun swapSplitPanes() {
        sessionState.update { sess ->
            val sec = sess.secondarySplitTabId ?: return@update sess
            sess.copy(
                activeTabId = sec,
                secondarySplitTabId = sess.activeTabId
            )
        }
    }

    fun toggleReaderMode() {
        sessionState.update { it.copy(isReaderModeEnabled = !it.isReaderModeEnabled) }
    }

    fun toggleDesktopMode(tabId: String? = null) {
        val targetTabId = tabId ?: uiState.value.focusedTab?.id ?: return
        sessionState.update { current ->
            val isCurrentlyDesktop = current.desktopModeTabIds.contains(targetTabId)
            val newSet = if (isCurrentlyDesktop) {
                current.desktopModeTabIds - targetTabId
            } else {
                current.desktopModeTabIds + targetTabId
            }
            current.copy(
                desktopModeTabIds = newSet,
                statusBannerMessage = if (!isCurrentlyDesktop) "Switched to Desktop Site" else "Switched to Mobile Site"
            )
        }
    }

    fun toggleCompactFullscreen() {
        sessionState.update {
            val next = !it.isCompactFullscreen
            it.copy(
                isCompactFullscreen = next,
                isSidebarOpen = false
            )
        }
    }

    fun openSheet(sheet: ActiveModalSheet) {
        sessionState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        sessionState.update { it.copy(activeSheet = ActiveModalSheet.NONE) }
    }

    fun updateSidebarPosition(position: SidebarPosition) {
        updateSidebarConfig { it.copy(position = position) }
    }

    fun updateSidebarWidthMode(mode: SidebarWidthMode) {
        updateSidebarConfig { it.copy(widthMode = mode) }
    }

    fun updateSidebarUiScale(scale: Float) {
        updateSidebarConfig { it.copy(sidebarUiScale = scale.coerceIn(0.75f, 1.35f)) }
    }

    fun updateUrlBarPosition(position: UrlBarPosition) {
        updateSidebarConfig { it.copy(urlBarPosition = position) }
    }

    fun updateSingleToolbarNewTabBehavior(behavior: SingleToolbarNewTabBehavior) {
        updateSidebarConfig { it.copy(singleToolbarNewTabBehavior = behavior) }
    }

    fun updateHideStatusBarFullscreen(hide: Boolean) {
        updateSidebarConfig { it.copy(hideStatusBarFullscreen = hide) }
    }

    fun updateBackSwipeSidebarMode(mode: BackSwipeSidebarMode) {
        updateSidebarConfig { it.copy(backSwipeSidebarMode = mode) }
    }

    fun updateHorizontalSwipeMode(mode: HorizontalSwipeNavigationMode) {
        updateSidebarConfig { it.copy(horizontalSwipeMode = mode) }
    }

    fun updateShowSidebarNavControls(show: Boolean) {
        updateSidebarConfig { it.copy(showSidebarNavControls = show) }
    }

    fun updateOpenInNewTab(openInNewTab: Boolean) {
        updateSidebarConfig { it.copy(openInNewTab = openInNewTab) }
    }

    fun updateOpenHistoryInNewTab(openHistoryInNewTab: Boolean) {
        updateSidebarConfig { it.copy(openHistoryInNewTab = openHistoryInNewTab) }
    }

    fun updateNotificationSettings(
        showTabCloseNotification: Boolean? = null,
        showActionNotifications: Boolean? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                showTabCloseNotification = showTabCloseNotification ?: cfg.showTabCloseNotification,
                showActionNotifications = showActionNotifications ?: cfg.showActionNotifications
            )
        }
    }

    fun updateFakeSearchConfig(
        customAiOverviewText: String? = null,
        fakeSearchQueryTrigger: String? = null,
        fakeSearchHeadline: String? = null,
        fakeSearchSnippet: String? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                customAiOverviewText = customAiOverviewText ?: cfg.customAiOverviewText,
                fakeSearchQueryTrigger = fakeSearchQueryTrigger ?: cfg.fakeSearchQueryTrigger,
                fakeSearchHeadline = fakeSearchHeadline ?: cfg.fakeSearchHeadline,
                fakeSearchSnippet = fakeSearchSnippet ?: cfg.fakeSearchSnippet
            )
        }
    }

    fun toggleSidebarToolItem(item: SidebarToolItem) {
        updateSidebarConfig { cfg ->
            val current = cfg.enabledSidebarTools.toMutableList()
            if (current.contains(item)) {
                current.remove(item)
            } else {
                current.add(item)
            }
            cfg.copy(enabledSidebarTools = current)
        }
    }

    fun moveSidebarToolItem(item: SidebarToolItem, direction: Int) {
        updateSidebarConfig { cfg ->
            val current = cfg.enabledSidebarTools.toMutableList()
            val idx = current.indexOf(item)
            if (idx == -1) return@updateSidebarConfig cfg
            val targetIdx = (idx + direction).coerceIn(0, current.lastIndex)
            if (idx != targetIdx) {
                val removed = current.removeAt(idx)
                current.add(targetIdx, removed)
            }
            cfg.copy(enabledSidebarTools = current)
        }
    }

    fun toggleTopToolbarToolItem(item: TopToolbarToolItem) {
        updateSidebarConfig { cfg ->
            val current = cfg.enabledTopToolbarTools.toMutableList()
            if (current.contains(item)) {
                current.remove(item)
            } else {
                current.add(item)
            }
            cfg.copy(enabledTopToolbarTools = current)
        }
    }

    fun moveTopToolbarToolItem(item: TopToolbarToolItem, direction: Int) {
        updateSidebarConfig { cfg ->
            val current = cfg.enabledTopToolbarTools.toMutableList()
            val idx = current.indexOf(item)
            if (idx == -1) return@updateSidebarConfig cfg
            val targetIdx = (idx + direction).coerceIn(0, current.lastIndex)
            if (idx != targetIdx) {
                val removed = current.removeAt(idx)
                current.add(targetIdx, removed)
            }
            cfg.copy(enabledTopToolbarTools = current)
        }
    }

    fun updateGestureAndSessionConfig(
        tabSwipeRight: TabSwipeAction? = null,
        tabSwipeLeft: TabSwipeAction? = null,
        newTabSwipeLeft: NewTabSwipeGestureAction? = null,
        newTabSwipeRight: NewTabSwipeGestureAction? = null,
        closePrivateOnClose: Boolean? = null,
        startupBehavior: StartupBehavior? = null,
        homepageHoursThreshold: Int? = null,
        backSwipeSidebarMode: BackSwipeSidebarMode? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                tabSwipeRightAction = tabSwipeRight ?: cfg.tabSwipeRightAction,
                tabSwipeLeftAction = tabSwipeLeft ?: cfg.tabSwipeLeftAction,
                newTabSwipeLeftAction = newTabSwipeLeft ?: cfg.newTabSwipeLeftAction,
                newTabSwipeRightAction = newTabSwipeRight ?: cfg.newTabSwipeRightAction,
                closePrivateOnAppClose = closePrivateOnClose ?: cfg.closePrivateOnAppClose,
                startupBehavior = startupBehavior ?: cfg.startupBehavior,
                homepageAfterHoursThreshold = homepageHoursThreshold ?: cfg.homepageAfterHoursThreshold,
                backSwipeSidebarMode = backSwipeSidebarMode ?: cfg.backSwipeSidebarMode
            )
        }
    }

    fun updateStartPageAppearance(
        homeScreenStyle: HomeScreenStyle? = null,
        zenCustomText: String? = null,
        zenCustomTextColorHex: Long? = null,
        zenCustomTextXFraction: Float? = null,
        zenCustomTextYFraction: Float? = null,
        gradientId: String? = null,
        imageUri: String? = null,
        clearImage: Boolean = false,
        blurEnabled: Boolean? = null,
        blurRadius: Float? = null,
        brightness: Float? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                homeScreenStyle = homeScreenStyle ?: cfg.homeScreenStyle,
                zenCustomText = zenCustomText ?: cfg.zenCustomText,
                zenCustomTextColorHex = zenCustomTextColorHex ?: cfg.zenCustomTextColorHex,
                zenCustomTextXFraction = zenCustomTextXFraction?.coerceIn(0.05f, 0.95f) ?: cfg.zenCustomTextXFraction,
                zenCustomTextYFraction = zenCustomTextYFraction?.coerceIn(0.05f, 0.92f) ?: cfg.zenCustomTextYFraction,
                startPageGradientId = gradientId ?: cfg.startPageGradientId,
                startPageImageUri = if (clearImage) null else (imageUri ?: cfg.startPageImageUri),
                startPageBlurEnabled = blurEnabled ?: cfg.startPageBlurEnabled,
                startPageBlurRadius = blurRadius ?: cfg.startPageBlurRadius,
                startPageBrightness = brightness?.coerceIn(0.15f, 1.5f) ?: cfg.startPageBrightness
            )
        }
    }

    fun updateEdgeBarSettings(
        widthDp: Float? = null,
        lengthDp: Float? = null,
        verticalOffsetFraction: Float? = null,
        hideInCompactMode: Boolean? = null,
        hideAlways: Boolean? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                edgeBarWidthDp = widthDp ?: cfg.edgeBarWidthDp,
                edgeBarLengthDp = lengthDp ?: cfg.edgeBarLengthDp,
                edgeBarVerticalOffsetFraction = verticalOffsetFraction ?: cfg.edgeBarVerticalOffsetFraction,
                hideEdgeBarInCompactMode = hideInCompactMode ?: cfg.hideEdgeBarInCompactMode,
                hideEdgeBarAlways = hideAlways ?: cfg.hideEdgeBarAlways
            )
        }
    }

    fun updateSidebarGlassAndColor(
        blurIntensity: Float? = null,
        sidebarOpacity: Float? = null,
        liquidGlassEnabled: Boolean? = null,
        colorMode: SidebarColorMode? = null,
        customColorHex: Long? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                glassBlurIntensity = blurIntensity?.coerceIn(0f, 1f) ?: cfg.glassBlurIntensity,
                sidebarOpacity = sidebarOpacity?.coerceIn(0f, 1f) ?: cfg.sidebarOpacity,
                liquidGlassEnabled = liquidGlassEnabled ?: cfg.liquidGlassEnabled,
                sidebarColorMode = colorMode ?: cfg.sidebarColorMode,
                sidebarCustomColorHex = customColorHex ?: cfg.sidebarCustomColorHex
            )
        }
    }

    fun toggleSidebarOption(
        compactAutoHide: Boolean? = null,
        showEssentials: Boolean? = null,
        showPinned: Boolean? = null,
        showWorkspaceStrip: Boolean? = null,
        showFavicons: Boolean? = null,
        borderGlow: Boolean? = null,
        glassIntensity: Float? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                compactModeAutoHide = compactAutoHide ?: cfg.compactModeAutoHide,
                showEssentialGrid = showEssentials ?: cfg.showEssentialGrid,
                showPinnedTabs = showPinned ?: cfg.showPinnedTabs,
                showWorkspaceStripInBottomBar = showWorkspaceStrip ?: cfg.showWorkspaceStripInBottomBar,
                showTabFaviconBadges = showFavicons ?: cfg.showTabFaviconBadges,
                borderGlowEnabled = borderGlow ?: cfg.borderGlowEnabled,
                glassBlurIntensity = glassIntensity ?: cfg.glassBlurIntensity
            )
        }
    }

    fun moveSidebarSection(section: SidebarSectionType, direction: Int) {
        updateSidebarConfig { cfg ->
            val currentList = cfg.sectionOrder.toMutableList()
            val index = currentList.indexOf(section)
            if (index == -1) return@updateSidebarConfig cfg
            val targetIndex = (index + direction).coerceIn(0, currentList.lastIndex)
            if (index != targetIndex) {
                val item = currentList.removeAt(index)
                currentList.add(targetIndex, item)
            }
            cfg.copy(sectionOrder = currentList)
        }
    }

    fun resetSidebarDefaults() {
        val defaultCfg = SidebarConfig(hasCompletedTutorial = true)
        localSidebarConfig.value = defaultCfg
        viewModelScope.launch {
            repository.saveSidebarConfig(defaultCfg)
        }
    }

    private var saveSidebarConfigJob: Job? = null
    private fun updateSidebarConfig(transform: (SidebarConfig) -> SidebarConfig) {
        val updated = transform(localSidebarConfig.value)
        localSidebarConfig.value = updated
        saveSidebarConfigJob?.cancel()
        saveSidebarConfigJob = viewModelScope.launch {
            delay(120)
            repository.saveSidebarConfig(updated)
        }
    }

    fun updatePrivacyToggle(
        blockTrackers: Boolean? = null,
        adBlockerLevel: AdBlockerLevel? = null,
        httpsEverywhere: Boolean? = null,
        stripParams: Boolean? = null,
        fingerprintProtection: Boolean? = null,
        blockThirdPartyCookies: Boolean? = null,
        normalSearchEngineId: String? = null,
        privateSearchEngineId: String? = null,
        deletionInterval: HistoryDeletionInterval? = null
    ) {
        val current = localPrivacyConfig.value
        val updated = current.copy(
            blockTrackersAndAds = blockTrackers ?: current.blockTrackersAndAds,
            adBlockerLevel = adBlockerLevel ?: current.adBlockerLevel,
            httpsEverywhere = httpsEverywhere ?: current.httpsEverywhere,
            stripTrackingParams = stripParams ?: current.stripTrackingParams,
            fingerprintProtection = fingerprintProtection ?: current.fingerprintProtection,
            blockThirdPartyCookies = blockThirdPartyCookies ?: current.blockThirdPartyCookies,
            normalSearchEngineId = normalSearchEngineId ?: current.normalSearchEngineId,
            privateSearchEngineId = privateSearchEngineId ?: current.privateSearchEngineId,
            historyDeletionInterval = deletionInterval ?: current.historyDeletionInterval
        )
        localPrivacyConfig.value = updated
        viewModelScope.launch {
            repository.savePrivacyConfig(updated)
        }
    }

    fun openGlance(url: String? = null) {
        val targetUrl = if (!url.isNullOrBlank()) {
            url
        } else {
            val focusedUrl = uiState.value.focusedTab?.url
            if (!focusedUrl.isNullOrBlank() && focusedUrl != "zen://home") {
                focusedUrl
            } else {
                "https://duckduckgo.com"
            }
        }
        sessionState.update { it.copy(activeGlanceUrl = targetUrl, isSidebarOpen = false) }
    }

    fun closeGlance() {
        sessionState.update { it.copy(activeGlanceUrl = null) }
    }

    fun toggleOpenLinksInGlanceAutomatically(enabled: Boolean) {
        updateSidebarConfig { it.copy(openLinksInGlanceAutomatically = enabled) }
    }

    fun openSplitViewWithUrl(url: String) {
        val state = uiState.value
        val effectivePrivate = state.isCurrentContextPrivate
        val maxSort = (state.allTabs.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val newTab = TabEntity(
            id = "tab_${UUID.randomUUID().toString().take(8)}",
            workspaceId = state.activeWorkspaceId,
            title = ZenPrivacyEngine.extractHostDisplay(url),
            url = url,
            isPinned = false,
            isEssential = false,
            isPrivate = effectivePrivate,
            sortOrder = maxSort,
            lastAccessed = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertTab(newTab)
            openSplitViewWithTabs(state.activeTabId, newTab.id)
        }
    }

    fun updateHistoryDeletionInterval(interval: HistoryDeletionInterval) {
        updatePrivacyToggle(
            blockTrackers = null,
            httpsEverywhere = null,
            stripParams = null,
            fingerprintProtection = null,
            blockThirdPartyCookies = null,
            deletionInterval = interval
        )
        applyHistoryDeletionPolicy(interval)
    }

    fun applyHistoryDeletionPolicy(overrideInterval: HistoryDeletionInterval? = null) {
        val interval = overrideInterval ?: uiState.value.privacyConfig.historyDeletionInterval
        if (interval == HistoryDeletionInterval.NEVER) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val cutoff = if (interval == HistoryDeletionInterval.ON_APP_CLOSE) {
                now
            } else {
                now - interval.intervalMillis
            }
            repository.pruneHistoryOlderThan(cutoff)
        }
    }

    fun addCustomSearchEngine(name: String, urlTemplate: String) {
        val cleanName = name.trim().ifEmpty { "Custom Search" }
        val cleanUrl = urlTemplate.trim().let {
            if (!it.startsWith("http://") && !it.startsWith("https://")) "https://$it" else it
        }
        val finalTemplate = if (cleanUrl.contains("%s")) cleanUrl else "$cleanUrl?q=%s"
        val domain = ZenPrivacyEngine.extractHostDisplay(finalTemplate)
        val engine = CustomSearchEngineEntity(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            searchUrlTemplate = finalTemplate,
            homeDomain = domain
        )
        viewModelScope.launch {
            repository.insertCustomSearchEngine(engine)
            showBanner("Added search engine '$cleanName'")
        }
    }

    fun deleteCustomSearchEngine(engineId: String) {
        viewModelScope.launch {
            repository.deleteCustomSearchEngine(engineId)
        }
    }

    fun onTrackerBlocked(event: BlockedTrackerEvent) {
        val updatedPrivacy = localPrivacyConfig.value.copy(
            totalTrackersBlocked = localPrivacyConfig.value.totalTrackersBlocked + 1
        )
        localPrivacyConfig.value = updatedPrivacy
        sessionState.update { sess ->
            val updatedList = (listOf(event) + sess.recentBlockedEvents).distinctBy { it.domain }.take(18)
            sess.copy(recentBlockedEvents = updatedList)
        }
        viewModelScope.launch {
            repository.savePrivacyConfig(updatedPrivacy)
        }
    }

    private fun recordSanitizationMetrics(strippedCount: Int, httpsUpgraded: Boolean) {
        if (strippedCount == 0 && !httpsUpgraded) return
        val current = localPrivacyConfig.value
        val updated = current.copy(
            totalParamsStripped = current.totalParamsStripped + strippedCount,
            totalHttpsUpgrades = current.totalHttpsUpgrades + (if (httpsUpgraded) 1 else 0)
        )
        localPrivacyConfig.value = updated
        viewModelScope.launch {
            repository.savePrivacyConfig(updated)
        }
    }

    fun toggleZenMod(mod: ZenModEntity) {
        viewModelScope.launch {
            repository.updateMod(mod.copy(isEnabled = !mod.isEnabled))
        }
    }

    fun installOrUninstallFirefoxExtension(extension: FirefoxExtensionEntity) {
        viewModelScope.launch {
            val nextInstalled = !extension.isInstalled
            repository.updateFirefoxExtension(
                extension.copy(
                    isInstalled = nextInstalled,
                    isEnabled = nextInstalled
                )
            )
            showBanner(
                if (nextInstalled) "Installed '${extension.name}' to Zen Browser"
                else "Removed '${extension.name}'"
            )
        }
    }

    fun toggleFirefoxExtensionEnabled(extension: FirefoxExtensionEntity) {
        if (!extension.isInstalled) return
        viewModelScope.launch {
            val nextEnabled = !extension.isEnabled
            repository.updateFirefoxExtension(extension.copy(isEnabled = nextEnabled))
            showBanner(
                if (nextEnabled) "Enabled '${extension.name}'"
                else "Disabled '${extension.name}'"
            )
        }
    }

    fun toggleFirefoxExtensionPrivateAllowed(extension: FirefoxExtensionEntity) {
        viewModelScope.launch {
            val nextPrivate = !extension.allowInPrivate
            repository.updateFirefoxExtension(extension.copy(allowInPrivate = nextPrivate))
            showBanner(
                if (nextPrivate) "Allowed '${extension.name}' in Private Tabs"
                else "Restricted '${extension.name}' from Private Tabs"
            )
        }
    }

    fun updateFirefoxExtensionDetails(
        extension: FirefoxExtensionEntity,
        matchPatterns: String,
        runAt: String,
        cssPayload: String,
        jsPayload: String
    ) {
        viewModelScope.launch {
            repository.updateFirefoxExtension(
                extension.copy(
                    matchPatterns = matchPatterns.trim().ifEmpty { "*://*/*" },
                    runAt = runAt,
                    cssPayload = cssPayload.trim(),
                    jsPayload = jsPayload.trim()
                )
            )
            showBanner("Updated '${extension.name}' configuration")
        }
    }

    fun installExtensionFromAmoPage(url: String, pageTitle: String?) {
        val parsed = runCatching { java.net.URI(url) }.getOrNull()
        if (parsed == null) {
            showBanner("Extension installs require a valid HTTPS URL")
            return
        }
        val scheme = parsed.scheme?.lowercase()
        val host = parsed.host?.lowercase()
        if (scheme != "https" || host.isNullOrBlank()) {
            showBanner("Extension installs require a valid HTTPS URL")
            return
        }

        val lower = url.lowercase()
        if (lower.endsWith(".crx") || lower.contains(".crx?")) {
            showBanner("Chrome .crx packages are not compatible with this browser engine. Import a Firefox .xpi or userscript instead.")
            return
        }

        if (lower.endsWith(".user.js") || lower.contains(".user.js?")) {
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val script = downloadExtensionText(url, 1_000_000)
                    if (!script.contains("// ==UserScript==", ignoreCase = true)) {
                        showBanner("This download is not a valid userscript")
                        return@launch
                    }
                    installUserscript(script, fallbackUrl = url, fallbackName = pageTitle)
                } catch (e: Exception) {
                    showBanner("Could not download userscript: ${e.localizedMessage ?: "network error"}")
                }
            }
            return
        }

        val isAmo = host == "addons.mozilla.org" || host.endsWith(".addons.mozilla.org")
        val isDirectXpi = parsed.path?.lowercase()?.endsWith(".xpi") == true
        if (!isAmo && !isDirectXpi) {
            showBanner("Unsupported extension source. Use Mozilla Add-ons, a Firefox .xpi, or a .user.js script.")
            return
        }

        // A direct .xpi URL is passed by the download interceptor. AMO pages are resolved
        // through Mozilla's API so we download the actual package rather than saving a fake
        // extension record that only logs to the console.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val currentExtensions = uiState.value.firefoxExtensions
                val slug = if (isAmo) {
                    val segments = parsed.path.orEmpty().split('/').filter { it.isNotBlank() }
                    val addonIndex = segments.indexOfFirst { it.equals("addon", ignoreCase = true) }
                    if (addonIndex >= 0 && addonIndex + 1 < segments.size) segments[addonIndex + 1] else ""
                } else {
                    parsed.path.orEmpty().substringAfterLast('/').removeSuffix(".xpi")
                }

                val builtIn = currentExtensions.find { ext ->
                    slug.isNotBlank() && ext.amoSlug.isNotBlank() &&
                        ext.amoSlug.equals(slug, ignoreCase = true)
                }
                if (builtIn != null) {
                    repository.updateFirefoxExtension(builtIn.copy(isInstalled = true, isEnabled = true))
                    showBanner("Enabled built-in compatibility for '${builtIn.name}'")
                    return@launch
                }

                val xpiUrl = if (isDirectXpi) {
                    url
                } else {
                    if (slug.isBlank()) {
                        showBanner("Open a specific Firefox add-on page before installing")
                        return@launch
                    }
                    val apiUrl = "https://addons.mozilla.org/api/v5/addons/addon/" +
                        java.net.URLEncoder.encode(slug, "UTF-8") + "/"
                    val metadata = org.json.JSONObject(downloadExtensionText(apiUrl, 1_000_000))
                    metadata.optJSONObject("current_version")
                        ?.optJSONArray("files")
                        ?.optJSONObject(0)
                        ?.optString("url")
                        ?.takeIf { it.startsWith("https://addons.mozilla.org/") }
                        ?: throw IllegalArgumentException("Mozilla did not provide a downloadable XPI")
                }

                val packageUri = java.net.URI(xpiUrl)
                val packageHost = packageUri.host?.lowercase()
                val packageIsAmo = packageHost == "addons.mozilla.org" ||
                    packageHost?.endsWith(".addons.mozilla.org") == true
                if (packageUri.scheme != "https" || packageHost.isNullOrBlank() ||
                    (isAmo && !isDirectXpi && !packageIsAmo)
                ) {
                    throw IllegalArgumentException("The XPI download must use HTTPS and have a valid host")
                }
                val bytes = downloadExtensionBytes(xpiUrl, 20 * 1024 * 1024)
                importFirefoxXpiPackage(
                    fileName = slug.ifBlank { "firefox-addon" } + ".xpi",
                    inputStream = java.io.ByteArrayInputStream(bytes)
                )
            } catch (e: Exception) {
                showBanner("Could not install desktop extension: ${e.localizedMessage ?: "unsupported package"}")
            }
        }
    }

    private fun downloadExtensionText(url: String, maxBytes: Int): String =
        downloadExtensionBytes(url, maxBytes).toString(Charsets.UTF_8)

    private fun downloadExtensionBytes(url: String, maxBytes: Int): ByteArray {
        val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        connection.connectTimeout = 12_000
        connection.readTimeout = 20_000
        connection.instanceFollowRedirects = true
        try {
            connection.setRequestProperty("User-Agent", "ZenBrowser-Android ExtensionInstaller")
            connection.connect()
            val status = connection.responseCode
            if (connection.url.protocol != "https") throw java.io.IOException("Extension downloads may not redirect to HTTP")
            if (status !in 200..299) throw java.io.IOException("HTTP $status")
            val declaredLength = connection.contentLengthLong
            if (declaredLength > maxBytes) throw java.io.IOException("Download exceeds the size limit")
            connection.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) throw java.io.IOException("Download exceeds the size limit")
                    output.write(buffer, 0, count)
                }
                return output.toByteArray()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun addCustomFirefoxExtension(
        name: String,
        description: String,
        matchPatterns: String,
        runAt: String,
        allowInPrivate: Boolean,
        cssPayload: String,
        jsPayload: String
    ) {
        val cleanName = name.trim().ifEmpty { "Custom WebExtension" }
        val cleanDesc = description.trim().ifEmpty { "User-installed custom Firefox WebExtension script" }
        val ext = FirefoxExtensionEntity(
            id = "ff_custom_${UUID.randomUUID().toString().take(8)}",
            name = cleanName,
            author = "User Script / XPI",
            version = "1.0.0",
            description = cleanDesc,
            category = "Custom Add-on",
            permissions = "Access your data for matching sites",
            isInstalled = true,
            isEnabled = true,
            allowInPrivate = allowInPrivate,
            runAt = runAt,
            matchPatterns = matchPatterns.trim().ifEmpty { "*://*/*" },
            cssPayload = cssPayload.trim(),
            jsPayload = jsPayload.trim()
        )
        viewModelScope.launch {
            repository.insertFirefoxExtension(ext)
            showBanner("Added custom Firefox extension '$cleanName'")
        }
    }

    fun installUserscript(rawScript: String, fallbackUrl: String = "", fallbackName: String? = null) {
        viewModelScope.launch {
            var scriptName = fallbackName ?: fallbackUrl.substringAfterLast("/").substringBefore("?").ifBlank { "Desktop Userscript" }
            var scriptVersion = "1.0.0"
            var scriptDesc = "Desktop Userscript installed into Zen Browser"
            var scriptAuthor = "Userscript Author"
            var scriptRunAt = "document_idle"
            val matchPatterns = mutableListOf<String>()

            val lines = rawScript.lines()
            var insideHeader = false
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.startsWith("// ==UserScript==", ignoreCase = true)) {
                    insideHeader = true
                    continue
                }
                if (trimmed.startsWith("// ==/UserScript==", ignoreCase = true)) {
                    insideHeader = false
                    break
                }
                if (insideHeader) {
                    when {
                        trimmed.startsWith("// @name", ignoreCase = true) -> {
                            val nameVal = trimmed.substringAfter("@name").trim()
                            if (nameVal.isNotBlank()) scriptName = nameVal
                        }
                        trimmed.startsWith("// @version", ignoreCase = true) -> {
                            val verVal = trimmed.substringAfter("@version").trim()
                            if (verVal.isNotBlank()) scriptVersion = verVal
                        }
                        trimmed.startsWith("// @description", ignoreCase = true) -> {
                            val descVal = trimmed.substringAfter("@description").trim()
                            if (descVal.isNotBlank()) scriptDesc = descVal
                        }
                        trimmed.startsWith("// @author", ignoreCase = true) -> {
                            val authVal = trimmed.substringAfter("@author").trim()
                            if (authVal.isNotBlank()) scriptAuthor = authVal
                        }
                        trimmed.startsWith("// @match", ignoreCase = true) -> {
                            val matchVal = trimmed.substringAfter("@match").trim()
                            if (matchVal.isNotBlank()) matchPatterns.add(matchVal)
                        }
                        trimmed.startsWith("// @include", ignoreCase = true) -> {
                            val incVal = trimmed.substringAfter("@include").trim()
                            if (incVal.isNotBlank()) matchPatterns.add(incVal)
                        }
                        trimmed.startsWith("// @run-at", ignoreCase = true) -> {
                            val runVal = trimmed.substringAfter("@run-at").trim()
                            if (runVal.isNotBlank()) scriptRunAt = runVal
                        }
                    }
                }
            }

            val ext = FirefoxExtensionEntity(
                id = "ff_userjs_${UUID.randomUUID().toString().take(8)}",
                name = scriptName,
                author = scriptAuthor,
                version = scriptVersion,
                description = scriptDesc,
                category = "Userscript",
                permissions = "Access your data for matching sites",
                isInstalled = true,
                isEnabled = true,
                allowInPrivate = true,
                runAt = scriptRunAt,
                matchPatterns = if (matchPatterns.isNotEmpty()) matchPatterns.joinToString(", ") else "*://*/*",
                amoSlug = fallbackUrl.substringAfterLast("/").substringBefore("?"),
                cssPayload = "",
                jsPayload = rawScript
            )
            repository.insertFirefoxExtension(ext)
            showBanner("Installed Desktop Userscript '$scriptName' v$scriptVersion")
        }
    }

    fun importFirefoxXpiPackage(fileName: String, inputStream: java.io.InputStream) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val entryMap = mutableMapOf<String, String>()
                var entryCount = 0
                var selectedBytes = 0L
                var totalArchiveBytes = 0L
                java.util.zip.ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        entryCount++
                        if (entryCount > 1_000) throw IllegalArgumentException("Archive contains too many files")
                        val normalizedName = entry.name.replace('\\', '/')
                            .removePrefix("./")
                            .trimStart('/')
                        val safePath = normalizedName.split('/').none { it == ".." }
                        val lowerName = normalizedName.lowercase()
                        val shouldRead = !entry.isDirectory && safePath &&
                            (lowerName == "manifest.json" ||
                                lowerName.endsWith(".js") || lowerName.endsWith(".css"))
                        val buffer = ByteArray(8192)
                        if (shouldRead && (entryMap.size < 120 || lowerName == "manifest.json")) {
                            val output = java.io.ByteArrayOutputStream()
                            var fileBytes = 0
                            while (true) {
                                val count = zis.read(buffer)
                                if (count < 0) break
                                fileBytes += count
                                selectedBytes += count
                                totalArchiveBytes += count
                                if (fileBytes > 512 * 1024 || selectedBytes > 8L * 1024 * 1024 ||
                                    totalArchiveBytes > 40L * 1024 * 1024
                                ) {
                                    throw IllegalArgumentException("Extension archive exceeds the import size limit")
                                }
                                output.write(buffer, 0, count)
                            }
                            entryMap[lowerName] = output.toByteArray().toString(Charsets.UTF_8)
                        } else {
                            // Count skipped entries too: ZipInputStream.closeEntry() would otherwise
                            // decompress an unbounded payload from an archive with huge ignored files.
                            while (true) {
                                val count = zis.read(buffer)
                                if (count < 0) break
                                totalArchiveBytes += count
                                if (totalArchiveBytes > 40L * 1024 * 1024) {
                                    throw IllegalArgumentException("Extension archive exceeds the import size limit")
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val manifestRaw = entryMap["manifest.json"]
                    ?: throw IllegalArgumentException("The XPI has no root manifest.json")
                val json = org.json.JSONObject(manifestRaw)
                val rawName = json.optString("name", "")
                val extName = rawName.takeIf { it.isNotBlank() && !it.startsWith("__MSG_") }
                    ?: fileName.substringBeforeLast(".").replace("-", " ").replace("_", " ").trim()
                        .ifEmpty { "Firefox XPI Add-on" }
                val extVersion = json.optString("version", "1.0.0").ifBlank { "1.0.0" }
                val rawDescription = json.optString("description", "")
                val extAuthor = json.optString("author", "Firefox XPI Package")
                val permissions = json.optJSONArray("permissions")
                val extPermissions = if (permissions != null) {
                    (0 until permissions.length()).map { permissions.optString(it) }
                        .filter { it.isNotBlank() }.distinct().joinToString(", ")
                } else {
                    "No declared permissions"
                }

                val contentScripts = json.optJSONArray("content_scripts")
                    ?: throw IllegalArgumentException(
                        "This desktop add-on has no content_scripts. Background scripts, popups, and browser APIs cannot run in Android WebView."
                    )
                if (contentScripts.length() == 0) {
                    throw IllegalArgumentException("This add-on does not define supported content scripts")
                }

                val matchesSet = linkedSetOf<String>()
                val collectedCss = StringBuilder()
                val collectedJs = StringBuilder()
                var extRunAt = "document_idle"
                for (i in 0 until contentScripts.length()) {
                    val cs = contentScripts.optJSONObject(i) ?: continue
                    when (cs.optString("run_at", "document_idle")) {
                        "document_start" -> extRunAt = "document_start"
                        "document_end" -> if (extRunAt != "document_start") extRunAt = "document_end"
                        else -> if (extRunAt != "document_start" && extRunAt != "document_end") extRunAt = "document_idle"
                    }
                    val matches = cs.optJSONArray("matches")
                    if (matches != null) {
                        for (m in 0 until matches.length()) {
                            val pattern = matches.optString(m).trim()
                            if (pattern.isNotBlank()) matchesSet.add(pattern)
                        }
                    }
                    val css = cs.optJSONArray("css")
                    if (css != null) {
                        for (c in 0 until css.length()) {
                            val pathKey = css.optString(c).replace('\\', '/').removePrefix("./").trimStart('/').lowercase()
                            entryMap[pathKey]?.let { collectedCss.append(it).append('\n') }
                        }
                    }
                    val js = cs.optJSONArray("js")
                    if (js != null) {
                        for (j in 0 until js.length()) {
                            val pathKey = js.optString(j).replace('\\', '/').removePrefix("./").trimStart('/').lowercase()
                            entryMap[pathKey]?.let { collectedJs.append(it).append('\n') }
                        }
                    }
                }

                if (matchesSet.isEmpty()) {
                    throw IllegalArgumentException("The add-on has no supported URL match patterns")
                }
                if (collectedCss.isBlank() && collectedJs.isBlank()) {
                    throw IllegalArgumentException("No supported content script files were found in the XPI")
                }

                val unsupported = mutableListOf<String>()
                if (json.has("background")) unsupported.add("background/service worker")
                if (json.has("action") || json.has("browser_action") || json.has("page_action")) unsupported.add("toolbar popup/action")
                if (json.has("options_ui") || json.has("options_page")) unsupported.add("extension settings page")
                val compatibilityNote = "Imported in compatibility mode: content scripts and CSS only. " +
                    (if (unsupported.isEmpty()) "Browser-extension APIs are not emulated." else
                        "Unsupported features: ${unsupported.distinct().joinToString()}.")
                val ext = FirefoxExtensionEntity(
                    id = "ff_xpi_${java.util.UUID.randomUUID().toString().take(8)}",
                    name = extName,
                    author = extAuthor,
                    version = extVersion,
                    description = listOf(rawDescription.takeIf { it.isNotBlank() && !it.startsWith("__MSG_") }, compatibilityNote)
                        .filterNotNull().joinToString("\n\n"),
                    category = "XPI compatibility import",
                    permissions = extPermissions,
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = false,
                    runAt = extRunAt,
                    matchPatterns = matchesSet.joinToString(", "),
                    amoSlug = fileName.substringBeforeLast("."),
                    cssPayload = collectedCss.toString().trim(),
                    jsPayload = collectedJs.toString().trim()
                )
                repository.insertFirefoxExtension(ext)
                showBanner("Imported '$extName' in content-script compatibility mode")
            } catch (e: Exception) {
                showBanner("Could not import desktop extension: ${e.localizedMessage ?: "invalid or unsupported XPI"}")
            } finally {
                runCatching { inputStream.close() }
            }
        }
    }

    fun clearBrowsingDataAndUnpinnedTabs(onCleared: () -> Unit) {
        tabHistoryStacks.clear()
        tabForwardStacks.clear()
        viewModelScope.launch {
            repository.clearUnpinnedTabs()
            onCleared()
        }
    }

    fun updateNavigationProgress(progress: Int, canBack: Boolean, canForward: Boolean) {
        val currentTab = uiState.value.focusedTab
        val tabId = currentTab?.id ?: ""
        val canGoBackEffective = canBack || (tabId.isNotBlank() && canTabHistoryGoBack(tabId))
        val canGoForwardEffective = canForward || (tabId.isNotBlank() && canTabForwardGoForward(tabId))
        sessionState.update {
            it.copy(
                pageLoadingProgress = progress,
                canGoBack = canGoBackEffective,
                canGoForward = canGoForwardEffective
            )
        }
    }

    fun toggleFindInPage(visible: Boolean) {
        sessionState.update {
            it.copy(
                isFindInPageVisible = visible,
                findInPageQuery = if (!visible) "" else it.findInPageQuery
            )
        }
    }

    fun updateFindInPageQuery(query: String) {
        sessionState.update { it.copy(findInPageQuery = query) }
    }

    fun showBanner(message: String?) {
        val cfg = uiState.value.sidebarConfig
        val filtered = if (message != null) {
            val isTabClose = message.contains("Tab closed", ignoreCase = true)
            if (isTabClose && !cfg.showTabCloseNotification) null
            else if (!cfg.showActionNotifications) null
            else message
        } else null
        sessionState.update { it.copy(statusBannerMessage = filtered) }
    }


    private var searchSuggestionJob: Job? = null

    fun clearSearchSuggestions() {
        searchSuggestionJob?.cancel()
        sessionState.update { it.copy(searchSuggestions = emptyList()) }
    }

    fun fetchSearchSuggestions(rawQuery: String) {
        val query = rawQuery.trim()
        if (query.isEmpty() || query.startsWith("zen://")) {
            clearSearchSuggestions()
            return
        }

        searchSuggestionJob?.cancel()
        searchSuggestionJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val state = uiState.value
            val localResults = mutableListOf<com.example.data.SearchSuggestionItem>()

            val bookmarkMatches = state.allTabs
                .filter { it.isBookmarked && (it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)) }
                .take(3)
                .map {
                    com.example.data.SearchSuggestionItem(
                        title = it.title.ifBlank { it.url },
                        subtitle = it.url,
                        target = it.url,
                        type = com.example.data.SuggestionType.BOOKMARK
                    )
                }
            localResults.addAll(bookmarkMatches)

            val historyMatches = state.historyEntries
                .filter { it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true) }
                .distinctBy { it.url }
                .take(3)
                .map {
                    com.example.data.SearchSuggestionItem(
                        title = it.title.ifBlank { it.url },
                        subtitle = it.url,
                        target = it.url,
                        type = com.example.data.SuggestionType.HISTORY
                    )
                }
            localResults.addAll(historyMatches)

            localResults.add(
                0,
                com.example.data.SearchSuggestionItem(
                    title = query,
                    subtitle = "Search ${state.activeSearchEngineLabel}",
                    target = query,
                    type = com.example.data.SuggestionType.SEARCH_QUERY
                )
            )

            sessionState.update { it.copy(searchSuggestions = localResults) }

            delay(150)

            try {
                val encoded = java.net.URLEncoder.encode(query, "UTF-8")
                val url = "https://duckduckgo.com/ac/?q=$encoded&type=list"
                val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 1500
                connection.readTimeout = 1500
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)")

                if (connection.responseCode == 200) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    val remoteSuggestions = mutableListOf<String>()

                    val jsonArray = org.json.JSONArray(responseText)
                    if (jsonArray.length() >= 2 && jsonArray.optJSONArray(1) != null) {
                        val items = jsonArray.getJSONArray(1)
                        for (i in 0 until items.length()) {
                            val text = items.optString(i)
                            if (text.isNotBlank() && !text.equals(query, ignoreCase = true)) {
                                remoteSuggestions.add(text)
                            }
                        }
                    } else {
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.optJSONObject(i)
                            val phrase = obj?.optString("phrase") ?: jsonArray.optString(i)
                            if (phrase.isNotBlank() && !phrase.equals(query, ignoreCase = true)) {
                                remoteSuggestions.add(phrase)
                            }
                        }
                    }

                    val webQueryItems = remoteSuggestions.distinct().take(6).map {
                        com.example.data.SearchSuggestionItem(
                            title = it,
                            subtitle = null,
                            target = it,
                            type = com.example.data.SuggestionType.SEARCH_QUERY
                        )
                    }

                    val combined = (localResults.take(1) + bookmarkMatches + historyMatches + webQueryItems).distinctBy { it.title.lowercase() }
                    sessionState.update { it.copy(searchSuggestions = combined) }
                }
            } catch (e: Exception) {
                // Ignore network exceptions for search suggestions
            }
        }
    }

    class Factory(private val repository: ZenRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZenBrowserViewModel(repository) as T
        }
    }
}
