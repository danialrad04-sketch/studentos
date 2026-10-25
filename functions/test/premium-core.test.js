'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const core = require('../lib/premium-core');
const { PremiumService } = require('../lib/premium-service');
const { privateKey, publicKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
const env = { BAZAAR_RSA_PUBLIC_KEY: publicKey.export({ format: 'der', type: 'spki' }).toString('base64'), BAZAAR_API_SECRET: 'test-only', BILLING_TOKEN_ENCRYPTION_KEY: crypto.randomBytes(32).toString('base64'), PREMIUM_SALES_ENABLED: 'true' };
let now = 1900000000000;
function signed(overrides = {}, owner = 'owner') {
  const originalJson = JSON.stringify({ packageName: core.PACKAGE, productId: core.PLANS[0].productId, purchaseState: 0, developerPayload: 'studentos:' + owner, purchaseToken: 'initial-token', ...overrides });
  return { originalJson, dataSignature: crypto.sign('RSA-SHA1', Buffer.from(originalJson), privateKey).toString('base64') };
}
function active(overrides = {}) { return { subscriptions: [{ kind: 'androidpublisher#subscriptionPurchase', sku: core.PLANS[0].productId, validUntilTimestampMsec: now + 86400000, autoRenewing: false, linkedSubscriptionToken: 'stable-linked-id', ...overrides }] }; }
const response = (body, status = 200) => ({ ok: status < 300, status, json: async () => body });
const { MemoryFirestore } = require('./helpers/memory-firestore');
test('signed receipts reject tampering, package mismatch, refunds, unknown SKUs and a different account', () => {
  assert.equal(core.receipt(signed(), 'owner', env).token, 'initial-token');
  assert.throws(() => core.receipt({ ...signed(), originalJson: signed().originalJson + ' ' }, 'owner', env), /امضای/);
  for (const change of [{ packageName: 'other.app' }, { purchaseState: 1 }, { productId: 'forged_pro' }]) assert.throws(() => core.receipt(signed(change), 'owner', env));
  assert.throws(() => core.receipt(signed(), 'other', env), /حساب دیگری/);
  assert.throws(() => core.receipt(signed(), 'owner', {}));
});
test('receipt encryption round trips, randomizes ciphertext, and detects corruption', () => {
  const a = core.seal('purchase-token', env), b = core.seal('purchase-token', env);
  assert.notEqual(a, b); assert.equal(core.unseal(a, env), 'purchase-token');
  const parts = a.split('.'); parts[2] = Buffer.alloc(14).toString('base64');
  assert.throws(() => core.unseal(parts.join('.'), env));
  assert.throws(() => core.seal('token', {}));
});
test('sales fail closed without all secrets and an explicit operational sales switch', () => {
  assert.equal(core.ready(env), true);
  for (const key of Object.keys(env)) assert.equal(core.ready({ ...env, [key]: '' }), false);
  const testOnly = { ...env, PREMIUM_SALES_ENABLED: 'false', PREMIUM_TESTER_UIDS: 'tester-a, tester-b' };
  assert.equal(core.ready(testOnly, 'tester-a'), true);
  assert.equal(core.ready(testOnly, 'tester'), false);
  assert.equal(core.ready(testOnly), false);
  assert.equal(core.ready({ ...testOnly, AI_API_KEY: '' }, 'tester-a'), true);
});
test('active-subscription API uses the new private header and works with renewal tokens', async () => {
  const result = await core.bazaarSubscription('renewal/token', env, async (url, options) => {
    assert.ok(url.endsWith('/active-subscriptions/renewal%2Ftoken/'));
    assert.equal(options.headers['CAFEBAZAAR-PISHKHAN-API-SECRET'], 'test-only');
    assert.equal(url.includes('test-only'), false);
    return response(active());
  }, now);
  assert.equal(result.autoRenewing, false); assert.equal(result.productId, core.PLANS[0].productId);
});
test('successful HTTP status is insufficient for an expired or unknown subscription', async () => {
  for (const overrides of [{ validUntilTimestampMsec: now }, { validUntilTimestampMsec: 'NaN' }, { sku: 'untrusted' }, { linkedSubscriptionToken: '' }]) {
    assert.equal(await core.bazaarSubscription('token', env, async () => response(active(overrides)), now), null);
  }
  assert.equal(await core.bazaarSubscription('token', env, async () => response({ error: 'not_found' }, 404), now), null);
  await assert.rejects(core.bazaarSubscription('token', env, async () => response({ error: 'invalid_value' }, 404), now), /پیکربندی/);
  await assert.rejects(core.bazaarSubscription('token', env, async () => response({}, 404), now), /پیکربندی/);
  await assert.rejects(core.bazaarSubscription('token', env, async () => response({}, 401), now), /اعتبارسنجی/);
});
test('server dates determine entitlements; autoRenewing does not create access', () => {
  assert.equal(core.entitlement({ subscriptionTier: 'PRO', subscriptionExpiresAt: now }, now).tier, 'FREE');
  assert.equal(core.entitlement({ subscriptionTier: 'PRO', subscriptionExpiresAt: now + 1, bazaarAutoRenewing: false }, now).tier, 'PRO');
  assert.equal(core.entitlement({ subscriptionTier: 'ADMIN' }, now).tier, 'FREE');
});
test('verified purchases are idempotent and store only encrypted tokens; ownership is permanent', async () => {
  const db = new MemoryFirestore(); const service = new PremiumService(db, env, async () => response(active()), () => now);
  const results = await Promise.all([service.verify('owner', signed()), service.verify('owner', signed())]);
  assert.equal(results[0].tier, 'PRO');
  assert.equal([...db.rows.keys()].filter(k => k.startsWith('bazaarReceipts/')).length, 1);
  assert.equal(JSON.stringify([...db.rows.values()]).includes('initial-token'), false);
  await assert.rejects(service.verify('other', signed({}, 'other')), /قبلاً به حساب دیگری/);
  assert.equal((await service.status('owner')).tier, 'PRO');
});
test('refund / cancellation without remaining paid time removes access on revalidation', async () => {
  const db = new MemoryFirestore(); let available = true; let time = now;
  const service = new PremiumService(db, env, async () => response(available ? active() : { subscriptions: [] }), () => time);
  await service.verify('owner', signed()); available = false; time += 301000;
  assert.equal((await service.status('owner')).tier, 'FREE');
});
test('Bazaar outages never grant new access or destroy a valid recorded purchase', async () => {
  const db = new MemoryFirestore(); let outage = false; let time = now;
  const service = new PremiumService(db, env, async () => { if (outage) throw Error('secret upstream body'); return response(active()); }, () => time);
  await service.verify('owner', signed()); outage = true; time += 301000;
  await assert.rejects(service.status('owner'), /ارتباط با بازار/);
  assert.equal(db.rows.get('users/owner').subscriptionTier, 'PRO');
});
test('restoring older receipts never reduces the verified remaining subscription time', async () => {
  const db = new MemoryFirestore(); let long = true;
  const service = new PremiumService(db, env, async () => response(active({
    validUntilTimestampMsec: now + (long ? 90 : 30) * 86400000,
    linkedSubscriptionToken: long ? 'quarterly-receipt' : 'monthly-receipt'
  })), () => now);
  const first = await service.verify('owner', signed());
  long = false;
  const restored = await service.verify('owner', signed({ purchaseToken: 'older-token' }));
  assert.equal(restored.expiresAt, first.expiresAt);
  assert.equal(db.rows.get('users/owner').bazaarReceiptId, core.hash('quarterly-receipt'));
});
