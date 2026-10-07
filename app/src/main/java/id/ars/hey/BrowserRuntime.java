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
  private String observer;private Callback navigation;private Bitmap frame;private long frameAt;private boolean screenshotPending,closed;private ValueCallback<Uri[]> fileCallback;
  BrowserRuntime(Context context,StateStore state,SecureStore store)throws Exception {
    this.context=context;this.state=state;this.store=store;
    try(InputStream in=context.getAssets().open("observe.js")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;while((n=in.read(chunk))!=-1)out.write(chunk,0,n);observer=out.toString("UTF-8");}
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
  private final class ControlledWebView extends WebView {
    ControlledWebView(Context context){super(context);}
    @Override public boolean dispatchTouchEvent(MotionEvent event){if(!injectingInput&&!state.snapshot().optString("control").equals("HUMAN"))return true;return super.dispatchTouchEvent(event);}
    @Override public boolean performAccessibilityAction(int action,Bundle args){if(!state.snapshot().optString("control").equals("HUMAN"))return false;return super.performAccessibilityAction(action,args);}
  }
  private WebView web(){return tabs.get(active);}
  private void invalidate(){version=UUID.randomUUID().toString();}
  private String newTab(String url)throws Exception {
    if(tabs.size()>=8)throw new IllegalStateException("TAB_CAPACITY");publicUrl(url);
    String tab=UUID.randomUUID().toString();MutableContextWrapper wrapper=new MutableContextWrapper(presentation.getContext());WebView view=new ControlledWebView(wrapper);contexts.put(view,wrapper);WebSettings s=view.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setMediaPlaybackRequiresUserGesture(true);s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSafeBrowsingEnabled(true);s.setBuiltInZoomControls(false);
    CookieManager.getInstance().setAcceptThirdPartyCookies(view,false);
    view.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){try{publicUrl(r.getUrl().toString());return false;}catch(Exception e){finishNavigation(error(e));return true;}}
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){try{networkUrl(r.getUrl().toString());return null;}catch(Exception e){if(r.isForMainFrame())main.post(()->finishNavigation(error(e)));return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Map.of(),new ByteArrayInputStream(new byte[0]));}}
      @Override public void onPageStarted(WebView v,String url,Bitmap icon){epochs.put(v,epochs.getOrDefault(v,0L)+1);invalidate();state.browser("LOADING");}
      @Override public void onPageFinished(WebView v,String url){CookieManager.getInstance().flush();persistTabs();state.browser("READY");if(navigation!=null&&v==web()){Callback c=navigation;navigation=null;observe(true,c);}}
      @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();finishNavigation("TLS_ERROR");}
      @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())finishNavigation("NAVIGATION_FAILED");}
      @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail detail){finishNavigation("RENDERER_LOST");state.browser("RENDERER_LOST");container.removeView(v);tabs.values().remove(v);v.destroy();return true;}
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
  void humanNavigate(String url,Callback callback){try{publicUrl(url);if(!state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"HUMAN_CONTROL_REQUIRED");return;}navigation=callback;web().loadUrl(url);}catch(Exception e){callback.result(null,error(e));}}
  private static String error(Exception e){String message=e.getMessage();return message!=null&&message.matches("[A-Z_]{3,60}")?message:"COMMAND_FAILED";}
  private void persistTabs(){try{JSONArray urls=new JSONArray();for(WebView w:tabs.values()){String u=w.getUrl();if(u!=null&&u.startsWith("https://"))urls.put(u);}store.put("tabs",urls.toString());}catch(Exception ignored){state.browser("PERSIST_ERROR");}}
  static String observedUrl(String value){return UrlPolicy.redact(value);}
  static void publicUrl(String url)throws Exception{UrlPolicy.validate(url);}
  static void networkUrl(String url)throws Exception {URI u=new URI(url);if(Set.of("data","blob","about").contains(u.getScheme()))return;publicUrl(url);for(InetAddress a:InetAddress.getAllByName(u.getHost())){byte[] bytes=a.getAddress();boolean uniqueLocal=bytes.length==16&&(bytes[0]&0xfe)==0xfc;if(uniqueLocal||a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress())throw new SecurityException("PRIVATE_NETWORK_BLOCKED");}}
  private void finishNavigation(String error){if(navigation!=null){Callback c=navigation;navigation=null;c.result(null,error);}state.browser(error);}
  void execute(String method,JSONObject payload,Callback callback){long epoch=operationEpoch;main.post(()->{try{
    if(epoch!=operationEpoch){callback.result(null,"EXECUTION_ABORTED");return;}
    if(closed||web()==null){callback.result(null,"BROWSER_UNAVAILABLE");return;}
    if(state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"HUMAN_CONTROL_ACTIVE");return;}
    switch(method){
      case "navigate":publicUrl(payload.getString("url"));navigation=callback;web().loadUrl(payload.getString("url"));main.postDelayed(()->{if(navigation==callback)finishNavigation("NAVIGATION_TIMEOUT");},30000);break;
      case "observe":observe(payload.optBoolean("screenshot",true),callback);break;
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
    try{PixelCopy.request(window,new Rect(xy[0],xy[1],xy[0]+width,xy[1]+height),bitmap,result->{if(result==PixelCopy.SUCCESS)captureDone(target,bitmap,rects,"COMPOSITED_SURFACE",o,callback);else{bitmap.recycle();captureDocument(target,rects,o,callback);}},main);}catch(Exception e){bitmap.recycle();captureDocument(target,rects,o,callback);}
  }
  private void captureDocument(WebView target,JSONArray rects,JSONObject o,Callback callback){
    try{Bitmap bitmap=Bitmap.createBitmap(target.getWidth(),target.getHeight(),Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(bitmap);target.draw(canvas);captureDone(target,bitmap,rects,"DOCUMENT_CANVAS",o,callback);}catch(Exception e){captureDone(target,null,rects,"CAPTURE_FAILED",o,callback);}
  }
  private void captureDone(WebView target,Bitmap bitmap,JSONArray rects,String mode,JSONObject o,Callback callback){
    try{if(bitmap!=null&&web()==target&&!closed){Canvas canvas=new Canvas(bitmap);Paint paint=new Paint();paint.setColor(0xffe5e5e5);for(int i=0;i<rects.length();i++){JSONObject r=rects.getJSONObject(i);float k=(float)r.getDouble("scale");canvas.drawRect((float)r.getDouble("x")*k-2,(float)r.getDouble("y")*k-2,(float)(r.getDouble("x")+r.getDouble("w"))*k+2,(float)(r.getDouble("y")+r.getDouble("h"))*k+2,paint);}ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,65,bytes);o.put("image",new JSONObject().put("mimeType","image/jpeg").put("data",Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP))).put("screenshotStatus","MASKED").put("captureMode",mode).put("visualMediaVerified",mode.equals("COMPOSITED_SURFACE")).put("frameAt",System.currentTimeMillis());}else o.put("screenshotStatus","UNAVAILABLE").put("captureMode",mode);}catch(Exception e){try{o.remove("image");o.put("screenshotStatus","ERROR");}catch(Exception ignored){}}finally{if(bitmap!=null)bitmap.recycle();}
    mask(target,false,r->callback.result(o,null));
  }
  private void action(JSONObject p,Callback callback)throws Exception {
    long epoch=operationEpoch;String action=p.getString("action");boolean refAction=action.equals("click")||action.equals("fill");
    if(refAction&&!version.equals(p.optString("stateVersion"))){callback.result(null,"STALE_REFERENCE");return;}
    String selected=namespace+"_target";
    Runnable run=()->observe(false,(before,error)->{if(epoch!=operationEpoch||closed||state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"EXECUTION_ABORTED");return;}if(error!=null){callback.result(null,error);return;}
      try{
        switch(action){
          case "fill":String script="(()=>{let e=window["+JSONObject.quote(selected)+"];if(!e?.isConnected)return 'STALE_REFERENCE';if(e.disabled)return 'DISABLED';let proto=e instanceof HTMLTextAreaElement?HTMLTextAreaElement.prototype:HTMLInputElement.prototype,setter=Object.getOwnPropertyDescriptor(proto,'value')?.set;if(!setter)return 'UNSUPPORTED_FILL';e.focus();setter.call(e,"+JSONObject.quote(p.getString("text"))+");e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));return e.value==="+JSONObject.quote(p.getString("text"))+"?'OK':'FILL_NOT_APPLIED'})()";
            web().evaluateJavascript(script,v->{if(!"\"OK\"".equals(v)){callback.result(null,"FILL_NOT_APPLIED");return;}afterAction(p,before,true,callback);});return;
          case "click":web().evaluateJavascript("(()=>{let e=window["+JSONObject.quote(selected)+"];if(!e?.isConnected)return JSON.stringify({error:'STALE_REFERENCE'});if(e.disabled)return JSON.stringify({error:'DISABLED'});let r=e.getBoundingClientRect();if(r.x+r.width/2<0||r.y+r.height/2<0||r.x+r.width/2>=innerWidth||r.y+r.height/2>=innerHeight)return JSON.stringify({error:'ELEMENT_OUTSIDE_VIEWPORT'});return JSON.stringify({x:r.x+r.width/2,y:r.y+r.height/2,scale:"+web().getWidth()+"/innerWidth})})()",v->{try{if(epoch!=operationEpoch)return;JSONObject rect=new JSONObject(new JSONArray("["+v+"]").getString(0));if(rect.has("error")){callback.result(null,rect.getString("error"));return;}touch((float)(rect.getDouble("x")*rect.getDouble("scale")),(float)(rect.getDouble("y")*rect.getDouble("scale")),(float)(rect.getDouble("x")*rect.getDouble("scale")),(float)(rect.getDouble("y")*rect.getDouble("scale")));afterAction(p,before,false,callback);}catch(Exception e){callback.result(null,error(e));}});return;
          case "back":if(!web().canGoBack()){callback.result(null,"HISTORY_UNAVAILABLE");return;}web().goBack();break;
          case "forward":if(!web().canGoForward()){callback.result(null,"HISTORY_UNAVAILABLE");return;}web().goForward();break;
          case "reload":web().reload();break;
          case "scroll":web().scrollBy(p.getInt("x"),p.getInt("y"));break;
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
  private void afterAction(JSONObject p,JSONObject before,boolean fillVerified,Callback callback){
    long epoch=operationEpoch;invalidate();long deadline=SystemClock.elapsedRealtime()+5000;
    Runnable[] wait=new Runnable[1];wait[0]=()->{if(epoch!=operationEpoch||closed||state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"HUMAN_CONTROL_ACTIVE");return;}
      observe(false,(after,error)->{if(error!=null){callback.result(null,error);return;}boolean verified=BrowserVerifier.action(p,before,after,fillVerified);
        if(!verified&&SystemClock.elapsedRealtime()<deadline&&Set.of("tab_open","back","forward","reload","click").contains(p.optString("action"))){main.postDelayed(wait[0],200);return;}
        observe(true,(result,failure)->{if(result==null){callback.result(null,failure);return;}if(p.optString("action").equals("fill")){web().evaluateJavascript("(()=>{let e=window["+JSONObject.quote(namespace+"_target")+"];return !!e?.isConnected && e.value==="+JSONObject.quote(p.optString("text"))+"})()",v->{try{result.put("postconditionVerified",v.equals("true")).put("postcondition","fill");}catch(Exception ignored){}callback.result(result,failure);});}else{try{result.put("postconditionVerified",verified).put("postcondition",p.optString("action"));}catch(Exception ignored){}callback.result(result,failure);}});
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
  void abort(){operationEpoch++;if(web()!=null)web().stopLoading();navigation=null;invalidate();}
  void manualTouch(float x,float y,float toX,float toY){main.post(()->{if(web()!=null&&state.snapshot().optString("control").equals("HUMAN")){try{touch(x,y,toX,toY);invalidate();}catch(IllegalArgumentException ignored){state.browser("INPUT_OUTSIDE_VIEWPORT");}}});}
  private void touch(float x,float y,float toX,float toY){int w=web().getWidth(),h=web().getHeight();if(x<0||y<0||x>=w||y>=h||toX<0||toY<0||toX>=w||toY>=h)throw new IllegalArgumentException("OUTSIDE_VIEWPORT");long time=SystemClock.uptimeMillis();dispatch(time,time,MotionEvent.ACTION_DOWN,x,y);if(x!=toX||y!=toY)for(int i=1;i<=8;i++)dispatch(time,time+i*16,MotionEvent.ACTION_MOVE,x+(toX-x)*i/8,y+(toY-y)*i/8);dispatch(time,time+150,MotionEvent.ACTION_UP,toX,toY);}
  private void dispatch(long down,long at,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,at,action,x,y,0);injectingInput=true;try{web().dispatchTouchEvent(e);}finally{injectingInput=false;e.recycle();}}
  Bitmap preview(){screenshotPending=true;if(web()!=null)web().invalidate();return frame;}
  @Override public void close(){closed=true;persistTabs();CookieManager.getInstance().flush();if(fileCallback!=null)fileCallback.onReceiveValue(null);for(WebView w:tabs.values()){detach(w);w.destroy();}contexts.clear();visibleActivity=null;visibleHost=null;tabs.clear();presentation.dismiss();display.release();reader.close();if(frame!=null)frame.recycle();}
}
