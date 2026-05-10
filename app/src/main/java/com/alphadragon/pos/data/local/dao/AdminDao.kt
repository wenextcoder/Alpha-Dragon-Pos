package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.AdminEntity

@Dao
interface AdminDao {
    @Query("SELECT * FROM admin LIMIT 1")
    suspend fun getAdmin(): AdminEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(admin: AdminEntity)

    @Update
    suspend fun update(admin: AdminEntity)

    @Query("UPDATE admin SET last_login = :timestamp WHERE id = :id")
    suspend fun updateLastLogin(id: String, timestamp: Long)

    @Query("UPDATE admin SET pin_hash = :pinHash WHERE id = :id")
    suspend fun updatePinHash(id: String, pinHash: String)
}
