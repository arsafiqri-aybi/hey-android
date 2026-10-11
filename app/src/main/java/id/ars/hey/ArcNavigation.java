package id.ars.hey;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
import android.widget.*;

/** Transient equal-size cards over the viewport. Alpha glass fallback avoids blurring web text. */
final class ArcNavigation extends FrameLayout {
  interface Destination {
    void select(int index);
  }

  private final HeyUi ui;
  private final Handler main = new Handler();
  private final LinearLayout[] cards = new LinearLayout[4];
  private final String[] names = {"Home", "Browser", "Tasks", "Settings"};
  private final Destination destination;
  private final Runnable hide = this::dismiss;
  private int selected;
  private float position;
  private ValueAnimator snap;

  ArcNavigation(Context c, Destination destination) {
    super(c);
    ui = new HeyUi(c);
    this.destination = destination;
    setClipChildren(false);
    setClipToPadding(false);
    setVisibility(GONE);
    int[] icons = {
      R.drawable.ic_home, R.drawable.ic_globe, R.drawable.ic_tasks, R.drawable.ic_settings
    };
    for (int i = 0; i < 4; i++) {
      final int index = i;
      LinearLayout card = new LinearLayout(c);
      card.setGravity(Gravity.CENTER);
      card.setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10));
      card.setElevation(ui.dp(8));
      card.addView(ui.icon(icons[i], ui.text, 19));
      TextView label = ui.text(names[i], 13, ui.text, true);
      LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
      lp.leftMargin = ui.dp(10);
      card.addView(label, lp);
      card.setContentDescription(names[i]);
      card.setFocusable(true);
      card.setClickable(true);
      card.setOnClickListener(v -> commit(index));
      cards[i] = card;
      addView(card);
    }
  }

  float step() {
    return cardWidth() + ui.dp(12);
  }

  private int cardWidth() {
    return ui.dp(Math.min(220, Math.max(142, 142 * getResources().getConfiguration().fontScale)));
  }

  private int cardHeight() {
    return ui.dp(Math.max(54, 54 * getResources().getConfiguration().fontScale));
  }

  @Override
  protected void onMeasure(int w, int h) {
    super.onMeasure(w, h);
    for (View card : cards)
      card.measure(
          MeasureSpec.makeMeasureSpec(cardWidth(), MeasureSpec.EXACTLY),
          MeasureSpec.makeMeasureSpec(cardHeight(), MeasureSpec.EXACTLY));
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    for (View card : cards)
      card.layout(
          (getWidth() - cardWidth()) / 2,
          getHeight() - cardHeight() - ui.dp(24),
          (getWidth() + cardWidth()) / 2,
          getHeight() - ui.dp(24));
    layoutCards();
  }

  private void layoutCards() {
    for (int i = 0; i < 4; i++) {
      float d = i - position;
      View v = cards[i];
      v.setTranslationX(d * step());
      v.setTranslationY(ArcGesture.drop(d, getResources().getDisplayMetrics().density));
      v.setAlpha(Math.max(.22f, 1 - Math.abs(d) * .31f));
      boolean center = Math.abs(d) < .48;
      v.setBackground(
          ui.surface(center ? 0xe650606b : 0xa633434e, 18, center ? 0xbbe8f2f6 : 0x667d919c));
      v.setSelected(i == selected);
    }
  }

  void show(int current) {
    main.removeCallbacks(hide);
    if (snap != null) snap.cancel();
    selected = current;
    position = current;
    setVisibility(VISIBLE);
    animate().cancel();
    setAlpha(1);
    setTranslationY(0);
    layoutCards();
  }

  void drag(float dx) {
    position = Math.max(0, Math.min(3, selected - dx / step()));
    layoutCards();
  }

  void release(float dx, boolean cancelled) {
    if (cancelled) {
      commit(selected);
      return;
    }
    commit(ArcGesture.target(selected, dx, step()));
  }

  void commit(int index) {
    main.removeCallbacks(hide);
    float from = position;
    selected = index;
    destination.select(index);
    if (HeyUi.motion()) {
      snap = ValueAnimator.ofFloat(from, index);
      snap.setDuration(240);
      snap.addUpdateListener(
          a -> {
            position = (float) a.getAnimatedValue();
            layoutCards();
          });
      snap.start();
    } else {
      position = index;
      layoutCards();
    }
    schedule();
  }

  void schedule() {
    AccessibilityManager a = getContext().getSystemService(AccessibilityManager.class);
    main.removeCallbacks(hide);
    main.postDelayed(
        hide, a.getRecommendedTimeoutMillis(2900, AccessibilityManager.FLAG_CONTENT_CONTROLS));
  }

  void dismiss() {
    if (HeyUi.motion())
      animate()
          .alpha(0)
          .translationY(ui.dp(18))
          .setDuration(260)
          .withEndAction(() -> setVisibility(GONE))
          .start();
    else setVisibility(GONE);
  }

  @Override
  protected void onDetachedFromWindow() {
    main.removeCallbacksAndMessages(null);
    if (snap != null) snap.cancel();
    super.onDetachedFromWindow();
  }
}
