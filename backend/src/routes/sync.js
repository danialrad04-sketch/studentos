const express = require('express');
const { z } = require('zod');
const { pool } = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth);

const ALLOWED_DATA_TYPES = [
  'profile',
  'semesters',
  'courses',
  'sessions',
  'attendance',
  'grades',
  'exams',
  'tasks',
  'notes',
];

function isAllowedType(t) {
  return ALLOWED_DATA_TYPES.includes(t);
}

// ── Pull everything for this user (used for full restore on a new device) ─
router.get('/', async (req, res) => {
  try {
    const result = await pool.query(
      'SELECT data_type, payload, updated_at FROM user_data WHERE user_id = $1',
      [req.userId]
    );
    const out = {};
    for (const row of result.rows) {
      out[row.data_type] = { payload: row.payload, updatedAt: Number(row.updated_at) };
    }
    return res.json({ data: out });
  } catch (err) {
    console.error('[sync/pull-all]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Pull a single data type ────────────────────────────────────────────
router.get('/:dataType', async (req, res) => {
  const { dataType } = req.params;
  if (!isAllowedType(dataType)) {
    return res.status(400).json({ error: 'UNKNOWN_DATA_TYPE' });
  }
  try {
    const result = await pool.query(
      'SELECT payload, updated_at FROM user_data WHERE user_id = $1 AND data_type = $2',
      [req.userId, dataType]
    );
    if (!result.rows[0]) {
      return res.json({ payload: null, updatedAt: 0 });
    }
    return res.json({
      payload: result.rows[0].payload,
      updatedAt: Number(result.rows[0].updated_at),
    });
  } catch (err) {
    console.error('[sync/pull-one]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

// ── Push (upsert) a single data type — last-writer-wins by updatedAt ─────
// The client always sends its local updatedAt timestamp. The server only
// overwrites its stored copy if the incoming timestamp is newer, exactly
// mirroring the conflict-resolution rule already used by the app's
// Firestore sync (see FirestoreSyncManager.restoreAllDataFromCloud), so the
// client-side merge logic doesn't need any special-casing for this backend.
router.put('/:dataType', async (req, res) => {
  const { dataType } = req.params;
  if (!isAllowedType(dataType)) {
    return res.status(400).json({ error: 'UNKNOWN_DATA_TYPE' });
  }
  const parsed = z
    .object({
      payload: z.any(),
      updatedAt: z.number().int().nonnegative(),
    })
    .safeParse(req.body);
  if (!parsed.success) {
    return res.status(400).json({ error: 'INVALID_INPUT' });
  }
  const { payload, updatedAt } = parsed.data;

  // Prevent a malicious client clock from pinning a data type in the future.
  // Offline clients can still tolerate normal device clock drift (24h).
  const MAX_FUTURE_SKEW_MS = 24 * 60 * 60 * 1000;
  if (updatedAt > Date.now() + MAX_FUTURE_SKEW_MS) {
    return res.status(400).json({ error: 'UPDATED_AT_TOO_FAR_IN_FUTURE' });
  }

  try {
    const existing = await pool.query(
      'SELECT updated_at FROM user_data WHERE user_id = $1 AND data_type = $2',
      [req.userId, dataType]
    );

    if (existing.rows[0] && Number(existing.rows[0].updated_at) >= updatedAt) {
      // Server already has an equal-or-newer version — tell the client so
      // it can pull and reconcile instead of assuming its push "won".
      return res.status(409).json({
        error: 'STALE_WRITE',
        serverUpdatedAt: Number(existing.rows[0].updated_at),
      });
    }

    await pool.query(
      `INSERT INTO user_data (user_id, data_type, payload, updated_at)
       VALUES ($1, $2, $3, $4)
       ON CONFLICT (user_id, data_type)
       DO UPDATE SET payload = EXCLUDED.payload, updated_at = EXCLUDED.updated_at`,
      [req.userId, dataType, payload, updatedAt]
    );
    return res.json({ ok: true, updatedAt });
  } catch (err) {
    console.error('[sync/push]', err);
    return res.status(500).json({ error: 'INTERNAL_ERROR' });
  }
});

module.exports = router;
