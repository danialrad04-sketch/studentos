const express = require('express');
const bcrypt = require('bcrypt');
const { z } = require('zod');
const { pool } = require('../db');
const {
  signAccessToken,
  generateRefreshToken,
  hashRefreshToken,
} = require('../utils/jwt');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();
const BCRYPT_ROUNDS = 12;

const emailSchema = z.string().email().max(255);
const passwordSchema = z.string().min(8).max(128);

function publicUser(row) {
  return { id: row.id, email: row.email, displayName: row.display_name };
}

async function issueTokenPair(userId, deviceLabel) {
  const accessToken = signAccessToken(userId);
  const { raw, hash, expiresAt } = generateRefreshToken();
  await pool.query(
    `INSERT INTO refresh_tokens (user_id, token_hash, device_label, expires_at)
     VALUES ($1, $2, $3, $4)`,
    [userId, hash, deviceLabel || 'unknown-device', expiresAt]
  );
  return { accessToken, refreshToken: raw };
}

// ── Sign up ──────────────────────────────────────────────────────────────
router.post('/signup', async (req, res) => {
  const parsed = z
    .object({
      email: emailSchema,
      password: passwordSchema,
      displayName: z.string().max(120).optional().default(''),
      deviceLabel: z.string().max(120).optional(),
    })
    .safeParse(req.body);

  if (!parsed.success) {
    return res.status(400).json({ error: 'INVALID_INPUT', details: parsed.error.flatten() });
  }
  const { email, password, displayName, deviceLabel } = parsed.data;

  try {
    const existing = await pool.query('SELECT id FROM users WHERE email = $1', [
      email.toLowerCase(),
    ]);
    if (existing.rows.length > 0) {
      return res.status(409).json({ error: 'EMAIL_ALREADY_REGISTERED' });
    }

    const passwordHash = await bcrypt.hash(password, BCRYPT_ROUNDS);
    const insert = await pool.query(
      `INSERT INTO users (email, password_hash, display_name)
       VALUES ($1, $2, $3) RETURNING id, email, display_name`,
      [email.toLowerCase(), passwordHash, displayName]
    );
    const user = insert.rows[0];
    const tokens = await issueTokenPair(user.id, deviceLabel);

    return res.status(201).json({ user: publicUser(user), ...tokens });
  } catch (err) {
    console.error('[auth/signup]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Log in ───────────────────────────────────────────────────────────────
router.post('/login', async (req, res) => {
  const parsed = z
    .object({
      email: emailSchema,
      password: z.string().min(1).max(128),
      deviceLabel: z.string().max(120).optional(),
    })
    .safeParse(req.body);

  if (!parsed.success) {
    return res.status(400).json({ error: 'INVALID_INPUT' });
  }
  const { email, password, deviceLabel } = parsed.data;

  try {
    const result = await pool.query(
      'SELECT id, email, password_hash, display_name, is_active FROM users WHERE email = $1',
      [email.toLowerCase()]
    );
    const user = result.rows[0];

    // Same generic error for "no such user" and "wrong password" —
    // never reveal which one it was, to avoid account enumeration.
    if (!user || !user.is_active) {
      return res.status(401).json({ error: 'INVALID_CREDENTIALS' });
    }
    const valid = await bcrypt.compare(password, user.password_hash);
    if (!valid) {
      return res.status(401).json({ error: 'INVALID_CREDENTIALS' });
    }

    const tokens = await issueTokenPair(user.id, deviceLabel);
    return res.json({ user: publicUser(user), ...tokens });
  } catch (err) {
    console.error('[auth/login]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Refresh access token ─────────────────────────────────────────────────
router.post('/refresh', async (req, res) => {
  const parsed = z.object({ refreshToken: z.string().min(10) }).safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: 'INVALID_INPUT' });
  }
  const { refreshToken } = parsed.data;
  const hash = hashRefreshToken(refreshToken);

  try {
    const result = await pool.query(
      `SELECT id, user_id, expires_at, revoked_at, device_label
       FROM refresh_tokens WHERE token_hash = $1`,
      [hash]
    );
    const row = result.rows[0];
    if (!row || row.revoked_at || new Date(row.expires_at) < new Date()) {
      return res.status(401).json({ error: 'INVALID_OR_EXPIRED_REFRESH_TOKEN' });
    }

    // Rotate: revoke the used refresh token and issue a brand new pair.
    // This limits the blast radius if a refresh token is ever stolen —
    // it can only be used once before rotation invalidates it.
    await pool.query('UPDATE refresh_tokens SET revoked_at = now() WHERE id = $1', [row.id]);
    const tokens = await issueTokenPair(row.user_id, row.device_label);

    return res.json(tokens);
  } catch (err) {
    console.error('[auth/refresh]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Log out (this device only) ───────────────────────────────────────────
router.post('/logout', async (req, res) => {
  const parsed = z.object({ refreshToken: z.string().min(10) }).safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: 'INVALID_INPUT' });
  }
  const hash = hashRefreshToken(parsed.data.refreshToken);
  try {
    await pool.query(
      'UPDATE refresh_tokens SET revoked_at = now() WHERE token_hash = $1 AND revoked_at IS NULL',
      [hash]
    );
    return res.json({ ok: true });
  } catch (err) {
    console.error('[auth/logout]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Log out of ALL devices ───────────────────────────────────────────────
router.post('/logout-all', requireAuth, async (req, res) => {
  try {
    await pool.query(
      'UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = $1 AND revoked_at IS NULL',
      [req.userId]
    );
    return res.json({ ok: true });
  } catch (err) {
    console.error('[auth/logout-all]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Delete current account and all server-owned data ──────────────────────
// The user id comes exclusively from the verified access token. The foreign
// keys in the migration schema cascade deletion to refresh_tokens and user_data,
// so the operation removes the account and its server-owned data together.
router.delete('/account', requireAuth, async (req, res) => {
  try {
    const result = await pool.query(
      'DELETE FROM users WHERE id = $1 RETURNING id',
      [req.userId]
    );
    if (!result.rows[0]) {
      return res.status(404).json({ error: 'USER_NOT_FOUND' });
    }
    return res.status(204).send();
  } catch (err) {
    console.error('[auth/delete-account]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Current user (sanity/profile check) ──────────────────────────────────
router.get('/me', requireAuth, async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT id, email, display_name FROM users WHERE id = $1',
      [req.userId]
    );
    if (!result.rows[0]) return res.status(404).json({ error: 'USER_NOT_FOUND' });
    return res.json({ user: publicUser(result.rows[0]) });
  } catch (err) {
    console.error('[auth/me]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

module.exports = router;
