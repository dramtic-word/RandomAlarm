package com.dsh.randomalarm;

import android.content.Context;
import android.content.SharedPreferences;

/** 新建闹钟的默认配置。 */
public class Prefs {
    private static final String P = "defaults";

    private static SharedPreferences sp(Context c) {
        Context de = c.createDeviceProtectedStorageContext();
        SharedPreferences sp = de.getSharedPreferences(P, Context.MODE_PRIVATE);
        if (!sp.getBoolean("migrated_from_credential", false)) {
            try {
                SharedPreferences old = c.getSharedPreferences(P, Context.MODE_PRIVATE);
                if (sp.getAll().isEmpty()) {
                    SharedPreferences.Editor ed = sp.edit();
                    for (java.util.Map.Entry<String, ?> e : old.getAll().entrySet()) {
                        Object v = e.getValue();
                        if (v instanceof String) ed.putString(e.getKey(), (String) v);
                        else if (v instanceof Integer) ed.putInt(e.getKey(), (Integer) v);
                        else if (v instanceof Boolean) ed.putBoolean(e.getKey(), (Boolean) v);
                        else if (v instanceof Long) ed.putLong(e.getKey(), (Long) v);
                        else if (v instanceof Float) ed.putFloat(e.getKey(), (Float) v);
                    }
                    ed.apply();
                }
            } catch (Exception ignored) {
            }
            sp.edit().putBoolean("migrated_from_credential", true).apply();
        }
        return sp;
    }

    public static boolean randomStart(Context c) {
        return sp(c).getBoolean("randomStart", true);
    }

    public static int randomStartMin(Context c) {
        return Math.max(0, Math.min(95, sp(c).getInt("randomStartMin", 15)));
    }

    public static int randomStartMax(Context c) {
        return Math.max(5, Math.min(100, sp(c).getInt("randomStartMax", 90)));
    }

    public static String timerLabel(Context c) {
        return sp(c).getString("timerLabel", "");
    }

    public static int coverBrightness(Context c) {
        return Math.max(10, Math.min(100, sp(c).getInt("coverBrightness", 60)));
    }

    public static final int SWIPE_DISABLE = 0;
    public static final int SWIPE_STOP = 1;
    public static final int SWIPE_SNOOZE = 2;
    public static final int SWIPE_SKIP = 3;

    /** dir: up / down / left / right */
    public static int swipeAction(Context c, String dir) {
        int def = "up".equals(dir) ? SWIPE_STOP
                : "down".equals(dir) ? SWIPE_DISABLE : SWIPE_SKIP;
        return sp(c).getInt("swipe_" + dir, def);
    }

    public static void setSwipeAction(Context c, String dir, int v) {
        sp(c).edit().putInt("swipe_" + dir, v).apply();
    }

    public static int minRingSeconds(Context c) {
        return Math.max(0, sp(c).getInt("minRingSeconds", 5));
    }

    public static void setMinRingSeconds(Context c, int v) {
        sp(c).edit().putInt("minRingSeconds", Math.max(0, v)).apply();
    }

    public static final int ORIENT_FREE = 0;
    public static final int ORIENT_PORTRAIT = 1;
    public static final int ORIENT_LANDSCAPE = 2;

    public static int orientation(Context c) {
        return sp(c).getInt("orientation", ORIENT_FREE);
    }

    public static void setOrientation(Context c, int v) {
        sp(c).edit().putInt("orientation", v).apply();
    }

