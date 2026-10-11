package id.ars.hey;

/** Direction lock is irreversible until next DOWN; CANCEL never commits navigation. */
final class ArcGesture {
  private float x, y;
  private boolean eligible, dragging, rejected;

  void begin(float x, float y, boolean eligible) {
    this.x = x;
    this.y = y;
    this.eligible = eligible;
    dragging = false;
    rejected = false;
  }

  boolean move(float x, float y, int pointers, float threshold) {
    if (!eligible || rejected) return false;
    if (pointers != 1) {
      rejected = true;
      dragging = false;
      return false;
    }
    float dx = Math.abs(x - this.x), dy = Math.abs(y - this.y);
    if (!dragging && dy > threshold) {
      rejected = true;
      return false;
    }
    if (!dragging && dx > threshold && dx > dy * 2.2f) dragging = true;
    return dragging;
  }

  float offset(float x) {
    return x - this.x;
  }

  boolean dragging() {
    return dragging;
  }

  void cancel() {
    rejected = true;
    dragging = false;
  }

  static int target(int selected, float dx, float step) {
    return Math.max(0, Math.min(3, Math.round(selected - dx / step)));
  }

  static float drop(float distance, float dp) {
    return Math.min(16, 7 * distance * distance) * dp;
  }
}
