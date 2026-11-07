package com.dsh.randomalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String a = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(a)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)) {
            Context app = context.getApplicationContext();
            int enabled = 0;
            for (Alarm alarm : AlarmStore.list(app)) {
                if (alarm.enabled) enabled++;
            }
            HistoryStore.add(app, "开机/更新", "恢复 " + enabled + " 个闹钟");
            android.util.Log.i("BootReceiver", "action=" + a + " enabled=" + enabled);
            AlarmScheduler.rescheduleAll(app);
        }
    }
}
