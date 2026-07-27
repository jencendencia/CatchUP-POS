# Remove unused yStart/yEnd variables from KPI LaunchedEffect

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

old_code = """        val yCal = Calendar.getInstance().apply { timeInMillis = todayStart }
        yCal.add(Calendar.DAY_OF_MONTH, -1)
        val yStart = yCal.timeInMillis
        yCal.set(Calendar.HOUR_OF_DAY, 23); yCal.set(Calendar.MINUTE, 59)
        yCal.set(Calendar.SECOND, 59); yCal.set(Calendar.MILLISECOND, 999)
        val yEnd = yCal.timeInMillis

        // Today"""

new_code = """        // Today"""

if old_code in content:
    content = content.replace(old_code, new_code, 1)
    print("OK: Removed unused yStart/yEnd variables")
else:
    print("WARN: Could not find dead code to remove")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
