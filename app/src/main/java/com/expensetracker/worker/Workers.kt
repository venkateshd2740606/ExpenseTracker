package com.expensetracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.expensetracker.data.local.PreferencesDataStore
import com.expensetracker.data.local.database.ExpenseTrackerDatabase
import com.expensetracker.data.mapper.EntityMappers
import com.expensetracker.domain.model.*
import com.expensetracker.domain.repository.ExpenseRepository
import com.expensetracker.domain.repository.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class RecurringExpenseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: ExpenseTrackerDatabase,
    private val mappers: EntityMappers,
    private val expenseRepo: ExpenseRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val recurring = db.expenseDao().getRecurring().map { mappers.toDomain(it) }
        val now = System.currentTimeMillis()
        recurring.forEach { expense ->
            if (shouldGenerate(expense, now)) {
                expenseRepo.addExpense(expense.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    parentRecurringId = expense.id,
                    expenseDate = now,
                    isRecurring = false,
                    createdAt = now,
                    updatedAt = now
                ))
            }
        }
        return Result.success()
    }

    private fun shouldGenerate(expense: Expense, now: Long): Boolean {
        if (expense.recurrenceEndDate != null && now > expense.recurrenceEndDate) return false
        val freq = expense.recurrenceFrequency ?: return false
        val cal = Calendar.getInstance().apply { timeInMillis = expense.expenseDate }
        val nowCal = Calendar.getInstance()
        return when (freq) {
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> cal.get(Calendar.WEEK_OF_YEAR) != nowCal.get(Calendar.WEEK_OF_YEAR)
            RecurrenceFrequency.MONTHLY -> cal.get(Calendar.MONTH) != nowCal.get(Calendar.MONTH)
            RecurrenceFrequency.YEARLY -> cal.get(Calendar.YEAR) != nowCal.get(Calendar.YEAR)
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RecurringExpenseWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("recurring_expenses", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepo: SyncRepository,
    private val prefs: PreferencesDataStore
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = prefs.sessionUserId.firstOrNull() ?: return Result.success()
        return syncRepo.syncAll(userId).fold({ Result.success() }, { Result.retry() })
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
