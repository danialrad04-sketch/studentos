'use strict';
// Explicit opt-in live request using synthetic data; never prints credentials or response text.
const { generateAi, config } = require('../lib/ai-provider');
const core = require('../lib/premium-core');
async function main() {
  if (process.env.AI_LIVE_TEST !== '1') throw new Error('Set AI_LIVE_TEST=1 to authorize one billable synthetic model request.');
  if (!config(process.env)) throw new Error('Configure AI_API_KEY in the server environment; OpenAI model and endpoint have defaults.');
  const result = await generateAi(core.aiRequest({ prompt: 'به فارسی، تفاوت حافظهٔ اصلی و حافظهٔ نهان را با یک مثال و یک سؤال تمرینی توضیح بده.', studentContext: JSON.stringify({ gpaOfRecordedGrades: null, courses: [], tasks: [] }), kind: 'chat' }), process.env);
  if (!/[\u0600-\u06ff]/u.test(result.text)) throw new Error('Model replied without Persian text.');
  console.log(JSON.stringify({ liveProviderRequest: 'passed', nonEmptyPersianResponse: true, characters: result.text.length }));
}
main().catch(error => { console.error(error instanceof core.ServiceError ? error.message : 'Live probe did not pass: ' + error.message); process.exitCode = 1; });
