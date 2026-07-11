package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.catchuppos.app.CatchUpApp
import com.catchuppos.app.data.*
import com.catchuppos.app.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ProfitScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository

    // ── Date / filter state ──
    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }
    var selectedDateRange by remember { mutableStateOf("Today") }
    var selectedDateLabel by remember { mutableStateOf("Today, ${dateFormat.format(Date())}") }
    var customStartDate by remember { mutableStateOf(0L) }
    var customEndDate by remember { mutableStateOf(0L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf("All") }

    // ── Data states ──
    var dailySales by remember { mutableStateOf<List<DailySalesSummary>>(emptyList()) }
    var kpiData by remember { mutableStateOf(KPIData()) }
    var itemsSold by remember { mutableIntStateOf(0) }
    var realExpenses by remember { mutableDoubleStateOf(0.0) }
    var perDayExpenses by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }
    var perDayItemsSold by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }
    var allTransactions by remember { mutableStateOf<List<TransactionEntity>>(emptyList()) }

    // ── Load data reactively when date range or filter changes ──
    LaunchedEffect(selectedDateRange, activeFilter, customStartDate, customEndDate) {
        val (startTime, endTime) = if (selectedDateRange == "Custom Range") {
            customStartDate to customEndDate
        } else {
            computeDateRange(selectedDateRange)
        }

        // Daily sales (for ledger and charts)
        dailySales = repository.getDailySales(startTime, endTime)

        // Items sold in period
        itemsSold = repository.getItemsSoldByDateRange(startTime, endTime)

        // Total expenses in period
        realExpenses = repository.getTotalExpensesByDateRange(startTime, endTime)

        // Per-day expenses grouped by dayOffset (for ledger table)
        val expensesInRange = repository.getExpensesByDateRangeAsc(startTime, endTime)
        perDayExpenses = expensesInRange
            .groupBy { it.date / 86400000L }
            .mapValues { (_, list) -> list.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others } }

        // Per-day items sold grouped by dayOffset (for ledger table)
        val itemsInRange = repository.getDailyItemsSold(startTime, endTime)
        perDayItemsSold = itemsInRange.associate { it.dayOffset to it.totalItems }

        // Transactions for the period
        allTransactions = repository.getTransactionsByDateRange(startTime, endTime)

        // KPIs
        val currentSales = repository.getSalesTotalByDateRange(startTime, endTime)
        val currentOrders = repository.getOrdersCountByDateRange(startTime, endTime)
        val currentItems = repository.getItemsSoldByDateRange(startTime, endTime)
        val currentAvg = if (currentOrders > 0) currentSales / currentOrders else 0.0

        // Yesterday for comparison
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

        val yesterdaySales = repository.getSalesTotalByDateRange(yesterdayStart, yesterdayEnd)
        val yesterdayOrders = repository.getOrdersCountByDateRange(yesterdayStart, yesterdayEnd)
        val yesterdayItems = repository.getItemsSoldByDateRange(yesterdayStart, yesterdayEnd)
        val yesterdayAvg = if (yesterdayOrders > 0) yesterdaySales / yesterdayOrders else 0.0

        fun calcChange(cur: Double, prev: Double) = if (prev > 0) ((cur - prev) / prev) * 100.0 else 0.0

        kpiData = KPIData(
            totalSales = currentSales,
            totalOrders = currentOrders,
            avgOrderValue = currentAvg,
            totalItemsSold = currentItems,
            grossProfit = currentSales * 0.5,
            salesChange = calcChange(currentSales, yesterdaySales),
            ordersChange = calcChange(currentOrders.toDouble(), yesterdayOrders.toDouble()),
            avgOrderChange = calcChange(currentAvg, yesterdayAvg),
            itemsChange = calcChange(currentItems.toDouble(), yesterdayItems.toDouble()),
            profitChange = calcChange(currentSales * 0.5, yesterdaySales * 0.5)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        ProfitTabContent(
            dailySales = dailySales,
            kpiData = kpiData,
            itemsSold = itemsSold,
            realExpenses = realExpenses,
            perDayExpenses = perDayExpenses,
            perDayItemsSold = perDayItemsSold,
            allTransactions = allTransactions,
            selectedDateLabel = selectedDateLabel,
            onDateClick = { showDatePicker = true },
            onFilterClick = { showFilterDialog = true },
            onGranularityChange = { g ->
                when (g) {
                    "Daily" -> {
                        selectedDateRange = "Today"
                        selectedDateLabel = "Today, " + dateFormat.format(Date())
                    }
                    "Weekly" -> {
                        selectedDateRange = "This Week"
                        selectedDateLabel = "This Week, " + dateFormat.format(Date())
                    }
                    "Monthly" -> {
                        selectedDateRange = "This Month"
                        selectedDateLabel = "This Month, " + dateFormat.format(Date())
                    }
                }
                customStartDate = 0L
                customEndDate = 0L
            },
            activeFilter = activeFilter
        )
    }

    // ── Date Range Dialog (Calendar-based) ──
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onApply = { startMillis, endMillis ->
                if (startMillis != null && endMillis != null) {
                    // Convert UTC midnight to local time day boundaries
                    val cal = Calendar.getInstance()
                    // Start of the selected start day
                    cal.timeInMillis = startMillis
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startDay = cal.timeInMillis
                    // End of the selected end day
                    cal.timeInMillis = endMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val endDay = cal.timeInMillis

                    customStartDate = startDay
                    customEndDate = endDay
                    selectedDateRange = "Custom Range"
                    selectedDateLabel = "${dateFormat.format(Date(startDay))} - ${dateFormat.format(Date(endDay))}"
                }
                showDatePicker = false
            }
        )
    }

    // ── Filter Dialog ──
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = activeFilter,
            onDismiss = { showFilterDialog = false },
            onApply = { filter ->
                activeFilter = filter
                showFilterDialog = false
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
// Date Range Picker Dialog (Calendar-based)
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onApply: (startMillis: Long?, endMillis: Long?) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    // Use AlertDialog to ensure buttons are always visible and properly themed
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
// Filter Dialog
// ════════════════════════════════════════════════════════════════════

@Composable
private fun FilterDialog(
    currentFilter: String,
    onDismiss: () -> Unit,
    onApply: (filter: String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(currentFilter) }
    val filters = listOf("All", "Profitable Only", "Loss Making Only", "With Expenses Only")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Apply Filter", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("View entries that match:", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                filters.forEach { filter ->
                    val isSelected = filter == selectedFilter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) OrangeAccent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { selectedFilter = filter }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            filter,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) OrangeAccent else TextWhite,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(selectedFilter) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}
