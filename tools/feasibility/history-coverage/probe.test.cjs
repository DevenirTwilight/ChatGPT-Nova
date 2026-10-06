const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {chromium} = require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const script = fs.readFileSync(path.join(__dirname, 'probe.js'), 'utf8');
(async () => {
  const browser = await chromium.launch({executablePath: '/usr/bin/chromium', args: ['--no-sandbox']});
  try {
    const page = await browser.newPage();
    await page.route('**/*', r => r.fulfill({contentType: 'text/html', body: '<main></main>'}));
    await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');
    const set = n => page.evaluate(n => {
      document.querySelector('main').innerHTML = Array.from({length: n}, (_, i) =>
        `<article data-message-id="fixture-${i}"><div data-message-author-role="${i % 2 ? 'assistant' : 'user'}">PRIVATE-BODY</div></article>`).join('');
    }, n);
    const read = async () => JSON.parse(await page.evaluate(script));
    await set(400);
    const before = await page.content();
    await page.evaluate(() => { window.fetch = () => { throw Error('no network'); }; });
    const report = await read();
    assert.equal(report.rows.length, 400);
    assert.equal(report.rows[200].id, 'fixture-200');
    assert.equal(report.bounded, false);
    assert.equal(report.historyCompleteness, 'not-proven');
    assert(!JSON.stringify(report).includes('PRIVATE-BODY'));
    assert.equal(await page.content(), before);
    await set(2001);
    const capped = await read();
    assert.equal(capped.count, 2001); assert.equal(capped.rows.length, 2000);
    assert.equal(capped.bounded, true);
    await page.goto('https://chatgpt.com/c/fixture');
    assert.equal((await read()).error, 'not-project-conversation');
    await page.goto('https://example.org/g/g-p-fixture/c/fixture');
    assert.equal((await read()).error, 'wrong-origin-or-frame');
    console.log('PASS: browser metadata, cap, route and read-only checks; synthetic only');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
