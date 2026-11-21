package com.example.ui.components

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ViewSidebar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.SearchEngine
import com.example.data.SidebarPosition
import com.example.data.StartPageGradientPreset
import com.example.data.UrlBarPosition
import com.example.ui.ZenBrowserUiState

fun isAppDefaultBrowser(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        rm?.isRoleAvailable(RoleManager.ROLE_BROWSER) == true &&
            rm.isRoleHeld(RoleManager.ROLE_BROWSER)
    } else {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        val ri = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ri?.activityInfo?.packageName == context.packageName
    }
}

fun launchDefaultBrowserPrompt(
    context: Context,
    roleLauncher: ((Intent) -> Unit)? = null
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_BROWSER) && !rm.isRoleHeld(RoleManager.ROLE_BROWSER)) {
            if (roleLauncher != null) {
                runCatching {
                    roleLauncher(rm.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                    return
                }
            }
        }
    }
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    } else true
}

fun openBatteryOptimizationSettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure {
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ZenSetupWizardDialog(
    uiState: ZenBrowserUiState,
    onDismiss: () -> Unit,
    onCompleteSetup: (
        searchEngineId: String,
        sidebarPosition: SidebarPosition,
        urlBarPosition: UrlBarPosition,
        startPageGradientId: String,
        backgroundPlayEnabled: Boolean,
        playInWindowPipEnabled: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val totalSteps = 6

    var selectedEngineId by remember { mutableStateOf(uiState.privacyConfig.normalSearchEngineId) }
    var selectedSidebarPos by remember { mutableStateOf(uiState.sidebarConfig.position) }
    var selectedUrlBarPos by remember { mutableStateOf(uiState.sidebarConfig.urlBarPosition) }
    var selectedGradientId by remember {
        mutableStateOf(
            uiState.sidebarConfig.startPageGradientId.takeIf { it != StartPageGradientPreset.PITCH_BLACK_AMOLED.id }
                ?: StartPageGradientPreset.CYBERWAVE_NEON.id
        )
    }
    var bgPlayEnabled by remember { mutableStateOf(uiState.sidebarConfig.backgroundPlayEnabled) }
    var pipEnabled by remember { mutableStateOf(uiState.sidebarConfig.playInWindowPipEnabled) }
    var defaultBrowserActive by remember { mutableStateOf(isAppDefaultBrowser(context)) }
    var notifGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var batteryUnrestricted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        defaultBrowserActive = isAppDefaultBrowser(context)
    }
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notifGranted = granted
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF101014),
            border = BorderStroke(1.dp, Color(0xFF27272E)),
            modifier = Modifier
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("zen_tutorial_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Step Progress Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(totalSteps) { idx ->
                        val active = idx <= step
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (idx == step) Color(0xFF60A5FA)
                                    else if (active) Color(0xFF3B82F6).copy(alpha = 0.65f)
                                    else Color(0xFF27272E)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step Content
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "setup_wizard_step"
                    ) { curStep ->
                        when (curStep) {
                            0 -> WizardWelcomeStep()
                            1 -> WizardSearchEngineStep(
                                selectedEngineId = selectedEngineId,
                                onSelect = { selectedEngineId = it }
                            )
                            2 -> WizardSidePanelStep(
                                sidebarPosition = selectedSidebarPos,
                                urlBarPosition = selectedUrlBarPos,
                                onSelectPosition = { selectedSidebarPos = it },
                                onSelectUrlBar = { selectedUrlBarPos = it }
                            )
                            3 -> WizardGradientStep(
                                selectedGradientId = selectedGradientId,
                                onSelectGradient = { selectedGradientId = it }
                            )
                            4 -> WizardDefaultBrowserStep(
                                isDefault = defaultBrowserActive,
                                onSetDefault = {
                                    launchDefaultBrowserPrompt(context) { intent ->
                                        roleLauncher.launch(intent)
                                    }
                                    defaultBrowserActive = isAppDefaultBrowser(context)
                                }
                            )
                            else -> WizardPermissionsStep(
                                notifGranted = notifGranted,
                                batteryUnrestricted = batteryUnrestricted,
                                bgPlayEnabled = bgPlayEnabled,
                                pipEnabled = pipEnabled,
                                onRequestNotifications = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                onOpenBatterySettings = {
                                    openBatteryOptimizationSettings(context)
                                    batteryUnrestricted = isIgnoringBatteryOptimizations(context)
                                },
                                onToggleBgPlay = { bgPlayEnabled = it },
                                onTogglePip = { pipEnabled = it }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("tutorial_skip_button")
                        ) {
                            Text("Skip", color = Color(0xFFA1A1AA))
                        }
                        if (step > 0) {
                            TextButton(onClick = { step-- }) {
                                Text("Back", color = Color(0xFFD4D4D8))
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (step < totalSteps - 1) {
                                step++
                            } else {
                                onCompleteSetup(
                                    selectedEngineId,
                                    selectedSidebarPos,
                                    selectedUrlBarPos,
                                    selectedGradientId,
                                    bgPlayEnabled,
                                    pipEnabled
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF4F4F6),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("tutorial_next_or_done_button")
                    ) {
                        Text(
                            text = if (step < totalSteps - 1) "Continue" else "Finish Setup",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E293B),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF4F4F6)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFA1A1AA)
            )
        }
    }
}

