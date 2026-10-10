package id.ars.hey;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;

/** Shared visual roles. Layout values are dp/sp; content grows with system font scale. */
final class HeyUi {
  final Context c;
  final int canvas,
      primary,
      home,
      task,
      group,
      nested,
      well,
      strong,
      divider,
      text,
      secondary,
      muted,
      healthy,
      working,
      attention,
      problem;

  HeyUi(Context c) {
    this.c = c;
    canvas = color(R.color.hey_canvas);
    primary = color(R.color.hey_primary);
    home = color(R.color.hey_home);
    task = color(R.color.hey_task);
    group = color(R.color.hey_group);
    nested = color(R.color.hey_nested);
    well = color(R.color.hey_icon);
    strong = color(R.color.hey_icon_strong);
    divider = color(R.color.hey_divider);
    text = color(R.color.hey_text);
    secondary = color(R.color.hey_secondary);
    muted = color(R.color.hey_muted);
    healthy = color(R.color.hey_healthy);
    working = color(R.color.hey_working);
    attention = color(R.color.hey_attention);
    problem = color(R.color.hey_problem);
  }

  int color(int id) {
    return c.getColor(id);
  }

  int dp(float n) {
    return Math.round(n * c.getResources().getDisplayMetrics().density);
  }

  static boolean motion() {
    return ValueAnimator.areAnimatorsEnabled();
  }

  GradientDrawable surface(int color, float radius, int stroke) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    if (stroke != 0) d.setStroke(dp(1), stroke);
    return d;
  }

  TextView text(String s, float size, int color, boolean bold) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextSize(size);
    t.setTextColor(color);
    t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
    t.setLineSpacing(dp(2), 1);
    t.setFontFeatureSettings("kern");
    return t;
  }

  LinearLayout column() {
    LinearLayout l = new LinearLayout(c);
    l.setOrientation(LinearLayout.VERTICAL);
    return l;
  }

  void gap(LinearLayout parent, int height) {
    parent.addView(new View(c), new LinearLayout.LayoutParams(1, dp(height)));
  }

  LinearLayout card(int color) {
    LinearLayout l = column();
    l.setPadding(dp(22), dp(22), dp(22), dp(22));
    l.setBackground(surface(color, 26, 0));
    l.setElevation(dp(2));
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.bottomMargin = dp(20);
    l.setLayoutParams(p);
    return l;
  }

  void divider(LinearLayout l) {
    View v = new View(c);
    v.setBackgroundColor(divider);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(1));
    p.topMargin = dp(18);
    p.bottomMargin = dp(14);
    l.addView(v, p);
  }

  ImageView icon(int resource, int color, int size) {
    ImageView i = new ImageView(c);
    i.setImageResource(resource);
    i.setImageTintList(ColorStateList.valueOf(color));
    i.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    i.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
    return i;
  }

  FrameLayout well(int resource, int size) {
    FrameLayout f = new FrameLayout(c);
    f.setBackground(surface(well, 14, 0));
    ImageView i = icon(resource, working, 23);
    FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER);
    f.addView(i, p);
    f.setLayoutParams(new LinearLayout.LayoutParams(dp(size), dp(size)));
    return f;
  }

  Button button(String label, Runnable action) {
    Button b = new Button(c);
    b.setAllCaps(false);
    b.setText(label);
    b.setTextSize(14);
    b.setTextColor(text);
    b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setPadding(dp(16), dp(10), dp(16), dp(10));
    b.setBackground(
        new RippleDrawable(
            ColorStateList.valueOf(0x22ffffff), surface(0x08ffffff, 15, divider), null));
    b.setOnClickListener(
        v -> {
          v.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
          action.run();
        });
    return b;
  }

  int accent(UiState.Mode m) {
    return switch (m) {
      case READY -> healthy;
      case PAUSED, HUMAN -> attention;
      case OFFLINE, STOPPED, BROWSER_ERROR -> problem;
      case UNPAIRED -> muted;
      default -> working;
    };
  }
}
