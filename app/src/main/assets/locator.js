(function(){
  const scope=window[HEY_NAMESPACE],s=scope?.semantic;
  if(!scope?.refs||!s)return JSON.stringify({error:'OBSERVATION_REPLACED'});
  const p=JSON.parse(HEY_QUERY),matches=(value,wanted)=>p.exact===false?s.clean(value).toLowerCase().includes(s.clean(wanted).toLowerCase()):s.clean(value)===s.clean(wanted);
  if(p.by==='css')try{document.querySelector(p.query)}catch(_){return JSON.stringify({error:'INVALID_LOCATOR_QUERY'})}
  const scan=s.scan();
  // Truncated scans cannot establish uniqueness, even if an early match exists.
  if(scan.truncated)return JSON.stringify({error:'LOCATOR_SCAN_LIMIT',scanLimit:6000});
  let found=scan.nodes.filter(el=>{
    if(!s.visible(el)||s.hidden(el))return false;
    switch(p.by){
      case 'role':return s.role(el)===p.query&&(p.name===undefined||matches(s.name(el),p.name));
      case 'text':return matches(el.innerText||el.textContent,p.query);
      case 'label':return matches(el.labels?.length?[...el.labels].map(l=>l.textContent||'').join(' '):s.name(el),p.query)&&['INPUT','TEXTAREA','SELECT'].includes(el.tagName);
      case 'placeholder':return ['INPUT','TEXTAREA'].includes(el.tagName)&&matches(el.getAttribute('placeholder'),p.query);
      case 'testId':return matches(el.getAttribute('data-testid')||el.getAttribute('data-test-id'),p.query);
      case 'css':return el.matches(p.query);
      default:return false;
    }
  });
  // Nested text wrappers describe the same text target; independent duplicates stay ambiguous.
  if(p.by==='text')found=found.filter(el=>!found.some(child=>child!==el&&s.contains(el,child)));
  if(found.length>1)return JSON.stringify({error:'MULTIPLE_MATCHES',matchCount:found.length});
  if(!found.length)return JSON.stringify({error:'ELEMENT_NOT_FOUND',matchCount:0});
  const el=found[0],ref='l'+(++scope.nextRef),r=el.getBoundingClientRect(),secret=s.sensitive(el);
  scope.refs[ref]=el;
  return JSON.stringify({ref,tag:el.tagName.toLowerCase(),role:s.role(el),label:secret?'[sensitive]':s.name(el).slice(0,160),sensitive:secret,disabled:s.disabled(el),rect:{x:r.x,y:r.y,width:r.width,height:r.height},locatorUnique:true,matchCount:1,scope:'top-document-and-open-shadow-roots'});
})();
