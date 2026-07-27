# Fix missing allTransactions parameter in ProfitTabContent function signature
# and fix the duplicate comment line

import sys

reports_path = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

with open(reports_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Fix the function signature to add allTransactions parameter
old_sig = """fun ProfitTabContent(
    dailySales: List<DailySalesSummary>,
    kpiData: KPIData,
    itemsSold: Int,
    realExpenses: Double = 0.0,
    perDayExpenses: Map<Long, Double> = emptyMap(),
    perDayItemsSold: Map<Long, Int> = emptyMap(),
    selectedDateLabel: String = "",
    activeFilter: String = "All",
    onDateClick: () -> Unit = {},
    onFilterClick: () -> Unit = {}
)"""

new_sig = """fun ProfitTabContent(
    dailySales: List<DailySalesSummary>,
    kpiData: KPIData,
    itemsSold: Int,
    realExpenses: Double = 0.0,
    perDayExpenses: Map<Long, Double> = emptyMap(),
    perDayItemsSold: Map<Long, Int> = emptyMap(),
    allTransactions: List<TransactionEntity> = emptyList(),
    selectedDateLabel: String = "",
    activeFilter: String = "All",
    onDateClick: () -> Unit = {},
    onFilterClick: () -> Unit = {}
)"""

if old_sig in content:
    content = content.replace(old_sig, new_sig)
    print("OK: Added allTransactions parameter to ProfitTabContent signature")
else:
    print("WARN: Could not find the exact function signature to replace")
    # Try a more flexible replacement
    if "perDayItemsSold: Map<Long, Int> = emptyMap()," in content:
        content = content.replace(
            "perDayItemsSold: Map<Long, Int> = emptyMap(),",
            "perDayItemsSold: Map<Long, Int> = emptyMap(),\n    allTransactions: List<TransactionEntity> = emptyList(),",
            1
        )
        print("OK: Added allTransactions with flexible replacement")

# 2. Fix duplicate comment line
old_dup = """        // --- Summary Ledger Table ---
                // --- Summary Ledger Table ---"""

new_dup = """        // --- Summary Ledger Table ---"""

# Use a regex to find the duplicate (em-dash is \u2500)
import re
dup_pattern = r"(// ── Summary Ledger Table ──)\s+\1"
if re.search(dup_pattern, content):
    content = re.sub(dup_pattern, r"\1", content)
    print("OK: Fixed duplicate comment line")
else:
    print("INFO: No duplicate comment line found (may already be fixed)")

with open(reports_path, "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved successfully")
