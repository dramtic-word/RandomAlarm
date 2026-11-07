package com.dsh.randomalarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        boolean snoozeFire = AlarmScheduler.ACTION_SNOOZE_FIRE.equals(action);
        if (!AlarmScheduler.ACTION_FIRE.equals(action) && !snoozeFire) return;
        long id = intent.getLongExtra("alarmId", -1);
        if (id < 0) return;

        Context app = context.getApplicationContext();
        Alarm alarm = AlarmStore.get(app, id);
        if (snoozeFire && alarm != null) {
            alarm.snoozeAt = 0;
            AlarmStore.save(app, alarm);
        }

        // 重复闹钟先排下一次，避免用户不点停止就错过明天（贪睡到点不重排）
        HistoryStore.add(app, "触发", (snoozeFire ? "贪睡到点 " : "闹钟 ")
                + (alarm != null ? alarm.timeText() : "?")
                + (alarm != null && !alarm.label.isEmpty() ? " · " + alarm.label : ""));
        if (!snoozeFire && alarm != null && alarm.repeats()) {
            AlarmScheduler.schedule(app, alarm);
        }

        Intent svc = new Intent(app, AlarmService.class);
        svc.setAction(AlarmService.ACTION_START);
        svc.putExtra(AlarmService.EXTRA_ID, id);
        try {
            if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(svc);
            else app.startService(svc);
        } catch (Exception ignored) {
        }

        // 部分系统允许从闹钟广播直接拉起界面；不允许时靠全屏通知兜底
        try {
            Intent ring = new Intent(app, RingActivity.class);
            ring.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            ring.putExtra("alarmId", id);
            app.startActivity(ring);
        } catch (Exception ignored) {
        }
    }
}
