package com.alphadragon.merchant.manya

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Umanya API integration. */
class ManyaPaymentProvider @Inject constructor() : PaymentProvider {
    override val merchantId = "manya"
    override val displayName = "Umanya"

    override suspend fun initializeSDK(config: MerchantConfig): Result<Unit> =
        Result.failure(NotImplementedError("Umanya integration is Phase 2"))

    override suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse> =
        Result.failure(NotImplementedError("Umanya integration is Phase 2"))

    override suspend fun processRefund(request: RefundRequest): Result<RefundResponse> =
        Result.failure(NotImplementedError("Umanya integration is Phase 2"))

    override suspend fun voidTransaction(transactionId: String): Result<VoidResponse> =
        Result.failure(NotImplementedError("Umanya integration is Phase 2"))

    override suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus> =
        Result.failure(NotImplementedError("Umanya integration is Phase 2"))

    override fun isAvailableOffline() = false
}
