const {test}=require('node:test'); const assert=require('node:assert/strict');
const core=require('../../app/src/main/assets/export/core.js'); const {fixture}=require('./fixture.cjs');
function normalize(tree,ids=['m3']) {return core.normalize(tree,'fixture',ids);}
test('short bilingual conversation exports selected branch including unrendered ancestors',()=> {
 const data=normalize(fixture()); assert.equal(data.messages.length,4); assert.equal(data.completeness.nodes,5);
 assert(!core.markdown(data).includes('OTHER BRANCH')); assert(core.markdown(data).includes('```javascript'));
 assert(core.markdown(data).includes('https://example.org/table')); assert(core.markdown(data).includes('$$'));
});
test('400 messages with only final DOM message retain complete chain',()=> {
 const data=normalize(fixture(200),['m399']); assert.equal(data.messages.length,400);
 assert.equal(data.messages[0].id,'m0'); assert.equal(data.messages.at(-1).id,'m399');
});
for(const [name,mutate,ids] of [
 ['wrong conversation',t=>t.conversation_id='elsewhere'],
 ['missing parent',t=>delete t.mapping.m1],
 ['broken child relationship',t=>t.mapping.m1.children=[]],
 ['cycle',t=>{t.mapping.root.parent='m3';t.mapping.m3.children.push('root');}],
 ['hidden terminal cycle',t=>{t.mapping.m3.message.author.role='tool';t.mapping.m3.parent='m3';}],
 ['streaming reply',t=>t.mapping.m3.message.status='in_progress'],
 ['unknown message content',t=>t.mapping.m2.message.content.content_type='audio'],
 ['attachments fail explicitly',t=>t.mapping.m1.message.metadata={attachments:[{id:'file'}]}],
 ['scrolled-up ancestor cannot select incomplete branch',()=>{},['m1']],
 ['mixed DOM branches',()=>{},['other','m3']],
 ['missing DOM evidence',()=>{},[]]
]) test(name,()=>{const t=fixture();mutate(t);assert.throws(()=>normalize(t,ids),/无法确认完整会话/);});
test('image pointers give explicit warning and preserve a placeholder',()=> {
 const t=fixture();t.mapping.m1.message.content={content_type:'multimodal_text',parts:['text',{content_type:'image_asset_pointer'}]};
 const d=normalize(t);assert.equal(d.warnings.length,1);assert.match(d.messages[1].markdown,/图片/);
});
test('links keep targets and reject active schemes',()=> {
 assert.equal(core.safeUrl('https://example.com/a?q=1#part',false),'https://example.com/a?q=1#part');
 for(const s of ['javascript:alert(1)','file:///secret','https://user:password@example.com']) assert.equal(core.safeUrl(s,false),null);
});

test('metadata citation URLs are retained without guessing targets',()=> {
 const t=fixture();t.mapping.m1.message.content.parts=['See \uE200cite\uE202turn0search0\uE201'];
 t.mapping.m1.message.metadata={content_references:[{matched_text:'\uE200cite\uE202turn0search0\uE201',url:'https://example.org/real-source',title:'Actual source'}]};
 const data=normalize(t);assert.match(data.messages[1].markdown,/https:\/\/example.org\/real-source/);assert(!data.messages[1].markdown.includes('turn0search0'));
});
test('partial flags and missing root sentinel are rejected',()=> {
 const t=fixture();t.has_more=true;assert.throws(()=>normalize(t),/不完整/);
 delete t.has_more;t.mapping.m0.parent=null;assert.throws(()=>normalize(t),/起点/);
});

test('regenerated assistant branch follows server-selected leaf',()=> {
 const tree=fixture();tree.current_node='other';
 const data=normalize(tree,['m0','other']);
 assert.deepEqual(data.messages.map(m=>m.id),['m0','other']);
 assert.match(data.messages[1].markdown,/OTHER BRANCH/);
});

test('page-selected regenerated leaf can differ from server current_node',()=> {
 const data=normalize(fixture(),['m0','other']);
 assert.deepEqual(data.messages.map(m=>m.id),['m0','other']);
 assert.equal(data.completeness.terminal,'other');
});
test('DOM message order must follow the verified branch',()=> {
 assert.throws(()=>normalize(fixture(),['m1','m0','m3']),/顺序/);
});

test('unclosed user code fence cannot swallow the next role separator',()=> {
 const data=normalize(fixture());data.messages[0].markdown='```python\nprint("中文")';
 const md=core.markdown(data);assert.match(md,/print\("中文"\)\n```\n\n---\n\n## Assistant/);
});
test('long fences keep literal shorter fences and correct terminators',()=> {
 const data=normalize(fixture());data.messages[0].markdown='````markdown\n```javascript\nconst text="English";\n```';
 assert.match(core.markdown(data),/const text="English";\n```\n````\n\n---/);
});

test('unfinished hidden assistant tail cannot masquerade as a complete branch',()=> {
 const t=fixture();t.mapping.m3.children=['pending'];
 t.mapping.pending={id:'pending',parent:'m3',children:[],message:{id:'pending',author:{role:'assistant'},channel:'analysis',status:'in_progress',content:{content_type:'text',parts:['hidden work']}}};
 t.current_node='pending';assert.throws(()=>core.normalize(t,'fixture',['m3']),/未完成/);
});
