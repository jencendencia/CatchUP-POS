# Fix missing closing braces for ProfitTabContent function
# Need to add:
#     }   (close outer Column at 4 spaces)
# }       (close function body at 0 spaces)
# before fun ProfitKPICard

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "r", encoding="utf-8") as f:
    content = f.read()

# Insert missing braces before "fun ProfitKPICard"
old_text = "        }\nfun ProfitKPICard("
new_text = "        }\n    }\n}\nfun ProfitKPICard("

if old_text in content:
    content = content.replace(old_text, new_text, 1)
    print("OK: Added missing closing braces before fun ProfitKPICard")
else:
    print("WARN: Could not find exact pattern. Trying flexible match...")
    import re
    # Try to match the pattern with flexible whitespace
    match = re.search(r"(\s{8}\})\s*\n\s*fun ProfitKPICard\(", content)
    if match:
        full_match = match.group(0)
        replacement = "        }\n    }\n}\nfun ProfitKPICard("
        content = content.replace(full_match, replacement, 1)
        print("OK: Added missing closing braces via regex")
    else:
        print("FAIL: Could not find the insertion point")

with open("app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt", "w", encoding="utf-8") as f:
    f.write(content)

print("OK: File saved")
