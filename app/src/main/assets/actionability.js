(function(){
  const el=window[HEY_TARGET],s=window[HEY_NAMESPACE]?.semantic;
  if(!el?.isConnected||!s)return JSON.stringify({error:'STALE_REFERENCE'});
  if(s.disabled(el))return JSON.stringify({error:'ELEMENT_DISABLED'});
  if(!s.visible(el)||s.hidden(el))return JSON.stringify({error:'ELEMENT_HIDDEN'});
  el.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'});
  const r=el.getBoundingClientRect(),left=Math.max(0,r.left),right=Math.min(innerWidth,r.right),top=Math.max(0,r.top),bottom=Math.min(innerHeight,r.bottom);
  if(right<=left||bottom<=top)return JSON.stringify({error:'ELEMENT_OUTSIDE_VIEWPORT'});
  // Use the visible intersection; a wide/clipped element may have an offscreen center.
  const x=(left+right)/2,y=(top+bottom)/2,hit=s.hit(x,y);
  if(!hit||!s.contains(el,hit))return JSON.stringify({error:'ELEMENT_OBSCURED'});
  const scale=innerWidth?HEY_WEB_WIDTH/innerWidth:0;
  if(!Number.isFinite(scale)||scale<=0)return JSON.stringify({error:'SURFACE_UNAVAILABLE'});
  return JSON.stringify({x,y,scale,width:r.width,height:r.height,left:r.left,top:r.top});
})();
