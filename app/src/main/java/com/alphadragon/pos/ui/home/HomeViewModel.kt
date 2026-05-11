package com.alphadragon.pos.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alphadragon.pos.domain.model.AuditLogEntry
import com.alphadragon.pos.domain.model.DailySummary
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.repository.AdminRepository
import com.alphadragon.pos.domain.repository.AuditLogRepository
import com.alphadragon.pos.domain.repository.TransactionRepository
import com.alphadragon.pos.domain.usecase.report.GetDailySummaryUseCase
import com.alphadragon.pos.security.DeviceFingerprint
import com.alphadragon.pos.security.RootDetection
import com.alphadragon.pos.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

enum class GrowthChartGranularity {
    WEEKLY,
    MONTHLY,
    YEARLY
}

enum class GrowthChartStyle {
    LINE,
    COLUMN
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val todaySummary: DailySummary = DailySummary(
        transactionCount = 0,
        totalRevenue = 0.0,
        totalTax = 0.0
    ),
    val recentTransactions: List<Transaction> = emptyList(),
    val growthPoints: List<GrowthPoint> = emptyList(),
    val growthGranularity: GrowthChartGranularity = GrowthChartGranularity.WEEKLY,
    val growthChartStyle: GrowthChartStyle = GrowthChartStyle.LINE,
    /** Always local-calendar today (overview uses this day only). */
    val overviewAnchorMillis: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

data class GrowthPoint(
    val label: String,
    val revenue: Double
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val deviceFingerprint: DeviceFingerprint,
    private val rootDetection: RootDetection,
    private val auditLogRepository: AuditLogRepository,
    private val transactionRepository: TransactionRepository,
    private val getDailySummaryUseCase: GetDailySummaryUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _startDestination = MutableStateFlow(NavRoutes.LOGIN)
    val startDestination: StateFlow<String> = _startDestination.asStateFlow()

    private val _startDestinationReady = MutableStateFlow(false)
    val startDestinationReady: StateFlow<Boolean> = _startDestinationReady.asStateFlow()

    private val _showRootWarning = MutableStateFlow(false)
    val showRootWarning: StateFlow<Boolean> = _showRootWarning.asStateFlow()

    private val _deviceMismatch = MutableStateFlow(false)
    val deviceMismatch: StateFlow<Boolean> = _deviceMismatch.asStateFlow()

    private val _growthGranularity = MutableStateFlow(GrowthChartGranularity.WEEKLY)

    private val _growthChartStyle = MutableStateFlow(GrowthChartStyle.LINE)

    private val _uiState = MutableStateFlow(
        HomeUiState(overviewAnchorMillis = startOfLocalDay(System.currentTimeMillis()), isLoading = true)
    )
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isSetup = adminRepository.isSetupComplete()
            _startDestination.value = if (isSetup) NavRoutes.LOGIN else NavRoutes.SETUP
            _startDestinationReady.value = true

            if (isSetup) {
                val result = deviceFingerprint.verify()
                if (result is DeviceFingerprint.FingerprintResult.Mismatch) {
                    _deviceMismatch.value = true
                    auditLogRepository.log(
                        AuditLogEntry(
                            id = UUID.randomUUID().toString(),
                            action = "DEVICE_FP_MISMATCH",
                            detail = "{\"stored\":\"${result.stored}\",\"current\":\"${result.current}\"}",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
        viewModelScope.launch {
            _showRootWarning.value = rootDetection.isRooted(context)
        }
        viewModelScope.launch {
            transactionRepository.observeAll().collect { transactions ->
                _uiState.value = _uiState.value.copy(recentTransactions = transactions.take(3))
                refreshDashboard()
            }
        }
    }

    fun dismissRootWarning() { _showRootWarning.value = false }

    fun acknowledgeFingerprintMismatch() { _deviceMismatch.value = false }

    fun setGrowthGranularity(granularity: GrowthChartGranularity) {
        _growthGranularity.value = granularity
        viewModelScope.launch { refreshDashboard() }
    }

    fun setGrowthChartStyle(style: GrowthChartStyle) {
        _growthChartStyle.value = style
        _uiState.value = _uiState.value.copy(growthChartStyle = style)
    }

    private suspend fun refreshDashboard() {
        val anchorMillis = startOfLocalDay(System.currentTimeMillis())
        val granularity = _growthGranularity.value
        val chartStyle = _growthChartStyle.value
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            overviewAnchorMillis = anchorMillis,
            growthGranularity = granularity,
            growthChartStyle = chartStyle
        )

        val daySummaryResult = getDailySummaryUseCase(anchorMillis)

        var growthError: Throwable? = null
        val growthPoints = runCatching {
            buildGrowthSeries(granularity)
        }.getOrElse { e ->
            growthError = e
            emptyList()
        }

        val firstError = daySummaryResult.exceptionOrNull() ?: growthError

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            todaySummary = daySummaryResult.getOrElse { _uiState.value.todaySummary },
            growthPoints = growthPoints,
            errorMessage = firstError?.message
        )
    }

    private suspend fun buildGrowthSeries(granularity: GrowthChartGranularity): List<GrowthPoint> {
        val now = System.currentTimeMillis()
        return when (granularity) {
            GrowthChartGranularity.WEEKLY -> {
                val weekStart = startOfWeekContaining(now)
                (0 until 7).map { dayOffset ->
                    val day = weekStart.clone() as Calendar
                    day.add(Calendar.DATE, dayOffset)
                    normalizeStartOfDay(day)
                    val from = day.timeInMillis
                    val endDay = day.clone() as Calendar
                    endDay.set(Calendar.HOUR_OF_DAY, 23)
                    endDay.set(Calendar.MINUTE, 59)
                    endDay.set(Calendar.SECOND, 59)
                    endDay.set(Calendar.MILLISECOND, 999)
                    val summary = transactionRepository.getDailySummary(from, endDay.timeInMillis)
                    val labelFmt = SimpleDateFormat("EEE", Locale.getDefault())
                    GrowthPoint(label = labelFmt.format(from), revenue = summary.totalRevenue)
                }
            }
            GrowthChartGranularity.MONTHLY -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.set(Calendar.DAY_OF_MONTH, 1)
                normalizeStartOfDay(cal)
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val today = Calendar.getInstance()
                val lastDayThisMonth = today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    today.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
                val maxDay = if (lastDayThisMonth) {
                    today.get(Calendar.DAY_OF_MONTH)
                } else {
                    daysInMonth
                }
                (1..maxDay).map { day ->
                    val dayCal = cal.clone() as Calendar
                    dayCal.set(Calendar.DAY_OF_MONTH, day)
                    normalizeStartOfDay(dayCal)
                    val from = dayCal.timeInMillis
                    val endDay = dayCal.clone() as Calendar
                    endDay.set(Calendar.HOUR_OF_DAY, 23)
                    endDay.set(Calendar.MINUTE, 59)
                    endDay.set(Calendar.SECOND, 59)
                    endDay.set(Calendar.MILLISECOND, 999)
                    val summary = transactionRepository.getDailySummary(from, endDay.timeInMillis)
                    val labelFmt = SimpleDateFormat("d", Locale.getDefault())
                    GrowthPoint(label = labelFmt.format(from), revenue = summary.totalRevenue)
                }
            }
            GrowthChartGranularity.YEARLY -> {
                val year = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
                (0 until 12).map { month ->
                    val c = Calendar.getInstance()
                    c.set(year, month, 1)
                    normalizeStartOfDay(c)
                    val from = c.timeInMillis
                    val endCal = c.clone() as Calendar
                    endCal.add(Calendar.MONTH, 1)
                    val to = endCal.timeInMillis - 1L
                    val summary = transactionRepository.getDailySummary(from, to)
                    val labelFmt = SimpleDateFormat("MMM", Locale.getDefault())
                    GrowthPoint(label = labelFmt.format(from), revenue = summary.totalRevenue)
                }
            }
        }
    }

    private fun startOfWeekContaining(millis: Long): Calendar {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        normalizeStartOfDay(cal)
        val fdw = cal.firstDayOfWeek
        var diff = cal.get(Calendar.DAY_OF_WEEK) - fdw
        if (diff < 0) diff += 7
        cal.add(Calendar.DATE, -diff)
        return cal
    }

    private fun normalizeStartOfDay(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private companion object {
        fun startOfLocalDay(millis: Long): Long {
            val cal = Calendar.getInstance()
            cal.timeInMillis = millis
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
