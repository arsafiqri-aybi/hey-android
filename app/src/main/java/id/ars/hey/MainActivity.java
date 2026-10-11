package id.ars.hey;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.concurrent.Executors;
import org.json.*;

/** Owner-approved native chrome. BrowserRuntime owns the WebView, never the page renderer. */
public final class MainActivity extends Activity implements StateStore.Listener {
  HeyApp app;
  HeyUi ui;
  private HeyShell shell;
  private FrameLayout stage, browserHost;
  LinearLayout content, browserPage;
  private EditText address;
  private TextView browserEmpty;
  private final Handler main = new Handler(Looper.getMainLooper());
  private final java.util.concurrent.ExecutorService network = Executors.newSingleThreadExecutor();
  private final RuntimeStartPolicy startPolicy = new RuntimeStartPolicy();
  private boolean visible;
  private int page;
  private int renderedPage = -1;
  String expanded = "", taskSignature = "";
  TextView homeStatus,
      heroTitle,
      heroDescription,
      activity,
      connectionState,
      pairValue,
      serviceValue,
      browserValue;
  ImageView connectionDot;
  Button homeAction, serviceAction;
  TextView audioStatus;
  Button audioButton;
  private UiState.Mode mode;

  @Override
  public void onCreate(Bundle state) {
    setTheme(R.style.HeyTheme);
    super.onCreate(state);
    app = (HeyApp) getApplication();
    ui = new HeyUi(this);
    app.state.listen(this);
    if (state != null) {
      page = state.getInt("page", 0);
      expanded = state.getString("expanded", "");
    }
    getWindow().setDecorFitsSystemWindows(false);
    getWindow().setStatusBarColor(ui.canvas);
    getWindow().setNavigationBarColor(ui.canvas);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    build();
    getWindow()
        .getInsetsController()
        .setSystemBarsAppearance(
            0,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
    pairIntent(getIntent());
  }

  @Override
  protected void onSaveInstanceState(Bundle s) {
    s.putInt("page", page);
    s.putString("expanded", expanded);
    super.onSaveInstanceState(s);
  }

  @Override
  protected void onNewIntent(Intent i) {
    super.onNewIntent(i);
    setIntent(i);
    pairIntent(i);
  }

  int dp(float n) {
    return ui.dp(n);
  }

  boolean paired() {
    return !app.secure.get("deviceToken", "").isEmpty();
  }

  private void build() {
    shell = new HeyShell(this, this::select, this::navigationMenu);
    stage = new FrameLayout(this);
    shell.addView(stage, new FrameLayout.LayoutParams(-1, -1));
    shell.overlay();
    setContentView(shell);
    shell.setOnApplyWindowInsetsListener(
        (v, insets) -> {
          Insets bars =
              insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
          Insets gestures = insets.getInsets(WindowInsets.Type.mandatorySystemGestures());
          Insets systemGestures = insets.getInsets(WindowInsets.Type.systemGestures());
          int bottom = Math.max(bars.bottom, Math.max(gestures.bottom, systemGestures.bottom));
          boolean ime = insets.isVisible(WindowInsets.Type.ime());
          if (ime) bottom = Math.max(bottom, insets.getInsets(WindowInsets.Type.ime()).bottom);
          v.setPadding(
              Math.max(bars.left, gestures.left),
              bars.top,
              Math.max(bars.right, gestures.right),
              bottom);
          shell.ime(ime);
          return WindowInsets.CONSUMED;
        });
    shell.requestApplyInsets();
    render();
  }

  void navigationMenu() {
    new AlertDialog.Builder(this)
        .setTitle("Buka halaman")
        .setItems(new String[] {"Home", "Browser", "Tasks", "Settings"}, (d, i) -> select(i))
        .show();
  }

  void select(int index) {
    if (page == index) return;
    page = index;
    render();
  }

  void render() {
    int retainedScroll = 0;
    if (renderedPage == page
        && stage.getChildCount() > 0
        && stage.getChildAt(0) instanceof ScrollView) {
      retainedScroll = stage.getChildAt(0).getScrollY();
    }
    renderedPage = page;
    HeyService s = HeyService.current;
    if (s != null && s.browser != null && page != 1) s.browser.detach(this);
    stage.removeAllViews();
    homeStatus =
        heroTitle =
            heroDescription =
                activity =
                    connectionState = pairValue = serviceValue = browserValue = audioStatus = null;
    homeAction = serviceAction = audioButton = null;
    connectionDot = null;
    shell.page(page, page == 1 ? this::probeArc : null);
    shell.setContentDescription(
        new String[] {"Home", "Browser", "Tasks", "Settings"}[page]
            + ". Navigasi tersedia melalui aksi aksesibilitas atau tekan lama.");
    if (page == 1) {
      browser();
      update();
      return;
    }
    ScrollView scroll = new ScrollView(this);
    scroll.setFillViewport(true);
    scroll.setVerticalScrollBarEnabled(false);
    content = ui.column();
    content.setPadding(dp(22), dp(20), dp(22), dp(24));
    scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
    FrameLayout.LayoutParams centered =
        new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER_HORIZONTAL);
    stage.addView(scroll, centered);
    scroll.addOnLayoutChangeListener(
        (v, l, t, r, b, ol, ot, or, ob) -> {
          int extra = Math.max(0, (r - l - dp(620)) / 2);
          if (content.getPaddingLeft() != dp(22) + extra)
            content.setPadding(dp(22) + extra, dp(20), dp(22) + extra, dp(24));
        });
    switch (page) {
      case 2:
        new TaskScreen(this).build();
        break;
      case 3:
        new SettingsScreen(this).build();
        break;
      default:
        new HomeScreen(this).build();
    }
    update();
    final int restoreY = retainedScroll;
    scroll.post(() -> scroll.scrollTo(0, restoreY));
  }

