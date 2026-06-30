package com.expensetracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.domain.model.PersonalCategory
import com.expensetracker.domain.model.PersonalExpense
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.repository.PersonalExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class CategoryBreakdown(val category: PersonalCategory, val amount: Double, val fraction: Float)

data class PersonalFinanceUiState(
    val transactions: List<PersonalExpense> = emptyList(),
    val monthExpenseTotal: Double = 0.0,
    val monthIncomeTotal: Double = 0.0,
    val categoryBreakdown: List<CategoryBreakdown> = emptyList(),
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH),
    val message: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class PersonalFinanceViewModel @Inject constructor(
    private val repo: PersonalExpenseRepository
) : ViewModel() {
    private val monthFilter = MutableStateFlow(
        Calendar.getInstance().let { it.get(Calendar.YEAR) to it.get(Calendar.MONTH) }
    )
    private val _state = MutableStateFlow(PersonalFinanceUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            monthFilter
                .flatMapLatest { (year, month) -> repo.observeMonth(year, month) }
                .collect { list -> updateFromTransactions(list) }
        }
    }

    fun setMonth(year: Int, month: Int) {
        monthFilter.value = year to month
        _state.update { it.copy(selectedYear = year, selectedMonth = month) }
    }

    fun addTransaction(amount: Double, category: PersonalCategory, note: String, type: TransactionType, date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repo.add(PersonalExpense(UUID.randomUUID().toString(), amount, category, note, date, type))
            _state.update { it.copy(message = "Saved") }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch { repo.delete(id) }
    }

    private fun updateFromTransactions(list: List<PersonalExpense>) {
        val expenses = list.filter { it.type == TransactionType.EXPENSE }
        val income = list.filter { it.type == TransactionType.INCOME }
        val expenseTotal = expenses.sumOf { it.amount }
        val byCategory = expenses.groupBy { it.category }.map { (cat, items) ->
            CategoryBreakdown(cat, items.sumOf { it.amount }, if (expenseTotal > 0) (items.sumOf { it.amount } / expenseTotal).toFloat() else 0f)
        }.sortedByDescending { it.amount }
        _state.update {
            it.copy(
                transactions = list,
                monthExpenseTotal = expenseTotal,
                monthIncomeTotal = income.sumOf { it.amount },
                categoryBreakdown = byCategory
            )
        }
    }
}
