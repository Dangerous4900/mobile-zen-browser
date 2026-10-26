package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.FirefoxExtensionEntity
import com.example.ui.ZenBrowserUiState

/**
 * Extensions Menu Popup:
 * Recreates the exact Zen Browser side panel extension popover (Image 1):
 * - Top Action Row: Bookmark, Screenshot, Reader mode, Share
 * - "Extensions" header
 * - 4-column compact grid of extension tiles with badges (uBlock with badge 14, Badger with badge 5, etc.)
 * - "+" tile to add more extensions
 * - "Settings" section: Tracking Protection Enabled
 * - Bottom Bar: "Secure" lock pill, Clean/Broom button, "..." menu
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZenExtensionsMenuPopup(
    uiState: ZenBrowserUiState,
    activeTabDomain: String,
    onDismiss: () -> Unit,
    onSelectExtension: (FirefoxExtensionEntity) -> Unit,
    onToggleExtension: (FirefoxExtensionEntity) -> Unit,
    onOpenManageExtensions: () -> Unit,
    onSavePageOffline: () -> Unit = {}
) {
    val installedExtensions = remember(uiState.firefoxExtensions) {
        val list = uiState.firefoxExtensions.filter { it.isInstalled }.toMutableList()
        // Ensure uBlock Origin is first in the grid
        val ublockIdx = list.indexOfFirst { it.id == "ff_ublock_origin" }
        if (ublockIdx > 0) {
            val ublock = list.removeAt(ublockIdx)
            list.add(0, ublock)
        }
        list
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x75000000))
                .clickable { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 32.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Surface(
                modifier = Modifier
                    .width(300.dp)
                    .clickable(enabled = false) {}
                    .shadow(30.dp, RoundedCornerShape(16.dp))
                    .testTag("zen_extensions_menu_popup"),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1E22),
                border = BorderStroke(1.dp, Color(0xFF2E2E36))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top quick actions bar (Bookmark, Save Offline, Screenshot, Reader, Share)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF26262D))
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = Color(0xFFD4D4D8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                onSavePageOffline()
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp).testTag("save_page_offline_quick_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Save Page Offline",
                                tint = Color(0xFF818CF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Screenshot",
                                tint = Color(0xFFD4D4D8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Reader View",
                                tint = Color(0xFFD4D4D8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDismiss() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color(0xFFD4D4D8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Section Title: "Extensions"
                    Text(
                        text = "Extensions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF4F4F6),
                        fontSize = 14.sp
                    )

                    // 4-Column Compact Icon Grid (Zen Browser desktop style with FlowRow for full scrollability)
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 4
                    ) {
                        installedExtensions.forEach { ext ->
                            ExtensionGridTile(
                                extension = ext,
                                onClick = {
                                    onSelectExtension(ext)
                                }
                            )
                        }

                        // "+" button to add/install extensions
                        Surface(
                            onClick = {
                                onDismiss()
                                onOpenManageExtensions()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF282830),
                            border = BorderStroke(1.dp, Color(0xFF383842)),
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("add_extension_tile")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Extension",
                                    tint = Color(0xFFD4D4D8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Section: "Settings"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F6),
                            fontSize = 14.sp
                        )

                        // Tracking Protection row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF25252C),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tracking_protection_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFBFDBFE),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Shield,
                                            contentDescription = null,
                                            tint = Color(0xFF1E293B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = "Tracking Protection",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFF4F4F6),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Enabled",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFA1A1AA),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Status Bar (Lock "Secure", Broom, "...")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "Secure" chip with lock icon
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF282830),
                            border = BorderStroke(1.dp, Color(0xFF383842))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFFF4F4F6),
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Secure",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFF4F4F6),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Broom / Clean Cache Icon
                            IconButton(
                                onClick = { onDismiss() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF282830))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Brush,
                                    contentDescription = "Clean Cache",
                                    tint = Color(0xFFD4D4D8),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // "..." More Menu (Opens full extension settings)
                            IconButton(
                                onClick = {
                                    onDismiss()
                                    onOpenManageExtensions()
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF282830))
                                    .testTag("extensions_more_options_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = "Manage Extensions",
                                    tint = Color(0xFFD4D4D8),
                                    modifier = Modifier.size(18.dp)
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
 * 4x4 Grid Tile for an extension (matches Image 1).
 */
