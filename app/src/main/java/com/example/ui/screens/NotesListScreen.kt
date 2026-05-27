package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Category
import com.example.data.model.Note
import com.example.security.BiometricHelper
import com.example.ui.components.NoteCard
import com.example.ui.components.SearchBar
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val categories by viewModel.allCategories.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderName by remember { mutableStateOf("") }
    val presetColors = listOf("#000000", "#7F7F7F", "#E03B2B", "#FCA311", "#1E96FC", "#00A896", "#9C27B0")
    var selectedColor by remember { mutableStateOf(presetColors[0]) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var selectedNoteForActions by remember { mutableStateOf<Note?>(null) }
    var showActionsDialog by remember { mutableStateOf(false) }
    var showCategorySelectionDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Notes",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 24.sp
                    )
                },
                actions = {
                    // Export / Import Options Menu
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        menuExpanded = true
                    }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Backup Notes (Export JSON)") },
                            onClick = {
                                menuExpanded = false
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                val backupStr = viewModel.exportBackupJson(notes, categories)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Notes Backup", backupStr)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()

                                // Share backup option
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Minimal Notes Backup")
                                    putExtra(Intent.EXTRA_TEXT, backupStr)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Backup"))
                            },
                            leadingIcon = { Icon(Icons.Default.Upload, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Restore Notes (Import JSON)") },
                            onClick = {
                                menuExpanded = false
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                importText = ""
                                showImportDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    // Create basic blank note and edit
                    onNoteClick(Note(id = -1, title = "", content = ""))
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_note_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create note",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Elegant real-time Search Box
            SearchBar(
                query = searchQuery,
                onQueryChange = { viewModel.search(it) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Horizontally Scrollable Folder Tags (System of Horizontal Pills)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.size(8.dp))
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Folders divider",
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        // "All" Folder Tag
                        val isAllSelected = selectedId == null
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isAllSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.selectCategory(null)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "All",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAllSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    items(categories) { cat ->
                        val isSelected = selectedId == cat.id
                        val catColor = Color(android.graphics.Color.parseColor(cat.colorHex))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) catColor else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    viewModel.selectCategory(cat.id)
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.onPrimary else catColor,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    cat.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        // If selected color is dark, make text white, if selected color is light, make text black
                                        if (cat.colorHex == "#FFFFFF" || cat.colorHex == "#E0E0E0") Color.Black else Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                // Add Folder Button (+)
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        folderName = ""
                        selectedColor = presetColors[0]
                        showCreateFolderDialog = true
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New folder",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes List Block
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "No Notes",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No match for \"$searchQuery\"" else "No notes found",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap + to write down your first note",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(notes) { note ->
                        val cat = categories.find { it.id == note.categoryId }
                        NoteCard(
                            note = note,
                            category = cat,
                            onClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                // If locked, require biometric prompt
                                if (note.isLocked && !viewModel.isNoteUnlocked(note.id)) {
                                    BiometricHelper.authenticate(
                                        context = context,
                                        title = note.title.ifBlank { "Locked Note" },
                                        subtitle = "Please verify your identity to view note",
                                        onSuccess = {
                                            viewModel.unlockNoteSession(note.id)
                                            onNoteClick(note)
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    onNoteClick(note)
                                }
                            },
                            onLongClick = {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                selectedNoteForActions = note
                                showActionsDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // CREATE FOLDER DIALOG
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create Folder") },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        label = { Text("Folder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Select Minimalist Label Color:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetColors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .border(
                                        width = if (selectedColor == hex) 3.dp else 1.dp,
                                        color = if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                        selectedColor = hex
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (folderName.isNotBlank()) {
                            viewModel.createCategory(folderName.trim(), selectedColor)
                            showCreateFolderDialog = false
                        } else {
                            Toast.makeText(context, "Folder name cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ACTIONS SHEET DIALOG (LONG PRESS NOTE)
    if (showActionsDialog && selectedNoteForActions != null) {
        val note = selectedNoteForActions!!
        AlertDialog(
            onDismissRequest = { showActionsDialog = false },
            title = { Text(note.title.ifBlank { "Note Actions" }, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pin / Unpin Action
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.togglePin(note)
                            showActionsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (note.isPinned) "Unpin Note" else "Pin Note to Top")
                        }
                    }

                    // Lock / Unlock Action
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            BiometricHelper.authenticate(
                                context = context,
                                title = "Lock/Unlock",
                                subtitle = "Verify identity to change security status",
                                onSuccess = {
                                    viewModel.toggleLock(note)
                                    showActionsDialog = false
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = if (note.isLocked) Icons.Default.LockOpen else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(if (note.isLocked) "Remove Lock Screen" else "Protect with Biometrics Lock")
                        }
                    }

                    // Move to Category Folder Action
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            showCategorySelectionDialog = true
                            showActionsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Set Folder/Category")
                        }
                    }

                    // Share note contents Action
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            val shareStripped = "--- ${note.title} ---\n\n${note.content}"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, note.title)
                                putExtra(Intent.EXTRA_TEXT, shareStripped)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share note"))
                            showActionsDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Share raw note")
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                    // Delete Action
                    TextButton(
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            viewModel.deleteNote(note)
                            showActionsDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Delete Note")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // CATEGORY ASSIGNMENT SELECTION DIALOG
    if (showCategorySelectionDialog && selectedNoteForActions != null) {
        val note = selectedNoteForActions!!
        AlertDialog(
            onDismissRequest = { showCategorySelectionDialog = false },
            title = { Text("Assign Folder") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        TextButton(
                            onClick = {
                                viewModel.updateNote(note.copy(categoryId = null))
                                showCategorySelectionDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("No Category (Personal)", fontWeight = FontWeight.Bold)
                        }
                    }
                    items(categories) { cat ->
                        TextButton(
                            onClick = {
                                viewModel.updateNote(note.copy(categoryId = cat.id))
                                showCategorySelectionDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cat.name, color = MaterialTheme.colorScheme.onSurface)
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color(android.graphics.Color.parseColor(cat.colorHex)), CircleShape)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategorySelectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // IMPORT RESTORE DIALOG
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import/Restore Notes") },
            text = {
                Column {
                    Text(
                        "Paste a valid Minimal Notes Backup JSON string below to restore notes and categories.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("{ \"version\": 1, ... }") },
                        label = { Text("Backup JSON Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (importText.isNotBlank()) {
                            viewModel.importBackupJson(
                                jsonString = importText,
                                onSuccess = {
                                    Toast.makeText(context, "Notes successfully imported and restored!", Toast.LENGTH_LONG).show()
                                    showImportDialog = false
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Import failed: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Restore Database")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
