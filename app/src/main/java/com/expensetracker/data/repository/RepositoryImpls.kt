package com.expensetracker.data.repository

import com.expensetracker.data.local.PreferencesDataStore
import com.expensetracker.data.local.database.ExpenseTrackerDatabase
import com.expensetracker.data.local.database.entity.*
import com.expensetracker.data.mapper.EntityMappers
import com.expensetracker.domain.engine.*
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.*
import com.expensetracker.export.ExportManager
import com.expensetracker.security.InputValidator
import com.expensetracker.security.OtpGenerator
import com.expensetracker.security.PasswordHasher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val prefs: PreferencesDataStore,
    private val passwordHasher: PasswordHasher,
    private val otpGenerator: OtpGenerator,
    private val mappers: EntityMappers,
    private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    private val _session = MutableStateFlow<AuthSession?>(null)

    override val currentSession: Flow<AuthSession?> = _session.asStateFlow()

    override suspend fun restoreSession() {
        val userId = prefs.sessionUserId.firstOrNull() ?: return
        val user = db.userDao().getUser(userId) ?: return
        _session.value = AuthSession(
            userId = user.id, email = user.email, mobile = user.mobile,
            isVerified = true, token = UUID.randomUUID().toString(),
            expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        )
    }

    override suspend fun registerWithEmail(email: String, password: String, name: String): Result<AuthSession> =
        withContext(ioDispatcher) {
            if (!InputValidator.isValidEmail(email)) return@withContext Result.failure(IllegalArgumentException("Invalid email"))
            if (!InputValidator.isValidPassword(password)) return@withContext Result.failure(IllegalArgumentException("Password must be at least 8 characters"))
            if (db.userDao().getByEmail(email) != null) return@withContext Result.failure(IllegalArgumentException("Email already registered"))

            val salt = passwordHasher.generateSalt()
            val profile = UserProfile(name = InputValidator.sanitize(name), email = email.lowercase())
            db.userDao().insert(mappers.toEntity(profile, passwordHasher.hash(password, salt), salt))
            createSession(profile)
        }

    override suspend fun loginWithEmail(email: String, password: String): Result<AuthSession> =
        withContext(ioDispatcher) {
            val user = db.userDao().getByEmail(email.lowercase())
                ?: return@withContext Result.failure(IllegalArgumentException("Account not found"))
            if (!passwordHasher.verify(password, user.salt, user.passwordHash)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid credentials"))
            }
            createSession(mappers.toDomain(user))
        }

    override suspend fun sendEmailOtp(email: String): Result<OtpChallenge> = withContext(ioDispatcher) {
        if (!InputValidator.isValidEmail(email)) return@withContext Result.failure(IllegalArgumentException("Invalid email"))
        createOtpChallenge(email.lowercase())
    }

    override suspend fun verifyOtp(challengeId: String, code: String, name: String?): Result<AuthSession> =
        withContext(ioDispatcher) {
            val challenge = db.otpDao().get(challengeId)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid challenge"))
            if (challenge.expiresAt < System.currentTimeMillis()) {
                return@withContext Result.failure(IllegalArgumentException("OTP expired"))
            }
            if (!otpGenerator.verifyCode(code, challenge.codeHash)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid OTP"))
            }
            db.otpDao().markVerified(challengeId)

            val existing = db.userDao().getByEmail(challenge.destination)
            val profile = existing?.let { mappers.toDomain(it) } ?: run {
                val p = UserProfile(
                    name = name?.let { InputValidator.sanitize(it) } ?: "User",
                    email = challenge.destination
                )
                val salt = passwordHasher.generateSalt()
                db.userDao().insert(mappers.toEntity(p, passwordHasher.hash(UUID.randomUUID().toString(), salt), salt))
                p
            }
            createSession(profile)
        }

    override suspend fun resetPassword(email: String): Result<Unit> = withContext(ioDispatcher) {
        if (!InputValidator.isValidEmail(email)) return@withContext Result.failure(IllegalArgumentException("Invalid email"))
        createOtpChallenge(email.lowercase())
        Result.success(Unit)
    }

    override suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit> =
        withContext(ioDispatcher) {
            val user = db.userDao().getUser(userId) ?: return@withContext Result.failure(IllegalArgumentException("User not found"))
            if (!passwordHasher.verify(oldPassword, user.salt, user.passwordHash)) {
                return@withContext Result.failure(IllegalArgumentException("Current password incorrect"))
            }
            val salt = passwordHasher.generateSalt()
            db.userDao().insert(user.copy(passwordHash = passwordHasher.hash(newPassword, salt), salt = salt))
            Result.success(Unit)
        }

    override suspend fun logout() {
        prefs.clearSession()
        _session.value = null
    }

    private suspend fun createOtpChallenge(destination: String): Result<OtpChallenge> {
        db.otpDao().deleteExpired(System.currentTimeMillis())
        val code = otpGenerator.generateCode()
        val challenge = OtpChallenge(
            challengeId = UUID.randomUUID().toString(),
            destination = destination,
            expiresAt = System.currentTimeMillis() + 5 * 60 * 1000,
            localVerificationCode = code
        )
        db.otpDao().insert(OtpChallengeEntity(
            challengeId = challenge.challengeId, destination = destination,
            type = OtpType.EMAIL.name, codeHash = otpGenerator.hashCode(code),
            expiresAt = challenge.expiresAt
        ))
        return Result.success(challenge)
    }

    private suspend fun createSession(profile: UserProfile): Result<AuthSession> {
        val token = UUID.randomUUID().toString()
        val expires = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        prefs.saveSession(profile.id, token, expires)
        val session = AuthSession(profile.id, profile.email, profile.mobile, true, token, expires)
        _session.value = session
        return Result.success(session)
    }
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers
) : UserRepository {
    override fun observeProfile(userId: String) = db.userDao().observeUser(userId).map { it?.let { e -> mappers.toDomain(e) } }
    override suspend fun getProfile(userId: String) = db.userDao().getUser(userId)?.let { mappers.toDomain(it) }
    override suspend fun saveProfile(profile: UserProfile) {
        val existing = db.userDao().getUser(profile.id)
        db.userDao().insert(mappers.toEntity(profile, existing?.passwordHash ?: "", existing?.salt ?: ""))
    }
    override suspend fun updateProfile(profile: UserProfile) = saveProfile(profile)
    override suspend fun deleteAccount(userId: String) { db.userDao().delete(userId) }
}

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers,
    private val activityRepo: ActivityRepository
) : FriendRepository {
    override fun observeFriends(userId: String) = db.friendDao().observeFriends(userId).map { list -> list.map { mappers.toDomain(it) } }
    override suspend fun addFriend(userId: String, name: String, email: String, mobile: String): Friend {
        val friend = Friend(userId = userId, name = InputValidator.sanitize(name), email = email, mobile = mobile)
        db.friendDao().insert(mappers.toEntity(friend))
        activityRepo.logActivity(ActivityItem(type = ActivityType.FRIEND_ADDED, title = "Friend added", subtitle = name, actorUserId = userId, actorName = name))
        return friend
    }
    override suspend fun searchFriends(userId: String, query: String) =
        db.friendDao().search(userId, query).map { mappers.toDomain(it) }
    override suspend fun removeFriend(friendId: String) { db.friendDao().delete(friendId) }
    override suspend fun acceptFriendRequest(friendId: String) { db.friendDao().accept(friendId) }
}