@Composable
private fun ExtensionGridTile(
    extension: FirefoxExtensionEntity,
    onClick: () -> Unit
) {
    val icon = resolveExtensionIcon(extension.id)
    val iconColor = resolveExtensionColor(extension.id, extension.isEnabled)
    val badgeCount = when (extension.id) {
        "ff_ublock_origin" -> "14"
        "ff_privacy_badger" -> "5"
        "ff_sponsorblock" -> "1"
        "ff_return_dislike" -> "98%"
        "ff_violentmonkey" -> "2"
        else -> null
    }

    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF282830))
            .clickable { onClick() }
            .testTag("extension_tile_${extension.id}"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = extension.name,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        // Badge counter at top (e.g., uBlock "14", Badger "5")
        if (badgeCount != null && extension.isEnabled) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (extension.id == "ff_ublock_origin") Color(0xFF4B5563) else Color(0xFFF97316),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
            ) {
                Text(
                    text = badgeCount,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * Laptop-Style Floating Options Window:
 * Recreates the exact authentic uBlock Origin popup (Image 2) or custom options window for other extensions.
 */
@Composable
fun ZenExtensionFloatingPopup(
    extension: FirefoxExtensionEntity,
    activeTabDomain: String,
    onDismiss: () -> Unit,
    onBackToMenu: () -> Unit,
    onToggleEnabled: () -> Unit,
    onLaunchUblockElementPicker: () -> Unit,
    onRunScript: (String) -> Unit,
    onReloadTab: () -> Unit,
    onOpenExtensionSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x8A000000))
                .clickable { onDismiss() }
                .padding(horizontal = 14.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            when (extension.id) {
                "ff_ublock_origin" -> {
                    // Exact authentic uBlock Origin window (Image 2)
                    AuthenticUblockOriginDialogContent(
                        activeDomain = activeTabDomain,
                        isEnabled = extension.isEnabled,
                        onTogglePower = onToggleEnabled,
                        onLaunchElementPicker = onLaunchUblockElementPicker,
                        onRunScript = onRunScript,
                        onReloadTab = onReloadTab,
                        onOpenSettings = onOpenExtensionSettings,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
                "ff_dark_reader" -> {
                    // Authentic Dark Reader dialog
                    AuthenticDarkReaderDialogContent(
                        activeDomain = activeTabDomain,
                        isEnabled = extension.isEnabled,
                        onTogglePower = onToggleEnabled,
                        onRunScript = onRunScript,
                        onReloadTab = onReloadTab,
                        onOpenSettings = onOpenExtensionSettings,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
                "ff_privacy_badger" -> {
                    AuthenticPrivacyBadgerDialogContent(
                        activeDomain = activeTabDomain,
                        isEnabled = extension.isEnabled,
                        onTogglePower = onToggleEnabled,
                        onReloadTab = onReloadTab,
                        onOpenSettings = onOpenExtensionSettings,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
                "ff_sponsorblock" -> {
                    AuthenticSponsorBlockDialogContent(
                        activeDomain = activeTabDomain,
                        isEnabled = extension.isEnabled,
                        onTogglePower = onToggleEnabled,
                        onReloadTab = onReloadTab,
                        onOpenSettings = onOpenExtensionSettings,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
                "ff_bitwarden" -> {
                    AuthenticBitwardenDialogContent(
                        activeDomain = activeTabDomain,
                        isEnabled = extension.isEnabled,
                        onTogglePower = onToggleEnabled,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
                "ff_violentmonkey" -> {
                    AuthenticViolentmonkeyDialogContent(
                        activeDomain = activeTabDomain,
                        isEnabled = extension.isEnabled,
                        onTogglePower = onToggleEnabled,
                        onReloadTab = onReloadTab,
                        onOpenSettings = onOpenExtensionSettings,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
                "ff_return_dislike" -> {
                    AuthenticReturnDislikeDialogContent(
                        activeDomain = activeTabDomain,
                        isEnabled = extension.isEnabled,
                        onTogglePower = onToggleEnabled,
                        onReloadTab = onReloadTab,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
                else -> {
                    // General extension dialog
                    AuthenticGeneralExtensionDialogContent(
                        extension = extension,
                        activeDomain = activeTabDomain,
                        onTogglePower = onToggleEnabled,
                        onRunScript = onRunScript,
                        onReloadTab = onReloadTab,
                        onOpenSettings = onOpenExtensionSettings,
                        onClose = onDismiss,
                        onBackToMenu = onBackToMenu
                    )
                }
            }
        }
    }
}

/**
 * Exact replica of the uBlock Origin popup from Image 2:
 * - Domain title bar: "www.ecosia.org"
 * - Giant blue circular power button
 * - 5 toolbar icons: Element picker, Zapper, Cosmetic filtering, Remote fonts, Disable JS
 * - Blocked statistics text:
 *     "Blocked on this page: 14 (13%)"
 *     "Domains connected: 5 out of 8"
 *     "Blocked since install: 2,031 (5%)"
 * - Bottom tool icons: Zap, Pipette, Chat, Rules, Gear
 * - "More ⌄" and "Less ⌃"
 */
@Composable
private fun AuthenticUblockOriginDialogContent(
    activeDomain: String,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    onLaunchElementPicker: () -> Unit,
    onRunScript: (String) -> Unit,
    onReloadTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val cleanDomain = if (activeDomain.isNotBlank() && activeDomain != "New Tab") {
        if (!activeDomain.startsWith("www.") && activeDomain.contains(".")) "www.$activeDomain" else activeDomain
    } else {
        "www.ecosia.org"
    }

    var cosmeticFilterActive by remember { mutableStateOf(true) }
    var remoteFontsBlocked by remember { mutableStateOf(false) }
    var jsDisabled by remember { mutableStateOf(false) }
    var showMoreSection by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .width(260.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(12.dp))
            .testTag("authentic_ublock_origin_popup"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E222D),
        border = BorderStroke(1.dp, Color(0xFF2C3242))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Domain banner with back/close options
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2B3141)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBackToMenu,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Text(
                        text = cleanDomain,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE4E4E7),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Giant Circular Blue Power Button (exact look from screenshot)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Color(0xFF1C2740) else Color(0xFF242730))
                    .clickable { onTogglePower() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = if (isEnabled) "uBlock ON" else "uBlock OFF",
                    tint = if (isEnabled) Color(0xFF3B82F6) else Color(0xFF6B7280),
                    modifier = Modifier.size(54.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 Tools Action Strip (Picker, Zapper, Eye, Fonts, JS)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF242836)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Element Picker tool
                    IconButton(
                        onClick = onLaunchElementPicker,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Element Picker",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 2. Element Zapper tool
                    IconButton(
                        onClick = {
                            onRunScript(
                                """
                                (function() {
                                    var z = document.createElement('div');
                                    z.style = 'position:fixed;top:12px;left:50%;transform:translateX(-50%);background:#EF4444;color:white;padding:6px 14px;border-radius:16px;font-size:12px;z-index:9999999;';
                                    z.textContent = '⚡ uBlock Zapper: Click any element to remove';
                                    document.body.appendChild(z);
                                    var zapClick = function(e) {
                                        e.preventDefault(); e.stopPropagation();
                                        e.target.remove(); z.remove();
                                        document.removeEventListener('click', zapClick, true);
                                    };
                                    document.addEventListener('click', zapClick, true);
                                })();
                                """.trimIndent()
                            )
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Element Zapper",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3. Cosmetic filtering
                    IconButton(
                        onClick = { cosmeticFilterActive = !cosmeticFilterActive },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = "Cosmetic Filtering",
                            tint = if (cosmeticFilterActive) Color(0xFFD4D4D8) else Color(0xFF6B7280),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 4. Remote Fonts
                    IconButton(
                        onClick = { remoteFontsBlocked = !remoteFontsBlocked },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text(
                            text = "Aa¹",
                            fontWeight = FontWeight.Bold,
                            color = if (remoteFontsBlocked) Color(0xFFEF4444) else Color(0xFFD4D4D8),
                            fontSize = 13.sp
                        )
                    }

                    // 5. JavaScript toggle
                    IconButton(
                        onClick = { jsDisabled = !jsDisabled },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Text(
                            text = "</>⁹",
                            fontWeight = FontWeight.Bold,
                            color = if (jsDisabled) Color(0xFFEF4444) else Color(0xFFD4D4D8),
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Blocked Statistics (Exact layout from Image 2)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Blocked on this page
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Blocked on this page",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (isEnabled) "14 (13%)" else "0 (0%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6),
                        fontSize = 15.sp
                    )
                }

                // Domains connected
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Domains connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "5 out of 8",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6),
                        fontSize = 15.sp
                    )
                }

                // Blocked since install
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Blocked since install",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA1A1AA),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "2,031 (5%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6),
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Tools Row (Lightning, Pipette, Chat, Logger, Settings)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF242836)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onLaunchElementPicker,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Zap",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onLaunchElementPicker,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Pipette",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onReloadTab,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubbleOutline,
                            contentDescription = "Report issue",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onReloadTab,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ListAlt,
                            contentDescription = "Logger",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            onClose()
                            onOpenSettings()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Dashboard",
                            tint = Color(0xFFD4D4D8),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Bottom Accordion Bar: "More ⌄" and "Less ⌃"
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1B1E28)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { showMoreSection = !showMoreSection },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.clickable { showMoreSection = false },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color(0xFFA1A1AA),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Less",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            // Optional "More" expanded details
            AnimatedVisibility(visible = showMoreSection) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1A1D27))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Static Filter Rules: 312,410 active",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF93C5FD)
                    )
                    OutlinedButton(
                        onClick = onReloadTab,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF3B82F6))
                    ) {
                        Text("Reload Page with Updated Rules", fontSize = 11.sp, color = Color(0xFF93C5FD))
                    }
                }
            }
        }
    }
}

/**
 * Dark Reader Options Popup:
 */
@Composable
private fun AuthenticDarkReaderDialogContent(
    activeDomain: String,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    onRunScript: (String) -> Unit,
    onReloadTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var brightness by remember { mutableFloatStateOf(100f) }
    var contrast by remember { mutableFloatStateOf(90f) }
    var sepia by remember { mutableFloatStateOf(10f) }

    Surface(
        modifier = Modifier
            .width(280.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1C1C22),
        border = BorderStroke(1.dp, Color(0xFF2E2E38))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToMenu, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
                Text("Dark Reader", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6))
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262630))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(activeDomain.ifBlank { "all websites" }, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE4E4E7))
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { onTogglePower() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF59E0B))
                )
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Brightness", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                    Text("${brightness.toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Slider(value = brightness, onValueChange = { brightness = it }, valueRange = 50f..150f, colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B)))
            }

            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Contrast", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                    Text("${contrast.toInt()}%", style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
                Slider(value = contrast, onValueChange = { contrast = it }, valueRange = 50f..150f, colors = SliderDefaults.colors(thumbColor = Color(0xFFF59E0B), activeTrackColor = Color(0xFFF59E0B)))
            }

            Button(
                onClick = {
                    onRunScript(
                        """
                        (function() {
                            var style = document.getElementById('dark-reader-style');
                            if (!style) {
                                style = document.createElement('style');
                                style.id = 'dark-reader-style';
                                document.head.appendChild(style);
                            }
                            style.textContent = 'html { filter: invert(90%) hue-rotate(180deg) brightness(${brightness / 100f}) contrast(${contrast / 100f}) !important; background-color: #121216 !important; } img, video { filter: invert(100%) hue-rotate(180deg) !important; }';
                        })();
                        """.trimIndent()
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply to Page")
            }
        }
    }
}

