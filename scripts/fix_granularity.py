"""Make granularity toggle aggregate the ledger table and KPI data by day/week/month."""

filepath = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# The current code from "// Compute derived profit data" to the end of the ledgerRows block
# I need to replace the section that computes totalSales, totalExpenses, etc. and the ledgerRows

# Find the section to replace
old_start = """    // Compute derived profit data
    val totalSales = dailySales.sumOf { it.total }
    val totalExpenses = realExpenses
    val netProfit = totalSales - totalExpenses
    val profitMargin = if (totalSales > 0) (netProfit / totalSales) * 100 else 0.0
    val totalCups = itemsSold

    // Build ledger rows from daily sales data
    data class LedgerRow(
        val dateLabel: String,
        val dateTimestamp: Long,
        val cupsSold: Int,
        val sales: Double,
        val expenses: Double,
        val profit: Double,
        val margin: Double
    )

    val dateFmt = remember { SimpleDateFormat(\"MMMM dd, yyyy (EEE)\", Locale.US) }
    val allLedgerRows = remember(dailySales, perDayExpenses) {
        dailySales.sortedByDescending { it.dayOffset }.map { day ->
            val timestamp = day.dayOffset * 86400000L
            val expenses = perDayExpenses[day.dayOffset] ?: 0.0
            val profit = day.total - expenses
            val margin = if (day.total > 0) (profit / day.total) * 100 else 0.0
            LedgerRow(
                dateLabel = if (timestamp > 0) dateFmt.format(Date(timestamp)) else \"Unknown\",
                dateTimestamp = timestamp,
                cupsSold = day.orderCount,
                sales = day.total,
                expenses = expenses,
                profit = profit,
                margin = margin
            )
        }
    }
    val ledgerRows = remember(allLedgerRows, activeFilter) {
        when (activeFilter) {
            \"Profitable Only\" -> allLedgerRows.filter { it.profit > 0 }
            \"Loss Making Only\" -> allLedgerRows.filter { it.profit <= 0 }
            \"With Expenses Only\" -> allLedgerRows.filter { it.expenses > 0 }
            else -> allLedgerRows
        }
    }"""

new_code = """    // ── Aggregation helpers ──
    data class LedgerRow(
        val dateLabel: String,
        val dateTimestamp: Long,
        val cupsSold: Int,
        val sales: Double,
        val expenses: Double,
        val profit: Double,
        val margin: Double
    )

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
                LedgerRow(
                    dateLabel = if (timestamp > 0) dateFmt.format(Date(timestamp)) else "Unknown",
                    dateTimestamp = timestamp,
                    cupsSold = day.orderCount,
                    sales = day.total,
                    expenses = expenses,
                    profit = profit,
                    margin = margin
                )
            }
            "Weekly" -> {
                // Group by ISO week (dayOffset / 7 gives week number since epoch)
                sorted.groupBy { it.dayOffset / 7 }.map { (weekOffset, days) ->
                    val weekStartTimestamp = weekOffset * 7 * 86400000L
                    val weekEndTimestamp = (weekOffset * 7 + 6) * 86400000L
                    val weekMidTimestamp = (weekStartTimestamp + weekEndTimestamp) / 2
                    val totalSalesWeek = days.sumOf { it.total }
                    val totalExpensesWeek = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }
                    val totalCupsWeek = days.sumOf { it.orderCount }
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
                // Group by month: convert dayOffset to Calendar and extract year+month
                sorted.groupBy { day ->
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = day.dayOffset * 86400000L
                    }
                    cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                }.map { (yearMonth, days) ->
                    val firstDay = days.minBy { it.dayOffset }
                    val timestamp = firstDay.dayOffset * 86400000L
                    val totalSalesMonth = days.sumOf { it.total }
                    val totalExpensesMonth = days.sumOf { perDayExpenses[it.dayOffset] ?: 0.0 }
                    val totalCupsMonth = days.sumOf { it.orderCount }
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

    val allLedgerRows = remember(dailySales, perDayExpenses, granularity) {
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

    // Compute derived profit data from aggregated rows (top of the list = current period)
    val aggregatedSales = allLedgerRows.sumOf { it.sales }
    val aggregatedExpenses = allLedgerRows.sumOf { it.expenses }
    val aggregatedNetProfit = aggregatedSales - aggregatedExpenses
    val aggregatedProfitMargin = if (aggregatedSales > 0) (aggregatedNetProfit / aggregatedSales) * 100 else 0.0
    val aggregatedCups = allLedgerRows.sumOf { it.cupsSold }"""

