import sys
sys.stdout.reconfigure(encoding='utf-8')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'rb')
data = f.read()
f.close()

# Find the ProfitTabContent function signature
idx = data.find(b'fun ProfitTabContent(')
print('Found fun ProfitTabContent at byte', idx)

if idx >= 0:
    # Show the full signature
    sig = data[idx:idx+200]
    print('Current signature:', repr(sig))
    
    # Try to find the exact pattern to match
    old_sig = b'fun ProfitTabContent(\n    dailySales: List<DailySalesSummary>,\n    kpiData: KPIData,\n    itemsSold: Int\n) {'
    
    search_idx = data.find(old_sig)
    if search_idx >= 0:
        print(f'Found exact match at byte {search_idx}')
        new_sig = b'fun ProfitTabContent(\n    dailySales: List<DailySalesSummary>,\n    kpiData: KPIData,\n    itemsSold: Int,\n    realExpenses: Double = 0.0\n) {'
        data = data.replace(old_sig, new_sig, 1)
        print('Signature updated!')
    else:
        print('Exact match not found')
        # Try with different whitespace
        # Look for the closing paren
        end_idx = data.find(b'\n) {', idx)
        if end_idx >= 0:
            sig_text = data[idx:end_idx+4]
            print('Actual signature:', repr(sig_text))
            
            # Try to append the parameter before the closing paren
            # Check if realExpenses is already there
            if b'realExpenses' in sig_text:
                print('realExpenses already present in signature')
            else:
                # Find the last parameter line
                last_param_start = data.rfind(b'    itemsSold: Int', idx, end_idx)
                if last_param_start >= 0:
                    old_param = data[last_param_start:end_idx]
                    new_param = b'    itemsSold: Int,\n    realExpenses: Double = 0.0\n) {'
                    data = data.replace(old_param, new_param, 1)
                    print('Signature updated via alternate method!')
                else:
                    print('Could not find itemsSold param in the signature')
else:
    print('ProfitTabContent not found!')

# Also check the function body for totalExpenses = realExpenses
if b'val totalExpenses = realExpenses' in data:
    print('totalExpenses = realExpenses already set')
else:
    print('WARNING: totalExpenses = realExpenses NOT FOUND')
    # Check what's there
    exp_idx = data.find(b'val totalExpenses')
    if exp_idx >= 0:
        print('Found totalExpenses at', exp_idx, ':', repr(data[exp_idx:exp_idx+60]))

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'wb')
f.write(data)
f.close()
print('\nDONE')
