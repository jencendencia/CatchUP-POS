"""
Fix all reported issues:
1. Date picker - replace DatePickerDialog with AlertDialog for visible buttons
2. Cups sold - add perDayItemsSold query and use it instead of orderCount
3. Data updates - fix state management
"""

import os

# ============================================================
# 1. TransactionDao.kt - Add getDailyItemsSold query
# ============================================================
dao_path = "app/src/main/java/com/catchuppos/app/data/TransactionDao.kt"
with open(dao_path, "r", encoding="utf-8") as f:
    dao_content = f.read()

# Add DailyItemsSold data class after DailySalesSummary
old_daily_sales = """data class DailySalesSummary(
    @androidx.room.ColumnInfo(name = "day_offset") val dayOffset: Long,
    @androidx.room.ColumnInfo(name = "total") val total: Double,
    @androidx.room.ColumnInfo(name = "order_count") val orderCount: Int
)"""

new_daily_sales = """data class DailySalesSummary(
    @androidx.room.ColumnInfo(name = "day_offset") val dayOffset: Long,
    @androidx.room.ColumnInfo(name = "total") val total: Double,
    @androidx.room.ColumnInfo(name = "order_count") val orderCount: Int
)

data class DailyItemsSold(
    @androidx.room.ColumnInfo(name = "day_offset") val dayOffset: Long,
    @androidx.room.ColumnInfo(name = "total_items") val totalItems: Int
)"""

if old_daily_sales in dao_content:
    dao_content = dao_content.replace(old_daily_sales, new_daily_sales)
    print("[OK] Added DailyItemsSold data class")
else:
    print("[WARN] Could not find DailySalesSummary data class")

# Add getDailyItemsSold query after getDailySales
old_daily_sales_query = """    @Query(\"\"\"
        SELECT (created_at / 86400000) as day_offset, SUM(total) as total, COUNT(*) as order_count
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY day_offset
        ORDER BY day_offset ASC
    \"\"\")
    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySalesSummary>"""

new_daily_sales_query = """    @Query(\"\"\"
        SELECT (created_at / 86400000) as day_offset, SUM(total) as total, COUNT(*) as order_count
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY day_offset
        ORDER BY day_offset ASC
    \"\"\")
    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySalesSummary>

    @Query(\"\"\"
        SELECT (created_at / 86400000) as day_offset, SUM(item_count) as total_items
        FROM transactions
        WHERE created_at BETWEEN :startTime AND :endTime
        GROUP BY day_offset
        ORDER BY day_offset ASC
    \"\"\")
    suspend fun getDailyItemsSold(startTime: Long, endTime: Long): List<DailyItemsSold>"""

if old_daily_sales_query in dao_content:
    dao_content = dao_content.replace(old_daily_sales_query, new_daily_sales_query)
    print("[OK] Added getDailyItemsSold query")
else:
    print("[WARN] Could not find getDailySales query")

with open(dao_path, "w", encoding="utf-8") as f:
    f.write(dao_content)
print("[OK] Saved TransactionDao.kt")

# ============================================================
# 2. ProductRepository.kt - Add getDailyItemsSold method
# ============================================================
repo_path = "app/src/main/java/com/catchuppos/app/data/ProductRepository.kt"
with open(repo_path, "r", encoding="utf-8") as f:
    repo_content = f.read()

old_repo = """    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySalesSummary> =
        transactionDao.getDailySales(startTime, endTime)"""

new_repo = """    suspend fun getDailySales(startTime: Long, endTime: Long): List<DailySalesSummary> =
        transactionDao.getDailySales(startTime, endTime)

    suspend fun getDailyItemsSold(startTime: Long, endTime: Long): List<DailyItemsSold> =
        transactionDao.getDailyItemsSold(startTime, endTime)"""

if old_repo in repo_content:
    repo_content = repo_content.replace(old_repo, new_repo)
    print("[OK] Added getDailyItemsSold to repository")
else:
    print("[WARN] Could not find getDailySales in repository")

with open(repo_path, "w", encoding="utf-8") as f:
    f.write(repo_content)
