#!/usr/bin/env python3
"""Audit the packaged APK, including permissions contributed by dependencies."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

PACKAGE = 'com.aistudio.studentos.appvzk'
CERTIFICATE = 'a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950'
FORBIDDEN = {
    'com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR',
    'com.android.vending.BILLING', 'ir.mservices.market.BILLING',
    'android.permission.VIBRATE', 'android.permission.USE_EXACT_ALARM', 'android.permission.MANAGE_EXTERNAL_STORAGE',
    'android.permission.REQUEST_INSTALL_PACKAGES', 'android.permission.QUERY_ALL_PACKAGES',
    'android.permission.READ_CONTACTS', 'android.permission.WRITE_CONTACTS',
    'android.permission.READ_SMS', 'android.permission.SEND_SMS', 'android.permission.RECEIVE_SMS',
    'android.permission.READ_CALL_LOG', 'android.permission.WRITE_CALL_LOG',
    'android.permission.CALL_PHONE', 'android.permission.RECORD_AUDIO',
    'android.permission.ACCESS_FINE_LOCATION', 'android.permission.ACCESS_COARSE_LOCATION',
    'android.permission.ACCESS_BACKGROUND_LOCATION', 'android.permission.SYSTEM_ALERT_WINDOW',
    'android.permission.READ_EXTERNAL_STORAGE', 'android.permission.WRITE_EXTERNAL_STORAGE',
    'android.permission.READ_MEDIA_IMAGES', 'android.permission.READ_MEDIA_VIDEO',
    'android.permission.READ_MEDIA_AUDIO', 'android.permission.CAMERA',
    'com.google.android.gms.permission.AD_ID',
}


def audit(badging, permissions_output, manifest, certificates, version_code, version_name):
    match = re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'", badging)
    if not match or match.groups() != (PACKAGE, str(version_code), version_name):
        raise ValueError('APK identity does not match the requested release')
    if version_code <= 2011:
        raise ValueError('Release versionCode must exceed rejected/distributed code 2011')
    permissions = sorted(set(re.findall(r"uses-permission(?:-sdk-\d+)?: name='([^']+)'", permissions_output)))
    if not permissions:
        raise ValueError('Could not inspect packaged permissions')
    forbidden = FORBIDDEN.intersection(permissions)
    if forbidden:
        raise ValueError('Unused or unsupported permissions: ' + ', '.join(sorted(forbidden)))
    for flag in ('debuggable', 'testOnly'):
        if re.search(r'android:' + flag + r'\([^\n]+\)0xffffffff', manifest):
            raise ValueError('Production APK has ' + flag + ' enabled')
    if not re.search(r'android:usesCleartextTraffic\([^\n]+\)0x0(?:\s|$)', manifest):
        raise ValueError('Production APK must explicitly disable cleartext traffic')
    digests = re.findall(r'Signer #\d+ certificate SHA-256 digest: ([0-9a-f]+)', certificates, re.I)
    if len(digests) != 1 or digests[0].lower() != CERTIFICATE:
        raise ValueError('APK does not use the existing Student OS production certificate')
    return {'package': PACKAGE, 'versionCode': version_code, 'versionName': version_name,
            'certificateSha256': CERTIFICATE, 'permissions': permissions, 'billingEnabled': False}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--version-code', required=True, type=int)
    parser.add_argument('--version-name', required=True)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        parser.error('ANDROID_HOME or ANDROID_SDK_ROOT is required; inspection cannot be skipped')
    versions = [p for p in (Path(sdk) / 'build-tools').iterdir() if (p / 'aapt').is_file()]
    build_tools = max(versions, key=lambda p: tuple(int(n) for n in re.findall(r'\d+', p.name)))
    def run(name, *arguments):
        return subprocess.check_output([str(build_tools / name), *map(str, arguments)], text=True, stderr=subprocess.STDOUT)
    with zipfile.ZipFile(args.apk) as archive:
        if archive.testzip():
            raise ValueError('APK ZIP integrity failure')
        for name in ('Vazirmatn-OFL.txt', 'JetBrainsMono-OFL.txt', 'PlusJakartaSans-OFL.txt'):
            if b'SIL OPEN FONT LICENSE' not in archive.read('assets/licenses/' + name):
                raise ValueError('Packaged font license missing: ' + name)
        abis = sorted({p.split('/')[1] for p in archive.namelist() if p.startswith('lib/') and p.endswith('.so')})
    result = audit(run('aapt', 'dump', 'badging', args.apk),
                   run('aapt', 'dump', 'permissions', args.apk),
                   run('aapt', 'dump', 'xmltree', args.apk, 'AndroidManifest.xml'),
                   run('apksigner', 'verify', '--verbose', '--print-certs', args.apk),
                   args.version_code, args.version_name)
    if abis and not {'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64'}.issubset(abis):
        raise ValueError('Universal APK is missing a supported ABI: ' + ', '.join(abis))
    result.update(sha256=hashlib.sha256(args.apk.read_bytes()).hexdigest(), size=args.apk.stat().st_size,
                  nativeAbis=abis, architecture='ALL' if not abis or len(abis) >= 4 else abis,
                  sourceCommit=os.environ.get('GITHUB_SHA', 'local-uncommitted'))
    rendered = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered)
    print(rendered, end='')


if __name__ == '__main__':
    main()
