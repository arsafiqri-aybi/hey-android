package id.ars.hey;
import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.json.JSONObject;

public final class HeyApp extends Application {
  public StateStore state;SecureStore secure;
  @Override public void onCreate(){super.onCreate();secure=new SecureStore(this);state=new StateStore();if(!secure.get("deviceToken","").isEmpty())state.connection("PAIRED","");initPush();}
  void initPush(){
    try{String options=secure.get("firebase","");if(options.isEmpty())return;JSONObject j=new JSONObject(options);if(FirebaseApp.getApps(this).isEmpty())FirebaseApp.initializeApp(this,new FirebaseOptions.Builder().setApplicationId(j.getString("applicationId")).setApiKey(j.getString("apiKey")).setGcmSenderId(j.getString("senderId")).setProjectId(j.getString("projectId")).build());FirebaseMessaging.getInstance().setAutoInitEnabled(true);FirebaseMessaging.getInstance().getToken().addOnSuccessListener(t->{try{secure.put("pushToken",t);state.wake("REGISTERED");}catch(Exception e){state.wake("REGISTRATION_ERROR");}});}catch(Exception e){state.wake("CONFIGURATION_ERROR");}
  }
}
