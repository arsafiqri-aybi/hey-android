package id.ars.hey;

/** Selects one pre-sized image; never decodes a 4K bitmap on a small/low-memory screen. */
final class WallpaperPolicy {
  static String asset(int width, int height, int memoryClassMb, boolean lowRam) {
    int edge = Math.max(width, height);
    if (lowRam || memoryClassMb < 192) edge = Math.min(edge, 1920);
    else if (memoryClassMb < 384) edge = Math.min(edge, 2560);
    if (edge <= 1280) return "wallpaper.webp";
    if (edge <= 1920) return "wallpaper-1080.webp";
    if (edge <= 2560) return "wallpaper-1440.webp";
    return "wallpaper-2160.webp";
  }
}
