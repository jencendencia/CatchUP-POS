# Fix missing @Composable annotation on ProfitKPICard

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

old_text = "}\nfun ProfitKPICard(label: String, value: String, valueColor: Color, trend: String, modifier: Modifier = Modifier) {"
new_text = "}\n\n@Composable\nfun ProfitKPICard(label: String, value: String, valueColor: Color, trend: String, modifier: Modifier = Modifier) {"

if old_text in content:
    content = content.replace(old_text, new_text, 1)
    print("OK: Added @Composable annotation")
else:
    print("WARN: Could not find pattern for replacement")
    # Try another variant
    old_text2 = "}\nfun ProfitKPICard("
    new_text2 = "}\n\n@Composable\nfun ProfitKPICard("
    if old_text2 in content:
        content = content.replace(old_text2, new_text2, 1)
        print("OK: Added @Composable via fallback pattern")
    else:
        print("FAIL: Could not find insertion point")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
