((snapshotId, expectedUrl) => {
  'use strict';
  const limit=12*1024*1024;
  const sourceUrl=location.href, title=document.title, capturedAt=new Date().toISOString();
  const allowed=u=>u.protocol==='https:' && u.hostname==='chatgpt.com' && !u.port && !u.username && !u.password;
  try {
    if(!allowed(new URL(sourceUrl)) || sourceUrl!==expectedUrl) return JSON.stringify({error:'F01_ORIGIN'});
    // Capture public presentation/state before cloning. No asynchronous work and
    // no subsequent live-body reads: all conversions below use only the clone.
    const state=[...document.documentElement.querySelectorAll('*')].map(e=>{
      const s=getComputedStyle(e);
      return {hidden:e.hidden || e.getAttribute('aria-hidden')==='true' || s.display==='none' || s.visibility==='hidden' || s.contentVisibility==='hidden',
        scroll:(s.overflowY==='auto'||s.overflowY==='scroll') && e.scrollHeight>e.clientHeight,
        value:e.tagName==='TEXTAREA'?e.value:null,open:e.tagName==='DETAILS'?e.open:null};
    });
    const clone=document.documentElement.cloneNode(true);
    const nodes=[...clone.querySelectorAll('*')];
    let removed=0;
    const metadata={source:'frozen-page-v1',scope:'currently-loaded-page',history:'not-proven',canvas:clone.querySelectorAll('canvas').length,
      shadowDomNotSerialized:true,staticResourcesMayChange:true};
    const safeUrl=(raw, image=false)=>{
      try {const u=new URL(raw,sourceUrl);if(u.username||u.password)return '';
        if(u.protocol==='https:' || (image && /^data:image\/(?:png|jpeg|gif|webp|avif);/i.test(raw))) return u.href;
      }catch(_){}return '';
    };
    // Runtime JS/shadow/canvas state cannot be represented by cloneNode. Inputs
    // may contain credentials, so do not serialize their current values.
    nodes.forEach((e,i)=>{
      const tag=e.tagName, st=state[i];
      if(st.hidden)e.setAttribute('data-nova-hidden','true');
      if(st.scroll)e.setAttribute('data-nova-scroll','true');
      if(st.value!==null)e.textContent=st.value;
      if(st.open!==null)e.toggleAttribute('open',st.open);
      for(const a of [...e.attributes]) {
        const n=a.name.toLowerCase();
        if(n.startsWith('on') || n==='srcdoc' || n==='nonce' || n==='integrity' || n==='autofocus' || n==='ping' || n==='formaction' || n==='action')e.removeAttribute(a.name);
        if(['href','src','poster','xlink:href'].includes(n)) {const u=safeUrl(a.value,tag==='IMG'||n==='poster');if(u)e.setAttribute(a.name,u);else e.removeAttribute(a.name);}
        if(n==='srcset')e.removeAttribute(a.name); // preserve the existing src; no new responsive selection
      }
      if(tag==='INPUT'){e.removeAttribute('value');e.removeAttribute('checked');}
      if(tag==='FORM')e.removeAttribute('action');
      if(['SCRIPT','IFRAME','FRAME','OBJECT','EMBED','APPLET','BASE','NOSCRIPT'].includes(tag)
        || (tag==='META' && /^(?:refresh|content-security-policy|set-cookie)$/i.test(e.getAttribute('http-equiv')||''))
        || (tag==='LINK' && (e.rel.toLowerCase()!=='stylesheet' || !e.getAttribute('href')))) {e.remove();removed++;}
    });
    const head=clone.querySelector('head'),body=clone.querySelector('body');
    if(!head||!body)throw Error('structure');
    const base=document.createElement('base');base.href=sourceUrl;head.prepend(base);
    const csp=document.createElement('meta');csp.httpEquiv='Content-Security-Policy';
    csp.content="default-src 'none'; script-src 'none'; style-src 'unsafe-inline' https:; img-src https: data:; font-src https: data:; connect-src 'none'; frame-src 'none'; object-src 'none'; form-action 'none'; base-uri https:";head.prepend(csp);
    const css=document.createElement('style');css.textContent='*{animation:none!important;transition:none!important}[data-nova-scroll]{overflow:visible!important;height:auto!important;max-height:none!important;content-visibility:visible!important} @media print{html,body{height:auto!important;overflow:visible!important}pre{white-space:pre-wrap!important;overflow-wrap:anywhere}table{max-width:100%}img{max-width:100%}button,input,textarea,select,[role=menu],[role=toolbar]{display:none!important}}';head.append(css);
    const escape=s=>s.replace(/([\\`*_\[\]])/g,'\\$1');
    const excluded=e=>e.nodeType===1&&(e.hasAttribute('data-nova-hidden')||e.hidden||['BUTTON','INPUT','TEXTAREA','SELECT','OPTION','SCRIPT','STYLE','META','LINK','SVG','CANVAS','NAV','HEADER','FOOTER'].includes(e.tagName)||['button','menu','menubar','toolbar','navigation'].includes(e.getAttribute('role')));
    const children=e=>[...e.childNodes].map(convert).join('');
    const plain=e=>[...e.childNodes].map(n=>n.nodeType===3?n.nodeValue:excluded(n)?'':plain(n)).join('');
    const convert=n=>{
      if(n.nodeType===3)return escape(n.nodeValue.replace(/\s+/g,' '));
      if(n.nodeType!==1||excluded(n))return '';
      const tag=n.tagName;
      const tex=n.querySelector('annotation[encoding="application/x-tex"]');
      if(tex && (n.classList.contains('katex')||tag==='MATH'))return '$'+tex.textContent+'$';
      if(tag==='PRE'){const code=n.querySelector('code')||n, text=plain(code).replace(/\n$/,'');
        const runs=text.match(/`+/g)||[], fence='`'.repeat(Math.max(3,...runs.map(x=>x.length+1)));
        const lang=(code.className||'').match(/language-([a-zA-Z0-9_+-]+)/);return '\n\n'+fence+(lang?lang[1]:'')+'\n'+text+'\n'+fence+'\n\n';}
      if(tag==='CODE'){const text=plain(n),fence='`'.repeat(Math.max(1,...(text.match(/`+/g)||[]).map(x=>x.length+1)));return fence+' '+text+' '+fence;}
      if(tag==='IMG'){const u=n.getAttribute('src');return u?'!['+escape(n.alt||'图片')+'](<'+u.replace(/>/g,'%3E')+'>)':'';}
      if(tag==='TABLE') {const rows=[...n.querySelectorAll('tr')].filter(r=>r.closest('table')===n).map(r=>[...r.children].filter(c=>/^(TH|TD)$/.test(c.tagName)).map(c=>children(c).trim().replace(/\|/g,'\\|').replace(/\n+/g,'<br>')));
        if(!rows.length)return '';const cols=Math.max(...rows.map(r=>r.length)),line=r=>'| '+Array.from({length:cols},(_,i)=>r[i]||'').join(' | ')+' |';return '\n\n'+line(rows[0])+'\n'+line(Array(cols).fill('---'))+'\n'+rows.slice(1).map(line).join('\n')+'\n\n';}
      const text=children(n);
      if(/^H[1-6]$/.test(tag))return '\n\n'+'#'.repeat(+tag[1])+' '+text.trim()+'\n\n';
      if(tag==='BR')return '  \n';if(tag==='HR')return '\n\n---\n\n';
      if(tag==='STRONG'||tag==='B')return '**'+text+'**';if(tag==='EM'||tag==='I')return '*'+text+'*';
      if(tag==='A'){const u=n.getAttribute('href');return u?'['+text.trim()+'](<'+u.replace(/>/g,'%3E')+'>)':text;}
      if(tag==='BLOCKQUOTE')return '\n\n'+text.trim().split('\n').map(x=>'> '+x).join('\n')+'\n\n';
      if(tag==='UL'||tag==='OL'){return '\n\n'+[...n.children].filter(e=>e.tagName==='LI').map((li,i)=>(tag==='OL'?String((+n.getAttribute('start')||1)+i)+'. ':'- ')+children(li).trim().replace(/\n/g,'\n  ')).join('\n')+'\n\n';}
      if(tag==='DETAILS'&&!n.open)return children(n.querySelector('summary')||n.cloneNode(false));
      const role=n.getAttribute('data-message-author-role');
      if(role==='user'||role==='assistant')return '\n\n## '+(role==='user'?'用户':'助手')+'\n\n'+text+'\n\n';
      if(['P','DIV','SECTION','ARTICLE','MAIN','DL','DT','DD'].includes(tag))return '\n\n'+text.trim()+'\n\n';
      return text;
    };
    const reading=body.querySelector('main')||body.querySelector('[role="main"]')||body;
    const markdown='# '+title.replace(/[\r\n]/g,' ')+'\n\n> 保存时间：'+capturedAt+'\n> 来源：当前网页冻结快照\n> 本文件只反映保存时网页已经加载的内容，不声明服务器端完整会话历史。\n\n'+convert(reading).trim()+'\n';
    const frozenHtml='<!doctype html>\n'+clone.outerHTML;
    if(location.href!==sourceUrl||document.title!==title)return JSON.stringify({error:'F02_CHANGED'});
    metadata.removedExecutableNodes=removed;metadata.htmlChars=frozenHtml.length;metadata.markdownChars=markdown.length;
    metadata.limitations=['canvas bitmap not captured','shadow DOM not serialized','runtime JS state not captured','remote static resource bytes not frozen','virtualized unloaded history not captured'];
    const payload=JSON.stringify({snapshotId,capturedAt,sourceUrl,title,baseUrl:sourceUrl,frozenHtml,markdown,metadata});
    if(payload.length>limit)return JSON.stringify({error:'F03_LIMIT'});
    return payload;
  }catch(_){return JSON.stringify({error:'F04_CAPTURE'});}
})(__NOVA_SNAPSHOT_ID__, __NOVA_EXPECTED_URL__)
