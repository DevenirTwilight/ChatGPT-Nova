const fs=require('node:fs'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE||'playwright-core');
const source=fs.readFileSync('app/src/main/assets/snapshot/freeze.js','utf8');
const fixture=fs.readFileSync('tools/snapshot/fixture.html','utf8');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE||'/usr/bin/chromium',args:['--no-sandbox']});
 let checks=0;
 try{
 const page=await browser.newPage();await page.route('**/*',r=>r.fulfill({contentType:'text/html',body:fixture}));await page.goto('https://chatgpt.com/c/snapshot-fixture');
 const capture=async(url=page.url())=>JSON.parse(await page.evaluate(source.replace('__NOVA_SNAPSHOT_ID__',JSON.stringify('fixture-snapshot')).replace('__NOVA_EXPECTED_URL__',JSON.stringify(url))));
 await page.evaluate(()=>{window.fetch=()=>{throw Error('no fetch')};Object.defineProperty(window,'localStorage',{get(){throw Error('no storage')}});Object.defineProperty(document,'cookie',{get(){throw Error('no cookie')}});});
 const before=await page.content(),data=await capture();fs.mkdirSync('/tmp/nova-static-html',{recursive:true});fs.writeFileSync('/tmp/nova-static-html/frozen-page.html',data.frozenHtml);assert(!data.error,JSON.stringify(data));assert.equal(await page.content(),before);checks++;
 for(const marker of ['Before snapshot marker','TAIL-SNAPSHOT-MARKER','中文','café','😀','CODE-LAST','TABLE-LAST','E=mc^2','Quoted','Ordered one','Reference','fixture image','LONG-CODE-HEAD','LONG-CODE-TAIL','LONG-TABLE-TAIL'])assert(data.markdown.includes(marker),marker);
 for(const junk of ['BUTTON-GARBAGE','CODE-CONTROL-GARBAGE','MENU-GARBAGE','HIDDEN-CONTROL-GARBAGE','INPUT-GARBAGE','SCRIPT-GARBAGE','CREDENTIAL-DO-NOT-SAVE'])assert(!data.markdown.includes(junk),junk);
 assert(data.markdown.includes('````java'));assert(data.markdown.includes('3. Ordered'));assert(data.markdown.includes('A\\|B'));assert(data.markdown.includes('> Quoted'));checks++;
 assert(!data.frozenHtml.includes('<script'));assert(!data.frozenHtml.includes('onclick='));assert(!data.frozenHtml.includes('CREDENTIAL-DO-NOT-SAVE'));assert(!data.frozenHtml.includes('<base'));assert(!data.frozenHtml.includes('BUTTON-GARBAGE'));assert(!data.frozenHtml.includes('MENU-GARBAGE'));assert(!data.frozenHtml.includes('<link'));assert(data.frozenHtml.includes('@media print'));checks++;
 await page.evaluate(()=>{document.title='After title';document.querySelector('h1').textContent='After snapshot marker';document.querySelector('table').innerHTML='<tr><td>AFTER-TABLE</td></tr>';document.querySelector('#long').textContent='AFTER-BODY';});
 for(const key of ['frozenHtml','markdown']){assert(data[key].includes('Before snapshot marker'));assert(!data[key].includes('After snapshot marker'));assert(!data[key].includes('AFTER-TABLE'));assert(!data[key].includes('AFTER-BODY'));}assert.equal(data.title,'Frozen 中文 café 😀');checks++;
 const staticPage=await browser.newPage({javaScriptEnabled:false});await staticPage.setContent(data.frozenHtml);assert((await staticPage.locator('body').innerText()).includes('TAIL-SNAPSHOT-MARKER'));assert((await staticPage.locator('table').first().innerText()).includes('TABLE-LAST'));checks++;
 await page.evaluate(()=>document.body.insertAdjacentHTML('beforeend','<svg xmlns="http://www.w3.org/2000/svg"><script type="text/javascript">window.svgLeak="SVG-SCRIPT-GARBAGE"</script><text>ICON-CONTROL-GARBAGE</text><animate attributeName="opacity" values="0;1" dur="1s" repeatCount="indefinite"/></svg>'));
 const svg=await capture();assert(!svg.error);assert(!svg.frozenHtml.includes('SVG-SCRIPT-GARBAGE'));assert(!svg.frozenHtml.includes('<animate'));assert(!svg.markdown.includes('ICON-CONTROL-GARBAGE'));checks++;
 assert.equal((await capture('https://chatgpt.com/c/old')).error,'S01_INVALID_PAGE');checks++;
 await page.goto('https://chatgpt.com/c/snapshot-fixture');await page.evaluate(()=>document.querySelector('main').innerHTML='<div data-message-author-role="user" style="white-space:pre-wrap">Same words\nSecond line</div><section data-turn="assistant" data-testid="conversation-turn-1"><p>Same words</p></section>');
 const roles=await capture();assert(roles.markdown.includes('## 用户'));assert(roles.markdown.includes('## 助手'));assert(/class="[^"]*message user/.test(roles.frozenHtml));assert(roles.frozenHtml.includes('class="message assistant"'));assert(roles.markdown.includes('Same words\nSecond line'));checks++;
 await page.goto('https://chatgpt.com/c/snapshot-fixture');await page.evaluate(()=>document.querySelector('#long').innerHTML=Array.from({length:400},(_,i)=>'<p>'+i+' '+('LONG-SCREEN '.repeat(120))+'</p>').join(''));
 assert(await page.evaluate(()=>document.body.scrollHeight/innerHeight)>100);const long=await capture();assert(!long.error);assert(long.frozenHtml.includes('TAIL-SNAPSHOT-MARKER'));fs.writeFileSync('/tmp/nova-static-html/long.html',long.frozenHtml);checks++;
 await page.goto('https://example.org/c/snapshot-fixture');assert.equal((await capture()).error,'S01_INVALID_PAGE');checks++;
 await page.goto('https://chatgpt.com/c/snapshot-fixture');await page.evaluate(()=>{const root=document.documentElement,real=root.cloneNode.bind(root);root.cloneNode=(deep)=>{const c=real(deep);history.replaceState({},'', '/c/changed');document.title='changed';return c;};});assert.equal((await capture()).error,'S08_PAGE_CHANGED_BEFORE_SNAPSHOT');checks++;
 await page.goto('https://chatgpt.com/c/snapshot-fixture');await page.evaluate(()=>document.body.innerHTML='<main>'+('x'.repeat(7*1024*1024))+'</main>');assert.equal((await capture()).error,'S03_SNAPSHOT_TOO_LARGE');checks++;
 console.log(`PASS: ${checks} frozen snapshot browser scenarios (synthetic only)`);
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
