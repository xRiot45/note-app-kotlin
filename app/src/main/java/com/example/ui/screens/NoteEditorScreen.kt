package com.example.ui.screens

import android.content.Intent
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Note
import com.example.security.BiometricHelper
import com.example.ui.components.EditorToolbar
import com.example.ui.components.MarkdownText
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NotesViewModel,
    noteId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current

    val categories by viewModel.allCategories.collectAsStateWithLifecycle()

    // Find note in-memory or from list state
    val notes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val existingNote = remember(noteId, notes) {
        notes.find { it.id == noteId } ?: Note(id = -1, title = "", content = "")
    }

    var title by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var isPinned by remember { mutableStateOf(false) }
    var isLocked by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var previewMode by remember { mutableStateOf(false) }

    // On launch, load data from existingNote
    LaunchedEffect(existingNote) {
        if (title.isEmpty() && contentValue.text.isEmpty()) {
            title = existingNote.title
            contentValue = TextFieldValue(existingNote.content)
            isPinned = existingNote.isPinned
            isLocked = existingNote.isLocked
            selectedCategoryId = existingNote.categoryId
        }
    }

    fun handleSave() {
        // Prevent saving duplicates if note exists
        val updatedNote = existingNote.copy(
            title = title,
            content = contentValue.text,
            categoryId = selectedCategoryId,
            isPinned = isPinned,
            isLocked = isLocked,
            updatedAt = System.currentTimeMillis()
        )
        if (noteId == -1) {
            viewModel.insertNote(
                title = updatedNote.title,
                content = updatedNote.content,
                categoryId = updatedNote.categoryId,
                isPinned = updatedNote.isPinned,
                isLocked = updatedNote.isLocked
            )
        } else {
            viewModel.updateNote(updatedNote)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Editor",
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        handleSave()
                        onBack()
                    }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back and Save")
                    }
                },
                actions = {
                    // PIN Toggle
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        isPinned = !isPinned
                        Toast.makeText(context, if (isPinned) "Note pinned" else "Note unpinned", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Default.PushPin else Icons.Default.PushPin,
                            contentDescription = "Pin Toggle",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    // LOCK Toggle (Biometrics authenticated)
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        BiometricHelper.authenticate(
                            context = context,
                            title = "Security Settings",
                            subtitle = "Verify to lock or unlock this note",
                            onSuccess = {
                                isLocked = !isLocked
                                if (isLocked && existingNote.id != -1) {
                                    viewModel.unlockNoteSession(existingNote.id)
                                }
                                Toast.makeText(context, if (isLocked) "Secure lock enabled" else "Secure lock disabled", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock Toggle",
                            tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    // Markdown Preview Toggle
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        previewMode = !previewMode
                    }) {
                        Icon(
                            imageVector = if (previewMode) Icons.Default.EditNote else Icons.Default.RemoveRedEye,
                            contentDescription = "Preview Toggle",
                            tint = if (previewMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Share and Export single note block
                    IconButton(onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        // Export as clean markdown
                        val mdString = buildString {
                            append("# ").append(title.ifBlank { "Untitled" }).append("\n\n")
                            append(contentValue.text)
                        }
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/markdown"
                            putExtra(Intent.EXTRA_SUBJECT, "$title.md")
                            putExtra(Intent.EXTRA_TEXT, mdString)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share as Markdown (.md)"))
                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export raw markdown")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Category/Folder Assignment Select row if folders exist
            if (categories.isNotEmpty()) {
                var folderMenuExpanded by remember { mutableStateOf(false) }
                val currentCategory = categories.find { it.id == selectedCategoryId }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Folder: ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                    Box(
                        modifier = Modifier
                            .background(
                                color = currentCategory?.let { Color(android.graphics.Color.parseColor(it.colorHex)).copy(alpha = 0.15f) } ?: MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = currentCategory?.let { Color(android.graphics.Color.parseColor(it.colorHex)).copy(alpha = 0.4f) } ?: MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { folderMenuExpanded = true }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = currentCategory?.name ?: "Personal (None)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = currentCategory?.let { Color(android.graphics.Color.parseColor(it.colorHex)) } ?: MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = folderMenuExpanded,
                        onDismissRequest = { folderMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedCategoryId = null
                                folderMenuExpanded = false
                            }
                        )
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(Color(android.graphics.Color.parseColor(cat.colorHex)), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(cat.name)
                                    }
                                },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    folderMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Standard Title Input Box
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Note Title", fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input")
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

            if (previewMode) {
                // Formatting preview wrapper
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(vertical = 12.dp)
                ) {
                    MarkdownText(
                        text = if (contentValue.text.replace("\n", "").trim().isEmpty()) "_No content preview available._" else contentValue.text,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                // Native Markdown wrapping edit stream
                TextField(
                    value = contentValue,
                    onValueChange = { contentValue = it },
                    placeholder = { Text("Write your thoughts down, supports Markdown (e.g. **bold**, *italic*, `code`, # Title)...") },
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag("note_content_input")
                )

                // Inline markdown wrapping bar
                EditorToolbar(
                    onFormattingClick = { syntax ->
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

                        val text = contentValue.text
                        val selection = contentValue.selection
                        val start = selection.start
                        val end = selection.end

                        contentValue = if (start != end) {
                            val selectedText = text.substring(start, end)
                            val wrapped = if (syntax == "- " || syntax == "# " || syntax == "## ") {
                                "\n$syntax$selectedText"
                            } else {
                                "$syntax$selectedText$syntax"
                            }
                            val replaced = text.replaceRange(start, end, wrapped)
                            TextFieldValue(
                                text = replaced,
                                selection = TextRange(start + wrapped.length)
                            )
                        } else {
                            val added = if (syntax == "- " || syntax == "# " || syntax == "## ") {
                                "\n$syntax"
                            } else {
                                "$syntax$syntax"
                            }
                            val replaced = text.replaceRange(start, start, added)
                            // If it added a wrapping syntax (like ** or * or `), position selection cursor in center!
                            val newSelect = if (syntax == "**" || syntax == "*" || syntax == "`") {
                                start + syntax.length
                            } else {
                                start + added.length
                            }
                            TextFieldValue(
                                text = replaced,
                                selection = TextRange(newSelect)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}
