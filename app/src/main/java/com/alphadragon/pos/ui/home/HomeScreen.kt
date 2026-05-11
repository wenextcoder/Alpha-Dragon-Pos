package com.alphadragon.pos.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alphadragon.pos.domain.model.DailySummary
import com.alphadragon.pos.domain.model.Transaction
import com.alphadragon.pos.domain.model.TransactionStatus
import com.alphadragon.pos.ui.currency.rememberCurrencyFormatter
import com.alphadragon.pos.ui.theme.BrandRed
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSale: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToMerchant: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToReports: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val showRootWarning by viewModel.showRootWarning.collectAsState()
    val deviceMismatch by viewModel.deviceMismatch.collectAsState()
    val state by viewModel.uiState.collectAsState()

    if (deviceMismatch) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Security Alert") },
            text = { Text("This device's hardware ID has changed since the POS was installed. This may indicate the app was moved to a different device. Proceed with caution — all data remains encrypted and this event has been logged.") },
            confirmButton = {
                TextButton(onClick = { viewModel.acknowledgeFingerprintMismatch() }) {
                    Text("I understand, continue")
                }
            }
        )
    }

    if (showRootWarning) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Security Warning") },
            text = { Text("This device appears to be rooted. This may compromise the security of your POS data. You may continue, but this event has been logged.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissRootWarning() }) {
                    Text("I Understand, Continue")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                DashboardHeader()
            }
            item {
                TodayOverview(
                    summary = state.todaySummary,
                    isLoading = state.isLoading,
                    overviewDateMillis = state.overviewAnchorMillis
                )
            }
            item {
                QuickActions(
                    onNavigateToSale = onNavigateToSale,
                    onNavigateToAddProduct = onNavigateToAddProduct,
                    onNavigateToShop = onNavigateToShop,
                    onNavigateToCategories = onNavigateToCategories,
                    onNavigateToCustomers = onNavigateToCustomers,
                    onNavigateToImport = onNavigateToImport,
                    onNavigateToExport = onNavigateToExport,
                    onNavigateToMerchant = onNavigateToMerchant,
                    onNavigateToReports = onNavigateToReports
                )
            }
            item {
                GrowthChartCard(
                    points = state.growthPoints,
                    granularity = state.growthGranularity,
                    chartStyle = state.growthChartStyle,
                    onGranularityChange = viewModel::setGrowthGranularity,
                    onChartStyleChange = viewModel::setGrowthChartStyle
                )
            }
            item {
                RecentTransactionsCard(
                    transactions = state.recentTransactions,
                    onNavigateToTransactions = onNavigateToTransactions
                )
            }
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TodayOverview(
    summary: DailySummary,
    isLoading: Boolean,
    overviewDateMillis: Long
) {
    val currency = rememberCurrencyFormatter()
    val dateFmt = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("Overview", style = MaterialTheme.typography.titleMedium)
            Text(
                "Today · ${dateFmt.format(Date(overviewDateMillis))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewCard(
                title = "Revenue",
                value = if (isLoading) "..." else currency.format(summary.totalRevenue),
                icon = Icons.Default.Payments,
                modifier = Modifier.weight(1f),
                isPrimary = true
            )
            OverviewCard(
                title = "Transactions",
                value = if (isLoading) "..." else summary.transactionCount.toString(),
                icon = Icons.Default.Receipt,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OverviewCard(
                title = "Cash",
                value = if (isLoading) "..." else currency.format(summary.cashRevenue),
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f)
            )
            OverviewCard(
                title = "Card",
                value = if (isLoading) "..." else currency.format(summary.cardRevenue),
                icon = Icons.Default.CreditCard,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false
) {
    Card(
        modifier = modifier.height(112.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) BrandRed else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickActions(
    onNavigateToSale: () -> Unit,
    onNavigateToAddProduct: () -> Unit,
    onNavigateToShop: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToCustomers: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToMerchant: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    var layout by remember { mutableStateOf(QuickActionLayout.IconTextGrid) }
    val actions = listOf(
        QuickAction("Sale", Icons.Default.ShoppingCart, onNavigateToSale, true),
        QuickAction("Add Product", Icons.Default.AddBox, onNavigateToAddProduct),
        QuickAction("Reports", Icons.Default.BarChart, onNavigateToReports),
        QuickAction("Customers", Icons.Default.Groups, onNavigateToCustomers),
        QuickAction("Shop", Icons.Default.Store, onNavigateToShop),
        QuickAction("Category", Icons.Default.Category, onNavigateToCategories),
        QuickAction("Import", Icons.Default.FileDownload, onNavigateToImport),
        QuickAction("Export", Icons.Default.FileUpload, onNavigateToExport),
        QuickAction("Merchant", Icons.Default.Business, onNavigateToMerchant)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
            IconButton(
                onClick = {
                    layout = if (layout == QuickActionLayout.IconTextGrid) {
                        QuickActionLayout.IconGrid
                    } else {
                        QuickActionLayout.IconTextGrid
                    }
                }
            ) {
                Icon(Icons.Default.Dashboard, contentDescription = "Change quick action layout")
            }
        }

        val gridSpacing = 10.dp
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cols = when (layout) {
                QuickActionLayout.IconTextGrid ->
                    (maxWidth / 100.dp).toInt().coerceIn(2, 5)
                QuickActionLayout.IconGrid ->
                    (maxWidth / 64.dp).toInt().coerceIn(3, 8)
            }
            val cellHeight = when (layout) {
                QuickActionLayout.IconTextGrid -> 80.dp
                QuickActionLayout.IconGrid -> 60.dp
            }
            val showText = layout == QuickActionLayout.IconTextGrid
            val iconSize = when (layout) {
                QuickActionLayout.IconTextGrid -> 26.dp
                QuickActionLayout.IconGrid -> 24.dp
            }
            val rowCount = (actions.size + cols - 1) / cols
            val gridHeight = cellHeight * rowCount + gridSpacing * (rowCount - 1).coerceAtLeast(0)

            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(gridHeight),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(gridSpacing),
                verticalArrangement = Arrangement.spacedBy(gridSpacing),
            ) {
                items(actions.size, key = { actions[it].title }) { index ->
                    val action = actions[index]
                    QuickActionCard(
                        title = action.title,
                        icon = action.icon,
                        onClick = action.onClick,
                        modifier = Modifier.fillMaxWidth(),
                        isPrimary = action.isPrimary,
                        showText = showText,
                        compactHeight = cellHeight,
                        iconSize = iconSize,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    showText: Boolean = true,
    compactHeight: Dp = 80.dp,
    iconSize: Dp = 26.dp,
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(compactHeight),
        colors = CardDefaults.cardColors(
            containerColor = if (isPrimary) BrandRed
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = if (showText) 8.dp else 6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showText) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val isPrimary: Boolean = false
)

private enum class QuickActionLayout {
    IconTextGrid,
    IconGrid
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrowthChartCard(
    points: List<GrowthPoint>,
    granularity: GrowthChartGranularity,
    chartStyle: GrowthChartStyle,
    onGranularityChange: (GrowthChartGranularity) -> Unit,
    onChartStyleChange: (GrowthChartStyle) -> Unit
) {
    val monthYearFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val yearFmt = remember { SimpleDateFormat("yyyy", Locale.getDefault()) }

    val subtitle = when (granularity) {
        GrowthChartGranularity.WEEKLY -> "This week (by day)"
        GrowthChartGranularity.MONTHLY ->
            "This month — ${monthYearFmt.format(Date())} (daily through today)"
        GrowthChartGranularity.YEARLY ->
            "${yearFmt.format(Date())} (by month)"
    }

    var chartMenuOpen by remember { mutableStateOf(false) }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Growth", style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = { chartMenuOpen = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "Chart style")
                    }
                    DropdownMenu(
                        expanded = chartMenuOpen,
                        onDismissRequest = { chartMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Line") },
                            onClick = {
                                onChartStyleChange(GrowthChartStyle.LINE)
                                chartMenuOpen = false
                            },
                            leadingIcon = { Icon(Icons.Default.TrendingUp, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Column") },
                            onClick = {
                                onChartStyleChange(GrowthChartStyle.COLUMN)
                                chartMenuOpen = false
                            },
                            leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = granularity == GrowthChartGranularity.WEEKLY,
                    onClick = { onGranularityChange(GrowthChartGranularity.WEEKLY) },
                    label = { Text("Week") }
                )
                FilterChip(
                    selected = granularity == GrowthChartGranularity.MONTHLY,
                    onClick = { onGranularityChange(GrowthChartGranularity.MONTHLY) },
                    label = { Text("Month") }
                )
                FilterChip(
                    selected = granularity == GrowthChartGranularity.YEARLY,
                    onClick = { onGranularityChange(GrowthChartGranularity.YEARLY) },
                    label = { Text("Year") }
                )
            }

            if (points.isEmpty()) {
                Text(
                    text = "No sales data yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                GrowthChartBody(points = points, style = chartStyle)
            }
        }
    }
}

@Composable
private fun GrowthChartBody(points: List<GrowthPoint>, style: GrowthChartStyle) {
    if (points.isEmpty()) return

    val primary = MaterialTheme.colorScheme.primary
    val primaryFill = primary.copy(alpha = 0.12f)
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    val maxRevenue = remember(points) {
        points.maxOfOrNull { it.revenue }?.takeIf { it > 0.0 } ?: 1.0
    }

    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val labelH = with(density) { 22.dp.toPx() }
        val topPad = with(density) { 12.dp.toPx() }
        val sidePad = with(density) { 6.dp.toPx() }
        val chartBottom = size.height - labelH
        val chartTop = topPad
        val chartH = chartBottom - chartTop
        val chartW = size.width - sidePad * 2
        val n = points.size
        if (n == 0) return@Canvas

        // baseline
        drawLine(
            color = axisColor,
            start = Offset(sidePad, chartBottom),
            end = Offset(size.width - sidePad, chartBottom),
            strokeWidth = with(density) { 1.dp.toPx() }
        )

        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            textSize = with(density) { 9.5.sp.toPx() }
            color = labelColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val labelY = size.height - with(density) { 3.dp.toPx() }

        when (style) {
            GrowthChartStyle.LINE -> {
                val step = if (n > 1) chartW / (n - 1) else 0f
                val offsets = points.mapIndexed { i, p ->
                    val x = sidePad + if (n > 1) i * step else chartW / 2f
                    val y = (chartBottom - (p.revenue / maxRevenue).toFloat() * chartH)
                        .coerceIn(chartTop, chartBottom)
                    Offset(x, y)
                }

                // semi-transparent area fill
                val areaPath = Path().apply {
                    moveTo(offsets.first().x, chartBottom)
                    offsets.forEach { lineTo(it.x, it.y) }
                    lineTo(offsets.last().x, chartBottom)
                    close()
                }
                drawPath(areaPath, color = primaryFill)

                // line
                val linePath = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = linePath,
                    color = primary,
                    style = Stroke(
                        width = with(density) { 2.dp.toPx() },
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // dots + labels
                offsets.forEachIndexed { i, o ->
                    drawCircle(color = primary, radius = with(density) { 3.5.dp.toPx() }, center = o)
                    drawIntoCanvas { c ->
                        c.nativeCanvas.drawText(points[i].label, o.x, labelY, labelPaint)
                    }
                }
            }

            GrowthChartStyle.COLUMN -> {
                val slot = chartW / n
                val barW = slot * 0.58f
                val cornerR = with(density) { 3.dp.toPx() }

                points.forEachIndexed { i, p ->
                    val barH = ((p.revenue / maxRevenue).toFloat() * chartH).coerceAtLeast(0f)
                    val bx = sidePad + i * slot + (slot - barW) / 2f
                    val centerX = bx + barW / 2f

                    if (barH >= cornerR * 2) {
                        drawRoundRect(
                            color = primary,
                            topLeft = Offset(bx, chartBottom - barH),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(cornerR, cornerR)
                        )
                    } else if (barH > 0f) {
                        drawRect(
                            color = primary,
                            topLeft = Offset(bx, chartBottom - barH),
                            size = Size(barW, barH)
                        )
                    } else {
                        // zero-value tick
                        drawRect(
                            color = primary.copy(alpha = 0.25f),
                            topLeft = Offset(bx, chartBottom - with(density) { 2.dp.toPx() }),
                            size = Size(barW, with(density) { 2.dp.toPx() })
                        )
                    }

                    drawIntoCanvas { c ->
                        c.nativeCanvas.drawText(p.label, centerX, labelY, labelPaint)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    transactions: List<Transaction>,
    onNavigateToTransactions: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Transactions", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNavigateToTransactions) { Text("View all") }
            }

            if (transactions.isEmpty()) {
                Text(
                    text = "No transactions yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                transactions.forEach { transaction ->
                    TransactionPreviewRow(
                        transaction = transaction,
                        onClick = onNavigateToTransactions
                    )
                    if (transaction != transactions.last()) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TransactionPreviewRow(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val currency = rememberCurrencyFormatter()
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    ListItem(
        headlineContent = {
            Text(
                text = currency.format(transaction.total),
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(dateFormat.format(Date(transaction.timestamp)))
        },
        trailingContent = {
            Text(
                text = transaction.status.value.replace('_', ' ').replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = when (transaction.status) {
                    TransactionStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                    TransactionStatus.VOIDED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
