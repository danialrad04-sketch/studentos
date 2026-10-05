'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const core = require('../lib/premium-core');
const { PremiumService } = require('../lib/premium-service');
const { privateKey, publicKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
const env = { BAZAAR_RSA_PUBLIC_KEY: publicKey.export({ format: 'der', type: 'spki' }).toString('base64'), BAZAAR_API_SECRET: 'test-only', BILLING_TOKEN_ENCRYPTION_KEY: crypto.randomBytes(32).toString('base64'), GEMINI_SERVER_API_KEY: 'test-only', PREMIUM_SALES_ENABLED: 'true' };
let now = 1900000000000;
function signed(overrides = {}, owner = 'owner') {
  const originalJson = JSON.stringify({ packageName: core.PACKAGE, productId: core.PLANS[0].productId, purchaseState: 0, developerPayload: 'studentos:' + owner, purchaseToken: 'initial-token', ...overrides });
  return { originalJson, dataSignature: crypto.sign('RSA-SHA1', Buffer.from(originalJson), privateKey).toString('base64') };
}
function active(overrides = {}) { return { subscriptions: [{ kind: 'androidpublisher#subscriptionPurchase', sku: core.PLANS[0].productId, validUntilTimestampMsec: now + 86400000, autoRenewing: false, linkedSubscriptionToken: 'stable-linked-id', ...overrides }] }; }
const response = (body, status = 200) => ({ ok: status < 300, status, json: async () => body });
class MemoryFirestore {
  constructor() { this.rows = new Map(); this.tail = Promise.resolve(); }
  collection(name) { return { doc: id => this.ref(name + '/' + id) }; }
  ref(path) { return { path, get: async () => this.snapshot(path), set: async (data, options) => this.write(path, data, options) }; }
  snapshot(path) { const row = this.rows.get(path); return { exists: row != null, data: () => row == null ? undefined : structuredClone(row) }; }
  write(path, data, options) { this.rows.set(path, options?.merge ? { ...(this.rows.get(path) || {}), ...data } : data); }
  runTransaction(action) {
    const result = this.tail.then(async () => {
      const writes = [];
      const tx = { get: ref => ref.get(), set: (ref, data, options) => writes.push([ref.path, data, options]), create: (ref, data) => { assert.equal(this.rows.has(ref.path), false); writes.push([ref.path, data]); } };
      const result = await action(tx);
      for (const args of writes) this.write(...args);
      return result;
    });
    this.tail = result.catch(() => {}); return result;
  }
}
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
  assert.equal(await core.bazaarSubscription('token', env, async () => response({}, 404), now), null);
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
test('premium study tools cannot be invoked by a free account even with a modified client', async () => {
  const service = new PremiumService(new MemoryFirestore(), env);
  for (const kind of ['coach', 'exam', 'review']) await assert.rejects(service.generate('free', { kind, prompt: 'plan', requestId: crypto.randomUUID() }), /اشتراک پرو/);
});
test('quota is transactional, duplicate requests cannot be charged twice, and failed generation refunds quota', async () => {
  const db = new MemoryFirestore(); let time = now; let fails = false;
  const service = new PremiumService(db, env, async () => { if (fails) return response({}, 503); return response({ candidates: [{ content: { parts: [{ text: 'پاسخ تست' }] } }] }); }, () => time);
  const request = { prompt: 'help', requestId: crypto.randomUUID() };
  const concurrent = await Promise.allSettled([service.generate('free', request), service.generate('free', request)]);
  assert.equal(concurrent.filter(r => r.status === 'fulfilled').length, 1);
  assert.equal((await service.status('free')).dailyAiQuotaUsed, 1);
  fails = true; time += 3000;
  await assert.rejects(service.generate('free', { ...request, requestId: crypto.randomUUID() }), /سهمیه شما کسر نشد/);
  assert.equal((await service.status('free')).dailyAiQuotaUsed, 1);
  fails = false;
  for (let i = 0; i < 4; i++) { time += 3000; await service.generate('free', { ...request, requestId: crypto.randomUUID() }); }
  time += 3000;
  await assert.rejects(service.generate('free', { ...request, requestId: crypto.randomUUID() }), /سهمیهٔ امروز/);
  assert.equal((await service.status('free')).dailyAiQuotaUsed, 5);
  assert.equal(JSON.stringify([...db.rows.values()]).includes('پاسخ تست'), false);
});
test('provider errors never expose its private key or raw response', async () => {
  await assert.rejects(core.generateAi(core.aiRequest({ prompt: 'help' }), env, async () => { throw Error('test-only secret'); }), error => !error.message.includes('test-only'));
  assert.throws(() => core.aiRequest({ kind: 'schedule', prompt: 'scan', imageBase64: 'bad', mimeType: 'image/svg+xml' }));
  assert.throws(() => core.aiRequest({ prompt: 'a'.repeat(4001) }));
});
module.exports = { MemoryFirestore };
