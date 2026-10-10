package id.ars.hey;

import org.json.*;

/** Verifies a specific observed transition; never infers success from dispatch alone. */
final class BrowserVerifier {
  static JSONObject media(JSONObject o){JSONArray a=o.optJSONArray("media");return a==null?null:a.optJSONObject(0);}
  static boolean ready(JSONObject o){return "complete".equals(o.optString("documentReady"));}
  static boolean navigation(String expected,JSONObject o){return !UrlPolicy.redact(expected).isEmpty()&&UrlPolicy.redact(expected).equals(UrlPolicy.redact(o.optString("url")))&&ready(o);}
  private static boolean moved(JSONObject p,JSONObject before,JSONObject after){
    JSONObject a=before.optJSONObject("viewport"),b=after.optJSONObject("viewport");
    String region=p.optString("scrollRegionId");
    if((region.isEmpty()||region.equals("window"))&&a!=null&&b!=null&&(Math.abs(a.optDouble("scrollX")-b.optDouble("scrollX"))>.5||Math.abs(a.optDouble("scrollY")-b.optDouble("scrollY"))>.5))return true;
    JSONArray old=before.optJSONArray("scrollRegions"),next=after.optJSONArray("scrollRegions");
    if(old==null||next==null)return false;
    for(int i=0;i<old.length();i++){JSONObject x=old.optJSONObject(i);if(x==null)continue;
      for(int j=0;j<next.length();j++){JSONObject y=next.optJSONObject(j);if(y==null)continue;
        boolean identity=x.has("id")&&y.has("id")?x.optString("id").equals(y.optString("id")):old.length()==next.length()&&i==j;
        if(identity&&(region.isEmpty()||region.equals(x.optString("id")))&&(Math.abs(x.optDouble("top")-y.optDouble("top"))>.5||Math.abs(x.optDouble("left")-y.optDouble("left"))>.5))return true;
      }
    }
    return false;
  }
  private static boolean hasTab(JSONObject o,String id){JSONArray a=o.optJSONArray("tabs");if(a!=null)for(int i=0;i<a.length();i++)if(id.equals(a.optJSONObject(i).optString("tabId")))return true;return false;}
  private static boolean controlChanged(JSONObject p,JSONObject before,JSONObject after){
    String id=p.optString("targetElementId");if(id.isEmpty())return false;
    JSONArray a=before.optJSONArray("elements"),b=after.optJSONArray("elements");if(a==null||b==null)return false;
    for(int i=0;i<a.length();i++)for(int j=0;j<b.length();j++){
      JSONObject x=a.optJSONObject(i),y=b.optJSONObject(j);if(x==null||y==null||!id.equals(x.optString("elementId"))||!id.equals(y.optString("elementId")))continue;
      for(String key:new String[]{"checked","expanded"})if(x.has(key)&&y.has(key)&&!String.valueOf(x.opt(key)).equals(String.valueOf(y.opt(key))))return true;
    }
    return false;
  }
  static boolean action(JSONObject p,JSONObject before,JSONObject after,boolean fillVerified){
    boolean navigation=!before.optString("url").equals(after.optString("url"))&&ready(after);
    boolean focus=!before.optString("focusedElement").equals(after.optString("focusedElement"));
    // A visible DOM change verifies only a browser-side effect, not a business outcome.
    boolean surfaceChange=!before.optString("text").equals(after.optString("text"))||!before.optString("title").equals(after.optString("title"));
    return switch(p.optString("action")){
      case "fill" -> fillVerified;
      case "scroll","drag" -> moved(p,before,after);
      case "tab_open" -> !hasTab(before,after.optString("tabId"))&&navigation(p.optString("url"),after);
      case "tab_activate" -> p.optString("value").equals(after.optString("tabId"));
      case "tab_close" -> hasTab(before,p.optString("value"))&&!hasTab(after,p.optString("value"));
      case "back","forward" -> navigation||after.optLong("documentEpoch")>before.optLong("documentEpoch")&&ready(after);
      case "reload" -> after.optLong("documentEpoch")>before.optLong("documentEpoch")&&ready(after);
      case "click","key" -> navigation||focus||surfaceChange||controlChanged(p,before,after);
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