    /** 按设置强制屏幕方向；自由时跟随系统。 */
    public static void applyOrientation(android.app.Activity a) {
        int o = orientation(a);
        if (o == ORIENT_PORTRAIT) {
            a.setRequestedOrientation(
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (o == ORIENT_LANDSCAPE) {
            a.setRequestedOrientation(
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            a.setRequestedOrientation(
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    public static boolean fadeIn(Context c) {
        return sp(c).getBoolean("fadeIn", true);
    }

    public static boolean vibrate(Context c) {
        return sp(c).getBoolean("vibrate", true);
    }

    public static int snooze(Context c) {
        return Math.max(1, sp(c).getInt("snooze", 5));
    }

    public static int fadeSeconds(Context c) {
        return Math.max(5, sp(c).getInt("fadeSeconds", 60));
    }

    public static int maxVolume(Context c) {
        return Math.max(5, Math.min(100, sp(c).getInt("maxVolume", 60)));
    }

    public static int initialVolume(Context c) {
        return Math.max(5, Math.min(100, sp(c).getInt("initialVolume", 15)));
    }

    public static int ringSeconds(Context c) {
        return Math.max(0, sp(c).getInt("ringSeconds", 0));
    }

    public static int timeoutAction(Context c) {
        return sp(c).getInt("timeoutAction", 0);
    }

    public static int timeoutSnoozeCount(Context c) {
        return Math.max(1, sp(c).getInt("timeoutSnoozeCount", 1));
    }

    public static int timeoutLeft(Context c, long alarmId) {
        return sp(c).getInt("timeoutLeft_" + alarmId, -1);
    }

    public static class TimerPreset {
        public int seconds;
        public String label;

        public TimerPreset(int seconds, String label) {
            this.seconds = seconds;
            this.label = label == null ? "" : label;
        }
    }

    public static java.util.List<TimerPreset> timerPresets(Context c) {
        String raw = sp(c).getString("timerPresets", "300,600,900,1800,3600");
        java.util.List<TimerPreset> out = new java.util.ArrayList<>();
        for (String item : raw.split(",")) {
            String[] parts = item.split("\\|", 2);
            try {
                int v = Integer.parseInt(parts[0].trim());
                if (v > 0) {
                    out.add(new TimerPreset(v, parts.length > 1 ? parts[1].trim() : ""));
                }
            } catch (Exception ignored) {
            }
        }
        if (out.isEmpty()) out.add(new TimerPreset(300, ""));
        return out;
    }

    public static long timerAt(Context c) {
        return sp(c).getLong("timerAt", 0);
    }

    public static String folderUri(Context c) {
        return sp(c).getString("folderUri", "");
    }

    public static String songUri(Context c) {
        return sp(c).getString("songUri", "");
    }

    public static void setSongUri(Context c, String v) {
        sp(c).edit().putString("songUri", v == null ? "" : v).apply();
    }

    public static void setRandomStart(Context c, boolean v) {
        sp(c).edit().putBoolean("randomStart", v).apply();
    }

    public static void setRandomStartMin(Context c, int v) {
        sp(c).edit().putInt("randomStartMin", Math.max(0, Math.min(95, v))).apply();
    }

    public static void setRandomStartMax(Context c, int v) {
        sp(c).edit().putInt("randomStartMax", Math.max(5, Math.min(100, v))).apply();
    }

    public static void setTimerLabel(Context c, String v) {
        sp(c).edit().putString("timerLabel", v == null ? "" : v).apply();
    }

    public static void setCoverBrightness(Context c, int v) {
        sp(c).edit().putInt("coverBrightness", Math.max(10, Math.min(100, v))).apply();
    }

    public static void setFadeIn(Context c, boolean v) {
        sp(c).edit().putBoolean("fadeIn", v).apply();
    }

    public static void setVibrate(Context c, boolean v) {
        sp(c).edit().putBoolean("vibrate", v).apply();
    }

    public static void setSnooze(Context c, int v) {
        sp(c).edit().putInt("snooze", Math.max(1, v)).apply();
    }

    public static void setFadeSeconds(Context c, int v) {
        sp(c).edit().putInt("fadeSeconds", Math.max(5, v)).apply();
    }

    public static void setMaxVolume(Context c, int v) {
        sp(c).edit().putInt("maxVolume", Math.max(5, Math.min(100, v))).apply();
    }

    public static void setInitialVolume(Context c, int v) {
        sp(c).edit().putInt("initialVolume", Math.max(5, Math.min(100, v))).apply();
    }

    public static void setRingSeconds(Context c, int v) {
        sp(c).edit().putInt("ringSeconds", Math.max(0, v)).apply();
    }

    public static void setTimeoutAction(Context c, int v) {
        sp(c).edit().putInt("timeoutAction", v).apply();
    }

    public static void setTimeoutSnoozeCount(Context c, int v) {
        sp(c).edit().putInt("timeoutSnoozeCount", Math.max(1, v)).apply();
    }

    public static void setTimeoutLeft(Context c, long alarmId, int v) {
        sp(c).edit().putInt("timeoutLeft_" + alarmId, v).apply();
    }

    public static void setTimerPresets(Context c, java.util.List<TimerPreset> list) {
        StringBuilder sb = new StringBuilder();
        for (TimerPreset p : list) {
            if (sb.length() > 0) sb.append(',');
            sb.append(p.seconds);
            if (p.label != null && !p.label.isEmpty()) {
                sb.append('|').append(p.label.replace(',', ' ').replace('|', ' '));
            }
        }
        sp(c).edit().putString("timerPresets", sb.toString()).apply();
    }

    public static void setTimerAt(Context c, long at) {
        sp(c).edit().putLong("timerAt", at).apply();
    }

    public static void setFolderUri(Context c, String v) {
        sp(c).edit().putString("folderUri", v == null ? "" : v).apply();
    }
}
