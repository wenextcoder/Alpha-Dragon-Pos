package com.alphadragon.terminal.ingenico

import com.alphadragon.core.payment.*
import javax.inject.Inject

/** Phase 2: Uingenico terminal driver. */
class UingenicoTerminalDriver @Inject constructor() : TerminalDriver {
    override val terminalModel = "ingenico"
    override val connectionType = ConnectionType.USB

    private var status = TerminalConnectionStatus.DISCONNECTED

    override suspend fun connect(config: TerminalConfig): Result<Unit> =
        Result.failure(NotImplementedError("Uingenico terminal driver is Phase 2"))

    override suspend fun disconnect(): Result<Unit> = Result.success(Unit)

    override suspend fun startPayment(request: TerminalPaymentRequest): Result<TerminalPaymentResult> =
        Result.failure(NotImplementedError("Uingenico terminal driver is Phase 2"))

    override suspend fun startRefund(request: TerminalRefundRequest): Result<TerminalRefundResult> =
        Result.failure(NotImplementedError("Uingenico terminal driver is Phase 2"))

    override suspend fun cancelTransaction(): Result<Unit> = Result.success(Unit)
    override fun getConnectionStatus() = status
    override fun isConnected() = false
}
