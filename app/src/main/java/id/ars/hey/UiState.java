package id.ars.hey;

import org.json.JSONObject;

/** Pure projection: a retained pairing is never proof of a live gateway or ready browser. */
final class UiState {
  enum Mode {
    UNPAIRED,
    PAUSED,
    STARTING,
    OFFLINE,
    STOPPED,
    WORKING,
    HUMAN,
    READY,
    BROWSER_ERROR
  }

  static Mode mode(JSONObject s, boolean paired) {
    if (!paired) return Mode.UNPAIRED;
    if ("PAUSED".equals(s.optString("ownerIntent"))) return Mode.PAUSED;
    if ("STARTING".equals(s.optString("runtime"))) return Mode.STARTING;
    if (!"RUNNING".equals(s.optString("runtime"))) return Mode.STOPPED;
    if (!"ONLINE".equals(s.optString("connection"))) return Mode.OFFLINE;
    if (!java.util.Set.of("READY", "LOADING").contains(s.optString("browser")))
      return Mode.BROWSER_ERROR;
    if ("HUMAN".equals(s.optString("control"))) return Mode.HUMAN;
    if (!s.optString("taskId").isEmpty()) return Mode.WORKING;
    return "READY".equals(s.optString("browser")) ? Mode.READY : Mode.WORKING;
  }

  static String status(Mode m) {
    return switch (m) {
      case UNPAIRED -> "Belum terhubung";
      case PAUSED -> "Dijeda";
      case READY -> "Browser siap";
      case WORKING -> "AI bekerja";
      case HUMAN -> "Kendali manusia";
      case STARTING -> "Menyiapkan";
      case OFFLINE -> "Koneksi terputus";
      case STOPPED -> "Layanan terhenti";
      case BROWSER_ERROR -> "Browser belum siap";
    };
  }

  static String title(Mode m) {
    return switch (m) {
      case UNPAIRED -> "Hubungkan Hey";
      case PAUSED -> "Hey dijeda";
      case READY -> "Hello.";
      case WORKING, HUMAN -> "Browser aktif";
      case STARTING -> "Menyiapkan Hey";
      case OFFLINE -> "Menunggu koneksi";
      case STOPPED -> "Lanjutkan ruangmu";
      case BROWSER_ERROR -> "Periksa browser";
    };
  }

  static String description(Mode m) {
    return switch (m) {
      case UNPAIRED -> "Sambungkan perangkat untuk mulai menggunakan Hey.";
      case PAUSED -> "Aktivitas AI telah dihentikan sementara.";
      case READY -> "Browser Hey siap menerima pekerjaan.";
      case WORKING -> "AI sedang menjelajahi halaman melalui Browser Hey.";
      case HUMAN -> "Kamu memegang kendali. AI menunggu hingga kamu mengembalikannya.";
      case STARTING -> "Menjalankan layanan dan memeriksa koneksi browser.";
      case OFFLINE -> "Pairing tersimpan. Layanan sedang mencoba menyambungkan kembali.";
      case STOPPED -> "Pairing tersimpan. Jalankan kembali layanan saat kamu siap.";
      case BROWSER_ERROR -> "Layanan aktif, tetapi browser memerlukan perhatian.";
    };
  }

  static String action(Mode m) {
    return switch (m) {
      case UNPAIRED -> "Mulai pairing";
      case PAUSED -> "Lanjutkan Hey";
      case WORKING -> "Ambil alih browser";
      case HUMAN -> "Buka browser";
      case STOPPED, BROWSER_ERROR -> "Coba jalankan Hey";
      default -> "";
    };
  }
}
