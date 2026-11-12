package com.pranav.spread;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Insets;
import android.os.Bundle;
import android.provider.AlarmClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Random;

public class MainActivity extends Activity {
    /** Label on every alarm, so they are easy to spot in the Clock app. */
    static final String LABEL = "Spread";

    final Random random = new Random();
    SharedPreferences prefs;
    int start, end; // minutes since midnight
    int[] preview;

    TextView fromTime, toTime, countNote, problem, setButton, tabNew, tabSets, setsNote;
    EditText count;
    GridLayout chips;
    LinearLayout setsList;
    View pageNew, pageSets, bottomBar;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        fitSystemBars(findViewById(R.id.root));

        fromTime = findViewById(R.id.from_time);
        toTime = findViewById(R.id.to_time);
        countNote = findViewById(R.id.count_note);
        problem = findViewById(R.id.problem);
        setButton = findViewById(R.id.set);
        count = findViewById(R.id.count);
        chips = findViewById(R.id.chips);
        tabNew = findViewById(R.id.tab_new);
        tabSets = findViewById(R.id.tab_sets);
        setsNote = findViewById(R.id.sets_note);
        setsList = findViewById(R.id.sets_list);
        pageNew = findViewById(R.id.page_new);
        pageSets = findViewById(R.id.page_sets);
        bottomBar = findViewById(R.id.bottom_bar);

        prefs = getSharedPreferences("spread", MODE_PRIVATE);
        start = prefs.getInt("start", 6 * 60);
        end = prefs.getInt("end", 7 * 60);
        count.setText(prefs.getString("count", ""));

        findViewById(R.id.from).setOnClickListener(v -> pickTime(true));
        findViewById(R.id.to).setOnClickListener(v -> pickTime(false));
        findViewById(R.id.shuffle).setOnClickListener(v -> reroll());
        setButton.setOnClickListener(v -> setAlarms());
        tabNew.setOnClickListener(v -> showTab(false));
        tabSets.setOnClickListener(v -> showTab(true));
        // OnePlus Clock ignores other apps' dismiss requests, so turning off happens in Clock itself.
        findViewById(R.id.turn_off).setOnClickListener(v -> send(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        count.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { reroll(); }
        });

