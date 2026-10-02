#!/usr/bin/env python3
"""Rerun the entire native check once only for an observed external font-provider death."""
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
if not provider_death or 'Process: com.example.chatgptnova,' in log:
    sys.exit(first.returncode)

# Preserve the failed attempt and every assertion. This recovery never clears
# Nova data, bypasses website verification, or retries a Nova application crash.
previous = Path('smoke-results-provider-restart')
shutil.move(str(out), str(previous))
print('System killed Nova as a client of a restarting GMS font provider; rerunning all native checks once.', flush=True)
try:
    retry = subprocess.run(command)
finally:
    out.mkdir(exist_ok=True)
    shutil.move(str(previous), str(out/'external-provider-first-attempt'))
    (out/'external-provider-recovery.json').write_text(json.dumps({
        'reason': 'Observed GMS FontsProvider process death killed its Nova client',
        'native_check_attempts': 2,
        'assertions_skipped': False,
        'nova_data_cleared_for_retry': False,
    }, indent=2))
sys.exit(retry.returncode)
