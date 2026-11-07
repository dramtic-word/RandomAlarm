package com.dsh.randomalarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;
import java.util.List;

public class AlarmScheduler {
    public static final String ACTION_FIRE = "com.dsh.randomalarm.FIRE";
    public static final String ACTION_SNOOZE_FIRE = "com.dsh.randomalarm.SNOOZE_FIRE";

    public static PendingIntent fireIntent(Context c, long id) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction(ACTION_FIRE);
        i.putExtra("alarmId", id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, (int) (id ^ (id >>> 32)), i, flags);
    }

    public static PendingIntent preAlertIntent(Context c, long id) {
        Intent i = new Intent(c, PreAlertReceiver.class);
        i.setAction("com.dsh.randomalarm.PRE_ALERT");
        i.putExtra("alarmId", id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, (int) (id ^ (id >>> 32)) + 100000, i, flags);
    }

    public static PendingIntent snoozeIntent(Context c, long id) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction(ACTION_SNOOZE_FIRE);
        i.putExtra("alarmId", id);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getBroadcast(c, (int) (id ^ (id >>> 32)) + 200000, i, flags);
    }

    public static long nextTrigger(Alarm a, long now) {
        Calendar base = Calendar.getInstance();
        base.setTimeInMillis(now);
        for (int i = 0; i < 8; i++) {
            Calendar t = (Calendar) base.clone();
            t.add(Calendar.DAY_OF_YEAR, i);
            t.set(Calendar.HOUR_OF_DAY, a.hour);
            t.set(Calendar.MINUTE, a.minute);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);
            long ms = t.getTimeInMillis();
            if (ms <= now + 500) continue;
            if (a.skipAt > 0 && ms == a.skipAt) continue;
            if (a.daysMask == 0) return ms;
            int dow = t.get(Calendar.DAY_OF_WEEK); // 1=周日
            int bit = (dow + 5) % 7;               // 周一=0
            if ((a.daysMask & (1 << bit)) != 0) return ms;
        }
        return 0;
    }

    public static void schedule(Context c, Alarm a) {
        schedule(c, a, true);
    }

    public static void schedule(Context c, Alarm a, boolean record) {
        if (!a.enabled) {
            cancel(c, a);
            return;
        }
        long at = nextTrigger(a, System.currentTimeMillis());
        if (at <= 0) {
            cancel(c, a);
            return;
        }
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent fire = fireIntent(c, a.id);
        PendingIntent show = PendingIntent.getActivity(c, (int) a.id,
                new Intent(c, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, show), fire);
        } catch (SecurityException e) {
            // 没有精确闹钟权限时退回普通调度，至少别丢闹钟
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire);
        }
        PendingIntent pre = preAlertIntent(c, a.id);
        long preAt = at - 10 * 60_000L;
        if (preAt > System.currentTimeMillis()) {
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preAt, pre);
            } catch (Exception ignored) {
            }
        } else {
            am.cancel(pre);
        }
        if (record) {
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                    "MM-dd HH:mm", java.util.Locale.getDefault());
            HistoryStore.add(c, "调度", a.timeText() + " → " + fmt.format(new java.util.Date(at)));
        }
    }

    public static void cancel(Context c, Alarm a) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(fireIntent(c, a.id));
            am.cancel(preAlertIntent(c, a.id));
            am.cancel(snoozeIntent(c, a.id));
        }
    }

    /** 取消某个闹钟当前挂起的贪睡。 */
    public static void cancelSnooze(Context c, Alarm a) {
        if (a == null) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(snoozeIntent(c, a.id));
        a.snoozeAt = 0;
        AlarmStore.save(c, a);
    }

    public static void rescheduleAll(Context c) {
        List<Alarm> all = AlarmStore.list(c);
        for (Alarm a : all) {
            try {
                if (a.enabled) schedule(c, a, false);
                else cancel(c, a);
            } catch (Exception ignored) {
            }
        }
    }

    /** 一次性闹钟响过之后关闭；重复闹钟排下一次。 */
    public static void afterFired(Context c, Alarm a) {
        if (a == null) return;
        if (!a.repeats()) {
            a.enabled = false;
            AlarmStore.save(c, a);
            cancel(c, a);
        } else {
            schedule(c, a);
        }
    }

    public static void snooze(Context c, Alarm a, int minutes) {
        if (a == null) return;
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        long at = System.currentTimeMillis() + minutes * 60_000L;
        a.snoozeAt = at;
        AlarmStore.save(c, a);
        PendingIntent fire = snoozeIntent(c, a.id);
        try {
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(at, null), fire);
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, fire);
        }
    }
}
