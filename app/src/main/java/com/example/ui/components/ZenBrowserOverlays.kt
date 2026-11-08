package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.TabEntity

/** Full-screen grid of the workspace's tabs with page previews. */
@Composable
fun ZenTabOverview(
    tabs: List<TabEntity>,
    activeTabId: String?,
    onSelect: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onDismiss: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val observe = ZenTabThumbnails.version // recompose when a new preview arrives
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0B0D)) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tabs overview · ${tabs.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close overview", tint = Color.White)
                    }
                }
                if (tabs.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tabs open", color = Color(0xFFA1A1AA), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val thumb = ZenTabThumbnails.get(tab.id)
                            val selected = tab.id == activeTabId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF16161A),
                                border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color(0xFF60A5FA) else Color(0xFF2A2A31)),
                                modifier = Modifier.fillMaxWidth().clickable { onSelect(tab.id) }
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.8f)
                                            .background(Color(0xFF101013)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (thumb != null) {
                                            Image(
                                                bitmap = thumb.asImageBitmap(),
                                                contentDescription = tab.title,
                                                contentScale = ContentScale.Crop,
                                                alignment = Alignment.TopCenter,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Text(
                                                text = (tab.title.firstOrNull() ?: tab.url.firstOrNull() ?: '•').uppercase(),
                                                style = MaterialTheme.typography.headlineMedium,
                                                color = Color(0xFF52525B)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = tab.title.ifBlank { tab.url },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { onCloseTab(tab.id) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Close tab", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
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

/** Long-press menu for images, links and media (like other browsers). */
@Composable
fun ZenContextMenu(
    info: com.example.gecko.ZenContextInfo,
    onOpenInNewTab: (String) -> Unit,
    onDownload: (String) -> Unit,
    onShare: (String) -> Unit,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF17171B)) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                val header = info.imageUrl ?: info.mediaUrl ?: info.linkUrl ?: ""
                Text(
                    text = (info.title?.takeIf { it.isNotBlank() } ?: header),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFA1A1AA),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
                @Composable
                fun item(label: String, onClick: () -> Unit) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClick(); onDismiss() }
                            .padding(horizontal = 18.dp, vertical = 13.dp)
                    )
                }
                info.linkUrl?.let { link ->
                    item("Open link in new tab") { onOpenInNewTab(link) }
                    item("Copy link") { onCopy(link) }
                    item("Share link") { onShare(link) }
                }
                info.imageUrl?.let { img ->
                    item("Download image") { onDownload(img) }
                    item("Share image") { onShare(img) }
                    item("Open image in new tab") { onOpenInNewTab(img) }
                    item("Copy image link") { onCopy(img) }
                }
                info.mediaUrl?.let { media ->
                    item("Download media") { onDownload(media) }
                    item("Copy media link") { onCopy(media) }
                }
            }
        }
    }
}
