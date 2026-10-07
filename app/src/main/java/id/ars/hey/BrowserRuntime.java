package id.ars.hey;

import android.app.Presentation;
import android.app.DownloadManager;
import android.content.Context;
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
  private WebView web(){return tabs.get(active);}
  private void invalidate(){version=UUID.randomUUID().toString();}
  private String newTab(String url)throws Exception {
    if(tabs.size()>=8)throw new IllegalStateException("TAB_CAPACITY");publicUrl(url);
    String tab=UUID.randomUUID().toString();WebView view=new WebView(presentation.getContext());WebSettings s=view.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setMediaPlaybackRequiresUserGesture(true);s.setSupportMultipleWindows(true);s.setJavaScriptCanOpenWindowsAutomatically(false);s.setSafeBrowsingEnabled(true);s.setBuiltInZoomControls(false);
    CookieManager.getInstance().setAcceptThirdPartyCookies(view,false);
    view.setWebViewClient(new WebViewClient(){
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){try{publicUrl(r.getUrl().toString());return false;}catch(Exception e){state.browser("NAVIGATION_BLOCKED");return true;}}
      @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){try{networkUrl(r.getUrl().toString());return null;}catch(Exception e){return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",Map.of(),new ByteArrayInputStream(new byte[0]));}}
      @Override public void onPageStarted(WebView v,String url,Bitmap icon){invalidate();state.browser("LOADING");}
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
    view.setDownloadListener((downloadUrl,userAgent,disposition,mime,length)->{new Thread(()->{try{networkUrl(downloadUrl);DownloadManager.Request request=new DownloadManager.Request(Uri.parse(downloadUrl));request.addRequestHeader("User-Agent",userAgent);String cookies=CookieManager.getInstance().getCookie(downloadUrl);if(cookies!=null)request.addRequestHeader("Cookie",cookies);request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);request.setDestinationInExternalFilesDir(context,"Downloads","hey-"+UUID.randomUUID());long download=context.getSystemService(DownloadManager.class).enqueue(request);state.browser("DOWNLOAD_QUEUED");store.put("lastDownload",String.valueOf(download));}catch(Exception e){state.browser("DOWNLOAD_FAILED");}}).start();});
    tabs.put(tab,view);activate(tab);view.loadUrl(url);return tab;
  }
  void chooseFile(Uri uri){if(fileCallback!=null){fileCallback.onReceiveValue(uri==null?null:new Uri[]{uri});fileCallback=null;state.browser("READY");}}
  private void activate(String tab)throws Exception {if(!tabs.containsKey(tab))throw new IllegalStateException("TAB_NOT_FOUND");if(web()!=null)web().onPause();container.removeAllViews();active=tab;container.addView(web(),new FrameLayout.LayoutParams(-1,-1));web().onResume();invalidate();}
  private void persistTabs(){try{JSONArray urls=new JSONArray();for(WebView w:tabs.values()){String u=w.getUrl();if(u!=null&&u.startsWith("https://"))urls.put(u);}store.put("tabs",urls.toString());}catch(Exception ignored){state.browser("PERSIST_ERROR");}}
  private static String observedUrl(String value){if(value==null)return "";try{Uri u=Uri.parse(value);if(!u.isHierarchical())return "";Uri.Builder b=u.buildUpon().clearQuery();for(String key:u.getQueryParameterNames())for(String v:u.getQueryParameters(key))b.appendQueryParameter(key,key.matches("(?i)access_token|refresh_token|id_token|token|code|password|secret|api_key|apikey|otp|authorization|session|credential")?"[redacted]":v);if(u.getFragment()!=null&&u.getFragment().matches("(?is).*(access_token|id_token|token|password|secret)=.*"))b.fragment("[redacted]");return b.build().toString();}catch(Exception e){return "";}}
  static void publicUrl(String url)throws Exception {URI u=new URI(url);String h=u.getHost();if(h!=null)h=h.toLowerCase(Locale.ROOT);if(!"https".equals(u.getScheme())||h==null||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||h.equals("localhost")||h.endsWith(".localhost")||h.endsWith(".local")||h.endsWith(".internal")||h.matches("[0-9.]+")||h.contains(":"))throw new SecurityException("PUBLIC_HTTPS_REQUIRED");}
  static void networkUrl(String url)throws Exception {URI u=new URI(url);if(Set.of("data","blob","about").contains(u.getScheme()))return;publicUrl(url);for(InetAddress a:InetAddress.getAllByName(u.getHost())){byte[] bytes=a.getAddress();boolean uniqueLocal=bytes.length==16&&(bytes[0]&0xfe)==0xfc;if(uniqueLocal||a.isAnyLocalAddress()||a.isLoopbackAddress()||a.isLinkLocalAddress()||a.isSiteLocalAddress()||a.isMulticastAddress())throw new SecurityException("PRIVATE_NETWORK_BLOCKED");}}
  private void finishNavigation(String error){if(navigation!=null){Callback c=navigation;navigation=null;c.result(null,error);}state.browser(error);}
  void execute(String method,JSONObject payload,Callback callback){main.post(()->{try{
    if(closed||web()==null){callback.result(null,"BROWSER_UNAVAILABLE");return;}
    if(state.snapshot().optString("control").equals("HUMAN")){callback.result(null,"HUMAN_CONTROL_ACTIVE");return;}
    switch(method){
      case "navigate":publicUrl(payload.getString("url"));navigation=callback;web().loadUrl(payload.getString("url"));main.postDelayed(()->{if(navigation==callback)finishNavigation("NAVIGATION_TIMEOUT");},30000);break;
      case "observe":observe(payload.optBoolean("screenshot",true),callback);break;
      case "action":action(payload,callback);break;
      case "media":media(payload,callback);break;
      default:callback.result(null,"UNSUPPORTED_METHOD");
    }
  }catch(Exception e){callback.result(null,"COMMAND_FAILED");}});}
  void observe(boolean screenshot,Callback callback){
    if(web()==null){callback.result(null,"BROWSER_UNAVAILABLE");return;}String stateVersion=version,script=observer.replace("HEY_NAMESPACE",JSONObject.quote(namespace));
    web().evaluateJavascript(script,value->{try{String decoded=new JSONArray("["+value+"]").getString(0);JSONObject observation=new JSONObject(decoded);observation.put("stateVersion",stateVersion).put("tabId",active).put("observedAt",System.currentTimeMillis());JSONArray list=new JSONArray();for(Map.Entry<String,WebView> t:tabs.entrySet())list.put(new JSONObject().put("tabId",t.getKey()).put("url",observedUrl(t.getValue().getUrl())).put("title",t.getValue().getTitle()));observation.put("tabs",list);
      if(!screenshot){callback.result(observation,null);return;}mask(true,()->{screenshotPending=true;long requested=System.currentTimeMillis();web().invalidate();main.postDelayed(()->{try{if(frame==null||frameAt<requested){observation.put("screenshotStatus","UNAVAILABLE");}else{ByteArrayOutputStream out=new ByteArrayOutputStream();frame.compress(Bitmap.CompressFormat.JPEG,65,out);observation.put("image",new JSONObject().put("mimeType","image/jpeg").put("data",Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP))).put("frameAt",frameAt).put("screenshotStatus","MASKED");}}catch(Exception e){try{observation.put("screenshotStatus","ERROR");}catch(Exception ignored){}}mask(false,()->callback.result(observation,null));},250);});
    }catch(Exception e){callback.result(null,"OBSERVATION_FAILED");}});
  }
  private void mask(boolean enabled,Runnable done){if(web()==null){done.run();return;}String key=namespace+"_mask";
    String script=enabled?"(()=>{let old=document.getElementById("+JSONObject.quote(key)+");if(old)old.remove();let root=document.createElement('div');root.id="+JSONObject.quote(key)+";for(let e of document.querySelectorAll('input[type=password],input[autocomplete=one-time-code],input[autocomplete=cc-number],input[autocomplete=cc-csc]')){let r=e.getBoundingClientRect(),m=document.createElement('div');Object.assign(m.style,{position:'fixed',left:r.x+'px',top:r.y+'px',width:r.width+'px',height:r.height+'px',background:'#e5e5e5',zIndex:'2147483647',pointerEvents:'none'});root.appendChild(m);}document.documentElement.appendChild(root)})()":"document.getElementById("+JSONObject.quote(key)+")?.remove()";
    web().evaluateJavascript(script,v->done.run());
  }
  private void action(JSONObject p,Callback callback)throws Exception {
    String a=p.getString("action");
    if(a.equals("click")||a.equals("fill")){
      if(!version.equals(p.getString("stateVersion"))){callback.result(null,"STALE_REFERENCE");return;}
      String base="window["+JSONObject.quote(namespace)+"]?.refs["+JSONObject.quote(p.getString("ref"))+"]";
      if(a.equals("fill")){String script="(()=>{let e="+base+";if(!e?.isConnected)return 'STALE_REFERENCE';if(e.disabled)return 'DISABLED';let proto=e instanceof HTMLTextAreaElement?HTMLTextAreaElement.prototype:HTMLInputElement.prototype, setter=Object.getOwnPropertyDescriptor(proto,'value')?.set;if(!setter)return 'UNSUPPORTED_FILL';e.focus();setter.call(e,"+JSONObject.quote(p.getString("text"))+");e.dispatchEvent(new Event('input',{bubbles:true}));e.dispatchEvent(new Event('change',{bubbles:true}));return 'OK'})()";web().evaluateJavascript(script,v->{if(!"\"OK\"".equals(v)){callback.result(null,"FILL_FAILED");return;}after(callback);});}
      else web().evaluateJavascript("(()=>{let e="+base+";if(!e?.isConnected||e.disabled)return null;let r=e.getBoundingClientRect();return JSON.stringify({x:r.x+r.width/2,y:r.y+r.height/2,scale:"+web().getWidth()+"/innerWidth})})()",v->{try{JSONObject rect=new JSONObject(new JSONArray("["+v+"]").getString(0));float x=(float)(rect.getDouble("x")*rect.getDouble("scale")),y=(float)(rect.getDouble("y")*rect.getDouble("scale"));touch(x,y,x,y);after(callback);}catch(Exception e){callback.result(null,"STALE_REFERENCE");}});
      return;
    }
    switch(a){
      case "back":if(web().canGoBack())web().goBack();break;
      case "forward":if(web().canGoForward())web().goForward();break;
      case "reload":web().reload();break;
      case "scroll":web().scrollBy(p.getInt("x"),p.getInt("y"));break;
      case "drag":touch((float)p.getDouble("x"),(float)p.getDouble("y"),(float)p.getDouble("toX"),(float)p.getDouble("toY"));break;
      case "key":int key=switch(p.getString("value")){case "ENTER"->KeyEvent.KEYCODE_ENTER;case "TAB"->KeyEvent.KEYCODE_TAB;case "ESCAPE"->KeyEvent.KEYCODE_ESCAPE;default->throw new IllegalArgumentException();};web().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,key));web().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,key));break;
      case "tab_open":newTab(p.getString("url"));break;
      case "tab_activate":activate(p.getString("value"));break;
      case "tab_close":String tab=p.getString("value");if(!tabs.containsKey(tab)||tabs.size()==1)throw new IllegalStateException("LAST_TAB");WebView removed=tabs.remove(tab);container.removeView(removed);removed.destroy();if(tab.equals(active))activate(tabs.keySet().iterator().next());break;
      default:throw new IllegalArgumentException();
    }after(callback);
  }
  private void after(Callback callback){invalidate();main.postDelayed(()->observe(true,callback),400);}
  private void media(JSONObject p,Callback c)throws Exception {
    String action=p.getString("action"),cmd=switch(action){case "play"->"e.play().catch(()=>{})";case "pause"->"e.pause()";case "mute"->"e.muted=true";case "unmute"->"e.muted=false";case "seek"->"e.currentTime="+p.getDouble("seconds");default->throw new IllegalArgumentException();};
    // A genuine native tap may be needed on sites requiring a user gesture. The next observation reports the actual outcome.
    web().evaluateJavascript("(()=>{let e=document.querySelector('video,audio');if(!e)return 'NO_MEDIA';"+cmd+";return 'REQUESTED'})()",v->{if(v.contains("NO_MEDIA"))c.result(null,"NO_MEDIA");else main.postDelayed(()->observe(true,c),500);});
  }
  void manualTouch(float x,float y,float toX,float toY){main.post(()->{if(web()!=null&&state.snapshot().optString("control").equals("HUMAN")){try{touch(x,y,toX,toY);invalidate();}catch(IllegalArgumentException ignored){state.browser("INPUT_OUTSIDE_VIEWPORT");}}});}
  private void touch(float x,float y,float toX,float toY){int w=web().getWidth(),h=web().getHeight();if(x<0||y<0||x>=w||y>=h||toX<0||toY<0||toX>=w||toY>=h)throw new IllegalArgumentException("OUTSIDE_VIEWPORT");long time=SystemClock.uptimeMillis();dispatch(time,time,MotionEvent.ACTION_DOWN,x,y);if(x!=toX||y!=toY)for(int i=1;i<=8;i++)dispatch(time,time+i*16,MotionEvent.ACTION_MOVE,x+(toX-x)*i/8,y+(toY-y)*i/8);dispatch(time,time+150,MotionEvent.ACTION_UP,toX,toY);}
  private void dispatch(long down,long at,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,at,action,x,y,0);web().dispatchTouchEvent(e);e.recycle();}
  Bitmap preview(){screenshotPending=true;if(web()!=null)web().invalidate();return frame;}
  @Override public void close(){closed=true;persistTabs();CookieManager.getInstance().flush();if(fileCallback!=null)fileCallback.onReceiveValue(null);for(WebView w:tabs.values())w.destroy();tabs.clear();presentation.dismiss();display.release();reader.close();if(frame!=null)frame.recycle();}
}
