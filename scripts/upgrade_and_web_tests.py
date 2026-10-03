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
failures = []
diagnostics = []

def independent(name, operation, required=True):
    try:
        return operation()
    except Exception as error:
        (failures if required else diagnostics).append({"suite": name, "error": str(error)})
        # Independent suites continue, and the later UI stage clears logcat.
        # Keep evidence now so a crashed instrumentation process can be diagnosed.
        prefix = OUT / ('failure-' + re.sub(r'[^A-Za-z0-9_-]', '_', name))
        try:
            prefix.with_name(prefix.name + '-logcat.txt').write_text(adb('logcat', '-d', '-v', 'threadtime'))
        except Exception:
            pass
        try:
            screenshot = subprocess.run(['adb', 'exec-out', 'screencap', '-p'], capture_output=True, timeout=30, check=True)
            prefix.with_suffix('.png').write_bytes(screenshot.stdout)
        except Exception:
            pass
        return None

def adb(*args, timeout=120):
    result = subprocess.run(['adb', *args], capture_output=True, timeout=timeout, check=True)
    return result.stdout.decode(errors='replace')

def install(path):
    result = adb('install', '-r', path)
    assert 'Success' in result, result

def suite(selection, filename, allow_provider_retry=True):
    # Fresh Google APIs images can leave a boot-time Pixel Launcher ANR
    # covering a resumed test Activity. Restart only that emulator home process;
    # retain the focused-window and every Nova assertion below.
    (OUT/('windows-before-'+filename)).write_text(adb('shell','dumpsys','window','windows'))
    adb('shell','input','keyevent','224')
    adb('shell','wm','dismiss-keyguard')
    adb('shell','am','force-stop','com.google.android.apps.nexuslauncher')
    adb('logcat','-c')
    result = adb('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', selection, RUNNER, timeout=240)
    (OUT/filename).write_text(result)
    if allow_provider_retry and 'Process crashed.' in result:
        log = adb('logcat', '-d', '-v', 'threadtime')
        provider_death = re.search(
            r'Killing \d+:com\.example\.chatgptnova/[^\n]*depends on provider '
            r'com\.google\.android\.gms/\.fonts\.provider\.FontsProvider in dying proc', log)
        if provider_death and 'Process: com.example.chatgptnova,' not in log:
            (OUT/(filename+'.external-provider-first-attempt.txt')).write_text(result)
            (OUT/(filename+'.external-provider-logcat.txt')).write_text(log)
            recovery = {'suite': selection, 'reason': 'Observed GMS FontsProvider death killed its Nova client',
                        'attempts': 2, 'assertions_skipped': False, 'nova_data_cleared_for_retry': False}
            diagnostics.append(recovery)
            (OUT/(filename+'.external-provider-recovery.json')).write_text(json.dumps(recovery, indent=2))
            print(recovery['reason'] + '; rerunning the complete suite once.', flush=True)
            return suite(selection, filename, allow_provider_retry=False)
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
    # Observe persisted baseline data before installing the candidate. A marker
    # lost while seeding v1 must not be mistaken for a candidate upgrade failure.
    assert suite(PACKAGE+'.UpgradeTest#testSeedDataPersistedBeforeUpgrade','v1-persisted.txt') == 1
    adb('shell','am','force-stop',PACKAGE)
    install(release)
    upgrade_count = independent('upgrade', lambda: suite(PACKAGE+'.UpgradeTest#testUpgradeDataPreserved','upgrade.txt'))
    if upgrade_count == 1:
        checks.append('Original signed v1 is upgraded with install -r; synthetic cookie and localStorage survive process restart')
    share_count = independent('web-share', lambda: suite(PACKAGE+'.WebShareTest','web-share-fixtures.txt'))
    if share_count == 5:
        checks.append('Synthetic webpage navigator.share launches the Android Sharesheet with the supplied public URL; target callback, cancellation, invalid data, user gesture, SPA/reload and origin/frame restrictions are exercised')
    clipboard_ui_count = independent('clipboard-ui', lambda: suite(PACKAGE+'.ClipboardUiTest','clipboard-ui.txt'))
    if clipboard_ui_count == 5:
        checks.append('Clipboard menu preserves selections and SPA replacement; native-menu and system long pastes reach a page-owned transaction, retain undo and paint within the fixture budget')
    try:
        clipboard_count = 0
        for method in ('nativeMenuPaste','imeCommitText','imePasteCommand','longPressSystemPaste'):
            value = independent('clipboard-'+method, lambda method=method: suite(PACKAGE+'.ClipboardProbeTest#'+method,'clipboard-'+method+'.txt'), required=method!='imeCommitText')
            if value == 1: clipboard_count += 1
    finally:
        adb('pull','/sdcard/Android/data/'+PACKAGE+'/files/clipboard-probe/.',str(OUT/'clipboard-probe'))
    if clipboard_count == 4:
        checks.append('Baseline full-text clipboard checks: real long-press Paste, IME paste command and IME commitText; 1/10/50 KB, multiline, Markdown, Chinese/English and emoji; textarea and contenteditable')
    count = independent('webview', lambda: suite(PACKAGE+'.NovaWebViewTest','webview-fixtures.txt'))
    if count == 9:
        checks += ['Real WebView multiple-file input reads two synthetic documents and correct MIME types',
               'Delegated camera result remains readable after opening another chooser',
               'Real WebView blob download saves all 131089 fixture bytes',
               'HTTPS transport fixture streams 131089 bytes, scopes redirect cookies and refuses HTTP downgrade',
               'Media permission policy rejects untrusted top-level origins and unknown resources',
               'Network error retry is usable; Microsoft and Apple URLs are not preemptively blocked',
               'Confirmed clear removes synthetic cookies and localStorage',
               'Browser login requires explicit consent, starts a fresh official login URL and preserves independent WebView data',
               'Visible account controls drive signed-in, signed-out and unknown menus; sensitive actions stay in settings with confirmation']
    report = {'api':int(api),'passed':not failures,'checks':checks,'failures':failures,'diagnostics':diagnostics,
              'not_tested':['Real ChatGPT account authentication, long-term authenticated session and provider OAuth',
                            'The live ChatGPT conversation Share button and actual delivery to external share targets on a physical device',
                            'Physical camera, live microphone capture and real authenticated ChatGPT attachments']}
    (OUT/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print(json.dumps(report,ensure_ascii=False))
    if failures: sys.exit(1)
except Exception as error:
    (OUT/'result.json').write_text(json.dumps({'passed':False,'checks':checks,'error':str(error)},ensure_ascii=False,indent=2))
    try:
        screenshot = subprocess.run(['adb','exec-out','screencap','-p'],capture_output=True,timeout=30,check=True)
        (OUT/'failure.png').write_bytes(screenshot.stdout)
        (OUT/'logcat.txt').write_text(adb('logcat','-d','-v','brief'))
    except Exception: pass
    raise