@Singleton
class GroupRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers,
    private val activityRepo: ActivityRepository
) : GroupRepository {
    override fun observeGroups(userId: String) = db.groupDao().observeGroups(userId).map { groups ->
        groups.map { g -> mappers.toDomain(g) }
    }
    override fun observeGroup(groupId: String) = db.groupDao().observeGroup(groupId).map { g ->
        g?.let { mappers.toDomain(it) }
    }
    override fun observeMembers(groupId: String) = db.groupMemberDao().observeMembers(groupId).map { it.map { e -> mappers.toDomain(e) } }
    override suspend fun createGroup(group: Group, members: List<GroupMember>): Group {
        db.groupDao().insert(mappers.toEntity(group))
        members.forEach { db.groupMemberDao().insert(mappers.toEntity(it)) }
        activityRepo.logActivity(ActivityItem(type = ActivityType.GROUP_CREATED, title = "Group created", subtitle = group.name, groupId = group.id, actorUserId = group.ownerId, actorName = group.name))
        return group
    }
    override suspend fun updateGroup(group: Group) {
        db.groupDao().insert(mappers.toEntity(group.copy(updatedAt = System.currentTimeMillis())))
        activityRepo.logActivity(ActivityItem(type = ActivityType.GROUP_UPDATED, title = "Group updated", subtitle = group.name, groupId = group.id, actorUserId = group.ownerId, actorName = group.name))
    }
    override suspend fun archiveGroup(groupId: String) { db.groupDao().archive(groupId) }
    override suspend fun deleteGroup(groupId: String) { db.groupDao().delete(groupId) }
    override suspend fun addMember(groupId: String, member: GroupMember) {
        db.groupMemberDao().insert(mappers.toEntity(member))
        activityRepo.logActivity(ActivityItem(type = ActivityType.MEMBER_ADDED, title = "Member added", subtitle = member.name, groupId = groupId, actorUserId = member.userId, actorName = member.name))
    }
    override suspend fun removeMember(groupId: String, userId: String) { db.groupMemberDao().remove(groupId, userId) }
}

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers,
    private val activityRepo: ActivityRepository
) : ExpenseRepository {
    override fun observeExpenses(userId: String) = db.expenseDao().observeForUser(userId).map { it.map { e -> mappers.toDomain(e) } }
    override fun observeGroupExpenses(groupId: String) = db.expenseDao().observeForGroup(groupId).map { it.map { e -> mappers.toDomain(e) } }
    override suspend fun getExpense(expenseId: String) = db.expenseDao().get(expenseId)?.let { mappers.toDomain(it) }
    override suspend fun addExpense(expense: Expense): Expense {
        val categorized = if (expense.categoryId.isBlank()) {
            expense.copy(categoryId = ExpenseCategorizer.categorize(expense.title, expense.notes))
        } else expense
        val withShares = categorized.copy(
            participants = SplitCalculator.calculateShares(categorized.amount, categorized.splitType, categorized.participants, categorized.paidByUserId)
        )
        db.expenseDao().insert(mappers.toEntity(withShares))
        activityRepo.logActivity(ActivityItem(type = ActivityType.EXPENSE_ADDED, title = withShares.title, subtitle = String.format("%.2f %s", withShares.amount, withShares.currencyCode), groupId = withShares.groupId, entityId = withShares.id, actorUserId = withShares.createdByUserId, actorName = withShares.paidByName))
        return withShares
    }
    override suspend fun updateExpense(expense: Expense) {
        val updated = expense.copy(updatedAt = System.currentTimeMillis(), participants = SplitCalculator.calculateShares(expense.amount, expense.splitType, expense.participants, expense.paidByUserId))
        db.expenseDao().insert(mappers.toEntity(updated))
        activityRepo.logActivity(ActivityItem(type = ActivityType.EXPENSE_UPDATED, title = updated.title, subtitle = "Updated", groupId = updated.groupId, entityId = updated.id, actorUserId = updated.createdByUserId, actorName = updated.paidByName))
    }
    override suspend fun deleteExpense(expenseId: String) {
        val expense = getExpense(expenseId)
        db.expenseDao().delete(expenseId)
        expense?.let {
            activityRepo.logActivity(ActivityItem(type = ActivityType.EXPENSE_DELETED, title = it.title, subtitle = "Deleted", groupId = it.groupId, entityId = expenseId, actorUserId = it.createdByUserId, actorName = it.paidByName))
        }
    }
    override suspend fun duplicateExpense(expenseId: String, userId: String): Expense {
        val original = getExpense(expenseId) ?: throw IllegalArgumentException("Expense not found")
        return addExpense(original.copy(id = UUID.randomUUID().toString(), createdByUserId = userId, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
    }
    override suspend fun searchExpenses(userId: String, query: String, filter: ExpenseFilter?) =
        applyFilter(db.expenseDao().search(userId, query).map { mappers.toDomain(it) }, filter)
    override suspend fun getPendingSync() = db.expenseDao().getPendingSync().map { mappers.toDomain(it) }

    private fun applyFilter(expenses: List<Expense>, filter: ExpenseFilter?): List<Expense> {
        if (filter == null) return expenses
        return expenses.filter { e ->
            (filter.startDate == null || e.expenseDate >= filter.startDate) &&
                (filter.endDate == null || e.expenseDate <= filter.endDate) &&
                (filter.groupId == null || e.groupId == filter.groupId) &&
                (filter.categoryId == null || e.categoryId == filter.categoryId) &&
                (filter.minAmount == null || e.amount >= filter.minAmount) &&
                (filter.maxAmount == null || e.amount <= filter.maxAmount)
        }
    }
}

@Singleton
class SettlementRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers,
    private val activityRepo: ActivityRepository
) : SettlementRepository {
    override fun observeSettlements(userId: String) = db.settlementDao().observeForUser(userId).map { it.map { e -> mappers.toDomain(e) } }
    override suspend fun addSettlement(settlement: Settlement): Settlement {
        db.settlementDao().insert(mappers.toEntity(settlement))
        activityRepo.logActivity(ActivityItem(type = ActivityType.SETTLEMENT_MADE, title = "Settlement", subtitle = "${settlement.fromUserName} → ${settlement.toUserName}: ${settlement.amount}", groupId = settlement.groupId, entityId = settlement.id, actorUserId = settlement.fromUserId, actorName = settlement.fromUserName))
        return settlement
    }
    override suspend fun getSettlementHistory(userId: String, groupId: String?) =
        db.settlementDao().getHistory(userId, groupId).map { mappers.toDomain(it) }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers
) : CategoryRepository {
    override fun observeCategories(userId: String) = db.categoryDao().observeCategories(userId).map { it.map { e -> mappers.toDomain(e) } }

    override suspend fun getDefaultCategories(): List<ExpenseCategory> {
        val existing = db.categoryDao().getDefaults()
        if (existing.isNotEmpty()) return existing.map { mappers.toDomain(it) }
        val defaults = listOf(
            ExpenseCategory("food", "Food", "🍔", 0xFFE57373),
            ExpenseCategory("travel", "Travel", "✈️", 0xFF64B5F6),
            ExpenseCategory("shopping", "Shopping", "🛍️", 0xFFBA68C8),
            ExpenseCategory("rent", "Rent", "🏠", 0xFF81C784),
            ExpenseCategory("utilities", "Utilities", "💡", 0xFFFFB74D),
            ExpenseCategory("entertainment", "Entertainment", "🎬", 0xFF4DD0E1),
            ExpenseCategory("fuel", "Fuel", "⛽", 0xFFA1887F),
            ExpenseCategory("healthcare", "Healthcare", "🏥", 0xFFEF5350),
            ExpenseCategory("education", "Education", "📚", 0xFF7986CB),
            ExpenseCategory("bills", "Bills", "📄", 0xFF90A4AE)
        )
        defaults.forEach { db.categoryDao().insert(mappers.toEntity(it)) }
        return defaults
    }

    override suspend fun addCustomCategory(category: ExpenseCategory) { db.categoryDao().insert(mappers.toEntity(category.copy(isCustom = true))) }
    override suspend fun deleteCategory(categoryId: String) { db.categoryDao().deleteCustom(categoryId) }
}

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers
) : ActivityRepository {
    override fun observeActivity(userId: String, limit: Int) = db.activityDao().observe(userId, limit).map { it.map { e -> mappers.toDomain(e) } }
    override suspend fun logActivity(item: ActivityItem) { db.activityDao().insert(mappers.toEntity(item)) }
}

