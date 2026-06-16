package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // ---------- User Profile ----------
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // ---------- Unlocked Cosmetics ----------
    @Query("SELECT * FROM unlocked_cosmetics")
    fun getUnlockedCosmetics(): Flow<List<UnlockedCosmetic>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUnlockedCosmetic(cosmetic: UnlockedCosmetic)

    // ---------- Match Records ----------
    @Query("SELECT * FROM match_records ORDER BY timestamp DESC")
    fun getMatchRecords(): Flow<List<MatchRecord>>

    @Insert
    suspend fun insertMatchRecord(record: MatchRecord)

    // ---------- Clans ----------
    @Query("SELECT * FROM clans")
    fun getClans(): Flow<List<Clan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClan(clan: Clan)

    // ---------- Achievements ----------
    @Query("SELECT * FROM achievements")
    fun getAchievements(): Flow<List<Achievement>>

    // *** NEW METHOD: The one your repository calls ***
    @Query("UPDATE achievements SET currentValue = :currentValue, completed = :completed WHERE id = :id")
    suspend fun updateAchievementProgress(id: String, currentValue: Int, completed: Boolean)

    // Optional: If you need to fetch a single achievement
    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): Achievement?

    // If you need to update the whole entity (alternative)
    @Update
    suspend fun updateAchievement(achievement: Achievement)
}