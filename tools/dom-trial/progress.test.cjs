const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const source=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8');
const discovery=fs.readFileSync('app/src/main/assets/export/progress-discovery.js','utf8');
assert.equal(discovery,fs.readFileSync('tools/feasibility/progress-coverage/probe.js','utf8'));
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});let checks=0;
 try {
  const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<main></main>'}));
  await page.goto('https://chatgpt.com/c/PRIVATE-ROUTE');
  const setup=html=>page.evaluate(html=>document.querySelector('main').innerHTML=html,html);
  const read=async()=>{
   const before=await page.content();
   const expression=`(function(){const r=JSON.parse(${source.replace('__NOVA_VERIFY_ONLY__','false')});r.diagnostic.progressDiscovery=JSON.parse(${discovery});return JSON.stringify(r);})()`;
   const r=JSON.parse(await page.evaluate(expression));assert.equal(await page.content(),before);
   for(const secret of ['PRIVATE-ROUTE','PRIVATE-ID','SECRET-BODY','SECRET-CONTROL'])assert(!JSON.stringify(r.diagnostic).includes(secret));
   return r;
  };
  const ordinary=(id,role,text)=>`<article data-message-id="${id}"><div data-message-author-role="${role}"><div class="markdown"><p>${text}</p></div><button>SECRET-CONTROL</button></div></article>`;
  const progress='<article data-turn="assistant" data-message-channel="commentary" data-message-id="PRIVATE-ID"><p>SECRET-BODY progress</p><button>SECRET-CONTROL</button><p hidden>SECRET-HIDDEN</p></article>';
  // Fabricated 30-message baseline: a missing-author progress turn amid 29 ordinary messages.
  const rows=Array.from({length:29},(_,i)=>ordinary('m'+i,i%2?'assistant':'user','SECRET-BODY '+i));rows.splice(11,0,progress);
  await setup(rows.join(''));let r=await read();assert(!r.error);assert.equal(r.messages.length,30);assert.equal(r.messages[11].messageType,'assistant-progress');
  assert.equal(r.messages[11].id,'PRIVATE-ID');assert(r.messages[11].markdown.includes('progress'));assert.equal(r.diagnostic.turnFallbacks,1);
  assert(!JSON.stringify(r.messages).includes('SECRET-HIDDEN'));assert(!JSON.stringify(r.messages).includes('SECRET-CONTROL'));checks++;
  const signature=r.diagnostic.signature;await page.evaluate(()=>document.querySelector('[data-turn]').dataset.messageChannel='final');
  assert.notEqual(JSON.parse(await page.evaluate(source.replace('__NOVA_VERIFY_ONLY__','true'))).diagnostic.signature,signature);checks++;
  await setup(progress.replace('data-turn="assistant"','data-testid="conversation-turn-12"')+ordinary('a','assistant','SECRET-BODY final'));
  r=await read();assert.equal(r.messages.length,1);assert.equal(r.diagnostic.progressDiscovery.turns.visibleWithoutSupportedAuthor,1);checks++;
  await setup(progress.replace('data-turn="assistant"','data-turn="assistant" hidden')+ordinary('a','assistant','SECRET-BODY final'));
  r=await read();assert.equal(r.messages.length,1);checks++;
  await setup(progress.replace('<p>SECRET-BODY progress</p>','<div data-message-author-role="tool">SECRET-BODY tool</div>')+ordinary('a','assistant','SECRET-BODY final'));
  r=await read();assert.equal(r.messages.length,1);assert.equal(r.diagnostic.turnFallbacks,0);checks++;
  await setup(progress.replace('<p>SECRET-BODY progress</p>','<div data-message-author-role="assistant"><p>SECRET-BODY progress</p></div>'));
  r=await read();assert.equal(r.messages.length,1);assert.equal(r.diagnostic.turnFallbacks,0);assert.equal(r.messages[0].messageType,'assistant-progress');checks++;
  await setup(ordinary('a','assistant','SECRET-BODY final').replace('<div class="markdown">','<p data-message-channel="commentary">SECRET-BODY progress</p><div class="markdown">'));
  r=await read();assert.equal(r.messages.length,1);assert(r.messages[0].markdown.includes('progress'));assert(r.messages[0].markdown.indexOf('progress')<r.messages[0].markdown.indexOf('final'));assert.equal(r.diagnostic.explicitProgressBlocks,1);checks++;
  await setup(ordinary('a','assistant','SECRET-BODY repeated')+ordinary('b','assistant','SECRET-BODY repeated'));
  r=await read();assert.equal(r.messages.length,2);assert.equal(r.messages[0].markdown,r.messages[1].markdown);checks++;
  await setup(progress+ordinary('PRIVATE-ID','assistant','SECRET-BODY final'));
  r=await read();assert.equal(r.error,'D08_DUPLICATE_ID');assert(!r.messages);checks++;
  await setup(progress.replace(' data-message-id="PRIVATE-ID"',''));
  r=await read();assert.equal(r.diagnostic.missingIds,1);assert.equal(r.messages[0].id,'');checks++;
  await setup(progress.replace('<p>SECRET-BODY progress</p>','<article data-turn="assistant"><p>SECRET-BODY progress</p></article>'));
  r=await read();assert.equal(r.messages.length,1);checks++;
  console.log(`PASS ${checks} bounded progress-capture scenarios; synthetic markup, target message still unverified`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
