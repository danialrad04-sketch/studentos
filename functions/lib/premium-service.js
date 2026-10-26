'use strict';
const core = require('./premium-core');

/** All writes use Admin SDK transactions. Never store prompts or provider keys in Firestore. */
class PremiumService {
  constructor(db, env = process.env, fetcher = fetch, clock = Date.now) {
    this.db = db; this.env = env; this.fetcher = fetcher; this.clock = clock;
  }
  async verify(uid, data) {
    const purchase = core.receipt(data, uid, this.env);
    const active = await core.bazaarSubscription(purchase.token, this.env, this.fetcher, this.clock());
    if (!active) core.fail('failed-precondition', 'اشتراک فعال پیدا نشد؛ وضعیت خرید را در بازار بررسی کنید.');
    const ref = this.db.collection('bazaarReceipts').doc(active.linkedId);
    const user = this.db.collection('users').doc(uid);
    const tokenCipher = core.seal(purchase.token, this.env);
    await this.db.runTransaction(async tx => {
      const previous = await tx.get(ref);
      const existing = (await tx.get(user)).data() || {};
      if (previous.exists && previous.data().uid !== uid) core.fail('permission-denied', 'این اشتراک قبلاً به حساب دیگری متصل شده است.');
      tx.set(ref, { uid, tokenCipher, productId: active.productId, expiresAt: active.expiresAt, checkedAt: this.clock() }, { merge: true });
      // Restoring several receipts in a different order must not shorten a
      // longer, previously verified Bazaar entitlement.
      if (existing.subscriptionProvider !== 'bazaar' || core.entitlement(existing, this.clock()).tier === 'FREE' || Number(existing.subscriptionExpiresAt) <= active.expiresAt) {
        tx.set(user, { subscriptionTier: 'PRO', subscriptionExpiresAt: active.expiresAt, bazaarReceiptId: active.linkedId, bazaarAutoRenewing: active.autoRenewing, subscriptionProvider: 'bazaar', maxDailyAiQuota: 50, allowsPdfExport: true, gpaPredictorUnlocked: true }, { merge: true });
      }
    });
    return this.status(uid, false);
  }
  async status(uid, refresh = true) {
    const ref = this.db.collection('users').doc(uid);
    let user = (await ref.get()).data() || {};
    if (refresh && user.subscriptionProvider === 'bazaar' && user.bazaarReceiptId) {
      const receiptRef = this.db.collection('bazaarReceipts').doc(user.bazaarReceiptId);
      const stored = (await receiptRef.get()).data();
      if (!stored || stored.uid !== uid) core.fail('permission-denied', 'مالکیت اشتراک تأیید نشد.');
      if (this.clock() - (stored.checkedAt || 0) >= 5 * 60 * 1000 || Number(user.subscriptionExpiresAt) <= this.clock()) {
        const active = await core.bazaarSubscription(core.unseal(stored.tokenCipher, this.env), this.env, this.fetcher, this.clock());
        // Stale refreshes must not overwrite a newer purchase / another provider.
        await this.db.runTransaction(async tx => {
          const latest = (await tx.get(ref)).data() || {};
          const latestReceipt = (await tx.get(receiptRef)).data() || {};
          if (latest.bazaarReceiptId !== user.bazaarReceiptId || latest.subscriptionProvider !== 'bazaar' || latestReceipt.tokenCipher !== stored.tokenCipher) return;
          const expiresAt = active?.expiresAt || this.clock();
          tx.set(ref, { subscriptionTier: active ? 'PRO' : 'FREE', subscriptionExpiresAt: expiresAt, bazaarAutoRenewing: active?.autoRenewing || false }, { merge: true });
          tx.set(receiptRef, { checkedAt: this.clock(), expiresAt }, { merge: true });
        });
        user = (await ref.get()).data() || {};
      }
    }
    const day = new Date(this.clock() + 210 * 60000).toISOString().slice(0, 10); // Tehran midnight.
    const usage = (await this.db.collection('aiUsage').doc(core.hash(uid + day)).get()).data() || {};
    return { ...core.entitlement(user, this.clock()), dailyAiQuotaUsed: usage.chat || 0, dailyScanQuotaUsed: usage.schedule || 0 };
  }
  async generate(uid, data) {
    return new (require('./ai-service').AcademicAiService)(this.db, this.env, this.fetcher, this.clock).generate(uid, data);
  }
}
module.exports = { PremiumService };
