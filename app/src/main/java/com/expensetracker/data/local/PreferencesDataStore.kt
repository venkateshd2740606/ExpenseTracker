package com.expensetracker.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.expensetracker.domain.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("splitwise_prefs")

@Singleton
class PreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val SESSION_USER_ID = stringPreferencesKey("session_user_id")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
        val SESSION_EXPIRES = longPreferencesKey("session_expires")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val CHART_STYLE = stringPreferencesKey("chart_style")
        val DASHBOARD_LAYOUT = stringPreferencesKey("dashboard_layout")
        val NOTIF_EXPENSE = booleanPreferencesKey("notif_expense")
        val NOTIF_SETTLEMENT = booleanPreferencesKey("notif_settlement")
        val NOTIF_FRIEND = booleanPreferencesKey("notif_friend")
        val NOTIF_GROUP = booleanPreferencesKey("notif_group")
        val NOTIF_MONTHLY = booleanPreferencesKey("notif_monthly")
    }

    val sessionUserId: Flow<String?> = dataStore.data.map { it[SESSION_USER_ID] }
    val themeMode: Flow<ThemeMode> = dataStore.data.map {
        ThemeMode.valueOf(it[THEME_MODE] ?: ThemeMode.SYSTEM.name)
    }
    val accentColor: Flow<Long> = dataStore.data.map { it[ACCENT_COLOR] ?: 0xFF1B8A6B }
    val chartStyle: Flow<ChartStyle> = dataStore.data.map {
        ChartStyle.valueOf(it[CHART_STYLE] ?: ChartStyle.PIE.name)
    }
    val dashboardLayout: Flow<DashboardLayout> = dataStore.data.map {
        DashboardLayout.valueOf(it[DASHBOARD_LAYOUT] ?: DashboardLayout.DETAILED.name)
    }
    val notificationPrefs: Flow<NotificationPreference> = dataStore.data.map {
        NotificationPreference(
            expenseAdded = it[NOTIF_EXPENSE] ?: true,
            settlementReminder = it[NOTIF_SETTLEMENT] ?: true,
            friendRequest = it[NOTIF_FRIEND] ?: true,
            groupInvitation = it[NOTIF_GROUP] ?: true,
            monthlySummary = it[NOTIF_MONTHLY] ?: true
        )
    }

    suspend fun saveSession(userId: String, token: String, expiresAt: Long) {
        dataStore.edit {
            it[SESSION_USER_ID] = userId
            it[SESSION_TOKEN] = token
            it[SESSION_EXPIRES] = expiresAt
        }
    }

    suspend fun clearSession() {
        dataStore.edit {
            it.remove(SESSION_USER_ID)
            it.remove(SESSION_TOKEN)
            it.remove(SESSION_EXPIRES)
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun setAccentColor(color: Long) {
        dataStore.edit { it[ACCENT_COLOR] = color }
    }

    suspend fun setChartStyle(style: ChartStyle) {
        dataStore.edit { it[CHART_STYLE] = style.name }
    }

    suspend fun setDashboardLayout(layout: DashboardLayout) {
        dataStore.edit { it[DASHBOARD_LAYOUT] = layout.name }
    }

    suspend fun setNotificationPrefs(prefs: NotificationPreference) {
        dataStore.edit {
            it[NOTIF_EXPENSE] = prefs.expenseAdded
            it[NOTIF_SETTLEMENT] = prefs.settlementReminder
            it[NOTIF_FRIEND] = prefs.friendRequest
            it[NOTIF_GROUP] = prefs.groupInvitation
            it[NOTIF_MONTHLY] = prefs.monthlySummary
        }
    }
}
