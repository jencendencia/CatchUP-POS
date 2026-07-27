import sys
sys.stdout.reconfigure(encoding='utf-8')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'rb')
data = f.read()
f.close()

changes = []

# ── Fix: Revert DateRangeDialog's onDismissRequest back ──
# The first occurrence (in DateRangeDialog) was incorrectly changed to { }
# We need to put it back to onDismiss
# Find the DateRangeDialog section and fix it
date_range_idx = data.find(b'fun DateRangeDialog(')
if date_range_idx >= 0:
    # Find onDismissRequest = { } in this section
    section = data[date_range_idx:date_range_idx+1000]
    noop_idx = section.find(b'onDismissRequest = { }')
    if noop_idx >= 0:
        # Only replace within DateRangeDialog section
        actual_idx = date_range_idx + noop_idx
        old = b'onDismissRequest = { }'
        new = b'onDismissRequest = onDismiss'
        # Make sure we're replacing within the DateRangeDialog
        data = data[:actual_idx] + new + data[actual_idx+len(old):]
        changes.append('Reverted DateRangeDialog onDismissRequest back to onDismiss')
        print('Reverted DateRangeDialog')
    else:
        # Check if it's already correct
        if b'onDismissRequest = onDismiss' in section:
            print('DateRangeDialog already has onDismiss - no revert needed')
        else:
            print('WARN: DateRangeDialog section not as expected')
            # Show what's there
            disp_idx = section.find(b'onDismissRequest')
            if disp_idx >= 0:
                print('  Found:', repr(section[disp_idx:disp_idx+40]))
else:
    print('DateRangeDialog not found')

# ── Fix: Change only AddExpenseDialog's onDismissRequest ──
add_expense_idx = data.find(b'fun AddExpenseDialog(')
if add_expense_idx >= 0:
    # Find the AlertDialog call within AddExpenseDialog
    # Look for onDismissRequest after add_expense_idx
    # Find the AlertDialog start
    alert_idx = data.find(b'AlertDialog(', add_expense_idx)
    if alert_idx >= 0:
        # Find onDismissRequest within alert dialog section
        section = data[alert_idx:alert_idx+2000]
        dismiss_idx = section.find(b'onDismissRequest')
        if dismiss_idx >= 0:
            actual_dismiss_idx = alert_idx + dismiss_idx
            old_dismiss = data[actual_dismiss_idx:actual_dismiss_idx+50]
            print('AddExpenseDialog onDismissRequest:', repr(old_dismiss))
            
            # Replace whatever the current value is with { }
            # Find the end of this line
            line_end = data.find(b'\n', actual_dismiss_idx)
            if line_end >= 0:
                line = data[actual_dismiss_idx:line_end]
                print('Full line:', repr(line))
                
                # Replace just the value part
                if b'= onDismiss' in line:
                    new_line = line.replace(b'= onDismiss', b'= { }')
                    data = data[:actual_dismiss_idx] + new_line + data[line_end:]
                    changes.append('Changed AddExpenseDialog onDismissRequest to no-op')
                    print('Changed AddExpenseDialog onDismissRequest to no-op')
                elif b'= { }' in line:
                    print('AddExpenseDialog already has no-op - OK')
                else:
                    print('Unexpected pattern')
        else:
            print('onDismissRequest not found in AddExpenseDialog AlertDialog')
else:
    print('AddExpenseDialog not found')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'wb')
f.write(data)
f.close()
print('\nChanges:', ', '.join(changes) if changes else 'NONE')
