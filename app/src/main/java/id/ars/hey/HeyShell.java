package id.ars.hey;

import android.content.Context;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;

/**
 * The shell direction-locks only deliberate lower-zone swipes; normal website gestures pass
 * through.
 */
final class HeyShell extends FrameLayout {
  interface Probe {
    void check(float x, float y, java.util.function.Consumer<Boolean> result);
  }

  final ArcNavigation arc;
  private final HeyUi ui;
  private final ArcGesture gesture = new ArcGesture();
  private int page, sequence;
  private boolean siteAllows, ime;
  private Probe probe;
  private final Runnable menu;

  HeyShell(Context c, ArcNavigation.Destination destination, Runnable menu) {
    super(c);
    ui = new HeyUi(c);
    this.menu = menu;
    arc = new ArcNavigation(c, destination);
    setBackgroundColor(ui.canvas);
    setAccessibilityDelegate(
        new AccessibilityDelegate() {
          @Override
          public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo n) {
            super.onInitializeAccessibilityNodeInfo(host, n);
            for (int i = 0; i < 4; i++)
              n.addAction(
                  new AccessibilityNodeInfo.AccessibilityAction(
                      0x01010000 + i,
                      "Buka " + new String[] {"Home", "Browser", "Tasks", "Settings"}[i]));
          }

          @Override
          public boolean performAccessibilityAction(View host, int action, Bundle args) {
            if (action >= 0x01010000 && action < 0x01010004) {
              destination.select(action - 0x01010000);
              return true;
            }
            return super.performAccessibilityAction(host, action, args);
          }
        });
  }

  void overlay() {
    FrameLayout.LayoutParams p =
        new FrameLayout.LayoutParams(
            -1,
            ui.dp(Math.max(110, 110 * getResources().getConfiguration().fontScale)),
            Gravity.BOTTOM);
    addView(arc, p);
  }

  void page(int index, Probe probe) {
    page = index;
    this.probe = probe;
  }

  void ime(boolean value) {
    ime = value;
    if (value) arc.dismiss();
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent e) {
    int a = e.getActionMasked();
    if (a == MotionEvent.ACTION_DOWN) {
      int ticket = ++sequence;
      float x = e.getX(), y = e.getY();
      // Keep a small ergonomic buffer above Android's home/quick-switch recognition area.
      float safeBottom = getHeight() - getPaddingBottom() - ui.dp(24);
      boolean eligible =
          !ime
              && x > ui.dp(28)
              && x < getWidth() - ui.dp(28)
              && y < safeBottom
              && y > safeBottom - ui.dp(arc.getVisibility() == VISIBLE ? 104 : 28);
      gesture.begin(x, y, eligible);
      siteAllows = probe == null;
      if (eligible && probe != null)
        probe.check(
            x,
            y,
            allowed -> {
              if (sequence == ticket) siteAllows = allowed;
            });
    } else if (a == MotionEvent.ACTION_MOVE) {
      if (siteAllows && gesture.move(e.getX(), e.getY(), e.getPointerCount(), ui.dp(24))) {
        arc.show(page);
        arc.drag(gesture.offset(e.getX()));
        return true;
      }
    } else if (a == MotionEvent.ACTION_POINTER_DOWN || a == MotionEvent.ACTION_CANCEL) {
      gesture.cancel();
      sequence++;
    }
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    if (!gesture.dragging()) return super.onTouchEvent(e);
    switch (e.getActionMasked()) {
      case MotionEvent.ACTION_MOVE:
        arc.drag(gesture.offset(e.getX()));
        break;
      case MotionEvent.ACTION_UP:
        arc.release(gesture.offset(e.getX()), false);
        gesture.cancel();
        break;
      case MotionEvent.ACTION_CANCEL:
        arc.release(0, true);
        gesture.cancel();
        break;
    }
    return true;
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (e.getAction() == KeyEvent.ACTION_UP && e.getKeyCode() == KeyEvent.KEYCODE_MENU) {
      menu.run();
      return true;
    }
    return super.dispatchKeyEvent(e);
  }
}
