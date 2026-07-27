"""Fix accidental replacements in ExpensesTabContent that should have stayed unchanged."""

filepath = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

with open(filepath, "r", encoding="utf-8") as f:
    content = f.read()

count = 0

# The replacements accidentally happened in the ExpensesTabContent section
# (after line ~1790). We need to revert those specific occurrences.
# Strategy: Find the SECOND occurrence of each pattern (the first is in ProfitTabContent, the second in ExpensesTabContent).

# 1. Fix onClick = onDateClick in ExpensesTabContent (line ~1798)
# There are 3 occurrences: SubTabAndFilterRow (line 389), ProfitTabContent (line 1398), ExpensesTabContent (line 1798)
# We want to revert the 3rd one.
# Find all occurrences - replace only the one after "ExpensesTabContent" / "Daily Expenses Record"

# Actually, let's use a targeted approach: find the specific pattern that's unique to the ExpensesTabContent section
# The ExpensesTabContent has "| DAILY EXPENSES RECORD" nearby

old_dc = "                    onClick = onDateClick,\n                    shape = RoundedCornerShape(10.dp),\n                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),\n                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextWhite),\n                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),\n                    modifier = Modifier.height(38.dp)\n                ) {\n                    Icon(Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))\n                    Spacer(modifier = Modifier.width(6.dp))\n                    Text(if (selectedDateLabel.isNotEmpty()) selectedDateLabel else SimpleDateFormat(\"MMMM dd, yyyy\", Locale.US).format(Date()), style = MaterialTheme.typography.bodySmall, maxLines = 1)"
new_dc = "                    onClick = { /* Calendar */ },\n                    shape = RoundedCornerShape(10.dp),\n                    border = androidx.compose.foundation.BorderStroke(1.dp, SubtleWhite),\n                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkCard, contentColor = TextWhite),\n                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),\n                    modifier = Modifier.height(38.dp)\n                ) {\n                    Icon(Icons.Default.DateRange, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))\n                    Spacer(modifier = Modifier.width(6.dp))\n                    Text(SimpleDateFormat(\"MMMM dd, yyyy\", Locale.US).format(Date()), style = MaterialTheme.typography.bodySmall, maxLines = 1)"

# Replace only the LAST occurrence (in ExpensesTabContent)
last_dc_idx = content.rfind(old_dc)
if last_dc_idx >= 0:
    content = content[:last_dc_idx] + new_dc + content[last_dc_idx + len(old_dc):]
    count += 1
    print("[OK] Fixed last occurrence of date button in ExpensesTabContent")
else:
    print("[WARN] Could not find date button pattern in ExpensesTabContent")

with open(filepath, "w", encoding="utf-8") as f:
    f.write(content)

print(f"\n[OK] {count} fix(es) applied!")
