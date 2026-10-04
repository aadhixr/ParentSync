package com.example.parentsync.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_usage_limits")
data class AppUsageLimitEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val category: String,
    val dailyQuotaMinutes: Int, // 0 = unlimited, 30, 60, etc.
    val isBlocked: Boolean = false
)

@Dao
interface AppUsageLimitDao {
    @Query("SELECT * FROM app_usage_limits")
    fun getAllLimits(): Flow<List<AppUsageLimitEntity>>

    @Query("SELECT * FROM app_usage_limits WHERE packageName = :packageName")
    suspend fun getLimit(packageName: String): AppUsageLimitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLimit(limit: AppUsageLimitEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLimitIfNotExists(limit: AppUsageLimitEntity)

    @Query("UPDATE app_usage_limits SET dailyQuotaMinutes = :quotaMinutes WHERE packageName = :packageName")
    suspend fun updateQuota(packageName: String, quotaMinutes: Int)

    @Query("UPDATE app_usage_limits SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateBlockStatus(packageName: String, isBlocked: Boolean)
}

@Database(entities = [AppUsageLimitEntity::class], version = 1, exportSchema = false)
abstract class ParentSyncDatabase : RoomDatabase() {
    abstract fun appUsageLimitDao(): AppUsageLimitDao

    companion object {
        @Volatile
        private var INSTANCE: ParentSyncDatabase? = null

        fun getDatabase(context: Context): ParentSyncDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ParentSyncDatabase::class.java,
                    "parent_sync_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
