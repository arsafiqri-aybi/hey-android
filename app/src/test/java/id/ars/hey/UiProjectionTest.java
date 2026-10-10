package id.ars.hey;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.Test;

public final class UiProjectionTest {
  private JSONObject state(String runtime, String connection, String browser) throws Exception {
    return new JSONObject()
        .put("ownerIntent", "ACTIVE")
        .put("runtime", runtime)
        .put("connection", connection)
        .put("browser", browser)
        .put("control", "AGENT");
  }

  @Test
  public void pairingIsNotReadiness() throws Exception {
    assertEquals(UiState.Mode.UNPAIRED, UiState.mode(state("RUNNING", "ONLINE", "READY"), false));
    assertEquals(UiState.Mode.STOPPED, UiState.mode(state("STOPPED", "PAIRED", "READY"), true));
    assertEquals(UiState.Mode.OFFLINE, UiState.mode(state("RUNNING", "UNKNOWN", "READY"), true));
    assertEquals(
        UiState.Mode.BROWSER_ERROR,
        UiState.mode(state("RUNNING", "ONLINE", "RENDERER_LOST"), true));
  }

  @Test
  public void pausedWinsOverStaleRuntime() throws Exception {
    JSONObject s = state("RUNNING", "ONLINE", "READY").put("ownerIntent", "PAUSED");
    assertEquals(UiState.Mode.PAUSED, UiState.mode(s, true));
  }

  @Test
  public void humanAndActiveTaskAreExplicit() throws Exception {
    JSONObject s = state("RUNNING", "ONLINE", "READY");
    assertEquals(UiState.Mode.READY, UiState.mode(s, true));
    s.put("taskId", "fixture-task");
    assertEquals(UiState.Mode.WORKING, UiState.mode(s, true));
    s.put("control", "HUMAN");
    assertEquals(UiState.Mode.HUMAN, UiState.mode(s, true));
  }

  @Test
  public void secureAddressResolution() throws Exception {
    assertEquals("https://example.com/path", AddressInput.resolve(" example.com/path "));
    assertEquals(
        "https://www.google.com/search?q=tugas+kuliah", AddressInput.resolve("tugas kuliah"));
    for (String bad :
        new String[] {
          "http://example.com",
          "javascript:alert(1)",
          "https://localhost",
          "https://user:pass@example.com"
        }) {
      try {
        AddressInput.resolve(bad);
        fail(bad);
      } catch (SecurityException expected) {
      }
    }
  }

  @Test
  public void arcRejectsVerticalAndMultitouch() {
    ArcGesture g = new ArcGesture();
    g.begin(100, 500, true);
    assertFalse(g.move(102, 530, 1, 24));
    assertFalse(g.move(250, 530, 1, 24));
    g.begin(100, 500, true);
    assertFalse(g.move(180, 500, 2, 24));
    assertFalse(g.move(250, 500, 1, 24));
  }

  @Test
  public void arcCommitsNearestAndClamps() {
    ArcGesture g = new ArcGesture();
    g.begin(100, 500, true);
    assertTrue(g.move(180, 504, 1, 24));
    assertEquals(0, ArcGesture.target(0, 400, 154));
    assertEquals(3, ArcGesture.target(1, -700, 154));
    assertEquals(2, ArcGesture.target(1, -100, 154));
    assertEquals(1, ArcGesture.target(1, -50, 154));
    g.cancel();
    assertFalse(g.dragging());
    assertTrue(ArcGesture.drop(1, 1) < 17);
    assertEquals(ArcGesture.drop(-1, 1), ArcGesture.drop(1, 1), 0);
  }
}
