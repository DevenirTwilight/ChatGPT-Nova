((snapshotId, expectedUrl) => {
  'use strict';
  const limit=12*1024*1024;
  const sourceUrl=location.href, title=document.title, capturedAt=new Date().toISOString();
  const allowed=u=>u.protocol==='https:' && u.hostname==='chatgpt.com' && !u.port && !u.username && !u.password;
  try {
    if(!allowed(new URL(sourceUrl)) || sourceUrl!==expectedUrl) return JSON.stringify({error:'S01_INVALID_PAGE'});
    // Capture public presentation/state before cloning. No asynchronous work and
    // no subsequent live-body reads: all conversions below use only the clone.
    const liveNodes=[...document.documentElement.querySelectorAll('*')];
    if(liveNodes.length>100000)return JSON.stringify({error:'S03_SNAPSHOT_TOO_LARGE'});
    const state=liveNodes.map(e=>{
      const s=getComputedStyle(e);
      return {hidden:e.hidden || e.getAttribute('aria-hidden')==='true' || s.display==='none' || s.visibility==='hidden' || s.contentVisibility==='hidden' || ((s.position==='absolute'||s.position==='fixed') && s.clip==='rect(0px, 0px, 0px, 0px)') || s.clipPath==='inset(50%)',
        scroll:(s.overflowY==='auto'||s.overflowY==='scroll') && e.scrollHeight>e.clientHeight,
        whiteSpace:s.whiteSpace, imageSrc:e.tagName==='IMG'?(e.currentSrc||e.getAttribute('src')):null,
        value:e.tagName==='TEXTAREA'?e.value:null,open:e.tagName==='DETAILS'?e.open:null};
    });
    const clone=document.documentElement.cloneNode(true);
    const nodes=[...clone.querySelectorAll('*')];
    let removed=0;
    const metadata={source:'frozen-reading-page-v2',scope:'currently-loaded-page',history:'not-proven',canvas:clone.querySelectorAll('canvas').length,
      shadowDomNotSerialized:true,staticResourcesMayChange:true};
    const safeUrl=(raw, image=false)=>{
      try {const u=new URL(raw,sourceUrl);if(u.username||u.password)return '';
        if(image&&/\/(?:backend-api|api)(?:\/|$)/.test(u.pathname))return '';
        if(u.protocol==='https:' || (image && /^data:image\/(?:png|jpeg|gif|webp|avif);/i.test(raw))) return u.href;
      }catch(_){}return '';
    };
    // Runtime JS/shadow/canvas state cannot be represented by cloneNode. Inputs
    // may contain credentials, so do not serialize their current values.
    nodes.forEach((e,i)=>{
      const tag=e.tagName.toUpperCase(), st=state[i];
      if(st.imageSrc)e.setAttribute('src',st.imageSrc);
      if(/pre/.test(st.whiteSpace)&&tag!=='PRE'&&tag!=='CODE')e.setAttribute('data-nova-lines','true');
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
      if(['SCRIPT','IFRAME','FRAME','OBJECT','EMBED','APPLET','BASE','NOSCRIPT','ANIMATE','ANIMATETRANSFORM','ANIMATEMOTION','SET'].includes(tag)
        || (tag==='META' && /^(?:refresh|content-security-policy|set-cookie)$/i.test(e.getAttribute('http-equiv')||''))
        || (tag==='LINK' && ((e.getAttribute('rel')||'').toLowerCase()!=='stylesheet' || !e.getAttribute('href')))) {e.remove();removed++;}
    });
    const head=clone.querySelector('head'),body=clone.querySelector('body');
    if(!head||!body)throw Error('structure');
    const escape=s=>s.replace(/([\\`*_\[\]])/g,'\\$1');
    const excluded=e=>e.nodeType===1&&(e.hasAttribute('data-nova-hidden')||e.hidden||['BUTTON','INPUT','TEXTAREA','SELECT','OPTION','SCRIPT','STYLE','META','LINK','SVG','CANVAS','NAV','HEADER','FOOTER','FORM'].includes(e.tagName.toUpperCase())||['button','menu','menubar','toolbar','navigation'].includes(e.getAttribute('role')));
    const publicRole=n=>n.getAttribute('data-message-author-role')||((/^(user|assistant)$/.test(n.getAttribute('data-turn')||'')&&/^conversation-turn/.test(n.getAttribute('data-testid')||'')&&!n.querySelector('[data-message-author-role]'))?n.getAttribute('data-turn'):null);
    const children=e=>[...e.childNodes].map(convert).join('');
    const plain=e=>[...e.childNodes].map(n=>n.nodeType===3?n.nodeValue:excluded(n)?'':plain(n)).join('');
    const convert=n=>{
      if(n.nodeType===3)return escape(n.parentElement?.closest('[data-nova-lines],pre')?n.nodeValue:n.nodeValue.replace(/\s+/g,' '));
      if(n.nodeType!==1||excluded(n))return '';
      const tag=n.tagName.toUpperCase();
      const tex=n.querySelector('annotation[encoding="application/x-tex"]');
      if(tex && (n.classList.contains('katex')||tag==='MATH'))return '$'+tex.textContent+'$';
      if(tag==='PRE'){const code=n.querySelector('code')||n, text=plain(code).replace(/\n$/,'');
        const runs=text.match(/`+/g)||[], fence='`'.repeat(Math.max(3,...runs.map(x=>x.length+1)));
        const lang=(code.className||'').match(/language-([a-zA-Z0-9_+-]+)/);return '\n\n'+fence+(lang?lang[1]:'')+'\n'+text+'\n'+fence+'\n\n';}
      if(tag==='CODE'){const text=plain(n),fence='`'.repeat(Math.max(1,...(text.match(/`+/g)||[]).map(x=>x.length+1)));return fence+' '+text+' '+fence;}
      if(tag==='IMG'){const u=n.getAttribute('src');return u?'!['+escape(n.alt||'图片')+'](<'+u.replace(/>/g,'%3E')+'>)':'[图片：'+escape(n.alt||'图片')+'。临时资源未保存]';}
      if(tag==='TABLE') {const rows=[...n.querySelectorAll('tr')].filter(r=>r.closest('table')===n&&!excluded(r)).map(r=>[...r.children].filter(c=>/^(TH|TD)$/.test(c.tagName)&&!excluded(c)).map(c=>children(c).trim().replace(/\|/g,'\\|').replace(/\n+/g,'<br>')));
        if(!rows.length)return '';const cols=Math.max(...rows.map(r=>r.length)),line=r=>'| '+Array.from({length:cols},(_,i)=>r[i]||'').join(' | ')+' |';return '\n\n'+line(rows[0])+'\n'+line(Array(cols).fill('---'))+'\n'+rows.slice(1).map(line).join('\n')+'\n\n';}
      const text=children(n);
      if(/^H[1-6]$/.test(tag))return '\n\n'+'#'.repeat(+tag[1])+' '+text.trim()+'\n\n';
      if(tag==='BR')return '  \n';if(tag==='HR')return '\n\n---\n\n';
      if(tag==='STRONG'||tag==='B')return '**'+text+'**';if(tag==='EM'||tag==='I')return '*'+text+'*';
      if(tag==='A'){const u=n.getAttribute('href');return u?'['+text.trim()+'](<'+u.replace(/>/g,'%3E')+'>)':text;}
      if(tag==='BLOCKQUOTE')return '\n\n'+text.trim().split('\n').map(x=>'> '+x).join('\n')+'\n\n';
      if(tag==='UL'||tag==='OL'){return '\n\n'+[...n.children].filter(e=>e.tagName==='LI'&&!excluded(e)).map((li,i)=>(tag==='OL'?String((+n.getAttribute('start')||1)+i)+'. ':'- ')+children(li).trim().replace(/\n/g,'\n  ')).join('\n')+'\n\n';}
      if(tag==='DETAILS'&&!n.open)return children(n.querySelector('summary')||n.cloneNode(false));
      const role=publicRole(n);
      if(role==='user'||role==='assistant')return '\n\n## '+(role==='user'?'用户':'助手')+'\n\n'+text+'\n\n';
      if(['P','DIV','SECTION','ARTICLE','MAIN','DL','DT','DD'].includes(tag))return '\n\n'+text.trim()+'\n\n';
      return text;
    };
    const reading=body.querySelector('main')||body.querySelector('[role="main"]')||body;
    const markdown='# '+title.replace(/[\r\n]/g,' ')+'\n\n> 保存时间：'+capturedAt+'\n> 来源：当前网页冻结快照\n> 本文件只反映保存时网页已经加载的内容，不声明服务器端完整会话历史。\n\n'+convert(reading).trim()+'\n';
    // Derive a reading-only DOM from the already frozen clone. Never clone/read live again.
    const htmlReading=reading.cloneNode(true);
    for(const n of [...htmlReading.querySelectorAll('.katex,math')]){const tex=n.querySelector('annotation[encoding="application/x-tex"]');if(tex)n.textContent=tex.textContent;}
    const unsafe=['button','input','textarea','select','option','script','style','link','meta','nav','header','footer','svg','canvas','iframe','object','embed','form','[data-nova-hidden]','[role="button"]','[role="menu"]','[role="menubar"]','[role="toolbar"]','[role="navigation"]'];
    htmlReading.querySelectorAll(unsafe.join(',')).forEach(n=>n.remove());
    for(const n of [...htmlReading.querySelectorAll('*')]) {
      const role=publicRole(n);
      const lines=n.hasAttribute('data-nova-lines');
      const tex=n.querySelector('annotation[encoding="application/x-tex"]');
      if(tex&&(n.classList.contains('katex')||n.tagName==='MATH')){n.textContent=tex.textContent;}
      for(const a of [...n.attributes]) {
        const keep=(n.tagName==='A'&&a.name==='href')||(n.tagName==='IMG'&&['src','alt','width','height'].includes(a.name))||(['TH','TD'].includes(n.tagName)&&['colspan','rowspan'].includes(a.name))||(n.tagName==='OL'&&a.name==='start')||(n.tagName==='DETAILS'&&a.name==='open');
        if(!keep)n.removeAttribute(a.name);
      }
      if(lines)n.classList.add('preserve-lines');
      if(role==='user'||role==='assistant'){n.classList.add('message',role);const label=document.createElement('h2');label.className='author';label.textContent=role==='user'?'用户':'助手';n.prepend(label);}
      if(n.tagName==='IMG'&&!n.getAttribute('src')){const note=document.createElement('span');note.textContent='[图片：'+(n.getAttribute('alt')||'图片')+'。临时资源未保存]';n.replaceWith(note);}
    }
    // No inherited SPA CSS, attributes, script, runtime state or credential-bearing controls.
    const readingDocument=document.implementation.createHTMLDocument(title);
    const h=readingDocument.head,b=readingDocument.body;
    const charset=readingDocument.createElement('meta');charset.setAttribute('charset','utf-8');h.prepend(charset);
    const viewport=readingDocument.createElement('meta');viewport.name='viewport';viewport.content='width=device-width, initial-scale=1';h.append(viewport);
    const csp=readingDocument.createElement('meta');csp.httpEquiv='Content-Security-Policy';csp.content="default-src 'none'; script-src 'none'; style-src 'unsafe-inline'; img-src https: data:; connect-src 'none'; frame-src 'none'; object-src 'none'; form-action 'none'; base-uri 'none'";h.append(csp);
    const css=readingDocument.createElement('style');css.textContent="html{color-scheme:light;background:#fff;color:#161616}body{max-width:860px;margin:32px auto;padding:0 24px;font:16px/1.65 system-ui,-apple-system,\"Segoe UI\",sans-serif;overflow-wrap:anywhere}h1,h2,h3,h4,h5,h6{line-height:1.3;margin:1.2em 0 .6em}h1{font-size:1.65em}h2.author{font-size:.9em;color:#555;margin:.2em 0 1em}.snapshot-note,footer{font-size:.85em;color:#555}footer{border-top:1px solid #ddd;margin-top:2em;padding-top:1em}.message{padding:18px 20px;margin:22px 0;border:1px solid #ddd;border-radius:8px}.message.user{background:#f5f7fa}p{margin:.8em 0}.preserve-lines{white-space:pre-wrap}pre{font:13px/1.5 ui-monospace,SFMono-Regular,Consolas,monospace;white-space:pre;overflow-x:auto;background:#f5f5f5;border:1px solid #ddd;border-radius:4px;padding:14px}code{font-family:ui-monospace,SFMono-Regular,Consolas,monospace}pre code{white-space:inherit}table{display:block;overflow-x:auto;max-width:100%;border-collapse:collapse;margin:1em 0}th,td{border:1px solid #ccc;padding:6px 10px;text-align:left;vertical-align:top}blockquote{border-left:3px solid #bbb;margin:1em 0;padding:0 1em;color:#333}img{max-width:100%;height:auto}a{color:#0755a1;text-decoration:underline}hr{border:0;border-top:1px solid #ccc;margin:1.5em 0}@page{margin:16mm}@media print{html,body{background:#fff!important;color:#000!important}body{max-width:none;margin:0;padding:0;font-size:11pt;line-height:1.5}.message{background:#fff!important;border:0;border-top:1px solid #ddd;border-radius:0;padding:12pt 0;margin:12pt 0;break-inside:auto}h1,h2,h3,h4,h5,h6{break-after:avoid}p{orphans:3;widows:3}pre{white-space:pre-wrap;overflow:visible;overflow-wrap:anywhere;word-break:break-word;break-inside:auto;background:#fff;border:1px solid #ccc;font-size:9pt}table{display:table;table-layout:fixed;width:100%;overflow:visible;break-inside:auto;font-size:9pt}th,td{overflow-wrap:anywhere;word-break:break-word}thead{display:table-header-group}tr{break-inside:avoid}blockquote{break-inside:auto}img{break-inside:avoid;max-height:min(240mm,80vh);object-fit:contain}a{color:#000}footer,.snapshot-note{color:#333}}";h.append(css);
    const heading=readingDocument.createElement('h1');heading.textContent=title;b.append(heading);
    const info=readingDocument.createElement('p');info.className='snapshot-note';info.textContent='保存时间：'+capturedAt+'。当前网页冻结快照，仅包含保存时已加载的内容。';b.append(info);
    // Move children, not the source container's SPA-specific layout attributes.
    const main=readingDocument.createElement('main');while(htmlReading.firstChild)main.append(htmlReading.firstChild);b.append(main);
    const footer=readingDocument.createElement('footer');footer.textContent='某些外部图片或临时资源可能需要网络连接。未挂载的较早内容不会包含在本快照中；这不是完整服务器会话备份。';b.append(footer);
    const frozenHtml='<!doctype html>\n'+readingDocument.documentElement.outerHTML;
    if(location.href!==sourceUrl||document.title!==title)return JSON.stringify({error:'S08_PAGE_CHANGED_BEFORE_SNAPSHOT'});
    metadata.removedExecutableNodes=removed;metadata.htmlChars=frozenHtml.length;metadata.markdownChars=markdown.length;
    metadata.limitations=['canvas bitmap not captured','shadow DOM not serialized','runtime JS state not captured','remote static resource bytes not frozen','virtualized unloaded history not captured'];
    const payload=JSON.stringify({snapshotId,capturedAt,sourceUrl,title,baseUrl:sourceUrl,frozenHtml,markdown,metadata});
    if(payload.length>limit)return JSON.stringify({error:'S03_SNAPSHOT_TOO_LARGE'});
    return payload;
  }catch(_){return JSON.stringify({error:'S02_SNAPSHOT_FAILED'});}
})(__NOVA_SNAPSHOT_ID__, __NOVA_EXPECTED_URL__)
