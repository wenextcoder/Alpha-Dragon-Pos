package com.alphadragon.pos.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.DailySummary
import com.alphadragon.pos.domain.usecase.report.GetDailySummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class ReportsUiState(
    val isLoading: Boolean = false,
    val todaySummary: DailySummary? = null,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val getDailySummaryUseCase: GetDailySummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState(isLoading = true))
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init { loadForDate(_uiState.value.selectedDateMillis) }

    fun previousDay() {
        val newDate = _uiState.value.selectedDateMillis - 86_400_000L
        _uiState.value = _uiState.value.copy(selectedDateMillis = newDate)
        loadForDate(newDate)
    }

    fun nextDay() {
        val now = System.currentTimeMillis()
        val newDate = (_uiState.value.selectedDateMillis + 86_400_000L).coerceAtMost(now)
        _uiState.value = _uiState.value.copy(selectedDateMillis = newDate)
        loadForDate(newDate)
    }

    fun reload() { loadForDate(_uiState.value.selectedDateMillis) }

    private fun loadForDate(dateMillis: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            getDailySummaryUseCase(dateMillis).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false, todaySummary = it) },
                onFailure = { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
            )
        }
    }
}
