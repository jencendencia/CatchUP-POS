# Fix ExpensesTabContent in ReportsScreen.kt:
# 1. Remove duplicate calendar button
# 2. Fix KPI cards to load data directly from repository

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# ── Fix 1: Remove the duplicate calendar button (OutlinedButton with onClick = { /* Calendar */ }) ──
old_cal_button = """            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { /* Calendar */ },
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextWhite),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(SimpleDateFormat(\"MMMM dd, yyyy\", Locale.US).format(Date()), style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                Button("""

new_cal_button = """            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button("""

if old_cal_button in content:
    content = content.replace(old_cal_button, new_cal_button, 1)
    print("OK: Removed duplicate calendar button")
else:
    print("WARN: Could not find duplicate calendar button")

# ── Fix 2: Replace KPI computation to use repository directly ──
old_kpi_compute = """    val now = System.currentTimeMillis()
    val cal = Calendar.getInstance()
    cal.timeInMillis = now
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    val todayExpenses = expensesList.filter { it.date in todayStart..System.currentTimeMillis() }.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others }

    cal.add(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val weekStart = cal.timeInMillis
    val weekExpenses = expensesList.filter { it.date in weekStart..now }.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others }

    cal.timeInMillis = todayStart
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val monthStart = cal.timeInMillis
    val monthExpenses = expensesList.filter { it.date in monthStart..now }.sumOf { it.syrups + it.sauce + it.milk + it.ice + it.others }
    val daysInMonth = Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.DAY_OF_MONTH)
    val dailyAvg = monthExpenses / daysInMonth"""

new_kpi_compute = """    // KPI state loaded directly from repository (independent of filtered date range)
    var todayExpenses by remember { mutableDoubleStateOf(0.0) }
    var weekExpenses by remember { mutableDoubleStateOf(0.0) }
    var monthExpenses by remember { mutableDoubleStateOf(0.0) }
    var dailyAvg by remember { mutableDoubleStateOf(0.0) }
    var daysInMonth by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val todayCal = Calendar.getInstance().apply { timeInMillis = now }
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0)
        val todayStart = todayCal.timeInMillis

        val yCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        yCal.add(Calendar.DAY_OF_MONTH, -1)
        val yStart = yCal.timeInMillis
        yCal.set(Calendar.HOUR_OF_DAY, 23); yCal.set(Calendar.MINUTE, 59)
        yCal.set(Calendar.SECOND, 59); yCal.set(Calendar.MILLISECOND, 999)
        val yEnd = yCal.timeInMillis

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
    }"""

if old_kpi_compute in content:
    content = content.replace(old_kpi_compute, new_kpi_compute, 1)
    print("OK: Replaced KPI computation with repository-backed state")
else:
    print("WARN: Could not find old KPI computation")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
