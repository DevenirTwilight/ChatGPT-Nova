(() => {
  if (window !== window.top || location.origin !== 'https://chatgpt.com' || window.__novaExportCapture) return;
  const original = window.fetch;
  const state = window.__novaExportCapture = {tree:null, id:null, generation:0,pages:[],collectPages:false};
  const diagnostics={modules:0,imports:0,candidates:0,reader:'not-found',http:'not-requested',pagination:'not-requested',pages:0,pageStage:'not-requested',pageFailure:'not-requested'};
  const moduleUrls=new Set(), assetUrls=new Set();
  const publicAsset=u=>(u.origin===location.origin || u.origin==='https://cdn.oaistatic.com') &&
    /\/(?:cdn\/)?assets\/[a-zA-Z0-9_-]+\.js$/.test(u.pathname);
  const remember=name=> {
    try {
      const u=new URL(name,location.href);
      if (publicAsset(u)) assetUrls.add(u.href);
      if (publicAsset(u) &&
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
      const method=args[1]?.method || (typeof args[0]==='object' && !(args[0] instanceof URL) ? args[0].method : null) || 'GET';
      const kind=url.pathname==='/backend-api/conversations/'+id ? 'head' :
        url.pathname==='/backend-api/conversations/'+id+'/messages' ? 'older' : null;
      if (id && kind && method.toUpperCase()==='GET' && url.origin===location.origin && response.ok &&
          generation===state.generation && state.collectPages && state.pages.length<802) {
        // Body-only observation is bound to this read's exact conversation path
        // and opaque cursor. No request/response headers or credentials are read.
        const body=response.clone().text().then(raw=> {
          if (raw.length>16*1024*1024 || generation!==state.generation || currentId()!==id) throw Error('Invalid page');
          return JSON.parse(raw);
        });
        body.catch(()=>{});
        state.pages.push({id,kind,before:url.searchParams.get('before'),
          numTurns:url.searchParams.get('num_turns'),includeMessageId:url.searchParams.get('include_message_id'),
          ambiguousWindow:['num_turns','include_message_id'].some(key=>url.searchParams.getAll(key).length>1),body});
      }
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
  const category = error => {
    const status=Number.isInteger(error?.status) && error.status>=100 && error.status<=599 ? String(error.status) :
      (['TypeError','SyntaxError','AbortError'].includes(error?.name) ? error.name : 'error');
    // Only fixed, public error categories are useful here. Never display a
    // server message, arbitrary code, request URL, or account identifier.
    const codes=['workspace_ip_not_authorized','validate_whitelist_failed','invalid_workspace_selected','not_json_response'];
    return codes.includes(error?.code) ? status+'-'+error.code : status;
  };
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
        if (readers.length===1) return {fn:readers[0],url};
      } catch (error) {
        diagnostics.reader='import-'+category(error);
        if (signal.aborted) throw error;
      }
    }
    return null;
  };
  const paginationReaders = async (reader,signal) => {
    // Resolve only the direct dependency called by the discovered reader's
    // normal paginated path. All source/imports are already loaded public JS.
    diagnostics.pageStage='discovery-binding';
    const source=Function.prototype.toString.call(reader.fn);
    const callees=[...new Set([...source.matchAll(/await\s+([\w$]+)\(\{\s*additionalHeaders:[^,}]+,\s*clientThreadId:/g)].map(m=>m[1]))];
    // The current site reader names the initial and older-page helpers
    // separately (for example `aoe` and `gae`); the initial helper may also
    // be called from a cursor-restart path. Keep all discovered bindings and
    // resolve their single already-loaded static dependency below.
    if (!callees.length || callees.length>4) throw Error('Unsupported pagination binding');
    // The reader module is already executing in this page. Re-read its public
    // source with the page's same-origin fetch context; some WebViews reject
    // an otherwise valid static asset when credentials are explicitly omitted.
    const findReaders = exports => {
      const functions=[...new Set(Object.values(exports))].filter(fn=>typeof fn==='function');
      const initial=functions.filter(fn=> {
        const s=Function.prototype.toString.call(fn);
        // The initial helper has an includeMessageId argument. Some WebViews
        // or bundler revisions expose the nested older-route literal in its
        // source, so route absence is not a safe discriminator by itself.
        return s.includes('/conversations/{conversation_id}') && s.includes('include_has_versions') &&
          s.includes('includeMessageId') && (s.includes('messagesLeafToRoot') || s.includes('serverCurrentLeafId'));
      });
      const older=functions.filter(fn=> {
        const s=Function.prototype.toString.call(fn);
        return s.includes('/conversations/{conversation_id}/messages') && s.includes('include_has_versions') &&
          s.includes('moderationResults') && s.includes('cursor');
      });
      return initial.length===1 && older.length===1 ? {initial:initial[0],older:older[0]} : null;
    };
    let sourceFailure=false;
    try {
      diagnostics.pageStage='discovery-source';
      const response=await bounded(()=>original(reader.url,{credentials:'same-origin',cache:'force-cache',signal}),signal);
      if (!response.ok) throw Error('Public module unavailable');
      const moduleSource=await bounded(()=>response.text(),signal);
      if (moduleSource.length>4*1024*1024) throw Error('Public module too large');
      diagnostics.pageStage='discovery-import';
      const dependencies=new Set();
      for (const match of moduleSource.matchAll(/import\s*\{([^}]+)\}\s*from\s*([`"'])\s*([^`"']+)\2/g)) {
        if (match[1].split(',').some(binding=>callees.includes(binding.trim().split(/\s+as\s+/).at(-1)))) {
          const dependency=new URL(match[3],reader.url);
          if (publicAsset(dependency)) dependencies.add(dependency.href);
        }
      }
      if (dependencies.size!==1) throw Error('Pagination dependency ambiguous');
      diagnostics.pageStage='discovery-markers';
      const found=findReaders(await bounded(()=>import([...dependencies][0]),signal));
      if (!found) throw Error('Pagination readers ambiguous');
      return found;
    } catch (error) {
      if (signal.aborted) throw error;
      sourceFailure=true;
    }
    // A WebView may allow an ESM import but reject a follow-up fetch of that
    // module (for example because its script request has different fetch
    // metadata). Use only public JS modules already observed in this page and
    // select the unique pair by the same route/response markers. This remains
    // bounded and never walks stores, account objects, or arbitrary globals.
    if (sourceFailure) {
      diagnostics.pageStage='discovery-loaded-assets';
      performance.getEntriesByType('resource').forEach(e=>remember(e.name));
      document.querySelectorAll('script[src],link[rel="modulepreload"]').forEach(e=>remember(e.src || e.href));
      const allLoaded=[...assetUrls].filter(url=>url!==reader.url);
      const readerIndex=[...assetUrls].indexOf(reader.url);
      const loaded=allLoaded.sort((left,right)=> {
        if (readerIndex<0) return 0;
        return Math.abs([...assetUrls].indexOf(left)-readerIndex)-Math.abs([...assetUrls].indexOf(right)-readerIndex);
      }).slice(0,256);
      let found=null;
      for (const url of loaded) {
        try {
          const candidate=findReaders(await bounded(()=>import(url),signal));
          if (candidate) {
            if (found) throw Error('Pagination readers ambiguous');
            found=candidate;
          }
        } catch (error) {
          if (signal.aborted) throw error;
          if (error?.message==='Pagination readers ambiguous') throw error;
        }
      }
      if (found) return found;
      throw Error('Pagination readers unavailable');
    }
    throw Error('Pagination readers unavailable');
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
  const readPagination = async (id,reader,signal) => {
    if (!window.NovaExportPagination) throw Error('Pagination verifier unavailable');
    const startUrl=location.href;
    const stable=()=>{if (id!==currentId() || location.href!==startUrl) throw Error('Conversation changed');};
    const observed=async (from,kind,cursor) => {
      stable();
      const records=state.pages.slice(from).filter(r=>r.id===id && r.kind===kind && r.before===cursor);
      if (records.length!==1) throw Error('Pagination response missing or ambiguous');
      return bounded(()=>records[0].body,signal);
    };
    state.collectPages=true;
    try {
      // First let the ordinary page reader supply its own current route context.
      // A legacy full graph is acceptable; a partial synthetic graph is not.
      let from=state.pages.length, tree=null;
      diagnostics.pageStage='normal';
      try {
        let raw=null;
        const result=await bounded(()=>reader.fn(id,{includeFullConversation:false,forceNetworkFetch:true,...routeContext(),signal,
          onConversationLoadedFromNetwork:value=>{if (!signal.aborted) raw=value;}}),signal);
        tree=raw || result;
        if (!tree?.__paginatedConversationPage && tree?.conversation_id===id && tree.mapping) {
          if (JSON.stringify(tree).length>16*1024*1024) throw Error('Conversation too large');
          diagnostics.pagination='legacy-ok';
          return tree;
        }
      } catch (error) {diagnostics.pagination='normal-'+category(error);if (signal.aborted) throw error;}
      diagnostics.pageStage='discovery';
      const readers=await paginationReaders(reader,signal);
      // These identifiers come exclusively from the public address bar. The
      // website's existing safeGet helper supplies its own authorization.
      const context=routeContext(), additionalHeaders={};
      if (context.projectId) {
        additionalHeaders['chatgpt-project-id']=context.projectId;
        if (context.ownerUserId) additionalHeaders['chatgpt-conv-owner-id']=context.ownerUserId;
      }
      const options={clientThreadId:id,numTurns:50,signal,...Object.keys(additionalHeaders).length?{additionalHeaders}:{}};
      diagnostics.pageStage='head';
      let head;
      if (tree?.__paginatedConversationPage) head=await observed(from,'head',null);
      else {
        from=state.pages.length;stable();
        await bounded(()=>readers.initial(options),signal);
        head=await observed(from,'head',null);
      }
      // Preserve the two public pagination query parameters actually used for
      // this head response, including their absence. DOM selection and the
      // reader's normalized marker cannot establish the original request window.
      // The page helper continues to supply authentication; no headers are read.
      const initialRecord=state.pages.slice(from).find(record=>record.id===id && record.kind==='head' && record.before===null);
      const sameWindow={...options};delete sameWindow.numTurns;
      const invalidWindow=()=>{const error=Error('Unsupported pagination window');error.novaExportReason='request-window-invalid';throw error;};
      if (!initialRecord || initialRecord.ambiguousWindow) invalidWindow();
      if (initialRecord.numTurns!==null) {
        if (!/^(0|[1-9][0-9]{0,2})$/.test(initialRecord.numTurns) || Number(initialRecord.numTurns)>400) invalidWindow();
        sameWindow.numTurns=Number(initialRecord.numTurns);
      }
      if (initialRecord.includeMessageId!==null) {
        if (!/^[a-zA-Z0-9-]{0,128}$/.test(initialRecord.includeMessageId)) invalidWindow();
        sameWindow.includeMessageId=initialRecord.includeMessageId;
      }
      const collector=NovaExportPagination.create(id).start(head,id);
      diagnostics.pages=1;
      while (collector.nextCursor()!==null) {
        diagnostics.pageStage='older';
        const cursor=collector.nextCursor();from=state.pages.length;stable();
        await bounded(()=>readers.older({...sameWindow,cursor,moderationResults:head.moderation_results || []}),signal);
        collector.add(await observed(from,'older',cursor),id,cursor);
        diagnostics.pages++;
      }
      diagnostics.pageStage='recheck-request';
      from=state.pages.length;stable();
      await bounded(()=>readers.initial(sameWindow),signal);
      diagnostics.pageStage='recheck-response';
      const freshHead=await observed(from,'head',null);
      diagnostics.pageStage='recheck-verify';
      collector.recheck(freshHead,id);stable();
      const complete=collector.finish();diagnostics.pagination='ok';diagnostics.pageStage='done';return complete;
    } finally {state.collectPages=false;}
  };
  // Used only by an explicit export gesture; no session/token endpoints.
  window.__novaReadConversation = async id => {
    Object.assign(diagnostics,{modules:0,imports:0,candidates:0,reader:'not-found',http:'not-requested',pagination:'not-requested',pages:0,pageStage:'not-requested',pageFailure:'not-requested'});
    if (id!==currentId()) throw new Error('会话发生变化，请重试');
    // An earlier response/export cannot establish the freshness of this gesture.
    // Only a full response started during this read can supply observed fallback.
    state.generation++; state.tree=null; state.id=null;state.pages=[];state.collectPages=false;
    const abort = new AbortController(), timeout = setTimeout(()=>abort.abort(),15000);
    let reader=null;
    try {
      reader=await pageReader(abort.signal);
      if (id!==currentId()) throw new Error('会话发生变化');
      if (reader) {
        diagnostics.reader='calling';
        let rawTree=null;
        const result=await bounded(()=>reader.fn(id,{includeFullConversation:true,forceNetworkFetch:true,...routeContext(),signal:abort.signal,
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
    if (reader) {
      const pagesAbort=new AbortController(),pagesTimeout=setTimeout(()=>pagesAbort.abort(),75000);
      try {return await readPagination(id,reader,pagesAbort.signal);}
      catch (error) {
        diagnostics.pagination='failed-'+category(error);
        const reasons=['request-window-invalid','head-messages-changed','head-branch-changed','head-metadata-changed'];
        diagnostics.pageFailure=reasons.includes(error?.novaExportReason) ? error.novaExportReason : 'unclassified';
      }
      finally {clearTimeout(pagesTimeout);pagesAbort.abort();}
    }
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
    throw new Error('无法确认完整会话：未能通过网页的完整会话读取流程取得消息树。不会导出仅已加载的内容。\n诊断 E2：模块=' + diagnostics.modules + '，导入=' + diagnostics.imports + '，读取器=' + diagnostics.candidates + '，状态=' + diagnostics.reader + '，接口=' + diagnostics.http+'，分页='+diagnostics.pagination+'，页数='+diagnostics.pages+'，阶段='+diagnostics.pageStage+'，原因='+diagnostics.pageFailure);
  };
})();
