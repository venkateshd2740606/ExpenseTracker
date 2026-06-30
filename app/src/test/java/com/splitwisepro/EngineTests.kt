package com.expensetracker.domain.engine

import com.expensetracker.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class SplitCalculatorTest {
    @Test
    fun equalSplit_dividesEvenly() {
        val participants = listOf(
            ExpenseParticipant("a", "A"),
            ExpenseParticipant("b", "B")
        )
        val result = SplitCalculator.calculateShares(100.0, SplitType.EQUAL, participants, "a")
        assertEquals(50.0, result[0].shareAmount, 0.01)
        assertEquals(50.0, result[1].shareAmount, 0.01)
    }

    @Test
    fun percentageSplit_respectsPercentages() {
        val participants = listOf(
            ExpenseParticipant("a", "A", sharePercent = 70.0),
            ExpenseParticipant("b", "B", sharePercent = 30.0)
        )
        val result = SplitCalculator.calculateShares(100.0, SplitType.PERCENTAGE, participants, "a")
        assertEquals(70.0, result[0].shareAmount, 0.01)
        assertEquals(30.0, result[1].shareAmount, 0.01)
    }

    @Test
    fun validatePercentage_requires100() {
        val participants = listOf(
            ExpenseParticipant("a", "A", sharePercent = 60.0),
            ExpenseParticipant("b", "B", sharePercent = 30.0)
        )
        assertFalse(SplitCalculator.validateSplit(100.0, SplitType.PERCENTAGE, participants))
    }
}

class DebtSimplifierTest {
    @Test
    fun simplify_reducesTransactions() {
        val balances = listOf(
            BalanceEntry("a", "A", "b", "B", 50.0, "USD"),
            BalanceEntry("b", "B", "c", "C", 50.0, "USD")
        )
        val simplified = DebtSimplifier.simplify(balances)
        assertTrue(simplified.size <= 2)
    }
}

class ExpenseCategorizerTest {
    @Test
    fun categorizes_food() {
        assertEquals("food", ExpenseCategorizer.categorize("Dinner at restaurant"))
    }

    @Test
    fun categorizes_travel() {
        assertEquals("travel", ExpenseCategorizer.categorize("Uber to airport"))
    }
}

class BalanceEngineTest {
    @Test
    fun computes_whoOwesWhom() {
        val expense = Expense(
            title = "Dinner", amount = 100.0, currencyCode = "USD", categoryId = "food",
            paidByUserId = "a", paidByName = "Alice", splitType = SplitType.EQUAL,
            participants = listOf(
                ExpenseParticipant("a", "Alice", shareAmount = 50.0),
                ExpenseParticipant("b", "Bob", shareAmount = 50.0)
            ),
            createdByUserId = "a"
        )
        val balances = BalanceEngine.computeBalances(listOf(expense), emptyList(), "USD", mapOf("a" to "Alice", "b" to "Bob"))
        assertTrue(balances.any { it.fromUserId == "b" && it.toUserId == "a" })
    }
}
