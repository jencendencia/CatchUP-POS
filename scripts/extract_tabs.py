#!/usr/bin/env python3
"""Remove private from profit/expenses composables, remove enum entries and when cases"""
import sys
sys.stdout.reconfigure(encoding='utf-8')

with open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'r', encoding='utf-8') as f:
    c = f.read()

changes = 0

# 1. Remove private from all helper composables
funcs = [
    'ProfitTabContent',
    'ProfitKPICard',
    'LegendDot',
    'ProfitMultiLineChart',
    'ProfitBreakdownDonut',
    'PageButton',
    'ExpensesTabContent',
    'ExpensesKPICard',
    'ExpenseDonutChart',
    'ExpenseLineChart',
    'SmallMetricCard',
    'AddExpenseDialog',
    'CategoryInputField',
    'DailySalesBarChart',
    'DailyOrdersBarChart',
]
for fname in funcs:
    old = 'private fun ' + fname + '('
    new = 'fun ' + fname + '('
    if old in c:
        c = c.replace(old, new)
        changes += 1
        print('  FUNC: ' + fname)

# 2. Remove PROFIT and EXPENSES from enum
old_enum = '    PRODUCTS("Products"),\n    PROFIT("Profit"),\n    EXPENSES("Expenses"),\n    CUSTOMERS("Customers"),'
new_enum = '    PRODUCTS("Products"),\n    CUSTOMERS("Customers"),'
if old_enum in c:
    c = c.replace(old_enum, new_enum)
    changes += 1
    print('  ENUM: PROFIT/EXPENSES removed')
else:
    print('  ENUM: pattern not matched')
    idx = c.find('PROFIT("Profit")')
    if idx >= 0:
        print('  ENUM: PROFIT still found in file')

# 3. Remove PROFIT case from when block
old_profit = '            ReportSubTab.PROFIT -> ProfitTabContent(\n                dailySales = profitPeriodSales,\n                kpiData = kpiData,\n                itemsSold = kpiData.totalItemsSold\n            )\n            ReportSubTab.EXPENSES -> ExpensesTabContent('
new_expenses_start = '            ReportSubTab.EXPENSES -> ExpensesTabContent('
if old_profit in c:
    c = c.replace(old_profit, new_expenses_start)
    changes += 1
    print('  WHEN: PROFIT case removed')
else:
    print('  WHEN: PROFIT case pattern not matched')

# 4. Remove EXPENSES case from when block
old_expenses = '            ReportSubTab.EXPENSES -> ExpensesTabContent(\n                expensesList = expensesList,\n                categoryTotals = expenseCategoryTotals,\n                kpiData = kpiData,\n                onExpenseSaved = { expenseRefreshCounter++ }\n            )\n            ReportSubTab.CUSTOMERS -> CustomersTabContent('
new_customers_start = '            ReportSubTab.CUSTOMERS -> CustomersTabContent('
if old_expenses in c:
    c = c.replace(old_expenses, new_customers_start)
    changes += 1
    print('  WHEN: EXPENSES case removed')
else:
    print('  WHEN: EXPENSES case pattern not matched')

with open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'w', encoding='utf-8') as f:
    f.write(c)
print('\nTotal changes: ' + str(changes))
