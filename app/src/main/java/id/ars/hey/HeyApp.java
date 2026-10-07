package id.ars.hey;
import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;

public final class HeyApp extends Application {
  public StateStore state;SecureStore secure;private final java.util.concurrent.ExecutorService configNetwork=java.util.concurrent.Executors.newSingleThreadExecutor();private long lastConfigAttempt;
  @Override public void onCreate(){super.onCreate();secure=new SecureStore(this);state=new StateStore();if(!secure.get("deviceToken","").isEmpty())state.connection("PAIRED","");state.owner(secure.get("ownerIntent","ACTIVE"));if(secure.get("ownerIntent","ACTIVE").equals("PAUSED")){state.runtime("PAUSED","OWNER_PAUSED");state.connection("PAUSED","OWNER_PAUSED");}initPush();}
  synchronized void refreshConfig(){if(secure.get("deviceToken","").isEmpty()||System.currentTimeMillis()-lastConfigAttempt<300000)return;lastConfigAttempt=System.currentTimeMillis();configNetwork.execute(()->{try{JSONObject c=new Transport(secure).send("/api/config",null,false);if(!c.isNull("firebase")){String next=c.getJSONObject("firebase").toString();boolean changed=!next.equals(secure.get("firebase",""));if(changed)secure.put("firebase",next);if(changed||state.snapshot().optString("wake").equals("REGISTRATION_ERROR"))new android.os.Handler(getMainLooper()).post(this::initPush);}}catch(Exception ignored){}});}
  void initPush(){
    try{String options=secure.get("firebase","");if(options.isEmpty()){state.wake("UNCONFIGURED");return;}JSONObject j=new JSONObject(options);if(FirebaseApp.getApps(this).isEmpty())FirebaseApp.initializeApp(this,new FirebaseOptions.Builder().setApplicationId(j.getString("applicationId")).setApiKey(j.getString("apiKey")).setGcmSenderId(j.getString("senderId")).setProjectId(j.getString("projectId")).build());FirebaseMessaging.getInstance().setAutoInitEnabled(true);FirebaseMessaging.getInstance().getToken().addOnSuccessListener(t->{try{secure.put("pushToken",t);state.wake(t.equals(secure.get("registeredPush",""))?"REGISTERED":"TOKEN_READY");}catch(Exception e){state.wake("REGISTRATION_ERROR");}}).addOnFailureListener(e->state.wake("REGISTRATION_ERROR"));}catch(Exception e){state.wake("CONFIGURATION_ERROR");}
  }
}

