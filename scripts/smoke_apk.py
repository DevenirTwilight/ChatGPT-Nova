#!/usr/bin/env python3
"""Install the actual signed APK and exercise native UI on API 33/34/35.
This does not assert authenticated website features or OAuth success.
"""
import json
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

PACKAGE = 'com.example.chatgptnova'
OUT = Path('smoke-results')
OUT.mkdir(exist_ok=True)
checks = []

def adb(*args, binary=False):
    result = subprocess.run(['adb', *args], stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, timeout=60, check=True)
    return result.stdout if binary else result.stdout.decode(errors='replace')

def tree():
    last_dump = ''
    for _ in range(3):
        try:
            adb('shell', 'rm', '-f', '/sdcard/nova-ui.xml')
            last_dump = adb('shell', 'uiautomator', 'dump', '--compressed', '/sdcard/nova-ui.xml')
            xml = adb('exec-out', 'cat', '/sdcard/nova-ui.xml')
            start = xml.find('<hierarchy')
            if start >= 0:
                try: return ET.fromstring(xml[start:])
                except ET.ParseError: pass
        except (subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
            last_dump = 'Snapshot process failed: ' + str(error)
        time.sleep(0.5)
    raise RuntimeError('UI snapshot unavailable after bounded retries: ' + last_dump.strip())

def find(text=None, description=None, attempts=8):
    for _ in range(attempts):
        root = tree()
        for node in root.iter('node'):
            if text is not None and text == node.get('text'):
                return node
            if description is not None and description == node.get('content-desc'):
                return node
        time.sleep(0.5)
    raise AssertionError(f'UI control not found: {text or description}')

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    adb('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def menu(item):
    tap(find(description='Menu'))
    tap(find(text=item))

def launch():
    result = adb('shell', 'am', 'start', '-W', '-n', PACKAGE+'/.MainActivity')
    assert 'Status: ok' in result, result
    find(description='Menu')

def capture(name):
    (OUT/(name+'.png')).write_bytes(adb('exec-out', 'screencap', '-p', binary=True))
    (OUT/(name+'.xml')).write_text(ET.tostring(tree(), encoding='unicode'))

try:
    assert 'Success' in adb('install', '-r', sys.argv[1])
    adb('logcat', '-c')
    launch()
    checks.append('Signed Release APK installs and launches')
    capture('portrait')
    public_text = [n.get('text','') for n in tree().iter('node')]
    website = {'public_chatgpt_page_visible': 'Log in' in public_text or 'Chat with ChatGPT' in public_text,
               'native_network_error_visible': '重试加载' in public_text,
               'authenticated_features_tested': False}
    (OUT/'website-observation.json').write_text(json.dumps(website,ensure_ascii=False,indent=2))
    menu('关于 / 登录帮助')
    root = tree()
    assert any('非官方客户端' in n.get('text','') and 'chatgpt.com' in n.get('text','') for n in root.iter('node'))
    capture('about')
    tap(find(text='知道了'))
    checks.append('About dialog shows unofficial status and website origin')
    menu('刷新')
    find(description='Menu')
    checks.append('Refresh keeps native controls usable')
    menu('清除第二账号登录数据')
    tap(find(text='取消'))
    find(description='Menu')
    menu('清除第二账号登录数据')
    tap(find(text='清除'))
    find(description='Menu')
    checks.append('Cancel and confirm clear-data flows remain usable')
    adb('shell', 'settings', 'put', 'system', 'accelerometer_rotation', '0')
    adb('shell', 'settings', 'put', 'system', 'user_rotation', '1')
    time.sleep(2)
    find(description='Menu')
    capture('landscape')
    adb('shell', 'settings', 'put', 'system', 'user_rotation', '0')
    time.sleep(2)
    find(description='Menu')
    checks.append('Rotation preserves a usable Activity and menu')
    adb('shell', 'input', 'keyevent', '4')
    launch()
    adb('shell', 'am', 'force-stop', PACKAGE)
    launch()
    checks.append('Android Back and process restart do not crash')
    log = adb('logcat', '-d', '-v', 'brief')
    (OUT/'logcat.txt').write_text(log)
    lines = log.splitlines()
    app_pids = set(re.findall(r'Start proc (\d+):'+re.escape(PACKAGE)+r'(?:/|:)', log))
    for i, line in enumerate(lines):
        if 'FATAL EXCEPTION' not in line: continue
        pid = re.search(r'AndroidRuntime\(\s*(\d+)\)', line)
        block = '\n'.join(lines[i:i+20])
        assert not ((pid and pid.group(1) in app_pids)
                    or 'Process: '+PACKAGE+',' in block), 'Nova recorded a fatal exception'
    checks.append('No Nova FATAL EXCEPTION during the smoke test')
    report = {'api': int(sys.argv[2]), 'passed': True, 'checks': checks,
              'not_tested': ['Second-account authentication and persistent authenticated cookies',
                             'Live file/image upload, camera and microphone',
                             'Authenticated downloads and provider OAuth redirects']}
    (OUT/'result.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
    print(json.dumps(report,ensure_ascii=False))
except Exception as error:
    (OUT/'result.json').write_text(json.dumps({'passed':False,'checks':checks,'error':str(error)},indent=2))
    try: (OUT/'logcat.txt').write_text(adb('logcat', '-d', '-v', 'brief'))
    except Exception: pass
    try: capture('failure')
    except Exception: pass
    raise
