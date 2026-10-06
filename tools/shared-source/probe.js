(function () {
  'use strict';
  const u = new URL(location.href);
  if (u.origin !== 'https://chatgpt.com' || !/^\/share\/[^/]+\/?$/.test(u.pathname)) return JSON.stringify({error:'SHARE_ORIGIN'});
  const visible = n => {const s=getComputedStyle(n);return !n.closest('[hidden],[aria-hidden="true"]') && s.display!=='none' && s.visibility!=='hidden';};
  const nodes = [...document.querySelectorAll('[data-message-author-role]')].filter(n=>visible(n)&&['user','assistant'].includes(n.getAttribute('data-message-author-role')));
  const turns = [...document.querySelectorAll('[data-testid^="conversation-turn"]')].filter(visible);
  const roots = turns.length ? turns : nodes;
  const text = roots.map(n=>n.innerText).join('\n');
  let hash=2166136261;for(let i=0;i<text.length;i++){hash^=text.charCodeAt(i);hash=Math.imul(hash,16777619);}
  const loading=[...document.querySelectorAll('[role="progressbar"],[aria-busy="true"],[data-testid*="loading"]')].some(visible);
  const main=document.querySelector('main')||document.body;
  const unavailable = !roots.length && /shared (?:link|conversation).*(?:unavailable|not found)|unable to load conversation|此共享.*(?:不可用|不存在)/i.test(main.innerText);
  // A public shared page may contain a Log in button. It is not itself a login page.
  const loginPage = !roots.length && !!document.querySelector('input[type="password"],form[action*="auth"]');
  return JSON.stringify({ready:document.readyState==='complete',bodyPresent:roots.length>0,loading,unavailable,loginPage,count:nodes.length,turns:turns.length,chars:text.length,fingerprint:(hash>>>0).toString(16),roles:nodes.map(n=>n.getAttribute('data-message-author-role')),codeBlocks:main.querySelectorAll('pre').length,tables:main.querySelectorAll('table').length,math:main.querySelectorAll('.katex,math').length,images:main.querySelectorAll('img').length});
})()
