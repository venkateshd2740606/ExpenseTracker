package com.expensetracker.presentation.ui.screens.personal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.PersonalCategory
import com.expensetracker.domain.model.PersonalExpense
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.viewmodel.PersonalFinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDashboardScreen(
    viewModel: PersonalFinanceViewModel = hiltViewModel(),
    onAdd: () -> Unit,
    onViewTransactions: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(
        Calendar.getInstance().apply { set(state.selectedYear, state.selectedMonth, 1) }.time
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Expense Tracker") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, "Add") } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply { set(state.selectedYear, state.selectedMonth, 1) }
                        cal.add(Calendar.MONTH, -1)
                        viewModel.setMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    }) { Icon(Icons.Default.ChevronLeft, "Prev") }
                    Text(monthLabel, modifier = Modifier.weight(1f).align(Alignment.CenterVertically), fontWeight = FontWeight.Bold)
                    IconButton(onClick = {
                        val cal = Calendar.getInstance().apply { set(state.selectedYear, state.selectedMonth, 1) }
                        cal.add(Calendar.MONTH, 1)
                        viewModel.setMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    }) { Icon(Icons.Default.ChevronRight, "Next") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Expenses", state.monthExpenseTotal, MaterialTheme.colorScheme.errorContainer, Modifier.weight(1f))
                    SummaryCard("Income", state.monthIncomeTotal, MaterialTheme.colorScheme.primaryContainer, Modifier.weight(1f))
                    SummaryCard("Balance", state.monthIncomeTotal - state.monthExpenseTotal, MaterialTheme.colorScheme.tertiaryContainer, Modifier.weight(1f))
                }
            }
            item { Text("Category Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (state.categoryBreakdown.isEmpty()) {
                item { Text("No expenses this month", style = MaterialTheme.typography.bodyMedium) }
            } else {
                items(state.categoryBreakdown) { item ->
                    CategoryRow(item.category.label, item.amount, item.fraction)
                }
            }
            item {
                TextButton(onClick = onViewTransactions) { Text("View all transactions") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalTransactionListScreen(viewModel: PersonalFinanceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Scaffold(topBar = { TopAppBar(title = { Text("Transactions") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(state.transactions, key = { it.id }) { tx ->
                TransactionRow(tx, fmt, onDelete = { viewModel.deleteTransaction(tx.id) })
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalExpenseScreen(
    viewModel: PersonalFinanceViewModel = hiltViewModel(),
    onDone: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(PersonalCategory.FOOD) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var expanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.Default.Close, "Close") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TabRow(selectedTabIndex = if (type == TransactionType.EXPENSE) 0 else 1) {
                Tab(selected = type == TransactionType.EXPENSE, onClick = { type = TransactionType.EXPENSE }, text = { Text("Expense") })
                Tab(selected = type == TransactionType.INCOME, onClick = { type = TransactionType.INCOME }, text = { Text("Income") })
            }
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = category.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    PersonalCategory.entries.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat.label) }, onClick = { category = cat; expanded = false })
                    }
                }
            }
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    viewModel.addTransaction(amt, category, note, type)
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
        }
    }
}

@Composable
private fun SummaryCard(label: String, amount: Double, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("₹${"%.2f".format(amount)}", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategoryRow(label: String, amount: Double, fraction: Float) {
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("₹${"%.2f".format(amount)}", fontWeight = FontWeight.SemiBold)
        }
        LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(6.dp))
    }
}

@Composable
private fun TransactionRow(tx: PersonalExpense, fmt: SimpleDateFormat, onDelete: () -> Unit) {
    val color = if (tx.type == TransactionType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    ListItem(
        headlineContent = { Text(tx.category.label) },
        supportingContent = { Text(tx.note.ifBlank { tx.type.name }) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text("${if (tx.type == TransactionType.EXPENSE) "-" else "+"}₹${"%.2f".format(tx.amount)}", color = color, fontWeight = FontWeight.Bold)
                Text(fmt.format(Date(tx.date)), style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", modifier = Modifier.size(18.dp)) }
            }
        }
    )
}
