package com.expensetracker.data.repository

import com.expensetracker.data.local.database.dao.PersonalExpenseDao
import com.expensetracker.data.local.database.entity.PersonalExpenseEntity
import com.expensetracker.domain.model.PersonalCategory
import com.expensetracker.domain.model.PersonalExpense
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.domain.repository.PersonalExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalExpenseRepositoryImpl @Inject constructor(
    private val dao: PersonalExpenseDao
) : PersonalExpenseRepository {

    override fun observeAll(): Flow<List<PersonalExpense>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeMonth(year: Int, month: Int): Flow<List<PersonalExpense>> {
        val cal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis
        return dao.observeBetween(start, end).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun add(expense: PersonalExpense) {
        dao.insert(expense.toEntity())
    }

    override suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    private fun PersonalExpenseEntity.toDomain() = PersonalExpense(
        id = id,
        amount = amount,
        category = PersonalCategory.entries.find { it.name == category } ?: PersonalCategory.OTHER,
        note = note,
        date = date,
        type = TransactionType.valueOf(type)
    )

    private fun PersonalExpense.toEntity() = PersonalExpenseEntity(
        id = id,
        amount = amount,
        category = category.name,
        note = note,
        date = date,
        type = type.name
    )
}
