#!/usr/bin/env python3
"""Retain and retry a native check once for a recorded external startup disruption."""
import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

command = [sys.executable, 'scripts/native_ui_tests.py', *sys.argv[1:]]
first = subprocess.run(command)
if first.returncode == 0:
    sys.exit(0)

out = Path('smoke-results')
logfile = out/'logcat.txt'
log = logfile.read_text(errors='replace') if logfile.exists() else ''
provider_death = re.search(
    r'Killing \d+:com\.example\.chatgptnova/[^\n]*depends on provider '
    r'com\.google\.android\.gms/\.fonts\.provider\.FontsProvider in dying proc', log)
resultfile = out/'result.json'
report = json.loads(resultfile.read_text()) if resultfile.exists() else {}
# Fresh emulator boot can enable resource overlays after an Activity is resumed.
# Android reports CONFIG_ASSETS_PATHS (0x80000000), destroys the Activity and
# creates its replacement, closing a dialog being exercised by this test.
activities = set(re.findall(r'Lifecycle status change: com\.example\.chatgptnova\.MainActivity@(\w+) in: PRE_ON_CREATE', log))
overlay_restart = ('Config changes=80000000' in log and len(activities) > 1
                   and not report.get('checks')
                   and 'nativeControlsAndLifecycleRemainUsable' in report.get('error', ''))
if 'Process: com.example.chatgptnova,' in log or not (provider_death or overlay_restart):
    sys.exit(first.returncode)

# Preserve the failed attempt and every assertion. This recovery never clears
# Nova data, bypasses website verification, or retries a Nova application crash.
previous = Path('smoke-results-startup-restart')
shutil.move(str(out), str(previous))
reason = ('Observed GMS FontsProvider process death killed its Nova client' if provider_death
          else 'Observed emulator startup resource-overlay configuration recreated the Activity during a dialog check')
print(reason + '; rerunning all native checks once.', flush=True)
try:
    retry = subprocess.run(command)
finally:
    out.mkdir(exist_ok=True)
    shutil.move(str(previous), str(out/'external-startup-first-attempt'))
    (out/'external-startup-recovery.json').write_text(json.dumps({
        'reason': reason,
        'native_check_attempts': 2,
        'assertions_skipped': False,
        'nova_data_cleared_for_retry': False,
    }, indent=2))
sys.exit(retry.returncode)
