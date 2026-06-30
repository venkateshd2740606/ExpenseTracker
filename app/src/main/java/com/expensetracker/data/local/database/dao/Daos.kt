package com.expensetracker.data.local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.data.local.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    fun observeUser(id: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUser(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE mobile = :mobile LIMIT 1")
    suspend fun getByMobile(mobile: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface OtpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(challenge: OtpChallengeEntity)

    @Query("SELECT * FROM otp_challenges WHERE challengeId = :id")
    suspend fun get(id: String): OtpChallengeEntity?

    @Query("UPDATE otp_challenges SET verified = 1 WHERE challengeId = :id")
    suspend fun markVerified(id: String)

    @Query("DELETE FROM otp_challenges WHERE expiresAt < :now")
    suspend fun deleteExpired(now: Long)
}

@Dao
interface FriendDao {
    @Query("SELECT * FROM friends WHERE userId = :userId ORDER BY name")
    fun observeFriends(userId: String): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE userId = :userId AND (name LIKE '%' || :q || '%' OR email LIKE '%' || :q || '%' OR mobile LIKE '%' || :q || '%')")
    suspend fun search(userId: String, q: String): List<FriendEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(friend: FriendEntity)

    @Query("DELETE FROM friends WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE friends SET isPending = 0 WHERE id = :id")
    suspend fun accept(id: String)
}

@Dao
interface GroupDao {
    @Query("SELECT g.* FROM groups g INNER JOIN group_members gm ON g.id = gm.groupId WHERE gm.userId = :userId AND g.isArchived = 0 ORDER BY g.updatedAt DESC")
    fun observeGroups(userId: String): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id")
    fun observeGroup(id: String): Flow<GroupEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(group: GroupEntity)

    @Update
    suspend fun update(group: GroupEntity)

    @Query("UPDATE groups SET isArchived = 1 WHERE id = :id")
    suspend fun archive(id: String)

    @Query("DELETE FROM groups WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun observeMembers(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    suspend fun getMembers(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE userId = :userId")
    suspend fun getGroupsForUser(userId: String): List<GroupMemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: GroupMemberEntity)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND userId = :userId")
    suspend fun remove(groupId: String, userId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId IS NULL OR userId = :userId")
    fun observeCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId IS NULL")
    suspend fun getDefaults(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: String)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE createdByUserId = :userId OR paidByUserId = :userId ORDER BY expenseDate DESC")
    fun observeForUser(userId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY expenseDate DESC")
    fun observeForGroup(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun get(id: String): ExpenseEntity?

    @Query("SELECT * FROM expenses WHERE syncPending = 1")
    suspend fun getPendingSync(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE (createdByUserId = :userId OR paidByUserId = :userId) AND (title LIKE '%' || :q || '%' OR notes LIKE '%' || :q || '%') ORDER BY expenseDate DESC")
    suspend fun search(userId: String, q: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE createdByUserId = :userId OR paidByUserId = :userId")
    suspend fun getAllForUser(userId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE groupId = :groupId")
    suspend fun getAllForGroup(groupId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE isRecurring = 1")
    suspend fun getRecurring(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface SettlementDao {
    @Query("SELECT * FROM settlements WHERE fromUserId = :userId OR toUserId = :userId ORDER BY settledAt DESC")
    fun observeForUser(userId: String): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE (fromUserId = :userId OR toUserId = :userId) AND (:groupId IS NULL OR groupId = :groupId) ORDER BY settledAt DESC")
    suspend fun getHistory(userId: String, groupId: String?): List<SettlementEntity>

    @Query("SELECT * FROM settlements WHERE fromUserId = :userId OR toUserId = :userId")
    suspend fun getAllForUser(userId: String): List<SettlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settlement: SettlementEntity)
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE actorUserId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun observe(userId: String, limit: Int): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: ActivityEntity)
}

@Dao
interface InviteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: InviteLinkEntity)

    @Query("SELECT * FROM invite_links WHERE code = :code AND expiresAt > :now")
    suspend fun getValid(code: String, now: Long): InviteLinkEntity?
}

@Dao
interface StreakDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: StreakEntity)

    @Query("SELECT dateKey FROM streaks WHERE userId = :userId ORDER BY dateKey DESC")
    suspend fun getDates(userId: String): List<String>
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(achievement: AchievementEntity)

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getForUser(userId: String): List<AchievementEntity>
}
