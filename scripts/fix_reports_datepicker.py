# Replace preset DateRangeDialog with calendar-based DateRangePickerDialog in ReportsScreen.kt

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Add customStartDate/customEndDate state variables
# Current: var showDatePicker by remember { mutableStateOf(false) }
old_state = "    var showDatePicker by remember { mutableStateOf(false) }"
new_state = """    var showDatePicker by remember { mutableStateOf(false) }
    var customStartDate by remember { mutableStateOf(0L) }
    var customEndDate by remember { mutableStateOf(0L) }"""

if old_state in content:
    content = content.replace(old_state, new_state, 1)
    print("OK: Added customStartDate/customEndDate states")
else:
    print("WARN: Could not find showDatePicker state")

# 2. Update LaunchedEffect to handle custom date range
# Current: LaunchedEffect(selectedDateRange) {
old_launch = """    // Load all report data
    LaunchedEffect(selectedDateRange) {
        val (startTime, endTime) = computeDateRange(selectedDateRange)"""

new_launch = """    // Load all report data
    LaunchedEffect(selectedDateRange, customStartDate, customEndDate) {
        val (startTime, endTime) = if (selectedDateRange == "Custom Range") {
            customStartDate to customEndDate
        } else {
            computeDateRange(selectedDateRange)
        }"""

if old_launch in content:
    content = content.replace(old_launch, new_launch, 1)
    print("OK: Updated LaunchedEffect to handle custom date range")
else:
    print("WARN: Could not find LaunchedEffect")

# 3. Replace DateRangeDialog call with DateRangePickerDialog call
old_dialog_call = """    // Date picker dialog
    if (showDatePicker) {
        DateRangeDialog(
            onDismiss = { showDatePicker = false },
            onApply = { option, label ->
                selectedDateRange = option
                selectedDateLabel = label
                showDatePicker = false
            }
        )
    }"""

new_dialog_call = """    // Date picker dialog (calendar-based)
    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onApply = { startMillis, endMillis ->
                if (startMillis != null && endMillis != null) {
                    // Convert UTC midnight to local time day boundaries
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
    }"""

if old_dialog_call in content:
    content = content.replace(old_dialog_call, new_dialog_call, 1)
    print("OK: Replaced DateRangeDialog call with DateRangePickerDialog")
else:
    print("WARN: Could not find DateRangeDialog call")

# 4. Replace DateRangeDialog function with DateRangePickerDialog (calendar-based)
old_dialog_func = """@Composable
private fun DateRangeDialog(
    onDismiss: () -> Unit,
    onApply: (option: String, label: String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMMM dd, yyyy", Locale.US) }
    var selectedOption by remember { mutableStateOf("Today") }
    val options = listOf("Today", "Yesterday", "This Week", "This Month", "Last Month", "All Time")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { Text("Select Date Range", color = TextWhite, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) OrangeAccent.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { selectedOption = option }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(option, modifier = Modifier.weight(1f), color = if (isSelected) OrangeAccent else TextWhite, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = OrangeAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val label = "$selectedOption, ${dateFormat.format(Date())}"
                    onApply(selectedOption, label)
                },
                colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent)
            ) { Text("Apply", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) }
        }
    )
}"""

new_dialog_func = """@OptIn(ExperimentalMaterial3Api::class)
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

if old_dialog_func in content:
    content = content.replace(old_dialog_func, new_dialog_func, 1)
    print("OK: Replaced DateRangeDialog function with DateRangePickerDialog")
else:
    print("WARN: Could not find DateRangeDialog function body")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
