(function(){
  const scope=window[HEY_NAMESPACE];
  if(!scope||!scope.refs)return JSON.stringify({error:'OBSERVATION_REPLACED'});
  const p=JSON.parse(HEY_QUERY);
  const clean=v=>String(v||'').replace(/\s+/g,' ').trim();
  const matches=(value,wanted)=>p.exact===false?clean(value).toLowerCase().includes(clean(wanted).toLowerCase()):clean(value)===clean(wanted);
  const visible=el=>{
    if(!el.isConnected||!el.getClientRects().length)return false;
    for(let a=el;a&&a.nodeType===1;a=a.parentElement){const s=getComputedStyle(a);if(s.display==='none'||s.visibility==='hidden'||s.visibility==='collapse')return false;}
    const r=el.getBoundingClientRect();return r.width>0&&r.height>0;
  };
  const role=el=>{
    const explicit=el.getAttribute('role');if(explicit)return explicit;
    const tag=el.tagName.toLowerCase();
    if(tag==='button')return 'button';if(tag==='a'&&el.hasAttribute('href'))return 'link';
    if(tag==='select')return 'combobox';if(tag==='textarea')return 'textbox';
    if(tag==='input'){const type=(el.type||'text').toLowerCase();if(['button','submit','reset'].includes(type))return 'button';if(['checkbox','radio'].includes(type))return type;if(type==='search')return 'searchbox';if(['text','email','password','tel','url','number'].includes(type))return 'textbox';}
    return '';
  };
  const name=el=>{
    const ids=clean(el.getAttribute('aria-labelledby')).split(' ').filter(Boolean);
    const labelled=ids.map(id=>document.getElementById(id)?.textContent||'').join(' ');
    const nativeLabel=el.labels?[...el.labels].map(l=>l.textContent||'').join(' '):'';
    return clean(el.getAttribute('aria-label')||labelled||nativeLabel||el.getAttribute('alt')||el.getAttribute('title')||el.innerText||el.getAttribute('placeholder'));
  };
  let nodes;
  try{
    const selectors={role:'[role],button,a[href],input,textarea,select',text:'a,button,label,p,span,h1,h2,h3,h4,li,[role="button"]',label:'input,textarea,select',placeholder:'input,textarea',testId:'[data-testid],[data-test-id]'};
    nodes=[...document.querySelectorAll(p.by==='css'?p.query:selectors[p.by])].slice(0,2500);
  }catch(_){return JSON.stringify({error:'INVALID_LOCATOR_QUERY'});}
  const found=[];
  for(const el of nodes){
    if(!visible(el))continue;
    let match=false;
    switch(p.by){
      case 'role':match=role(el)===p.query&&(p.name===undefined||matches(name(el),p.name));break;
      case 'text':match=matches(el.innerText||el.textContent,p.query);break;
      case 'label':match=matches(el.labels?[...el.labels].map(l=>l.textContent||'').join(' '):el.getAttribute('aria-label'),p.query);break;
      case 'placeholder':match=matches(el.getAttribute('placeholder'),p.query);break;
      case 'testId':match=matches(el.getAttribute('data-testid')||el.getAttribute('data-test-id'),p.query);break;
      case 'css':match=true;break;
    }
    if(!match)continue;
    found.push(el);if(found.length>1)return JSON.stringify({error:'MULTIPLE_MATCHES',matchCount:2});
  }
  if(!found.length)return JSON.stringify({error:'ELEMENT_NOT_FOUND',matchCount:0});
  const el=found[0];scope.nextRef=(scope.nextRef||0)+1;const ref='l'+scope.nextRef;scope.refs[ref]=el;
  const r=el.getBoundingClientRect();
  const sensitive=(el.tagName==='INPUT'&&(el.type==='password'||/one-time-code|cc-number|cc-csc/.test(el.autocomplete||'')));
  return JSON.stringify({ref,tag:el.tagName.toLowerCase(),role:role(el),label:sensitive?'[sensitive]':name(el).slice(0,160),sensitive,rect:{x:r.x,y:r.y,width:r.width,height:r.height},locatorUnique:true,matchCount:1});
})()
