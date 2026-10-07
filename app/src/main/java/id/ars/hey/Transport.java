package id.ars.hey;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import java.net.URI;
import java.net.URL;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class Transport {
  static final String GATEWAY="https://hey-gateway.arsafiqri-ua03.workers.dev";
  private final SecureStore store;
  Transport(SecureStore store){this.store=store;}
  static void gateway(String value)throws Exception{URI u=new URI(value);if(!GATEWAY.equals(value)||!"https".equals(u.getScheme()))throw new IOException("GATEWAY_NOT_TRUSTED");}
  JSONObject send(String path,JSONObject body,boolean authenticated)throws Exception{
    String base=store.get("gateway",GATEWAY);gateway(base);HttpsURLConnection c=(HttpsURLConnection)new URL(base+path).openConnection();
    c.setConnectTimeout(10000);c.setReadTimeout(12000);c.setInstanceFollowRedirects(false);c.setRequestMethod(body==null?"GET":"POST");c.setRequestProperty("Accept","application/json");
    if(authenticated){String token=store.get("deviceToken","");if(token.isEmpty())throw new IOException("NOT_PAIRED");c.setRequestProperty("Authorization","Bearer "+token);}
    try{if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
      int status=c.getResponseCode();InputStream in=status>=400?c.getErrorStream():c.getInputStream();if(in==null)throw new IOException("HTTP_"+status);
      ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[4096];int count;try(in){while((count=in.read(buf))!=-1){if(out.size()+count>1000000)throw new IOException("RESPONSE_TOO_LARGE");out.write(buf,0,count);}}
      JSONObject result=new JSONObject(out.toString("UTF-8"));if(status>=400)throw new IOException(result.optString("error","HTTP_"+status));return result;
    }finally{c.disconnect();}
  }
}
