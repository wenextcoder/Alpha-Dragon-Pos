package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.AppConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppConfigDao {
    @Query("SELECT value FROM app_config WHERE key = :key")
    suspend fun get(key: String): String?

    @Query("SELECT value FROM app_config WHERE key = :key")
    fun observe(key: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(config: AppConfigEntity)

    @Query("DELETE FROM app_config WHERE key = :key")
    suspend fun delete(key: String)

    @Query("SELECT COUNT(*) FROM app_config WHERE key = 'setup_complete' AND value = 'true'")
    suspend fun isSetupComplete(): Int
}
