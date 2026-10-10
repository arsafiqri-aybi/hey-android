package id.ars.hey;

import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Native composition for the approved Settings page. */
final class SettingsScreen {
  private final MainActivity a;

  SettingsScreen(MainActivity a) {
    this.a = a;
  }

  void build() {
    LinearLayout c = a.ui.card(a.ui.primary);
    LinearLayout head = a.row();
    head.addView(a.ui.well(R.drawable.ic_radio_tower, 46));
    LinearLayout texts = a.ui.column();
    texts.setPadding(a.dp(14), 0, a.dp(8), 0);
    texts.addView(a.ui.text("Koneksi & Layanan", 16, a.ui.text, true));
    a.connectionState = a.ui.text("", 12, a.ui.secondary, false);
    texts.addView(a.connectionState);
    a.addWeighted(head, texts);
    a.connectionDot = a.ui.icon(R.drawable.ic_circle_dot, a.ui.muted, 19);
    head.addView(a.connectionDot);
    c.addView(head);
    a.ui.divider(c);
    a.pairValue = a.ui.text("", 13, a.ui.text, true);
    a.serviceValue = a.ui.text("", 13, a.ui.text, true);
    a.browserValue = a.ui.text("", 13, a.ui.text, true);
    pairLine(c, "ChatGPT Pairing", a.pairValue);
    pairLine(c, "Layanan Hey", a.serviceValue);
    pairLine(c, "Browser", a.browserValue);
    a.ui.gap(c, 16);
    a.serviceAction =
        a.ui.button(
            "",
            () -> {
              if (!a.paired()) a.connect();
              else if ("PAUSED".equals(a.app.secure.get("ownerIntent", "ACTIVE"))) a.startBrowser();
              else if (HeyService.current != null && HeyService.current.running())
                HeyService.pause(a);
              else a.startBrowser();
            });
    c.addView(a.serviceAction, new LinearLayout.LayoutParams(-1, -2));
    c.setOnLongClickListener(
        v -> {
          a.navigationMenu();
          return true;
        });
    a.content.addView(c);
    a.content.addView(a.ui.text("Browser & Privasi", 13, a.ui.secondary, true));
    a.ui.gap(a.content, 12);
    LinearLayout privacy = a.ui.card(a.ui.group);
    privacy.setPadding(0, a.dp(4), 0, a.dp(4));
    settingRow(
        privacy,
        "session",
        R.drawable.ic_globe,
        "Sesi Browser",
        "Data browsing dan sesi login",
        () -> {
          LinearLayout d =
              detailColumn(
                  "Profil browser milik Hey. Sesi login dan cookie dipertahankan saat berpindah"
                      + " halaman. Data tidak dihapus dari halaman ini.");
          return d;
        });
    settingRow(
        privacy,
        "privacy",
        R.drawable.ic_shield_check,
        "Privasi & Izin",
        "Akses Android dan pengamatan audio",
        () -> {
          LinearLayout d =
              detailColumn(
                  "Password, OTP, dan data kartu dilindungi saat pengamatan. Izin aplikasi dan sesi"
                      + " audio tetap melalui Android.");
          a.audioStatus = a.ui.text("", 12, a.ui.secondary, false);
          d.addView(a.audioStatus);
          a.audioButton =
              a.ui.button(
                  "Aktifkan audio",
                  () -> {
                    HeyService s = HeyService.current;
                    if ("CAPTURING".equals(a.app.state.snapshot().optString("audio")) && s != null)
                      s.stopAudio();
                    else a.requestAudio();
                  });
          a.ui.gap(d, 12);
          d.addView(a.audioButton);
          a.ui.gap(d, 10);
          d.addView(
              a.ui.button(
                  "Pengaturan izin Android",
                  () ->
                      a.startActivity(
                          new Intent(
                              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                              Uri.parse("package:" + a.getPackageName())))));
          return d;
        });
    settingRow(
        privacy,
        "control",
        R.drawable.ic_hand,
        "Kendali AI & Manusia",
        "Prioritas dan keamanan interaksi",
        () -> {
          LinearLayout d =
              detailColumn(
                  "Sentuhan manusia menghentikan tindakan AI yang berkonflik pada sesi browser yang"
                      + " sama. Kendali dikembalikan secara eksplisit; ChatGPT harus mengamati"
                      + " ulang sebelum melanjutkan.");
          d.addView(
              a.ui.button(
                  "HUMAN".equals(a.app.state.snapshot().optString("control"))
                      ? "Kembalikan kendali ke Hey"
                      : "Ambil alih browser",
                  () -> {
                    HeyService s = HeyService.current;
                    if (s == null) {
                      a.toast("Layanan belum aktif.");
                      return;
                    }
                    s.control(!"HUMAN".equals(a.app.state.snapshot().optString("control")));
                    a.render();
                  }));
          return d;
        });
    a.content.addView(privacy);
    a.content.addView(a.ui.text("Aplikasi", 13, a.ui.secondary, true));
    a.ui.gap(a.content, 12);
    LinearLayout application = a.ui.card(a.ui.group);
    application.setPadding(0, a.dp(4), 0, a.dp(4));
    settingRow(
        application,
        "motion",
        R.drawable.ic_sliders_horizontal,
        "Tampilan & Gerakan",
        "Adaptif mengikuti perangkat",
        () ->
            detailColumn(
                "Ukuran teks, area aman, keyboard, dan gerakan mengikuti Android. Animasi "
                    + (HeyUi.motion() ? "aktif" : "dikurangi oleh sistem")
                    + ". Navigasi tersedia melalui swipe bawah, tekan lama kartu/kolom URL, aksi"
                    + " TalkBack, atau tombol Menu."));
    settingRow(
        application,
        "about",
        R.drawable.ic_info,
        "Tentang Hey",
        "Versi aplikasi dan diagnostik",
        () -> {
          JSONObject s = a.app.state.snapshot();
          return detailColumn(
              "Hey "
                  + BuildConfig.VERSION_NAME
                  + " ("
                  + BuildConfig.VERSION_CODE
                  + ")\n"
                  + a.getPackageName()
                  + "\nRuntime: "
                  + s.optString("runtime")
                  + "\nKoneksi: "
                  + s.optString("connection")
                  + "\nBrowser: "
                  + s.optString("browser")
                  + "\nWake: "
                  + s.optString("wake")
                  + "\n"
                  + s.optString("runtimeReason"));
        });
    a.content.addView(application);
    a.ui.gap(a.content, 8);
    TextView footer = a.ui.text("Hey by Ars", 12, a.ui.muted, true);
    footer.setGravity(Gravity.CENTER);
    a.content.addView(footer);
    TextView sub = a.ui.text("Personal AI Browser Companion", 11, a.ui.muted, false);
    sub.setGravity(Gravity.CENTER);
    a.content.addView(sub);
  }

