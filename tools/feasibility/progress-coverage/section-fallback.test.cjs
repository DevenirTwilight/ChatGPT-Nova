const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const current=require('node:child_process').execFileSync('git',['show','e56c225:app/src/main/assets/export/dom-trial.js'],{encoding:'utf8'}).replace('__NOVA_VERIFY_ONLY__','false');
// Historical proposal against the shipped e56c225 baseline; kept for before/after evidence.
const proposed=current
 .replace("querySelectorAll('article[data-turn=\"assistant\"]')","querySelectorAll('article[data-turn=\"assistant\"],section[data-turn=\"assistant\"][data-testid^=\"conversation-turn-\"]')")
 .replace("closest('[data-message-author-role],article[data-turn=\"assistant\"]')","closest('[data-message-author-role],article[data-turn=\"assistant\"],section[data-turn=\"assistant\"][data-testid^=\"conversation-turn-\"]')");
assert.notEqual(current,proposed);
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});let checks=0;
 try {
  const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<main></main>'}));await page.goto('https://chatgpt.com/c/PRIVATE-ROUTE');
  const normal=Array.from({length:12},(_,i)=>`<section data-turn="${i%2?'assistant':'user'}" data-testid="conversation-turn-${i}"><div data-message-author-role="${i%2?'assistant':'user'}" data-message-id="PRIVATE-ID-${i}"><div class="markdown"><p>${i===3 || i===11?'TRACE-TARGET':'ordinary text'}</p></div></div></section>`).join('');
  const orphan='<section data-turn="assistant" data-testid="conversation-turn-missing">'+ '<div>'.repeat(14)+'<div class="markdown"><p><strong>TRACE-TARGET</strong> original progress</p></div>'+ '</div>'.repeat(14)+'</section>';
  const setup=html=>page.evaluate(html=>document.querySelector('main').innerHTML=html,html);
  const read=async source=>{const before=await page.content();const r=JSON.parse(await page.evaluate(source));assert.equal(await page.content(),before);return r;};
  await setup(orphan+normal);let r=await read(current);assert.equal(r.messages.length,12);assert(!r.messages.some(m=>m.markdown.includes('original progress')));checks++;
  r=await read(proposed);assert.equal(r.messages.length,13);assert.equal(r.messages[0].role,'assistant');assert.equal(r.messages[0].id,'');assert.equal(r.diagnostic.missingIds,1);assert(r.messages[0].markdown.includes('original progress'));
  assert.equal(r.messages.filter(m=>m.markdown.includes('TRACE-TARGET')).length,3);checks++;
  await setup(orphan.replace(' data-turn="assistant"','')+normal);assert.equal((await read(proposed)).messages.length,12);checks++;
  await setup(orphan.replace('data-turn="assistant"','data-turn="assistant" hidden')+normal);assert.equal((await read(proposed)).messages.length,12);checks++;
  // A section containing a normal author AND an orphan remains a different problem.
  await setup(normal.replace('<div data-message-author-role="assistant"','<div class="markdown"><p>original progress</p></div><div data-message-author-role="assistant"'));
  r=await read(proposed);assert.equal(r.messages.length,12);assert(!r.messages.some(m=>m.markdown.includes('original progress')));checks++;
  console.log(`PASS ${checks} section-fallback proposal checks; tool-only assumptions, no production fix`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
