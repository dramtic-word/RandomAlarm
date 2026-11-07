package com.dsh.randomalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** 计时器到点：直接拉起响铃服务（不带闹钟配置，用默认设置）。 */
public class TimerReceiver extends BroadcastReceiver {
    public static final String ACTION_TIMER = "com.dsh.randomalarm.TIMER";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Context app = context.getApplicationContext();
        long id = intent.getLongExtra("timerId", -1);
        String label = intent.getStringExtra("timerLabel");
        if (label == null) label = "";
        if (id >= 0) TimerStore.remove(app, id);
        Prefs.setTimerLabel(app, label);
        Intent svc = new Intent(app, AlarmService.class);
        svc.setAction(AlarmService.ACTION_TIMER);
        svc.putExtra("timerLabel", label);
        try {
            if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(svc);
            else app.startService(svc);
        } catch (Exception ignored) {
        }
        try {
            Intent ring = new Intent(app, RingActivity.class);
            ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ring.putExtra("timer", true);
            app.startActivity(ring);
        } catch (Exception ignored) {
        }
    }
}
