#!/usr/bin/env python3
"""Run the keyboard regression suite on a debug APK without release signing keys."""
import json
import re
import subprocess
import sys
from pathlib import Path

out = Path('clipboard-results')
out.mkdir(exist_ok=True)
package = 'com.example.chatgptnova.debug'
runner = package + '.test/androidx.test.runner.AndroidJUnitRunner'

def adb(*args, timeout=120):
    result = subprocess.run(['adb', *args], capture_output=True, timeout=timeout, check=True)
    return result.stdout.decode(errors='replace')

passed = False
try:
    apk, tests, api = sys.argv[1:]
    for path in (apk, tests):
        assert 'Success' in adb('install', '-r', path)
    adb('shell', 'input', 'keyevent', '224')
    adb('shell', 'wm', 'dismiss-keyguard')
    adb('shell', 'am', 'force-stop', 'com.google.android.apps.nexuslauncher')
    result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class',
                 'com.example.chatgptnova.ClipboardProbeTest', runner, timeout=900)
    (out / 'instrumentation.txt').write_text(result)
    passed = bool(re.search(r'OK \(9 tests\)', result)) and not any(
        marker in result for marker in ('FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed'))
    (out / 'result.json').write_text(json.dumps({'api': int(api), 'passed': passed,
        'page': 'locally served test APK fixtures; not an authenticated ChatGPT session'}, indent=2))
finally:
    for args, filename in ((('logcat', '-d', '-v', 'brief'), 'logcat.txt'),
                           (('shell', 'dumpsys', 'webviewupdate'), 'webview.txt')):
        try:
            (out / filename).write_text(adb(*args))
        except Exception as error:
            (out / (filename + '.error')).write_text(str(error))
    subprocess.run(['adb', 'pull', '/sdcard/Android/data/' + package + '/files/clipboard-probe/.',
                    str(out / 'clipboard-probe')], capture_output=True, timeout=60)
if not passed:
    raise SystemExit('Clipboard instrumentation failed; see clipboard-results/instrumentation.txt')
print('All 9 clipboard regression tests passed on Android API ' + api)
