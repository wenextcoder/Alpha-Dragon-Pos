package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.RefundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RefundDao {
    @Query("SELECT * FROM refunds ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<RefundEntity>>

    @Query("SELECT * FROM refunds WHERE original_tx_id = :transactionId ORDER BY timestamp DESC")
    suspend fun getByTransactionId(transactionId: String): List<RefundEntity>

    @Query("SELECT SUM(amount) FROM refunds WHERE original_tx_id = :transactionId AND status = 'completed'")
    suspend fun getTotalRefundedForTransaction(transactionId: String): Double?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(refund: RefundEntity)

    @Update
    suspend fun update(refund: RefundEntity)
}
