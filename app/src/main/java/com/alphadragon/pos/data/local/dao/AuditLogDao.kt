package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int = 100): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: AuditLogEntity)

    /** Log the clear action first, then delete all older entries. */
    @Query("DELETE FROM audit_log WHERE id != :preserveId")
    suspend fun clearExcept(preserveId: String)
}
