package com.expensetracker.domain.engine

import com.expensetracker.domain.model.BalanceEntry
import com.expensetracker.domain.model.Expense
import com.expensetracker.domain.model.ExpenseParticipant
import com.expensetracker.domain.model.Settlement
import com.expensetracker.domain.model.SplitType
import com.expensetracker.domain.model.UserBalance
import kotlin.math.abs
import kotlin.math.round

object SplitCalculator {

    fun calculateShares(
        totalAmount: Double,
        splitType: SplitType,
        participants: List<ExpenseParticipant>,
        payerId: String
    ): List<ExpenseParticipant> {
        if (participants.isEmpty()) return emptyList()
        val rounded = { value: Double -> round(value * 100) / 100.0 }

        return when (splitType) {
            SplitType.EQUAL -> {
                val share = rounded(totalAmount / participants.size)
                var remainder = totalAmount - share * participants.size
                participants.mapIndexed { index, p ->
                    val adjustment = if (index == 0) remainder else 0.0
                    p.copy(
                        shareAmount = rounded(share + adjustment),
                        paidAmount = if (p.userId == payerId) totalAmount else 0.0
                    )
                }
            }
            SplitType.UNEQUAL, SplitType.EXACT -> {
                participants.map { p ->
                    p.copy(paidAmount = if (p.userId == payerId) totalAmount else 0.0)
                }
            }
            SplitType.PERCENTAGE -> {
                participants.map { p ->
                    p.copy(
                        shareAmount = rounded(totalAmount * p.sharePercent / 100.0),
                        paidAmount = if (p.userId == payerId) totalAmount else 0.0
                    )
                }
            }
            SplitType.SHARES -> {
                val totalShares = participants.sumOf { it.shareCount.coerceAtLeast(1) }
                participants.map { p ->
                    val share = rounded(totalAmount * p.shareCount / totalShares.toDouble())
                    p.copy(
                        shareAmount = share,
                        paidAmount = if (p.userId == payerId) totalAmount else 0.0
                    )
                }
            }
        }
    }

    fun validateSplit(totalAmount: Double, splitType: SplitType, participants: List<ExpenseParticipant>): Boolean {
        return when (splitType) {
            SplitType.EQUAL, SplitType.SHARES -> participants.isNotEmpty()
            SplitType.PERCENTAGE -> abs(participants.sumOf { it.sharePercent } - 100.0) < 0.01
            SplitType.UNEQUAL, SplitType.EXACT -> {
                val sum = participants.sumOf { it.shareAmount }
                abs(sum - totalAmount) < 0.01
            }
        }
    }
}

object BalanceEngine {

    fun computeBalances(
        expenses: List<Expense>,
        settlements: List<Settlement>,
        currencyCode: String,
        userNames: Map<String, String>
    ): List<BalanceEntry> {
        val net = mutableMapOf<Pair<String, String>, Double>()

        fun addDebt(from: String, to: String, amount: Double) {
            if (from == to || amount <= 0.001) return
            val key = from to to
            val reverse = to to from
            if (net.containsKey(reverse)) {
                val existing = net[reverse] ?: 0.0
                if (existing >= amount) {
                    net[reverse] = existing - amount
                    if (net[reverse]!! < 0.001) net.remove(reverse)
                } else {
                    net.remove(reverse)
                    net[key] = (net[key] ?: 0.0) + (amount - existing)
                }
            } else {
                net[key] = (net[key] ?: 0.0) + amount
            }
        }

        expenses.filter { it.currencyCode == currencyCode }.forEach { expense ->
            expense.participants.forEach { participant ->
                val owed = participant.shareAmount
                val paid = if (participant.userId == expense.paidByUserId) expense.amount else 0.0
                val netPaid = paid - owed
                if (netPaid > 0.001) {
                    expense.participants
                        .filter { it.userId != participant.userId }
                        .forEach { other ->
                            val otherOwes = other.shareAmount
                            val totalOthers = expense.participants.sumOf { it.shareAmount }
                            if (totalOthers > 0) {
                                val portion = expense.amount * (otherOwes / totalOthers)
                                addDebt(other.userId, participant.userId, portion)
                            }
                        }
                }
            }
        }

        settlements.filter { it.currencyCode == currencyCode }.forEach { s ->
            addDebt(s.fromUserId, s.toUserId, s.amount)
        }

        return net.map { (pair, amount) ->
            BalanceEntry(
                fromUserId = pair.first,
                fromUserName = userNames[pair.first] ?: pair.first,
                toUserId = pair.second,
                toUserName = userNames[pair.second] ?: pair.second,
                amount = round(amount * 100) / 100.0,
                currencyCode = currencyCode
            )
        }.filter { it.amount > 0.001 }
    }

