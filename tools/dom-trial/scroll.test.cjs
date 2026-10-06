const assert=require('node:assert/strict'),fs=require('node:fs');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const snapshot=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8').replace('__NOVA_VERIFY_ONLY__','false');
const source=fs.readFileSync(process.env.NOVA_SCROLL_SOURCE_PATH || 'app/src/main/assets/export/scroll-trial.js','utf8').replace('__NOVA_SNAPSHOT__',()=>snapshot);
const fixture=fs.readFileSync('tools/dom-trial/virtual-fixture.js','utf8');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});let checks=0;
 try {
  const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<!doctype html><main></main>'}));
  const setup=async(n=40)=>{await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');await page.evaluate(fixture.replace('__NOVA_FIXTURE_COUNT__',String(n)));};
  const step=async(command='poll',token='fixture-token')=>JSON.parse(await page.evaluate(source.replace('__NOVA_SCROLL_COMMAND__',JSON.stringify(command)).replace('__NOVA_SCROLL_TOKEN__',JSON.stringify(token))));
  const finish=async()=>{let result;for(let i=0;i<500;i++){result=await step();if(result.error || result.done) return result;await page.waitForTimeout(20);}throw Error('fixture poll limit');};
  const collectFirst=async()=>{let r=await step('start');for(let i=0;i<50 && !(r.coverage?.count>0);i++){await page.waitForTimeout(20);r=await step();}assert(r.coverage?.count>0,JSON.stringify(r));await page.waitForTimeout(60);};
  // Same messages, only the scroll extent jitters: content can be cached safely.
  await setup(7);await step('start');
  await page.evaluate(()=>document.getElementById('space').style.height='450px');await step();
  await page.evaluate(()=>{document.getElementById('space').style.height='448px';window.__novaHistoryScrollTrial.stepStarted-=6000;});
  const geometryOnly=await step();assert(!geometryOnly.error,JSON.stringify(geometryOnly));assert.equal(geometryOnly.coverage.count,7);
  assert.equal(geometryOnly.coverage.settling.totalExtentChanges,2);await step('cancel');checks++;
  if(process.env.NOVA_SETTLE_REGRESSION_ONLY) {console.log('PASS: layout-only settling regression; synthetic only');return;}
  await setup();const original=await page.locator('#history').evaluate(e=>e.scrollTop);await step('start');
  const all=await finish();assert(all.done,JSON.stringify(all));assert.equal(all.messages.length,40);assert.deepEqual(all.messages.map(m=>m.id),Array.from({length:40},(_,i)=>'m'+i));
  assert.equal(all.messages.filter(m=>m.markdown.includes('REPEATED-TEXT')).length,2);
  assert(all.coverage.topObserved && all.coverage.bottomObserved && !all.coverage.secondPass);assert.equal(all.coverage.history,'not-proven');
  assert.equal(await page.locator('[data-message-author-role]').count(),7);assert.equal(await page.locator('#history').evaluate(e=>e.scrollTop),original);
  assert.equal(await page.evaluate(()=>typeof window.__novaHistoryScrollTrial),'undefined');checks++;
  await setup();await page.evaluate(()=>document.getElementById('history').style.scrollBehavior='smooth');await step('start');const smooth=await finish();assert(smooth.done,JSON.stringify(smooth));assert.equal(smooth.messages.length,40);checks++;
  // Even a stable short window and observed edges must never claim full history.
  await setup(7);await step('start');const short=await finish();assert(short.done);assert.equal(short.coverage.history,'not-proven');assert.equal(short.coverage.text,'not-proven');checks++;
  await setup(7);await page.evaluate(()=>{
    const e=document.querySelector('[data-message-id="m3"]');e.dataset.turn='assistant';e.dataset.messageChannel='commentary';
    e.querySelector('[data-message-author-role]').removeAttribute('data-message-author-role');
    e.insertAdjacentHTML('beforeend','<span class="animate-spin" style="display:block;width:10px;height:10px"></span>');
  });await step('start');const progressTurn=await finish();assert(progressTurn.done,JSON.stringify(progressTurn));
  assert.equal(progressTurn.messages.length,7);assert.deepEqual(progressTurn.messages.map(m=>m.id),Array.from({length:7},(_,i)=>'m'+i));
  assert.equal(progressTurn.messages[3].messageType,'assistant-progress');assert(progressTurn.messages[3].markdown.includes('ROW-3'));checks++;
  await setup(7);await page.evaluate(()=>{
    const e=document.querySelector('[data-message-id="m3"]'), section=document.createElement('section');
    section.dataset.turn='assistant';section.dataset.testid='conversation-turn-3';section.dataset.messageId=e.dataset.messageId;
    section.innerHTML='<div class="markdown"><p>SECTION-PROGRESS</p><span class="animate-spin" style="display:block;width:10px;height:10px"></span></div>';e.replaceWith(section);
  });await step('start');const sectionProgress=await finish();assert(sectionProgress.done,JSON.stringify(sectionProgress));assert.equal(sectionProgress.messages.length,7);assert(sectionProgress.messages[3].markdown.includes('SECTION-PROGRESS'));checks++;
  await setup(7);await page.evaluate(()=>{
    const e=document.querySelector('[data-message-id="m3"]');e.outerHTML='<section data-turn="assistant" data-testid="conversation-turn-3"><div class="markdown"><p>NO-ID-PROGRESS</p></div></section>';
  });const noSectionStart=await step('start');const noSectionId=noSectionStart.error ? noSectionStart : await finish();assert.equal(noSectionId.error,'H02_MISSING_ID');assert(!noSectionId.messages);checks++;
  // Use the same pixel tolerance at the endpoint and while comparing edge geometry.
  await setup(7);await step('start');let jitterEnd;for(let i=0;i<100;i++) {
   await page.evaluate(i=>document.getElementById('space').style.height=(448+(i%2)*2)+'px',i);
   jitterEnd=await step();if(jitterEnd.error || jitterEnd.done) break;await page.waitForTimeout(20);
  }
  assert(jitterEnd.done,JSON.stringify(jitterEnd));assert.equal(jitterEnd.messages.length,7);checks++;
  // A history spinner must block caching/scrolling even when mounted text looks stable.
  // Real delayed completion beyond the old 5s window, then all 40 cached and checked.
  await setup();const slowOriginal=await page.locator('#history').evaluate(e=>e.scrollTop);
  await page.evaluate(()=>{const spinner=document.createElement('div');spinner.id='loader';spinner.setAttribute('role','progressbar');spinner.style='position:sticky;bottom:0;height:10px;width:20px';document.getElementById('history').append(spinner);setTimeout(()=>spinner.remove(),6500);});
  await step('start');await page.waitForTimeout(5500);const slowWait=await step();
  assert(slowWait.waiting,JSON.stringify(slowWait));assert.equal(slowWait.coverage.settling.reason,'history-loading');assert.equal(slowWait.coverage.count,0);assert.equal(slowWait.coverage.steps,0);assert(slowWait.coverage.settling.loadingObserved);
  await page.waitForTimeout(1100);const slowDone=await finish();assert(slowDone.done,JSON.stringify(slowDone));assert.equal(slowDone.messages.length,40);assert.equal(slowDone.coverage.history,'not-proven');assert.equal(await page.locator('#history').evaluate(e=>e.scrollTop),slowOriginal);checks++;
  // A fresh list grants a new readiness grace even without a recognizable spinner.
  await setup(7);await step('start');await page.evaluate(()=>{window.__novaHistoryScrollTrial.stepStarted-=6000;window.__novaHistoryScrollTrial.readyStarted-=6000;document.querySelector('[data-message-id]').dataset.messageId='new-window-id';});
  const listGrace=await step();assert(listGrace.waiting,JSON.stringify(listGrace));assert(listGrace.coverage.settling.readyAgeMs<1000);await step('cancel');checks++;
  // Repeated remounting cannot reset the absolute window deadline indefinitely.
  await setup(7);await step('start');await page.evaluate(()=>{window.__novaHistoryScrollTrial.stepStarted-=30001;document.querySelector('[data-message-id]').dataset.messageId='SECRET-REMOUNT';});
  const endlessList=await step();assert.equal(endlessList.error,'H06_UNSETTLED');assert(!JSON.stringify(endlessList).includes('SECRET-REMOUNT'));assert.equal(await page.evaluate(()=>typeof window.__novaHistoryScrollTrial),'undefined');checks++;
  await setup();await page.evaluate(()=>{document.getElementById('history').setAttribute('aria-busy','true');});await step('start');await page.evaluate(()=>window.__novaHistoryScrollTrial.stepStarted-=30001);
  const endlessLoader=await step();assert.equal(endlessLoader.error,'H06_UNSETTLED');assert.equal(endlessLoader.coverage.settling.reason,'history-loading');assert.equal(endlessLoader.coverage.count,0);checks++;
  // Loader cancellation immediately releases memory and restores the original position.
  await setup();await page.evaluate(()=>document.getElementById('history').setAttribute('aria-busy','true'));await step('start');await step('cancel');assert.equal(await page.evaluate(()=>typeof window.__novaHistoryScrollTrial),'undefined');checks++;
  // Hidden/outside-scroller indicators and decorative message spinners are ignored.
  await setup(7);await page.evaluate(()=>{document.body.insertAdjacentHTML('beforeend','<div role="progressbar">elsewhere</div>');document.getElementById('history').insertAdjacentHTML('beforeend','<div role="progressbar" hidden>hidden</div>');document.querySelector('.markdown').insertAdjacentHTML('beforeend','<span class="animate-spin">decoration</span>');});await step('start');assert((await finish()).done);checks++;
  // A single downwards traversal from the top retains all 40 ordered bodies.
  await setup();await page.locator('#history').evaluate(e=>e.scrollTo({top:0,behavior:'instant'}));await page.waitForTimeout(50);await step('start');const down=await finish();assert(down.done,JSON.stringify(down));assert.deepEqual(down.messages.map(m=>m.id),Array.from({length:40},(_,i)=>'m'+i));assert.equal(down.coverage.leg,1);assert.equal(down.coverage.plannedLegs,1);assert.equal(down.coverage.direction,'down');assert.equal(down.coverage.secondPass,false);assert.equal(down.coverage.history,'not-proven');assert.equal(await page.locator('#history').evaluate(e=>e.scrollTop),0);checks++;
  await setup();await page.locator('#history').evaluate(e=>e.scrollTo({top:640,behavior:'instant'}));await page.waitForTimeout(50);await step('start');const middle=await finish();assert(middle.done,JSON.stringify(middle));assert.equal(middle.messages.length,40);assert.equal(middle.coverage.direction,'up');assert.equal(middle.coverage.leg,1);assert.equal(await page.locator('#history').evaluate(e=>e.scrollTop),640);checks++;
  // Narrow virtual windows force backtracking instead of skipping unconnected rows.
  await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');await page.evaluate(fixture.replace('__NOVA_FIXTURE_COUNT__','40,3'));await step('start');const narrow=await finish();assert(narrow.done,JSON.stringify(narrow));assert.equal(narrow.messages.length,40);assert(narrow.coverage.overlapRetries>0);assert.equal(narrow.coverage.history,'not-proven');checks++;
  // A never-mounted message is still unknowable without an independent baseline.
  await setup();await page.evaluate(()=>{const omit=()=>document.querySelector('[data-message-id="m17"]')?.remove();omit();document.getElementById('history').addEventListener('scroll',omit);});await step('start');const omitted=await finish();assert(omitted.done,JSON.stringify(omitted));assert.equal(omitted.messages.length,39);assert(!omitted.messages.some(m=>m.id==='m17'));assert.equal(omitted.coverage.history,'not-proven');assert.equal(omitted.coverage.text,'not-proven');checks++;
  // Initial endpoint layout cannot delay the window deadline indefinitely.
  await setup(7);await step('start');await step();await page.evaluate(()=>{document.getElementById('space').style.height='2000px';window.__novaHistoryScrollTrial.stepStarted-=30001;});
  const initialLayout=await step();assert.equal(initialLayout.error,'H06_UNSETTLED');assert.equal(initialLayout.coverage.settling.reason,'start-layout-changing');checks++;
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
  await setup();await step('start');await page.evaluate(()=>window.__novaHistoryScrollTrial.steps=1200);assert.equal((await step()).error,'H05_LIMIT');checks++;
  await setup();await step('start');await page.evaluate(()=>window.__novaHistoryScrollTrial.started-=600001);
  assert.equal((await step()).error,'H05_LIMIT');checks++;
  await setup();await step('start');await page.evaluate(()=>{window.__novaHistoryScrollTrial.stepStarted-=6000;window.__novaHistoryScrollTrial.readyStarted-=6000;document.querySelector('.markdown p').textContent='not settled';});
  const unsettledBody=await step();assert.equal(unsettledBody.error,'H06_UNSETTLED');assert.equal(unsettledBody.coverage.settling.reason,'body-or-structure-changing');assert.equal(unsettledBody.coverage.settling.bodyChanges,1);assert(!JSON.stringify(unsettledBody).includes('not settled'));checks++;
  await setup(7);await step('start');await page.evaluate(()=>{window.__novaHistoryScrollTrial.stepStarted-=6000;document.querySelector('[data-message-id]').dataset.messageId='changed-private-id';});
  const changingList=await step();assert(changingList.waiting,JSON.stringify(changingList));assert.equal(changingList.coverage.settling.reason,'message-list-changing');assert(!JSON.stringify(changingList).includes('changed-private-id'));await step('cancel');checks++;
  // Layout jitter at an observed edge still cannot approve traversal completion.
  await setup(7);await collectFirst();await step();await step();await step();await page.evaluate(()=>{document.getElementById('history').scrollTo({top:0,behavior:'instant'});window.__novaHistoryScrollTrial.stepStarted-=30001;document.getElementById('space').style.height='2000px';});
  const unsettledEdge=await step();assert.equal(unsettledEdge.error,'H06_UNSETTLED');assert.equal(unsettledEdge.coverage.settling.reason,'edge-layout-changing');checks++;
  // No overlap between observed windows: never sort by IDs or first-seen order.
  await setup(40);await page.evaluate(()=>{const p=document.getElementById('history');p.addEventListener('scroll',()=>{const ids=[...document.querySelectorAll('[data-message-id]')];if(p.scrollTop<1200) ids.forEach((e,i)=>{e.dataset.messageId='extra-'+i;e.querySelector('.markdown').innerHTML='<p>DISCONNECTED-'+i+'</p>';});});});
  await step('start');const disconnected=await finish();assert(['H03_ORDER','H04_SECOND_PASS','H04_CHANGED'].includes(disconnected.error),JSON.stringify(disconnected));checks++;
  await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');await page.evaluate(()=>{document.body.style.overflow='hidden';document.body.innerHTML='<main style="height:20px;overflow:hidden"><article data-message-id="x"><div data-message-author-role="user" style="height:2000px">OFFSCREEN</div></article></main>';});
  assert.equal((await step('start')).error,'H01_SCROLL_CONTAINER');checks++;
  console.log(`PASS: ${checks} scroll history browser scenarios; virtualized 40-message fixture only, no real-history proof`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
