package com.expensetracker.domain.model

import java.util.UUID

enum class SplitType { EQUAL, UNEQUAL, PERCENTAGE, SHARES, EXACT }

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY, YEARLY }

enum class SettlementMethod { CASH, UPI, BANK_TRANSFER, MANUAL }

enum class ActivityType {
    EXPENSE_ADDED, EXPENSE_UPDATED, EXPENSE_DELETED,
    SETTLEMENT_MADE, MEMBER_ADDED, MEMBER_REMOVED,
    GROUP_CREATED, GROUP_UPDATED, FRIEND_ADDED, FRIEND_REQUEST
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class ChartStyle { PIE, BAR, LINE }

enum class DashboardLayout { COMPACT, DETAILED, VISUAL }

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val mobile: String = "",
    val avatarUri: String? = null,
    val currencyCode: String = "USD",
    val languageCode: String = "en",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorArgb: Long = 0xFF1B8A6B,
    val countryCode: String = "US",
    val chartStyle: ChartStyle = ChartStyle.PIE,
    val dashboardLayout: DashboardLayout = DashboardLayout.DETAILED,
    val createdAt: Long = System.currentTimeMillis()
)

data class Friend(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val friendUserId: String? = null,
    val name: String,
    val email: String = "",
    val mobile: String = "",
    val avatarUri: String? = null,
    val isPending: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class Group(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val iconEmoji: String = "👥",
    val ownerId: String,
    val memberIds: List<String> = emptyList(),
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class GroupMember(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String,
    val userId: String,
    val name: String,
    val isAdmin: Boolean = false,
    val joinedAt: Long = System.currentTimeMillis()
)

data class ExpenseCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val colorArgb: Long,
    val isCustom: Boolean = false,
    val userId: String? = null
)

data class ExpenseParticipant(
    val userId: String,
    val name: String,
    val shareAmount: Double = 0.0,
    val sharePercent: Double = 0.0,
    val shareCount: Int = 1,
    val paidAmount: Double = 0.0
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val groupId: String? = null,
    val title: String,
    val amount: Double,
    val currencyCode: String,
    val notes: String = "",
    val categoryId: String,
    val paidByUserId: String,
    val paidByName: String,
    val splitType: SplitType,
    val participants: List<ExpenseParticipant>,
    val receiptUri: String? = null,
    val expenseDate: Long = System.currentTimeMillis(),
    val createdByUserId: String,
    val isRecurring: Boolean = false,
    val recurrenceFrequency: RecurrenceFrequency? = null,
    val recurrenceEndDate: Long? = null,
    val parentRecurringId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncPending: Boolean = false
)

data class Settlement(
    val id: String = UUID.randomUUID().toString(),
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double,
    val currencyCode: String,
    val method: SettlementMethod,
    val groupId: String? = null,
    val notes: String = "",
    val settledAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val syncPending: Boolean = false
)

data class BalanceEntry(
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double,
    val currencyCode: String
)

data class UserBalance(
    val userId: String,
    val userName: String,
    val netBalance: Double,
    val totalOwed: Double,
    val totalReceivable: Double,
    val currencyCode: String
)

data class ActivityItem(
    val id: String = UUID.randomUUID().toString(),
    val type: ActivityType,
    val title: String,
    val subtitle: String,
    val groupId: String? = null,
    val entityId: String? = null,
    val actorUserId: String,
    val actorName: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class SpendingInsight(
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val categoryId: String? = null
)

enum class InsightSeverity { INFO, WARNING, POSITIVE }

data class HealthScore(
    val score: Int,
    val label: String,
    val factors: List<String>
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val amount: Double,
    val colorArgb: Long,
    val percentage: Float
)

data class MonthlySpending(
    val year: Int,
    val month: Int,
    val amount: Double
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val unlockedAt: Long? = null,
    val progress: Float = 0f,
    val target: Int = 1
)

data class DashboardSummary(
    val totalExpenses: Double,
    val totalOwed: Double,
    val totalReceivable: Double,
    val monthlySpending: Double,
    val activeGroups: Int,
    val currencyCode: String,
    val spendingHealthScore: HealthScore,
    val recentActivity: List<ActivityItem>,
    val categoryBreakdown: List<CategorySpending>,
    val monthlyTrend: List<MonthlySpending>,
    val streakDays: Int
)

data class NotificationPreference(
    val expenseAdded: Boolean = true,
    val settlementReminder: Boolean = true,
    val friendRequest: Boolean = true,
    val groupInvitation: Boolean = true,
    val monthlySummary: Boolean = true
)

data class AuthSession(
    val userId: String,
    val email: String,
    val mobile: String,
    val isVerified: Boolean,
    val token: String,
    val expiresAt: Long
)

data class OtpChallenge(
    val challengeId: String,
    val destination: String,
    val expiresAt: Long,
    /** Shown in-app for free on-device verification (no email provider required). */
    val localVerificationCode: String
)

enum class OtpType { EMAIL }

data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val subtitle: String,
    val entityId: String
)

enum class SearchResultType { EXPENSE, FRIEND, GROUP, SETTLEMENT }

data class ExpenseFilter(
    val startDate: Long? = null,
    val endDate: Long? = null,
    val groupId: String? = null,
    val friendUserId: String? = null,
    val categoryId: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)

data class InviteLink(
    val code: String,
    val groupId: String? = null,
    val inviterUserId: String,
    val expiresAt: Long
)
