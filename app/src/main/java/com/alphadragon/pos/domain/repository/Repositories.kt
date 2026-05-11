package com.alphadragon.pos.domain.repository

import com.alphadragon.pos.domain.model.*
import kotlinx.coroutines.flow.Flow

/** Zero Android imports in this file — pure Kotlin interfaces. */

interface AdminRepository {
    suspend fun isSetupComplete(): Boolean
    suspend fun getAdmin(): Admin?
    suspend fun createAdmin(admin: Admin, shopName: String): Result<Unit>
    suspend fun updateLastLogin(adminId: String, timestamp: Long): Result<Unit>
    suspend fun updatePinHash(adminId: String, pinHash: String): Result<Unit>
}

interface AppConfigRepository {
    suspend fun get(key: String): String?
    fun observe(key: String): Flow<String?>
    suspend fun set(key: String, value: String): Result<Unit>
    suspend fun delete(key: String): Result<Unit>
}

interface ProductRepository {
    fun observeActiveProducts(): Flow<List<Product>>
    fun observeProductsByCategory(categoryId: String): Flow<List<Product>>
    fun searchProducts(query: String): Flow<List<Product>>
    suspend fun getProductByBarcode(barcode: String): Product?
    suspend fun getProductById(id: String): Product?
    fun observeLowStockProducts(): Flow<List<Product>>
    suspend fun saveProduct(product: Product): Result<Unit>
    suspend fun saveProducts(products: List<Product>): Result<Unit>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun deactivateProduct(id: String): Result<Unit>
    suspend fun decrementStock(productId: String, qty: Int): Result<Unit>
    fun observeTopLevelCategories(): Flow<List<Category>>
    fun observeSubcategories(parentId: String): Flow<List<Category>>
    suspend fun saveCategory(category: Category): Result<Unit>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deactivateCategory(id: String): Result<Unit>
}

interface CustomerRepository {
    fun observeAll(): Flow<List<Customer>>
    suspend fun getById(id: String): Customer?
    suspend fun insert(customer: Customer): Result<Unit>
    suspend fun update(customer: Customer): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}

interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeByDateRange(from: Long, to: Long): Flow<List<Transaction>>
    suspend fun getById(id: String): Transaction?
    suspend fun getItemsForTransaction(transactionId: String): List<TransactionItem>
    suspend fun recordTransaction(transaction: Transaction, items: List<TransactionItem>): Result<Unit>
    suspend fun updateStatus(id: String, status: TransactionStatus): Result<Unit>
    suspend fun recordRefund(refund: Refund): Result<Unit>
    suspend fun getRefundsForTransaction(transactionId: String): List<Refund>
    suspend fun getDailySummary(from: Long, to: Long): DailySummary
}

interface AuditLogRepository {
    fun observeAll(): Flow<List<AuditLogEntry>>
    suspend fun log(entry: AuditLogEntry): Result<Unit>
    suspend fun clearLog(clearEntryId: String): Result<Unit>
}
