// Regression of the current production extractor's coverage limit, not a history collector.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const {audit}=require('./audit.cjs');
const source=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});
 try {
  const page=await browser.newPage();
  await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<!doctype html><title>synthetic</title><main></main>'}));
  await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');
  const rows=Array.from({length:40},(_,i)=>({id:`m${i}`,role:i%2?'assistant':'user',nested:0}));
  const reference={route:'/g/g-p-fixture/c/fixture',source:'independent synthetic 40-message fixture',scope:'selected fixture branch',rows};
  const mount=async indices=>page.evaluate(indices=>{
   document.querySelector('main').innerHTML=indices.map(i=>`<article data-message-id="m${i}"><div data-message-author-role="${i%2?'assistant':'user'}"><div class="markdown">BODY-${i}</div></div></article>`).join('');
   window.scrollTo(0,0);
  },indices);
  const read=async verify=>JSON.parse(await page.evaluate(source.replace('__NOVA_VERIFY_ONLY__',String(!!verify))));
  const sample=async()=>({schema:1,route:reference.route,routeAfter:reference.route,rows:await page.evaluate(()=>[...document.querySelectorAll('[data-message-author-role]')].map(e=>({id:e.closest('[data-message-id]').dataset.messageId,role:e.dataset.messageAuthorRole,nested:0}))),count:await page.locator('[data-message-author-role]').count(),bounded:false,ready:'complete',streamingHint:false,capturedAt:new Date().toISOString()});
  // Known middle gaps: even a stable, top-positioned snapshot successfully extracts seven.
  await mount([0,1,18,25,26,37,39]);
  const seven=await read();assert(!seven.error);assert.equal(seven.messages.length,7);
  assert.equal(seven.messages.filter(m=>m.role==='user').length,3);
  assert.equal(seven.messages.filter(m=>m.role==='assistant').length,4);
  assert.equal((await read(true)).diagnostic.signature,seven.diagnostic.signature);
  assert.equal(await page.evaluate(()=>window.scrollY),0);
  assert.equal(seven.diagnostic.history,'not-proven');
  const gap=audit(reference,[await sample()]);assert.equal(gap.missingAcrossEligibleSamples.length,33);assert(!gap.singleSnapshotMatches);
  // Replaced windows cover 40 in their union; the last DOM still only contains seven.
  const snapshots=[];
  for(let start=0;start<40;start+=5){await mount(Array.from({length:Math.min(7,40-start)},(_,i)=>start+i));snapshots.push(await sample());}
  await mount([33,34,35,36,37,38,39]);
  assert.equal((await read()).messages.length,7);
  const union=audit(reference,snapshots);assert.equal(union.status,'union-only-match');assert.equal(union.historyCompleteness,'not-proven');assert(!union.singleSnapshotMatches);
  // ID coverage is not body fidelity: unchanged IDs with changed text still match ID audit.
  await mount(Array.from({length:40},(_,i)=>i));
  const original=await read();await page.evaluate(()=>document.querySelector('.markdown').textContent='CHANGED-BODY');
  assert.notEqual((await read(true)).diagnostic.signature,original.diagnostic.signature);
  const body=audit(reference,[await sample()]);assert(body.singleSnapshotMatches);assert.equal(body.bodyFidelity,'not-tested');assert.equal(body.historyCompleteness,'not-proven');
  console.log('PASS: 3 production snapshot limitation scenarios (stable 7 with 33 missing; virtual-window union; ID/body distinction); synthetic only');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
