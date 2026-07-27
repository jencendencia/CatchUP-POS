# Replace preset date dropdown with calendar-based DateRangePickerDialog in TransactionsScreen.kt

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/TransactionsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add state variables after selectedStatus line
old_states = """    var selectedStatus by remember { mutableStateOf("All Status") }
    var currentPage by remember { mutableIntStateOf(1) }"""

new_states = """    var selectedStatus by remember { mutableStateOf("All Status") }
    var selectedDateLabel by remember { mutableStateOf("Today") }
    var showDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf(0L) }
    var customEndDate by remember { mutableStateOf(0L) }
    var currentPage by remember { mutableIntStateOf(1) }"""

if old_states in content:
    content = content.replace(old_states, new_states, 1)
    print("OK: Added date state variables")
else:
    print("WARN: Could not find state section")

# 2. Replace the date FilterDropdown with a button that opens the calendar picker
old_date_dropdown = """            FilterDropdown(
                label = selectedDateRange,
                icon = Icons.Default.DateRange,
                options = listOf("Today", "This Week", "This Month", "All Time"),
                onSelected = { selectedDateRange = it; currentPage = 1 },
                modifier = Modifier.weight(1.2f)
            )"""

new_date_button = """            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.weight(1.2f).height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextWhite),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(selectedDateLabel, style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
            }"""

if old_date_dropdown in content:
    content = content.replace(old_date_dropdown, new_date_button, 1)
    print("OK: Replaced date dropdown with calendar picker button")
else:
    print("WARN: Could not find date dropdown. Checking anchor...")
    # More flexible search
    if "selectedDateRange" in content:
        print("INFO: Found selectedDateRange reference - may need different replacement")

# 3. Update the filter logic to handle custom date ranges
old_filter = """    val filteredTransactions = remember(allTransactions, selectedType, selectedStatus, selectedDateRange) {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDay(now)

        allTransactions.filter { txn ->
            // Date filter
            val dateMatch = when (selectedDateRange) {
                "Today" -> txn.createdAt >= startOfDay
                "This Week" -> txn.createdAt >= startOfDay - 7 * 86400000L
                "This Month" -> txn.createdAt >= startOfDay - 30 * 86400000L
                else -> true
            }"""

new_filter = """    val filteredTransactions = remember(allTransactions, selectedType, selectedStatus, selectedDateRange, customStartDate, customEndDate) {
        val now = System.currentTimeMillis()
        val startOfDay = getStartOfDay(now)

        allTransactions.filter { txn ->
            // Date filter
            val dateMatch = when (selectedDateRange) {
                "Today" -> txn.createdAt >= startOfDay
                "This Week" -> txn.createdAt >= startOfDay - 7 * 86400000L
                "This Month" -> txn.createdAt >= startOfDay - 30 * 86400000L
                "Custom Range" -> txn.createdAt >= customStartDate && txn.createdAt <= customEndDate
                else -> true
            }"""

if old_filter in content:
    content = content.replace(old_filter, new_filter, 1)
    print("OK: Updated filter logic for custom date range")
else:
    print("WARN: Could not find filter logic")

# 4. Add DateRangePickerDialog composable and the dialog call before the detail dialog
# Add the dialog call after the filters section
old_detail_dialog = """    // Transaction Detail Dialog
    if (selectedTransaction != null) {"""

new_detail_dialog = """    // Date Range Picker Dialog (calendar-based)
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onApply = { startMillis, endMillis ->
                if (startMillis != null && endMillis != null) {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = startMillis
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    val startDay = cal.timeInMillis
                    cal.timeInMillis = endMillis
                    cal.set(Calendar.HOUR_OF_DAY, 23)
                    cal.set(Calendar.MINUTE, 59)
                    cal.set(Calendar.SECOND, 59)
                    cal.set(Calendar.MILLISECOND, 999)
                    val endDay = cal.timeInMillis
                    customStartDate = startDay
                    customEndDate = endDay
                    selectedDateRange = "Custom Range"
                    selectedDateLabel = "${SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date(startDay))} - ${SimpleDateFormat("MMMM dd, yyyy", Locale.US).format(Date(endDay))}"
                }
                showDatePicker = false
            }
        )
    }

    // Transaction Detail Dialog
    if (selectedTransaction != null) {"""

if old_detail_dialog in content:
    content = content.replace(old_detail_dialog, new_detail_dialog, 1)
    print("OK: Added DateRangePickerDialog call")
else:
    print("WARN: Could not find detail dialog anchor")

# 5. Add DateRangePickerDialog function at the end of the file
# Find the last closing brace of the file
# The file ends with the PaginationButton function closing
old_end = """            }
        }
    }
}"""

# We need to add the dialog function before the very last closing of the file
# Actually let's add it after the PaginationButton function

old_file_end = """// ════════════════════════════════════════════════════════════════════
// Pagination Button
// ════════════════════════════════════════════════════════════════════

@Composable
private fun PaginationButton(
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) OrangeAccent else Color.Transparent,
        border = if (isActive) null else BorderStroke(1.dp, DarkBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) TextWhite else if (enabled) TextWhite else TextGray,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}"""

new_file_extension = """// ════════════════════════════════════════════════════════════════════
// Pagination Button
// ════════════════════════════════════════════════════════════════════

@Composable
private fun PaginationButton(
    label: String,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isActive) OrangeAccent else Color.Transparent,
        border = if (isActive) null else BorderStroke(1.dp, DarkBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) TextWhite else if (enabled) TextWhite else TextGray,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Date Range Picker Dialog (Calendar-based)
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onApply: (startMillis: Long?, endMillis: Long?) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Select Date Range", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.height(380.dp)) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    showModeToggle = false,
                    colors = DatePickerDefaults.colors(
                        containerColor = DarkCard,
                        titleContentColor = TextWhite,
                        headlineContentColor = TextWhite,
                        weekdayContentColor = TextMuted,
                        subheadContentColor = TextWhite,
                        yearContentColor = TextWhite,
                        currentYearContentColor = OrangeAccent,
                        selectedDayContentColor = Color.White,
                        selectedDayContainerColor = OrangeAccent,
                        dayContentColor = TextWhite,
                        todayContentColor = OrangeAccent,
                        todayDateBorderColor = OrangeAccent,
                        dayInSelectionRangeContentColor = Color.White,
                        dayInSelectionRangeContainerColor = OrangeAccent.copy(alpha = 0.3f),
                        dividerColor = DarkBorder
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(
                        dateRangePickerState.selectedStartDateMillis,
                        dateRangePickerState.selectedEndDateMillis
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                          dateRangePickerState.selectedEndDateMillis != null
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}"""

if old_file_end in content:
    content = content.replace(old_file_end, new_file_extension, 1)
    print("OK: Added DateRangePickerDialog function")
else:
    print("WARN: Could not find file end for extension")
    # Try to append instead
    content += "\n" + new_file_extension.split("// ════════════════════════════════════════════════════════════════════\n// Pagination Button")[1]
    print("INFO: Appended dialog function to end of file")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/TransactionsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
