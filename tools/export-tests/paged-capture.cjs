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
import {initial,older} from './page-helper-fixture.js';
export async function reader(id,options={}) {
  if (!options.forceNetworkFetch) throw Error('A fresh read is required');
  const endpoint='/conversation/{conversation_id}';
  if (options.includeFullConversation) {
    if (window.fixturePreRead) void fetch('/backend-api/conversations/'+id+'?num_turns=20&include_message_id=m399&pending=before-read');
    const response=await fetch('/backend-api/conversation/'+id+'?include_full_conversation=true',{signal:options.signal});
    if (!response.ok) {const error=Error('Full graph unavailable');error.status=response.status;throw error;}
    const tree=await response.json();options.onConversationLoadedFromNetwork(tree);return tree;
  }
  // Match builds whose legacy graph route is unavailable before pagination.
  const legacy=await fetch('/backend-api/conversation/'+id,{signal:options.signal});
  if (legacy.ok) {const tree=await legacy.json();options.onConversationLoadedFromNetwork(tree);return tree;}
  const context={};
  if (options.projectId) context['chatgpt-project-id']=options.projectId;
  // Keep a second, older-page binding in the reader source as current public
  // builds do; it must resolve to the same loaded dependency.
  if (false) await older({additionalHeaders:context,clientThreadId:id,cursor:'unused',moderationResults:[],numTurns:20,signal:options.signal});
  const data=await initial({additionalHeaders:context,clientThreadId:id,...window.fixtureReadWindow,signal:options.signal});
  const tree={conversation_id:id,current_node:data.serverCurrentLeafId,mapping:{},__paginatedConversationPage:{numTurns:window.fixtureMarkerTurns}};
  options.onConversationLoadedFromNetwork(tree);return tree;
}
`;
const helperModule=`
const api={safeGet:async(path,{additionalHeaders,signal})=> {
  // Check only the fixture's public route context, not fetch/session headers.
  if (additionalHeaders?.['chatgpt-project-id']!=='${projectId}' || Object.keys(additionalHeaders).length!==1)
    throw Error('Expected current project route context');
  const init=window.fixtureFrozenInit ? Object.freeze({signal,cache:'force-cache'}) : {signal};
  window.fixtureExpectedSignal=signal;
  return fetch('/backend-api'+path,init);
}};
export async function initial({clientThreadId,includeMessageId,numTurns,signal,additionalHeaders}) {
  const endpoint='/conversations/{conversation_id}',include_has_versions=true;
  const query=new URLSearchParams({include_has_versions});
  if (numTurns!==undefined) query.set('num_turns',numTurns);
  if (includeMessageId!=null) query.set('include_message_id',includeMessageId);
  if (window.fixtureDuplicateWindow) query.append('num_turns','30');
  window.fixtureHeadCalls=(window.fixtureHeadCalls||0)+1;
  if (window.fixtureHeadCalls>1 && window.fixtureWrongRecheckTurns) query.set('num_turns','50');
  if (window.fixtureHeadCalls>1 && window.fixtureWrongRecheckMessage) query.set('include_message_id','m398');
  const response=await api.safeGet('/conversations/'+clientThreadId+'?'+query,{additionalHeaders,signal});
  if (!response.ok) {const error=Error('Initial page unavailable');error.status=response.status;throw error;}
  const raw=await response.json();
  if (window.fixtureLateHead && window.fixtureHeadCalls===1)
    void fetch('/backend-api/conversations/'+clientThreadId+'?'+query+'&pending=previous-phase');
  const messagesLeafToRoot=raw.messages.slice().reverse(),serverCurrentLeafId=raw.current_node;
  return {messagesLeafToRoot,serverCurrentLeafId};
}
export async function older({clientThreadId,cursor,moderationResults,numTurns,signal,additionalHeaders}) {
  const endpoint='/conversations/{conversation_id}/messages',include_has_versions=true;
  const query=new URLSearchParams({before:cursor,include_has_versions});
  if (numTurns!==undefined) query.set('num_turns',numTurns);
  if (window.fixtureWrongOlderTurns) query.set('num_turns','25');
  if (window.fixtureWrongOlderMessage) query.set('include_message_id','m399');
  const response=await api.safeGet('/conversations/'+clientThreadId+'/messages?'+query,{additionalHeaders,signal});
  if (!response.ok) {const error=Error('Older page unavailable');error.status=response.status;throw error;}
  const raw=await response.json();
  return {messages:raw.messages,moderationResults,cursor};
}
`;

async function setup(browser,mode={}) {
  const page=await browser.newPage();
  const requests={full:0,legacy:0,heads:[],windows:[],older:[],olderTurns:[],readerSource:0,pending:0},headReads=new Map();
  const rotatingCursor=!!(mode.unknownMetadata || mode.endCursorOnly || mode.replayOldEdit || mode.finalHeadChange || mode.replayMissingBoolean);
  let firstWindow=null,heldPending=null;
  await page.route('https://chatgpt.com/**',async route=> {
    // Scope mocks by public origin/path only; never inspect headers or credentials.
    const url=new URL(route.request().url());
    assert.equal(url.origin,'https://chatgpt.com');
    if (url.pathname==='/cdn/assets/conversation-paged-fixture.js') {
      requests.readerSource++;
      if (mode.sourceDenied && requests.readerSource>1)
        return route.fulfill({status:403,contentType:'text/plain',body:'module source unavailable'});
      return route.fulfill({contentType:'application/javascript',body:readerModule});
    }
    if (url.pathname==='/cdn/assets/page-helper-fixture.js')
      return route.fulfill({contentType:'application/javascript',body:helperModule});
    if (url.pathname==='/backend-api/conversation/fixture') {
      if (url.searchParams.get('include_full_conversation')==='true') requests.full++;
      else requests.legacy++;
      return route.fulfill({status:403,contentType:'application/json',body:'{"detail":"Graph route unavailable"}'});
    }
    if (url.pathname==='/backend-api/conversations/fixture') {
      if (url.searchParams.has('pending')) {
        requests.pending++;heldPending=route;return;
      }
      const actualTurns=url.searchParams.get('num_turns'),numTurns=actualTurns===null ? 20 : Number(actualTurns);
      assert([20,50].includes(numTurns));requests.heads.push(actualTurns===null ? null : numTurns);
      const count=(headReads.get(numTurns)||0)+1;headReads.set(numTurns,count);
      if (mode.normalDenied && numTurns===20)
        return route.fulfill({status:403,contentType:'application/json',body:'{"detail":"Normal reader unavailable"}'});
      const window={numTurns:actualTurns,includeMessageId:url.searchParams.get('include_message_id')};
      requests.windows.push(window);
      if (firstWindow && JSON.stringify(window)!==JSON.stringify(firstWindow) && !mode.wrongRecheckTurns && !mode.wrongRecheckMessage)
        return route.fulfill({contentType:'application/json',body:JSON.stringify({conversation_id:'fixture',title:'different-window',current_node:'m399',messages:rows(320,400),page_info:{has_previous_page:true,start_cursor:'different'}})});
      firstWindow=window;
      // The normal reader uses a different window from the direct helper. A
      // successful traversal must recheck the same window that established it.
      const start=(mode.wrongRecheckTurns ? Number(firstWindow.numTurns) : numTurns)===20 ? 320 : 200;
      const body={conversation_id:'fixture',title:conversation.title,current_node:'m399',messages:rows(start,400),
        page_info:{has_previous_page:true,start_cursor:'older-'+(mode.wrongRecheckTurns ? firstWindow.numTurns : numTurns)+(rotatingCursor ? '-'+count : '')}};
      if (mode.missingBoolean==='head') delete body.page_info.has_previous_page;
      if (mode.wrongId==='head') body.conversation_id='another-conversation';
      if (mode.changedRecheck && count>1) body.messages.at(-1).content.parts=['Changed after older-page traversal'];
      if (mode.changedMetadata && count>1) body.update_time=1001;
      if (mode.unknownMetadata || mode.replayOldEdit || mode.finalHeadChange || mode.replayMissingBoolean) body.request_metadata={read:count};
      if (mode.endCursorOnly || mode.unknownMetadata) body.page_info.end_cursor='head-end-'+count;
      if (mode.criticalMetadata && count>1) body[mode.criticalMetadata]=mode.criticalMetadata==='title' ? 'Changed title' : ['changed'];
      if (mode.finalHeadChange && count>2) {
        if (mode.finalHeadChange==='branch') body.current_node='m398';
        else if (mode.finalHeadChange==='raw') body.messages.at(-1).metadata={final_head_changed:true};
        else if (mode.finalHeadChange==='title') body.title='Changed final title';
        else if (mode.finalHeadChange==='update_time') body.update_time=1002;
      }
      if (heldPending && mode.preRead) {
        const pending=heldPending;heldPending=null;
        await pending.fulfill({contentType:'application/json',body:JSON.stringify(body)});
      }
      return route.fulfill({contentType:'application/json',body:JSON.stringify(body)});
    }
    if (url.pathname==='/backend-api/conversations/fixture/messages') {
      const cursor=url.searchParams.get('before');requests.older.push(cursor);
      requests.olderTurns.push(url.searchParams.get('num_turns'));
      assert(/^older-(20|50)(-[1-3])?$/.test(cursor));
      if (mode.olderDenied)
        return route.fulfill({status:403,contentType:'application/json',body:'{"detail":"Older page unavailable"}'});
      const end=cursor.startsWith('older-20') ? 320 : 200;
      const body={conversation_id:'fixture',title:conversation.title,messages:rows(0,end),
        page_info:{has_previous_page:false,start_cursor:null}};
      if (mode.missingBoolean==='older') delete body.page_info.has_previous_page;
      if (mode.wrongId==='older') body.conversation_id='another-conversation';
      if (mode.repeatedCursor) body.page_info={has_previous_page:true,start_cursor:cursor};
      if (mode.replayOldEdit && requests.older.length===2) {
        const message=body.messages[0];
        if (mode.replayOldEdit==='text') message.content.parts=['Older message changed during replay'];
        else if (mode.replayOldEdit==='citations') message.metadata={citations:[{start_ix:0,end_ix:5,url:'https://example.com/changed'}]};
        else if (mode.replayOldEdit==='hidden') message.metadata={is_visually_hidden_from_conversation:true};
        else if (mode.replayOldEdit==='role') message.author.role='assistant';
      }
      if (mode.replayMissingBoolean && requests.older.length===2) delete body.page_info.has_previous_page;
      if (heldPending && mode.lateHead) {
        const pending=heldPending;heldPending=null;
        const head={conversation_id:'fixture',title:conversation.title,current_node:'m399',messages:rows(320,400),
          page_info:{has_previous_page:true,start_cursor:'older-20'}};
        await pending.fulfill({contentType:'application/json',body:JSON.stringify(head)});
      }
      if (mode.changedRoute) await page.evaluate(()=>history.replaceState(null,'','/c/another-conversation'));
      return route.fulfill({contentType:'application/json',body:JSON.stringify(body)});
    }
    if (url.pathname===conversationPath)
      return route.fulfill({contentType:'text/html',body:'<main><div data-message-id="m399" data-message-author-role="assistant">Only the final message is rendered</div></main>'});
    return route.fulfill({status:404,body:'Unexpected fixture path'});
  });
  await page.goto('https://chatgpt.com'+conversationPath);
  await page.evaluate(mode=> {
    window.fixtureReadWindow={};
    if (!mode.omitTurns) window.fixtureReadWindow.numTurns=20;
    if (!mode.omitMessage) window.fixtureReadWindow.includeMessageId=mode.emptyMessage ? '' : 'm399';
    window.fixtureMarkerTurns=mode.markerTurns===undefined ? 20 : mode.markerTurns;
    window.fixtureDuplicateWindow=!!mode.duplicateWindow;
    window.fixturePreRead=!!mode.preRead;
    window.fixtureLateHead=!!mode.lateHead;
    window.fixtureWrongRecheckTurns=!!mode.wrongRecheckTurns;
    window.fixtureWrongRecheckMessage=!!mode.wrongRecheckMessage;
    window.fixtureWrongOlderTurns=!!mode.wrongOlderTurns;
    window.fixtureWrongOlderMessage=!!mode.wrongOlderMessage;
    window.fixtureFrozenInit=!!mode.frozenInit;
    const fetch=window.fetch;
    window.fixtureFetchOptions=[];
    window.fetch=function(input,init) {
      const url=new URL(typeof input==='string' ? input : input.url,location.href);
      if (url.pathname.startsWith('/backend-api/conversations/fixture'))
        window.fixtureFetchOptions.push({url:url.href,cache:init?.cache,signalForwarded:init?.signal===window.fixtureExpectedSignal});
      return fetch.apply(this,arguments);
    };
  },mode);
  await page.evaluate(()=>import('/cdn/assets/conversation-paged-fixture.js'));
  await page.evaluate(capture);
  return {page,requests};
}

let scenarios=0;
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
    const replay=!!(mode.unknownMetadata || mode.endCursorOnly);
    assert.deepEqual(tree.__novaPaginationProof,{method:'cursor-pagination',pages:replay ? 4 : 2,exhausted:true});
    assert.equal(result.completeness.root,'paginated-complete:fixture');
    assert.equal(requests.full,1);assert.equal(requests.legacy,1);
    const expectedHeads=mode.omitTurns ? [null,null] : expectedWindow===20 ? [20,20] : [20,50,50];
    if (replay) expectedHeads.push(mode.omitTurns ? null : expectedWindow);
    assert.deepEqual(requests.heads,expectedHeads);
    assert.deepEqual(requests.older,replay ? ['older-'+expectedWindow+'-1','older-'+expectedWindow+'-2'] : ['older-'+expectedWindow]);
    assert.deepEqual(requests.windows[0],requests.windows.at(-1),'recheck must preserve the original query values and absences');
    assert.deepEqual(requests.olderTurns,Array(replay ? 2 : 1).fill(requests.windows[0].numTurns),'older requests retain the original turn window');
    const fetchOptions=await page.evaluate(()=>window.fixtureFetchOptions);
    for (const request of fetchOptions.filter(request=>!new URL(request.url).searchParams.has('pending'))) {
      assert.equal(request.cache,'no-store','each authorized page request must bypass the browser cache');
      assert.equal(request.signalForwarded,true,'site AbortSignal must be forwarded without replacement');
    }
    if (mode.preRead || mode.lateHead) assert.equal(requests.pending,1,'delayed fixture response must actually overlap collection');
    assert.equal(await page.evaluate(()=>window.__novaExportCapture.collectPages),false);
    scenarios++;
  } finally {await page.close();}
}

async function refuses(browser,name,mode) {
  const {page,requests}=await setup(browser,{normalDenied:true,...mode});
  let emitted=null;
  try {
    await assert.rejects(async()=>{emitted=await page.evaluate(()=>window.__novaReadConversation('fixture'));},error=> {
      assert.match(error.message,/无法确认完整会话|会话发生变化/);
      if (mode.changedRecheck) assert.match(error.message,/阶段=recheck-verify，原因=head-messages-changed/);
      if (mode.changedMetadata) assert.match(error.message,/阶段=recheck-verify，原因=head-update-time-changed/);
      if (mode.criticalMetadata) {
        const reason={title:'head-title-changed',moderation_results:'head-moderation-changed',safe_urls:'head-link-metadata-changed',blocked_urls:'head-link-metadata-changed'}[mode.criticalMetadata];
        assert(error.message.includes('阶段=recheck-verify，原因='+reason),error.message);
      }
      if (mode.replayOldEdit) assert.match(error.message,/阶段=replay-compare，原因=replay-messages-changed/);
      if (mode.finalHeadChange) {
        const reason={branch:'head-branch-changed',raw:'head-messages-changed',title:'head-title-changed',update_time:'head-update-time-changed'}[mode.finalHeadChange];
        assert(error.message.includes('阶段=replay-recheck-verify，原因='+reason),error.message);
      }
      if (mode.wrongRecheckTurns || mode.wrongRecheckMessage || mode.wrongOlderTurns || mode.wrongOlderMessage)
        assert.match(error.message,/原因=request-window-invalid/);
      assert(!error.message.includes(projectId),'diagnostics must not contain identifiers');
      return true;
    });
    assert.equal(emitted,null,name+' must not emit a partial tree');
    const state=await page.evaluate(()=>({tree:window.__novaExportCapture.tree,id:window.__novaExportCapture.id,
      collecting:window.__novaExportCapture.collectPages}));
    assert.deepEqual(state,{tree:null,id:null,collecting:false},name+' must not retain a partial export');
    assert.equal(requests.legacy,1);
    assert(requests.full>=1);
    if (mode.replayOldEdit || mode.finalHeadChange || mode.replayMissingBoolean) {
      assert.equal(requests.older.length,2,'refusal must exercise the second complete traversal');
      assert.deepEqual(requests.older,['older-50-1','older-50-2'],'replay must use the cursor from its fresh head');
    }
    scenarios++;
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
    await complete(browser,{sourceDenied:true},20);
    console.log('PASS: loaded-asset marker fallback survives a blocked reader source refetch');
    await complete(browser,{omitMessage:true},20);
    console.log('PASS: an absent message parameter stays absent during recheck despite a visible DOM leaf');
    await complete(browser,{emptyMessage:true},20);
    console.log('PASS: an empty message parameter stays present and empty during recheck');
    await complete(browser,{markerTurns:50},20);
    console.log('PASS: observed request turns take precedence over a different normalized marker');
    await complete(browser,{omitTurns:true,omitMessage:true},20);
    console.log('PASS: absent turn and message parameters stay absent in older reads and recheck');
    await complete(browser,{unknownMetadata:true},20);
    console.log('PASS: unknown request metadata drift requires two complete matching raw traversals and a third head');
    await complete(browser,{normalDenied:true,unknownMetadata:true},50);
    console.log('PASS: 403 fallback recovery uses newly issued 50-turn cursors for both complete traversals');
    await complete(browser,{endCursorOnly:true},20);
    console.log('PASS: unknown page end cursor drift requires the same full replay proof');
    await complete(browser,{unknownMetadata:true,omitMessage:true,omitTurns:true},20);
    console.log('PASS: replay preserves absent window parameters through both traversals and final head');
    await complete(browser,{preRead:true},20);
    console.log('PASS: a request started before page collection cannot contaminate the selected head');
    await complete(browser,{lateHead:true},20);
    console.log('PASS: a response from a previous operation cannot contaminate the current page phase');
    await complete(browser,{frozenInit:true},20);
    console.log('PASS: no-store overrides frozen RequestInit cache while forwarding the site signal');
    for (const [name,mode] of [
      ['older-page 403',{olderDenied:true}],
      ['missing initial exhaustion boolean',{missingBoolean:'head'}],
      ['missing older exhaustion boolean',{missingBoolean:'older'}],
      ['wrong initial conversation ID',{wrongId:'head'}],
      ['wrong older conversation ID',{wrongId:'older'}],
      ['repeated cursor',{repeatedCursor:true}],
      ['changed head on recheck',{changedRecheck:true}],
      ['changed metadata on recheck',{changedMetadata:true}],
      ['changed conversation route',{changedRoute:true}]
    ]) await refuses(browser,name,mode);
    for (const criticalMetadata of ['title','moderation_results','safe_urls','blocked_urls'])
      await refuses(browser,'changed critical '+criticalMetadata,{criticalMetadata});
    for (const replayOldEdit of ['text','citations','hidden','role'])
      await refuses(browser,'older raw '+replayOldEdit+' changes during replay',{replayOldEdit});
    for (const finalHeadChange of ['branch','raw','title','update_time'])
      await refuses(browser,'final head '+finalHeadChange+' changes during replay',{finalHeadChange});
    await refuses(browser,'missing replay exhaustion boolean',{replayMissingBoolean:true});
    await refuses(browser,'recheck uses a different turn window',{normalDenied:false,wrongRecheckTurns:true});
    await refuses(browser,'recheck uses a different message window',{normalDenied:false,wrongRecheckMessage:true});
    await refuses(browser,'older request uses a different turn window',{wrongOlderTurns:true});
    await refuses(browser,'older request injects a message window',{wrongOlderMessage:true});
    const ambiguous=await setup(browser,{duplicateWindow:true});
    try {
      await assert.rejects(()=>ambiguous.page.evaluate(()=>window.__novaReadConversation('fixture')),/原因=request-window-invalid/);
      assert.equal(ambiguous.requests.older.length,0);
    } finally {await ambiguous.page.close();}
    scenarios++;
    console.log('PASS: duplicate window parameters cannot establish a unique request window');
    console.log('PASS: paged capture integration ('+scenarios+' scenarios)');
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