@Singleton
class BalanceRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers
) : BalanceRepository {
    override suspend fun getBalances(userId: String, groupId: String?, currencyCode: String): List<BalanceEntry> {
        val expenses = if (groupId != null) db.expenseDao().getAllForGroup(groupId).map { mappers.toDomain(it) }
        else db.expenseDao().getAllForUser(userId).map { mappers.toDomain(it) }
        val settlements = db.settlementDao().getAllForUser(userId).map { mappers.toDomain(it) }
        val names = buildUserNames(userId, expenses, settlements)
        return BalanceEngine.computeBalances(expenses, settlements, currencyCode, names)
    }

    override suspend fun getSimplifiedBalances(userId: String, groupId: String?, currencyCode: String) =
        DebtSimplifier.simplify(getBalances(userId, groupId, currencyCode))

    override suspend fun getUserBalances(userId: String, currencyCode: String): List<UserBalance> {
        val balances = getBalances(userId, null, currencyCode)
        val userIds = balances.flatMap { listOf(it.fromUserId, it.toUserId) }.toSet() + userId
        val names = balances.associate { it.fromUserId to it.fromUserName } + balances.associate { it.toUserId to it.toUserName }
        return BalanceEngine.computeUserBalances(balances, userIds, currencyCode, names)
    }

    private suspend fun buildUserNames(userId: String, expenses: List<Expense>, settlements: List<Settlement>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        expenses.forEach { e ->
            map[e.paidByUserId] = e.paidByName
            e.participants.forEach { p -> map[p.userId] = p.name }
        }
        settlements.forEach { s -> map[s.fromUserId] = s.fromUserName; map[s.toUserId] = s.toUserName }
        db.userDao().getUser(userId)?.let { map[it.id] = it.name }
        return map
    }
}

