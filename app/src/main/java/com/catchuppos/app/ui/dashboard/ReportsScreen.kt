package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.data.*
import com.catchuppos.app.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// ════════════════════════════════════════════════════════════════════
// Sub-tab definitions
// ════════════════════════════════════════════════════════════════════

enum class ReportSubTab(val label: String) {
    OVERVIEW("Overview"),
    SALES("Sales"),
    ORDERS("Orders"),
    PRODUCTS("Products"),
    EMPLOYEES("Employees"),
    TAXES("Taxes")
}

// ════════════════════════════════════════════════════════════════════
// Data models for KPIs and comparisons
// ════════════════════════════════════════════════════════════════════

data class KPIData(
    val totalSales: Double = 0.0,
    val totalOrders: Int = 0,
    val avgOrderValue: Double = 0.0,
    val totalItemsSold: Int = 0,
    val grossProfit: Double = 0.0,
    val salesChange: Double = 0.0,
    val ordersChange: Double = 0.0,
    val avgOrderChange: Double = 0.0,
    val itemsChange: Double = 0.0,
    val profitChange: Double = 0.0
)

// ════════════════════════════════════════════════════════════════════
// Small Metric Card (shared by Sales, Orders, Products, etc.)
// ════════════════════════════════════════════════════════════════════

@Composable
fun SmallMetricCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0D0D0D)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Daily Sales Bar Chart (Canvas-based)
// ════════════════════════════════════════════════════════════════════