        reroll();
        refreshSets();
        showTab(false);
    }

    void showTab(boolean sets) {
        tabNew.setSelected(!sets);
        tabSets.setSelected(sets);
        pageNew.setVisibility(sets ? View.GONE : View.VISIBLE);
        pageSets.setVisibility(sets ? View.VISIBLE : View.GONE);
        bottomBar.setVisibility(sets ? View.GONE : View.VISIBLE);
    }

    /** Rebuild the Sets tab from storage. Each entry is "start,end,count,t1 t2 ...": count "" means auto, t are the exact alarm minutes. */
    void refreshSets() {
        List<String> sets = Sets.parse(prefs.getString("sets", ""));
        setsList.removeAllViews();
        for (String set : sets) {
            String[] p = set.split(",", -1);
            if (p.length < 4) continue; // saved by 1.4 before sets kept their times; nothing to replay
            int s = Integer.parseInt(p[0]), e = Integer.parseInt(p[1]);
            int[] times = Arrays.stream(p[3].split(" ")).mapToInt(Integer::parseInt).toArray();
            View card = getLayoutInflater().inflate(R.layout.set_item, setsList, false);
            ((TextView) card.findViewById(R.id.range)).setText(fmt(s) + " – " + fmt(e));
            ((TextView) card.findViewById(R.id.detail)).setText(times.length + " alarms");
            // Sends the saved times straight to Clock and stays on this tab.
            card.findViewById(R.id.load).setOnClickListener(v -> {
                if (!sendAll(times)) return;
                prefs.edit().putString("sets", Sets.add(prefs.getString("sets", ""), set)).apply();
                refreshSets();
                toast(times.length + " alarms sent to Clock");
            });
            card.findViewById(R.id.delete).setOnClickListener(v -> {
                prefs.edit().putString("sets", Sets.remove(prefs.getString("sets", ""), set)).apply();
                refreshSets();
            });
            setsList.addView(card);
        }
        int shown = setsList.getChildCount();
        tabSets.setText(shown == 0 ? "Sets" : "Sets · " + shown);
        setsNote.setText(shown == 0
                ? "No sets yet. Each time you set alarms, those exact times are saved here so you can set them again."
                : "Tap the clock to set the same alarms again.");
    }

    /** Android 15 draws apps edge to edge; keep content clear of the status bar, nav bar and keyboard. */
    void fitSystemBars(View root) {
        int l = root.getPaddingLeft(), t = root.getPaddingTop(), r = root.getPaddingRight(), bt = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, in) -> {
            Insets i = in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
            v.setPadding(l + i.left, t + i.top, r + i.right, bt + i.bottom);
            return in;
        });
    }

    void pickTime(boolean isStart) {
        int m = isStart ? start : end;
        new TimePickerDialog(this, (tp, h, min) -> {
            if (isStart) start = h * 60 + min; else end = h * 60 + min;
            prefs.edit().putInt("start", start).putInt("end", end).apply();
            reroll();
        }, m / 60, m % 60, DateFormat.is24HourFormat(this)).show();
    }

    /** Recompute the preview from the current inputs. The Set button sends exactly what is shown. */
    void reroll() {
        String c = count.getText().toString().trim();
        boolean auto = c.isEmpty();
        int n = auto ? Spread.autoCount(start, end) : Integer.parseInt(c);
        countNote.setText(auto ? "Auto: " + n + ", about one every 5 min" : "Start and end are added on top");
        show(Spread.times(start, end, n, random));
    }

    /** Show these times as the preview; Set sends exactly them. Null means the inputs are impossible. */
    void show(int[] times) {
        preview = times;
        fromTime.setText(fmt(start));
        toTime.setText(fmt(end));
        chips.removeAllViews();
        boolean ok = preview != null;
        problem.setVisibility(ok ? View.GONE : View.VISIBLE);
        setButton.setEnabled(ok);
        if (!ok) {
            problem.setText(Spread.span(start, end) == 0
                    ? "Start and end can't be the same time."
                    : "At most " + Spread.maxBetween(start, end) + " alarms fit in between.");
            setButton.setText("Set alarms");
            return;
        }
        for (int i = 0; i < preview.length; i++) {
            TextView chip = (TextView) getLayoutInflater().inflate(R.layout.chip, chips, false);
            chip.setText(fmt(preview[i]));
            if (i == 0 || i == preview.length - 1) {
                chip.setBackgroundResource(R.drawable.bg_chip_edge);
                chip.setTextColor(getColor(R.color.on_primary));
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams(chip.getLayoutParams());
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            chips.addView(chip, lp);
        }
        setButton.setText("Set " + preview.length + " alarms");
    }

    boolean sendAll(int[] times) {
        for (int m : times) {
            boolean sent = send(new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, m / 60)
                    .putExtra(AlarmClock.EXTRA_MINUTES, m % 60)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, LABEL)
                    .putExtra(AlarmClock.EXTRA_VIBRATE, true)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true));
            if (!sent) return false;
        }
        return true;
    }

    void setAlarms() {
        if (preview == null || !sendAll(preview)) return;
        setButton.setEnabled(false); // same preview again would duplicate alarms; reroll() re-enables
        setButton.setText("Alarms set");
        String c = count.getText().toString().trim();
        if (!c.isEmpty()) c = String.valueOf(Integer.parseInt(c)); // "010" and "10" are the same set
        prefs.edit().putString("count", c)
                .putString("sets", Sets.add(prefs.getString("sets", ""), start + "," + end + "," + c + ","
                        + Arrays.stream(preview).mapToObj(String::valueOf).collect(Collectors.joining(" ")))).apply();
        refreshSets();
        toast(preview.length + " alarms sent to Clock");
    }

    boolean send(Intent i) {
        try {
            startActivity(i);
            return true;
        } catch (ActivityNotFoundException e) {
            toast("Your Clock app doesn't support this");
            return false;
        }
    }

    String fmt(int m) {
        String pattern = DateFormat.is24HourFormat(this) ? "H:mm" : "h:mm a";
        return LocalTime.of(m / 60, m % 60).format(DateTimeFormatter.ofPattern(pattern));
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }
}
