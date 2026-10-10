package id.ars.hey;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import android.webkit.*;
import org.json.*;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements StateStore.Listener {
  private HeyApp app;private final Handler main=new Handler(Looper.getMainLooper());private final java.util.concurrent.ExecutorService network=Executors.newSingleThreadExecutor();
  private final RuntimeStartPolicy startPolicy=new RuntimeStartPolicy();private boolean visible;private Button connectButton;private TextView runtimeDetail;
  private FrameLayout shell;private WebView homeView;private boolean homeLoaded,lowEffects;private Insets homeInsets=Insets.NONE;private String wallpaperAsset="wallpaper.webp";private LinearLayout root,content,nav;private TextView connection,detail,taskTitle,taskDetail,taskTime,taskCounts;private FrameLayout browserHost;private Button pauseButton;private TextView browserStatus,audioStatus;private Button audioButton;private String page="Home";private float downX,downY;
  private final int ink=Color.rgb(35,42,37),muted=Color.rgb(108,117,108),green=Color.rgb(51,82,61),background=Color.rgb(243,242,239);
  @Override public void onCreate(Bundle state){setTheme(R.style.HeyTheme);super.onCreate(state);app=(HeyApp)getApplication();app.state.listen(this);getWindow().setStatusBarColor(background);getWindow().setNavigationBarColor(background);getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);build();pairIntent(getIntent());}
  @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);pairIntent(intent);}
  private int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
  private GradientDrawable surface(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));d.setStroke(dp(1),Color.argb(18,40,58,45));return d;}
  private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setTypeface(Typeface.create(bold?"sans-serif-medium":"sans-serif",Typeface.NORMAL));t.setLineSpacing(dp(3),1);return t;}
  private void space(LinearLayout target,int height){View v=new View(this);target.addView(v,new LinearLayout.LayoutParams(1,dp(height)));}
  private Button button(String label,Runnable action,boolean primary){Button b=new MotionButton(this);b.setText(label);b.setAllCaps(false);b.setTextSize(15);b.setTextColor(primary?Color.WHITE:ink);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(primary?0x30ffffff:0x1833523d),surface(primary?green:Color.rgb(236,239,233),15),null));b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setOnClickListener(v->{v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);action.run();});LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(54));params.topMargin=dp(12);b.setLayoutParams(params);return b;}
  private static final class MotionButton extends Button {
    MotionButton(Context context){super(context);}
    @Override public void setPressed(boolean pressed){super.setPressed(pressed);if(Settings.Global.getFloat(getContext().getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)>0)animate().scaleX(pressed?.985f:1).scaleY(pressed?.985f:1).setDuration(pressed?85:180).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}
  }
  private LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(22),dp(22),dp(22),dp(22));c.setBackground(surface(Color.rgb(255,254,251),24));c.setElevation(dp(3));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(1),dp(4),dp(1),dp(18));c.setLayoutParams(p);return c;}
  private void build(){
    root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(background);root.setPadding(dp(20),dp(18),dp(20),dp(12));shell=new FrameLayout(this);shell.addView(root,new FrameLayout.LayoutParams(-1,-1));
    root.setOnApplyWindowInsetsListener((v,insets)->{Insets i=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(dp(20)+i.left,dp(12)+i.top,dp(20)+i.right,dp(8)+i.bottom);return insets;});
    LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);ImageView wordmark=new ImageView(this);wordmark.setImageResource(R.drawable.ic_hey);wordmark.setContentDescription("Hey.");header.addView(wordmark,new LinearLayout.LayoutParams(dp(44),dp(44)));TextView by=text("  by Ars",13,muted,false);header.addView(by);root.addView(header);space(root,16);
    ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
    nav=new LinearLayout(this);nav.setPadding(dp(4),dp(4),dp(4),dp(4));nav.setBackground(surface(Color.rgb(230,233,226),20));root.addView(nav,new LinearLayout.LayoutParams(-1,dp(56)));createHomeView();setContentView(shell);render();
  }
  private void render(){HeyService service=HeyService.current;if(service!=null&&service.browser!=null)service.browser.detach(this);content.removeAllViews();connection=detail=taskTitle=taskDetail=taskTime=taskCounts=null;browserHost=null;browserStatus=null;pauseButton=null;audioStatus=null;audioButton=null;connectButton=null;runtimeDetail=null;nav.removeAllViews();for(String name:new String[]{"Home","Browser","Tasks","Settings"}){TextView tab=text(name,12,name.equals(page)?green:muted,name.equals(page));tab.setGravity(Gravity.CENTER);if(name.equals(page))tab.setBackground(surface(Color.WHITE,16));tab.setOnClickListener(v->{page=name;render();});nav.addView(tab,new LinearLayout.LayoutParams(0,-1,1));}
    switch(page){case "Browser":browser();break;case "Tasks":tasks();break;case "Settings":settings();break;default:break;}
    boolean home="Home".equals(page);root.setVisibility(home?View.GONE:View.VISIBLE);homeView.setVisibility(home?View.VISIBLE:View.GONE);
    getWindow().setStatusBarColor(home?Color.TRANSPARENT:background);getWindow().setNavigationBarColor(home?Color.TRANSPARENT:background);
    if(Build.VERSION.SDK_INT>=29)getWindow().setNavigationBarContrastEnforced(false);
    getWindow().getDecorView().setSystemUiVisibility(home?(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE):(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
    if(home)homeView.onResume();else homeView.onPause();
    if(homeLoaded)homeView.evaluateJavascript("window.heySetActive&&window.heySetActive("+(home&&visible)+");",null);
    if(Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)>0){content.setAlpha(.65f);content.setTranslationY(dp(5));content.animate().alpha(1).translationY(0).setDuration(220).start();}update();
  }

  // App-owned, offline-only UI WebView. Never expose JS interfaces to browsing pages.
  private void createHomeView(){
    homeView=new WebView(this);
    ActivityManager manager=getSystemService(ActivityManager.class);
    android.util.DisplayMetrics metrics=getResources().getDisplayMetrics();
    wallpaperAsset=WallpaperPolicy.asset(metrics.widthPixels,metrics.heightPixels,manager.getMemoryClass(),manager.isLowRamDevice());
    lowEffects=manager.isLowRamDevice()||manager.getMemoryClass()<192;
    homeView.setBackgroundColor(Color.rgb(38,65,54));
    homeView.setVerticalScrollBarEnabled(false);
    homeView.setOnApplyWindowInsetsListener((v,insets)->{
      homeInsets=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());configureHomeSurface();
      return new WindowInsets.Builder(insets).setInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout(),Insets.NONE).build();
    });
    WebSettings config=homeView.getSettings();
    config.setJavaScriptEnabled(true);config.setDomStorageEnabled(false);
    config.setAllowFileAccess(false);config.setAllowContentAccess(false);config.setBlockNetworkLoads(true);
    homeView.setWebViewClient(new WebViewClient(){
      @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
        // android_asset URLs bypass this callback; use an app-owned HTTPS origin instead.
        String asset=HomeAssetPolicy.asset(request.getUrl().toString(),wallpaperAsset);
        if(asset!=null)try{return new WebResourceResponse(asset.endsWith(".webp")?"image/webp":"text/html",asset.endsWith(".webp")?null:"UTF-8",getAssets().open(asset));}catch(java.io.IOException ignored){}
        return new WebResourceResponse("text/plain","UTF-8",403,"Blocked",java.util.Map.of(),new java.io.ByteArrayInputStream(new byte[0]));
      }
      @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){
        Uri uri=request.getUrl();
        if("hey-action".equals(uri.getScheme())){
          String command=uri.getHost();main.post(()->handleHomeAction(command));
        }
        return true;
      }
      @Override public void onPageFinished(WebView v,String url){
        if(HomeAssetPolicy.HOME_URL.equals(url)){
          homeLoaded=true;configureHomeSurface();updateHomeSurface(app.state.snapshot());
        }
      }
    });
    shell.addView(homeView,new FrameLayout.LayoutParams(-1,-1));
    homeView.loadUrl(HomeAssetPolicy.HOME_URL);
  }
  private void configureHomeSurface(){
    if(!homeLoaded||homeView==null)return;
    float density=getResources().getDisplayMetrics().density;
    try{JSONObject config=new JSONObject().put("safeTop",homeInsets.top/density).put("safeBottom",homeInsets.bottom/density)
      .put("safeLeft",homeInsets.left/density).put("safeRight",homeInsets.right/density).put("lowEffects",lowEffects)
      .put("reduceMotion",Settings.Global.getFloat(getContentResolver(),Settings.Global.ANIMATOR_DURATION_SCALE,1)==0);
      homeView.evaluateJavascript("window.heyConfigure&&window.heyConfigure("+config+");window.heySetActive&&window.heySetActive("+(visible&&"Home".equals(page))+");",null);
    }catch(JSONException ignored){}
  }
  private void handleHomeAction(String action){
    if(action==null)return;
    if(action.equals("status")){
      JSONObject s=app.state.snapshot();
      String reason=s.optString("runtimeReason",s.optString("reason",""));
      boolean paired=!app.secure.get("deviceToken","").isEmpty();
      new AlertDialog.Builder(this).setTitle("Koneksi Hey")
        .setMessage("Koneksi: "+s.optString("connection","UNKNOWN")+"\nService: "+s.optString("runtime","UNKNOWN")
          +(reason.isEmpty()?"":"\nDetail: "+reason)+"\n"+(paired?"Pairing tersimpan.":"Pairing belum tersimpan."))
        .setPositiveButton("Tutup",null)
        .setNeutralButton(paired?"Jalankan Hey":"Pengaturan",(dialog,which)->{
          if(paired)connect();else{page="Settings";render();}
        }).show();
      return;
    }
    if(action.equals("browser"))page="Browser";
    else if(action.equals("tasks"))page="Tasks";
    else if(action.equals("settings"))page="Settings";
    else return;
    render();
  }
  private void updateHomeSurface(JSONObject state){
    if(!homeLoaded||homeView==null)return;
    String status=state.optString("connection","UNKNOWN");
    String label=switch(status){case "ONLINE"->"Terhubung";case "CONNECTING","REGISTERING"->"Menghubungkan";
      case "PAUSED"->"Dijeda";case "OFFLINE"->"Offline";default->"Belum terhubung";};
    if("PAUSED".equals(app.secure.get("ownerIntent","ACTIVE"))){status="PAUSED";label="Dijeda";}
    String activity="Belum ada aktivitas";
    try{JSONObject last=new JSONObject(app.secure.get("lastTask","{}"));
      String value=last.optString("status","");
      if(!value.isEmpty())activity=value.equals("DONE")?"Tugas terakhir selesai":
        value.equals("FAILED")?"Tugas terakhir gagal":"Tugas terakhir: "+value;
    }catch(Exception ignored){}
    homeView.evaluateJavascript("window.heySetState&&window.heySetState("+
      JSONObject.quote(status)+","+JSONObject.quote(label)+","+JSONObject.quote(activity)+");",null);
  }
  @Override public void onBackPressed(){
    if(!"Home".equals(page)){page="Home";render();}else super.onBackPressed();
  }

  private void home(){space(content,22);content.addView(text("A little hello.",35,ink,true));content.addView(text("Ruang untuk browsermu.",18,muted,false));space(content,30);LinearLayout c=card();connection=text("Memeriksa…",18,green,true);c.addView(connection);detail=text("",14,muted,false);space(c,8);c.addView(detail);
    connectButton=button("Jalankan Hey",this::connect,true);c.addView(connectButton);runtimeDetail=text("",12,muted,false);space(c,8);c.addView(runtimeDetail);content.addView(c);
    LinearLayout control=card();control.addView(text("Tetap di tanganmu.",18,ink,true));space(control,8);control.addView(text("Lihat browser yang sedang bekerja, ambil alih kapan pun, atau jeda seluruh aktivitas.",14,muted,false));control.addView(button("Lihat browser",()->{page="Browser";render();},false));content.addView(control);
  }
  private void browser(){content.addView(text("Your browser.",29,ink,true));content.addView(text("Surface yang sama untuk kamu dan Hey.",14,muted,false));space(content,14);LinearLayout c=card();browserHost=new FrameLayout(this);browserHost.setBackground(surface(Color.rgb(237,240,233),16));browserHost.setContentDescription("Browser Hey");c.addView(browserHost,new LinearLayout.LayoutParams(-1,dp(420)));browserStatus=text("Menyiapkan browser…",13,muted,false);c.addView(browserStatus);connectButton=button("Jalankan Hey",this::connect,true);c.addView(connectButton);runtimeDetail=text("",12,muted,false);c.addView(runtimeDetail);attachBrowser();
    c.addView(button(app.state.snapshot().optString("control").equals("HUMAN")?"Kembalikan ke Hey":"Ambil alih",()->{HeyService s=HeyService.current;if(s!=null){s.control(!app.state.snapshot().optString("control").equals("HUMAN"));render();}},true));
    c.addView(button("Pilih file untuk halaman ini",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,40);},false));content.addView(c);
    LinearLayout navigate=card();EditText url=new EditText(this);url.setSingleLine(true);url.setHint("https://…");url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);navigate.addView(url);navigate.addView(button("Buka halaman",()->{HeyService s=HeyService.current;if(s==null||s.browser==null){startBrowser();return;}if(!app.state.snapshot().optString("control").equals("HUMAN")){Toast.makeText(this,"Ambil alih browser terlebih dahulu",Toast.LENGTH_SHORT).show();return;}try{String address=url.getText().toString();BrowserRuntime.publicUrl(address);s.browser.humanNavigate(address,(o,error)->{if(error!=null)Toast.makeText(this,"Halaman belum dapat dibuka",Toast.LENGTH_SHORT).show();});}catch(Exception e){Toast.makeText(this,"Gunakan alamat HTTPS publik",Toast.LENGTH_SHORT).show();}},false));content.addView(navigate);
  }
  private void tasks(){content.addView(text("A clear trail.",29,ink,true));space(content,18);LinearLayout c=card();taskTitle=text("",19,green,true);taskDetail=text("",14,muted,false);taskTime=text("",30,ink,true);taskCounts=text("",13,muted,false);c.addView(taskTitle);c.addView(taskDetail);space(c,12);c.addView(taskTime);c.addView(taskCounts);content.addView(c);updateTasks();}
  private void updateTasks(){if(taskTitle==null)return;JSONObject state=app.state.snapshot();if(!state.optString("taskId").isEmpty()){taskTitle.setText("Sedang bekerja");taskDetail.setText("Progress diamati langsung dari browser.");JSONObject p=state.optJSONObject("progress");taskTime.setText(p!=null&&p.has("currentTime")?String.format(java.util.Locale.US,"%.1f detik",p.optDouble("currentTime",0)):"");taskCounts.setText(p!=null&&p.has("frames")?p.optInt("frames")+" frame · "+p.optInt("audioChunks")+" segmen audio":"");}else{try{JSONObject last=new JSONObject(app.secure.get("lastTask","{}"));taskTitle.setText(last.has("status")?last.optString("status"):"Belum ada aktivitas");taskDetail.setText(last.optBoolean("verified")?"Hasil task terverifikasi.":"Hasil terverifikasi akan muncul setelah ada bukti yang sesuai.");taskTime.setText("");taskCounts.setText("");}catch(Exception ignored){}}}
  private void settings(){content.addView(text("Make it yours.",29,ink,true));space(content,18);LinearLayout connectionCard=card();connectionCard.addView(text("Koneksi Hey",18,ink,true));connectButton=button("Jalankan Hey",this::connect,true);connectionCard.addView(connectButton);runtimeDetail=text("",12,muted,false);connectionCard.addView(runtimeDetail);content.addView(connectionCard);LinearLayout c=card();c.addView(text("Suara browser",18,ink,true));c.addView(text("Aktifkan sesi audio untuk mengamati suara dari Hey. Android akan meminta izin sesi; aplikasi lain tidak menjadi sumber audio.",14,muted,false));audioStatus=text("",13,muted,false);c.addView(audioStatus);audioButton=button("Aktifkan audio",()->{HeyService service=HeyService.current;if(app.state.snapshot().optString("audio").equals("CAPTURING")&&service!=null)service.stopAudio();else requestAudio();},true);c.addView(audioButton);content.addView(c);
    LinearLayout device=card();device.addView(text("Kendali perangkat",18,ink,true));device.addView(button("Pengaturan Android",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName()))),false));pauseButton=button(app.secure.get("ownerIntent","ACTIVE").equals("PAUSED")?"Lanjutkan Hey":"Jeda Hey",()->{if(app.secure.get("ownerIntent","ACTIVE").equals("PAUSED"))startBrowser();else HeyService.pause(this);update();},false);device.addView(pauseButton);content.addView(device);content.addView(text("Hey by Ars · "+BuildConfig.VERSION_NAME+"\nBrowser, audio, koneksi, dan wake diverifikasi secara terpisah.",12,muted,false));
  }
  private void connect(){if(app.secure.get("deviceToken","").isEmpty()){new AlertDialog.Builder(this).setTitle("Hubungkan Hey").setMessage("Pairing belum tersimpan di aplikasi ini. Di ChatGPT, panggil Hey dan minta tautan pairing, lalu buka tautan pada ponsel ini.").setPositiveButton("Mengerti",null).show();return;}startBrowser();}
  private void startBrowser(){startRuntime(true);}
  private void startRuntime(boolean explicit){
    HeyService service=HeyService.current;boolean paired=!app.secure.get("deviceToken","").isEmpty();boolean paused=app.secure.get("ownerIntent","ACTIVE").equals("PAUSED");
    if(!visible||!startPolicy.begin(SystemClock.elapsedRealtime(),explicit,paired,paused,service!=null&&service.running()))return;
    app.state.runtime("STARTING","");app.state.connection("CONNECTING","");
    try{Intent i=new Intent(this,HeyService.class);if(explicit)i.setAction("RESUME");startForegroundService(i);}
    catch(RuntimeException e){app.state.runtime("ERROR","SERVICE_START_FAILED:"+e.getClass().getSimpleName());app.state.connection("ERROR","SERVICE_START_FAILED");}
    // Notification permission is optional and must not gate foreground service startup.
    if(explicit&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED&&app.secure.get("notificationAsked","").isEmpty()){
      try{app.secure.put("notificationAsked","true");requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},20);}catch(Exception ignored){}
    }
  }
  private void requestAudio(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},30);return;}startBrowser();startActivityForResult(getSystemService(MediaProjectionManager.class).createScreenCaptureIntent(),31);}
  @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results){super.onRequestPermissionsResult(request,permissions,results);if(request==30&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)requestAudio();}
  @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==31&&result==RESULT_OK&&data!=null)startForegroundService(new Intent(this,HeyService.class).putExtra("projectionData",data).putExtra("projectionResult",result));if(request==40&&HeyService.current!=null&&HeyService.current.browser!=null)HeyService.current.browser.chooseFile(result==RESULT_OK&&data!=null?data.getData():null);}
  private void pairIntent(Intent intent){Uri uri=intent.getData();if(uri==null||!"hey".equals(uri.getScheme())||!"pair".equals(uri.getHost()))return;intent.setData(null);String gateway=uri.getQueryParameter("gateway"),code=uri.getQueryParameter("code");if(!app.secure.get("deviceToken","").isEmpty()){Toast.makeText(this,"Ponsel ini sudah dipasangkan",Toast.LENGTH_SHORT).show();return;}
    app.state.connection("REGISTERING","");network.execute(()->{try{Transport.gateway(gateway);app.secure.put("gateway",gateway);Transport t=new Transport(app.secure);JSONObject result=t.send("/api/enroll",new JSONObject().put("code",code),false);app.secure.put("deviceId",result.getString("deviceId"));app.secure.put("deviceToken",result.getString("deviceToken"));app.state.connection("PAIRED","");app.refreshConfig();main.post(()->{startBrowser();render();});}catch(Exception e){app.state.connection("UNREGISTERED","PAIRING_FAILED");main.post(()->Toast.makeText(this,"Pairing belum berhasil. Minta tautan baru dari Hey.",Toast.LENGTH_LONG).show());}});
  }
  @Override public void changed(){main.post(this::update);}
  private void update(){JSONObject s=app.state.snapshot();updateRuntime(s);if(audioStatus!=null){String audio=s.optString("audio");audioStatus.setText(audio.equals("CAPTURING")?"Sesi audio aktif.":audio.equals("CONSENT_ENDED")?"Izin sesi berakhir. Aktifkan ulang untuk mengamati suara.":audio.equals("OFF")?"Audio belum diaktifkan.":"Audio belum tersedia: "+audio);audioButton.setText(audio.equals("CAPTURING")?"Hentikan audio":"Aktifkan audio");}if(pauseButton!=null)pauseButton.setText(app.secure.get("ownerIntent","ACTIVE").equals("PAUSED")?"Lanjutkan Hey":"Jeda Hey");attachBrowser();if(connection!=null){String status=s.optString("connection");connection.setText(switch(status){case "PAUSED"->"Hey dijeda";case "ONLINE"->"Terhubung";case "PAIRED"->"Ponsel sudah dikenali";case "REGISTERING","CONNECTING"->"Menyambungkan…";case "ERROR"->"Hey belum dapat berjalan";case "OFFLINE"->"Menghubungkan kembali…";case "UNKNOWN"->"Memeriksa koneksi…";default->"Satu koneksi, sekali saja";});detail.setText(app.secure.get("deviceToken","").isEmpty()?"Hubungkan ponsel melalui tautan Hey di ChatGPT.":app.secure.get("ownerIntent","ACTIVE").equals("PAUSED")?"Aktivitas dijeda. Tekan Lanjutkan Hey saat kamu siap.":status.equals("ONLINE")?(s.optString("browser").equals("READY")?"Browser siap. Kamu tetap bebas menggunakan ponsel.":"Koneksi tersambung. Status browser: "+s.optString("browser")):status.equals("CONNECTING")?"Menjalankan Hey dan menunggu heartbeat pertama…":s.optString("runtime").equals("ERROR")?"Startup belum berhasil. Tekan Jalankan Hey untuk mencoba lagi.":"Pairing tersimpan. Tekan Jalankan Hey untuk memulihkan koneksi.");}updateTasks();updateHomeSurface(s);}
  private void updateRuntime(JSONObject s){
    boolean paired=!app.secure.get("deviceToken","").isEmpty(),paused=app.secure.get("ownerIntent","ACTIVE").equals("PAUSED");
    if(connectButton!=null)connectButton.setText(!paired?"Hubungkan dari ChatGPT":paused?"Lanjutkan Hey":s.optString("connection").equals("ONLINE")?"Periksa koneksi":"Jalankan Hey");
    if(runtimeDetail!=null){String reason=s.optString("runtimeReason");if(reason.isEmpty()&&!s.optString("connection").equals("ONLINE"))reason=s.optString("reason");String status=!paired?"Pairing belum tersimpan di aplikasi ini.":paused?"Hey dijeda olehmu.":s.optString("runtime").equals("RUNNING")?"Service aktif · koneksi "+s.optString("connection"):s.optString("runtime").equals("STARTING")?"Service sedang dimulai…":"Service belum aktif"+(reason.isEmpty()?".":" · "+reason);runtimeDetail.setText("Hey "+BuildConfig.VERSION_NAME+" · "+status+(s.optString("runtime").equals("RUNNING")&&!reason.isEmpty()?" · "+reason:""));}
  }
  private void attachBrowser(){if(browserHost==null)return;HeyService s=HeyService.current;if(s!=null&&s.browser!=null&&browserHost.getChildCount()==0)s.browser.attach(this,browserHost);if(browserStatus!=null)browserStatus.setText(s==null||!s.running()?"Browser belum aktif. Tekan Jalankan Hey.":s.browser==null?"Browser sedang disiapkan: "+app.state.snapshot().optString("browser"):app.state.snapshot().optString("control").equals("HUMAN")?"Kamu memegang kendali.":"Hey memegang kendali. Ambil alih untuk berinteraksi.");}
  private final Runnable surfaceTick=new Runnable(){@Override public void run(){if(visible)startRuntime(false);attachBrowser();main.postDelayed(this,1000);}};
  @Override protected void onResume(){super.onResume();visible=true;if(homeView!=null&&"Home".equals(page))homeView.onResume();configureHomeSurface();startRuntime(false);app.refreshConfig();main.removeCallbacks(surfaceTick);main.post(surfaceTick);update();}
  @Override protected void onPause(){visible=false;if(homeView!=null){if(homeLoaded)homeView.evaluateJavascript("window.heySetActive&&window.heySetActive(false);",null);homeView.onPause();}main.removeCallbacks(surfaceTick);HeyService s=HeyService.current;if(s!=null&&s.browser!=null)s.browser.detach(this);super.onPause();}
  @Override protected void onDestroy(){if(homeView!=null){shell.removeView(homeView);homeView.destroy();}app.state.unlisten(this);main.removeCallbacksAndMessages(null);network.shutdownNow();super.onDestroy();}
}
