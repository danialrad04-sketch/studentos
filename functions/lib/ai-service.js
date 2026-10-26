'use strict';
const core = require('./premium-core');
const provider = require('./ai-provider');
function limit(value, fallback, maximum) {
  const n = Number(value); return Number.isInteger(n) && n > 0 && n <= maximum ? n : fallback;
}
class AcademicAiService {
  constructor(db, env = process.env, fetcher = fetch, clock = Date.now) { Object.assign(this, { db, env, fetcher, clock }); }
  policy() { return { chat: limit(this.env.AI_DAILY_CHAT_LIMIT, 20, 1000), schedule: limit(this.env.AI_DAILY_SCAN_LIMIT, 3, 100), global: limit(this.env.AI_DAILY_TOTAL_LIMIT, 500, 100000) }; }
  day() { return new Date(this.clock() + 210 * 60000).toISOString().slice(0, 10); }
  async status(uid) {
    const cfg = provider.config(this.env), limits = this.policy();
    const usage = (await this.db.collection('aiUsage').doc(core.hash(uid + this.day())).get()).data() || {};
    return { enabled: !!cfg, visionEnabled: !!cfg?.vision, model: cfg?.model || null, subscriptionRequired: false,
      dailyAiQuotaUsed: usage.chat || 0, maxDailyAiQuota: limits.chat, dailyScanQuotaUsed: usage.schedule || 0, maxDailyScanQuota: limits.schedule };
  }
  async generate(uid, data) {
    const request = core.aiRequest(data);
    if (!provider.config(this.env)) core.fail('failed-precondition', 'اتصال مدل روی سرور تنظیم نشده است؛ تحلیل محلی همچنان کار می‌کند.');
    const now = this.clock(), day = this.day(), field = request.kind === 'schedule' ? 'schedule' : 'chat', limits = this.policy();
    const ref = this.db.collection('aiUsage').doc(core.hash(uid + day));
    const total = this.db.collection('aiUsage').doc(core.hash('global:' + day));
    const job = this.db.collection('aiRequests').doc(core.hash(uid + core.requireText(data.requestId, 16, 128)));
    await this.db.runTransaction(async tx => {
      const previous = await tx.get(job), usage = (await tx.get(ref)).data() || {}, global = (await tx.get(total)).data() || {};
      if (previous.exists) core.fail('already-exists', 'این درخواست قبلاً ثبت شده است؛ درخواست تازه‌ای بفرستید.');
      if ((usage[field] || 0) >= limits[field]) core.fail('resource-exhausted', 'سهمیهٔ امروز تمام شده؛ فردا دوباره در دسترس است.');
      if ((global.requests || 0) >= limits.global) core.fail('resource-exhausted', 'ظرفیت روزانهٔ سرویس تکمیل شده است؛ بعداً تلاش کنید.');
      if (now - (usage.lastAt || 0) < 2000) core.fail('resource-exhausted', 'چند لحظه صبر کنید و دوباره بفرستید.');
      tx.set(ref, { [field]: (usage[field] || 0) + 1, lastAt: now, expiresAt: new Date(now + 7 * 86400000) }, { merge: true });
      tx.set(total, { requests: (global.requests || 0) + 1, expiresAt: new Date(now + 7 * 86400000) }, { merge: true });
      tx.create(job, { uid, status: 'pending', expiresAt: new Date(now + 86400000) });
    });
    try {
      const response = await provider.generateAi(request, this.env, this.fetcher);
      await job.set({ status: 'completed' }, { merge: true });
      return { ...response, ...await this.status(uid) };
    } catch (error) {
      await this.db.runTransaction(async tx => {
        const state = (await tx.get(job)).data() || {}, usage = (await tx.get(ref)).data() || {}, global = (await tx.get(total)).data() || {};
        if (state.status !== 'pending') return;
        tx.set(ref, { [field]: Math.max(0, (usage[field] || 0) - 1) }, { merge: true });
        tx.set(total, { requests: Math.max(0, (global.requests || 0) - 1) }, { merge: true });
        tx.set(job, { status: 'failed' }, { merge: true });
      });
      throw error;
    }
  }
}
module.exports = { AcademicAiService };
