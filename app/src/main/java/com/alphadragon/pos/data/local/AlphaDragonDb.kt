package com.alphadragon.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alphadragon.pos.data.local.dao.*
import com.alphadragon.pos.data.local.entity.*

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN customer_address TEXT")
    }
}

@Database(
    entities = [
        AppConfigEntity::class,
        AdminEntity::class,
        CategoryEntity::class,
        ProductEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        RefundEntity::class,
        MerchantConfigEntity::class,
        TerminalConfigEntity::class,
        TaxRuleEntity::class,
        AuditLogEntity::class,
    ],
    version = 2,
    exportSchema = true
)
abstract class AlphaDragonDb : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
    abstract fun adminDao(): AdminDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun refundDao(): RefundDao
    abstract fun merchantConfigDao(): MerchantConfigDao
    abstract fun terminalConfigDao(): TerminalConfigDao
    abstract fun taxRuleDao(): TaxRuleDao
    abstract fun auditLogDao(): AuditLogDao
}
