package id.ars.hey;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.projection.*;
import android.os.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

public final class HeyService extends Service implements StateStore.Listener {
  static volatile HeyService current;
  private final ServiceLifecycle lifecycle=new ServiceLifecycle();private boolean foregroundReady;private String startupFailure="",startupFailureDetail="";
  boolean running(){return initialized&&!stopped&&!pausing&&foregroundReady;}
  private HeyApp app;private Transport transport;BrowserRuntime browser;private AudioObserver audio;private ActionJournal journal;
  private final ScheduledThreadPoolExecutor network=new ScheduledThreadPoolExecutor(1);private final ExecutorService maintenance=Executors.newSingleThreadExecutor();private final java.util.concurrent.atomic.AtomicBoolean maintaining=new java.util.concurrent.atomic.AtomicBoolean();private volatile long nextMaintenance;private final Handler main=new Handler(Looper.getMainLooper());
  private final String session=UUID.randomUUID().toString();private volatile JSONObject active,progress=new JSONObject();private volatile long generation;private volatile boolean stopped,finishing,initialized,pausing;
  private int sequence,frameCount,audioCount,sampleCount;private boolean visualGap,audioGap,signal,startedAtZero;private double lastTime=-1;private long watchStarted,lastFrameAt,advancingAt;private String watchId;
  @Override public IBinder onBind(Intent intent){return null;}
  @Override public void onCreate(){super.onCreate();app=(HeyApp)getApplication();transport=new Transport(app.secure);app.state.listen(this);}
  private void initialize()throws Exception{
    if(initialized)return;
    journal=new ActionJournal(this,app.secure);journal.getWritableDatabase();journal.migrate();
    initialized=true;current=this;app.state.task("",new JSONObject());app.state.runtime("RUNNING","");
    // Start the control plane before constructing the graphical runtime on the main thread.
    network.scheduleWithFixedDelay(this::poll,0,2,TimeUnit.SECONDS);
    app.state.browser("STARTING");app.refreshConfig();
  }
  @Override public int onStartCommand(Intent intent,int flags,int startId){
    boolean explicit=intent!=null&&"RESUME".equals(intent.getAction());
    if(intent!=null&&"STOP".equals(intent.getAction())){pause(this);if(!initialized)stopSelf();return START_NOT_STICKY;}
    if(app.secure.get("deviceToken","").isEmpty()){app.state.runtime("STOPPED","NOT_PAIRED");stopSelf();return START_NOT_STICKY;}
    if(app.secure.get("ownerIntent","ACTIVE").equals("PAUSED")&&!explicit){app.state.runtime("PAUSED","OWNER_PAUSED");stopSelf();return START_NOT_STICKY;}
    try{
      if(!foregroundReady){foreground(false);foregroundReady=true;}
    }catch(RuntimeException e){startupFailure="FOREGROUND_START_FAILED";startupFailureDetail=startupFailure+":"+e.getClass().getSimpleName();app.state.runtime("ERROR",startupFailureDetail);app.state.connection("ERROR",startupFailure);stopSelf();return START_NOT_STICKY;}
    try{
      lifecycle.resume();pausing=false;app.secure.put("ownerIntent","ACTIVE");app.state.owner("ACTIVE");app.state.connection("CONNECTING","");initialize();app.state.runtime("RUNNING","");
      // Reuse the same runtime if resume arrives before a previous pause finishes.
      if(browser==null)main.post(()->{if(stopped||pausing||browser!=null)return;try{browser=new BrowserRuntime(this,app.state,app.secure);}catch(Exception e){app.state.browser("RUNTIME_UNAVAILABLE");}});
    }catch(Exception e){startupFailure="SERVICE_INIT_FAILED";startupFailureDetail=startupFailure+":"+e.getClass().getSimpleName();app.state.runtime("ERROR",startupFailureDetail);app.state.connection("ERROR",startupFailure);stopSelf();return START_NOT_STICKY;}
    if(intent!=null&&intent.hasExtra("projectionData")){
      try{if(audio!=null)audio.close();foreground(true);Intent data=(Intent)intent.getParcelableExtra("projectionData");MediaProjection projection=getSystemService(MediaProjectionManager.class).getMediaProjection(intent.getIntExtra("projectionResult",0),data);audio=new AudioObserver(this,projection,app.state);}catch(Exception e){app.state.audio("CONSENT_OR_CAPTURE_FAILED");}
    }
    // System restart may recreate an ACTIVE paired runtime, never a user-paused runtime.
    return START_STICKY;
  }
  static void pause(Context context){HeyApp app=(HeyApp)context.getApplicationContext();try{app.secure.put("ownerIntent","PAUSED");app.secure.put("pendingOwnerIntent","PAUSED");app.state.owner("PAUSED");HeyService service=current;if(service!=null)service.pauseRuntime();else{app.state.runtime("PAUSED","OWNER_PAUSED");app.state.connection("PAUSED","OWNER_PAUSED");}}catch(Exception e){app.state.connection("ERROR","PERSIST_FAILED");}}
  private void pauseRuntime(){if(pausing)return;pausing=true;long pauseTicket=lifecycle.pause();app.state.runtime("PAUSING","OWNER_PAUSED");
    if(active!=null)finish("CANCELLED",false,new JSONObject(),"OWNER_PAUSED");if(browser!=null)browser.abort();
    app.state.connection("PAUSED","OWNER_PAUSED");
    network.execute(()->{try{transport.send("/api/device/intent",new JSONObject().put("ownerIntent","PAUSED"),true);app.secure.remove("pendingOwnerIntent");}catch(Exception ignored){}finally{main.post(()->{if(lifecycle.shouldStop(pauseTicket))stopSelf();});}});
    main.postDelayed(()->{if(lifecycle.shouldStop(pauseTicket))stopSelf();},5000);
  }
  private void foreground(boolean projection){NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("hey.runtime","Hey browser",NotificationManager.IMPORTANCE_LOW));Notification n=notification();if(Build.VERSION.SDK_INT>=34)startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE|(projection?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION:0));else startForeground(1,n,projection?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION:0);}
  private Notification notification(){PendingIntent pause=PendingIntent.getBroadcast(this,1,new Intent(this,PauseReceiver.class),PendingIntent.FLAG_IMMUTABLE);boolean online=app.state.snapshot().optString("connection").equals("ONLINE");return new Notification.Builder(this,"hey.runtime").setSmallIcon(R.drawable.ic_hey_notification).setContentTitle(pausing?"Hey dijeda":online?"Hey terhubung":"Hey memeriksa koneksi").setContentText("Buka Hey untuk melihat browser atau mengambil alih.").setContentIntent(PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).setOngoing(true).addAction(new Notification.Action.Builder(null,"Jeda",pause).build()).build();}
  @Override public void changed(){main.post(()->{if(!stopped&&foregroundReady)getSystemService(NotificationManager.class).notify(1,notification());});}
  static void resumeNotification(Context context){NotificationManager manager=context.getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("hey.wake","Hey tasks",NotificationManager.IMPORTANCE_DEFAULT));manager.notify(2,new Notification.Builder(context,"hey.wake").setSmallIcon(R.drawable.ic_hey_notification).setContentTitle("Hey memerlukan satu tap").setContentText("Buka Hey untuk melanjutkan task.").setContentIntent(PendingIntent.getActivity(context,3,new Intent(context,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).build());}
  private void poll(){if(stopped||pausing)return;long pollTicket=lifecycle.ticket();try{
    String owner=app.secure.get("pendingOwnerIntent","");if(!owner.isEmpty()){transport.send("/api/device/intent",new JSONObject().put("ownerIntent",owner),true);app.secure.remove("pendingOwnerIntent");}
    String pending=app.secure.get("pendingResult","");if(!pending.isEmpty()&&active==null){try{transport.send("/api/device/result",new JSONObject(pending),true);app.secure.remove("pendingResult");}catch(Exception e){if(Set.of("RESULT_CONFLICT","STALE_EXECUTION","TASK_CLOSED").contains(e.getMessage())){app.secure.put("unacknowledgedResult",pending);app.secure.remove("pendingResult");}else throw e;}}
    transport.send("/api/device/intent",new JSONObject().put("ownerIntent","ACTIVE"),true);
    JSONObject cmd=active;JSONObject b=new JSONObject().put("session",session).put("health",app.state.snapshot()).put("activeTaskId",cmd==null?JSONObject.NULL:cmd.optString("taskId")).put("progress",progress).put("progressTaskId",cmd==null?JSONObject.NULL:cmd.optString("taskId")).put("progressGeneration",cmd==null?0:cmd.optLong("generation"));
    JSONObject r=transport.send("/api/device/poll",b,true);generation=r.getLong("generation");if(pausing||stopped||!lifecycle.active(pollTicket))return;app.state.connection("ONLINE","");
    if(!r.isNull("cancelTaskId")&&active!=null&&active.optString("taskId").equals(r.optString("cancelTaskId")))main.post(()->{if(!lifecycle.active(pollTicket)||active!=cmd)return;finish("CANCELLED",false,new JSONObject(),"OWNER_CANCELLED");if(browser!=null)browser.abort();});
    if(!r.isNull("command")&&active==null&&!finishing){JSONObject command=r.getJSONObject("command");main.post(()->{if(lifecycle.active(pollTicket))dispatch(command);});}
    maintain();
  }catch(Exception e){if(!stopped&&!pausing)app.state.connection("OFFLINE",safeReason(e));}}
  private void maintain(){if(stopped||pausing||System.currentTimeMillis()<nextMaintenance||!maintaining.compareAndSet(false,true))return;
    maintenance.execute(()->{try{
      long renewed=Long.parseLong(app.secure.get("renewedAt","0"));
      if(System.currentTimeMillis()-renewed>86400000){JSONObject r=transport.send("/api/device/renew",new JSONObject(),true);app.secure.put("deviceToken",r.getString("deviceToken"));app.secure.put("renewedAt",String.valueOf(System.currentTimeMillis()));}
      String push=app.secure.get("pushToken","");if(!push.isEmpty()&&!push.equals(app.secure.get("registeredPush",""))){transport.send("/api/device/push",new JSONObject().put("pushToken",push),true);app.secure.put("registeredPush",push);if(!stopped)app.state.wake("REGISTERED");}
      app.refreshConfig();if(!stopped)app.state.maintenance("");
    }catch(Exception e){if(!stopped)app.state.maintenance(safeReason(e));}finally{nextMaintenance=System.currentTimeMillis()+300000;maintaining.set(false);}});
  }
  private static String safeReason(Exception e){String s=e.getMessage();return s!=null&&s.matches("[A-Z_]{3,60}")?s:"CONNECTION_UNAVAILABLE";}
  private boolean valid(JSONObject cmd){return !stopped&&!pausing&&!finishing&&active==cmd;}
  private void dispatch(JSONObject cmd){if(active!=null||pausing||stopped)return;active=cmd;progress=new JSONObject();sequence=0;finishing=false;app.state.task(cmd.optString("taskId"),progress);
    try{if(System.currentTimeMillis()>cmd.getLong("deadlineAt")){finish("ERROR",false,new JSONObject(),"TASK_DEADLINE");return;}
      if(!journal.dispatch(cmd.getString("actionId"),new JSONObject().put("digest",cmd.getString("digest")).put("taskId",cmd.getString("taskId")).put("dispatchedAt",System.currentTimeMillis()))){finish("UNKNOWN",false,new JSONObject(),"LOCAL_ACTION_ALREADY_DISPATCHED");return;}
      if(browser==null){finish("ERROR",false,new JSONObject(),"RUNTIME_UNAVAILABLE");return;}
      if(cmd.getString("method").equals("watch")){startWatch(cmd);return;}
      browser.execute(cmd.getString("method"),cmd.getJSONObject("payload"),(observation,error)->{
        if(!valid(cmd))return;if(error!=null){finish("ERROR",false,new JSONObject(),error);return;}
        network.execute(()->{if(!valid(cmd))return;try{sendEvidence(cmd,observation,0);JSONObject result=withoutMedia(observation);boolean verified=verifyCommand(cmd,observation);if(valid(cmd))finish("DONE",verified,result,verified?null:"POSTCONDITION_UNCERTAIN");}catch(Exception e){if(valid(cmd))finish("UNKNOWN",false,new JSONObject(),"RESULT_DELIVERY_FAILED");}});
      });
    }catch(Exception e){finish("ERROR",false,new JSONObject(),"JOURNAL_PERSIST_FAILED");}
  }
  private boolean verifyCommand(JSONObject cmd,JSONObject o){return switch(cmd.optString("method")){case "observe"->true;case "navigate"->BrowserVerifier.navigation(cmd.optJSONObject("payload").optString("url"),o);case "action","media"->o.optBoolean("postconditionVerified",false);default->false;};}
  private static JSONObject withoutMedia(JSONObject source)throws Exception{JSONObject copy=new JSONObject(source.toString());copy.remove("image");copy.remove("audio");return copy;}
  private void sendEvidence(JSONObject cmd,JSONObject o,int seq)throws Exception{
    JSONObject sound=o.optJSONObject("audio");if(sound!=null){JSONObject metadata=new JSONObject(sound.toString());metadata.remove("data");metadata.remove("mimeType");o.put("audioMetadata",metadata);o.put("audio",new JSONObject().put("mimeType",sound.getString("mimeType")).put("data",sound.getString("data")));}
    transport.send("/api/device/evidence",new JSONObject().put("taskId",cmd.getString("taskId")).put("generation",cmd.getLong("generation")).put("digest",cmd.getString("digest")).put("sequence",seq).put("observation",o),true);
  }
  private void startWatch(JSONObject cmd){frameCount=audioCount=sampleCount=0;visualGap=audioGap=signal=startedAtZero=false;lastTime=-1;lastFrameAt=0;watchId=cmd.optString("taskId");
    if(cmd.optJSONObject("payload").optBoolean("audioRequired")&&(audio==null||!audio.active())){finish("ERROR",false,new JSONObject(),"AUDIO_RECONSENT_REQUIRED");return;}
    browser.prepareWatch(()->{if(!valid(cmd))return;if(audio!=null&&audio.active())audio.reset();watchStarted=advancingAt=System.currentTimeMillis();},(zero,error)->{
      if(!valid(cmd))return;if(error!=null){finishWatch(false,error);return;}
      // Persist a zero-time capture before play. This keeps the beginning within the evidence window.
      sample(cmd,zero,true);
    });
  }
  private void sampleWatch(JSONObject cmd){if(!valid(cmd)||!cmd.optString("taskId").equals(watchId))return;if(System.currentTimeMillis()>cmd.optLong("deadlineAt")||System.currentTimeMillis()-watchStarted>cmd.optJSONObject("payload").optInt("maxSeconds")*1000L){finishWatch(false,"WATCH_LIMIT_REACHED");return;}browser.execute("observe",new JSONObject(),(o,e)->{if(!valid(cmd))return;if(e!=null){finishWatch(false,e);return;}sample(cmd,o,false);});}
  private void sample(JSONObject cmd,JSONObject o,boolean initial){if(!valid(cmd))return;
    try{JSONObject m=BrowserVerifier.media(o);if(m==null){finishWatch(false,"MEDIA_NOT_FOUND");return;}double time=m.optDouble("currentTime",0);long now=System.currentTimeMillis();boolean ended=m.optBoolean("ended"),paused=m.optBoolean("paused",true);
      if(initial)startedAtZero=time<=.35;else{double wall=(now-lastFrameAt)/1000.0;if(time<lastTime-.25||time-lastTime>wall*1.1+1||wall>4)visualGap=true;if(time>lastTime+.05)advancingAt=now;if(!ended&&paused){finishWatch(false,"PLAYER_PAUSED");return;}if(!ended&&now-advancingAt>4000){finishWatch(false,"PLAYBACK_NOT_ADVANCING");return;}}
      lastTime=time;lastFrameAt=now;if(!o.has("image")||!o.optBoolean("visualMediaVerified"))visualGap=true;
      JSONObject chunk=null;if(audio!=null&&audio.active())chunk=audio.drain();if(chunk!=null){o.put("audio",chunk);signal|=chunk.optString("signal").equals("PRESENT");audioGap|=chunk.optBoolean("gap");}else if(!initial)audioGap=true;
      if(cmd.optJSONObject("payload").optBoolean("audioRequired")&&(audio==null||!audio.active())){finishWatch(false,"AUDIO_RECONSENT_REQUIRED");return;}
      int seq=sequence;network.execute(()->{if(!valid(cmd))return;try{sendEvidence(cmd,o,seq);main.post(()->{if(!valid(cmd))return;sequence++;sampleCount++;if(o.has("image")&&o.optBoolean("visualMediaVerified"))frameCount++;if(o.has("audio"))audioCount++;
        try{progress=new JSONObject().put("currentTime",time).put("duration",m.opt("duration")).put("samples",sampleCount).put("frames",frameCount).put("audioChunks",audioCount).put("visualGap",visualGap).put("audioGap",audioGap).put("paused",paused);app.state.task(cmd.optString("taskId"),progress);}catch(Exception ignored){}
        if(initial){browser.execute("media",play(),(playing,error)->{if(!valid(cmd))return;if(error!=null)finishWatch(false,error);else sampleWatch(cmd);});}
        else if(ended)finishWatch(true,null);else main.postDelayed(()->sampleWatch(cmd),1000);
      });}catch(Exception e){main.post(()->{if(valid(cmd))finishWatch(false,"OBSERVATION_DELIVERY_GAP");});}});
    }catch(Exception e){finishWatch(false,"MEDIA_OBSERVATION_FAILED");}
  }
  private JSONObject play(){JSONObject p=new JSONObject();try{p.put("action","play");}catch(Exception ignored){}return p;}
  private void finishWatch(boolean ended,String reason){try{boolean required=active!=null&&active.optJSONObject("payload").optBoolean("audioRequired");boolean coverage=startedAtZero&&ended&&!visualGap&&frameCount>=2;boolean sound=coverage&&!audioGap&&signal&&audioCount>=2;JSONObject result=new JSONObject().put("playbackEnded",ended).put("coverageComplete",coverage).put("audioCoverageComplete",sound).put("visualObservationMode","timestamped-samples").put("samples",sampleCount).put("frames",frameCount).put("audioChunks",audioCount).put("lastMediaTime",lastTime).put("understandingVerified",false);finish(ended?"DONE":"ERROR",coverage&&(!required||sound),result,reason!=null?reason:(!coverage||required&&!sound?"COVERAGE_UNCERTAIN":null));}catch(Exception e){finish("ERROR",false,new JSONObject(),"WATCH_FINALIZATION_FAILED");}}
  private synchronized void finish(String status,boolean verified,JSONObject result,String reason){if(active==null||finishing||stopped)return;finishing=true;JSONObject cmd=active;JSONObject response;
    try{response=new JSONObject().put("taskId",cmd.getString("taskId")).put("generation",cmd.getLong("generation")).put("digest",cmd.getString("digest")).put("status",status).put("verified",verified).put("result",result).put("reason",reason==null?JSONObject.NULL:reason);app.secure.put("pendingResult",response.toString());app.secure.put("lastTask",new JSONObject().put("taskId",cmd.getString("taskId")).put("status",status).put("verified",verified).put("reason",reason).put("completedAt",System.currentTimeMillis()).toString());}catch(Exception e){app.state.connection("ERROR","RESULT_PERSIST_FAILED");return;}
    progress=new JSONObject();app.state.task("",new JSONObject());watchId=null;
    network.execute(()->{try{transport.send("/api/device/result",response,true);app.secure.remove("pendingResult");}catch(Exception e){if(!pausing)app.state.connection("OFFLINE","RESULT_UNACKNOWLEDGED");}finally{active=null;finishing=false;}});
  }
  void stopAudio(){if(audio!=null){audio.close();audio=null;}app.state.audio("OFF");foreground(false);}
  void control(boolean human){app.state.control(human?"HUMAN":"AGENT");if(human&&active!=null){finish("ERROR",false,new JSONObject(),"HUMAN_CONTROL_ACTIVE");if(browser!=null)browser.abort();}}
  @Override public void onDestroy(){
    stopped=true;lifecycle.destroy();if(current==this)current=null;app.state.unlisten(this);main.removeCallbacksAndMessages(null);network.shutdownNow();maintenance.shutdownNow();
    try{if(audio!=null)audio.close();}catch(RuntimeException ignored){}
    try{if(browser!=null)browser.close();}catch(RuntimeException ignored){}
    try{if(journal!=null)journal.close();}catch(RuntimeException ignored){}
    progress=new JSONObject();app.state.task("",new JSONObject());
    boolean paused=app.secure.get("ownerIntent","ACTIVE").equals("PAUSED"),paired=!app.secure.get("deviceToken","").isEmpty();
    app.state.runtime(startupFailure.isEmpty()?(paused?"PAUSED":"STOPPED"):"ERROR",startupFailure.isEmpty()?(paused?"OWNER_PAUSED":paired?"SERVICE_STOPPED":"NOT_PAIRED"):startupFailureDetail);
    app.state.connection(startupFailure.isEmpty()?(paused?"PAUSED":paired?"PAIRED":"UNREGISTERED"):"ERROR",startupFailure.isEmpty()?(paused?"OWNER_PAUSED":paired?"SERVICE_STOPPED":"NOT_PAIRED"):startupFailure);
    app.state.browser("IDLE");super.onDestroy();
  }
}
