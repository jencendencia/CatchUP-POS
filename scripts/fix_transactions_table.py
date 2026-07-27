"""Update ProfitTabContent to show transactions per day in the summary table."""

import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')

filepath = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

changes = 0

# 1. Find where the table header starts by searching for unique strings
idx_cups = content.find('CUPS SOLD')
if idx_cups >= 0:
    print(f"[DEBUG] Found 'CUPS SOLD' at position {idx_cups}")
    # Show context before this
    ctx_start = max(0, idx_cups - 100)
    print(f"[DEBUG] Context before: {repr(content[ctx_start:idx_cups][-80:])}")

# 2. The table section starts with the header row
# Let me search for the exact header pattern
header_pattern = 'Row(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 12.dp, vertical = 10.dp))'
idx_header = content.find(header_pattern)
if idx_header >= 0:
    print(f"[DEBUG] Found header pattern at position {idx_header}")
    print(f"[DEBUG] Content after header: {repr(content[idx_header:idx_header+400])}")

# Find the section to replace - from the first header row to the end of the paginated rows section
# Let me find the specific block
search_from = content.find('// ── Summary Ledger Table ──')
if search_from >= 0:
    # Find the card containing the table
    card_start = content.find('Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {', search_from)
    if card_start >= 0:
        print(f"[DEBUG] Summary table Card starts at {card_start}")
        
        # Find the end of this card by counting braces
        # The card ends with "}" that closes the Column, then "}" that closes the Card
        # Find "Periods" in the pagination text to get close to the end
        pagination_end = content.find('periods\"', card_start)
        if pagination_end >= 0:
            # Find the end - look for "}" after pagination, then another "}" for the Card
            after_pag = content[pagination_end:pagination_end+500]
            # The end should be: ...content... } }\n        }\n    }\n}\n
            end_marker = '\n            }\n        }\n    }\n}\n\n@Composable'
            card_end = content.find(end_marker, pagination_end)
            if card_end >= 0:
                print(f"[DEBUG] Card ends at {card_end}")
                
                # Now I can replace the entire table content
                # The old table content includes the header, data rows, and pagination
                old_table_content = content[card_start:pagination_end+len('periods\"')]
                # Find where the pagination Row ends
                # Look for the closing braces after the periods text
                # Pattern: ...periods" ... pagination rows ... }  }  }  }
                # Let me find the right closing braces
                
                # Simpler approach: replace from card_start to card_end
                old_block = content[card_start:card_end + len(end_marker)]
                
                # New block with transaction view for Daily
                new_block = """        // ── Summary Ledger Table ──
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D))) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Table toolbar
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("SUMMARY TABLE", style = MaterialTheme.typography.labelSmall, color = TextMuted, letterSpacing = 1.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Show transactions per day for Daily, aggregated for Weekly/Monthly
                if (granularity == "Daily") {
                    // Transaction view header
                    Row(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("TIME", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("ORDER #", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("CUSTOMER", modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("ITEMS", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("PAYMENT", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("AMOUNT (" + "\u20B1" + ")", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }

                    if (allTransactions.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("No transactions found for this period", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    } else {
                        val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.US) }
                        val dateFmt2 = remember { SimpleDateFormat("MMMM dd, yyyy (EEE)", Locale.US) }
                        // Group transactions by day
                        val txnsByDay = allTransactions.groupBy { it.createdAt / 86400000L }
                            .toSortedMap(compareByDescending { it })
                        
                        txnsByDay.forEach { (dayOffset, txns) ->
                            // Date header
                            val dateStr = if (dayOffset > 0) dateFmt2.format(Date(dayOffset * 86400000L)) else "Unknown"
                            Surface(
                                color = OrangeAccent.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                Text("  " + dateStr, modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                                    style = MaterialTheme.typography.labelMedium, color = OrangeAccent, fontWeight = FontWeight.Bold)
                            }

                            txns.forEach { txn ->
                                HorizontalDivider(color = DarkBorder, thickness = 0.3.dp)
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(timeFmt.format(Date(txn.createdAt)), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                    Text("#" + String.format(Locale.US, "%05d", txn.id), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = OrangeAccent, fontWeight = FontWeight.SemiBold)
                                    Text(txn.customerName, modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("" + txn.itemCount, modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                    val pmColor = if (txn.paymentMethod.equals("GCash", ignoreCase = true)) Color(0xFF2196F3) else StatusGreen
                                    Surface(shape = RoundedCornerShape(10.dp), color = pmColor.copy(alpha = 0.12f), modifier = Modifier.weight(1f)) {
                                        Text(txn.paymentMethod, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = pmColor, fontWeight = FontWeight.SemiBold)
                                    }
                                    Text("\u20B1" + String.format(Locale.US, "%,.2f", txn.total), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    // Aggregated view (Weekly/Monthly)
                    Row(modifier = Modifier.fillMaxWidth().background(DarkCard).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text("DATE", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("CUPS SOLD", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("SALES (" + "\u20B1" + ")", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("EXPENSES (" + "\u20B1" + ")", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("PROFIT (" + "\u20B1" + ")", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text("MARGIN (%)", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Spacer(modifier = Modifier.weight(0.4f))
                    }

                    if (paginatedRows.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text("No data available", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    } else {
                        paginatedRows.forEach { row ->
                            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                                Text(row.dateLabel, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, color = TextWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("" + row.cupsSold, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                Text("\u20B1" + String.format(Locale.US, "%,.2f", row.sales), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                Text("\u20B1" + String.format(Locale.US, "%,.2f", row.expenses), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = MutedRed)
                                Text("\u20B1" + String.format(Locale.US, "%,.2f", row.profit), modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, color = StatusGreen, fontWeight = FontWeight.SemiBold)
                                Text("" + String.format(Locale.US, "%.1f", row.margin) + "%", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, color = TextWhite)
                                Text("\u22EE", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = TextGray)
                            }
                        }
                    }

                    // Pagination for aggregated view
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val from = if (ledgerRows.isEmpty()) 0 else (safePage - 1) * itemsPerPage + 1
                        val to = minOf(safePage * itemsPerPage, ledgerRows.size)
                        Text("Showing " + from + " to " + to + " of " + ledgerRows.size + " periods", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PageButton("<", enabled = safePage > 1) { if (currentPage > 1) currentPage-- }
                            for (p in 1..totalPages) {
                                PageButton("" + p, isActive = p == safePage, enabled = true) { currentPage = p }
                            }
                            PageButton(">", enabled = safePage < totalPages) { if (currentPage < totalPages) currentPage++ }
                        }
                    }
                }
            }
        }"""

                content = content.replace(old_block, new_block)
                changes += 1
                print("[OK] Replaced summary table with per-day transaction view for Daily granularity")
            else:
                print("[WARN] Could not find card end")
        else:
            print("[WARN] Could not find pagination end")
    else:
        print("[WARN] Could not find summary table Card")
else:
    print("[WARN] Could not find Summary Ledger Table section")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print(f"\n[OK] {changes} change(s) applied to ReportsScreen.kt")
