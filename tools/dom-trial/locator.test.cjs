const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const snapshot=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8').replace('__NOVA_VERIFY_ONLY__','false');
const source=fs.readFileSync('app/src/main/assets/export/locate-text.js','utf8').replace('__NOVA_SNAPSHOT__',()=>snapshot);
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});let checks=0;
 try {
  const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<main></main>'}));await page.goto('https://chatgpt.com/c/PRIVATE-ROUTE');
  const setup=html=>page.evaluate(html=>document.querySelector('main').innerHTML=html,html);
  const read=async(query='SECRET-PHRASE')=>{
   const before=await page.content(),position=await page.evaluate(()=>scrollY);
   const r=JSON.parse(await page.evaluate(source.replace('__NOVA_QUERY__',()=>JSON.stringify(query))));
   assert.equal(await page.content(),before);assert.equal(await page.evaluate(()=>scrollY),position);
   for(const secret of ['SECRET-PHRASE','PRIVATE-ID','PRIVATE-ROUTE','PRIVATE-LABEL','PRIVATE-TITLE'])assert(!JSON.stringify(r).includes(secret));return r;
  };
  const final='<div data-message-author-role="assistant" data-message-id="PRIVATE-ID"><div class="markdown"><p>SECRET-PHRASE</p></div></div>';
  await setup(final);let r=await read();assert.equal(r.code,'L00_MATCH');assert.equal(r.matches[0].authorIndex,1);assert(r.matches[0].authorSelected);assert(r.matches[0].insideMarkdown);assert(r.matches[0].capturedHere);assert.deepEqual(r.capture.matchingMessageIndexes,[1]);checks++;
  await setup('<section><p>SECRET-PHRASE</p>'+final.replace('SECRET-PHRASE','ordinary final')+'</section>');r=await read();assert.equal(r.matches[0].authorIndex,0);assert.equal(r.matches[0].chain[1].tag,'SECTION');assert(!r.matches[0].capturedHere);assert.deepEqual(r.capture.matchingMessageIndexes,[]);checks++;
  await setup(final.replace('<div class="markdown">','<p>SECRET-PHRASE</p><div class="markdown">').replace('<p>SECRET-PHRASE</p></div>','<p>ordinary final</p></div>'));
  r=await read();assert(r.matches[0].authorSelected);assert(!r.matches[0].insideMarkdown);assert.equal(r.matches[0].markdownRoots,1);checks++;
  await setup('<p aria-hidden="true">SECRET-PHRASE</p>');r=await read();assert(r.matches[0].visibility.ariaHidden);assert(!r.matches[0].visibility.cssHidden);checks++;
  await setup('<section style="display:none"><p>SECRET-PHRASE</p></section>');r=await read();assert(r.matches[0].visibility.cssHidden);checks++;
  await setup('<div data-message-author-role="assistant" aria-hidden="true"><p>SECRET-PHRASE</p></div>');r=await read();assert.equal(r.matches[0].authorRole,'assistant');assert(!r.matches[0].authorSelected);checks++;
  await setup('<p><span>SECRET-</span><strong>PHRASE</strong></p>');r=await read();assert.equal(r.matches.length,1);checks++;
  await setup('<p><span>SECRET </span><strong> PHRASE</strong></p>');r=await read('SECRET PHRASE');assert.equal(r.matches.length,1);checks++;
  await setup('<p>SECRET-</p><p>PHRASE</p>');r=await read();assert.equal(r.code,'L01_NOT_FOUND');checks++;
  await setup('<button>SECRET-PHRASE</button><script>"SECRET-PHRASE"</script><input value="SECRET-PHRASE"><textarea>SECRET-PHRASE</textarea>');r=await read();assert.equal(r.code,'L01_NOT_FOUND');checks++;
  await setup('<p>different</p>');r=await read();assert.equal(r.code,'L01_NOT_FOUND');checks++;
  await setup(Array.from({length:21},()=>'<p>SECRET-PHRASE</p>').join(''));r=await read();assert.equal(r.code,'L03_LIMIT');assert(r.truncated);checks++;
  await setup('<p>'+ 'x'.repeat(2000001)+'</p>');r=await read();assert.equal(r.code,'L03_LIMIT');assert(r.truncated);checks++;
  await setup('<p>ordinary</p>');r=await read('x');assert.equal(r.code,'L02_QUERY');checks++;
  const injection='";window.PRIVATE_PWNED=true;//';await setup('<p></p>');await page.evaluate(t=>document.querySelector('p').textContent=t,injection);
  r=await read(injection);assert.equal(r.code,'L00_MATCH');assert.equal(await page.evaluate(()=>typeof window.PRIVATE_PWNED),'undefined');checks++;
  await setup('<section data-turn="assistant" data-testid="conversation-turn-3" data-message-id="PRIVATE-ID"><div class="markdown"><p>SECRET-PHRASE</p></div></section>');
  r=await read();assert.equal(r.matches[0].authorIndex,0);assert(r.matches[0].capturedHere);assert.deepEqual(r.capture.matchingMessageIndexes,[1]);checks++;
  await setup('<section data-turn="assistant" data-testid="conversation-turn-3"><div class="markdown"><p>SECRET-PHRASE</p></div></section>');
  r=await read();assert(!r.matches[0].exportSourceHasId);assert.deepEqual(r.capture.matchingMessageIndexes,[1]);checks++;
  await page.goto('https://example.org/c/private');r=await read();assert.equal(r.code,'L02_ORIGIN');checks++;
  console.log(`PASS ${checks} read-only locator scenarios; private query never returned, synthetic only`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
