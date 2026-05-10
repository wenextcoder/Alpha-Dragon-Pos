package com.alphadragon.merchant.cardnet

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Ucardnet API integration. */
class CardnetPaymentProvider @Inject constructor() : PaymentProvider {
    override val merchantId = "cardnet"
    override val displayName = "Ucardnet"

    override suspend fun initializeSDK(config: MerchantConfig): Result<Unit> =
        Result.failure(NotImplementedError("Ucardnet integration is Phase 2"))

    override suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse> =
        Result.failure(NotImplementedError("Ucardnet integration is Phase 2"))

    override suspend fun processRefund(request: RefundRequest): Result<RefundResponse> =
        Result.failure(NotImplementedError("Ucardnet integration is Phase 2"))

    override suspend fun voidTransaction(transactionId: String): Result<VoidResponse> =
        Result.failure(NotImplementedError("Ucardnet integration is Phase 2"))

    override suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus> =
        Result.failure(NotImplementedError("Ucardnet integration is Phase 2"))

    override fun isAvailableOffline() = false
}
