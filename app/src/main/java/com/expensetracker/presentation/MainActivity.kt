package com.expensetracker.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.expensetracker.domain.repository.PreferencesRepository
import com.expensetracker.domain.model.ThemeMode
import com.expensetracker.presentation.navigation.ExpenseTrackerNavHost
import com.expensetracker.presentation.ui.theme.ExpenseTrackerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsRepo: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by prefsRepo.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val accent by prefsRepo.accentColor.collectAsState(initial = 0xFF1B8A6B)

            ExpenseTrackerTheme(themeMode = themeMode, accentColorArgb = accent) {
                ExpenseTrackerNavHost()
            }
        }
    }
}
