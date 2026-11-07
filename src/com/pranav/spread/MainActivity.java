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
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class MainActivity extends Activity {
    /** Label on every alarm, so they are easy to spot in the Clock app. */
    static final String LABEL = "Spread";

    final Random random = new Random();
    SharedPreferences prefs;
    int start, end; // minutes since midnight
    int[] preview;

    TextView fromTime, toTime, countNote, problem, setButton;
    EditText count;
    GridLayout chips;

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

        prefs = getSharedPreferences("spread", MODE_PRIVATE);
        start = prefs.getInt("start", 6 * 60);
        end = prefs.getInt("end", 7 * 60);
        count.setText(prefs.getString("count", ""));

        findViewById(R.id.from).setOnClickListener(v -> pickTime(true));
        findViewById(R.id.to).setOnClickListener(v -> pickTime(false));
        findViewById(R.id.shuffle).setOnClickListener(v -> reroll());
        setButton.setOnClickListener(v -> setAlarms());
        // OnePlus Clock ignores other apps' dismiss requests, so turning off happens in Clock itself.
        findViewById(R.id.turn_off).setOnClickListener(v -> send(new Intent(AlarmClock.ACTION_SHOW_ALARMS)));
        count.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { reroll(); }
        });

        reroll();
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
        fromTime.setText(fmt(start));
        toTime.setText(fmt(end));
        String c = count.getText().toString().trim();
        boolean auto = c.isEmpty();
        int n = auto ? Spread.autoCount(start, end) : Integer.parseInt(c);
        preview = Spread.times(start, end, n, random);
        countNote.setText(auto ? "Auto: " + n + ", about one every 5 min" : "Start and end are added on top");

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

    void setAlarms() {
        if (preview == null) return;
        for (int m : preview) {
            boolean sent = send(new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, m / 60)
                    .putExtra(AlarmClock.EXTRA_MINUTES, m % 60)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, LABEL)
                    .putExtra(AlarmClock.EXTRA_VIBRATE, true)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true));
            if (!sent) return;
        }
        setButton.setEnabled(false); // same preview again would duplicate alarms; reroll() re-enables
        setButton.setText("Alarms set");
        prefs.edit().putString("count", count.getText().toString().trim()).apply();
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
