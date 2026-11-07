package com.dsh.randomalarm;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 闹钟列表。用 device-protected 存储，保证重启后未解锁（direct boot）时
 * BootReceiver / AlarmService 也能读到数据。
 */
public class AlarmStore {
    private static final String PREF = "randomalarm";
    private static final String KEY = "alarms";

    private static SharedPreferences prefs(Context c) {
        Context de = c.createDeviceProtectedStorageContext();
        SharedPreferences sp = de.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (!sp.getBoolean("migrated_from_credential", false)) {
            try {
                SharedPreferences old = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
                String oldRaw = old.getString(KEY, null);
                if (oldRaw != null && sp.getString(KEY, null) == null) {
                    sp.edit().putString(KEY, oldRaw).apply();
                }
            } catch (Exception ignored) {
            }
            sp.edit().putBoolean("migrated_from_credential", true).apply();
        }
        return sp;
    }

    public static List<Alarm> list(Context c) {
        List<Alarm> out = new ArrayList<>();
        String raw = prefs(c).getString(KEY, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                out.add(Alarm.fromJson(arr.getJSONObject(i)));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static Alarm get(Context c, long id) {
        for (Alarm a : list(c)) if (a.id == id) return a;
        return null;
    }

    public static void save(Context c, Alarm alarm) {
        List<Alarm> all = list(c);
        boolean replaced = false;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id == alarm.id) {
                all.set(i, alarm);
                replaced = true;
                break;
            }
        }
        if (!replaced) all.add(alarm);
        write(c, all);
    }

    public static void delete(Context c, long id) {
        List<Alarm> all = list(c);
        all.removeIf(a -> a.id == id);
        write(c, all);
    }

    public static void setEnabled(Context c, long id, boolean enabled) {
        Alarm a = get(c, id);
        if (a == null) return;
        a.enabled = enabled;
        save(c, a);
    }

    private static void write(Context c, List<Alarm> all) {
        JSONArray arr = new JSONArray();
        for (Alarm a : all) {
            try {
                arr.put(a.toJson());
            } catch (Exception ignored) {
            }
        }
        prefs(c).edit().putString(KEY, arr.toString()).apply();
    }
}
