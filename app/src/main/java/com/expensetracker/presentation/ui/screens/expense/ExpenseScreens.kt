package com.expensetracker.presentation.ui.screens.expense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expensetracker.domain.engine.ExpenseCategorizer
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.viewmodel.ExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(viewModel: ExpenseViewModel, onAdd: () -> Unit, onEdit: (String) -> Unit) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        topBar = { TopAppBar(title = { Text("Expenses") }) },
        floatingActionButton = { FloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, null) } }
    ) { padding ->
        LazyColumn(Modifier.padding(padding)) {
            items(state.expenses) { expense ->
                ListItem(
                    headlineContent = { Text(expense.title) },
                    supportingContent = { Text("${expense.paidByName} • ${SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(expense.expenseDate))}") },
                    trailingContent = { Text("${expense.currencyCode} ${"%.2f".format(expense.amount)}") },
                    modifier = Modifier,
                    leadingContent = { Icon(Icons.Default.Receipt, null) }
                )
                Row(Modifier.padding(horizontal = 16.dp)) {
                    TextButton(onClick = { onEdit(expense.id) }) { Text("Edit") }
                    TextButton(onClick = { viewModel.duplicate(expense.id) }) { Text("Duplicate") }
                    TextButton(onClick = { viewModel.deleteExpense(expense.id) }) { Text("Delete") }
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ExpenseViewModel,
    groupId: String? = null,
    onDone: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var splitType by remember { mutableStateOf(SplitType.EQUAL) }
    var categoryId by remember { mutableStateOf("food") }
    var isRecurring by remember { mutableStateOf(false) }
    var recurrence by remember { mutableStateOf(RecurrenceFrequency.MONTHLY) }

    LaunchedEffect(title) { if (title.length > 2) categoryId = ExpenseCategorizer.categorize(title, notes) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense") },
                navigationIcon = { IconButton(onClick = onDone) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                state.categories.take(5).forEach { cat ->
                    FilterChip(selected = categoryId == cat.id, onClick = { categoryId = cat.id }, label = { Text("${cat.icon} ${cat.name}") })
                }
            }

            Text("Split Type", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SplitType.entries.forEach { st ->
                    FilterChip(selected = splitType == st, onClick = { splitType = st }, label = { Text(st.name) })
                }
            }

            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = isRecurring, onCheckedChange = { isRecurring = it })
                Text("Recurring expense")
            }
            if (isRecurring) {
                RecurrenceFrequency.entries.forEach { freq ->
                    FilterChip(selected = recurrence == freq, onClick = { recurrence = freq }, label = { Text(freq.name) })
                }
            }

            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@Button
                    val participant = ExpenseParticipant(userId = "self", name = "You", shareAmount = amt)
                    viewModel.addExpense(Expense(
                        groupId = groupId, title = title, amount = amt, currencyCode = "USD",
                        notes = notes, categoryId = categoryId, paidByUserId = "self", paidByName = "You",
                        splitType = splitType, participants = listOf(participant),
                        createdByUserId = "self",
                        isRecurring = isRecurring, recurrenceFrequency = if (isRecurring) recurrence else null
                    ))
                    onDone()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save Expense") }
        }
    }
}
