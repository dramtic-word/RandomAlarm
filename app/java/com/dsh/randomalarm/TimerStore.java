package com.dsh.randomalarm;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 多个计时器（device-protected 存储，重启后仍可恢复）。 */
public class TimerStore {
    private static final String P = "timers";

    public static class Item {
        public long id;
        public long at;
        public String label = "";
    }

    private static SharedPreferences sp(Context c) {
        return c.createDeviceProtectedStorageContext()
                .getSharedPreferences(P, Context.MODE_PRIVATE);
    }

    public static List<Item> list(Context c) {
        List<Item> out = new ArrayList<>();
        String raw = sp(c).getString("list", "");
        if (raw == null || raw.isEmpty()) return out;
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Item it = new Item();
                it.id = o.optLong("id");
                it.at = o.optLong("at");
                it.label = o.optString("label", "");
                out.add(it);
            }
        } catch (Exception ignored) {
        }
        out.sort((a, b) -> Long.compare(a.at, b.at));
        return out;
    }

    public static void saveList(Context c, List<Item> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Item it : list) {
                JSONObject o = new JSONObject();
                o.put("id", it.id);
                o.put("at", it.at);
                o.put("label", it.label);
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        sp(c).edit().putString("list", arr.toString()).apply();
    }

    public static void add(Context c, Item it) {
        List<Item> list = list(c);
        list.add(it);
        saveList(c, list);
    }

    public static void remove(Context c, long id) {
        List<Item> list = list(c);
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id == id) list.remove(i);
        }
        saveList(c, list);
    }
}