  private void pairLine(LinearLayout card, String label, TextView value) {
    LinearLayout r = a.row();
    r.setMinimumHeight(a.dp(34));
    boolean stacked = a.getResources().getConfiguration().fontScale > 1.3f;
    if (stacked) {
      r.setOrientation(LinearLayout.VERTICAL);
      r.setGravity(Gravity.START);
      r.setPadding(0, a.dp(6), 0, a.dp(6));
      r.addView(a.ui.text(label, 13, a.ui.secondary, false));
    } else {
      a.addWeighted(r, a.ui.text(label, 13, a.ui.secondary, false));
    }
    value.setGravity(stacked ? Gravity.START : Gravity.END);
    r.addView(value, new LinearLayout.LayoutParams(-2, -2));
    card.addView(r);
  }

  private LinearLayout detailColumn(String body) {
    LinearLayout d = a.ui.column();
    d.setPadding(a.dp(18), a.dp(16), a.dp(18), a.dp(16));
    d.setBackgroundColor(a.ui.nested);
    d.addView(a.ui.text(body, 13, a.ui.secondary, false));
    return d;
  }

  private void settingRow(
      LinearLayout group,
      String key,
      int icon,
      String title,
      String subtitle,
      java.util.function.Supplier<LinearLayout> detail) {
    if (group.getChildCount() > 0) {
      View line = new View(a);
      line.setBackgroundColor(a.ui.divider);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, a.dp(1));
      lp.leftMargin = a.dp(76);
      lp.rightMargin = a.dp(18);
      group.addView(line, lp);
    }
    LinearLayout r = a.row();
    r.setPadding(a.dp(18), a.dp(16), a.dp(18), a.dp(16));
    r.setMinimumHeight(a.dp(80));
    r.addView(a.ui.well(icon, 44));
    LinearLayout labels = a.ui.column();
    labels.setPadding(a.dp(14), 0, a.dp(8), 0);
    labels.addView(a.ui.text(title, 14, a.ui.text, true));
    a.ui.gap(labels, 3);
    labels.addView(a.ui.text(subtitle, 11, a.ui.secondary, false));
    a.addWeighted(r, labels);
    r.addView(
        a.ui.icon(
            a.expanded.equals(key) ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right,
            a.ui.muted,
            17));
    r.setBackground(
        new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x15ffffff), null, null));
    r.setFocusable(true);
    r.setContentDescription(
        title + ". " + subtitle + ". " + (a.expanded.equals(key) ? "Diperluas" : "Diciutkan"));
    r.setOnClickListener(
        v -> {
          a.expanded = a.expanded.equals(key) ? "" : key;
          a.render();
        });
    group.addView(r);
    if (a.expanded.equals(key)) group.addView(detail.get());
  }
}
