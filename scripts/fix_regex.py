import sys
sys.stdout.reconfigure(encoding='utf-8')

f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'rb')
data = f.read()
f.close()

# Find the regex pattern
idx = data.find(b'matches(Regex')
if idx >= 0:
    snippet = data[idx-5:idx+120]
    print('Found at byte offset:', idx)
    print('REPR:', repr(snippet))
    
    # The bug: quadruple-escaped backslash sequences in Kotlin source
    # Current: "^-?\\\\d*\\.?\\\\d*$"  (too many backslashes)
    # Correct: "^-?\\d*\\.?\\d*$"       (proper regex for digits and optional decimal)
    
    # Find the exact Regex string content between the quotes
    quote_start = data.find(b'"^-?', idx)
    if quote_start >= 0:
        quote_end = data.find(b'$")', quote_start)
        if quote_end >= 0:
            old_str = data[quote_start:quote_end+3]
            print('OLD string:', repr(old_str))
            
            # Build the correct regex string
            new_str = b'"^-?\\\\d*\\\\.?\\\\d*$")'
            print('NEW string:', repr(new_str))
            
            data = data.replace(old_str, new_str)
            f = open('app/src/main/java/com/catchuppos/app/ui/dashboard/ReportsScreen.kt', 'wb')
            f.write(data)
            f.close()
            print('Fixed successfully!')
        else:
            print('Could not find end quote')
    else:
        print('Could not find start quote')
else:
    print('Pattern not found in file')
