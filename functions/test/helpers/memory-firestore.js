'use strict';
const assert = require('node:assert/strict');
class MemoryFirestore {
  constructor() { this.rows = new Map(); this.tail = Promise.resolve(); }
  collection(name) { return { doc: id => this.ref(name + '/' + id) }; }
  ref(path) { return { path, get: async () => this.snapshot(path), set: async (data, options) => this.write(path, data, options) }; }
  snapshot(path) { const row = this.rows.get(path); return { exists: row != null, data: () => row == null ? undefined : structuredClone(row) }; }
  write(path, data, options) { this.rows.set(path, options?.merge ? { ...(this.rows.get(path) || {}), ...data } : data); }
  runTransaction(action) {
    const result = this.tail.then(async () => {
      const writes = [];
      const tx = { get: ref => ref.get(), set: (ref, data, options) => writes.push([ref.path, data, options]), create: (ref, data) => { assert.equal(this.rows.has(ref.path), false); writes.push([ref.path, data]); } };
      const result = await action(tx);
      for (const args of writes) this.write(...args);
      return result;
    });
    this.tail = result.catch(() => {}); return result;
  }
}

module.exports = { MemoryFirestore };
