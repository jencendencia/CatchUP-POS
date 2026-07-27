import sys
sys.stdout.reconfigure(encoding='utf-8')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'rb')
data = f.read()
f.close()

changes = []

# ── FIX 1: Duplicate ) { in ProfitTabContent ──
# The signature currently looks like:
# fun ProfitTabContent(
#     ...
#     realExpenses: Double = 0.0
# ) {
# ) {
# Need to remove the duplicate
idx = data.find(b'    realExpenses: Double = 0.0\n) {\n) {')
if idx >= 0:
    old = b'    realExpenses: Double = 0.0\n) {\n) {'
    new = b'    realExpenses: Double = 0.0\n) {'
    data = data.replace(old, new, 1)
    changes.append('Fixed duplicate ) {')
    print('FIX 1: Fixed duplicate ) { in ProfitTabContent')
else:
    # Try alternate pattern
    idx2 = data.find(b'realExpenses: Double = 0.0\n)')
    if idx2 >= 0:
        print('realExpenses line found at', idx2, 'context:', repr(data[idx2:idx2+40]))
        # Check what comes after
        after = data[idx2:idx2+50]
        print('After realExpenses:', repr(after))
        
        # Try to find and fix the duplicate
        if b') {\n) {' in after or b')\n) {' in after:
            print('Duplicate detected!')
            # Find the precise location
            brace_pos = data.find(b') {', idx2)
            if brace_pos >= 0:
                # Check if there's another ) { right after
                rest = data[brace_pos:brace_pos+30]
                print('Brace context:', repr(rest))
                
    else:
        print('realExpenses line not found')

# ── FIX 2: Make onDismissRequest a no-op in AddExpenseDialog ──
# Find the AddExpenseDialog's onDismissRequest
# Looking for the pattern: onDismissRequest = onDismiss,  (inside AddExpenseDialog)
# We can identify AddExpenseDialog specifically by context
idx3 = data.find(b'fun AddExpenseDialog(')
if idx3 >= 0:
    # Find onDismissRequest in AddExpenseDialog (between this function and next function)
    # Find the AlertDialog's onDismissRequest
    dismiss_idx = data.find(b'onDismissRequest = onDismiss,', idx3)
    # There might be multiple onDismissRequest lines. The AddExpenseDialog one is at line ~1972
    # Find the one with shape = RoundedCornerShape(20.dp) nearby (unique to AddExpenseDialog)
    
    # Actually let me search for the specific context. Looking for the shape + onDismissRequest combo
    shape_idx = data.find(b'shape = RoundedCornerShape(20.dp),', idx3)
    if shape_idx >= 0:
        # Find the onDismissRequest before this shape
        search_start = shape_idx - 200
        if search_start < 0: search_start = 0
        dismiss_idx = data.find(b'onDismissRequest', search_start)
        if dismiss_idx >= 0 and dismiss_idx < shape_idx:
            old_dismiss = data[dismiss_idx:dismiss_idx+40]
            print('AddExpenseDialog dismiss line:', repr(old_dismiss))
            data = data.replace(b'onDismissRequest = onDismiss,', b'onDismissRequest = { },', 1)
            changes.append('Made onDismissRequest a no-op')
            print('FIX 2: Made onDismissRequest a no-op')
else:
    print('AddExpenseDialog not found')

# ── FIX 3: Widen CategoryInputField text field ──
# Change Modifier.width(120.dp).height(44.dp) to Modifier.width(180.dp).height(44.dp)
old_width = b'Modifier.width(120.dp).height(44.dp)'
new_width = b'Modifier.width(180.dp).height(44.dp)'
if old_width in data:
    data = data.replace(old_width, new_width)
    changes.append('Widened input field from 120dp to 180dp')
    print('FIX 3: Widened input field from 120dp to 180dp')
else:
    print('FIX 3: Pattern not found')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'wb')
f.write(data)
f.close()
print('\nChanges applied:', ', '.join(changes) if changes else 'NONE')
