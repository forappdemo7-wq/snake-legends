package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    // ---------- User Profile ----------
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getUserProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    // ---------- Unlocked Cosmetics ----------
    @Query("SELECT * FROM unlocked_cosmetic")
    fun getUnlockedCosmetics(): Flow<List<UnlockedCosmetic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnlockedCosmetic(cosmetic: UnlockedCosmetic)

    // ---------- Match Records ----------
    @Query("SELECT * FROM match_record ORDER BY timestamp DESC")
    fun getMatchRecords(): Flow<List<MatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatchRecord(record: MatchRecord)

    // ---------- Clans ----------
    @Query("SELECT * FROM clan ORDER BY totalScore DESC")
    fun getClans(): Flow<List<Clan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClan(clan: Clan)

    // ---------- Achievements ----------
    @Query("SELECT * FROM achievement")
    fun getAchievements(): Flow<List<Achievement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: Achievement)   // <-- Added missing method

    @Query("UPDATE achievement SET currentValue = :currentVal, completed = :compl WHERE id = :id")
    suspend fun updateAchievementProgress(id: String, currentVal: Int, compl: Boolean)

    @Query("SELECT * FROM achievement WHERE id = :id")
    suspend fun getAchievementById(id: String): Achievement?   // <-- Added for completeness

    @Update
    suspend fun updateAchievement(achievement: Achievement)   // <-- Added for completeness
}