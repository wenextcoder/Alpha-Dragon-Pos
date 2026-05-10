package com.alphadragon.core.payment

/**
 * Implemented by each terminal module. The core app interacts only through this interface —
 * no terminal SDK is ever imported directly in :app.
 */
interface TerminalDriver {
    val terminalModel: String
    val connectionType: ConnectionType

    suspend fun connect(config: TerminalConfig): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    suspend fun startPayment(request: TerminalPaymentRequest): Result<TerminalPaymentResult>
    suspend fun startRefund(request: TerminalRefundRequest): Result<TerminalRefundResult>
    suspend fun cancelTransaction(): Result<Unit>
    fun getConnectionStatus(): TerminalConnectionStatus
    fun isConnected(): Boolean
}

enum class ConnectionType { USB, BLUETOOTH, TCPIP, BUILTIN }

enum class TerminalConnectionStatus { CONNECTED, DISCONNECTED, CONNECTING, ERROR }
