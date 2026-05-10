package com.alphadragon.merchant.firstdata

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Ufirstdata API integration. */
class FirstdataPaymentProvider @Inject constructor() : PaymentProvider {
    override val merchantId = "firstdata"
    override val displayName = "Ufirstdata"

    override suspend fun initializeSDK(config: MerchantConfig): Result<Unit> =
        Result.failure(NotImplementedError("Ufirstdata integration is Phase 2"))

    override suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse> =
        Result.failure(NotImplementedError("Ufirstdata integration is Phase 2"))

    override suspend fun processRefund(request: RefundRequest): Result<RefundResponse> =
        Result.failure(NotImplementedError("Ufirstdata integration is Phase 2"))

    override suspend fun voidTransaction(transactionId: String): Result<VoidResponse> =
        Result.failure(NotImplementedError("Ufirstdata integration is Phase 2"))

    override suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus> =
        Result.failure(NotImplementedError("Ufirstdata integration is Phase 2"))

    override fun isAvailableOffline() = false
}
