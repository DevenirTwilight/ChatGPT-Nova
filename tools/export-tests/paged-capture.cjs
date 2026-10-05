const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE || 'playwright-core');
const {fixture}=require('./fixture.cjs');
const core=require('../../app/src/main/assets/export/core.js');
const capture=['pagination.js','capture.js'].map(file=>fs.readFileSync(path.resolve(__dirname,'../../app/src/main/assets/export/',file),'utf8')).join('\n');
const conversation=fixture(200),clone=value=>JSON.parse(JSON.stringify(value));
const rows=(start,end)=>Array.from({length:end-start},(_,index)=>clone(conversation.mapping['m'+(start+index)].message));
const projectId='g-p-1234567890abcdef1234567890abcdef',conversationPath='/g/'+projectId+'/c/fixture';

// These loaded public modules retain the site's reader/dependency markers. The
// helper intentionally returns normalized data without page_info: completeness
// must come from capture.js observing the original response bodies instead.
const readerModule=`
import {initial} from './page-helper-fixture.js';
export async function reader(id,options={}) {
  if (!options.forceNetworkFetch) throw Error('A fresh read is required');
  const endpoint='/conversation/{conversation_id}';
  if (options.includeFullConversation) {
    const response=await fetch('/backend-api/conversation/'+id+'?include_full_conversation=true',{signal:options.signal});
    if (!response.ok) {const error=Error('Full graph unavailable');error.status=response.status;throw error;}
    const tree=await response.json();options.onConversationLoadedFromNetwork(tree);return tree;
  }
  // Match builds whose legacy graph route is unavailable before pagination.
  const legacy=await fetch('/backend-api/conversation/'+id,{signal:options.signal});
  if (legacy.ok) {const tree=await legacy.json();options.onConversationLoadedFromNetwork(tree);return tree;}
  const context={};
  if (options.projectId) context['chatgpt-project-id']=options.projectId;
  const data=await initial({additionalHeaders:context,clientThreadId:id,numTurns:20,signal:options.signal});
  const tree={conversation_id:id,current_node:data.serverCurrentLeafId,mapping:{},__paginatedConversationPage:true};
  options.onConversationLoadedFromNetwork(tree);return tree;
}
`;
const helperModule=`
const api={safeGet:async(path,{additionalHeaders,signal})=> {
  // Check only the fixture's public route context, not fetch/session headers.
  if (additionalHeaders?.['chatgpt-project-id']!=='${projectId}' || Object.keys(additionalHeaders).length!==1)
    throw Error('Expected current project route context');
  return fetch('/backend-api'+path,{signal});
}};
export async function initial({clientThreadId,numTurns,signal,additionalHeaders}) {
  const endpoint='/conversations/{conversation_id}',include_has_versions=true;
  const response=await api.safeGet('/conversations/'+clientThreadId+'?num_turns='+numTurns+'&include_has_versions='+include_has_versions,{additionalHeaders,signal});
  if (!response.ok) {const error=Error('Initial page unavailable');error.status=response.status;throw error;}
  const raw=await response.json();
  const messagesLeafToRoot=raw.messages.slice().reverse(),serverCurrentLeafId=raw.current_node;
  return {messagesLeafToRoot,serverCurrentLeafId};
}
export async function older({clientThreadId,cursor,moderationResults,signal,additionalHeaders}) {
  const endpoint='/conversations/{conversation_id}/messages',include_has_versions=true;
  const response=await api.safeGet('/conversations/'+clientThreadId+'/messages?before='+encodeURIComponent(cursor)+'&include_has_versions='+include_has_versions,{additionalHeaders,signal});
  if (!response.ok) {const error=Error('Older page unavailable');error.status=response.status;throw error;}
  const raw=await response.json();
  return {messages:raw.messages,moderationResults,cursor};
}
`;

