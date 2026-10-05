/* Normalized current-branch export. No account, cookie, or storage access. */
(function (root) {
  'use strict';
  const fail = detail => { throw new Error('无法确认完整会话：' + detail); };
  const esc = s => String(s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  function normalize(tree, conversationId, visibleIds) {
    if (!tree || tree.conversation_id !== conversationId || !tree.mapping || !tree.current_node)
      fail('当前会话消息树不可用');
    const map = tree.mapping, ids = [...new Set(visibleIds)];
    if (typeof map !== 'object' || Array.isArray(map)) fail('当前会话消息树不可用');
    const nodeAt = id => typeof id === 'string' ? Object.getOwnPropertyDescriptor(map,id)?.value : undefined;
    if (!nodeAt(tree.current_node)) fail('消息树末端缺失');
    if (!ids.length || ids.some(id => !nodeAt(id))) fail('无法核对页面消息 ID');
    // Empty children alone cannot prove a leaf when another mapped node names
    // it as a parent. Check both directions before accepting either terminal.
    const mappedParents = new Set(Object.keys(map).map(id=>nodeAt(id)?.parent).filter(id=>id!=null));
    const isLeaf = id => {
      const node = nodeAt(id);
      return !!node && Array.isArray(node.children) && node.children.length === 0 && !mappedParents.has(id);
    };
    // A DOM subset is only evidence of the selected branch, never its content source.
    // Server current_node may retain a different regenerated reply. The page may
    // select another terminal leaf only when the complete tree proves it has no
    // descendants. A scrolled-up ancestor cannot select a truncated branch.
    let selected = tree.current_node;
    if (!isLeaf(selected)) fail('消息树末端不是完整叶节点');
    let terminal = selected;
    const hiddenSeen = new Set();
    while (nodeAt(terminal) && !displayable(nodeAt(terminal).message)) {
      if (hiddenSeen.has(terminal)) fail("末尾消息循环");
      hiddenSeen.add(terminal);
      terminal = nodeAt(terminal).parent;
      if (!terminal) fail('没有可导出的消息');
    }

    if (ids[ids.length - 1] !== terminal) {
      const leaf = ids[ids.length - 1], node = nodeAt(leaf);
      if (!displayable(node?.message) || !isLeaf(leaf))
        fail('当前分支末尾无法确认，请滚动到末尾，等待回复完成或刷新后重试');
      selected = leaf;
    }
    const chain = [], seen = new Set();
    let id = selected;
    while (id != null) {
      const node = nodeAt(id);
      if (seen.has(id) || !node || node.id !== id) fail('消息链断裂或循环');
      seen.add(id);
      const parent = nodeAt(node.parent);
      if (node.parent != null && (!parent || !Array.isArray(parent.children)
          || !parent.children.includes(id))) fail('父子关系不完整');
      chain.push(node);
      id = node.parent;
    }
    const rootNode = chain[chain.length - 1];
    if (rootNode.message && rootNode.message.author?.role !== 'system') fail('无法确认消息链起点');
    if (tree.has_missing_conversation_data || tree.is_partial || tree.has_more) fail('消息树标记为不完整');
    if (ids.some(id => !seen.has(id))) fail('页面包含另一分支的消息');
    chain.reverse();
    const order = new Map(chain.map((node,index)=>[node.id,index]));
    if (ids.some((id,index)=>index>0 && order.get(id)<=order.get(ids[index-1]))) fail('页面消息顺序不一致');
    const warnings = [], messages = [];
    for (const node of chain) {
      const m = node.message;
      if (m?.author?.role === 'assistant' && m.status && m.status !== 'finished_successfully')
        fail('当前分支仍有未完成的回复');
      if (!displayable(m)) continue;
      if (m.id !== node.id || m.status !== 'finished_successfully') fail('消息未完成或格式无法验证');
      const content = m.content;
      if (!content || !['text','multimodal_text'].includes(content.content_type) || !Array.isArray(content.parts))
        fail('不支持的消息类型，无法可靠保留正文');
      const parts = content.parts.map(part => {
        if (typeof part === 'string') return part;
        if (part && part.content_type === 'image_asset_pointer') {
          warnings.push('部分原始图片只有资源指针，无法离线获取；已保留占位说明。');
          return '[图片：当前页面未提供可直接保存的原始资源]';
        }
        fail('消息含无法可靠保存的内容');
      });
      if (m.metadata && m.metadata.attachments && m.metadata.attachments.length) {
        warnings.push('附件文件不包含在导出中；附件中的正文无法确认完整。');
        fail('当前分支包含附件，无法确认附件正文完整');
      }
      messages.push({id:m.id, role:m.author.role, evidenceMarkdown:parts.join('\n\n'), markdown:withSources(parts.join('\n\n'),m.metadata,warnings)});
    }
    if (!messages.length) fail('会话为空');
    return {conversationId, title:tree.title || '未命名会话', messages, warnings:[...new Set(warnings)],
      completeness:{root:chain[0].id, terminal:selected, nodes:chain.length, messages:messages.length}};
  }
  function withSources(text, metadata, warnings) {
    const entries = [...(Array.isArray(metadata?.citations) ? metadata.citations : []),
      ...(Array.isArray(metadata?.content_references) ? metadata.content_references : [])];
    const urls = new Map();
    for (const reference of entries) {
      const sources = [reference, reference?.metadata,
        ...(Array.isArray(reference?.items) ? reference.items : []),
        ...(Array.isArray(reference?.sources) ? reference.sources : [])];
      const links = [];
      for (const source of sources) {
        const url = safeUrl(source?.url, false);
        if (!url) continue;
        const title = String(source.title || source.name || url).replace(/[\[\]\r\n]/g,' ');
        const link = '[' + title + '](<' + url.replace(/[<>]/g,encodeURIComponent) + '>)';
        urls.set(url,link); links.push(link);
      }
      if (reference?.matched_text && links.length) text = text.split(reference.matched_text).join(links.join(' '));
    }
    if (urls.size) text += '\n\n### Sources\n\n' + [...urls.values()].map(link=>'- '+link).join('\n');
    if (/\uE200cite/.test(text)) warnings.push('部分网页引用未提供可验证 URL；已保留引用原文。');
    return text;
  }
  // Preserve LaTeX as readable source without external script/font dependencies.
  // Register once on this isolated renderer; never change the website's parser.
  function renderMarkdown(text, marked, evidence = false) {
    if (!marked.__novaMath) {
      marked.use({extensions:[
        {name:'novaMathBlock',level:'block',start:src=>src.indexOf('$$'),
          tokenizer:src=> { const match=/^\$\$[ \t]*\n([\s\S]*?)\n\$\$(?:\n|$)/.exec(src); return match && {type:'novaMathBlock',raw:match[0],text:match[1]}; },
          renderer:token=>'<pre><code>'+esc(marked.__novaMathEvidence ? token.text : '$$\n'+token.text+'\n$$')+'</code></pre>'},
        {name:'novaMathInline',level:'inline',start:src=>src.indexOf('$'),
          tokenizer:src=> { const match=/^\$(?!\$)([^\n$]+)\$/.exec(src); return match && {type:'novaMathInline',raw:match[0],text:match[1]}; },
          renderer:token=>'<code>'+esc(marked.__novaMathEvidence ? token.text : token.raw)+'</code>'},
        {name:'novaMathBracketBlock',level:'block',start:src=>src.indexOf('\\['),
          tokenizer:src=> { const match=/^\\\[[ \t]*\n?([\s\S]*?)\\\](?:[ \t]*\n|$)/.exec(src); return match && {type:'novaMathBracketBlock',raw:match[0],text:match[1]}; },
          renderer:token=>'<pre><code>'+esc(marked.__novaMathEvidence ? token.text : token.raw)+'</code></pre>'},
        {name:'novaMathParenInline',level:'inline',start:src=>src.indexOf('\\('),
          tokenizer:src=> { const match=/^\\\(([^\n]*?)\\\)/.exec(src); return match && {type:'novaMathParenInline',raw:match[0],text:match[1]}; },
          renderer:token=>'<code>'+esc(marked.__novaMathEvidence ? token.text : token.raw)+'</code>'}
      ]});
      marked.__novaMath=true;
    }
    const previous=marked.__novaMathEvidence; marked.__novaMathEvidence=evidence;
    try { return marked.parse(text,{gfm:true}); } finally { marked.__novaMathEvidence=previous; }
  }
  function displayable(m) {
    return !!(m && m.author && ['user','assistant'].includes(m.author.role)
      && m.channel !== 'analysis' && (!m.recipient || m.recipient === 'all') && !(m.metadata && m.metadata.is_visually_hidden_from_conversation));
  }
  function closeFence(text) {
    let fence = null;
    for (const line of text.split(/\r?\n/)) {
      const match = /^ {0,3}(`{3,}|~{3,})(.*)$/.exec(line);
      if (!match) continue;
      const marker=match[1], rest=match[2];
      if (!fence) {
        if (marker[0]==='`' && rest.includes('`')) continue;
        fence={char:marker[0],length:marker.length};
      } else if (marker[0]===fence.char && marker.length>=fence.length && !rest.trim()) fence=null;
    }
    return fence ? text+'\n'+fence.char.repeat(fence.length) : text;
  }
  function markdown(data) {
    return '# ' + data.title.replace(/[\r\n]/g, ' ') + '\n\n' +
      data.messages.map(m => '## ' + (m.role === 'user' ? 'User' : 'Assistant') + '\n\n' + closeFence(m.markdown)).join('\n\n---\n\n') +
      (data.warnings.length ? '\n\n---\n\n导出说明：\n' + data.warnings.map(s=>'- '+s).join('\n') : '') + '\n';
  }
  function safeUrl(value, image) {
    if (typeof value !== 'string' || !value.trim()) return null;
    try {
      const u = new URL(value, 'https://chatgpt.com/');
      if (u.username || u.password) return null;
      return (image ? u.protocol === 'https:' || /^data:image\/(png|jpeg|webp|gif);base64,/i.test(value)
        : ['https:','http:','mailto:'].includes(u.protocol)) ? u.href : null;
    } catch (_) { return null; }
  }
  function clean(html, document) {
    const parsed = document.createElement('template'); parsed.innerHTML = html;
    const allowed = new Set('P BR STRONG EM DEL H1 H2 H3 H4 H5 H6 UL OL LI BLOCKQUOTE PRE CODE TABLE THEAD TBODY TR TH TD A IMG HR SUP SUB'.split(' '));
    for (const el of [...parsed.content.querySelectorAll('*')]) {
      if (!allowed.has(el.tagName)) { el.replaceWith(document.createTextNode(el.textContent || '')); continue; }
      const href = el.getAttribute('href'), src = el.getAttribute('src'), alt = el.getAttribute('alt');
      const start = el.getAttribute('start');
      for (const a of [...el.attributes]) el.removeAttribute(a.name);
      if (el.tagName === 'A' && safeUrl(href, false)) {
        el.setAttribute('href',safeUrl(href,false)); el.setAttribute('rel','noreferrer noopener');
      }
      if (el.tagName === 'IMG') {
        const url = safeUrl(src,true);
        if (url) { el.setAttribute('src',url); el.setAttribute('alt',alt || '图片'); }
        else el.replaceWith(document.createTextNode('[图片无法安全保存]'));
      }
      if (el.tagName === 'OL' && /^\d+$/.test(start || '')) el.setAttribute('start',start);
    }
    for (const table of parsed.content.querySelectorAll('table')) {
      const wrap = document.createElement('div'); wrap.className='table-scroll'; table.replaceWith(wrap); wrap.append(table);
    }
    return parsed.innerHTML;
  }
  const css = `:root{color-scheme:light dark;--bg:#f6f5f1;--paper:#fff;--text:#24272b;--muted:#626b75;--line:#dce0e3;--user:#edf4f4;--link:#196caa}*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:17px/1.75 system-ui,-apple-system,sans-serif;overflow-wrap:anywhere}main{max-width:860px;margin:32px auto;padding:36px;background:var(--paper);border-radius:16px}header{border-bottom:1px solid var(--line);padding-bottom:24px}h1{font-size:1.8em;line-height:1.3}h2,h3,h4,h5,h6{line-height:1.4;margin-top:1.6em}article{padding:22px 0;border-bottom:1px solid var(--line)}article.user{background:var(--user);padding:20px;border-radius:10px;margin-top:24px}.role{font-size:.78em;letter-spacing:.06em;color:var(--muted);font-weight:700}a{color:var(--link);overflow-wrap:anywhere}pre{overflow-x:auto;white-space:pre;padding:16px;background:var(--bg);border:1px solid var(--line);border-radius:8px;font-size:.85em;line-height:1.5}code{font-family:ui-monospace,SFMono-Regular,Consolas,monospace}p code,li code{background:var(--bg);padding:2px 4px;border-radius:3px}blockquote{margin-left:0;border-left:3px solid var(--line);padding-left:18px;color:var(--muted)}.table-scroll{overflow-x:auto}table{border-collapse:collapse;font-size:.9em;min-width:100%}th,td{border:1px solid var(--line);padding:8px 12px;text-align:left}img{max-width:100%;height:auto}footer{font-size:.8em;color:var(--muted);margin-top:24px}@media(max-width:600px){main{margin:0;padding:20px 16px;border-radius:0}body{font-size:16px}article.user{padding:14px}}@media(prefers-color-scheme:dark){:root{--bg:#171b20;--paper:#20262c;--text:#e2e7eb;--muted:#aab6bf;--line:#3b454e;--user:#25383c;--link:#8bcaff}}@media print{:root{color-scheme:light;--bg:#fff;--paper:#fff;--text:#111;--muted:#444;--line:#ccc;--user:#f4f6f6;--link:#174d75}body{font-size:11pt}main{margin:0;padding:0;max-width:none}pre{white-space:pre-wrap;overflow:visible;overflow-wrap:anywhere;font-size:8pt}.table-scroll{overflow:visible}table{width:100%;table-layout:fixed}td,th{overflow-wrap:anywhere;padding:5px}tr,img{break-inside:avoid}h1,h2,h3,.role{break-after:avoid}article{break-inside:auto}a{color:var(--link)}@page{size:A4;margin:16mm}}`;
  function html(data, marked, document) {
    const body = data.messages.map(m => '<article class="' + m.role + '"><div class="role">' +
      (m.role === 'user' ? 'User' : 'ChatGPT') + '</div>' + clean(renderMarkdown(m.markdown,marked),document) + '</article>').join('');
    return '<!doctype html><html lang="zh"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">' +
      '<meta http-equiv="Content-Security-Policy" content="default-src \'none\'; style-src \'unsafe-inline\'; img-src data:; base-uri \'none\'; form-action \'none\'">' +
      '<title>'+esc(data.title)+'</title><style>'+css+'</style></head><body><main><header><h1>'+esc(data.title)+
      '</h1><div class="role">ChatGPT · '+data.messages.length+' 条消息</div></header>'+body+
      '<footer>'+data.warnings.map(esc).join('<br>')+'</footer></main></body></html>';
  }
  const api = {normalize, markdown, html, safeUrl, clean, displayable, renderMarkdown};
  if (typeof module !== 'undefined') module.exports = api;
  else root.NovaExportCore = api;
})(typeof globalThis !== 'undefined' ? globalThis : this);
