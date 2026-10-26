'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { main, mergeConfig, FUNCTIONS } = require('../scripts/setup-openai');
function fixture() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'studentos-owner-'));
  fs.mkdirSync(path.join(root, 'app')); fs.mkdirSync(path.join(root, 'functions'));
  fs.writeFileSync(path.join(root, 'app/google-services.json'), JSON.stringify({ project_info: { project_id: 'demo-studentos' } }));
  const cli = path.join(root, 'functions/node_modules/firebase-tools/lib/bin');
  fs.mkdirSync(cli, { recursive: true }); fs.writeFileSync(path.join(cli, 'firebase.js'), '');
  return root;
}
test('owner check is read only, needs no API key and never invokes deployment', () => {
  const root = fixture();
  try {
    const result = main(['--check'], { root, log() {}, run() { throw Error('unexpected cloud mutation'); } });
    assert.equal(result.deploy, false);
    assert.equal(fs.existsSync(path.join(root, 'functions/.env.demo-studentos')), false);
  } finally { fs.rmSync(root, { recursive: true }); }
});
test('configuration updates preserve billing, quotas and unrelated owner settings', () => {
  const text = '# keep\nPREMIUM_SALES_ENABLED=false\nAI_MODEL=old\nAI_MODEL=duplicate\nAI_DAILY_TOTAL_LIMIT=50\nOTHER_VALUE=keep\n';
  const output = mergeConfig(text);
  assert.ok(output.includes('PREMIUM_SALES_ENABLED=false\n'));
  assert.ok(output.includes('AI_DAILY_TOTAL_LIMIT=50\n'));
  assert.ok(output.includes('OTHER_VALUE=keep\n'));
  assert.equal((output.match(/^AI_MODEL=/gm) || []).length, 1);
  assert.equal(mergeConfig(output), output);
});
test('explicit owner setup uses secure secret input then deploys only the two assistant functions', () => {
  const root = fixture(), calls = [];
  try {
    main(['--deploy'], { root, log() {}, run(exe, args, opts) { calls.push({ exe, args, opts }); return { status: 0 }; } });
    assert.equal(calls.length, 2);
    assert.deepEqual(calls[0].args.slice(1), ['functions:secrets:set', 'AI_API_KEY', '--project', 'demo-studentos']);
    assert.deepEqual(calls[1].args.slice(1), ['deploy', '--only', FUNCTIONS, '--project', 'demo-studentos']);
    assert.equal(calls[0].opts.stdio, 'inherit'); assert.equal(calls[0].opts.shell, false);
    assert.ok(!fs.readFileSync(path.join(root, 'functions/.env.demo-studentos'), 'utf8').includes('AI_API_KEY='));
    assert.throws(() => main(['--deploy', '--project', '--unsafe'], { root, log() {} }), /معتبر/);
    assert.throws(() => main(['--deploy', '--project'], { root, log() {} }), /معتبر/);
  } finally { fs.rmSync(root, { recursive: true }); }
});
test('failed secret registration never proceeds to deployment', () => {
  const root = fixture(); let calls = 0;
  try {
    assert.throws(() => main(['--deploy'], { root, log() {}, run() { calls++; return { status: 1 }; } }), /موفق نشد/);
    assert.equal(calls, 1);
  } finally { fs.rmSync(root, { recursive: true }); }
});

test('a plaintext key in an existing env file blocks deployment without printing or rewriting it', () => {
  const root = fixture(); let calls = 0;
  const file = path.join(root, 'functions/.env.demo-studentos');
  fs.writeFileSync(file, 'AI_API_KEY=private-test-only\n');
  try {
    assert.throws(() => main(['--deploy'], { root, log() {}, run() { calls++; } }), e => !e.message.includes('private-test-only') && /Secret Manager/.test(e.message));
    assert.equal(calls, 0); assert.equal(fs.readFileSync(file, 'utf8'), 'AI_API_KEY=private-test-only\n');
  } finally { fs.rmSync(root, { recursive: true }); }
});
