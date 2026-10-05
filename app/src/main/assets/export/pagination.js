/* Verified cursor traversal of the website's current-conversation response bodies. */
(function (root) {
  'use strict';
  const MAX_PAGES=400, MAX_BYTES=16*1024*1024;
  const own=(object,key)=>Object.prototype.hasOwnProperty.call(object,key);
  const object=value=>value && typeof value==='object' && !Array.isArray(value);
  const canonical=value=>Array.isArray(value) ? value.map(canonical) : object(value) ?
    Object.fromEntries(Object.keys(value).sort().map(key=>[key,canonical(value[key])])) : value;
  const fingerprint=value=>JSON.stringify(canonical(value));
  const byteLength=value=>new TextEncoder().encode(value).length;
  const internals=new WeakMap();

  function create(conversationId) {
    return collect(conversationId,{pages:0,bytes:0,broken:false},null);
  }
  function collect(conversationId,budget,replayOwner,seed) {
    let started=false, checked=false, cursor=null, replay=null;
    let head=null, headProof=null, messages=[];
    const cursors=new Set();
    // An independent replay shares both resource limits and failure state with
    // the original traversal. A failed replay can never leave an exportable one.
    const fail=(detail,reason)=>{budget.broken=true;const error=new Error('无法确认完整会话：'+detail);if(reason) error.novaExportReason=reason;throw error;};
    const healthy=()=>{if (budget.broken) fail('分页验证已经失败');};
    if (typeof conversationId!=='string' || !conversationId) fail('分页会话 ID 无效');
    const rootId='paginated-complete:'+conversationId;

    // scopedPathId is supplied by the origin/path-scoped response observer.
    // An absent body ID is usable only because that caller proved the request path.
    const readPage=(raw,scopedPathId)=> {
      healthy();
      if (scopedPathId!==conversationId || !object(raw)) fail('分页响应不属于当前会话');
      for (const key of ['conversation_id','id'])
        if (own(raw,key) && raw[key]!==conversationId) fail('分页响应会话 ID 不匹配');
      let serialized;
      try {serialized=JSON.stringify(raw);} catch (_) {fail('分页响应无法验证');}
      if (typeof serialized!=='string') fail('分页响应无法验证');
      budget.bytes+=byteLength(serialized);
      if (budget.bytes>MAX_BYTES) fail('分页会话数据过大');
      // Retain JSON data rather than live objects or accessors supplied by a page.
      const page=JSON.parse(serialized), info=page.page_info;
      if (page.has_missing_conversation_data===true || page.is_partial===true)
        fail('分页响应标记为不完整');
      if (!object(info) || typeof info.has_previous_page!=='boolean') fail('分页完整性标记缺失');
      if (info.has_previous_page && (typeof info.start_cursor!=='string' || !info.start_cursor))
        fail('分页游标缺失');
      if (!Array.isArray(page.messages)) fail('分页消息列表缺失');
      const ids=new Set();
      for (const message of page.messages) {
        if (!object(message) || typeof message.id!=='string' || !message.id || message.id===rootId || ids.has(message.id))
          fail('分页消息 ID 无效或重复');
        ids.add(message.id);
      }
      return page;
    };
    // Retain all initial metadata, including update_time: an edit to an older
    // message can change the conversation while its latest page stays identical.
    // The opaque start cursor is a newly issued pagination token on some
    // servers, so it is deliberately excluded from the fresh-head comparison;
    // the explicit has_previous_page boolean remains part of the proof.
    const headEvidence=page=> {
      const {page_info:info,...rest}=page;
      const {start_cursor,...stableInfo}=info;
      return fingerprint({...rest,page_info:stableInfo});
    };
    const sameField=(left,right,key)=>own(left,key)===own(right,key) && fingerprint(left[key])===fingerprint(right[key]);
    const consistentHead=page=> {
      if (page.current_node!==head.current_node) fail('分页读取期间服务器分支发生变化','head-branch-changed');
      if (fingerprint(page.messages)!==fingerprint(head.messages)) fail('分页读取期间会话正文发生变化','head-messages-changed');
      if (!sameField(page,head,'title')) fail('分页读取期间会话元数据发生变化（标题）','head-title-changed');
      if (page.page_info.has_previous_page!==head.page_info.has_previous_page)
        fail('分页读取期间会话元数据发生变化（完整性）','head-metadata-changed');
      if (!sameField(page,head,'update_time')) fail('分页读取期间会话元数据发生变化（更新时间）','head-update-time-changed');
      if (!sameField(page,head,'moderation_results')) fail('分页读取期间会话元数据发生变化（审核结果）','head-moderation-changed');
      if (['safe_urls','blocked_urls'].some(key=>!sameField(page,head,key)))
        fail('分页读取期间会话元数据发生变化（链接状态）','head-link-metadata-changed');
    };
    const advance=page=> {
      const next=page.page_info.has_previous_page ? page.page_info.start_cursor : null;
      if (next!==null && cursors.has(next)) fail('分页游标未前进或重复');
      if (next!==null) cursors.add(next);
      cursor=next;
    };
    const initialize=page=> {
      if (budget.pages>=MAX_PAGES) fail('分页数量超出限制');
      if (!page.messages.length || typeof page.current_node!=='string' ||
          page.messages[page.messages.length-1].id!==page.current_node)
        fail('分页末条消息与服务器分支不一致');
      head=page;headProof=headEvidence(page);messages=page.messages.slice();
      started=true;budget.pages++;advance(page);
    };
    const ready=()=> {
      healthy();
      if (!started || cursor!==null) fail('分页尚未读取完整');
      if (replay) fail('独立复读验证尚未完成');
    };
    const api={
      start(raw,scopedPathId) {
        healthy();
        if (started) fail('分页读取已经开始');
        initialize(readPage(raw,scopedPathId));
        return api;
      },
      nextCursor() {healthy();if (!started) fail('分页读取尚未开始');return cursor;},
      add(raw,scopedPathId,requestedCursor) {
        healthy();
        if (!started || cursor===null || requestedCursor!==cursor) fail('分页请求游标不匹配');
        if (budget.pages>=MAX_PAGES) fail('分页数量超出限制');
        const page=readPage(raw,scopedPathId);
        if (own(page,'current_node') && page.current_node!==head.current_node) fail('分页读取期间服务器分支发生变化');
        const positions=new Map(messages.map((message,index)=>[message.id,index]));
        const firstOverlap=page.messages.findIndex(message=>positions.has(message.id));
        let older=page.messages;
        if (firstOverlap!==-1) {
          const overlap=page.messages.slice(firstOverlap);
          if (overlap.length>messages.length || overlap.some((message,index)=>
              message.id!==messages[index].id || fingerprint(message)!==fingerprint(messages[index])))
            fail('分页重叠消息不连续或正文冲突');
          older=page.messages.slice(0,firstOverlap);
        }
        if (!older.length && page.page_info.has_previous_page) fail('分页未提供更早消息');
        advance(page);messages=older.concat(messages);budget.pages++;checked=false;
        return api;
      },
      recheck(raw,scopedPathId) {
        ready();
        const page=readPage(raw,scopedPathId);
        consistentHead(page);
        if (!replayOwner && headEvidence(page)!==headProof) fail('分页读取期间会话元数据发生变化','head-metadata-changed');
        checked=true;
        return api;
      },
      // A metadata-only difference is insufficient to emit. The fresh head is
      // instead the start of one independent, fully exhausted cursor traversal.
      review(raw,scopedPathId) {
        ready();
        if (replayOwner) fail('独立复读不能重复恢复');
        const page=readPage(raw,scopedPathId);
        consistentHead(page);
        if (headEvidence(page)===headProof) {checked=true;return null;}
        checked=false;
        replay=collect(conversationId,budget,api,page);
        return replay;
      },
      confirmReplay(candidate) {
        healthy();
        const proof=internals.get(candidate);
        if (replayOwner || !replay || candidate!==replay || !proof || proof.owner!==api ||
            proof.budget!==budget || proof.id!==conversationId || !proof.complete())
          fail('独立复读完整性或末端复核尚未完成');
        const fresh=proof.head();
        consistentHead(fresh);
        if (fingerprint(proof.messages())!==fingerprint(messages))
          fail('独立复读期间会话正文发生变化','replay-messages-changed');
        checked=true;
        return api;
      },
      finish() {
        healthy();
        if (replayOwner || !started || cursor!==null || !checked) fail('分页完整性或末端复核尚未完成');
        const mapping=Object.create(null);
        mapping[rootId]={id:rootId,parent:null,children:[messages[0].id],message:null};
        messages.forEach((message,index)=> {
          mapping[message.id]={id:message.id,message,parent:index ? messages[index-1].id : rootId,
            children:index+1<messages.length ? [messages[index+1].id] : []};
        });
        const tree={conversation_id:conversationId,title:head.title || '未命名会话',current_node:head.current_node,mapping,
          __novaPaginationProof:{method:'cursor-pagination',pages:budget.pages,exhausted:true}};
        if (byteLength(JSON.stringify(tree))>MAX_BYTES) fail('分页会话数据过大');
        return tree;
      }
    };
    internals.set(api,{id:conversationId,owner:replayOwner,budget,
      complete:()=>started && cursor===null && checked && !budget.broken,
      head:()=>head,messages:()=>messages});
    // review() already validated and charged this response body. Count it once
    // as the new traversal's head rather than charging its bytes a second time.
    if (seed) initialize(seed);
    return api;
  }
  const api={create};
  if (typeof module!=='undefined') module.exports=api;
  else root.NovaExportPagination=api;
})(typeof globalThis!=='undefined' ? globalThis : this);
