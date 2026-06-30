package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.expensetracker.data.local.database.ExpenseTrackerDatabase
import com.expensetracker.data.mapper.EntityMappers
import com.expensetracker.data.repository.*
import com.expensetracker.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideGson() = Gson()

    @Provides @Singleton
    fun provideMappers(gson: Gson) = EntityMappers(gson)

    @Provides @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ExpenseTrackerDatabase =
        Room.databaseBuilder(context, ExpenseTrackerDatabase::class.java, "expensetracker.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun providePersonalExpenseDao(db: ExpenseTrackerDatabase) = db.personalExpenseDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun bindAuth(impl: AuthRepositoryImpl): AuthRepository
    @Binds @Singleton abstract fun bindUser(impl: UserRepositoryImpl): UserRepository
    @Binds @Singleton abstract fun bindFriend(impl: FriendRepositoryImpl): FriendRepository
    @Binds @Singleton abstract fun bindGroup(impl: GroupRepositoryImpl): GroupRepository
    @Binds @Singleton abstract fun bindExpense(impl: ExpenseRepositoryImpl): ExpenseRepository
    @Binds @Singleton abstract fun bindSettlement(impl: SettlementRepositoryImpl): SettlementRepository
    @Binds @Singleton abstract fun bindCategory(impl: CategoryRepositoryImpl): CategoryRepository
    @Binds @Singleton abstract fun bindActivity(impl: ActivityRepositoryImpl): ActivityRepository
    @Binds @Singleton abstract fun bindBalance(impl: BalanceRepositoryImpl): BalanceRepository
    @Binds @Singleton abstract fun bindDashboard(impl: DashboardRepositoryImpl): DashboardRepository
    @Binds @Singleton abstract fun bindPrefs(impl: PreferencesRepositoryImpl): PreferencesRepository
    @Binds @Singleton abstract fun bindSync(impl: SyncRepositoryImpl): SyncRepository
    @Binds @Singleton abstract fun bindExport(impl: ExportRepositoryImpl): ExportRepository
    @Binds @Singleton abstract fun bindInvite(impl: InviteRepositoryImpl): InviteRepository
    @Binds @Singleton abstract fun bindPersonalExpense(impl: PersonalExpenseRepositoryImpl): PersonalExpenseRepository
}
