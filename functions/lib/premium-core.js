'use strict';
const crypto = require('node:crypto');
const PACKAGE = 'com.aistudio.studentos.appvzk';
const PLANS = Object.freeze([
  { productId: 'studentos_pro_monthly', title: 'یک‌ماهه', months: 1, priceToman: 194500 },
  { productId: 'studentos_pro_quarterly', title: 'سه‌ماهه', months: 3, priceToman: 583500 },
]);
class ServiceError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}
function fail(code, message) { throw new ServiceError(code, message); }
function hash(value) { return crypto.createHash('sha256').update(value).digest('hex'); }
function requireText(value, min, max) {
  if (typeof value !== 'string' || value.length < min || value.length > max) fail('invalid-argument', 'اطلاعات درخواست معتبر نیست.');
  return value;
}
function encryptionKey(env) {
  const key = Buffer.from(env.BILLING_TOKEN_ENCRYPTION_KEY || '', 'base64');
  if (key.length !== 32) fail('failed-precondition', 'سرویس پرداخت هنوز آماده نیست.');
  return key;
}
function seal(token, env) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-256-gcm', encryptionKey(env), iv);
  const data = Buffer.concat([cipher.update(token, 'utf8'), cipher.final()]);
  return [iv, cipher.getAuthTag(), data].map(b => b.toString('base64')).join('.');
}
function unseal(value, env) {
  const [iv, tag, data] = value.split('.').map(v => Buffer.from(v, 'base64'));
  const decipher = crypto.createDecipheriv('aes-256-gcm', encryptionKey(env), iv);
  decipher.setAuthTag(tag);
  return Buffer.concat([decipher.update(data), decipher.final()]).toString('utf8');
}
function plans(env) {
  const configured = Number(env.PREMIUM_MONTHLY_PRICE_TOMAN || 194500);
  if (!Number.isSafeInteger(configured) || configured < 1000 || configured > 100000000) fail('failed-precondition', 'قیمت اشتراک معتبر نیست.');
  return PLANS.map(plan => ({ ...plan, priceToman: configured * plan.months }));
}
function ready(env, uid = '') {
  try {
    encryptionKey(env);
    plans(env);
    const tester = !!uid && (env.PREMIUM_TESTER_UIDS || '').split(',').map(value => value.trim()).filter(Boolean).includes(uid);
    return (env.PREMIUM_SALES_ENABLED === 'true' || tester) && !!env.BAZAAR_API_SECRET && !!env.BAZAAR_RSA_PUBLIC_KEY;
  } catch (_) { return false; }
}
function receipt(data, uid, env) {
  const originalJson = requireText(data.originalJson, 10, 16384);
  const signature = requireText(data.dataSignature, 10, 4096);
  if (!env.BAZAAR_RSA_PUBLIC_KEY) fail('failed-precondition', 'سرویس پرداخت هنوز آماده نیست.');
  let verified = false;
  try {
    const key = crypto.createPublicKey({ key: Buffer.from(env.BAZAAR_RSA_PUBLIC_KEY, 'base64'), type: 'spki', format: 'der' });
    verified = crypto.verify('RSA-SHA1', Buffer.from(originalJson), key, Buffer.from(signature, 'base64'));
  } catch (_) {}
  if (!verified) fail('permission-denied', 'امضای خرید معتبر نیست.');
  let purchase;
  try { purchase = JSON.parse(originalJson); } catch (_) { fail('invalid-argument', 'رسید خرید معتبر نیست.'); }
  if (purchase.packageName !== PACKAGE || !PLANS.some(p => p.productId === purchase.productId) || purchase.purchaseState !== 0) fail('permission-denied', 'رسید برای این اشتراک معتبر نیست.');
  const expected = 'studentos:' + uid;
  if (purchase.developerPayload !== expected) fail('permission-denied', 'این خرید به حساب دیگری تعلق دارد؛ با حساب زمان خرید وارد شوید.');
  const token = requireText(purchase.purchaseToken, 1, 4096);
  return { token, productId: purchase.productId };
}
async function bazaarSubscription(token, env, fetcher = fetch, now = Date.now()) {
  if (!env.BAZAAR_API_SECRET) fail('failed-precondition', 'سرویس پرداخت هنوز آماده نیست.');
  const url = `https://pardakht.cafebazaar.ir/devapi/v2/api/applications/${PACKAGE}/active-subscriptions/${encodeURIComponent(token)}/`;
  let response;
  try { response = await fetcher(url, { headers: { 'CAFEBAZAAR-PISHKHAN-API-SECRET': env.BAZAAR_API_SECRET }, signal: AbortSignal.timeout(12000), redirect: 'error' }); }
  catch (_) { fail('unavailable', 'ارتباط با بازار انجام نشد؛ خرید را دوباره بازیابی کنید.'); }
  if (response.status === 404) {
    let error;
    try { error = await response.json(); } catch (_) { fail('unavailable', 'پاسخ بازار معتبر نیست.'); }
    if (error?.error === 'not_found') return null;
    // A wrong package is also a 404; it must not revoke a recorded purchase.
    fail('unavailable', 'پیکربندی اعتبارسنجی بازار نیاز به بررسی دارد.');
  }
  if (!response.ok) fail('unavailable', 'اعتبارسنجی بازار موقتاً در دسترس نیست.');
  const body = await response.json();
  if (!Array.isArray(body.subscriptions)) fail('unavailable', 'پاسخ بازار معتبر نیست.');
  const allowed = body.subscriptions.filter(s => PLANS.some(p => p.productId === s.sku) && s.kind === 'androidpublisher#subscriptionPurchase' && Number.isSafeInteger(Number(s.validUntilTimestampMsec)) && Number(s.validUntilTimestampMsec) > now && typeof s.linkedSubscriptionToken === 'string' && s.linkedSubscriptionToken.length > 0);
  const current = allowed.sort((a, b) => Number(b.validUntilTimestampMsec) - Number(a.validUntilTimestampMsec))[0];
  return current ? { productId: current.sku, expiresAt: Number(current.validUntilTimestampMsec), autoRenewing: current.autoRenewing === true, linkedId: hash(current.linkedSubscriptionToken) } : null;
}
function entitlement(user = {}, now = Date.now()) {
  const expiry = user.subscriptionExpiresAt;
  const expiresAt = expiry && typeof expiry.toMillis === 'function' ? expiry.toMillis() : Number(expiry) || null;
  const tier = ['PRO', 'ULTRA', 'CAMPUS_UNLIMITED'].includes(user.subscriptionTier) && (expiresAt == null || expiresAt > now) ? user.subscriptionTier : 'FREE';
  return { tier, expiresAt, maxDailyAiQuota: tier === 'FREE' ? 5 : 50, maxDailyScanQuota: tier === 'FREE' ? 1 : 10, autoRenewing: user.bazaarAutoRenewing === true, allowsCloudSync: tier !== 'FREE', allowsPdfExport: tier !== 'FREE', gpaPredictorUnlocked: tier !== 'FREE' };
}
function aiRequest(data) {
  const kind = data.kind || 'chat';
  if (!['chat', 'coach', 'exam', 'review', 'schedule'].includes(kind)) fail('invalid-argument', 'نوع درخواست معتبر نیست.');
  const prompt = requireText(requireText(data.prompt, 1, 4000).trim(), 1, 4000);
  const context = data.studentContext == null ? '' : requireText(data.studentContext, 0, 16000);
  const history = Array.isArray(data.history) ? data.history.slice(-6).map(h => {
    if (!h || !['user', 'model'].includes(h.role)) fail('invalid-argument', 'سابقهٔ گفتگو معتبر نیست.');
    return { role: h.role, parts: [{ text: requireText(requireText(h.text, 1, 3000).trim(), 1, 3000) }] };
  }) : [];
  let image = null;
  if (kind === 'schedule') {
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(data.mimeType)) fail('invalid-argument', 'نوع تصویر پشتیبانی نمی‌شود.');
    const base64 = requireText(data.imageBase64, 4, 2800000);
    if (!/^[A-Za-z0-9+/]+={0,2}$/.test(base64)) fail('invalid-argument', 'تصویر معتبر نیست.');
    image = { inlineData: { mimeType: data.mimeType, data: base64 } };
  }
  return { kind, prompt, context, history, image };
}
// Compatibility entry point; AI is independent of billing and Gemini.
async function generateAi(request, env, fetcher = fetch) {
  return require('./ai-provider').generateAi(request, env, fetcher);
}
module.exports = { PACKAGE, PLANS, ServiceError, fail, hash, ready, receipt, seal, unseal, bazaarSubscription, entitlement, aiRequest, generateAi, requireText, plans };
