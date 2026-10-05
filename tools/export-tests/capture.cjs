const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const {fixture}=require('./fixture.cjs');
const capture=['pagination.js','capture.js'].map(file=>fs.readFileSync(path.resolve(__dirname,'../../app/src/main/assets/export/',file),'utf8')).join('\n');
const core=require('../../app/src/main/assets/export/core.js');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});
 try {
  const page=await browser.newPage(); const tree=fixture(200); let apiTree=null, directCalls=0;
  await page.route('https://chatgpt.com/**',async route=>{
   const url=route.request().url();
   if(url.includes('/backend-api/')) {
    directCalls++;
    assert.equal(new URL(url).searchParams.get('include_full_conversation'),'true');
    return route.fulfill({status:apiTree?200:401,contentType:'application/json',body:apiTree?JSON.stringify(apiTree):'Unauthorized'});
   }
   if(url.includes('conversation-small-test.js')) return route.fulfill({contentType:'application/javascript',body:`
    export async function reader(id, options={}) {
      // Structural markers match the publicly observed read-only site reader.
      const endpoint='/conversation/{conversation_id}';
      if(!options.includeFullConversation || !options.forceNetworkFetch) throw Error('missing full read options');
      if (options.projectId!=='g-p-project') throw Error('missing route project context');
      if (options.ownerUserId!=='shared-owner') throw Error('missing route owner context');
      if (window.readerMode==='throw') throw new TypeError('private exception content must not be reported');
      if (window.readerMode==='hang') await new Promise(resolve=>setTimeout(resolve,100));
      window.readerCalls=(window.readerCalls||0)+1;
      window.readerOptions=Object.keys(options);
      const tree=window.fixture;
      options.onConversationLoadedFromNetwork(tree);
      return {normalized:true};
    }
    export {reader as alias};
   `});
   return route.fulfill({contentType:'text/html',body:'<main>Only last message is rendered</main>'});
  });
  await page.goto('https://chatgpt.com/g/g-p-project-friendly-name/c/fixture?owner_user_id=shared-owner');
  await page.evaluate(tree=>{window.fixture=tree;},tree);
  await page.evaluate(()=>import('/cdn/assets/conversation-small-test.js'));
  await page.evaluate(capture);
  const result=await page.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.equal(core.normalize(result,'fixture',['m399']).messages.length,400);
  assert.equal(await page.evaluate(()=>readerCalls),1);
  assert.deepEqual(await page.evaluate(()=>readerOptions),['includeFullConversation','forceNetworkFetch','projectId','ownerUserId','signal','onConversationLoadedFromNetwork']);
  // Missing ancestors are still rejected, even when the website returns them.
  await page.evaluate(()=>{delete fixture.mapping.m0;});
  const partial=await page.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.throws(()=>core.normalize(partial,'fixture',['m399']));
  await page.evaluate(()=>{fixture.conversation_id='another-account-conversation';});
  await assert.rejects(()=>page.evaluate(()=>window.__novaReadConversation('fixture')),/无法确认完整会话/);
  // No loaded reader and an unauthorized direct endpoint: no DOM-only export.
  const fresh=await browser.newPage();
  await fresh.route('https://chatgpt.com/**',r=>r.fulfill({status:r.request().url().includes('/backend-api/')?401:200,body:'visible message'}));
  await fresh.goto('https://chatgpt.com/c/fixture'); await fresh.evaluate(capture);
  await assert.rejects(()=>fresh.evaluate(()=>window.__novaReadConversation('fixture')),/诊断 E2：模块=0，导入=0，读取器=0，状态=not-found，接口=401/);
  // Discovery still works when ResourceTiming no longer contains the module.
  await page.evaluate(()=>{
   fixture.conversation_id='fixture';
   const link=document.createElement('link');link.rel='modulepreload';link.href='/cdn/assets/conversation-small-test.js';document.head.append(link);
   performance.getEntriesByType=()=>[];
  });
  assert.equal((await page.evaluate(()=>window.__novaReadConversation('fixture'))).conversation_id,'fixture');
  // Scoped raw React conversation props can supply the full graph without an API/module.
  await fresh.evaluate(tree=> {
   document.body.innerHTML='<div data-message-id="m399" data-message-author-role="assistant">last</div>';
   const element=document.querySelector('[data-message-id]');
   element.__reactFiber$fixture={memoizedProps:{conversation:tree},return:null};
  },tree);
  const retained=await fresh.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.equal(core.normalize(retained,'fixture',['m399']).messages.length,400);
  await fresh.evaluate(()=> {
   const props=document.querySelector('[data-message-id]').__reactFiber$fixture.memoizedProps;
   delete props.conversation.mapping.m0;
  });
  const incomplete=await fresh.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.throws(()=>core.normalize(incomplete,'fixture',['m399']));
  await fresh.evaluate(()=>{document.querySelector('[data-message-id]').__reactFiber$fixture.memoizedProps.conversation.conversation_id='other';});
  await assert.rejects(()=>fresh.evaluate(()=>window.__novaReadConversation('fixture')),/无法确认完整会话/);
  // Reader exceptions must leave the independently verified endpoint available.
  apiTree=fixture(200);
  await page.evaluate(()=>{window.readerMode='throw';});
  const calls=directCalls;
  const fallback=await page.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.equal(directCalls,calls+1);
  assert.equal(core.normalize(fallback,'fixture',['m399']).messages.length,400);
  // A previous successful full response cannot rescue a later failed fresh read.
  await page.evaluate(()=>fetch('/backend-api/conversation/fixture?include_full_conversation=true'));
  await page.waitForFunction(()=>__novaExportCapture.id==='fixture');
  apiTree=null;
  await assert.rejects(()=>page.evaluate(()=>window.__novaReadConversation('fixture')),error=> {
   assert.match(error.message,/状态=failed-TypeError，接口=401/);
   assert(!error.message.includes('private exception'));
   assert(!error.message.includes('shared-owner'));
   assert(!error.message.includes('fixture'));
   return true;
  });
  // An uncooperative reader settles through the deadline, and a late callback
  // cannot cache its result. Accelerate only the production read deadlines.
  await page.evaluate(()=>{
   window.readerMode='hang';window.fixture.conversation_id='fixture';
   const nativeTimeout=window.setTimeout.bind(window);
   window.setTimeout=(callback,delay,...args)=>nativeTimeout(callback,delay===15000?20:delay,...args);
  });
  apiTree=fixture(200);
  const recovered=await page.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.equal(core.normalize(recovered,'fixture',['m399']).messages.length,400);
  await page.waitForTimeout(150);
  assert.equal(await page.evaluate(()=>__novaExportCapture.tree),null);
  // Top-level await in a loaded module must also be bounded, before calling API.
  const stalled=await browser.newPage();
  await stalled.route('https://chatgpt.com/**',route=> {
   if(route.request().url().includes('conversation-stalled.js')) return route.fulfill({contentType:'application/javascript',body:'await new Promise(()=>{});'});
   if(route.request().url().includes('/backend-api/')) return route.fulfill({contentType:'application/json',body:JSON.stringify(fixture(200))});
   return route.fulfill({contentType:'text/html',body:'<link rel="modulepreload" href="/cdn/assets/conversation-stalled.js"><main>last</main>'});
  });
  await stalled.goto('https://chatgpt.com/c/fixture');
  await stalled.evaluate(()=>{
   const nativeTimeout=window.setTimeout.bind(window);
   window.setTimeout=(callback,delay,...args)=>nativeTimeout(callback,delay===15000?20:delay,...args);
  });
  await stalled.evaluate(capture);
  const imported=await stalled.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.equal(core.normalize(imported,'fixture',['m399']).messages.length,400);
  console.log('PASS: page-owned full reader, project route, 400-message chain, alias deduplication, partial/wrong-ID rejection, route project/owner context, independent fallback, bounded reader/import, safe diagnostics, unauthorized fail-closed');
 } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
