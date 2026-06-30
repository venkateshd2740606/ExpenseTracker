package com.expensetracker.data.local.database.dao

import androidx.room.*
import com.expensetracker.data.local.database.entity.PersonalExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonalExpenseDao {
    @Query("SELECT * FROM personal_expenses ORDER BY date DESC")
    fun observeAll(): Flow<List<PersonalExpenseEntity>>

    @Query("SELECT * FROM personal_expenses WHERE date >= :start AND date < :end ORDER BY date DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<PersonalExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PersonalExpenseEntity)

    @Query("DELETE FROM personal_expenses WHERE id = :id")
    suspend fun deleteById(id: String)
}
