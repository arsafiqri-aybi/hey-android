package id.ars.hey;

import android.content.*;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Native composition for the approved Task page. */
final class TaskScreen {
  private final MainActivity a;

  TaskScreen(MainActivity a) {
    this.a = a;
  }

  void build() {
    JSONObject s = a.app.state.snapshot(), last = a.lastTask();
    boolean active = !s.optString("taskId").isEmpty();
    JSONObject info = active ? s.optJSONObject("taskInfo") : last;
    if (info == null) info = new JSONObject();
    boolean exists = active || last.has("status");
    boolean human =
        "HUMAN".equals(s.optString("control"))
            && "HUMAN_CONTROL_ACTIVE".equals(last.optString("reason"));
    LinearLayout card = a.ui.card(a.ui.task);
    LinearLayout heading = a.row();
    heading.addView(a.ui.well(R.drawable.ic_tasks, 46));
    LinearLayout labels = a.ui.column();
    labels.setPadding(a.dp(14), 0, 0, 0);
    labels.addView(
        a.ui.text(
            !exists
                ? "Belum ada tugas aktif"
                : human
                    ? "Menunggu bantuanmu"
                    : active
                        ? "Sedang berlangsung"
                        : "DONE".equals(last.optString("status"))
                            ? "Eksekusi selesai"
                            : "Tugas terhenti",
            16,
            a.ui.text,
            true));
    labels.addView(
        a.ui.text(
            !exists
                ? "Pekerjaan dimulai dari ChatGPT"
                : active
                    ? "AI bekerja"
                    : human
                        ? "Kendali manusia"
                        : last.optBoolean("verified")
                            ? "Hasil terverifikasi"
                            : "Hasil belum terverifikasi",
            12,
            !exists
                ? a.ui.muted
                : human
                    ? a.ui.attention
                    : active
                        ? a.ui.working
                        : last.optBoolean("verified") ? a.ui.healthy : a.ui.attention,
            false));
    a.addWeighted(heading, labels);
    card.addView(heading);
    a.ui.gap(card, 30);
    TextView name =
        a.ui.text(
            exists
                ? a.taskName(info.optString("method"), info.optString("action"))
                : "Ruang untuk pekerjaanmu.",
            25,
            a.ui.text,
            true);
    card.addView(name);
    a.ui.gap(card, 12);
    card.addView(
        a.ui.text(
            !exists
                ? "Tugas dan hasil pekerjaan Hey akan muncul di sini."
                : human
                    ? "Selesaikan interaksi di browser. Kembalikan kendali kepada Hey saat siap."
                    : active
                        ? "Hey sedang menjalankan instruksi. Hasil akan ditampilkan setelah"
                            + " eksekusi tercatat."
                        : "DONE".equals(last.optString("status"))
                            ? last.optBoolean("verified")
                                ? "Eksekusi tercatat dengan bukti yang sesuai."
                                : "Eksekusi selesai. Bukti belum memastikan hasil akhirnya."
                            : interruption(last.optString("reason")),
            14,
            a.ui.secondary,
            false));
    a.ui.gap(card, 18);
    if (human) {
      card.addView(a.ui.button("Buka browser", () -> a.select(1)));
      a.ui.gap(card, 8);
    }
    if (exists) {
      Button expand =
          a.ui.button(
              a.expanded.equals("task") ? "Tutup detail tugas" : "Lihat detail tugas",
              () -> {
                a.expanded = a.expanded.equals("task") ? "" : "task";
                a.render();
              });
      card.addView(expand);
      if (a.expanded.equals("task")) {
        LinearLayout nested = a.ui.column();
        nested.setPadding(a.dp(16), a.dp(16), a.dp(16), a.dp(16));
        nested.setBackground(a.ui.surface(a.ui.nested, 16, 0));
        a.ui.gap(card, 12);
        JSONObject p = active ? s.optJSONObject("progress") : last.optJSONObject("evidence");
        String body =
            "Status: "
                + statusLabel(active ? "RUNNING" : last.optString("status"))
                + "\nHasil: "
                + (active
                    ? "belum selesai"
                    : last.optBoolean("verified") ? "terverifikasi" : "belum terverifikasi");
        if (p != null) {
          if (p.has("frames")) body += "\nFrame terekam: " + p.optInt("frames");
          if (p.has("audioChunks")) body += "\nSegmen audio: " + p.optInt("audioChunks");
          if (p.has("postcondition"))
            body += "\nBukti perubahan: " + evidenceLabel(p.optString("postcondition"));
        }
        if (last.has("completedAt") && !active)
          body +=
              "\nTercatat: "
                  + android.text.format.DateFormat.format(
                      "dd MMM · HH:mm", last.optLong("completedAt"));
        nested.addView(a.ui.text(body, 13, a.ui.secondary, false));
        card.addView(nested);
      }
    }
    a.content.addView(card);
    TextView section = a.ui.text("Aktivitas terakhir", 14, a.ui.secondary, true);
    a.content.addView(section);
    a.ui.gap(a.content, 12);
    LinearLayout recent = a.ui.card(a.ui.group);
    try {
      JSONArray history = new JSONArray(a.app.secure.get("taskHistory", "[]"));
      if (history.length() == 0 && last.has("status")) history.put(last);
      if (history.length() == 0)
        recent.addView(a.ui.text("Belum ada aktivitas", 13, a.ui.muted, false));
      for (int i = 0; i < Math.min(3, history.length()); i++) {
        JSONObject t = history.getJSONObject(i);
        if (i > 0) a.ui.divider(recent);
        recent.addView(
            a.ui.text(
                a.taskName(t.optString("method"), t.optString("action")), 14, a.ui.text, true));
        a.ui.gap(recent, 5);
        recent.addView(
            a.ui.text(
                statusLabel(t.optString("status"))
                    + " · "
                    + (t.optBoolean("verified") ? "Terverifikasi" : "Belum terverifikasi"),
                12,
                a.ui.muted,
                false));
      }
    } catch (Exception e) {
      recent.addView(a.ui.text("Riwayat belum tersedia", 13, a.ui.muted, false));
    }
    a.content.addView(recent);
    card.setOnLongClickListener(
        v -> {
          a.navigationMenu();
          return true;
        });
    a.taskSignature = a.taskSignature();
  }

