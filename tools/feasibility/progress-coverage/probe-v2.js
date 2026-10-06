(function() {
 'use strict';
 const started=performance.now();
 const result={source:'read-only-progress-discovery-v2',history:'not-proven',bodyIncluded:false,idsIncluded:false,truncated:false};
 const styleCache=new WeakMap();
 const style=e=>{if(!styleCache.has(e))styleCache.set(e,getComputedStyle(e));return styleCache.get(e);};
 const hidden=e=>{for(let n=e;n;n=n.parentElement){const css=style(n);if(n.hidden)return 'hidden';if(n.getAttribute('aria-hidden')==='true')return 'ariaHidden';if(css.display==='none')return 'displayNone';if(['hidden','collapse'].includes(css.visibility))return 'visibilityHidden';if(css.contentVisibility==='hidden')return 'contentVisibilityHidden';}return '';};
 const role=e=>{const r=e.getAttribute('data-message-author-role');return ['user','assistant','tool'].includes(r)?r:r===null?'absent':'other';};
 let checkedTextNodes=0;
 function textCounts(root,exclude) {
  const walk=document.createTreeWalker(root,NodeFilter.SHOW_TEXT);let nodes=0,chars=0;
  for(let n=walk.nextNode();n;n=walk.nextNode()) {
   if(++checkedTextNodes>20000 || performance.now()-started>2000)throw Error('Q03_LIMIT');
   const e=n.parentElement;
   if(!n.textContent.trim() || !e || e.closest('button,script,style,svg,input,textarea,select,iframe,noscript') || hidden(e) || exclude(e))continue;
   nodes++;chars+=n.textContent.trim().length;
  }
  return {nodes,chars};
 }
 try {
  if(window!==window.top || location.origin!=='https://chatgpt.com')throw Error('Q01_ORIGIN');
  if(!/^\/(?:c\/[^/]+|g\/[^/]+\/c\/[^/]+)\/?$/.test(location.pathname))throw Error('Q02_ROUTE');
  if(document.readyState!=='complete')throw Error('Q04_LOADING');
  const authors=[...document.querySelectorAll('[data-message-author-role]')];
  const turns=[...document.querySelectorAll('article[data-testid^="conversation-turn-"],article[data-turn]')];
  if(authors.length>1000 || turns.length>1000)throw Error('Q03_LIMIT');
  result.authors={raw:authors.length,supported:0,visibleSupported:0,user:0,assistant:0,other:0,missingId:0,outsideMarkdownTextNodes:0,outsideMarkdownChars:0,hidden:{}};
  result.authorSamples=[];
  for(const [index,e] of authors.entries()) {
   if(performance.now()-started>2000)throw Error('Q03_LIMIT');
   const r=role(e),why=hidden(e),supported=['user','assistant'].includes(r);
   if(supported)result.authors.supported++;else result.authors.other++;
   if(why)result.authors.hidden[why]=(result.authors.hidden[why]||0)+1;
   if(!supported || why)continue;
   result.authors.visibleSupported++;result.authors[r]++;
   const hasId=Boolean(e.getAttribute('data-message-id') || e.closest('[data-message-id]')?.getAttribute('data-message-id'));
   if(!hasId)result.authors.missingId++;
   const roots=[...e.querySelectorAll('.markdown')].filter(x=>!hidden(x) && !x.parentElement.closest('.markdown'));
   const outside=r==='assistant' && roots.length ? textCounts(e,x=>roots.some(root=>root.contains(x))) : {nodes:0,chars:0};
   result.authors.outsideMarkdownTextNodes+=outside.nodes;result.authors.outsideMarkdownChars+=outside.chars;
   if(outside.nodes && result.authorSamples.length<80)result.authorSamples.push({domIndex:index+1,role:r,hasId,markdownRoots:roots.length,outsideMarkdownTextNodes:outside.nodes,outsideMarkdownChars:outside.chars});
  }
  result.turns={raw:turns.length,visible:0,visibleWithoutSupportedAuthor:0,
   withOutsideAuthorText:0,outsideAuthorTextNodes:0,outsideAuthorChars:0};result.turnSamples=[];result.outsideAuthorSamples=[];
  for(const [index,e] of turns.entries()) {
   if(performance.now()-started>2000)throw Error('Q03_LIMIT');
   if(hidden(e))continue;result.turns.visible++;
   const selected=[...(e.hasAttribute('data-message-author-role')?[e]:[]),...e.querySelectorAll('[data-message-author-role]')].filter(x=>['user','assistant'].includes(role(x)) && !hidden(x));
   // A turn containing a final author may still have sibling progress text.
   // Count the uncovered region instead of skipping that entire turn.
   if(selected.length) {
    const outside=textCounts(e,x=>selected.some(author=>author.contains(x)));
    if(outside.nodes) {
     result.turns.withOutsideAuthorText++;result.turns.outsideAuthorTextNodes+=outside.nodes;result.turns.outsideAuthorChars+=outside.chars;
     const declared=e.getAttribute('data-turn');
     if(result.outsideAuthorSamples.length<80)result.outsideAuthorSamples.push({domIndex:index+1,
      declaredRole:['user','assistant'].includes(declared)?declared:declared===null?'absent':'other',
      supportedAuthors:selected.length,outsideAuthorTextNodes:outside.nodes,outsideAuthorChars:outside.chars});
    }
   }
   if(selected.length)continue;
   result.turns.visibleWithoutSupportedAuthor++;
   const text=textCounts(e,()=>false),declared=e.getAttribute('data-turn');
   if(result.turnSamples.length<80)result.turnSamples.push({domIndex:index+1,declaredRole:['user','assistant'].includes(declared)?declared:declared===null?'absent':'other',authorNodes:e.querySelectorAll('[data-message-author-role]').length,visibleTextNodes:text.nodes,visibleTextChars:text.chars});
  }
  result.elapsedMs=Math.round(performance.now()-started);return JSON.stringify(result);
 } catch(e) {return JSON.stringify({...result,error:e.message==='Q03_LIMIT'?'Q03_LIMIT':['Q01_ORIGIN','Q02_ROUTE','Q04_LOADING'].includes(e.message)?e.message:'Q99_PROBE',truncated:e.message==='Q03_LIMIT',elapsedMs:Math.round(performance.now()-started)});}
})()