/**
 * Authentic Privacy Badger Dialog:
 */
@Composable
private fun AuthenticPrivacyBadgerDialogContent(
    activeDomain: String,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    onReloadTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var tracker1State by remember { mutableStateOf("block") }
    var tracker2State by remember { mutableStateOf("block") }
    var tracker3State by remember { mutableStateOf("cookie") }

    Surface(
        modifier = Modifier
            .width(280.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(14.dp))
            .testTag("privacy_badger_popup"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E24),
        border = BorderStroke(1.dp, Color(0xFF32323D))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToMenu, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(16.dp))
                    Text("Privacy Badger", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
            }

            // Power switch for active domain
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF262630),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (activeDomain.isNotBlank() && activeDomain != "New Tab") activeDomain else "This Website",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF4F4F6)
                        )
                        Text(
                            text = if (isEnabled) "Protection active" else "Disabled on this site",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isEnabled) Color(0xFF10B981) else Color(0xFF71717A)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onTogglePower() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF97316))
                    )
                }
            }

            Text("Detected Trackers on Page", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFFD4D4D8))

            // Trackers list
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TrackerSliderRow(
                    domain = "doubleclick.net",
                    currentState = tracker1State,
                    onStateChange = { tracker1State = it }
                )
                TrackerSliderRow(
                    domain = "adservice.google.com",
                    currentState = tracker2State,
                    onStateChange = { tracker2State = it }
                )
                TrackerSliderRow(
                    domain = "analytics.yahoo.com",
                    currentState = tracker3State,
                    onStateChange = { tracker3State = it }
                )
            }

            Text(
                text = "3 potential trackers blocked on this site • 68 overall",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA1A1AA),
                fontSize = 11.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReloadTab,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reload Tab", color = Color(0xFFE4E4E7), fontSize = 12.sp)
                }
                Button(
                    onClick = {
                        onClose()
                        onOpenSettings()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF282834)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Settings", color = Color(0xFFE4E4E7), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun TrackerSliderRow(
    domain: String,
    currentState: String,
    onStateChange: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF24242E),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(domain, style = MaterialTheme.typography.bodySmall, color = Color(0xFFE4E4E7), fontSize = 11.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    onClick = { onStateChange("block") },
                    shape = RoundedCornerShape(4.dp),
                    color = if (currentState == "block") Color(0xFFEF4444) else Color(0xFF2C2C38),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🚫", fontSize = 10.sp)
                    }
                }
                Surface(
                    onClick = { onStateChange("cookie") },
                    shape = RoundedCornerShape(4.dp),
                    color = if (currentState == "cookie") Color(0xFFF59E0B) else Color(0xFF2C2C38),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🍪", fontSize = 10.sp)
                    }
                }
                Surface(
                    onClick = { onStateChange("allow") },
                    shape = RoundedCornerShape(4.dp),
                    color = if (currentState == "allow") Color(0xFF10B981) else Color(0xFF2C2C38),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("✓", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Authentic SponsorBlock Dialog:
 */
@Composable
private fun AuthenticSponsorBlockDialogContent(
    activeDomain: String,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    onReloadTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var skipSponsors by remember { mutableStateOf(true) }
    var skipIntros by remember { mutableStateOf(true) }
    var skipEndcards by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .width(280.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(14.dp))
            .testTag("sponsorblock_popup"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1C2220),
        border = BorderStroke(1.dp, Color(0xFF2B3A34))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToMenu, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(17.dp))
                    Text("SponsorBlock", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF23312B),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto-Skip Segments", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFF4F4F6))
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { onTogglePower() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sponsor segments", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4D4D8))
                    Switch(checked = skipSponsors, onCheckedChange = { skipSponsors = it }, modifier = Modifier.size(36.dp, 24.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Intros & Intermissions", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4D4D8))
                    Switch(checked = skipIntros, onCheckedChange = { skipIntros = it }, modifier = Modifier.size(36.dp, 24.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Endcards & Credits", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4D4D8))
                    Switch(checked = skipEndcards, onCheckedChange = { skipEndcards = it }, modifier = Modifier.size(36.dp, 24.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF192520),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("42 segments skipped • 14.5 mins saved", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF34D399))
                }
            }

            Button(
                onClick = { onClose() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Submit Segment at Current Time", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Authentic Bitwarden Password Manager Dialog:
 */
@Composable
private fun AuthenticBitwardenDialogContent(
    activeDomain: String,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var copiedText by remember { mutableStateOf<String?>(null) }
    var passwordLength by remember { mutableFloatStateOf(16f) }
    val generatedPassword = remember(passwordLength) {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789!@#$%&*+?"
        (1..passwordLength.toInt()).map { chars.random() }.joinToString("")
    }

    Surface(
        modifier = Modifier
            .width(280.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(14.dp))
            .testTag("bitwarden_popup"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF171B26),
        border = BorderStroke(1.dp, Color(0xFF283248))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToMenu, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                    Text("Bitwarden", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
            }

            Text("Matching Logins (${if (activeDomain.isNotBlank() && activeDomain != "New Tab") activeDomain else "Web"})", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF202738),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("user@example.com", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFF4F4F6))
                            Text("••••••••••••", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                        }
                        Button(
                            onClick = {
                                onClose()
                            },
                            shape = RoundedCornerShape(6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Autofill", fontSize = 11.sp)
                        }
                    }
                }
            }

            Text("Password Generator", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF202738),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = generatedPassword,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF60A5FA),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(generatedPassword))
                                copiedText = "Copied!"
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Password", tint = Color(0xFF94A3B8), modifier = Modifier.size(15.dp))
                        }
                    }
                    Slider(
                        value = passwordLength,
                        onValueChange = { passwordLength = it },
                        valueRange = 8f..32f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF3B82F6), activeTrackColor = Color(0xFF2563EB))
                    )
                    Text("Length: ${passwordLength.toInt()} characters", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                }
            }

            if (copiedText != null) {
                Text(copiedText ?: "", style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Authentic Violentmonkey / Tampermonkey Userscripts Dialog:
 */
@Composable
private fun AuthenticViolentmonkeyDialogContent(
    activeDomain: String,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    onReloadTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    var script1Active by remember { mutableStateOf(true) }
    var script2Active by remember { mutableStateOf(true) }

    Surface(
        modifier = Modifier
            .width(280.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(14.dp))
            .testTag("violentmonkey_popup"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF172024),
        border = BorderStroke(1.dp, Color(0xFF23363E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToMenu, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Tune, contentDescription = null, tint = Color(0xFF06B6D4), modifier = Modifier.size(16.dp))
                    Text("Userscripts Engine", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1F2E35),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Greasemonkey GM_* Engine", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF4F4F6))
                    Switch(checked = isEnabled, onCheckedChange = { onTogglePower() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF06B6D4)))
                }
            }

            Text("Active Scripts for Current Tab", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF22323A), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clean Links & Bypasses", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE4E4E7), fontSize = 11.sp)
                        Switch(checked = script1Active, onCheckedChange = { script1Active = it }, modifier = Modifier.size(34.dp, 20.dp))
                    }
                }
                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF22323A), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("YouTube Cinema Framer", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE4E4E7), fontSize = 11.sp)
                        Switch(checked = script2Active, onCheckedChange = { script2Active = it }, modifier = Modifier.size(34.dp, 20.dp))
                    }
                }
            }

            Button(
                onClick = {
                    onClose()
                    onOpenSettings()
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0891B2), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Install New Userscript (.user.js)", fontSize = 11.sp)
            }
        }
    }
}

