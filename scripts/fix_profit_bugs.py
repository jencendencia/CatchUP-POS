"""Fix remaining profit bugs: hardcoded otherIncome and always-up trend arrows."""

filepath = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

changes = 0

# 1. Fix hardcoded `otherIncome = 500.0` in ProfitBreakdownDonut
old_other_income = "    val otherIncome = 500.0 // Placeholder other income\n    val grandTotal = totalSales + otherIncome"
new_other_income = "    val otherIncome = 0.0\n    val grandTotal = totalSales + totalExpenses"
if old_other_income in content:
    content = content.replace(old_other_income, new_other_income)
    changes += 1
    print("[OK] Fixed hardcoded otherIncome placeholder")
else:
    print("[WARN] Could not find otherIncome placeholder")

old_other_filter = "            Slice(\"Other\", otherIncome, Color(0xFF2196F3))\n        ).filter { it.value > 0 }"
new_other_filter = "            Slice(\"Expenses\", totalExpenses, MutedRed)\n        ).filter { it.value > 0 }"
if old_other_filter in content:
    content = content.replace(old_other_filter, new_other_filter)
    changes += 1
    print("[OK] Removed fake Other slice from donut")
else:
    print("[WARN] Could not find Other slice filter")

# Remove the duplicate Expenses and Other Income from the legend
old_legend_slices = """    val slices = listOf(
            Triple(\"Sales\", totalSales, StatusGreen),
            Triple(\"Expenses\", totalExpenses, MutedRed),
            Triple(\"Other Income\", 500.0, Color(0xFF2196F3))
        )"""
new_legend_slices = """    val slices = listOf(
            Triple(\"Sales\", totalSales, StatusGreen),
            Triple(\"Expenses\", totalExpenses, MutedRed),
            Triple(\"Net Profit\", netProfit, OrangeAccent)
        )"""
if old_legend_slices in content:
    content = content.replace(old_legend_slices, new_legend_slices)
    changes += 1
    print("[OK] Fixed donut legend slices")
else:
    print("[WARN] Could not find donut legend slices")

# 2. Fix ProfitKPICard calls to pass dynamic trend strings
# Find the KPI card section in ProfitTabContent
# The issue: all ProfitKPICard calls hardcode "▲" prefix
# We need to make the trend string show ▼ for negative changes

# Actually, this is harder to fix because the trend strings are inlined string templates.
# The ProfitKPICard takes a flat `trend: String` parameter.
# To fix, I'd need to generate the trend strings dynamically.
# Let me fix the card invocations instead.

# Replace all hardcoded "▲" in ProfitKPICard calls with dynamic arrows
old_sales_card = 'ProfitKPICard(\"Total Sales\", \"₱${String.format(java.util.Locale.US, \"%,.2f\", totalSales)}\", StatusGreen, \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.salesChange)}% vs Yesterday\", Modifier.weight(1f))'
new_sales_card = 'ProfitKPICard(\"Total Sales\", \"₱${String.format(java.util.Locale.US, \"%,.2f\", totalSales)}\", StatusGreen, if (kpiData.salesChange >= 0) \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.salesChange)}% vs Yesterday\" else \"▼ ${String.format(java.util.Locale.US, \"%.1f\", -kpiData.salesChange)}% vs Yesterday\", Modifier.weight(1f))'
if old_sales_card in content:
    content = content.replace(old_sales_card, new_sales_card)
    changes += 1
    print("[OK] Fixed sales KPI card trend arrow")
else:
    print("[WARN] Could not find sales KPI card")

old_expenses_card = 'ProfitKPICard(\"Total Expenses\", \"₱${String.format(java.util.Locale.US, \"%,.2f\", totalExpenses)}\", MutedRed, \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}% vs Yesterday\", Modifier.weight(1f))'
new_expenses_card = 'ProfitKPICard(\"Total Expenses\", \"₱${String.format(java.util.Locale.US, \"%,.2f\", totalExpenses)}\", MutedRed, if (kpiData.profitChange >= 0) \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}% vs Yesterday\" else \"▼ ${String.format(java.util.Locale.US, \"%.1f\", -kpiData.profitChange)}% vs Yesterday\", Modifier.weight(1f))'
if old_expenses_card in content:
    content = content.replace(old_expenses_card, new_expenses_card)
    changes += 1
    print("[OK] Fixed expenses KPI card trend arrow")
