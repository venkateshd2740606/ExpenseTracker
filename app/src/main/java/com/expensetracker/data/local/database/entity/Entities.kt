package com.expensetracker.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val mobile: String,
    val avatarUri: String?,
    val currencyCode: String,
    val languageCode: String,
    val themeMode: String,
    val accentColorArgb: Long,
    val countryCode: String,
    val chartStyle: String,
    val dashboardLayout: String,
    val passwordHash: String,
    val salt: String,
    val createdAt: Long
)

@Entity(tableName = "otp_challenges")
data class OtpChallengeEntity(
    @PrimaryKey val challengeId: String,
    val destination: String,
    val type: String,
    val codeHash: String,
    val expiresAt: Long,
    val verified: Boolean = false
)

@Entity(tableName = "friends", indices = [Index("userId")])
data class FriendEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val friendUserId: String?,
    val name: String,
    val email: String,
    val mobile: String,
    val avatarUri: String?,
    val isPending: Boolean,
    val createdAt: Long
)

@Entity(tableName = "groups", indices = [Index("ownerId")])
data class GroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconEmoji: String,
    val ownerId: String,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "group_members", indices = [Index("groupId"), Index("userId")])
data class GroupMemberEntity(
    @PrimaryKey val id: String,
    val groupId: String,
    val userId: String,
    val name: String,
    val isAdmin: Boolean,
    val joinedAt: Long
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val colorArgb: Long,
    val isCustom: Boolean,
    val userId: String?
)

@Entity(tableName = "expenses", indices = [Index("groupId"), Index("createdByUserId"), Index("expenseDate")])
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val groupId: String?,
    val title: String,
    val amount: Double,
    val currencyCode: String,
    val notes: String,
    val categoryId: String,
    val paidByUserId: String,
    val paidByName: String,
    val splitType: String,
    val participantsJson: String,
    val receiptUri: String?,
    val expenseDate: Long,
    val createdByUserId: String,
    val isRecurring: Boolean,
    val recurrenceFrequency: String?,
    val recurrenceEndDate: Long?,
    val parentRecurringId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncPending: Boolean
)

@Entity(tableName = "settlements", indices = [Index("fromUserId"), Index("toUserId")])
data class SettlementEntity(
    @PrimaryKey val id: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double,
    val currencyCode: String,
    val method: String,
    val groupId: String?,
    val notes: String,
    val settledAt: Long,
    val createdAt: Long,
    val syncPending: Boolean
)

@Entity(tableName = "activities", indices = [Index("actorUserId"), Index("timestamp")])
data class ActivityEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val groupId: String?,
    val entityId: String?,
    val actorUserId: String,
    val actorName: String,
    val timestamp: Long
)

@Entity(tableName = "invite_links")
data class InviteLinkEntity(
    @PrimaryKey val code: String,
    val groupId: String?,
    val inviterUserId: String,
    val expiresAt: Long
)

@Entity(tableName = "streaks", primaryKeys = ["userId", "dateKey"])
data class StreakEntity(
    val userId: String,
    val dateKey: String,
    val recordedAt: Long
)

@Entity(tableName = "achievements", primaryKeys = ["userId", "achievementId"])
data class AchievementEntity(
    val userId: String,
    val achievementId: String,
    val unlockedAt: Long
)
