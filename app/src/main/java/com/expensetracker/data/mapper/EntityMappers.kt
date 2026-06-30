package com.expensetracker.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.expensetracker.data.local.database.entity.*
import com.expensetracker.domain.model.*

private val participantListType = object : TypeToken<List<ExpenseParticipant>>() {}.type

class EntityMappers(private val gson: Gson) {

    fun toDomain(entity: UserEntity) = UserProfile(
        id = entity.id, name = entity.name, email = entity.email, mobile = entity.mobile,
        avatarUri = entity.avatarUri, currencyCode = entity.currencyCode,
        languageCode = entity.languageCode, themeMode = ThemeMode.valueOf(entity.themeMode),
        accentColorArgb = entity.accentColorArgb, countryCode = entity.countryCode,
        chartStyle = ChartStyle.valueOf(entity.chartStyle),
        dashboardLayout = DashboardLayout.valueOf(entity.dashboardLayout),
        createdAt = entity.createdAt
    )

    fun toEntity(profile: UserProfile, passwordHash: String = "", salt: String = "") = UserEntity(
        id = profile.id, name = profile.name, email = profile.email, mobile = profile.mobile,
        avatarUri = profile.avatarUri, currencyCode = profile.currencyCode,
        languageCode = profile.languageCode, themeMode = profile.themeMode.name,
        accentColorArgb = profile.accentColorArgb, countryCode = profile.countryCode,
        chartStyle = profile.chartStyle.name, dashboardLayout = profile.dashboardLayout.name,
        passwordHash = passwordHash, salt = salt, createdAt = profile.createdAt
    )

    fun toDomain(entity: FriendEntity) = Friend(
        id = entity.id, userId = entity.userId, friendUserId = entity.friendUserId,
        name = entity.name, email = entity.email, mobile = entity.mobile,
        avatarUri = entity.avatarUri, isPending = entity.isPending, createdAt = entity.createdAt
    )

    fun toEntity(friend: Friend) = FriendEntity(
        id = friend.id, userId = friend.userId, friendUserId = friend.friendUserId,
        name = friend.name, email = friend.email, mobile = friend.mobile,
        avatarUri = friend.avatarUri, isPending = friend.isPending, createdAt = friend.createdAt
    )

    fun toDomain(entity: GroupEntity, memberIds: List<String> = emptyList()) = Group(
        id = entity.id, name = entity.name, description = entity.description,
        iconEmoji = entity.iconEmoji, ownerId = entity.ownerId, memberIds = memberIds,
        isArchived = entity.isArchived, createdAt = entity.createdAt, updatedAt = entity.updatedAt
    )

    fun toEntity(group: Group) = GroupEntity(
        id = group.id, name = group.name, description = group.description,
        iconEmoji = group.iconEmoji, ownerId = group.ownerId,
        isArchived = group.isArchived, createdAt = group.createdAt, updatedAt = group.updatedAt
    )

    fun toDomain(entity: GroupMemberEntity) = GroupMember(
        id = entity.id, groupId = entity.groupId, userId = entity.userId,
        name = entity.name, isAdmin = entity.isAdmin, joinedAt = entity.joinedAt
    )

    fun toEntity(member: GroupMember) = GroupMemberEntity(
        id = member.id, groupId = member.groupId, userId = member.userId,
        name = member.name, isAdmin = member.isAdmin, joinedAt = member.joinedAt
    )

    fun toDomain(entity: CategoryEntity) = ExpenseCategory(
        id = entity.id, name = entity.name, icon = entity.icon,
        colorArgb = entity.colorArgb, isCustom = entity.isCustom, userId = entity.userId
    )

    fun toEntity(category: ExpenseCategory) = CategoryEntity(
        id = category.id, name = category.name, icon = category.icon,
        colorArgb = category.colorArgb, isCustom = category.isCustom, userId = category.userId
    )

    fun toDomain(entity: ExpenseEntity) = Expense(
        id = entity.id, groupId = entity.groupId, title = entity.title, amount = entity.amount,
        currencyCode = entity.currencyCode, notes = entity.notes, categoryId = entity.categoryId,
        paidByUserId = entity.paidByUserId, paidByName = entity.paidByName,
        splitType = SplitType.valueOf(entity.splitType),
        participants = gson.fromJson(entity.participantsJson, participantListType),
        receiptUri = entity.receiptUri, expenseDate = entity.expenseDate,
        createdByUserId = entity.createdByUserId, isRecurring = entity.isRecurring,
        recurrenceFrequency = entity.recurrenceFrequency?.let { RecurrenceFrequency.valueOf(it) },
        recurrenceEndDate = entity.recurrenceEndDate, parentRecurringId = entity.parentRecurringId,
        createdAt = entity.createdAt, updatedAt = entity.updatedAt, syncPending = entity.syncPending
    )

    fun toEntity(expense: Expense) = ExpenseEntity(
        id = expense.id, groupId = expense.groupId, title = expense.title, amount = expense.amount,
        currencyCode = expense.currencyCode, notes = expense.notes, categoryId = expense.categoryId,
        paidByUserId = expense.paidByUserId, paidByName = expense.paidByName,
        splitType = expense.splitType.name,
        participantsJson = gson.toJson(expense.participants),
        receiptUri = expense.receiptUri, expenseDate = expense.expenseDate,
        createdByUserId = expense.createdByUserId, isRecurring = expense.isRecurring,
        recurrenceFrequency = expense.recurrenceFrequency?.name,
        recurrenceEndDate = expense.recurrenceEndDate, parentRecurringId = expense.parentRecurringId,
        createdAt = expense.createdAt, updatedAt = expense.updatedAt, syncPending = expense.syncPending
    )

    fun toDomain(entity: SettlementEntity) = Settlement(
        id = entity.id, fromUserId = entity.fromUserId, fromUserName = entity.fromUserName,
        toUserId = entity.toUserId, toUserName = entity.toUserName, amount = entity.amount,
        currencyCode = entity.currencyCode, method = SettlementMethod.valueOf(entity.method),
        groupId = entity.groupId, notes = entity.notes, settledAt = entity.settledAt,
        createdAt = entity.createdAt, syncPending = entity.syncPending
    )

    fun toEntity(settlement: Settlement) = SettlementEntity(
        id = settlement.id, fromUserId = settlement.fromUserId, fromUserName = settlement.fromUserName,
        toUserId = settlement.toUserId, toUserName = settlement.toUserName, amount = settlement.amount,
        currencyCode = settlement.currencyCode, method = settlement.method.name,
        groupId = settlement.groupId, notes = settlement.notes, settledAt = settlement.settledAt,
        createdAt = settlement.createdAt, syncPending = settlement.syncPending
    )

    fun toDomain(entity: ActivityEntity) = ActivityItem(
        id = entity.id, type = ActivityType.valueOf(entity.type),
        title = entity.title, subtitle = entity.subtitle, groupId = entity.groupId,
        entityId = entity.entityId, actorUserId = entity.actorUserId,
        actorName = entity.actorName, timestamp = entity.timestamp
    )

    fun toEntity(item: ActivityItem) = ActivityEntity(
        id = item.id, type = item.type.name, title = item.title, subtitle = item.subtitle,
        groupId = item.groupId, entityId = item.entityId, actorUserId = item.actorUserId,
        actorName = item.actorName, timestamp = item.timestamp
    )
}
