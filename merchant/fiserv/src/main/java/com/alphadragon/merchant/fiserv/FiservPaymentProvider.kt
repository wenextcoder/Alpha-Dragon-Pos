package com.alphadragon.merchant.fiserv

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Ufiserv API integration. */
class FiservPaymentProvider @Inject constructor() : PaymentProvider {
    override val merchantId = "fiserv"
    override val displayName = "Ufiserv"

    override suspend fun initializeSDK(config: MerchantConfig): Result<Unit> =
        Result.failure(NotImplementedError("Ufiserv integration is Phase 2"))

    override suspend fun processPayment(request: PaymentRequest): Result<PaymentResponse> =
        Result.failure(NotImplementedError("Ufiserv integration is Phase 2"))

    override suspend fun processRefund(request: RefundRequest): Result<RefundResponse> =
        Result.failure(NotImplementedError("Ufiserv integration is Phase 2"))

    override suspend fun voidTransaction(transactionId: String): Result<VoidResponse> =
        Result.failure(NotImplementedError("Ufiserv integration is Phase 2"))

    override suspend fun getTransactionStatus(transactionId: String): Result<TransactionStatus> =
        Result.failure(NotImplementedError("Ufiserv integration is Phase 2"))

    override fun isAvailableOffline() = false
}
