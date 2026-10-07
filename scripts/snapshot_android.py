#!/usr/bin/env python3
"""Controlled Android evidence, never a real-account or full-history proof."""
import json
import re
import os
import hashlib
import zipfile
import unicodedata
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
    for marker in ['After snapshot marker','AFTER-TITLE','AFTER-TABLE','AFTER-BODY','CREDENTIAL-DO-NOT-SAVE','BUTTON-GARBAGE','MENU-GARBAGE','CLIPPED-UI-CONTROL','<script','onclick=']:
        assert marker not in text,(name,marker)
    assert '@media print' in text
def markdown():
    md=(out/'frozen-page.md').read_text(encoding='utf-8')
    for marker in ['Before snapshot marker','TAIL-SNAPSHOT-MARKER','````java','TABLE-LAST','> Quoted','3. Ordered','- Unordered','Reference','中文','café','😀','E=mc^2']:
        assert marker in md,marker
    for marker in ['After snapshot marker','AFTER-TABLE','AFTER-BODY','BUTTON-GARBAGE','CLIPPED-UI-CONTROL','SCRIPT-GARBAGE']:
        assert marker not in md,marker
def pdf(name='frozen-page.pdf'):
    subprocess.run(['pdftotext','-layout',str(out/name),str(out/(name+'.txt'))],check=True)
    text=unicodedata.normalize('NFKC',(out/(name+'.txt')).read_text());logical=re.sub(r'\s+','',text)
    before_method='pdf-text'
    ocr_logical=''
    if name=='firefox-frozen.pdf' and 'Beforesnapshotmarker' not in logical:
        # Firefox draws synthetic bold headings without extractable glyph text.
        # Preserve the first-marker assertion against actual rendered PDF pixels.
        prefix=out/(name+'-first-page')
        subprocess.run(['pdftoppm','-f','1','-l','1','-r','144','-singlefile','-png',str(out/name),str(prefix)],check=True)
        ocr=subprocess.check_output(['tesseract',str(prefix)+'.png','stdout','-l','eng'],stderr=subprocess.DEVNULL).decode()
        (out/(name+'.first-page-ocr.txt')).write_text(ocr)
        ocr_logical=re.sub(r'\s+','',unicodedata.normalize('NFKC',ocr))
        assert 'Beforesnapshotmarker' in ocr_logical,('PDF rendered first marker',name)
        before_method='first-page-render-ocr'
    else:
        assert 'Beforesnapshotmarker' in logical,('PDF first marker',name)
    for marker in ['TAIL-SNAPSHOT-MARKER','CODE-LAST','TABLE-LAST','中文','café','Reference','Longparagraph','Noël','naïve','français','E=mc^2','LONG-CODE-HEAD','LONG-CODE-TAIL','LONG-TABLE-TAIL']:
        assert marker in logical,('PDF',marker)
    for late in ['Aftersnapshotmarker','AFTER-TITLE','AFTER-TABLE','AFTER-BODY','CLIPPED-UI-CONTROL']:assert late not in logical+ocr_logical,late
    images=subprocess.check_output(['pdfimages','-list',str(out/name)]).decode();(out/(name+'.images.txt')).write_text(images)
    assert re.search(r'\b320\s+120\b',images),('PDF fixture image',name)
    (out/(name+'.urls.txt')).write_text(subprocess.check_output(['pdfinfo','-url',str(out/name)]).decode())
    (out/(name+'.info.txt')).write_text(subprocess.check_output(['pdfinfo',str(out/name)]).decode())
    (out/(name+'.verification.json')).write_text(json.dumps({'beforeMarker':before_method,'otherMarkers':'pdf-text','unicodeNormalization':'NFKC','lateMarkersAbsent':True,'fixtureImage':'320x120'},indent=2))
def assert_same_print_source():
    assert (out/'firefox-source.html').read_bytes()==(out/'frozen-page-print-source.html').read_bytes(), 'Firefox A/B source differs from actual System Print input'

def assert_print_markdown():
    text=(out/'frozen-page-print-source.md').read_text(encoding='utf-8')
    assert 'Before snapshot marker' in text and 'TAIL-SNAPSHOT-MARKER' in text
    for marker in ['After snapshot marker','AFTER-TABLE','AFTER-TITLE','AFTER-BODY']:assert marker not in text

