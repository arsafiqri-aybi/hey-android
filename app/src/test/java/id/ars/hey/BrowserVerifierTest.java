package id.ars.hey;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;

public class BrowserVerifierTest {
  private JSONObject o(String json)throws Exception{return new JSONObject(json);}
  @Test public void navigationUsesRedactedCanonicalUrlWithoutExposingSecrets()throws Exception{
    JSONObject after=o("{url:'https://example.com/?token=%5Bredacted%5D&q=a+b',documentReady:'complete'}");
    assertTrue(BrowserVerifier.navigation("https://example.com/?token=private-test-value&q=a%20b",after));
    assertFalse(BrowserVerifier.navigation("https://different.example/?token=private-test-value",after));
    assertFalse(UrlPolicy.redact("https://example.com/?token=private-test-value").contains("private-test-value"));
  }
  @Test public void scrollRequiresObservedMovement()throws Exception{
    JSONObject p=o("{action:'scroll',x:0,y:600}"),before=o("{viewport:{scrollX:0,scrollY:0}}");
    assertTrue(BrowserVerifier.action(p,before,o("{viewport:{scrollX:0,scrollY:176.66}}"),false));
    assertFalse(BrowserVerifier.action(p,before,before,false));
  }
  @Test public void nestedScrollIsVerifiedWithoutWindowMovement()throws Exception{
    JSONObject before=o("{viewport:{scrollX:0,scrollY:0},scrollRegions:[{index:0,top:0,left:0}]}");
    JSONObject after=o("{viewport:{scrollX:0,scrollY:0},scrollRegions:[{index:0,top:180,left:0}]}");
    assertTrue(BrowserVerifier.action(o("{action:'scroll'}"),before,after,false));
    assertFalse(BrowserVerifier.action(o("{action:'scroll'}"),before,before,false));
    assertFalse(BrowserVerifier.action(o("{action:'scroll'}"),before,o("{viewport:{scrollX:0,scrollY:0},scrollRegions:[]}"),false));
  }
  @Test public void clickRequiresObservableEffectRatherThanDispatchAlone()throws Exception{
    JSONObject before=o("{text:'Before',title:'Page',url:'https://example.com/',documentReady:'complete',focusedElement:'button'}");
    JSONObject changed=o("{text:'After',title:'Page',url:'https://example.com/',documentReady:'complete',focusedElement:'button'}");
    assertTrue(BrowserVerifier.action(o("{action:'click'}"),before,changed,false));
    assertFalse(BrowserVerifier.action(o("{action:'click'}"),before,before,false));
  }
  @Test public void nestedRegionsUseIdentityRatherThanArrayPosition()throws Exception{
    JSONObject p=o("{action:'scroll',scrollRegionId:'e1'}");
    JSONObject before=o("{scrollRegions:[{id:'e1',top:0,left:0},{id:'e2',top:80,left:0}]}");
    JSONObject reordered=o("{scrollRegions:[{id:'e2',top:80,left:0},{id:'e1',top:0,left:0}]}");
    assertFalse(BrowserVerifier.action(p,before,reordered,false));
    reordered.getJSONArray("scrollRegions").getJSONObject(1).put("top",40);
    assertTrue(BrowserVerifier.action(p,before,reordered,false));
    assertFalse(BrowserVerifier.action(o("{action:'scroll',scrollRegionId:'e2'}"),before,reordered,false));
  }
  @Test public void checkboxChangesAreBoundToTheClickedElement()throws Exception{
    JSONObject p=o("{action:'click',targetElementId:'e1'}");
    JSONObject before=o("{elements:[{elementId:'e1',checked:false}]}");
    assertTrue(BrowserVerifier.action(p,before,o("{elements:[{elementId:'e1',checked:true}]}"),false));
    assertFalse(BrowserVerifier.action(p,before,o("{elements:[{elementId:'other',checked:true}]}"),false));
  }
  @Test public void tabVerificationUsesIdentityAndCommittedPage()throws Exception{
    JSONObject before=o("{tabs:[{tabId:'one'}],tabId:'one'}"),after=o("{tabs:[{tabId:'one'},{tabId:'two'}],tabId:'two',url:'https://example.com/',documentReady:'complete'}");
    assertTrue(BrowserVerifier.action(o("{action:'tab_open',url:'https://example.com'}"),before,after,false));
    after.put("url","about:blank");assertFalse(BrowserVerifier.action(o("{action:'tab_open',url:'https://example.com'}"),before,after,false));
    assertTrue(BrowserVerifier.action(o("{action:'tab_activate',value:'two'}"),before,after,false));
    assertTrue(BrowserVerifier.action(o("{action:'tab_close',value:'two'}"),after,before,false));
  }
  @Test public void reloadNeedsAnActualDocumentLifecycle()throws Exception{
    JSONObject before=o("{documentEpoch:1,documentReady:'complete'}");
    assertFalse(BrowserVerifier.action(o("{action:'reload'}"),before,before,false));
    assertTrue(BrowserVerifier.action(o("{action:'reload'}"),before,o("{documentEpoch:2,documentReady:'complete'}"),false));
  }
  @Test public void playRequiresBothUnpausedAndClockAdvancement()throws Exception{
    JSONObject before=o("{media:[{paused:true,currentTime:0}]}");
    assertFalse(BrowserVerifier.mediaAction(o("{action:'play'}"),before,o("{media:[{paused:false,currentTime:0}]}")));
    assertTrue(BrowserVerifier.mediaAction(o("{action:'play'}"),before,o("{media:[{paused:false,currentTime:0.25}]}")));
    assertFalse(BrowserVerifier.mediaAction(o("{action:'play'}"),before,o("{media:[{paused:true,currentTime:0.25}]}")));
  }
  @Test public void sensitiveFillReturnsOnlyAnEqualityPostcondition()throws Exception{
    assertTrue(BrowserVerifier.action(o("{action:'fill'}"),new JSONObject(),new JSONObject(),true));
    assertFalse(BrowserVerifier.action(o("{action:'fill'}"),new JSONObject(),new JSONObject(),false));
  }
  @Test public void hostnamePolicyRejectsTerminalDotLocalAndNumericHosts()throws Exception{
    for(String url:new String[]{"https://localhost./","https://a.localhost./","https://127.1/","https://2130706433/","http://example.com/"}){try{UrlPolicy.validate(url);fail(url);}catch(SecurityException expected){}}
    UrlPolicy.validate("https://example.com./");
  }
}
