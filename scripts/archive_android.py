#!/usr/bin/env python3
"""Synthetic Archive validation, including a real System Print PDF and process restart."""
import subprocess, re, json, os, unicodedata
from pathlib import Path
out=Path('archive-results');out.mkdir(exist_ok=True)
failures=[]
def adb(*args):
    return subprocess.check_output(['adb',*args],stderr=subprocess.STDOUT,timeout=900).decode(errors='replace')
def suite(selection,label,count):
    try:
        text=adb('shell','am','instrument','-w','-r','-e','class',selection,'com.example.chatgptnova.test/androidx.test.runner.AndroidJUnitRunner')
    except subprocess.TimeoutExpired as error:
        partial=error.output or b''
        (out/(label+'.txt')).write_text(partial.decode(errors='replace') if isinstance(partial,bytes) else partial)
        raise
    (out/(label+'.txt')).write_text(text)
    assert re.search(r'OK \('+str(count)+r' tests?\)',text),text
    assert 'FAILURES!!!' not in text and 'INSTRUMENTATION_FAILED' not in text,text
try:
    suite('com.example.chatgptnova.ArchiveResearchTest','archive-research',5)
    suite('com.example.chatgptnova.ArchiveAssetPersistenceTest','archive-asset-persistence',7)
    suite('com.example.chatgptnova.ArchiveAssetReaderTest','archive-asset-reader',3)
    suite('com.example.chatgptnova.ArchiveTest','archive-instrumentation',20)
    suite('com.example.chatgptnova.ArchiveStoreConcurrencyTest','archive-store-concurrency',2)
    for name in ['archive.html','archive.md','archive-print-source.html','archive.pdf']:
        adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name))
    html=(out/'archive.html').read_text();md=(out/'archive.md').read_text()
    assert html.startswith('<!doctype html>') and '<table>' in html and '<script' not in html
    assert 'current-branch' in html and 'OTHER-BRANCH' not in html
    assert '```java' in md and 'OTHER-BRANCH' not in md
    assert 'data:image/png;base64,' in html and 'fictional.docx' in html
    assert '/attachments/' not in html and 'nova-archive.invalid/images/' not in html
    assert 'fictional.png' in md and 'fictional.docx' in md
    image_list=subprocess.check_output(['pdfimages','-list',str(out/'archive.pdf')]).decode()
    (out/'archive-pdf-images.txt').write_text(image_list)
    assert any(re.match(r'\s*\d+\s+\d+\s+image\s',row) for row in image_list.splitlines()), 'ARCHIVE_PDF_IMAGE_MISSING'
    for document in [html,md,(out/'archive-print-source.html').read_text()]:
        assert 'SCHEMA-OBJECT-TEXT' in document and 'SCHEMA-RECAP' in document and '推理摘要' in document, 'ARCHIVE_SCHEMA_OUTPUT_MARKERS_MISSING'
        assert 'SYNTHETIC-HIDDEN-THOUGHT' not in document
        assert 'RESEARCH-FIRST' in document and 'RESEARCH-LAST' in document
        assert 'RESEARCH-HIDDEN-ACTIVITY' not in document
    subprocess.run(['pdftotext','-layout',str(out/'archive.pdf'),str(out/'archive-pdf.txt')],check=True)
    text=re.sub(r'\s+','',unicodedata.normalize('NFKC',(out/'archive-pdf.txt').read_text()))
    for marker in ['ARCHIVE-FIRST','ARCHIVE-LAST','CODE-TAIL','TABLE-TAIL','中文','café','LongArchiveparagraph','Noël','E=mc^2','SCHEMA-OBJECT-TEXT','SCHEMA-RECAP','推理摘要','fictional.docx','RESEARCH-FIRST','RESEARCH-LAST','RESEARCH-CODE-TAIL','RESEARCH-TABLE-TAIL']:
        assert marker in text,('PDF marker',marker)
    assert 'OTHER-BRANCH' not in text and 'SYNTHETIC-HIDDEN-THOUGHT' not in text
    suite('com.example.chatgptnova.ArchiveProcessTest#seedPrivateArchiveForProcessRestart','archive-seed',1)
    adb('shell','am','force-stop','com.example.chatgptnova')
    suite('com.example.chatgptnova.ArchiveProcessTest#verifyPrivateArchiveAfterProcessRestart','archive-restart',1)
except Exception as e:
    failures.append(str(e));print('Archive validation failure:',str(e)[:2000])
finally:
    # Retain actual fixture documents even when another method/teardown fails.
    for name in ['archive.html','archive.md','archive-print-source.html','archive.pdf']:
        if not (out/name).exists():
            try:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/'+name,str(out/name))
            except subprocess.CalledProcessError:pass
    try:adb('pull','/sdcard/Android/data/com.example.chatgptnova/files/archive-test-threads.txt',str(out/'archive-test-threads.txt'))
    except subprocess.CalledProcessError:pass
    (out/'logcat.txt').write_text(adb('logcat','-d','-v','threadtime'))
    (out/'summary.json').write_text(json.dumps({'sourceCommit':os.environ.get('GITHUB_SHA'),'api':adb('shell','getprop','ro.build.version.sdk').strip(),'validation':'synthetic / fixture only; no real OpenAI export verified','failures':failures},indent=2))
if failures:raise SystemExit(1)
