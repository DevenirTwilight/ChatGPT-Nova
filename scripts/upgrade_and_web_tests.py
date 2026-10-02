#!/usr/bin/env python3
"""Exercise the signed Release APK, original-v1 upgrades and synthetic WebView fixtures."""
import json
import re
import subprocess
import sys
from pathlib import Path

OUT = Path('instrumented-results')
OUT.mkdir(exist_ok=True)
PACKAGE = 'com.example.chatgptnova'
RUNNER = PACKAGE + '.test/androidx.test.runner.AndroidJUnitRunner'
checks = []

def adb(*args, timeout=120):
    result = subprocess.run(['adb', *args], capture_output=True, timeout=timeout, check=True)
    return result.stdout.decode(errors='replace')

def install(path):
    result = adb('install', '-r', path)
    assert 'Success' in result, result

def suite(selection, filename):
    # Fresh Google APIs images can leave a boot-time Pixel Launcher ANR
    # covering a resumed test Activity. Restart only that emulator home process;
    # retain the focused-window and every Nova assertion below.
    (OUT/('windows-before-'+filename)).write_text(adb('shell','dumpsys','window','windows'))
    adb('shell','input','keyevent','224')
    adb('shell','wm','dismiss-keyguard')
    adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
    result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', selection, RUNNER, timeout=240)
    (OUT/filename).write_text(result)
    assert re.search(r'OK \(\d+ tests?\)', result), result
    assert 'FAILURES!!!' not in result and 'INSTRUMENTATION_FAILED' not in result, result
    return int(re.search(r'OK \((\d+) tests?\)', result).group(1))

try:
    release, tests, baseline, api = sys.argv[1:]
    install(baseline)
    install(tests)
    for permission in ('android.permission.CAMERA','android.permission.RECORD_AUDIO'):
        adb('shell','pm','grant',PACKAGE,permission)
    assert suite(PACKAGE+'.UpgradeTest#testSeedUpgradeData','v1-seed.txt') == 1
    adb('shell','am','force-stop',PACKAGE)
    install(release)
    assert suite(PACKAGE+'.UpgradeTest#testUpgradeDataPreserved','upgrade.txt') == 1
    checks.append('Original signed v1 is upgraded with install -r; synthetic cookie and localStorage survive process restart')
    count = suite(PACKAGE+'.NovaWebViewTest','webview-fixtures.txt')
    assert count == 9, count
    checks += ['Real WebView multiple-file input reads two synthetic documents and correct MIME types',
               'Delegated camera result remains readable after opening another chooser',
               'Real WebView blob download saves all 131089 fixture bytes',
               'HTTPS transport fixture streams 131089 bytes, scopes redirect cookies and refuses HTTP downgrade',
               'Media permission policy rejects untrusted top-level origins and unknown resources',
               'Network error retry is usable; Microsoft and Apple URLs are not preemptively blocked',
               'Confirmed clear removes synthetic cookies and localStorage',
               'Browser login requires explicit consent, starts a fresh official login URL and preserves independent WebView data',
               'Visible account controls drive signed-in, signed-out and unknown menus; sensitive actions stay in settings with confirmation']
    try:
        assert suite(PACKAGE+'.ClipboardProbeTest','clipboard-baseline.txt') == 3
    finally:
        adb('pull','/sdcard/Android/data/'+PACKAGE+'.test/files/clipboard-probe/.',str(OUT/'clipboard-probe'))
    checks.append('Baseline full-text clipboard checks: real long-press Paste, IME paste command and IME commitText; 1/10/50 KB, multiline, Markdown, Chinese/English and emoji; textarea and contenteditable')
    report = {'api':int(api),'passed':True,'checks':checks,
              'not_tested':['Real ChatGPT account authentication, long-term authenticated session and provider OAuth',
                            'Physical camera, live microphone capture and real authenticated ChatGPT attachments']}
    (OUT/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print(json.dumps(report,ensure_ascii=False))
except Exception as error:
    (OUT/'result.json').write_text(json.dumps({'passed':False,'checks':checks,'error':str(error)},ensure_ascii=False,indent=2))
    try:
        screenshot = subprocess.run(['adb','exec-out','screencap','-p'],capture_output=True,timeout=30,check=True)
        (OUT/'failure.png').write_bytes(screenshot.stdout)
        (OUT/'logcat.txt').write_text(adb('logcat','-d','-v','brief'))
    except Exception: pass
    raise