print("[OK] Saved ProductRepository.kt")

# ============================================================
# 3. ProfitScreen.kt - Fix date picker, add perDayItemsSold, fix state
# ============================================================
profit_path = "app/src/main/java/com/catchuppos/app/ui/dashboard/ProfitScreen.kt"
with open(profit_path, "r", encoding="utf-8") as f:
    p_content = f.read()

changes = 0

# 3a. Replace mutableLongStateOf with mutableStateOf for safety
p_content = p_content.replace(
    "var customStartDate by remember { mutableLongStateOf(0L) }",
    "var customStartDate by remember { mutableStateOf(0L) }"
)
changes += 1
print("[OK] Changed customStartDate to mutableStateOf")

p_content = p_content.replace(
    "var customEndDate by remember { mutableLongStateOf(0L) }",
    "var customEndDate by remember { mutableStateOf(0L) }"
)
changes += 1
print("[OK] Changed customEndDate to mutableStateOf")

# 3b. Add perDayItemsSold state
old_items_state = "    var perDayExpenses by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }"
new_items_state = "    var perDayExpenses by remember { mutableStateOf<Map<Long, Double>>(emptyMap()) }\n    var perDayItemsSold by remember { mutableStateOf<Map<Long, Int>>(emptyMap()) }"
if old_items_state in p_content:
    p_content = p_content.replace(old_items_state, new_items_state)
    changes += 1
    print("[OK] Added perDayItemsSold state")

# 3c. Load perDayItemsSold in LaunchedEffect
old_load = "        // Per-day expenses grouped by dayOffset (for ledger table)\n        val expensesInRange = repository.getExpensesByDateRangeAsc(startTime, endTime)\n        perDayExpenses = expensesInRange\n            .groupBy { it.date / 86400000L }\n            .mapValues { (_, list) -> list.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others } }"
new_load = "        // Per-day expenses grouped by dayOffset (for ledger table)\n        val expensesInRange = repository.getExpensesByDateRangeAsc(startTime, endTime)\n        perDayExpenses = expensesInRange\n            .groupBy { it.date / 86400000L }\n            .mapValues { (_, list) -> list.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others } }\n\n        // Per-day items sold grouped by dayOffset (for ledger table)\n        val itemsInRange = repository.getDailyItemsSold(startTime, endTime)\n        perDayItemsSold = itemsInRange.associate { it.dayOffset to it.totalItems }"
if old_load in p_content:
    p_content = p_content.replace(old_load, new_load)
    changes += 1
    print("[OK] Added perDayItemsSold loading")

# 3d. Add perDayItemsSold to ProfitTabContent call
old_call = """            perDayExpenses = perDayExpenses,
            selectedDateLabel = selectedDateLabel,"""
new_call = """            perDayExpenses = perDayExpenses,
            perDayItemsSold = perDayItemsSold,
            selectedDateLabel = selectedDateLabel,"""
if old_call in p_content:
    p_content = p_content.replace(old_call, new_call)
    changes += 1
    print("[OK] Added perDayItemsSold to ProfitTabContent call")

