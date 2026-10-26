#!/usr/bin/env python3
"""Exercise the actual signed/R8 APK offline, including upgrade and persisted profile."""
import argparse
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

PACKAGE = 'com.aistudio.studentos.appvzk'
MARKER = 'BZ50'


def adb(*args):
    return subprocess.check_output(['adb', *map(str, args)], text=True, stderr=subprocess.STDOUT)


def dump():
    adb('shell', 'uiautomator', 'dump', '/sdcard/bazaar-smoke.xml')
    return adb('shell', 'cat', '/sdcard/bazaar-smoke.xml')


def wait_ui(predicate, timeout=60):
    deadline = time.monotonic() + timeout
    last = ''
    while time.monotonic() < deadline:
        try:
            last = dump()
            if predicate(last):
                return last
        except (subprocess.CalledProcessError, ET.ParseError):
            pass
        time.sleep(1)
    raise AssertionError('Expected release screen not found. Last hierarchy: ' + last[:3000])


def tap_node(xml, predicate):
    nodes = [n for n in ET.fromstring(xml).iter('node') if predicate(n.attrib)]
    assert nodes, 'Requested UI element missing'
    bounds = list(map(int, re.findall(r'\d+', nodes[0].attrib['bounds'])))
    adb('shell', 'input', 'tap', (bounds[0]+bounds[2])//2, (bounds[1]+bounds[3])//2)


def launch():
    adb('shell', 'am', 'force-stop', PACKAGE)
    adb('shell', 'am', 'start', '-W', '-n', PACKAGE + '/com.example.MainActivity')


def onboard():
    xml = wait_ui(lambda x: 'رد شدن' in x)
    tap_node(xml, lambda a: a.get('class') == 'android.widget.EditText')
    # Fast adb input can drop the last characters while Compose updates its field.
    for index, character in enumerate(MARKER, start=1):
        adb('shell', 'input', 'text', character)
        expected = MARKER[:index]
        wait_ui(lambda x: any(n.attrib.get('text') == expected
                              for n in ET.fromstring(x).iter('node')), timeout=15)
    adb('shell', 'input', 'keyevent', '4')
    xml = wait_ui(lambda x: MARKER in x and 'رد شدن' in x)
    tap_node(xml, lambda a: 'رد شدن' in a.get('text', ''))
    # Reject optional notifications on API 33+, without rejecting normal navigation.
    xml = wait_ui(lambda x: 'خانه' in x or 'بعداً' in x or 'permission_deny_button' in x)
    for _ in range(3):
        if 'خانه' in xml and 'بعداً' not in xml and 'permission_deny_button' not in xml:
            break
        try:
            tap_node(xml, lambda a: a.get('resource-id', '').endswith('permission_deny_button') or
                     a.get('text') in ['فعلاً نه', 'بعداً', 'انصراف'])
        except AssertionError:
            adb('shell', 'input', 'keyevent', '4')
        xml = wait_ui(lambda x: 'خانه' in x or 'بعداً' in x or 'permission_deny_button' in x, timeout=30)
    assert 'خانه' in xml, 'Local navigation did not become available'
    return xml


def assert_profile():
    xml = wait_ui(lambda x: 'خانه' in x)
    assert 'رد شدن' not in xml, 'Onboarding state was lost'
    tap_node(xml, lambda a: a.get('text') == 'بیشتر')
    xml = wait_ui(lambda x: 'حساب کاربری' in x)
    assert MARKER in xml, 'Saved local profile was lost on update/restart'
    adb('shell', 'input', 'keyevent', '4')
    wait_ui(lambda x: 'خانه' in x)


def find_scroll(predicate):
    size = re.search(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))
    width, height = map(int, size.groups())
    for _ in range(8):
        xml = dump()
        if any(predicate(n.attrib) for n in ET.fromstring(xml).iter('node')):
            return xml
        adb('shell', 'input', 'swipe', width//2, height*4//5, width//2, height//3, '300')
    raise AssertionError('Scrollable destination not found')


def inspect_licenses(out):
    xml = wait_ui(lambda x: 'خانه' in x)
    tap_node(xml, lambda a: a.get('text') == 'بیشتر')
    xml = wait_ui(lambda x: 'حساب کاربری' in x)
    tap_node(xml, lambda a: a.get('text') == 'تنظیمات')
    xml = find_scroll(lambda a: a.get('text') == 'راهنما و دربارهٔ برنامه')
    tap_node(xml, lambda a: a.get('text') == 'راهنما و دربارهٔ برنامه')
    xml = find_scroll(lambda a: a.get('text') == 'مجوز قلم‌ها')
    tap_node(xml, lambda a: a.get('text') == 'مجوز قلم‌ها')
    wait_ui(lambda x: 'Copyright 2015' in x)
    capture(out, 'font-licenses-release')


def capture(out, name):
    xml = dump()
    (out / (name + '.xml')).write_text(xml)
    data = subprocess.check_output(['adb', 'exec-out', 'screencap', '-p'])
    (out / (name + '.png')).write_bytes(data)
    assert 'ساخت حساب' not in xml and 'ورود با گوگل' not in xml, 'Login blocked release use'
    return xml


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--previous-apk', type=Path, required=True)
    p.add_argument('--candidate-apk', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    args = p.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    adb('shell', 'svc', 'wifi', 'disable')
    adb('shell', 'svc', 'data', 'disable')
    adb('logcat', '-c')
    adb('install', args.previous_apk)
    launch()
    onboard()
    assert_profile()
    capture(args.output, 'before-upgrade')
    # Android itself checks signature/package compatibility during this real update.
    assert 'Success' in adb('install', '-r', args.candidate_apk)
    launch()
    assert_profile()
    xml = wait_ui(lambda x: 'خانه' in x)
    assert '۲۴:۱۸ مانده' not in xml, 'Home still displays a fabricated countdown'
    assert 'هنوز کلاسی ثبت نشده' in xml, 'Empty home status should not invent a class'
    capture(args.output, 'after-upgrade-offline')
    launch()
    assert_profile()
    adb('uninstall', PACKAGE)
    assert 'Success' in adb('install', args.candidate_apk)
    launch()
    xml = wait_ui(lambda x: 'رد شدن' in x)
    assert 'سیاست حفظ حریم خصوصی' in xml, 'First-run privacy entry is missing'
    capture(args.output, 'fresh-release-offline')
    tap_node(xml, lambda a: a.get('content-desc') == 'سیاست حفظ حریم خصوصی')
    wait_ui(lambda x: 'نحوهٔ نگهداری و استفاده از اطلاعات' in x)
    capture(args.output, 'privacy-release')
    adb('shell', 'input', 'keyevent', '4')
    wait_ui(lambda x: 'رد شدن' in x)
    onboard()
    capture(args.output, 'fresh-local-home')
    inspect_licenses(args.output)
    logs = adb('logcat', '-d', '-s', 'AndroidRuntime:E')
    (args.output / 'android-runtime.log').write_text(logs)
    assert 'FATAL EXCEPTION' not in logs, 'Signed release crashed'
    (args.output / 'result.txt').write_text('PASS: signed release offline startup, local profile, update from selected 2.4.2 source baseline, restart, fresh installation and optional notification refusal.\n')
    print('Signed Bazaar release smoke and update checks passed.')


if __name__ == '__main__':
    try:
        main()
    except Exception:
        import sys
        try:
            out = Path(sys.argv[sys.argv.index('--output') + 1])
            out.mkdir(parents=True, exist_ok=True)
            capture(out, 'failure')
            (out / 'failure-logcat.log').write_text(adb('logcat', '-d'))
        except Exception:
            pass
        raise
