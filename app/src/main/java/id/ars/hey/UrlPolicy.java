package id.ars.hey;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class UrlPolicy {
  static void validate(String value)throws Exception{
    URI u=new URI(value);String host=u.getHost();
    if(!"https".equalsIgnoreCase(u.getScheme())||host==null||u.getUserInfo()!=null||u.getPort()!=-1&&u.getPort()!=443)throw new SecurityException("HTTPS_REQUIRED");
    host=host.toLowerCase(Locale.ROOT).replaceFirst("\\.$","");
    if(host.equals("localhost")||host.endsWith(".localhost")||host.endsWith(".local")||host.endsWith(".internal")||host.matches("[0-9.]+")||host.contains(":"))throw new SecurityException("PUBLIC_HOST_REQUIRED");
  }
  static String redact(String value){
    if(value==null)return "";try{
      URI u=new URI(value);if(u.getHost()==null)return value.equals("about:blank")?value:"";
      StringBuilder query=new StringBuilder();if(u.getRawQuery()!=null)for(String pair:u.getRawQuery().split("&",-1)){
        String[] fields=pair.split("=",2);String key=URLDecoder.decode(fields[0],"UTF-8"),v=fields.length==2?URLDecoder.decode(fields[1],"UTF-8"):"";
        if(key.matches("(?i)access_token|refresh_token|id_token|token|code|password|secret|api_key|apikey|otp|authorization|session|credential"))v="[redacted]";
        if(query.length()>0)query.append('&');query.append(URLEncoder.encode(key,"UTF-8")).append('=').append(URLEncoder.encode(v,"UTF-8"));
      }
      String path=u.getRawPath();if(path==null||path.isEmpty())path="/";String fragment=u.getRawFragment();
      if(fragment!=null&&fragment.matches("(?is).*(access_token|id_token|token|password|secret)=.*"))fragment="%5Bredacted%5D";
      return u.getScheme().toLowerCase(Locale.ROOT)+"://"+u.getHost().toLowerCase(Locale.ROOT).replaceFirst("\\.$","")+(u.getPort()!=-1&&u.getPort()!=443?":"+u.getPort():"")+path+(u.getRawQuery()!=null?"?"+query:"")+(fragment!=null?"#"+fragment:"");
    }catch(Exception e){return "";}
  }
}
