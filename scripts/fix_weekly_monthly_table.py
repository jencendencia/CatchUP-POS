# Replace the Weekly/Monthly aggregated table view with per-day rows
# grouped under week/month headers

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Use a simpler approach: find the anchor comment and replace from there
# to the matching closing brace (counting indent level)

anchor = "// Aggregated view (Weekly/Monthly)"
old_text_start = "                } else {\n                    // Aggregated view (Weekly/Monthly)"
old_text_end = "\n                }"

# Find the anchor
pos = content.find(old_text_start)
if pos < 0:
    print(f"FAIL: Could not find anchor: {old_text_start}")
    exit(1)

# Find the matching closing brace at same indent level (16 spaces + })
end_anchor = "                }"
pos_end = content.find(end_anchor, pos + len(old_text_start))
if pos_end < 0:
    print("FAIL: Could not find closing brace")
    exit(1)

# Verify this is the right one by checking it's followed by \n            }
next_line = content[pos_end + len(end_anchor):pos_end + len(end_anchor) + 20]
if next_line.startswith("\n            }"):
    # This is the right closing brace (closes the else block, then the Column, then Card)
    pass
else:
    # Try to find the right one
    while pos_end >= 0:
        next_line = content[pos_end + len(end_anchor):pos_end + len(end_anchor) + 20]
        if next_line.startswith("\n            }"):
            break
        pos_end = content.find(end_anchor, pos_end + 1)
    if pos_end < 0:
        print("FAIL: Could not find correct closing brace")
        exit(1)

extracted = content[pos:pos_end + len(end_anchor)]
print(f"Found text to replace ({len(extracted)} chars, from pos {pos} to {pos_end + len(end_anchor)})")
print(f"First line: {extracted.split(chr(10))[0]}")
print(f"Last line: {extracted.split(chr(10))[-1]}")

# The replacement text
replacement = """                } else {
                    // Per-day view grouped by Week/Month
                    val dailyLedgerRows = remember(dailySales, perDayExpenses, perDayItemsSold, activeFilter) {
                        val raw = aggregateDailySales(dailySales, "Daily")
                        when (activeFilter) {
                            "Profitable Only" -> raw.filter { it.profit > 0 }
                            "Loss Making Only" -> raw.filter { it.profit <= 0 }
                            "With Expenses Only" -> raw.filter { it.expenses > 0 }
                            else -> raw
                        }
                    }

                    // Group daily rows by week or month
                    data class DayGroup(val label: String, val timestamp: Long, val days: List<LedgerRow>)

                    val groupedByPeriod = remember(dailyLedgerRows, granularity) {
                        if (granularity == "Weekly") {
                            val weekFmt = SimpleDateFormat("'Week of' MMM dd, yyyy", Locale.US)
                            dailyLedgerRows.groupBy { row ->
                                row.dateTimestamp / (7 * 86400000L)
                            }.map { (weekOffset, rows) ->
                                val weekStart = weekOffset * 7 * 86400000L
                                val weekMid = weekStart + 3 * 86400000L
                                DayGroup(
                                    label = weekFmt.format(Date(weekMid)),
                                    timestamp = weekStart,
                                    days = rows
                                )
                            }.sortedByDescending { it.timestamp }
                        } else { // Monthly
                            val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.US)
                            dailyLedgerRows.groupBy { row ->
                                val cal = Calendar.getInstance().apply { timeInMillis = row.dateTimestamp }
                                cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
                            }.map { (_, rows) ->
                                val firstDay = rows.minBy { it.dateTimestamp }
                                DayGroup(
                                    label = monthFmt.format(Date(firstDay.dateTimestamp)),
                                    timestamp = firstDay.dateTimestamp,
                                    days = rows
                                )
                            }.sortedByDescending { it.timestamp }
                        }
                    }

                    // Header row
                    Row(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("DATE", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("CUPS SOLD", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("SALES (\u20b1)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("EXPENSES (\u20b1)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("PROFIT (\u20b1)", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("MARGIN (%)", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.weight(0.4f))
                    }

                    if (groupedByPeriod.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("No data available", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    } else {
                        groupedByPeriod.forEach { group ->
                            // Group header with total for the period
                            val groupTotalSales = group.days.sumOf { it.sales }
                            val groupTotalExpenses = group.days.sumOf { it.expenses }
                            val groupTotalProfit = groupTotalSales - groupTotalExpenses
                            val groupMargin = if (groupTotalSales > 0) (groupTotalProfit / groupTotalSales) * 100 else 0.0
                            val groupTotalCups = group.days.sumOf { it.cupsSold }

                            Surface(
                                color = OrangeAccent.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp)) {
                                    Text(group.label, modifier = Modifier.weight(2f),
                                        style = MaterialTheme.typography.labelMedium, color = OrangeAccent, fontWeight = FontWeight.Bold)
                                    Text("" + groupTotalCups + " cups", modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("\u20b1" + String.format(Locale.US, "%,.2f", groupTotalSales), modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("\u20b1" + String.format(Locale.US, "%,.2f", groupTotalExpenses), modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("\u20b1" + String.format(Locale.US, "%,.2f", groupTotalProfit), modifier = Modifier.weight(1.2f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                    Text("" + String.format(Locale.US, "%.1f", groupMargin) + "%", modifier = Modifier.weight(0.8f),
                                        style = MaterialTheme.typography.labelSmall, color = TextMuted)
                                }
                            }

                            // Per-day rows under the group header
                            group.days.forEach { row ->
                                HorizontalDivider(color = DarkBorder, thickness = 0.3.dp)
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(row.dateLabel, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("" + row.cupsSold, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    Text("\u20b1" + String.format(Locale.US, "%,.2f", row.sales), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    Text("\u20b1" + String.format(Locale.US, "%,.2f", row.expenses), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MutedRed)
                                    Text("\u20b1" + String.format(Locale.US, "%,.2f", row.profit), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = StatusGreen, fontWeight = FontWeight.SemiBold)
                                    Text("" + String.format(Locale.US, "%.1f", row.margin) + "%", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    Text("\u22ee", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = TextGray)
                                }
                            }
                        }
                    }
                }"""

content = content[:pos] + replacement + content[pos_end + len(end_anchor):]

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: Successfully replaced aggregated view with per-day grouped view")