    fun computeUserBalances(
        balances: List<BalanceEntry>,
        userIds: Set<String>,
        currencyCode: String,
        userNames: Map<String, String>
    ): List<UserBalance> {
        return userIds.map { userId ->
            val owed = balances.filter { it.fromUserId == userId }.sumOf { it.amount }
            val receivable = balances.filter { it.toUserId == userId }.sumOf { it.amount }
            UserBalance(
                userId = userId,
                userName = userNames[userId] ?: userId,
                netBalance = round((receivable - owed) * 100) / 100.0,
                totalOwed = round(owed * 100) / 100.0,
                totalReceivable = round(receivable * 100) / 100.0,
                currencyCode = currencyCode
            )
        }
    }
}

object DebtSimplifier {

    fun simplify(balances: List<BalanceEntry>): List<BalanceEntry> {
        val netMap = mutableMapOf<String, Double>()
        balances.forEach { b ->
            netMap[b.fromUserId] = (netMap[b.fromUserId] ?: 0.0) - b.amount
            netMap[b.toUserId] = (netMap[b.toUserId] ?: 0.0) + b.amount
        }

        val creditors = netMap.filter { it.value > 0.001 }.toMutableMap()
        val debtors = netMap.filter { it.value < -0.001 }.mapValues { abs(it.value) }.toMutableMap()
        val result = mutableListOf<BalanceEntry>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditorEntry = creditors.maxByOrNull { it.value }!!
            val debtorEntry = debtors.maxByOrNull { it.value }!!
            val amount = round(minOf(creditorEntry.value, debtorEntry.value) * 100) / 100.0

            result.add(
                BalanceEntry(
                    fromUserId = debtorEntry.key,
                    fromUserName = debtorEntry.key,
                    toUserId = creditorEntry.key,
                    toUserName = creditorEntry.key,
                    amount = amount,
                    currencyCode = balances.firstOrNull()?.currencyCode ?: "USD"
                )
            )

            creditors[creditorEntry.key] = creditorEntry.value - amount
            debtors[debtorEntry.key] = debtorEntry.value - amount
            if (creditors[creditorEntry.key]!! < 0.001) creditors.remove(creditorEntry.key)
            if (debtors[debtorEntry.key]!! < 0.001) debtors.remove(debtorEntry.key)
        }
        return result
    }
}

object ExpenseCategorizer {

    private val rules = listOf(
        Regex("(?i)(restaurant|food|lunch|dinner|breakfast|cafe|pizza|burger)") to "food",
        Regex("(?i)(flight|hotel|taxi|uber|travel|trip|fuel|gas|petrol)") to "travel",
        Regex("(?i)(shop|amazon|store|mall|clothes)") to "shopping",
        Regex("(?i)(rent|lease|mortgage)") to "rent",
        Regex("(?i)(electric|water|internet|wifi|utility|bill)") to "utilities",
        Regex("(?i)(movie|concert|game|netflix|spotify|entertainment)") to "entertainment",
        Regex("(?i)(doctor|pharmacy|hospital|health|medical)") to "healthcare",
        Regex("(?i)(school|tuition|course|education|book)") to "education"
    )

    fun categorize(title: String, notes: String = ""): String {
        val text = "$title $notes"
        return rules.firstOrNull { it.first.containsMatchIn(text) }?.second ?: "bills"
    }
}
