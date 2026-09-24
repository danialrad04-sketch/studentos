# Student OS release setup

The production Release build is signed with a private keystore and is never committed to the repository.

## GitHub Actions secrets

Add these repository Actions secrets:

- RELEASE_KEYSTORE_BASE64: base64 of `studentos-release.jks`
- RELEASE_KEYSTORE_PASSWORD: keystore password
- RELEASE_KEY_PASSWORD: private-key password
- RELEASE_KEY_ALIAS: `studentos`

The release workflow then builds both:
- `app-release.apk`
- `app-release.aab`

and publishes them as GitHub Release assets.

## Local release signing

The Gradle release configuration also reads:
- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`
- `KEY_ALIAS`

The Student OS release keystore must be backed up securely. Losing it can prevent future updates from being accepted by an Android marketplace.

Never commit the `.jks` file, passwords, or private keys.
