import unittest
from benchmark_v3 import start_ms, pss_kib, frame_durations, compare
from smoke_v3_release import fixture_pdf, write_fixture
import tempfile
from pathlib import Path
import zipfile,json,hashlib

class V3EvidenceTest(unittest.TestCase):
    def test_missing_platform_metrics_cannot_be_treated_as_passes(self):
        for call,value in [(start_ms,'Status: ok'),(pss_kib,'no process'),(frame_durations,'---PROFILEDATA---\nFlags,IntendedVsync,FrameCompleted\n0,1,2\n---PROFILEDATA---')]:
            with self.assertRaises(ValueError):call(value)
    def test_real_platform_formats_and_regression_boundary(self):
        self.assertEqual(180,start_ms('Status: ok\nTotalTime: 180\nWaitTime: 200'))
        self.assertEqual(45000,pss_kib('TOTAL PSS: 45000 TOTAL RSS: 50000'))
        raw='---PROFILEDATA---\nFlags,IntendedVsync,FrameCompleted\n'+'\n'.join(f'0,{i*100000000},{i*100000000+20000000}' for i in range(40))+'\n---PROFILEDATA---'
        self.assertEqual([20.0]*40,frame_durations(raw))
        baseline=dict(cold_start_p50_ms=100,pss_p50_kib=100,frame_p95_median_ms=20)
        self.assertFalse(compare(baseline,dict(cold_start_p50_ms=100,pss_p50_kib=112,frame_p95_median_ms=20))['passed'])
    def test_signed_smoke_fixture_has_complete_real_bytes_and_hashes(self):
        with tempfile.TemporaryDirectory() as tmp:
            file=Path(tmp)/'fixture.zip';key=write_fixture(file)
            with zipfile.ZipFile(file) as z:
                m=json.loads(z.read('manifest.json'))
                for entry in m['entries']:
                    data=z.read(entry['name']);self.assertEqual(entry['sha256'],hashlib.sha256(data).hexdigest());self.assertEqual(entry['size'],len(data))
                self.assertEqual(fixture_pdf(),z.read('blobs/'+key))
                self.assertTrue(z.read('blobs/'+key).startswith(b'%PDF-1.4'))
