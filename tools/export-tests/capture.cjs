const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const {fixture}=require('./fixture.cjs');
const capture=fs.readFileSync(path.resolve(__dirname,'../../app/src/main/assets/export/capture.js'),'utf8');
const core=require('../../app/src/main/assets/export/core.js');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});
 try {
  const page=await browser.newPage(); const tree=fixture(200);
  await page.route('https://chatgpt.com/**',async route=>{
   const url=route.request().url();
   if(url.includes('/backend-api/')) return route.fulfill({status:401,body:'Unauthorized'});
   if(url.includes('conversation-small-test.js')) return route.fulfill({contentType:'application/javascript',body:`
    export async function reader(id, options={}) {
      // Structural markers match the publicly observed read-only site reader.
      const endpoint='/conversation/{conversation_id}';
      if(!options.includeFullConversation || !options.forceNetworkFetch) throw Error('missing full read options');
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
  await page.goto('https://chatgpt.com/g/g-p-project/c/fixture');
  await page.evaluate(tree=>{window.fixture=tree;},tree);
  await page.evaluate(()=>import('/cdn/assets/conversation-small-test.js'));
  await page.evaluate(capture);
  const result=await page.evaluate(()=>window.__novaReadConversation('fixture'));
  assert.equal(core.normalize(result,'fixture',['m399']).messages.length,400);
  assert.equal(await page.evaluate(()=>readerCalls),1);
  assert.deepEqual(await page.evaluate(()=>readerOptions),['includeFullConversation','forceNetworkFetch','signal','onConversationLoadedFromNetwork']);
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
  console.log('PASS: page-owned full reader, project route, 400-message chain, alias deduplication, partial/wrong-ID rejection, unauthorized fail-closed');
 } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
