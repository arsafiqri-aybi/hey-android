package id.ars.hey;
import org.junit.Test;
import static org.junit.Assert.*;

public class WallpaperPolicyTest {
  @Test public void matchesScreenWithoutOverDecoding() {
    assertEquals("wallpaper.webp", WallpaperPolicy.asset(720,1280,256,false));
    assertEquals("wallpaper-1080.webp", WallpaperPolicy.asset(720,1600,256,false));
    assertEquals("wallpaper-1440.webp", WallpaperPolicy.asset(1080,2400,256,false));
    assertEquals("wallpaper-2160.webp", WallpaperPolicy.asset(2160,3840,512,false));
  }
  @Test public void largeLowMemoryScreenHasBoundedDecode() {
    assertEquals("wallpaper-1080.webp", WallpaperPolicy.asset(2160,3840,128,true));
    assertEquals("wallpaper-1440.webp", WallpaperPolicy.asset(2160,3840,256,false));
    assertEquals(WallpaperPolicy.asset(2400,1080,256,false), WallpaperPolicy.asset(1080,2400,256,false));
  }
}
