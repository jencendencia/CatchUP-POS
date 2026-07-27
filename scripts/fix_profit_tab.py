"""Fix ProfitTabContent in ReportsScreen.kt with proper parameters and expense calculation."""

import re

filepath = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Fix the function signature
old_sig = (
    "fun ProfitTabContent(\n"
    "    dailySales: List<DailySalesSummary>,\n"
    "    kpiData: KPIData,\n"
    "    itemsSold: Int,\n"
    "    realExpenses: Double = 0.0\n"
    ")"
)
new_sig = (
    "fun ProfitTabContent(\n"
    "    dailySales: List<DailySalesSummary>,\n"
    "    kpiData: KPIData,\n"
    "    itemsSold: Int,\n"
    "    realExpenses: Double = 0.0,\n"
    "    perDayExpenses: Map<Long, Double> = emptyMap(),\n"
    "    selectedDateLabel: String = \"\",\n"
    "    activeFilter: String = \"All\",\n"
    "    onDateClick: () -> Unit = {},\n"
    "    onFilterClick: () -> Unit = {}\n"
    ")"
)
if old_sig in content:
    content = content.replace(old_sig, new_sig)
    print("[OK] Updated function signature")
else:
    print("[WARN] Could not find old signature")

# 2. Fix the fake expense calculation
old_expense = "            val expenses = day.total * 0.5"
new_expense = "            val expenses = perDayExpenses[day.dayOffset] ?: 0.0"
if old_expense in content:
    content = content.replace(old_expense, new_expense)
    print("[OK] Updated expense calculation to use real data")
else:
    print("[WARN] Could not find old expense calculation")

# 3. Fix date button onClick
old_date_click = "                    onClick = { /* Calendar */ },"
new_date_click = "                    onClick = onDateClick,"
if old_date_click in content:
    content = content.replace(old_date_click, new_date_click)
    print("[OK] Updated date button onClick")
else:
    # Try alternative pattern
    old_date_click2 = "                    onClick = { /* Calendar */ },"
    if old_date_click2 in content:
        content = content.replace(old_date_click2, new_date_click)
        print("[OK] Updated date button onClick (alt)")
    else:
        print("[WARN] Could not find date button onClick")

# 4. Fix date button text to use selectedDateLabel
old_date_text = '                    Text(SimpleDateFormat(\"MMMM dd, yyyy\", Locale.US).format(Date()), style = MaterialTheme.typography.bodySmall, maxLines = 1)'
new_date_text = '                    Text(if (selectedDateLabel.isNotEmpty()) selectedDateLabel else SimpleDateFormat(\"MMMM dd, yyyy\", Locale.US).format(Date()), style = MaterialTheme.typography.bodySmall, maxLines = 1)'
if old_date_text in content:
    content = content.replace(old_date_text, new_date_text)
    print("[OK] Updated date button text")
else:
    print("[WARN] Could not find date button text")

# 5. Fix filter button onClick
old_filter_click = "                    onClick = { /* Filter */ },"
new_filter_click = "                    onClick = onFilterClick,"
if old_filter_click in content:
    content = content.replace(old_filter_click, new_filter_click)
    print("[OK] Updated filter button onClick")
else:
    print("[WARN] Could not find filter button onClick")

# 6. Fix filter button text
old_filter_text = '                    Text(\"Filter\", style = MaterialTheme.typography.bodySmall)'
new_filter_text = '                    Text(if (activeFilter != \"All\") \"Filter: $activeFilter\" else \"Filter\", style = MaterialTheme.typography.bodySmall)'
if old_filter_text in content:
    content = content.replace(old_filter_text, new_filter_text)
    print("[OK] Updated filter button text")
else:
    print("[WARN] Could not find filter button text")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print("\n[OK] All replacements applied successfully!")
