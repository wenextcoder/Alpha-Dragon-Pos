package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: CustomerEntity)

    @Update
    suspend fun update(entity: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun delete(id: String)
}
