package id.ars.hey;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public final class HeyService extends Service implements StateStore.Listener {
  static HeyService current;
  private HeyApp app;private Transport transport;BrowserRuntime browser;private AudioObserver audio;
  private final ScheduledThreadPoolExecutor network=new ScheduledThreadPoolExecutor(1);private final Handler main=new Handler(Looper.getMainLooper());
  private final String session=UUID.randomUUID().toString();private volatile JSONObject active,progress=new JSONObject();private volatile long generation;private volatile boolean stopped,finishing;
  private int sequence,frameCount,audioCount;private boolean visualGap,audioGap,signal,startedAtZero;private double lastTime=-1;private long watchStarted,lastFrameAt;private String watchId;
  private final IBinder binder=new LocalBinder();
  final class LocalBinder extends Binder {HeyService service(){return HeyService.this;}}
  @Override public IBinder onBind(Intent intent){return binder;}
  @Override public void onCreate(){super.onCreate();current=this;app=(HeyApp)getApplication();transport=new Transport(app.secure);foreground(false);app.state.listen(this);try{browser=new BrowserRuntime(this,app.state,app.secure);}catch(Exception e){app.state.browser("RUNTIME_UNAVAILABLE");}
    network.scheduleWithFixedDelay(this::poll,0,2,TimeUnit.SECONDS);
  }
  @Override public int onStartCommand(Intent intent,int flags,int startId){
    if(intent!=null&&"STOP".equals(intent.getAction())){try{app.secure.put("ownerIntent","PAUSED");}catch(Exception ignored){}stopSelf();return START_NOT_STICKY;}
    try{app.secure.put("ownerIntent","ACTIVE");}catch(Exception e){app.state.connection("ERROR","PERSIST_FAILED");}
    if(intent!=null&&intent.hasExtra("projectionData")){
      try{foreground(true);Intent data=(Intent)intent.getParcelableExtra("projectionData");MediaProjection projection=getSystemService(MediaProjectionManager.class).getMediaProjection(intent.getIntExtra("projectionResult",0),data);if(audio!=null)audio.close();audio=new AudioObserver(this,projection,app.state);}catch(Exception e){app.state.audio("CONSENT_OR_CAPTURE_FAILED");}
    }return START_NOT_STICKY;
  }
  private void foreground(boolean projection){
    NotificationManager manager=getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("hey.runtime","Hey browser",NotificationManager.IMPORTANCE_LOW));
    Notification n=notification();
    if(Build.VERSION.SDK_INT>=34)startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE|(projection?ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION:0));else startForeground(1,n);
  }
  private Notification notification(){
    Intent stop=new Intent(this,HeyService.class).setAction("STOP");PendingIntent pi=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE);
    boolean online=app.state.snapshot().optString("connection").equals("ONLINE");
    return new Notification.Builder(this,"hey.runtime").setSmallIcon(id.ars.hey.R.drawable.ic_hey).setContentTitle(online?"Hey terhubung":"Hey memeriksa koneksi").setContentText("Buka Hey untuk melihat browser atau mengambil alih.").setContentIntent(PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).setOngoing(true).addAction(new Notification.Action.Builder(null,"Jeda",pi).build()).build();
  }
  @Override public void changed(){main.post(()->{if(!stopped)getSystemService(NotificationManager.class).notify(1,notification());});}
  static void resumeNotification(Context context){NotificationManager manager=context.getSystemService(NotificationManager.class);manager.createNotificationChannel(new NotificationChannel("hey.wake","Hey tasks",NotificationManager.IMPORTANCE_DEFAULT));manager.notify(2,new Notification.Builder(context,"hey.wake").setSmallIcon(id.ars.hey.R.drawable.ic_hey).setContentTitle("Hey memerlukan satu tap").setContentText("Android meminta Hey dibuka untuk melanjutkan task.").setContentIntent(PendingIntent.getActivity(context,3,new Intent(context,MainActivity.class),PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).build());}
  private void poll(){if(stopped)return;try{
    String pending=app.secure.get("pendingResult","");if(!pending.isEmpty()&&active==null){try{transport.send("/api/device/result",new JSONObject(pending),true);app.secure.remove("pendingResult");}catch(Exception e){if("RESULT_CONFLICT".equals(e.getMessage())||"STALE_EXECUTION".equals(e.getMessage()))app.secure.remove("pendingResult");else throw e;}}
    long renewed=Long.parseLong(app.secure.get("renewedAt","0"));if(System.currentTimeMillis()-renewed>86400000){JSONObject renewedToken=transport.send("/api/device/renew",new JSONObject(),true);app.secure.put("deviceToken",renewedToken.getString("deviceToken"));app.secure.put("renewedAt",String.valueOf(System.currentTimeMillis()));}
    JSONObject b=new JSONObject().put("session",session).put("health",app.state.snapshot()).put("activeTaskId",active==null?JSONObject.NULL:active.optString("taskId")).put("progress",progress);
    JSONObject r=transport.send("/api/device/poll",b,true);generation=r.getLong("generation");app.state.connection("ONLINE","");
    String push=app.secure.get("pushToken","");if(!push.isEmpty()&&!push.equals(app.secure.get("registeredPush",""))){transport.send("/api/device/push",new JSONObject().put("pushToken",push),true);app.secure.put("registeredPush",push);}
    if(!r.isNull("cancelTaskId")&&active!=null&&active.optString("taskId").equals(r.optString("cancelTaskId")))main.post(()->finish("CANCELLED",false,new JSONObject(),"OWNER_CANCELLED"));
    if(!r.isNull("command")&&active==null&&!finishing){JSONObject cmd=r.getJSONObject("command");dispatch(cmd);}
  }catch(Exception e){app.state.connection("OFFLINE",safeReason(e));}}
  private static String safeReason(Exception e){String s=e.getMessage();return s!=null&&s.matches("[A-Z_]{3,60}")?s:"CONNECTION_UNAVAILABLE";}
  private void dispatch(JSONObject cmd)throws Exception {
    if(System.currentTimeMillis()>cmd.getLong("deadlineAt"))return;
    String actionId=cmd.getString("actionId"),key="receipt:"+actionId;JSONObject receipts=new JSONObject(app.secure.get("receipts","{}"));
    if(receipts.has(key)){active=cmd;finish("UNKNOWN",false,new JSONObject(),"LOCAL_ACTION_ALREADY_DISPATCHED");return;}
    if(receipts.length()>=10000){active=cmd;finish("ERROR",false,new JSONObject(),"LOCAL_JOURNAL_FULL");return;}
    // Persist before a side effect. It deliberately survives reconnect and process death.
    receipts.put(key,new JSONObject().put("digest",cmd.getString("digest")).put("taskId",cmd.getString("taskId")).put("dispatchedAt",System.currentTimeMillis()));app.secure.put("receipts",receipts.toString());
    active=cmd;sequence=0;finishing=false;app.state.task(cmd.getString("taskId"),new JSONObject().put("status","RUNNING"));
    if(browser==null){finish("ERROR",false,new JSONObject(),"RUNTIME_UNAVAILABLE");return;}
    if(cmd.getString("method").equals("watch")){main.post(()->startWatch(cmd));return;}
    browser.execute(cmd.getString("method"),cmd.getJSONObject("payload"),(observation,error)->{
      if(error!=null){finish("ERROR",false,new JSONObject(),error);return;}
      network.execute(()->{try{sendEvidence(cmd,observation,sequence++);JSONObject result=withoutMedia(observation);boolean verified=verifyCommand(cmd,observation);finish("DONE",verified,result,verified?null:"POSTCONDITION_UNCERTAIN");}catch(Exception e){finish("UNKNOWN",false,new JSONObject(),"RESULT_DELIVERY_FAILED");}});
    });
  }
  private boolean verifyCommand(JSONObject cmd,JSONObject observed){String method=cmd.optString("method");if(method.equals("observe"))return true;if(method.equals("navigate"))return observed.optString("url").equals(cmd.optJSONObject("payload").optString("url"))&&observed.optString("documentReady").equals("complete");if(method.equals("media")){JSONObject m=observed.optJSONArray("media")==null?null:observed.optJSONArray("media").optJSONObject(0);if(m==null)return false;String a=cmd.optJSONObject("payload").optString("action");return switch(a){case "play"->!m.optBoolean("paused",true);case "pause"->m.optBoolean("paused",false);case "mute"->m.optBoolean("muted",false);case "unmute"->!m.optBoolean("muted",true);case "seek"->Math.abs(m.optDouble("currentTime")-cmd.optJSONObject("payload").optDouble("seconds"))<2;default->false;};}return false;}
  private static JSONObject withoutMedia(JSONObject source)throws Exception {JSONObject copy=new JSONObject(source.toString());copy.remove("image");copy.remove("audio");return copy;}
  private void sendEvidence(JSONObject cmd,JSONObject observation,int seq)throws Exception {
    // Audio transport includes only standard MCP fields; timestamps and quality are separate metadata.
    JSONObject sound=observation.optJSONObject("audio");if(sound!=null){JSONObject metadata=new JSONObject(sound.toString());metadata.remove("data");metadata.remove("mimeType");observation.put("audioMetadata",metadata);observation.put("audio",new JSONObject().put("mimeType",sound.getString("mimeType")).put("data",sound.getString("data")));}
    transport.send("/api/device/evidence",new JSONObject().put("taskId",cmd.getString("taskId")).put("generation",cmd.getLong("generation")).put("digest",cmd.getString("digest")).put("sequence",seq).put("observation",observation),true);
  }
  private void startWatch(JSONObject cmd){frameCount=audioCount=0;visualGap=audioGap=signal=false;lastTime=-1;watchStarted=System.currentTimeMillis();lastFrameAt=0;watchId=cmd.optString("taskId");
    if(cmd.optJSONObject("payload").optBoolean("audioRequired")&&(audio==null||!audio.active())){finish("ERROR",false,new JSONObject(),"AUDIO_SESSION_REQUIRED");return;}if(audio!=null)audio.reset();sampleWatch(cmd);
  }
  private void sampleWatch(JSONObject cmd){if(stopped||active==null||finishing||!cmd.optString("taskId").equals(watchId))return;
    if(System.currentTimeMillis()>cmd.optLong("deadlineAt")||System.currentTimeMillis()-watchStarted>cmd.optJSONObject("payload").optInt("maxSeconds")*1000L){finishWatch(false,"WATCH_LIMIT_REACHED");return;}
    browser.execute("observe",new JSONObject(),(observation,error)->{if(error!=null){finishWatch(false,error);return;}
      try{JSONArray media=observation.optJSONArray("media");JSONObject m=media==null?null:media.optJSONObject(0);if(m==null){finishWatch(false,"MEDIA_NOT_FOUND");return;}
        double time=m.optDouble("currentTime",0);long now=System.currentTimeMillis();if(frameCount==0){startedAtZero=time<=2;if(!startedAtZero){finishWatch(false,"PLAYBACK_MUST_START_AT_ZERO");return;}}else{double wall=(now-lastFrameAt)/1000.0;if(time<lastTime-.25||time-lastTime>wall*1.1+1||wall>4)visualGap=true;}
        lastTime=time;lastFrameAt=now;frameCount++;if(!observation.has("image"))visualGap=true;
        if(audio!=null&&audio.active()){JSONObject chunk=audio.drain();if(chunk!=null){observation.put("audio",chunk);audioCount++;signal|=chunk.optString("signal").equals("PRESENT");audioGap|=chunk.optBoolean("gap");}else audioGap=true;}else audioGap=true;
        boolean ended=m.optBoolean("ended"),paused=m.optBoolean("paused");
        progress=new JSONObject().put("currentTime",time).put("duration",m.opt("duration")).put("frames",frameCount).put("audioChunks",audioCount).put("visualGap",visualGap).put("audioGap",audioGap).put("paused",paused);app.state.task(cmd.optString("taskId"),progress);
        int seq=sequence++;if(network.getQueue().size()>5){finishWatch(false,"OBSERVATION_UPLOAD_BACKLOG");return;}
        network.execute(()->{try{sendEvidence(cmd,observation,seq);main.post(()->{if(ended)finishWatch(true,null);else main.postDelayed(()->sampleWatch(cmd),1000);});}catch(Exception e){main.post(()->finishWatch(false,"OBSERVATION_DELIVERY_GAP"));}});
      }catch(Exception e){finishWatch(false,"MEDIA_OBSERVATION_FAILED");}
    });
  }
  private void finishWatch(boolean ended,String reason){try{boolean required=active!=null&&active.optJSONObject("payload").optBoolean("audioRequired");boolean coverage=startedAtZero&&ended&&!visualGap&&frameCount>=2;boolean sound=coverage&&!audioGap&&signal&&audioCount>=2;
    JSONObject result=new JSONObject().put("playbackEnded",ended).put("coverageComplete",coverage).put("audioCoverageComplete",sound).put("visualObservationMode","timestamped-samples").put("frames",frameCount).put("audioChunks",audioCount).put("lastMediaTime",lastTime).put("understandingVerified",false);
    finish(ended?"DONE":"ERROR",coverage&&(!required||sound),result,reason!=null?reason:(!coverage||required&&!sound?"COVERAGE_UNCERTAIN":null));}catch(Exception e){finish("ERROR",false,new JSONObject(),"WATCH_FINALIZATION_FAILED");}}
  private synchronized void finish(String status,boolean verified,JSONObject result,String reason){if(active==null||finishing)return;finishing=true;JSONObject cmd=active;network.execute(()->{
    try{JSONObject response=new JSONObject().put("taskId",cmd.getString("taskId")).put("generation",cmd.getLong("generation")).put("digest",cmd.getString("digest")).put("status",status).put("verified",verified).put("result",result).put("reason",reason==null?JSONObject.NULL:reason);app.secure.put("pendingResult",response.toString());transport.send("/api/device/result",response,true);app.secure.remove("pendingResult");app.secure.put("lastTask",new JSONObject().put("taskId",cmd.getString("taskId")).put("status",status).put("verified",verified).put("reason",reason).put("completedAt",System.currentTimeMillis()).toString());app.state.task("",new JSONObject().put("lastStatus",status).put("verified",verified));}
    catch(Exception e){app.state.task(cmd.optString("taskId"),new JSONObject());app.state.connection("OFFLINE","RESULT_UNACKNOWLEDGED");}
    finally{active=null;finishing=false;watchId=null;}
  });}
  void control(boolean human){app.state.control(human?"HUMAN":"AGENT");if(human&&active!=null&&active.optString("method").equals("watch"))visualGap=true;}
  @Override public void onDestroy(){stopped=true;current=null;app.state.unlisten(this);main.removeCallbacksAndMessages(null);network.shutdownNow();if(audio!=null)audio.close();if(browser!=null)browser.close();app.state.connection("PAIRED","SERVICE_STOPPED");app.state.browser("IDLE");super.onDestroy();}
}