  LinearLayout row() {
    LinearLayout r = new LinearLayout(this);
    r.setGravity(Gravity.CENTER_VERTICAL);
    return r;
  }

  void addWeighted(LinearLayout r, View child) {
    r.addView(child, new LinearLayout.LayoutParams(0, -2, 1));
  }

  void homeAction() {
    switch (mode) {
      case WORKING:
        takeHuman();
        select(1);
        break;
      case HUMAN:
        select(1);
        break;
      default:
        connect();
    }
  }

  private void browser() {
    if (browserPage == null) {
      browserPage = ui.column();
      browserPage.setBackgroundColor(getColor(R.color.hey_chrome));
      LinearLayout urlRegion = ui.column();
      urlRegion.setPadding(dp(16), dp(10), dp(16), dp(10));
      address = new EditText(this);
      address.setTextColor(ui.text);
      address.setHintTextColor(ui.muted);
      address.setTextSize(14);
      address.setSingleLine(true);
      address.setSelectAllOnFocus(true);
      address.setHint("Alamat atau pencarian");
      address.setContentDescription(
          "Alamat atau pencarian. Navigasi halaman tersedia melalui aksi aksesibilitas.");
      address.setInputType(
          android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
      address.setImeOptions(EditorInfo.IME_ACTION_GO);
      address.setPadding(dp(16), dp(12), dp(16), dp(12));
      address.setMinimumHeight(dp(48));
      address.setBackground(ui.surface(ui.group, 17, ui.divider));
      address.setOnEditorActionListener(
          (v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO
                || event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
              navigateAddress();
              return true;
            }
            return false;
          });
      urlRegion.addView(address, new LinearLayout.LayoutParams(-1, -2));
      browserPage.addView(urlRegion, new LinearLayout.LayoutParams(-1, -2));
      browserPage.setFocusableInTouchMode(true);
      browserPage.requestFocus();
      browserHost = new FrameLayout(this);
      browserHost.setContentDescription("Browser Hey");
      browserPage.addView(browserHost, new LinearLayout.LayoutParams(-1, 0, 1));
    }
    if (browserPage.getParent() instanceof android.view.ViewGroup)
      ((android.view.ViewGroup) browserPage.getParent()).removeView(browserPage);
    stage.addView(browserPage, new FrameLayout.LayoutParams(-1, -1));
    attachBrowser();
  }

