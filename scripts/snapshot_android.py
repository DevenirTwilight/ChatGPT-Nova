#!/usr/bin/env python3
"""Controlled Android evidence, never a real-account or full-history proof."""
import json
import re
import subprocess
from pathlib import Path
out=Path('snapshot-results');out.mkdir(exist_ok=True)
failures=[]
def adb(*args,timeout=600):
    return subprocess.check_output(['adb',*args],timeout=timeout,stderr=subprocess.STDOUT).decode(errors='replace')
def independent(label,action):
    try:
        action()
    except Exception as error:
        failures.append({'check':label,'error':str(error)})
        print('FAIL:',label,str(error)[:500])
def suite(selection,label,count):
    def run():
        result=adb('shell','am','instrument','-w','-r','-e','class',selection,'com.example.chatgptnova.test/androidx.test.runner.AndroidJUnitRunner',timeout=900)
        (out/(label+'.txt')).write_text(result)
        assert re.search(r'OK \('+str(count)+r' tests?\)',result),result
        assert 'FAILURES!!!' not in result and 'INSTRUMENTATION_FAILED' not in result,result
    independent(label,run)
def html(name):
    text=(out/name).read_text(encoding='utf-8');assert text.startswith('<!doctype html>')
    for marker in ['Before snapshot marker','TAIL-SNAPSHOT-MARKER','CODE-LAST','TABLE-LAST','中文','café','E=mc^2']:
        assert marker in text,(name,marker)
    for marker in ['After snapshot marker','AFTER-TITLE','AFTER-TABLE','AFTER-BODY','CREDENTIAL-DO-NOT-SAVE','BUTTON-GARBAGE','MENU-GARBAGE','<script','onclick=']:
        assert marker not in text,(name,marker)
    assert '@media print' in text
def markdown():
    md=(out/'frozen-page.md').read_text(encoding='utf-8')
    for marker in ['Before snapshot marker','TAIL-SNAPSHOT-MARKER','````java','TABLE-LAST','> Quoted','3. Ordered','- Unordered','Reference','中文','café','😀','E=mc^2']:
        assert marker in md,marker
    for marker in ['After snapshot marker','AFTER-TABLE','AFTER-BODY','BUTTON-GARBAGE','SCRIPT-GARBAGE']:
        assert marker not in md,marker
def pdf(name='frozen-page.pdf'):
    subprocess.run(['pdftotext','-layout',str(out/name),str(out/(name+'.txt'))],check=True)
    text=(out/(name+'.txt')).read_text();logical=re.sub(r'\s+','',text)
    for marker in ['Beforesnapshotmarker','TAIL-SNAPSHOT-MARKER','CODE-LAST','TABLE-LAST','中文','café','Reference','Longparagraph']:
        assert marker in logical,('PDF',marker)
    assert 'Aftersnapshotmarker' not in logical and 'AFTER-TABLE' not in logical
    (out/(name+'.info.txt')).write_text(subprocess.check_output(['pdfinfo',str(out/name)]).decode())
try:
    suite('com.example.chatgptnova.FrozenPageSnapshotTest','snapshot',10)
    for name in ['frozen-page.md','frozen-page.html','frozen-page-saf.html','frozen-page.pdf']:
        independent('pull-'+name,lambda name=name:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name)))
    # Retain synthetic UI evidence only when the controlled print test failed.
    for name in ['snapshot-ui-failure.txt','snapshot-ui-failure.png']:
        try:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name))
        except Exception:pass
    independent('html-same-object',lambda:html('frozen-page.html'))
    independent('html-saf',lambda:html('frozen-page-saf.html'))
    independent('markdown-content',markdown)
    independent('pdf-content',pdf)
    independent('chromium-open',lambda:subprocess.run(['node','tools/snapshot/open-html.cjs'],check=True))
    for permission in ['CAMERA','RECORD_AUDIO']:adb('shell','pm','grant','com.example.chatgptnova','android.permission.'+permission)
    suite('com.example.chatgptnova.NovaWebViewTest','web-regressions',9)
    suite('com.example.chatgptnova.WebShareTest','share-regressions',5)
    suite('com.example.chatgptnova.ClipboardUiTest','input-regressions',5)
    for method in ['imeCommitText','imePasteCommand','longPressSystemPaste']:
        suite('com.example.chatgptnova.ClipboardProbeTest#'+method,'input-'+method,1)
    suite('com.example.chatgptnova.NativeUiTest#nativeControlsAndLifecycleRemainUsable','native-lifecycle',1)
    adb('shell','am','force-stop','com.example.chatgptnova')
    suite('com.example.chatgptnova.NativeUiTest#processRestartPreservesSyntheticSessionAndControls','native-restart',1)
    # Original signed v1 -> seed -> separate persistence check -> new signed APK.
    baseline=list(Path('baseline').rglob('ChatGPT-Nova.apk'))
    assert len(baseline)==1,baseline
    adb('uninstall','com.example.chatgptnova')
    assert 'Success' in adb('install',str(baseline[0]))
    suite('com.example.chatgptnova.UpgradeTest#testSeedUpgradeData','upgrade-seed',1)
    suite('com.example.chatgptnova.UpgradeTest#testSeedDataPersistedBeforeUpgrade','upgrade-persistence',1)
    assert 'Success' in adb('install','-r','dist/ChatGPT-Nova.apk')
    suite('com.example.chatgptnova.UpgradeTest#testUpgradeDataPreserved','upgrade-current',1)
    suite('com.example.chatgptnova.FirefoxSnapshotTest#sameFrozenHtmlCanBeSavedAsPdfInFirefoxAndroid','firefox-android',1)
    for name in ['firefox-source.html','firefox-frozen.pdf']:
        independent('pull-'+name,lambda name=name:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name)))
    for name in ['firefox-ui-failure.txt','firefox-ui-failure.png']:
        try:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name))
        except Exception:pass
    independent('firefox-pdf-content',lambda:pdf('firefox-frozen.pdf'))
    (out/'firefox-version.txt').write_text(adb('shell','dumpsys','package','org.mozilla.firefox'))
finally:
    (out/'checks.json').write_text(json.dumps({'failures':failures,'scope':'synthetic-fixtures'},ensure_ascii=False,indent=2))
    (out/'logcat.txt').write_text(adb('logcat','-d','-v','threadtime'))
assert not failures,json.dumps(failures,ensure_ascii=False)
print('PASS: actual same-snapshot HTML/MD/system-print-UI PDF files, Chromium HTML open, existing web/share/full input/native/original-v1 upgrade regressions; no live-account or manual Firefox proof')
