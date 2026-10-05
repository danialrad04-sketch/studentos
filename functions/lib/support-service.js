'use strict';
const crypto = require('node:crypto');
const { ServiceError } = require('./premium-core');
const categories = ['ACADEMIC_COPILOT', 'SCHEDULE_AND_CALENDAR', 'GRADES_AND_CURRICULUM', 'ACCOUNT_AND_SYNC', 'BUG_REPORT', 'FEATURE_SUGGESTION'];
const priorities = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
function text(value, max, label) {
  if (typeof value !== 'string' || !value.trim() || value.length > max) throw new ServiceError('invalid-argument', `${label} معتبر نیست.`);
  return value.trim();
}
class SupportService {
  constructor(db, clock = Date.now) { this.db = db; this.clock = clock; }
  ref(uid, id) { return this.db.collection('users').doc(uid).collection('tickets').doc(id); }
  global(id) { return this.db.collection('support_tickets').doc(id); }
  async create(uid, data = {}) {
    if (!uid || !data || typeof data !== "object" || Array.isArray(data)) throw new ServiceError("invalid-argument", "اطلاعات درخواست معتبر نیست.");
    const subject = text(data.subject, 160, 'موضوع');
    const message = text(data.message, 4000, 'پیام');
    const requestId = text(data.requestId, 64, 'شناسه درخواست');
    if (!/^[a-f0-9-]{36}$/i.test(requestId) || !categories.includes(data.category) || !priorities.includes(data.priority)) throw new ServiceError('invalid-argument', 'اطلاعات درخواست معتبر نیست.');
    const id = 'TKT-' + crypto.createHash('sha256').update(uid + ':' + requestId).digest('hex').slice(0, 20);
    const now = this.clock();
    const name = typeof data.studentName === 'string' ? data.studentName.trim().slice(0, 100) : 'دانشجو';
    const ticket = { id, userId: uid, userEmail: null, studentName: name, subject, category: data.category, priority: data.priority,
      status: 'OPEN', createdAt: now, updatedAt: now, messages: [{ id: requestId, senderId: uid, senderName: name, senderRole: 'STUDENT', message, timestamp: now }] };
    return this.db.runTransaction(async tx => {
      const current = await tx.get(this.ref(uid, id));
      if (current.exists) return current.data();
      tx.create(this.ref(uid, id), ticket); tx.create(this.global(id), ticket);
      return ticket;
    });
  }
  async reply(uid, data = {}) {
    if (!uid || !data || typeof data !== "object" || Array.isArray(data)) throw new ServiceError("invalid-argument", "اطلاعات درخواست معتبر نیست.");
    const id = text(data.ticketId, 64, 'شماره تیکت');
    const message = text(data.message, 4000, 'پاسخ');
    return this.db.runTransaction(async tx => {
      const ref = this.ref(uid, id), current = await tx.get(ref);
      if (!current.exists || current.data().userId !== uid) throw new ServiceError('not-found', 'تیکت متعلق به این حساب یافت نشد.');
      const ticket = current.data();
      if (ticket.status === 'CLOSED') throw new ServiceError('failed-precondition', 'این تیکت بسته شده است. درخواست تازه‌ای ثبت کنید.');
      if (ticket.messages.length >= 100) throw new ServiceError('resource-exhausted', 'این گفتگو به سقف پیام رسیده؛ درخواست تازه‌ای ثبت کنید.');
      const now = this.clock();
      const updated = { ...ticket, status: 'IN_PROGRESS', updatedAt: now, messages: [...ticket.messages,
        { id: crypto.randomUUID(), senderId: uid, senderName: ticket.studentName, senderRole: 'STUDENT', message, timestamp: now }] };
      tx.set(ref, updated); tx.set(this.global(id), updated);
      return { ok: true };
    });
  }
  async close(uid, data = {}) {
    if (!uid || !data || typeof data !== "object" || Array.isArray(data)) throw new ServiceError("invalid-argument", "اطلاعات درخواست معتبر نیست.");
    const id = text(data.ticketId, 64, 'شماره تیکت');
    return this.db.runTransaction(async tx => {
      const ref = this.ref(uid, id), current = await tx.get(ref);
      if (!current.exists || current.data().userId !== uid) throw new ServiceError('not-found', 'تیکت متعلق به این حساب یافت نشد.');
      const updated = { ...current.data(), status: 'CLOSED', updatedAt: this.clock() };
      tx.set(ref, updated); tx.set(this.global(id), updated);
      return { ok: true };
    });
  }
}
module.exports = { SupportService };