@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val expenseRepo: ExpenseRepository,
    private val groupRepo: GroupRepository,
    private val categoryRepo: CategoryRepository,
    private val activityRepo: ActivityRepository,
    private val settlementRepo: SettlementRepository,
    private val userRepo: UserRepository,
    private val prefsRepo: PreferencesRepository
) : DashboardRepository {
    override fun observeDashboard(userId: String): Flow<DashboardSummary> {
        val dataFlow = combine(
            expenseRepo.observeExpenses(userId),
            groupRepo.observeGroups(userId),
            categoryRepo.observeCategories(userId),
            activityRepo.observeActivity(userId, 10),
            settlementRepo.observeSettlements(userId)
        ) { expenses, groups, categories, activity, settlements ->
            DashboardData(expenses, groups, categories, activity, settlements)
        }
        return combine(dataFlow, userRepo.observeProfile(userId)) { data, profile ->
            val currency = profile?.currencyCode ?: "USD"
            val names = buildNames(data.expenses, data.settlements, profile?.name ?: "You", userId)
            val balances = BalanceEngine.computeBalances(data.expenses, data.settlements, currency, names)
            val myBalance = BalanceEngine.computeUserBalances(balances, setOf(userId), currency, names).firstOrNull()
            val streak = 0
            DashboardSummary(
                totalExpenses = data.expenses.sumOf { it.amount },
                totalOwed = myBalance?.totalOwed ?: 0.0,
                totalReceivable = myBalance?.totalReceivable ?: 0.0,
                monthlySpending = InsightEngine.categoryBreakdown(data.expenses, data.categories).sumOf { it.amount },
                activeGroups = data.groups.size,
                currencyCode = currency,
                spendingHealthScore = InsightEngine.computeSpendingHealthScore(data.expenses, data.settlements.size, streak),
                recentActivity = data.activity,
                categoryBreakdown = InsightEngine.categoryBreakdown(data.expenses, data.categories),
                monthlyTrend = InsightEngine.monthlyTrend(data.expenses),
                streakDays = streak
            )
        }
    }

    private data class DashboardData(
        val expenses: List<Expense>,
        val groups: List<Group>,
        val categories: List<ExpenseCategory>,
        val activity: List<ActivityItem>,
        val settlements: List<Settlement>
    )

    private fun buildNames(expenses: List<Expense>, settlements: List<Settlement>, userName: String, userId: String): Map<String, String> {
        val map = mutableMapOf(userId to userName)
        expenses.forEach { e ->
            map[e.paidByUserId] = e.paidByName
            e.participants.forEach { p -> map[p.userId] = p.name }
        }
        settlements.forEach { s -> map[s.fromUserId] = s.fromUserName; map[s.toUserId] = s.toUserName }
        return map
    }
}

