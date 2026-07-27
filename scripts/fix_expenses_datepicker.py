# Add calendar-based DateRangePickerDialog to ExpensesScreen.kt

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ExpensesScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add state variables after the existing states
old_states = """    // Data states
    var expensesList by remember { mutableStateOf<List<ExpenseEntity>>(emptyList()) }
    var categoryTotals by remember { mutableStateOf<ExpenseCategoryTotals?>(null) }
    var kpiData by remember { mutableStateOf(KPIData()) }
    var refreshCounter by remember { mutableIntStateOf(0) }"""

new_states = """    // Date range state
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
    var refreshCounter by remember { mutableIntStateOf(0) }"""

if old_states in content:
    content = content.replace(old_states, new_states, 1)
    print("OK: Added date range state variables")
else:
    print("WARN: Could not find state section")

# 2. Update LaunchedEffect to use selected date range
old_launch = """    // Load data
    LaunchedEffect(refreshCounter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayEnd = now

        // Yesterday
        val yCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        yCal.add(Calendar.DAY_OF_MONTH, -1)
        val yStart = yCal.timeInMillis
        yCal.set(Calendar.HOUR_OF_DAY, 23); yCal.set(Calendar.MINUTE, 59)
        yCal.set(Calendar.SECOND, 59); yCal.set(Calendar.MILLISECOND, 999)
        val yEnd = yCal.timeInMillis

        expensesList = repository.getExpensesByDateRange(todayStart, todayEnd)
        categoryTotals = repository.getExpenseCategoryTotals(todayStart, todayEnd)"""

new_launch = """    // Load data
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

        val yesterdayCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        yesterdayCal.add(Calendar.DAY_OF_MONTH, -1)
        val yStart = yesterdayCal.timeInMillis
        yesterdayCal.set(Calendar.HOUR_OF_DAY, 23); yesterdayCal.set(Calendar.MINUTE, 59)
        yesterdayCal.set(Calendar.SECOND, 59); yesterdayCal.set(Calendar.MILLISECOND, 999)
        val yEnd = yesterdayCal.timeInMillis

        expensesList = repository.getExpensesByDateRange(startTime, endTime)
        categoryTotals = repository.getExpenseCategoryTotals(startTime, endTime)"""

if old_launch in content:
    content = content.replace(old_launch, new_launch, 1)
    print("OK: Updated LaunchedEffect for selected date range")
else:
    print("WARN: Could not find LaunchedEffect")

# 3. Add date button before ExpensesTabContent and the DateRangePickerDialog after the Column
# The Column starts with:
old_column = """    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(28.dp)
    ) {
        ExpensesTabContent("""

new_column = """    Column(
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
        ExpensesTabContent("""

if old_column in content:
    content = content.replace(old_column, new_column, 1)
    print("OK: Added date button and title")
else:
    print("WARN: Could not find Column start")

# 4. Add DateRangePickerDialog call after the Column closes
# Find the closing of the Column + closing of the function
old_end = """        ExpensesTabContent(
            expensesList = expensesList,
            categoryTotals = categoryTotals,
            kpiData = kpiData,
            onExpenseSaved = { refreshCounter++ }
        )
    }
}"""

new_end = """        ExpensesTabContent(
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
}"""

if old_end in content:
    content = content.replace(old_end, new_end, 1)
    print("OK: Added DateRangePickerDialog and computeDateRange helpers")
else:
    print("WARN: Could not find file end")

# 5. Add missing imports for Icons and other Material components
# The file only has limited imports - need to add icon and layout imports
old_imports = """import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp"""

new_imports = """import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.dp"""

if old_imports in content:
    content = content.replace(old_imports, new_imports, 1)
    print("OK: Added missing imports")
else:
    print("WARN: Could not find imports section")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ExpensesScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
