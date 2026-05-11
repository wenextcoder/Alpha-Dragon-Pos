package com.alphadragon.pos.ui.currency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.core.common.ConfigKeys
import com.alphadragon.pos.domain.repository.AppConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject

const val DEFAULT_CURRENCY_CODE = "GBP"

val SupportedCurrencies = listOf("GBP", "USD", "BDT", "EUR", "INR")

@HiltViewModel
class CurrencyViewModel @Inject constructor(
    appConfigRepository: AppConfigRepository
) : ViewModel() {
    val currencyCode: StateFlow<String> = appConfigRepository.observe(ConfigKeys.SHOP_CURRENCY)
        .map { it?.takeIf(::isSupportedCurrencyCode) ?: DEFAULT_CURRENCY_CODE }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DEFAULT_CURRENCY_CODE
        )
}

@Composable
fun rememberCurrencyFormatter(
    viewModel: CurrencyViewModel = hiltViewModel()
): NumberFormat {
    val currencyCode by viewModel.currencyCode.collectAsState()
    return remember(currencyCode) { currencyFormatter(currencyCode) }
}

fun currencyFormatter(currencyCode: String): NumberFormat =
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance(
            currencyCode.takeIf(::isSupportedCurrencyCode) ?: DEFAULT_CURRENCY_CODE
        )
    }

fun isSupportedCurrencyCode(value: String): Boolean =
    runCatching { Currency.getInstance(value) }.isSuccess
