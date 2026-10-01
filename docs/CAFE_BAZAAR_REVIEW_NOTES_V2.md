Cafe Bazaar Review Notes — Student OS v2

Review path

Guest / offline review
From the account gate, choose:
فعلاً بدون حساب ادامه بده

This opens the core local-first experience without cloud authentication.

Email account review
Use the Email sign-in/register form.

Student OS tries Firebase Authentication first. If Firebase transport/service access is unavailable from the reviewer network, the app can fall back to the configured self-hosted Student OS backend.

The fallback is limited to service/transport failures; invalid email/password credentials do not trigger provider fallback.

Google Sign-In
Google Sign-In continues through Android Credential Manager + Firebase Authentication. It intentionally keeps the existing production Web Client ID and release signing certificate.

A network that blocks Google/Firebase endpoints may prevent the Google flow from opening; this does not mean the signing certificate changed.

Credentials

For a Bazaar review account, provide a dedicated test email/password through the Bazaar release description or reviewer-support channel.

Do not commit reviewer credentials to GitHub.

Release configuration requirement

The release build must have the GitHub Actions secret:
STUDENTOS_API_BASE_URL

configured with the real HTTPS Student OS backend endpoint.

The release workflow verifies the endpoint health response before building the production artifact.

Placeholder endpoints such as api.example.com are rejected by the release build configuration.

Important signing continuity

Production certificate must remain unchanged.

SHA-1:
b25fe31884ec18a96ae09e6fc0070fd20717a766

SHA-256:
a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950
