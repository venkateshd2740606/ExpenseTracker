package com.expensetracker.presentation.ui.screens.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.model.DashboardLayout
import com.expensetracker.presentation.ui.components.*
import com.expensetracker.presentation.viewmodel.DashboardViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddExpense: () -> Unit,
    onNavigateGroups: () -> Unit,
    onNavigateAnalytics: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val summary = state.summary

    Scaffold(
        topBar = { TopAppBar(title = { Text("Dashboard") }, actions = { IconButton(onClick = onNavigateAnalytics) { Icon(Icons.Default.Analytics, "Analytics") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { viewModel.onAction(); onAddExpense() }) { Icon(Icons.Default.Add, "Add expense") } }
    ) { padding ->
        if (state.isLoading || summary == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Total", "${summary.currencyCode} ${"%.2f".format(summary.totalExpenses)}", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer)
                    SummaryCard("You owe", "${"%.2f".format(summary.totalOwed)}", Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer)
                    SummaryCard("Owed to you", "${"%.2f".format(summary.totalReceivable)}", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer)
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        HealthScoreRing(summary.spendingHealthScore.score)
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("Spending Health", style = MaterialTheme.typography.titleMedium)
                            Text(summary.spendingHealthScore.label, fontWeight = FontWeight.Bold)
                            summary.spendingHealthScore.factors.take(2).forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary)
                            Text("${summary.streakDays} day streak", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            item {
                Text("Monthly Trend", style = MaterialTheme.typography.titleMedium)
                BarChart(summary.monthlyTrend, barColor = MaterialTheme.colorScheme.primary)
            }
            item {
                Text("Categories", style = MaterialTheme.typography.titleMedium)
                PieChart(summary.categoryBreakdown)
            }
            item {
                Text("Spending Heat Map", style = MaterialTheme.typography.titleMedium)
                SpendingHeatMap(summary.monthlyTrend)
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Active Groups (${summary.activeGroups})", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onNavigateGroups) { Text("View all") }
                }
            }
            item {
                Text("Recent Activity", style = MaterialTheme.typography.titleMedium)
            }
            items(summary.recentActivity) { activity ->
                ListItem(
                    headlineContent = { Text(activity.title) },
                    supportingContent = { Text(activity.subtitle) },
                    leadingContent = { Icon(Icons.Default.Receipt, null) },
                    trailingContent = { Text(SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(activity.timestamp)), style = MaterialTheme.typography.labelSmall) }
                )
            }
            item { AdBanner() }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier, containerColor: androidx.compose.ui.graphics.Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = containerColor), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}