async function setup(browser,mode={}) {
  const page=await browser.newPage();
  const requests={full:0,legacy:0,heads:[],older:[]},headReads=new Map();
  await page.route('https://chatgpt.com/**',async route=> {
    // Scope mocks by public origin/path only; never inspect headers or credentials.
    const url=new URL(route.request().url());
    assert.equal(url.origin,'https://chatgpt.com');
    if (url.pathname==='/cdn/assets/conversation-paged-fixture.js')
      return route.fulfill({contentType:'application/javascript',body:readerModule});
    if (url.pathname==='/cdn/assets/page-helper-fixture.js')
      return route.fulfill({contentType:'application/javascript',body:helperModule});
    if (url.pathname==='/backend-api/conversation/fixture') {
      if (url.searchParams.get('include_full_conversation')==='true') requests.full++;
      else requests.legacy++;
      return route.fulfill({status:403,contentType:'application/json',body:'{"detail":"Graph route unavailable"}'});
    }
    if (url.pathname==='/backend-api/conversations/fixture') {
      const numTurns=Number(url.searchParams.get('num_turns'));
      assert([20,50].includes(numTurns));requests.heads.push(numTurns);
      const count=(headReads.get(numTurns)||0)+1;headReads.set(numTurns,count);
      if (mode.normalDenied && numTurns===20)
        return route.fulfill({status:403,contentType:'application/json',body:'{"detail":"Normal reader unavailable"}'});
      // The normal reader uses a different window from the direct helper. A
      // successful traversal must recheck the same window that established it.
      const start=numTurns===20 ? 320 : 200;
      const body={conversation_id:'fixture',title:conversation.title,current_node:'m399',messages:rows(start,400),
        page_info:{has_previous_page:true,start_cursor:'older-'+numTurns}};
      if (mode.missingBoolean==='head') delete body.page_info.has_previous_page;
      if (mode.wrongId==='head') body.conversation_id='another-conversation';
      if (mode.changedRecheck && count>1) body.messages.at(-1).content.parts=['Changed after older-page traversal'];
      return route.fulfill({contentType:'application/json',body:JSON.stringify(body)});
    }
    if (url.pathname==='/backend-api/conversations/fixture/messages') {
      const cursor=url.searchParams.get('before');requests.older.push(cursor);
      assert(['older-20','older-50'].includes(cursor));
      if (mode.olderDenied)
        return route.fulfill({status:403,contentType:'application/json',body:'{"detail":"Older page unavailable"}'});
      const end=cursor==='older-20' ? 320 : 200;
      const body={conversation_id:'fixture',title:conversation.title,messages:rows(0,end),
        page_info:{has_previous_page:false,start_cursor:null}};
      if (mode.missingBoolean==='older') delete body.page_info.has_previous_page;
      if (mode.wrongId==='older') body.conversation_id='another-conversation';
      if (mode.repeatedCursor) body.page_info={has_previous_page:true,start_cursor:cursor};
      if (mode.changedRoute) await page.evaluate(()=>history.replaceState(null,'','/c/another-conversation'));
      return route.fulfill({contentType:'application/json',body:JSON.stringify(body)});
    }
    if (url.pathname===conversationPath)
      return route.fulfill({contentType:'text/html',body:'<main><div data-message-id="m399" data-message-author-role="assistant">Only the final message is rendered</div></main>'});
    return route.fulfill({status:404,body:'Unexpected fixture path'});
  });
  await page.goto('https://chatgpt.com'+conversationPath);
  await page.evaluate(()=>import('/cdn/assets/conversation-paged-fixture.js'));
  await page.evaluate(capture);
  return {page,requests};
}

async function complete(browser,mode,expectedWindow) {
  const {page,requests}=await setup(browser,mode);
  try {
    const tree=await page.evaluate(()=>window.__novaReadConversation('fixture'));
    const result=core.normalize(tree,'fixture',['m399']);
    assert.equal(result.messages.length,400);
    assert.equal(result.messages[0].id,'m0');assert.equal(result.messages.at(-1).id,'m399');
    assert.deepEqual(result.messages.map(message=>message.id),rows(0,400).map(message=>message.id));
    for (const message of result.messages)
      assert.equal(message.evidenceMarkdown,conversation.mapping[message.id].message.content.parts.join('\n\n'));
    assert.deepEqual(tree.__novaPaginationProof,{method:'cursor-pagination',pages:2,exhausted:true});
    assert.equal(result.completeness.root,'paginated-complete:fixture');
    assert.equal(requests.full,1);assert.equal(requests.legacy,expectedWindow===20 ? 2 : 1);
    assert.deepEqual(requests.heads,expectedWindow===20 ? [20,20] : [20,50,50]);
    assert.deepEqual(requests.older,['older-'+expectedWindow]);
    assert.equal(await page.evaluate(()=>window.__novaExportCapture.collectPages),false);
  } finally {await page.close();}
}

async function refuses(browser,name,mode) {
  const {page,requests}=await setup(browser,{normalDenied:true,...mode});
  let emitted=null;
  try {
    await assert.rejects(async()=>{emitted=await page.evaluate(()=>window.__novaReadConversation('fixture'));},
      /无法确认完整会话|会话发生变化/);
    assert.equal(emitted,null,name+' must not emit a partial tree');
    const state=await page.evaluate(()=>({tree:window.__novaExportCapture.tree,id:window.__novaExportCapture.id,
      collecting:window.__novaExportCapture.collectPages}));
    assert.deepEqual(state,{tree:null,id:null,collecting:false},name+' must not retain a partial export');
    assert.equal(requests.legacy,1);
    assert(requests.full>=1);
    console.log('PASS: '+name+' refuses partial export');
  } finally {await page.close();}
}

(async()=> {
  const browser=await chromium.launch({executablePath:process.env.CHROMIUM_PATH || '/usr/bin/chromium',args:['--no-sandbox']});
  try {
    await complete(browser,{normalDenied:true},50);
    console.log('PASS: full/legacy/normal 403 fall back to the loaded helper; two raw pages verify all 400 messages');
    await complete(browser,{},20);
    console.log('PASS: successful normal pagination rechecks its 20-turn reader window instead of the 50-turn direct window');
    for (const [name,mode] of [
      ['older-page 403',{olderDenied:true}],
      ['missing initial exhaustion boolean',{missingBoolean:'head'}],
      ['missing older exhaustion boolean',{missingBoolean:'older'}],
      ['wrong initial conversation ID',{wrongId:'head'}],
      ['wrong older conversation ID',{wrongId:'older'}],
      ['repeated cursor',{repeatedCursor:true}],
      ['changed head on recheck',{changedRecheck:true}],
      ['changed conversation route',{changedRoute:true}]
    ]) await refuses(browser,name,mode);
    console.log('PASS: paged capture integration (10 scenarios)');
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
