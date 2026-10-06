(function(command, token) {
 'use strict';
 const KEY='__novaHistoryScrollTrial';
 const snapshot=()=>JSON.parse(__NOVA_SNAPSHOT__);
 const abort=code=>{throw Object.assign(new Error(code),{code});};
 let s=window[KEY];
 function dispose(restore) {
  if(!s) return;
  s.observer?.disconnect();document.removeEventListener('click',s.interact,true);document.removeEventListener('keydown',s.interact,true);
  if(restore && s.container?.isConnected) s.container.scrollTop=s.original;
  s.messages.clear();s.edges.clear();delete window[KEY];
 }
 function stats() {return {source:'scroll-dom-trial',history:'not-proven',count:s.messages.size,
  users:[...s.messages.values()].filter(m=>m.role==='user').length,assistants:[...s.messages.values()].filter(m=>m.role==='assistant').length,
  steps:s.steps,leg:s.leg,topObserved:s.top,bottomObserved:s.bottom,secondPass:s.leg>=2,
  chars:s.chars,cacheBytes:s.bytes,elapsedMs:Math.round(performance.now()-s.started)};}
 try {
  if(command==='cancel') {if(s?.token===token) dispose(true);return JSON.stringify({cancelled:true});}
  if(command==='start') {
   if(s) abort('H04_BUSY');
   const first=snapshot();if(first.error) return JSON.stringify(first);
   const authors=[...document.querySelectorAll('[data-message-author-role]')].filter(e=>['user','assistant'].includes(e.dataset.messageAuthorRole)
    && !e.closest('[hidden],[aria-hidden="true"]'));
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
    messages:new Map(),edges:new Map(),second:new Set(),warnings:new Set(),chars:0,bytes:0,steps:0,leg:0,top:false,bottom:false,stable:0,edgeStable:0,last:'',lastPosition:'',invalid:false};
   window[KEY]=s;
   s.interact=e=>{if(e.isTrusted) s.invalid=true;};
   document.addEventListener('click',s.interact,true);document.addEventListener('keydown',s.interact,true);
   // Public DOM observation catches SPA changes without wrapping history/fetch or reading React.
   s.observer=new MutationObserver(()=>{if(location.href!==s.route || !s.container.isConnected) s.invalid=true;});
   s.observer.observe(document.documentElement,{subtree:true,childList:true,attributes:true});
  }
  if(!s || s.token!==token) abort('H04_SESSION');
  if(s.invalid || location.href!==s.route || !s.container.isConnected) abort('H04_CHANGED');
  if(performance.now()-s.started>120000 || s.steps>=300) abort('H05_LIMIT');
  const data=snapshot();
  if(data.error) {if(['D03_LOADING','D05_NO_MESSAGES'].includes(data.error) && performance.now()-s.stepStarted<5000)
    return JSON.stringify({waiting:true,coverage:stats()});
   return JSON.stringify(failure(data.error,data.diagnostic));}
  for(const warning of data.warnings || []) if(!warning.startsWith("试用版仅导出当前页面")) s.warnings.add(warning);
  const rows=data.messages;
  if(rows.some(m=>!m.id)) abort('H02_MISSING_ID');
  const value=JSON.stringify(rows);
  const pos=s.container.scrollTop,max=Math.max(0,s.container.scrollHeight-s.container.clientHeight);
  const position=pos.toFixed(1)+':'+max.toFixed(1);
  s.stable=(value===s.last && position===s.lastPosition) ? s.stable+1 : 1;s.last=value;s.lastPosition=position;
  if(s.stable<3) {
   if(performance.now()-s.stepStarted>5000) abort('H06_UNSETTLED');
   return JSON.stringify({waiting:true,coverage:stats()});
  }
  // Store strings immediately, never references to message DOM nodes that can be unmounted.
  let added=0;
  for(const m of rows) {
   const old=s.messages.get(m.id);
   if(old && (old.role!==m.role || old.html!==m.html || old.markdown!==m.markdown)) abort('H04_CHANGED');
   if(!old) {
    if(s.leg>=2) abort('H04_SECOND_PASS');
    const bytes=new TextEncoder().encode(JSON.stringify(m)).length;
    if(s.messages.size>=1000 || s.chars+m.markdown.length>2000000 || s.bytes+bytes>64*1024*1024) abort('H05_LIMIT');
    s.messages.set(m.id,m);s.edges.set(m.id,new Set());s.chars+=m.markdown.length;s.bytes+=bytes;added++;
   }
   if(s.leg>=2) s.second.add(m.id);
  }
  for(let i=1;i<rows.length;i++) s.edges.get(rows[i-1].id).add(rows[i].id);
  const upward=s.leg%2===0,atEdge=upward?pos<=1:pos>=max-1;
  s.edgeStable=atEdge && added===0 ? s.edgeStable+1 : 0;
  // Staying at an observed edge is only a traversal stopping condition, never completeness proof.
  if(atEdge && s.edgeStable>=5) {
   if(upward) s.top=true;else s.bottom=true;
   s.leg++;
   if(s.leg===4) {
    if(s.second.size!==s.messages.size) abort('H04_SECOND_PASS');
    const degree=new Map([...s.messages.keys()].map(id=>[id,0]));
    for(const next of s.edges.values()) for(const id of next) degree.set(id,degree.get(id)+1);
    const ready=[...degree].filter(([,d])=>d===0).map(([id])=>id),ordered=[];
    while(ready.length) {
     if(ready.length!==1) abort('H03_ORDER');
     const id=ready.pop();ordered.push(s.messages.get(id));
     for(const next of s.edges.get(id)) {degree.set(next,degree.get(next)-1);if(degree.get(next)===0) ready.push(next);}
    }
    if(ordered.length!==s.messages.size) abort('H03_ORDER');
    const coverage={...stats(),secondPass:true,order:'consistent',text:'not-proven',attachmentMetadata:'not-audited',attachmentFiles:0,imageFiles:0};
    const result={done:true,title:s.title,messages:ordered,warnings:[
      '试用版：已上下滚动并缓存可见历史，完整历史仍未确认。到达顶部或两轮一致不证明无遗漏。',
      '附件元数据覆盖未核实；附件原文件、图片原文件均未包含。',
      'Markdown根据渲染DOM转换，不保证原始写法。',...s.warnings],coverage};
    // Keep transport bounded too; no truncated JSON or successful partial file.
    const raw=JSON.stringify(result);if(raw.length>8*1024*1024) abort('H05_LIMIT');
    dispose(true);return raw;
   }
   s.edgeStable=0;s.stable=0;s.last='';s.stepStarted=performance.now();
  } else if(!atEdge) {
   const target=pos+(upward?-1:1)*Math.max(1,s.container.clientHeight*0.45);
   s.container.scrollTop=Math.max(0,Math.min(max,target));s.steps++;
   if(Math.abs(s.container.scrollTop-pos)<0.5) abort('H01_SCROLL_CONTAINER');
   s.stable=0;s.last='';s.stepStarted=performance.now();
  }
  return JSON.stringify({waiting:true,coverage:stats()});
 } catch(e) {return JSON.stringify(failure(e.code || 'H99_SCAN'));}
 function failure(code,dom) {
  const result={error:code};if(s) {result.coverage=stats();dispose(true);}if(dom) result.diagnostic=dom;return result;
 }
})(__NOVA_SCROLL_COMMAND__, __NOVA_SCROLL_TOKEN__)