  private void navigateAddress() {
    HeyService s = HeyService.current;
    if (s == null || s.browser == null) {
      connect();
      return;
    }
    try {
      String url = AddressInput.resolve(address.getText().toString());
      takeHuman();
      s.browser.humanNavigate(
          url,
          (o, e) -> {
            if (e != null) toast("Halaman belum dapat dibuka: " + e);
          });
      address.clearFocus();
      ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
          .hideSoftInputFromWindow(address.getWindowToken(), 0);
    } catch (Exception e) {
      toast("Gunakan alamat HTTPS publik atau kata pencarian.");
    }
  }

  private void takeHuman() {
    HeyService s = HeyService.current;
    if (s != null) s.control(true);
  }

  private void probeArc(float x, float y, java.util.function.Consumer<Boolean> result) {
    HeyService s = HeyService.current;
    if (s == null || s.browser == null || browserHost == null) {
      result.accept(true);
      return;
    }
    int[] pos = new int[2];
    browserHost.getLocationOnScreen(pos);
    int[] shellPos = new int[2];
    shell.getLocationOnScreen(shellPos);
    s.browser.checkGesture(x + shellPos[0] - pos[0], y + shellPos[1] - pos[1], result);
  }

  private void attachBrowser() {
    if (page != 1 || browserHost == null || !visible) return;
    HeyService s = HeyService.current;
    if (s != null && s.browser != null) {
      if (browserEmpty != null) {
        browserHost.removeView(browserEmpty);
        browserEmpty = null;
      }
      s.browser.attachIfNeeded(this, browserHost);
      if (!address.hasFocus()) address.setText(s.browser.currentUrl());
      if ("FILE_SELECTION_REQUIRED".equals(app.state.snapshot().optString("browser")))
        showFilePicker();
    } else if (browserEmpty == null) {
      browserEmpty =
          ui.text(
              paired()
                  ? "Browser belum aktif.\nKetuk untuk menjalankan Hey."
                  : "Hubungkan Hey untuk membuka browser.\nKetuk untuk panduan pairing.",
              15,
              ui.secondary,
              false);
      browserEmpty.setGravity(Gravity.CENTER);
      browserEmpty.setPadding(dp(30), dp(20), dp(30), dp(20));
      browserEmpty.setOnClickListener(v -> connect());
      browserEmpty.setOnLongClickListener(
          v -> {
            navigationMenu();
            return true;
          });
      browserHost.removeAllViews();
      browserHost.addView(browserEmpty, new FrameLayout.LayoutParams(-1, -1));
    }
  }

  private boolean filePickerShown;

  private void showFilePicker() {
    if (filePickerShown) return;
    filePickerShown = true;
    startActivityForResult(
        new Intent(Intent.ACTION_OPEN_DOCUMENT)
            .setType("*/*")
            .addCategory(Intent.CATEGORY_OPENABLE),
        40);
  }

  JSONObject lastTask() {
    try {
      return new JSONObject(app.secure.get("lastTask", "{}"));
    } catch (Exception e) {
      return new JSONObject();
    }
  }

  String taskName(String method, String action) {
    return switch (method) {
      case "navigate" -> "Membuka halaman";
      case "observe" -> "Mengamati browser";
      case "watch" -> "Mengamati media";
      case "locate" -> "Mencari elemen halaman";
      case "media" -> "Mengendalikan media";
      case "action" ->
          switch (action) {
            case "click" -> "Menekan elemen halaman";
            case "fill" -> "Mengisi halaman";
            case "scroll" -> "Menjelajahi halaman";
            default -> "Interaksi browser";
          };
      default -> "Pekerjaan browser";
    };
  }

  String taskSignature() {
    JSONObject s = app.state.snapshot();
    return s.optString("taskId")
        + s.optString("control")
        + s.optJSONObject("progress")
        + app.secure.get("lastTask", "{}");
  }

  void toast(String s) {
    main.post(() -> Toast.makeText(this, s, Toast.LENGTH_LONG).show());
  }

