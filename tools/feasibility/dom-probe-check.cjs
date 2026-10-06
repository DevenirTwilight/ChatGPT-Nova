const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {chromium} = require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const script = fs.readFileSync(path.join(__dirname, 'dom-probe.js'), 'utf8');

(async () => {
  const browser = await chromium.launch({executablePath: '/usr/bin/chromium',
    args: ['--no-sandbox', '--disable-dev-shm-usage']});
  const reports = [];
  try {
    const page = await browser.newPage();
    await page.route('**/*', r => r.fulfill({contentType: 'text/html', body: '<main></main>'}));
    await page.goto('https://chatgpt.com/c/fixture');
    const setRows = async (start, count) => page.evaluate(({start, count}) => {
      document.querySelector('main').innerHTML = Array.from({length: count}, (_, i) =>
        `<article data-message-id="m${start+i}" data-message-author-role="${i%2?'assistant':'user'}">MESSAGE-${start+i}</article>`).join('');
    }, {start, count});
    const read = async () => JSON.parse(await page.evaluate(script));
    const passed = (name, report) => { reports.push({name, report}); console.log('PASS: ' + name); };

    await setRows(0, 80);
    let report = await read();
    assert.equal(report.count, 80); assert.equal(report.samples[0].id, 'm0');
    assert.equal(report.samples.at(-1).id, 'm79'); assert.equal(report.bounded, false);
    assert.equal(report.historyCompleteness, 'not-proven');
    passed('all loaded messages remain explicitly unproven as complete history', report);

    await setRows(60, 20);
    report = await read();
    assert.equal(report.count, 20); assert.equal(report.samples[0].id, 'm60');
    assert.equal(report.historyCompleteness, 'not-proven');
    passed('partial DOM does not become complete history', report);
    await setRows(0, 20);
    const top = await read();
    assert.equal(top.count, report.count); assert.notEqual(top.signature, report.signature);
    passed('equal-count virtualization changes identity signature', top);

    await page.evaluate(() => document.querySelector('article').textContent += ' STREAMING');
    const changed = await read();
    assert.notEqual(changed.signature, top.signature);
    passed('same-ID edits change the content signature', changed);

    await page.evaluate(() => {
      const a = document.querySelector('article');
      a.innerHTML = '<button>Copy</button><pre><code>a\nb</code></pre><table><tr><td>x</td></tr></table>'
        + '<span class="katex"><annotation encoding="application/x-tex">E=mc^2</annotation></span>';
      window.fetch = () => { throw Error('probe must not request data'); };
      a.__reactFiber$fixture = Object.freeze({memoizedProps: Object.freeze({fixture: true})});
      window.originalFetch = window.fetch;
      window.originalFiber = a.__reactFiber$fixture;
      window.beforeHtml = document.documentElement.outerHTML;
      window.beforeKeys = '';
      window.beforeKeys = Object.getOwnPropertyNames(window).sort().join('|');
    });
    report = await read();
    assert.equal(report.samples[0].codeBlocks, 1); assert.equal(report.samples[0].tables, 1);
    assert.equal(report.samples[0].texSources, 1);
    assert(await page.evaluate(() => window.fetch === window.originalFetch
      && document.querySelector('article').__reactFiber$fixture === window.originalFiber
      && document.documentElement.outerHTML === window.beforeHtml
      && Object.getOwnPropertyNames(window).sort().join('|') === window.beforeKeys));
    passed('rich structure is detected without modifying DOM, fetch, Fiber or globals', report);

    await page.evaluate(() => {
      const rows = document.querySelectorAll('article');
      rows[1].setAttribute('data-message-id', rows[0].getAttribute('data-message-id'));
      rows[2].removeAttribute('data-message-id');
    });
    report = await read(); assert.equal(report.duplicateIds, 1); assert.equal(report.missingIds, 1);
    passed('ambiguous and missing IDs are reported', report);

    await setRows(0, 400);
    report = await read(); assert.equal(report.count, 400); assert.equal(report.inspected, 300);
    assert.equal(report.bounded, true);
    passed('node cap is explicit rather than silent truncation', report);
    await setRows(0, 1);
    await page.evaluate(() => document.querySelector('article').textContent = 'x'.repeat(200000));
    report = await read(); assert.equal(report.chars, 200000); assert.equal(report.bounded, true);
    passed('text sampling cap is explicit', report);

    await page.goto('https://example.org/');
    assert.equal((await read()).error, 'wrong-origin-or-frame');
    passed('other origins are rejected', {error: 'wrong-origin-or-frame'});
    fs.writeFileSync(path.join(__dirname, 'dom-probe-results.json'), JSON.stringify(reports, null, 2) + '\n');
    console.log(`PASS: ${reports.length} synthetic DOM feasibility checks; no live-account proof`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
