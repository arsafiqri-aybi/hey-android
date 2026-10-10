// Bounded semantic helpers shared by observation, lookup and readiness checks.
// This is a WebView adapter inspired by Playwright, not the Node.js Playwright engine.
(function(){
  const key=HEY_NAMESPACE,scope=window[key]||{};
  const clean=value=>String(value??'').replace(/\s+/g,' ').trim();
  const parent=el=>el.parentElement||el.getRootNode()?.host||null;
  const ancestors=el=>{const out=[];for(let a=el;a&&out.length<128;a=parent(a))out.push(a);return out};
  const visible=el=>{
    if(!el?.isConnected||!el.getClientRects().length)return false;
    for(const a of ancestors(el)){const s=getComputedStyle(a);if(s.display==='none'||s.visibility==='hidden'||s.visibility==='collapse')return false}
    const r=el.getBoundingClientRect();return r.width>0&&r.height>0;
  };
  const hidden=el=>ancestors(el).some(a=>a.getAttribute('aria-hidden')==='true');
  const disabled=el=>el.matches(':disabled')||ancestors(el).some(a=>a.getAttribute('aria-disabled')==='true'||a.hasAttribute('inert'));
  const sensitive=el=>el.tagName==='INPUT'&&(el.type==='password'||/password|one-time-code|cc-number|cc-csc/.test(el.autocomplete||''));
  const role=el=>{
    const explicit=clean(el.getAttribute('role')).split(' ')[0];if(explicit)return explicit;
    const tag=el.tagName.toLowerCase();
    if(tag==='button')return 'button';if(tag==='a'&&el.hasAttribute('href'))return 'link';
    if(tag==='select')return el.multiple||el.size>1?'listbox':'combobox';if(tag==='option')return 'option';
    if(tag==='textarea'||el.isContentEditable)return 'textbox';if(/^h[1-6]$/.test(tag))return 'heading';
    if(tag==='img'&&el.getAttribute('alt'))return 'img';
    if(tag==='input'){const type=el.type;if(['button','submit','reset','image'].includes(type))return 'button';if(['checkbox','radio'].includes(type))return type;if(type==='search')return 'searchbox';if(type==='number')return 'spinbutton';if(type==='range')return 'slider';if(type!=='hidden')return 'textbox'}
    return '';
  };
  const name=el=>{
    const tree=el.getRootNode(),ids=clean(el.getAttribute('aria-labelledby')).split(' ').filter(Boolean);
    const labelled=ids.map(id=>(tree.getElementById?.(id)||document.getElementById(id))?.textContent||'').join(' ');
    const labels=el.labels?[...el.labels].map(l=>l.textContent||'').join(' '):'';
    const buttonValue=el.tagName==='INPUT'&&['button','submit','reset'].includes(el.type)?el.value:'';
    return clean(labelled||el.getAttribute('aria-label')||labels||el.getAttribute('alt')||buttonValue||el.innerText||el.getAttribute('title')||el.getAttribute('placeholder'));
  };
  const scan=()=>{
    const nodes=[],roots=[document];let truncated=false;
    while(roots.length){
      const root=roots.shift(),walker=document.createTreeWalker(root,NodeFilter.SHOW_ELEMENT);
      for(let el=walker.nextNode();el;el=walker.nextNode()){
        if(nodes.length>=6000){truncated=true;break}
        nodes.push(el);if(el.shadowRoot)roots.push(el.shadowRoot);
      }
      if(truncated)break;
    }
    return {nodes,truncated};
  };
  const hit=(x,y)=>{let el=document.elementFromPoint(x,y);for(let depth=0;el?.shadowRoot&&depth<16;depth++){const inner=el.shadowRoot.elementFromPoint(x,y);if(!inner||inner===el)break;el=inner}return el};
  const contains=(outer,inner)=>ancestors(inner).includes(outer);
  scope.elementIds=scope.elementIds||new WeakMap();scope.nextElementId=scope.nextElementId||0;
  const identity=el=>{let id=scope.elementIds.get(el);if(!id){id='e'+(++scope.nextElementId);scope.elementIds.set(el,id)}return id};
  scope.semantic={clean,parent,visible,hidden,disabled,sensitive,role,name,scan,hit,contains,identity};
  window[key]=scope;
})();
