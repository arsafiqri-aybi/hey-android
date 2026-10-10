(function(){
  const scope=window[HEY_NAMESPACE],s=scope.semantic,scan=s.scan(),refs={},elements=[],media=[],scrollRegions=[];
  scope.refs=refs;scope.nextRef=0;
  const publicLocation=()=>{const u=new URL(location.href);for(const key of [...u.searchParams.keys()])if(/^(access_token|refresh_token|id_token|token|code|password|secret|api_key|apikey|otp|authorization|session|credential)$/i.test(key))u.searchParams.set(key,'[redacted]');if(/(access_token|id_token|token|password|secret)=/i.test(u.hash))u.hash='[redacted]';return u.href};
  const viewportVisible=el=>{if(!s.visible(el)||s.hidden(el))return false;const r=el.getBoundingClientRect();return r.bottom>0&&r.right>0&&r.top<innerHeight&&r.left<innerWidth};
  for(const el of scan.nodes){
    const tag=el.tagName.toLowerCase();
    if(elements.length<120&&(s.role(el)||el.isContentEditable)&&viewportVisible(el)){
      const ref='r'+(elements.length+1),r=el.getBoundingClientRect(),secret=s.sensitive(el);refs[ref]=el;
      elements.push({ref,elementId:s.identity(el),tag,role:s.role(el),label:secret?'[sensitive]':s.name(el).slice(0,160),type:el.type||null,sensitive:secret,disabled:s.disabled(el),checked:typeof el.checked==='boolean'?el.checked:el.getAttribute('aria-checked'),expanded:el.getAttribute('aria-expanded'),rect:{x:r.x,y:r.y,width:r.width,height:r.height}});
    }
    if(media.length<8&&(tag==='video'||tag==='audio'))media.push({index:media.length,type:tag,currentTime:el.currentTime,duration:Number.isFinite(el.duration)?el.duration:null,paused:el.paused,ended:el.ended,muted:el.muted,readyState:el.readyState,playbackRate:el.playbackRate});
    if(scrollRegions.length<48&&s.visible(el)&&(el.scrollHeight>el.clientHeight+2||el.scrollWidth>el.clientWidth+2)){
      const css=getComputedStyle(el);if(/(auto|scroll)/.test(css.overflowX+' '+css.overflowY))scrollRegions.push({id:s.identity(el),index:scrollRegions.length,top:el.scrollTop,left:el.scrollLeft,maxTop:Math.max(0,el.scrollHeight-el.clientHeight),maxLeft:Math.max(0,el.scrollWidth-el.clientWidth)});
    }
  }
  let focused=document.activeElement;while(focused?.shadowRoot?.activeElement)focused=focused.shadowRoot.activeElement;
  const focusedElement=focused?s.identity(focused):'';
  return JSON.stringify({url:publicLocation(),title:document.title,text:(document.body?.innerText||'').slice(0,12000),viewport:{width:innerWidth,height:innerHeight,scrollX,scrollY},scrollRegions,elements,media,focusedElement,documentReady:document.readyState,contentAuthority:'untrusted-webpage',observationLimits:{scanLimit:6000,truncated:scan.truncated,crossOriginFrames:'not-inspected',closedShadowRoots:'not-inspected'}});
})();
