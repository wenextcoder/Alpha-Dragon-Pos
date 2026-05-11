package com.alphadragon.pos.domain.model

/** Admin account — single record, created at first launch. */
data class Admin(
    val id: String,
    val username: String,
    val displayName: String,
    val pinHash: String,    // bcrypt hash — never plain
    val createdAt: Long,
    val lastLogin: Long? = null
)

data class Product(
    val id: String,
    val name: String,
    val description: String? = null,
    val sku: String? = null,
    val barcode: String? = null,
    val categoryId: String? = null,
    val price: Double,
    val taxRate: Double? = null,   // null = use category/global rule
    val imagePath: String? = null,
    val trackStock: Boolean = false,
    val stockQty: Int = 0,
    val lowStockAlert: Int = 5,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
) {
    val isLowStock: Boolean get() = trackStock && stockQty <= lowStockAlert
}

data class Category(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val taxRate: Double? = null,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

/** Saved customer for quick lookup and marketing (optional; receipts still store snapshot on each sale). */
data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)

/** Cart item — transient, lives in ViewModel only until payment confirmed. */
data class CartItem(
    val product: Product,
    val quantity: Double,
    val discountAmount: Double = 0.0
) {
    val effectiveTaxRate: Double get() = product.taxRate ?: 0.0
    val lineSubtotal: Double get() = product.price * quantity
    val lineTax: Double get() = (lineSubtotal - discountAmount) * effectiveTaxRate
    val lineTotal: Double get() = lineSubtotal - discountAmount + lineTax
}

data class Transaction(
    val id: String,
    val deviceFp: String,
    val timestamp: Long,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerEmail: String? = null,
    val customerAddress: String? = null,
    val subtotal: Double,
    val taxTotal: Double,
    val discountTotal: Double = 0.0,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val merchantId: String? = null,
    val terminalId: String? = null,
    val approvalCode: String? = null,
    val status: TransactionStatus,
    val notes: String? = null,
    val receiptRef: String? = null
)

data class TransactionItem(
    val id: String,
    val transactionId: String,
    val productId: String? = null,
    val productName: String,
    val unitPrice: Double,
    val quantity: Double,
    val taxRate: Double,
    val taxAmount: Double,
    val discount: Double = 0.0,
    val lineTotal: Double
)

data class Refund(
    val id: String,
    val originalTxId: String,
    val amount: Double,
    val timestamp: Long,
    val reason: String? = null,
    val merchantRef: String? = null,
    val status: RefundStatus
)

data class AuditLogEntry(
    val id: String,
    val action: String,
    val detail: String? = null,  // JSON
    val timestamp: Long
)

enum class PaymentMethod(val value: String) {
    CASH("cash"), CARD("card"), SPLIT("split");

    companion object {
        fun from(value: String) = entries.first { it.value == value }
    }
}

enum class TransactionStatus(val value: String) {
    COMPLETED("completed"),
    REFUNDED("refunded"),
    PARTIAL_REFUND("partial_refund"),
    VOIDED("voided"),
    PENDING_REFUND("pending_refund");

    companion object {
        fun from(value: String) = entries.first { it.value == value }
    }
}

enum class RefundStatus(val value: String) {
    COMPLETED("completed"), PENDING("pending");

    companion object {
        fun from(value: String) = entries.first { it.value == value }
    }
}

data class DailySummary(
    val transactionCount: Int,
    val totalRevenue: Double,
    val totalTax: Double,
    val cashRevenue: Double = 0.0,
    val cardRevenue: Double = 0.0
)

/** In-memory session — never persisted to disk. */
data class Session(
    val token: String,         // UUID
    val adminId: String,
    val createdAt: Long,
    val lastActivityAt: Long
)
