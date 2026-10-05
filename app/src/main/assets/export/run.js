(async () => {
  try {
    if (window !== window.top || location.origin !== 'https://chatgpt.com') throw new Error('请在 ChatGPT 会话页面导出');
    const startUrl = location.href, id = location.pathname.match(/\/c\/([a-zA-Z0-9-]+)\/?$/)?.[1];
    if (!id) throw new Error('无法确认完整会话：请打开已保存的会话');
    const visible = () => [...document.querySelectorAll('[data-message-id][data-message-author-role]')]
      .filter(el => ['user','assistant'].includes(el.getAttribute('data-message-author-role')))
      .map(el=>el.getAttribute('data-message-id'));
    // IDs alone do not detect streaming or edits that keep the same message ID.
    const evidenceText = () => [...document.querySelectorAll('[data-message-id][data-message-author-role]')]
      .filter(el => ['user','assistant'].includes(el.getAttribute('data-message-author-role')))
      .map(el=>el.textContent || '');
    const before = visible(), beforeText=JSON.stringify(evidenceText());
    if (document.querySelector('[data-testid="stop-button"]')) throw new Error('请等待回复完成后再导出');
    const tree = await window.__novaReadConversation(id);
    if (location.href !== startUrl || (JSON.stringify(before) !== JSON.stringify(visible()) || beforeText !== JSON.stringify(evidenceText()))) throw new Error('会话发生变化，请重试');
    const data = NovaExportCore.normalize(tree,id,before);
    // Require the terminal rendered message's text to agree with the tree, too.
    const last = data.messages[data.messages.length-1];
    const rendered = document.querySelector('[data-message-id="'+CSS.escape(last.id)+'"]');
    const temp = document.createElement('div');
    temp.innerHTML = NovaExportCore.clean(NovaExportCore.renderMarkdown(last.evidenceMarkdown.replace(/\uE200[^\uE201]*\uE201/g,'NOVA_REFERENCE_BREAK'),marked,true),document);
    const text = s=>s.replace(/\s+/g,'').trim();
    const evidence=rendered?.cloneNode(true);
    if (evidence) {
      for (const control of evidence.querySelectorAll('button,[role="button"]')) control.remove();
      // Site code blocks may wrap their body with a language label/toolbar.
      // Compare the code body rather than those presentation controls.
      for (const block of evidence.querySelectorAll('pre')) {
        const code=block.querySelector('code');
        if (code) block.replaceChildren(document.createTextNode(code.textContent));
      }
      for (const math of evidence.querySelectorAll('.katex-display')) {
        const source=math.querySelector('annotation[encoding="application/x-tex"]');
        if (source) math.replaceWith(document.createTextNode(source.textContent));
      }
      for (const math of evidence.querySelectorAll('.katex')) {
        const source=math.querySelector('annotation[encoding="application/x-tex"]');
        if (source) math.replaceWith(document.createTextNode(source.textContent));
      }
    }
    const actual=text(evidence?.textContent || '');
    let position=0;
    const parts=temp.textContent.split('NOVA_REFERENCE_BREAK');
    const matches=parts.length===1 ? actual===text(temp.textContent) : parts.every(part=> {
      const expected=text(part), index=actual.indexOf(expected,position);
      if (index<0) return false; position=index+expected.length; return true;
    });
    if (!evidence || !matches)
      throw new Error('无法确认完整会话：末条消息内容与消息树不同');
    let html = NovaExportCore.html(data,marked,document);
    const parsed = new DOMParser().parseFromString(html,'text/html');
    let bytes = 0;
    for (const image of parsed.images) {
      const src = image.getAttribute('src');
      if (/^data:image\/(png|jpeg|webp|gif);base64,/i.test(src)) continue;
      // Only embed already loaded page images. Never fetch arbitrary model-written URLs.
      const ids=new Set(data.messages.map(m=>m.id));
      const live = [...document.querySelectorAll('[data-message-id] img')].find(i=>
        ids.has(i.closest('[data-message-id]').getAttribute('data-message-id')) && i.src === src && i.complete && i.naturalWidth);
      try {
        if (!live) throw new Error();
        const canvas=document.createElement('canvas');
        if (live.naturalWidth*live.naturalHeight > 16000000) throw new Error();
        canvas.width=live.naturalWidth; canvas.height=live.naturalHeight;
        canvas.getContext('2d').drawImage(live,0,0);
        const uri=canvas.toDataURL('image/png'); bytes+=uri.length;
        if (bytes > 8*1024*1024) throw new Error();
        image.setAttribute('src',uri);
      } catch (_) {
        image.replaceWith(parsed.createTextNode('[图片未能离线保存]'));
        data.warnings.push('图片因未加载、跨域或大小限制未能离线保存；Markdown 保留原始引用。');
      }
    }
    data.warnings=[...new Set(data.warnings)];
    parsed.querySelector('footer').textContent=data.warnings.join(' ');
    html='<!doctype html>'+parsed.documentElement.outerHTML;
    if (location.href !== startUrl || (JSON.stringify(before) !== JSON.stringify(visible()) || beforeText !== JSON.stringify(evidenceText()))) throw new Error('会话发生变化，请重试');
    const raw=JSON.stringify({title:data.title,html,markdown:NovaExportCore.markdown(data),warnings:data.warnings,proof:data.completeness});
    if (raw.length>16*1024*1024) throw new Error('会话过大，无法安全导出');
    // Chunking avoids a single oversized WebMessage/JavaScript result.
    for (let offset=0;offset<raw.length;offset+=48000)
      NovaConversationExport.postMessage(JSON.stringify({nonce:__NOVA_NONCE__,chunk:raw.slice(offset,offset+48000),done:offset+48000>=raw.length}));
  } catch (error) {
    NovaConversationExport.postMessage(JSON.stringify({nonce:__NOVA_NONCE__,error:String(error.message || error)}));
  }
})();