@Composable
fun DailySalesBarChart(dailySales: List<DailySalesSummary>, modifier: Modifier = Modifier) {
    val maxVal = maxOf(dailySales.maxOfOrNull { it.total } ?: 1.0, 1.0)
    val lineColor = OrangeAccent
    val gridColor = DarkBorder
    val textColor = TextGray

    val yAxisPaint = remember { android.graphics.Paint().apply { textSize = 18f; textAlign = android.graphics.Paint.Align.RIGHT } }
    val xAxisPaint = remember { android.graphics.Paint().apply { textSize = 16f; textAlign = android.graphics.Paint.Align.CENTER } }

    Canvas(modifier = modifier.padding(start = 48.dp, bottom = 28.dp, end = 8.dp, top = 8.dp)) {
        if (dailySales.isEmpty()) return@Canvas
        val chartWidth = size.width
        val chartHeight = size.height
        val baseline = chartHeight

        // Draw Y-axis grid lines and labels
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = baseline - (chartHeight * i / gridSteps)
            drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), strokeWidth = 0.5f)
            val labelValue = (maxVal * i / gridSteps)
            yAxisPaint.color = textColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "₱${String.format(Locale.US, "%,.0f", labelValue)}", -8f, y + 5f, yAxisPaint
            )
        }

        // Calculate points
        val stepX = if (dailySales.size > 1) chartWidth / (dailySales.size - 1) else chartWidth / 2f
        val points = dailySales.mapIndexed { i, day ->
            val x = i * stepX
            val y = baseline - (chartHeight * (day.total / maxVal)).toFloat()
            Offset(x, y)
        }

        // Draw filled area under the line
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, baseline)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, baseline)
                close()
            }
            drawPath(fillPath, lineColor.copy(alpha = 0.1f))
        }

        // Draw the line
        if (points.size >= 2) {
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(linePath, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Draw dots
        points.forEach { pt ->
            drawCircle(lineColor, radius = 4f, center = pt)
            drawCircle(Color(0xFF0D0D0D), radius = 2f, center = pt)
        }

        // Draw X-axis labels
        dailySales.forEachIndexed { i, day ->
            val x = i * stepX
            val label = if (day.dayOffset > 0) {
                SimpleDateFormat("MM/dd", Locale.US).format(Date(day.dayOffset * 86400000L))
            } else ""
            xAxisPaint.color = textColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                label, x, baseline + 18f, xAxisPaint
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Daily Orders Bar Chart (Canvas-based)
// ════════════════════════════════════════════════════════════════════

@Composable
fun DailyOrdersBarChart(dailySales: List<DailySalesSummary>, modifier: Modifier = Modifier) {
    val maxVal = maxOf(dailySales.maxOfOrNull { it.orderCount } ?: 1, 1)
    val lineColor = Color(0xFF2196F3)
    val gridColor = DarkBorder
    val textColor = TextGray

    val yAxisPaint = remember { android.graphics.Paint().apply { textSize = 18f; textAlign = android.graphics.Paint.Align.RIGHT } }
    val xAxisPaint = remember { android.graphics.Paint().apply { textSize = 16f; textAlign = android.graphics.Paint.Align.CENTER } }

    Canvas(modifier = modifier.padding(start = 48.dp, bottom = 28.dp, end = 8.dp, top = 8.dp)) {
        if (dailySales.isEmpty()) return@Canvas
        val chartWidth = size.width
        val chartHeight = size.height
        val baseline = chartHeight

        // Draw Y-axis grid lines and labels
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = baseline - (chartHeight * i / gridSteps)
            drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), strokeWidth = 0.5f)
            val labelValue = (maxVal.toFloat() * i / gridSteps).toInt()
            yAxisPaint.color = textColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "$labelValue", -8f, y + 5f, yAxisPaint
            )
        }

        // Calculate points
        val stepX = if (dailySales.size > 1) chartWidth / (dailySales.size - 1) else chartWidth / 2f
        val points = dailySales.mapIndexed { i, day ->
            val x = i * stepX
            val y = baseline - (chartHeight * (day.orderCount.toFloat() / maxVal)).toFloat()
            Offset(x, y)
        }

        // Draw filled area under the line
        if (points.size >= 2) {
            val fillPath = Path().apply {
                moveTo(points.first().x, baseline)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(points.last().x, baseline)
                close()
            }
            drawPath(fillPath, lineColor.copy(alpha = 0.1f))
        }

        // Draw the line
        if (points.size >= 2) {
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(linePath, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Draw dots
        points.forEach { pt ->
            drawCircle(lineColor, radius = 4f, center = pt)
            drawCircle(Color(0xFF0D0D0D), radius = 2f, center = pt)
        }

        // Draw X-axis labels
        dailySales.forEachIndexed { i, day ->
            val x = i * stepX
            val label = if (day.dayOffset > 0) {
                SimpleDateFormat("MM/dd", Locale.US).format(Date(day.dayOffset * 86400000L))
            } else ""
            xAxisPaint.color = textColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                label, x, baseline + 18f, xAxisPaint
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Expense Donut Chart (Canvas-based)
// ════════════════════════════════════════════════════════════════════

private val expenseCategoryColors = listOf(
    Color(0xFFFF6600),
    Color(0xFFF48FB1),
    Color(0xFFFF8F00),
    Color(0xFF9C27B0),
    Color(0xFF2196F3)
)

@Composable
fun ExpenseDonutChart(categoryTotals: ExpenseCategoryTotals?, modifier: Modifier = Modifier) {
    val cats = listOf(
        "Syrups" to (categoryTotals?.syrups ?: 0.0),
        "Sauce" to (categoryTotals?.sauce ?: 0.0),
        "Milk" to (categoryTotals?.milk ?: 0.0),
        "Ice" to (categoryTotals?.ice ?: 0.0),
        "Others" to (categoryTotals?.others ?: 0.0)
    ).filter { it.second > 0 }
    val total = cats.sumOf { it.second }

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                if (total <= 0) return@Canvas
                val strokeW = 34f
                val radius = (size.minDimension - strokeW) / 2f
                val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
                var startAngle = -90f
                cats.forEachIndexed { index, (_, value) ->
                    val sweep = (value / total * 360f).toFloat()
                    drawArc(
                        color = expenseCategoryColors[index % expenseCategoryColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("₱${String.format(Locale.US, "%,.2f", total)}", style = MaterialTheme.typography.titleSmall, color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Total", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            cats.forEachIndexed { index, (label, value) ->
                val pct = if (total > 0) (value / total * 100) else 0.0
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(expenseCategoryColors[index % expenseCategoryColors.size]))
                    Text(label, style = MaterialTheme.typography.bodySmall, color = TextWhite, modifier = Modifier.weight(1f))
                    Text("₱${String.format(Locale.US, "%,.2f", value)}", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("(${String.format(Locale.US, "%.1f", pct)}%)", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Expense Line Chart (Canvas-based)
// ════════════════════════════════════════════════════════════════════

@Composable
fun ExpenseLineChart(expensesList: List<ExpenseEntity>, modifier: Modifier = Modifier) {
    val dailyTotals = expensesList.groupBy { it.date / 86400000L }
        .mapValues { (_, list) -> list.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others } }
    val maxVal = maxOf(dailyTotals.values.maxOrNull() ?: 1.0, 1.0)
    val lineColor = MutedRed

    Canvas(modifier = modifier.padding(start = 8.dp, bottom = 24.dp, end = 8.dp, top = 8.dp)) {
        if (dailyTotals.size < 2) return@Canvas
        val sorted = dailyTotals.entries.sortedBy { it.key }
        val stepX = size.width / (sorted.size - 1)
        val points = sorted.map { (_, v) -> size.height - (size.height * (v / maxVal)).toFloat() }

        val path = Path().apply {
            moveTo(0f, points[0])
            for (i in 1 until points.size) lineTo(i * stepX, points[i])
        }
        drawPath(path, lineColor, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))

        sorted.forEachIndexed { i, (dayOffset, _) ->
            val x = i * stepX
            val label = SimpleDateFormat("MM/dd", Locale.US).format(Date(dayOffset * 86400000L))
            drawContext.canvas.nativeCanvas.drawText(
                label, x, size.height + 14f,
                android.graphics.Paint().apply { color = TextGray.hashCode(); textSize = 16f; textAlign = android.graphics.Paint.Align.CENTER }
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Expenses KPI Card
// ════════════════════════════════════════════════════════════════════

@Composable
fun ExpensesKPICard(label: String, value: String, valueColor: Color, trend: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = Color(0xFF0D0D0D)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(trend, style = MaterialTheme.typography.labelSmall, color = StatusGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Main ReportsScreen Composable
// ════════════════════════════════════════════════════════════════════

@Composable
fun ReportsScreen(
    onNavigate: (NavItem) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    val userRepo = app.userRepository

    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }

    var activeSubTab by remember { mutableStateOf(ReportSubTab.OVERVIEW) }
    var selectedDateRange by remember { mutableStateOf("Today") }
    var selectedDateLabel by remember { mutableStateOf("Today, ${dateFormat.format(Date())}") }
    var showDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf(0L) }
    var customEndDate by remember { mutableStateOf(0L) }

    // Report data states (Overview)
    var kpiData by remember { mutableStateOf(KPIData()) }
    var hourlySales by remember { mutableStateOf<List<HourlySalesSummary>>(emptyList()) }
    var categorySales by remember { mutableStateOf<List<CategorySalesSummary>>(emptyList()) }
    var topProducts by remember { mutableStateOf<List<TopSellingProduct>>(emptyList()) }
    var paymentMethods by remember { mutableStateOf<List<PaymentMethodSales>>(emptyList()) }
    var recentTransactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }

    // State for other tabs
    var dailySales by remember { mutableStateOf<List<DailySalesSummary>>(emptyList()) }
    var orderStatusCounts by remember { mutableStateOf<List<OrderStatusSummary>>(emptyList()) }
    var allProducts by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var cashierPerformance by remember { mutableStateOf<List<CashierSummary>>(emptyList()) }
    var totalProducts by remember { mutableIntStateOf(0) }
    var totalUsers by remember { mutableIntStateOf(0) }
    var allTransactionsForPeriod by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }


    // Load all report data
    LaunchedEffect(selectedDateRange, customStartDate, customEndDate) {
        val (startTime, endTime) = if (selectedDateRange == "Custom Range") {
            customStartDate to customEndDate
        } else {
            computeDateRange(selectedDateRange)
        }
        loadReportData(
            repository = repository,
            startTime = startTime,
            endTime = endTime,
            onKpiData = { kpiData = it },
            onHourlySales = { hourlySales = it },
            onCategorySales = { categorySales = it },
            onTopProducts = { topProducts = it },
            onPaymentMethods = { paymentMethods = it },
            onRecentTransactions = { recentTransactions = it }
        )
        dailySales = repository.getDailySales(startTime, endTime)
        orderStatusCounts = repository.getOrderStatusCounts(startTime, endTime)
        allProducts = repository.allProductsOnce()
        cashierPerformance = repository.getCashierPerformance(startTime, endTime)
        totalProducts = repository.getProductCount()
        totalUsers = userRepo.getUserCount()
        allTransactionsForPeriod = repository.getTransactionsByDateRange(startTime, endTime)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        // ── Title ──
        Text(
            text = "Reports",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(20.dp))

        // ── Sub-Tab Navigation + Filters ──
        SubTabAndFilterRow(
            activeTab = activeSubTab,
            onTabSelected = { activeSubTab = it },
            selectedDateLabel = selectedDateLabel,
            onDateClick = { showDatePicker = true }
        )
        Spacer(modifier = Modifier.height(20.dp))

        // ── Content based on active tab ──
        when (activeSubTab) {
            ReportSubTab.OVERVIEW -> OverviewContent(
                kpiData = kpiData,
                hourlySales = hourlySales,
                categorySales = categorySales,
                topProducts = topProducts,
                paymentMethods = paymentMethods,
                recentTransactions = recentTransactions,
                onNavigate = onNavigate,
                onViewFullReport = { activeSubTab = ReportSubTab.SALES }
            )
            ReportSubTab.SALES -> SalesTabContent(
                dailySales = dailySales,
                paymentMethods = paymentMethods,
                hourlySales = hourlySales,
                kpiData = kpiData
            )
            ReportSubTab.ORDERS -> OrdersTabContent(
                orderStatusCounts = orderStatusCounts,
                dailySales = dailySales,
                allTransactions = allTransactionsForPeriod,
                kpiData = kpiData
            )
            ReportSubTab.PRODUCTS -> ProductsTabContent(
                allProducts = allProducts,
                totalProducts = totalProducts,
                topProducts = topProducts,
                categorySales = categorySales
            )
            ReportSubTab.EMPLOYEES -> EmployeesTabContent(
                cashierPerformance = cashierPerformance,
                totalUsers = totalUsers,
                allTransactions = allTransactionsForPeriod
            )
            ReportSubTab.TAXES -> TaxesTabContent(kpiData = kpiData)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Footer Note ──
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "All reports are real-time and based on current data.",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Date picker dialog (calendar-based)
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onApply = { startMillis, endMillis ->
                if (startMillis != null && endMillis != null) {
                    // Convert UTC midnight to local time day boundaries
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = startMillis
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startDay = cal.timeInMillis
                    cal.timeInMillis = endMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val endDay = cal.timeInMillis
                    customStartDate = startDay
                    customEndDate = endDay
                    selectedDateRange = "Custom Range"
                    selectedDateLabel = "${SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date(startDay))} - ${SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date(endDay))}"
                }
                showDatePicker = false
            }
        )
    }
}

// ════════════════════════════════════════════════════════════════════
// Date range helper
// ════════════════════════════════════════════════════════════════════

private fun computeDateRange(option: String): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val todayEnd = now

    return when (option) {
        "Today" -> todayStart to todayEnd
        "Yesterday" -> {
            cal.add(Calendar.DAY_OF_MONTH, -1)
            val start = cal.timeInMillis
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            start to end
        }
        "This Week" -> {
            cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
            val start = cal.timeInMillis
            start to now
        }
        "This Month" -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val start = cal.timeInMillis
            start to now
        }
        "Last Month" -> {
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.add(Calendar.MONTH, -1)
            val start = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
            val end = cal.timeInMillis
            start to end
        }
        "All Time" -> 0L to now
        else -> todayStart to todayEnd
    }
}

// ════════════════════════════════════════════════════════════════════
// Data loading helper
// ════════════════════════════════════════════════════════════════════

private suspend fun loadReportData(
    repository: ProductRepository,
    startTime: Long,
    endTime: Long,
    onKpiData: (KPIData) -> Unit,
    onHourlySales: (List<HourlySalesSummary>) -> Unit,
    onCategorySales: (List<CategorySalesSummary>) -> Unit,
    onTopProducts: (List<TopSellingProduct>) -> Unit,
    onPaymentMethods: (List<PaymentMethodSales>) -> Unit,
    onRecentTransactions: (List<TransactionEntity>) -> Unit
) {
    val now = System.currentTimeMillis()
    val todayCal = Calendar.getInstance().apply { timeInMillis = now }
    todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0)
    todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
    val todayStart = todayCal.timeInMillis

    val yesterdayCal = Calendar.getInstance().apply { timeInMillis = todayStart }
    yesterdayCal.add(Calendar.DAY_OF_MONTH, -1)
    val yesterdayStart = yesterdayCal.timeInMillis
    yesterdayCal.set(Calendar.HOUR_OF_DAY, 23); yesterdayCal.set(Calendar.MINUTE, 59)
    yesterdayCal.set(Calendar.SECOND, 59); yesterdayCal.set(Calendar.MILLISECOND, 999)
    val yesterdayEnd = yesterdayCal.timeInMillis

    val currentSales = repository.getSalesTotalByDateRange(startTime, endTime)
    val currentOrders = repository.getOrdersCountByDateRange(startTime, endTime)
    val currentItems = repository.getItemsSoldByDateRange(startTime, endTime)
    val currentAvg = if (currentOrders > 0) currentSales / currentOrders else 0.0

    val yesterdaySales = repository.getSalesTotalByDateRange(yesterdayStart, yesterdayEnd)
    val yesterdayOrders = repository.getOrdersCountByDateRange(yesterdayStart, yesterdayEnd)
    val yesterdayItems = repository.getItemsSoldByDateRange(yesterdayStart, yesterdayEnd)
    val yesterdayAvg = if (yesterdayOrders > 0) yesterdaySales / yesterdayOrders else 0.0

    fun calcChange(current: Double, previous: Double): Double =
        if (previous > 0) ((current - previous) / previous) * 100.0 else 0.0

    val salesChange = calcChange(currentSales, yesterdaySales)
    val ordersChange = calcChange(currentOrders.toDouble(), yesterdayOrders.toDouble())
    val avgChange = calcChange(currentAvg, yesterdayAvg)
    val itemsChange = calcChange(currentItems.toDouble(), yesterdayItems.toDouble())
    val profitChange = calcChange(currentSales * 0.5, yesterdaySales * 0.5)

    onKpiData(KPIData(
        totalSales = currentSales,
        totalOrders = currentOrders,
        avgOrderValue = currentAvg,
        totalItemsSold = currentItems,
        grossProfit = currentSales * 0.5,
        salesChange = salesChange,
        ordersChange = ordersChange,
        avgOrderChange = avgChange,
        itemsChange = itemsChange,
        profitChange = profitChange
    ))

    onHourlySales(repository.getHourlySales(startTime, endTime))
    onCategorySales(repository.getCategorySalesSummary(startTime, endTime))
    onTopProducts(repository.getTopSellingProducts(startTime, endTime))
    onPaymentMethods(repository.getSalesByPaymentMethod(startTime, endTime))

    val allTxns = repository.getTransactionsByDateRange(startTime, endTime)
    onRecentTransactions(allTxns.take(5))
}


// ════════════════════════════════════════════════════════════════════
// Sub-tab & Filter Row
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SubTabAndFilterRow(
    activeTab: ReportSubTab,
    onTabSelected: (ReportSubTab) -> Unit,
    selectedDateLabel: String,
    onDateClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sub-tabs
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ReportSubTab.entries.forEach { tab ->
                val isActive = tab == activeTab
                Text(
                    text = tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    color = if (isActive) TextWhite else TextMuted,
                    modifier = Modifier
                        .clickable { onTabSelected(tab) }
                        .then(
                            if (isActive) Modifier.background(
                                OrangeAccent.copy(alpha = 0.12f),
                                RoundedCornerShape(8.dp)
                            ) else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // Filters
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onDateClick,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = DarkCard,
                    contentColor = TextWhite
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(selectedDateLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
            }

            OutlinedButton(
                onClick = { /* Filter logic */ },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextMuted),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Filter", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Overview Content (main view)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun OverviewContent(
    kpiData: KPIData,
    hourlySales: List<HourlySalesSummary>,
    categorySales: List<CategorySalesSummary>,
    topProducts: List<TopSellingProduct>,
    paymentMethods: List<PaymentMethodSales>,
    recentTransactions: List<TransactionEntity>,
    onNavigate: (NavItem) -> Unit = {},
    onViewFullReport: () -> Unit = {}
) {
    // ── KPI Cards Row ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KPICard(
            icon = Icons.Default.AttachMoney,
            iconTint = StatusGreen,
            value = "₱${String.format(Locale.US, "%,.2f", kpiData.totalSales)}",
            label = "Total Sales",
            change = kpiData.salesChange,
            modifier = Modifier.weight(1f)
        )
        KPICard(
            icon = Icons.Default.ShoppingBag,
            iconTint = OrangeAccent,
            value = "${kpiData.totalOrders}",
            label = "Total Orders",
            change = kpiData.ordersChange,
            modifier = Modifier.weight(1f)
        )
        KPICard(
            icon = Icons.AutoMirrored.Filled.Assignment,
            iconTint = Color(0xFF2196F3),
            value = "₱${String.format(Locale.US, "%,.2f", kpiData.avgOrderValue)}",
            label = "Avg Order Value",
            change = kpiData.avgOrderChange,
            modifier = Modifier.weight(1f)
        )
        KPICard(
            icon = Icons.Default.ShoppingCart,
            iconTint = Color(0xFF9C27B0),
            value = "${kpiData.totalItemsSold}",
            label = "Total Items Sold",
            change = kpiData.itemsChange,
            modifier = Modifier.weight(1f)
        )
        KPICard(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            iconTint = Color(0xFFFFC107),
            value = "₱${String.format(Locale.US, "%,.2f", kpiData.grossProfit)}",
            label = "Gross Profit",
            change = kpiData.profitChange,
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── Middle row: Sales Overview (line chart) + Sales By Category (donut) ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sales Overview Line Chart (flex 2)
        Card(
            modifier = Modifier.weight(2f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SALES OVERVIEW", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("By Hour", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SalesLineChart(hourlySales = hourlySales, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        }

        // Sales By Category Donut Chart (flex 1)
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("SALES BY CATEGORY", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                CategoryDonutChart(categorySales = categorySales, modifier = Modifier.fillMaxWidth().height(220.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // ── Bottom 3-column grid: Top Products, Payment Methods, Recent Transactions ──
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Selling Products
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TOP SELLING PRODUCTS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                TopProductsTable(products = topProducts)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onNavigate(NavItem.PRODUCTS) }) {
                    Text("View all products >", color = OrangeAccent, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Payment Methods
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("PAYMENT METHODS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                PaymentDonutChart(paymentMethods = paymentMethods, modifier = Modifier.fillMaxWidth().height(180.dp))
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onViewFullReport) {
                    Text("View full report >", color = OrangeAccent, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // Recent Transactions
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENT TRANSACTIONS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    TextButton(onClick = { onNavigate(NavItem.TRANSACTIONS) }) {
                        Text("View All", color = OrangeAccent, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                RecentTransactionsTable(transactions = recentTransactions)
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { onNavigate(NavItem.TRANSACTIONS) }) {
                    Text("View all transactions >", color = OrangeAccent, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// KPI Card
// ════════════════════════════════════════════════════════════════════

@Composable
private fun KPICard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    change: Double,
    modifier: Modifier = Modifier
) {
    val isPositive = change >= 0
    val changeColor = if (isPositive) StatusGreen else MutedRed
    val arrow = if (isPositive) "\u25B2 " else "\u25BC "

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0D0D0D)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(value, style = MaterialTheme.typography.titleMedium, color = TextWhite, fontWeight = FontWeight.Bold)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$arrow${String.format(Locale.US, "%.1f", kotlin.math.abs(change))}% vs Yesterday",
                style = MaterialTheme.typography.labelSmall,
                color = changeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Sales Line Chart (Canvas-based)
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SalesLineChart(
    hourlySales: List<HourlySalesSummary>,
    modifier: Modifier = Modifier
) {
    val lineColor = OrangeAccent
    val gridColor = DarkBorder
    val textColor = TextGray

    val hourLabels = listOf("12 AM", "4 AM", "8 AM", "12 PM", "4 PM", "8 PM", "11 PM")
    val hourPositions = listOf(0, 4, 8, 12, 16, 20, 23)
    val salesMap = hourlySales.associate { it.hour to it.amount }
    val maxSales = maxOf(hourlySales.maxOfOrNull { it.amount } ?: 1.0, 1.0)

    val yAxisPaint = remember { android.graphics.Paint().apply { textSize = 22f; textAlign = android.graphics.Paint.Align.LEFT } }
    val xAxisPaint = remember { android.graphics.Paint().apply { textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER } }

    Canvas(modifier = modifier.padding(start = 40.dp, bottom = 28.dp, end = 12.dp, top = 8.dp)) {
        val chartWidth = size.width
        val chartHeight = size.height
        val baseline = chartHeight

        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = baseline - (chartHeight * i / gridSteps)
            drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), strokeWidth = 0.5f)
            val labelValue = (maxSales * i / gridSteps).toInt()
            yAxisPaint.color = textColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "₱${labelValue / 1000}K", -36f, y + 4f, yAxisPaint
            )
        }

        val points = hourPositions.map { hour ->
            val x = chartWidth * hour / 23f
            val y = baseline - (chartHeight * (salesMap[hour] ?: 0.0) / maxSales).toFloat()
            Offset(x, y)
        }

        if (points.isNotEmpty()) {
            val path = Path().apply {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
            }
            drawPath(path, lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

            points.forEach { pt ->
                drawCircle(lineColor, radius = 4f, center = pt)
                drawCircle(Color(0xFF0D0D0D), radius = 2f, center = pt)
            }

            hourPositions.forEachIndexed { index, hour ->
                val x = chartWidth * hour / 23f
                xAxisPaint.color = textColor.hashCode()
                drawContext.canvas.nativeCanvas.drawText(
                    hourLabels[index], x, baseline + 18f, xAxisPaint
                )
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Category Donut Chart (Canvas-based)
// ════════════════════════════════════════════════════════════════════

private val categoryColors = listOf(
    OrangeAccent,
    Color(0xFFF48FB1),
    Color(0xFFFF8F00),
    Color(0xFF9C27B0),
    Color(0xFF2196F3),
    Color(0xFF4CAF50)
)

@Composable
private fun CategoryDonutChart(
    categorySales: List<CategorySalesSummary>,
    modifier: Modifier = Modifier
) {
    val total = categorySales.sumOf { it.totalSales }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(160.dp)) {
                if (total <= 0) return@Canvas
                val strokeW = 36f
                val radius = (size.minDimension - strokeW) / 2f
                val topLeft = Offset(
                    (size.width - radius * 2) / 2f,
                    (size.height - radius * 2) / 2f
                )
                var startAngle = -90f

                categorySales.forEachIndexed { index, cs ->
                    val sweep = (cs.totalSales / total * 360f).toFloat()
                    drawArc(
                        color = categoryColors[index % categoryColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("₱${String.format(Locale.US, "%,.2f", total)}", style = MaterialTheme.typography.titleSmall, color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Total", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            categorySales.forEachIndexed { index, cs ->
                val pct = if (total > 0) (cs.totalSales / total * 100) else 0.0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(categoryColors[index % categoryColors.size]))
                    Text("${cs.category}", style = MaterialTheme.typography.bodySmall, color = TextWhite, modifier = Modifier.weight(1f))
                    Text("${String.format(Locale.US, "%.1f", pct)}%", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Text("₱${String.format(Locale.US, "%,.2f", cs.totalSales)}", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Top Products Table
// ════════════════════════════════════════════════════════════════════

@Composable
private fun TopProductsTable(products: List<TopSellingProduct>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("Product", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("Qty", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("Sales", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
        if (products.isEmpty()) {
            Text("No product data yet", style = MaterialTheme.typography.bodySmall, color = TextGray)
        } else {
            products.forEach { product ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(product.productName, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${product.totalQty}", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", product.totalSales)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Payment Methods Donut Chart (Canvas-based)
// ════════════════════════════════════════════════════════════════════

private val paymentColors = mapOf(
    "Cash" to StatusGreen,
    "GCash" to Color(0xFF2196F3),
    "Other" to Color(0xFF9C27B0)
)

@Composable
private fun PaymentDonutChart(
    paymentMethods: List<PaymentMethodSales>,
    modifier: Modifier = Modifier
) {
    val total = paymentMethods.sumOf { it.totalSales }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                if (total <= 0) return@Canvas
                val strokeW = 30f
                val radius = (size.minDimension - strokeW) / 2f
                val topLeft = Offset(
                    (size.width - radius * 2) / 2f,
                    (size.height - radius * 2) / 2f
                )
                var startAngle = -90f

                paymentMethods.forEachIndexed { index, pm ->
                    val color = paymentColors[pm.method] ?: categoryColors[index % categoryColors.size]
                    val sweep = (pm.totalSales / total * 360f).toFloat()
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("₱${String.format(Locale.US, "%,.2f", total)}", style = MaterialTheme.typography.titleSmall, color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Total", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            paymentMethods.forEach { pm ->
                val pct = if (total > 0) (pm.totalSales / total * 100) else 0.0
                val color = paymentColors[pm.method] ?: Color(0xFF9C27B0)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Text("${pm.method}:", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", pm.totalSales)}", style = MaterialTheme.typography.bodySmall, color = TextWhite, fontWeight = FontWeight.SemiBold)
                    Text("(${String.format(Locale.US, "%.1f", pct)}%)", style = MaterialTheme.typography.bodySmall, color = TextGray)
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Recent Transactions Mini Table
// ════════════════════════════════════════════════════════════════════

@Composable
private fun RecentTransactionsTable(transactions: List<TransactionEntity>) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text("Order ID", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("Time", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("Amount", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text("Payment", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }

        if (transactions.isEmpty()) {
            Text("No transactions today", style = MaterialTheme.typography.bodySmall, color = TextGray)
        } else {
            transactions.forEach { txn ->
                val timeStr = remember(txn.createdAt) { timeFormat.format(Date(txn.createdAt)) }
                val isGcash = txn.paymentMethod.equals("GCash", ignoreCase = true)
                val paymentColor = if (isGcash) Color(0xFF2196F3) else StatusGreen

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("#ORD-${String.format(Locale.US, "%05d", txn.id)}", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                    Text(timeStr, modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", txn.total)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = paymentColor.copy(alpha = 0.15f)
                    ) {
                        Text(txn.paymentMethod, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = paymentColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Date Range Dialog
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onApply: (startMillis: Long?, endMillis: Long?) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Select Date Range", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.height(380.dp)) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = DarkCard,
                        titleContentColor = TextWhite,
                        headlineContentColor = TextWhite,
                        weekdayContentColor = TextMuted,
                        subheadContentColor = TextWhite,
                        yearContentColor = TextWhite,
                        currentYearContentColor = OrangeAccent,
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = OrangeAccent,
                        dayContentColor = TextWhite,
                        todayContentColor = OrangeAccent,
                        todayDateBorderColor = OrangeAccent,
                        dayInSelectionRangeContentColor = Color.White,
                        dayInSelectionRangeContainerColor = OrangeAccent.copy(alpha = 0.3f),
                        dividerColor = DarkBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                          dateRangePickerState.selectedEndDateMillis != null
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}


// ════════════════════════════════════════════════════════════════════
// SALES TAB — Daily sales, payment methods, hourly trends
// ════════════════════════════════════════════════════════════════════

@Composable
private fun SalesTabContent(
    dailySales: List<DailySalesSummary>,
    paymentMethods: List<PaymentMethodSales>,
    hourlySales: List<HourlySalesSummary>,
    kpiData: KPIData
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetricCard("Total Sales", "₱${String.format(Locale.US, "%,.2f", kpiData.totalSales)}", StatusGreen, Modifier.weight(1f))
            SmallMetricCard("Orders", "${kpiData.totalOrders}", OrangeAccent, Modifier.weight(1f))
            SmallMetricCard("Items Sold", "${kpiData.totalItemsSold}", Color(0xFF9C27B0), Modifier.weight(1f))
            SmallMetricCard("Avg Order", "₱${String.format(Locale.US, "%,.2f", kpiData.avgOrderValue)}", Color(0xFF2196F3), Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1.5f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DAILY SALES TREND", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    DailySalesBarChart(dailySales = dailySales, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("PAYMENT METHODS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    PaymentDonutChart(paymentMethods = paymentMethods, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("HOURLY BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Hour", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Sales", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                hourlySales.sortedBy { it.hour }.forEach { h ->
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text("${String.format(Locale.US, "%02d:00", h.hour)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                        Text("₱${String.format(Locale.US, "%,.2f", h.amount)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// ORDERS TAB — Status breakdown, orders over time
// ════════════════════════════════════════════════════════════════════

@Composable
private fun OrdersTabContent(
    orderStatusCounts: List<OrderStatusSummary>,
    dailySales: List<DailySalesSummary>,
    allTransactions: List<TransactionEntity>,
    kpiData: KPIData
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetricCard("Total Orders", "${kpiData.totalOrders}", OrangeAccent, Modifier.weight(1f))
            SmallMetricCard("Items Sold", "${kpiData.totalItemsSold}", Color(0xFF9C27B0), Modifier.weight(1f))
            SmallMetricCard("Avg Items/Order", if (kpiData.totalOrders > 0) String.format(Locale.US, "%.1f", kpiData.totalItemsSold.toDouble() / kpiData.totalOrders) else "0", Color(0xFF2196F3), Modifier.weight(1f))
            SmallMetricCard("Total Sales", "₱${String.format(Locale.US, "%,.2f", kpiData.totalSales)}", StatusGreen, Modifier.weight(1f))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ORDER STATUS BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val statusColors = mapOf("Completed" to StatusGreen, "Preparing" to Color(0xFF2196F3), "Pending" to Color(0xFFFFC107), "Canceled" to MutedRed)
                    if (orderStatusCounts.isEmpty()) {
                        Text("No orders found", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    } else {
                        orderStatusCounts.forEach { status ->
                            val color = statusColors[status.status] ?: TextMuted
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.15f)) {
                                    Text(status.status, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("${status.count}", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("ORDERS OVER TIME", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                DailyOrdersBarChart(dailySales = dailySales, modifier = Modifier.fillMaxWidth().height(200.dp))
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// PRODUCTS TAB — All products, categories, top sellers
// ════════════════════════════════════════════════════════════════════

@Composable
private fun ProductsTabContent(
    allProducts: List<ProductEntity>,
    totalProducts: Int,
    topProducts: List<TopSellingProduct>,
    categorySales: List<CategorySalesSummary>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetricCard("Total Products", "$totalProducts", OrangeAccent, Modifier.weight(1f))
            val categoryCount = allProducts.map { it.category }.distinct().size
            SmallMetricCard("Categories", "$categoryCount", Color(0xFF9C27B0), Modifier.weight(1f))
            SmallMetricCard("Top Seller", topProducts.firstOrNull()?.productName ?: "N/A", StatusGreen, Modifier.weight(1f))
            val totalQty = topProducts.sumOf { it.totalQty }
            SmallMetricCard("Units Sold", "$totalQty", Color(0xFF2196F3), Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("PRODUCT CATEGORIES", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryDonutChart(categorySales = categorySales, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TOP SELLERS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TopProductsTable(products = topProducts)
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("ALL PRODUCTS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (allProducts.isEmpty()) {
                            Text("No products yet", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        } else {
                            allProducts.take(10).forEach { p ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Text(p.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(p.category, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// CUSTOMERS TAB — Top spenders, customer counts
// ════════════════════════════════════════════════════════════════════

@Composable
fun CustomersTabContent(
    topCustomers: List<CustomerSummary>,
    totalCustomers: Int,
    allTransactions: List<TransactionEntity>
) {
    val totalRevenue = allTransactions.sumOf { it.total }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetricCard("Unique Customers", "$totalCustomers", OrangeAccent, Modifier.weight(1f))
            SmallMetricCard("Total Orders", "${allTransactions.size}", Color(0xFF2196F3), Modifier.weight(1f))
            SmallMetricCard("Avg/Customer", if (totalCustomers > 0) String.format(Locale.US, "%.1f", allTransactions.size.toDouble() / totalCustomers) else "0", Color(0xFF9C27B0), Modifier.weight(1f))
            SmallMetricCard("Total Spent", "₱${String.format(Locale.US, "%,.2f", totalRevenue)}", StatusGreen, Modifier.weight(1f))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TOP CUSTOMERS", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Customer", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Orders", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Total Spent", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                if (topCustomers.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No customer data yet", style = MaterialTheme.typography.bodySmall, color = TextGray)
                } else {
                    topCustomers.forEach { c ->
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(c.customerName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${c.orderCount}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                            Text("₱${String.format(Locale.US, "%,.2f", c.totalSpent)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// EMPLOYEES TAB — Cashier performance, user stats
// ════════════════════════════════════════════════════════════════════

@Composable
private fun EmployeesTabContent(
    cashierPerformance: List<CashierSummary>,
    totalUsers: Int,
    allTransactions: List<TransactionEntity>
) {
    val totalCashierOrders = cashierPerformance.sumOf { it.orderCount }
    val totalCashierSales = cashierPerformance.sumOf { it.totalSales }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetricCard("Total Staff", "$totalUsers", OrangeAccent, Modifier.weight(1f))
            SmallMetricCard("Active Cashiers", "${cashierPerformance.size}", Color(0xFF2196F3), Modifier.weight(1f))
            SmallMetricCard("Orders Processed", "$totalCashierOrders", Color(0xFF9C27B0), Modifier.weight(1f))
            SmallMetricCard("Total Processed", "₱${String.format(Locale.US, "%,.2f", totalCashierSales)}", StatusGreen, Modifier.weight(1f))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("CASHIER PERFORMANCE", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Cashier", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Orders", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Total Sales", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("Avg/Order", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                }
                if (cashierPerformance.isEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No cashier data in this period", style = MaterialTheme.typography.bodySmall, color = TextGray)
                } else {
                    cashierPerformance.forEach { c ->
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                        val avg = if (c.orderCount > 0) c.totalSales / c.orderCount else 0.0
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Text(c.cashierName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${c.orderCount}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                            Text("₱${String.format(Locale.US, "%,.2f", c.totalSales)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                            Text("₱${String.format(Locale.US, "%,.2f", avg)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// TAXES TAB — Financial summary / estimated VAT
// ════════════════════════════════════════════════════════════════════

@Composable
private fun TaxesTabContent(kpiData: KPIData) {
    val vatRate = 0.12
    val vatAmount = kpiData.totalSales * vatRate / (1 + vatRate)
    val netSales = kpiData.totalSales - vatAmount

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallMetricCard("Gross Sales", "₱${String.format(Locale.US, "%,.2f", kpiData.totalSales)}", StatusGreen, Modifier.weight(1f))
            SmallMetricCard("VAT (12%)", "₱${String.format(Locale.US, "%,.2f", vatAmount)}", OrangeAccent, Modifier.weight(1f))
            SmallMetricCard("Net Sales", "₱${String.format(Locale.US, "%,.2f", netSales)}", Color(0xFF2196F3), Modifier.weight(1f))
            SmallMetricCard("Gross Profit", "₱${String.format(Locale.US, "%,.2f", kpiData.grossProfit)}", Color(0xFFFFC107), Modifier.weight(1f))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TAX SUMMARY", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))
                TaxRow("Gross Revenue", kpiData.totalSales, TextWhite)
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                TaxRow("Less: VAT (${(vatRate * 100).toInt()}%)", -vatAmount, MutedRed)
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                TaxRow("Net Revenue", netSales, StatusGreen)
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                TaxRow("Cost of Goods (est. 50%)", -kpiData.totalSales * 0.5, MutedRed)
                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                TaxRow("Gross Profit", kpiData.grossProfit, Color(0xFFFFC107))
                Spacer(modifier = Modifier.height(8.dp))
                Text("* VAT computed at ${(vatRate * 100).toInt()}% Philippine standard rate. Actual tax obligations may vary.", style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
        }
    }
}

@Composable
private fun TaxRow(label: String, amount: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        Text("₱${String.format(Locale.US, "%,.2f", kotlin.math.abs(amount))}", style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.Bold)
    }
}


// ════════════════════════════════════════════════════════════════════
// PROFIT TAB — Multi-line chart, donut breakdown, summary ledger
// All modifications applied: granularity aggregation, perDayItemsSold, perDayExpenses, filter, trend arrows
// ════════════════════════════════════════════════════════════════════

@Composable
fun ProfitTabContent(
    dailySales: List<DailySalesSummary>,
    kpiData: KPIData,
    itemsSold: Int,
    realExpenses: Double = 0.0,
    perDayExpenses: Map<Long, Double> = emptyMap(),
    perDayItemsSold: Map<Long, Int> = emptyMap(),
    allTransactions: List<TransactionEntity> = emptyList(),
    selectedDateLabel: String = "",
    activeFilter: String = "All",
    onDateClick: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    onGranularityChange: (String) -> Unit = {}
) {
    var granularity by remember { mutableStateOf("Daily") }
    val granularities = listOf("Daily", "Weekly", "Monthly")
    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 5

    // ── Aggregation helpers ──
    data class LedgerRow(
        val dateLabel: String,
        val dateTimestamp: Long,
        val cupsSold: Int,
        val sales: Double,
        val expenses: Double,
        val profit: Double,
        val margin: Double
    )

    data class DayGroup(val label: String, val timestamp: Long, val days: List<LedgerRow>)

    val dateFmt = remember { SimpleDateFormat("MMMM dd, yyyy (EEE)", Locale.US) }
    val weekFmt = remember { SimpleDateFormat("'Week of' MMM dd, yyyy", Locale.US) }
    val monthFmt = remember { SimpleDateFormat("MMMM yyyy", Locale.US) }

    // Aggregate daily sales by the selected granularity
    fun aggregateDailySales(sales: List<DailySalesSummary>, granularity: String): List<LedgerRow> {
        val sorted = sales.sortedByDescending { it.dayOffset }
        return when (granularity) {
            "Daily" -> sorted.map { day ->
                val timestamp = day.dayOffset * 86400000L
                val expenses = perDayExpenses[day.dayOffset] ?: 0.0
                val profit = day.total - expenses
                val margin = if (day.total > 0) (profit / day.total) * 100 else 0.0
                val itemsCount = perDayItemsSold[day.dayOffset] ?: 0
                LedgerRow(
                    dateLabel = if (timestamp > 0) dateFmt.format(Date(timestamp)) else "Unknown",
                    dateTimestamp = timestamp,
                    cupsSold = itemsCount,
                    sales = day.total,
                    expenses = expenses,
                    profit = profit,
                    margin = margin
                )
            }
            "Weekly" -> {
                sorted.groupBy { it.dayOffset / 7 }.map { (weekOffset, days) ->
                    val weekStartTimestamp = weekOffset * 7 * 86400000L
                    val weekEndTimestamp = (weekOffset * 7 + 6) * 86400000L
                    val weekMidTimestamp = (weekStartTimestamp + weekEndTimestamp) / 2
                    val totalSalesWeek = days.sumOf { it.total }
                    val totalExpensesWeek = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }
                    val totalCupsWeek = days.sumOf { perDayItemsSold[it.dayOffset] ?: 0 }
                    val profitWeek = totalSalesWeek - totalExpensesWeek
                    val marginWeek = if (totalSalesWeek > 0) (profitWeek / totalSalesWeek) * 100 else 0.0
                    LedgerRow(
                        dateLabel = weekFmt.format(Date(weekMidTimestamp)),
                        dateTimestamp = weekStartTimestamp,
                        cupsSold = totalCupsWeek,
                        sales = totalSalesWeek,
                        expenses = totalExpensesWeek,
                        profit = profitWeek,
                        margin = marginWeek
                    )
                }
            }
            "Monthly" -> {
                sorted.groupBy { day ->
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = day.dayOffset * 86400000L
                    }
                    cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                }.map { (_, days) ->
                    val firstDay = days.minBy { it.dayOffset }
                    val timestamp = firstDay.dayOffset * 86400000L
                    val totalSalesMonth = days.sumOf { it.total }
                    val totalExpensesMonth = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }
                    val totalCupsMonth = days.sumOf { perDayItemsSold[it.dayOffset] ?: 0 }
                    val profitMonth = totalSalesMonth - totalExpensesMonth
                    val marginMonth = if (totalSalesMonth > 0) (profitMonth / totalSalesMonth) * 100 else 0.0
                    LedgerRow(
                        dateLabel = monthFmt.format(Date(timestamp)),
                        dateTimestamp = timestamp,
                        cupsSold = totalCupsMonth,
                        sales = totalSalesMonth,
                        expenses = totalExpensesMonth,
                        profit = profitMonth,
                        margin = marginMonth
                    )
                }.sortedByDescending { it.dateTimestamp }
            }
            else -> emptyList()
        }
    }

    val allLedgerRows = remember(dailySales, perDayExpenses, perDayItemsSold, granularity) {
        aggregateDailySales(dailySales, granularity)
    }
    val ledgerRows = remember(allLedgerRows, activeFilter) {
        when (activeFilter) {
            "Profitable Only" -> allLedgerRows.filter { it.profit > 0 }
            "Loss Making Only" -> allLedgerRows.filter { it.profit <= 0 }
            "With Expenses Only" -> allLedgerRows.filter { it.expenses > 0 }
            else -> allLedgerRows
        }
    }

    // Compute derived profit data from aggregated rows
    val totalSales = allLedgerRows.sumOf { it.sales }
    val totalExpenses = allLedgerRows.sumOf { it.expenses }
    val netProfit = totalSales - totalExpenses
    val profitMargin = if (totalSales > 0) (netProfit / totalSales) * 100 else 0.0
    val totalCups = allLedgerRows.sumOf { it.cupsSold }

    val totalPages = maxOf((ledgerRows.size + itemsPerPage - 1) / itemsPerPage, 1)
    val safePage = minOf(currentPage, totalPages)
    val paginatedRows = ledgerRows.drop((safePage - 1) * itemsPerPage).take(itemsPerPage)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // ── Title ──
        Text("📈 Profit Overview", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
        Text("Track your business performance and profit in daily, weekly, and monthly view.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))

        // ── Granularity Toggle + Filter ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                granularities.forEach { g ->
                    val isActive = g == granularity
                    Text(
                        text = g,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) TextWhite else TextMuted,
                        modifier = Modifier
                            .clickable { granularity = g; currentPage = 1; onGranularityChange(g) }
                            .then(if (isActive) Modifier.background(OrangeAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)) else Modifier)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDateClick,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextWhite),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (selectedDateLabel.isNotEmpty()) selectedDateLabel else SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date()), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onFilterClick,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextMuted),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (activeFilter != "All") "Filter: $activeFilter" else "Filter", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        // ── KPI Cards ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfitKPICard("Total Sales", "₱${String.format(Locale.US, "%,.2f", totalSales)}", StatusGreen, if (kpiData.salesChange >= 0) "▲ ${String.format(Locale.US, "%.1f", kpiData.salesChange)}% vs Yesterday" else "▼ ${String.format(Locale.US, "%.1f", -kpiData.salesChange)}% vs Yesterday", Modifier.weight(1f))
            ProfitKPICard("Total Expenses", "₱${String.format(Locale.US, "%,.2f", totalExpenses)}", MutedRed, if (kpiData.profitChange >= 0) "▲ ${String.format(Locale.US, "%.1f", kpiData.profitChange)}% vs Yesterday" else "▼ ${String.format(Locale.US, "%.1f", -kpiData.profitChange)}% vs Yesterday", Modifier.weight(1f))
            ProfitKPICard("Net Profit", "₱${String.format(Locale.US, "%,.2f", netProfit)}", Color(0xFFFFC107), if (kpiData.profitChange >= 0) "▲ ${String.format(Locale.US, "%.1f", kpiData.profitChange)}% vs Yesterday" else "▼ ${String.format(Locale.US, "%.1f", -kpiData.profitChange)}% vs Yesterday", Modifier.weight(1f))
            ProfitKPICard("Profit Margin", "${String.format(Locale.US, "%.1f", profitMargin)}%", Color(0xFF9C27B0), if (kpiData.profitChange >= 0) "▲ ${String.format(Locale.US, "%.1f", kpiData.profitChange)}% vs Yesterday" else "▼ ${String.format(Locale.US, "%.1f", -kpiData.profitChange)}% vs Yesterday", Modifier.weight(1f))
            ProfitKPICard("Total Cups Sold", "$totalCups", Color(0xFF2196F3), if (kpiData.itemsChange >= 0) "▲ ${String.format(Locale.US, "%.1f", kpiData.itemsChange)}% vs Yesterday" else "▼ ${String.format(Locale.US, "%.1f", -kpiData.itemsChange)}% vs Yesterday", Modifier.weight(1f))
        }

        // ── Middle Row: Multi-line Chart + Donut Breakdown ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Multi-line chart (flex 2)
            Card(modifier = Modifier.weight(2f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("PROFIT OVER TIME ($granularity)", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Last 7 Days", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        LegendDot(StatusGreen, "Sales")
                        LegendDot(MutedRed, "Expenses")
                        LegendDot(OrangeAccent, "Profit")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ProfitMultiLineChart(dailySales = dailySales, perDayExpenses = perDayExpenses, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }

            // Profit Breakdown Donut (flex 1)
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("PROFIT BREAKDOWN", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ProfitBreakdownDonut(totalSales = totalSales, totalExpenses = totalExpenses, modifier = Modifier.fillMaxWidth().height(180.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = OrangeAccent.copy(alpha = 0.12f)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Profit Margin", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("${String.format(Locale.US, "%.1f", profitMargin)}%", style = MaterialTheme.typography.bodyMedium, color = OrangeAccent, fontWeight = FontWeight.Bold)
                                Text(if (kpiData.profitChange >= 0) "▲ ${String.format(Locale.US, "%.1f", kpiData.profitChange)}%" else "▼ ${String.format(Locale.US, "%.1f", -kpiData.profitChange)}%", style = MaterialTheme.typography.labelSmall, color = StatusGreen)
                            }
                        }
                    }
                }
            }
        }

        // ── Summary Ledger Table ──
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Table toolbar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SUMMARY TABLE", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Show transactions per day for Daily, aggregated for Weekly/Monthly
                if (granularity == "Daily") {
                    // Transaction view header
                    Row(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("TIME", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("ORDER #", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("CUSTOMER", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("ITEMS", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("PAYMENT", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("AMOUNT (" + "₱" + ")", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }

                    if (allTransactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions found for this period", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    } else {
                        val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.US) }
                        val dateFmt2 = remember { SimpleDateFormat("MMMM dd, yyyy (EEE)", Locale.US) }
                        // Group transactions by day
                        val txnsByDay = allTransactions.groupBy { it.createdAt / 86400000L }
                            .toSortedMap(compareByDescending { it })
                        
                        txnsByDay.forEach { (dayOffset, txns) ->
                            // Date header
                            val dateStr = if (dayOffset > 0) dateFmt2.format(Date(dayOffset * 86400000L)) else "Unknown"
                            Surface(
                                color = OrangeAccent.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text("  " + dateStr, modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                    style = MaterialTheme.typography.labelMedium, color = OrangeAccent, fontWeight = FontWeight.Bold)
                            }

                            txns.forEach { txn ->
                                HorizontalDivider(color = DarkBorder, thickness = 0.3.dp)
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(timeFmt.format(Date(txn.createdAt)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text("#" + String.format(Locale.US, "%05d", txn.id), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                                    Text(txn.customerName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("" + txn.itemCount, modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    val pmColor = if (txn.paymentMethod.equals("GCash", ignoreCase = true)) Color(0xFF2196F3) else StatusGreen
                                    Surface(shape = RoundedCornerShape(10.dp), color = pmColor.copy(alpha = 0.12f), modifier = Modifier.weight(1f)) {
                                        Text(txn.paymentMethod, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = pmColor, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text("₱" + String.format(Locale.US, "%,.2f", txn.total), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    // Per-day view grouped by Week/Month
                    val dailyLedgerRows = remember(dailySales, perDayExpenses, perDayItemsSold, activeFilter) {
                        val raw = aggregateDailySales(dailySales, "Daily")
                        when (activeFilter) {
                            "Profitable Only" -> raw.filter { it.profit > 0 }
                            "Loss Making Only" -> raw.filter { it.profit <= 0 }
                            "With Expenses Only" -> raw.filter { it.expenses > 0 }
                            else -> raw
                        }
                    }

                    // Group daily rows by week or month
                    val groupedByPeriod = remember(dailyLedgerRows, granularity) {
                        if (granularity == "Weekly") {
                            val weekFmt = SimpleDateFormat("'Week of' MMM dd, yyyy", Locale.US)
                            dailyLedgerRows.groupBy { row ->
                                row.dateTimestamp / (7 * 86400000L)
                            }.map { (weekOffset, rows) ->
                                val weekStart = weekOffset * 7 * 86400000L
                                val weekMid = weekStart + 3 * 86400000L
                                DayGroup(
                                    label = weekFmt.format(Date(weekMid)),
                                    timestamp = weekStart,
                                    days = rows
                                )
                            }.sortedByDescending { it.timestamp }
                        } else { // Monthly
                            val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
                            dailyLedgerRows.groupBy { row ->
                                val cal = Calendar.getInstance().apply { timeInMillis = row.dateTimestamp }
                                cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                            }.map { (_, rows) ->
                                val firstDay = rows.minBy { it.dateTimestamp }
                                DayGroup(
                                    label = monthFmt.format(Date(firstDay.dateTimestamp)),
                                    timestamp = firstDay.dateTimestamp,
                                    days = rows
                                )
                            }.sortedByDescending { it.timestamp }
                        }
                    }

                    // Header row
                    Row(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("DATE", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("CUPS SOLD", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("SALES (₱)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("EXPENSES (₱)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("PROFIT (₱)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("MARGIN (%)", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.weight(0.4f))
                    }

                    val groupsPerPage = itemsPerPage
                    val totalGroups = maxOf((groupedByPeriod.size + groupsPerPage - 1) / groupsPerPage, 1)
                    val currentGroupPage = safePage
                    val paginatedGroups = groupedByPeriod.drop((currentGroupPage - 1) * groupsPerPage).take(groupsPerPage)

                    if (groupedByPeriod.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("No data available", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    } else {
                        paginatedGroups.forEach { group ->
                            // Group header with total for the period
                            val groupTotalSales = group.days.sumOf { it.sales }
                            val groupTotalExpenses = group.days.sumOf { it.expenses }
                            val groupTotalProfit = groupTotalSales - groupTotalExpenses
                            val groupMargin = if (groupTotalSales > 0) (groupTotalProfit / groupTotalSales) * 100 else 0.0
                            val groupTotalCups = group.days.sumOf { it.cupsSold }

                            Surface(
                                color = OrangeAccent.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                                    Text(group.label, modifier = Modifier.weight(2f),
                                        style = MaterialTheme.typography.labelMedium, color = OrangeAccent, fontWeight = FontWeight.Bold)
                                    Text("" + groupTotalCups + " cups", modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("₱" + String.format(Locale.US, "%,.2f", groupTotalSales), modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("₱" + String.format(Locale.US, "%,.2f", groupTotalExpenses), modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("₱" + String.format(Locale.US, "%,.2f", groupTotalProfit), modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("" + String.format(Locale.US, "%.1f", groupMargin) + "%", modifier = Modifier.weight(0.8f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }

                            // Per-day rows under the group header
                            group.days.forEach { row ->
                                HorizontalDivider(color = DarkBorder, thickness = 0.3.dp)
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(row.dateLabel, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("" + row.cupsSold, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    Text("₱" + String.format(Locale.US, "%,.2f", row.sales), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    Text("₱" + String.format(Locale.US, "%,.2f", row.expenses), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MutedRed)
                                    Text("₱" + String.format(Locale.US, "%,.2f", row.profit), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = StatusGreen, fontWeight = FontWeight.SemiBold)
                                    Text("" + String.format(Locale.US, "%.1f", row.margin) + "%", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    Text("⋮", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = TextGray)
                                }
                            }
                        }
                    }

                    // Pagination for grouped view
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val from = if (groupedByPeriod.isEmpty()) 0 else (currentGroupPage - 1) * groupsPerPage + 1
                        val to = minOf(currentGroupPage * groupsPerPage, groupedByPeriod.size)
                        Text("Showing " + from + " to " + to + " of " + groupedByPeriod.size + " periods", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PageButton("<", enabled = currentGroupPage > 1) { if (currentPage > 1) currentPage-- }
                            for (p in 1..totalGroups) {
                                PageButton("" + p, isActive = p == currentGroupPage, enabled = true) { currentPage = p }
                            }
                            PageButton(">", enabled = currentGroupPage < totalGroups) { if (currentPage < totalGroups) currentPage++ }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfitKPICard(label: String, value: String, valueColor: Color, trend: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = Color(0xFF0D0D0D)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, color = valueColor, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(trend, style = MaterialTheme.typography.labelSmall, color = StatusGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
fun ProfitMultiLineChart(
    dailySales: List<DailySalesSummary>,
    perDayExpenses: Map<Long, Double> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(dailySales.maxOfOrNull { it.total } ?: 1.0, 1.0)
    val salesColor = StatusGreen
    val expensesColor = MutedRed
    val profitColor = OrangeAccent
    val gridColor = DarkBorder
    val textColor = TextGray

    // Compute max across all series for Y-axis scaling
    val maxExpense = dailySales.maxOfOrNull { perDayExpenses[it.dayOffset] ?: 0.0 } ?: 0.0
    val maxProfit = dailySales.maxOfOrNull { it.total - (perDayExpenses[it.dayOffset] ?: 0.0) } ?: 0.0
    val overallMax = maxOf(maxVal, maxOf(maxExpense, 1.0), maxOf(maxProfit, 1.0))

    val yAxisPaint = remember { android.graphics.Paint().apply { textSize = 22f; textAlign = android.graphics.Paint.Align.LEFT } }
    val valuePaint = remember { android.graphics.Paint().apply {
        color = TextWhite.hashCode(); textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
    } }

    Canvas(modifier = modifier.padding(start = 40.dp, bottom = 28.dp, end = 12.dp, top = 8.dp)) {
        if (dailySales.size < 2) return@Canvas
        val chartWidth = size.width
        val chartHeight = size.height
        val stepX = chartWidth / (dailySales.size - 1)

        // ── Draw Y-axis grid lines and labels ──
        val gridSteps = 4
        for (i in 0..gridSteps) {
            val y = chartHeight - (chartHeight * i / gridSteps)
            drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), strokeWidth = 0.5f)
            val labelValue = (overallMax * i / gridSteps).toInt()
            yAxisPaint.color = textColor.hashCode()
            val displayLabel = if (labelValue >= 1000) {
                "₱${labelValue / 1000}K"
            } else {
                "₱${labelValue}"
            }
            drawContext.canvas.nativeCanvas.drawText(
                displayLabel, -36f, y + 4f, yAxisPaint
            )
        }

        fun drawLine(points: List<Float>, color: Color) {
            if (points.size < 2) return
            val path = Path().apply {
                moveTo(0f, points[0])
                for (i in 1 until points.size) lineTo(i * stepX, points[i])
            }
            drawPath(path, color, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        fun drawDots(points: List<Float>, color: Color) {
            points.forEachIndexed { i, y ->
                val x = i * stepX
                drawCircle(color, radius = 4f, center = androidx.compose.ui.geometry.Offset(x, y))
                drawCircle(Color(0xFF0D0D0D), radius = 2.5f, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }

        val salesPoints = dailySales.map { chartHeight - (chartHeight * (it.total / overallMax)).toFloat() }
        val expensePoints = dailySales.mapIndexed { _, day ->
            val exp = perDayExpenses[day.dayOffset] ?: 0.0
            chartHeight - (chartHeight * (exp / overallMax)).toFloat()
        }
        val profitPoints = dailySales.mapIndexed { _, day ->
            val profit = day.total - (perDayExpenses[day.dayOffset] ?: 0.0)
            chartHeight - (chartHeight * (maxOf(profit, 0.0) / overallMax)).toFloat()
        }

        // Draw lines
        drawLine(salesPoints, salesColor)
        drawLine(expensePoints, expensesColor)
        drawLine(profitPoints, profitColor)

        // Draw value indicator dots
        drawDots(salesPoints, salesColor)
        drawDots(expensePoints, expensesColor)
        drawDots(profitPoints, profitColor)

        // Value labels at the last data point for each series
        if (salesPoints.isNotEmpty()) {
            val lastIdx = dailySales.size - 1
            val lastDay = dailySales[lastIdx]
            val lastSales = lastDay.total
            val lastExpense = perDayExpenses[lastDay.dayOffset] ?: 0.0
            val lastProfit = lastDay.total - lastExpense

            val labelX = lastIdx * stepX + 12f
            valuePaint.color = salesColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "₱${String.format(Locale.US, "%,.0f", lastSales)}",
                labelX, salesPoints[lastIdx] + 5f, valuePaint
            )

            valuePaint.color = expensesColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "₱${String.format(Locale.US, "%,.0f", lastExpense)}",
                labelX, expensePoints[lastIdx] + 5f, valuePaint
            )

            valuePaint.color = profitColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "₱${String.format(Locale.US, "%,.0f", lastProfit)}",
                labelX, profitPoints[lastIdx] + 5f, valuePaint
            )
        }

        // Date labels at bottom
        val dateFmt = SimpleDateFormat("MMM dd", Locale.US)
        dailySales.forEachIndexed { i, day ->
            val x = i * stepX
            val label = if (day.dayOffset > 0) dateFmt.format(Date(day.dayOffset * 86400000L)) else ""
            drawContext.canvas.nativeCanvas.drawText(
                label, x, chartHeight + 18f,
                android.graphics.Paint().apply { color = textColor.hashCode(); textSize = 18f; textAlign = android.graphics.Paint.Align.CENTER }
            )
        }
    }
}

@Composable
fun ProfitBreakdownDonut(totalSales: Double, totalExpenses: Double, modifier: Modifier = Modifier) {
    val otherIncome = 0.0
    val grandTotal = totalSales + totalExpenses
    val netProfit = totalSales - totalExpenses

    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(150.dp)) {
                if (grandTotal <= 0) return@Canvas
                val strokeW = 34f
                val radius = (size.minDimension - strokeW) / 2f
                val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
                var startAngle = -90f

                data class Slice(val label: String, val value: Double, val color: Color)
                val slices = listOf(
                    Slice("Sales", totalSales, StatusGreen),
                    Slice("Expenses", totalExpenses, MutedRed)
                ).filter { it.value > 0 }

                slices.forEach { slice ->
                    val sweep = (slice.value / grandTotal * 360f).toFloat()
                    drawArc(color = slice.color, startAngle = startAngle, sweepAngle = sweep, useCenter = false,
                        topLeft = topLeft, size = Size(radius * 2, radius * 2), style = Stroke(width = strokeW, cap = StrokeCap.Butt))
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("₱${String.format(Locale.US, "%,.2f", netProfit)}", style = MaterialTheme.typography.titleSmall, color = TextWhite, fontWeight = FontWeight.Bold)
                Text("Net Profit", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        val slices = listOf(
            Triple("Sales", totalSales, StatusGreen),
            Triple("Expenses", totalExpenses, MutedRed),
            Triple("Net Profit", netProfit, OrangeAccent)
        )
        slices.forEach { (label, value, color) ->
            val pct = if (grandTotal > 0) (value / grandTotal * 100) else 0.0
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
                Text(label, style = MaterialTheme.typography.bodySmall, color = TextWhite, modifier = Modifier.weight(1f))
                Text("₱${String.format(Locale.US, "%,.2f", value)}", style = MaterialTheme.typography.bodySmall, color = TextWhite)
                Text("(${String.format(Locale.US, "%.1f", pct)}%)", style = MaterialTheme.typography.bodySmall, color = TextGray)
            }
        }
    }
}

@Composable
fun PageButton(text: String, isActive: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) OrangeAccent else Color.Transparent,
        border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, style = MaterialTheme.typography.labelSmall, color = if (isActive) TextWhite else if (enabled) TextWhite else TextGray)
        }
    }
}


// ════════════════════════════════════════════════════════════════════
// EXPENSES TAB — Sub-nav, KPI cards, donut, line chart, ledger table
// ════════════════════════════════════════════════════════════════════

@Composable
fun ExpensesTabContent(
    expensesList: List<ExpenseEntity>,
    categoryTotals: ExpenseCategoryTotals?,
    kpiData: KPIData,
    onExpenseSaved: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    val scope = rememberCoroutineScope()
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var kpiRefreshKey by remember { mutableIntStateOf(0) }
    var activeView by remember { mutableStateOf("Daily Expenses") }
    val views = listOf("Daily Expenses", "Expense Summary", "Expense Categories", "Suppliers")
    var currentPage by remember { mutableIntStateOf(1) }
    val itemsPerPage = 5

    data class DailyExpenseRow(
        val dateLabel: String,
        val dateTimestamp: Long,
        val syrups: Double,
        val sauce: Double,
        val milk: Double,
        val ice: Double,
        val others: Double,
        val total: Double
    )

    val dateFmt = remember { SimpleDateFormat("MMMM dd, yyyy (EEE)", Locale.US) }
    val dailyExpenseMap = remember(expensesList) {
        expensesList.groupBy { it.date / 86400000L }.mapValues { (_, expenses) ->
            DailyExpenseRow(
                dateLabel = "",
                dateTimestamp = expenses.first().date,
                syrups = expenses.sumOf { it.syrups },
                sauce = expenses.sumOf { it.sauce },
                milk = expenses.sumOf { it.milk },
                ice = expenses.sumOf { it.ice },
                others = expenses.sumOf { it.others },
                total = expenses.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others }
            )
        }
    }

    // KPI state loaded directly from repository (independent of filtered date range)
    var todayExpenses by remember { mutableDoubleStateOf(0.0) }
    var weekExpenses by remember { mutableDoubleStateOf(0.0) }
    var monthExpenses by remember { mutableDoubleStateOf(0.0) }
    var dailyAvg by remember { mutableDoubleStateOf(0.0) }
    var daysInMonth by remember { mutableIntStateOf(1) }

    LaunchedEffect(kpiRefreshKey) {
        val now = System.currentTimeMillis()
        val todayCal = Calendar.getInstance().apply { timeInMillis = now }
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
        val todayStart = todayCal.timeInMillis

        // Today
        val todayTotal = repository.getTotalExpensesByDateRange(todayStart, now)
        todayExpenses = todayTotal

        // This week (Monday to today)
        val weekCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        weekCal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = weekCal.timeInMillis
        val weekTotal = repository.getTotalExpensesByDateRange(weekStart, now)
        weekExpenses = weekTotal

        // This month
        val monthCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        monthCal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = monthCal.timeInMillis
        val monthTotal = repository.getTotalExpensesByDateRange(monthStart, now)
        monthExpenses = monthTotal

        val currentDayOfMonth = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH)
        daysInMonth = currentDayOfMonth
        dailyAvg = if (currentDayOfMonth > 0) monthTotal / currentDayOfMonth else 0.0
    }

    val cats = categoryTotals

    val ledgerRows = remember(expensesList) {
        dailyExpenseMap.entries.map { (_, row) ->
            row.copy(dateLabel = if (row.dateTimestamp > 0) dateFmt.format(Date(row.dateTimestamp)) else "Unknown")
        }.sortedByDescending { it.dateTimestamp }
    }

    val totalPages = maxOf((ledgerRows.size + itemsPerPage - 1) / itemsPerPage, 1)
    val safePage = minOf(currentPage, totalPages)
    val paginatedRows = ledgerRows.drop((safePage - 1) * itemsPerPage).take(itemsPerPage)

    val totalSyrups = ledgerRows.sumOf { it.syrups }
    val totalSauce = ledgerRows.sumOf { it.sauce }
    val totalMilk = ledgerRows.sumOf { it.milk }
    val totalIce = ledgerRows.sumOf { it.ice }
    val totalOthers = ledgerRows.sumOf { it.others }
    val totalAll = ledgerRows.sumOf { it.total }

    // Add Expense Dialog
    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onSaved = {
                showAddExpenseDialog = false
                onExpenseSaved()
                kpiRefreshKey++
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("📁 Expenses", style = MaterialTheme.typography.titleLarge, color = TextWhite, fontWeight = FontWeight.Bold)
        Text("Track and manage your daily expenses. These will be automatically deducted from sales to calculate profit.",
            style = MaterialTheme.typography.bodySmall, color = TextMuted)
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                views.forEach { v ->
                    val isActive = v == activeView
                    Text(
                        text = v,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = if (isActive) TextWhite else TextMuted,
                        modifier = Modifier
                            .clickable { activeView = v; currentPage = 1 }
                            .then(if (isActive) Modifier.background(OrangeAccent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)) else Modifier)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAddExpenseDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("+ Add Expense", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ExpensesKPICard("Total Expenses (Today)", "₱${String.format(Locale.US, "%,.2f", todayExpenses)}", StatusGreen, "▼ 8.3% vs Yesterday", Modifier.weight(1f))
            ExpensesKPICard("Total Expenses (This Week)", "₱${String.format(Locale.US, "%,.2f", weekExpenses)}", Color(0xFF2196F3), "▲ 12.6% vs Last Week", Modifier.weight(1f))
            ExpensesKPICard("Total Expenses (This Month)", "₱${String.format(Locale.US, "%,.2f", monthExpenses)}", Color(0xFF9C27B0), "▲ 9.8% vs Last Month", Modifier.weight(1f))
            ExpensesKPICard("Daily Average (${SimpleDateFormat("MMMM", Locale.US).format(Date())})", "₱${String.format(Locale.US, "%,.2f", dailyAvg)}", OrangeAccent, "Based on $daysInMonth days", Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DAILY EXPENSES BREAKDOWN (Today)", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExpenseDonutChart(categoryTotals = cats, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
            Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("DAILY EXPENSES OVER TIME", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Last 7 Days", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ExpenseLineChart(expensesList = expensesList, modifier = Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("DAILY EXPENSES RECORD", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text("DATE", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("SYRUPS (₱)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("SAUCE (₱)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("MILK (₱)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("ICE (₱)", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("OTHERS (₱)", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Text("TOTAL (₱)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                if (paginatedRows.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        Text("No expense entries yet. Use + Add Expense to start tracking.", style = MaterialTheme.typography.bodySmall, color = TextGray)
                    }
                } else {
                    paginatedRows.forEach { row ->
                        HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(row.dateLabel, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("₱${String.format(Locale.US, "%,.2f", row.syrups)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                            Text("₱${String.format(Locale.US, "%,.2f", row.sauce)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                            Text("₱${String.format(Locale.US, "%,.2f", row.milk)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                            Text("₱${String.format(Locale.US, "%,.2f", row.ice)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                            Text("₱${String.format(Locale.US, "%,.2f", row.others)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                            Text("₱${String.format(Locale.US, "%,.2f", row.total)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                            Text("⋮", modifier = Modifier.weight(0.5f), style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    }
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1.5f))
                    Text("₱${String.format(Locale.US, "%,.2f", totalSyrups)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", totalSauce)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", totalMilk)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", totalIce)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", totalOthers)}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                    Text("₱${String.format(Locale.US, "%,.2f", totalAll)}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(0.5f))
                }

                HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    val from = if (ledgerRows.isEmpty()) 0 else (safePage - 1) * itemsPerPage + 1
                    val to = minOf(safePage * itemsPerPage, ledgerRows.size)
                    Text("Showing $from to $to of ${ledgerRows.size} entries", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        PageButton("<", enabled = safePage > 1) { if (currentPage > 1) currentPage-- }
                        for (p in 1..totalPages) {
                            PageButton("$p", isActive = p == safePage, enabled = true) { currentPage = p }
                        }
                        PageButton(">", enabled = safePage < totalPages) { if (currentPage < totalPages) currentPage++ }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Add Expense Dialog
// ════════════════════════════════════════════════════════════════════

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository
    val scope = rememberCoroutineScope()

    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var syrups by remember { mutableStateOf("") }
    var sauce by remember { mutableStateOf("") }
    var milk by remember { mutableStateOf("") }
    var ice by remember { mutableStateOf("") }
    var others by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Add Expense", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dateFormat.format(Date(date)),
                    onValueChange = {},
                    label = { Text("Date", color = TextMuted) },
                    readOnly = true,
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = DarkBorder
                    )
                )
                OutlinedTextField(value = syrups, onValueChange = { syrups = it }, label = { Text("Syrups (₱)", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = OrangeAccent, unfocusedBorderColor = DarkBorder))
                OutlinedTextField(value = sauce, onValueChange = { sauce = it }, label = { Text("Sauce (₱)", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = OrangeAccent, unfocusedBorderColor = DarkBorder))
                OutlinedTextField(value = milk, onValueChange = { milk = it }, label = { Text("Milk (₱)", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = OrangeAccent, unfocusedBorderColor = DarkBorder))
                OutlinedTextField(value = ice, onValueChange = { ice = it }, label = { Text("Ice (₱)", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = OrangeAccent, unfocusedBorderColor = DarkBorder))
                OutlinedTextField(value = others, onValueChange = { others = it }, label = { Text("Others (₱)", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = OrangeAccent, unfocusedBorderColor = DarkBorder))
                OutlinedTextField(value = vendor, onValueChange = { vendor = it }, label = { Text("Vendor", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = OrangeAccent, unfocusedBorderColor = DarkBorder))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description", color = TextMuted) }, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextWhite, unfocusedTextColor = TextWhite, focusedBorderColor = OrangeAccent, unfocusedBorderColor = DarkBorder))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    saving = true
                    scope.launch {
                        repository.insertExpense(
                            ExpenseEntity(
                                date = date,
                                syrups = syrups.toDoubleOrNull() ?: 0.0,
                                sauce = sauce.toDoubleOrNull() ?: 0.0,
                                milk = milk.toDoubleOrNull() ?: 0.0,
                                ice = ice.toDoubleOrNull() ?: 0.0,
                                others = others.toDoubleOrNull() ?: 0.0,
                                vendor = vendor,
                                description = description
                            )
                        )
                        saving = false
                        onSaved()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                enabled = !saving
            ) { Text("Save", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}
