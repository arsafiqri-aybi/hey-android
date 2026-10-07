package id.ars.hey;

import org.json.*;

/** Verifies a specific observed transition; never infers success from dispatch alone. */
final class BrowserVerifier {
  static JSONObject media(JSONObject o){JSONArray a=o.optJSONArray("media");return a==null?null:a.optJSONObject(0);}
  static boolean ready(JSONObject o){return "complete".equals(o.optString("documentReady"));}
  static boolean navigation(String expected,JSONObject o){return !UrlPolicy.redact(expected).isEmpty()&&UrlPolicy.redact(expected).equals(UrlPolicy.redact(o.optString("url")))&&ready(o);}
  private static boolean moved(JSONObject before,JSONObject after){JSONObject a=before.optJSONObject("viewport"),b=after.optJSONObject("viewport");return a!=null&&b!=null&&(Math.abs(a.optDouble("scrollX")-b.optDouble("scrollX"))>.5||Math.abs(a.optDouble("scrollY")-b.optDouble("scrollY"))>.5);}
  private static boolean hasTab(JSONObject o,String id){JSONArray a=o.optJSONArray("tabs");if(a!=null)for(int i=0;i<a.length();i++)if(id.equals(a.optJSONObject(i).optString("tabId")))return true;return false;}
  static boolean action(JSONObject p,JSONObject before,JSONObject after,boolean fillVerified){
    boolean navigation=!before.optString("url").equals(after.optString("url"))&&ready(after);
    boolean focus=!before.optString("focusedElement").equals(after.optString("focusedElement"));
    return switch(p.optString("action")){
      case "fill" -> fillVerified;
      case "scroll","drag" -> moved(before,after);
      case "tab_open" -> !hasTab(before,after.optString("tabId"))&&navigation(p.optString("url"),after);
      case "tab_activate" -> p.optString("value").equals(after.optString("tabId"));
      case "tab_close" -> hasTab(before,p.optString("value"))&&!hasTab(after,p.optString("value"));
      case "back","forward" -> navigation||after.optLong("documentEpoch")>before.optLong("documentEpoch")&&ready(after);
      case "reload" -> after.optLong("documentEpoch")>before.optLong("documentEpoch")&&ready(after);
      case "click","key" -> navigation||focus;
      default -> false;
    };
  }
  static boolean mediaAction(JSONObject p,JSONObject before,JSONObject after){
    JSONObject a=media(before),b=media(after);if(a==null||b==null)return false;
    return switch(p.optString("action")){
      case "play" -> !b.optBoolean("paused",true)&&b.optDouble("currentTime")>a.optDouble("currentTime")+.08;
      case "pause" -> b.optBoolean("paused",false);
      case "mute" -> b.optBoolean("muted",false);
      case "unmute" -> !b.optBoolean("muted",true);
      case "seek" -> Math.abs(b.optDouble("currentTime")-p.optDouble("seconds"))<.35;
      default -> false;
    };
  }
}
