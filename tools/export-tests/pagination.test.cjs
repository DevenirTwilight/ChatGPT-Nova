const {test}=require('node:test');
const assert=require('node:assert/strict');
const pagination=require('../../app/src/main/assets/export/pagination.js');
const core=require('../../app/src/main/assets/export/core.js');
const message=id=>({id,author:{role:Number(id.slice(1))%2?'assistant':'user'},recipient:'all',status:'finished_successfully',content:{content_type:'text',parts:['Body '+id]}});
const rows=(start,end)=>Array.from({length:end-start},(_,index)=>message('m'+(start+index)));
const page=(messages,hasPrevious=false,cursor=null,extra={})=>({conversation_id:'fixture',title:'Complete branch 中文',messages,
 page_info:{has_previous_page:hasPrevious,start_cursor:cursor},...extra});
const initial=(messages=rows(2,4),hasPrevious=true,cursor='older')=>page(messages,hasPrevious,cursor,{current_node:messages.at(-1)?.id});
const clone=value=>JSON.parse(JSON.stringify(value));
const collector=()=>pagination.create('fixture');

test('two pages produce the complete ordered branch with cursor proof',()=> {
 const head=initial(), c=collector().start(head,'fixture');
 assert.equal(c.nextCursor(),'older');
 c.add(page(rows(0,2)),'fixture','older');assert.equal(c.nextCursor(),null);
 const tree=c.recheck(clone(head),'fixture').finish(), data=core.normalize(tree,'fixture',['m3']);
 assert.deepEqual(data.messages.map(m=>m.id),['m0','m1','m2','m3']);
 assert.deepEqual(tree.__novaPaginationProof,{method:'cursor-pagination',pages:2,exhausted:true});
 assert.equal(data.completeness.root,'paginated-complete:fixture');
});
test('400 messages from four cursor pages normalize with only the final DOM message',()=> {
 const head=initial(rows(300,400),true,'p2'), c=collector().start(head,'fixture');
 c.add(page(rows(200,300),true,'p3'),'fixture','p2');
 c.add(page(rows(100,200),true,'p4'),'fixture','p3');c.add(page(rows(0,100)),'fixture','p4');
 const data=core.normalize(c.recheck(head,'fixture').finish(),'fixture',['m399']);
 assert.equal(data.messages.length,400);assert.equal(data.messages[0].id,'m0');assert.equal(data.messages.at(-1).id,'m399');
});
test('an already exhausted initial page still requires a fresh head recheck',()=> {
 const head=initial(rows(0,2),false), c=collector().start(head,'fixture');
 assert.equal(c.recheck(head,'fixture').finish().current_node,'m1');
});
test('missing body IDs rely on the verified current-conversation path',()=> {
 const head=initial(rows(0,2),false);delete head.conversation_id;
 assert.equal(collector().start(head,'fixture').recheck(head,'fixture').finish().conversation_id,'fixture');
});
test('the alternate id field also proves the current conversation',()=> {
 const head=initial(rows(0,2),false);delete head.conversation_id;head.id='fixture';
 assert.equal(collector().start(head,'fixture').recheck(head,'fixture').finish().conversation_id,'fixture');
});
for (const [name,mutate] of [
 ['wrong conversation_id',p=>p.conversation_id='other'],['wrong id',p=>p.id='other'],
 ['missing page_info',p=>delete p.page_info],['missing exhaustion flag',p=>delete p.page_info.has_previous_page],
 ['nonboolean exhaustion flag',p=>p.page_info.has_previous_page='false'],
 ['empty next cursor',p=>p.page_info.start_cursor=''],['nonstring cursor',p=>p.page_info.start_cursor=4],
 ['missing message list',p=>delete p.messages],['duplicate message IDs',p=>p.messages.push(clone(p.messages[0]))],
 ['missing message ID',p=>delete p.messages[0].id],['current_node absent from the head',p=>p.current_node='not-loaded'],
 ['current_node points to a nonterminal head message',p=>p.current_node='m2'],
 ['explicit missing conversation data',p=>p.has_missing_conversation_data=true],
 ['explicit partial response',p=>p.is_partial=true]
]) test(name+' is rejected',()=> {
 const head=initial();mutate(head);assert.throws(()=>collector().start(head,'fixture'),/无法确认完整会话/);
});
test('an initial response from another request path is rejected',()=> {
 assert.throws(()=>collector().start(initial(),'other'),/不属于当前会话/);
});
test('older pages must retain the current conversation path',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page(rows(0,2)),'other','older'),/不属于当前会话/);
});
test('a recheck response from another request path is rejected',()=> {
 const head=initial(rows(0,2),false), c=collector().start(head,'fixture');
 assert.throws(()=>c.recheck(head,'other'),/不属于当前会话/);
});
test('contiguous compatible boundary overlap is deduplicated',()=> {
 const head=initial(rows(2,4)), c=collector().start(head,'fixture');
 c.add(page(rows(0,3)),'fixture','older');
 assert.deepEqual(core.normalize(c.recheck(head,'fixture').finish(),'fixture',['m3']).messages.map(m=>m.id),['m0','m1','m2','m3']);
});
test('equivalent object property order does not create a false body conflict',()=> {
 const head=initial(), c=collector().start(head,'fixture'), overlap=message('m2');
 const reordered={content:overlap.content,status:overlap.status,recipient:overlap.recipient,author:overlap.author,id:overlap.id};
 c.add(page([message('m0'),message('m1'),reordered]),'fixture','older');
 assert.equal(Object.keys(c.recheck(head,'fixture').finish().mapping).length,5);
});
test('overlapping repeated message bodies must agree',()=> {
 const c=collector().start(initial(),'fixture'), older=page(rows(0,3));older.messages[2].content.parts=['changed'];
 assert.throws(()=>c.add(older,'fixture','older'),/正文冲突/);
});
test('overlap with an interior current-page message cannot splice another branch',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page([message('m0'),message('m3')]),'fixture','older'),/不连续/);
});
test('duplicate IDs within an older page are rejected',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page([message('m0'),message('m0')]),'fixture','older'),/重复/);
});
test('the requested cursor must equal the outstanding server cursor',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page(rows(0,2)),'fixture','guessed'),/游标不匹配/);
});
test('a next cursor cannot repeat the current cursor',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page(rows(0,2),true,'older'),'fixture','older'),/游标未前进/);
});
test('a next cursor cannot loop back to an earlier cursor',()=> {
 const c=collector().start(initial(rows(4,6),true,'first'),'fixture');
 c.add(page(rows(2,4),true,'second'),'fixture','first');
 assert.throws(()=>c.add(page(rows(0,2),true,'first'),'fixture','second'),/游标未前进/);
});
test('empty pages that still claim older content fail closed',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page([],true,'next'),'fixture','older'),/未提供更早消息/);
});
test('repeated boundary-only pages cannot claim further progress',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page([message('m2')],true,'next'),'fixture','older'),/未提供更早消息/);
});
test('an explicitly exhausted empty older page is valid',()=> {
 const head=initial(), c=collector().start(head,'fixture');c.add(page([]),'fixture','older');
 assert.equal(core.normalize(c.recheck(head,'fixture').finish(),'fixture',['m3']).messages.length,2);
});
test('older pages cannot change a supplied server current_node',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.add(page(rows(0,2),false,null,{current_node:'m1'}),'fixture','older'),/分支发生变化/);
});
test('an older response explicitly marked partial cannot be synthesized as complete',()=> {
 const c=collector().start(initial(),'fixture');
 assert.throws(()=>c.add(page(rows(0,2),false,null,{is_partial:true}),'fixture','older'),/标记为不完整/);
 assert.throws(()=>c.finish(),/已经失败/);
});
test('a final head recheck explicitly missing data cannot establish completeness',()=> {
 const head=initial(rows(0,2),false), c=collector().start(head,'fixture');
 assert.throws(()=>c.recheck({...head,has_missing_conversation_data:true},'fixture'),/标记为不完整/);
});
test('unexhausted pagination cannot emit a synthesized tree',()=> {
 const c=collector().start(initial(),'fixture');assert.throws(()=>c.finish(),/尚未完成/);
});
test('exhausted pagination cannot emit without a fresh head recheck',()=> {
 const c=collector().start(initial(),'fixture');c.add(page(rows(0,2)),'fixture','older');
 assert.throws(()=>c.finish(),/尚未完成/);
});
for (const [name,mutate] of [
 ['same-ID text mutation',p=>p.messages.at(-1).content.parts=['new body']],
 ['server branch mutation',p=>p.current_node='m2'],
 ['initial cursor mutation',p=>p.page_info.start_cursor='new-cursor'],
 ['same-ID ancestor mutation',p=>p.messages[0].content.parts=['edited prompt']]
]) test('head recheck rejects '+name,()=> {
 const head=initial(), c=collector().start(head,'fixture');c.add(page(rows(0,2)),'fixture','older');
 const reread=clone(head);mutate(reread);assert.throws(()=>c.recheck(reread,'fixture'),/发生变化/);
});
test('a failed recheck permanently prevents emission even if the caller catches it',()=> {
 const head=initial(rows(0,2),false), c=collector().start(head,'fixture'), changed=clone(head);changed.messages[1].content.parts=['changed'];
 assert.throws(()=>c.recheck(changed,'fixture'));assert.throws(()=>c.recheck(head,'fixture'),/已经失败/);assert.throws(()=>c.finish(),/已经失败/);
});
for (const [name,mutate] of [
 ['update_time after an older-message edit',p=>p.update_time=1001],
 ['title',p=>p.title='Changed title'],
 ['nested conversation metadata',p=>p.conversation_origin.type='changed'],
 ['new top-level metadata',p=>p.new_metadata={revision:2}]
]) test('head recheck rejects changed '+name+' even with identical recent messages',()=> {
 const head=initial();head.update_time=1000;head.conversation_origin={type:'regular'};
 const c=collector().start(head,'fixture');c.add(page(rows(0,2)),'fixture','older');
 const reread=clone(head);mutate(reread);
 assert.throws(()=>c.recheck(reread,'fixture'),/元数据发生变化/);
});
test('the 400-page cap rejects further cursor traversal',()=> {
 const c=collector().start(initial([message('m400')],true,'c1'),'fixture');
 for(let n=1;n<400;n++) c.add(page([message('m'+(400-n))],true,'c'+(n+1)),'fixture','c'+n);
 assert.throws(()=>c.add(page([message('m0')]),'fixture','c400'),/分页数量/);
});
test('a response over 16 MiB is rejected before accumulation',()=> {
 const head=initial();head.messages[0].content.parts=['x'.repeat(16*1024*1024)];
 assert.throws(()=>collector().start(head,'fixture'),/数据过大/);
});
test('page bytes are bounded cumulatively, including repeated content',()=> {
 const head=initial([message('m3')]), c=collector();head.messages[0].content.parts=['x'.repeat(6*1024*1024)];c.start(head,'fixture');
 const second=page([message('m2')],true,'last');second.messages[0].content.parts=['y'.repeat(6*1024*1024)];c.add(second,'fixture','older');
 const final=page([message('m1')]);final.messages[0].content.parts=['z'.repeat(6*1024*1024)];
 assert.throws(()=>c.add(final,'fixture','last'),/数据过大/);
});
test('unsupported or unfinished content is still rejected by the shared core',()=> {
 const head=initial(rows(0,2),false);head.messages[1].status='in_progress';
 const tree=collector().start(head,'fixture').recheck(head,'fixture').finish();
 assert.throws(()=>core.normalize(tree,'fixture',['m1']),/未完成/);
});
test('reserved synthetic root IDs cannot overwrite the proof root',()=> {
 const head=initial([{...message('m1'),id:'paginated-complete:fixture'}],false);
 assert.throws(()=>collector().start(head,'fixture'),/消息 ID/);
});
