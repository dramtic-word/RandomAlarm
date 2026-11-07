package com.dsh.randomalarm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 闹钟响铃前的提醒通知（带“贪睡”和“停止”快捷操作）。 */
public class PreAlertReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "pre_alert";

    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent != null ? intent.getLongExtra("alarmId", -1) : -1;
        int minutes = intent != null ? intent.getIntExtra("preMinutes", 10) : 10;
        Alarm a = AlarmStore.get(context, id);
        String title;
        String text;
        if (a != null) {
            title = a.timeText() + " 的闹钟快到了";
            text = "还有 " + minutes + " 分钟" + (a.label.isEmpty() ? "" : " · " + a.label);
        } else {
            title = "闹钟快到了";
            text = "还有 " + minutes + " 分钟";
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "闹钟预告",
                    NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(ch);
        }
        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, (int) id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        Intent stopI = new Intent(context, PreAlertActionReceiver.class);
        stopI.setAction(PreAlertActionReceiver.ACTION_CANCEL);
        stopI.putExtra("alarmId", id);
        PendingIntent stopPi = PendingIntent.getBroadcast(context,
                (int) (id ^ (id >>> 32)) + 300001, stopI, flags);
        Intent snoozeI = new Intent(context, PreAlertActionReceiver.class);
        snoozeI.setAction(PreAlertActionReceiver.ACTION_SNOOZE);
        snoozeI.putExtra("alarmId", id);
        PendingIntent snoozePi = PendingIntent.getBroadcast(context,
                (int) (id ^ (id >>> 32)) + 300002, snoozeI, flags);

        Notification.Builder b = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setTimeoutAfter(30 * 60_000L)
                .setContentIntent(pi)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_lock_idle_alarm, "停止", stopPi).build())
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_lock_idle_alarm,
                        "贪睡" + (a != null ? " " + a.snoozeMinutes + " 分钟" : ""),
                        snoozePi).build());
        try {
            nm.notify((int) (1000 + (id & 0x7FFFFFFF)), b.build());
        } catch (Exception ignored) {
        }
    }
}
