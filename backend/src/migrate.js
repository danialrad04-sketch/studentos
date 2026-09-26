/**
 * Idempotent schema migration. Safe to run on every deploy — uses
 * CREATE TABLE IF NOT EXISTS, never drops or destructively alters data.
 */
const { pool } = require('./db');

const SCHEMA_SQL = `
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email TEXT UNIQUE NOT NULL,
  password_hash TEXT NOT NULL,
  display_name TEXT NOT NULL DEFAULT '',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  is_active BOOLEAN NOT NULL DEFAULT true,
  subscription_tier TEXT NOT NULL DEFAULT 'FREE',
  subscription_expires_at TIMESTAMPTZ
);

-- Keep entitlement fields additive and migration-safe for existing databases.
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_tier TEXT NOT NULL DEFAULT 'FREE';
ALTER TABLE users ADD COLUMN IF NOT EXISTS subscription_expires_at TIMESTAMPTZ;

-- Refresh tokens are stored HASHED (sha256), never in plaintext, and are
-- individually revocable (e.g. on logout, password change, or "sign out of
-- all devices"). One row per device/session, so a user can be logged in on
-- multiple devices simultaneously without invalidating each other.
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL UNIQUE,
  device_label TEXT NOT NULL DEFAULT 'unknown-device',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Server-authoritative entitlement codes. Codes are never written by the client;
-- redemption is transactional and recorded per user to prevent duplicate use.
CREATE TABLE IF NOT EXISTS entitlement_codes (
  code TEXT PRIMARY KEY,
  subscription_tier TEXT NOT NULL,
  expires_at TIMESTAMPTZ,
  max_redemptions INTEGER NOT NULL DEFAULT 1 CHECK (max_redemptions > 0),
  redeemed_count INTEGER NOT NULL DEFAULT 0 CHECK (redeemed_count >= 0),
  is_active BOOLEAN NOT NULL DEFAULT true,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS entitlement_redemptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code TEXT NOT NULL REFERENCES entitlement_codes(code) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  redeemed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (code, user_id)
);
CREATE INDEX IF NOT EXISTS idx_entitlement_redemptions_user_id ON entitlement_redemptions(user_id);

-- Generic per-user synced data store, mirroring the app's existing
-- Firestore document-per-collection sync model: one row per
-- (user, data_type), storing the full JSON payload for that data type
-- (profile, courses, sessions, attendance, grades, exams, tasks, notes).
-- Last-writer-wins by updated_at, matching the conflict-resolution
-- approach already used by the app's cloud sync.
CREATE TABLE IF NOT EXISTS user_data (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  data_type TEXT NOT NULL,
  payload JSONB NOT NULL,
  updated_at BIGINT NOT NULL,
  PRIMARY KEY (user_id, data_type)
);
`;

async function migrate() {
  const client = await pool.connect();
  try {
    console.log('[migrate] Applying schema...');
    await client.query(SCHEMA_SQL);
    console.log('[migrate] Done.');
  } finally {
    client.release();
    await pool.end();
  }
}

migrate().catch((err) => {
  console.error('[migrate] Failed:', err);
  process.exit(1);
});
