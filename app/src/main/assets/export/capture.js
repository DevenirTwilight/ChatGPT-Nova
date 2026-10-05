(() => {
  if (window !== window.top || location.origin !== 'https://chatgpt.com' || window.__novaExportCapture) return;
  const original = window.fetch;
  const state = window.__novaExportCapture = {tree:null, id:null};
  const currentId = () => location.pathname.match(/\/c\/([a-zA-Z0-9-]+)\/?$/)?.[1];
  // Observe only the current conversation response. Never inspect request headers.
  window.fetch = async function (...args) {
    const response = await original.apply(this,args);
    try {
      const url = new URL(typeof args[0] === 'string' || args[0] instanceof URL ? String(args[0]) : args[0].url,location.href);
      const id = currentId();
      if (id && url.origin === location.origin && url.pathname === '/backend-api/conversation/' + id && response.ok) {
        response.clone().text().then(raw => {
          if (raw.length > 16 * 1024 * 1024 || currentId() !== id) return;
          const tree = JSON.parse(raw);
          if (tree.conversation_id === id && tree.mapping) { state.tree=tree; state.id=id; }
        }).catch(()=>{});
      }
    } catch (_) { /* Preserve the website's request/result behavior. */ }
    return response;
  };
  // Resolve the website's already loaded, read-only full-conversation reader.
  // The website handles authorization/project context; Nova never reads credentials.
  // Discover by behavior markers rather than changing CDN hashes or minified names.
  const pageReader = async () => {
    const urls = [...new Set(performance.getEntriesByType('resource').map(e=>e.name))]
      .filter(name => {
        try {
          const u=new URL(name);
          return (u.origin===location.origin || u.origin==='https://cdn.oaistatic.com') &&
            /\/(?:cdn\/)?assets\/conversation-small-[a-zA-Z0-9_-]+\.js$/.test(u.pathname);
        } catch (_) { return false; }
      }).slice(-3);
    for (const url of urls) {
      try {
        const exports=await import(url);
        const readers=[...new Set(Object.values(exports))].filter(value=> {
          if (typeof value!=='function') return false;
          const source=Function.prototype.toString.call(value);
          return source.includes('includeFullConversation') && source.includes('forceNetworkFetch') &&
            source.includes('/conversation/{conversation_id}') && source.includes('onConversationLoadedFromNetwork');
        });
        if (readers.length===1) return readers[0];
      } catch (_) { /* Unsupported site build: fail closed or use a verified response. */ }
    }
    return null;
  };
  // Used only by an explicit export gesture; no session/token endpoints.
  window.__novaReadConversation = async id => {
    const abort = new AbortController(), timeout = setTimeout(()=>abort.abort(),15000);
    try {
      if (id!==currentId()) throw new Error('会话发生变化');
      const reader=await pageReader();
      if (reader) {
        let rawTree=null;
        const result=await reader(id,{includeFullConversation:true,forceNetworkFetch:true,signal:abort.signal,
          onConversationLoadedFromNetwork:tree=>{rawTree=tree;}});
        const tree=rawTree || result;
        if (currentId()===id && tree?.conversation_id===id && tree.mapping) {
          if (JSON.stringify(tree).length>16*1024*1024) throw new Error('会话数据过大');
          state.tree=tree; state.id=id;
          return tree;
        }
        state.tree=null; state.id=null;
        throw new Error('网页完整会话读取结果不匹配');
      }
      const response = await original('/backend-api/conversation/' + encodeURIComponent(id) + '?include_full_conversation=true',
        {credentials:'same-origin',cache:'no-store',signal:abort.signal});
      if (response.ok) {
        const raw = await response.text();
        if (raw.length > 16 * 1024 * 1024) throw new Error('会话数据过大');
        return JSON.parse(raw);
      }
    } catch (_) {} finally { clearTimeout(timeout); }
    if (state.id === id && state.tree) return state.tree;
    throw new Error('无法确认完整会话：未能通过网页的完整会话读取流程取得消息树。请刷新当前会话后重试；不会导出仅已加载的内容。');
  };
})();