/**
 * Authentic Return YouTube Dislike Dialog:
 */
@Composable
private fun AuthenticReturnDislikeDialogContent(
    activeDomain: String,
    isEnabled: Boolean,
    onTogglePower: () -> Unit,
    onReloadTab: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(14.dp))
            .testTag("return_dislike_popup"),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF241C1E),
        border = BorderStroke(1.dp, Color(0xFF3D2A2E))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToMenu, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.ThumbDown, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(16.dp))
                    Text("Return Dislike", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
            }

            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF332326), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Restore Dislike Counts", style = MaterialTheme.typography.bodySmall, color = Color(0xFFF4F4F6))
                    Switch(checked = isEnabled, onCheckedChange = { onTogglePower() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFF43F5E)))
                }
            }

            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2A1C20), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Active Rating Estimation", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA1A1AA))
                    Text("14.8K Likes • 240 Dislikes", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6))
                    Text("98.4% Like Ratio", style = MaterialTheme.typography.labelSmall, color = Color(0xFF34D399))
                }
            }

            Text("API: connected to returnyoutubedislikeapi.com", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF), fontSize = 10.sp)

            OutlinedButton(
                onClick = onReloadTab,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF4B5563)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refresh Video Metrics", color = Color(0xFFE4E4E7), fontSize = 12.sp)
            }
        }
    }
}

