const test = require('node:test');
const assert = require('node:assert/strict');
const { Pool } = require('pg');

if (process.env.RUN_BACKEND_INTEGRATION !== '1') {
  test('backend integration suite (opt-in)', { skip: 'Set RUN_BACKEND_INTEGRATION=1 with a disposable PostgreSQL database.' }, () => {});
} else {
  const BASE = process.env.BACKEND_TEST_URL || 'http://127.0.0.1:31337';
  const pool = new Pool({
    host: process.env.DB_HOST || '127.0.0.1',
    port: Number(process.env.DB_PORT || 5432),
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
  });
  let server;
  let accessToken;
  let refreshToken;
  let userId;

  async function waitForHealth() {
    for (let i = 0; i < 40; i += 1) {
      try {
        const response = await fetch(BASE + '/health');
        if (response.ok) return;
      } catch (_) {}
      await new Promise((resolve) => setTimeout(resolve, 250));
    }
    throw new Error('Backend health endpoint did not become ready.');
  }

  async function request(path, options = {}) {
    const response = await fetch(BASE + path, {
      ...options,
      headers: { 'content-type': 'application/json', ...(options.headers || {}) },
    });
    const text = await response.text();
    return { response, body: text ? JSON.parse(text) : null };
  }

  test.before(async () => {
    await pool.query('TRUNCATE entitlement_redemptions, entitlement_codes, user_data, refresh_tokens, users CASCADE');
    const { app } = require('../src/server');
    server = app.listen(31337);
    await waitForHealth();
  });

  test('signup -> authenticated profile -> sync LWW -> logout/refresh lifecycle', async () => {
    const email = 'integration-' + Date.now() + '@studentos.test';
    const signup = await request('/api/auth/signup', {
      method: 'POST',
      body: JSON.stringify({ email, password: 'StrongPass!123', displayName: 'Integration User' }),
    });
    assert.equal(signup.response.status, 201);
    accessToken = signup.body.accessToken;
    refreshToken = signup.body.refreshToken;
    userId = signup.body.user.id;
    assert.ok(accessToken);
    assert.ok(refreshToken);

    const me = await request('/api/auth/me', {
      headers: { authorization: 'Bearer ' + accessToken },
    });
    assert.equal(me.response.status, 200);
    assert.equal(me.body.user.id, userId);

    const push = await request('/api/sync/profile', {
      method: 'PUT',
      headers: { authorization: 'Bearer ' + accessToken },
      body: JSON.stringify({ payload: { name: 'Local User' }, updatedAt: 1000 }),
    });
    assert.equal(push.response.status, 200);

    const stale = await request('/api/sync/profile', {
      method: 'PUT',
      headers: { authorization: 'Bearer ' + accessToken },
      body: JSON.stringify({ payload: { name: 'Stale User' }, updatedAt: 999 }),
    });
    assert.equal(stale.response.status, 409);
    assert.equal(stale.body.error, 'STALE_WRITE');

    const pull = await request('/api/sync/profile', {
      headers: { authorization: 'Bearer ' + accessToken },
    });
    assert.equal(pull.response.status, 200);
    assert.equal(pull.body.payload.name, 'Local User');
    assert.equal(pull.body.updatedAt, 1000);

    // Academic planning metadata must survive the real JSONB backend unchanged.
    const plannedTasks = [
      { id: 42, title: 'Weekly exercise', dueDate: '1405/07/01', isCompleted: true,
        courseId: 'math', semesterId: 'term', repeatEveryDays: 7, reminderTime: '09:30', recurrenceParentId: null },
      { id: 43, title: 'Weekly exercise', dueDate: '1405/07/08', isCompleted: false,
        courseId: 'math', semesterId: 'term', repeatEveryDays: 7, reminderTime: '09:30', recurrenceParentId: 42 },
    ];
    const taskPush = await request('/api/sync/tasks', {
      method: 'PUT', headers: { authorization: 'Bearer ' + accessToken },
      body: JSON.stringify({ payload: plannedTasks, updatedAt: 2000 }),
    });
    assert.equal(taskPush.response.status, 200);
    const taskPull = await request('/api/sync/tasks', { headers: { authorization: 'Bearer ' + accessToken } });
    assert.equal(taskPull.response.status, 200);
    assert.deepEqual(taskPull.body.payload, plannedTasks);

    const concurrent = await Promise.all(Array.from({ length: 8 }, (_, index) => request('/api/sync/tasks', {
      method: 'PUT', headers: { authorization: 'Bearer ' + accessToken },
      body: JSON.stringify({ payload: plannedTasks.map((task) => ({ ...task, title: 'Revision ' + index })), updatedAt: 3000 + index }),
    })));
    concurrent.forEach(({ response }) => assert.ok([200, 409].includes(response.status)));
    const latestTasks = await request('/api/sync/tasks', { headers: { authorization: 'Bearer ' + accessToken } });
    assert.equal(latestTasks.body.updatedAt, 3007);
    assert.equal(latestTasks.body.payload[0].title, 'Revision 7');
    const clearTasks = await request('/api/sync/tasks', {
      method: 'PUT', headers: { authorization: 'Bearer ' + accessToken },
      body: JSON.stringify({ payload: [], updatedAt: 4000 }),
    });
    assert.equal(clearTasks.response.status, 200);
    const emptyTasks = await request('/api/sync/tasks', { headers: { authorization: 'Bearer ' + accessToken } });
    assert.deepEqual(emptyTasks.body.payload, []);
    const missingPayload = await request('/api/sync/tasks', {
      method: 'PUT', headers: { authorization: 'Bearer ' + accessToken }, body: JSON.stringify({ updatedAt: 5000 }),
    });
    assert.equal(missingPayload.response.status, 400);

    const refresh = await request('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
    assert.equal(refresh.response.status, 200);
    assert.ok(refresh.body.accessToken);
    assert.ok(refresh.body.refreshToken);

    const oldRefresh = await request('/api/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    });
    assert.equal(oldRefresh.response.status, 401);

    const logout = await request('/api/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken: refresh.body.refreshToken }),
    });
    assert.equal(logout.response.status, 200);
  });

  test.after(async () => {
    if (userId) {
      await pool.query('DELETE FROM users WHERE id = $1', [userId]);
    }
    await pool.end();
    if (server) await new Promise((resolve) => server.close(resolve));
  });
}
