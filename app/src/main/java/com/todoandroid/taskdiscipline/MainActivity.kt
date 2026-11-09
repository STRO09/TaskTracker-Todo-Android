package com.todoandroid.taskdiscipline

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.todoandroid.taskdiscipline.data.local.TaskDatabase
import com.todoandroid.taskdiscipline.data.local.UserPreferencesRepository
import com.todoandroid.taskdiscipline.data.repository.TaskRepositoryImpl
import com.todoandroid.taskdiscipline.ui.main.MainScreen
import com.todoandroid.taskdiscipline.ui.main.MainViewModel
import com.todoandroid.taskdiscipline.ui.main.MainViewModelFactory
import com.todoandroid.taskdiscipline.ui.settings.SettingsScreen
import com.todoandroid.taskdiscipline.ui.settings.SettingsViewModel
import com.todoandroid.taskdiscipline.ui.settings.SettingsViewModelFactory
import com.todoandroid.taskdiscipline.ui.theme.TodoAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = TaskDatabase.getInstance(applicationContext)
        val userPrefs = UserPreferencesRepository(applicationContext)
        val repository = TaskRepositoryImpl(database.taskDao(), userPrefs)

        setContent {
            TodoAndroidTheme {
                AppNavigation(repository = repository)
            }
        }
    }
}

@Composable
fun AppNavigation(repository: TaskRepositoryImpl) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            val mainViewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository, navController.context)
            )
            MainScreen(
                viewModel = mainViewModel,
                onOpenSettings = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModelFactory(repository)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
