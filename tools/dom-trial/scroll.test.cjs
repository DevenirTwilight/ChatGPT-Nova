const assert=require('node:assert/strict'),fs=require('node:fs');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const snapshot=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8').replace('__NOVA_VERIFY_ONLY__','false');
const source=fs.readFileSync('app/src/main/assets/export/scroll-trial.js','utf8').replace('__NOVA_SNAPSHOT__',()=>snapshot);
const fixture=fs.readFileSync('tools/dom-trial/virtual-fixture.js','utf8');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});let checks=0;
 try {
  const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<!doctype html><main></main>'}));
  const setup=async(n=40)=>{await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');await page.evaluate(fixture.replace('__NOVA_FIXTURE_COUNT__',String(n)));};
  const step=async(command='poll',token='fixture-token')=>JSON.parse(await page.evaluate(source.replace('__NOVA_SCROLL_COMMAND__',JSON.stringify(command)).replace('__NOVA_SCROLL_TOKEN__',JSON.stringify(token))));
  const finish=async()=>{let result;for(let i=0;i<500;i++){result=await step();if(result.error || result.done) return result;await page.waitForTimeout(20);}throw Error('fixture poll limit');};
  const collectFirst=async()=>{let r=await step('start');for(let i=0;i<50 && !(r.coverage?.count>0);i++){await page.waitForTimeout(20);r=await step();}assert(r.coverage?.count>0,JSON.stringify(r));await page.waitForTimeout(60);};
  await setup();const original=await page.locator('#history').evaluate(e=>e.scrollTop);await step('start');
  const all=await finish();assert(all.done,JSON.stringify(all));assert.equal(all.messages.length,40);assert.deepEqual(all.messages.map(m=>m.id),Array.from({length:40},(_,i)=>'m'+i));
  assert.equal(all.messages.filter(m=>m.markdown.includes('REPEATED-TEXT')).length,2);
  assert(all.coverage.topObserved && all.coverage.bottomObserved && all.coverage.secondPass);assert.equal(all.coverage.history,'not-proven');
  assert.equal(await page.locator('[data-message-author-role]').count(),7);assert.equal(await page.locator('#history').evaluate(e=>e.scrollTop),original);
  assert.equal(await page.evaluate(()=>typeof window.__novaHistoryScrollTrial),'undefined');checks++;
  await setup();await page.evaluate(()=>document.getElementById('history').style.scrollBehavior='smooth');await step('start');const smooth=await finish();assert(smooth.done,JSON.stringify(smooth));assert.equal(smooth.messages.length,40);checks++;
  // Even a stable short window and observed edges must never claim full history.
  await setup(7);await step('start');const short=await finish();assert(short.done);assert.equal(short.coverage.history,'not-proven');assert.equal(short.coverage.text,'not-proven');checks++;
  // Cancellation releases cached bodies, observers and the session and restores position.
  await setup();await collectFirst();await step('cancel');assert.equal(await page.evaluate(()=>typeof window.__novaHistoryScrollTrial),'undefined');checks++;
  await setup();await page.evaluate(()=>document.querySelector('[data-message-id]').removeAttribute('data-message-id'));
  assert.equal((await step('start')).error,'H02_MISSING_ID');checks++;
  await setup(7);await collectFirst();await page.evaluate(()=>document.querySelector('.markdown p').textContent='EDITED');
  assert.equal((await finish()).error,'H04_CHANGED');checks++;
  await setup(7);await collectFirst();await page.evaluate(()=>{const space=document.getElementById('space');[...space.children].reverse().forEach(e=>space.append(e));});
  assert.equal((await finish()).error,'H03_ORDER');checks++;
  await setup();await step('start');await page.evaluate(async()=>{history.replaceState({},'','/c/other');document.body.dataset.changed='yes';await new Promise(r=>setTimeout(r,0));history.replaceState({},'','/g/g-p-fixture/c/fixture');});
  assert.equal((await step()).error,'H04_CHANGED');checks++;
  await setup();await page.evaluate(()=>document.body.insertAdjacentHTML('beforeend','<button data-testid="stop-button">stop</button>'));
  assert.equal((await step('start')).error,'D04_STREAMING');checks++;
  await setup();await step('start');await page.evaluate(()=>window.__novaHistoryScrollTrial.started-=120001);
  assert.equal((await step()).error,'H05_LIMIT');checks++;
  await setup();await step('start');await page.evaluate(()=>{window.__novaHistoryScrollTrial.stepStarted-=6000;document.querySelector('.markdown p').textContent='not settled';});
  assert.equal((await step()).error,'H06_UNSETTLED');checks++;
  await setup(7);await step('start');for(let i=0;i<200;i++){await step();if(await page.evaluate(()=>window.__novaHistoryScrollTrial?.leg===2))break;}
  await page.evaluate(()=>document.querySelector('[data-message-id]').dataset.messageId='second-pass-new');
  assert.equal((await finish()).error,'H04_SECOND_PASS');checks++;
  // No overlap between observed windows: never sort by IDs or first-seen order.
  await setup(40);await page.evaluate(()=>{const p=document.getElementById('history');p.addEventListener('scroll',()=>{const ids=[...document.querySelectorAll('[data-message-id]')];if(p.scrollTop<1200) ids.forEach((e,i)=>{e.dataset.messageId='extra-'+i;e.querySelector('.markdown').innerHTML='<p>DISCONNECTED-'+i+'</p>';});});});
  await step('start');const disconnected=await finish();assert(['H03_ORDER','H04_SECOND_PASS','H04_CHANGED'].includes(disconnected.error),JSON.stringify(disconnected));checks++;
  await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');await page.evaluate(()=>{document.body.style.overflow='hidden';document.body.innerHTML='<main style="height:20px;overflow:hidden"><article data-message-id="x"><div data-message-author-role="user" style="height:2000px">OFFSCREEN</div></article></main>';});
  assert.equal((await step('start')).error,'H01_SCROLL_CONTAINER');checks++;
  console.log(`PASS: ${checks} scroll history browser scenarios; virtualized 40-message fixture only, no real-history proof`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