/**
 * General Desktop Extension Options Popup:
 */
@Composable
private fun AuthenticGeneralExtensionDialogContent(
    extension: FirefoxExtensionEntity,
    activeDomain: String,
    onTogglePower: () -> Unit,
    onRunScript: (String) -> Unit,
    onReloadTab: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
    onBackToMenu: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .clickable(enabled = false) {}
            .shadow(28.dp, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1C1C22),
        border = BorderStroke(1.dp, Color(0xFF2E2E38))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToMenu, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
                Text(extension.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF4F4F6), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFA1A1AA), modifier = Modifier.size(16.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF262630))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Active on this site", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE4E4E7))
                Switch(
                    checked = extension.isEnabled,
                    onCheckedChange = { onTogglePower() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF3B82F6))
                )
            }

            Text(extension.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFA1A1AA), maxLines = 3)

            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF24242E), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Permissions: ${extension.permissions}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF), fontSize = 10.sp)
                    Text("Match: ${extension.matchPatterns}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF), fontSize = 10.sp)
                }
            }

            if (extension.jsPayload.isNotBlank()) {
                Button(
                    onClick = { onRunScript(extension.jsPayload) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB), contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Execute Extension Script", fontSize = 12.sp)
                }
            }

            OutlinedButton(
                onClick = onReloadTab,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF3F3F46)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reload Webpage", color = Color(0xFFE4E4E7), fontSize = 12.sp)
            }
        }
    }
}

