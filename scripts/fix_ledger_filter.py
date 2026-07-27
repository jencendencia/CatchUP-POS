"""Fix ledgerRows in ReportsScreen.kt - add proper filtering by activeFilter and fix remember dependencies."""

filepath = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

# The old ledgerRows computation
old_block = "    val dateFmt = remember { SimpleDateFormat(\"MMMM dd, yyyy (EEE)\", Locale.US) }\n    val ledgerRows = remember(dailySales) {\n        dailySales.sortedByDescending { it.dayOffset }.map { day ->\n            val timestamp = day.dayOffset * 86400000L\n            val expenses = perDayExpenses[day.dayOffset] ?: 0.0\n            val profit = day.total - expenses\n            val margin = if (day.total > 0) (profit / day.total) * 100 else 0.0\n            LedgerRow(\n                dateLabel = if (timestamp > 0) dateFmt.format(Date(timestamp)) else \"Unknown\",\n                dateTimestamp = timestamp,\n                cupsSold = day.orderCount,\n                sales = day.total,\n                expenses = expenses,\n                profit = profit,\n                margin = margin\n            )\n        }\n    }"

new_block = """    val dateFmt = remember { SimpleDateFormat("MMMM dd, yyyy (EEE)", Locale.US) }
    val allLedgerRows = remember(dailySales, perDayExpenses) {
        dailySales.sortedByDescending { it.dayOffset }.map { day ->
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
    }
    val ledgerRows = remember(allLedgerRows, activeFilter) {
        when (activeFilter) {
            "Profitable Only" -> allLedgerRows.filter { it.profit > 0 }
            "Loss Making Only" -> allLedgerRows.filter { it.profit <= 0 }
            "With Expenses Only" -> allLedgerRows.filter { it.expenses > 0 }
            else -> allLedgerRows
        }
    }"""

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)
    print("[OK] Fixed ledgerRows with proper filter and dependencies")
else:
    print("[WARN] Could not find the old ledgerRows block")
    # Debug: show what's around the area
    idx = content.find("val dateFmt = remember")
    if idx >= 0:
        print(f"Found dateFmt at position {idx}")
        print(content[idx:idx+600])
