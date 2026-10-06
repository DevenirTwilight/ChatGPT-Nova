(function(query) {
 'use strict';
 const started=performance.now(),r={source:'read-only-text-locator',history:'not-proven',bodyIncluded:false,idsIncluded:false,truncated:false,matches:[]},sources=[];
 const normalize=s=>String(s).replace(/\s+/g,' ');
 const styles=new WeakMap(),css=e=>{if(!styles.has(e))styles.set(e,getComputedStyle(e));return styles.get(e);};
 const allowedTags=new Set('HTML BODY MAIN DIV SPAN P SECTION ARTICLE LI UL OL PRE CODE BLOCKQUOTE A STRONG EM B I DETAILS SUMMARY TABLE TR TD TH H1 H2 H3 H4 H5 H6'.split(' '));
 const value=(e,name,allowed)=>{const v=e?.getAttribute(name);return v==null?'absent':allowed.includes(v)?v:'other';};
 const flags=e=>{
  const f={ariaHidden:false,hiddenAttribute:false,cssHidden:false};
  for(let p=e;p;p=p.parentElement) {const s=css(p);f.ariaHidden ||= p.getAttribute('aria-hidden')==='true';f.hiddenAttribute ||= p.hidden;
   f.cssHidden ||= s.display==='none' || ['hidden','collapse'].includes(s.visibility) || s.contentVisibility==='hidden';}
  return f;
 };
 const excluded=f=>f.ariaHidden || f.hiddenAttribute || f.cssHidden;
 try {
  if(window!==window.top || location.origin!=='https://chatgpt.com')throw Error('L02_ORIGIN');
  if(!/^\/(?:c\/[^/]+|g\/[^/]+\/c\/[^/]+)\/?$/.test(location.pathname))throw Error('L02_ROUTE');
  if(document.readyState!=='complete' || !document.body)throw Error('L02_LOADING');
  const needle=normalize(query).trim();if(needle.length<4 || needle.length>160)throw Error('L02_QUERY');
  const authors=[...document.querySelectorAll('[data-message-author-role]')];
  r.authorNodes=authors.length;if(authors.length>1000)throw Error('L03_LIMIT');
  const walker=document.createTreeWalker(document.body,NodeFilter.SHOW_TEXT);
  let visited=0,chars=0,tail='',previousBlock=null,previousOwner=null,previousVisibility='';
  for(let n=walker.nextNode();n;n=walker.nextNode()) {
   if(++visited>50000 || performance.now()-started>2000)throw Error('L03_LIMIT');
   const e=n.parentElement;if(!e)continue;
   if(e.closest('button,script,style,svg,input,textarea,select,iframe,noscript,[role="button"]')) {tail='';continue;}
   const text=normalize(n.textContent);chars+=text.length;if(chars>2000000)throw Error('L03_LIMIT');
   const owner=e.closest('[data-message-author-role]'),block=e.closest('p,pre,li,td,th,h1,h2,h3,h4,h5,h6,blockquote,div,section,article') || e;
   const visibility=flags(e),key=JSON.stringify(visibility);
   if(block!==previousBlock || owner!==previousOwner || key!==previousVisibility)tail='';
   previousBlock=block;previousOwner=owner;previousVisibility=key;
   const joined=normalize(tail+text);let from=0,offset;
   while((offset=joined.indexOf(needle,from))!==-1) {
    if(r.matches.length>=20)throw Error('L03_LIMIT');from=offset+needle.length;
    const authorRole=value(owner,'data-message-author-role',['user','assistant','tool']);
    const supported=['user','assistant'].includes(authorRole),ownerFlags=owner?flags(owner):null;
    const roots=owner && authorRole==='assistant' ? [...owner.querySelectorAll('.markdown')].filter(x=>!excluded(flags(x)) && !x.parentElement.closest('.markdown')) : [];
    const insideMarkdown=roots.some(root=>root.contains(e));
    const chain=[];for(let p=e;p && chain.length<12;p=p.parentElement)chain.push({tag:allowedTags.has(p.tagName)?p.tagName:'OTHER',
     role:value(p,'data-message-author-role',['user','assistant','tool']),turn:value(p,'data-turn',['user','assistant']),
     channel:value(p,'data-message-channel',['commentary','final']),hasMessageId:p.hasAttribute('data-message-id'),
     hasTestId:p.hasAttribute('data-testid'),conversationTurnTestId:(p.getAttribute('data-testid') || '').startsWith('conversation-turn-'),markdown:p.classList.contains('markdown')});
    r.matches.push({authorIndex:owner?authors.indexOf(owner)+1:0,authorRole,authorSelected:supported && !excluded(ownerFlags),
     visibility,markdownRoots:roots.length,insideMarkdown,chain});
    sources.push(e.closest('[data-message-author-role],article[data-turn="assistant"]'));
   }
   tail=joined.slice(-(needle.length-1));
  }
  r.visitedTextNodes=visited;r.scannedChars=chars;r.code=r.matches.length?'L00_MATCH':'L01_NOT_FOUND';
  if(r.matches.length) {
   // Inspect the actual converter result locally; never return its text or IDs.
   const captured=JSON.parse(__NOVA_SNAPSHOT__),matchingIds=new Set();
   r.capture={code:captured.error || 'D00_CAPTURED',count:captured.messages?.length || 0,matchingMessageIndexes:[]};
   for(const [index,m] of (captured.messages || []).entries()) {
    const text=new DOMParser().parseFromString(m.html,'text/html').body.textContent;
    if(normalize(text).includes(needle)) {r.capture.matchingMessageIndexes.push(index+1);matchingIds.add(m.id);}
   }
   for(const [index,e] of sources.entries()) {
    const id=e?.getAttribute('data-message-id') || e?.closest('[data-message-id]')?.getAttribute('data-message-id');
    r.matches[index].exportSourceHasId=Boolean(id);
    r.matches[index].capturedHere=Boolean(id && matchingIds.has(id));
   }
  }
 } catch(e) {r.code=['L02_ORIGIN','L02_ROUTE','L02_LOADING','L02_QUERY','L03_LIMIT'].includes(e.message)?e.message:'L99_LOCATOR';r.truncated=r.code==='L03_LIMIT';}
 r.elapsedMs=Math.round(performance.now()-started);return JSON.stringify(r);
})(__NOVA_QUERY__)