@Composable
private fun WizardWelcomeStep() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WizardHeader(
            icon = Icons.Default.ViewSidebar,
            title = "Welcome to Zen Browser",
            subtitle = "Let's configure your browser in under a minute"
        )
        listOf(
            Icons.Default.Search to "Pick your default search engine for fast queries",
            Icons.Default.ViewSidebar to "Choose your side panel side & toolbar layout",
            Icons.Default.ColorLens to "Pick a vibrant gradient theme for your start page",
            Icons.Default.PlayCircle to "Enable background playback, auto-PiP & permissions"
        ).forEach { (ic, desc) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF17171D),
                border = BorderStroke(1.dp, Color(0xFF272730)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = ic,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE4E4E7)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WizardSearchEngineStep(
    selectedEngineId: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WizardHeader(
            icon = Icons.Default.Search,
            title = "Search Engine",
            subtitle = "Choose your primary search provider"
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            SearchEngine.entries.forEach { engine ->
                val isSelected = selectedEngineId == engine.id
                Surface(
                    onClick = { onSelect(engine.id) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF1E293B) else Color(0xFF17171D),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color(0xFF60A5FA) else Color(0xFF272730)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.47f)
                        .testTag("wizard_engine_${engine.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = engine.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F6),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = engine.homeDomain,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardSidePanelStep(
    sidebarPosition: SidebarPosition,
    urlBarPosition: UrlBarPosition,
    onSelectPosition: (SidebarPosition) -> Unit,
    onSelectUrlBar: (UrlBarPosition) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WizardHeader(
            icon = Icons.Default.ViewSidebar,
            title = "Side Panel & Toolbars",
            subtitle = "Customize panel placement & navigation bar"
        )
        Text(
            text = "Side Panel Edge",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFA1A1AA)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SidebarPosition.entries.forEach { pos ->
                val selected = sidebarPosition == pos
                Surface(
                    onClick = { onSelectPosition(pos) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) Color(0xFF1E293B) else Color(0xFF17171D),
                    border = BorderStroke(1.dp, if (selected) Color(0xFF60A5FA) else Color(0xFF272730)),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (pos == SidebarPosition.LEFT) "Left Side" else "Right Side",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        Text(
            text = "Address Bar Style",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFA1A1AA)
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            UrlBarPosition.entries.forEach { pos ->
                val selected = urlBarPosition == pos
                Surface(
                    onClick = { onSelectUrlBar(pos) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) Color(0xFF1E293B) else Color(0xFF17171D),
                    border = BorderStroke(1.dp, if (selected) Color(0xFF60A5FA) else Color(0xFF272730)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = pos.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFF4F4F6)
                        )
                        if (selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF60A5FA),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WizardGradientStep(
    selectedGradientId: String,
    onSelectGradient: (String) -> Unit
) {
    val active = StartPageGradientPreset.fromId(selectedGradientId)
    val ordered = StartPageGradientPreset.entries.sortedByDescending { it.isColorful }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WizardHeader(
            icon = Icons.Default.ColorLens,
            title = "Start Page Gradient",
            subtitle = "Choose a vibrant colorful backdrop for your homepage"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(78.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(active.topHex), Color(active.midHex), Color(active.bottomHex))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = active.label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ordered.forEach { preset ->
                val isSelected = selectedGradientId == preset.id
                Surface(
                    onClick = { onSelectGradient(preset.id) },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF17171D),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) Color.White else Color(0xFF272730)
                    ),
                    modifier = Modifier.testTag("wizard_gradient_${preset.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(preset.topHex), Color(preset.midHex), Color(preset.bottomHex))
                                    )
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        )
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else Color(0xFFD4D4D8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardDefaultBrowserStep(
    isDefault: Boolean,
    onSetDefault: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        WizardHeader(
            icon = Icons.Default.Language,
            title = "Set as Default Browser",
            subtitle = "Open links from other apps directly in Zen Browser"
        )
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF17171D),
            border = BorderStroke(1.dp, if (isDefault) Color(0xFF10B981) else Color(0xFF272730)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (isDefault) Icons.Default.CheckCircle else Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isDefault) Color(0xFF10B981) else Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isDefault) "Zen Browser is your default browser"
                        else "Make Zen Browser your default web app",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF4F4F6)
                    )
                }
                Text(
                    text = "Includes strict ad & popup blocking, scroll position resume, and mobile + desktop extensions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA1A1AA)
                )
                OutlinedButton(
                    onClick = onSetDefault,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isDefault) "Default Browser Settings" else "Set as Default Browser")
                }
            }
        }
    }
}

@Composable
private fun WizardPermissionsStep(
    notifGranted: Boolean,
    batteryUnrestricted: Boolean,
    bgPlayEnabled: Boolean,
    pipEnabled: Boolean,
    onRequestNotifications: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onToggleBgPlay: (Boolean) -> Unit,
    onTogglePip: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        WizardHeader(
            icon = Icons.Default.Notifications,
            title = "Media & Permissions",
            subtitle = "Keep audio/video playing when screen turns off or switching apps"
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF17171D),
            border = BorderStroke(1.dp, Color(0xFF272730)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Background & Screen-Off Playback", color = Color(0xFFF4F4F6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Keeps music & video playing even when screen is off", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                        }
                    }
                    Switch(checked = bgPlayEnabled, onCheckedChange = onToggleBgPlay)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureInPictureAlt,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Auto Picture-in-Picture", color = Color(0xFFF4F4F6), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Float playing video in a mini window when leaving app", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                        }
                    }
                    Switch(checked = pipEnabled, onCheckedChange = onTogglePip)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRequestNotifications,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (notifGranted) "Notifications On" else "Allow Alerts", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onOpenBatterySettings,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.BatteryChargingFull, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (batteryUnrestricted) "Unrestricted" else "Battery Allow", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
