const rich = `# 中文与 English\n\n段落含 **粗体**、*斜体*、\`inline code\` 和 [链接](https://example.com/path?q=chat%20gpt#section)。\n\n1. 第一项\n2. Second item\n\n- 无序列表\n  - Nested item\n\n> 引用内容\n>\n> 第二段引用\n\n\`\`\`javascript\nconst greeting = "你好";\nconsole.log(greeting);\n\`\`\`\n\n\`\`\`text\n${'very-long-code-'.repeat(90)}\n\`\`\`\n\n| 名称 | Value |\n| --- | --- |\n| 中文 | **bold** |\n| 链接 | [target](https://example.org/table) |\n\n公式原文：$E = mc^2$。\n\n$$\n\\int_0^1 x^2\\,dx = \\frac{1}{3}\n$$\n\n[长 URL](https://example.com/${'long-path/'.repeat(40)})\n\n![像素图片](data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD1sAAAAASUVORK5CYII=)\n`;
function fixture(rounds=2) {
 const mapping={root:{id:'root',parent:null,children:['m0'],message:null}};
 for(let i=0;i<rounds*2;i++) {
  mapping['m'+i]={id:'m'+i,parent:i?'m'+(i-1):'root',children:i<rounds*2-1?['m'+(i+1)]:[],
   message:{id:'m'+i,author:{role:i%2?'assistant':'user'},recipient:'all',status:'finished_successfully',
    content:{content_type:'text',parts:[i===rounds*2-1?rich:`第 ${i+1} 条消息。Chinese / English ${'完整正文。'.repeat(15)}`]}}};
 }
 mapping.root.children.push('other');
 mapping.other={id:'other',parent:'root',children:[],message:{id:'other',author:{role:'assistant'},status:'finished_successfully',content:{content_type:'text',parts:['OTHER BRANCH MUST NOT EXPORT']}}};
 return {conversation_id:'fixture',title:'中文会话 / Conversation: 富文本',current_node:'m'+(rounds*2-1),mapping};
}
module.exports={fixture,rich};
