const fs=require('fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE||'playwright-core');
const probe=fs.readFileSync('tools/feasibility/progress-coverage/probe-v2.js','utf8');
const capture=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8').replace('__NOVA_VERIFY_ONLY__','false');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE||'/usr/bin/chromium',args:['--no-sandbox']});let checks=0;
 try {
  const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<main></main>'}));
  const setup=async html=>{await page.goto('https://chatgpt.com/c/PRIVATE-ROUTE');await page.evaluate(html=>document.querySelector('main').innerHTML=html,html);};
  const read=async()=>{const before=await page.content();const r=JSON.parse(await page.evaluate(probe));assert.equal(await page.content(),before);for(const secret of ['SECRET-TEXT','PRIVATE-ID','PRIVATE-ROUTE','PRIVATE-LABEL'])assert(!JSON.stringify(r).includes(secret));return r;};
  const final='<article data-testid="conversation-turn-1" data-turn="assistant" data-message-id="PRIVATE-ID"><div data-message-author-role="assistant"><div class="markdown"><p>SECRET-TEXT final</p></div><button>PRIVATE-LABEL</button></div></article>';
  await setup(final);let r=await read();assert.equal(r.source,'read-only-progress-discovery-v2');assert.equal(r.authors.visibleSupported,1);assert.equal(r.authors.outsideMarkdownChars,0);checks++;
  // Progress may be a sibling of the author rather than inside its Markdown.
  await setup(final.replace('<div data-message-author-role="assistant">','<p>SECRET-TEXT sibling progress</p><div data-message-author-role="assistant">'));
  r=await read();assert.equal(r.turns.visibleWithoutSupportedAuthor,0);assert.equal(r.authors.outsideMarkdownChars,0);
  assert.equal(r.turns.withOutsideAuthorText,1);assert.equal(r.outsideAuthorSamples[0].domIndex,1);assert(r.turns.outsideAuthorChars>0);
  let siblingCapture=JSON.parse(await page.evaluate(capture));assert.equal(siblingCapture.messages.length,1);assert(!siblingCapture.messages[0].markdown.includes('sibling progress'));checks++;
  await setup(final.replace('<div data-message-author-role="assistant">','<p hidden>SECRET-TEXT sibling progress</p><button>PRIVATE-LABEL</button><div data-message-author-role="assistant">'));
  r=await read();assert.equal(r.turns.withOutsideAuthorText,0);checks++;
  await setup('<article data-testid="conversation-turn-1" data-turn="assistant" data-message-author-role="assistant" data-message-id="PRIVATE-ID"><p>SECRET-TEXT root author</p></article>');
  r=await read();assert.equal(r.turns.visibleWithoutSupportedAuthor,0);assert.equal(r.turns.withOutsideAuthorText,0);checks++;
  await setup(final.replaceAll('article','section').replace('<div data-message-author-role="assistant">','<p>SECRET-TEXT sibling progress</p><div data-message-author-role="assistant">'));
  r=await read();assert.equal(r.turns.raw,1);assert.equal(r.turns.withOutsideAuthorText,1);checks++;
  // Same author with ordinary final Markdown: sibling progress is omitted by capture.
  await setup(final.replace('<div class="markdown">','<p>SECRET-TEXT progress</p><div class="markdown">'));r=await read();assert.equal(r.authorSamples.length,1);assert(r.authors.outsideMarkdownChars>0);let exported=JSON.parse(await page.evaluate(capture));assert(!exported.messages[0].markdown.includes('progress'));checks++;
  // An explicitly assistant-labelled turn is now a bounded capture fallback.
  await setup('<article data-testid="conversation-turn-0" data-turn="assistant"><p>SECRET-TEXT progress</p></article>'+final);r=await read();assert.equal(r.turns.visibleWithoutSupportedAuthor,1);assert.equal(r.turnSamples[0].declaredRole,'assistant');exported=JSON.parse(await page.evaluate(capture));assert.equal(exported.messages.length,2);assert.equal(exported.diagnostic.turnFallbacks,1);checks++;
  await setup('<article data-testid="conversation-turn-0" data-turn="assistant"><div data-message-author-role="tool">SECRET-TEXT progress</div></article>'+final);r=await read();assert.equal(r.authors.other,1);assert.equal(r.turns.visibleWithoutSupportedAuthor,1);checks++;
  await setup('<section aria-hidden="true"><div data-message-author-role="assistant">SECRET-TEXT progress</div></section>'+final);r=await read();assert.equal(r.authors.hidden.ariaHidden,1);assert.equal(r.authors.visibleSupported,1);checks++;
  // Conventional assistant markup already includes progress; type itself is not rejected.
  await setup('<article data-message-id="PRIVATE-ID-2"><div data-message-author-role="assistant"><p>SECRET-TEXT progress</p></div></article>'+final);r=await read();assert.equal(r.authors.visibleSupported,2);exported=JSON.parse(await page.evaluate(capture));assert(exported.messages.some(m=>m.markdown.includes('progress')));checks++;
  await setup('<article data-message-id="PRIVATE-ID-2"><div data-message-author-role="assistant"><p>SECRET-TEXT repeated</p></div></article><article data-message-id="PRIVATE-ID-3"><div data-message-author-role="assistant"><p>SECRET-TEXT repeated</p></div></article>'+final);r=await read();assert.equal(r.authors.visibleSupported,3);exported=JSON.parse(await page.evaluate(capture));assert.equal(exported.messages.filter(m=>m.markdown==='SECRET-TEXT repeated').length,2);checks++;
  await setup('<div data-message-author-role="assistant"><p>SECRET-TEXT</p></div>');r=await read();assert.equal(r.authors.missingId,1);checks++;
  await setup('<section>'+Array.from({length:1001},()=>'<div data-message-author-role="assistant">SECRET-TEXT</div>').join('')+'</section>');assert.equal((await read()).error,'Q03_LIMIT');checks++;
  await page.goto('https://example.org/c/private');assert.equal((await read()).error,'Q01_ORIGIN');checks++;
  console.log(`PASS ${checks} read-only progress-discovery scenarios; fabricated markup, real cause not established`);
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