@Singleton
class PreferencesRepositoryImpl @Inject constructor(
    private val prefs: PreferencesDataStore,
    private val db: ExpenseTrackerDatabase
) : PreferencesRepository {
    override val themeMode = prefs.themeMode
    override val accentColor = prefs.accentColor
    override val notificationPrefs = prefs.notificationPrefs
    override val chartStyle = prefs.chartStyle
    override val dashboardLayout = prefs.dashboardLayout
    override suspend fun setThemeMode(mode: ThemeMode) = prefs.setThemeMode(mode)
    override suspend fun setAccentColor(colorArgb: Long) = prefs.setAccentColor(colorArgb)
    override suspend fun setNotificationPrefs(prefs: NotificationPreference) = this.prefs.setNotificationPrefs(prefs)
    override suspend fun setChartStyle(style: ChartStyle) = prefs.setChartStyle(style)
    override suspend fun setDashboardLayout(layout: DashboardLayout) = prefs.setDashboardLayout(layout)

    override suspend fun getStreakDays(userId: String): Int {
        val dates = db.streakDao().getDates(userId).sortedDescending()
        if (dates.isEmpty()) return 0
        var streak = 1
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var prev = fmt.parse(dates.first())!!
        for (i in 1 until dates.size) {
            val current = fmt.parse(dates[i])!!
            val diff = (prev.time - current.time) / (24 * 60 * 60 * 1000)
            if (diff == 1L) { streak++; prev = current } else break
        }
        return streak
    }

    override suspend fun recordDailyActivity(userId: String) {
        val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        db.streakDao().insert(StreakEntity(userId, key, System.currentTimeMillis()))
    }
}

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : SyncRepository {
    private val _online = MutableStateFlow(true)
    override val isOnline: Flow<Boolean> = _online.asStateFlow()

    override suspend fun syncAll(userId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val pending = db.expenseDao().getPendingSync()
            pending.forEach { expense ->
                db.expenseDao().insert(expense.copy(syncPending = false))
            }
            _online.value = true
            Result.success(Unit)
        } catch (e: Exception) {
            _online.value = false
            Result.failure(e)
        }
    }
}

