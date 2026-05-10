package com.alphadragon.merchant.worldpay

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Worldpay REST API + Android SDK integration. */
class WorldpayPaymentProvider @Inject constructor() : PaymentProvider {
    override val merchantId = "worldpay"
    override val displayName = "Worldpay"

    override suspend fun initializeSDK(config: MerchantConfig): Result<Unit> =
        Result.failure(NotImplementedError("Worldpay integration is Phase 2"))

    override suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse> =
        Result.failure(NotImplementedError("Worldpay integration is Phase 2"))

    override suspend fun processRefund(request: RefundRequest): Result<RefundResponse> =
        Result.failure(NotImplementedError("Worldpay integration is Phase 2"))

    override suspend fun voidTransaction(transactionId: String): Result<VoidResponse> =
        Result.failure(NotImplementedError("Worldpay integration is Phase 2"))

    override suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus> =
        Result.failure(NotImplementedError("Worldpay integration is Phase 2"))

    override fun isAvailableOffline() = false
}
