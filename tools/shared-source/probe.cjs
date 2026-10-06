// User supplies an already-created public link explicitly; no bodies/URLs are stored.
const fs=require('node:fs');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE||'playwright-core');
const {SharedConversationSource,allowed}=require('./SharedConversationSource.cjs');
(async()=>{
 const url=process.env.NOVA_SHARE_URL;if(!allowed(url))throw Error('SHARE_ORIGIN');
 const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE||'/usr/bin/chromium',args:['--no-sandbox'],proxy:process.env.HTTPS_PROXY?{server:process.env.HTTPS_PROXY}:undefined});
 try{const desktop=!!process.env.NOVA_SHARE_DESKTOP,page=await browser.newPage({viewport:desktop?{width:1280,height:720}:{width:412,height:850}});const start=Date.now();
 const response=await page.goto(url,{waitUntil:'domcontentloaded',timeout:30000});
 const source=new SharedConversationSource(page,url,{markers:Object.fromEntries([['first',process.env.NOVA_SHARE_FIRST_MARKER],['last',process.env.NOVA_SHARE_LAST_MARKER]].filter(([,v])=>v))}),evidence=await source.inspectVirtualization();
 const report={source:'public-shared-page',browser:browser.version(),viewport:desktop?'desktop-1280x720':'mobile-412x850',httpStatus:response.status(),...evidence,elapsedMs:Date.now()-start};
 console.log(JSON.stringify(report,null,2));if(process.env.NOVA_SHARE_REPORT)fs.writeFileSync(process.env.NOVA_SHARE_REPORT,JSON.stringify(report,null,2)+'\n');
 }finally{await browser.close();}
})().catch(e=>{console.error(JSON.stringify({error:/^SHARE_/.test(e.message)?e.message:'SHARE_LOAD_FAILED'}));process.exitCode=1;});
