#!/usr/bin/env python3
"""Test the installed signed UI against fixture pages, not live authentication."""
import json
import re
import subprocess
import sys
from pathlib import Path

OUT = Path('smoke-results')
OUT.mkdir(exist_ok=True)
PACKAGE = 'com.example.chatgptnova'
RUNNER = PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner'
checks = []

def adb(*args, timeout=120):
    result = subprocess.run(['adb', *args], capture_output=True, timeout=timeout, check=True)
    return result.stdout.decode(errors='replace')

def test(method, filename):
    result = adb('shell','am','instrument','-w','-r','-e','class',
                 PACKAGE+'.NativeUiTest#'+method,RUNNER,timeout=240)
    (OUT/filename).write_text(result)
    assert re.search(r'OK \(1 test\)',result), result
    assert 'FAILURES!!!' not in result and 'INSTRUMENTATION_FAILED' not in result, result

try:
    assert 'Success' in adb('install','-r',sys.argv[1])
    adb('logcat','-c')
    adb('shell','input','keyevent','224')
    adb('shell','wm','dismiss-keyguard')
    adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
    test('nativeControlsAndLifecycleRemainUsable','native-ui.txt')
    checks += ['Signed Release Activity launches with the clean title and concise native menu',
               'Settings and About are reached by accessibility clicks and identify the unofficial client',
               'Login help offers browser fallback and explains separate sessions; internal login stays in Nova on the official URL using a fixture page',
               'Refresh and ChatGPT Home remain usable',
               'Cancel retains synthetic data; confirmed clearing removes cookies and localStorage',
               'Landscape and portrait preserve usable controls',
               'Android Back navigates inside Nova']
    adb('shell','am','force-stop',PACKAGE)
    test('processRestartPreservesSyntheticSessionAndControls','native-restart.txt')
    checks.append('After force-stop, the signed Activity restarts with synthetic cookies, localStorage and native controls preserved')
    log = adb('logcat','-d','-v','brief')
    (OUT/'logcat.txt').write_text(log)
    lines = log.splitlines()
    app_pids = set(re.findall(r'Start proc (\d+):'+re.escape(PACKAGE)+r'(?:/|:)',log))
    for i,line in enumerate(lines):
        if 'FATAL EXCEPTION' not in line: continue
        pid = re.search(r'AndroidRuntime\(\s*(\d+)\)',line)
        block = '\n'.join(lines[i:i+20])
        assert not ((pid and pid.group(1) in app_pids) or 'Process: '+PACKAGE+',' in block), 'Nova recorded a fatal exception'
    checks.append('No Nova FATAL EXCEPTION during the native fixture checks')
    adb('pull','/sdcard/Android/data/'+PACKAGE+'/files/native-smoke/.',str(OUT))
    for name in ('portrait','menu','settings','about','login-help','login-entry','landscape','restart-menu'):
        assert (OUT/(name+'.png')).read_bytes().startswith(b'\x89PNG\r\n\x1a\n')
    report = {'api':int(sys.argv[2]),'passed':True,'page_mode':'synthetic fixture served only by the test APK',
              'checks':checks,'not_tested':['Live ChatGPT login-page behavior and real Google/other account authentication',
                                          'Long-term authenticated sessions and physical camera/microphone',
                                          'Real authenticated attachments and downloads']}
    (OUT/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print(json.dumps(report,ensure_ascii=False))
except Exception as error:
    (OUT/'result.json').write_text(json.dumps({'passed':False,'checks':checks,'error':str(error)},ensure_ascii=False,indent=2))
    try: (OUT/'logcat.txt').write_text(adb('logcat','-d','-v','brief'))
    except Exception: pass
    try: adb('pull','/sdcard/Android/data/'+PACKAGE+'/files/native-smoke/.',str(OUT))
    except Exception: pass
    raise
