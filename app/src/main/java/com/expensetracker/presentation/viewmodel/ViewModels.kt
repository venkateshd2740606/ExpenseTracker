package com.expensetracker.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.ads.AdManager
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val otpChallenge: OtpChallenge? = null,
    val otpDisplayCode: String? = null,
    val session: AuthSession? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val categoryRepo: CategoryRepository,
    private val prefsRepo: PreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.restoreSession()
            authRepo.currentSession.collect { session ->
                _uiState.update { it.copy(session = session) }
            }
        }
    }

    fun register(email: String, password: String, name: String) = launchAuth {
        authRepo.registerWithEmail(email, password, name).also { result ->
            result.onSuccess { initUser(it) }
        }
    }

    fun login(email: String, password: String) = launchAuth {
        authRepo.loginWithEmail(email, password).also { result ->
            result.onSuccess { initUser(it) }
        }
    }

    fun sendEmailOtp(email: String) = launchAuth {
        authRepo.sendEmailOtp(email).also { result ->
            result.onSuccess { challenge ->
                _uiState.update { it.copy(otpChallenge = challenge, otpDisplayCode = challenge.localVerificationCode) }
            }
        }
    }

    fun verifyOtp(code: String, name: String?) = launchAuth {
        val challengeId = _uiState.value.otpChallenge?.challengeId
            ?: return@launchAuth Result.failure<AuthSession>(IllegalArgumentException("No OTP challenge"))
        authRepo.verifyOtp(challengeId, code, name).also { result ->
            result.onSuccess { initUser(it) }
        }
    }

    fun resetPassword(email: String) = launchAuth { authRepo.resetPassword(email) }
    fun logout() = viewModelScope.launch { authRepo.logout() }

    private fun initUser(session: AuthSession) {
        viewModelScope.launch {
            if (userRepo.getProfile(session.userId) == null) {
                userRepo.saveProfile(UserProfile(id = session.userId, name = "User", email = session.email, mobile = session.mobile))
            }
            categoryRepo.getDefaultCategories()
            prefsRepo.recordDailyActivity(session.userId)
        }
    }

    private fun launchAuth(block: suspend () -> Result<*>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            block().onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

data class DashboardUiState(
    val summary: DashboardSummary? = null,
    val insights: List<SpendingInsight> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dashboardRepo: DashboardRepository,
    private val expenseRepo: ExpenseRepository,
    private val categoryRepo: CategoryRepository,
    private val authRepo: AuthRepository,
    private val adManager: AdManager
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.currentSession.filterNotNull().flatMapLatest { session ->
                combine(
                    dashboardRepo.observeDashboard(session.userId),
                    expenseRepo.observeExpenses(session.userId),
                    categoryRepo.observeCategories(session.userId)
                ) { summary, expenses, categories ->
                    Triple(summary, expenses, categories)
                }
            }.collect { (summary, expenses, categories) ->
                val cal = Calendar.getInstance()
                cal.add(Calendar.MONTH, -1)
                val prevMonth = expenses.filter {
                    val c = Calendar.getInstance().apply { timeInMillis = it.expenseDate }
                    c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && c.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                }.sumOf { it.amount }
                _state.update {
                    it.copy(
                        summary = summary,
                        insights = InsightEngine.generateInsights(expenses, categories, prevMonth),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onAction() = adManager.onSignificantAction()
}

data class ExpenseUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<ExpenseCategory> = emptyList(),
    val selectedExpense: Expense? = null,
    val filter: ExpenseFilter? = null,
    val message: String? = null
)

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepo: ExpenseRepository,
    private val categoryRepo: CategoryRepository,
    private val authRepo: AuthRepository,
    private val balanceRepo: BalanceRepository,
    private val adManager: AdManager
) : ViewModel() {
    private val _state = MutableStateFlow(ExpenseUiState())
    val state = _state.asStateFlow()
    private var userId = ""

    init {
        viewModelScope.launch {
            authRepo.currentSession.filterNotNull().flatMapLatest { session ->
                userId = session.userId
                combine(
                    expenseRepo.observeExpenses(session.userId),
                    categoryRepo.observeCategories(session.userId)
                ) { expenses, cats -> ExpenseUiState(expenses = expenses, categories = cats) }
            }.collect { _state.value = it }
        }
    }

    fun addExpense(expense: Expense) = viewModelScope.launch {
        expenseRepo.addExpense(expense.copy(createdByUserId = userId))
        adManager.onSignificantAction()
        _state.update { it.copy(message = "Expense added") }
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch { expenseRepo.updateExpense(expense) }
    fun deleteExpense(id: String) = viewModelScope.launch { expenseRepo.deleteExpense(id) }
    fun duplicate(id: String) = viewModelScope.launch { expenseRepo.duplicateExpense(id, userId) }

    suspend fun getSimplifiedBalances(groupId: String?, currency: String) =
        balanceRepo.getSimplifiedBalances(userId, groupId, currency)
}

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val groupRepo: GroupRepository,
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository,
    private val inviteRepo: InviteRepository
) : ViewModel() {
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups = _groups.asStateFlow()
    private val _members = MutableStateFlow<List<GroupMember>>(emptyList())
    val members = _members.asStateFlow()
    private var userId = ""

    init {
        viewModelScope.launch {
            authRepo.currentSession.filterNotNull().collect { session ->
                userId = session.userId
                groupRepo.observeGroups(session.userId).collect { _groups.value = it }
            }
        }
    }

    fun loadMembers(groupId: String) = viewModelScope.launch {
        groupRepo.observeMembers(groupId).collect { _members.value = it }
    }

    fun createGroup(name: String, description: String, emoji: String) = viewModelScope.launch {
        val profile = userRepo.getProfile(userId) ?: return@launch
        val group = Group(name = name, description = description, iconEmoji = emoji, ownerId = userId)
        val member = GroupMember(groupId = group.id, userId = userId, name = profile.name, isAdmin = true)
        groupRepo.createGroup(group, listOf(member))
    }

    fun archiveGroup(id: String) = viewModelScope.launch { groupRepo.archiveGroup(id) }
    fun deleteGroup(id: String) = viewModelScope.launch { groupRepo.deleteGroup(id) }
    fun createInvite(groupId: String?) = viewModelScope.launch { inviteRepo.createInviteLink(userId, groupId) }
}

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendRepo: FriendRepository,
    private val authRepo: AuthRepository
) : ViewModel() {
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends = _friends.asStateFlow()
    private var userId = ""

    init {
        viewModelScope.launch {
            authRepo.currentSession.filterNotNull().collect { session ->
                userId = session.userId
                friendRepo.observeFriends(session.userId).collect { _friends.value = it }
            }
        }
    }

    fun addFriend(name: String, email: String, mobile: String) = viewModelScope.launch {
        friendRepo.addFriend(userId, name, email, mobile)
    }

    fun search(query: String) = viewModelScope.launch {
        _friends.value = friendRepo.searchFriends(userId, query)
    }
}

@HiltViewModel
class SettlementViewModel @Inject constructor(
    private val settlementRepo: SettlementRepository,
    private val balanceRepo: BalanceRepository,
    private val authRepo: AuthRepository,
    private val userRepo: UserRepository
) : ViewModel() {
    val settlements = MutableStateFlow<List<Settlement>>(emptyList())
    val balances = MutableStateFlow<List<BalanceEntry>>(emptyList())
    private var userId = ""

    init {
        viewModelScope.launch {
            authRepo.currentSession.filterNotNull().collect { session ->
                userId = session.userId
                settlementRepo.observeSettlements(session.userId).collect { settlements.value = it }
            }
        }
    }

    fun loadBalances(currency: String) = viewModelScope.launch {
        balances.value = balanceRepo.getSimplifiedBalances(userId, null, currency)
    }

    fun settle(fromId: String, fromName: String, toId: String, toName: String, amount: Double, currency: String, method: SettlementMethod) =
        viewModelScope.launch {
            settlementRepo.addSettlement(Settlement(
                fromUserId = fromId, fromUserName = fromName,
                toUserId = toId, toUserName = toName,
                amount = amount, currencyCode = currency, method = method
            ))
            loadBalances(currency)
        }
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: PreferencesRepository,
    private val userRepo: UserRepository,
    private val authRepo: AuthRepository,
    private val exportRepo: ExportRepository
) : ViewModel() {
    val themeMode = prefsRepo.themeMode.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
    val accentColor = prefsRepo.accentColor.stateIn(viewModelScope, SharingStarted.Eagerly, 0xFF1B8A6B)
    val notificationPrefs = prefsRepo.notificationPrefs.stateIn(viewModelScope, SharingStarted.Eagerly, NotificationPreference())
    val chartStyle = prefsRepo.chartStyle.stateIn(viewModelScope, SharingStarted.Eagerly, ChartStyle.PIE)
    val dashboardLayout = prefsRepo.dashboardLayout.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardLayout.DETAILED)

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile = _profile.asStateFlow()

    init {
        viewModelScope.launch {
            authRepo.currentSession.filterNotNull().collect { session ->
                _profile.value = userRepo.getProfile(session.userId)
            }
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    fun setAccent(color: Long) = viewModelScope.launch { prefsRepo.setAccentColor(color) }
    fun setNotifications(prefs: NotificationPreference) = viewModelScope.launch { prefsRepo.setNotificationPrefs(prefs) }
    fun updateProfile(profile: UserProfile) = viewModelScope.launch { userRepo.updateProfile(profile); _profile.value = profile }
    fun changePassword(old: String, new: String, onResult: (Result<Unit>) -> Unit) = viewModelScope.launch {
        val id = _profile.value?.id ?: return@launch
        onResult(authRepo.changePassword(id, old, new))
    }
    fun deleteAccount() = viewModelScope.launch {
        _profile.value?.id?.let { userRepo.deleteAccount(it); authRepo.logout() }
    }
    suspend fun exportCsv(userId: String) = exportRepo.exportCsv(userId, null)
    suspend fun exportPdf(userId: String) = exportRepo.exportPdf(userId, null)
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val expenseRepo: ExpenseRepository,
    private val friendRepo: FriendRepository,
    private val groupRepo: GroupRepository,
    private val authRepo: AuthRepository
) : ViewModel() {
    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results = _results.asStateFlow()

    fun search(query: String) = viewModelScope.launch {
        val session = authRepo.currentSession.firstOrNull() ?: return@launch
        val expenses = expenseRepo.searchExpenses(session.userId, query, null).map {
            SearchResult(it.id, SearchResultType.EXPENSE, it.title, "${it.amount} ${it.currencyCode}", it.id)
        }
        val friends = friendRepo.searchFriends(session.userId, query).map {
            SearchResult(it.id, SearchResultType.FRIEND, it.name, it.email.ifBlank { it.mobile }, it.id)
        }
        val groups = groupRepo.observeGroups(session.userId).firstOrNull()?.filter {
            it.name.contains(query, true)
        }?.map {
            SearchResult(it.id, SearchResultType.GROUP, it.name, it.description, it.id)
        } ?: emptyList()
        _results.value = expenses + friends + groups
    }
}

private object InsightEngine {
    fun generateInsights(expenses: List<Expense>, categories: List<ExpenseCategory>, prev: Double) =
        com.expensetracker.domain.engine.InsightEngine.generateInsights(expenses, categories, prev)
}
