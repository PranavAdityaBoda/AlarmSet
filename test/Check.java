import static com.pranav.spread.Spread.*;

import java.util.HashSet;
import java.util.Random;

// Run by build.sh on the desktop JVM. Fails the build if the alarm math breaks.
public class Check {
    public static void main(String[] a) {
        int[][] ranges = {{6 * 60, 7 * 60}, {23 * 60 + 30, 30}, {6 * 60, 6 * 60 + 5}, {22 * 60, 6 * 60}};
        for (int[] rg : ranges) {
            int s = rg[0], e = rg[1], span = span(s, e), max = maxBetween(s, e);
            check(times(s, e, max + 1, new Random()) == null, "too many accepted");
            check(times(s, e, -1, new Random()) == null, "negative accepted");
            for (int n = 0; n <= max; n++)
                for (int seed = 0; seed < 200; seed++) {
                    int[] t = times(s, e, n, new Random(seed));
                    check(t.length == n + 2 && t[0] == s && t[n + 1] == e, "endpoints");
                    int prev = 0;
                    for (int i = 1; i < t.length; i++) {
                        int off = span(s, t[i]) == 0 ? span : span(s, t[i]);
                        check(off > prev, "not strictly increasing");
                        prev = off;
                    }
                }
        }
        check(times(360, 360, 0, new Random()) == null, "start == end accepted");
        check(autoCount(360, 420) == 11, "auto count for one hour");
        check(maxBetween(360, 365) == 4, "max for five minutes");
        check(maxBetween(0, 720) == MAX_BETWEEN, "cap");

        // 10 alarms between 6:00 and 7:00: uneven gaps, and every 6-minute slice gets one.
        int[] t = times(360, 420, 10, new Random(7));
        HashSet<Integer> gaps = new HashSet<>();
        for (int i = 1; i < t.length; i++) gaps.add(t[i] - t[i - 1]);
        check(gaps.size() > 2, "gaps look even");
        for (int i = 1; i <= 10; i++) check(t[i] > 360 + (i - 1) * 59 / 10 && t[i] <= 360 + i * 59 / 10, "slice coverage");
        System.out.println("Check passed");
    }

    static void check(boolean ok, String what) {
        if (!ok) throw new AssertionError(what);
    }
}