else:
    print("[WARN] Could not find expenses KPI card")

old_profit_card = 'ProfitKPICard(\"Net Profit\", \"₱${String.format(java.util.Locale.US, \"%,.2f\", netProfit)}\", Color(0xFFFFC107), \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}% vs Yesterday\", Modifier.weight(1f))'
new_profit_card = 'ProfitKPICard(\"Net Profit\", \"₱${String.format(java.util.Locale.US, \"%,.2f\", netProfit)}\", Color(0xFFFFC107), if (kpiData.profitChange >= 0) \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}% vs Yesterday\" else \"▼ ${String.format(java.util.Locale.US, \"%.1f\", -kpiData.profitChange)}% vs Yesterday\", Modifier.weight(1f))'
if old_profit_card in content:
    content = content.replace(old_profit_card, new_profit_card)
    changes += 1
    print("[OK] Fixed net profit KPI card trend arrow")
else:
    print("[WARN] Could not find net profit KPI card")

old_margin_card = 'ProfitKPICard(\"Profit Margin\", \"${String.format(java.util.Locale.US, \"%.1f\", profitMargin)}%\", Color(0xFF9C27B0), \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}% vs Yesterday\", Modifier.weight(1f))'
new_margin_card = 'ProfitKPICard(\"Profit Margin\", \"${String.format(java.util.Locale.US, \"%.1f\", profitMargin)}%\", Color(0xFF9C27B0), if (kpiData.profitChange >= 0) \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}% vs Yesterday\" else \"▼ ${String.format(java.util.Locale.US, \"%.1f\", -kpiData.profitChange)}% vs Yesterday\", Modifier.weight(1f))'
if old_margin_card in content:
    content = content.replace(old_margin_card, new_margin_card)
    changes += 1
    print("[OK] Fixed profit margin KPI card trend arrow")
else:
    print("[WARN] Could not find profit margin KPI card")

old_cups_card = 'ProfitKPICard(\"Total Cups Sold\", \"$totalCups\", Color(0xFF2196F3), \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.itemsChange)}% vs Yesterday\", Modifier.weight(1f))'
new_cups_card = 'ProfitKPICard(\"Total Cups Sold\", \"$totalCups\", Color(0xFF2196F3), if (kpiData.itemsChange >= 0) \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.itemsChange)}% vs Yesterday\" else \"▼ ${String.format(java.util.Locale.US, \"%.1f\", -kpiData.itemsChange)}% vs Yesterday\", Modifier.weight(1f))'
if old_cups_card in content:
    content = content.replace(old_cups_card, new_cups_card)
    changes += 1
    print("[OK] Fixed cups sold KPI card trend arrow")
else:
    print("[WARN] Could not find cups sold KPI card")

# Also fix the profit margin card in the footer status bar
old_margin_footer = '                            Text(\"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}%\", style = MaterialTheme.typography.labelSmall, color = StatusGreen)'
new_margin_footer = '                            Text(if (kpiData.profitChange >= 0) \"▲ ${String.format(java.util.Locale.US, \"%.1f\", kpiData.profitChange)}%\" else \"▼ ${String.format(java.util.Locale.US, \"%.1f\", -kpiData.profitChange)}%\", style = MaterialTheme.typography.labelSmall, color = StatusGreen)'
if old_margin_footer in content:
    content = content.replace(old_margin_footer, new_margin_footer)
    changes += 1
    print("[OK] Fixed profit margin footer trend arrow")
else:
    print("[WARN] Could not find profit margin footer")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print(f"\n[OK] {changes} fix(es) applied!")
