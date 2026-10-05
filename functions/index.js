/**
 * Firebase Cloud Functions for Student OS
 * Handles Server-Side Subscription Verification, Promo Code Redemption, and Play Billing Receipts.
 */

const functions = require("firebase-functions/v1");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

initializeApp();
const db = getFirestore();
const premiumCore = require('./lib/premium-core');
const { PremiumService } = require('./lib/premium-service');
const premium = new PremiumService(db);
const secured = functions.runWith({ enforceAppCheck: true, timeoutSeconds: 60, memory: '256MB', maxInstances: 10, secrets: ['BAZAAR_API_SECRET', 'BAZAAR_RSA_PUBLIC_KEY', 'BILLING_TOKEN_ENCRYPTION_KEY', 'GEMINI_SERVER_API_KEY'] });
function member(context) {
  if (!context.auth || context.auth.token.firebase?.sign_in_provider === 'anonymous') throw new functions.https.HttpsError('unauthenticated', 'ابتدا وارد حساب خود شوید؛ اطلاعات مهمان روی دستگاه باقی می‌ماند.');
  return context.auth.uid;
}
async function safe(action) {
  try { return await action(); }
  catch (error) {
    if (error instanceof premiumCore.ServiceError) throw new functions.https.HttpsError(error.code, error.message);
    // Never log receipt tokens, prompts, signatures, upstream bodies or secrets.
    throw new functions.https.HttpsError('unavailable', 'سرویس موقتاً در دسترس نیست؛ دوباره تلاش کنید.');
  }
}
exports.getPremiumCatalog = secured.https.onCall(async (_data, context) => {
  const uid = member(context);
  return safe(async () => ({ plans: premiumCore.plans(process.env), salesEnabled: premiumCore.ready(process.env), rsaPublicKey: process.env.BAZAAR_RSA_PUBLIC_KEY || '', aiEnabled: !!process.env.GEMINI_SERVER_API_KEY, ...await premium.status(uid) }));
});
exports.verifyBazaarSubscription = secured.https.onCall(async (data, context) => {
  const uid = member(context);
  return safe(() => premium.verify(uid, data || {}));
});
exports.generateAcademicAdvice = secured.https.onCall(async (data, context) => {
  const uid = member(context);
  return safe(() => premium.generate(uid, data || {}));
});

/**
 * Callable Cloud Function: validateAndApplyPromoCode
 * Securely redeems promo codes on server side and updates user's subscriptionTier.
 */
exports.validateAndApplyPromoCode = functions.https.onCall(async (data, context) => {
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError(
      "unauthenticated",
      "برای ثبت کد تخفیف باید وارد حساب کاربری خود شده باشید."
    );
  }

  const userId = context.auth.uid;
  const rawCode = data.code ? data.code.trim().toUpperCase() : "";
  if (!rawCode) {
    throw new functions.https.HttpsError("invalid-argument", "کد وارد شده معتبر نمی‌باشد.");
  }

  const promoRef = db.collection("promoCodes").doc(rawCode);
  const userRef = db.collection("users").doc(userId);
  const redemptionRef = userRef.collection("redemptions").doc(rawCode);

  const allowedTiers = new Set(["FREE", "PRO", "ULTRA", "CAMPUS_UNLIMITED"]);
  let targetTier;
  let maxAiQueries;
  let expiresAt = null;
  let maxRedemptions = 1;

  const promoDoc = await promoRef.get();
  if (promoDoc.exists) {
    const promoData = promoDoc.data() || {};
    if (!promoData.isActive) {
      throw new functions.https.HttpsError(
        "failed-precondition",
        "این کد تخفیف منقضی یا غیرفعال شده است."
      );
    }
    targetTier = String(promoData.targetTier || "PRO").trim().toUpperCase();
    if (promoData.expiresAt) {
      expiresAt = promoData.expiresAt;
      const expiresMillis = typeof expiresAt.toMillis === "function"
        ? expiresAt.toMillis()
        : new Date(expiresAt).getTime();
      if (Number.isFinite(expiresMillis) && expiresMillis <= Date.now()) {
        throw new functions.https.HttpsError(
          "failed-precondition",
          "این کد تخفیف منقضی شده است."
        );
      }
    }
    maxRedemptions = Math.max(1, Number(promoData.maxRedemptions || 1));
  } else {
    // Promo codes are data, not source code. Unknown codes fail closed.
    throw new functions.https.HttpsError(
      "not-found",
      "کد تخفیف در سامانه یافت نشد."
    );
  }

  if (!allowedTiers.has(targetTier)) {
    throw new functions.https.HttpsError(
      "failed-precondition",
      "پیکربندی سطح اشتراک این کد معتبر نیست."
    );
  }
  maxAiQueries = targetTier === "FREE" ? 5 : targetTier === "PRO" ? 50 : 999;

  await db.runTransaction(async (transaction) => {
    const userDoc = await transaction.get(userRef);
    const redemptionDoc = await transaction.get(redemptionRef);
    const promoStateDoc = await transaction.get(promoRef);

    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "حساب کاربری یافت نشد.");
    }
    if (redemptionDoc.exists) {
      throw new functions.https.HttpsError(
        "already-exists",
        "این کد قبلاً برای این حساب مصرف شده است."
      );
    }

    const currentPromoData = promoStateDoc.exists ? (promoStateDoc.data() || {}) : {};
    const redeemedCount = Number(currentPromoData.redeemedCount || 0);
    const active = currentPromoData.isActive !== false;
    if (!active || redeemedCount >= maxRedemptions) {
      throw new functions.https.HttpsError(
        "resource-exhausted",
        "ظرفیت مصرف این کد تخفیف تکمیل شده است."
      );
    }

    transaction.set(userRef, {
      subscriptionTier: targetTier,
      subscriptionProvider: "promo",
      bazaarReceiptId: null,
      bazaarAutoRenewing: false,
      maxDailyAiQuota: maxAiQueries,
      isCloudSyncEnabled: targetTier !== "FREE",
      allowsPdfExport: targetTier !== "FREE",
      gpaPredictorUnlocked: targetTier !== "FREE",
      subscriptionExpiresAt: expiresAt,
      lastPromoCode: rawCode,
      updatedAt: FieldValue.serverTimestamp()
    }, { merge: true });

    transaction.create(redemptionRef, {
      code: rawCode,
      tierGranted: targetTier,
      redeemedAt: FieldValue.serverTimestamp()
    });
    transaction.set(
      promoRef,
      {
        redeemedCount: redeemedCount + 1,
        isActive: redeemedCount + 1 < maxRedemptions,
        updatedAt: FieldValue.serverTimestamp()
      },
      { merge: true }
    );
  });

  return {
    success: true,
    tier: targetTier,
    expiresAt: expiresAt || null,
    maxDailyAiQuota: maxAiQueries,
    message: `اشتراک شما به پلن ${targetTier} با موفقیت ارتقا یافت.`
  };
});

/**
 * Callable Cloud Function: verifyPlayBillingReceipt
 * Validates Google Play Developer API purchase tokens server-side.
 */
exports.verifyPlayBillingReceipt = functions.https.onCall(async (data, context) => {
  if (!context.auth || !context.auth.uid) {
    throw new functions.https.HttpsError("unauthenticated", "کاربر احراز هویت نشده است.");
  }

  // Fail closed: this endpoint must not grant entitlements from an
  // unverified purchase token. Wire it to the Google Play Developer API
  // before enabling production purchases.
  throw new functions.https.HttpsError(
    "failed-precondition",
    "اعتبارسنجی خرید Google Play هنوز در سرور فعال نشده است."
  );
});
