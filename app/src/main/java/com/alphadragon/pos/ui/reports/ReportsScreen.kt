package com.alphadragon.pos.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.ui.components.AlphaDragonTopBar
import com.alphadragon.pos.ui.theme.BrandRed
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val fmt = NumberFormat.getCurrencyInstance()
    val dateFmt = remember { SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()) }
    val todayMidnight = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }
    val isToday = state.selectedDateMillis >= todayMidnight

    Scaffold(topBar = { AlphaDragonTopBar("Reports", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date navigation row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousDay) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
                }
                Text(
                    text = if (isToday) "Today — ${dateFmt.format(Date(state.selectedDateMillis))}"
                    else dateFmt.format(Date(state.selectedDateMillis)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = viewModel::nextDay,
                    enabled = !isToday
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next day", tint = if (isToday) MaterialTheme.colorScheme.outline else LocalContentColor.current)
                }
            }

            HorizontalDivider()

            if (state.isLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            } else if (state.errorMessage != null) {
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            } else {
                val summary = state.todaySummary
                if (summary != null) {
                    SummaryCard("Transactions", summary.transactionCount.toString())
                    SummaryCard("Total Revenue", fmt.format(summary.totalRevenue))
                    SummaryCard("Tax Collected", fmt.format(summary.totalTax))

                    HorizontalDivider()
                    Text("Breakdown by payment", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SummaryCard("Cash Revenue", fmt.format(summary.cashRevenue))
                    SummaryCard("Card Revenue", fmt.format(summary.cardRevenue))
                } else {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No transactions for this day.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium, color = BrandRed, fontWeight = FontWeight.Bold)
        }
    }
}
