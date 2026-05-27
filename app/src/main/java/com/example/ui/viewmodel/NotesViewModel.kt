package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.Note
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    val allCategories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedCategoryId = MutableStateFlow<Int?>(null)
    val searchQuery = MutableStateFlow("")
    val unlockedNoteIds = MutableStateFlow<Set<Int>>(emptySet())

    val filteredNotes: StateFlow<List<Note>> = combine(
        repository.allNotes,
        selectedCategoryId,
        searchQuery
    ) { notes, selectedId, query ->
        notes.filter { note ->
            val matchesCategory = selectedId == null || note.categoryId == selectedId
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectCategory(categoryId: Int?) {
        selectedCategoryId.value = categoryId
    }

    fun search(query: String) {
        searchQuery.value = query
    }

    // Unlocking a note for the current session
    fun unlockNoteSession(noteId: Int) {
        unlockedNoteIds.value = unlockedNoteIds.value + noteId
    }

    fun lockNoteSession(noteId: Int) {
        unlockedNoteIds.value = unlockedNoteIds.value - noteId
    }

    fun isNoteUnlocked(noteId: Int): Boolean {
        return unlockedNoteIds.value.contains(noteId)
    }

    fun insertNote(title: String, content: String, categoryId: Int? = null, isPinned: Boolean = false, isLocked: Boolean = false) {
        viewModelScope.launch {
            val note = Note(
                title = title,
                content = content,
                categoryId = categoryId,
                isPinned = isPinned,
                isLocked = isLocked,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            repository.insertNote(note)
        }
    }

    fun createCategory(name: String, colorHex: String = "#000000") {
        viewModelScope.launch {
            repository.insertCategory(Category(name = name, colorHex = colorHex))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(updatedAt = System.currentTimeMillis())
            repository.updateNote(updated)
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(
                isPinned = !note.isPinned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
        }
    }

    fun toggleLock(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(
                isLocked = !note.isLocked,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
        }
    }

    fun deleteCategory(categoryId: Int) {
        viewModelScope.launch {
            repository.deleteCategoryById(categoryId)
            if (selectedCategoryId.value == categoryId) {
                selectedCategoryId.value = null
            }
        }
    }

    // Export notes to JSON string
    fun exportBackupJson(notesList: List<Note>, categoriesList: List<Category>): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": 1,\n")
        sb.append("  \"categories\": [\n")
        categoriesList.forEachIndexed { index, cat ->
            sb.append("    {\n")
            sb.append("      \"id\": ${cat.id},\n")
            sb.append("      \"name\": \"${escapeJson(cat.name)}\",\n")
            sb.append("      \"colorHex\": \"${escapeJson(cat.colorHex)}\"\n")
            sb.append("    }${if (index < categoriesList.size - 1) "," else ""}\n")
        }
        sb.append("  ],\n")
        sb.append("  \"notes\": [\n")
        notesList.forEachIndexed { index, note ->
            sb.append("    {\n")
            sb.append("      \"title\": \"${escapeJson(note.title)}\",\n")
            sb.append("      \"content\": \"${escapeJson(note.content)}\",\n")
            sb.append("      \"categoryId\": ${note.categoryId ?: "null"},\n")
            sb.append("      \"isPinned\": ${note.isPinned},\n")
            sb.append("      \"isLocked\": ${note.isLocked}\n")
            sb.append("    }${if (index < notesList.size - 1) "," else ""}\n")
        }
        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    fun importBackupJson(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val root = org.json.JSONObject(jsonString)
            val categoriesArray = root.optJSONArray("categories")
            val notesArray = root.optJSONArray("notes")

            viewModelScope.launch {
                val categoryIdMap = mutableMapOf<Int, Int>()

                if (categoriesArray != null) {
                    for (i in 0 until categoriesArray.length()) {
                        val catObj = categoriesArray.getJSONObject(i)
                        val oldId = catObj.getInt("id")
                        val name = catObj.getString("name")
                        val colorHex = catObj.optString("colorHex", "#000000")

                        val newId = repository.insertCategory(Category(name = name, colorHex = colorHex))
                        categoryIdMap[oldId] = newId.toInt()
                    }
                }

                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        val noteObj = notesArray.getJSONObject(i)
                        val title = noteObj.getString("title")
                        val content = noteObj.getString("content")
                        val oldCategoryId = if (noteObj.isNull("categoryId")) null else noteObj.getInt("categoryId")
                        val isPinned = noteObj.optBoolean("isPinned", false)
                        val isLocked = noteObj.optBoolean("isLocked", false)

                        val newCategoryId = oldCategoryId?.let { categoryIdMap[it] }

                        repository.insertNote(
                            Note(
                                title = title,
                                content = content,
                                categoryId = newCategoryId,
                                isPinned = isPinned,
                                isLocked = isLocked,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    }
                }
                onSuccess()
            }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Invalid JSON file structure")
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

class NotesViewModelFactory(private val repository: NoteRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
