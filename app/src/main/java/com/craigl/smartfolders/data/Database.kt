package com.craigl.smartfolders.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "app_overrides", primaryKeys = ["packageName", "category"])
data class AppOverride(
    val packageName: String,
    val category: String,
    val isIncluded: Boolean // true = force include, false = force exclude
)

@Dao
interface AppOverrideDao {
    @Query("SELECT * FROM app_overrides")
    fun getAllOverrides(): Flow<List<AppOverride>>

    @Query("SELECT * FROM app_overrides")
    suspend fun getAllOverridesList(): List<AppOverride>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveOverride(override: AppOverride)

    @Query("DELETE FROM app_overrides WHERE packageName = :packageName AND category = :category")
    suspend fun deleteOverride(packageName: String, category: String)

    @Query("SELECT * FROM app_overrides WHERE packageName = :packageName")
    suspend fun getOverridesForApp(packageName: String): List<AppOverride>
}

@Database(entities = [AppOverride::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appOverrideDao(): AppOverrideDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_folders_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
