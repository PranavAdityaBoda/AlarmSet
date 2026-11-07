package com.pranav.spread;

import java.util.Random;

/** Alarm math. No Android classes here, so build.sh can test it on the desktop JVM. Times are minutes since midnight. */
public final class Spread {
    /** Cap on alarms between start and end, so one tap can't flood the Clock app. */
    public static final int MAX_BETWEEN = 48;

    private Spread() {}

    /** Minutes from start to end, wrapping past midnight. 0 when they are equal. */
    public static int span(int start, int end) {
        return Math.floorMod(end - start, 1440);
    }

    public static int maxBetween(int start, int end) {
        return Math.min(MAX_BETWEEN, Math.max(0, span(start, end) - 1));
    }

    /** Count used when the field is empty: roughly one alarm every 5 minutes. */
    public static int autoCount(int start, int end) {
        return Math.max(0, Math.min(maxBetween(start, end), Math.round(span(start, end) / 5f) - 1));
    }

    /**
     * Start, n random minutes strictly between, then end, in time order. Null when impossible.
     * The free minutes are cut into n equal slices with one random pick per slice, so the
     * alarms are uneven but still cover the whole range and never land on the same minute.
     */
    public static int[] times(int start, int end, int n, Random r) {
        int span = span(start, end);
        if (span == 0 || n < 0 || n > maxBetween(start, end)) return null;
        int free = span - 1;
        int[] t = new int[n + 2];
        t[0] = start;
        for (int i = 0; i < n; i++) {
            int lo = i * free / n + 1, hi = (i + 1) * free / n;
            t[i + 1] = (start + lo + r.nextInt(hi - lo + 1)) % 1440;
        }
        t[n + 1] = end;
        return t;
    }
}
