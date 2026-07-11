package com.catchuppos.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
fun ExpensesScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as CatchUpApp
    val repository = app.productRepository

    // Date range state
    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }
    var selectedDateRange by remember { mutableStateOf("Today") }
    var selectedDateLabel by remember { mutableStateOf("Today, ${dateFormat.format(Date())}") }
    var showDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf(0L) }
    var customEndDate by remember { mutableStateOf(0L) }

    // Data states
    var expensesList by remember { mutableStateOf<List<ExpenseEntity>>(emptyList()) }
    var categoryTotals by remember { mutableStateOf<ExpenseCategoryTotals?>(null) }
    var kpiData by remember { mutableStateOf(KPIData()) }
    var refreshCounter by remember { mutableIntStateOf(0) }

    // Load data
    LaunchedEffect(refreshCounter, selectedDateRange, customStartDate, customEndDate) {
        val (startTime, endTime) = if (selectedDateRange == "Custom Range") {
            customStartDate to customEndDate
        } else {
            computeDateRange(selectedDateRange)
        }

        val now = System.currentTimeMillis()
        val todayCal = Calendar.getInstance().apply { timeInMillis = now }
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
        val todayStart = todayCal.timeInMillis
        val todayEnd = now

        val yesterdayCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        yesterdayCal.add(Calendar.DAY_OF_MONTH, -1)
        val yStart = yesterdayCal.timeInMillis
        yesterdayCal.set(Calendar.HOUR_OF_DAY, 23); yesterdayCal.set(Calendar.MINUTE, 59)
        yesterdayCal.set(Calendar.SECOND, 59); yesterdayCal.set(Calendar.MILLISECOND, 999)
        val yEnd = yesterdayCal.timeInMillis

        expensesList = repository.getExpensesByDateRange(startTime, endTime)
        categoryTotals = repository.getExpenseCategoryTotals(startTime, endTime)

        // Compute KPIs
        val currentSales = repository.getSalesTotalByDateRange(todayStart, todayEnd)
        val currentOrders = repository.getOrdersCountByDateRange(todayStart, todayEnd)
        val currentItems = repository.getItemsSoldByDateRange(todayStart, todayEnd)
        val currentAvg = if (currentOrders > 0) currentSales / currentOrders else 0.0

        val yesterdaySales = repository.getSalesTotalByDateRange(yStart, yEnd)
        val yesterdayOrders = repository.getOrdersCountByDateRange(yStart, yEnd)
        val yesterdayItems = repository.getItemsSoldByDateRange(yStart, yEnd)
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
        // ── Date Range Button ──
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("EXPENSES", style = MaterialTheme.typography.headlineSmall, color = TextWhite, fontWeight = FontWeight.Bold)
            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextWhite),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(selectedDateLabel, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ExpensesTabContent(
            expensesList = expensesList,
            categoryTotals = categoryTotals,
            kpiData = kpiData,
            onExpenseSaved = { refreshCounter++ }
        )
    }

    // ── Date Range Picker Dialog ──
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onApply = { startMillis, endMillis ->
                if (startMillis != null && endMillis != null) {
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
                    selectedDateLabel = "${dateFormat.format(Date(startDay))} - ${dateFormat.format(Date(endDay))}"
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
// Date Range Picker Dialog (Calendar-based)
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
