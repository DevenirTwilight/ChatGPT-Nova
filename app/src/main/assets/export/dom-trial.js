(function(verifyOnly) {
  'use strict';
  const MAX_MESSAGES = 1000, MAX_CHARS = 2000000;
  const diagnostic = {source:'read-only-dom-v3', history:'not-proven', count:0, chars:0, processed:0, authorFallbacks:0, turnFallbacks:0, explicitProgressBlocks:0,
    missingIds:0, duplicateIds:0, codeBlocks:0, tables:0, math:0, images:0, elapsedMs:0};
  const started = performance.now();
  const error = code => { throw Object.assign(new Error(code), {code}); };
  const esc = s => String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  const md = s => String(s).replace(/([\\`*_\[\]<>])/g,'\\$1');
  const warnings = new Set(['试用版仅导出当前页面已加载的消息；完整历史未确认。',
    'Markdown 根据渲染页面转换，不保证原始写法。附件文件不包含在导出中。']);
  const styles = new WeakMap();
  const style = e => {
    if (!styles.has(e)) styles.set(e,getComputedStyle(e));
    return styles.get(e);
  };
  // No layout-box test: display:contents and offscreen content-visibility:auto
  // can have readable loaded descendants without an element's own painted box.
  const hiddenReason = e => {
    for (let n=e;n && n.nodeType===Node.ELEMENT_NODE;n=n.parentElement) {
      if (n.hidden) return 'hidden';
      if (n.getAttribute('aria-hidden')==='true') return 'ariaHidden';
      const css=style(n);
      if (css.display==='none') return 'displayNone';
      if (css.visibility==='hidden' || css.visibility==='collapse') return 'visibilityHidden';
      if (css.contentVisibility==='hidden') return 'contentVisibilityHidden';
    }
    return '';
  };
  const visible = e => !hiddenReason(e);
  diagnostic.filtered = {hidden:0,ariaHidden:0,displayNone:0,visibilityHidden:0,contentVisibilityHidden:0,controls:0};
  function link(raw) {
    if (typeof raw !== 'string' || !raw.trim()) return null;
    try { const u = new URL(raw,location.href);
      return ['https:','http:','mailto:'].includes(u.protocol) && !u.username && !u.password ? u.href : null;
    } catch (_) { return null; }
  }
  function content(node) {
    if (node.nodeType === Node.TEXT_NODE) return {html:esc(node.textContent),markdown:md(node.textContent)};
    if (node.nodeType !== Node.ELEMENT_NODE) return {html:'',markdown:''};
    const tag = node.tagName;
    if (['BUTTON','SCRIPT','STYLE','SVG','INPUT','TEXTAREA','SELECT','IFRAME','NOSCRIPT','NAV'].includes(tag) || node.matches('[role="button"],[role="navigation"],[role="toolbar"]')) {diagnostic.filtered.controls++;return {html:'',markdown:''};}
    const reason=hiddenReason(node);
    if (reason) {diagnostic.filtered[reason]++;return {html:'',markdown:''};}
    if (tag === 'IMG') { diagnostic.images++; warnings.add('图片仅保留占位说明，未下载原始图片。');
      const text='[图片：'+(node.getAttribute('alt') || '未离线保存')+']'; return {html:esc(text),markdown:md(text)}; }
    if (node.classList.contains('katex')) {
      diagnostic.math++;
      const tex = node.querySelector('annotation[encoding="application/x-tex"]')?.textContent;
      if (tex) return {html:'<code>'+esc(tex)+'</code>',markdown:'$'+tex+'$'};
      warnings.add('部分公式未提供 TeX 源，保留可见文字。');
    }
    if (tag === 'PRE') {
      diagnostic.codeBlocks++;
      const code=node.querySelector('code'), text=(code || node).textContent;
      const match=(code?.className || '').match(/(?:^|\s)language-([\w+-]+)/);
      const fence='`'.repeat(Math.max(3,...(text.match(/`+/g) || []).map(s=>s.length+1)));
      return {html:'<pre><code>'+esc(text)+'</code></pre>',markdown:'\n\n'+fence+(match?.[1] || '')+'\n'+text+'\n'+fence+'\n\n'};
    }
    if (tag === 'DETAILS' && !node.open) {
      warnings.add('存在未展开折叠内容，请展开后重新导出以保留正文。');
      return {html:'<p>[折叠内容未展开]</p>',markdown:'\n\n[折叠内容未展开]\n\n'};
    }
    if (tag === 'TABLE') {
      diagnostic.tables++;
      const cells=[...node.rows].filter(visible).map(row=>[...row.cells].map(cell=>content(cell)));
      const width=Math.max(0,...cells.map(row=>row.length));
      const line=row=>'| '+Array.from({length:width},(_,i)=>(row[i]?.markdown || '').trim().replace(/\|/g,'\\|').replace(/\s*\n\s*/g,'<br>')).join(' | ')+' |';
      const lines=cells.map(line);
      if (lines.length) lines.splice(1,0,'| '+Array(width).fill('---').join(' | ')+' |');
      return {html:'<table>'+cells.map(row=>'<tr>'+row.map(cell=>'<td>'+cell.html+'</td>').join('')+'</tr>').join('')+'</table>',markdown:'\n\n'+lines.join('\n')+'\n\n'};
    }
    const parts=[...node.childNodes].map(content);
    let html=parts.map(p=>p.html).join(''), markdown=parts.map(p=>p.markdown).join('');
    if (tag === 'BR') return {html:'<br>',markdown:'\n'};
    if (tag === 'HR') return {html:'<hr>',markdown:'\n\n---\n\n'};
    if (tag === 'A') { const href=link(node.getAttribute('href'));
      return href ? {html:'<a href="'+esc(href)+'" rel="noreferrer noopener">'+html+'</a>',markdown:'['+markdown+'](<'+href.replace(/[<>\n\r]/g,encodeURIComponent)+'>)'} : {html,markdown}; }
    if (/^H[1-6]$/.test(tag)) return {html:'<'+tag.toLowerCase()+'>'+html+'</'+tag.toLowerCase()+'>',markdown:'\n\n'+'#'.repeat(Number(tag[1]))+' '+markdown.trim()+'\n\n'};
    if (['STRONG','B','EM','I','DEL','S','CODE','BLOCKQUOTE','UL','OL','LI','P'].includes(tag)) {
      const clean={B:'strong',I:'em',S:'del'}[tag] || tag.toLowerCase();
      html='<'+clean+'>'+html+'</'+clean+'>';
      if (['STRONG','B'].includes(tag)) markdown='**'+markdown+'**';
      else if (['EM','I'].includes(tag)) markdown='*'+markdown+'*';
      else if (['DEL','S'].includes(tag)) markdown='~~'+markdown+'~~';
      else if (tag === 'CODE') { const ticks='`'.repeat(Math.max(1,...(node.textContent.match(/`+/g)||[]).map(s=>s.length+1))); markdown=ticks+' '+node.textContent+' '+ticks; }
      else if (tag === 'BLOCKQUOTE') markdown='\n\n'+markdown.trim().split('\n').map(s=>'> '+s).join('\n')+'\n\n';
      else if (tag === 'LI') markdown='\n'+(node.parentElement.tagName==='OL' ? '1. ' : '- ')+markdown.trim();
      else if (['P','UL','OL'].includes(tag)) markdown='\n\n'+markdown.trim()+'\n\n';
    } else if (['DIV','SECTION','ARTICLE'].includes(tag)) { html='<div>'+html+'</div>'; markdown='\n'+markdown+'\n'; }
    return {html,markdown};
  }
  try {
    if (window !== window.top || location.origin !== 'https://chatgpt.com') error('D01_ORIGIN');
    if (!/^\/(?:c\/[^/]+|g\/[^/]+\/c\/[^/]+)\/?$/.test(location.pathname)) error('D02_ROUTE');
    if (document.readyState !== 'complete') error('D03_LOADING');
    if (document.querySelector('[data-testid="stop-button"], [data-is-streaming="true"]')) error('D04_STREAMING');
    const route=location.href;
    const authors=[...document.querySelectorAll('[data-message-author-role]')];
    const nodes=authors
      .filter(e=>['user','assistant'].includes(e.getAttribute('data-message-author-role')) && visible(e));
    // Only an explicitly assistant-labelled turn can supply a missing author.
    // Never infer a role from prose, status labels, an ordinal, or nearby messages.
    const turns=[...document.querySelectorAll('article[data-turn="assistant"],[data-turn="assistant"][data-testid^="conversation-turn-"]')];
    for (const e of turns) {
      if (!visible(e) || e.hasAttribute('data-message-author-role')
          || e.querySelector('[data-message-author-role]')
          || e.parentElement.closest('[data-message-author-role],article[data-turn="assistant"],[data-turn="assistant"][data-testid^="conversation-turn-"]')) continue;
      nodes.push(e);diagnostic.turnFallbacks++;
    }
    nodes.sort((a,b)=>a===b ? 0 : a.compareDocumentPosition(b)&Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1);
    diagnostic.authors={raw:authors.length,supported:authors.filter(e=>['user','assistant'].includes(e.getAttribute('data-message-author-role'))).length,
      visibleSupported:nodes.length-diagnostic.turnFallbacks};
    diagnostic.count=nodes.length;
    if (!nodes.length) error('D05_NO_MESSAGES');
    if (nodes.length>MAX_MESSAGES) error('D06_LIMIT');
    let hash=2166136261;
    const hashText=s=>{for(let i=0;i<s.length;i++) hash=Math.imul(hash ^ s.charCodeAt(i),16777619); hash=Math.imul(hash ^ 0,16777619);};
    hashText(route); hashText(document.title);
    const ids=new Set(), messages=[];
    for (const [index,e] of nodes.entries()) {
      if (e.querySelector('[data-message-author-role]')) error('D07_NESTED');
      const id=e.getAttribute('data-message-id') || e.closest('[data-message-id]')?.getAttribute('data-message-id') || '';
      if (!id) diagnostic.missingIds++;
      else if (ids.has(id)) { diagnostic.duplicateIds++; error('D08_DUPLICATE_ID'); }
      ids.add(id);
      const role=e.getAttribute('data-message-author-role') || e.getAttribute('data-turn');
      const channel=e.getAttribute('data-message-channel') || e.closest('article[data-message-channel]')?.getAttribute('data-message-channel');
      const messageType=role==='user' ? 'user' : channel==='commentary' ? 'assistant-progress' : channel==='final' ? 'assistant-final' : 'assistant-unknown';
      const candidates=role==='assistant' ? [...e.querySelectorAll('.markdown')] : [];
      const roots=candidates.filter(body=>visible(body) && !body.parentElement.closest('.markdown'));
      // Explicit commentary siblings outside final Markdown are readable content,
      // but without independent identity they remain part of this same message.
      const progress=[...e.querySelectorAll('[data-message-channel="commentary"]')].filter(body=>visible(body)
        && !roots.some(root=>root.contains(body) || body.contains(root))
        && !body.parentElement.closest('[data-message-channel="commentary"]'));
      diagnostic.explicitProgressBlocks+=progress.length;
      const bodies=channel==='commentary' || !roots.length ? [e]
        : [...roots,...progress].sort((a,b)=>a.compareDocumentPosition(b)&Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1);
      // Bound and verify the complete author subtree, including a safe fallback's source.
      const authorChars=e.textContent.length;
      diagnostic.chars+=authorChars;
      if (diagnostic.chars>MAX_CHARS) error('D06_LIMIT');
      hashText(id);hashText(role);hashText(messageType);hashText(e.outerHTML);
      if (!verifyOnly) {
        let parts=bodies.map(content);
        let markdown=parts.map(p=>p.markdown.trim()).filter(Boolean).join('\n\n');
        let fallbackTried=false;
        if (!markdown && roots.length) {
          // Same structural filtering, never raw textContent or hidden-content recovery.
          fallbackTried=true;parts=[content(e)];markdown=parts[0].markdown.trim();
          if (markdown) diagnostic.authorFallbacks++;
        }
        if (!markdown) {
          const css=style(e);
          diagnostic.failedMessage={index:index+1,role,authorChars,
            selectedChars:bodies.reduce((n,b)=>n+b.textContent.length,0),
            markdownCandidates:candidates.length,selectedBlocks:roots.length,
            display:css.display,visibility:css.visibility,contentVisibility:css.contentVisibility || 'unknown',
            hasOwnBox:e.getClientRects().length>0,fallbackTried,
            images:e.querySelectorAll('img').length,canvas:e.querySelectorAll('canvas').length,
            media:e.querySelectorAll('audio,video').length,details:e.querySelectorAll('details').length,
            reason:authorChars===0 ? 'no-dom-text' : 'no-readable-content-after-filtering'};
          error('D09_EMPTY_BODY');
        }
        messages.push({id,role,messageType,captureFallback:!e.hasAttribute('data-message-author-role') || fallbackTried,html:parts.map(p=>p.html).join(''),markdown});
      }
      diagnostic.processed++;
    }
    if (location.href !== route) error('D10_CHANGED');
    if (diagnostic.missingIds) warnings.add('部分消息没有公开 ID，无法按 ID 核对历史。');
    diagnostic.elapsedMs=Math.round(performance.now()-started);
    diagnostic.signature=(hash>>>0).toString(16);
    const result={diagnostic};
    if (!verifyOnly) Object.assign(result,{title:document.title || '未命名会话',messages,warnings:[...warnings]});
    const raw=JSON.stringify(result);
    if (raw.length>8*1024*1024) error('D06_LIMIT');
    return raw;
  } catch (e) {
    diagnostic.elapsedMs=Math.round(performance.now()-started);
    return JSON.stringify({error:e.code || 'D99_EXTRACT',diagnostic});
  }
})(__NOVA_VERIFY_ONLY__)
