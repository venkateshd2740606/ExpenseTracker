package com.expensetracker.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.expensetracker.data.local.database.dao.*
import com.expensetracker.data.local.database.entity.*

@Database(
    entities = [
        UserEntity::class, OtpChallengeEntity::class, FriendEntity::class,
        GroupEntity::class, GroupMemberEntity::class, CategoryEntity::class,
        ExpenseEntity::class, SettlementEntity::class, ActivityEntity::class,
        InviteLinkEntity::class, StreakEntity::class, AchievementEntity::class,
        PersonalExpenseEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class ExpenseTrackerDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun otpDao(): OtpDao
    abstract fun friendDao(): FriendDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun settlementDao(): SettlementDao
    abstract fun activityDao(): ActivityDao
    abstract fun inviteDao(): InviteDao
    abstract fun streakDao(): StreakDao
    abstract fun achievementDao(): AchievementDao
    abstract fun personalExpenseDao(): PersonalExpenseDao
}
