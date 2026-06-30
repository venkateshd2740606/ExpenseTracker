package com.expensetracker.domain.model

enum class TransactionType { INCOME, EXPENSE }

enum class PersonalCategory(val label: String) {
    FOOD("Food"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    HEALTH("Health"),
    ENTERTAINMENT("Entertainment"),
    OTHER("Other")
}

data class PersonalExpense(
    val id: String,
    val amount: Double,
    val category: PersonalCategory,
    val note: String,
    val date: Long,
    val type: TransactionType
)
