package com.alphadragon.core.testing

import com.alphadragon.core.payment.*

/** Shared test fake for PaymentProvider. */
class FakePaymentProvider(
    override val merchantId: String = "fake_merchant",
    override val displayName: String = "Fake Merchant"
) : PaymentProvider {

    var initResult: Result<Unit> = Result.success(Unit)
    var paymentResult: Result<PaymentResponse> = Result.success(
        PaymentResponse("tx_001", "APPROVED", "ref_001", true)
    )
    var refundResult: Result<RefundResponse> = Result.success(
        RefundResponse("ref_001", "ref_001", true)
    )
    var voidResult: Result<VoidResponse> = Result.success(VoidResponse("tx_001", true))
    var statusResult: Result<TransactionStatus> = Result.success(
        TransactionStatus("tx_001", "COMPLETED", "APPROVED")
    )

    override suspend fun initializeSDK(config: MerchantConfig) = initResult
    override suspend fun processPayment(request: PaymentRequest) = paymentResult
    override suspend fun processRefund(request: RefundRequest) = refundResult
    override suspend fun voidTransaction(transactionId: String) = voidResult
    override suspend fun getTransactionStatus(transactionId: String) = statusResult
    override fun isAvailableOffline() = false
}

/** Shared test fake for TerminalDriver. */
class FakeTerminalDriver(
    override val terminalModel: String = "fake_terminal",
    override val connectionType: ConnectionType = ConnectionType.TCPIP
) : TerminalDriver {

    private var status = TerminalConnectionStatus.DISCONNECTED
    var connectResult: Result<Unit> = Result.success(Unit)
    var paymentResult: Result<TerminalPaymentResult> = Result.success(
        TerminalPaymentResult(true, "APPROVED", "1234")
    )

    override suspend fun connect(config: TerminalConfig): Result<Unit> {
        return connectResult.also { if (it.isSuccess) status = TerminalConnectionStatus.CONNECTED }
    }

    override suspend fun disconnect(): Result<Unit> {
        status = TerminalConnectionStatus.DISCONNECTED
        return Result.success(Unit)
    }

    override suspend fun startPayment(request: TerminalPaymentRequest) = paymentResult

    override suspend fun startRefund(request: TerminalRefundRequest) =
        Result.success(TerminalRefundResult(true))

    override suspend fun cancelTransaction() = Result.success(Unit)
    override fun getConnectionStatus() = status
    override fun isConnected() = status == TerminalConnectionStatus.CONNECTED
}