  void connect() {
    if (app.secure.get("deviceToken", "").isEmpty()) {
      new AlertDialog.Builder(this)
          .setTitle("Hubungkan Hey")
          .setMessage(
              "Pairing belum tersimpan di aplikasi ini. Di ChatGPT, panggil Hey dan minta tautan"
                  + " pairing, lalu buka tautan pada ponsel ini.")
          .setPositiveButton("Mengerti", null)
          .show();
      return;
    }
    startBrowser();
  }

  void startBrowser() {
    HeyService service = HeyService.current;
    if (service != null && service.browser != null && !service.browser.usable())
      service.recoverBrowser();
    startRuntime(true);
  }

  private void startRuntime(boolean explicit) {
    HeyService service = HeyService.current;
    boolean paired = !app.secure.get("deviceToken", "").isEmpty();
    boolean paused = app.secure.get("ownerIntent", "ACTIVE").equals("PAUSED");
    if (!visible
        || !startPolicy.begin(
            SystemClock.elapsedRealtime(),
            explicit,
            paired,
            paused,
            service != null && service.running())) return;
    app.state.runtime("STARTING", "");
    app.state.connection("CONNECTING", "");
    try {
      Intent i = new Intent(this, HeyService.class);
      if (explicit) i.setAction("RESUME");
      startForegroundService(i);
    } catch (RuntimeException e) {
      app.state.runtime("ERROR", "SERVICE_START_FAILED:" + e.getClass().getSimpleName());
      app.state.connection("ERROR", "SERVICE_START_FAILED");
    }
    // Notification permission is optional and must not gate foreground service startup.
    if (explicit
        && Build.VERSION.SDK_INT >= 33
        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        && app.secure.get("notificationAsked", "").isEmpty()) {
      try {
        app.secure.put("notificationAsked", "true");
        requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 20);
      } catch (Exception ignored) {
      }
    }
  }

  void requestAudio() {
    if (!paired()) {
      connect();
      return;
    }
    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[] {Manifest.permission.RECORD_AUDIO}, 30);
      return;
    }
    startBrowser();
    startActivityForResult(
        getSystemService(MediaProjectionManager.class).createScreenCaptureIntent(), 31);
  }

  @Override
  public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(request, permissions, results);
    if (request == 30 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED)
      requestAudio();
  }

  @Override
  protected void onActivityResult(int request, int result, Intent data) {
    super.onActivityResult(request, result, data);
    if (request == 31 && result == RESULT_OK && data != null)
      startForegroundService(
          new Intent(this, HeyService.class)
              .putExtra("projectionData", data)
              .putExtra("projectionResult", result));
    if (request == 40) filePickerShown = false;
    if (request == 40 && HeyService.current != null && HeyService.current.browser != null)
      HeyService.current.browser.chooseFile(
          result == RESULT_OK && data != null ? data.getData() : null);
  }

  private void pairIntent(Intent intent) {
    Uri uri = intent.getData();
    if (uri == null
        || !"hey".equals(uri.getScheme())
        || !(BuildConfig.DEBUG ? "pair-preview" : "pair").equals(uri.getHost())) return;
    intent.setData(null);
    String gateway = uri.getQueryParameter("gateway"), code = uri.getQueryParameter("code");
    if (!app.secure.get("deviceToken", "").isEmpty()) {
      Toast.makeText(this, "Ponsel ini sudah dipasangkan", Toast.LENGTH_SHORT).show();
      return;
    }
    app.state.connection("REGISTERING", "");
    network.execute(
        () -> {
          try {
            Transport.gateway(gateway);
            app.secure.put("gateway", gateway);
            Transport t = new Transport(app.secure);
            JSONObject result = t.send("/api/enroll", new JSONObject().put("code", code), false);
            app.secure.put("deviceId", result.getString("deviceId"));
            app.secure.put("deviceToken", result.getString("deviceToken"));
            app.state.connection("PAIRED", "");
            app.refreshConfig();
            main.post(
                () -> {
                  startBrowser();
                  render();
                });
          } catch (Exception e) {
            app.state.connection("UNREGISTERED", "PAIRING_FAILED");
            main.post(
                () ->
                    Toast.makeText(
                            this,
                            "Pairing belum berhasil. Minta tautan baru dari Hey.",
                            Toast.LENGTH_LONG)
                        .show());
          }
        });
  }

  @Override
  public void changed() {
    main.post(this::update);
  }

  private void update() {
    if (ui == null) return;
    JSONObject s = app.state.snapshot();
    mode = UiState.mode(s, paired());
    if (homeStatus != null) {
      homeStatus.setText("•  " + UiState.status(mode));
      homeStatus.setTextColor(ui.accent(mode));
      heroTitle.setText(UiState.title(mode));
      heroDescription.setText(UiState.description(mode));
      String action = UiState.action(mode);
      homeAction.setText(action);
      homeAction.setVisibility(action.isEmpty() ? View.GONE : View.VISIBLE);
      activity.setText(
          !paired()
              ? "Belum ada aktivitas"
              : mode == UiState.Mode.PAUSED
                  ? "Menunggu dilanjutkan"
                  : !s.optString("taskId").isEmpty()
                      ? "Tugas sedang berlangsung"
                      : lastTask().has("status")
                          ? "DONE".equals(lastTask().optString("status"))
                              ? lastTask().optBoolean("verified")
                                  ? "Tugas terakhir terverifikasi"
                                  : "Eksekusi terakhir selesai · hasil belum terverifikasi"
                              : "Tugas terakhir terhenti"
                          : "Tidak ada tugas aktif");
    }
    if (pairValue != null) {
      connectionState.setText(UiState.status(mode));
      connectionState.setTextColor(ui.accent(mode));
      connectionDot.setImageTintList(android.content.res.ColorStateList.valueOf(ui.accent(mode)));
      pairValue.setText(paired() ? "Terpasang" : "Belum terpasang");
      serviceValue.setText(
          "PAUSED".equals(s.optString("ownerIntent"))
              ? "Dijeda"
              : "RUNNING".equals(s.optString("runtime"))
                  ? "Berjalan"
                  : "STARTING".equals(s.optString("runtime")) ? "Memulai" : "Terhenti");
      browserValue.setText(
          "READY".equals(s.optString("browser"))
              ? "Siap"
              : "LOADING".equals(s.optString("browser"))
                  ? "Memuat"
                  : java.util.Set.of("IDLE", "").contains(s.optString("browser"))
                      ? "Tidak aktif"
                      : "Belum siap");
      serviceAction.setText(
          !paired()
              ? "Panduan pairing"
              : "PAUSED".equals(s.optString("ownerIntent"))
                  ? "Lanjutkan Hey"
                  : "RUNNING".equals(s.optString("runtime")) ? "Jeda Hey" : "Coba jalankan Hey");
    }
    if (audioStatus != null) {
      String audio = s.optString("audio");
      audioStatus.setText(
          audio.equals("CAPTURING")
              ? "Sesi audio aktif."
              : audio.equals("OFF") ? "Audio belum diaktifkan." : "Audio: " + audio);
      audioButton.setText(audio.equals("CAPTURING") ? "Hentikan audio" : "Aktifkan audio");
    }
    if (page == 2 && !taskSignature().equals(taskSignature)) {
      render();
      return;
    }
    attachBrowser();
  }

  private final Runnable surfaceTick =
      new Runnable() {
        @Override
        public void run() {
          if (!visible) return;
          startRuntime(false);
          update();
          main.postDelayed(this, 1000);
        }
      };

  @Override
  protected void onResume() {
    super.onResume();
    visible = true;
    startRuntime(false);
    app.refreshConfig();
    main.removeCallbacks(surfaceTick);
    main.post(surfaceTick);
    update();
  }

  @Override
  protected void onPause() {
    visible = false;
    main.removeCallbacks(surfaceTick);
    HeyService s = HeyService.current;
    if (s != null && s.browser != null) s.browser.detach(this);
    super.onPause();
  }

  @Override
  public void onBackPressed() {
    HeyService s = HeyService.current;
    if (page == 1 && s != null && s.browser != null && s.browser.humanBack()) return;
    if (page != 0) {
      select(0);
      return;
    }
    super.onBackPressed();
  }

  @Override
  protected void onDestroy() {
    app.state.unlisten(this);
    main.removeCallbacksAndMessages(null);
    network.shutdownNow();
    super.onDestroy();
  }
}
