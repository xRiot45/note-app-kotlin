package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.repository.NoteRepository
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.NotesListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.NotesViewModelFactory

class MainActivity : FragmentActivity() {

    // Lazy initialization of Database and Repository layers
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy { NoteRepository(database.noteDao(), database.categoryDao()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Instantiate our NotesViewModel injecting Repository constructor
                val viewModel: NotesViewModel = viewModel(
                    factory = NotesViewModelFactory(repository)
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "notes_list",
        modifier = modifier
    ) {
        composable("notes_list") {
            NotesListScreen(
                viewModel = viewModel,
                onNoteClick = { note ->
                    navController.navigate("note_editor/${note.id}")
                }
            )
        }
        composable(
            route = "note_editor/{noteId}",
            arguments = listOf(
                navArgument("noteId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
            NoteEditorScreen(
                viewModel = viewModel,
                noteId = noteId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
