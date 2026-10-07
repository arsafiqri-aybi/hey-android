(function(){
  const refs={};let n=0;
  const publicLocation=()=>{const u=new URL(location.href);for(const key of [...u.searchParams.keys()])if(/^(access_token|refresh_token|id_token|token|code|password|secret|api_key|apikey|otp|authorization|session|credential)$/i.test(key))u.searchParams.set(key,'[redacted]');if(/(access_token|id_token|token|password|secret)=/i.test(u.hash))u.hash='[redacted]';return u.href;};
  const visible=el=>{const r=el.getBoundingClientRect(),s=getComputedStyle(el);return r.width>0&&r.height>0&&r.bottom>0&&r.right>0&&r.top<innerHeight&&r.left<innerWidth&&s.visibility!=='hidden'&&s.display!=='none';};
  const elements=[];
  for(const el of document.querySelectorAll('a,button,input,textarea,select,[role="button"],[contenteditable="true"]')){
    if(elements.length>=120)break;if(!visible(el))continue;
    const ref='r'+(++n);refs[ref]=el;
    const r=el.getBoundingClientRect(),sensitive=el.type==='password'||/password|one-time-code|cc-number|cc-csc/.test(el.autocomplete||'');
    elements.push({ref,tag:el.tagName.toLowerCase(),role:el.getAttribute('role'),label:(el.getAttribute('aria-label')||el.innerText||el.placeholder||el.name||'').slice(0,160),type:el.type||null,sensitive,disabled:!!el.disabled,rect:{x:r.x,y:r.y,width:r.width,height:r.height}});
  }
  window[HEY_NAMESPACE]={refs};
  const media=[...document.querySelectorAll('video,audio')].slice(0,8).map((e,index)=>({index,type:e.tagName.toLowerCase(),currentTime:e.currentTime,duration:Number.isFinite(e.duration)?e.duration:null,paused:e.paused,ended:e.ended,muted:e.muted,readyState:e.readyState,playbackRate:e.playbackRate}));
  return JSON.stringify({url:publicLocation(),title:document.title,text:(document.body?.innerText||'').slice(0,12000),viewport:{width:innerWidth,height:innerHeight,scrollX,scrollY},elements,media,documentReady:document.readyState,contentAuthority:'untrusted-webpage'});
})()
