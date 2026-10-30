#!/usr/bin/env python3
"""Verify the actual R8 package's V3 file format, native PDF and practice persistence."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile
from smoke_bazaar_release import adb, dump, wait_ui, tap_node, launch, capture, PACKAGE


def fixture_pdf():
    objects = [
        b'<< /Type /Catalog /Pages 2 0 R >>',
        b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
        b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 400 500] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
        b'<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    ]
    content = b'BT /F1 20 Tf 40 420 Td (Student OS V3 PDF) Tj ET'
    objects.append(b'<< /Length ' + str(len(content)).encode() + b' >>\nstream\n' + content + b'\nendstream')
    data = bytearray(b'%PDF-1.4\n'); offsets = [0]
    for index, obj in enumerate(objects, 1):
        offsets.append(len(data)); data.extend(f'{index} 0 obj\n'.encode()+obj+b'\nendobj\n')
    xref = len(data); data.extend(f'xref\n0 {len(offsets)}\n0000000000 65535 f \n'.encode())
    for offset in offsets[1:]: data.extend(f'{offset:010d} 00000 n \n'.encode())
    data.extend(f'trailer\n<< /Size {len(offsets)} /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode())
    return bytes(data)


def write_fixture(destination):
    pdf = fixture_pdf(); key = hashlib.sha256(pdf).hexdigest()
    payload = {'version': 2, 'profile': {'name': 'V3Student', 'university': 'Test University', 'major': 'Math', 'entryYear': 1405, 'currentSemester': 1, 'isOnboardingCompleted': True},
               'courses': [{'id': 'v3-r8-course', 'name': 'V3Math', 'semesterId': 'current'}],
               'v3': {
                   'files': [{'id': 'v3-r8-file', 'courseId': 'v3-r8-course', 'title': 'V3 reader.pdf', 'mimeType': 'application/pdf', 'blobKey': key, 'byteCount': len(pdf), 'pageCount': 1, 'updatedAt': 100}],
                   'bookmarks': [{'id': 'v3-r8-file:0', 'attachmentId': 'v3-r8-file', 'page': 0, 'note': 'V3 bookmark', 'updatedAt': 100}],
                   'projects': [{'id': 'v3-r8-project', 'courseId': 'v3-r8-course', 'title': 'V3Project', 'description': 'Offline project', 'updatedAt': 100}],
                   'steps': [{'id': 'v3-r8-step', 'projectId': 'v3-r8-project', 'title': 'V3 milestone', 'updatedAt': 100}],
                   'questions': [{'id': 'v3-r8-question', 'courseId': 'v3-r8-course', 'prompt': 'V3 question', 'kind': 'MCQ', 'choicesJson': json.dumps(['Correct', 'Wrong']), 'answer': '0', 'updatedAt': 100}]}}
    metadata = json.dumps(payload, ensure_ascii=True).encode()
    entries = {'metadata.json': metadata, f'blobs/{key}': pdf}
    manifest = {'version': 2, 'entries': [{'name': name, 'sha256': hashlib.sha256(data).hexdigest(), 'size': len(data)} for name, data in entries.items()]}
    with zipfile.ZipFile(destination, 'w', zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('manifest.json', json.dumps(manifest))
        for name, data in entries.items(): archive.writestr(name, data)
    return key


def tap_text(text, scroll=False):
    width, height = map(int, re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1])
    for _ in range(18 if scroll else 1):
        xml = dump()
        matches = [n.attrib for n in ET.fromstring(xml).iter('node') if n.attrib.get('text') == text]
        if matches:
            bounds = list(map(int, re.findall(r'\d+', matches[0]['bounds'])))
            if not scroll or (bounds[1]+bounds[3])/2 < height*.80:
                tap_node(xml, lambda a: a.get('text') == text); return
        if scroll: adb('shell', 'input', 'swipe', width//2, int(height*.70), width//2, int(height*.34), 280)
    raise AssertionError(f'Visible control not found: {text}')


def root_surface():
    launch()
    for _ in range(5):
        xml = dump()
        if 'باز کردن منوی برنامه' in xml: return xml
        adb('shell', 'input', 'keyevent', '4')
    return wait_ui(lambda x: 'باز کردن منوی برنامه' in x)


def drawer(text):
    root_surface()
    xml = wait_ui(lambda x: 'باز کردن منوی برنامه' in x)
    tap_node(xml, lambda a: a.get('content-desc') == 'باز کردن منوی برنامه')
    wait_ui(lambda x: 'جست‌وجوی بخش‌ها' in x)
    tap_text(text, scroll=True)


def choose_download(name):
    xml = wait_ui(lambda x: name in x or 'Show roots' in x or 'Recent' in x or 'Downloads' in x)
    if name not in xml:
        roots = [n for n in ET.fromstring(xml).iter('node') if n.attrib.get('content-desc') in ('Show roots', 'Navigate up')]
        if roots: tap_node(xml, lambda a: a.get('content-desc') in ('Show roots', 'Navigate up'))
        xml = wait_ui(lambda x: 'Downloads' in x)
        tap_node(xml, lambda a: a.get('text') == 'Downloads')
    wait_ui(lambda x: name in x)
    tap_text(name)


def open_backup():
    root_surface(); wait_ui(lambda x: 'حساب' in x); tap_text('حساب')
    wait_ui(lambda x: 'حساب من' in x)
    tap_text('پشتیبان و بازیابی', scroll=True)
    wait_ui(lambda x: 'ذخیرهٔ پشتیبان کامل ZIP' in x)


def verify_export(path, pdf_key):
    with zipfile.ZipFile(path) as archive:
        manifest = json.loads(archive.read('manifest.json'))
        assert manifest['version'] == 2
        for item in manifest['entries']:
            data = archive.read(item['name'])
            assert len(data) == item['size'] and hashlib.sha256(data).hexdigest() == item['sha256']
        payload = json.loads(archive.read('metadata.json'))
        assert payload['version'] == 2 and payload['profile']['name'] == 'V3Student'
        assert any(f['blobKey'] == pdf_key for f in payload['v3']['files'])
        assert payload['v3']['bookmarks'][0]['note'] == 'V3 bookmark'
        attempts = payload['v3']['attempts']
        assert any(a['finishedAt'] is not None and a['correctCount'] == 1 for a in attempts), 'R8 result did not persist'


def main():
    parser = argparse.ArgumentParser(description=__doc__);parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args(); args.output.mkdir(parents=True, exist_ok=True)
    fixture = args.output/'StudentOS-v3-r8.zip'; key = write_fixture(fixture)
    adb('push', fixture, '/sdcard/Download/StudentOS-v3-r8.zip')
    adb('shell', 'am', 'broadcast', '-a', 'android.intent.action.MEDIA_SCANNER_SCAN_FILE', '-d', 'file:///sdcard/Download/StudentOS-v3-r8.zip')
    open_backup();tap_text('بازیابی پشتیبان کامل');choose_download('StudentOS-v3-r8.zip')
    wait_ui(lambda x: 'بازیابی اطلاعات و فایل' in x);tap_text('بازیابی')
    wait_ui(lambda x: 'V3Student' in x and 'در حال آماده‌سازی اطلاعات' not in x, timeout=90)
    drawer('کتابخانه');wait_ui(lambda x: 'V3 reader.pdf' in x);tap_text('مطالعه', scroll=True)
    wait_ui(lambda x: 'صفحهٔ 1 از V3 reader.pdf' in x)
    capture(args.output, 'v3-signed-pdf-reader')
    drawer('تکلیف‌ها');wait_ui(lambda x: 'پروژه‌ها' in x);tap_text('پروژه‌ها')
    wait_ui(lambda x: 'V3Project' in x);tap_text('باز کردن پروژه', scroll=True)
    wait_ui(lambda x: 'V3 milestone' in x);capture(args.output, 'v3-signed-project')
    drawer('امتحان‌ها');wait_ui(lambda x: 'تمرین و آزمون' in x);tap_text('تمرین و آزمون')
    wait_ui(lambda x: 'V3 question' in x);tap_text('شروع آزمون', scroll=True)
    wait_ui(lambda x: 'Correct' in x and 'زمان باقی‌مانده' in x);tap_text('Correct')
    tap_text('پایان و بررسی پاسخ‌ها', scroll=True);wait_ui(lambda x: 'آزمون پایان یابد' in x);tap_text('پایان آزمون')
    wait_ui(lambda x: 'نتیجهٔ ثبت‌شده' in x and '1 درست' in x);capture(args.output, 'v3-signed-practice-result')
    open_backup();tap_text('ذخیرهٔ پشتیبان کامل ZIP')
    xml = wait_ui(lambda x: 'Save' in x or 'SAVE' in x)
    tap_node(xml, lambda a: a.get('text') in ('Save', 'SAVE') and a.get('enabled') == 'true')
    wait_ui(lambda x: 'پشتیبان کامل همراه فایل‌ها ذخیره شد' in x, timeout=90)
    adb('pull', '/sdcard/Download/StudentOS-full-backup.zip', args.output/'v3-r8-export.zip')
    verify_export(args.output/'v3-r8-export.zip', key)
    (args.output/'v3-result.txt').write_text('PASS: actual signed R8 full ZIP import/export, hashes, native PDF, project, real practice answer/result and persisted export; offline.\n')


if __name__ == '__main__':
    try: main()
    except Exception:
        import sys
        try:
            out = Path(sys.argv[sys.argv.index('--output')+1]);out.mkdir(parents=True, exist_ok=True)
            capture(out, 'v3-failure');(out/'v3-failure-logcat.log').write_text(adb('logcat', '-d'))
        except Exception: pass
        raise
