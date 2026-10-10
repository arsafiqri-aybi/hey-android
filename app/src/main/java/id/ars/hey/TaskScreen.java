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
                            : "Pekerjaan dihentikan: "
                                + last.optString("reason", "Penyebab tidak tersedia"),
            14,
            a.ui.secondary,
            false));
    a.ui.gap(card, 18);
    if (human) card.addView(a.ui.button("Buka browser", () -> a.select(1)));
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
                + (active ? "RUNNING" : last.optString("status"))
                + "\nHasil: "
                + (active
                    ? "belum selesai"
                    : last.optBoolean("verified") ? "terverifikasi" : "belum terverifikasi");
        if (p != null) {
          if (p.has("frames")) body += "\nFrame terekam: " + p.optInt("frames");
          if (p.has("audioChunks")) body += "\nSegmen audio: " + p.optInt("audioChunks");
          if (p.has("postcondition")) body += "\nBukti perubahan: " + p.optString("postcondition");
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
                t.optString("status")
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
}
