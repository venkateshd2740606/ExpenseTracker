package com.expensetracker.domain.engine

import com.expensetracker.domain.model.CategorySpending
import com.expensetracker.domain.model.Expense
import com.expensetracker.domain.model.ExpenseCategory
import com.expensetracker.domain.model.Group
import com.expensetracker.domain.model.GroupMember
import com.expensetracker.domain.model.HealthScore
import com.expensetracker.domain.model.MonthlySpending
import com.expensetracker.domain.model.SpendingInsight
import com.expensetracker.domain.model.InsightSeverity
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.round

object InsightEngine {

    fun generateInsights(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>,
        previousMonthTotal: Double
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()
        val categoryMap = categories.associateBy { it.id }
        val now = Calendar.getInstance()
        val thisMonth = expenses.filter { isSameMonth(it.expenseDate, now) }
        val thisMonthTotal = thisMonth.sumOf { it.amount }

        if (previousMonthTotal > 0 && thisMonthTotal > previousMonthTotal * 1.2) {
            insights.add(
                SpendingInsight(
                    title = "Spending up this month",
                    description = "You've spent ${formatPercent((thisMonthTotal - previousMonthTotal) / previousMonthTotal * 100)} more than last month.",
                    severity = InsightSeverity.WARNING
                )
            )
        }

        val topCategory = thisMonth.groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .maxByOrNull { it.value }

        topCategory?.let { (catId, amount) ->
            val name = categoryMap[catId]?.name ?: "Unknown"
            insights.add(
                SpendingInsight(
                    title = "Top category: $name",
                    description = "You spent ${formatMoney(amount)} on $name this month.",
                    severity = InsightSeverity.INFO,
                    categoryId = catId
                )
            )
        }

        if (thisMonthTotal < previousMonthTotal * 0.8 && previousMonthTotal > 0) {
            insights.add(
                SpendingInsight(
                    title = "Great savings!",
                    description = "You're spending less than last month. Keep it up!",
                    severity = InsightSeverity.POSITIVE
                )
            )
        }

        return insights
    }

    fun computeSpendingHealthScore(
        expenses: List<Expense>,
        settlementsCount: Int,
        streakDays: Int
    ): HealthScore {
        var score = 50
        val factors = mutableListOf<String>()

        val recentExpenses = expenses.filter {
            it.expenseDate > System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        }
        if (recentExpenses.isNotEmpty()) {
            score += 15
            factors.add("Active expense tracking")
        }
        if (settlementsCount > 0) {
            score += 20
            factors.add("Regular settlements")
        }
        if (streakDays >= 7) {
            score += 15
            factors.add("${streakDays}-day streak")
        }
        val categorized = recentExpenses.count { it.categoryId.isNotBlank() }
        if (recentExpenses.isNotEmpty() && categorized.toFloat() / recentExpenses.size > 0.8f) {
            score += 10
            factors.add("Well categorized expenses")
        }

        score = score.coerceIn(0, 100)
        val label = when {
            score >= 80 -> "Excellent"
            score >= 60 -> "Good"
            score >= 40 -> "Fair"
            else -> "Needs attention"
        }
        return HealthScore(score = score, label = label, factors = factors)
    }

    fun computeGroupHealthScore(
        members: List<GroupMember>,
        expenses: List<Expense>
    ): HealthScore {
        if (members.size < 2) return HealthScore(100, "Solo group", emptyList())
        val paidByCounts = expenses.groupBy { it.paidByUserId }.mapValues { it.value.size }
        val max = paidByCounts.values.maxOrNull() ?: 0
        val min = paidByCounts.values.minOrNull() ?: 0
        val fairness = if (max == 0) 100 else ((1.0 - (max - min).toDouble() / max.coerceAtLeast(1)) * 100).toInt()
        val label = when {
            fairness >= 80 -> "Balanced"
            fairness >= 50 -> "Moderate"
            else -> "Uneven"
        }
        return HealthScore(
            score = fairness.coerceIn(0, 100),
            label = label,
            factors = listOf("Payment distribution among ${members.size} members")
        )
    }

    fun monthlyTrend(expenses: List<Expense>, months: Int = 6): List<MonthlySpending> {
        val cal = Calendar.getInstance()
        return (0 until months).map { offset ->
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.MONTH, -offset)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val amount = expenses.filter { e ->
                val c = Calendar.getInstance().apply { timeInMillis = e.expenseDate }
                c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
            }.sumOf { it.amount }
            MonthlySpending(year, month + 1, round(amount * 100) / 100.0)
        }.reversed()
    }

    fun categoryBreakdown(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>,
        monthOnly: Boolean = true
    ): List<CategorySpending> {
        val filtered = if (monthOnly) {
            val now = Calendar.getInstance()
            expenses.filter { isSameMonth(it.expenseDate, now) }
        } else expenses
        val total = filtered.sumOf { it.amount }
        if (total <= 0) return emptyList()
        val catMap = categories.associateBy { it.id }
        return filtered.groupBy { it.categoryId }
            .map { (catId, list) ->
                val amount = list.sumOf { it.amount }
                val cat = catMap[catId]
                CategorySpending(
                    categoryId = catId,
                    categoryName = cat?.name ?: "Other",
                    amount = amount,
                    colorArgb = cat?.colorArgb ?: 0xFF9E9E9E,
                    percentage = (amount / total * 100).toFloat()
                )
            }
            .sortedByDescending { it.amount }
    }

    private fun isSameMonth(timestamp: Long, cal: Calendar): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        return c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
            c.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
    }

    private fun formatPercent(value: Double) = "${round(abs(value) * 10) / 10.0}%"
    private fun formatMoney(value: Double) = String.format("%.2f", value)
}

object AchievementEngine {

    val definitions = listOf(
        Triple("first_expense", "First Expense", "📝"),
        Triple("ten_expenses", "Expense Tracker", "📊"),
        Triple("first_settlement", "Debt Free", "✅"),
        Triple("streak_7", "Week Warrior", "🔥"),
        Triple("streak_30", "Monthly Master", "🏆"),
        Triple("five_groups", "Social Splitter", "👥"),
        Triple("hundred_expenses", "Century Club", "💯")
    )

    fun evaluate(expenseCount: Int, settlementCount: Int, groupCount: Int, streakDays: Int): List<String> {
        val unlocked = mutableListOf<String>()
        if (expenseCount >= 1) unlocked.add("first_expense")
        if (expenseCount >= 10) unlocked.add("ten_expenses")
        if (expenseCount >= 100) unlocked.add("hundred_expenses")
        if (settlementCount >= 1) unlocked.add("first_settlement")
        if (streakDays >= 7) unlocked.add("streak_7")
        if (streakDays >= 30) unlocked.add("streak_30")
        if (groupCount >= 5) unlocked.add("five_groups")
        return unlocked
    }
}
