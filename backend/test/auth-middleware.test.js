const test = require('node:test');
const assert = require('node:assert/strict');

process.env.JWT_ACCESS_SECRET = 'studentos-test-secret-2026';

const { signAccessToken } = require('../src/utils/jwt');
const { requireAuth } = require('../src/middleware/auth');

function runMiddleware(headers = {}) {
  return new Promise((resolve) => {
    const req = { headers };
    let statusCode = 200;
    const body = {};
    const res = {
      status(code) {
        statusCode = code;
        return this;
      },
      json(payload) {
        Object.assign(body, payload);
        resolve({ statusCode, body, nextCalled: false });
      },
    };
    requireAuth(req, res, () => resolve({ statusCode, body, nextCalled: true, userId: req.userId }));
  });
}

test('rejects missing bearer token', async () => {
  const result = await runMiddleware();
  assert.equal(result.statusCode, 401);
  assert.equal(result.body.error, 'MISSING_TOKEN');
  assert.equal(result.nextCalled, false);
});

test('accepts a valid access token and sets userId', async () => {
  const token = signAccessToken('user-test-1');
  const result = await runMiddleware({ authorization: 'Bearer ' + token });
  assert.equal(result.statusCode, 200);
  assert.equal(result.nextCalled, true);
  assert.equal(result.userId, 'user-test-1');
});

test('rejects a refresh-shaped token as an access token', async () => {
  const jwt = require('jsonwebtoken');
  const token = jwt.sign({ sub: 'user-test-1', type: 'refresh' }, process.env.JWT_ACCESS_SECRET);
  const result = await runMiddleware({ authorization: 'Bearer ' + token });
  assert.equal(result.statusCode, 401);
  assert.equal(result.body.error, 'INVALID_OR_EXPIRED_TOKEN');
});
