package com.pranav.spread;

import java.util.ArrayList;
import java.util.List;

/** Saved alarm sets, newest first, stored as one string of "start,end,count" lines. count is "" for auto. */
public final class Sets {
    private Sets() {}

    public static List<String> parse(String stored) {
        List<String> list = new ArrayList<>();
        for (String s : stored.split("\n")) if (!s.isEmpty()) list.add(s);
        return list;
    }

    /** Puts the set first. An identical saved set moves to the top instead of being added twice. */
    public static String add(String stored, String set) {
        List<String> list = parse(stored);
        list.remove(set);
        list.add(0, set);
        return String.join("\n", list);
    }

    public static String remove(String stored, String set) {
        List<String> list = parse(stored);
        list.remove(set);
        return String.join("\n", list);
    }
}
