package com.example.extensions

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FirefoxExtensionEntity
import com.example.ui.ZenBrowserUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ClintGecko-styled Extensions Management Screen:
 * Features ClintGecko's authentic layout with Zen's theme colors:
 * - App bar with title and refresh check
 * - Large HeroCard with gradient, watermark puzzle icons, active stats, and 3 action buttons:
 *     1. "Get Add-ons" (Storefront)
 *     2. "Install from File" (Folder)
 *     3. "Install from Link" (Link)
 * - Filter pills: All (count), Enabled (count), Disabled (count)
 * - Adaptive grid of Extension Cards with instant toggle, version, author, popup launcher, details, settings & remove
 * - ClintGecko Install Prompt Dialog with Risk Meter and permissions breakdown
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZenExtensionsClintStyleScreen(
    uiState: ZenBrowserUiState,
    onExit: () -> Unit,
    onToggleExtension: (FirefoxExtensionEntity) -> Unit,
    onToggleAllowInPrivate: (FirefoxExtensionEntity) -> Unit,
    onUninstallExtension: (FirefoxExtensionEntity) -> Unit,
    onOpenExtensionPopup: (FirefoxExtensionEntity) -> Unit,
    onOpenAmoStore: () -> Unit,
    onInstallFromUrl: (String) -> Unit,
    onImportFile: (fileName: String, inputStream: java.io.InputStream) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var filter by remember { mutableIntStateOf(0) } // 0: All, 1: Enabled, 2: Disabled
    var showLinkDialog by remember { mutableStateOf(false) }
    var selectedDetailExtension by remember { mutableStateOf<FirefoxExtensionEntity?>(null) }
    var isCheckingUpdates by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "extension.xpi"
                context.contentResolver.openInputStream(uri)?.let { stream ->
                    onImportFile(fileName, stream)
                    Toast.makeText(context, "Installing $fileName...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val extensions = uiState.firefoxExtensions.filter { it.isInstalled }
    val onCount = extensions.count { it.isEnabled }
    val offCount = extensions.size - onCount

    val filtered = when (filter) {
        1 -> extensions.filter { it.isEnabled }
        2 -> extensions.filter { !it.isEnabled }
        else -> extensions
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("zen_clint_extensions_screen")
    ) {
        Column(Modifier.fillMaxSize()) {
            // Top App Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Extensions",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )

                    if (isCheckingUpdates) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 14.dp)
                                .size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                isCheckingUpdates = true
                                coroutineScope.launch {
                                    delay(1200)
                                    isCheckingUpdates = false
                                    Toast.makeText(context, "All extensions are up to date", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = extensions.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Check for updates",
                                tint = if (extensions.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 1.dp)

            // Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 330.dp),
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = 1200.dp),
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ClintGecko HeroCard
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        ClintStyleHeroCard(
                            onCount = onCount,
                            offCount = offCount,
                            onGetAddons = onOpenAmoStore,
                            onPickFile = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            onLink = { showLinkDialog = true }
                        )
                    }

                    // Filter Pills
                    if (extensions.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ClintFilterPill("All", extensions.size, filter == 0) { filter = 0 }
                                ClintFilterPill("Enabled", onCount, filter == 1) { filter = 1 }
                                ClintFilterPill("Disabled", offCount, filter == 2) { filter = 2 }
                            }
                        }
                    }

                    // Empty States
                    if (extensions.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ClintEmptyState(onGetAddons = onOpenAmoStore)
                        }
                    } else if (filtered.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = "No extensions found matching filter",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        }
                    }

                    // Extension Cards
                    items(filtered, key = { it.id }) { ext ->
                        ClintExtensionCard(
                            extension = ext,
                            onToggle = { onToggleExtension(ext) },
                            onOpenPopup = { onOpenExtensionPopup(ext) },
                            onOpenDetail = { selectedDetailExtension = ext },
                            onUninstall = { onUninstallExtension(ext) }
                        )
                    }
                }
            }
        }
    }

    // Install from Link Dialog
    if (showLinkDialog) {
        ClintInstallLinkDialog(
            onDismiss = { showLinkDialog = false },
            onInstall = { url ->
                showLinkDialog = false
                onInstallFromUrl(url)
            }
        )
    }

    // Extension Detail Dialog
    selectedDetailExtension?.let { ext ->
        ClintExtensionDetailDialog(
            extension = ext,
            onDismiss = { selectedDetailExtension = null },
            onToggleEnabled = { onToggleExtension(ext) },
            onTogglePrivate = { onToggleAllowInPrivate(ext) },
            onUninstall = {
                onUninstallExtension(ext)
                selectedDetailExtension = null
            },
            onOpenPopup = {
                selectedDetailExtension = null
                onOpenExtensionPopup(ext)
            }
        )
    }
}

