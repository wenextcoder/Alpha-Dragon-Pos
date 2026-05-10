package com.alphadragon.core.payment

/**
 * Implemented by each merchant module. The core app interacts only through this interface —
 * no merchant SDK is ever imported directly in :app.
 */
interface PaymentProvider {
    val merchantId: String
    val displayName: String

    suspend fun initializeSDK(config: MerchantConfig): Result<Unit>
    suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse>
    suspend fun processRefund(request: RefundRequest): Result<RefundResponse>
    suspend fun voidTransaction(transactionId: String): Result<VoidResponse>
    suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus>
    fun isAvailableOffline(): Boolean
}