private fun resolveExtensionIcon(extensionId: String): ImageVector {
    return when (extensionId) {
        "ff_ublock_origin" -> Icons.Default.Shield
        "ff_dark_reader" -> Icons.Default.DarkMode
        "ff_video_bg_play" -> Icons.Default.Videocam
        "ff_privacy_badger" -> Icons.Default.Security
        "ff_sponsorblock" -> Icons.Default.FlashOn
        "ff_zen_translator" -> Icons.Default.Language
        "ff_bitwarden" -> Icons.Default.Lock
        "ff_violentmonkey" -> Icons.Default.Tune
        "ff_return_dislike" -> Icons.Default.ThumbDown
        "ff_clearurls" -> Icons.Default.DeleteSweep
        "ff_enhancer_youtube" -> Icons.Default.PlayArrow
        "ff_consent_o_matic" -> Icons.Default.Check
        else -> Icons.Default.Extension
    }
}

private fun resolveExtensionColor(extensionId: String, isEnabled: Boolean): Color {
    if (!isEnabled) return Color(0xFF71717A)
    return when (extensionId) {
        "ff_ublock_origin" -> Color(0xFFEF4444) // Iconic uBlock red shield
        "ff_dark_reader" -> Color(0xFFFBBF24) // Amber Yellow
        "ff_video_bg_play" -> Color(0xFF60A5FA) // Sky Blue
        "ff_privacy_badger" -> Color(0xFFF97316) // Orange badger
        "ff_sponsorblock" -> Color(0xFF10B981) // Emerald Green
        "ff_zen_translator" -> Color(0xFF38BDF8) // Cyan
        "ff_bitwarden" -> Color(0xFF3B82F6) // Blue
        "ff_violentmonkey" -> Color(0xFF06B6D4) // Cyan
        "ff_return_dislike" -> Color(0xFFF43F5E) // Rose Red
        "ff_clearurls" -> Color(0xFF10B981) // Green
        "ff_enhancer_youtube" -> Color(0xFFA855F7) // Purple
        "ff_consent_o_matic" -> Color(0xFF34D399) // Mint Green
        else -> Color(0xFFF4F4F6)
    }
}

