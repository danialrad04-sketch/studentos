'use strict';
// Owner-only setup. API key input is handled by Firebase Secret Manager's secure CLI prompt.
const fs = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const { DEFAULT_OPENAI_MODEL } = require('../lib/ai-provider');
const DEFAULTS = Object.freeze({ AI_MODEL: DEFAULT_OPENAI_MODEL, AI_API_BASE_URL: 'https://api.openai.com/v1',
  AI_TOKEN_PARAMETER: 'max_completion_tokens', AI_VISION_ENABLED: 'true' });
const FUNCTIONS = 'functions:getAcademicAiStatus,functions:generateAcademicAssistantReply';
function mergeConfig(text) {
  const seen = new Set();
  const lines = text.split(/\r?\n/).map(line => {
    const key = /^\s*([A-Z_]+)\s*=/.exec(line)?.[1];
    if (!Object.hasOwn(DEFAULTS, key)) return line;
    if (seen.has(key)) return null;
    seen.add(key); return key + '=' + DEFAULTS[key];
  }).filter(line => line !== null);
  for (const [key, value] of Object.entries(DEFAULTS)) if (!seen.has(key)) lines.push(key + '=' + value);
  return lines.join('\n').replace(/\n*$/, '\n');
}
function main(args = process.argv.slice(2), options = {}) {
  const root = options.root || path.resolve(__dirname, '../..');
  const run = options.run || spawnSync;
  const log = options.log || console.log;
  let deploy = false, project;
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--deploy') deploy = true;
    else if (args[i] === '--check') continue;
    else if (args[i] === '--project') {
      project = args[++i];
      if (!project || project.startsWith('--')) throw Error('شناسهٔ Firebase معتبر نیست.');
    }
    else throw Error('گزینهٔ ناشناخته؛ از --check یا --deploy و در صورت نیاز --project استفاده کن.');
  }
  if (!project) project = JSON.parse(fs.readFileSync(path.join(root, 'app/google-services.json'), 'utf8')).project_info.project_id;
  if (!/^[a-z][a-z0-9-]{4,28}[a-z0-9]$/.test(project)) throw Error('شناسهٔ Firebase معتبر نیست.');
  const cli = path.join(root, 'functions/node_modules/firebase-tools/lib/bin/firebase.js');
  log('Firebase: ' + project + ' | OpenAI: ' + DEFAULT_OPENAI_MODEL);
  log('مالک یک‌بار Anonymous Auth و App Check مناسب نسخهٔ بازار را در Firebase فعال می‌کند؛ کاربران هیچ تنظیمی ندارند.');
  log('پیش‌نیاز مدیر: حساب OpenAI API دارای اعتبار مصرف و Firebase با طرح Blaze؛ اشتراک ChatGPT کافی نیست.');
  log('راهنمای شروع: docs/AI_OWNER_QUICKSTART_FA.md | حالت بررسی هیچ فایل یا تنظیم ابری را تغییر نمی‌دهد.');
  if (!deploy) return { project, deploy: false };
  if (!fs.existsSync(cli)) throw Error('ابتدا npm --prefix functions ci --ignore-scripts --no-audit --no-fund را اجرا کن.');
  const configFile = path.join(root, 'functions/.env.' + project);
  const content = fs.existsSync(configFile) ? fs.readFileSync(configFile, 'utf8') : '';
  if (/^\s*AI_API_KEY\s*=/m.test(content)) throw Error('کلید AI_API_KEY نباید در فایل env باشد؛ آن را در Secret Manager ثبت کن.');
  const temporary = configFile + '.openai-tmp';
  try { fs.writeFileSync(temporary, mergeConfig(content), { mode: 0o600 }); fs.renameSync(temporary, configFile); }
  finally { if (fs.existsSync(temporary)) fs.unlinkSync(temporary); }
  function firebase(command) {
    const result = run(process.execPath, [cli, ...command, '--project', project], { cwd: root, stdio: 'inherit', shell: false });
    if (result.error || result.status !== 0) throw Error('مرحلهٔ Firebase موفق نشد؛ ورود مدیر، دسترسی پروژه و راهنمای راه‌اندازی را بررسی کن.');
  }
  log('کلید را فقط در ورودی امن فرمان بعد وارد کن؛ در گفتگو یا گیت قرار نده.');
  firebase(['functions:secrets:set', 'AI_API_KEY']);
  firebase(['deploy', '--only', FUNCTIONS]);
  log('استقرار انجام شد؛ پاسخ واقعی و App Check را روی APK امضاشده بررسی کن. استقرار به‌تنهایی تأیید پاسخ مدل نیست.');
  return { project, deploy: true };
}
if (require.main === module) {
  try { main(); } catch (error) { console.error(error.message); process.exitCode = 1; }
}
module.exports = { main, mergeConfig, DEFAULTS, FUNCTIONS };
