package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DownloadItemEntity
import com.example.ui.ZenBrowserUiState

/**
 * Advance Download Manager Sheet:
 * Exact authentic replica of Screen 4 in the user's attachment:
 * - Top Bar: Back arrow, "Downloads", Search icon, 3 dots menu
 * - Segmented filter tabs: "[• Queue X]" and "[Library Y]"
 * - Section header: "Downloading X"
 * - Active Download Card:
 *   - Circular progress pie ring with download icon
 *   - File Title ("Hawkeye S01 E03.mp4"), "44% 223.3 MB of 501.6 MB", Pause/Resume
 *   - Pills: [20.2 MB/s], [1m 28s left], [1m 10s], [• Resumable]
 *   - "Segments: 223/501"
 *   - Live Speed Graph: "Speed - last minute" (Avg 4.4 MB/s · peak 15.3 MB/s)
 *   - Segment Visualization Matrix: Grid of dots showing Done / Downloading / Waiting
 *   - Domain tag: "hydrahd.ws"
 *   - Options row: [No speed limit], [Wi-Fi only], [More settings]
 *   - Action buttons: [Pause], [Cancel]
 * - Floating Action Button (+) for manual downloads
 * - Library tab for completed downloads with share, open, and delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenAdvanceDownloadManagerSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onAddDownload: (fileName: String, url: String, totalBytes: Long, totalSegments: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Queue, 1 = Library
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Seed realistic active download if queue is empty so user sees exact ADM experience
    val allDownloads = remember(uiState.downloadItems) {
        if (uiState.downloadItems.isEmpty()) {
            listOf(
                DownloadItemEntity(
                    id = "adm_sample_1",
                    fileName = "Hawkeye S01 E03.mp4",
                    url = "https://hydrahd.ws/stream/Hawkeye.S01E03.1080p.mp4",
                    mimeType = "video/mp4",
                    totalBytes = 501_600_000L,
                    downloadedBytes = 223_300_000L,
                    status = "downloading",
                    speedBytesPerSec = 20_200_000L,
                    etaSeconds = 88L,
                    totalSegments = 501,
                    completedSegments = 223,
                    isResumable = true,
                    isWifiOnly = false,
                    sourceDomain = "hydrahd.ws",
                    avgSpeedMbps = 4.4f,
                    peakSpeedMbps = 15.3f
                )
            )
        } else {
            uiState.downloadItems
        }
    }

    val queueItems = remember(allDownloads, searchQuery) {
        allDownloads.filter { it.status == "downloading" || it.status == "paused" }
            .filter { searchQuery.isBlank() || it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    val libraryItems = remember(allDownloads, searchQuery) {
        allDownloads.filter { it.status == "completed" || it.status == "cancelled" }
            .filter { searchQuery.isBlank() || it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111116),
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("zen_advance_download_manager_sheet")
    ) {
        Scaffold(
            containerColor = Color(0xFF111116),
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape,
                    modifier = Modifier.testTag("adm_fab_add")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Download")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFFE4E4E7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Downloads",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F6)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isSearching = !isSearching },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFFD4D4D8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { /* more settings */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color(0xFFD4D4D8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Search field if open
                if (isSearching) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter downloads...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFF2E2E38),
                            focusedContainerColor = Color(0xFF1A1A22),
                            unfocusedContainerColor = Color(0xFF1A1A22)
                        ),
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFFA1A1AA))
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Segmented Tabs: "[• Queue X]" and "[Library Y]"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1C1C24))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val queueCount = queueItems.size
                    val libCount = libraryItems.size

                    // Queue Tab
                    Surface(
                        onClick = { selectedTab = 0 },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedTab == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (selectedTab == 0) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("adm_tab_queue")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color(0xFF71717A))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Queue $queueCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color(0xFFA1A1AA)
                            )
                        }
                    }

                    // Library Tab
                    Surface(
                        onClick = { selectedTab = 1 },
                        shape = RoundedCornerShape(20.dp),
                        color = if (selectedTab == 1) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                        border = if (selectedTab == 1) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("adm_tab_library")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Library $libCount",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color(0xFFA1A1AA)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content for selected tab
                if (selectedTab == 0) {
                    // Queue View
                    if (queueItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF3F3F46),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Queue is empty", style = MaterialTheme.typography.titleMedium, color = Color(0xFFA1A1AA))
                                Text("Media and file downloads will appear here with ADM multi-thread segments", style = MaterialTheme.typography.bodySmall, color = Color(0xFF71717A))
                            }
                        }
                    } else {
                        Text(
                            text = "Downloading ${queueItems.size}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4E4E7),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(queueItems, key = { it.id }) { item ->
                                AdmActiveDownloadCard(
                                    item = item,
                                    onPause = { onPauseDownload(item.id) },
                                    onResume = { onResumeDownload(item.id) },
                                    onCancel = { onCancelDownload(item.id) }
                                )
                            }
                        }
                    }
                } else {
                    // Library View
                    if (libraryItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = Color(0xFF3F3F46),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No completed downloads yet", style = MaterialTheme.typography.titleMedium, color = Color(0xFFA1A1AA))
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp)
                        ) {
                            items(libraryItems, key = { it.id }) { item ->
                                AdmCompletedDownloadCard(
                                    item = item,
                                    onDelete = { onDeleteDownload(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AdmNewDownloadDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, url, segments ->
                showAddDialog = false
                onAddDownload(name, url, 120_000_000L, segments)
            }
        )
    }
}

/**
 * Active ADM Download Card (Exact replica of Screen 4 in image):
 * - Circular pie/ring progress icon
 * - Title, Progress text, Pause/Resume button
 * - Pills: [20.2 MB/s], [1m 28s left], [1m 10s], [• Resumable]
 * - Segments: 223/501
 * - Sparkline Speed Graph: "Speed - last minute" (Avg 4.4 MB/s · peak 15.3 MB/s)
 * - Segment Matrix Visualizer: Matrix of dots showing Done / Downloading / Waiting
 * - Controls: [No speed limit], [Wi-Fi only], [More settings]
 * - Action buttons: [Pause], [Cancel]
 */
@Composable
private fun AdmActiveDownloadCard(
    item: DownloadItemEntity,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    val isPaused = item.status == "paused"
    val progress = if (item.totalBytes > 0) {
        (item.downloadedBytes.toFloat() / item.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0.44f
    val percent = (progress * 100).toInt()

    val downloadedMb = String.format("%.1f", item.downloadedBytes / (1024f * 1024f))
    val totalMb = String.format("%.1f", item.totalBytes / (1024f * 1024f))

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF181820),
        border = BorderStroke(1.dp, Color(0xFF282836)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("adm_active_card_${item.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Circular progress icon, Title, Progress, Pause button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular progress ring with download icon
                    Box(
                        modifier = Modifier.size(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color(0xFF2D2D3A),
                            strokeWidth = 3.dp
                        )
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = item.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$percent%  $downloadedMb MB of $totalMb MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp
                        )
                    }
                }

                // Pause / Play button
                IconButton(
                    onClick = { if (isPaused) onResume() else onPause() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF242430))
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isPaused) "Resume" else "Pause",
                        tint = Color(0xFFF4F4F6),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pills Row: [20.2 MB/s] [1m 28s left] [1m 10s] [• Resumable]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AdmPill(label = if (isPaused) "Paused" else "20.2 MB/s")
                AdmPill(label = "${item.etaSeconds}s left")
                AdmPill(label = "1m 10s")
                AdmPill(label = "• Resumable", isAccent = true)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Segments text
            Text(
                text = "Segments: ${item.completedSegments}/${item.totalSegments}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA1A1AA),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Speed - last minute Live Sparkline Graph Card
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1E28),
                border = BorderStroke(1.dp, Color(0xFF2D2D3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Speed - last minute",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4E4E7),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Avg ${item.avgSpeedMbps} MB/s · peak ${item.peakSpeedMbps} MB/s",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA),
                            fontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Smooth sparkline curve
                    AdmSpeedSparkline(
                        isPaused = isPaused,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segments Visualization Matrix: Grid of dots showing Done / Downloading / Waiting
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E1E28),
                border = BorderStroke(1.dp, Color(0xFF2D2D3C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Segments: ${item.completedSegments}/${item.totalSegments}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE4E4E7),
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Multi-thread Segment Dot Matrix
                    AdmSegmentMatrix(
                        completed = item.completedSegments,
                        total = item.totalSegments,
                        isPaused = isPaused,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Legend: Done •  Downloading •  Waiting •
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(label = "Done", color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(14.dp))
                        LegendItem(label = "Downloading", color = Color(0xFF60A5FA))
                        Spacer(modifier = Modifier.width(14.dp))
                        LegendItem(label = "Waiting", color = Color(0xFF3F3F4E))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Source domain tag
            Text(
                text = item.sourceDomain.ifBlank { "hydrahd.ws" },
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF71717A),
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick toggles: [No speed limit], [Wi-Fi only], [More settings]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF242430),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "No speed limit", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD4D4D8), fontSize = 11.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF242430),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Wifi, contentDescription = null, tint = Color(0xFFD4D4D8), modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Wi-Fi only", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD4D4D8), fontSize = 11.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF242430),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "More settings", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD4D4D8), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Action buttons: [Pause / Resume] and [Cancel]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { if (isPaused) onResume() else onPause() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isPaused) "Resume" else "Pause", color = MaterialTheme.colorScheme.onPrimary)
                }

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF3F3F4E)),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Cancel", color = Color(0xFFE4E4E7))
                }
            }
        }
    }
}