def installed_metrics(apk_path='dist/ChatGPT-Nova.apk',name='package-metrics.json',revision=None):
    apk=Path(apk_path);info={'sourceCommit':revision or os.environ.get('GITHUB_SHA','unknown'),'android':adb('shell','getprop','ro.build.version.sdk').strip(),'apkBytes':apk.stat().st_size,'scope':'installed code directory; excludes app data/cache and shared system WebView'}
    with apk.open('rb') as stream:info['apkSha256']=hashlib.file_digest(stream,'sha256').hexdigest()
    with zipfile.ZipFile(apk) as z:info['nativeAbis']=sorted({n.split('/')[1] for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')})
    path=adb('shell','pm','path','com.example.chatgptnova').strip().removeprefix('package:')
    try:info['installedBaseApkBytes']=int(adb('shell','stat','-c','%s',path).strip())
    except Exception:info['installedBaseApkBytes']=None
    try:info['installedCodeAllocatedBytes']=int(adb('shell','du','-sk',str(Path(path).parent)).split()[0])*1024
    except Exception:info['installedCodeAllocatedBytes']=None
    (out/name).write_text(json.dumps(info,indent=2))

try:
    independent('installed-package-metrics',installed_metrics)
    suite('com.example.chatgptnova.FrozenPageSnapshotTest','snapshot',10)
    for name in ['frozen-page.md','frozen-page.html','frozen-page-saf.html','frozen-page-print-source.html','frozen-page-print-source.md','frozen-page.pdf']:
        independent('pull-'+name,lambda name=name:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name)))
    # Retain synthetic UI evidence only when the controlled print test failed.
    for name in ['snapshot-ui-failure.txt','snapshot-ui-failure.png']:
        try:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name))
        except Exception:pass
    (out/'fixture-downloads.txt').write_text(adb('shell','ls','-l','/sdcard/Download'))
    independent('html-same-object',lambda:html('frozen-page.html'))
    independent('html-saf',lambda:html('frozen-page-saf.html'))
    independent('actual-print-source-html',lambda:html('frozen-page-print-source.html'))
    independent('actual-print-source-markdown',lambda:assert_print_markdown())
    independent('markdown-content',markdown)
    independent('pdf-content',pdf)
    independent('chromium-open',lambda:subprocess.run(['node','tools/snapshot/open-html.cjs'],check=True))
    suite('com.example.chatgptnova.FirefoxSnapshotTest#sameFrozenHtmlCanBeSavedAsPdfInFirefoxAndroid','firefox-android',1)
    for name in ['firefox-source.html','firefox-frozen.pdf']:
        independent('pull-'+name,lambda name=name:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name)))
    independent('firefox-exact-print-source',lambda:assert_same_print_source())
    for name in ['firefox-ui-failure.txt','firefox-ui-failure.png']:
        try:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name))
        except Exception:pass
    independent('firefox-pdf-content',lambda:pdf('firefox-frozen.pdf'))
    (out/'firefox-version.txt').write_text(adb('shell','dumpsys','package','org.mozilla.firefox'))
    for permission in ['CAMERA','RECORD_AUDIO']:adb('shell','pm','grant','com.example.chatgptnova','android.permission.'+permission)
    suite('com.example.chatgptnova.NovaWebViewTest','web-regressions',9)
    suite('com.example.chatgptnova.WebShareTest','share-regressions',5)
    suite('com.example.chatgptnova.ClipboardUiTest','input-regressions',5)
    for method in ['imeCommitText','imePasteCommand','longPressSystemPaste']:
        suite('com.example.chatgptnova.ClipboardProbeTest#'+method,'input-'+method,1)
    suite('com.example.chatgptnova.NativeUiTest#nativeControlsAndLifecycleRemainUsable','native-lifecycle',1)
    adb('shell','am','force-stop','com.example.chatgptnova')
    suite('com.example.chatgptnova.NativeUiTest#processRestartPreservesSyntheticSessionAndControls','native-restart',1)
    # Actual validated Gecko build -> current no-Gecko build, same release identity.
    gecko=Path('gecko-baseline/ChatGPT-Nova.apk')
    if gecko.is_file():
        adb('uninstall','com.example.chatgptnova')
        assert 'Success' in adb('install',str(gecko))
        baseline_info=json.loads(Path('gecko-baseline/package-size.json').read_text())
        independent('gecko-baseline-metrics',lambda:installed_metrics(str(gecko),'gecko-baseline-metrics.json',baseline_info['sourceCommit']))
        suite('com.example.chatgptnova.UpgradeTest#testSeedUpgradeData','gecko-upgrade-seed',1)
        suite('com.example.chatgptnova.UpgradeTest#testSeedDataPersistedBeforeUpgrade','gecko-upgrade-persistence',1)
        assert 'Success' in adb('install','-r','dist/ChatGPT-Nova.apk')
        suite('com.example.chatgptnova.UpgradeTest#testUpgradeDataPreserved','gecko-upgrade-current',1)
    # Original signed v1 -> seed -> separate persistence check -> new signed APK.
    baseline=list(Path('baseline').rglob('ChatGPT-Nova.apk'))
    assert len(baseline)==1,baseline
    adb('uninstall','com.example.chatgptnova')
    assert 'Success' in adb('install',str(baseline[0]))
    suite('com.example.chatgptnova.UpgradeTest#testSeedUpgradeData','upgrade-seed',1)
    suite('com.example.chatgptnova.UpgradeTest#testSeedDataPersistedBeforeUpgrade','upgrade-persistence',1)
    assert 'Success' in adb('install','-r','dist/ChatGPT-Nova.apk')
    suite('com.example.chatgptnova.UpgradeTest#testUpgradeDataPreserved','upgrade-current',1)

finally:
    (out/'checks.json').write_text(json.dumps({'failures':failures,'scope':'synthetic-fixtures'},ensure_ascii=False,indent=2))
    (out/'logcat.txt').write_text(adb('logcat','-d','-v','threadtime'))
assert not failures,json.dumps(failures,ensure_ascii=False)
print('PASS: actual same-snapshot HTML/MD/system-print-UI PDF files, Chromium HTML open, existing web/share/full input/native/original-v1 upgrade regressions; controlled Firefox actual PDF A/B; no live-account or physical-device proof')
