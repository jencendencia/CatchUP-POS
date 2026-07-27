# Wire granularity buttons to change the date range
# "Daily" → "Today", "Weekly" → "This Week", "Monthly" → "This Month"

# ── Step 1: Update ReportsScreen.kt (ProfitTabContent) ──
with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    r_content = f.read()

# Add onGranularityChange parameter to ProfitTabContent signature
old_sig_end = "    onDateClick: () -> Unit = {},\n    onFilterClick: () -> Unit = {}\n) {"
new_sig_end = "    onDateClick: () -> Unit = {},\n    onFilterClick: () -> Unit = {},\n    onGranularityChange: (String) -> Unit = {}\n) {"

if old_sig_end in r_content:
    r_content = r_content.replace(old_sig_end, new_sig_end, 1)
    print("OK: Added onGranularityChange param to ProfitTabContent in ReportsScreen.kt")
else:
    print("WARN: Could not find signature end in ReportsScreen.kt")

# Wire the granularity click handler
old_click = '.clickable { granularity = g; currentPage = 1 }'
new_click = '.clickable { granularity = g; currentPage = 1; onGranularityChange(g) }'

if old_click in r_content:
    r_content = r_content.replace(old_click, new_click, 1)
    print("OK: Wired granularity click to call onGranularityChange")
else:
    # Try with possible whitespace differences
    import re
    # Find the pattern with flexible spacing
    click_pattern = re.compile(r'\.clickable\s*\{\s*granularity\s*=\s*g\s*;\s*currentPage\s*=\s*1\s*\}')
    if click_pattern.search(r_content):
        r_content = click_pattern.sub('.clickable { granularity = g; currentPage = 1; onGranularityChange(g) }', r_content, 1)
        print("OK: Wired granularity click via regex")
    else:
        print("FAIL: Could not find click handler")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(r_content)

# ── Step 2: Update ProfitScreen.kt ──
with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ProfitScreen.kt", "r", encoding="utf-8") as f:
    p_content = f.read()

# Add onGranularityChange callback after onFilterClick in the ProfitTabContent call
old_call = """            onDateClick = { showDatePicker = true },
            onFilterClick = { showFilterDialog = true },
            activeFilter = activeFilter
        )"""

new_call = """            onDateClick = { showDatePicker = true },
            onFilterClick = { showFilterDialog = true },
            onGranularityChange = { g ->
                val dateFormat = SimpleDateFormat(\"MMMM dd, yyyy\", Locale.US)
                when (g) {
                    \"Daily\" -> {
                        selectedDateRange = \"Today\"
                        selectedDateLabel = \"Today, \" + dateFormat.format(Date())
                    }
                    \"Weekly\" -> {
                        selectedDateRange = \"This Week\"
                        selectedDateLabel = \"This Week, \" + dateFormat.format(Date())
                    }
                    \"Monthly\" -> {
                        selectedDateRange = \"This Month\"
                        selectedDateLabel = \"This Month, \" + dateFormat.format(Date())
                    }
                }
                customStartDate = 0L
                customEndDate = 0L
            },
            activeFilter = activeFilter
        )"""

if old_call in p_content:
    p_content = p_content.replace(old_call, new_call, 1)
    print("OK: Added onGranularityChange in ProfitScreen.kt call site")
else:
    print("WARN: Could not find call site in ProfitScreen.kt")
    # Try a simpler search - just find the last few params
    simple_old = "            onFilterClick = { showFilterDialog = true },"
    if simple_old in p_content:
        print("INFO: Found onFilterClick line - doing targeted insert")
        insert_after = """            onFilterClick = { showFilterDialog = true },
            onGranularityChange = { g ->
                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
                when (g) {
                    "Daily" -> {
                        selectedDateRange = "Today"
                        selectedDateLabel = "Today, " + dateFormat.format(Date())
                    }
                    "Weekly" -> {
                        selectedDateRange = "This Week"
                        selectedDateLabel = "This Week, " + dateFormat.format(Date())
                    }
                    "Monthly" -> {
                        selectedDateRange = "This Month"
                        selectedDateLabel = "This Month, " + dateFormat.format(Date())
                    }
                }
                customStartDate = 0L
                customEndDate = 0L
            },"""
        p_content = p_content.replace(
            "            onFilterClick = { showFilterDialog = true },",
            insert_after,
            1
        )
        print("OK: Added via targeted insert")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ProfitScreen.kt", "w", encoding="utf-8") as f:
    f.write(p_content)

print("OK: Both files saved")