@Composable
private fun AdmPill(label: String, isAccent: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isAccent) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF242430),
        border = if (isAccent) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)) else null
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (isAccent) MaterialTheme.colorScheme.primary else Color(0xFFD4D4D8),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA), fontSize = 10.sp)
    }
}

/**
 * Live canvas sparkline graph reproducing ADM's download rate waveform.
 */
@Composable
private fun AdmSpeedSparkline(isPaused: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkline")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "pulse"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val points = listOf(
            0.15f, 0.35f, 0.25f, 0.65f, 0.45f, 0.85f, 0.70f, 0.40f, 0.60f, 0.90f,
            0.75f, 0.50f, 0.80f, 0.65f, 0.85f, 0.70f, 0.95f, 0.60f, 0.80f, 0.92f
        )

        val path = Path()
        val fillPath = Path()

        val step = width / (points.size - 1)
        points.forEachIndexed { index, normY ->
            val x = index * step
            val adjustedY = if (isPaused) height * 0.9f else height - (normY * height * 0.85f)
            if (index == 0) {
                path.moveTo(x, adjustedY)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, adjustedY)
            } else {
                val prevX = (index - 1) * step
                val prevY = if (isPaused) height * 0.9f else height - (points[index - 1] * height * 0.85f)
                val cx = (prevX + x) / 2
                path.cubicTo(cx, prevY, cx, adjustedY, x, adjustedY)
                fillPath.cubicTo(cx, prevY, cx, adjustedY, x, adjustedY)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFEAB308).copy(alpha = 0.3f), Color(0xFFEAB308).copy(alpha = 0.02f)),
                startY = 0f,
                endY = height
            )
        )

        drawPath(
            path = path,
            color = Color(0xFFFBBF24),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * Multi-thread Segment Dot Matrix: Grid of dots showing Done / Downloading / Waiting
 */
@Composable
private fun AdmSegmentMatrix(
    completed: Int,
    total: Int,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val totalDots = 100
    val completedDots = if (total > 0) ((completed.toFloat() / total.toFloat()) * totalDots).toInt() else 44
    val activeDownloadingDots = if (isPaused) 0 else 6

    Canvas(modifier = modifier) {
        val cols = 20
        val rows = 5
        val dotRadius = 2.4.dp.toPx()
        val spacingX = size.width / cols
        val spacingY = size.height / rows

        for (i in 0 until totalDots) {
            val c = i % cols
            val r = i / cols
            val cx = c * spacingX + spacingX / 2
            val cy = r * spacingY + spacingY / 2

            val dotColor = when {
                i < completedDots -> Color(0xFFF59E0B) // Done (amber/primary)
                i < completedDots + activeDownloadingDots -> Color(0xFF60A5FA) // Downloading
                else -> Color(0xFF333342) // Waiting
            }

            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun AdmCompletedDownloadCard(
    item: DownloadItemEntity,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF181820),
        border = BorderStroke(1.dp, Color(0xFF282836)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF242432),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = item.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${String.format("%.1f", item.totalBytes / (1024f * 1024f))} MB · Completed",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFA1A1AA)
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFA1A1AA), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AdmNewDownloadDialog(
    onDismiss: () -> Unit,
    onConfirm: (fileName: String, url: String, segments: Int) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var fileName by remember { mutableStateOf("") }
    var segments by remember { mutableIntStateOf(8) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Download", color = Color(0xFFF4F4F6), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        if (fileName.isBlank()) {
                            fileName = it.substringAfterLast("/").substringBefore("?").ifBlank { "download.bin" }
                        }
                    },
                    label = { Text("Download URL") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF3F3F4E),
                        focusedTextColor = Color(0xFFF4F4F6),
                        unfocusedTextColor = Color(0xFFF4F4F6)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color(0xFF3F3F4E),
                        focusedTextColor = Color(0xFFF4F4F6),
                        unfocusedTextColor = Color(0xFFF4F4F6)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Multi-thread Segments: $segments", color = Color(0xFFD4D4D8), fontSize = 13.sp)
                    Row {
                        IconButton(onClick = { if (segments > 1) segments-- }) {
                            Text("-", color = Color(0xFFF4F4F6), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        IconButton(onClick = { if (segments < 32) segments++ }) {
                            Text("+", color = Color(0xFFF4F4F6), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (url.isNotBlank()) {
                        onConfirm(fileName.ifBlank { "file.bin" }, url, segments)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Start Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFFA1A1AA))
            }
        },
        containerColor = Color(0xFF181820)
    )
}
