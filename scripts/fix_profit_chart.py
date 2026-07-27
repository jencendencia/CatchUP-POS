# Fix ProfitMultiLineChart: add value indicators, fix expense calculation

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update call site to pass perDayExpenses
old_call = "ProfitMultiLineChart(dailySales = dailySales, modifier = Modifier.fillMaxWidth().height(200.dp))"
new_call = "ProfitMultiLineChart(dailySales = dailySales, perDayExpenses = perDayExpenses, modifier = Modifier.fillMaxWidth().height(200.dp))"

if old_call in content:
    content = content.replace(old_call, new_call, 1)
    print("OK: Updated ProfitMultiLineChart call site")
else:
    print("WARN: Could not find call site")

# 2. Replace the entire ProfitMultiLineChart function
old_chart = """@Composable
fun ProfitMultiLineChart(dailySales: List<DailySalesSummary>, modifier: Modifier = Modifier) {
    val maxVal = maxOf(dailySales.maxOfOrNull { it.total } ?: 1.0, 1.0)
    val salesColor = StatusGreen
    val expensesColor = MutedRed
    val profitColor = OrangeAccent

    Canvas(modifier = modifier.padding(start = 8.dp, bottom = 24.dp, end = 8.dp, top = 8.dp)) {
        if (dailySales.size < 2) return@Canvas
        val stepX = size.width / (dailySales.size - 1)
        fun drawLine(points: List<Float>, color: Color) {
            if (points.size < 2) return
            val path = Path().apply {
                moveTo(0f, points[0])
                for (i in 1 until points.size) lineTo(i * stepX, points[i])
            }
            drawPath(path, color, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        val salesPoints = dailySales.map { size.height - (size.height * (it.total / maxVal)).toFloat() }
        val expensePoints = dailySales.map { size.height - (size.height * ((it.total * 0.5) / maxVal)).toFloat() }
        val profitPoints = dailySales.map { size.height - (size.height * ((it.total * 0.5) / maxVal)).toFloat() }

        drawLine(salesPoints, salesColor)
        drawLine(expensePoints, expensesColor)
        drawLine(profitPoints, profitColor)

        val dateFmt = SimpleDateFormat("MMM dd", Locale.US)
        dailySales.forEachIndexed { i, day ->
            val x = i * stepX
            val label = if (day.dayOffset > 0) dateFmt.format(Date(day.dayOffset * 86400000L)) else ""
            drawContext.canvas.nativeCanvas.drawText(
                label, x, size.height + 14f,
                android.graphics.Paint().apply { color = TextGray.hashCode(); textSize = 18f; textAlign = android.graphics.Paint.Align.CENTER }
            )
        }
    }
}"""

new_chart = """@Composable
fun ProfitMultiLineChart(
    dailySales: List<DailySalesSummary>,
    perDayExpenses: Map<Long, Double> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val maxVal = maxOf(dailySales.maxOfOrNull { it.total } ?: 1.0, 1.0)
    val salesColor = StatusGreen
    val expensesColor = MutedRed
    val profitColor = OrangeAccent

    // Compute actual expenses per day from the map
    val expensePerDay = dailySales.associate { it.dayOffset to (perDayExpenses[it.dayOffset] ?: 0.0) }
    val maxExpense = maxOf((if (expensePerDay.values.maxOrNull() != null) expensePerDay.values.max() else 0.0), 1.0)
    // Profit = sales - expenses
    val profitPerDay = dailySales.associate { it.dayOffset to (it.total - (perDayExpenses[it.dayOffset] ?: 0.0)) }
    val maxProfit = maxOf((if (profitPerDay.values.maxOrNull() != null) profitPerDay.values.max() else 0.0), 1.0)

    // Use the actual max across all series for Y-axis scaling
    val overallMax = maxOf(maxVal, maxExpense, maxProfit)
    val valuePaint = remember { android.graphics.Paint().apply {
        color = TextWhite.hashCode(); textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
    } }

    Canvas(modifier = modifier.padding(start = 8.dp, bottom = 24.dp, end = 8.dp, top = 8.dp)) {
        if (dailySales.size < 2) return@Canvas
        val stepX = size.width / (dailySales.size - 1)
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
                // Outer circle (line color)
                drawCircle(color, radius = 4f, center = androidx.compose.ui.geometry.Offset(x, y))
                // Inner circle (background color)
                drawCircle(Color(0xFF0D0D0D), radius = 2.5f, center = androidx.compose.ui.geometry.Offset(x, y))
            }
        }

        val salesPoints = dailySales.map { size.height - (size.height * (it.total / overallMax)).toFloat() }
        val expensePoints = dailySales.mapIndexed { i, day ->
            val exp = perDayExpenses[day.dayOffset] ?: 0.0
            size.height - (size.height * (exp / overallMax)).toFloat()
        }
        val profitPoints = dailySales.mapIndexed { i, day ->
            val profit = day.total - (perDayExpenses[day.dayOffset] ?: 0.0)
            size.height - (size.height * (maxOf(profit, 0.0) / overallMax)).toFloat()
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

            // Sales label (offset to the right of the point)
            val labelX = lastIdx * stepX + 12f
            valuePaint.color = salesColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "\u20b1${String.format(Locale.US, "%,.0f", lastSales)}",
                labelX, salesPoints[lastIdx] + 5f, valuePaint
            )

            // Expenses label
            valuePaint.color = expensesColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "\u20b1${String.format(Locale.US, "%,.0f", lastExpense)}",
                labelX, expensePoints[lastIdx] + 5f, valuePaint
            )

            // Profit label
            valuePaint.color = profitColor.hashCode()
            drawContext.canvas.nativeCanvas.drawText(
                "\u20b1${String.format(Locale.US, "%,.0f", lastProfit)}",
                labelX, profitPoints[lastIdx] + 5f, valuePaint
            )
        }

        // Date labels at bottom
        val dateFmt = SimpleDateFormat("MMM dd", Locale.US)
        dailySales.forEachIndexed { i, day ->
            val x = i * stepX
            val label = if (day.dayOffset > 0) dateFmt.format(Date(day.dayOffset * 86400000L)) else ""
            drawContext.canvas.nativeCanvas.drawText(
                label, x, size.height + 14f,
                android.graphics.Paint().apply { color = TextGray.hashCode(); textSize = 18f; textAlign = android.graphics.Paint.Align.CENTER }
            )
        }
    }
}"""

if old_chart in content:
    content = content.replace(old_chart, new_chart, 1)
    print("OK: Replaced ProfitMultiLineChart with value indicators")
else:
    print("WARN: Could not find exact old_chart text")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
