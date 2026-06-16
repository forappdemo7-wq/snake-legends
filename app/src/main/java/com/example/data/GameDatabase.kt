package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserProfile::class,
        UnlockedCosmetic::class,
        MatchRecord::class,
        Clan::class,
        Achievement::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "snake_legends_db"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.gameDao()
                    // Prepopulate User
                    dao.insertUserProfile(UserProfile())
                    // Prepopulate Default Unlocked Cosmetics
                    dao.insertUnlockedCosmetic(UnlockedCosmetic("Neon Cyber", "skin"))
                    dao.insertUnlockedCosmetic(UnlockedCosmetic("Neon Sparkle", "trail"))
                    
                    // Prepopulate Achievements
                    val achievementsList = listOf(
                        Achievement("first_kill", "First Slaying", "Eliminate your first opponent", 0, 1, false, 150),
                        Achievement("score_500", "Bounty Collector", "Achieve a length of 500 in a single match", 0, 500, false, 200),
                        Achievement("level_5", "Rising Legend", "Reach Player Level 5", 1, 5, false, 250),
                        Achievement("br_win", "Slither King", "Win a Battle Royale match", 0, 1, false, 500)
                    )
                    achievementsList.forEach { dao.insertAchievement(it) }

                    // Prepopulate Clans
                    val clansList = listOf(
                        Clan("Cyber Snakes", "CYBER", 14500, 18),
                        Clan("Apex Predators", "APEX", 12100, 15),
                        Clan("Viper Strike", "VIPER", 9800, 12)
                    )
                    clansList.forEach { dao.insertClan(it) }
                }
            }
        }
    }
}
