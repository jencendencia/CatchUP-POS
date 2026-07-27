import sys
sys.stdout.reconfigure(encoding='utf-8')

# ── 1. Fix ProfitTabContent in ReportsScreen.kt ──
f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'rb')
data = f.read()
f.close()

changes = []

# Change 1: Add realExpenses parameter to ProfitTabContent
old_sig = b'fun ProfitTabContent(\n    dailySales: List<DailySalesSummary>,\n    kpiData: KPIData,\n    itemsSold: Int\n) {'
new_sig = b'fun ProfitTabContent(\n    dailySales: List<DailySalesSummary>,\n    kpiData: KPIData,\n    itemsSold: Int,\n    realExpenses: Double = 0.0\n) {'

if old_sig in data:
    data = data.replace(old_sig, new_sig)
    changes.append('Added realExpenses param')
else:
    print('WARN: Could not find ProfitTabContent signature')
    # Try alternate encoding
    idx = data.find(b'fun ProfitTabContent(')
    if idx >= 0:
        print('Found at', idx, 'context:', repr(data[idx:idx+120]))

# Change 2: Replace totalExpenses = totalSales * 0.5 with realExpenses
old_expense = b'    val totalExpenses = totalSales * 0.5'
new_expense = b'    val totalExpenses = realExpenses'

if old_expense in data:
    data = data.replace(old_expense, new_expense)
    changes.append('Replaced hardcoded 50% with realExpenses')
else:
    print('WARN: Could not find totalExpenses = totalSales * 0.5')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'wb')
f.write(data)
f.close()
print('ReportsScreen.kt:', ', '.join(changes) if changes else 'NO CHANGES')

# ── 2. Fix ProfitScreen.kt ──
f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ProfitScreen.kt', 'rb')
data = f.read()
f.close()

changes = []

# Change 1: Add realExpenses state variable
old_state = b'    var itemsSold by remember { mutableIntStateOf(0) }'
new_state = b'    var itemsSold by remember { mutableIntStateOf(0) }\n    var realExpenses by remember { mutableDoubleStateOf(0.0) }'

if old_state in data:
    data = data.replace(old_state, new_state)
    changes.append('Added realExpenses state')
else:
    print('WARN: Could not find itemsSold state')

# Change 2: Add expense loading
old_load = b'        itemsSold = repository.getItemsSoldByDateRange(todayStart, todayEnd)'
new_load = b'        itemsSold = repository.getItemsSoldByDateRange(todayStart, todayEnd)\n        realExpenses = repository.getTotalExpensesByDateRange(todayStart, todayEnd)'

if old_load in data:
    data = data.replace(old_load, new_load)
    changes.append('Added realExpenses loading')
else:
    print('WARN: Could not find itemsSold loading line')

# Change 3: Add import for mutableDoubleStateOf
old_import = b'import androidx.compose.runtime.*'
new_import = b'import androidx.compose.runtime.*'

# No need to add import - mutableDoubleStateOf is already in the wildcard import

# Change 4: Pass realExpenses to ProfitTabContent
old_call = b'        ProfitTabContent(\n            dailySales = dailySales,\n            kpiData = kpiData,\n            itemsSold = itemsSold\n        )'
new_call = b'        ProfitTabContent(\n            dailySales = dailySales,\n            kpiData = kpiData,\n            itemsSold = itemsSold,\n            realExpenses = realExpenses\n        )'

if old_call in data:
    data = data.replace(old_call, new_call)
    changes.append('Passed realExpenses to ProfitTabContent')
else:
    print('WARN: Could not find ProfitTabContent call')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ProfitScreen.kt', 'wb')
f.write(data)
f.close()
print('ProfitScreen.kt:', ', '.join(changes) if changes else 'NO CHANGES')

print('\nDONE')
