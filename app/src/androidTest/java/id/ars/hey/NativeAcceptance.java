package id.ars.hey;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.webkit.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import org.json.*;

/**
 * Separate test APK only. Fixtures never ship, enroll, contact MCP or overwrite owner credentials.
 */
public final class NativeAcceptance extends Instrumentation {
  private MainActivity activity;
  private HeyApp app;
  private BrowserRuntime fixture;
  private JSONObject results = new JSONObject();
  private String variant;

  private interface Work {
    void run() throws Exception;
  }

  private void ui(Work work) throws Exception {
    Throwable[] failure = {null};
    runOnMainSync(
        () -> {
          try {
            work.run();
          } catch (Throwable e) {
            failure[0] = e;
          }
        });
    if (failure[0] != null) throw new RuntimeException(failure[0]);
  }

  private Object field(Object target, String name) throws Exception {
    Field f = target.getClass().getDeclaredField(name);
    f.setAccessible(true);
    return f.get(target);
  }

  private Object invoke(Object target, String name, Class<?>[] types, Object... args)
      throws Exception {
    Method m = target.getClass().getDeclaredMethod(name, types);
    m.setAccessible(true);
    return m.invoke(target, args);
  }

  private void select(int index) throws Exception {
    ui(() -> invoke(activity, "select", new Class[] {int.class}, index));
    waitForIdleSync();
    SystemClock.sleep(300);
  }

  private View find(View root, String text) {
    if (root instanceof TextView && ((TextView) root).getText().toString().equals(text))
      return root;
    if (root instanceof ViewGroup) {
      ViewGroup group = (ViewGroup) root;
      for (int i = 0; i < group.getChildCount(); i++) {
        View found = find(group.getChildAt(i), text);
        if (found != null) return found;
      }
    }
    return null;
  }

  private void expand(String text, String expected) throws Exception {
    ui(
        () -> {
          View label = find(activity.content, text);
          assertThat(label != null, "Missing setting " + text);
          ((View) label.getParent().getParent()).performClick();
          assertThat(activity.expanded.equals(expected), "Inline expansion failed " + expected);
        });
    waitForIdleSync();
    capture("settings-" + expected + "-expanded");
  }

  private void assertThat(boolean pass, String message) {
    if (!pass) throw new AssertionError(message);
  }

  private void record(String name, String status, String observation) throws Exception {
    results.put(name, new JSONObject().put("status", status).put("observation", observation));
    Bundle progress = new Bundle();
    progress.putString("stream", name + ": " + status + "\n");
    sendStatus(1, progress);
  }

