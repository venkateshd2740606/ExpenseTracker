package com.expensetracker.domain.repository

import com.expensetracker.domain.model.PersonalExpense
import kotlinx.coroutines.flow.Flow

interface PersonalExpenseRepository {
    fun observeAll(): Flow<List<PersonalExpense>>
    fun observeMonth(year: Int, month: Int): Flow<List<PersonalExpense>>
    suspend fun add(expense: PersonalExpense)
    suspend fun delete(id: String)
}
