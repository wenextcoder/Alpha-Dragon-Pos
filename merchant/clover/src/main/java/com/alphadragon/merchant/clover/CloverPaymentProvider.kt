package com.alphadragon.merchant.clover

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Uclover API integration. */
class CloverPaymentProvider @Inject constructor() : PaymentProvider {
    override val merchantId = "clover"
    override val displayName = "Uclover"

    override suspend fun initializeSDK(config: MerchantConfig): Result<Unit> =
        Result.failure(NotImplementedError("Uclover integration is Phase 2"))

    override suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse> =
        Result.failure(NotImplementedError("Uclover integration is Phase 2"))

    override suspend fun processRefund(request: RefundRequest): Result<RefundResponse> =
        Result.failure(NotImplementedError("Uclover integration is Phase 2"))

    override suspend fun voidTransaction(transactionId: String): Result<VoidResponse> =
        Result.failure(NotImplementedError("Uclover integration is Phase 2"))

    override suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus> =
        Result.failure(NotImplementedError("Uclover integration is Phase 2"))

    override fun isAvailableOffline() = false
}
