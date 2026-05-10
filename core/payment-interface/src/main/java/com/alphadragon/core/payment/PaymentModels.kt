package com.alphadragon.core.payment

import kotlinx.serialization.Serializable

enum class Environment { SANDBOX, PRODUCTION }

@Serializable
data class MerchantConfig(
    val merchantId: String,
    val apiKey: String,           // loaded from Android Keystore at runtime — never in SQLite
    val environment: Environment,
    val terminalId: String,
    val currencyCode: String,     // ISO 4217 e.g. "GBP"
    val countryCode: String       // ISO 3166-1 alpha-2 e.g. "GB"
)

@Serializable
data class PaymentRequest(
    val transactionId: String,
    val amountCents: Long,        // amount in smallest currency unit
    val currencyCode: String,
    val description: String = ""
)

@Serializable
data class PaymentResponse(
    val transactionId: String,
    val approvalCode: String,
    val merchantRef: String,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class RefundRequest(
    val originalTransactionId: String,
    val amountCents: Long,
    val reason: String = ""
)

@Serializable
data class RefundResponse(
    val refundId: String,
    val merchantRef: String,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class VoidResponse(
    val transactionId: String,
    val success: Boolean,
    val errorMessage: String? = null
)

@Serializable
data class TransactionStatus(
    val transactionId: String,
    val status: String,
    val approvalCode: String? = null
)

// Terminal models

@Serializable
data class TerminalConfig(
    val terminalModel: String,
    val connectionType: ConnectionType,
    val ipAddress: String? = null,
    val bluetoothAddress: String? = null,
    val port: Int? = null
)

@Serializable
data class TerminalPaymentRequest(
    val transactionId: String,
    val amountCents: Long,
    val currencyCode: String
)

@Serializable
data class TerminalPaymentResult(
    val approved: Boolean,
    val approvalCode: String? = null,
    val cardLast4: String? = null,
    val errorMessage: String? = null
)

@Serializable
data class TerminalRefundRequest(
    val originalTransactionId: String,
    val amountCents: Long
)

@Serializable
data class TerminalRefundResult(
    val success: Boolean,
    val errorMessage: String? = null
)