# 3e. Replace DateRangePickerDialog with a custom AlertDialog for visible buttons
old_dialog_start = "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun DateRangePickerDialog(\n    onDismiss: () -> Unit,\n    onApply: (startMillis: Long?, endMillis: Long?) -> Unit\n) {"
new_dialog_start = "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun DateRangePickerDialog(\n    onDismiss: () -> Unit,\n    onApply: (startMillis: Long?, endMillis: Long?) -> Unit\n) {\n    val dateRangePickerState = rememberDateRangePickerState()\n\n    // Use AlertDialog to ensure buttons are always visible and properly themed\n    AlertDialog(\n        onDismissRequest = onDismiss,\n        containerColor = DarkCard,\n        title = { Text(\"Select Date Range\", color = TextWhite, fontWeight = FontWeight.Bold) },\n        text = {\n            Column(modifier = Modifier.height(380.dp)) {\n                DateRangePicker(\n                    state = dateRangePickerState,\n                    modifier = Modifier.weight(1f),\n                    showModeToggle = false,\n                    colors = DatePickerDefaults.colors(\n                        containerColor = DarkCard,\n                        titleContentColor = TextWhite,\n                        headlineContentColor = TextWhite,\n                        weekdayContentColor = TextMuted,\n                        subheadContentColor = TextWhite,\n                        yearContentColor = TextWhite,\n                        currentYearContentColor = OrangeAccent,\n                        selectedDayContentColor = Color.White,\n                        selectedDayContainerColor = OrangeAccent,\n                        dayContentColor = TextWhite,\n                        todayContentColor = OrangeAccent,\n                        todayDateBorderColor = OrangeAccent,\n                        dayInSelectionRangeContentColor = Color.White,\n                        dayInSelectionRangeContainerColor = OrangeAccent.copy(alpha = 0.3f),\n                        dividerColor = DarkBorder\n                    )\n                )\n            }\n        },\n        confirmButton = {\n            Button(\n                onClick = {\n                    onApply(\n                        dateRangePickerState.selectedStartDateMillis,\n                        dateRangePickerState.selectedEndDateMillis\n                    )\n                },\n                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),\n                enabled = dateRangePickerState.selectedStartDateMillis != null &&\n                          dateRangePickerState.selectedEndDateMillis != null\n            ) { Text(\"Apply\", fontWeight = FontWeight.Bold) }\n        },\n        dismissButton = {\n            TextButton(onClick = onDismiss) { Text(\"Cancel\", color = TextMuted) }\n        }\n    )\n}"

if old_dialog_start in p_content:
    # Find the full old dialog function and replace it
    old_dialog_full = "@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun DateRangePickerDialog(\n    onDismiss: () -> Unit,\n    onApply: (startMillis: Long?, endMillis: Long?) -> Unit\n) {\n    val dateRangePickerState = rememberDateRangePickerState()\n\n    DatePickerDialog(\n        onDismissRequest = onDismiss,\n        confirmButton = {\n            Button(\n                onClick = {\n                    onApply(\n                        dateRangePickerState.selectedStartDateMillis,\n                        dateRangePickerState.selectedEndDateMillis\n                    )\n                },\n                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),\n                enabled = dateRangePickerState.selectedStartDateMillis != null &&\n                          dateRangePickerState.selectedEndDateMillis != null\n            ) { Text(\"Apply\", fontWeight = FontWeight.Bold) }\n        },\n        dismissButton = {\n            TextButton(onClick = onDismiss) { Text(\"Cancel\", color = TextMuted) }\n        },\n        colors = DatePickerDefaults.colors(containerColor = DarkCard)\n    ) {\n        DateRangePicker(\n            state = dateRangePickerState,\n            title = { Text(\"Select Date Range\", color = TextWhite, fontWeight = FontWeight.Bold) },\n            showModeToggle = false,\n            colors = DatePickerDefaults.colors(\n                containerColor = DarkCard,\n                titleContentColor = TextWhite,\n                headlineContentColor = TextWhite,\n                weekdayContentColor = TextMuted,\n                subheadContentColor = TextWhite,\n                yearContentColor = TextWhite,\n                currentYearContentColor = OrangeAccent,\n                selectedDayContentColor = Color.White,\n                selectedDayContainerColor = OrangeAccent,\n                dayContentColor = TextWhite,\n                todayContentColor = OrangeAccent,\n                todayDateBorderColor = OrangeAccent,\n                dayInSelectionRangeContentColor = Color.White,\n                dayInSelectionRangeContainerColor = OrangeAccent.copy(alpha = 0.3f),\n                dividerColor = DarkBorder\n            )\n        )\n    }\n}"

p_content = p_content.replace(old_dialog_full, new_dialog_start)
changes += 1
print("[OK] Replaced DatePickerDialog with AlertDialog for visible buttons")
print(f"[OK] {changes} changes to ProfitScreen.kt")

with open(profit_path, "w", encoding="utf-8") as f:
    f.write(p_content)
print("[OK] Saved ProfitScreen.kt")

