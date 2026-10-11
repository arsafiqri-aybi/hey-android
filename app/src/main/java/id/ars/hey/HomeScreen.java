package id.ars.hey;

import android.content.*;
import android.view.*;
import android.widget.*;
import org.json.*;

/** Native composition for the approved Home page. */
final class HomeScreen {
  private final MainActivity a;

  HomeScreen(MainActivity a) {
    this.a = a;
  }

  void build() {
    LinearLayout header = a.row();
    ImageView mark = new ImageView(a);
    mark.setImageResource(R.drawable.hey_wordmark);
    mark.setContentDescription("Hey.");
    mark.setScaleType(ImageView.ScaleType.FIT_CENTER);
    header.addView(mark, new LinearLayout.LayoutParams(a.dp(56), a.dp(30)));
    View spacer = new View(a);
    header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
    a.homeStatus = a.ui.text("", 11, a.ui.secondary, true);
    a.homeStatus.setPadding(a.dp(12), a.dp(8), a.dp(12), a.dp(8));
    a.homeStatus.setBackground(a.ui.surface(a.ui.group, 30, 0));
    header.addView(a.homeStatus);
    header.setMinimumHeight(a.dp(48));
    a.content.addView(header);
    a.ui.gap(a.content, 28);
    LinearLayout card = a.ui.card(a.ui.home);
    LinearLayout identity = a.row();
    identity.addView(a.ui.icon(R.drawable.ic_globe, a.ui.secondary, 21));
    TextView label = a.ui.text("Browser Hey", 14, a.ui.text, true);
    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
    lp.leftMargin = a.dp(10);
    identity.addView(label, lp);
    card.addView(identity);
    LinearLayout center = a.ui.column();
    center.setGravity(Gravity.CENTER);
    center.setPadding(a.dp(4), a.dp(46), a.dp(4), a.dp(46));
    center.setMinimumHeight(a.dp(264));
    a.heroTitle = a.ui.text("", 38, a.ui.text, true);
    a.heroTitle.setGravity(Gravity.CENTER);
    a.heroTitle.setLetterSpacing(-.045f);
    center.addView(a.heroTitle);
    a.ui.gap(center, 14);
    a.heroDescription = a.ui.text("", 14, a.ui.secondary, false);
    a.heroDescription.setGravity(Gravity.CENTER);
    a.heroDescription.setMaxWidth(a.dp(300));
    center.addView(a.heroDescription);
    card.addView(center);
    a.homeAction = a.ui.button("", a::homeAction);
    card.addView(a.homeAction, new LinearLayout.LayoutParams(-1, -2));
    card.setOnLongClickListener(
        v -> {
          a.navigationMenu();
          return true;
        });
    a.content.addView(card);
    LinearLayout recent = a.ui.card(a.ui.group);
    LinearLayout heading = a.row();
    heading.addView(a.ui.icon(R.drawable.ic_activity, a.ui.secondary, 20));
    TextView title = a.ui.text("Aktivitas terbaru", 14, a.ui.text, true);
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-2, -2);
    p.leftMargin = a.dp(10);
    heading.addView(title, p);
    recent.addView(heading);
    a.ui.gap(recent, 12);
    a.activity = a.ui.text("", 13, a.ui.secondary, false);
    recent.addView(a.activity);
    recent.setOnLongClickListener(
        v -> {
          a.navigationMenu();
          return true;
        });
    a.content.addView(recent);
  }
}
