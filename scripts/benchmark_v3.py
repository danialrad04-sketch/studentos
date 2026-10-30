#!/usr/bin/env python3
"""Comparable signed 2.9.1/V3 emulator measurements; no synthetic pass values."""
import argparse
import csv
import io
import json
from pathlib import Path
import re
import statistics
import time
from smoke_bazaar_release import adb, onboard, launch, assert_profile, wait_ui, capture, PACKAGE


def start_ms(output):
    match = re.search(r'^TotalTime:\s*(\d+)\s*$', output, re.M)
    if not match: raise ValueError('Android did not report TotalTime')
    return int(match.group(1))


def pss_kib(output):
    match = re.search(r'TOTAL PSS:\s*(\d+)', output)
    if not match: match = re.search(r'^\s*TOTAL\s+(\d+)\s+', output, re.M)
    if not match or int(match.group(1)) <= 0: raise ValueError('No real PSS measurement')
    return int(match.group(1))


def frame_durations(output):
    durations = []
    # Android emits one CSV section per rendered window.
    for section in output.split('---PROFILEDATA---')[1::2]:
        rows = list(csv.DictReader(io.StringIO(section.strip())))
        for row in rows:
            if int(row.get('Flags', '1') or '1') != 0: continue
            begin = int(row['IntendedVsync']); end = int(row['FrameCompleted'])
            if end == 9223372036854775807 or end <= begin: continue
            durations.append((end-begin)/1_000_000)
    if len(durations) < 30: raise ValueError(f'Insufficient actual rendered frames: {len(durations)}')
    return durations


def percentile(values, fraction):
    data = sorted(values)
    index = (len(data)-1)*fraction; lower = int(index); upper = min(lower+1, len(data)-1)
    return data[lower]+(data[upper]-data[lower])*(index-lower)


def profile(apk, out, name):
    try: adb('uninstall', PACKAGE)
    except Exception: pass
    assert 'Success' in adb('install', apk)
    launch(); onboard()
    startup, memory = [], []
    time.sleep(2)
    for _ in range(5):
        adb('shell', 'am', 'force-stop', PACKAGE); time.sleep(.4)
        startup.append(start_ms(adb('shell', 'am', 'start', '-W', '-n', PACKAGE+'/com.example.MainActivity')))
        wait_ui(lambda x: 'خانه' in x and 'حساب' in x)
        time.sleep(.3); memory.append(pss_kib(adb('shell', 'dumpsys', 'meminfo', PACKAGE)))
    width, height = map(int, re.findall(r'(\d+)x(\d+)', adb('shell', 'wm', 'size'))[-1])
    rounds = []
    for round_index in range(3):
        adb('shell', 'dumpsys', 'gfxinfo', PACKAGE, 'reset')
        for direction in [1]*6+[-1]*6:
            first, last = (int(height*.70), int(height*.28)) if direction == 1 else (int(height*.28), int(height*.70))
            adb('shell', 'input', 'swipe', width//2, first, width//2, last, 260)
        raw = adb('shell', 'dumpsys', 'gfxinfo', PACKAGE, 'framestats')
        (out/f'{name}-frames-{round_index}.txt').write_text(raw)
        frames = frame_durations(raw)
        rounds.append({'count': len(frames), 'p95_ms': percentile(frames, .95), 'jank_ratio_60hz': sum(v>16.666667 for v in frames)/len(frames)})
    result = {'cold_start_ms_samples': startup, 'cold_start_p50_ms': statistics.median(startup), 'pss_kib_samples': memory, 'pss_p50_kib': statistics.median(memory), 'frame_rounds': rounds, 'frame_p95_median_ms': statistics.median(r['p95_ms'] for r in rounds)}
    capture(out, name+'-home')
    return result


def compare(baseline, candidate):
    ratios = {metric: candidate[metric]/baseline[metric] for metric in ['cold_start_p50_ms', 'pss_p50_kib', 'frame_p95_median_ms']}
    return {'ratios': ratios, 'limit_ratio': 1.10, 'passed': all(r<=1.10 for r in ratios.values())}


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--baseline-apk',type=Path,required=True);p.add_argument('--candidate-apk',type=Path,required=True);p.add_argument('--output',type=Path,required=True)
    args=p.parse_args();args.output.mkdir(parents=True,exist_ok=True)
    adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
    baseline=profile(args.baseline_apk,args.output,'baseline-2.9.1')
    # Real 13->14 APK upgrade with the existing profile, before the candidate fresh-install samples.
    assert 'Success' in adb('install','-r',args.candidate_apk)
    launch();assert_profile(direct_account=True);capture(args.output,'v3-update-from-2.9.1')
    candidate=profile(args.candidate_apk,args.output,'candidate-v3')
    comparison=compare(baseline,candidate)
    result={'device':adb('shell','getprop','ro.product.model').strip(),'sdk':adb('shell','getprop','ro.build.version.sdk').strip(),'baseline_source':'4a5a72f8a794eaf8a9e2dfdad369954bf5587f6a','scope':'same-emulator release measurements; no physical-device certification','baseline':baseline,'candidate':candidate,'comparison':comparison}
    (args.output/'performance.json').write_text(json.dumps(result,indent=2)+'\n')
    assert comparison['passed'], 'Measured release regression exceeds 10%; inspect performance.json and raw frames'


if __name__=='__main__': main()