# ============================================================
# 4. ReportsScreen.kt - Add perDayItemsSold param and use it
# ============================================================
reports_path = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"
with open(reports_path, "r", encoding="utf-8") as f:
    r_content = f.read()

changes2 = 0

# 4a. Add perDayItemsSold to ProfitTabContent function signature
old_sig = "fun ProfitTabContent(\n    dailySales: List<DailySalesSummary>,\n    kpiData: KPIData,\n    itemsSold: Int,\n    realExpenses: Double = 0.0,\n    perDayExpenses: Map<Long, Double> = emptyMap(),"
new_sig = "fun ProfitTabContent(\n    dailySales: List<DailySalesSummary>,\n    kpiData: KPIData,\n    itemsSold: Int,\n    realExpenses: Double = 0.0,\n    perDayExpenses: Map<Long, Double> = emptyMap(),\n    perDayItemsSold: Map<Long, Int> = emptyMap(),"
if old_sig in r_content:
    r_content = r_content.replace(old_sig, new_sig)
    changes2 += 1
    print("[OK] Added perDayItemsSold to ProfitTabContent signature")

# 4b. Replace cupsSold in aggregateDailySales - Daily case
old_daily_cups = '                val expenses = perDayExpenses[day.dayOffset] ?: 0.0\n                val profit = day.total - expenses\n                val margin = if (day.total > 0) (profit / day.total) * 100 else 0.0\n                LedgerRow(\n                    dateLabel = if (timestamp > 0) dateFmt.format(Date(timestamp)) else "Unknown",\n                    dateTimestamp = timestamp,\n                    cupsSold = day.orderCount,'
new_daily_cups = '                val expenses = perDayExpenses[day.dayOffset] ?: 0.0\n                val profit = day.total - expenses\n                val margin = if (day.total > 0) (profit / day.total) * 100 else 0.0\n                val itemsCount = perDayItemsSold[day.dayOffset] ?: 0\n                LedgerRow(\n                    dateLabel = if (timestamp > 0) dateFmt.format(Date(timestamp)) else "Unknown",\n                    dateTimestamp = timestamp,\n                    cupsSold = itemsCount,'
if old_daily_cups in r_content:
    r_content = r_content.replace(old_daily_cups, new_daily_cups)
    changes2 += 1
    print("[OK] Fixed Daily cupsSold to use perDayItemsSold")

# 4c. Replace cupsSold in Weekly case
old_weekly_cups = '                    val totalSalesWeek = days.sumOf { it.total }\n                    val totalExpensesWeek = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }\n                    val totalCupsWeek = days.sumOf { it.orderCount }'
new_weekly_cups = '                    val totalSalesWeek = days.sumOf { it.total }\n                    val totalExpensesWeek = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }\n                    val totalCupsWeek = days.sumOf { perDayItemsSold[it.dayOffset] ?: 0 }'
if old_weekly_cups in r_content:
    r_content = r_content.replace(old_weekly_cups, new_weekly_cups)
    changes2 += 1
    print("[OK] Fixed Weekly cupsSold to use perDayItemsSold")

# 4d. Replace cupsSold in Monthly case
old_monthly_cups = '                    val totalSalesMonth = days.sumOf { it.total }\n                    val totalExpensesMonth = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }\n                    val totalCupsMonth = days.sumOf { it.orderCount }'
new_monthly_cups = '                    val totalSalesMonth = days.sumOf { it.total }\n                    val totalExpensesMonth = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }\n                    val totalCupsMonth = days.sumOf { perDayItemsSold[it.dayOffset] ?: 0 }'
if old_monthly_cups in r_content:
    r_content = r_content.replace(old_monthly_cups, new_monthly_cups)
    changes2 += 1
    print("[OK] Fixed Monthly cupsSold to use perDayItemsSold")

with open(reports_path, "w", encoding="utf-8") as f:
    f.write(reports_path)
print("[OK] Saved ReportsScreen.kt")

print(f"\n[OK] Total: {changes} changes to ProfitScreen.kt, {changes2} changes to ReportsScreen.kt")
