const fs=require('node:fs'),path=require('node:path');
const probe=fs.readFileSync(path.join(__dirname,'probe.js'),'utf8');
function allowed(url){try{const u=new URL(url);return u.origin==='https://chatgpt.com'&&!u.username&&!u.password&&/^\/share\/[^/]+\/?$/.test(u.pathname);}catch{return false;}}
// Feasibility source only. No account storage, share creation, private reader or export fallback.
class SharedConversationSource {
 constructor(page,url,{timeoutMs=15000,pollMs=250,markers={}}={}){if(!allowed(url))throw Error('SHARE_ORIGIN');this.page=page;this.url=url;this.timeoutMs=Math.min(timeoutMs,15000);this.pollMs=pollMs;this.markers=markers;}
 async observe(){if(this.page.url()!==this.url)throw Error('SHARE_URL_CHANGED');const d=JSON.parse(await this.page.evaluate(probe));if(d.error||d.unavailable||d.loginPage)throw Error(d.error||'SHARE_UNAVAILABLE');return d;}
 async waitStable(){let last='',stable=0;const start=Date.now();do{const d=await this.observe(),sig=JSON.stringify(d);stable=d.ready&&d.bodyPresent&&!d.loading&&sig===last?stable+1:0;last=sig;if(stable>=4)return d;await this.page.waitForTimeout(this.pollMs);}while(Date.now()-start<this.timeoutMs);throw Error('SHARE_NOT_STABLE');}
 async inspectVirtualization(){
  const page=this.page;await this.waitStable();
  const first=await page.locator('[data-message-author-role]').first().elementHandle();
  const scroll=await first.evaluateHandle(n=>{for(let e=n.parentElement;e;e=e.parentElement){const s=getComputedStyle(e);if(/auto|scroll/.test(s.overflowY)&&e.scrollHeight>e.clientHeight+2)return e;}return document.scrollingElement;});
  const sample=async()=>({dom:await this.observe(),markers:await page.evaluate(m=>Object.fromEntries(Object.entries(m).map(([k,v])=>[k,document.body.innerText.includes(v)])),this.markers),scroll:await scroll.evaluate(n=>({top:n.scrollTop,max:Math.max(0,n.scrollHeight-n.clientHeight),viewport:n.clientHeight})),firstStillMounted:await first.evaluate(n=>n.isConnected)});
  const initial=await sample();await scroll.evaluate(n=>n.scrollTop=0);await this.waitStable();const top=await sample();
  const topFirst=await page.locator('[data-message-author-role]').first().elementHandle();
  // Kept only in memory for this identity check; never emitted or persisted, never merged.
  const topText=await topFirst.evaluate(n=>n.innerText);
  await scroll.evaluate(n=>n.scrollTop=n.scrollHeight);await this.waitStable();const bottom=await sample();
  const topFirstStillMounted=await topFirst.evaluate(n=>n.isConnected);
  const topFirstTextStillPresent=await page.evaluate(t=>[...document.querySelectorAll('[data-message-author-role]')].some(n=>n.innerText===t),topText);
  return {initial,top,bottom,topFirstStillMounted,topFirstTextStillPresent,virtualizationObserved:!!topText&&!topFirstStillMounted&&!topFirstTextStillPresent,coverage:'not-independently-verified',sameUrlUpdate:'not-tested'};
 }
}
module.exports={SharedConversationSource,allowed};
