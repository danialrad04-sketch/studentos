require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const rateLimit = require('express-rate-limit');

const authRoutes = require('./routes/auth');
const syncRoutes = require('./routes/sync');

const REQUIRED_ENV = ['JWT_ACCESS_SECRET', 'DB_USER', 'DB_PASSWORD', 'DB_NAME'];
for (const key of REQUIRED_ENV) {
  if (!process.env[key]) {
    console.error(`[server] Missing required environment variable: ${key}`);
    process.exit(1);
  }
}

const app = express();

// Trust the Nginx reverse proxy in front of us (for correct client IPs in
// rate limiting / logs) — exactly one hop, not a wildcard trust.
app.set('trust proxy', 1);

app.use(helmet());
app.use(
  cors({
    // Only the Android app calls this API directly (no browser CORS needed
    // for a mobile client), so we keep this restrictive by default.
    // Add your own web origin here only if you build a companion web app.
    origin: false,
  })
);
app.use(express.json({ limit: '2mb' }));

// Global rate limit: generous for normal use, blocks brute-force/abuse.
const globalLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 300,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(globalLimiter);

// Stricter limiter specifically on auth endpoints to slow down credential
// stuffing / brute-force login attempts.
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 20,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'TOO_MANY_ATTEMPTS' },
});
app.use('/api/auth/login', authLimiter);
app.use('/api/auth/signup', authLimiter);

app.get('/health', (req, res) => res.json({ ok: true, time: new Date().toISOString() }));

app.use('/api/auth', authRoutes);
app.use('/api/sync', syncRoutes);

// 404 + generic error handler — never leak stack traces to the client.
app.use((req, res) => res.status(404).json({ error: 'NOT_FOUND' }));
app.use((err, req, res, next) => {
  console.error('[server] Unhandled error:', err);
  res.status(500).json({ error: 'INTERNAL_ERROR' });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`[server] Student OS backend listening on :${PORT}`));
