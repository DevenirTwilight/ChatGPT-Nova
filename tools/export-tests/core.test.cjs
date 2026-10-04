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
 ['stale selected branch',()=>{},['other']],
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
