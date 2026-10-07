package id.ars.hey;

import android.content.Context;
import android.webkit.CookieManager;
import javax.net.ssl.HttpsURLConnection;
import java.net.*;
import java.io.*;
import java.util.UUID;

/** Validates every redirect hop; credentials are never forwarded across origins. */
final class DownloadTransfer {
  static void start(Context context,StateStore state,SecureStore store,String url,String userAgent){
    new Thread(()->{
      String currentUrl=url;
      File output=null;HttpsURLConnection connection=null;
      try{
        String originalOrigin=URI.create(currentUrl).getScheme()+"://"+URI.create(currentUrl).getAuthority();
        for(int hop=0;hop<6;hop++){
          BrowserRuntime.networkUrl(currentUrl);
          connection=(HttpsURLConnection)new URL(currentUrl).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(15000);
          connection.setRequestProperty("User-Agent",userAgent);
          String origin=URI.create(currentUrl).getScheme()+"://"+URI.create(currentUrl).getAuthority();
          if(origin.equals(originalOrigin)){String cookie=CookieManager.getInstance().getCookie(currentUrl);if(cookie!=null)connection.setRequestProperty("Cookie",cookie);}
          int status=connection.getResponseCode();
          if(status>=300&&status<400){String next=connection.getHeaderField("Location");if(next==null)throw new IOException("DOWNLOAD_REDIRECT_INVALID");currentUrl=new URL(new URL(currentUrl),next).toString();connection.disconnect();connection=null;continue;}
          if(status!=200)throw new IOException("DOWNLOAD_HTTP_ERROR");
          File directory=context.getExternalFilesDir("Downloads");if(directory==null)throw new IOException("DOWNLOAD_STORAGE_UNAVAILABLE");
          output=new File(directory,"hey-"+UUID.randomUUID());long total=0;
          try(InputStream in=connection.getInputStream();OutputStream out=new FileOutputStream(output)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){total+=n;if(total>100L*1024*1024)throw new IOException("DOWNLOAD_TOO_LARGE");out.write(b,0,n);}}
          store.put("lastDownload",output.getName());state.browser("DOWNLOAD_COMPLETE");return;
        }
        throw new IOException("DOWNLOAD_REDIRECT_LIMIT");
      }catch(Exception e){if(output!=null)output.delete();state.browser(e.getMessage()!=null&&e.getMessage().matches("[A-Z_]{3,60}")?e.getMessage():"DOWNLOAD_FAILED");}
      finally{if(connection!=null)connection.disconnect();}
    },"Hey download").start();
  }
}
