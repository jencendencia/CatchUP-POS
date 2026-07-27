# Add KPI refresh key to ExpensesTabContent

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add kpiRefreshKey state after showAddExpenseDialog
old_states = "    var showAddExpenseDialog by remember { mutableStateOf(false) }"
new_states = "    var showAddExpenseDialog by remember { mutableStateOf(false) }\n    var kpiRefreshKey by remember { mutableIntStateOf(0) }"

if old_states in content:
    content = content.replace(old_states, new_states, 1)
    print("OK: Added kpiRefreshKey state")
else:
    print("WARN: Could not find showAddExpenseDialog")

# 2. Update onSaved callback to increment kpiRefreshKey
old_saved = """            onSaved = {
                showAddExpenseDialog = false
                onExpenseSaved()
                // Reload data via scope
                scope.launch {
                    val (st, et) = computeDateRange(\"Today\")
                    // The parent will handle refreshing
                }
            }"""

new_saved = """            onSaved = {
                showAddExpenseDialog = false
                onExpenseSaved()
                kpiRefreshKey++
            }"""

if old_saved in content:
    content = content.replace(old_saved, new_saved, 1)
    print("OK: Updated onSaved to increment kpiRefreshKey")
else:
    print("WARN: Could not find onSaved callback")

# 3. Update LaunchedEffect(Unit) to use kpiRefreshKey as key
old_launch = "    LaunchedEffect(Unit) {"
new_launch = "    LaunchedEffect(kpiRefreshKey) {"

if old_launch in content:
    content = content.replace(old_launch, new_launch, 1)
    print("OK: Updated LaunchedEffect to depend on kpiRefreshKey")
else:
    print("WARN: Could not find LaunchedEffect(Unit)")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
