// Controlled page: only seven messages are ever mounted, at arbitrary scroll positions.
(function(count=40) {
 history.replaceState({},'', '/g/g-p-fixture/c/fixture');document.title='Scroll fixture';
 document.body.style.margin='0';document.body.innerHTML='<main id="history" style="height:360px;overflow-y:auto;position:relative"><div id="space" style="position:relative;height:'+count*64+'px"></div></main>';
 const panel=document.getElementById('history'),space=document.getElementById('space');
 let previousStart=-1;
 function draw() {
  const start=Math.min(Math.max(0,count-7),Math.floor(panel.scrollTop/64));
  if(start===previousStart) return;previousStart=start;
  space.innerHTML=Array.from({length:Math.min(7,count)},(_,n)=>{const i=start+n;return '<article data-message-id="m'+i+'" style="position:absolute;top:'+i*64+'px;height:64px"><div data-message-author-role="'+(i%2?'assistant':'user')+'"><div class="markdown"><p>'+(i===12 || i===14 ? 'REPEATED-TEXT' : 'ROW-'+i)+'</p></div></div></article>';}).join('');
 }
 panel.addEventListener('scroll',draw);panel.scrollTop=Math.max(0,count*64-360);draw();return true;
})(__NOVA_FIXTURE_COUNT__)
