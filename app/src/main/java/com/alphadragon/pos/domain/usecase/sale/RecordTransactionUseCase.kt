package com.alphadragon.pos.domain.usecase.sale

import com.alphadragon.pos.domain.model.*
import com.alphadragon.pos.domain.repository.AuditLogRepository
import com.alphadragon.pos.domain.repository.ProductRepository
import com.alphadragon.pos.domain.repository.TransactionRepository
import com.alphadragon.pos.security.DeviceFingerprint
import java.util.UUID
import javax.inject.Inject

/**
 * Converts a completed cart into a persisted Transaction.
 * Called immediately on payment approval — no network required.
 */
class RecordTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val productRepository: ProductRepository,
    private val auditLogRepository: AuditLogRepository,
    private val deviceFingerprint: DeviceFingerprint
) {
    suspend operator fun invoke(
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        approvalCode: String? = null,
        merchantId: String? = null,
        terminalId: String? = null,
        customerName: String? = null,
        customerPhone: String? = null,
        customerEmail: String? = null,
        customerAddress: String? = null,
        cartDiscount: Double = 0.0
    ): Result<Transaction> {
        val now = System.currentTimeMillis()
        val txId = UUID.randomUUID().toString()
        val fp = deviceFingerprint.getCurrentFingerprint()

        val items = cartItems.map { cart ->
            TransactionItem(
                id = UUID.randomUUID().toString(),
                transactionId = txId,
                productId = cart.product.id,
                productName = cart.product.name,
                unitPrice = cart.product.price,
                quantity = cart.quantity,
                taxRate = cart.effectiveTaxRate,
                taxAmount = cart.lineTax,
                discount = cart.discountAmount,
                lineTotal = cart.lineTotal
            )
        }

        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val taxTotal = items.sumOf { it.taxAmount }
        val total = subtotal + taxTotal - cartDiscount

        val transaction = Transaction(
            id = txId,
            deviceFp = fp,
            timestamp = now,
            customerName = customerName?.takeIf { it.isNotBlank() },
            customerPhone = customerPhone?.takeIf { it.isNotBlank() },
            customerEmail = customerEmail?.takeIf { it.isNotBlank() },
            customerAddress = customerAddress?.takeIf { it.isNotBlank() },
            subtotal = subtotal,
            taxTotal = taxTotal,
            discountTotal = cartDiscount,
            total = total,
            paymentMethod = paymentMethod,
            merchantId = merchantId,
            terminalId = terminalId,
            approvalCode = approvalCode,
            status = TransactionStatus.COMPLETED
        )

        return transactionRepository.recordTransaction(transaction, items)
            .map { transaction }
            .also { result ->
                if (result.isSuccess) {
                    // Decrement stock for tracked products
                    cartItems.forEach { cart ->
                        if (cart.product.trackStock) {
                            productRepository.decrementStock(
                                cart.product.id,
                                cart.quantity.toInt()
                            )
                        }
                    }
                    auditLogRepository.log(
                        AuditLogEntry(
                            id = UUID.randomUUID().toString(),
                            action = "TRANSACTION_RECORDED",
                            detail = "{\"txId\":\"$txId\",\"total\":$total,\"method\":\"${paymentMethod.value}\"}",
                            timestamp = now
                        )
                    )
                }
            }
    }
}
