package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BlockedTrackerEvent
import com.example.data.BackSwipeSidebarMode
import com.example.data.CustomSearchEngineEntity
import com.example.data.DownloadItemEntity
import com.example.data.FirefoxExtensionEntity
import com.example.data.HistoryEntryEntity
import com.example.data.HomeScreenStyle
import com.example.data.HomeShortcutEntity
import com.example.data.MediaPlaybackState
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
import com.example.data.UrlBarPosition
import com.example.data.WorkspaceEntity
import com.example.data.ZenModEntity
import com.example.data.ZenRepository
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.theme.ZenAccentTheme
import java.util.UUID
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
    LIBRARY
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
    val isCenterTopSearchPopupVisible: Boolean = false,
    val requestSidebarSearchFocus: Boolean = false,
    val showTutorialDialog: Boolean = false,
    val mediaPlaybackState: MediaPlaybackState = MediaPlaybackState(),
    val homeShortcuts: List<HomeShortcutEntity> = emptyList(),
    val historyEntries: List<HistoryEntryEntity> = emptyList(),
    val downloadItems: List<DownloadItemEntity> = emptyList(),
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
    val statusBannerMessage: String? = null
) {
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
        if (url.startsWith("zen://") || url.isBlank() || url == "about:blank") return false
        val trimmedPatterns = patterns.trim()
        if (trimmedPatterns.isEmpty() || trimmedPatterns == "*://*/*" || trimmedPatterns == "<all_urls>" || trimmedPatterns == "*") {
            return true
        }
        val host = ZenPrivacyEngine.extractHostDisplay(url).lowercase()
        return trimmedPatterns.split(",").any { rawPattern ->
            val p = rawPattern.trim().lowercase()
            when {
                p == "*://*/*" || p == "<all_urls>" || p == "*" -> true
                p.contains("*.") -> {
                    val domainPart = p.substringAfter("*.").substringBefore("/")
                    host == domainPart || host.endsWith(".$domainPart")
                }
                else -> {
                    val clean = p.removePrefix("https://").removePrefix("http://").removePrefix("*://").substringBefore("/")
                    clean.isEmpty() || host.contains(clean)
                }
            }
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
    val statusBannerMessage: String? = null
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
        combine(repository.mods, repository.firefoxExtensions, repository.history, repository.downloads) { mods, extensions, hist, dls ->
            tuple4(mods, extensions, hist, dls)
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
    ) { workspaces, tabs, (mods, extensions, hist, dls), sidebarCfg, (privacyCfg, customEngines, shortcuts, session) ->
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
            isCenterTopSearchPopupVisible = session.isCenterTopSearchPopupVisible,
            requestSidebarSearchFocus = session.requestSidebarSearchFocus,
            showTutorialDialog = session.showTutorialDialog,
            mediaPlaybackState = session.mediaPlaybackState,
            homeShortcuts = shortcuts,
            historyEntries = hist,
            downloadItems = dls,
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
            statusBannerMessage = session.statusBannerMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZenBrowserUiState()
    )

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private fun <A, B, C, D> tuple4(a: A, b: B, c: C, d: D) = Quad(a, b, c, d)

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
        }
    }

    fun onAppBackgroundedOrClosed() {
        val cfg = localSidebarConfig.value
        viewModelScope.launch {
            if (cfg.closePrivateOnAppClose) {
                repository.deletePrivateTabsAndWorkspaces()
            }
            // Do not unload tabs when exiting the app so user's active tabs remain loaded
            val updated = cfg.copy(lastSessionTimestamp = System.currentTimeMillis())
            localSidebarConfig.value = updated
            repository.saveSidebarConfig(updated)
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
        sessionState.update { it.copy(isSidebarOpen = !it.isSidebarOpen) }
    }

    fun setSidebarOpen(open: Boolean) {
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

    fun onNewTabButtonClicked() {
        val cfg = localSidebarConfig.value
        if (cfg.urlBarPosition == UrlBarPosition.SIDEBAR_ONLY) {
            val state = uiState.value
            val effectivePrivate = state.activeWorkspace?.isPrivate == true
            val newTab = TabEntity(
                id = "tab_${UUID.randomUUID().toString().take(8)}",
                workspaceId = state.activeWorkspaceId,
                title = if (effectivePrivate) "Private Tab" else "New Tab",
                url = "zen://home",
                isPinned = false,
                isEssential = false,
                isPrivate = effectivePrivate,
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

        viewModelScope.launch {
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
        isPrivate: Boolean = false
    ) {
        val state = uiState.value
        val effectivePrivate = isPrivate || (state.activeWorkspace?.isPrivate == true)
        val sanitized = ZenPrivacyEngine.resolveInputToUrl(
            rawInput = initialInput,
            privacyConfig = state.privacyConfig,
            isPrivateContext = effectivePrivate,
            customEngines = state.customSearchEngines
        )
        val title = when {
            sanitized.finalUrl == "zen://home" -> if (effectivePrivate) "Private Tab" else "New Tab"
            else -> ZenPrivacyEngine.extractHostDisplay(sanitized.finalUrl)
        }
        val maxSortOrder = (state.allTabs.maxOfOrNull { it.sortOrder } ?: 0) + 1
        val newTab = TabEntity(
            id = "tab_${UUID.randomUUID().toString().take(8)}",
            workspaceId = state.activeWorkspaceId,
            title = title,
            url = sanitized.finalUrl,
            isPinned = pinToWorkspace,
            isEssential = false,
            isPrivate = effectivePrivate,
            sortOrder = maxSortOrder,
            lastAccessed = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.insertTab(newTab)
            recordSanitizationMetrics(sanitized.strippedParamsCount, sanitized.upgradedToHttps)
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
        }
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
        sessionState.update {
            it.copy(
                mediaPlaybackState = MediaPlaybackState(
                    isActive = isActive,
                    isPlaying = isPlaying,
                    title = title.ifBlank { domain },
                    domain = domain,
                    tabId = tabId
                )
            )
        }
    }

    fun dismissMediaPlaybackBar() {
        sessionState.update {
            it.copy(mediaPlaybackState = MediaPlaybackState(isActive = false))
        }
    }

    fun navigateFocusedTab(rawInput: String) {
        val state = uiState.value
        val targetTab = state.focusedTab
        val isPrivate = state.isCurrentContextPrivate
        val sanitized = ZenPrivacyEngine.resolveInputToUrl(
            rawInput = rawInput,
            privacyConfig = state.privacyConfig,
            isPrivateContext = isPrivate,
            customEngines = state.customSearchEngines
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
        viewModelScope.launch {
            repository.deleteTab(tabId)
            if (remainingTabs.isEmpty()) {
                createNewTab("zen://home")
            } else {
                sessionState.update { sess ->
                    val nextPrimary = if (sess.activeTabId == tabId) {
                        remainingTabs.firstOrNull { it.workspaceId == sess.activeWorkspaceId && !it.isEssential }?.id
                            ?: remainingTabs.first().id
                    } else sess.activeTabId

                    val nextSecondary = if (sess.secondarySplitTabId == tabId) null else sess.secondarySplitTabId
                    val updatedMedia = if (sess.mediaPlaybackState.tabId == tabId) {
                        MediaPlaybackState(isActive = false)
                    } else sess.mediaPlaybackState

                    sess.copy(
                        activeTabId = nextPrimary,
                        secondarySplitTabId = nextSecondary,
                        isSplitViewEnabled = sess.isSplitViewEnabled && nextSecondary != null,
                        mediaPlaybackState = updatedMedia
                    )
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

    fun updateShowSidebarNavControls(show: Boolean) {
        updateSidebarConfig { it.copy(showSidebarNavControls = show) }
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
        hideInCompactMode: Boolean? = null
    ) {
        updateSidebarConfig { cfg ->
            cfg.copy(
                edgeBarWidthDp = widthDp ?: cfg.edgeBarWidthDp,
                edgeBarLengthDp = lengthDp ?: cfg.edgeBarLengthDp,
                edgeBarVerticalOffsetFraction = verticalOffsetFraction ?: cfg.edgeBarVerticalOffsetFraction,
                hideEdgeBarInCompactMode = hideInCompactMode ?: cfg.hideEdgeBarInCompactMode
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

    private fun updateSidebarConfig(transform: (SidebarConfig) -> SidebarConfig) {
        val updated = transform(localSidebarConfig.value)
        localSidebarConfig.value = updated
        viewModelScope.launch {
            repository.saveSidebarConfig(updated)
        }
    }

    fun updatePrivacyToggle(
        blockTrackers: Boolean? = null,
        httpsEverywhere: Boolean? = null,
        stripParams: Boolean? = null,
        fingerprintProtection: Boolean? = null,
        blockThirdPartyCookies: Boolean? = null,
        normalSearchEngineId: String? = null,
        privateSearchEngineId: String? = null
    ) {
        val current = localPrivacyConfig.value
        val updated = current.copy(
            blockTrackersAndAds = blockTrackers ?: current.blockTrackersAndAds,
            httpsEverywhere = httpsEverywhere ?: current.httpsEverywhere,
            stripTrackingParams = stripParams ?: current.stripTrackingParams,
            fingerprintProtection = fingerprintProtection ?: current.fingerprintProtection,
            blockThirdPartyCookies = blockThirdPartyCookies ?: current.blockThirdPartyCookies,
            normalSearchEngineId = normalSearchEngineId ?: current.normalSearchEngineId,
            privateSearchEngineId = privateSearchEngineId ?: current.privateSearchEngineId
        )
        localPrivacyConfig.value = updated
        viewModelScope.launch {
            repository.savePrivacyConfig(updated)
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
        val lower = url.lowercase()
        val existingList = uiState.value.firefoxExtensions
        val matchedBuiltIn = existingList.find {
            it.amoSlug.isNotBlank() && lower.contains(it.amoSlug.lowercase())
        }
        viewModelScope.launch {
            if (matchedBuiltIn != null) {
                repository.updateFirefoxExtension(
                    matchedBuiltIn.copy(isInstalled = true, isEnabled = true)
                )
                showBanner("Installed '${matchedBuiltIn.name}' from Mozilla Add-ons")
            } else {
                val slug = lower.substringAfter("/addon/", "").substringBefore("/").substringBefore("?")
                val cleanTitle = pageTitle
                    ?.substringBefore(" – Get this Extension")
                    ?.substringBefore(" — Get this Extension")
                    ?.substringBefore(" - Android")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: slug.replace("-", " ").replaceFirstChar { it.uppercase() }.ifBlank { "Mozilla Add-on" }

                val ext = FirefoxExtensionEntity(
                    id = "ff_amo_${slug.ifBlank { UUID.randomUUID().toString().take(6) }}",
                    name = cleanTitle,
                    author = "Mozilla Add-ons (AMO)",
                    version = "1.0.0",
                    description = "Installed directly from addons.mozilla.org ($slug)",
                    category = "Mozilla Add-on",
                    permissions = "Access your data for all websites",
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = "document_idle",
                    matchPatterns = "*://*/*",
                    amoSlug = slug,
                    cssPayload = "",
                    jsPayload = "console.log('Zen AMO Extension Active: ${cleanTitle.replace("'", "")}');"
                )
                repository.insertFirefoxExtension(ext)
                showBanner("Installed '$cleanTitle' to Zen Browser")
            }
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

    fun importFirefoxXpiPackage(fileName: String, inputStream: java.io.InputStream) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val entryMap = mutableMapOf<String, String>()
                java.util.zip.ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (!entry.isDirectory && (
                                name.equals("manifest.json", ignoreCase = true) ||
                                    name.endsWith(".js", ignoreCase = true) ||
                                    name.endsWith(".css", ignoreCase = true)
                                ) && entryMap.size < 40
                        ) {
                            val bytes = zis.readBytes()
                            if (bytes.size <= 512_000) {
                                entryMap[name.removePrefix("./")] = bytes.toString(Charsets.UTF_8)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                val manifestRaw = entryMap["manifest.json"]
                    ?: entryMap.entries.firstOrNull { it.key.endsWith("manifest.json", ignoreCase = true) }?.value

                var extName = fileName.substringBeforeLast(".").replace("-", " ").replace("_", " ").trim()
                    .ifEmpty { "Firefox XPI Add-on" }
                var extVersion = "1.0.0"
                var extDesc = "Installed from local Firefox XPI package ($fileName)"
                var extAuthor = "Firefox XPI Package"
                var extPermissions = "Access your data for matching sites"
                var extMatchPatterns = "*://*/*"
                var extRunAt = "document_idle"
                val collectedCss = StringBuilder()
                val collectedJs = StringBuilder()

                if (!manifestRaw.isNullOrBlank()) {
                    runCatching {
                        val json = org.json.JSONObject(manifestRaw)
                        val rawName = json.optString("name", "")
                        if (rawName.isNotBlank() && !rawName.startsWith("__MSG_")) {
                            extName = rawName
                        }
                        val rawVer = json.optString("version", "")
                        if (rawVer.isNotBlank()) {
                            extVersion = rawVer
                        }
                        val rawDesc = json.optString("description", "")
                        if (rawDesc.isNotBlank() && !rawDesc.startsWith("__MSG_")) {
                            extDesc = rawDesc
                        }
                        val rawAuthor = json.optString("author", "")
                        if (rawAuthor.isNotBlank()) {
                            extAuthor = rawAuthor
                        }
                        val permsArr = json.optJSONArray("permissions")
                        if (permsArr != null && permsArr.length() > 0) {
                            val permsList = mutableListOf<String>()
                            for (i in 0 until permsArr.length()) {
                                permsList.add(permsArr.optString(i))
                            }
                            extPermissions = permsList.filter { it.isNotBlank() }.joinToString(", ")
                        }

                        val contentScripts = json.optJSONArray("content_scripts")
                        if (contentScripts != null && contentScripts.length() > 0) {
                            val matchesSet = mutableSetOf<String>()
                            for (i in 0 until contentScripts.length()) {
                                val cs = contentScripts.optJSONObject(i) ?: continue
                                val runAtVal = cs.optString("run_at", "")
                                if (runAtVal.isNotBlank()) extRunAt = runAtVal
                                val matchesArr = cs.optJSONArray("matches")
                                if (matchesArr != null) {
                                    for (m in 0 until matchesArr.length()) {
                                        val pat = matchesArr.optString(m)
                                        if (pat.isNotBlank()) matchesSet.add(pat)
                                    }
                                }
                                val cssArr = cs.optJSONArray("css")
                                if (cssArr != null) {
                                    for (c in 0 until cssArr.length()) {
                                        val path = cssArr.optString(c).removePrefix("./").removePrefix("/")
                                        entryMap[path]?.let { collectedCss.append(it).append("\n") }
                                    }
                                }
                                val jsArr = cs.optJSONArray("js")
                                if (jsArr != null) {
                                    for (j in 0 until jsArr.length()) {
                                        val path = jsArr.optString(j).removePrefix("./").removePrefix("/")
                                        entryMap[path]?.let { collectedJs.append(it).append("\n") }
                                    }
                                }
                            }
                            if (matchesSet.isNotEmpty()) {
                                extMatchPatterns = matchesSet.joinToString(", ")
                            }
                        }
                    }
                }

                // Fallback: if no content_scripts were explicitly listed, include top-level .css/.js files
                if (collectedCss.isBlank()) {
                    entryMap.filter { it.key.endsWith(".css", ignoreCase = true) }.values.take(3).forEach {
                        collectedCss.append(it).append("\n")
                    }
                }
                if (collectedJs.isBlank()) {
                    entryMap.filter {
                        it.key.endsWith(".js", ignoreCase = true) && !it.key.contains("background", ignoreCase = true)
                    }.values.take(3).forEach {
                        collectedJs.append(it).append("\n")
                    }
                }

                val ext = FirefoxExtensionEntity(
                    id = "ff_xpi_${UUID.randomUUID().toString().take(8)}",
                    name = extName,
                    author = extAuthor,
                    version = extVersion,
                    description = extDesc,
                    category = "XPI Add-on",
                    permissions = extPermissions,
                    isInstalled = true,
                    isEnabled = true,
                    allowInPrivate = true,
                    runAt = extRunAt,
                    matchPatterns = extMatchPatterns,
                    amoSlug = fileName.substringBeforeLast("."),
                    cssPayload = collectedCss.toString().trim(),
                    jsPayload = collectedJs.toString().trim()
                )
                repository.insertFirefoxExtension(ext)
                showBanner("Installed '$extName' v$extVersion from XPI")
            } catch (e: Exception) {
                showBanner("Could not parse XPI file: ${e.localizedMessage ?: "Invalid archive"}")
            }
        }
    }

    fun clearBrowsingDataAndUnpinnedTabs(onCleared: () -> Unit) {
        viewModelScope.launch {
            repository.clearUnpinnedTabs()
            onCleared()
        }
    }

    fun updateNavigationProgress(progress: Int, canBack: Boolean, canForward: Boolean) {
        sessionState.update {
            it.copy(
                pageLoadingProgress = progress,
                canGoBack = canBack,
                canGoForward = canForward
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
        sessionState.update { it.copy(statusBannerMessage = message) }
    }

    class Factory(private val repository: ZenRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ZenBrowserViewModel(repository) as T
        }
    }
}
