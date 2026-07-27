"""
Restore ReportsScreen.kt - reconstruct from the content that was read earlier in the conversation,
append the missing tail, then apply the perDayItemsSold changes.
"""
import os

reports_path = "app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"

# Read the corrupted file (just a path string)
with open(reports_path, "r", encoding="utf-8") as f:
    corrupted = f.read()

# The corrupted file contains just a single line with the path string
# We need to restore it by reading the full content from somewhere
# Since we don't have a backup, let's check if there's a .bak or temp file
# Or we can regenerate from the build

# Actually, the simplest fix: fix the original script and re-run it
# The original script had the content in r_content but wrote reports_path instead
# Let me re-create the script with the fix

# First, let me extract what I can from the conversation and rebuild
# The easiest approach: the file was read in the conversation. Let me save that content.

# Actually, let me check if the build/classes directory has the original
build_dir = "app/build"
print("Looking for backup sources...")
# We can't recover from build directory

# The MOST practical approach: the last read_files output shows the complete file
# I need to extract it from there and save it. But I can't access the output programmatically.

# Instead, let me check if there's a cached version in the gradle cache
import subprocess
result = subprocess.run(["findstr", "package", reports_path], capture_output=True, text=True)
print(f"Current file first line: {result.stdout[:50] if result.stdout else 'EMPTY'}")

# Alternative: check if HEAD contains the file
result2 = subprocess.run(["git", "show", "HEAD:app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt"], 
                         capture_output=True, text=True)
if result2.stdout:
    print(f"Found in git HEAD: {len(result2.stdout)} chars")
    with open(reports_path, "w", encoding="utf-8") as f:
        f.write(result2.stdout)
    print("[OK] Restored ReportsScreen.kt from git HEAD")
else:
    print("Not in git HEAD:", result2.stderr[:200])

print("Done")
