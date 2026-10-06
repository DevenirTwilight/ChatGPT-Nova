#!/usr/bin/env python3
"""Controlled Android evidence, never a real-account or full-history proof."""
import re
import subprocess
from email import policy
from email.parser import BytesParser
from pathlib import Path
out=Path('snapshot-results');out.mkdir(exist_ok=True)
def adb(*args,timeout=600):
    return subprocess.check_output(['adb',*args],timeout=timeout,stderr=subprocess.STDOUT).decode(errors='replace')
def suite(selection,label,count):
    result=adb('shell','am','instrument','-w','-r','-e','class',selection,'com.example.chatgptnova.test/androidx.test.runner.AndroidJUnitRunner',timeout=900)
    (out/(label+'.txt')).write_text(result)
    assert re.search(r'OK \('+str(count)+r' tests?\)',result), result
    assert 'FAILURES!!!' not in result and 'INSTRUMENTATION_FAILED' not in result,result
try:
    suite('com.example.chatgptnova.FrozenPageSnapshotTest','snapshot',10)
    for name in ['frozen-page.md','frozen-page.mhtml','frozen-page-saf.mhtml','frozen-page.pdf']:
        adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name))
    def archive(name):
        data=(out/name).read_bytes();assert len(data)>0
        msg=BytesParser(policy=policy.default).parsebytes(data);assert msg.is_multipart(),name
        htmls=[p.get_payload(decode=True).decode(p.get_content_charset() or 'utf-8') for p in msg.walk() if p.get_content_type()=='text/html']
        assert htmls,name
        text=htmls[0]
        for marker in ['Before snapshot marker','TAIL-SNAPSHOT-MARKER','CODE-LAST','TABLE-LAST','中文','café']:
            assert marker in text,(name,marker)
        for marker in ['After snapshot marker','AFTER-TITLE','AFTER-TABLE','AFTER-BODY','CREDENTIAL-DO-NOT-SAVE']:
            assert marker not in text,(name,marker)
        (out/(name+'.html')).write_text(text)
    archive('frozen-page.mhtml');archive('frozen-page-saf.mhtml')
    md=(out/'frozen-page.md').read_text(encoding='utf-8')
    for marker in ['Before snapshot marker','TAIL-SNAPSHOT-MARKER','````java','TABLE-LAST','> Quoted','3. Ordered','- Unordered','Reference','中文','café','😀','E=mc^2']:
        assert marker in md,marker
    for marker in ['After snapshot marker','AFTER-TABLE','AFTER-BODY','BUTTON-GARBAGE','SCRIPT-GARBAGE']:
        assert marker not in md,marker
    subprocess.run(['pdftotext','-layout',str(out/'frozen-page.pdf'),str(out/'frozen-page.txt')],check=True)
    pdf=(out/'frozen-page.txt').read_text();logical=re.sub(r'\s+','',pdf)
    for marker in ['Beforesnapshotmarker','TAIL-SNAPSHOT-MARKER','CODE-LAST','TABLE-LAST','中文','café','Reference','Longparagraph']:
        assert marker in logical,('PDF',marker)
    assert 'Aftersnapshotmarker' not in logical and 'AFTER-TABLE' not in logical
    (out/'pdfinfo.txt').write_text(subprocess.check_output(['pdfinfo',str(out/'frozen-page.pdf')]).decode())
    subprocess.run(['node','tools/snapshot/open-archive.cjs'],check=True)
    suite('com.example.chatgptnova.NovaWebViewTest','web-regressions',9)
    suite('com.example.chatgptnova.WebShareTest','share-regressions',5)
    suite('com.example.chatgptnova.ClipboardUiTest','input-regressions',5)
    for method in ['imeCommitText','imePasteCommand','longPressSystemPaste']:
        suite('com.example.chatgptnova.ClipboardProbeTest#'+method,'input-'+method,1)
    suite('com.example.chatgptnova.NativeUiTest#nativeControlsAndLifecycleRemainUsable','native-lifecycle',1)
    adb('shell','am','force-stop','com.example.chatgptnova')
    suite('com.example.chatgptnova.NativeUiTest#processRestartPreservesSyntheticSessionAndControls','native-restart',1)
    print('PASS: snapshot consistency, real MHTML/MD/system-print-UI PDF files, Chromium archive open, existing browser/share/input/native regressions; no live-account or manual Firefox proof')
finally:
    (out/'logcat.txt').write_text(adb('logcat','-d','-v','threadtime'))
