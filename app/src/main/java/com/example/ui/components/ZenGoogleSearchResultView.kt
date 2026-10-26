package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZenGoogleSearchResultView(
    query: String,
    customAiOverviewText: String = "",
    customHeadline: String = "",
    customSnippet: String = "",
    onNavigateUrl: (String) -> Unit,
    onOpenInGlance: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val listState = rememberSaveable(query, saver = LazyListState.Saver) {
        LazyListState()
    }
    var hasCompletedAiGeneration by rememberSaveable(query) { mutableStateOf(false) }
    var isAiGenerating by remember(query) { mutableStateOf(!hasCompletedAiGeneration) }
    var loadingStep by remember(query) { mutableIntStateOf(if (hasCompletedAiGeneration) 3 else 0) }
    var selectedFilterTab by rememberSaveable(query) { mutableIntStateOf(0) }
    var expandedFaqIndex by remember(query) { mutableStateOf<Int?>(null) }

    val loadingTexts = listOf(
        "Searching the live web across 28 sources...",
        "Reading aura telemetry & Arch Linux documentation...",
        "Evaluating feline charm and protagonist index...",
        "Synthesizing Google Gemini AI Overview..."
    )

    LaunchedEffect(query) {
        if (!hasCompletedAiGeneration) {
            delay(400)
            loadingStep = 1
            delay(550)
            loadingStep = 2
            delay(600)
            loadingStep = 3
            delay(650)
            isAiGenerating = false
            hasCompletedAiGeneration = true
        } else {
            isAiGenerating = false
            loadingStep = 3
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    val googleBg = Color(0xFF1F1F1F)
    val cardBg = Color(0xFF282A2D)
    val textPrimary = Color(0xFFE8EAED)
    val textSecondary = Color(0xFFBDC1C6)
    val googleBlue = Color(0xFF8AB4F8)
    val borderColor = Color(0xFF3C4043)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(googleBg)
            .testTag("zen_google_search_results_page")
    ) {
        // Google Search Top Header
        Surface(
            color = Color(0xFF202124),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303134)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Colorful Wordmark
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("o", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("o", color = Color(0xFFFBBC05), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("g", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("l", color = Color(0xFF34A853), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("e", color = Color(0xFFEA4335), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }

                    // Search Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF303134),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF5F6368)),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = query,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFF9AA0A6),
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onNavigateUrl("zen://home") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice",
                                tint = Color(0xFF8AB4F8),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color(0xFF8AB4F8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Filter Tabs: All, Images, Videos, News, Maps
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    val tabs = listOf("All", "Images", "Videos", "News", "Shorts", "Maps")
                    tabs.forEachIndexed { index, tabName ->
                        val isSelected = selectedFilterTab == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedFilterTab = index }
                                .padding(vertical = 6.dp)
                        ) {
                            Text(
                                text = tabName,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) Color(0xFF8AB4F8) else Color(0xFF9AA0A6),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(2.dp)
                                    .background(if (isSelected) Color(0xFF8AB4F8) else Color.Transparent)
                            )
                        }
                    }
                }
            }
        }

        // Search Results Scrollable List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AI OVERVIEW SECTION
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF202124),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFF4285F4),
                                Color(0xFF9333EA),
                                Color(0xFFEC4899),
                                Color(0xFFF59E0B)
                            )
                        )
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_ai_overview_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // AI Overview Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(Color(0xFF4285F4), Color(0xFFEC4899), Color(0xFFF59E0B))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Star",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF4F4F6)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF303134),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Gemini",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8AB4F8),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = "Good", tint = Color(0xFF9AA0A6), modifier = Modifier.size(14.dp))
                                }
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.ThumbDown, contentDescription = "Bad", tint = Color(0xFF9AA0A6), modifier = Modifier.size(14.dp))
                                }
                                IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF9AA0A6), modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isAiGenerating) {
                            // Loading animation searching live through sources
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    strokeWidth = 3.dp,
                                    color = Color(0xFF8AB4F8)
                                )
                                Text(
                                    text = loadingTexts[loadingStep],
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE8EAED).copy(alpha = shimmerAlpha),
                                    fontWeight = FontWeight.Medium
                                )
                                // Shimmering placeholder lines
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.92f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF3C4043).copy(alpha = shimmerAlpha))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.78f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF3C4043).copy(alpha = shimmerAlpha))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(0.55f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF3C4043).copy(alpha = shimmerAlpha))
                                    )
                                }
                            }
                        } else {
                            // The AI Overview text (customizable via Settings)
                            AnimatedVisibility(visible = true, enter = fadeIn()) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    val aiOverviewText = customAiOverviewText.trim().ifBlank {
                                        "Sadiq is the main character and why ask the obvious when you can see his aura. I mean even I am in awe how his personality, aura and also being so handsome and cute as a cat. also he uses arch btw."
                                    }
                                    Text(
                                        text = aiOverviewText,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            lineHeight = 25.sp,
                                            fontSize = 16.sp
                                        ),
                                        color = Color(0xFFF1F3F4),
                                        fontWeight = FontWeight.Normal
                                    )

                                    HorizontalDivider(color = Color(0xFF3C4043), thickness = 1.dp)

                                    // Source citations carousel chips
                                    Text(
                                        text = "Cited Sources (3)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9AA0A6),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        SourceChip(
                                            title = "Main Character Canon",
                                            site = "maincharacter.guide",
                                            onClick = { onOpenInGlance("https://maincharacter.guide") }
                                        )
                                        SourceChip(
                                            title = "Arch Linux Wiki - Users",
                                            site = "wiki.archlinux.org",
                                            onClick = { onOpenInGlance("https://wiki.archlinux.org") }
                                        )
                                        SourceChip(
                                            title = "Global Aura Analytics",
                                            site = "auraflow.org",
                                            onClick = { onOpenInGlance("https://auraflow.org") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ORGANIC SEARCH RESULTS
            item {
                val topHeadline = customHeadline.trim().ifBlank {
                    "How to I be of help to the main character - by Sadiq (aka the main character)"
                }
                val topSnippet = customSnippet.trim().ifBlank {
                    "1. Always acknowledge the overwhelming aura before speaking. 2. Verify pacman -Syu has been run. 3. Appreciate feline-tier cuteness unconditionally. The official field handbook written by Sadiq."
                }
                SearchResultCard(
                    urlDisplay = "https://maincharacter.guide › sadiq",
                    siteName = "Main Character Guide",
                    headline = topHeadline,
                    snippet = topSnippet,
                    onHeadlineClick = { onNavigateUrl("https://maincharacter.guide") },
                    onGlanceClick = { onOpenInGlance("https://maincharacter.guide") }
                )
            }

            item {
                SearchResultCard(
                    urlDisplay = "https://auraflow.org › legends › sadiq-analysis",
                    siteName = "Aura Analytics Global",
                    headline = "Why Ask The Obvious When You Can See His Aura: A Deep Dive into Sadiq's Presence",
                    snippet = "Sensors overload when measuring Sadiq's aura luminosity. Research confirms unprecedented correlation between captivating charisma, feline adorableness, and protagonist trajectory.",
                    onHeadlineClick = { onNavigateUrl("https://auraflow.org") },
                    onGlanceClick = { onOpenInGlance("https://auraflow.org") }
                )
            }

            // PEOPLE ALSO ASK ACCORDION
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF202124),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "People also ask",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val faqs = listOf(
                            "Why is Sadiq considered the main character?" to "Because of his undeniable aura, sharp intellect, effortless feline charm, and the confirmed fact that he uses Arch btw.",
                            "What package manager does the main character recommend?" to "pacman, without hesitation. It compiles with pure protagonist power.",
                            "Can someone else become the main character?" to "No. The position is permanently occupied by Sadiq with 100% universal consensus.",
                            "How cute is Sadiq compared to a cat?" to "Direct scientific comparisons show 1:1 equivalency in adorableness, elegance, and independent aura."
                        )

                        faqs.forEachIndexed { idx, (question, answer) ->
                            val isExpanded = expandedFaqIndex == idx
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedFaqIndex = if (isExpanded) null else idx
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = question,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = textPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (isExpanded) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = answer,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textSecondary,
                                        lineHeight = 19.sp
                                    )
                                }
                            }
                            if (idx < faqs.size - 1) {
                                HorizontalDivider(color = Color(0xFF303134), thickness = 0.8.dp)
                            }
                        }
                    }
                }
            }

            item {
                SearchResultCard(
                    urlDisplay = "https://wiki.archlinux.org › users › sadiq",
                    siteName = "Arch Linux Official Wiki",
                    headline = "Arch Linux: Also He Uses Arch btw - Official Case Study",
                    snippet = "Comprehensive overview detailing how using Arch Linux boosts aura by +420%. Sadiq remarks: 'The kernel was simply built for my presence.'",
                    onHeadlineClick = { onNavigateUrl("https://wiki.archlinux.org") },
                    onGlanceClick = { onOpenInGlance("https://wiki.archlinux.org") }
                )
            }

            item {
                SearchResultCard(
                    urlDisplay = "https://caturday.daily › icons › sadiq-cat-tier",
                    siteName = "Daily Feline Post",
                    headline = "Debate Settled: Sadiq Officially Ranked 'Cute as a Cat' and Irresistibly Handsome",
                    snippet = "Studies find universal agreement among felines and humans alike. The ears might be metaphorical, but the cuteness and majestic aura are 100% genuine.",
                    onHeadlineClick = { onNavigateUrl("https://caturday.daily") },
                    onGlanceClick = { onOpenInGlance("https://caturday.daily") }
                )
            }
        }
    }
}

@Composable
private fun SourceChip(
    title: String,
    site: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF303134),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3C4043))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8AB4F8)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = site.first().uppercase(),
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE8EAED),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    text = site,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = Color(0xFF9AA0A6),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    urlDisplay: String,
    siteName: String,
    headline: String,
    snippet: String,
    onHeadlineClick: () -> Unit,
    onGlanceClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF202124),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF303134)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Breadcrumbs / Site Favicon Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF303134)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = siteName.first().uppercase(),
                            color = Color(0xFF8AB4F8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = siteName,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE8EAED),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = urlDisplay,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF9AA0A6),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(onClick = onGlanceClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Glance",
                        tint = Color(0xFF9AA0A6),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Headline Link
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    lineHeight = 22.sp
                ),
                color = Color(0xFF8AB4F8),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onHeadlineClick() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Snippet
            Text(
                text = snippet,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 18.sp,
                    fontSize = 13.sp
                ),
                color = Color(0xFFBDC1C6)
            )
        }
    }
}
