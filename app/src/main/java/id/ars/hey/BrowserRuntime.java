package id.ars.hey;

import android.app.Presentation;
import android.app.DownloadManager;
import android.content.Context;
import android.content.MutableContextWrapper;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.view.PixelCopy;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.webkit.*;
import android.widget.FrameLayout;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.URI;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.*;

final class BrowserRuntime implements AutoCloseable {
  interface Callback {void result(JSONObject observation,String error);}
  private final Context context;private final StateStore state;private final SecureStore store;
  final Handler main=new Handler(Looper.getMainLooper());
  private final ImageReader reader;private final VirtualDisplay display;private final Presentation presentation;private final FrameLayout container;
  private final LinkedHashMap<String,WebView> tabs=new LinkedHashMap<>();private String active="",version=UUID.randomUUID().toString(),namespace="hey_"+UUID.randomUUID().toString().replace("-","");
  private long operationEpoch;private boolean injectingInput;private FrameLayout visibleHost;private Activity visibleActivity;private final Map<WebView,MutableContextWrapper> contexts=new HashMap<>();private final Map<WebView,Long> epochs=new HashMap<>();
  private final Set<WebView> failedPages=new HashSet<>();
  private String observer,locatorScript,actionabilityScript;private Callback navigation;private Bitmap frame;private long frameAt;private boolean screenshotPending,closed;private ValueCallback<Uri[]> fileCallback;
  BrowserRuntime(Context context,StateStore state,SecureStore store)throws Exception {
    this.context=context;this.state=state;this.store=store;
    observer=loadAsset("observe.js");locatorScript=loadAsset("locator.js");actionabilityScript=loadAsset("actionability.js");
    WebView.setWebContentsDebuggingEnabled(false);
    reader=ImageReader.newInstance(720,1280,PixelFormat.RGBA_8888,2);
    DisplayManager dm=context.getSystemService(DisplayManager.class);
    display=dm.createVirtualDisplay("Hey Runtime",720,1280,240,reader.getSurface(),DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY|DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION);
    if(display==null)throw new IllegalStateException("PRIVATE_DISPLAY_UNAVAILABLE");
    presentation=new Presentation(context,display.getDisplay());container=new FrameLayout(presentation.getContext());presentation.setContentView(container);presentation.getWindow().setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION);presentation.getWindow().addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);presentation.show();
    reader.setOnImageAvailableListener(r->{try(Image image=r.acquireLatestImage()){if(image==null||!screenshotPending)return;screenshotPending=false;Image.Plane plane=image.getPlanes()[0];int width=image.getWidth(),height=image.getHeight(),padding=plane.getRowStride()/plane.getPixelStride()-width;Bitmap padded=Bitmap.createBitmap(width+padding,height,Bitmap.Config.ARGB_8888);padded.copyPixelsFromBuffer(plane.getBuffer());Bitmap next=Bitmap.createBitmap(padded,0,0,width,height);if(next!=padded)padded.recycle();if(frame!=null)frame.recycle();frame=next;frameAt=System.currentTimeMillis();}catch(Exception e){state.browser("CAPTURE_ERROR");}},main);
    CookieManager.getInstance().setAcceptCookie(true);
    JSONArray saved=new JSONArray(store.get("tabs","[]"));if(saved.length()==0)newTab("https://example.com");else for(int i=0;i<Math.min(saved.length(),8);i++)newTab(saved.getString(i));
    state.browser("READY");
  }
  private String loadAsset(String name)throws Exception{try(InputStream in=context.getAssets().open(name)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1)out.write(chunk,0,n);return out.toString("UTF-8");}}
  private void humanPriority(){
    HeyService service=HeyService.current;
    if(service!=null&&service.browser==this)service.control(true);else{state.control("HUMAN");abort();}
  }
  private final class ControlledWebView extends WebView {
    private float startX,startY;private boolean candidate,allowed;private int sequence;
    ControlledWebView(Context context){super(context);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){
      if(injectingInput)return super.dispatchTouchEvent(event);
      int action=event.getActionMasked();float density=getResources().getDisplayMetrics().density;
      if(action==MotionEvent.ACTION_DOWN){
        if(!state.snapshot().optString("control").equals("HUMAN"))humanPriority();
        startX=event.getX();startY=event.getY();candidate=true;allowed=false;int ticket=++sequence;
        // DOM hit test declines nested scrollers, editable controls and media. Unknown is fail-closed.
        checkGesture(startX,startY,result->{if(sequence==ticket)allowed=result;});
      }else if(action==MotionEvent.ACTION_POINTER_DOWN||action==MotionEvent.ACTION_CANCEL){candidate=false;sequence++;}
      else if(action==MotionEvent.ACTION_UP&&candidate&&allowed){
        float dx=event.getX()-startX,dy=event.getY()-startY;boolean refresh=startY<72*density&&getScrollY()==0&&dy>100*density&&dy>Math.abs(dx)*2.5f;
        android.graphics.Insets edges=getRootWindowInsets()==null?android.graphics.Insets.NONE:getRootWindowInsets().getInsets(android.view.WindowInsets.Type.systemGestures());
        float edge=Math.max(24*density,edges.left);boolean back=startX>=edge&&startX<edge+20*density&&startY<getHeight()-80*density&&dx>96*density&&dx>Math.abs(dy)*2.5f;
        if(refresh||back&&canGoBack()){MotionEvent cancel=MotionEvent.obtain(event);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();if(refresh)humanHardReload();else humanBack();candidate=false;return true;}
      }
      return super.dispatchTouchEvent(event);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
      if(!state.snapshot().optString("control").equals("HUMAN"))humanPriority();return super.performAccessibilityAction(action,args);
    }
  }
  void checkGesture(float x,float y,java.util.function.Consumer<Boolean> done){
    WebView target=web();if(target==null||closed||target.getWidth()==0){done.accept(false);return;}
    String script="(()=>{let e=document.elementFromPoint("+(x/target.getResources().getDisplayMetrics().density)+","+(y/target.getResources().getDisplayMetrics().density)+");if(!e)return false;for(;e&&e!==document.documentElement&&e!==document.body;e=e.parentElement){let s=getComputedStyle(e);if(e.matches('input,textarea,select,button,video,audio,[contenteditable=true],[role=slider]')||(/(auto|scroll)/.test(s.overflowX)&&e.scrollWidth>e.clientWidth+2)||(/(auto|scroll)/.test(s.overflowY)&&e.scrollHeight>e.clientHeight+2))return false;}return true})()";
    target.evaluateJavascript(script,result->done.accept(web()==target&&"true".equals(result)));
  }
  String currentUrl(){return web()==null?"":observedUrl(web().getUrl());}
  boolean usable(){return !closed&&web()!=null;}
  void attachIfNeeded(Activity activity,FrameLayout host){if(visibleActivity!=activity||visibleHost!=host||web()!=null&&web().getParent()!=host)attach(activity,host);}
  boolean humanBack(){if(web()==null||!web().canGoBack())return false;humanPriority();restoreCache();invalidate();web().goBack();return true;}
  private WebView reloadTarget;private int previousCache;private Runnable cacheTimeout;
  void humanHardReload(){if(web()==null||closed)return;humanPriority();restoreCache();reloadTarget=web();previousCache=reloadTarget.getSettings().getCacheMode();reloadTarget.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);invalidate();cacheTimeout=this::restoreCache;main.postDelayed(cacheTimeout,15000);reloadTarget.reload();}
  private void restoreCache(){if(cacheTimeout!=null)main.removeCallbacks(cacheTimeout);if(reloadTarget!=null&&!closed)reloadTarget.getSettings().setCacheMode(previousCache);reloadTarget=null;cacheTimeout=null;}
  private WebView web(){return tabs.get(active);}
  private void invalidate(){version=UUID.randomUUID().toString();}
  private String newTab(String url)throws Exception {
    if(tabs.size()>=8)throw new IllegalStateException("TAB_CAPACITY");publicUrl(url);
    String tab=UUID.randomUUID().toString();MutableContextWrapper wrapper=new MutableContextWrapper(presentation.getContext());WebView view=new ControlledWebView(wrapper);contexts.put(view,wrapper);WebSettings s=view.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setMediaPlaybackRequiresUserGesture(true);s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSafeBrowsingEnabled(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(true);
    CookieManager.getInstance().setAcceptThirdPartyCookies(view,false);
    view.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){try{publicUrl(r.getUrl().toString());return false;}catch(Exception e){finishNavigation(error(e));return true;}}
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){try{networkUrl(r.getUrl().toString());return null;}catch(Exception e){if(r.isForMainFrame())main.post(()->{if(v==web()){failedPages.add(v);finishNavigation(error(e));}});return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Map.of(),new ByteArrayInputStream(new byte[0]));}}
      @Override public void onPageStarted(WebView v,String url,Bitmap icon){epochs.put(v,epochs.getOrDefault(v,0L)+1);failedPages.remove(v);if(v==web()){invalidate();state.browser("LOADING");}}
      @Override public void onPageFinished(WebView v,String url){if(v==reloadTarget)restoreCache();CookieManager.getInstance().flush();persistTabs();if(v!=web()||failedPages.contains(v))return;state.browser("READY");if(navigation!=null&&v==web()){Callback c=navigation;navigation=null;observe(true,c);}}
      @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();failedPages.add(v);if(v==web())finishNavigation("TLS_ERROR");}
      @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){failedPages.add(v);if(v==web())finishNavigation("NAVIGATION_FAILED");}}
      @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){finishNavigation("RENDERER_LOST");state.browser("RENDERER_LOST");detach(v);tabs.values().remove(v);contexts.remove(v);epochs.remove(v);v.destroy();return true;}
    });
    view.setWebChromeClient(new WebChromeClient(){
      @Override public void onPermissionRequest(PermissionRequest request){request.deny();state.browser("WEBSITE_PERMISSION_REQUIRED");}
      @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean userGesture,android.os.Message result){state.browser("NEW_WINDOW_REQUIRES_TAB_ACTION");return false;}
      @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;state.browser("FILE_SELECTION_REQUIRED");return true;}
    });
    view.setDownloadListener((downloadUrl,userAgent,disposition,mime,length)->DownloadTransfer.start(context,state,store,downloadUrl,userAgent));
    tabs.put(tab,view);activate(tab);view.loadUrl(url);return tab;
  }
  void chooseFile(Uri uri){if(fileCallback!=null){fileCallback.onReceiveValue(uri==null?null:new Uri[]{uri});fileCallback=null;state.browser("READY");}}
  private void activate(String tab)throws Exception {if(!tabs.containsKey(tab))throw new IllegalStateException("TAB_NOT_FOUND");if(web()!=null)web().onPause();if(web()!=null)detach(web());active=tab;mount();web().onResume();invalidate();}
  private void detach(WebView view){if(view.getParent() instanceof ViewGroup)((ViewGroup)view.getParent()).removeView(view);}
  private void mount(){if(web()==null)return;detach(web());FrameLayout host=visibleHost==null?container:visibleHost;contexts.get(web()).setBaseContext(visibleActivity==null?presentation.getContext():visibleActivity);host.addView(web(),new FrameLayout.LayoutParams(-1,-1));web().requestLayout();web().invalidate();}
  void attach(Activity activity,FrameLayout host){if(closed)return;visibleActivity=activity;visibleHost=host;mount();}
  void detach(Activity activity){if(visibleActivity!=activity)return;visibleHost=null;visibleActivity=null;for(MutableContextWrapper wrapper:contexts.values())if(wrapper.getBaseContext()==activity)wrapper.setBaseContext(presentation.getContext());if(!closed)mount();}
  void humanNavigate(String url,Callback callback){try{if(web()==null||closed){callback.result(null,"BROWSER_UNAVAILABLE");return;}publicUrl(url);restoreCache();if(!state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"HUMAN_CONTROL_REQUIRED");return;}navigation=callback;web().loadUrl(url);}catch(Exception e){callback.result(null,error(e));}}
  private static String error(Exception e){String message=e.getMessage();return message!=null&&message.matches("[A-Z_]{3,60}")?message:"COMMAND_FAILED";}
  private void persistTabs(){try{JSONArray urls=new JSONArray();for(WebView w:tabs.values()){String u=w.getUrl();if(u!=null&&u.startsWith("https://"))urls.put(u);}store.put("tabs",urls.toString());}catch(Exception ignored){state.browser("PERSIST_ERROR");}}
  static String observedUrl(String value){return UrlPolicy.redact(value);}
  static void publicUrl(String url)throws Exception{UrlPolicy.validate(url);}
  static void networkUrl(String url)throws Exception {URI u=new URI(url);if(Set.of("data","blob","about").contains(u.getScheme()))return;publicUrl(url);for(InetAddress a:InetAddress.getAllByName(u.getHost())){byte[] bytes=a.getAddress();boolean uniqueLocal=bytes.length==16&&(bytes[0]&0xfe)==0xfc;if(uniqueLocal||a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress())throw new SecurityException("PRIVATE_NETWORK_BLOCKED");}}
  private void finishNavigation(String error){restoreCache();if(navigation!=null){Callback c=navigation;navigation=null;c.result(null,error);}state.browser(error);}
  void execute(String method,JSONObject payload,Callback callback){long epoch=operationEpoch;main.post(()->{try{
    if(epoch!=operationEpoch){callback.result(null,"EXECUTION_ABORTED");return;}
    if(closed||web()==null){callback.result(null,"BROWSER_UNAVAILABLE");return;}
    if(state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"HUMAN_CONTROL_ACTIVE");return;}
    switch(method){
      case "navigate":publicUrl(payload.getString("url"));navigation=callback;web().loadUrl(payload.getString("url"));main.postDelayed(()->{if(navigation==callback)finishNavigation("NAVIGATION_TIMEOUT");},30000);break;
      case "observe":observe(payload.optBoolean("screenshot",true),callback);break;
      case "locate":locate(payload,callback);break;
      case "action":action(payload,callback);break;
      case "media":media(payload,callback);break;
      default:callback.result(null,"UNSUPPORTED_METHOD");
    }
  }catch(Exception e){callback.result(null,error(e));}});}
  void observe(boolean screenshot,Callback callback){
    java.util.concurrent.atomic.AtomicBoolean delivered=new java.util.concurrent.atomic.AtomicBoolean();
    WebView target=web();Callback once=(o,e)->{if(delivered.compareAndSet(false,true))callback.result(o,e);};
    main.postDelayed(()->{if(delivered.compareAndSet(false,true)){if(target!=null&&!closed){mask(target,false,r->{});callback.result(null,"OBSERVATION_TIMEOUT");}else callback.result(null,"BROWSER_UNAVAILABLE");}},5000);
    observeNow(screenshot,once);
  }
  private void observeNow(boolean screenshot,Callback callback){
    if(closed||web()==null){callback.result(null,"BROWSER_UNAVAILABLE");return;}
    WebView target=web();String tabId=active;invalidate();String stateVersion=version,script=observer.replace("HEY_NAMESPACE",JSONObject.quote(namespace));
    target.evaluateJavascript(script,value->{try{
      if(closed||web()!=target||!active.equals(tabId)){callback.result(null,"OBSERVATION_REPLACED");return;}
      String decoded=new JSONArray("["+value+"]").getString(0);JSONObject observation=new JSONObject(decoded);observation.put("stateVersion",stateVersion).put("tabId",tabId).put("documentEpoch",epochs.getOrDefault(target,0L)).put("observedAt",System.currentTimeMillis());JSONArray list=new JSONArray();for(Map.Entry<String,WebView> t:tabs.entrySet())list.put(new JSONObject().put("tabId",t.getKey()).put("url",observedUrl(t.getValue().getUrl())).put("title",t.getValue().getTitle()));observation.put("tabs",list);
      if(!screenshot){callback.result(observation,null);return;}
      mask(target,true,rects->{target.postVisualStateCallback(SystemClock.uptimeMillis(),new WebView.VisualStateCallback(){@Override public void onComplete(long requestId){capture(target,rects,observation,callback);}});});
    }catch(Exception e){callback.result(null,"OBSERVATION_FAILED");}});
  }
  // The search is read-only. Its result holds an ephemeral ref in this same WebView and stateVersion.
  // Polling handles late-rendered controls; a missing/ambiguous element never triggers a guessed click.
  private void locate(JSONObject query,Callback callback){
    long epoch=operationEpoch;WebView target=web();
    observe(false,(before,error)->{
      if(error!=null){callback.result(null,error);return;}
      if(!operationActive(epoch)||target!=web()){callback.result(null,"OBSERVATION_REPLACED");return;}
      locateUntil(query,before,epoch,target,SystemClock.elapsedRealtime()+3500,callback);
    });
  }
  private void locateUntil(JSONObject query,JSONObject before,long epoch,WebView target,long deadline,Callback callback){
    if(!operationActive(epoch)||target!=web()||!version.equals(before.optString("stateVersion"))){callback.result(null,"STALE_REFERENCE");return;}
    String script=locatorScript.replace("HEY_NAMESPACE",JSONObject.quote(namespace)).replace("HEY_QUERY",JSONObject.quote(query.toString()));
    target.evaluateJavascript(script,value->{
      try{
        if(!operationActive(epoch)||target!=web()||!version.equals(before.optString("stateVersion"))){callback.result(null,"STALE_REFERENCE");return;}
        JSONObject match=new JSONObject(new JSONArray("["+value+"]").getString(0));
        String reason=match.optString("error");
        if("ELEMENT_NOT_FOUND".equals(reason)&&SystemClock.elapsedRealtime()<deadline){
          main.postDelayed(()->locateUntil(query,before,epoch,target,deadline,callback),180);return;
        }
        if(!reason.isEmpty()){callback.result(null,reason);return;}
        before.put("locator",match).put("postconditionVerified",match.optBoolean("locatorUnique"));
        callback.result(before,null);
      }catch(Exception e){callback.result(null,"LOCATOR_EVALUATION_FAILED");}
    });
  }
  interface MaskCallback {void done(JSONArray rects);}
  private void mask(WebView target,boolean enabled,MaskCallback done){String key=namespace+"_mask";
    String script=enabled?"(()=>{let old=document.getElementById("+JSONObject.quote(key)+");if(old)old.remove();let root=document.createElement('div'),rects=[];root.id="+JSONObject.quote(key)+";for(let e of document.querySelectorAll('input[type=password],input[autocomplete=one-time-code],input[autocomplete=cc-number],input[autocomplete=cc-csc]')){let r=e.getBoundingClientRect();rects.push({x:r.x,y:r.y,w:r.width,h:r.height});let m=document.createElement('div');Object.assign(m.style,{position:'fixed',left:r.x+'px',top:r.y+'px',width:r.width+'px',height:r.height+'px',background:'#e5e5e5',zIndex:'2147483647',pointerEvents:'none'});root.appendChild(m);}document.documentElement.appendChild(root);return JSON.stringify({rects,width:innerWidth,height:innerHeight})})()":"document.getElementById("+JSONObject.quote(key)+")?.remove()";
    target.evaluateJavascript(script,v->{try{JSONObject data=new JSONObject(new JSONArray("["+v+"]").getString(0));JSONArray rects=data.getJSONArray("rects");double scale=target.getWidth()/data.getDouble("width");for(int i=0;i<rects.length();i++)rects.getJSONObject(i).put("scale",scale);done.done(rects);}catch(Exception e){if(!enabled)done.done(new JSONArray());else done.done(null);}});
  }
  private void capture(WebView target,JSONArray rects,JSONObject o,Callback callback){
    if(closed||web()!=target){mask(target,false,r->callback.result(null,"OBSERVATION_REPLACED"));return;}
    if(rects==null){captureDone(target,null,null,"REDACTION_UNAVAILABLE",o,callback);return;}
    int width=target.getWidth(),height=target.getHeight();if(width<=0||height<=0){captureDone(target,null,rects,"SURFACE_UNAVAILABLE",o,callback);return;}
    Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);int[] xy=new int[2];target.getLocationInWindow(xy);Rect visible=new Rect();if(visibleActivity!=null&&(!target.getGlobalVisibleRect(visible)||visible.width()!=width||visible.height()!=height)){bitmap.recycle();captureDocument(target,rects,o,callback);return;}
    android.view.Window window=visibleActivity!=null?visibleActivity.getWindow():presentation.getWindow();
    try{PixelCopy.request(window,new Rect(xy[0],xy[1],xy[0]+width,xy[1]+height),bitmap,result->{if(result==PixelCopy.SUCCESS&&hasVisualDetail(bitmap))captureDone(target,bitmap,rects,"COMPOSITED_SURFACE",o,callback);else{bitmap.recycle();captureDocument(target,rects,o,callback);}},main);}catch(Exception e){bitmap.recycle();captureDocument(target,rects,o,callback);}
  }
  // A flat/blank PixelCopy is not trusted as a captured webpage; canvas is only a fallback.
  private static boolean hasVisualDetail(Bitmap image){
    if(image==null||image.getWidth()<2||image.getHeight()<2)return false;
    int first=image.getPixel(0,0)&0x00ffffff;
    for(int row=0;row<9;row++)for(int col=0;col<9;col++){
      int px=image.getPixel(col*(image.getWidth()-1)/8,row*(image.getHeight()-1)/8)&0x00ffffff;
      if(Math.abs((px&255)-(first&255))+Math.abs(((px>>8)&255)-((first>>8)&255))+Math.abs(((px>>16)&255)-((first>>16)&255))>36)return true;
    }
    return false;
  }
  private void captureDocument(WebView target,JSONArray rects,JSONObject o,Callback callback){
    try{Bitmap bitmap=Bitmap.createBitmap(target.getWidth(),target.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);target.draw(canvas);captureDone(target,bitmap,rects,"DOCUMENT_CANVAS",o,callback);}catch(Exception e){captureDone(target,null,rects,"CAPTURE_FAILED",o,callback);}
  }
  private void captureDone(WebView target,Bitmap bitmap,JSONArray rects,String mode,JSONObject o,Callback callback){
    try{if(bitmap!=null&&web()==target&&!closed){Canvas canvas=new Canvas(bitmap);Paint paint=new Paint();paint.setColor(0xffe5e5e5);for(int i=0;i<rects.length();i++){JSONObject r=rects.getJSONObject(i);float k=(float)r.getDouble("scale");canvas.drawRect((float)r.getDouble("x")*k-2,(float)r.getDouble("y")*k-2,(float)(r.getDouble("x")+r.getDouble("w"))*k+2,(float)(r.getDouble("y")+r.getDouble("h"))*k+2,paint);}ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,65,bytes);o.put("image",new JSONObject().put("mimeType","image/jpeg").put("data",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP))).put("screenshotStatus",hasVisualDetail(bitmap)?"MASKED":"SUSPECT_BLANK").put("captureMode",mode).put("visualMediaVerified",mode.equals("COMPOSITED_SURFACE")&&hasVisualDetail(bitmap)).put("frameAt",System.currentTimeMillis());}else o.put("screenshotStatus","UNAVAILABLE").put("captureMode",mode);}catch(Exception e){try{o.remove("image");o.put("screenshotStatus","ERROR");}catch(Exception ignored){}}finally{if(bitmap!=null)bitmap.recycle();}
    mask(target,false,r->callback.result(o,null));
  }
  private void action(JSONObject p,Callback callback)throws Exception {
    long epoch=operationEpoch;String action=p.getString("action");boolean refAction=action.equals("click")||action.equals("fill");
    if(refAction&&!version.equals(p.optString("stateVersion"))){callback.result(null,"STALE_REFERENCE");return;}
    String selected=namespace+"_target";
    Runnable run=()->observe(false,(before,error)->{if(epoch!=operationEpoch||closed||state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"EXECUTION_ABORTED");return;}if(error!=null){callback.result(null,error);return;}
      try{
        switch(action){
          case "fill":String script="(()=>{let e=window["+JSONObject.quote(selected)+"];if(!e?.isConnected)return 'STALE_REFERENCE';if(e.disabled||e.readOnly)return 'NOT_EDITABLE';if(!(e instanceof HTMLTextAreaElement||e instanceof HTMLInputElement&&/^(text|email|password|tel|url|number|search)$/.test(e.type)))return 'NOT_EDITABLE';let proto=e instanceof HTMLTextAreaElement?HTMLTextAreaElement.prototype:HTMLInputElement.prototype,setter=Object.getOwnPropertyDescriptor(proto,'value')?.set;if(!setter)return 'UNSUPPORTED_FILL';e.focus();setter.call(e,"+JSONObject.quote(p.getString("text"))+");e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));return e.value==="+JSONObject.quote(p.getString("text"))+"?'OK':'FILL_NOT_APPLIED'})()";
            web().evaluateJavascript(script,v->{if(!"\"OK\"".equals(v)){callback.result(null,v.contains("NOT_EDITABLE")?"NOT_EDITABLE":v.contains("STALE_REFERENCE")?"STALE_REFERENCE":"FILL_NOT_APPLIED");return;}afterAction(p,before,true,callback);});return;
          case "click":long clickedAt=SystemClock.elapsedRealtime();waitForClick(epoch,SystemClock.elapsedRealtime()+3500,null,(rect,reason)->{
             if(reason!=null){callback.result(null,reason);return;}
             try{
               if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}
               float x=(float)(rect.getDouble("x")*rect.getDouble("scale")),y=(float)(rect.getDouble("y")*rect.getDouble("scale"));
               touch(x,y,x,y);p.put("actionabilityWaitMs",SystemClock.elapsedRealtime()-clickedAt);
               afterAction(p,before,false,callback);
             }catch(Exception e){callback.result(null,error(e));}
           });return;
          case "back":if(!web().canGoBack()){callback.result(null,"HISTORY_UNAVAILABLE");return;}web().goBack();break;
          case "forward":if(!web().canGoForward()){callback.result(null,"HISTORY_UNAVAILABLE");return;}web().goForward();break;
          case "reload":web().reload();break;
          case "scroll":String scroll="(()=>{let e=document.elementFromPoint(innerWidth/2,innerHeight/2);while(e&&e!==document.documentElement){let s=getComputedStyle(e);if((/(auto|scroll)/.test(s.overflowY)&&e.scrollHeight>e.clientHeight+2)||(/(auto|scroll)/.test(s.overflowX)&&e.scrollWidth>e.clientWidth+2)){e.scrollBy({left:"+p.getInt("x")+",top:"+p.getInt("y")+",behavior:'instant'});return true;}e=e.parentElement;}window.scrollBy({left:"+p.getInt("x")+",top:"+p.getInt("y")+",behavior:'instant'});return true})()";
             web().evaluateJavascript(scroll,value->{if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}afterAction(p,before,false,callback);});return;
          case "drag":touch((float)p.getDouble("x"),(float)p.getDouble("y"),(float)p.getDouble("toX"),(float)p.getDouble("toY"));break;
          case "key":int key=switch(p.getString("value")){case "ENTER"->KeyEvent.KEYCODE_ENTER;case "TAB"->KeyEvent.KEYCODE_TAB;case "ESCAPE"->KeyEvent.KEYCODE_ESCAPE;default->throw new IllegalStateException("UNSUPPORTED_KEY");};web().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));web().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));break;
          case "tab_open":newTab(p.getString("url"));break;
          case "tab_activate":activate(p.getString("value"));break;
          case "tab_close":String tab=p.getString("value");if(!tabs.containsKey(tab))throw new IllegalStateException("TAB_NOT_FOUND");if(tabs.size()==1)throw new IllegalStateException("LAST_TAB_PROTECTED");WebView removed=tabs.remove(tab);detach(removed);contexts.remove(removed);epochs.remove(removed);removed.destroy();if(tab.equals(active))activate(tabs.keySet().iterator().next());break;
          default:throw new IllegalStateException("UNSUPPORTED_ACTION");
        }
        afterAction(p,before,false,callback);
      }catch(Exception e){callback.result(null,error(e));}
    });
    if(refAction)web().evaluateJavascript("(()=>{let e=window["+JSONObject.quote(namespace)+"]?.refs["+JSONObject.quote(p.getString("ref"))+"];if(!e)return 'REF_NOT_FOUND';if(!e.isConnected)return 'STALE_REFERENCE';window["+JSONObject.quote(selected)+"]=e;return 'OK'})()",v->{if("\"OK\"".equals(v))run.run();else callback.result(null,v.contains("REF_NOT_FOUND")?"REF_NOT_FOUND":"STALE_REFERENCE");});else run.run();
  }
  interface ClickReady{void done(JSONObject point,String error);}
  private void waitForClick(long epoch,long deadline,JSONObject previous,ClickReady callback){
    if(!operationActive(epoch)){callback.done(null,"EXECUTION_ABORTED");return;}
    WebView target=web();String script=actionabilityScript.replace("HEY_TARGET",JSONObject.quote(namespace+"_target")).replace("HEY_WEB_WIDTH",String.valueOf(target.getWidth()));
    target.evaluateJavascript(script,value->{
      try{
        if(!operationActive(epoch)||web()!=target){callback.done(null,"EXECUTION_ABORTED");return;}
        JSONObject point=new JSONObject(new JSONArray("["+value+"]").getString(0));String reason=point.optString("error");
        if(reason.isEmpty()){
          boolean stable=previous!=null&&Math.abs(previous.optDouble("x")-point.optDouble("x"))<1&&Math.abs(previous.optDouble("y")-point.optDouble("y"))<1;
          if(stable){callback.done(point,null);return;}
          if(SystemClock.elapsedRealtime()<deadline){main.postDelayed(()->waitForClick(epoch,deadline,point,callback),110);return;}
          callback.done(null,"ELEMENT_NOT_STABLE");return;
        }
        if("STALE_REFERENCE".equals(reason)||"ELEMENT_DISABLED".equals(reason)){callback.done(null,reason);return;}
        if(SystemClock.elapsedRealtime()>=deadline){callback.done(null,reason);return;}
        main.postDelayed(()->waitForClick(epoch,deadline,null,callback),160);
      }catch(Exception e){callback.done(null,"ACTIONABILITY_FAILED");}
    });
  }
  private void afterAction(JSONObject p,JSONObject before,boolean fillVerified,Callback callback){
    long epoch=operationEpoch;invalidate();long deadline=SystemClock.elapsedRealtime()+5000;
    Runnable[] wait=new Runnable[1];wait[0]=()->{if(epoch!=operationEpoch||closed||state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"HUMAN_CONTROL_ACTIVE");return;}
      observe(false,(after,error)->{if(error!=null){callback.result(null,error);return;}boolean verified=BrowserVerifier.action(p,before,after,fillVerified);
        if(!verified&&SystemClock.elapsedRealtime()<deadline&&Set.of("tab_open","tab_activate","tab_close","back","forward","reload","click","key","scroll","drag").contains(p.optString("action"))){main.postDelayed(wait[0],200);return;}
        observe(true,(result,failure)->{if(result==null){callback.result(null,failure);return;}if(p.optString("action").equals("fill")){web().evaluateJavascript("(()=>{let e=window["+JSONObject.quote(namespace+"_target")+"];return !!e?.isConnected && e.value==="+JSONObject.quote(p.optString("text"))+"})()",v->{try{result.put("postconditionVerified",v.equals("true")).put("postcondition","fill");}catch(Exception ignored){}callback.result(result,failure);});}else{try{result.put("postconditionVerified",verified).put("postcondition",p.optString("action"));if(p.has("actionabilityWaitMs"))result.put("actionabilityWaitMs",p.optLong("actionabilityWaitMs"));}catch(Exception ignored){}callback.result(result,failure);}});
      });};main.postDelayed(wait[0],200);
  }
  private void media(JSONObject p,Callback callback)throws Exception {
    long epoch=operationEpoch;String action=p.getString("action"),command=switch(action){case "play"->"e.play().catch(()=>{})";case "pause"->"e.pause()";case "mute"->"e.muted=true";case "unmute"->"e.muted=false";case "seek"->"e.currentTime="+p.getDouble("seconds");default->throw new IllegalStateException("INVALID_MEDIA_ACTION");};
    observe(false,(before,error)->{if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}if(error!=null){callback.result(null,error);return;}JSONObject m=BrowserVerifier.media(before);if(m==null){callback.result(null,"MEDIA_NOT_FOUND");return;}
      web().evaluateJavascript("(()=>{let e=document.querySelector('video,audio');if(!e)return 'NO_MEDIA';"+command+";return 'REQUESTED'})()",v->{if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}if(v.contains("NO_MEDIA")){callback.result(null,"MEDIA_NOT_FOUND");return;}
        waitMedia(p,before,callback,SystemClock.elapsedRealtime()+8000,false,epoch);
      });
    });
  }
  private void waitMedia(JSONObject p,JSONObject before,Callback callback,long deadline,boolean tapped,long epoch){
    if(epoch!=operationEpoch||closed||state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"EXECUTION_ABORTED");return;}
    observe(false,(after,error)->{if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}if(error!=null){callback.result(null,error);return;}boolean verified=BrowserVerifier.mediaAction(p,before,after);JSONObject m=BrowserVerifier.media(after);
      if(verified){observe(true,(result,failure)->{if(result!=null)try{result.put("postconditionVerified",true);}catch(Exception ignored){}callback.result(result,failure);});return;}
      if(SystemClock.elapsedRealtime()>deadline){callback.result(null,p.optString("action").equals("play")?"PLAYBACK_NOT_ADVANCING":"MEDIA_POSTCONDITION_FAILED");return;}
      if(p.optString("action").equals("play")&&m!=null&&m.optBoolean("paused",true)&&!tapped){tapPlay(epoch,()->main.postDelayed(()->waitMedia(p,before,callback,deadline,true,epoch),250));return;}
      main.postDelayed(()->waitMedia(p,before,callback,deadline,tapped,epoch),200);
    });
  }
  private boolean operationActive(long epoch){return epoch==operationEpoch&&!closed&&!state.snapshot().optString("control").equals("HUMAN");}
  private void tapPlay(long epoch,Runnable done){web().evaluateJavascript("(()=>{let e=document.querySelector('.ytp-play-button,button[aria-label*=Play],button[aria-label*=Putar]')||document.querySelector('video');if(!e)return null;let r=e.getBoundingClientRect();return JSON.stringify({x:r.x+r.width/2,y:r.y+r.height/2,k:"+web().getWidth()+"/innerWidth})})()",v->{if(!operationActive(epoch))return;try{JSONObject r=new JSONObject(new JSONArray("["+v+"]").getString(0));float x=(float)(r.getDouble("x")*r.getDouble("k")),y=(float)(r.getDouble("y")*r.getDouble("k"));touch(x,y,x,y);}catch(Exception ignored){}done.run();});}
  // All readiness/seek/play steps run locally; no MCP polling round trips.
  void prepareWatch(Runnable arm,Callback callback){long epoch=operationEpoch;execute("media",json("action","pause"),(paused,error)->{
    if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}if(error!=null){callback.result(null,error);return;}waitReady(epoch,SystemClock.elapsedRealtime()+10000,()->{if(!operationActive(epoch))return;arm.run();execute("media",seekZero(),(zero,seekError)->{
      if(seekError!=null){callback.result(null,seekError);return;}callback.result(zero,null);
    });},callback);
  });}
  private JSONObject json(String key,String value){JSONObject j=new JSONObject();try{j.put(key,value);}catch(Exception ignored){}return j;}
  private JSONObject seekZero(){JSONObject j=json("action","seek");try{j.put("seconds",0);}catch(Exception ignored){}return j;}
  private void waitReady(long epoch,long deadline,Runnable done,Callback callback){if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}observe(false,(o,e)->{if(!operationActive(epoch)){callback.result(null,"EXECUTION_ABORTED");return;}JSONObject m=o==null?null:BrowserVerifier.media(o);if(e!=null||m==null){callback.result(null,e==null?"MEDIA_NOT_FOUND":e);return;}if(m.optInt("readyState")>=2){done.run();return;}if(SystemClock.elapsedRealtime()>deadline){callback.result(null,"MEDIA_NOT_READY");return;}main.postDelayed(()->waitReady(epoch,deadline,done,callback),200);});}
  void abort(){restoreCache();operationEpoch++;if(web()!=null)web().stopLoading();navigation=null;invalidate();}
  void manualTouch(float x,float y,float toX,float toY){main.post(()->{if(web()!=null&&state.snapshot().optString("control").equals("HUMAN")){try{touch(x,y,toX,toY);invalidate();}catch(IllegalArgumentException ignored){state.browser("INPUT_OUTSIDE_VIEWPORT");}}});}
  private void touch(float x,float y,float toX,float toY){int w=web().getWidth(),h=web().getHeight();if(x<0||y<0||x>=w||y>=h||toX<0||toY<0||toX>=w||toY>=h)throw new IllegalArgumentException("OUTSIDE_VIEWPORT");long time=SystemClock.uptimeMillis();dispatch(time,time,MotionEvent.ACTION_DOWN,x,y);if(x!=toX||y!=toY)for(int i=1;i<=8;i++)dispatch(time,time+i*16,MotionEvent.ACTION_MOVE,x+(toX-x)*i/8,y+(toY-y)*i/8);dispatch(time,time+150,MotionEvent.ACTION_UP,toX,toY);}
  private void dispatch(long down,long at,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,at,action,x,y,0);injectingInput=true;try{web().dispatchTouchEvent(e);}finally{injectingInput=false;e.recycle();}}
  Bitmap preview(){screenshotPending=true;if(web()!=null)web().invalidate();return frame;}
  @Override public void close(){restoreCache();closed=true;persistTabs();CookieManager.getInstance().flush();if(fileCallback!=null)fileCallback.onReceiveValue(null);for(WebView w:tabs.values()){detach(w);w.destroy();}contexts.clear();visibleActivity=null;visibleHost=null;tabs.clear();presentation.dismiss();display.release();reader.close();if(frame!=null)frame.recycle();}
}
