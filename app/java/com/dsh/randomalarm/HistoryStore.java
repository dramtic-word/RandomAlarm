package com.dsh.randomalarm;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 响铃历史：记录每次闹钟触发、播放、失败、停止，便于排查漏响。 */
public class HistoryStore {
    private static final String P = "history";
    private static final String KEY = "events";
    private static final int MAX = 100;

    public static class Event {
        public long time;
        public String type = "";
        public String detail = "";
    }

    private static SharedPreferences sp(Context c) {
        return c.createDeviceProtectedStorageContext()
                .getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static void add(Context c, String type, String detail) {
        try {
            JSONArray old = new JSONArray(sp(c).getString(KEY, "[]"));
            JSONObject o = new JSONObject();
            o.put("t", System.currentTimeMillis());
            o.put("type", type);
            o.put("detail", detail == null ? "" : detail);
            JSONArray out = new JSONArray();
            out.put(o);
            for (int i = 0; i < old.length() && out.length() < MAX; i++) {
                out.put(old.getJSONObject(i));
            }
            sp(c).edit().putString(KEY, out.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    public static List<Event> list(Context c) {
        List<Event> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(sp(c).getString(KEY, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Event e = new Event();
                e.time = o.optLong("t", 0);
                e.type = o.optString("type", "");
                e.detail = o.optString("detail", "");
                out.add(e);
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void clear(Context c) {
        sp(c).edit().remove(KEY).apply();
    }
}
