package com.expensetracker.domain.repository

import com.expensetracker.domain.model.*
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun observeProfile(userId: String): Flow<UserProfile?>
    suspend fun getProfile(userId: String): UserProfile?
    suspend fun saveProfile(profile: UserProfile)
    suspend fun updateProfile(profile: UserProfile)
    suspend fun deleteAccount(userId: String)
}

interface AuthRepository {
    val currentSession: Flow<AuthSession?>
    suspend fun restoreSession()
    suspend fun registerWithEmail(email: String, password: String, name: String): Result<AuthSession>
    suspend fun loginWithEmail(email: String, password: String): Result<AuthSession>
    suspend fun sendEmailOtp(email: String): Result<OtpChallenge>
    suspend fun verifyOtp(challengeId: String, code: String, name: String? = null): Result<AuthSession>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit>
    suspend fun logout()
}

interface FriendRepository {
    fun observeFriends(userId: String): Flow<List<Friend>>
    suspend fun addFriend(userId: String, name: String, email: String, mobile: String): Friend
    suspend fun searchFriends(userId: String, query: String): List<Friend>
    suspend fun removeFriend(friendId: String)
    suspend fun acceptFriendRequest(friendId: String)
}

interface GroupRepository {
    fun observeGroups(userId: String): Flow<List<Group>>
    fun observeGroup(groupId: String): Flow<Group?>
    fun observeMembers(groupId: String): Flow<List<GroupMember>>
    suspend fun createGroup(group: Group, members: List<GroupMember>): Group
    suspend fun updateGroup(group: Group)
    suspend fun archiveGroup(groupId: String)
    suspend fun deleteGroup(groupId: String)
    suspend fun addMember(groupId: String, member: GroupMember)
    suspend fun removeMember(groupId: String, userId: String)
}

interface ExpenseRepository {
    fun observeExpenses(userId: String): Flow<List<Expense>>
    fun observeGroupExpenses(groupId: String): Flow<List<Expense>>
    suspend fun getExpense(expenseId: String): Expense?
    suspend fun addExpense(expense: Expense): Expense
    suspend fun updateExpense(expense: Expense)
    suspend fun deleteExpense(expenseId: String)
    suspend fun duplicateExpense(expenseId: String, userId: String): Expense
    suspend fun searchExpenses(userId: String, query: String, filter: ExpenseFilter?): List<Expense>
    suspend fun getPendingSync(): List<Expense>
}

interface SettlementRepository {
    fun observeSettlements(userId: String): Flow<List<Settlement>>
    suspend fun addSettlement(settlement: Settlement): Settlement
    suspend fun getSettlementHistory(userId: String, groupId: String?): List<Settlement>
}

interface CategoryRepository {
    fun observeCategories(userId: String): Flow<List<ExpenseCategory>>
    suspend fun getDefaultCategories(): List<ExpenseCategory>
    suspend fun addCustomCategory(category: ExpenseCategory)
    suspend fun deleteCategory(categoryId: String)
}

interface ActivityRepository {
    fun observeActivity(userId: String, limit: Int = 50): Flow<List<ActivityItem>>
    suspend fun logActivity(item: ActivityItem)
}

interface BalanceRepository {
    suspend fun getBalances(userId: String, groupId: String?, currencyCode: String): List<BalanceEntry>
    suspend fun getSimplifiedBalances(userId: String, groupId: String?, currencyCode: String): List<BalanceEntry>
    suspend fun getUserBalances(userId: String, currencyCode: String): List<UserBalance>
}

interface DashboardRepository {
    fun observeDashboard(userId: String): Flow<DashboardSummary>
}

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val accentColor: Flow<Long>
    val notificationPrefs: Flow<NotificationPreference>
    val chartStyle: Flow<ChartStyle>
    val dashboardLayout: Flow<DashboardLayout>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColor(colorArgb: Long)
    suspend fun setNotificationPrefs(prefs: NotificationPreference)
    suspend fun setChartStyle(style: ChartStyle)
    suspend fun setDashboardLayout(layout: DashboardLayout)
    suspend fun getStreakDays(userId: String): Int
    suspend fun recordDailyActivity(userId: String)
}

interface SyncRepository {
    suspend fun syncAll(userId: String): Result<Unit>
    val isOnline: Flow<Boolean>
}

interface ExportRepository {
    suspend fun exportCsv(userId: String, filter: ExpenseFilter?): String
    suspend fun exportExcel(userId: String, filter: ExpenseFilter?): String
    suspend fun exportPdf(userId: String, filter: ExpenseFilter?): ByteArray
}

interface InviteRepository {
    suspend fun createInviteLink(userId: String, groupId: String?): InviteLink
    suspend fun resolveInviteCode(code: String): InviteLink?
}
