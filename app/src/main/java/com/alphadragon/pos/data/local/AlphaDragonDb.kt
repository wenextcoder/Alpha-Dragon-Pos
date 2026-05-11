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

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS customers (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                phone TEXT NOT NULL,
                email TEXT,
                note TEXT,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_phone ON customers(phone)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_name ON customers(name)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE products ADD COLUMN description TEXT")
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
        CustomerEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class AlphaDragonDb : RoomDatabase() {
    abstract fun appConfigDao(): AppConfigDao
    abstract fun adminDao(): AdminDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun customerDao(): CustomerDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transactionItemDao(): TransactionItemDao
    abstract fun refundDao(): RefundDao
    abstract fun merchantConfigDao(): MerchantConfigDao
    abstract fun terminalConfigDao(): TerminalConfigDao
    abstract fun taxRuleDao(): TaxRuleDao
    abstract fun auditLogDao(): AuditLogDao
}
