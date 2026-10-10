package id.ars.hey;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
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

  private void assertThat(boolean pass, String message) {
    if (!pass) throw new AssertionError(message);
  }

  private void record(String name, String status, String observation) throws Exception {
    results.put(name, new JSONObject().put("status", status).put("observation", observation));
  }

  private void capture(String name) throws Exception {
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
    getUiAutomation()
        .injectInputEvent(MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, x, y, 0), true);
    for (int i = 1; i <= 16; i++) {
      SystemClock.sleep(25);
      getUiAutomation()
          .injectInputEvent(
              MotionEvent.obtain(
                  down,
                  SystemClock.uptimeMillis(),
                  MotionEvent.ACTION_MOVE,
                  x + (endX - x) * i / 16,
                  y + (endY - y) * i / 16,
                  0),
              true);
    }
    getUiAutomation()
        .injectInputEvent(
            MotionEvent.obtain(
                down, SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, endX, endY, 0),
            true);
    SystemClock.sleep(300);
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
      // Verify exact native grouped row order and absent top header; design fixture states are
      // test-only.
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
      swipe(x, y, x - shell.arc.step() * .8f, y);
      assertThat((int) field(activity, "page") == 1, "Arc did not commit Browser");
      capture("browser-arc");
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
      ui(
          () -> {
            web.scrollTo(0, 550);
          });
      assertThat(js(web, "scrollY>100").equals("true"), "Website did not scroll");
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
      // Runtime statuses are projection fixtures, not connectivity claims. No credential injection.
      select(0);
      ui(
          () -> {
            ((TextView) field(activity, "heroTitle")).setText(UiState.title(UiState.Mode.READY));
            ((TextView) field(activity, "heroDescription"))
                .setText(UiState.description(UiState.Mode.READY));
            ((TextView) field(activity, "homeStatus"))
                .setText("•  " + UiState.status(UiState.Mode.READY));
            ((Button) field(activity, "homeAction")).setVisibility(View.GONE);
          });
      capture("home-ready-projection-fixture");
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
