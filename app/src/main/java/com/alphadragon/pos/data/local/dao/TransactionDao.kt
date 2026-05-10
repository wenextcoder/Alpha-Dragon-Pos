package com.alphadragon.pos.data.local.dao

import androidx.room.*
import com.alphadragon.pos.data.local.entity.TransactionEntity
import com.alphadragon.pos.data.local.entity.TransactionItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    fun observeByDateRange(from: Long, to: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = :status ORDER BY timestamp DESC")
    fun observeByStatus(status: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE payment_method = :method ORDER BY timestamp DESC")
    fun observeByPaymentMethod(method: String): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE customer_name LIKE '%' || :query || '%'
        ORDER BY timestamp DESC
    """)
    fun searchByCustomerName(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :from AND :to ORDER BY timestamp DESC")
    suspend fun getByDateRange(from: Long, to: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Query("UPDATE transactions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("""
        SELECT COUNT(*) as count, 
               SUM(total) as revenue,
               SUM(tax_total) as taxTotal,
               SUM(CASE WHEN payment_method = 'cash' THEN total ELSE 0 END) as cashRevenue,
               SUM(CASE WHEN payment_method = 'card' THEN total ELSE 0 END) as cardRevenue
        FROM transactions 
        WHERE timestamp BETWEEN :from AND :to 
          AND status NOT IN ('voided')
    """)
    suspend fun getDailySummaryRaw(from: Long, to: Long): DailySummaryRaw

    data class DailySummaryRaw(
        val count: Int,
        val revenue: Double?,
        val taxTotal: Double?,
        val cashRevenue: Double?,
        val cardRevenue: Double?
    )
}

@Dao
interface TransactionItemDao {
    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    suspend fun getByTransactionId(transactionId: String): List<TransactionItemEntity>

    @Query("SELECT * FROM transaction_items WHERE transaction_id = :transactionId")
    fun observeByTransactionId(transactionId: String): Flow<List<TransactionItemEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(items: List<TransactionItemEntity>)
}
