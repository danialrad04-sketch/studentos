import unittest
from check_bazaar_release import audit, CERTIFICATE


class PackagedReleasePolicyTest(unittest.TestCase):
    def check(self, permissions=None, manifest=None, cert=None, code=2012):
        return audit(f"package: name='com.aistudio.studentos.appvzk' versionCode='{code}' versionName='2.4.2'",
                     permissions or "uses-permission: name='android.permission.INTERNET'",
                     manifest or 'A: android:usesCleartextTraffic(0x010104ec)=(type 0x12)0x0\n',
                     cert or f'Signer #1 certificate SHA-256 digest: {CERTIFICATE}', code, '2.4.2')

    def test_current_release_has_no_billing_permission(self):
        self.assertFalse(self.check()['billingEnabled'])

    def test_android_sdk_scheme_labels_repeat_the_same_production_certificate(self):
        cert = f'V2 Signer: certificate SHA-256 digest: {CERTIFICATE}\nV3 Signer: certificate SHA-256 digest: {CERTIFICATE}\n'
        self.assertEqual(CERTIFICATE, self.check(cert=cert)['certificateSha256'])

    def test_different_scheme_certificate_or_only_public_key_digest_is_rejected(self):
        for cert in [f'V2 Signer: certificate SHA-256 digest: {CERTIFICATE}\nV3 Signer: certificate SHA-256 digest: {"0" * 64}\n',
                     f'V2 Signer: public key SHA-256 digest: {CERTIFICATE}\n']:
            with self.subTest(cert=cert), self.assertRaises(ValueError):
                self.check(cert=cert)

    def test_transitive_billing_and_sdk_specific_storage_permission_are_rejected(self):
        for name in ['com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR', 'android.permission.READ_EXTERNAL_STORAGE']:
            with self.subTest(name=name), self.assertRaises(ValueError):
                self.check(permissions=f"uses-permission-sdk-23: name='{name}'")

    def test_reused_version_wrong_certificate_and_debuggable_are_rejected(self):
        for kwargs in [dict(code=2011), dict(cert='Signer #1 certificate SHA-256 digest: deadbeef'),
                       dict(manifest='A: android:debuggable(0x0101000f)=(type 0x12)0xffffffff\n'),
                       dict(manifest='A: android:usesCleartextTraffic(0x010104ec)=(type 0x12)0xffffffff\n')]:
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):
                self.check(**kwargs)


if __name__ == '__main__':
    unittest.main()