@Singleton
class ExportRepositoryImpl @Inject constructor(
    private val expenseRepo: ExpenseRepository,
    private val exportManager: ExportManager
) : ExportRepository {
    override suspend fun exportCsv(userId: String, filter: ExpenseFilter?) =
        exportManager.toCsv(expenseRepo.searchExpenses(userId, "", filter))
    override suspend fun exportExcel(userId: String, filter: ExpenseFilter?) =
        exportManager.toExcel(expenseRepo.searchExpenses(userId, "", filter))
    override suspend fun exportPdf(userId: String, filter: ExpenseFilter?) =
        exportManager.toPdf(expenseRepo.searchExpenses(userId, "", filter))
}

@Singleton
class InviteRepositoryImpl @Inject constructor(
    private val db: ExpenseTrackerDatabase
) : InviteRepository {
    override suspend fun createInviteLink(userId: String, groupId: String?): InviteLink {
        val link = InviteLink(code = UUID.randomUUID().toString().take(8).uppercase(), groupId = groupId, inviterUserId = userId, expiresAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)
        db.inviteDao().insert(InviteLinkEntity(link.code, link.groupId, link.inviterUserId, link.expiresAt))
        return link
    }
    override suspend fun resolveInviteCode(code: String): InviteLink? {
        val entity = db.inviteDao().getValid(code, System.currentTimeMillis()) ?: return null
        return InviteLink(entity.code, entity.groupId, entity.inviterUserId, entity.expiresAt)
    }
}
