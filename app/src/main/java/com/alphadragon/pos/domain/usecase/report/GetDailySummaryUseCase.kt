package com.alphadragon.pos.domain.usecase.report

import com.alphadragon.pos.domain.model.DailySummary
import com.alphadragon.pos.domain.repository.TransactionRepository
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class GetDailySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    /** Returns today's summary using UTC midnight boundaries. */
    suspend operator fun invoke(dateMillis: Long = System.currentTimeMillis()): Result<DailySummary> =
        runCatching {
            val (start, end) = dayBoundaries(dateMillis)
            transactionRepository.getDailySummary(start, end)
        }

    private fun dayBoundaries(millis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return start to cal.timeInMillis - 1
    }
}