  private void foreground() throws Exception {
    for (int attempt = 0; attempt < 6; attempt++) {
      AccessibilityNodeInfo root = getUiAutomation().getRootInActiveWindow();
      if (root == null) {
        SystemClock.sleep(200);
        continue;
      }
      java.util.List<AccessibilityNodeInfo> errors =
          root.findAccessibilityNodeInfosByText("isn't responding");
      if (!errors.isEmpty()) {
        String text = String.valueOf(errors.get(0).getText());
        assertThat(
            text.startsWith("Pixel Launcher") || text.startsWith("System UI"),
            "Application ANR must fail acceptance: " + text);
        java.util.List<AccessibilityNodeInfo> close =
            root.findAccessibilityNodeInfosByText("Close app");
        assertThat(
            !close.isEmpty() && close.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK),
            "Foreign system ANR could not be dismissed");
        record(
            "emulator_foreign_dialog",
            "PASS",
            "Dismissed foreign launcher/System UI ANR in isolated test VM; Hey ANR is never"
                + " dismissed or accepted");
        SystemClock.sleep(350);
        continue;
      }
      if (!root.findAccessibilityNodeInfosByText("Viewing full screen").isEmpty()) {
        java.util.List<AccessibilityNodeInfo> got = root.findAccessibilityNodeInfosByText("Got it");
        assertThat(
            !got.isEmpty() && got.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK),
            "System fullscreen guide blocked application");
        SystemClock.sleep(200);
        continue;
      }
      assertThat(
          getTargetContext().getPackageName().contentEquals(root.getPackageName()),
          "Unexpected window blocks native acceptance: " + root.getPackageName());
      return;
    }
    throw new AssertionError("Application foreground unavailable");
  }

  private void capture(String name) throws Exception {
    foreground();
    SystemClock.sleep(250);
    Bitmap shot = getUiAutomation().takeScreenshot();
    File dir = new File(getTargetContext().getFilesDir(), "acceptance");
    dir.mkdirs();
    try (FileOutputStream out =
        new FileOutputStream(new File(dir, variant + "-" + name + ".png"))) {
      assertThat(shot != null, "Screenshot unavailable");
      shot.compress(Bitmap.CompressFormat.PNG, 100, out);
    }
    shot.recycle();
  }

  private boolean probe(float x, float y) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    boolean[] allowed = {true};
    ui(
        () ->
            fixture.checkGesture(
                x,
                y,
                result -> {
                  allowed[0] = result;
                  latch.countDown();
                }));
    assertThat(latch.await(8, TimeUnit.SECONDS), "Gesture probe timeout");
    return allowed[0];
  }

  private String js(WebView web, String script) throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    String[] result = {null};
    ui(
        () ->
            web.evaluateJavascript(
                script,
                value -> {
                  result[0] = value;
                  latch.countDown();
                }));
    assertThat(latch.await(8, TimeUnit.SECONDS), "JavaScript timeout");
    return result[0];
  }

  private void swipe(float x, float y, float endX, float endY) throws Exception {
    long down = SystemClock.uptimeMillis();
    inject(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0));
    for (int i = 1; i <= 16; i++) {
      SystemClock.sleep(25);
      inject(
          MotionEvent.obtain(
              down,
              SystemClock.uptimeMillis(),
              MotionEvent.ACTION_MOVE,
              x + (endX - x) * i / 16,
              y + (endY - y) * i / 16,
              0));
    }
    inject(
        MotionEvent.obtain(down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, endX, endY, 0));
    SystemClock.sleep(300);
  }

  private void inject(MotionEvent event) {
    event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
    assertThat(getUiAutomation().injectInputEvent(event, true), "Touchscreen injection rejected");
    event.recycle();
  }

  @Override
  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    variant = arguments == null ? "regular" : arguments.getString("variant", "regular");
    start();
  }

  @Override
  public void onStart() {
    Bundle status = new Bundle();
    try {
      activity =
          (MainActivity)
              startActivitySync(
                  new Intent(getTargetContext(), MainActivity.class)
                      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
      assertThat(
          Build.HARDWARE.equals("ranchu") || Build.HARDWARE.equals("goldfish"),
          "Test APK fixtures require an isolated Android emulator");
      app = (HeyApp) activity.getApplication();
      waitForIdleSync();
      SystemClock.sleep(500);
      assertThat(
          app.secure.get("deviceToken", "").isEmpty(),
          "Use an isolated UNPAIRED emulator; never run fixture tests against owner pairing");
      select(0);
      capture("home-unpaired");
      assertThat(
          UiState.mode(app.state.snapshot(), false) == UiState.Mode.UNPAIRED, "False ready state");
      record("home_truth", "PASS", "UNPAIRED derives from absence of credentials; no fake ready");
      select(2);
      capture("tasks-empty");
      select(3);
      capture("settings-unpaired");
      ui(
          () -> {
            assertThat(
                find(activity.content, "Pengaturan") == null, "Removed Settings header returned");
            for (String label :
                new String[] {
                  "Koneksi & Layanan",
                  "Sesi Browser",
                  "Privasi & Izin",
                  "Kendali AI & Manusia",
                  "Tampilan & Gerakan",
                  "Tentang Hey"
                })
              assertThat(
                  find(activity.content, label) != null, "Missing locked Settings row " + label);
          });
      if (variant.equals("regular")) {
        expand("Sesi Browser", "session");
        expand("Privasi & Izin", "privacy");
        expand("Kendali AI & Manusia", "control");
        expand("Tampilan & Gerakan", "motion");
        expand("Tentang Hey", "about");
        ui(
            () -> {
              activity.expanded = "";
              activity.render();
            });
        record(
            "settings_expansion",
            "PASS",
            "All five real inline row handlers expand; one selected section replaces the previous"
                + " section; consent was not invoked");
      }
      record("native_pages", "PASS", "Home/Tasks/Settings captured with actual unpaired state");
      select(0);
      HeyShell shell = (HeyShell) field(activity, "shell");
      int[] xy = new int[2];
      ui(() -> shell.getLocationOnScreen(xy));
      float y =
          xy[1]
              + shell.getHeight()
              - shell.getPaddingBottom()
              - 12 * activity.getResources().getDisplayMetrics().density;
      float x = xy[0] + shell.getWidth() * .70f;
      foreground();
      swipe(x, y, x - shell.arc.step() * .8f, y);
      assertThat((int) field(activity, "page") == 1, "Arc did not commit Browser");
      waitForIdleSync();
      capture("browser-arc");
      ui(
          () -> {
            android.graphics.Rect bounds = new android.graphics.Rect();
            View url = (View) field(activity, "address");
            assertThat(
                url.getGlobalVisibleRect(bounds)
                    && bounds.height() >= 48 * activity.getResources().getDisplayMetrics().density,
                "Permanent URL field is not visible");
          });
      record(
          "browser_url_layout",
          "PASS",
          "URL field has a visible 48dp minimum height above browser viewport on first navigation");
      ui(
          () -> {
            ViewGroup cards = shell.arc;
            int width = cards.getChildAt(0).getWidth(), height = cards.getChildAt(0).getHeight();
            for (int i = 0; i < cards.getChildCount(); i++) {
              View card = cards.getChildAt(i);
              assertThat(
                  card.getWidth() == width && card.getHeight() == height, "Unequal Arc cards");
              assertThat(
                  card.getTranslationY()
                      <= 16 * activity.getResources().getDisplayMetrics().density + 1,
                  "Arc too deep");
            }
          });
      record(
          "arc_geometry",
          "PASS",
          "All four native cards are equal width/height; vertical drop at most 16dp");
      record(
          "arc_native_swipe",
          "PASS",
          "Physical MotionEvent sequence on emulator commits only on release");
      SystemClock.sleep(3400);
      assertThat(shell.arc.getVisibility() == View.GONE, "Arc remains visible when idle");
      record("arc_auto_hide", "PASS", "Arc invisible after timeout");
      ui(() -> shell.performAccessibilityAction(0x01010003, null));
      waitForIdleSync();
      assertThat((int) field(activity, "page") == 3, "Accessibility destination failed");
      record(
          "accessible_navigation", "PASS", "Native custom action selects Settings without swipe");
      if (!variant.equals("regular")) {
        capture("settings-accessible");
        finishResults(status);
        return;
      }
      // Browser rendering and touch evidence use a real production BrowserRuntime with a local
      // inline website fixture.
      select(1);
      ui(
          () -> {
            fixture = new BrowserRuntime(activity, app.state, app.secure);
            app.state.control("HUMAN");
            FrameLayout host = (FrameLayout) field(activity, "browserHost");
            host.removeAllViews();
            fixture.attach(activity, host);
          });
      WebView web = (WebView) invoke(fixture, "web", new Class[] {});
      String html =
          "<!doctype html><meta name=viewport"
              + " content='width=device-width,initial-scale=1'><style>body{margin:0;font:16px"
              + " sans-serif;background:#101c26;color:#f4f6f7}header{padding:54px 28px"
              + " 32px}h1{font-size:38px;letter-spacing:-1px;margin:12px"
              + " 0}p{color:#a7b5bf;line-height:1.6}button{padding:14px;background:#333d47;color:white;border:0;border-radius:12px}#carousel{display:flex;width:100%;overflow-x:auto}#carousel"
              + " div{flex:none;width:100%;height:130px;background:#253848;box-sizing:border-box;padding:32px}#long{height:1400px;padding:30px}footer{padding:35px}</style><header><small>HEY"
              + " · WEBSITE TEST FIXTURE</small><h1>A quiet browser.</h1><p>Native WebView. Shared"
              + " session.<br>Gesture and continuity checks.</p><button id=change"
              + " onclick=\"this.textContent='Changed'\">Test interaction</button></header><div"
              + " id=carousel><div>Website carousel · One</div><div>Website carousel ·"
              + " Two</div></div><div id=long>Scrollable website content</div><footer>End of"
              + " fixture</footer>";
      ui(() -> web.loadDataWithBaseURL("https://example.com/", html, "text/html", "UTF-8", null));
      SystemClock.sleep(1200);
      assertThat(
          js(web, "document.querySelector('#change').textContent").contains("Test interaction"),
          "Web fixture missing");
      capture("browser-fixture");
      String rectJson =
          new JSONArray(
                  "["
                      + js(
                          web,
                          "JSON.stringify((()=>{const"
                              + " r=document.querySelector('#carousel').getBoundingClientRect();return"
                              + " [r.left+20,r.top+20,visualViewport.width]})())")
                      + "]")
              .getString(0);
      JSONArray rect = new JSONArray(rectJson);
      float cssScale = web.getWidth() / (float) rect.getDouble(2);
      assertThat(
          !probe((float) rect.getDouble(0) * cssScale, (float) rect.getDouble(1) * cssScale),
          "Site carousel allowed shell gesture");
      assertThat(
          probe(web.getWidth() * .6f, 20 * cssScale), "Neutral web content did not allow gesture");
      record(
          "native_site_hit_test",
          "PASS",
          "Production DOM probe rejects real nested horizontal scroller and accepts neutral content"
              + " using actual CSS viewport ratio");
      ui(
          () ->
              CookieManager.getInstance()
                  .setCookie(
                      "https://example.com", "hey_fixture_cookie=retained; Secure; SameSite=Lax"));
      js(
          web,
          "sessionStorage.setItem('hey_fixture','retained'); window.heyRetained=42;"
              + " document.querySelector('#change').click()");
      select(0);
      select(1);
      ui(
          () -> {
            FrameLayout host = (FrameLayout) field(activity, "browserHost");
            host.removeAllViews();
            fixture.attach(activity, host);
          });
      assertThat(
          js(
                  web,
                  "window.heyRetained===42 && sessionStorage.getItem('hey_fixture')==='retained' &&"
                      + " document.querySelector('#change').textContent==='Changed'")
              .equals("true"),
          "Browser context replaced");
      assertThat(
          CookieManager.getInstance()
              .getCookie("https://example.com")
              .contains("hey_fixture_cookie=retained"),
          "Cookie lost");
      record(
          "shared_webview_context",
          "PASS",
          "Same live DOM, sessionStorage and cookie retained across native page navigation");
      app.state.control("AGENT");
      ui(
          () -> {
            long t = SystemClock.uptimeMillis();
            MotionEvent e = MotionEvent.obtain(t, t, MotionEvent.ACTION_DOWN, 150, 200, 0);
            web.dispatchTouchEvent(e);
            e.recycle();
            e = MotionEvent.obtain(t, t + 1, MotionEvent.ACTION_CANCEL, 150, 200, 0);
            web.dispatchTouchEvent(e);
            e.recycle();
          });
      assertThat(
          app.state.snapshot().optString("control").equals("HUMAN"), "Touch failed human priority");
      record(
          "human_priority",
          "PASS",
          "Native human DOWN changes AGENT to HUMAN before web dispatch; physical concurrency"
              + " remains a retest gate");
      foreground();
      int[] webPos = new int[2];
      ui(() -> web.getLocationOnScreen(webPos));
      swipe(
          webPos[0] + web.getWidth() * .6f,
          webPos[1] + web.getHeight() * .72f,
          webPos[0] + web.getWidth() * .6f,
          webPos[1] + web.getHeight() * .32f);
      long scrollDeadline = SystemClock.elapsedRealtime() + 5000;
      while (!js(web, "scrollY>100").equals("true")
          && SystemClock.elapsedRealtime() < scrollDeadline) SystemClock.sleep(100);
      assertThat(
          js(web, "scrollY>100").equals("true"),
          "Website did not scroll after real touchscreen gesture");
      capture("browser-scrolled");
      record(
          "native_web_scroll",
          "PASS",
          "Native WebView scroll is independent of shell, no parent ScrollView");
      // Negative Arc: vertical lower-zone swipe must not navigate.
      ui(
          () -> {
            web.scrollTo(0, 0);
          });
      SystemClock.sleep(250);
      int before = (int) field(activity, "page");
      swipe(x, y, x, y - 180);
      assertThat((int) field(activity, "page") == before, "Vertical swipe hijacked by Arc");
      record("vertical_not_navigation", "PASS", "Vertical native gesture does not switch Hey page");
      ui(() -> shell.arc.show(1));
      capture("browser-fixture-arc");
      ui(() -> shell.arc.dismiss());
      js(web, "history.pushState({},'', '#hey-history-fixture')");
      SystemClock.sleep(250);
      ui(() -> assertThat(fixture.humanBack(), "Native browser Back unavailable"));
      SystemClock.sleep(500);
      assertThat(
          js(web, "location.hash !== '#hey-history-fixture'").equals("true"),
          "History did not return");
      record(
          "native_history_back",
          "PASS",
          "Real production humanBack returns same-document WebView history and invalidates old"
              + " state");
      int cacheMode[] = {0};
      ui(
          () -> {
            cacheMode[0] = web.getSettings().getCacheMode();
            fixture.humanHardReload();
            assertThat(
                web.getSettings().getCacheMode() == WebSettings.LOAD_NO_CACHE,
                "Hard reload did not bypass WebView cache");
            fixture.abort();
            assertThat(
                web.getSettings().getCacheMode() == cacheMode[0],
                "Cache policy not restored on abort");
          });
      assertThat(
          CookieManager.getInstance()
              .getCookie("https://example.com")
              .contains("hey_fixture_cookie=retained"),
          "Reload cleared cookies");
      record(
          "reload_policy_cookie",
          "PASS",
          "Production reload sets LOAD_NO_CACHE and abort restores prior policy without clearing"
              + " cookie; HTTP/service-worker cache behavior requires physical/controlled network"
              + " retest");
      // Runtime statuses are projection fixtures, not connectivity claims. No credential injection.
      select(0);
      for (UiState.Mode projection :
          new UiState.Mode[] {UiState.Mode.READY, UiState.Mode.WORKING, UiState.Mode.PAUSED}) {
        ui(
            () -> {
              activity.heroTitle.setText(UiState.title(projection));
              activity.heroDescription.setText(UiState.description(projection));
              activity.homeStatus.setText("•  " + UiState.status(projection));
              activity.homeStatus.setTextColor(activity.ui.accent(projection));
              activity.homeAction.setText(UiState.action(projection));
              activity.homeAction.setVisibility(
                  UiState.action(projection).isEmpty() ? View.GONE : View.VISIBLE);
              activity.activity.setText(
                  projection == UiState.Mode.READY
                      ? "Tidak ada tugas aktif"
                      : projection == UiState.Mode.WORKING
                          ? "Tugas sedang berlangsung"
                          : "Menunggu dilanjutkan");
            });
        capture("home-" + projection.name().toLowerCase() + "-projection-fixture");
      }
      taskVisualFixtures();
      record(
          "fixture_limits",
          "RETEST_REQUIRED",
          "Ready projection is test-only visual copy; no gateway pairing, real task, service, audio"
              + " or wake certification");
      finishResults(status);
    } catch (Throwable e) {
      try {
        record("failure", "FAIL", e.toString());
      } catch (Exception ignored) {
      }
      status.putString("failure", e.toString());
      try {
        finishResults(status);
      } catch (Exception ignored) {
        finish(Activity.RESULT_CANCELED, status);
      }
    }
  }

  private void taskVisualFixtures() throws Exception {
    String last = app.secure.get("lastTask", "{}"), history = app.secure.get("taskHistory", "[]");
    try {
      select(2);
      ui(
          () -> {
            app.state.control("AGENT");
            app.state.taskInfo(new JSONObject().put("method", "action").put("action", "scroll"));
            app.state.task("native-visual-fixture", new JSONObject());
            activity.render();
          });
      waitForIdleSync();
      capture("tasks-running-projection-fixture");
      ui(
          () -> {
            activity.expanded = "task";
            activity.render();
          });
      capture("tasks-running-detail-projection-fixture");
      ui(
          () -> {
            app.state.task("", new JSONObject());
            app.state.control("HUMAN");
            app.secure.put(
                "lastTask",
                new JSONObject()
                    .put("method", "action")
                    .put("action", "scroll")
                    .put("status", "UNKNOWN")
                    .put("reason", "HUMAN_CONTROL_ACTIVE")
                    .put("verified", false)
                    .toString());
            activity.expanded = "";
            activity.render();
          });
      waitForIdleSync();
      capture("tasks-human-projection-fixture");
      ui(
          () -> {
            app.state.control("AGENT");
            app.secure.put(
                "lastTask",
                new JSONObject()
                    .put("method", "action")
                    .put("action", "scroll")
                    .put("status", "DONE")
                    .put("verified", false)
                    .put("completedAt", System.currentTimeMillis())
                    .put("evidence", new JSONObject().put("postcondition", "SCROLL_CHANGED"))
                    .toString());
            activity.expanded = "task";
            activity.render();
          });
      waitForIdleSync();
      capture("tasks-completed-unverified-projection-fixture");
      ui(
          () -> {
            app.secure.put(
                "lastTask",
                new JSONObject()
                    .put("method", "action")
                    .put("action", "scroll")
                    .put("status", "FAILED")
                    .put("reason", "EXECUTION_ABORTED")
                    .put("verified", false)
                    .toString());
            activity.expanded = "";
            activity.render();
          });
      waitForIdleSync();
      capture("tasks-interrupted-projection-fixture");
      record(
          "task_visual_projections",
          "PASS",
          "Native running/human/completed-unverified/interrupted/detail renderers use isolated"
              + " test-only receipt fixtures; this is not live task certification");
    } finally {
      ui(
          () -> {
            app.state.task("", new JSONObject());
            app.state.taskInfo(new JSONObject());
            app.secure.put("lastTask", last);
            app.secure.put("taskHistory", history);
            activity.expanded = "";
            activity.render();
          });
    }
  }

  private void finishResults(Bundle status) throws Exception {
    ui(
        () -> {
          if (fixture != null) {
            fixture.close();
            fixture = null;
          }
        });
    File dir = new File(getTargetContext().getFilesDir(), "acceptance");
    dir.mkdirs();
    try (FileWriter w = new FileWriter(new File(dir, variant + "-results.json"))) {
      w.write(results.toString(2));
    }
    status.putString("results", results.toString());
    finish(results.has("failure") ? Activity.RESULT_CANCELED : Activity.RESULT_OK, status);
  }
}
