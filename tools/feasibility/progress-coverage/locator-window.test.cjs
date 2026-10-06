const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const snapshot=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8').replace('__NOVA_VERIFY_ONLY__','false');
const source=fs.readFileSync('app/src/main/assets/export/locate-text.js','utf8').replace('__NOVA_SNAPSHOT__',()=>snapshot);
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});let checks=0;
 try {
  const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<main></main>'}));await page.goto('https://chatgpt.com/c/PRIVATE-ROUTE');
  const normal=i=>`<div data-message-author-role="${i%2?'assistant':'user'}" data-message-id="PRIVATE-ID-${i}"><p>ordinary message ${i}</p></div>`;
  const target='<p>TRACE-PROGRESS-TARGET</p>';
  const setup=html=>page.evaluate(html=>document.querySelector('main').innerHTML=html,html);
  const read=async(query='TRACE-PROGRESS-TARGET')=>{
   const before=await page.content();const r=JSON.parse(await page.evaluate(source.replace('__NOVA_QUERY__',()=>JSON.stringify(query))));
   assert.equal(await page.content(),before);
   for(const secret of ['TRACE-PROGRESS-TARGET','PRIVATE-ID','PRIVATE-ROUTE'])assert(!JSON.stringify(r).includes(secret));return r;
  };
  await setup(Array.from({length:31},(_,i)=>normal(i)+(i===10?target:'')).join(''));let r=await read();
  assert.equal(r.authorNodes,31);assert.equal(r.code,'L00_MATCH');assert.equal(r.matches[0].authorIndex,0);assert.deepEqual(r.capture.matchingMessageIndexes,[]);checks++;
  // A smaller mounted window loses the target without changing any collector rule.
  await setup(Array.from({length:6},(_,i)=>normal(i+25)).join(''));r=await read();
  assert.equal(r.authorNodes,6);assert.equal(r.code,'L01_NOT_FOUND');assert(!r.truncated);checks++;
  await setup(Array.from({length:6},(_,i)=>normal(i+8)+(i===2?target:'')).join(''));r=await read();
  assert.equal(r.authorNodes,6);assert.equal(r.code,'L00_MATCH');assert.equal(r.matches[0].authorIndex,0);checks++;
  // The same six-author, not-found result can arise with the target visibly loaded.
  r=await read('TRACE-PROGRESS-TARGXT');assert.equal(r.authorNodes,6);assert.equal(r.code,'L01_NOT_FOUND');checks++;
  await setup(Array.from({length:6},(_,i)=>normal(i+8)).join('')+'<button>TRACE-PROGRESS-TARGET</button>');r=await read();
  assert.equal(r.authorNodes,6);assert.equal(r.code,'L01_NOT_FOUND');assert(await page.locator('button').isVisible());checks++;
  console.log(`PASS ${checks} locator-window ambiguity checks; synthetic, target device cause unknown`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