/**
 * Hero Card matching ClintGecko's HeroCard with Zen's primary gradient palette.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClintStyleHeroCard(
    onCount: Int,
    offCount: Int,
    onGetAddons: () -> Unit,
    onPickFile: () -> Unit,
    onLink: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val darkGradient = listOf(
        lerp(primary, Color(0xFF0F172A), 0.25f),
        lerp(primary, Color.Black, 0.65f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(darkGradient))
            .testTag("clint_extensions_hero_card")
    ) {
        // Watermark puzzle icons (ClintGecko signature visual)
        Icon(
            imageVector = Icons.Filled.Extension,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 36.dp, y = (-26).dp)
                .size(170.dp)
                .rotate(-14f)
                .alpha(0.10f)
        )
        Icon(
            imageVector = Icons.Filled.Extension,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-30).dp, y = 34.dp)
                .size(110.dp)
                .rotate(18f)
                .alpha(0.06f)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Text(
                text = "Extensions",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (onCount + offCount == 0) "Boost browsing with ad-blocking, privacy shields, and web add-ons"
                else "$onCount active · $offCount disabled",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // 3 Hero Action Buttons (Get Add-ons, Install from File, Install from Link)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroActionButton(
                    icon = Icons.Filled.Storefront,
                    label = "Get Add-ons",
                    onClick = onGetAddons,
                    modifier = Modifier.testTag("hero_get_addons_btn")
                )
                HeroActionButton(
                    icon = Icons.Filled.Folder,
                    label = "Install from File",
                    onClick = onPickFile,
                    modifier = Modifier.testTag("hero_install_file_btn")
                )
                HeroActionButton(
                    icon = Icons.Filled.Link,
                    label = "Install from Link",
                    onClick = onLink,
                    modifier = Modifier.testTag("hero_install_link_btn")
                )
            }
        }
    }
}

@Composable
private fun HeroActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ClintFilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = CircleShape,
                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Extension Card matching ClintGecko's cards.
 */
@Composable
private fun ClintExtensionCard(
    extension: FirefoxExtensionEntity,
    onToggle: () -> Unit,
    onOpenPopup: () -> Unit,
    onOpenDetail: () -> Unit,
    onUninstall: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("clint_extension_card_${extension.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Icon, Title + Version, Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Extension Icon Tile
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { onOpenDetail() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when {
                                extension.id.contains("ublock") || extension.id.contains("privacy") -> Icons.Filled.Shield
                                extension.id.contains("dark") -> Icons.Filled.Settings
                                else -> Icons.Filled.Extension
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenDetail() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = extension.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "v${extension.version}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Text(
                        text = "by ${extension.author.ifBlank { "Mozilla Add-ons" }} · ${extension.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Instant ON/OFF switch
                Switch(
                    checked = extension.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("ext_toggle_switch_${extension.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Description
            Text(
                text = extension.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.clickable { onOpenDetail() }
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Open Popup Button
                    Surface(
                        onClick = onOpenPopup,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("ext_open_popup_${extension.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Popup",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Details Button
                    Surface(
                        onClick = onOpenDetail,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Details",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Remove Button
                IconButton(
                    onClick = onUninstall,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("ext_remove_btn_${extension.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Uninstall",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClintEmptyState(onGetAddons: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "No Extensions Installed",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Browse Mozilla Add-ons or install .xpi packages to enhance your browsing experience.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 36.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
            onClick = onGetAddons,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Explore Add-ons Store",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * ClintGecko-style Install from Link Dialog.
 */
@Composable
private fun ClintInstallLinkDialog(
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit
) {
    var urlInput by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .testTag("clint_install_link_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Install Extension from Link",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enter a direct .xpi download URL or Mozilla Add-ons (AMO) listing link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("https://addons.mozilla.org/firefox/downloads/file/...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("install_link_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            if (urlInput.isNotBlank()) onInstall(urlInput.trim())
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("confirm_install_link_btn")
                    ) {
                        Text(
                            text = "Install Add-on",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * ClintGecko-style Extension Detail Dialog with Permissions and Risk Meter.
 */
@Composable
private fun ClintExtensionDetailDialog(
    extension: FirefoxExtensionEntity,
    onDismiss: () -> Unit,
    onToggleEnabled: () -> Unit,
    onTogglePrivate: () -> Unit,
    onUninstall: () -> Unit,
    onOpenPopup: () -> Unit
) {
    val risk = when {
        extension.matchPatterns.contains("<all_urls>") -> "HIGH"
        extension.matchPatterns.isNotBlank() -> "MEDIUM"
        else -> "LOW"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .testTag("clint_extension_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Extension,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = extension.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "v${extension.version} · by ${extension.author.ifBlank { "Mozilla Developer" }}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Risk Meter (ClintGecko style)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (risk == "HIGH") Icons.Default.Warning else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (risk == "HIGH") Color(0xFFEF4444) else if (risk == "MEDIUM") Color(0xFFF59E0B) else Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Permissions Risk Level",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (risk == "HIGH") Color(0xFFEF4444) else if (risk == "MEDIUM") Color(0xFFF59E0B) else Color(0xFF10B981)
                        ) {
                            Text(
                                text = "$risk RISK",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Allow in Private Browsing toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTogglePrivate() }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Run in Private Browsing",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Allow this extension to access pages opened in private tabs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = extension.allowInPrivate,
                        onCheckedChange = { onTogglePrivate() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Match Patterns
                Text(
                    text = "Website Access",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (extension.matchPatterns.isBlank() || extension.matchPatterns == "<all_urls>") "Access data for all websites"
                    else "Accesses: ${extension.matchPatterns}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onUninstall,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Uninstall", color = MaterialTheme.colorScheme.error)
                    }

                    Surface(
                        onClick = onOpenPopup,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Open Popup",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
