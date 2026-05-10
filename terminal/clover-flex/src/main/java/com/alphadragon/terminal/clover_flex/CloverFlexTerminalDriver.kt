package com.alphadragon.terminal.clover_flex

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Clover Flex built-in terminal driver. */
class CloverFlexTerminalDriver @Inject constructor() : TerminalDriver {
    override val terminalModel = "clover-flex"
    override val connectionType = ConnectionType.BUILTIN

    private var status = TerminalConnectionStatus.DISCONNECTED

    override suspend fun connect(config: TerminalConfig): Result<Unit> =
        Result.failure(NotImplementedError("Clover Flex terminal driver is Phase 2"))

    override suspend fun disconnect(): Result<Unit> = Result.success(Unit)

    override suspend fun startPayment(request: TerminalPaymentRequest): Result<TerminalPaymentResult> =
        Result.failure(NotImplementedError("Clover Flex terminal driver is Phase 2"))

    override suspend fun startRefund(request: TerminalRefundRequest): Result<TerminalRefundResult> =
        Result.failure(NotImplementedError("Clover Flex terminal driver is Phase 2"))

    override suspend fun cancelTransaction(): Result<Unit> = Result.success(Unit)
    override fun getConnectionStatus() = status
    override fun isConnected() = false
}