if old_start in content:
    content = content.replace(old_start, new_code)
    print("[OK] Replaced aggregation logic with granularity-aware computation")
else:
    print("[WARN] Could not find the expected code block")
    # Debug: search for key phrases
    for phrase in ["totalSales = dailySales.sumOf", "data class LedgerRow", "val dateFmt = remember"]:
        idx = content.find(phrase)
        if idx >= 0:
            print(f"  Found '{phrase}' at position {idx}")
        else:
            print(f"  NOT found '{phrase}'")

# Now update the KPI card values to use aggregated variables instead of old ones
# Replace references in KPI cards
old_kpi_refs = [
    ("totalSales", "aggregatedSales"),
    ("totalExpenses", "aggregatedExpenses"),
    ("netProfit", "aggregatedNetProfit"),
    ("profitMargin", "aggregatedProfitMargin"),
    ("totalCups", "aggregatedCups"),
]

# These variables are used in the KPI card section
# The old code had: totalSales, totalExpenses, netProfit, profitMargin, totalCups
# The new code has: aggregatedSales, aggregatedExpenses, aggregatedNetProfit, etc.

# But the KPI card code still references the old variable names.
# Since we renamed them to aggregated*, we need to update the KPI card section.

# Actually, looking at the original code more carefully, the KPI cards use:
# totalSales, totalExpenses, netProfit, profitMargin, totalCups
# and also kpiData.salesChange, kpiData.profitChange, kpiData.itemsChange
# The ProfitBreakdownDonut also uses totalSales, totalExpenses
# The netProfit variable is used for the center of donut

# Since I renamed the variables, I need to find and replace references.
# But the simpler approach is to keep the original variable names.
# Let me change the strategy: keep the old names but compute from aggregated data.

# Actually, looking at the code, the issue is that the old variables (totalSales etc.)
# are used in the KPI cards AND in the ProfitBreakdownDonut AND in the footer.
# If I change them to aggregated versions, everything downstream will use aggregated data.
# Let me just use the old variable names but assign them from aggregated data.

# Let me fix by keeping the old variable names assigned from aggregated data.
# Replacing the aggregated section to use old names

old_agg = """    // Compute derived profit data from aggregated rows (top of the list = current period)
    val aggregatedSales = allLedgerRows.sumOf { it.sales }
    val aggregatedExpenses = allLedgerRows.sumOf { it.expenses }
    val aggregatedNetProfit = aggregatedSales - aggregatedExpenses
    val aggregatedProfitMargin = if (aggregatedSales > 0) (aggregatedNetProfit / aggregatedSales) * 100 else 0.0
    val aggregatedCups = allLedgerRows.sumOf { it.cupsSold }"""

new_agg = """    // Compute derived profit data from aggregated rows
    val totalSales = allLedgerRows.sumOf { it.sales }
    val totalExpenses = allLedgerRows.sumOf { it.expenses }
    val netProfit = totalSales - totalExpenses
    val profitMargin = if (totalSales > 0) (netProfit / totalSales) * 100 else 0.0
    val totalCups = allLedgerRows.sumOf { it.cupsSold }"""

if old_agg in content:
    content = content.replace(old_agg, new_agg)
    print("[OK] Switched to old variable names with aggregated data")
else:
    print("[WARN] Could not find aggregated variables block")

# Now update the pagination footer text to say "rows" instead of "days" since it may be weeks/months
old_pagination = 'Text(\"Showing $from to $to of ${ledgerRows.size} days\", style = MaterialTheme.typography.bodySmall, color = TextMuted)'
new_pagination = 'Text(\"Showing $from to $to of ${ledgerRows.size} periods\", style = MaterialTheme.typography.bodySmall, color = TextMuted)'
if old_pagination in content:
    content = content.replace(old_pagination, new_pagination)
    print("[OK] Updated pagination text")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("\n[OK] All granularity fixes applied!")
