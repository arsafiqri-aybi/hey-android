(function(){
  const el=window[HEY_TARGET];
  if(!el||!el.isConnected)return JSON.stringify({error:'STALE_REFERENCE'});
  if(el.matches(':disabled')||el.getAttribute('aria-disabled')==='true')return JSON.stringify({error:'ELEMENT_DISABLED'});
  if(!el.getClientRects().length)return JSON.stringify({error:'ELEMENT_HIDDEN'});
  for(let a=el;a&&a.nodeType===1;a=a.parentElement){const s=getComputedStyle(a);if(s.display==='none'||s.visibility==='hidden'||s.visibility==='collapse')return JSON.stringify({error:'ELEMENT_HIDDEN'});}
  el.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'});
  const r=el.getBoundingClientRect(),x=r.x+r.width/2,y=r.y+r.height/2;
  if(r.width<=0||r.height<=0||x<0||y<0||x>=innerWidth||y>=innerHeight)return JSON.stringify({error:'ELEMENT_OUTSIDE_VIEWPORT'});
  const top=document.elementFromPoint(x,y);
  if(!top||!(top===el||el.contains(top)))return JSON.stringify({error:'ELEMENT_OBSCURED'});
  return JSON.stringify({x,y,scale:innerWidth?HEY_WEB_WIDTH/innerWidth:0,width:r.width,height:r.height});
})()