  private static String statusLabel(String status) {
    return switch (status) {
      case "RUNNING" -> "Sedang berlangsung";
      case "DONE" -> "Selesai";
      case "FAILED" -> "Terhenti";
      case "CANCELLED" -> "Dibatalkan";
      case "UNKNOWN" -> "Belum dipastikan";
      case "QUEUED" -> "Dalam antrean";
      case "WAITING_DEVICE" -> "Menunggu perangkat";
      default -> "Status belum tersedia";
    };
  }

  private static String interruption(String reason) {
    return switch (reason) {
      case "EXECUTION_ABORTED" -> "Eksekusi dihentikan sebelum selesai.";
      case "OWNER_PAUSED" -> "Hey dijeda saat pekerjaan masih berlangsung.";
      case "HUMAN_CONTROL_ACTIVE" -> "Kendali browser sedang berada di tanganmu.";
      case "BROWSER_UNAVAILABLE" -> "Browser belum tersedia untuk melanjutkan pekerjaan.";
      case "STALE_REFERENCE" -> "Halaman berubah. Hey perlu mengamati ulang sebelum melanjutkan.";
      case "NAVIGATION_TIMEOUT" -> "Halaman belum selesai dibuka dalam waktu yang tersedia.";
      case "TLS_ERROR" -> "Koneksi aman ke halaman tidak dapat diverifikasi.";
      default -> "Pekerjaan terhenti. Penyebabnya belum dapat dipastikan.";
    };
  }

  private static String evidenceLabel(String evidence) {
    return switch (evidence) {
      case "SCROLL_CHANGED" -> "Posisi halaman berubah";
      case "VALUE_CHANGED" -> "Nilai isian berubah";
      case "URL_CHANGED" -> "Alamat halaman berubah";
      case "TAB_CHANGED" -> "Tab aktif berubah";
      default -> evidence;
    };
  }
}
