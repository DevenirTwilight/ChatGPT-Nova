const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require(process.env.NOVA_PLAYWRIGHT_MODULE||'/tmp/nova-dom-deps/node_modules/playwright-core');
(async()=>{const browser=await chromium.launch({executablePath:process.env.NOVA_CHROMIUM_EXECUTABLE||'/usr/bin/google-chrome',args:['--no-sandbox']});
try{for(const name of ['frozen-page.html','frozen-page-saf.html']){const page=await browser.newPage();await page.goto('file://'+path.resolve('snapshot-results',name));
const text=await page.locator('body').innerText();for(const m of ['Before snapshot marker','TAIL-SNAPSHOT-MARKER','CODE-LAST','TABLE-LAST'])assert(text.includes(m),m);
assert(!text.includes('After snapshot marker'));await page.screenshot({path:path.resolve('snapshot-results',name+'.png'),fullPage:true});await page.close();}
fs.writeFileSync('snapshot-results/chromium-open.txt','PASS: actual Android static .html files opened in '+browser.version()+'; text/code/table first and last markers present; late marker absent\n');
}finally{await browser.close();}})().catch(e=>{console.error(e);process.exitCode=1;});
