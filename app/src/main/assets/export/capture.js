(() => {
  if (window !== window.top || location.origin !== 'https://chatgpt.com' || window.__novaExportCapture) return;
  const original = window.fetch;
  const state = window.__novaExportCapture = {tree:null, id:null, generation:0};
  const diagnostics={modules:0,imports:0,candidates:0,reader:'not-found',http:'not-requested'};
  const moduleUrls=new Set();
  const remember=name=> {
    try {
      const u=new URL(name,location.href);
      if ((u.origin===location.origin || u.origin==='https://cdn.oaistatic.com') &&
          /\/(?:cdn\/)?assets\/conversation(?:-small)?-[a-zA-Z0-9_-]+\.js$/.test(u.pathname)) moduleUrls.add(u.href);
    } catch (_) {}
  };
  // ResourceTiming's default buffer can omit late conversation imports.
  // Record only public conversation module URLs, never request metadata.
  performance.getEntriesByType('resource').forEach(e=>remember(e.name));
  try {new PerformanceObserver(list=>list.getEntries().forEach(e=>remember(e.name))).observe({type:'resource',buffered:true});} catch (_) {}
  const currentId = () => location.pathname.match(/\/c\/([a-zA-Z0-9-]+)\/?$/)?.[1];
  // Observe only the current conversation response. Never inspect request headers.
  window.fetch = async function (...args) {
    const generation=state.generation;
    const response = await original.apply(this,args);
    try {
      const url = new URL(typeof args[0] === 'string' || args[0] instanceof URL ? String(args[0]) : args[0].url,location.href);
      const id = currentId();
      if (id && url.origin === location.origin && url.pathname === '/backend-api/conversation/' + id &&
          url.searchParams.get('include_full_conversation') === 'true' && response.ok) {
        response.clone().text().then(raw => {
          if (raw.length > 16 * 1024 * 1024 || currentId() !== id || generation!==state.generation) return;
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
  // AbortSignal alone cannot settle an import or a site reader that ignores it.
  const bounded = (operation, signal) => new Promise((resolve,reject) => {
    const aborted=()=>reject(new DOMException('Export read timed out','AbortError'));
    if (signal.aborted) { aborted(); return; }
    signal.addEventListener('abort',aborted,{once:true});
    Promise.resolve().then(operation).then(resolve,reject).finally(()=>signal.removeEventListener('abort',aborted));
  });
  const category = error => Number.isInteger(error?.status) && error.status>=100 && error.status<=599 ?
    String(error.status) : (['TypeError','SyntaxError','AbortError'].includes(error?.name) ? error.name : 'error');
  const pageReader = async signal => {
    performance.getEntriesByType('resource').forEach(e=>remember(e.name));
    document.querySelectorAll('script[src],link[rel="modulepreload"]').forEach(e=>remember(e.src || e.href));
    const urls=[...moduleUrls].slice(-6);
    diagnostics.modules=urls.length;
    for (const url of urls) {
      try {
        const exports=await bounded(()=>import(url),signal);
        diagnostics.imports++;
        const readers=[...new Set(Object.values(exports))].filter(value=> {
          if (typeof value!=='function') return false;
          const source=Function.prototype.toString.call(value);
          return source.includes('includeFullConversation') && source.includes('forceNetworkFetch') &&
            source.includes('onConversationLoadedFromNetwork');
        });
        diagnostics.candidates+=readers.length;
        if (readers.length===1) return readers[0];
      } catch (error) {
        diagnostics.reader='import-'+category(error);
        if (signal.aborted) throw error;
      }
    }
    return null;
  };
  // Some site builds retain the raw tree in current-message React props.
  // Inspect only explicit conversation fields on that message's ancestors; do
  // not crawl global stores, hooks, account providers or unrelated React branches.
  const pageTree = id => {
    const anchors=[...document.querySelectorAll('[data-message-id][data-message-author-role]')].slice(-2);
    const seen=new Set();
    const own=(object,key)=>object && typeof object==='object' ? Object.getOwnPropertyDescriptor(object,key)?.value : undefined;
    for (const anchor of anchors) {
      const key=Object.getOwnPropertyNames(anchor).find(k=>k.startsWith('__reactFiber$'));
      let fiber=key ? own(anchor,key) : null;
      for (let depth=0;fiber && depth<100 && !seen.has(fiber);depth++,fiber=own(fiber,'return')) {
        seen.add(fiber);
        const props=own(fiber,'memoizedProps');
        for (const name of ['conversation','conversationData','initialConversation','serverConversation']) {
          const tree=own(props,name);
          if (own(tree,'conversation_id')===id && own(tree,'mapping') && own(tree,'current_node')) {
            if (JSON.stringify(tree).length<=16*1024*1024) return tree;
          }
        }
      }
    }
    return null;
  };
  // Mirror the website's route-derived context for project/shared conversations.
  // A project URL contains a public g-p identifier followed by an optional slug.
  // Pass it to the page reader; only the website builds authenticated headers.
  const routeContext = () => {
    const context={}, project=location.pathname.match(/\/g\/(g-p-[\w-]+)/)?.[1];
    if (project) context.projectId='g-p-'+project.split('-')[2];
    const owner=new URLSearchParams(location.search).get('owner_user_id');
    if (owner && /^[a-zA-Z0-9-]{1,128}$/.test(owner)) context.ownerUserId=owner;
    return context;
  };
  // Used only by an explicit export gesture; no session/token endpoints.
  window.__novaReadConversation = async id => {
    Object.assign(diagnostics,{modules:0,imports:0,candidates:0,reader:'not-found',http:'not-requested'});
    if (id!==currentId()) throw new Error('会话发生变化，请重试');
    // An earlier response/export cannot establish the freshness of this gesture.
    // Only a full response started during this read can supply observed fallback.
    state.generation++; state.tree=null; state.id=null;
    const abort = new AbortController(), timeout = setTimeout(()=>abort.abort(),15000);
    try {
      const reader=await pageReader(abort.signal);
      if (id!==currentId()) throw new Error('会话发生变化');
      if (reader) {
        diagnostics.reader='calling';
        let rawTree=null;
        const result=await bounded(()=>reader(id,{includeFullConversation:true,forceNetworkFetch:true,...routeContext(),signal:abort.signal,
          onConversationLoadedFromNetwork:tree=>{if (!abort.signal.aborted) rawTree=tree;}}),abort.signal);
        const tree=rawTree || result;
        if (currentId()===id && tree?.conversation_id===id && tree.mapping) {
          if (JSON.stringify(tree).length>16*1024*1024) throw new Error('会话数据过大');
          state.tree=tree; state.id=id;
          diagnostics.reader='ok';
          return tree;
        }
        state.tree=null; state.id=null;
        diagnostics.reader='invalid-tree';
      }
    } catch (error) {
      if (diagnostics.reader==='calling') diagnostics.reader='failed-'+category(error);
    } finally { clearTimeout(timeout); abort.abort(); }
    if (id!==currentId()) throw new Error('会话发生变化，请重试');
    // Each source has its own failure boundary. An unavailable page reader must
    // not skip the matching current-conversation endpoint or use an expired signal.
    const networkAbort=new AbortController(), networkTimeout=setTimeout(()=>networkAbort.abort(),10000);
    try {
      const response = await bounded(()=>original('/backend-api/conversation/' + encodeURIComponent(id) + '?include_full_conversation=true',
        {credentials:'same-origin',cache:'no-store',signal:networkAbort.signal}),networkAbort.signal);
      diagnostics.http=response.status;
      if (response.ok) {
        const raw = await bounded(()=>response.text(),networkAbort.signal);
        if (raw.length > 16 * 1024 * 1024) throw new Error('会话数据过大');
        const tree=JSON.parse(raw);
        if (currentId()===id && tree?.conversation_id===id && tree.mapping) return tree;
        state.tree=null; state.id=null;
        diagnostics.http='invalid-tree';
      }
    } catch (error) { diagnostics.http='failed-'+category(error); }
    finally { clearTimeout(networkTimeout); networkAbort.abort(); }
    if (id!==currentId()) throw new Error('会话发生变化，请重试');
    const retained=pageTree(id);
    if (retained) return retained;
    if (state.id === id && state.tree) return state.tree;
    throw new Error('无法确认完整会话：未能通过网页的完整会话读取流程取得消息树。不会导出仅已加载的内容。\n诊断 E2：模块=' + diagnostics.modules + '，导入=' + diagnostics.imports + '，读取器=' + diagnostics.candidates + '，状态=' + diagnostics.reader + '，接口=' + diagnostics.http);
  };
})();
