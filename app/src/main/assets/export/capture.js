(() => {
  if (window !== window.top || location.origin !== 'https://chatgpt.com' || window.__novaExportCapture) return;
  const original = window.fetch;
  const state = window.__novaExportCapture = {tree:null, id:null};
  const currentId = () => location.pathname.match(/\/c\/([a-zA-Z0-9-]+)\/?$/)?.[1];
  // Observe only the current conversation response. Never inspect request headers.
  window.fetch = async function (...args) {
    const response = await original.apply(this,args);
    try {
      const url = new URL(typeof args[0] === 'string' ? args[0] : args[0].url,location.href);
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
  // Used only by an explicit export gesture; no session/token endpoints.
  window.__novaReadConversation = async id => {
    const abort = new AbortController(), timeout = setTimeout(()=>abort.abort(),15000);
    try {
      const response = await original('/backend-api/conversation/' + encodeURIComponent(id) + '?include_full_conversation=true',
        {credentials:'same-origin',cache:'no-store',signal:abort.signal});
      if (response.ok) {
        const raw = await response.text();
        if (raw.length > 16 * 1024 * 1024) throw new Error('会话数据过大');
        return JSON.parse(raw);
      }
    } catch (_) {} finally { clearTimeout(timeout); }
    if (state.id === id && state.tree) return state.tree;
    throw new Error('无法确认完整会话：页面未提供完整消息树。请刷新当前会话后重试；不会导出仅已加载的内容。');
  };
})();
