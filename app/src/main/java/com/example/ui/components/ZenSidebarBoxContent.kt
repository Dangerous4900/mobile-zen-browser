package com.example.ui.components

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DownloadItemEntity
import com.example.data.HistoryEntryEntity
import com.example.data.OfflinePageEntity
import com.example.data.SidebarBoxTab
import com.example.data.TabEntity
import com.example.data.WorkspaceEntity
import com.example.downloads.ZenDownloader
import com.example.privacy.ZenPrivacyEngine
import com.example.ui.ZenBrowserUiState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ZenSidebarBoxContent(
    uiState: ZenBrowserUiState,
    isLiquidGlass: Boolean,
    onExitBoxMode: () -> Unit,
    onSelectBoxTab: (SidebarBoxTab) -> Unit,
    onOpenSettings: () -> Unit,
    onNavigateUrl: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onSelectWorkspace: (String) -> Unit,
    onCreateNewTabInWorkspace: (String) -> Unit,
    onManageWorkspaces: () -> Unit,
    onToggleBookmarkTab: (TabEntity) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit,
    onClearRecentHistory: (Int) -> Unit = {},
    onClearAllHistory: () -> Unit,
    onDeleteDownloadItem: (String) -> Unit,
    onPauseDownload: (String) -> Unit = {},
    onResumeDownload: (String) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onClearAllDownloads: () -> Unit,
    onOpenOfflinePage: (OfflinePageEntity) -> Unit,
    onDeleteOfflinePage: (String) -> Unit,
    onClearAllOfflinePages: () -> Unit,
    onSaveCurrentPageOffline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBoxTab = uiState.activeSidebarBoxTab
    var searchQuery by remember(activeBoxTab) { mutableStateOf("") }
    var filterModeIndex by remember(activeBoxTab) { mutableIntStateOf(0) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    val focusedTab = uiState.focusedTab
    val canSaveCurrentOffline = focusedTab != null &&
        focusedTab.url.isNotBlank() &&
        !focusedTab.url.startsWith("zen://")

    Column(modifier = modifier.fillMaxSize()) {
        // Top Header Row: Back to Tabs + Title + Quick Action + Settings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onExitBoxMode,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("box_mode_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to tabs",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (activeBoxTab) {
                        SidebarBoxTab.OFFLINE -> "Offline Pages"
                        SidebarBoxTab.DOWNLOADS -> "Downloads"
                        SidebarBoxTab.BOOKMARKS -> "Bookmarks"
                        SidebarBoxTab.SPACES -> "Spaces"
                        SidebarBoxTab.HISTORY -> "History"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                when (activeBoxTab) {
                    SidebarBoxTab.OFFLINE -> {
                        if (canSaveCurrentOffline) {
                            Surface(
                                onClick = onSaveCurrentPageOffline,
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                                modifier = Modifier
                                    .height(30.dp)
                                    .testTag("box_save_offline_top_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Save",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        } else if (uiState.offlinePages.isNotEmpty()) {
                            IconButton(
                                onClick = onClearAllOfflinePages,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Offline Pages",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    SidebarBoxTab.DOWNLOADS -> {
                        if (uiState.downloadItems.isNotEmpty()) {
                            IconButton(
                                onClick = onClearAllDownloads,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear Downloads",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    SidebarBoxTab.BOOKMARKS -> {
                        if (canSaveCurrentOffline && focusedTab != null) {
                            IconButton(
                                onClick = { onToggleBookmarkTab(focusedTab) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (focusedTab.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkAdd,
                                    contentDescription = if (focusedTab.isBookmarked) "Remove Bookmark" else "Bookmark Current Tab",
                                    tint = if (focusedTab.isBookmarked) Color(0xFF60A5FA) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    SidebarBoxTab.SPACES -> {
                        Surface(
                            onClick = onManageWorkspaces,
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                            modifier = Modifier
                                .height(30.dp)
                                .testTag("box_manage_spaces_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Space",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    SidebarBoxTab.HISTORY -> {
                        if (uiState.historyEntries.isNotEmpty()) {
                            IconButton(
                                onClick = { showClearHistoryConfirm = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("box_history_clear_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Clear History",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("box_mode_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFFD4D4D8),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Search + Filter Bar (inspired by screenshots)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isLiquidGlass) Color.White.copy(alpha = 0.10f) else Color(0xFF16161B),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner ->
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Search ${activeBoxTab.label}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF71717A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("box_search_input")
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            if (activeBoxTab == SidebarBoxTab.HISTORY || activeBoxTab == SidebarBoxTab.DOWNLOADS || activeBoxTab == SidebarBoxTab.SPACES) {
                val filterLabel = when (activeBoxTab) {
                    SidebarBoxTab.HISTORY -> if (filterModeIndex % 2 == 0) "Filter" else "Today"
                    SidebarBoxTab.DOWNLOADS -> when (filterModeIndex % 3) {
                        1 -> "Active"
                        2 -> "Done"
                        else -> "Filter"
                    }
                    SidebarBoxTab.SPACES -> if (filterModeIndex % 2 == 0) "All" else "Current"
                    else -> "Filter"
                }
                val isFiltered = when (activeBoxTab) {
                    SidebarBoxTab.DOWNLOADS -> filterModeIndex % 3 != 0
                    else -> filterModeIndex % 2 != 0
                }
                Surface(
                    onClick = { filterModeIndex++ },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFiltered) Color.White.copy(alpha = 0.20f) else Color(0xFF16161B),
                    border = BorderStroke(1.dp, if (isFiltered) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("box_filter_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (isFiltered) Color.White else Color(0xFFD4D4D8),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = filterLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isFiltered) Color.White else Color(0xFFD4D4D8),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Main Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeBoxTab) {
                SidebarBoxTab.OFFLINE -> {
                    BoxOfflinePagesList(
                        pages = uiState.offlinePages,
                        searchQuery = searchQuery,
                        canSaveCurrentOffline = canSaveCurrentOffline,
                        onSaveCurrentPageOffline = onSaveCurrentPageOffline,
                        onOpenOfflinePage = onOpenOfflinePage,
                        onDeleteOfflinePage = onDeleteOfflinePage
                    )
                }

                SidebarBoxTab.DOWNLOADS -> {
                    BoxDownloadsList(
                        downloads = uiState.downloadItems,
                        searchQuery = searchQuery,
                        filterMode = filterModeIndex % 3,
                        onPauseDownload = onPauseDownload,
                        onResumeDownload = onResumeDownload,
                        onCancelDownload = onCancelDownload,
                        onDeleteDownloadItem = onDeleteDownloadItem
                    )
                }

                SidebarBoxTab.BOOKMARKS -> {
                    BoxBookmarksList(
                        bookmarks = uiState.bookmarkedTabs,
                        focusedTab = focusedTab,
                        canBookmarkCurrent = canSaveCurrentOffline,
                        searchQuery = searchQuery,
                        onOpenBookmark = { tab ->
                            onNavigateUrl(tab.url)
                        },
                        onToggleBookmark = onToggleBookmarkTab
                    )
                }

                SidebarBoxTab.SPACES -> {
                    BoxSpacesOverviewList(
                        workspaces = uiState.workspaces,
                        allTabs = uiState.allTabs,
                        activeWorkspaceId = uiState.activeWorkspaceId,
                        activeTabId = uiState.activeTabId,
                        searchQuery = searchQuery,
                        onlyActiveSpace = (filterModeIndex % 2) == 1,
                        onSelectWorkspace = onSelectWorkspace,
                        onSelectTab = onSelectTab,
                        onCloseTab = onCloseTab,
                        onCreateNewTabInWorkspace = onCreateNewTabInWorkspace,
                        onManageWorkspaces = onManageWorkspaces
                    )
                }

                SidebarBoxTab.HISTORY -> {
                    BoxHistoryList(
                        historyEntries = uiState.historyEntries,
                        searchQuery = searchQuery,
                        todayOnly = (filterModeIndex % 2) == 1,
                        openInNewTab = uiState.sidebarConfig.openHistoryInNewTab || uiState.sidebarConfig.openInNewTab,
                        onNavigateUrl = onNavigateUrl,
                        onOpenInNewTab = onOpenInNewTab,
                        onDeleteHistoryEntry = onDeleteHistoryEntry
                    )
                }
            }
        }

        // Bottom Navigation Rail inside the Side Panel (5 icons cleanly aligned at the bottom)
        HorizontalDivider(
            color = if (isLiquidGlass) Color.White.copy(alpha = 0.16f) else Color(0xFF222226),
            thickness = 1.dp,
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val navItems = listOf(
                Triple(SidebarBoxTab.OFFLINE, "Offline", Icons.Default.CloudOff),
                Triple(SidebarBoxTab.DOWNLOADS, "Downloads", Icons.Default.Download),
                Triple(SidebarBoxTab.BOOKMARKS, "Bookmarks", Icons.Default.Bookmark),
                Triple(SidebarBoxTab.SPACES, "Spaces", Icons.Default.Layers),
                Triple(SidebarBoxTab.HISTORY, "History", Icons.Default.History)
            )
            navItems.forEach { (tab, label, icon) ->
                val isSelected = activeBoxTab == tab
                Surface(
                    onClick = { onSelectBoxTab(tab) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color.White.copy(alpha = 0.16f) else Color.Transparent,
                    border = if (isSelected) BorderStroke(1.dp, Color.White.copy(alpha = 0.24f)) else null,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .testTag("box_nav_${tab.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (isSelected) Color.White else Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            lineHeight = 10.sp,
                            letterSpacing = (-0.2).sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFFA1A1AA),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Erase Browsing History", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose what you want to erase:",
                        color = Color(0xFFA1A1AA),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Surface(
                        onClick = {
                            showClearHistoryConfirm = false
                            onClearRecentHistory(1)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF23232A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("history_erase_recent_hour")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Erase recent history (Last hour)",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            showClearHistoryConfirm = false
                            onClearRecentHistory(24)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF23232A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("history_erase_recent_24h")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Erase recent history (Last 24 hours)",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Surface(
                        onClick = {
                            showClearHistoryConfirm = false
                            onClearAllHistory()
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF331B1E),
                        border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("history_erase_everything")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "Erase everything",
                                color = Color(0xFFF87171),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) {
                    Text("Cancel", color = Color(0xFFA1A1AA))
                }
            },
            containerColor = Color(0xFF18181D)
        )
    }
}

@Composable
private fun BoxHistoryList(
    historyEntries: List<HistoryEntryEntity>,
    searchQuery: String,
    todayOnly: Boolean,
    openInNewTab: Boolean,
    onNavigateUrl: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onDeleteHistoryEntry: (String) -> Unit
) {
    val startOfToday = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfYesterday = startOfToday - 86_400_000L

    val filtered = remember(historyEntries, searchQuery, todayOnly, startOfToday) {
        historyEntries.filter { entry ->
            val matchesTime = !todayOnly || entry.timestamp >= startOfToday
            val matchesQuery = searchQuery.isBlank() ||
                entry.title.contains(searchQuery, ignoreCase = true) ||
                entry.url.contains(searchQuery, ignoreCase = true)
            matchesTime && matchesQuery
        }
    }

    if (filtered.isEmpty()) {
        BoxEmptyState(
            icon = Icons.Default.History,
            title = if (searchQuery.isNotBlank()) "No Matching History" else "No Browsing History",
            subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Visited sites in regular workspaces appear here."
        )
        return
    }

    val grouped = remember(filtered, startOfToday, startOfYesterday) {
        filtered.groupBy { entry ->
            when {
                entry.timestamp >= startOfToday -> "Today"
                entry.timestamp >= startOfYesterday -> "Yesterday"
                else -> "Earlier"
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        listOf("Today", "Yesterday", "Earlier").forEach { sectionLabel ->
            val sectionItems = grouped[sectionLabel].orEmpty()
            if (sectionItems.isNotEmpty()) {
                item(key = "hist_hdr_$sectionLabel") {
                    Text(
                        text = sectionLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }
                items(sectionItems, key = { it.id }) { entry ->
                    val host = ZenPrivacyEngine.extractHostDisplay(entry.url)
                    val letter = (entry.title.firstOrNull() ?: host.firstOrNull() ?: '•').uppercase()
                    Surface(
                        onClick = {
                            if (openInNewTab) onOpenInNewTab(entry.url) else onNavigateUrl(entry.url)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.04f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("box_history_item_${entry.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.title.ifBlank { host },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFF4F4F6),
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = host.ifBlank { entry.url },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA1A1AA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { onOpenInNewTab(entry.url) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in new tab",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteHistoryEntry(entry.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete history entry",
                                    tint = Color(0xFF71717A),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxDownloadsList(
    downloads: List<DownloadItemEntity>,
    searchQuery: String,
    filterMode: Int,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownloadItem: (String) -> Unit
) {
    val context = LocalContext.current
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val filtered = remember(downloads, searchQuery, filterMode) {
        downloads.filter { item ->
            val statusLower = item.status.lowercase()
            val matchesFilter = when (filterMode) {
                1 -> statusLower == "downloading" || statusLower == "paused"
                2 -> statusLower == "completed"
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() ||
                item.fileName.contains(searchQuery, ignoreCase = true) ||
                item.url.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesQuery
        }
    }

    if (filtered.isEmpty()) {
        BoxEmptyState(
            icon = Icons.Default.Download,
            title = if (searchQuery.isNotBlank()) "No Matching Downloads" else "No Downloads Yet",
            subtitle = if (searchQuery.isNotBlank()) "Try another search term" else "Downloaded files and images will appear here with live progress."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        items(filtered, key = { it.id }) { item ->
            val isDownloading = item.status.equals("downloading", ignoreCase = true)
            val isPaused = item.status.equals("paused", ignoreCase = true)
            val isActiveDownload = isDownloading || isPaused
            val fileIcon = when {
                item.mimeType.startsWith("image/") -> Icons.Default.Image
                item.mimeType.startsWith("video/") -> Icons.Default.Movie
                item.mimeType.startsWith("audio/") -> Icons.Default.AudioFile
                isDownloading -> Icons.Default.Download
                isPaused -> Icons.Default.Pause
                else -> Icons.Default.InsertDriveFile
            }
            val host = item.sourceDomain.ifBlank { ZenPrivacyEngine.extractHostDisplay(item.url) }
            Surface(
                onClick = {
                    runCatching {
                        context.startActivity(
                            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF141419).copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("box_download_item_${item.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 9.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = fileIcon,
                                contentDescription = null,
                                tint = if (isPaused) Color(0xFFFBBF24) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isActiveDownload) {
                                val frac = if (item.totalBytes > 0) {
                                    (item.downloadedBytes.toFloat() / item.totalBytes).coerceIn(0f, 1f)
                                } else 0f
                                Spacer(modifier = Modifier.height(4.dp))
                                if (item.totalBytes > 0) {
                                    LinearProgressIndicator(
                                        progress = { frac },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (isPaused) Color(0xFFFBBF24) else Color(0xFF60A5FA),
                                        trackColor = Color.White.copy(alpha = 0.12f)
                                    )
                                } else if (isPaused) {
                                    LinearProgressIndicator(
                                        progress = { 0f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFFFBBF24),
                                        trackColor = Color.White.copy(alpha = 0.12f)
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF60A5FA),
                                        trackColor = Color.White.copy(alpha = 0.12f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = buildString {
                                        if (isPaused) append("Paused • ")
                                        append(ZenDownloader.formatBytes(item.downloadedBytes))
                                        if (item.totalBytes > 0) append("/${ZenDownloader.formatBytes(item.totalBytes)}")
                                        if (!isPaused && item.speedBytesPerSec > 0) {
                                            append(" • ${ZenDownloader.formatBytes(item.speedBytesPerSec)}/s")
                                        }
                                        if (!isPaused && item.etaSeconds > 0) append(" • ${item.etaSeconds}s")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isPaused) Color(0xFFFDE68A) else Color(0xFFA1A1AA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                val subtitle = buildString {
                                    if (item.totalBytes > 0) {
                                        append(ZenDownloader.formatBytes(item.totalBytes))
                                        append(" • ")
                                    }
                                    if (host.isNotBlank()) {
                                        append(host)
                                        append(" • ")
                                    }
                                    append(timeFormatter.format(Date(item.timestamp)))
                                }
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFA1A1AA),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (!isActiveDownload) {
                            IconButton(
                                onClick = { onDeleteDownloadItem(item.id) },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("box_download_remove_${item.id}")
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
                    if (isActiveDownload) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    if (isPaused) onResumeDownload(item.id) else onPauseDownload(item.id)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("box_download_pause_resume_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = if (isPaused) "Resume download" else "Pause download",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isPaused) "Resume" else "Pause",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                            Surface(
                                onClick = { onCancelDownload(item.id) },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF331B1E),
                                border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("box_download_cancel_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel download",
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFF87171),
                                        maxLines = 1
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

@Composable
private fun BoxBookmarksList(
    bookmarks: List<TabEntity>,
    focusedTab: TabEntity?,
    canBookmarkCurrent: Boolean,
    searchQuery: String,
    onOpenBookmark: (TabEntity) -> Unit,
    onToggleBookmark: (TabEntity) -> Unit
) {
    val filtered = remember(bookmarks, searchQuery) {
        if (searchQuery.isBlank()) bookmarks
        else bookmarks.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        if (canBookmarkCurrent && focusedTab != null && !focusedTab.isBookmarked) {
            item(key = "bookmark_current_banner") {
                Surface(
                    onClick = { onToggleBookmark(focusedTab) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("box_bookmark_current_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Bookmark current tab",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "bookmarks_empty") {
                BoxEmptyState(
                    icon = Icons.Default.BookmarkBorder,
                    title = if (searchQuery.isNotBlank()) "No Matching Bookmarks" else "No Bookmarks Yet",
                    subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Tap 'Bookmark current tab' or long-press any tab to save it."
                )
            }
        } else {
            items(filtered, key = { it.id }) { tab ->
                val host = ZenPrivacyEngine.extractHostDisplay(tab.url)
                val letter = (tab.title.firstOrNull() ?: host.firstOrNull() ?: 'B').uppercase()
                Surface(
                    onClick = { onOpenBookmark(tab) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("box_bookmark_item_${tab.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tab.title.ifBlank { host },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = host.ifBlank { tab.url },
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onToggleBookmark(tab) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Bookmark",
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxOfflinePagesList(
    pages: List<OfflinePageEntity>,
    searchQuery: String,
    canSaveCurrentOffline: Boolean,
    onSaveCurrentPageOffline: () -> Unit,
    onOpenOfflinePage: (OfflinePageEntity) -> Unit,
    onDeleteOfflinePage: (String) -> Unit
) {
    val filtered = remember(pages, searchQuery) {
        if (searchQuery.isBlank()) pages
        else pages.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        if (canSaveCurrentOffline) {
            item(key = "save_offline_banner") {
                Surface(
                    onClick = onSaveCurrentPageOffline,
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF818CF8).copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color(0xFF818CF8).copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("box_save_current_offline_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFA5B4FC),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save current page for offline reading",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item(key = "offline_empty") {
                BoxEmptyState(
                    icon = Icons.Default.CloudOff,
                    title = if (searchQuery.isNotBlank()) "No Matching Offline Pages" else "No Offline Pages",
                    subtitle = if (searchQuery.isNotBlank()) "Try a different search term" else "Save web pages here to read anytime without internet."
                )
            }
        } else {
            items(filtered, key = { it.id }) { page ->
                val sizeKb = (page.sizeBytes / 1024).coerceAtLeast(1)
                val host = ZenPrivacyEngine.extractHostDisplay(page.url)
                Surface(
                    onClick = { onOpenOfflinePage(page) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("box_offline_item_${page.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF818CF8).copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = page.title.ifBlank { host },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF4F4F6),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$sizeKb KB • ${host.ifBlank { page.url }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onDeleteOfflinePage(page.id) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Offline Page",
                                tint = Color(0xFF71717A),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Mobile-friendly Spaces overview inspired by the desktop Arc/Zen Space cards screenshot:
 * Shows every workspace as a clearly readable, vertical dark card listing all its Pinned and Open tabs.
 */
@Composable
private fun BoxSpacesOverviewList(
    workspaces: List<WorkspaceEntity>,
    allTabs: List<TabEntity>,
    activeWorkspaceId: String,
    activeTabId: String,
    searchQuery: String,
    onlyActiveSpace: Boolean,
    onSelectWorkspace: (String) -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onCreateNewTabInWorkspace: (String) -> Unit,
    onManageWorkspaces: () -> Unit
) {
    val visibleWorkspaces = remember(workspaces, activeWorkspaceId, onlyActiveSpace) {
        if (onlyActiveSpace) workspaces.filter { it.id == activeWorkspaceId } else workspaces
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("box_spaces_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 10.dp)
    ) {
        items(visibleWorkspaces, key = { it.id }) { ws ->
            val wsTabs = remember(allTabs, ws.id, searchQuery) {
                allTabs.filter { tab ->
                    tab.workspaceId == ws.id && !tab.isEssential && (
                        searchQuery.isBlank() ||
                            tab.title.contains(searchQuery, ignoreCase = true) ||
                            tab.url.contains(searchQuery, ignoreCase = true) ||
                            ws.name.contains(searchQuery, ignoreCase = true)
                        )
                }.sortedBy { it.sortOrder }
            }
            val pinnedTabs = wsTabs.filter { it.isPinned }
            val openTabs = wsTabs.filter { !it.isPinned }
            val isActiveWs = ws.id == activeWorkspaceId

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0E0E13).copy(alpha = 0.94f),
                border = BorderStroke(
                    width = if (isActiveWs) 1.5.dp else 1.dp,
                    color = if (isActiveWs) Color(0xFF60A5FA).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.16f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("box_space_card_${ws.id}")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Card Header: Workspace icon + Name + Active tag + Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectWorkspace(ws.id) }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isActiveWs) Color(0xFF60A5FA).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (ws.isPrivate) {
                                    ShadeGlassIcon(tint = Color.White, size = 13.dp)
                                } else {
                                    Icon(
                                        imageVector = workspaceIconForKey(ws.iconKey),
                                        contentDescription = ws.name,
                                        tint = if (isActiveWs) Color(0xFF93C5FD) else Color(0xFFE4E4E7),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ws.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.10f)
                            ) {
                                Text(
                                    text = "${pinnedTabs.size + openTabs.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFD4D4D8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onCreateNewTabInWorkspace(ws.id) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New tab in ${ws.name}",
                                    tint = Color(0xFFD4D4D8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onManageWorkspaces,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Manage ${ws.name}",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Pinned Tabs inside Space Card
                    pinnedTabs.forEach { tab ->
                        SpaceCardTabRow(
                            tab = tab,
                            isSelected = tab.id == activeTabId,
                            isPinned = true,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }

                    // Divider between pinned and open tabs (like screenshot)
                    if (pinnedTabs.isNotEmpty() && openTabs.isNotEmpty()) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.14f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp)
                        )
                    }

                    // Open Tabs inside Space Card
                    openTabs.forEach { tab ->
                        SpaceCardTabRow(
                            tab = tab,
                            isSelected = tab.id == activeTabId,
                            isPinned = false,
                            onSelect = { onSelectTab(tab.id) },
                            onClose = { onCloseTab(tab.id) }
                        )
                    }

                    if (pinnedTabs.isEmpty() && openTabs.isEmpty()) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No matching tabs" else "No open tabs • Tap + to open",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF71717A),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpaceCardTabRow(
    tab: TabEntity,
    isSelected: Boolean,
    isPinned: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit
) {
    val host = ZenPrivacyEngine.extractHostDisplay(tab.url)
    val letter = (tab.title.firstOrNull() ?: host.firstOrNull() ?: 'Z').uppercase()

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .testTag("box_space_tab_${tab.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    tab.isPrivate -> ShadeGlassIcon(tint = Color.White, size = 12.dp)
                    tab.url == "zen://home" -> Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFFD4D4D8),
                        modifier = Modifier.size(12.dp)
                    )
                    else -> Text(
                        text = letter,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tab.title.ifBlank { host.ifBlank { "New Tab" } },
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color.White else Color(0xFFE4E4E7),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    tint = Color(0xFF93C5FD),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(12.dp)
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close tab",
                    tint = Color(0xFF71717A),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun BoxEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF71717A),
                modifier = Modifier.size(36.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFFE4E4E7),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA1A1AA),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
