/**
 * Firebase Cloud Functions for Student OS
 * Handles Server-Side Subscription Verification, Promo Code Redemption, and Play Billing Receipts.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

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
    }
  } else {
    const validCodes = {
      STUDENT2026: "PRO",
      DANESHJOO: "PRO",
      AUT_PRO: "PRO",
      SHARIF_AI: "PRO",
      CAMPUS_ULTRA: "ULTRA",
      ELITE2026: "ULTRA"
    };
    targetTier = validCodes[rawCode];
    if (!targetTier) {
      throw new functions.https.HttpsError("not-found", "کد تخفیف در سامانه یافت نشد.");
    }
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

    if (!userDoc.exists) {
      throw new functions.https.HttpsError("not-found", "حساب کاربری یافت نشد.");
    }
    if (redemptionDoc.exists) {
      throw new functions.https.HttpsError(
        "already-exists",
        "این کد قبلاً برای این حساب مصرف شده است."
      );
    }

    transaction.set(userRef, {
      subscriptionTier: targetTier,
      maxDailyAiQuota: maxAiQueries,
      isCloudSyncEnabled: targetTier !== "FREE",
      allowsPdfExport: targetTier !== "FREE",
      gpaPredictorUnlocked: targetTier !== "FREE",
      subscriptionExpiresAt: expiresAt,
      lastPromoCode: rawCode,
      updatedAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });

    transaction.create(redemptionRef, {
      code: rawCode,
      tierGranted: targetTier,
      redeemedAt: admin.firestore.FieldValue.serverTimestamp()
    });
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
