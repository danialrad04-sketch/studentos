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
      if (previous.exists && previous.data().uid !== uid) core.fail('permission-denied', 'این اشتراک قبلاً به حساب دیگری متصل شده است.');
      tx.set(ref, { uid, tokenCipher, productId: active.productId, expiresAt: active.expiresAt, checkedAt: this.clock() }, { merge: true });
      tx.set(user, { subscriptionTier: 'PRO', subscriptionExpiresAt: active.expiresAt, bazaarReceiptId: active.linkedId, bazaarAutoRenewing: active.autoRenewing, subscriptionProvider: 'bazaar', maxDailyAiQuota: 50, allowsPdfExport: true, gpaPredictorUnlocked: true }, { merge: true });
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
    const request = core.aiRequest(data);
    if (!this.env.GEMINI_SERVER_API_KEY) core.fail('failed-precondition', 'دستیار آنلاین هنوز فعال نشده است.');
    const status = await this.status(uid);
    if (['coach', 'exam', 'review'].includes(request.kind) && status.tier === 'FREE') core.fail('permission-denied', 'ابزارهای مربی مطالعه، مرور امتحان و تحلیل پیشرفت در اشتراک پرو در دسترس‌اند.');
    const now = this.clock();
    const day = new Date(now + 210 * 60000).toISOString().slice(0, 10);
    const field = request.kind === 'schedule' ? 'schedule' : 'chat';
    const limit = field === 'schedule' ? status.maxDailyScanQuota : status.maxDailyAiQuota;
    const ref = this.db.collection('aiUsage').doc(core.hash(uid + day));
    const job = this.db.collection('aiRequests').doc(core.hash(uid + core.requireText(data.requestId, 16, 128)));
    await this.db.runTransaction(async tx => {
      const previous = await tx.get(job);
      const usage = (await tx.get(ref)).data() || {};
      if (previous.exists) core.fail('already-exists', 'این درخواست قبلاً ارسال شده؛ درخواست تازه‌ای بفرستید.');
      if ((usage[field] || 0) >= limit) core.fail('resource-exhausted', 'سهمیهٔ امروز تمام شده؛ فردا دوباره در دسترس است.');
      if (now - (usage.lastAt || 0) < 2000) core.fail('resource-exhausted', 'چند لحظه صبر کنید و دوباره بفرستید.');
      tx.set(ref, { [field]: (usage[field] || 0) + 1, lastAt: now, expiresAt: new Date(now + 7 * 86400000) }, { merge: true });
      tx.create(job, { uid, status: 'pending', expiresAt: new Date(now + 86400000) });
    });
    try {
      const response = await core.generateAi(request, this.env, this.fetcher);
      await job.set({ status: 'completed' }, { merge: true });
      return { ...response, ...await this.status(uid, false) };
    } catch (error) {
      await this.db.runTransaction(async tx => {
        const state = (await tx.get(job)).data() || {};
        const usage = (await tx.get(ref)).data() || {};
        if (state.status !== 'pending') return;
        tx.set(ref, { [field]: Math.max(0, (usage[field] || 0) - 1) }, { merge: true });
        tx.set(job, { status: 'failed' }, { merge: true });
      });
      throw error;
    }
  }
}
module.exports = { PremiumService };
