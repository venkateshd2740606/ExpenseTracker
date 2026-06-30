package com.expensetracker.presentation.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.ui.theme.AccentPalette
import com.expensetracker.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onLogout: () -> Unit) {
    val themeMode by viewModel.themeMode.collectAsState()
    val accent by viewModel.accentColor.collectAsState()
    val notifPrefs by viewModel.notificationPrefs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                profile?.let { p ->
                    ListItem(
                        headlineContent = { Text(p.name) },
                        supportingContent = { Text("${p.email}\n${p.mobile}") },
                        leadingContent = { Icon(Icons.Default.AccountCircle, null, modifier = Modifier.size(48.dp)) }
                    )
                }
            }
            item { Text("Theme", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(selected = themeMode == mode, onClick = { viewModel.setTheme(mode) }, label = { Text(mode.name) })
                    }
                }
            }
            item { Text("Accent Color", style = MaterialTheme.typography.titleMedium) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentPalette.forEach { color ->
                        val c = Color(color)
                        FilterChip(
                            selected = accent == color,
                            onClick = { viewModel.setAccent(color) },
                            label = { Box(Modifier.size(24.dp).padding(4.dp)) { Surface(color = c, shape = MaterialTheme.shapes.small) {} } }
                        )
                    }
                }
            }
            item { Text("Notifications", style = MaterialTheme.typography.titleMedium) }
            item {
                NotificationToggle("Expense added", notifPrefs.expenseAdded) { viewModel.setNotifications(notifPrefs.copy(expenseAdded = it)) }
                NotificationToggle("Settlement reminders", notifPrefs.settlementReminder) { viewModel.setNotifications(notifPrefs.copy(settlementReminder = it)) }
                NotificationToggle("Friend requests", notifPrefs.friendRequest) { viewModel.setNotifications(notifPrefs.copy(friendRequest = it)) }
                NotificationToggle("Group invitations", notifPrefs.groupInvitation) { viewModel.setNotifications(notifPrefs.copy(groupInvitation = it)) }
                NotificationToggle("Monthly summary", notifPrefs.monthlySummary) { viewModel.setNotifications(notifPrefs.copy(monthlySummary = it)) }
            }
            item {
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors()) { Text("Logout") }
            }
            item {
                TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Account", color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Text("Privacy Policy • Terms of Service", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Account?") },
            text = { Text("This permanently deletes your account and all data. This cannot be undone.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteAccount(); showDelete = false }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NotificationToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: com.expensetracker.presentation.viewmodel.DashboardViewModel) {
    val state by viewModel.state.collectAsState()
    val summary = state.summary

    Scaffold(topBar = { TopAppBar(title = { Text("Analytics") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            summary?.let { s ->
                Text("Financial Journey", style = MaterialTheme.typography.headlineMedium)
                Text("Total tracked: ${s.currencyCode} ${"%.2f".format(s.totalExpenses)}")
                Text("Monthly: ${"%.2f".format(s.monthlySpending)}")
                com.expensetracker.presentation.ui.components.PieChart(s.categoryBreakdown)
                com.expensetracker.presentation.ui.components.BarChart(s.monthlyTrend)
                com.expensetracker.presentation.ui.components.SpendingHeatMap(s.monthlyTrend)
                com.expensetracker.presentation.ui.components.HealthScoreRing(s.spendingHealthScore.score)
                Text("Insights", style = MaterialTheme.typography.titleMedium)
                state.insights.forEach { insight ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(insight.title, style = MaterialTheme.typography.titleSmall)
                            Text(insight.description, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(viewModel: com.expensetracker.presentation.viewmodel.SearchViewModel) {
    val results by viewModel.results.collectAsState()
    var query by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                query, { query = it; viewModel.search(it) },
                label = { Text("Search expenses, friends, groups...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            LazyColumn {
                items(results.size) { i ->
                    val r = results[i]
                    ListItem(
                        headlineContent = { Text(r.title) },
                        supportingContent = { Text("${r.type.name}: ${r.subtitle}") }
                    )
                }
            }
        }
    }
}
