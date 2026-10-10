(function(){
  const s=window[HEY_NAMESPACE]?.semantic,dx=HEY_SCROLL_X,dy=HEY_SCROLL_Y;
  if(!s)return JSON.stringify({error:'OBSERVATION_REPLACED'});
  let el=s.hit(innerWidth/2,innerHeight/2);
  for(let count=0;el&&count<128;count++,el=s.parent(el)){
    const css=getComputedStyle(el),canX=dx!==0&&/(auto|scroll)/.test(css.overflowX)&&el.scrollWidth>el.clientWidth+2,canY=dy!==0&&/(auto|scroll)/.test(css.overflowY)&&el.scrollHeight>el.clientHeight+2;
    if(!canX&&!canY)continue;
    const x=el.scrollLeft,y=el.scrollTop;el.scrollBy({left:canX?dx:0,top:canY?dy:0,behavior:'instant'});
    if(Math.abs(el.scrollLeft-x)>.5||Math.abs(el.scrollTop-y)>.5)return JSON.stringify({moved:true,regionId:s.identity(el)});
  }
  const x=scrollX,y=scrollY;window.scrollBy({left:dx,top:dy,behavior:'instant'});
  return JSON.stringify({moved:Math.abs(scrollX-x)>.5||Math.abs(scrollY-y)>.5,regionId:'window'});
})();
