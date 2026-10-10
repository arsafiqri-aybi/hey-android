package id.ars.hey;

/** Exact app-owned offline routes, separate from the browsing WebView. */
final class HomeAssetPolicy {
  static final String HOME_URL = "https://hey-home.invalid/home.html";
  static String asset(String url, String wallpaper) {
    if (HOME_URL.equals(url)) return "home.html";
    if ("https://hey-home.invalid/wallpaper.webp".equals(url)) return wallpaper;
    return null;
  }
}
