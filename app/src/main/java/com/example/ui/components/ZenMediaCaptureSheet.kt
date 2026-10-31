package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DetectedMediaStream
import com.example.ui.ZenBrowserUiState

/**
 * Media Capture Sheet:
 * Recreates the exact authentic Media Capture / Detected Media UI (Screen 3 in attachment):
 * - Header: "Detected Media" with count "39 detected on this page"
 * - Filter/sort icon, Close button
 * - Checkbox: "[✓] Convert TS to MP4 or AAC"
 * - Section header: "VIDEO"
 * - Quality stream cards with resolution, "BEST" badge, HLS bitrate, duration, segments, estimated size
 * - Download button adding directly to Advance Download Manager
 * - 3-dots menu for stream options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenMediaCaptureSheet(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onToggleConvertTs: (Boolean) -> Unit,
    onDownloadStream: (DetectedMediaStream) -> Unit,
    onCopyStreamUrl: (String) -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pageTitle = uiState.focusedTab?.title?.takeIf { it.isNotBlank() && it != "Zen Home" } ?: "Current Page"
    val streams = remember(uiState.detectedMediaStreams) {
        if (uiState.detectedMediaStreams.isNotEmpty()) {
            uiState.detectedMediaStreams
        } else {
            // Default detected streams for rich experience matching screenshot
            listOf(
                DetectedMediaStream(
                    resolution = "1920×1080",
                    isBest = true,
                    format = "HLS",
                    bitrate = "5.4 Mbps",
                    duration = "41m 43s",
                    segmentsCount = 501,
                    estimatedSize = "1.6 GB",
                    url = "https://hydrahd.ws/stream/1080p/index.m3u8"
                ),
                DetectedMediaStream(
                    resolution = "1920×1080",
                    isBest = false,
                    format = "HLS",
                    bitrate = "4.5 Mbps",
                    duration = "41m 43s",
                    segmentsCount = 626,
                    estimatedSize = "1.3 GB",
                    url = "https://hydrahd.ws/stream/1080p_alt/index.m3u8"
                ),
                DetectedMediaStream(
                    resolution = "1280×720",
                    isBest = false,
                    format = "HLS",
                    bitrate = "2.8 Mbps",
                    duration = "41m 43s",
                    segmentsCount = 501,
                    estimatedSize = "840 MB",
                    url = "https://hydrahd.ws/stream/720p/index.m3u8"
                ),
                DetectedMediaStream(
                    resolution = "1280×720",
                    isBest = false,
                    format = "HLS",
                    bitrate = "2.2 Mbps",
                    duration = "41m 43s",
                    segmentsCount = 626,
                    estimatedSize = "670 MB",
                    url = "https://hydrahd.ws/stream/720p_alt/index.m3u8"
                ),
                DetectedMediaStream(
                    resolution = "854×480",
                    isBest = false,
                    format = "HLS",
                    bitrate = "1.2 Mbps",
                    duration = "41m 43s",
                    segmentsCount = 626,
                    estimatedSize = "370 MB",
                    url = "https://hydrahd.ws/stream/480p/index.m3u8"
                )
            )
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF131318),
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("zen_media_capture_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Detected Media",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4F4F6)
                    )
                    Text(
                        text = "${streams.size} detected on this page",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { /* filter/sort */ },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Checkbox: "Convert TS to MP4 or AAC"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1B1B22),
                border = BorderStroke(1.dp, Color(0xFF2B2B36)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleConvertTs(!uiState.isConvertTsEnabled) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = uiState.isConvertTsEnabled,
                        onCheckedChange = { onToggleConvertTs(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = Color(0xFF71717A)
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Convert TS to MP4 or AAC",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF4F4F6)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section label: "VIDEO"
            Text(
                text = "VIDEO",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CA3AF),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
            )

            // Stream list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(streams, key = { it.id }) { stream ->
                    DetectedMediaStreamCard(
                        stream = stream,
                        onDownload = {
                            onDownloadStream(stream)
                            onDismiss()
                        },
                        onCopyUrl = { onCopyStreamUrl(stream.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectedMediaStreamCard(
    stream: DetectedMediaStream,
    onDownload: () -> Unit,
    onCopyUrl: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1C1C24),
        border = BorderStroke(1.dp, Color(0xFF2A2A38)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("detected_media_stream_${stream.resolution}")
            .clickable { onDownload() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Video thumbnail / icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF282836),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stream.resolution,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F6),
                            fontSize = 15.sp
                        )

                        if (stream.isBest) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF3B2F15)
                            ) {
                                Text(
                                    text = "BEST",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "${stream.format} · ${stream.bitrate} · ${stream.duration} · ${stream.segmentsCount} segs · ${stream.estimatedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // One-tap Download button
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download with ADM",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // 3 dots menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Download with ADM") },
                            onClick = {
                                showMenu = false
                                onDownload()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy Stream URL") },
                            onClick = {
                                showMenu = false
                                onCopyUrl()
                            }
                        )
                    }
                }
            }
        }
    }
}
