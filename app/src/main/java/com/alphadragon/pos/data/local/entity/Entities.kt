package com.alphadragon.pos.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val key: String,
    @ColumnInfo(name = "value") val value: String
)

@Entity(tableName = "admin")
data class AdminEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "pin_hash") val pinHash: String,       // bcrypt hash — never plain
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "last_login") val lastLogin: Long? = null
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("parent_id")]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "parent_id") val parentId: String? = null,  // null = top-level
    @ColumnInfo(name = "tax_rate") val taxRate: Double? = null,    // null = use global rule
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("category_id"), Index("barcode"), Index("sku")]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "sku") val sku: String? = null,
    @ColumnInfo(name = "barcode") val barcode: String? = null,
    @ColumnInfo(name = "category_id") val categoryId: String? = null,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "tax_rate") val taxRate: Double? = null,    // null = use category/global
    @ColumnInfo(name = "image_path") val imagePath: String? = null,
    @ColumnInfo(name = "track_stock") val trackStock: Boolean = false,
    @ColumnInfo(name = "stock_qty") val stockQty: Int = 0,
    @ColumnInfo(name = "low_stock_alert") val lowStockAlert: Int = 5,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "transactions", indices = [Index("timestamp"), Index("status")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "device_fp") val deviceFp: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "customer_name") val customerName: String? = null,
    @ColumnInfo(name = "customer_phone") val customerPhone: String? = null,
    @ColumnInfo(name = "customer_email") val customerEmail: String? = null,
    @ColumnInfo(name = "customer_address") val customerAddress: String? = null,
    @ColumnInfo(name = "subtotal") val subtotal: Double,
    @ColumnInfo(name = "tax_total") val taxTotal: Double,
    @ColumnInfo(name = "discount_total") val discountTotal: Double = 0.0,
    @ColumnInfo(name = "total") val total: Double,
    @ColumnInfo(name = "payment_method") val paymentMethod: String,  // cash | card | split
    @ColumnInfo(name = "merchant_id") val merchantId: String? = null,
    @ColumnInfo(name = "terminal_id") val terminalId: String? = null,
    @ColumnInfo(name = "approval_code") val approvalCode: String? = null,
    @ColumnInfo(name = "status") val status: String,  // completed | refunded | partial_refund | voided | pending_refund
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "receipt_ref") val receiptRef: String? = null
)

@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transaction_id")]
)
data class TransactionItemEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "transaction_id") val transactionId: String,
    @ColumnInfo(name = "product_id") val productId: String? = null,    // null if manual item
    @ColumnInfo(name = "product_name") val productName: String,        // snapshot at time of sale
    @ColumnInfo(name = "unit_price") val unitPrice: Double,
    @ColumnInfo(name = "quantity") val quantity: Double,
    @ColumnInfo(name = "tax_rate") val taxRate: Double,
    @ColumnInfo(name = "tax_amount") val taxAmount: Double,
    @ColumnInfo(name = "discount") val discount: Double = 0.0,
    @ColumnInfo(name = "line_total") val lineTotal: Double
)

@Entity(
    tableName = "refunds",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["original_tx_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("original_tx_id")]
)
data class RefundEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "original_tx_id") val originalTxId: String,
    @ColumnInfo(name = "amount") val amount: Double,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "reason") val reason: String? = null,
    @ColumnInfo(name = "merchant_ref") val merchantRef: String? = null,
    @ColumnInfo(name = "status") val status: String   // completed | pending
)

@Entity(tableName = "merchant_configs", indices = [Index("merchant_id", unique = true)])
data class MerchantConfigEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "merchant_id") val merchantId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "keystore_alias") val keystoreAlias: String,  // references Keystore entry
    @ColumnInfo(name = "terminal_id") val terminalId: String? = null,
    @ColumnInfo(name = "currency_code") val currencyCode: String,
    @ColumnInfo(name = "country_code") val countryCode: String,
    @ColumnInfo(name = "environment") val environment: String,  // SANDBOX | PRODUCTION
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)

@Entity(tableName = "terminal_configs")
data class TerminalConfigEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "terminal_model") val terminalModel: String,
    @ColumnInfo(name = "display_name") val displayName: String? = null,
    @ColumnInfo(name = "connection_type") val connectionType: String,  // USB|BLUETOOTH|TCPIP|BUILTIN
    @ColumnInfo(name = "ip_address") val ipAddress: String? = null,
    @ColumnInfo(name = "bluetooth_addr") val bluetoothAddr: String? = null,
    @ColumnInfo(name = "port") val port: Int? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

@Entity(tableName = "tax_rules")
data class TaxRuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "rate") val rate: Double,
    @ColumnInfo(name = "is_default") val isDefault: Boolean = false,
    @ColumnInfo(name = "applies_to") val appliesTo: String   // all | category | product
)

@Entity(tableName = "audit_log", indices = [Index("timestamp")])
data class AuditLogEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "detail") val detail: String? = null,  // JSON with action-specific context
    @ColumnInfo(name = "timestamp") val timestamp: Long
)
