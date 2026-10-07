package id.ars.hey;
import android.content.Intent;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public final class WakeService extends FirebaseMessagingService {
  @Override public void onNewToken(String token){HeyApp a=(HeyApp)getApplication();try{a.secure.put("pushToken",token);a.state.wake("TOKEN_READY");}catch(Exception e){a.state.wake("REGISTRATION_ERROR");}}
  @Override public void onMessageReceived(RemoteMessage message){HeyApp a=(HeyApp)getApplication();if(!"hey_task".equals(message.getData().get("type"))||!Transport.GATEWAY.equals(message.getData().get("gateway"))||a.secure.get("deviceToken","").isEmpty()||a.secure.get("ownerIntent","PAUSED").equals("PAUSED"))return;
    if(message.getPriority()!=RemoteMessage.PRIORITY_HIGH){a.state.wake("PRIORITY_DOWNGRADED");return;}
    try{startForegroundService(new Intent(this,HeyService.class));}catch(Exception e){a.state.wake("USER_RESUME_REQUIRED");HeyService.resumeNotification(this);}
  }
}

