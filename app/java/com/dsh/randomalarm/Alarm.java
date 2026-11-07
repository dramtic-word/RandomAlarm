package com.dsh.randomalarm;

import org.json.JSONException;
import org.json.JSONObject;

public class Alarm {
    public long id;
    public int hour;
    public int minute;
    /** bit0 = 周一 ... bit6 = 周日；0 表示只响一次 */
    public int daysMask;
    public boolean enabled = true;
    /** 静音播放：只震动/亮屏，不发出声音 */
    public boolean silent = false;
    public boolean randomStart = true;
    /** 随机起播区间（占歌曲时长的百分比） */
    public int randomStartMin = 15;
    public int randomStartMax = 90;
    public boolean fadeIn = true;
    public int fadeSeconds = 60;
    /** 最大音量百分比 5-100，响铃渐强的上限 */
    public int maxVolume = 100;
    /** 渐强起始音量百分比 5-100（相对最大音量） */
    public int initialVolume = 15;
    /** 响铃持续秒数，0 表示一直响到手动停止 */
    public int ringSeconds = 0;
    /** 最小响铃时长（秒），0 表示不限制 */
    public int minRingSeconds = 5;
    /** 跳过本次触发的时间点（毫秒），0 表示不跳过 */
    public long skipAt = 0;
    /** 贪睡到点时间（毫秒），0 表示没有贪睡 */
    public long snoozeAt = 0;
    /** 响铃时长到点后的动作：0=停止，1=贪睡 */
    public int timeoutAction = 0;
    /** 到点贪睡的次数 */
    public int timeoutSnoozeCount = 1;
    public boolean vibrate = true;
    /** 响铃后是否自动删除这个闹钟 */
    public boolean deleteAfterRing = false;
    public String folderUri = "";
    /** 指定歌曲（content URI），空表示随机/按文件夹选 */
    public String songUri = "";
    public int snoozeMinutes = 5;
    public String label = "";

    public boolean repeats() {
        return daysMask != 0;
    }

    public String timeText() {
        return String.format("%02d:%02d", hour, minute);
    }

    public String daysText() {
        if (daysMask == 0) return "仅一次";
        if (daysMask == 0x7F) return "每天";
        String[] names = {"一", "二", "三", "四", "五", "六", "日"};
        StringBuilder sb = new StringBuilder("周");
        for (int i = 0; i < 7; i++) {
            if ((daysMask & (1 << i)) != 0) {
                if (sb.charAt(sb.length() - 1) != '周') sb.append(' ');
                sb.append(names[i]);
            }
        }
        return sb.toString();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("hour", hour);
        o.put("minute", minute);
        o.put("days", daysMask);
        o.put("enabled", enabled);
        o.put("silent", silent);
        o.put("randomStart", randomStart);
        o.put("randomStartMin", randomStartMin);
        o.put("randomStartMax", randomStartMax);
        o.put("fadeIn", fadeIn);
        o.put("fadeSeconds", fadeSeconds);
        o.put("maxVolume", maxVolume);
        o.put("initialVolume", initialVolume);
        o.put("ringSeconds", ringSeconds);
        o.put("minRingSeconds", minRingSeconds);
        o.put("skipAt", skipAt);
        o.put("snoozeAt", snoozeAt);
        o.put("timeoutAction", timeoutAction);
        o.put("timeoutSnoozeCount", timeoutSnoozeCount);
        o.put("vibrate", vibrate);
        o.put("deleteAfterRing", deleteAfterRing);
        o.put("folderUri", folderUri);
        o.put("songUri", songUri);
        o.put("snooze", snoozeMinutes);
        o.put("label", label);
        return o;
    }

    public static Alarm fromJson(JSONObject o) {
        Alarm a = new Alarm();
        a.id = o.optLong("id", System.currentTimeMillis());
        a.hour = o.optInt("hour", 7);
        a.minute = o.optInt("minute", 0);
        a.daysMask = o.optInt("days", 0) & 0x7F;
        a.enabled = o.optBoolean("enabled", true);
        a.silent = o.optBoolean("silent", false);
        a.randomStart = o.optBoolean("randomStart", true);
        a.randomStartMin = Math.max(0, Math.min(95, o.optInt("randomStartMin", 15)));
        a.randomStartMax = Math.max(5, Math.min(100, o.optInt("randomStartMax", 90)));
        a.fadeIn = o.optBoolean("fadeIn", true);
        a.fadeSeconds = Math.max(5, o.optInt("fadeSeconds", 60));
        a.maxVolume = Math.max(5, Math.min(100, o.optInt("maxVolume", 100)));
        a.initialVolume = Math.max(5, Math.min(100, o.optInt("initialVolume", 15)));
        a.ringSeconds = Math.max(0, o.optInt("ringSeconds", 0));
        a.minRingSeconds = Math.max(0, o.optInt("minRingSeconds", 5));
        a.skipAt = o.optLong("skipAt", 0);
        a.snoozeAt = o.optLong("snoozeAt", 0);
        a.timeoutAction = o.optInt("timeoutAction", 0);
        a.timeoutSnoozeCount = Math.max(1, o.optInt("timeoutSnoozeCount", 1));
        a.vibrate = o.optBoolean("vibrate", true);
        a.deleteAfterRing = o.optBoolean("deleteAfterRing", false);
        a.folderUri = o.optString("folderUri", "");
        a.songUri = o.optString("songUri", "");
        a.snoozeMinutes = Math.max(1, o.optInt("snooze", 5));
        a.label = o.optString("label", "");
        return a;
    }
}
