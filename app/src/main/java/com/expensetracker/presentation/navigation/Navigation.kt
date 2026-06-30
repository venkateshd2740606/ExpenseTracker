package com.expensetracker.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.expensetracker.presentation.ui.screens.personal.*

sealed class PersonalRoute(val route: String, val label: String) {
    data object Dashboard : PersonalRoute("dashboard", "Home")
    data object Transactions : PersonalRoute("transactions", "Transactions")
    data object Add : PersonalRoute("add", "Add")
}

@Composable
fun ExpenseTrackerNavHost(navController: NavHostController = rememberNavController()) {
    val items = listOf(PersonalRoute.Dashboard, PersonalRoute.Transactions)
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            if (current != PersonalRoute.Add.route) {
                NavigationBar {
                    items.forEach { route ->
                        NavigationBarItem(
                            selected = current == route.route,
                            onClick = {
                                navController.navigate(route.route) {
                                    popUpTo(PersonalRoute.Dashboard.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    when (route) {
                                        PersonalRoute.Dashboard -> Icons.Default.Home
                                        PersonalRoute.Transactions -> Icons.Default.Receipt
                                        else -> Icons.Default.Add
                                    },
                                    route.label
                                )
                            },
                            label = { Text(route.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (current != PersonalRoute.Add.route) {
                FloatingActionButton(onClick = { navController.navigate(PersonalRoute.Add.route) }) {
                    Icon(Icons.Default.Add, "Add")
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController, startDestination = PersonalRoute.Dashboard.route) {
                composable(PersonalRoute.Dashboard.route) {
                    PersonalDashboardScreen(
                        onAdd = { navController.navigate(PersonalRoute.Add.route) },
                        onViewTransactions = { navController.navigate(PersonalRoute.Transactions.route) }
                    )
                }
                composable(PersonalRoute.Transactions.route) { PersonalTransactionListScreen() }
                composable(PersonalRoute.Add.route) {
                    AddPersonalExpenseScreen(onDone = { navController.popBackStack() })
                }
            }
        }
    }
}
