package com.expensetracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "personal_expenses", indices = [Index("date")])
data class PersonalExpenseEntity(
    @PrimaryKey val id: String,
    val amount: Double,
    val category: String,
    val note: String,
    val date: Long,
    val type: String
)
