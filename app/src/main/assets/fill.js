(function(){
  const el=window[HEY_TARGET],s=window[HEY_NAMESPACE]?.semantic,value=HEY_VALUE;
  if(!el?.isConnected||!s)return 'STALE_REFERENCE';
  if(!s.visible(el)||s.hidden(el))return 'ELEMENT_HIDDEN';
  if(s.disabled(el))return 'ELEMENT_DISABLED';
  if(el.readOnly||el.getAttribute('aria-readonly')==='true')return 'NOT_EDITABLE';
  if(el.isContentEditable){el.focus();el.textContent=value;el.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText'}));el.dispatchEvent(new Event('change',{bubbles:true}));return el.textContent===value?'OK':'FILL_NOT_APPLIED'}
  if(!(el instanceof HTMLTextAreaElement||el instanceof HTMLInputElement&&/^(text|email|password|tel|url|number|search)$/.test(el.type)))return 'UNSUPPORTED_FILL';
  const proto=el instanceof HTMLTextAreaElement?HTMLTextAreaElement.prototype:HTMLInputElement.prototype,setter=Object.getOwnPropertyDescriptor(proto,'value')?.set;
  if(!setter)return 'UNSUPPORTED_FILL';
  el.focus();setter.call(el,value);el.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText'}));el.dispatchEvent(new Event('change',{bubbles:true}));
  return el.value===value?'OK':'FILL_NOT_APPLIED';
})();
