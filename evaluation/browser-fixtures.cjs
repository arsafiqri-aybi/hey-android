const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require('playwright');
const repo=path.resolve(__dirname,'..'),assets=path.join(repo,'app/src/main/assets'),ns='hey_fixture',target='hey_target';
const source=name=>fs.readFileSync(path.join(assets,name+'.js'),'utf8');
const replace=(js,values)=>Object.entries(values).reduce((s,[k,v])=>s.replaceAll(k,v),js);
const semantic=replace(source('semantic'),{HEY_NAMESPACE:JSON.stringify(ns)});
const observer=replace(source('observe'),{HEY_NAMESPACE:JSON.stringify(ns)});
let checks=0;const check=(value,message)=>{assert.ok(value,message);checks++};
async function setup(page,html){await page.setContent(html);await page.evaluate(semantic);await page.evaluate(observer)}
async function locate(page,payload){return JSON.parse(await page.evaluate(replace(source('locator'),{HEY_NAMESPACE:JSON.stringify(ns),HEY_QUERY:JSON.stringify(JSON.stringify(payload))})))}
async function point(page){return JSON.parse(await page.evaluate(replace(source('actionability'),{HEY_NAMESPACE:JSON.stringify(ns),HEY_TARGET:JSON.stringify(target),HEY_WEB_WIDTH:'720'})))}
async function select(page,ref){await page.evaluate(({ref,ns,target})=>window[target]=window[ns].refs[ref],{ref,ns,target})}
async function fill(page,text){return page.evaluate(replace(source('fill'),{HEY_NAMESPACE:JSON.stringify(ns),HEY_TARGET:JSON.stringify(target),HEY_VALUE:JSON.stringify(text)}))}
async function scroll(page,x,y){return JSON.parse(await page.evaluate(replace(source('scroll'),{HEY_NAMESPACE:JSON.stringify(ns),HEY_SCROLL_X:String(x),HEY_SCROLL_Y:String(y)})))}
(async()=>{
  const browser=await chromium.launch({executablePath:process.env.HEY_CHROMIUM,headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});
  const page=await browser.newPage({viewport:{width:720,height:900}});
  await setup(page,`<style>#nested{height:160px;width:350px;overflow-y:auto}#space{height:1200px}#cover{display:none;position:fixed;z-index:999;inset:0;background:#3333}</style>
  <label for="email">Alamat Email</label><input id="email" placeholder="Email pribadi"><label for="secret">Kata sandi</label><input id="secret" type="password" value="dummy-do-not-disclose">
  <button id="save" data-testid="save-btn"><span>Simpan</span></button><button id="other">Batalkan</button><div id="nested"><div id="space"></div><button id="far">Lanjutkan</button></div><div id="cover"></div><div hidden><button>Hidden</button></div>`);
  const found=await locate(page,{by:'role',query:'button',name:'Simpan'});check(found.ref==='l1'&&found.locatorUnique,'unique role');
  for(const payload of [{by:'label',query:'Alamat Email'},{by:'placeholder',query:'Email pribadi'},{by:'testId',query:'save-btn'},{by:'text',query:'Simpan'},{by:'css',query:'#save'},{by:'role',query:'button',name:'Sim',exact:false}])check((await locate(page,payload)).locatorUnique,JSON.stringify(payload));
  const secret=await locate(page,{by:'label',query:'Kata sandi'});check(secret.sensitive&&secret.label==='[sensitive]'&&!JSON.stringify(secret).includes('dummy-do-not-disclose'),'secret redacted');
  check((await locate(page,{by:'role',query:'button'})).error==='MULTIPLE_MATCHES','strict duplicate');
  check((await locate(page,{by:'text',query:'Hidden'})).error==='ELEMENT_NOT_FOUND','hidden target');
  check((await locate(page,{by:'css',query:'['})).error==='INVALID_LOCATOR_QUERY','invalid selector');
  check((await locate(page,{by:'text',query:'");window.hey_injected=true;//'})).error==='ELEMENT_NOT_FOUND','query injection inert');
  check(!await page.evaluate('window.hey_injected===true'),'no injection');
  const far=await locate(page,{by:'role',query:'button',name:'Lanjutkan'});await select(page,far.ref);check(!(await point(page)).error,'nested target actionability');
  check(await page.evaluate("document.querySelector('#nested').scrollTop>700"),'nested region scrolled');
  await page.evaluate("document.querySelector('#cover').style.display='block'");check((await point(page)).error==='ELEMENT_OBSCURED','overlay blocks');
  await page.evaluate("document.querySelector('#cover').style.display='none';document.querySelector('#far').disabled=true");check((await point(page)).error==='ELEMENT_DISABLED','disabled blocks');
  await page.evaluate("document.querySelector('#far').remove()");check((await point(page)).error==='STALE_REFERENCE','detached blocks');
  const email=await locate(page,{by:'label',query:'Alamat Email'});await select(page,email.ref);
  await page.evaluate("window.events=0;document.querySelector('#email').addEventListener('input',()=>window.events++)");
  check(await fill(page,'hello@example.test')==='OK'&&await page.evaluate('window.events===1'),'framework input event once');
  await page.evaluate("document.querySelector('#email').readOnly=true");check(await fill(page,'second')==='NOT_EDITABLE','readonly blocks');
  await page.evaluate("document.querySelector('#email').readOnly=false;document.querySelector('#email').disabled=true");check(await fill(page,'second')==='ELEMENT_DISABLED','disabled fill blocks');
  await setup(page,'<span id="real">Real name</span><button id="named" aria-labelledby="real" aria-label="Wrong name">Other</button><fieldset disabled><button>Disabled fieldset</button></fieldset><div aria-hidden="true"><button>Hidden aria</button></div><div inert><button>Inert button</button></div><div contenteditable="true" aria-label="Editor">Before</div><input type="number" aria-label="Quantity"><div id="host"></div>');
  check((await locate(page,{by:'role',query:'button',name:'Real name'})).locatorUnique,'labelledby takes precedence');
  check((await locate(page,{by:'role',query:'button',name:'Hidden aria'})).error==='ELEMENT_NOT_FOUND','aria-hidden excluded');
  const disabled=await locate(page,{by:'role',query:'button',name:'Disabled fieldset'});await select(page,disabled.ref);check((await point(page)).error==='ELEMENT_DISABLED','native fieldset disabled');
  const inert=await locate(page,{by:'role',query:'button',name:'Inert button'});await select(page,inert.ref);check((await point(page)).error==='ELEMENT_DISABLED','inert blocks');
  check((await locate(page,{by:'role',query:'spinbutton',name:'Quantity'})).locatorUnique,'number input role');
  const editor=await locate(page,{by:'role',query:'textbox',name:'Editor'});await select(page,editor.ref);check(await fill(page,'New text')==='OK','contenteditable fill');
  await page.evaluate("document.querySelector('#host').attachShadow({mode:'open'}).innerHTML='<button aria-label=Shadow>Go</button><input type=password aria-label=Secret value=never-return-this>'");
  const shadow=await locate(page,{by:'role',query:'button',name:'Shadow'});check(shadow.locatorUnique,'open shadow lookup');await select(page,shadow.ref);check(!(await point(page)).error,'open shadow hit target');
  const shadowSecret=await locate(page,{by:'role',query:'textbox',name:'Secret'});check(shadowSecret.sensitive&&shadowSecret.label==='[sensitive]','shadow secret redacted');
  await setup(page,'<button style="width:2000px;height:80px">Clipped button</button>');
  const clipped=await locate(page,{by:'role',query:'button'});await select(page,clipped.ref);check(!(await point(page)).error,'visible intersection for oversized element');
  await setup(page,'<style>html,body{margin:0}#outer{height:800px;overflow:auto}#inner{height:600px;overflow:auto}#filler{height:2200px}#after{height:1200px}</style><div id="outer"><div id="inner"><div id="filler"></div></div><div id="after"></div></div>');
  const scrolled=await scroll(page,0,300);check(scrolled.moved&&await page.evaluate("document.querySelector('#inner').scrollTop>0"),'nearest nested scroll');
  const before=JSON.parse(await page.evaluate(observer));await page.evaluate("document.querySelector('#inner').scrollTop=99999");
  check((await scroll(page,0,300)).moved&&await page.evaluate("document.querySelector('#outer').scrollTop>0"),'nested edge bubbles to ancestor');
  const after=JSON.parse(await page.evaluate(observer));check(before.scrollRegions.some(a=>after.scrollRegions.some(b=>a.id===b.id)),'stable scroll region identity');
  await setup(page,'<button id="first">First</button>'+('<span>x</span>'.repeat(6001)));check((await locate(page,{by:'css',query:'#first'})).error==='LOCATOR_SCAN_LIMIT','large scan cannot assert uniqueness');
  const out=process.env.HEY_QA_OUTPUT||path.join(repo,'app/build/generated/qa-assets/qa');fs.mkdirSync(out,{recursive:true});
  const layout=[];
  for(const [width,height] of [[320,640],[360,740],[393,852],[412,915],[480,960],[852,393]]){
    const p=await browser.newPage({viewport:{width,height},deviceScaleFactor:2});await p.goto('file://'+path.join(assets,'home.html'));
    await p.evaluate(()=>{window.heyConfigure({safeTop:24,safeBottom:24,wallpaper:'wallpaper-1080.webp'});window.heySetState('UNREGISTERED','Belum terhubung','Belum ada aktivitas')});
    await p.locator('#wallpaper').evaluate(el=>el.decode());
    const boxes=await p.evaluate(()=>{const box=s=>{const r=document.querySelector(s).getBoundingClientRect();return {x:r.x,y:r.y,w:r.width,h:r.height,b:r.bottom}};return {header:box('.header'),preview:box('.preview'),activity:box('.activity'),dock:box('.dock'),bodyWidth:document.documentElement.scrollWidth,ratio:document.querySelector('.preview').offsetWidth/document.querySelector('.preview').offsetHeight}});
    check(Math.abs(boxes.ratio-.8)<.006,'preview remains 4:5 '+width);check(boxes.header.h>=68,'roomier header '+width);check(boxes.bodyWidth<=width,'no horizontal overflow '+width);
    if(height>=640)check(boxes.activity.b+8<boxes.dock.y,'dock does not cover activity '+width);
    if(width===393){check(boxes.preview.h>370,'large focal preview');check(await p.locator('.brand').evaluate(el=>parseFloat(getComputedStyle(el).fontSize))===25,'smaller wordmark');}
    const neutral=await p.locator('.dot').evaluate(el=>getComputedStyle(el).backgroundColor);await p.evaluate(()=>window.heySetState('ONLINE','Terhubung','Belum ada aktivitas'));const online=await p.locator('.dot').evaluate(el=>getComputedStyle(el).backgroundColor);check(neutral!==online,'unregistered is not green '+width);
    await p.evaluate(()=>{window.heySetState('UNREGISTERED','Belum terhubung','Belum ada aktivitas');window.heyConfigure({reduceMotion:true,lowEffects:false})});
    await p.screenshot({path:path.join(out,'home-'+width+'x'+height+'.png')});layout.push({width,height,...boxes});await p.close();
  }
  const p=await browser.newPage({viewport:{width:393,height:852}});await p.clock.install();await p.goto('file://'+path.join(assets,'home.html'));await p.evaluate(()=>window.heySetActive(false));const word=await p.locator('#greeting').textContent();await p.clock.fastForward(10000);check(await p.locator('#greeting').textContent()===word,'native hidden surface stops greeting timer');
  await p.evaluate(()=>window.heySetActive(true));await p.emulateMedia({reducedMotion:'reduce'});await p.clock.fastForward(10000);check(await p.locator('#greeting').textContent()===word,'live reduced motion stops timers');
  await p.evaluate(()=>window.heyConfigure({lowEffects:true}));check(await p.locator('.header').evaluate(el=>getComputedStyle(el).backdropFilter)==='none','low-memory effects fallback');
  await browser.close();
  const report={status:'PASS',checks,engine:'Desktop Chromium DOM and render fixtures; not Android WebView or physical-device evidence',layout};
  fs.writeFileSync(path.join(out,'report.json'),JSON.stringify(report,null,2));console.log('HEY_BROWSER_FIXTURES '+JSON.stringify({status:'PASS',checks,viewports:layout.map(x=>x.width+'x'+x.height)}));
})().catch(error=>{console.error(error);process.exit(1)});
