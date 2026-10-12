# Student OS 2.4.2 — Bazaar release preparation

Target: package `com.aistudio.studentos.appvzk`, versionCode **2012**, versionName **2.4.2**.
Baseline: `c1de38075bb7bf9af0ed52cc72ff7f9fb7361cd9` (2.4.1 / 2011).
Existing production certificate SHA-256: `a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950`.

## Rejection and correction

Bazaar reported an unused `com.farsitel.bazaar.permission.PAY_THROUGH_BAZAAR` permission in 2011. Poolakey declares it in its library manifest. This release has no usable new-account purchase path: new online account entry was disabled in 2.4.1, and production catalog/receipt verification deployment has not been established here.

The permission is explicitly removed during manifest merging. A uniform `bazaarBillingEnabled = false` policy prevents connect, purchase and restore for guests **and previously signed-in users**, even if a server catalog advertises sales. Routes show a truthful availability message without opening billing or fetching a catalog. Existing authentication, academic data, signing identity, subscription records and future billing code remain intact. No external payment route replaces Bazaar. To introduce sales later, ship a new audited version with the permission restored, accessible account entry, production catalog/products, receipt verification, restore and real end-to-end purchase evidence.

## Policy coverage

All nine primary policy topics and publication/permission/privacy guides were retrieved from the official Bazaar content API on 2026-10-13. Source IDs, paths and content hashes are in `BAZAAR_POLICY_SOURCES_2026_10_13.json`; entire copyrighted source articles are not copied into the repository.

| Topic | Current action / evidence | Limit |
| --- | --- | --- |
| Permissions / security | Billing permission, unused `VIBRATE` and redundant `USE_EXACT_ALARM` removed. Checker audits actual APK permissions including dependencies. HTTPS/system trust only in release; local development exceptions remain in debug. | Final packaged gate must pass. |
| Payment | Checkout/restore disabled consistently for every user; no external digital payment. | Production subscriptions remain a separate unverified capability. |
| Privacy | In-app Persian policy describes actual local data, existing connected accounts, Firebase/AI processing, Android backup, permissions and deletion. Policy is available from onboarding and settings. | Store contact details and hosted page cannot be verified without console access. |
| Function / quality | Local-first entry retained. Unit, lintDebug/lintRelease, emulator startup/recreation/navigation and release/R8 build gates run. | Emulator/debug evidence does not prove every physical device or live cloud provider. |
| Notifications | Permission refusal, application-level disable and channel disable return without playing a bypass tone. Notification sound comes from system settings. | Device vendor behavior requires physical validation. |
| Content | Academic schedule/grades/task features; no new public user-content publishing feature. | User notes, online generated answers and store screenshots are not exhaustively certifiable from source. |
| Copyright | Bundled fonts identified from their own metadata; original OFL licenses packaged and readable in Settings → About → Font licenses. No copied Bazaar policy articles or new visual assets added. | Developer must own all current store branding and bundled assets. |
| Advertising | No new advertising SDK or ad feature added; APK rejects advertising ID permission. | External metadata/marketing not inspected. |
| Misleading behavior | New release explains disabled checkout; actual build identity shown; metadata draft lists local features only. | Current Bazaar description/screenshots must be replaced if they promise available sales, login or online AI. |
| Enforcement / policy history | Official enforcement/history documents included in source inventory. | Bazaar makes the acceptance decision. |
| Versions / signing / ABIs | 2012 exceeds known distributed/rejected 2011. Original package/certificate kept. ZIP, 16KB, native ABI and AAB/BIN reconstruction checks. | If console contains a newer undisclosed code, increase before rebuilding. |

## Build evidence

Android CI on the immutable branch commit builds signed release APK/AAB, checks packaged identity and certificate, runs `check_bazaar_release.py`, generates a BIN from the exact AAB with the official Bazaar signer and verifies reconstructed APK signatures. It uploads **studentos-bazaar-2.4.2** as a candidate artifact with JSON evidence/checksums and submission notes. A separate `signed-release-smoke` job installs the actual R8 APK offline, tests an update from the original 2.4.1 APK and checks that a local profile and onboarding state survive restart/update. It does not publish a store version. The existing 2.4.1 automatic publisher rejects the new version constants; main is not changed by candidate preparation.

Do not label this version ready until every required CI job passes. Physical-device update/persistence tests and current store metadata/contact verification are still required. Store approval is not guaranteed.

## Upload

Use `StudentOS-2.4.2.apk`, or upload **the AAB then its matching BIN**. Never pair the new AAB with the old BIN. The zip groups the pair and evidence. Read `BAZAAR_SUBMISSION_2_4_2_FA.md` for factual review notes and a local-feature description. Keep previous releases unchanged.
