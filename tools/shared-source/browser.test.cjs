const assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE||'playwright-core');
const {SharedConversationSource,allowed}=require('./SharedConversationSource.cjs');
(async()=>{const b=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE||'/usr/bin/chromium',args:['--no-sandbox']});let checks=0;try{
 for(const u of ['http://chatgpt.com/share/x','https://chatgpt.com.evil/share/x','https://u:p@chatgpt.com/share/x','https://chatgpt.com/c/x','https://chatgpt.com/share/','https://chatgpt.com/share/x/extra'])assert(!allowed(u));assert(allowed('https://chatgpt.com/share/fixture'));checks++;
 const p=await b.newPage();await p.route('**/*',r=>r.fulfill({contentType:'text/html',body:'<main><button>Log in</button><div data-testid="conversation-turn-0" data-message-author-role="user">FIRST</div><div data-testid="conversation-turn-1" data-message-author-role="assistant">LAST</div></main>'}));await p.goto('https://chatgpt.com/share/fixture');
 const s=new SharedConversationSource(p,p.url(),{pollMs:20,timeoutMs:500});assert.equal((await s.waitStable()).count,2);checks++;
 const stable=await s.inspectVirtualization();assert(!stable.virtualizationObserved);assert(stable.topFirstStillMounted);checks++;
 await p.evaluate(()=>{document.body.innerHTML='<main id="scroll" style="height:200px;overflow:auto"><div style="height:2000px"><div id="messages"></div></div></main>';const root=document.querySelector('#scroll'),msgs=document.querySelector('#messages');const render=()=>msgs.innerHTML=root.scrollTop<1000?'<div data-testid="conversation-turn-0" data-message-author-role="user">TOP_ONLY</div><div data-message-author-role="assistant">TOP_REPLY</div>':'<div data-testid="conversation-turn-8" data-message-author-role="user">BOTTOM_ONLY</div><div data-message-author-role="assistant">BOTTOM_REPLY</div>';root.addEventListener('scroll',render);render();});
 const v=await s.inspectVirtualization();assert(v.virtualizationObserved);assert(!v.topFirstTextStillPresent);assert.equal(v.top.dom.count,v.bottom.dom.count);checks++;
 await p.evaluate(()=>document.body.innerHTML='<main><div data-message-author-role="assistant">TEXT</div><div aria-busy="true">Loading</div></main>');await assert.rejects(s.waitStable(),/SHARE_NOT_STABLE/);checks++;
 await p.evaluate(()=>document.body.innerHTML='<main>Shared link is unavailable</main>');await assert.rejects(s.waitStable(),/SHARE_UNAVAILABLE/);checks++;
 await p.evaluate(()=>history.replaceState({},'', '/c/changed'));await assert.rejects(s.observe(),/SHARE_URL_CHANGED/);checks++;
 console.log(`PASS: ${checks} shared-source fixture checks; not a real coverage proof`);
 }finally{await b.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
