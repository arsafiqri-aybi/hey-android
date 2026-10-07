package id.ars.hey;
import android.content.*;
public final class PauseReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context,Intent intent){HeyService.pause(context);}
}
