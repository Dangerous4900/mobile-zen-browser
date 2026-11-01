package com.example.ui.components

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkspaceEntity
import com.example.tabs.ImportedTab
import com.example.tabs.ZenTabImporter

enum class ImportMode {
    FILE,
    PASTE_URLS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZenImportTabsDialog(
    workspaces: List<WorkspaceEntity>,
    activeWorkspaceId: String,
    onDismiss: () -> Unit,
    onImportTabs: (tabs: List<ImportedTab>, targetWorkspaceId: String, switchToFirst: Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var mode by remember { mutableStateOf(ImportMode.FILE) }
    var selectedWorkspaceId by remember(activeWorkspaceId) { mutableStateOf(activeWorkspaceId) }
    var rawPastedText by remember { mutableStateOf("") }
    val detectedTabs = remember { mutableStateListOf<ImportedTab>() }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                selectedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Export File"
                val stream = context.contentResolver.openInputStream(uri)
                val content = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val parsed = ZenTabImporter.parse(content, "Imported File")
                detectedTabs.clear()
                if (parsed.isEmpty()) {
                    errorMessage = "No valid tab URLs or bookmarks found in this file."
                } else {
                    errorMessage = null
                    detectedTabs.addAll(parsed)
                }
            } catch (e: Exception) {
                errorMessage = "Failed to read file: ${e.localizedMessage ?: "Unknown error"}"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F0F13),
        modifier = Modifier.testTag("zen_import_tabs_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Import Tabs",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF4F4F6)
                        )
                        Text(
                            text = "From Chrome, Firefox, Brave, Safari, Edge or text lists",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFA1A1AA)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFA1A1AA)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: Pick File vs Paste URLs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF18181D))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { mode = ImportMode.FILE },
                    shape = RoundedCornerShape(8.dp),
                    color = if (mode == ImportMode.FILE) Color(0xFF27272A) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_import_mode_file")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = if (mode == ImportMode.FILE) Color(0xFF60A5FA) else Color(0xFFA1A1AA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "From Export File",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (mode == ImportMode.FILE) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == ImportMode.FILE) Color(0xFFF4F4F6) else Color(0xFFA1A1AA)
                        )
                    }
                }

                Surface(
                    onClick = { mode = ImportMode.PASTE_URLS },
                    shape = RoundedCornerShape(8.dp),
                    color = if (mode == ImportMode.PASTE_URLS) Color(0xFF27272A) else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tab_import_mode_paste")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = if (mode == ImportMode.PASTE_URLS) Color(0xFF60A5FA) else Color(0xFFA1A1AA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste URLs / Text",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (mode == ImportMode.PASTE_URLS) FontWeight.Bold else FontWeight.Normal,
                            color = if (mode == ImportMode.PASTE_URLS) Color(0xFFF4F4F6) else Color(0xFFA1A1AA)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (mode) {
                ImportMode.FILE -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF141419),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Select Browser HTML Bookmarks or Session JSON",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF4F4F6)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Export bookmarks or open tabs from Chrome (Bookmarks Manager -> Export bookmarks), Firefox, Edge, Safari, Brave, or OneTab/Session Buddy.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA1A1AA)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    filePickerLauncher.launch(arrayOf("text/html", "application/json", "text/plain", "*/*"))
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2563EB),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("choose_export_file_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (selectedFileName == null) "Select Export File (.html / .json / .txt)" else "Change File ($selectedFileName)")
                            }
                        }
                    }
                }
                ImportMode.PASTE_URLS -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paste URLs (one per line, or URL | Title):",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFA1A1AA)
                            )
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text.orEmpty()
                                    if (clip.isNotBlank()) {
                                        rawPastedText = clip
                                        val parsed = ZenTabImporter.parse(clip, "Pasted")
                                        detectedTabs.clear()
                                        detectedTabs.addAll(parsed)
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Paste Clipboard", fontSize = 11.sp)
                            }
                        }

                        OutlinedTextField(
                            value = rawPastedText,
                            onValueChange = { newText ->
                                rawPastedText = newText
                                val parsed = ZenTabImporter.parse(newText, "Pasted")
                                detectedTabs.clear()
                                detectedTabs.addAll(parsed)
                            },
                            placeholder = {
                                Text(
                                    "https://github.com | GitHub\nhttps://reddit.com/r/android\nhttps://wikipedia.org",
                                    color = Color(0xFF71717A),
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("import_paste_textfield"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF141419),
                                unfocusedContainerColor = Color(0xFF141419),
                                focusedTextColor = Color(0xFFF4F4F6),
                                unfocusedTextColor = Color(0xFFF4F4F6),
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFF27272A)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = Color(0xFFEF4444),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Target Workspace Picker
            if (workspaces.isNotEmpty()) {
                Text(
                    text = "Import Into Workspace",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFA1A1AA)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(workspaces) { ws ->
                        val isSelected = ws.id == selectedWorkspaceId
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedWorkspaceId = ws.id },
                            label = { Text(ws.name) },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF2563EB),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF1E1E24),
                                labelColor = Color(0xFFA1A1AA)
                            )
                        )
                    }
                }
            }

            // Preview of Detected Tabs
            AnimatedVisibility(visible = detectedTabs.isNotEmpty()) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detected Tabs (${detectedTabs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF4F4F6)
                        )
                        Text(
                            text = "Ready to import",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF141419),
                        border = BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(detectedTabs) { tab ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF1E1E24))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = Color(0xFF60A5FA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFF4F4F6),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = tab.url,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA1A1AA),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { detectedTabs.remove(tab) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
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

            Spacer(modifier = Modifier.height(18.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFA1A1AA)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (detectedTabs.isEmpty()) {
                            Toast.makeText(context, "No tabs found to import", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onImportTabs(detectedTabs.toList(), selectedWorkspaceId, true)
                        onDismiss()
                    },
                    enabled = detectedTabs.isNotEmpty(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("confirm_import_tabs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (detectedTabs.isEmpty()) "Import Tabs" else "Import (${detectedTabs.size}) Tabs"
                    )
                }
            }
        }
    }
}
