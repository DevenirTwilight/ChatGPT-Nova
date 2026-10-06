const assert=require('node:assert/strict');
const fs=require('node:fs');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const source=fs.readFileSync('app/src/main/assets/export/dom-trial.js','utf8');
(async()=>{
  const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE || '/usr/bin/chromium',args:['--no-sandbox']});
  let checks=0;
  try {
    const page=await browser.newPage();
    await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:`<!doctype html><title>测试标题 &lt;x&gt;</title><main>
      <article data-message-id="u"><div data-message-author-role="user"><div class="whitespace-pre-wrap">中文😀\nUSER-FIRST <span hidden>SECRET-HIDDEN</span></div><button>Copy user</button></div></article>
      <article data-message-id="a"><div data-message-author-role="assistant"><div class="markdown"><p>ASSISTANT-LAST <a href="https://example.org/ref?a=1&amp;b=2">source</a></p><pre><button>Copy code</button><code class="language-java">a &lt; b;\n\u0060\u0060\u0060CODE-LAST</code></pre><table><tr><th>first</th><th>second</th></tr><tr><td>A|B</td><td>TABLE-LAST</td></tr></table><span class="katex"><span>duplicated rendered math</span><annotation encoding="application/x-tex" style="display:none">E=mc^2</annotation></span><img src="https://example.org/img.png" alt="图片"><details><summary>Closed</summary>HIDDEN-FOLD</details><a href="javascript:alert(1)" onclick="alert(2)">danger</a><a href="https://user:password@example.org/">credential-link</a><iframe src="https://example.org/private"></iframe></div><div class="markdown"><p>SECOND-BLOCK-LAST</p></div><button>Copy assistant</button></div></article>
    </main>`}));
    await page.goto('https://chatgpt.com/g/g-p-fixture/c/fixture');
    const read=async(verify=false)=>JSON.parse(await page.evaluate(source.replace('__NOVA_VERIFY_ONLY__',String(verify))));
    const before=await page.content();
    await page.evaluate(()=>{window.fetch=()=>{throw Error('no network');};Object.defineProperty(document.querySelector('[data-message-author-role]'),'__reactFiber$fixture',{get(){throw Error('no Fiber');}});});
    const data=await read();
    assert(!data.error);assert.equal(data.messages.length,2);assert.equal(data.diagnostic.history,'not-proven');
    const html=data.messages.map(m=>m.html).join(''),md=data.messages.map(m=>m.markdown).join('\n');
    assert(html.includes('USER-FIRST'));assert(html.includes('ASSISTANT-LAST'));assert(html.includes('SECOND-BLOCK-LAST'));assert(md.includes('CODE-LAST'));assert(md.includes('TABLE-LAST'));
    assert(md.includes('````java'));assert(md.includes('E=mc^2'));assert(md.includes('A\\|B'));
    assert(!html.includes('onclick'));assert(!html.includes('javascript:'));assert(!html.includes('<img'));assert(!html.includes('<iframe'));
    for(const hidden of ['SECRET-HIDDEN','HIDDEN-FOLD','Copy code','Copy assistant','duplicated rendered math','user:password']) assert(!html.includes(hidden));
    assert.equal(await page.content(),before);checks++;
    const verify=await read(true);assert.equal(verify.diagnostic.signature,data.diagnostic.signature);assert(!verify.messages);checks++;
    await page.evaluate(()=>document.querySelector('.markdown p').textContent+=' change');
    assert.notEqual((await read(true)).diagnostic.signature,data.diagnostic.signature);checks++;
    await page.evaluate(()=>{let b=document.createElement('button');b.dataset.testid='stop-button';document.body.append(b);});
    assert.equal((await read()).error,'D04_STREAMING');checks++;
    await page.evaluate(()=>{document.querySelector('[data-testid="stop-button"]').remove();document.querySelector('[data-message-id="a"]').dataset.messageId='u';});
    assert.equal((await read()).error,'D08_DUPLICATE_ID');checks++;
    await page.evaluate(()=>{document.querySelector('main').innerHTML=Array.from({length:1001},(_,i)=>`<p data-message-author-role="user" data-message-id="${i}">x</p>`).join('');});
    assert.equal((await read()).error,'D06_LIMIT');checks++;
    await page.evaluate(()=>{document.querySelector('main').innerHTML='<p data-message-author-role="user">'+ 'x'.repeat(2000001) +'</p>';});
    assert.equal((await read()).error,'D06_LIMIT');checks++;
    await page.evaluate(()=>{document.querySelector('main').innerHTML=Array.from({length:400},(_,i)=>`<p data-message-author-role="${i%2?'assistant':'user'}" data-message-id="m${i}">BODY-${i} 中文</p>`).join('');});
    const long=await read();assert.equal(long.messages.length,400);assert(long.messages[399].markdown.includes('BODY-399'));assert.equal(long.diagnostic.history,'not-proven');checks++;
    await page.goto('https://chatgpt.com/');assert.equal((await read()).error,'D02_ROUTE');checks++;
    await page.goto('https://example.org/c/fixture');assert.equal((await read()).error,'D01_ORIGIN');checks++;
    console.log(`PASS: ${checks} DOM trial browser scenarios; synthetic only, no Android/account proof`);
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
