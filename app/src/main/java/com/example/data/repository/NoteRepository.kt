package com.example.data.repository

import com.example.data.database.CategoryDao
import com.example.data.database.NoteDao
import com.example.data.model.Category
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao
) {
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    fun getNotesByCategory(categoryId: Int): Flow<List<Note>> =
        noteDao.getNotesByCategory(categoryId)

    fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes(query)

    suspend fun getNoteById(id: Int): Note? =
        noteDao.getNoteById(id)

    suspend fun insertNote(note: Note): Long =
        noteDao.insertNote(note)

    suspend fun updateNote(note: Note) =
        noteDao.updateNote(note)

    suspend fun deleteNote(note: Note) =
        noteDao.deleteNote(note)

    suspend fun deleteNoteById(id: Int) =
        noteDao.deleteById(id)

    suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category)

    suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category)

    suspend fun deleteCategoryById(id: Int) =
        categoryDao.deleteById(id)
}
