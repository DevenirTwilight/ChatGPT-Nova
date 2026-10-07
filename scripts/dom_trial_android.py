#!/usr/bin/env python3
"""Release DOM trial integration checks on an emulator, never a real-account proof."""
import re
import subprocess
from pathlib import Path
out = Path('dom-trial-results')
out.mkdir(exist_ok=True)
def adb(*args, timeout=300):
    return subprocess.check_output(['adb', *args], timeout=timeout, stderr=subprocess.STDOUT).decode(errors='replace')
try:
    for apk in ('dist/ChatGPT-Nova.apk', 'dist/ChatGPT-Nova-tests.apk'):
        assert 'Success' in adb('install', '-r', apk)
    result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                 'com.example.chatgptnova.DomTrialExportTest',
                 'com.example.chatgptnova.test/androidx.test.runner.AndroidJUnitRunner', timeout=600)
    (out/'instrumentation.txt').write_text(result)
    assert re.search(r'OK \(16 tests\)', result), result
    assert 'FAILURES!!!' not in result and 'INSTRUMENTATION_FAILED' not in result, result
    print('PASS: 16 legacy DOM fixture tests; 2 Gecko PDF cases explicitly retired; no live-account proof')
finally:
    (out/'logcat.txt').write_text(adb('logcat', '-d', '-v', 'threadtime'))
