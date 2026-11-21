package com.example.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AiSidePanelProvider
import com.example.gecko.ZenGeckoManager
import org.mozilla.geckoview.GeckoView

@SuppressLint("ClickableViewAccessibility")
@Composable
fun ZenAiSidePanelContent(
    activeProviderId: String,
    isLiquidGlass: Boolean,
    onSelectProvider: (String) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val provider = remember(activeProviderId) { AiSidePanelProvider.fromId(activeProviderId) }

    val geckoSession = remember(provider.id) {
        ZenGeckoManager.getOrCreateAiPanelSession(
            context = context,
            providerId = provider.id,
            initialUrl = provider.url
        )
    }

    var currentUrl by remember(provider.id) {
        mutableStateOf(ZenGeckoManager.getAiPanelCurrentUrl(provider.id) ?: provider.url)
    }
    var loadingProgress by remember(provider.id) { mutableIntStateOf(100) }
    var canGoBack by remember(provider.id) { mutableStateOf(false) }

    DisposableEffect(provider.id, geckoSession) {
        runCatching { geckoSession.setActive(true) }
        ZenGeckoManager.attachAiPanelDelegates(
            session = geckoSession,
            providerId = provider.id,
            onLocationChange = { url -> currentUrl = url },
            onProgressChange = { progress, back ->
                loadingProgress = progress
                canGoBack = back
            },
            onOpenInNewTab = onOpenInNewTab
        )
        onDispose {}
    }

    Column(
        modifier = modifier.testTag("zen_ai_side_panel_content"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header row: AI icon + label + Back / Reload / Open in Tab / Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "AI Chat",
                    tint = Color(0xFFF4F4F6),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "AI Chat",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF4F4F6),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { geckoSession.goBack() },
                    enabled = canGoBack,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("ai_panel_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "AI Back",
                        tint = if (canGoBack) Color(0xFFF4F4F6) else Color(0xFF52525B),
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { geckoSession.reload() },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("ai_panel_reload_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload AI Chat",
                        tint = Color(0xFFD4D4D8),
                        modifier = Modifier.size(15.dp)
                    )
                }
                IconButton(
                    onClick = { onOpenInNewTab(currentUrl.ifBlank { provider.url }) },
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("ai_panel_open_tab_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Open in Browser Tab",
                        tint = Color(0xFFD4D4D8),
                        modifier = Modifier.size(14.dp)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("ai_panel_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close AI Side Panel",
                        tint = Color(0xFFA1A1AA),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }

        // Provider switcher row (ChatGPT, Claude, Gemini, Perplexity, Grok)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_provider_selector_row"),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            contentPadding = PaddingValues(horizontal = 1.dp)
        ) {
            items(AiSidePanelProvider.entries, key = { it.id }) { item ->
                val isSelected = item.id == provider.id
                Surface(
                    onClick = { onSelectProvider(item.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isSelected && isLiquidGlass -> Color.White.copy(alpha = 0.24f)
                        isSelected -> Color(0xFF27272B)
                        isLiquidGlass -> Color.White.copy(alpha = 0.08f)
                        else -> Color(0xFF121216)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = when {
                            isSelected -> Color(0xFFE4E4E7)
                            isLiquidGlass -> Color.White.copy(alpha = 0.18f)
                            else -> Color(0xFF27272A)
                        }
                    ),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("ai_provider_chip_${item.id}")
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFFFFFFFF) else Color(0xFFA1A1AA),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        if (loadingProgress in 1..99) {
            LinearProgressIndicator(
                progress = { loadingProgress / 100f },
                color = Color(0xFFE4E4E7),
                trackColor = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }

        // Fitted AI Web Viewport
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF0E0E12),
            border = BorderStroke(
                width = 1.dp,
                color = if (isLiquidGlass) Color.White.copy(alpha = 0.22f) else Color(0xFF27272A)
            ),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                factory = { ctx ->
                    GeckoView(ctx).apply {
                        isFocusable = true
                        isFocusableInTouchMode = true
                        setOnTouchListener { v, _ ->
                            v.parent?.requestDisallowInterceptTouchEvent(true)
                            false
                        }
                        ZenGeckoManager.attachAiSessionToView(provider.id, geckoSession, this)
                    }
                },
                update = { view ->
                    ZenGeckoManager.attachAiSessionToView(provider.id, geckoSession, view)
                },
                onRelease = { view ->
                    ZenGeckoManager.releaseAiSessionFromView(provider.id, view)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .testTag("ai_side_panel_webview")
            )
        }
    }
}
