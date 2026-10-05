const fs=require('node:fs'); const path=require('node:path'); const assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const {fixture}=require('./fixture.cjs');
const root=path.resolve(__dirname,'../..'), assets=path.join(root,'app/src/main/assets/export');
(async()=> {
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});
 try {
  const page=await browser.newPage({viewport:{width:390,height:844}});
  await page.route('**/*',r=>r.request().url().startsWith('file:') ? r.continue() : r.abort());
  await page.goto('about:blank');
  await page.addScriptTag({path:path.join(assets,'marked.js')});
  await page.addScriptTag({path:path.join(assets,'core.js')});
  const tree=fixture(40);
  const result=await page.evaluate(tree=> {
   const d=NovaExportCore.normalize(tree,'fixture',['m79']);
   return {html:NovaExportCore.html(d,marked,document),markdown:NovaExportCore.markdown(d)};
  },tree);
  fs.mkdirSync(path.join(root,'samples/export'),{recursive:true});
  fs.writeFileSync(path.join(root,'samples/export/conversation.html'),result.html);
  fs.writeFileSync(path.join(root,'samples/export/conversation.md'),result.markdown);
  const unsafe=await page.evaluate(()=>NovaExportCore.clean('<script>alert(1)</script><a href="javascript:alert(1)" onclick="bad()">link</a><img src="file:///secret" onerror="bad()"><table><tr><td>x</td></tr></table>',document));
  assert(!unsafe.includes('<script'));assert(!unsafe.includes('onclick'));assert(!unsafe.includes('onerror'));assert(!unsafe.includes('javascript:'));assert(unsafe.includes('table-scroll'));
  assert(result.html.includes('\\frac{1}{3}'));
  assert(result.html.includes('https://example.org/table'));
  assert(result.html.includes('data:image/png;base64,'));
  if (process.env.NOVA_OFFLINE_DOCUMENT_URL) {
   await page.route(process.env.NOVA_OFFLINE_DOCUMENT_URL,r=>r.fulfill({contentType:'text/html',body:result.html}));
   await page.goto(process.env.NOVA_OFFLINE_DOCUMENT_URL);
  } else await page.goto('file://'+path.join(root,'samples/export/conversation.html'));
  assert.equal(await page.locator('article').count(),80);
  assert.equal(await page.locator('a[href="https://example.org/table"]').count(),1);
  for (const width of [320,390,844,1280]) {
   await page.setViewportSize({width,height:844});
   assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'page overflow at '+width);
  }
  await page.setViewportSize({width:390,height:844});
  await page.emulateMedia({colorScheme:'light'});
  const light=await page.locator('body').evaluate(el=>getComputedStyle(el).backgroundColor);
  await page.screenshot({path:path.join(root,'samples/export/mobile-light.png')});
  await page.emulateMedia({colorScheme:'dark'});
  const dark=await page.locator('body').evaluate(el=>getComputedStyle(el).backgroundColor);
  assert.notEqual(light,dark);
  await page.screenshot({path:path.join(root,'samples/export/mobile-dark.png')});
  await page.locator('article').last().screenshot({path:path.join(root,'samples/export/rich-content.png')});
  await page.pdf({path:path.join(root,'samples/export/conversation.pdf'),preferCSSPageSize:true,printBackground:true});
  // Exercise the production extraction and chunk transport on a synthetic HTTPS page.
  await page.unroute('**/*');
  await page.route('https://chatgpt.com/**',r=>r.fulfill({contentType:'text/html',body:'<div data-message-id="m3" data-message-author-role="assistant">terminal reply</div>'}));
  await page.goto('https://chatgpt.com/c/fixture');
  const t=fixture();t.mapping.m3.message.content.parts=['terminal reply'];
  await page.evaluate(t=> {
   window.out=[];window.NovaConversationExport={postMessage:s=>out.push(JSON.parse(s))};
   window.__novaReadConversation=async()=>t;window.marked='website-owned-marked';
  },t);
  const source='(()=>{const module={exports:{}};const exports=module.exports;\n'+fs.readFileSync(path.join(assets,'marked.js'),'utf8')+
   '\nconst marked=module.exports.marked;\n'+fs.readFileSync(path.join(assets,'core.js'),'utf8')+
   '\nconst NovaExportCore=module.exports;\n'+fs.readFileSync(path.join(assets,'run.js'),'utf8').replaceAll('__NOVA_NONCE__','"test"')+'\n})();';
  await page.evaluate(source);
  await page.waitForFunction(()=>out.length>0);
  const out=await page.evaluate(()=>({out,marked:window.marked}));
  assert.equal(out.marked,'website-owned-marked');
  assert(!out.out[0].error,JSON.stringify(out.out));
  const data=JSON.parse(out.out.map(p=>p.chunk).join(''));assert.equal(data.proof.messages,4);assert(data.markdown.includes('第 1 条消息'));
  // A stale prefix cannot pass as the terminal message's entire body.
  await page.evaluate(()=>{out=[];document.querySelector('[data-message-id]').textContent='terminal reply with a newer suffix';});
  await page.evaluate(source);await page.waitForFunction(()=>out.length>0);
  assert.match(await page.evaluate(()=>out[0].error),/末条消息内容与消息树不同/);
  const codeTree=fixture();codeTree.mapping.m3.message.content.parts=['```javascript\nterminal reply\n```'];
  await page.evaluate(t=>{
   out=[];window.__novaReadConversation=async()=>t;
   document.querySelector('[data-message-id]').innerHTML='<pre><div>javascript<button>Copy code</button></div><code>terminal reply\n</code></pre>';
  },codeTree);
  await page.evaluate(source);await page.waitForFunction(()=>out.length>0);
  assert(!await page.evaluate(()=>out[0].error),'code language labels are presentation controls');
  const mathTree=fixture();mathTree.mapping.m3.message.content.parts=[String.raw`Equation \(E = mc^2\)`];
  await page.evaluate(t=> {
   out=[];window.__novaReadConversation=async()=>t;
   document.querySelector('[data-message-id]').innerHTML='Equation <span class="katex"><annotation encoding="application/x-tex">E = mc^2</annotation></span>';
  },mathTree);
  await page.evaluate(source);await page.waitForFunction(()=>out.length>0);
  assert(!await page.evaluate(()=>out[0].error),'KaTeX evidence must agree with backslash-delimited TeX');
  await page.evaluate(()=>{out=[];document.querySelector('[data-message-id]').setAttribute('data-message-id','m1');});
  await page.evaluate(source);await page.waitForFunction(()=>out.length>0);
  assert.match(await page.evaluate(()=>out[0].error),/无法确认完整会话/);
  // Same-ID streaming/edit changes during the read cancel this export.
  await page.evaluate(t=>{
   out=[];document.querySelector('[data-message-id]').setAttribute('data-message-id','m3');
   document.querySelector('[data-message-id]').textContent='terminal reply';
   window.__novaReadConversation=async()=>{
    document.querySelector('[data-message-id]').textContent='terminal reply changed during read';
    return t;
   };
  },t);
  await page.evaluate(source);await page.waitForFunction(()=>out.length>0);
  assert.match(await page.evaluate(()=>out[0].error),/会话发生变化/);
  // A response observer only captures the matching current conversation.
  await page.evaluate(()=>{window.fetch=async()=>new Response(JSON.stringify({conversation_id:'fixture',mapping:{},current_node:'root'}),{status:200});});
  await page.evaluate(fs.readFileSync(path.join(assets,'capture.js'),'utf8'));
  await page.evaluate(()=>fetch('/backend-api/conversation/elsewhere'));
  assert.equal(await page.evaluate(()=>__novaExportCapture.tree),null);
  await page.evaluate(()=>fetch('/backend-api/conversation/fixture'));
  assert.equal(await page.evaluate(()=>__novaExportCapture.tree),null,'a default/paginated read has no full-read provenance');
  await page.evaluate(()=>fetch('/backend-api/conversation/fixture?include_full_conversation=true'));
  await page.waitForFunction(()=>__novaExportCapture.id==='fixture');
  console.log('PASS: browser sanitizer, full-branch extraction, branch rejection, origin-scoped capture, 320/390/844/1280px layouts, light/dark, standalone HTML, Markdown and Chromium PDF samples');
 } finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1;});
