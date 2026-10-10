package id.ars.hey;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeAssetPolicyTest {
  @Test public void onlyTheSelectedWallpaperIsServedAtTheOfflineAlias() {
    String selected = WallpaperPolicy.asset(2160, 3840, 128, true);
    assertEquals("home.html", HomeAssetPolicy.asset(HomeAssetPolicy.HOME_URL, selected));
    assertEquals("wallpaper-1080.webp", HomeAssetPolicy.asset("https://hey-home.invalid/wallpaper.webp", selected));
    assertEquals("wallpaper-2160.webp", HomeAssetPolicy.asset("https://hey-home.invalid/wallpaper.webp", WallpaperPolicy.asset(2160, 3840, 512, false)));
  }
  @Test public void externalAndUnownedAssetRequestsHaveNoLocalRoute() {
    for (String url : new String[]{"https://example.com/wallpaper.webp", "http://hey-home.invalid/wallpaper.webp", "https://hey-home.invalid/wallpaper-2160.webp", "https://hey-home.invalid/private.txt", "file:///android_asset/wallpaper.webp", "https://hey-home.invalid.evil/home.html"})
      assertNull(HomeAssetPolicy.asset(url, "wallpaper-1080.webp"));
  }
}
