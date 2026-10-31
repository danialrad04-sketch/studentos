import unittest
from unittest.mock import patch
import smoke_v3_release
from benchmark_v3 import start_ms, pss_kib, frame_durations, compare
from smoke_v3_release import fixture_pdf, write_fixture, navigation_boundary
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
    def test_visible_control_is_compared_with_the_actual_floating_navigation(self):
        labels=['خانه','برنامه','کارها','کارنامه','حساب']
        xml='<hierarchy><node bounds="[0,0][1080,1920]"><node bounds="[42,1687][1038,1899]">'+''.join(f'<node bounds="[42,1780][200,1860]" text="{label}"/>' for label in labels)+'</node></node></hierarchy>'
        boundary=navigation_boundary(xml,1920)
        self.assertEqual(1687,boundary)
        self.assertLess(1569,boundary-8) # Actual project text rejected by the old 80% heuristic.
        self.assertGreater(1780,boundary-8) # A covered control must still be rejected.
        self.assertEqual(int(1920*.94),navigation_boundary('<hierarchy/>',1920))
    def test_question_below_initial_viewport_is_found_by_scrolling_without_tapping(self):
        absent = '<hierarchy><node text="بانک سؤال این درس" bounds="[0,0][100,100]"/></hierarchy>'
        visible = '<hierarchy><node text="V3 question" bounds="[20,800][700,900]"/></hierarchy>'
        with patch.object(smoke_v3_release, 'dump', side_effect=[absent, visible]), patch.object(smoke_v3_release, 'adb', return_value='Physical size: 1080x1920') as adb:
            self.assertEqual(visible, smoke_v3_release.find_visible_text('V3 question', scroll=True))
            self.assertTrue(any('swipe' in call.args for call in adb.call_args_list))
            self.assertFalse(any('tap' in call.args for call in adb.call_args_list))
    def test_missing_question_still_fails_after_bounded_scroll_search(self):
        with patch.object(smoke_v3_release, 'dump', return_value='<hierarchy/>'), patch.object(smoke_v3_release, 'adb', return_value='Physical size: 1080x1920'):
            with self.assertRaises(AssertionError):
                smoke_v3_release.find_visible_text('V3 question', scroll=True)
    def test_signed_smoke_fixture_has_complete_real_bytes_and_hashes(self):
        with tempfile.TemporaryDirectory() as tmp:
            file=Path(tmp)/'fixture.zip';key=write_fixture(file)
            with zipfile.ZipFile(file) as z:
                m=json.loads(z.read('manifest.json'))
                for entry in m['entries']:
                    data=z.read(entry['name']);self.assertEqual(entry['sha256'],hashlib.sha256(data).hexdigest());self.assertEqual(entry['size'],len(data))
                self.assertEqual(fixture_pdf(),z.read('blobs/'+key))
                self.assertTrue(z.read('blobs/'+key).startswith(b'%PDF-1.4'))
