package com.alphadragon.pos.data.repository

import com.alphadragon.pos.data.local.dao.RefundDao
import com.alphadragon.pos.data.local.dao.TransactionDao
import com.alphadragon.pos.data.local.dao.TransactionItemDao
import com.alphadragon.pos.data.local.entity.RefundEntity
import com.alphadragon.pos.data.local.entity.TransactionEntity
import com.alphadragon.pos.data.local.entity.TransactionItemEntity
import com.alphadragon.pos.domain.model.*
import com.alphadragon.pos.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transactionItemDao: TransactionItemDao,
    private val refundDao: RefundDao
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        transactionDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByDateRange(from: Long, to: Long): Flow<List<Transaction>> =
        transactionDao.observeByDateRange(from, to).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: String): Transaction? =
        withContext(Dispatchers.IO) { transactionDao.getById(id)?.toDomain() }

    override suspend fun getItemsForTransaction(transactionId: String): List<TransactionItem> =
        withContext(Dispatchers.IO) {
            transactionItemDao.getByTransactionId(transactionId).map { it.toDomain() }
        }

    override suspend fun recordTransaction(
        transaction: Transaction,
        items: List<TransactionItem>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            transactionDao.insert(transaction.toEntity())
            transactionItemDao.insertAll(items.map { it.toEntity() })
        }
    }

    override suspend fun updateStatus(id: String, status: TransactionStatus): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { transactionDao.updateStatus(id, status.value) }
        }

    override suspend fun recordRefund(refund: Refund): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { refundDao.insert(refund.toEntity()) }
        }

    override suspend fun getRefundsForTransaction(transactionId: String): List<Refund> =
        withContext(Dispatchers.IO) {
            refundDao.getByTransactionId(transactionId).map { it.toDomain() }
        }

    override suspend fun getDailySummary(from: Long, to: Long): DailySummary =
        withContext(Dispatchers.IO) {
            val raw = transactionDao.getDailySummaryRaw(from, to)
            DailySummary(
                transactionCount = raw.count,
                totalRevenue = raw.revenue ?: 0.0,
                totalTax = raw.taxTotal ?: 0.0,
                cashRevenue = raw.cashRevenue ?: 0.0,
                cardRevenue = raw.cardRevenue ?: 0.0
            )
        }

    // Mapping helpers
    private fun TransactionEntity.toDomain() = Transaction(
        id = id, deviceFp = deviceFp, timestamp = timestamp,
        customerName = customerName, customerPhone = customerPhone, customerEmail = customerEmail,
        customerAddress = customerAddress,
        subtotal = subtotal, taxTotal = taxTotal, discountTotal = discountTotal, total = total,
        paymentMethod = PaymentMethod.from(paymentMethod),
        merchantId = merchantId, terminalId = terminalId, approvalCode = approvalCode,
        status = TransactionStatus.from(status), notes = notes, receiptRef = receiptRef
    )

    private fun Transaction.toEntity() = TransactionEntity(
        id = id, deviceFp = deviceFp, timestamp = timestamp,
        customerName = customerName, customerPhone = customerPhone, customerEmail = customerEmail,
        customerAddress = customerAddress,
        subtotal = subtotal, taxTotal = taxTotal, discountTotal = discountTotal, total = total,
        paymentMethod = paymentMethod.value,
        merchantId = merchantId, terminalId = terminalId, approvalCode = approvalCode,
        status = status.value, notes = notes, receiptRef = receiptRef
    )

    private fun TransactionItemEntity.toDomain() = TransactionItem(
        id = id, transactionId = transactionId, productId = productId,
        productName = productName, unitPrice = unitPrice, quantity = quantity,
        taxRate = taxRate, taxAmount = taxAmount, discount = discount, lineTotal = lineTotal
    )

    private fun TransactionItem.toEntity() = TransactionItemEntity(
        id = id, transactionId = transactionId, productId = productId,
        productName = productName, unitPrice = unitPrice, quantity = quantity,
        taxRate = taxRate, taxAmount = taxAmount, discount = discount, lineTotal = lineTotal
    )

    private fun RefundEntity.toDomain() = Refund(
        id = id, originalTxId = originalTxId, amount = amount,
        timestamp = timestamp, reason = reason, merchantRef = merchantRef,
        status = RefundStatus.from(status)
    )

    private fun Refund.toEntity() = RefundEntity(
        id = id, originalTxId = originalTxId, amount = amount,
        timestamp = timestamp, reason = reason, merchantRef = merchantRef,
        status = status.value
    )
}
