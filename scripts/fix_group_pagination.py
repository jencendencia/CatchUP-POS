# Move DayGroup out of else block and add pagination to grouped view

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# 1. Remove data class DayGroup from inside the else block (around line 1789)
old_daygroup_inside = "                    data class DayGroup(val label: String, val timestamp: Long, val days: List<LedgerRow>)\n\n"
if old_daygroup_inside in content:
    content = content.replace(old_daygroup_inside, "", 1)
    print("OK: Removed DayGroup from else block")
else:
    print("WARN: Could not find DayGroup inside else block")
    # Try with different whitespace
    old_v2 = "                    data class DayGroup(val label: String, val timestamp: Long, val days: List<LedgerRow>)\n"
    if old_v2 in content:
        content = content.replace(old_v2, "", 1)
        print("OK: Removed DayGroup from else block (v2)")

# 2. Add DayGroup right after LedgerRow (before "val dateFmt = remember")
anchor = "    )\n\n    val dateFmt = remember { SimpleDateFormat(\"MMMM dd, yyyy (EEE)\", Locale.US) }"
new_ledger_section = """    )

    data class DayGroup(val label: String, val timestamp: Long, val days: List<LedgerRow>)

    val dateFmt = remember { SimpleDateFormat("MMMM dd, yyyy (EEE)", Locale.US) }"""

if anchor in content:
    content = content.replace(anchor, new_ledger_section, 1)
    print("OK: Added DayGroup after LedgerRow")
else:
    print("WARN: Could not add DayGroup after LedgerRow")

# 3. Add pagination to the grouped view
# Find the section: "if (groupedByPeriod.isEmpty()) {" and add pagination before the closing of the else block
old_pagination_section = """                    if (groupedByPeriod.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text(\"No data available\", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    } else {
                        groupedByPeriod.forEach { group ->"""

new_pagination_section = """                    val groupsPerPage = itemsPerPage
                    val totalGroups = maxOf((groupedByPeriod.size + groupsPerPage - 1) / groupsPerPage, 1)
                    val currentGroupPage = safePage
                    val paginatedGroups = groupedByPeriod.drop((currentGroupPage - 1) * groupsPerPage).take(groupsPerPage)

                    if (groupedByPeriod.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            Text(\"No data available\", style = MaterialTheme.typography.bodySmall, color = TextGray)
                        }
                    } else {
                        paginatedGroups.forEach { group ->"""

if old_pagination_section in content:
    content = content.replace(old_pagination_section, new_pagination_section, 1)
    print("OK: Added pagination to grouped view")
else:
    print("WARN: Could not add pagination to grouped view")

# 4. Add pagination buttons at the end (before the closing "}" of the else block)
# Find the last "}" of the else block and add pagination before it
# The else block currently ends with:
#                         }
#                     }
#                 }
# We need to find the closing pattern and add pagination buttons
# The pattern is: the closing of the else block ends with:
#                     }
#                 }
# followed by \n            }\n        }\n    }\n}\n\n@Composable

# Check if pagination buttons already exist
if "PageButton" not in content.split("groupedByPeriod")[-1][:200]:
    # Add pagination before the closing of the else block
    # Look for the pattern that closes the grouped table section
    pagination_buttons = """                    }

                    // Pagination for grouped view
                    HorizontalDivider(color = DarkBorder, thickness = 0.5.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val from = if (groupedByPeriod.isEmpty()) 0 else (currentGroupPage - 1) * groupsPerPage + 1
                        val to = minOf(currentGroupPage * groupsPerPage, groupedByPeriod.size)
                        Text("Showing " + from + " to " + to + " of " + groupedByPeriod.size + " periods", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            PageButton("<", enabled = currentGroupPage > 1) { if (currentPage > 1) currentPage-- }
                            for (p in 1..totalGroups) {
                                PageButton("" + p, isActive = p == currentGroupPage, enabled = true) { currentPage = p }
                            }
                            PageButton(">", enabled = currentGroupPage < totalGroups) { if (currentPage < totalGroups) currentPage++ }
                        }
                    }
                }"""

    # Find the pattern: the else block closes with just "}" after the groupedByPeriod loop
    # The closing pattern should be:
    #                         }
    #                     }  <- closes forEach/group
    #                 }      <- closes else
    # followed by closing of outer structures

    # Let's find this by looking for the end of group.days.forEach
    end_group_pattern = "                    }\n                }"
    
    # Find the last occurrence within the groupedByPeriod context
    # After the group.days.forEach { row -> ... } block, we have the end of groupedByPeriod.forEach { group ->
    # Then the else block closes
    # The pattern should be:
    #   (closing of group.days.forEach)
    #           }   <- closes Row inside group.days.forEach
    #         }     <- closes group.days.forEach
    #       }       <- closes groupedByPeriod.forEach 
    #     }         <- closes else
    
    # Find the section where the else block closes
    # After the last group.days.forEach row, we close with:
    #                     }  <- closes Row
    #                 }      <- closes group.days.forEach
    #             }          <- closes groupedByPeriod.forEach
    #         }              <- closes else
    
    # Let me look for this specific pattern
    # The end of the replacement looks like:
    #                             Text("...")
    #                         }
    #                     }  <- close of group.days.forEach
    #                     ... the above is the last line before the else closes
    
    # Find the last "}" that closes the groupedByPeriod.forEach loop
    # It should be at the same indent as "groupedByPeriod.forEach { group ->"  
    # which is 20 spaces
    
    # Actually, let me just add the pagination before the closing "}" of the else block
    # The else block's closing "}" is at 16 spaces indent
    # So I need to find: the last occurrence of "                }\n            }\n        }\n    }\n}\n\n@Composable"
    # and insert the pagination before it
    
    end_marker = "                }\n            }\n        }\n    }\n}\n\n@Composable\nfun ProfitKPICard("
    if end_marker in content:
        content = content.replace(
            end_marker,
            "" + pagination_buttons + "\n            }\n        }\n    }\n}\n\n@Composable\nfun ProfitKPICard(",
            1
        )
        print("OK: Added pagination buttons")
    else:
        print("WARN: Could not find end marker for pagination buttons")
else:
    print("INFO: Pagination buttons already present")
    
with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
