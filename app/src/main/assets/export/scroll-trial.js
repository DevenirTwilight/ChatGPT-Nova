(function(command, token) {
 'use strict';
 const KEY='__novaHistoryScrollTrial';
 const WINDOW_LIMIT_MS=30000, READY_GRACE_MS=5000, SCAN_LIMIT_MS=600000, STEP_LIMIT=1200;
 const snapshot=()=>JSON.parse(__NOVA_SNAPSHOT__);
 function readable(e) {
  for(let n=e;n;n=n.parentElement) {const css=getComputedStyle(n);
   if(n.hidden || n.getAttribute('aria-hidden')==='true' || css.display==='none' || ['hidden','collapse'].includes(css.visibility) || css.contentVisibility==='hidden') return false;}
  return true;
 }
 const abort=code=>{throw Object.assign(new Error(code),{code});};
 let s=window[KEY];
 function dispose(restore) {
  if(!s) return;
  s.observer?.disconnect();document.removeEventListener('click',s.interact,true);document.removeEventListener('keydown',s.interact,true);
  if(restore && s.container?.isConnected) s.container.scrollTo({top:s.original,behavior:"instant"});
  s.messages.clear();s.edges.clear();delete window[KEY];
 }
 // Only public, visible indicators in the selected history scroller. Decorative
 // spinning icons inside message bodies are not evidence of history loading.
 function loadingSignals() {
  const frame=s.container.getBoundingClientRect();
  return [...s.container.querySelectorAll('[aria-busy="true"],[role="progressbar"],[data-testid="loading-spinner"],[class~="animate-spin"]')].filter(e=>{
   if(!readable(e)) return false;
   if(e.matches('[class~="animate-spin"]') && e.closest('[data-message-author-role],article[data-turn="assistant"]')) return false;
   const r=e.getBoundingClientRect();return r.width>0 && r.height>0 && r.bottom>Math.max(0,frame.top) && r.top<Math.min(innerHeight,frame.bottom);
  }).length + (s.container.getAttribute('aria-busy')==='true' ? 1 : 0);
 }
 function resetWindow() {
  s.stable=0;s.last='';s.lastIds='';s.prevPos=null;s.prevMax=null;s.edgeStable=0;s.stepStarted=performance.now();s.readyStarted=s.stepStarted;s.wasLoading=false;
  s.window={polls:0,loadingSignals:0,loadingPolls:0,loadingObserved:false,listChanges:0,bodyChanges:0,positionChanges:0,extentChanges:0,mountedCount:0,reason:'awaiting-content',scrollTopPx:0,scrollMaxPx:0,viewportPx:0};
 }
 function stats() {return {source:'scroll-dom-trial',history:'not-proven',count:s.messages.size,
  users:[...s.messages.values()].filter(m=>m.role==='user').length,assistants:[...s.messages.values()].filter(m=>m.role==='assistant').length,
  steps:s.steps,leg:s.leg,traversal:'single-direction',direction:s.direction,plannedLegs:1,startObserved:s.startObserved,overlapRetries:s.overlapRetries,topObserved:s.top,bottomObserved:s.bottom,secondPass:false,
  chars:s.chars,cacheBytes:s.bytes,elapsedMs:Math.round(performance.now()-s.started),
  settling:{...s.window,readyAgeMs:Math.round(performance.now()-s.readyStarted),windowLimitMs:WINDOW_LIMIT_MS,scanLimitMs:SCAN_LIMIT_MS,stepLimit:STEP_LIMIT,contentStable:s.stable,windowAgeMs:Math.round(performance.now()-s.stepStarted),
   totalListChanges:s.changes.list,totalBodyChanges:s.changes.body,totalPositionChanges:s.changes.position,totalExtentChanges:s.changes.extent}};}
 try {
  if(command==='cancel') {if(s?.token===token) dispose(true);return JSON.stringify({cancelled:true});}
  if(command==='start') {
   if(s) abort('H04_BUSY');
   const first=snapshot();if(first.error) return JSON.stringify(first);
   const authors=[...document.querySelectorAll('[data-message-author-role],article[data-turn="assistant"]')].filter(e=>readable(e)
    && (['user','assistant'].includes(e.dataset.messageAuthorRole) || (!e.hasAttribute('data-message-author-role')
      && e.dataset.turn==='assistant' && !e.querySelector('[data-message-author-role]')
      && !e.parentElement.closest('[data-message-author-role],article[data-turn="assistant"]'))));
   const firstAuthor=authors[0];let container=null;
   for(let e=firstAuthor?.parentElement;e;e=e.parentElement) {
    const css=getComputedStyle(e);
    if(e.clientHeight>0 && e.scrollHeight>e.clientHeight+2 && /(auto|scroll)/.test(css.overflowY) && authors.every(a=>e.contains(a))) {container=e;break;}
   }
   if(!container) {
    const root=document.scrollingElement;
    if(root && root.clientHeight>0 && root.contains(firstAuthor) && (root.scrollHeight>root.clientHeight+2
      || authors.every(a=>{const r=a.getBoundingClientRect();return r.top>=0 && r.bottom<=innerHeight;}))) container=root;
   }
   if(!container) abort('H01_SCROLL_CONTAINER');
   s={token,container,original:container.scrollTop,route:location.href,title:document.title,started:performance.now(),stepStarted:performance.now(),
    direction:container.scrollTop<=2?'down':'up',startObserved:false,previousWindow:null,move:null,overlapRetries:0,messages:new Map(),edges:new Map(),warnings:new Set(),chars:0,bytes:0,steps:0,leg:0,top:false,bottom:false,stable:0,edgeStable:0,last:'',lastPosition:'',invalid:false,changes:{list:0,body:0,position:0,extent:0}};
   // One traversal only. Starting in the middle first positions at the bottom.
   if(s.direction==='up') container.scrollTo({top:container.scrollHeight,behavior:"instant"});
   resetWindow();window[KEY]=s;
   s.interact=e=>{if(e.isTrusted) s.invalid=true;};
   document.addEventListener('click',s.interact,true);document.addEventListener('keydown',s.interact,true);
   // Public DOM observation catches SPA changes without wrapping history/fetch or reading React.
   s.observer=new MutationObserver(()=>{if(location.href!==s.route || !s.container.isConnected) s.invalid=true;});
   s.observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true});
  }
  if(!s || s.token!==token) abort('H04_SESSION');
  if(s.invalid || location.href!==s.route || !s.container.isConnected) abort('H04_CHANGED');
  if(performance.now()-s.started>SCAN_LIMIT_MS || s.steps>=STEP_LIMIT) abort('H05_LIMIT');
  const data=snapshot();
  const signals=loadingSignals();s.window.loadingSignals=signals;
  const notReady=['D03_LOADING','D05_NO_MESSAGES'].includes(data.error);
  if(data.error && !notReady) return JSON.stringify(failure(data.error,data.diagnostic));
  if(signals || notReady) {
   s.window.polls++;s.window.loadingPolls++;s.window.loadingObserved=true;
   s.window.reason=signals ? 'history-loading' : 'page-not-ready';
   s.wasLoading=true;s.readyStarted=performance.now();s.stable=0;s.last='';s.lastIds='';s.edgeStable=0;
   if(performance.now()-s.stepStarted>=WINDOW_LIMIT_MS) abort('H06_UNSETTLED');
   return JSON.stringify({waiting:true,coverage:stats()});
  }
  if(s.wasLoading) {s.wasLoading=false;s.readyStarted=performance.now();}
  for(const warning of data.warnings || []) if(!warning.startsWith("试用版仅导出当前页面")) s.warnings.add(warning);
  const rows=data.messages;
  if(rows.some(m=>!m.id)) abort('H02_MISSING_ID');
  const value=JSON.stringify(rows);
  const pos=s.container.scrollTop,max=Math.max(0,s.container.scrollHeight-s.container.clientHeight);
  const identity=JSON.stringify(rows.map(m=>[m.id,m.role]));
  const hadPrevious=s.last!=='';
  const sameContent=hadPrevious && value===s.last;
  const sameList=hadPrevious && identity===s.lastIds;
  const hadGeometry=s.prevPos!==null;
  const positionDelta=hadGeometry ? Math.abs(pos-s.prevPos) : 0;
  const extentDelta=hadGeometry ? Math.abs(max-s.prevMax) : 0;
  const quietGeometry=hadGeometry && positionDelta<=2 && extentDelta<=2;
  s.window.polls++;s.window.mountedCount=rows.length;
  if(hadPrevious && !sameList) {s.window.listChanges++;s.changes.list++;s.readyStarted=performance.now();s.edgeStable=0;}
  if(hadPrevious && sameList && !sameContent) {s.window.bodyChanges++;s.changes.body++;}
  if(positionDelta>0.1) {s.window.positionChanges++;s.changes.position++;}
  if(extentDelta>0.1) {s.window.extentChanges++;s.changes.extent++;}
  Object.assign(s.window,{scrollTopPx:Math.round(pos),scrollMaxPx:Math.round(max),viewportPx:s.container.clientHeight,
   positionDeltaPx:Math.round(positionDelta*10)/10,extentDeltaPx:Math.round(extentDelta*10)/10,
   reason:!hadPrevious ? 'awaiting-content' : !sameList ? 'message-list-changing' : !sameContent ? 'body-or-structure-changing' : 'content-stable'});
  // Layout/scroll extent may change while exactly the same ID/role/body snapshot is readable.
  // Require repeated content, not pixel-perfect layout; edge observations remain separately gated.
  s.stable=sameContent ? s.stable+1 : 1;s.last=value;s.lastIds=identity;s.prevPos=pos;s.prevMax=max;
  if(s.stable<3) {
   if(performance.now()-s.stepStarted>=WINDOW_LIMIT_MS || performance.now()-s.readyStarted>READY_GRACE_MS) abort('H06_UNSETTLED');
   return JSON.stringify({waiting:true,coverage:stats()});
  }
  const upward=s.direction==='up',atEdge=upward?pos<=2:pos>=max-2;
  if(atEdge && !quietGeometry) s.window.reason='edge-layout-changing';
  if(!s.startObserved && !quietGeometry) s.window.reason='start-layout-changing';
  if(performance.now()-s.stepStarted>=WINDOW_LIMIT_MS) abort('H06_UNSETTLED');
  if(!s.startObserved) {
   const atStart=upward?pos>=max-2:pos<=2;
   if(!atStart) {
    s.container.scrollTo({top:upward?max:0,behavior:"instant"});s.steps++;resetWindow();
    s.window.reason='positioning-start';return JSON.stringify({waiting:true,coverage:stats()});
   }
   if(!quietGeometry) return JSON.stringify({waiting:true,coverage:stats()});
   s.startObserved=true;if(upward)s.bottom=true;else s.top=true;
  }
  // Larger steps are allowed only when adjacent stable windows share an ID.
  // Retry a smaller commanded step; never cache a disconnected window as progress.
  if(s.previousWindow && !rows.some(m=>s.previousWindow.has(m.id))) {
   if(!s.move || s.move.retries>=5 || Math.abs(s.move.target-s.move.from)<=1) abort('H03_ORDER');
   s.move.target=s.move.from+(s.move.target-s.move.from)/2;s.move.retries++;s.overlapRetries++;
   s.container.scrollTo({top:s.move.target,behavior:"instant"});s.steps++;resetWindow();
   s.window.reason='retrying-overlap';return JSON.stringify({waiting:true,coverage:stats()});
  }
  s.move=null;
  // Store strings immediately, never references to message DOM nodes that can be unmounted.
  let added=0;
  for(const m of rows) {
   const old=s.messages.get(m.id);
   if(old && (old.role!==m.role || old.messageType!==m.messageType || old.html!==m.html || old.markdown!==m.markdown)) abort('H04_CHANGED');
   if(!old) {
    const bytes=new TextEncoder().encode(JSON.stringify(m)).length;
    if(s.messages.size>=1000 || s.chars+m.markdown.length>2000000 || s.bytes+bytes>64*1024*1024) abort('H05_LIMIT');
    s.messages.set(m.id,m);s.edges.set(m.id,new Set());s.chars+=m.markdown.length;s.bytes+=bytes;added++;
   }
  }
  for(let i=1;i<rows.length;i++) s.edges.get(rows[i-1].id).add(rows[i].id);
  s.edgeStable=atEdge && added===0 && quietGeometry ? s.edgeStable+1 : 0;
  if(atEdge && s.edgeStable<5 && !quietGeometry) s.window.reason='edge-layout-changing';
  // Staying at an observed edge is only a traversal stopping condition, never completeness proof.
  if(atEdge && s.edgeStable>=5) {
   if(upward) s.top=true;else s.bottom=true;
   s.leg++;
   if(s.leg===1) {
    const degree=new Map([...s.messages.keys()].map(id=>[id,0]));
    for(const next of s.edges.values()) for(const id of next) degree.set(id,degree.get(id)+1);
    const ready=[...degree].filter(([,d])=>d===0).map(([id])=>id),ordered=[];
    while(ready.length) {
     if(ready.length!==1) abort('H03_ORDER');
     const id=ready.pop();ordered.push(s.messages.get(id));
     for(const next of s.edges.get(id)) {degree.set(next,degree.get(next)-1);if(degree.get(next)===0) ready.push(next);}
    }
    if(ordered.length!==s.messages.size) abort('H03_ORDER');
    const coverage={...stats(),secondPass:false,order:'consistent',text:'not-proven',attachmentMetadata:'not-audited',attachmentFiles:0,imageFiles:0};
    const result={done:true,title:s.title,messages:ordered,warnings:[
      '试用版：已单向滚动并缓存可见历史，完整历史仍未确认。到达边界或顺序一致不证明无遗漏。',
      '附件元数据覆盖未核实；附件原文件、图片原文件均未包含。',
      'Markdown根据渲染DOM转换，不保证原始写法。',...s.warnings],coverage};
    // Keep transport bounded too; no truncated JSON or successful partial file.
    const raw=JSON.stringify(result);if(raw.length>8*1024*1024) abort('H05_LIMIT');
    dispose(true);return raw;
   }
   resetWindow();
  } else if(!atEdge) {
   const target=pos+(upward?-1:1)*Math.max(1,s.container.clientHeight*0.8);
   s.previousWindow=new Set(rows.map(m=>m.id));
   s.move={from:pos,target:Math.max(0,Math.min(max,target)),retries:0};
   s.container.scrollTo({top:s.move.target,behavior:"instant"});s.steps++;
   if(Math.abs(s.container.scrollTop-pos)<0.5) abort('H01_SCROLL_CONTAINER');
   resetWindow();
  }
  return JSON.stringify({waiting:true,coverage:stats()});
 } catch(e) {return JSON.stringify(failure(e.code || 'H99_SCAN'));}
 function failure(code,dom) {
  const result={error:code};if(s) {result.coverage=stats();dispose(true);}if(dom) result.diagnostic=dom;return result;
 }
})(__NOVA_SCROLL_COMMAND__, __NOVA_SCROLL_TOKEN__)
