package id.ars.hey;
import org.json.JSONObject;
import java.util.concurrent.CopyOnWriteArrayList;

final class StateStore {
  interface Listener{void changed();}
  private final CopyOnWriteArrayList<Listener> listeners=new CopyOnWriteArrayList<>();
  private String connection="UNREGISTERED",browser="IDLE",audio="OFF",wake="UNCONFIGURED",control="AGENT",reason="",taskId="";
  private String runtime="STOPPED",runtimeReason="",maintenanceReason="";
  private String ownerIntent="ACTIVE";private long confirmedAt;
  private JSONObject progress=new JSONObject();
  synchronized JSONObject snapshot(){JSONObject j=new JSONObject();try{j.put("appVersion",BuildConfig.VERSION_NAME).put("runtime",runtime).put("runtimeReason",runtimeReason).put("maintenanceReason",maintenanceReason).put("capabilities",new JSONObject().put("atomicWatch",true).put("sharedWebView",true).put("taskScopedProgress",true)).put("connection",connection.equals("ONLINE")&&System.currentTimeMillis()-confirmedAt>20000?"UNKNOWN":connection).put("browser",browser).put("audio",audio).put("wake",wake).put("control",control).put("reason",reason).put("taskId",taskId).put("ownerIntent",ownerIntent).put("confirmedAt",confirmedAt).put("progress",progress);}catch(Exception ignored){}return j;}
  void listen(Listener l){listeners.add(l);}void unlisten(Listener l){listeners.remove(l);}
  private void emit(){for(Listener l:listeners)l.changed();}
  void maintenance(String why){synchronized(this){maintenanceReason=why;}emit();}
  void runtime(String value,String why){synchronized(this){runtime=value;runtimeReason=why;}emit();}
  void owner(String value){synchronized(this){ownerIntent=value;}emit();}
  void connection(String value,String why){synchronized(this){connection=value;reason=why;if(value.equals("ONLINE"))confirmedAt=System.currentTimeMillis();}emit();}
  void browser(String value){synchronized(this){browser=value;}emit();}
  void audio(String value){synchronized(this){audio=value;}emit();}
  void wake(String value){synchronized(this){wake=value;}emit();}
  void control(String value){synchronized(this){control=value;}emit();}
  void task(String id,JSONObject value){synchronized(this){taskId=id;progress=value;}emit();}
}

