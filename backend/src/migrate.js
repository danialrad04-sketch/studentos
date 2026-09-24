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
  is_active BOOLEAN NOT NULL DEFAULT true
);

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
