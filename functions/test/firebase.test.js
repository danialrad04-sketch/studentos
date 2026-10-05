'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
if (!process.env.FIRESTORE_EMULATOR_HOST) {
  test('Firestore integration and client entitlement rules', { skip: 'Run npm run test:emulator from functions.' }, () => {});
} else {
  const fs = require('node:fs');
  const crypto = require('node:crypto');
  const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
  const { doc, setDoc, getDoc } = require('firebase/firestore');
  const { initializeApp, deleteApp } = require('firebase-admin/app');
  const { getFirestore } = require('firebase-admin/firestore');
  const core = require('../lib/premium-core');
  const { PremiumService } = require('../lib/premium-service');
  const projectId = 'demo-studentos-premium';
  let environment, app, db;
  test.before(async () => {
    const [host, port] = process.env.FIRESTORE_EMULATOR_HOST.split(':');
    environment = await initializeTestEnvironment({ projectId, firestore: { host, port: Number(port), rules: fs.readFileSync('../firestore.rules', 'utf8') } });
    app = initializeApp({ projectId }, 'premium-emulator-test'); db = getFirestore(app);
  });
  test.after(async () => { await environment.cleanup(); await deleteApp(app); });
  test('clients cannot create paid entitlements, tamper with billing ownership/quotas, or forge gift redemptions', async () => {
    const client = environment.authenticatedContext('rules-user').firestore();
    await assertFails(setDoc(doc(client, 'users/rules-user'), { subscriptionTier: 'PRO' }));
    await assertSucceeds(setDoc(doc(client, 'users/rules-user'), { displayName: 'student' }));
    for (const field of ['subscriptionTier', 'subscriptionExpiresAt', 'bazaarReceiptId', 'subscriptionProvider', 'bazaarAutoRenewing']) {
      await assertFails(setDoc(doc(client, 'users/rules-user'), { [field]: 'forged' }, { merge: true }));
    }
    for (const path of ['bazaarReceipts/forged', 'aiUsage/forged', 'aiRequests/forged', 'users/rules-user/redemptions/GIFT']) await assertFails(setDoc(doc(client, path), { uid: 'rules-user', tier: 'PRO' }));
    await assertSucceeds(setDoc(doc(client, 'users/rules-user/tasks/test'), { title: 'Homework' }));
    const other = environment.authenticatedContext('other-user').firestore();
    await assertFails(getDoc(doc(other, 'users/rules-user/tasks/test')));
  });
  test('real Firestore transactions prevent concurrent receipt transfer and duplicate AI charging', async () => {
    const { privateKey, publicKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
    const env = { BAZAAR_RSA_PUBLIC_KEY: publicKey.export({ format: 'der', type: 'spki' }).toString('base64'), BAZAAR_API_SECRET: 'emulator-only', BILLING_TOKEN_ENCRYPTION_KEY: crypto.randomBytes(32).toString('base64'), GEMINI_SERVER_API_KEY: 'emulator-only' };
    const now = Date.now();
    function signed(uid) {
      const originalJson = JSON.stringify({ packageName: core.PACKAGE, productId: core.PLANS[0].productId, purchaseState: 0, purchaseToken: 'emulator-only-token', developerPayload: 'studentos:' + uid });
      return { originalJson, dataSignature: crypto.sign('RSA-SHA1', Buffer.from(originalJson), privateKey).toString('base64') };
    }
    const fetcher = async url => ({ status: 200, ok: true, json: async () => url.includes('cafebazaar') ? { subscriptions: [{ kind: 'androidpublisher#subscriptionPurchase', sku: core.PLANS[0].productId, validUntilTimestampMsec: now + 86400000, linkedSubscriptionToken: 'same-stable-subscription', autoRenewing: true }] } : { candidates: [{ content: { parts: [{ text: 'پاسخ تست' }] } }] } });
    const service = new PremiumService(db, env, fetcher, () => now);
    const purchases = await Promise.allSettled([service.verify('purchase-a', signed('purchase-a')), service.verify('purchase-b', signed('purchase-b'))]);
    assert.equal(purchases.filter(r => r.status === 'fulfilled').length, 1);
    const request = { prompt: 'Test', requestId: crypto.randomUUID() };
    const queries = await Promise.allSettled([service.generate('ai-user', request), service.generate('ai-user', request)]);
    assert.equal(queries.filter(r => r.status === 'fulfilled').length, 1);
    assert.equal((await service.status('ai-user')).dailyAiQuotaUsed, 1);
  });
  test('support submission is atomic/idempotent, account isolated, and cannot be forged by clients', async () => {
    const { SupportService } = require('../lib/support-service');
    const service = new SupportService(db);
    const data = { subject: 'Test ticket', message: 'Test only', category: 'BUG_REPORT', priority: 'LOW', studentName: 'Test', requestId: crypto.randomUUID() };
    const [first, duplicate] = await Promise.all([service.create('ticket-owner', data), service.create('ticket-owner', data)]);
    assert.equal(first.id, duplicate.id);
    await service.reply('ticket-owner', { ticketId: first.id, message: 'Follow-up', senderRole: 'SUPPORT', userId: 'forged' });
    const saved = (await db.collection('users').doc('ticket-owner').collection('tickets').doc(first.id).get()).data();
    assert.equal(saved.messages.length, 2); assert.equal(saved.messages[1].senderRole, 'STUDENT'); assert.equal(saved.messages[1].senderId, 'ticket-owner');
    await assert.rejects(service.reply('other-account', { ticketId: first.id, message: 'steal' }), /متعلق/);
    const client = environment.authenticatedContext('ticket-owner').firestore();
    await assertSucceeds(getDoc(doc(client, 'users/ticket-owner/tickets/' + first.id)));
    await assertFails(setDoc(doc(client, 'users/ticket-owner/tickets/' + first.id), { status: 'RESOLVED', messages: [] }));
    await assertFails(getDoc(doc(client, 'support_tickets/' + first.id)));
    const staff = environment.authenticatedContext('trusted-staff', { supportStaff: true }).firestore();
    await assertSucceeds(getDoc(doc(staff, 'support_tickets/' + first.id)));
    await service.close('ticket-owner', { ticketId: first.id });
    await assert.rejects(service.reply('ticket-owner', { ticketId: first.id, message: 'reopen silently' }), /بسته/);
    const mirrored = (await db.collection('support_tickets').doc(first.id).get()).data();
    assert.equal(mirrored.status, 'CLOSED'); assert.equal(mirrored.messages.length, 2);
    for (const invalid of [null, {}, { ...data, message: 'x'.repeat(4001) }, { ...data, category: 'fake' }]) await assert.rejects(service.create('ticket-owner', invalid));
  });
  test('callable exports load under the installed Firebase Functions SDK', () => {
    const functions = require('../index');
    for (const name of ['getPremiumCatalog', 'verifyBazaarSubscription', 'generateAcademicAdvice', 'submitSupportTicket', 'replySupportTicket', 'closeSupportTicket']) assert.equal(typeof functions[name], 'function');
  });
}
